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

import static org.eclipse.smarthome.core.library.unit.MetricPrefix.*;
import static org.openhab.binding.netatmo.internal.NetatmoBindingConstants.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.netatmo.internal.channelhelper.BatteryHelper;
import org.openhab.binding.netatmo.internal.channelhelper.RadioHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractNetatmoThingHandler} is the abstract class that handles
 * common behaviors of all netatmo things
 *
 * @author Gaël L'hopital - Initial contribution OH2 version
 * @author Rob Nielsen - Added day, week, and month measurements to the weather station and modules
 *
 */
public abstract class AbstractNetatmoThingHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(AbstractNetatmoThingHandler.class);

    // Units of measurement of the data delivered by the API
    public static final Unit<Temperature> API_TEMPERATURE_UNIT = SIUnits.CELSIUS;
    public static final Unit<Dimensionless> API_HUMIDITY_UNIT = SmartHomeUnits.PERCENT;
    public static final Unit<Pressure> API_PRESSURE_UNIT = HECTO(SIUnits.PASCAL);
    public static final Unit<Speed> API_WIND_SPEED_UNIT = SIUnits.KILOMETRE_PER_HOUR;
    public static final Unit<Angle> API_WIND_DIRECTION_UNIT = SmartHomeUnits.DEGREE_ANGLE;
    public static final Unit<Length> API_RAIN_UNIT = MILLI(SIUnits.METRE);
    public static final Unit<Dimensionless> API_CO2_UNIT = SmartHomeUnits.PARTS_PER_MILLION;
    public static final Unit<Dimensionless> API_NOISE_UNIT = SmartHomeUnits.DECIBEL;

    protected final TimeZoneProvider timeZoneProvider;
    protected final MeasurableChannels measurableChannels = new MeasurableChannels();
    protected Optional<RadioHelper> radioHelper;
    protected Optional<BatteryHelper> batteryHelper;
    protected Configuration config;
    protected NetatmoBridgeHandler bridgeHandler;

    AbstractNetatmoThingHandler(@NonNull Thing thing, final TimeZoneProvider timeZoneProvider) {
        super(thing);
        this.timeZoneProvider = timeZoneProvider;
    }

    @Override
    public void initialize() {
        logger.debug("initializing handler for thing {}", getThing().getUID());
        Bridge bridge = getBridge();
        initializeThing(bridge != null ? bridge.getStatus() : null);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {} for thing {}", bridgeStatusInfo, getThing().getUID());
        initializeThing(bridgeStatusInfo.getStatus());
    }

    private void initializeThing(ThingStatus bridgeStatus) {
        Bridge bridge = getBridge();
        BridgeHandler bridgeHandler = bridge != null ? bridge.getHandler() : null;
        if (bridgeHandler != null && bridgeStatus != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                config = getThing().getConfiguration();

                radioHelper = thing.getProperties().containsKey(PROPERTY_SIGNAL_LEVELS)
                        ? Optional.of(new RadioHelper(thing.getProperties().get(PROPERTY_SIGNAL_LEVELS)))
                        : Optional.empty();
                batteryHelper = thing.getProperties().containsKey(PROPERTY_BATTERY_LEVELS)
                        ? Optional.of(new BatteryHelper(thing.getProperties().get(PROPERTY_BATTERY_LEVELS)))
                        : Optional.empty();
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Pending parent object initialization");

                initializeThing();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        }
    }

    protected abstract void initializeThing();

    protected State getNAThingProperty(@NonNull String channelId) {
        Optional<State> result;

        result = batteryHelper.flatMap(helper -> helper.getNAThingProperty(channelId));
        if (result.isPresent()) {
            return result.get();
        }
        result = radioHelper.flatMap(helper -> helper.getNAThingProperty(channelId));
        if (result.isPresent()) {
            return result.get();
        }
        result = measurableChannels.getNAThingProperty(channelId);

        return result.orElse(UnDefType.UNDEF);
    }

    protected void updateChannels() {
        updateDataChannels();

        triggerEventChannels();
    }

    private void updateDataChannels() {
        getThing().getChannels().stream().filter(channel -> !channel.getKind().equals(ChannelKind.TRIGGER))
                .forEach(channel -> {

                    String channelId = channel.getUID().getId();
                    if (isLinked(channelId)) {
                        State state = getNAThingProperty(channelId);
                        if (state != null) {
                            updateState(channel.getUID(), state);
                        }
                    }
                });
    }

    /**
     * Triggers all event/trigger channels
     * (when a channel is triggered, a rule can get all other information from the updated non-trigger channels)
     */
    private void triggerEventChannels() {
        getThing().getChannels().stream().filter(channel -> channel.getKind().equals(ChannelKind.TRIGGER))
                .forEach(channel -> triggerChannelIfRequired(channel.getUID().getId()));
    }

    /**
     * Triggers the trigger channel with the given channel id when required (when an update is available)
     *
     * @param channelId channel id
     */
    protected void triggerChannelIfRequired(@NonNull String channelId) {
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);
        measurableChannels.addChannel(channelUID);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        super.channelUnlinked(channelUID);
        measurableChannels.removeChannel(channelUID);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH) {
            logger.debug("Refreshing {}", channelUID);
            updateChannels();
        }
    }

    protected NetatmoBridgeHandler getBridgeHandler() {
        if (bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge != null) {
                bridgeHandler = (NetatmoBridgeHandler) bridge.getHandler();
            }
        }
        return bridgeHandler;
    }

    public boolean matchesId(String searchedId) {
        return searchedId != null && searchedId.equalsIgnoreCase(getId());
    }

    protected String getId() {
        if (config != null) {
            String equipmentId = (String) config.get(EQUIPMENT_ID);
            return equipmentId.toLowerCase();
        } else {
            return null;
        }
    }

    protected void updateProperties(Integer firmware, String modelId) {
        Map<String, String> properties = editProperties();
        if (firmware != null || modelId != null) {
            properties.put(Thing.PROPERTY_VENDOR, VENDOR);
        }
        if (firmware != null) {
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, firmware.toString());
        }
        if (modelId != null) {
            properties.put(Thing.PROPERTY_MODEL_ID, modelId);
        }
        updateProperties(properties);
    }

    public void updateMeasurements() {
    }

    public void getMeasurements(String device, @Nullable String module, String scale, List<String> types,
            List<String> channels, Map<String, Float> channelMeasurements) {
        NetatmoBridgeHandler handler = getBridgeHandler();
        if (handler == null) {
            return;
        }

        if (types.size() != channels.size()) {
            throw new IllegalArgumentException("types and channels lists are different sizes.");
        }

        List<Float> measurements = handler.getStationMeasureResponses(device, module, scale, types);
        if (measurements.size() != types.size()) {
            throw new IllegalArgumentException("types and measurements lists are different sizes.");
        }

        int i = 0;
        for (Float measurement : measurements) {
            channelMeasurements.put(channels.get(i++), measurement);
        }
    }

    public void addMeasurement(List<String> channels, List<String> types, String channel, String type) {
        if (isLinked(channel)) {
            channels.add(channel);
            types.add(type);
        }
    }
}
