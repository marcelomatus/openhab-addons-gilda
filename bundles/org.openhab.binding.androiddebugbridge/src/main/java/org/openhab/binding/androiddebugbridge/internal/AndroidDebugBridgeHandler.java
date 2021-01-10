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
package org.openhab.binding.androiddebugbridge.internal;

import static org.openhab.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link AndroidDebugBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class AndroidDebugBridgeHandler extends BaseThingHandler {

    public static final String KEY_EVENT_PLAY = "126";
    public static final String KEY_EVENT_PAUSE = "127";
    public static final String KEY_EVENT_NEXT = "87";
    public static final String KEY_EVENT_PREVIOUS = "88";
    public static final String KEY_EVENT_MEDIA_REWIND = "89";
    public static final String KEY_EVENT_MEDIA_FAST_FORWARD = "90";

    private final Logger logger = LoggerFactory.getLogger(AndroidDebugBridgeHandler.class);
    private final AndroidDebugBridgeDevice adbConnection;
    private int maxMediaVolume = 0;

    private @Nullable AndroidDebugBridgeConfiguration config;
    private @Nullable ScheduledFuture<?> connectionCheckerSchedule;
    private final Gson gson;
    private AndroidDebugBridgeMediaStatePackageConfig @Nullable [] packageConfigs = null;

    public AndroidDebugBridgeHandler(Thing thing, Gson gson) {
        super(thing);
        this.adbConnection = new AndroidDebugBridgeDevice(scheduler);
        this.gson = gson;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        var currentConfig = config;
        if (currentConfig == null)
            return;
        try {
            if (!adbConnection.isConnected()) {
                // try reconnect
                adbConnection.connect();
            }
            handleCommandInternal(channelUID, command);
        } catch (InterruptedException ignored) {
        } catch (AndroidDebugBridgeDeviceException | ExecutionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            adbConnection.disconnect();
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("{} - read error: {}", currentConfig.ip, e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("{} - timeout error", currentConfig.ip);
        }
    }

    private void handleCommandInternal(ChannelUID channelUID, Command command)
            throws InterruptedException, AndroidDebugBridgeDeviceException, AndroidDebugBridgeDeviceReadException,
            TimeoutException, ExecutionException {
        if (!isLinked(channelUID))
            return;
        String channelId = channelUID.getId();
        if (KEY_EVENT_CHANNEL.equals(channelId)) {
            adbConnection.sendKeyEvent(command.toFullString());
        } else if (TEXT_CHANNEL.equals(channelId)) {
            adbConnection.sendText(command.toFullString());
        } else if (MEDIA_VOLUME_CHANNEL.equals(channelId)) {
            handleMediaVolume(channelUID, command);
        } else if (MEDIA_CONTROL_CHANNEL.equals(channelId)) {
            handleMediaControlCommand(channelUID, command);
        } else if (START_PACKAGE_CHANNEL.equals(channelId)) {
            adbConnection.startPackage(command.toFullString());
            updateState(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL),
                    new StringType(command.toFullString()));
        } else if (STOP_PACKAGE_CHANNEL.equals(channelId)) {
            adbConnection.stopPackage(command.toFullString());
        } else if (CURRENT_PACKAGE_CHANNEL.equals(channelId)) {
            if (command instanceof RefreshType) {
                var packageName = adbConnection.getCurrentPackage();
                updateState(channelUID, new StringType(packageName));
            }
        } else if (WAKE_LOCK_CHANNEL.equals(channelId)) {
            if (command instanceof RefreshType) {
                int lock = adbConnection.getPowerWakeLock();
                updateState(channelUID, new DecimalType(lock));
            }
        } else if (SCREEN_STATE_CHANNEL.equals(channelId)) {
            if (command instanceof RefreshType) {
                boolean screenState = adbConnection.isScreenOn();
                updateState(channelUID, OnOffType.from(screenState));
            }
        }
    }

    private void handleMediaVolume(ChannelUID channelUID, Command command)
            throws InterruptedException, AndroidDebugBridgeDeviceReadException, AndroidDebugBridgeDeviceException,
            TimeoutException, ExecutionException {
        if (command instanceof RefreshType) {
            var volumeInfo = adbConnection.getMediaVolume();
            maxMediaVolume = volumeInfo.max;
            updateState(channelUID, new PercentType((int) Math.round(toPercent(volumeInfo.current, volumeInfo.max))));
        } else {
            if (maxMediaVolume == 0)
                return; // We can not transform percentage
            int targetVolume = Integer.parseInt(command.toFullString());
            adbConnection.setMediaVolume((int) Math.round(fromPercent(targetVolume, maxMediaVolume)));
            updateState(channelUID, new PercentType(targetVolume));
        }
    }

    private double toPercent(double value, double maxValue) {
        return (value / maxValue) * 100;
    }

    private double fromPercent(double value, double maxValue) {
        return (value / 100) * maxValue;
    }

    private void handleMediaControlCommand(ChannelUID channelUID, Command command)
            throws InterruptedException, AndroidDebugBridgeDeviceException, AndroidDebugBridgeDeviceReadException,
            TimeoutException, ExecutionException {
        if (command instanceof RefreshType) {
            boolean playing;
            String currentPackage = adbConnection.getCurrentPackage();
            var currentPackageConfig = packageConfigs != null ? Arrays.stream(packageConfigs)
                    .filter(pc -> pc.name.equals(currentPackage)).findFirst().orElse(null) : null;
            if (currentPackageConfig != null) {
                logger.debug("media stream config found for {}, mode: {}", currentPackage, currentPackageConfig.mode);
                switch (currentPackageConfig.mode) {
                    case "idle":
                        playing = false;
                        break;
                    case "wake_lock":
                        int wakeLockState = adbConnection.getPowerWakeLock();
                        playing = currentPackageConfig.wakeLockPlayStates.contains(wakeLockState);
                        break;
                    case "media_state":
                        playing = adbConnection.isPlayingMedia(currentPackage);
                        break;
                    case "audio":
                        playing = adbConnection.isPlayingAudio();
                        break;
                    default:
                        logger.warn("media state config: package {} unsupported mode", currentPackage);
                        playing = false;
                }
            } else {
                logger.debug("media stream config not found for {}", currentPackage);
                playing = adbConnection.isPlayingMedia(currentPackage);
            }
            updateState(channelUID, playing ? PlayPauseType.PLAY : PlayPauseType.PAUSE);
        } else if (command instanceof PlayPauseType) {
            if (command == PlayPauseType.PLAY) {
                adbConnection.sendKeyEvent(KEY_EVENT_PLAY);
                updateState(channelUID, PlayPauseType.PLAY);
            } else if (command == PlayPauseType.PAUSE) {
                adbConnection.sendKeyEvent(KEY_EVENT_PAUSE);
                updateState(channelUID, PlayPauseType.PAUSE);
            }
        } else if (command instanceof NextPreviousType) {
            if (command == NextPreviousType.NEXT) {
                adbConnection.sendKeyEvent(KEY_EVENT_NEXT);
            } else if (command == NextPreviousType.PREVIOUS) {
                adbConnection.sendKeyEvent(KEY_EVENT_PREVIOUS);
            }
        } else if (command instanceof RewindFastforwardType) {
            if (command == RewindFastforwardType.FASTFORWARD) {
                adbConnection.sendKeyEvent(KEY_EVENT_MEDIA_FAST_FORWARD);
            } else if (command == RewindFastforwardType.REWIND) {
                adbConnection.sendKeyEvent(KEY_EVENT_MEDIA_REWIND);
            }
        } else {
            logger.warn("Unknown media control command: {}", command);
        }
    }

    @Override
    public void initialize() {
        var currentConfig = getConfigAs(AndroidDebugBridgeConfiguration.class);
        config = currentConfig;
        var mediaStateJSONConfig = currentConfig.mediaStateJSONConfig;
        if (mediaStateJSONConfig != null && !mediaStateJSONConfig.isEmpty()) {
            loadMediaStateConfig(mediaStateJSONConfig);
        }
        adbConnection.configure(currentConfig.ip, currentConfig.port, currentConfig.timeout);
        updateStatus(ThingStatus.UNKNOWN);
        connectionCheckerSchedule = scheduler.scheduleWithFixedDelay(this::checkConnection, 0,
                currentConfig.refreshTime, TimeUnit.SECONDS);
    }

    private void loadMediaStateConfig(String mediaStateJSONConfig) {
        try {
            this.packageConfigs = gson.fromJson(mediaStateJSONConfig,
                    AndroidDebugBridgeMediaStatePackageConfig[].class);
        } catch (JsonSyntaxException e) {
            logger.warn("unable to parse media state config");
        }
    }

    @Override
    public void dispose() {
        var schedule = connectionCheckerSchedule;
        if (schedule != null) {
            schedule.cancel(true);
            connectionCheckerSchedule = null;
        }
        packageConfigs = null;
        adbConnection.disconnect();
        super.dispose();
    }

    public void checkConnection() {
        var currentConfig = config;
        if (currentConfig == null)
            return;
        try {
            logger.debug("Refresh device {} status", currentConfig.ip);
            if (adbConnection.isConnected()) {
                updateStatus(ThingStatus.ONLINE);
                refreshStatus();
            } else {
                try {
                    adbConnection.connect();
                } catch (AndroidDebugBridgeDeviceException e) {
                    logger.debug("Error connecting to device; [{}]: {}", e.getClass().getCanonicalName(),
                            e.getMessage());
                    updateStatus(ThingStatus.OFFLINE);
                    return;
                }
                if (adbConnection.isConnected()) {
                    updateStatus(ThingStatus.ONLINE);
                    refreshStatus();
                }
            }
        } catch (InterruptedException ignored) {
        } catch (AndroidDebugBridgeDeviceException | ExecutionException e) {
            logger.debug("Connection checker error: {}", e.getMessage());
            adbConnection.disconnect();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void refreshStatus() throws InterruptedException, AndroidDebugBridgeDeviceException, ExecutionException {
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), MEDIA_VOLUME_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh media volume: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Unable to refresh media volume: Timeout");
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), MEDIA_CONTROL_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh play status: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Unable to refresh play status: Timeout");
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), CURRENT_PACKAGE_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh current package: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Unable to refresh current package: Timeout");
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), WAKE_LOCK_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh wake lock: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Unable to refresh wake lock: Timeout");
        }
        try {
            handleCommandInternal(new ChannelUID(this.thing.getUID(), SCREEN_STATE_CHANNEL), RefreshType.REFRESH);
        } catch (AndroidDebugBridgeDeviceReadException e) {
            logger.warn("Unable to refresh screen state: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Unable to refresh screen state: Timeout");
        }
    }

    static class AndroidDebugBridgeMediaStatePackageConfig {
        public String name = "";
        public String mode = "";
        public List<Integer> wakeLockPlayStates = List.of();
    }
}
