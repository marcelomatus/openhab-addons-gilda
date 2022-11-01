/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.lgthinq.internal.handler;

import static org.openhab.binding.lgthinq.internal.LGThinQBindingConstants.*;

import java.util.*;
import java.util.concurrent.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lgthinq.internal.LGThinQDeviceDynStateDescriptionProvider;
import org.openhab.binding.lgthinq.internal.errors.LGThinqApiException;
import org.openhab.binding.lgthinq.lgservices.LGThinQApiClientService;
import org.openhab.binding.lgthinq.lgservices.LGThinQWMApiClientService;
import org.openhab.binding.lgthinq.lgservices.LGThinQWMApiV2ClientServiceImpl;
import org.openhab.binding.lgthinq.lgservices.model.DevicePowerState;
import org.openhab.binding.lgthinq.lgservices.model.DeviceTypes;
import org.openhab.binding.lgthinq.lgservices.model.LGDevice;
import org.openhab.binding.lgthinq.lgservices.model.washerdryer.WasherCapability;
import org.openhab.binding.lgthinq.lgservices.model.washerdryer.WasherSnapshot;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LGThinQWasherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nemer Daud - Initial contribution
 */
@NonNullByDefault
public class LGThinQWasherHandler extends LGThinQAbstractDeviceHandler<WasherCapability, WasherSnapshot> {

    private final LGThinQDeviceDynStateDescriptionProvider stateDescriptionProvider;
    private final ChannelUID stateChannelUUID;
    private final ChannelUID courseChannelUUID;
    private final ChannelUID smartCourseChannelUUID;
    private final ChannelUID downloadedCourseChannelUUID;
    private final ChannelUID temperatureChannelUUID;
    private final ChannelUID doorLockChannelUUID;
    private final ChannelUID remoteStartChannelUUID;
    private final ChannelUID standbyChannelUUID;
    @Nullable
    private WasherSnapshot lastShot;

    private final Logger logger = LoggerFactory.getLogger(LGThinQWasherHandler.class);
    @NonNullByDefault
    private final LGThinQWMApiClientService lgThinqWMApiClientService;

    // *** Long running isolated threadpools.
    private final ScheduledExecutorService pollingScheduler = Executors.newScheduledThreadPool(1);

    private final LinkedBlockingQueue<AsyncCommandParams> commandBlockQueue = new LinkedBlockingQueue<>(20);

    @NonNullByDefault

    public LGThinQWasherHandler(Thing thing, LGThinQDeviceDynStateDescriptionProvider stateDescriptionProvider) {
        super(thing, stateDescriptionProvider);
        this.stateDescriptionProvider = stateDescriptionProvider;
        lgThinqWMApiClientService = LGThinQWMApiV2ClientServiceImpl.getInstance();
        stateChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_STATE_ID);
        courseChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_COURSE_ID);
        smartCourseChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_SMART_COURSE_ID);
        downloadedCourseChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_DOWNLOADED_COURSE_ID);
        temperatureChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_TEMP_LEVEL_ID);
        doorLockChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_DOOR_LOCK_ID);
        remoteStartChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_REMOTE_START_ID);
        standbyChannelUUID = new ChannelUID(getThing().getUID(), WM_CHANNEL_STAND_BY_ID);
    }

    static class AsyncCommandParams {
        final String channelUID;
        final Command command;

        public AsyncCommandParams(String channelUUID, Command command) {
            this.channelUID = channelUUID;
            this.command = command;
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Thinq thing. Washer Thing v0.1");
        Bridge bridge = getBridge();
        initializeThing((bridge == null) ? null : bridge.getStatus());
    }

    @Override
    public void updateChannelDynStateDescription() throws LGThinqApiException {
        WasherCapability wmCap = getCapabilities();
        if (isLinked(stateChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            wmCap.getState().forEach((k, v) -> options.add(new StateOption(v, keyIfValueNotFound(CAP_WP_STATE, k))));
            stateDescriptionProvider.setStateOptions(stateChannelUUID, options);
        }
        if (isLinked(courseChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            wmCap.getCourses().forEach((k, v) -> options.add(new StateOption(k, emptyIfNull(v))));
            stateDescriptionProvider.setStateOptions(courseChannelUUID, options);
        }
        if (isLinked(smartCourseChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            wmCap.getSmartCourses().forEach((k, v) -> options.add(new StateOption(k, emptyIfNull(v))));
            stateDescriptionProvider.setStateOptions(smartCourseChannelUUID, options);
        }
        if (isLinked(downloadedCourseChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            wmCap.getSmartCourses().forEach((k, v) -> options.add(new StateOption(k, emptyIfNull(v))));
            stateDescriptionProvider.setStateOptions(downloadedCourseChannelUUID, options);
        }
        if (isLinked(temperatureChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            wmCap.getTemperature()
                    .forEach((k, v) -> options.add(new StateOption(v, keyIfValueNotFound(CAP_WP_TEMPERATURE, k))));
            stateDescriptionProvider.setStateOptions(temperatureChannelUUID, options);
        }
        if (isLinked(doorLockChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            options.add(new StateOption("0", "Unlocked"));
            options.add(new StateOption("1", "Locked"));
            stateDescriptionProvider.setStateOptions(doorLockChannelUUID, options);
        }
        if (isLinked(remoteStartChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            options.add(new StateOption("REMOTE_START_OFF", "OFF"));
            options.add(new StateOption("REMOTE_START_ON", "ON"));
            stateDescriptionProvider.setStateOptions(remoteStartChannelUUID, options);
        }
        if (getThing().getChannel(standbyChannelUUID) == null) {
            createDynChannel(WM_CHANNEL_STAND_BY_ID, standbyChannelUUID, "Switch");
        }
        if (isLinked(standbyChannelUUID)) {
            List<StateOption> options = new ArrayList<>();
            options.add(new StateOption("STANDBY_OFF", "OFF"));
            options.add(new StateOption("STANDBY_ON", "ON"));
            stateDescriptionProvider.setStateOptions(remoteStartChannelUUID, options);
        }
    }

    @Override
    public LGThinQApiClientService<WasherCapability, WasherSnapshot> getLgThinQAPIClientService() {
        return lgThinqWMApiClientService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void updateDeviceChannels(WasherSnapshot shot) {
        lastShot = shot;
        updateState(CHANNEL_POWER_ID,
                (DevicePowerState.DV_POWER_ON.equals(shot.getPowerStatus()) ? OnOffType.ON : OnOffType.OFF));
        updateState(WM_CHANNEL_STATE_ID, new StringType(shot.getState()));
        updateState(WM_CHANNEL_COURSE_ID, new StringType(shot.getCourse()));
        updateState(WM_CHANNEL_SMART_COURSE_ID, new StringType(shot.getSmartCourse()));
        updateState(WM_CHANNEL_TEMP_LEVEL_ID, new StringType(shot.getTemperatureLevel()));
        updateState(WM_CHANNEL_DOOR_LOCK_ID, new StringType(shot.getDoorLock()));
        updateState(WM_CHANNEL_REMAIN_TIME_ID, new DateTimeType(shot.getRemainingTime()));
        updateState(WM_CHANNEL_DELAY_TIME_ID, new DateTimeType(shot.getReserveTime()));
        updateState(WM_CHANNEL_DOWNLOADED_COURSE_ID, new StringType(shot.getDownloadedCourse()));
        updateState(WM_CHANNEL_STAND_BY_ID, shot.isStandBy() ? OnOffType.ON : OnOffType.OFF);
        Channel remoteStartChannel = getThing().getChannel(remoteStartChannelUUID);
        // only can have remote start channel is the WM is not in sleep mode, and remote start is enabled.
        if (shot.isRemoteStartEnabled() && !shot.isStandBy()) {
            ThingHandlerCallback callback = getCallback();
            if (remoteStartChannel == null && callback != null) {
                ChannelBuilder builder = getCallback().createChannelBuilder(remoteStartChannelUUID,
                        new ChannelTypeUID(BINDING_ID, WM_CHANNEL_REMOTE_START_ID));
                Channel newChannel = builder.build();
                ThingBuilder thingBuilder = editThing();
                updateThing(thingBuilder.withChannel(newChannel).build());
            }
            if (isLinked(remoteStartChannelUUID)) {
                updateState(WM_CHANNEL_REMOTE_START_ID, new StringType(shot.getRemoteStart()));
            }
        } else {
            if (remoteStartChannel != null) {
                ThingBuilder builder = editThing().withoutChannels(remoteStartChannel);
                updateThing(builder.build());
            }
        }
    }

    @Override
    protected DeviceTypes getDeviceType() {
        if (THING_TYPE_WASHING_MACHINE.equals(getThing().getThingTypeUID())) {
            return DeviceTypes.WASHING_MACHINE;
        } else if (THING_TYPE_WASHING_TOWER.equals(getThing().getThingTypeUID())) {
            return DeviceTypes.WASHING_TOWER;
        } else {
            throw new IllegalArgumentException(
                    "DeviceTypeUuid [" + getThing().getThingTypeUID() + "] not expected for WashingTower/Machine");
        }
    }

    @Override
    protected void processCommand(LGThinQAbstractDeviceHandler.AsyncCommandParams params) throws LGThinqApiException {
        Command command = params.command;
        switch (params.channelUID) {
            case WM_CHANNEL_REMOTE_START_ID: {
                if (command instanceof StringType) {
                    if ("START".equalsIgnoreCase(command.toString())) {
                        if (lastShot != null && !lastShot.isStandBy()) {
                            lgThinqWMApiClientService.remoteStart(getBridgeId(), getDeviceId());
                        } else {
                            logger.warn(
                                    "WM is in StandBy mode. Command START can't be sent to Remote Start channel. Ignoring");
                        }
                    } else {
                        logger.warn(
                                "Command [{}] sent to Remote Start channel is invalid. Only command START is valid.",
                                command);
                    }
                } else {
                    logger.warn("Received command different of StringType in Remote Start Channel. Ignoring");
                }
                break;
            }
            case WM_CHANNEL_STAND_BY_ID: {
                if (command instanceof OnOffType) {
                    if (OnOffType.OFF.equals(command)) {
                        if (lastShot == null || !lastShot.isStandBy()) {
                            logger.warn(
                                    "Command OFF was sent to StandBy channel, but the state of the WM is unknown or already waked up. Ignoring");
                            break;
                        }
                        lgThinqWMApiClientService.wakeUp(getBridgeId(), getDeviceId());
                    } else {
                        logger.warn("Command [{}] sent to StandBy channel is invalid. Only command OFF is valid.",
                                command);
                    }
                } else {
                    logger.warn("Received command different of OnOffType in StandBy Channel. Ignoring");
                }
                break;
            }
            default: {
                logger.error("Command {} to the channel {} not supported. Ignored.", command, params.channelUID);
            }
        }
    }

    @Override
    public void onDeviceAdded(LGDevice device) {
        // TODO - handle it. Think if it's needed
    }

    @Override
    public String getDeviceAlias() {
        return emptyIfNull(getThing().getProperties().get(DEVICE_ALIAS));
    }

    @Override
    public String getDeviceUriJsonConfig() {
        return emptyIfNull(getThing().getProperties().get(MODEL_URL_INFO));
    }

    @Override
    public void onDeviceRemoved() {
        // TODO - HANDLE IT, Think if it's needed
    }

    /**
     * Put the channels in default state if the device is disconnected or gone.
     */
    @Override
    public void onDeviceDisconnected() {
        updateState(CHANNEL_POWER_ID, OnOffType.OFF);
        updateState(WM_CHANNEL_STATE_ID, new StringType(WM_POWER_OFF_VALUE));
        updateState(WM_CHANNEL_COURSE_ID, new StringType("NOT_SELECTED"));
        updateState(WM_CHANNEL_SMART_COURSE_ID, new StringType("NOT_SELECTED"));
        updateState(WM_CHANNEL_TEMP_LEVEL_ID, new StringType("NOT_SELECTED"));
        updateState(WM_CHANNEL_DOOR_LOCK_ID, new StringType("DOOR_LOCK_OFF"));
        updateState(WM_CHANNEL_REMAIN_TIME_ID, new StringType("00:00"));
        updateState(WM_CHANNEL_DOWNLOADED_COURSE_ID, new StringType("NOT_SELECTED"));
    }
}
