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
package org.openhab.binding.volvooncall.internal.handler;

import static org.eclipse.smarthome.core.library.unit.MetricPrefix.KILO;
import static org.openhab.binding.volvooncall.internal.VolvoOnCallBindingConstants.*;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.volvooncall.internal.action.VolvoOnCallActions;
import org.openhab.binding.volvooncall.internal.config.VehicleConfiguration;
import org.openhab.binding.volvooncall.internal.dto.Attributes;
import org.openhab.binding.volvooncall.internal.dto.DoorsStatus;
import org.openhab.binding.volvooncall.internal.dto.Heater;
import org.openhab.binding.volvooncall.internal.dto.HvBattery;
import org.openhab.binding.volvooncall.internal.dto.Position;
import org.openhab.binding.volvooncall.internal.dto.Status;
import org.openhab.binding.volvooncall.internal.dto.Trip;
import org.openhab.binding.volvooncall.internal.dto.TripDetail;
import org.openhab.binding.volvooncall.internal.dto.Trips;
import org.openhab.binding.volvooncall.internal.dto.TyrePressure;
import org.openhab.binding.volvooncall.internal.dto.Vehicles;
import org.openhab.binding.volvooncall.internal.dto.WindowsStatus;
import org.openhab.binding.volvooncall.internal.wrapper.VehiclePositionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

/**
 * The {@link VehicleHandler} is responsible for handling commands, which are sent
 * to one of the channels.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class VehicleHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(VehicleHandler.class);

    private @NonNullByDefault({}) Map<String, String> activeOptions;
    private @Nullable ScheduledFuture<?> refreshJob;

    private Vehicles vehicle = new Vehicles();
    private VehiclePositionWrapper vehiclePosition = new VehiclePositionWrapper(new Position());
    private Status vehicleStatus = new Status();
    private @NonNullByDefault({}) VehicleConfiguration configuration;
    private Integer lastTripId = 0;

    public VehicleHandler(Thing thing, VehicleStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
    }

    private Map<String, String> discoverAttributes(VolvoOnCallBridgeHandler bridgeHandler, String vin)
            throws JsonSyntaxException, IOException {
        Attributes attributes = bridgeHandler.getURL(vehicle.attributesURL, Attributes.class);

        Map<String, String> properties = new HashMap<>();
        properties.put(CAR_LOCATOR, attributes.carLocatorSupported.toString());
        properties.put(HONK_AND_OR_BLINK, Boolean.toString(attributes.honkAndBlinkSupported
                && attributes.honkAndBlinkVersionsSupported.contains(HONK_AND_OR_BLINK)));
        properties.put(HONK_BLINK, Boolean.toString(
                attributes.honkAndBlinkSupported && attributes.honkAndBlinkVersionsSupported.contains(HONK_BLINK)));
        properties.put(REMOTE_HEATER, attributes.remoteHeaterSupported.toString());
        properties.put(UNLOCK, attributes.unlockSupported.toString());
        properties.put(LOCK, attributes.lockSupported.toString());
        properties.put(JOURNAL_LOG, Boolean.toString(attributes.journalLogSupported && attributes.journalLogEnabled));
        properties.put(PRECLIMATIZATION, attributes.preclimatizationSupported.toString());
        properties.put(ENGINE_START, attributes.engineStartSupported.toString());
        properties.put(UNLOCK_TIME, attributes.unlockTimeFrame.toString());

        return properties;
    }

    @Override
    public void initialize() {
        logger.trace("Initializing the Volvo On Call handler for {}", getThing().getUID());

        VolvoOnCallBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            configuration = getConfigAs(VehicleConfiguration.class);
            try {
                vehicle = bridgeHandler.getURL(SERVICE_URL + "vehicles/" + configuration.vin, Vehicles.class);

                if (thing.getProperties().isEmpty()) {
                    Map<String, String> properties = discoverAttributes(bridgeHandler, configuration.vin);
                    updateProperties(properties);
                }

                activeOptions = thing.getProperties().entrySet().stream().filter(p -> "true".equals(p.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                if (thing.getProperties().containsKey(LAST_TRIP_ID)) {
                    lastTripId = Integer.parseInt(thing.getProperties().get(LAST_TRIP_ID));
                }

                updateStatus(ThingStatus.ONLINE);
                startAutomaticRefresh(configuration.refresh);
            } catch (JsonSyntaxException | IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, e.getMessage());
            }
        }
    }

    /**
     * Start the job refreshing the vehicle data
     *
     * @param refresh : refresh frequency in minutes
     */
    private void startAutomaticRefresh(int refresh) {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob == null || refreshJob.isCancelled()) {
            refreshJob = scheduler.scheduleWithFixedDelay(this::queryApiAndUpdateChannels, 10, refresh,
                    TimeUnit.MINUTES);
        }
    }

    private void queryApiAndUpdateChannels() {
        VolvoOnCallBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            try {
                vehicleStatus = bridgeHandler.getURL(vehicle.statusURL, Status.class);
                vehiclePosition = new VehiclePositionWrapper(bridgeHandler.getURL(Position.class, configuration.vin));
                // Update all channels from the updated data
                getThing().getChannels().stream().map(Channel::getUID)
                        .filter(channelUID -> isLinked(channelUID) && !LAST_TRIP_GROUP.equals(channelUID.getGroupId()))
                        .forEach(channelUID -> {
                            State state = getValue(channelUID.getGroupId(), channelUID.getIdWithoutGroup(),
                                    vehicleStatus, vehiclePosition);

                            updateState(channelUID, state);
                        });
                updateTrips(bridgeHandler);
            } catch (JsonSyntaxException | IOException e) {
                logger.warn("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                ScheduledFuture<?> refreshJob = this.refreshJob;
                if (refreshJob != null) {
                    refreshJob.cancel(true);
                    refreshJob = null;
                }
                startAutomaticRefresh(configuration.refresh);
            }
        }
    }

    private void updateTrips(VolvoOnCallBridgeHandler bridgeHandler) throws JsonSyntaxException, IOException {
        // This seems to rewind 100 days by default, did not find any way to filter it
        Trips carTrips = bridgeHandler.getURL(Trips.class, configuration.vin);
        List<Trip> tripList = carTrips.trips;

        if (tripList != null) {
            List<Trip> newTrips = tripList.stream().filter(trip -> trip.id >= lastTripId).collect(Collectors.toList());
            Collections.reverse(newTrips);

            logger.debug("Trips discovered : {}", newTrips.size());

            if (!newTrips.isEmpty()) {
                Integer newTripId = newTrips.get(newTrips.size() - 1).id;
                if (newTripId > lastTripId) {
                    updateProperty(LAST_TRIP_ID, newTripId.toString());
                    lastTripId = newTripId;
                }

                newTrips.stream().map(t -> t.tripDetails.get(0)).forEach(catchUpTrip -> {
                    logger.debug("Trip found {}", catchUpTrip.getStartTime());
                    getThing().getChannels().stream().map(Channel::getUID).filter(
                            channelUID -> isLinked(channelUID) && LAST_TRIP_GROUP.equals(channelUID.getGroupId()))
                            .forEach(channelUID -> {
                                State state = getTripValue(channelUID.getIdWithoutGroup(), catchUpTrip);
                                updateState(channelUID, state);
                            });
                });
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelID = channelUID.getIdWithoutGroup();
        if (command instanceof RefreshType) {
            queryApiAndUpdateChannels();
        } else if (command instanceof OnOffType) {
            OnOffType onOffCommand = (OnOffType) command;
            if (ENGINE_START.equals(channelID) && onOffCommand == OnOffType.ON) {
                actionStart(5);
            } else if (REMOTE_HEATER.equals(channelID)) {
                actionHeater(onOffCommand == OnOffType.ON);
            } else if (PRECLIMATIZATION.equals(channelID)) {
                actionPreclimatization(onOffCommand == OnOffType.ON);
            } else if (CAR_LOCKED.equals(channelID)) {
                if (onOffCommand == OnOffType.ON) {
                    actionClose();
                } else {
                    actionOpen();
                }
            }
        }
    }

    private State getTripValue(String channelId, TripDetail tripDetails) {
        switch (channelId) {
            case TRIP_CONSUMPTION:
                return tripDetails.getFuelConsumption()
                        .map(value -> (State) new QuantityType<>(value.floatValue() / 100, SmartHomeUnits.LITRE))
                        .orElse(UnDefType.UNDEF);
            case TRIP_DISTANCE:
                return new QuantityType<>((double) tripDetails.distance / 1000, KILO(SIUnits.METRE));
            case TRIP_START_TIME:
                return tripDetails.getStartTime();
            case TRIP_END_TIME:
                return tripDetails.getEndTime();
            case TRIP_DURATION:
                return new QuantityType<>(tripDetails.getDurationInMinutes(), SmartHomeUnits.MINUTE);
            case TRIP_START_ODOMETER:
                return new QuantityType<>((double) tripDetails.startOdometer / 1000, KILO(SIUnits.METRE));
            case TRIP_STOP_ODOMETER:
                return new QuantityType<>((double) tripDetails.endOdometer / 1000, KILO(SIUnits.METRE));
            case TRIP_START_POSITION:
                return tripDetails.getStartPosition();
            case TRIP_END_POSITION:
                return tripDetails.getEndPosition();
        }

        return UnDefType.NULL;
    }

    private State getDoorsValue(String channelId, DoorsStatus doors) {
        switch (channelId) {
            case TAILGATE:
                return doors.tailgateOpen;
            case REAR_RIGHT:
                return doors.rearRightDoorOpen;
            case REAR_LEFT:
                return doors.rearLeftDoorOpen;
            case FRONT_RIGHT:
                return doors.frontRightDoorOpen;
            case FRONT_LEFT:
                return doors.frontLeftDoorOpen;
            case HOOD:
                return doors.hoodOpen;
        }
        return UnDefType.NULL;
    }

    private State getWindowsValue(String channelId, WindowsStatus windows) {
        switch (channelId) {
            case REAR_RIGHT_WND:
                return windows.rearRightWindowOpen;
            case REAR_LEFT_WND:
                return windows.rearLeftWindowOpen;
            case FRONT_RIGHT_WND:
                return windows.frontRightWindowOpen;
            case FRONT_LEFT_WND:
                return windows.frontLeftWindowOpen;
        }
        return UnDefType.NULL;
    }

    private State getTyresValue(String channelId, TyrePressure tyrePressure) {
        switch (channelId) {
            case REAR_RIGHT_TYRE:
                return new StringType(tyrePressure.rearRightTyrePressure);
            case REAR_LEFT_TYRE:
                return new StringType(tyrePressure.rearLeftTyrePressure);
            case FRONT_RIGHT_TYRE:
                return new StringType(tyrePressure.frontRightTyrePressure);
            case FRONT_LEFT_TYRE:
                return new StringType(tyrePressure.frontLeftTyrePressure);
        }
        return UnDefType.NULL;
    }

    private State getHeaterValue(String channelId, Heater heater) {
        switch (channelId) {
            case REMOTE_HEATER:
                return heater.status != null ? heater.status : UnDefType.UNDEF;
            case PRECLIMATIZATION:
                return heater.status != null ? heater.status : UnDefType.UNDEF;
        }
        return UnDefType.NULL;
    }

    private State getBatteryValue(String channelId, HvBattery hvBattery) {
        switch (channelId) {
            case BATTERY_LEVEL:
                return hvBattery.hvBatteryLevel != UNDEFINED
                        ? new QuantityType<>(hvBattery.hvBatteryLevel, SmartHomeUnits.PERCENT)
                        : UnDefType.UNDEF;
            case BATTERY_DISTANCE_TO_EMPTY:
                return hvBattery.distanceToHVBatteryEmpty != UNDEFINED
                        ? new QuantityType<>(hvBattery.distanceToHVBatteryEmpty, KILO(SIUnits.METRE))
                        : UnDefType.UNDEF;
            case CHARGE_STATUS:
                return hvBattery.hvBatteryChargeStatusDerived != null
                        ? new StringType(hvBattery.hvBatteryChargeStatusDerived)
                        : UnDefType.UNDEF;
            case TIME_TO_BATTERY_FULLY_CHARGED:
                return hvBattery.timeToHVBatteryFullyCharged != UNDEFINED
                        ? new QuantityType<>(hvBattery.timeToHVBatteryFullyCharged, SmartHomeUnits.MINUTE)
                        : UnDefType.UNDEF;
            case CHARGING_END:
                return hvBattery.timeToHVBatteryFullyCharged != UNDEFINED && hvBattery.timeToHVBatteryFullyCharged > 0
                        ? new DateTimeType(ZonedDateTime.now().plusMinutes(hvBattery.timeToHVBatteryFullyCharged))
                        : UnDefType.UNDEF;

        }
        return UnDefType.NULL;
    }

    private State getValue(@Nullable String groupId, String channelId, Status status, VehiclePositionWrapper position) {
        if (groupId != null) {
            switch (groupId) {
                case GROUP_DOORS:
                    if (status.getDoors().isPresent()) {
                        return getDoorsValue(channelId, status.getDoors().get());
                    }
                    break;
                case GROUP_WINDOWS:
                    if (status.getWindows().isPresent()) {
                        return getWindowsValue(channelId, status.getWindows().get());
                    }
                    break;
                case GROUP_TYRES:
                    if (status.getTyrePressure().isPresent()) {
                        return getTyresValue(channelId, status.getTyrePressure().get());
                    }
                    break;
                case GROUP_BATTERY:
                    if (status.getHvBattery().isPresent()) {
                        return getBatteryValue(channelId, status.getHvBattery().get());
                    }
                    break;
            }
        }
        switch (channelId) {
            case ODOMETER:
                return status.odometer != UNDEFINED
                        ? new QuantityType<>((double) status.odometer / 1000, KILO(SIUnits.METRE))
                        : UnDefType.UNDEF;
            case TRIPMETER1:
                return status.tripMeter1 != UNDEFINED
                        ? new QuantityType<>((double) status.tripMeter1 / 1000, KILO(SIUnits.METRE))
                        : UnDefType.UNDEF;
            case TRIPMETER2:
                return status.tripMeter2 != UNDEFINED
                        ? new QuantityType<>((double) status.tripMeter2 / 1000, KILO(SIUnits.METRE))
                        : UnDefType.UNDEF;
            case DISTANCE_TO_EMPTY:
                return status.distanceToEmpty != UNDEFINED
                        ? new QuantityType<>(status.distanceToEmpty, KILO(SIUnits.METRE))
                        : UnDefType.UNDEF;
            case FUEL_AMOUNT:
                return status.fuelAmount != UNDEFINED ? new QuantityType<>(status.fuelAmount, SmartHomeUnits.LITRE)
                        : UnDefType.UNDEF;
            case FUEL_LEVEL:
                return status.fuelAmountLevel != UNDEFINED
                        ? new QuantityType<>(status.fuelAmountLevel, SmartHomeUnits.PERCENT)
                        : UnDefType.UNDEF;
            case FUEL_CONSUMPTION:
                return status.averageFuelConsumption != UNDEFINED ? new DecimalType(status.averageFuelConsumption / 10)
                        : UnDefType.UNDEF;
            case ACTUAL_LOCATION:
                return position.getPosition();
            case CALCULATED_LOCATION:
                return position.isCalculated();
            case HEADING:
                return position.isHeading();
            case LOCATION_TIMESTAMP:
                return position.getTimestamp();
            case CAR_LOCKED:
                if (status.getCarLocked().isPresent()) {
                    return status.getCarLocked().get();
                }
                break;
            case ENGINE_RUNNING:
                if (status.getEngineRunning().isPresent()) {
                    return status.getEngineRunning().get();
                }
                break;
            case BRAKE_FLUID_LEVEL:
                return new StringType(status.brakeFluid);
            case WASHER_FLUID_LEVEL:
                return new StringType(status.washerFluidLevel);
            case AVERAGE_SPEED:
                return status.averageSpeed != UNDEFINED
                        ? new QuantityType<>(status.averageSpeed, SIUnits.KILOMETRE_PER_HOUR)
                        : UnDefType.UNDEF;
            case SERVICE_WARNING:
                return new StringType(status.serviceWarningStatus);
            case FUEL_ALERT:
                return status.distanceToEmpty < 100 ? OnOffType.ON : OnOffType.OFF;
            case REMOTE_HEATER:
            case PRECLIMATIZATION:
                if (status.getHeater().isPresent()) {
                    return getHeaterValue(channelId, status.getHeater().get());
                }
                break;
        }
        return UnDefType.NULL;
    }

    public void actionHonkBlink(Boolean honk, Boolean blink) {
        VolvoOnCallBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            StringBuilder url = new StringBuilder(SERVICE_URL + "vehicles/" + vehicle.vehicleId + "/honk_blink/");

            if (honk && blink && activeOptions.containsKey(HONK_BLINK)) {
                url.append("both");
            } else if (honk && activeOptions.containsKey(HONK_AND_OR_BLINK)) {
                url.append("horn");
            } else if (blink && activeOptions.containsKey(HONK_AND_OR_BLINK)) {
                url.append("lights");
            } else {
                logger.warn("The vehicle is not capable of this action");
                return;
            }

            try {
                bridgeHandler.postURL(url.toString(), vehiclePosition.getPositionAsJSon());
            } catch (JsonSyntaxException | IOException e) {
                logger.warn("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    private void actionOpenClose(String action, OnOffType controlState) {
        VolvoOnCallBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            if (activeOptions.containsKey(action)) {
                if (!vehicleStatus.getCarLocked().isPresent() || vehicleStatus.getCarLocked().get() != controlState) {
                    try {
                        StringBuilder address = new StringBuilder(SERVICE_URL);
                        address.append("vehicles/");
                        address.append(configuration.vin);
                        address.append("/");
                        address.append(action);
                        bridgeHandler.postURL(address.toString(), "{}");
                    } catch (JsonSyntaxException | IOException e) {
                        logger.warn("Exception occurred during execution: {}", e.getMessage(), e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    }
                } else {
                    logger.info("The car {} is already {}ed", configuration.vin, action);
                }
            } else {
                logger.warn("The car {} does not support remote {}ing", configuration.vin, action);
            }
        }
    }

    private void actionHeater(String action, Boolean start) {
        VolvoOnCallBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            if (activeOptions.containsKey(action)) {
                try {
                    if (action.contains(REMOTE_HEATER)) {
                        String command = start ? "start" : "stop";
                        String address = SERVICE_URL + "vehicles/" + configuration.vin + "/heater/" + command;
                        bridgeHandler.postURL(address, start ? "{}" : null);
                    } else if (action.contains(PRECLIMATIZATION)) {
                        String command = start ? "start" : "stop";
                        String address = SERVICE_URL + "vehicles/" + configuration.vin + "/preclimatization/" + command;
                        bridgeHandler.postURL(address, start ? "{}" : null);
                    }
                } catch (JsonSyntaxException | IOException e) {
                    logger.warn("Exception occurred during execution: {}", e.getMessage(), e);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                }
            } else {
                logger.warn("The car {} does not support {}", configuration.vin, action);
            }
        }
    }

    public void actionHeater(Boolean start) {
        actionHeater(REMOTE_HEATER, start);
    }

    public void actionPreclimatization(Boolean start) {
        actionHeater(PRECLIMATIZATION, start);
    }

    public void actionOpen() {
        actionOpenClose(UNLOCK, OnOffType.OFF);
    }

    public void actionClose() {
        actionOpenClose(LOCK, OnOffType.ON);
    }

    public void actionStart(Integer runtime) {
        VolvoOnCallBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            if (activeOptions.containsKey(ENGINE_START)) {
                String url = SERVICE_URL + "vehicles/" + vehicle.vehicleId + "/engine/start";
                String json = "{\"runtime\":" + runtime.toString() + "}";

                try {
                    bridgeHandler.postURL(url, json);
                } catch (JsonSyntaxException | IOException e) {
                    logger.warn("Exception occurred during execution: {}", e.getMessage(), e);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                }
            } else {
                logger.warn("The car {} does not support remote engine starting", vehicle.vehicleId);
            }
        }
    }

    /*
     * Called by Bridge when it has to notify this of a potential state
     * update
     *
     */
    void updateIfMatches(String vin) {
        if (vin.equalsIgnoreCase(configuration.vin)) {
            queryApiAndUpdateChannels();
        }
    }

    private @Nullable VolvoOnCallBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            BridgeHandler handler = bridge.getHandler();
            if (handler != null) {
                return (VolvoOnCallBridgeHandler) handler;
            }
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        return null;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(VolvoOnCallActions.class);
    }
}
