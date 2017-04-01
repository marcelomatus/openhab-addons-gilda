/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.sleepiq.handler;

import static org.openhab.binding.sleepiq.SleepIQBindingConstants.THING_TYPE_CLOUD;
import static org.openhab.binding.sleepiq.config.SleepIQCloudConfiguration.USERNAME;
import static org.openhab.binding.sleepiq.config.SleepIQCloudConfiguration.PASSWORD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.sleepiq.SleepIQBindingConstants;
import org.openhab.binding.sleepiq.config.SleepIQCloudConfiguration;
import org.openhab.binding.sleepiq.internal.SleepIQConfigStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syphr.sleepiq.api.Configuration;
import org.syphr.sleepiq.api.LoginException;
import org.syphr.sleepiq.api.SleepIQ;
import org.syphr.sleepiq.api.UnauthorizedException;
import org.syphr.sleepiq.api.model.Bed;
import org.syphr.sleepiq.api.model.BedStatus;
import org.syphr.sleepiq.api.model.FamilyStatus;

import com.google.common.base.Objects;

/**
 * The {@link SleepIQCloudHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Gregory Moyer - Initial contribution
 */
public class SleepIQCloudHandler extends ConfigStatusBridgeHandler {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Collections.singleton(THING_TYPE_CLOUD);

    private final Logger logger = LoggerFactory.getLogger(SleepIQCloudHandler.class);

    private List<BedStatusListener> bedStatusListeners = new CopyOnWriteArrayList<>();

    private ScheduledFuture<?> pollingJob;

    private Runnable pollingRunnable = new Runnable() {

        @Override
        public void run() {
            publishBedStatusUpdates();
        }
    };

    private SleepIQ cloud;

    public SleepIQCloudHandler(final Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {

        try {
            createCloudConnection();
        } catch (UnauthorizedException e) {
            logger.error("SleepIQ cloud authentication failed", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid SleepIQ credentials");
            return;
        } catch (LoginException e) {
            logger.error("SleepIQ cloud login failed", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "SleepIQ cloud login failed");
            return;
        } catch (Exception e) {
            logger.error("Unexpected error while communicating with SleepIQ cloud", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Unable to connect to SleepIQ cloud");
            return;
        }

        logger.debug("Setting SleepIQ cloud online");
        updateListenerManagement();
        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * Create a new SleepIQ cloud service connection. If a connection already exists, it will be lost.
     *
     * @throws LoginException if there is an error while authenticating to the service
     */
    private synchronized void createCloudConnection() throws LoginException {

        logger.debug("Reading SleepIQ cloud binding configuration");
        SleepIQCloudConfiguration bindingConfig = getConfigAs(SleepIQCloudConfiguration.class);

        logger.debug("Creating SleepIQ client");
        Configuration cloudConfig = new Configuration().withUsername(bindingConfig.username)
                .withPassword(bindingConfig.password).withLogging(bindingConfig.logging);
        cloud = SleepIQ.create(cloudConfig);

        logger.info("Authenticating to SleepIQ cloud service");
        cloud.login();

        logger.info("Authentication successful");
    }

    @Override
    public synchronized void dispose() {

        logger.debug("Disposing SleepIQ cloud handler");

        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }

        cloud = null;
    }

    /**
     * Get the cloud service API.
     *
     * @return the service object (may be <code>null</code>)
     */
    private synchronized SleepIQ getCloud() {
        return cloud;
    }

    /**
     * Start or stop a background polling job to look for bed status updates based on the whether or not there are any
     * listeners to notify.
     */
    private synchronized void updateListenerManagement() {

        if (cloud == null) {
            return;
        }

        if (!bedStatusListeners.isEmpty() && (pollingJob == null || pollingJob.isCancelled())) {
            int pollingInterval = getConfigAs(SleepIQCloudConfiguration.class).pollingInterval;
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, pollingInterval, pollingInterval,
                    TimeUnit.SECONDS);
        } else if (bedStatusListeners.isEmpty() && pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
    }

    /**
     * Retrieve the latest status on all beds and update all registered listeners.
     */
    private void publishBedStatusUpdates() {
        publishBedStatusUpdates(null);
    }

    /**
     * Retrieve the latest status on all beds and update the given listener. If the listener is <code>null</code>, all
     * registered listeners will be updated.
     *
     * @param listener the listener to update (may be <code>null</code>)
     */
    private void publishBedStatusUpdates(final BedStatusListener listener) {

        SleepIQ cloud = getCloud();
        if (cloud == null) {
            return;
        }

        FamilyStatus status = cloud.getFamilyStatus();
        for (BedStatus bedStatus : status.getBeds()) {

            if (listener != null) {
                listener.onBedStateChanged(cloud, bedStatus);
            } else {
                bedStatusListeners.stream().forEach(l -> l.onBedStateChanged(cloud, bedStatus));
            }
        }
    }

    /**
     * Register the given listener to receive bed status updates.
     *
     * @param listener the listener to register
     */
    public void registerBedStatusListener(final BedStatusListener listener) {

        if (listener == null) {
            return;
        }

        bedStatusListeners.add(listener);
        publishBedStatusUpdates(listener);
        updateListenerManagement();
    }

    /**
     * Unregister the given listener from further bed status updates.
     *
     * @param listener the listener to unregister
     * @return <code>true</code> if listener was previously registered and is now unregistered; <code>false</code>
     *         otherwise
     */
    public boolean unregisterBedStatusListener(final BedStatusListener listener) {

        boolean result = bedStatusListeners.remove(listener);
        if (result) {
            updateListenerManagement();
        }

        return result;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // all channels are read-only
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {

        Collection<ConfigStatusMessage> configStatusMessages = new ArrayList<>();

        SleepIQCloudConfiguration config = getConfigAs(SleepIQCloudConfiguration.class);
        String username = config.username;
        String password = config.password;

        if (username == null || username.isEmpty()) {
            configStatusMessages.add(ConfigStatusMessage.Builder.error(USERNAME)
                    .withMessageKeySuffix(SleepIQConfigStatusMessage.USERNAME_MISSING.getMessageKey())
                    .withArguments(USERNAME).build());
        }

        if (password == null || password.isEmpty()) {
            configStatusMessages.add(ConfigStatusMessage.Builder.error(PASSWORD)
                    .withMessageKeySuffix(SleepIQConfigStatusMessage.PASSWORD_MISSING.getMessageKey())
                    .withArguments(PASSWORD).build());
        }

        return configStatusMessages;
    }

    /**
     * Get a list of all beds registered to the cloud service account.
     *
     * @return the list of beds (never <code>null</code>)
     */
    public List<Bed> getBeds() {

        SleepIQ cloud = getCloud();
        if (cloud == null) {
            return Collections.emptyList();
        }

        return cloud.getBeds();
    }

    /**
     * Get the {@link Bed} corresponding to the given identifier.
     *
     * @param bedId the bed identifier
     * @return the identified {@link Bed} or <code>null</code> if no such bed exists
     */
    public Bed getBed(final String bedId) {

        for (Bed bed : getBeds()) {

            if (bedId.equals(bed.getBedId())) {
                return bed;
            }
        }

        return null;
    }

    /**
     * Update the given properties with attributes of the given bed. If no properties are given, a new map will be
     * created.
     *
     * @param bed the source of data
     * @param properties the properties to update (this may be <code>null</code>)
     * @return the given map (or a new map if no map was given) with updated/set properties from the supplied bed
     */
    public Map<String, String> updateProperties(final Bed bed, Map<String, String> properties) {

        if (properties == null) {
            properties = new HashMap<>();
        }

        if (bed != null) {
            properties.put(Thing.PROPERTY_MODEL_ID, bed.getModel());
            properties.put(SleepIQBindingConstants.PROPERTY_BASE, bed.getBase());
            properties.put(SleepIQBindingConstants.PROPERTY_KIDS_BED,
                    Objects.firstNonNull(bed.isKidsBed(), "").toString());
            properties.put(SleepIQBindingConstants.PROPERTY_MAC_ADDRESS, bed.getMacAddress());
            properties.put(SleepIQBindingConstants.PROPERTY_NAME, bed.getName());
            properties.put(SleepIQBindingConstants.PROPERTY_PURCHASE_DATE,
                    Objects.firstNonNull(bed.getPurchaseDate(), "").toString());
            properties.put(SleepIQBindingConstants.PROPERTY_SIZE, bed.getSize());
            properties.put(SleepIQBindingConstants.PROPERTY_SKU, bed.getSku());
        }

        return properties;
    }
}
