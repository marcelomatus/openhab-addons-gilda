/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.lgthinq.internal;

import static org.openhab.binding.lgthinq.internal.LGThinqBindingConstants.*;
import static org.openhab.core.library.types.OnOffType.ON;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lgthinq.errors.LGApiException;
import org.openhab.binding.lgthinq.errors.LGDeviceV1OfflineException;
import org.openhab.binding.lgthinq.errors.LGThinqException;
import org.openhab.binding.lgthinq.handler.LGBridgeHandler;
import org.openhab.binding.lgthinq.lgapi.LGApiClientService;
import org.openhab.binding.lgthinq.lgapi.LGApiV1ClientServiceImpl;
import org.openhab.binding.lgthinq.lgapi.LGApiV2ClientServiceImpl;
import org.openhab.binding.lgthinq.lgapi.model.*;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LGAirConditionerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nemer Daud - Initial contribution
 */
@NonNullByDefault
public class LGAirConditionerHandler extends BaseThingHandler implements LGDeviceThing {
    public static final ThingTypeUID THING_TYPE_AIR_CONDITIONER = new ThingTypeUID(BINDING_ID,
            "" + DeviceTypes.AIR_CONDITIONER.deviceTypeId()); // deviceType from AirConditioner

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_AIR_CONDITIONER);
    private String lgPlatfomType = "";
    private final Logger logger = LoggerFactory.getLogger(LGAirConditionerHandler.class);
    private final LGApiClientService lgApiClientService;
    private @Nullable LGThinqConfiguration config;
    private ThingStatus lastThingStatus = ThingStatus.UNKNOWN;
    // Bridges status that this thing must top scanning for state change
    private static final Set<ThingStatusDetail> BRIDGE_STATUS_DETAIL_ERROR = Set.of(ThingStatusDetail.BRIDGE_OFFLINE,
            ThingStatusDetail.BRIDGE_UNINITIALIZED, ThingStatusDetail.COMMUNICATION_ERROR,
            ThingStatusDetail.CONFIGURATION_ERROR);
    private static final Set<String> SUPPORTED_LG_PLATFORMS = Set.of(PLATFORM_TYPE_V1, PLATFORM_TYPE_V2);
    private @Nullable ScheduledFuture<?> thingStatePoolingJob;
    private @Nullable Future<?> commandExecutorQueueJob;
    private boolean monitorV1Began = false;
    private String monitorWorkId = "";
    private final LinkedBlockingQueue<AsyncCommandParams> commandBlockQueue = new LinkedBlockingQueue<>(20);

    public LGAirConditionerHandler(Thing thing) {
        super(thing);
        lgPlatfomType = "" + thing.getProperties().get(PLATFORM_TYPE);
        lgApiClientService = lgPlatfomType.equals(PLATFORM_TYPE_V1) ? LGApiV1ClientServiceImpl.getInstance()
                : LGApiV2ClientServiceImpl.getInstance();
    }

    static class AsyncCommandParams {
        final String channelUID;
        final Command command;

        public AsyncCommandParams(String channelUUID, Command command) {
            this.channelUID = channelUUID;
            this.command = command;
        }
    }

    class UpdateThingStatusRunnable implements Runnable {

        @Override
        public void run() {
            final String deviceId = getDeviceId();
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return super.getServices();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing hue light handler.");
        Bridge bridge = getBridge();
        initializeThing((bridge == null) ? null : bridge.getStatus());
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {}", bridgeStatusInfo);
        initializeThing(bridgeStatusInfo.getStatus());
    }

    private void initializeThing(@Nullable ThingStatus bridgeStatus) {
        logger.debug("initializeThing LQ Thinq {}. Bridge status {}", getThing().getUID(), bridgeStatus);
        String deviceId = getThing().getUID().getId();
        if (!SUPPORTED_LG_PLATFORMS.contains(lgPlatfomType)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "LG Platform [" + lgPlatfomType + "] not supported for this thing");
            return;
        }
        Bridge bridge = getBridge();
        if (!deviceId.isBlank()) {
            if (bridge != null) {
                LGBridgeHandler handler = (LGBridgeHandler) bridge.getHandler();
                // registry this thing to the bridge
                if (handler == null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
                } else {
                    handler.registryListenerThing(this);
                    if (bridgeStatus == ThingStatus.ONLINE) {
                        updateStatus(ThingStatus.ONLINE);
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                    }
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.conf-error-no-device-id");
        }
        // finally, start command queue, regardless os the thing state, as we can still try to send commands without
        // property ONLINE (the successful result from command request can put the thing in ONLINE status).
        startCommandExecutorQueueJob();
    }

    private void startCommandExecutorQueueJob() {
        if (commandExecutorQueueJob == null || commandExecutorQueueJob.isDone()) {
            commandExecutorQueueJob = scheduler.submit(new CommandExecutorRunnable());
        }
    }

    private void stopCommandExecutorQueueJob() {
        if (commandExecutorQueueJob != null) {
            commandExecutorQueueJob.cancel(true);
        }
    }

    protected void startThingStatePooling() {
        if (thingStatePoolingJob == null || thingStatePoolingJob.isDone()) {
            thingStatePoolingJob = scheduler.scheduleWithFixedDelay(() -> {
                try {
                    ACSnapShot shot = getSnapshotDeviceAdapter(getDeviceId());
                    if (!shot.isOnline()) {
                        if (getThing().getStatus() != ThingStatus.OFFLINE) {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE);
                        }
                        return;
                    }
                    if (shot.getAcOpMode() != null) {
                        updateState(CHANNEL_MOD_OP_ID, new DecimalType(shot.getOperationMode()));
                    }
                    if (shot.getAcPowerStatus() != null) {
                        updateState(CHANNEL_POWER_ID,
                                OnOffType.from(shot.getAcPowerStatus() == DevicePowerState.DV_POWER_ON));
                        // TODO - validate if is needed to change the status of the thing from OFFLINE to ONLINE (as
                        // soon as LG WebOs do)
                    }
                    if (shot.getAcFanSpeed() != null) {
                        updateState(CHANNEL_FAN_SPEED_ID, new DecimalType(shot.getAirWindStrength()));
                    }
                    if (shot.getCurrentTemperature() != null) {
                        updateState(CHANNEL_CURRENT_TEMP_ID, new DecimalType(shot.getCurrentTemperature()));
                    }
                    if (shot.getTargetTemperature() != null) {
                        updateState(CHANNEL_TARGET_TEMP_ID, new DecimalType(shot.getTargetTemperature()));
                    }
                    updateStatus(ThingStatus.ONLINE);
                } catch (LGThinqException e) {
                    logger.error("Error updating thing {}/{} from LG API. Thing goes OFFLINE until next retry.",
                            getDeviceAlias(), getDeviceId());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                }
            }, 10, DEFAULT_STATE_POOLING_UPDATE_DELAY, TimeUnit.SECONDS);
        }
    }

    private void forceStopDeviceV1Monitor(String deviceId, String workId) {
        try {
            monitorV1Began = false;
            lgApiClientService.stopMonitor(deviceId, monitorWorkId);
        } catch (Exception e) {
        }
    }

    private ACSnapShot getSnapshotDeviceAdapter(String deviceId) throws LGApiException {
        // analise de platform version
        if (PLATFORM_TYPE_V2.equals(lgPlatfomType)) {
            return lgApiClientService.getAcDeviceData(getDeviceId());
        } else {
            try {
                if (!monitorV1Began) {
                    monitorWorkId = lgApiClientService.startMonitor(getDeviceId());
                    monitorV1Began = true;
                }
            } catch (LGDeviceV1OfflineException e) {
                forceStopDeviceV1Monitor(deviceId, monitorWorkId);
                ACSnapShot shot = new ACSnapShotV1();
                shot.setOnline(false);
                return shot;
            } catch (Exception e) {
                forceStopDeviceV1Monitor(deviceId, monitorWorkId);
                throw new LGApiException("Error starting device monitor in LG API for the device:" + deviceId, e);
            }
            int retries = 3;
            ACSnapShot shot = null;
            while (retries > 0) {
                // try to get monitoring data result 3 times.
                try {
                    shot = lgApiClientService.getMonitorData(deviceId, monitorWorkId);
                    if (shot != null) {
                        return shot;
                    }
                    Thread.sleep(100);
                    retries--;
                } catch (InterruptedException | IOException e) {
                    throw new LGApiException("Error getting monitor data for the device:" + deviceId, e);
                }
            }
            // If can't get monitoring, then stop monitor and restart the process again in new interaction
            forceStopDeviceV1Monitor(deviceId, monitorWorkId);
            throw new LGApiException("Exhausted trying to get monitor data for the device:" + deviceId);
        }
    }

    protected void stopThingStatePooling() {
        if (thingStatePoolingJob != null && !thingStatePoolingJob.isDone()) {
            logger.debug("Stopping LG thinq pooling for device/alias: {}/{}", getDeviceId(), getDeviceAlias());
            thingStatePoolingJob.cancel(true);
        }
    }

    private void handleStatusChanged(ThingStatus newStatus, ThingStatusDetail statusDetail) {
        if (lastThingStatus != ThingStatus.ONLINE && newStatus == ThingStatus.ONLINE) {
            // start the thing pooling
            startThingStatePooling();
        } else if (lastThingStatus == ThingStatus.ONLINE && newStatus == ThingStatus.OFFLINE
                && BRIDGE_STATUS_DETAIL_ERROR.contains(statusDetail)) {
            // comunication error is not a specific Bridge error, then we must analise it to give
            // this thinq the change to recovery from communication errors
            if (statusDetail != ThingStatusDetail.COMMUNICATION_ERROR
                    || (getBridge() != null && getBridge().getStatus() != ThingStatus.ONLINE)) {
                // in case of status offline, I only stop the pooling if is not an COMMUNICATION_ERROR or if
                // the bridge is out
                stopThingStatePooling();
            }

        }
        lastThingStatus = newStatus;
    }

    @Override
    protected void updateStatus(ThingStatus newStatus, ThingStatusDetail statusDetail, @Nullable String description) {
        handleStatusChanged(newStatus, statusDetail);
        super.updateStatus(newStatus, statusDetail, description);
    }

    @Override
    public void onDeviceAdded(LGDevice device) {
        // TODO - handle it
    }

    @Override
    public String getDeviceId() {
        return getThing().getUID().getId();
    }

    @Override
    public String getDeviceAlias() {
        return "" + getThing().getProperties().get(DEVICE_ALIAS);
    }

    @Override
    public String getDeviceModelName() {
        return "" + getThing().getProperties().get(MODEL_NAME);
    }

    @Override
    public boolean onDeviceStateChanged() {
        // TODO - HANDLE IT
        return false;
    }

    @Override
    public void onDeviceRemoved() {
        // TODO - HANDLE IT
    }

    @Override
    public void onDeviceGone() {
        // TODO - HANDLE IT
    }

    @Override
    public void dispose() {
        if (thingStatePoolingJob != null) {
            thingStatePoolingJob.cancel(true);
            stopThingStatePooling();
            stopCommandExecutorQueueJob();
            thingStatePoolingJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // TODO - implement refresh channels
        } else {
            AsyncCommandParams params = new AsyncCommandParams(channelUID.getId(), command);
            try {
                // Ensure commands are send in a pipe per device.
                commandBlockQueue.add(params);
            } catch (IllegalStateException ex) {
                // oubound
                logger.error(
                        "Device's command queue reached the size limit. Probably the device is busy ou stuck. Ignoring command.");
                updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Device Command Queue is Busy");
            }

        }
    }

    class CommandExecutorRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {
                AsyncCommandParams params;
                try {
                    params = commandBlockQueue.take();
                } catch (InterruptedException e) {
                    logger.debug("Interrupting async command queue executor.");
                    return;
                }
                Command command = params.command;
                try {
                    switch (params.channelUID) {
                        case CHANNEL_MOD_OP_ID: {
                            if (params.command instanceof DecimalType) {
                                lgApiClientService.changeOperationMode(getDeviceId(),
                                        ACOpMode.statusOf(((DecimalType) command).doubleValue()));
                            } else {
                                logger.warn("Received command different of Numeric in Mod Operation. Ignoring");
                            }
                            break;
                        }
                        case CHANNEL_FAN_SPEED_ID: {
                            if (command instanceof DecimalType) {
                                lgApiClientService.changeFanSpeed(getDeviceId(),
                                        ACFanSpeed.statusOf(((DecimalType) command).doubleValue()));
                            } else {
                                logger.warn("Received command different of Numeric in FanSpeed Channel. Ignoring");
                            }
                            break;
                        }
                        case CHANNEL_POWER_ID: {
                            if (command instanceof OnOffType) {
                                lgApiClientService.turnDevicePower(getDeviceId(),
                                        command == ON ? DevicePowerState.DV_POWER_ON : DevicePowerState.DV_POWER_OFF);
                            } else {
                                logger.warn("Received command different of OnOffType in Power Channel. Ignoring");
                            }
                            break;
                        }
                        case CHANNEL_TARGET_TEMP_ID: {
                            double targetTemp = 0.0;
                            if (command instanceof DecimalType) {
                                targetTemp = ((DecimalType) command).doubleValue();
                            } else if (command instanceof QuantityType) {
                                targetTemp = ((QuantityType<?>) command).doubleValue();
                            } else {
                                logger.warn("Received command different of Numeric in TargetTemp Channel. Ignoring");
                                break;
                            }
                            lgApiClientService.changeTargetTemperature(getDeviceId(), ACTargetTmp.statusOf(targetTemp));
                            break;
                        }
                        default: {
                            logger.error("Command {} to the channel {} not supported. Ignored.", command.toString(),
                                    params.channelUID);
                        }
                    }
                } catch (LGThinqException e) {
                    logger.error("Error executing Command {} to the channel {}. Thing goes offline until retry",
                            command, params.channelUID);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                }
            }
        }
    }
}
