/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.netatmo.internal.handler;

import static org.openhab.binding.netatmo.internal.NetatmoBindingConstants.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.netatmo.internal.ChannelTypeUtils;
import org.openhab.binding.netatmo.internal.RefreshStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.client.model.NAPlace;
import retrofit.RetrofitError;

/**
 * {@link NetatmoDeviceHandler} is the handler for a given
 * device accessed through the Netatmo Bridge
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public abstract class NetatmoDeviceHandler<DEVICE> extends AbstractNetatmoThingHandler {

    private static final int MIN_REFRESH_INTERVAL = 2000;
    private static final int DEFAULT_REFRESH_INTERVAL = 300000;

    private final Logger logger = LoggerFactory.getLogger(NetatmoDeviceHandler.class);
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable RefreshStrategy refreshStrategy;
    protected @Nullable DEVICE device;
    protected Map<String, Object> childs = new ConcurrentHashMap<>();

    public NetatmoDeviceHandler(Thing thing, final TimeZoneProvider timeZoneProvider) {
        super(thing, timeZoneProvider);
    }

    @Override
    protected void initializeThing() {
        defineRefreshInterval();
        updateStatus(ThingStatus.ONLINE);
        scheduleRefreshJob();
    }

    private void scheduleRefreshJob() {
        RefreshStrategy strategy = refreshStrategy;
        if (strategy == null) {
            return;
        }
        long delay = strategy.nextRunDelayInS();
        logger.debug("Scheduling update channel thread in {} s", delay);
        refreshJob = scheduler.schedule(() -> {
            updateChannels();
            ScheduledFuture<?> job = refreshJob;
            if (job != null) {
                logger.debug("cancel refresh job");
                job.cancel(false);
                refreshJob = null;
            }
            scheduleRefreshJob();
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        ScheduledFuture<?> job = refreshJob;
        if (job != null) {
            logger.debug("cancel refresh job");
            job.cancel(true);
            refreshJob = null;
        }
    }

    protected abstract @Nullable DEVICE updateReadings();

    protected void updateProperties(DEVICE deviceData) {
    }

    @Override
    protected void updateChannels() {
        RefreshStrategy strategy = refreshStrategy;
        if (strategy != null) {
            logger.debug("Data aged of {} s", strategy.dataAge() / 1000);
            if (strategy.isDataOutdated()) {
                logger.debug("Trying to update channels on device {}", getId());
                childs.clear();

                @Nullable
                DEVICE newDeviceReading = null;
                try {
                    newDeviceReading = updateReadings();
                } catch (RetrofitError e) {
                    if (logger.isDebugEnabled()) {
                        // we also attach the stack trace
                        logger.error("Unable to connect Netatmo API : {}", e.getMessage(), e);
                    } else {
                        logger.error("Unable to connect Netatmo API : {}", e.getMessage());
                    }
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Unable to connect Netatmo API : " + e.getLocalizedMessage());
                }
                NetatmoBridgeHandler bridgeHandler = getBridgeHandler();
                if (newDeviceReading != null) {
                    updateStatus(ThingStatus.ONLINE);
                    logger.debug("Successfully updated device {} readings! Now updating channels", getId());
                    DEVICE theDevice = newDeviceReading;
                    this.device = theDevice;
                    updateProperties(theDevice);
                    Integer dataTimeStamp = getDataTimestamp();
                    if (dataTimeStamp != null) {
                        strategy.setDataTimeStamp(dataTimeStamp, timeZoneProvider.getTimeZone());
                    }
                    radioHelper.ifPresent(helper -> helper.setModule(theDevice));
                    if (bridgeHandler != null) {
                        bridgeHandler.checkForNewThings(newDeviceReading);
                    }
                } else {
                    logger.debug("Failed to update device {} readings! Skip updating channels", getId());
                }
                // Be sure that all channels for the modules will be updated with refreshed data
                if (bridgeHandler != null) {
                    childs.forEach((childId, moduleData) -> {
                        Optional<AbstractNetatmoThingHandler> childHandler = bridgeHandler.findNAThing(childId);
                        childHandler.map(NetatmoModuleHandler.class::cast).ifPresent(naChildModule -> {
                            naChildModule.setRefreshRequired(true);
                        });
                    });
                }
            } else {
                logger.debug("Data still valid for device {}", getId());
            }
            super.updateChannels();
            updateChildModules();
        }
    }

    @Override
    protected State getNAThingProperty(String channelId) {
        try {
            @Nullable
            DEVICE theDevice = device;
            switch (channelId) {
                case CHANNEL_LAST_STATUS_STORE:
                    if (theDevice != null) {
                        Method getLastStatusStore = theDevice.getClass().getMethod("getLastStatusStore");
                        Integer lastStatusStore = (Integer) getLastStatusStore.invoke(theDevice);
                        return ChannelTypeUtils.toDateTimeType(lastStatusStore, timeZoneProvider.getTimeZone());
                    } else {
                        return UnDefType.UNDEF;
                    }
                case CHANNEL_LOCATION:
                    if (theDevice != null) {
                        Method getPlace = theDevice.getClass().getMethod("getPlace");
                        NAPlace place = (NAPlace) getPlace.invoke(theDevice);
                        PointType point = new PointType(new DecimalType(place.getLocation().get(1)),
                                new DecimalType(place.getLocation().get(0)));
                        if (place.getAltitude() != null) {
                            point.setAltitude(new DecimalType(place.getAltitude()));
                        }
                        return point;
                    } else {
                        return UnDefType.UNDEF;
                    }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.debug("The device has no method to access {} property ", channelId);
            return UnDefType.NULL;
        }

        return super.getNAThingProperty(channelId);
    }

    private void updateChildModules() {
        NetatmoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            logger.debug("Updating child modules of {}", getId());
            childs.forEach((childId, moduleData) -> {
                Optional<AbstractNetatmoThingHandler> childHandler = bridgeHandler.findNAThing(childId);
                childHandler.map(NetatmoModuleHandler.class::cast).ifPresent(naChildModule -> {
                    logger.debug("Updating child module {}", naChildModule.getId());
                    naChildModule.updateChannels(moduleData);
                });
            });
        }
    }

    /*
     * Sets the refresh rate of the device depending whether it's a property
     * of the thing or if it's defined by configuration
     */
    private void defineRefreshInterval() {
        BigDecimal dataValidityPeriod;
        if (thing.getProperties().containsKey(PROPERTY_REFRESH_PERIOD)) {
            String refreshPeriodProperty = thing.getProperties().get(PROPERTY_REFRESH_PERIOD);
            if ("auto".equalsIgnoreCase(refreshPeriodProperty)) {
                dataValidityPeriod = new BigDecimal(-1);
            } else {
                dataValidityPeriod = new BigDecimal(refreshPeriodProperty);
            }
        } else {
            Object interval = config.get(REFRESH_INTERVAL);
            if (interval instanceof BigDecimal) {
                dataValidityPeriod = (BigDecimal) interval;
                if (dataValidityPeriod.intValue() < MIN_REFRESH_INTERVAL) {
                    logger.info(
                            "Refresh interval setting is too small for thing {}, {} ms is considered as refresh interval.",
                            thing.getUID(), MIN_REFRESH_INTERVAL);
                    dataValidityPeriod = new BigDecimal(MIN_REFRESH_INTERVAL);
                }
            } else {
                dataValidityPeriod = new BigDecimal(DEFAULT_REFRESH_INTERVAL);
            }
        }
        refreshStrategy = new RefreshStrategy(dataValidityPeriod.intValue());
    }

    protected abstract @Nullable Integer getDataTimestamp();

    public void expireData() {
        RefreshStrategy strategy = refreshStrategy;
        if (strategy != null) {
            strategy.expireData();
        }
    }
}
