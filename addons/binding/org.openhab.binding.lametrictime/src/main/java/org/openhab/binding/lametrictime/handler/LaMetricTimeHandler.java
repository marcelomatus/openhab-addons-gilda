/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lametrictime.handler;

import static org.openhab.binding.lametrictime.LaMetricTimeBindingConstants.*;
import static org.openhab.binding.lametrictime.config.LaMetricTimeConfiguration.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.lametrictime.LaMetricTimeBindingConstants;
import org.openhab.binding.lametrictime.config.LaMetricTimeConfiguration;
import org.openhab.binding.lametrictime.internal.LaMetricTimeConfigStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syphr.lametrictime.api.Configuration;
import org.syphr.lametrictime.api.LaMetricTime;
import org.syphr.lametrictime.api.common.impl.GsonGenerator;
import org.syphr.lametrictime.api.local.NotificationCreationException;
import org.syphr.lametrictime.api.local.UpdateException;
import org.syphr.lametrictime.api.local.model.Application;
import org.syphr.lametrictime.api.local.model.Audio;
import org.syphr.lametrictime.api.local.model.Bluetooth;
import org.syphr.lametrictime.api.local.model.Device;
import org.syphr.lametrictime.api.local.model.Display;
import org.syphr.lametrictime.api.local.model.Notification;
import org.syphr.lametrictime.api.model.enums.BrightnessMode;

import com.google.gson.Gson;

/**
 * The {@link LaMetricTimeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Gregory Moyer - Initial contribution
 */
public class LaMetricTimeHandler extends ConfigStatusBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(LaMetricTimeHandler.class);

    private LaMetricTime clock;

    public LaMetricTimeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Reading LaMetric Time binding configuration");
        LaMetricTimeConfiguration bindingConfig = getConfigAs(LaMetricTimeConfiguration.class);

        logger.debug("Creating LaMetric Time client");
        Configuration clockConfig = new Configuration().withDeviceHost(bindingConfig.host)
                .withDeviceApiKey(bindingConfig.apiKey).withLogging(logger.isDebugEnabled());
        clock = LaMetricTime.create(clockConfig);

        logger.debug("Verifying communication with LaMetric Time");
        try {
            Device device = clock.getLocalApi().getDevice();
            if (device == null) {
                logger.debug("Failed to communicate with LaMetric Time");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Unable to connect to LaMetric Time");
                return;
            }

            updateProperties(device);
        } catch (Exception e) {
            logger.debug("Failed to communicate with LaMetric Time", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Unable to connect to LaMetric Time");
            return;
        }

        logger.debug("Setting LaMetric Time online");
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        clock = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);

        try {
            switch (channelUID.getId()) {
                case CHANNEL_NOTIFICATIONS_INFO:
                case CHANNEL_NOTIFICATIONS_ALERT:
                case CHANNEL_NOTIFICATIONS_WARN:
                    handleNotificationsCommand(channelUID, command);
                    break;
                case CHANNEL_NOTIFICATIONS_ADVANCED:
                    handleNotificationsAdvanceCommand(channelUID, command);
                    break;
                case CHANNEL_DISPLAY_BRIGHTNESS:
                case CHANNEL_DISPLAY_BRIGHTNESS_MODE:
                    handleBrightnessChannel(channelUID, command);
                    break;
                case CHANNEL_BLUETOOTH_ACTIVE:
                case CHANNEL_BLUETOOTH_AVAILABLE:
                case CHANNEL_BLUETOOTH_DISCOVERABLE:
                case CHANNEL_BLUETOOTH_MAC:
                case CHANNEL_BLUETOOTH_NAME:
                case CHANNEL_BLUETOOTH_PAIRABLE:
                    handleBluetoothCommand(channelUID, command);
                    break;
                case CHANNEL_AUDIO_VOLUME:
                    handleAudioCommand(channelUID, command);
                    break;
                default:
                    logger.warn("Channel '{}' not supported", channelUID);
                    break;
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (NotificationCreationException e) {
            logger.error("Failed to create notification - taking clock offline", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void handleNotificationsAdvanceCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Skipping refresh command for notifications");
            return;
        }
        String jsonCommand = command.toString();
        try {
            logger.debug("Send advanced notification: {}", jsonCommand);
            Gson gson = GsonGenerator.create(true);
            Notification notification = gson.fromJson(jsonCommand, Notification.class);
            String response = clock.getLocalApi().createNotification(notification);
            logger.debug("Advanced notification response was {}", response);
        } catch (NotificationCreationException e) {
            logger.error("Could not send advanced notification: {}", jsonCommand, e);
        }
    }

    private void handleNotificationsCommand(ChannelUID channelUID, Command command)
            throws NotificationCreationException {
        if (command instanceof RefreshType) {
            logger.debug("Skipping refresh command for notifications");
            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_NOTIFICATIONS_INFO:
                clock.notifyInfo(command.toString());
                break;
            case CHANNEL_NOTIFICATIONS_WARN:
                clock.notifyWarning(command.toString());
                break;
            case CHANNEL_NOTIFICATIONS_ALERT:
                clock.notifyCritical(command.toString());
                break;
            default:
                logger.error("Invalid notification channel: {}", channelUID);
        }
    }

    private void handleAudioCommand(ChannelUID channelUID, Command command) {
        Audio audio = clock.getLocalApi().getAudio();
        if (command instanceof RefreshType) {
            updateState(channelUID, new PercentType(audio.getVolume()));
        } else if (command instanceof PercentType) {
            try {
                PercentType percentTypeCommand = (PercentType) command;
                int volume = percentTypeCommand.intValue();
                if (volume >= 0 && volume != audio.getVolume()) {
                    audio.setVolume(volume);
                    clock.getLocalApi().updateAudio(audio);
                }
            } catch (UpdateException e) {
                logger.error("Failed to update audio volume - taking clock offline", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    private void handleBluetoothCommand(ChannelUID channelUID, Command command) {
        Bluetooth bluetooth = clock.getLocalApi().getBluetooth();
        if (command instanceof RefreshType) {
            readBluetoothValue(channelUID, bluetooth);
        } else {
            updateBluetoothValue(channelUID, command, bluetooth);
        }
    }

    private void updateBluetoothValue(ChannelUID channelUID, Command command, Bluetooth bluetooth) {
        try {
            if (command instanceof OnOffType && channelUID.getId().equals(CHANNEL_BLUETOOTH_ACTIVE)) {
                OnOffType onOffCommand = (OnOffType) command;
                if (onOffCommand == OnOffType.ON && !bluetooth.isActive()) {
                    bluetooth.setActive(true);
                    clock.getLocalApi().updateBluetooth(bluetooth);
                } else if (bluetooth.isActive()) {
                    bluetooth.setActive(false);
                    clock.getLocalApi().updateBluetooth(bluetooth);
                }
            } else if (command instanceof StringType && channelUID.getId().equals(CHANNEL_BLUETOOTH_NAME)) {
                StringType stringCommand = (StringType) command;
                if (!bluetooth.getName().equals(stringCommand.toString())) {
                    bluetooth.setName(stringCommand.toString());
                    clock.getLocalApi().updateBluetooth(bluetooth);
                }
            }
        } catch (UpdateException e) {
            logger.error("Failed to update bluetooth - taking clock offline", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void readBluetoothValue(ChannelUID channelUID, Bluetooth bluetooth) {
        switch (channelUID.getId()) {
            case CHANNEL_BLUETOOTH_ACTIVE:
                if (bluetooth.isActive()) {
                    updateState(channelUID, OnOffType.ON);
                } else {
                    updateState(channelUID, OnOffType.OFF);
                }
                break;
            case CHANNEL_BLUETOOTH_AVAILABLE:
                if (bluetooth.isAvailable()) {
                    updateState(channelUID, OnOffType.ON);
                } else {
                    updateState(channelUID, OnOffType.OFF);
                }
                break;
            case CHANNEL_BLUETOOTH_DISCOVERABLE:
                if (bluetooth.isDiscoverable()) {
                    updateState(channelUID, OnOffType.ON);
                } else {
                    updateState(channelUID, OnOffType.OFF);
                }
                break;
            case CHANNEL_BLUETOOTH_MAC:
                updateState(channelUID, new StringType(bluetooth.getMac()));
                break;
            case CHANNEL_BLUETOOTH_NAME:
                updateState(channelUID, new StringType(bluetooth.getName()));
                break;
            case CHANNEL_BLUETOOTH_PAIRABLE:
                if (bluetooth.isPairable()) {
                    updateState(channelUID, OnOffType.ON);
                } else {
                    updateState(channelUID, OnOffType.OFF);
                }
                break;
        }
    }

    private void handleBrightnessChannel(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            readDisplayValue(channelUID, clock.getLocalApi().getDisplay());
        } else {
            updateDisplayValue(channelUID, command);
        }
    }

    private void updateDisplayValue(ChannelUID channelUID, Command command) {
        try {
            if (channelUID.getId().equals(CHANNEL_DISPLAY_BRIGHTNESS)) {
                if (command instanceof PercentType) {
                    int brightness = ((PercentType) command).intValue();
                    logger.debug("Set Brightness to {}.", brightness);
                    Display newDisplay = clock.setBrightness(brightness);
                    updateState(CHANNEL_DISPLAY_BRIGHTNESS_MODE, new StringType(newDisplay.getBrightnessMode()));
                } else {
                    logger.debug("Unsupported command {} for display brightness! Supported commands: REFRESH", command);
                }
            } else if (channelUID.getId().equals(CHANNEL_DISPLAY_BRIGHTNESS_MODE)) {
                if (command instanceof StringType) {
                    BrightnessMode mode = BrightnessMode.toEnum(command.toFullString());
                    if (mode == null) {
                        logger.warn("Unknown brightness mode: {}", command);
                    } else {
                        clock.setBrightnessMode(mode);
                    }
                } else {
                    logger.debug("Unsupported command {} for display brightness! Supported commands: REFRESH", command);
                }
            }
        } catch (UpdateException e) {
            logger.error("Failed to update display - taking clock offline", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void readDisplayValue(ChannelUID channelUID, Display display) {
        if (channelUID.getId().equals(CHANNEL_DISPLAY_BRIGHTNESS)) {
            int brightness = display.getBrightness();
            State state = new PercentType(brightness);
            updateState(channelUID, state);
        } else if (channelUID.getId().equals(CHANNEL_DISPLAY_BRIGHTNESS_MODE)) {
            String mode = display.getBrightnessMode();
            StringType state = new StringType(mode);
            updateState(channelUID, state);
        }
    }

    private void updateProperties(Device device) {
        Map<String, String> properties = editProperties();
        properties.put(Thing.PROPERTY_SERIAL_NUMBER, device.getSerialNumber());
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, device.getOsVersion());
        properties.put(Thing.PROPERTY_MODEL_ID, device.getModel());
        properties.put(LaMetricTimeBindingConstants.PROPERTY_ID, device.getId());
        properties.put(LaMetricTimeBindingConstants.PROPERTY_NAME, device.getName());
        updateProperties(properties);
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        Collection<ConfigStatusMessage> configStatusMessages = new ArrayList<>();

        LaMetricTimeConfiguration config = getConfigAs(LaMetricTimeConfiguration.class);
        String host = config.host;
        String apiKey = config.apiKey;

        if (StringUtils.isEmpty(host)) {
            configStatusMessages.add(ConfigStatusMessage.Builder.error(HOST)
                    .withMessageKeySuffix(LaMetricTimeConfigStatusMessage.HOST_MISSING).withArguments(HOST).build());
        }

        if (StringUtils.isEmpty(apiKey)) {
            configStatusMessages.add(ConfigStatusMessage.Builder.error(API_KEY)
                    .withMessageKeySuffix(LaMetricTimeConfigStatusMessage.API_KEY_MISSING).withArguments(API_KEY)
                    .build());
        }

        return configStatusMessages;
    }

    protected LaMetricTime getClock() {
        return clock;
    }

    public SortedMap<String, Application> getApps() {
        return getClock().getLocalApi().getApplications();
    }
}
