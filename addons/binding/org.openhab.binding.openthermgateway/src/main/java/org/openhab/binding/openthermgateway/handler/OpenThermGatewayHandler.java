/**
 * Copyright (c) 2018,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.openthermgateway.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.openthermgateway.OpenThermGatewayBindingConstants;
import org.openhab.binding.openthermgateway.internal.CommandType;
import org.openhab.binding.openthermgateway.internal.DataItem;
import org.openhab.binding.openthermgateway.internal.DataItemGroup;
import org.openhab.binding.openthermgateway.internal.LogLevel;
import org.openhab.binding.openthermgateway.internal.Message;
import org.openhab.binding.openthermgateway.internal.OpenThermGatewayCallback;
import org.openhab.binding.openthermgateway.internal.OpenThermGatewayConfiguration;
import org.openhab.binding.openthermgateway.internal.OpenThermGatewayConnector;
import org.openhab.binding.openthermgateway.internal.OpenThermGatewaySocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenThermGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Arjen Korevaar
 */
@NonNullByDefault
public class OpenThermGatewayHandler extends BaseThingHandler implements OpenThermGatewayCallback {

    private final Logger logger = LoggerFactory.getLogger(OpenThermGatewayHandler.class);

    @Nullable
    private OpenThermGatewayConfiguration config;

    @Nullable
    private OpenThermGatewayConnector connector;

    @Nullable
    private Thread connectorThread;

    public OpenThermGatewayHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing OpenTherm Gateway handler for uid '{}'", getThing().getUID());

        updateStatus(ThingStatus.OFFLINE);

        connect();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);

        try {
            if (command.toFullString() != "REFRESH" && connect()) {
                String channel = channelUID.getId();

                if (channel.equals(OpenThermGatewayBindingConstants.CHANNEL_OVERRIDE_SETPOINT)) {
                    connector.sendCommand(CommandType.TemperatureTemporary, command.toFullString());
                } else if (channel.equals(OpenThermGatewayBindingConstants.CHANNEL_OVERRIDE_DHW_SETPOINT)) {
                    connector.sendCommand(CommandType.SetpointWater, command.toFullString());
                } else if (channel.equals(OpenThermGatewayBindingConstants.CHANNEL_OUTSIDE_TEMPERATURE)) {
                    connector.sendCommand(CommandType.TemperatureOutside, command.toFullString());
                }
            }
        } catch (Exception ex) {
            logger.error("error", ex);
        }
    }

    @Override
    public void handleRemoval() {
        logger.debug("Removing OpenTherm Gateway handler");
        disconnect();
        super.handleRemoval();
    }

    @Override
    public void connecting() {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void connected() {
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void disconnected() {
        try {
            updateStatus(ThingStatus.OFFLINE);
        } catch (IllegalStateException ex) {
        }
    }

    @Override
    public void receiveMessage(Message message) {
        if (DataItemGroup.dataItemGroups.containsKey(message.getID())) {
            DataItem[] dataItems = DataItemGroup.dataItemGroups.get(message.getID());

            for (int i = 0; i < dataItems.length; i++) {
                DataItem dataItem = dataItems[i];

                String channelId = dataItem.getSubject();

                if (!OpenThermGatewayBindingConstants.SUPPORTED_CHANNEL_IDS.contains(channelId)) {
                    continue;
                }

                State state = null;

                switch (dataItem.getDataType()) {
                    case Flags:
                        state = TypeConverter.toOnOffType(message.getBit(dataItem.getByteType(), dataItem.getBitPos()));
                        break;
                    case Uint8:
                    case Uint16:
                        state = TypeConverter.toDecimalType(message.getUInt(dataItem.getByteType()));
                        break;
                    case Int8:
                    case Int16:
                        state = TypeConverter.toDecimalType(message.getInt(dataItem.getByteType()));
                        break;
                    case Float:
                        state = TypeConverter.toDecimalType(message.getFloat());
                        break;
                    case DoWToD:
                        break;
                }

                if (state != null) {
                    logger.debug("Received update for channel '{}': {}", channelId, state.toFullString());
                    try {
                        updateState(channelId, state);
                    } catch (IllegalStateException e) {
                        // Missing callback, possibly due to incorrect initialization. how to handle correctly ?
                    }
                }
            }
        }
    }

    @Override
    public void log(LogLevel loglevel, String format, String arg) {
        String message = String.format(format, arg);
        log(loglevel, message);
    }

    @Override
    public void log(LogLevel loglevel, String message, Throwable t) {
        switch (loglevel) {
            case Debug:
                logger.debug(message, t);
                break;
            case Info:
                logger.info(message, t);
                break;
            case Warning:
                logger.warn(message, t);
                break;
            case Error:
                logger.error(message, t);
                break;
            default:
                break;
        }
    }

    @Override
    public void log(LogLevel loglevel, String message) {
        switch (loglevel) {
            case Debug:
                logger.debug(message);
                break;
            case Info:
                logger.info(message);
                break;
            case Warning:
                logger.warn(message);
                break;
            case Error:
                logger.error(message);
                break;
            default:
                break;
        }
    }

    private boolean connect() {
        if (connector != null && connector.isConnected()) {
            return true;
        }

        try {
            config = getConfigAs(OpenThermGatewayConfiguration.class);

            logger.info("Connecting to the OpenTherm Gateway at {}:{}", config.ipaddress, config.port);

            disconnect();

            // TODO: support different kinds of connectors, such as USB, serial port
            connector = new OpenThermGatewaySocketConnector(this, config.ipaddress, config.port);
            connectorThread = new Thread(connector);
            connectorThread.start();

            return true;
        } catch (Exception ex) {
            logger.error("error", ex);
        }

        return false;
    }

    private void disconnect() {
        if (connector != null) {
            logger.info("Disconnecting the OpenTherm Gateway");
            connector.stop();
        }
    }
}
