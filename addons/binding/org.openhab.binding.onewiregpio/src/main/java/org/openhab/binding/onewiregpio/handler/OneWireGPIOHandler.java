/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.onewiregpio.handler;

import static org.openhab.binding.onewiregpio.OneWireGPIOBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.onewiregpio.OneWireGPIOBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OneWireGPIOHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Anatol Ogorek - Initial contribution
 */
public class OneWireGPIOHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OneWireGPIOHandler.class);

    private String gpioBusFile;
    private Integer refreshTime;

    public OneWireGPIOHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(TEMPERATURE)) {
            if (command instanceof RefreshType) {
                publishSensorValue(channelUID);
            } else {
                logger.debug("Command {} is not supported for channel: {}. Supported command: REFRESH", command,
                        channelUID.getId());
            }
        }
    }

    @Override
    public void initialize() {
        checkConfiguration();
        updateStatus(ThingStatus.UNKNOWN);
        startAutomaticRefresh();
    }

    /**
     * This method checks if the provided configuration is valid.
     * When invalid parameter is found, default value is assigned.
     */
    private void checkConfiguration() {
        Configuration configuration = getConfig();
        gpioBusFile = (String) configuration.get(GPIO_BUS_FILE);
        if (gpioBusFile == null) {
            logger.warn("GPIO_BUS_FILE not set. Please check configuration, and set proper path to w1_slave file.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid path to w1_slave file");
            return;
        }

        refreshTime = ((Number) configuration.get(REFRESH_TIME)).intValue();
        if (refreshTime.intValue() <= 0) {
            logger.warn("Refresh time [{}] is not valid. Falling back to default value: {}.", refreshTime,
                    DEFAULT_REFRESH_TIME);
            refreshTime = DEFAULT_REFRESH_TIME;
        }
    }

    private void startAutomaticRefresh() {
        Runnable refresher = new Runnable() {
            @Override
            public void run() {
                List<Channel> channels = getThing().getChannels();
                for (Channel channel : channels) {
                    publishSensorValue(channel.getUID());
                }
            }
        };

        scheduler.scheduleWithFixedDelay(refresher, 0, refreshTime.intValue(), TimeUnit.SECONDS);
        logger.debug("Start automatic refresh every {} seconds", refreshTime.intValue());
    }

    private void publishSensorValue(ChannelUID channelUID) {
        String channelID = channelUID.getId();
        State state = null;
        switch (channelID) {
            case TEMPERATURE:
                BigDecimal temp = readSensorTemperature(gpioBusFile);
                temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
                state = new DecimalType(temp);
                break;
        }
        if (state != null) {
            updateState(channelID, state);
        } else {
            logger.debug("Can not update channel with ID : {} - channel name might be wrong!", channelID);
        }
    }

    private BigDecimal readSensorTemperature(String gpioFile) {
        try (Stream<String> stream = Files.lines(Paths.get(gpioFile))) {
            Optional<String> temperatureLine = stream
                    .filter(s -> s.contains(OneWireGPIOBindingConstants.FILE_TEMP_MARKER)).findFirst();
            if (temperatureLine.isPresent()) {
                String line = temperatureLine.get();
                String tempString = line.substring(line.indexOf(OneWireGPIOBindingConstants.FILE_TEMP_MARKER)
                        + OneWireGPIOBindingConstants.FILE_TEMP_MARKER.length());
                Integer intTemp = Integer.parseInt(tempString);
                if (getThing().getStatus() != ThingStatus.ONLINE) {
                    updateStatus(ThingStatus.ONLINE);
                }
                return BigDecimal.valueOf(intTemp).movePointLeft(3);
            } else {
                logger.debug(
                        "GPIO file didn't contain line with 't=' where temperature value should be available. Check if configuration points to the proper file");
                updateStatus(ThingStatus.OFFLINE);
                return OneWireGPIOBindingConstants.INVALID_TEMPERATURE;
            }
        } catch (IOException | InvalidPathException e) {
            logger.warn("error reading GPIO bus file. File path is: {}.  Check if path is proper. {}", gpioFile, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Check if path to w1_slave is proper");
            return OneWireGPIOBindingConstants.INVALID_TEMPERATURE;
        }

    }

}
