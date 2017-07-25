/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wifiled.handler;

import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.wifiled.WiFiLEDBindingConstants;
import org.openhab.binding.wifiled.configuration.WiFiLEDConfig;
import org.openhab.binding.wifiled.handler.AbstractWiFiLEDDriver.Protocol;
import org.openhab.binding.wifiled.handler.AbstractWiFiLEDDriver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link WiFiLEDHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Osman Basha - Initial contribution
 * @author Ries van Twisk
 */
public class WiFiLEDHandler extends BaseThingHandler {

    private static final int INC_DEC_STEP = 10;

    private Logger logger = LoggerFactory.getLogger(WiFiLEDHandler.class);
    private AbstractWiFiLEDDriver driver;
    private ScheduledFuture<?> pollingJob;

    public WiFiLEDHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing WiFiLED handler '{}'", getThing().getUID());

        WiFiLEDConfig config = getConfigAs(WiFiLEDConfig.class);

        int port = (config.getPort() == null) ? AbstractWiFiLEDDriver.DEFAULT_PORT : config.getPort();
        Protocol protocol = config.getProtocol() == null ? Protocol.LD382A : Protocol.valueOf(config.getProtocol());
        Driver driverName = config.getDriver() == null ? Driver.CLASSIC : Driver.valueOf(config.getDriver());

        switch (driverName) {
            case CLASSIC:
                driver = new ClassicWiFiLEDDriver(config.getIp(), port, protocol);
                break;

            case FADING:
                int fadeDurationInMs = config.getFadeDurationInMs() == null ? FadingWiFiLEDDriver.DEFAULT_FADE_DURATION_IN_MS : config.getFadeDurationInMs();
                int fadeSteps = config.getFadeSteps() == null ? FadingWiFiLEDDriver.DEFAULT_FADE_STEPS : config.getFadeSteps();
                driver = new FadingWiFiLEDDriver(config.getIp(), port, protocol, fadeDurationInMs, fadeSteps);
                break;
        }

        try {
            driver.init();

            logger.debug("Found a WiFi LED device '{}'", getThing().getUID());
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, e.getMessage());
            return;
        }
        updateStatus(ThingStatus.ONLINE);

        int pollingPeriod = (config.getPollingPeriod() == null) ? 30 : config.getPollingPeriod();
        if (pollingPeriod > 0) {
            pollingJob = scheduler.scheduleWithFixedDelay(() -> update(), 0, pollingPeriod, TimeUnit.SECONDS);
            logger.debug("Polling job scheduled to run every {} sec. for '{}'", pollingPeriod, getThing().getUID());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing WiFiLED handler '{}'", getThing().getUID());

        if (pollingJob != null) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        driver.shutdown();
        driver = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handle command '{}' for {}", command, channelUID);

        try {
            if (command == RefreshType.REFRESH) {
                update();
            } else if (channelUID.getId().equals(WiFiLEDBindingConstants.CHANNEL_POWER)) {
                handleColorCommand(command);
            } else if (channelUID.getId().equals(WiFiLEDBindingConstants.CHANNEL_COLOR)) {
                handleColorCommand(command);
            } else if (channelUID.getId().equals(WiFiLEDBindingConstants.CHANNEL_WHITE)) {
                handleWhiteCommand(command);
            } else if (channelUID.getId().equals(WiFiLEDBindingConstants.CHANNEL_WHITE2)) {
                handleWhite2Command(command);
            } else if (channelUID.getId().equals(WiFiLEDBindingConstants.CHANNEL_PROGRAM)
                    && (command instanceof StringType)) {
                driver.setProgram((StringType) command);
            } else if (channelUID.getId().equals(WiFiLEDBindingConstants.CHANNEL_PROGRAM_SPEED)) {
                handleProgramSpeedCommand(command);
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void handleColorCommand(Command command) throws IOException {
        if (command instanceof HSBType) {
            driver.setColor((HSBType) command);
        } else if (command instanceof PercentType) {
            driver.setBrightness((PercentType) command);
        } else if (command instanceof OnOffType) {
            driver.setPower((OnOffType) command);
        } else if (command instanceof IncreaseDecreaseType) {
            IncreaseDecreaseType increaseDecreaseType = (IncreaseDecreaseType) command;
            if (increaseDecreaseType.equals(IncreaseDecreaseType.INCREASE)) {
                driver.incBrightness(INC_DEC_STEP);
            } else {
                driver.decBrightness(INC_DEC_STEP);
            }
        }
    }

    private void handleWhiteCommand(Command command) throws IOException {
        if (command instanceof PercentType) {
            driver.setWhite((PercentType) command);
        } else if (command instanceof OnOffType) {
            OnOffType onOffCommand = (OnOffType) command;
            if (onOffCommand.equals(OnOffType.ON)) {
                driver.setWhite(PercentType.HUNDRED);
            } else {
                driver.setWhite(PercentType.ZERO);
            }
        } else if (command instanceof IncreaseDecreaseType) {
            IncreaseDecreaseType increaseDecreaseType = (IncreaseDecreaseType) command;
            if (increaseDecreaseType.equals(IncreaseDecreaseType.INCREASE)) {
                driver.incWhite(INC_DEC_STEP);
            } else {
                driver.decWhite(INC_DEC_STEP);
            }
        }
    }

    private void handleWhite2Command(Command command) throws IOException {
        if (command instanceof PercentType) {
            driver.setWhite2((PercentType) command);
        } else if (command instanceof OnOffType) {
            OnOffType onOffCommand = (OnOffType) command;
            if (onOffCommand.equals(OnOffType.ON)) {
                driver.setWhite2(PercentType.HUNDRED);
            } else {
                driver.setWhite2(PercentType.ZERO);
            }
        } else if (command instanceof IncreaseDecreaseType) {
            IncreaseDecreaseType increaseDecreaseType = (IncreaseDecreaseType) command;
            if (increaseDecreaseType.equals(IncreaseDecreaseType.INCREASE)) {
                driver.incWhite2(INC_DEC_STEP);
            } else {
                driver.decWhite2(INC_DEC_STEP);
            }
        }
    }

    private void handleProgramSpeedCommand(Command command) throws IOException {
        if (command instanceof PercentType) {
            driver.setProgramSpeed((PercentType) command);
        } else if (command instanceof OnOffType) {
            OnOffType onOffCommand = (OnOffType) command;
            if (onOffCommand.equals(OnOffType.ON)) {
                driver.setProgramSpeed(PercentType.HUNDRED);
            } else {
                driver.setProgramSpeed(PercentType.ZERO);
            }
        } else if (command instanceof IncreaseDecreaseType) {
            IncreaseDecreaseType increaseDecreaseType = (IncreaseDecreaseType) command;
            if (increaseDecreaseType.equals(IncreaseDecreaseType.INCREASE)) {
                driver.incProgramSpeed(INC_DEC_STEP);
            } else {
                driver.decProgramSpeed(INC_DEC_STEP);
            }
        }
    }

    private synchronized void update() {
        logger.debug("Updating WiFiLED data '{}'", getThing().getUID());

        try {
            LEDStateDTO ledState = driver.getLEDStateDTO();
            HSBType color = new HSBType(ledState.getHue(), ledState.getSaturation(), ledState.getBrightness());
            updateState(WiFiLEDBindingConstants.CHANNEL_POWER, ledState.power);
            updateState(WiFiLEDBindingConstants.CHANNEL_COLOR, color);
            updateState(WiFiLEDBindingConstants.CHANNEL_WHITE, ledState.getWhite());
            updateState(WiFiLEDBindingConstants.CHANNEL_PROGRAM, ledState.getProgram());
            updateState(WiFiLEDBindingConstants.CHANNEL_PROGRAM_SPEED, ledState.getProgramSpeed());

            if (getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
        }
    }

}
