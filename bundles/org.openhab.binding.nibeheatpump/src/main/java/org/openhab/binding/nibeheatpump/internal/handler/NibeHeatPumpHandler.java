/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.nibeheatpump.internal.handler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.nibeheatpump.internal.NibeHeatPumpCommandResult;
import org.openhab.binding.nibeheatpump.internal.NibeHeatPumpException;
import org.openhab.binding.nibeheatpump.internal.config.NibeHeatPumpConfiguration;
import org.openhab.binding.nibeheatpump.internal.connection.ConnectorFactory;
import org.openhab.binding.nibeheatpump.internal.connection.NibeHeatPumpConnector;
import org.openhab.binding.nibeheatpump.internal.connection.NibeHeatPumpEventListener;
import org.openhab.binding.nibeheatpump.internal.message.ModbusDataReadOutMessage;
import org.openhab.binding.nibeheatpump.internal.message.ModbusReadRequestMessage;
import org.openhab.binding.nibeheatpump.internal.message.ModbusReadResponseMessage;
import org.openhab.binding.nibeheatpump.internal.message.ModbusValue;
import org.openhab.binding.nibeheatpump.internal.message.ModbusWriteRequestMessage;
import org.openhab.binding.nibeheatpump.internal.message.ModbusWriteResponseMessage;
import org.openhab.binding.nibeheatpump.internal.message.NibeHeatPumpMessage;
import org.openhab.binding.nibeheatpump.internal.models.PumpModel;
import org.openhab.binding.nibeheatpump.internal.models.VariableInformation;
import org.openhab.binding.nibeheatpump.internal.models.VariableInformation.NibeDataType;
import org.openhab.binding.nibeheatpump.internal.models.VariableInformation.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NibeHeatPumpHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class NibeHeatPumpHandler extends BaseThingHandler implements NibeHeatPumpEventListener {

    private final Logger logger = LoggerFactory.getLogger(NibeHeatPumpHandler.class);

    private static final int TIMEOUT = 4500;

    private final PumpModel pumpModel;
    private NibeHeatPumpConfiguration configuration;

    private NibeHeatPumpConnector connector;

    private boolean reconnectionRequest;

    private NibeHeatPumpCommandResult writeResult;
    private NibeHeatPumpCommandResult readResult;

    private ScheduledFuture<?> connectorTask;
    private ScheduledFuture<?> pollingJob;

    private final List<Integer> itemsToPoll = Collections.synchronizedList(new ArrayList<>());

    private final List<Integer> itemsToEnableWrite = new ArrayList<>();

    private final Map<Integer, CacheObject> stateMap = Collections.synchronizedMap(new HashMap<Integer, CacheObject>());

    private long lastUpdateTime = 0;

    protected class CacheObject {

        /** Time when cache object updated in milliseconds */
        final long lastUpdateTime;

        /** Cache value */
        final int value;

        /**
         * Initialize cache object.
         *
         * @param lastUpdateTime
         *                           Time in milliseconds.
         *
         * @param value
         *                           Cache value.
         */
        CacheObject(long lastUpdateTime, int value) {
            this.lastUpdateTime = lastUpdateTime;
            this.value = value;
        }
    }

    public NibeHeatPumpHandler(Thing thing, PumpModel pumpModel) {
        super(thing);
        this.pumpModel = pumpModel;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);

        int coilAddress = parseCoilAddressFromChannelUID(channelUID);

        if (command.equals(RefreshType.REFRESH)) {
            logger.debug("Clearing cache value for channel '{}' to refresh channel data", channelUID);
            clearCache(coilAddress);
            return;
        }

        if (!configuration.enableWriteCommands) {
            logger.info(
                    "All write commands denied, ignoring command! Change Nibe heat pump binding configuration if you want to enable write commands.");
            return;
        }

        if (!itemsToEnableWrite.contains(coilAddress)) {
            logger.info(
                    "Write commands to register '{}' not allowed, ignoring command! Add this register to Nibe heat pump binding configuration if you want to enable write commands.",
                    coilAddress);
            return;
        }

        if (connector != null) {
            VariableInformation variableInfo = VariableInformation.getVariableInfo(pumpModel, coilAddress);
            logger.debug("Using variable information for register {}: {}", coilAddress, variableInfo);

            if (variableInfo != null && variableInfo.type == VariableInformation.Type.SETTING) {
                int value = convertStateToNibeValue(command);
                value = value * variableInfo.factor;

                ModbusWriteRequestMessage msg = new ModbusWriteRequestMessage.MessageBuilder().coilAddress(coilAddress)
                        .value(value).build();

                try {
                    writeResult = sendMessageToNibe(msg);
                    ModbusWriteResponseMessage result = (ModbusWriteResponseMessage) writeResult.get(TIMEOUT,
                            TimeUnit.MILLISECONDS);
                    if (result != null) {
                        if (result.isSuccessfull()) {
                            logger.debug("Write message sending to heat pump succeeded");
                        } else {
                            logger.error("Message sending to heat pump failed, value not accepted by the heat pump");
                        }
                    } else {
                        logger.debug("Something weird happen, result for write command is null");
                    }
                } catch (TimeoutException e) {
                    logger.warn("Message sending to heat pump failed, no response");
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "No response received from the heat pump");
                } catch (InterruptedException e) {
                    logger.debug("Message sending to heat pump failed, sending interrupted");
                } catch (NibeHeatPumpException e) {
                    logger.debug("Message sending to heat pump failed, exception {}", e.getMessage());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                } finally {
                    writeResult = null;
                }

                // Clear cache value to refresh coil data from the pump.
                // We might not know if write message have succeed or not, so let's always refresh it.
                logger.debug("Clearing cache value for channel '{}' to refresh channel data", channelUID);
                clearCache(coilAddress);
            } else {
                logger.debug("Command to channel '{}' rejected, because item is read only parameter", channelUID);
            }
        } else {
            logger.debug("No connection to heat pump");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR);
        }
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        logger.debug("channelLinked: {}", channelUID);

        // Add channel to polling loop
        int coilAddress = parseCoilAddressFromChannelUID(channelUID);
        synchronized (itemsToPoll) {
            if (!itemsToPoll.contains(coilAddress)) {
                logger.debug("New channel '{}' found, register '{}'", channelUID.getAsString(), coilAddress);
                itemsToPoll.add(coilAddress);
            }
        }
        clearCache(coilAddress);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        logger.debug("channelUnlinked: {}", channelUID);

        // remove channel from polling loop
        int coilAddress = parseCoilAddressFromChannelUID(channelUID);
        synchronized (itemsToPoll) {
            itemsToPoll.removeIf(c -> c.equals(coilAddress));
        }
    }

    private int parseCoilAddressFromChannelUID(ChannelUID channelUID) {
        if (channelUID.getId().contains("#")) {
            String[] parts = channelUID.getId().split("#");
            return Integer.parseInt(parts[parts.length - 1]);
        } else {
            return Integer.parseInt(channelUID.getId());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initialized Nibe Heat Pump device handler for {}", getThing().getUID());
        configuration = getConfigAs(NibeHeatPumpConfiguration.class);
        logger.debug("Using configuration: {}", configuration.toString());

        try {
            parseWriteEnabledItems();
            connector = ConnectorFactory.getConnector(thing.getThingTypeUID());
        } catch (IllegalArgumentException | NibeHeatPumpException e) {
            String description = String.format("Illegal configuration, %s", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, description);
            return;
        }

        itemsToPoll.clear();
        itemsToPoll.addAll(this.getThing().getChannels().stream().filter(c -> isLinked(c.getUID())).map(c -> {
            int coilAddress = parseCoilAddressFromChannelUID(c.getUID());
            logger.debug("Linked channel '{}' found, register '{}'", c.getUID().getAsString(), coilAddress);
            return coilAddress;
        }).filter(c -> c != 0).collect(Collectors.toSet()));

        logger.debug("Linked registers {}: {}", itemsToPoll.size(), itemsToPoll);

        clearCache();

        if (connectorTask == null || connectorTask.isCancelled()) {
            connectorTask = scheduler.scheduleWithFixedDelay(() -> {
                if (reconnectionRequest) {
                    logger.debug("Restarting requested, restarting...");
                    reconnectionRequest = false;
                    closeConnection();
                }

                logger.debug("Checking Nibe Heat pump connection, thing status = {}", thing.getStatus());
                connect();
            }, 0, 10, TimeUnit.SECONDS);
        }
    }

    private void connect() {
        if (!connector.isConnected()) {
            logger.debug("Connecting to heat pump");
            try {
                connector.addEventListener(this);
                connector.connect(configuration);
                updateStatus(ThingStatus.ONLINE);

                if (pollingJob == null || pollingJob.isCancelled()) {
                    logger.debug("Start refresh task, interval={}sec", 1);
                    pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 0, 1, TimeUnit.SECONDS);
                }
            } catch (NibeHeatPumpException e) {
                logger.debug("Error occurred when connecting to heat pump, exception {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        } else {
            logger.debug("Connection to heat pump already open");
        }
    }

    @Override
    public void dispose() {
        logger.debug("Thing {} disposed.", getThing().getUID());

        if (connectorTask != null && !connectorTask.isCancelled()) {
            connectorTask.cancel(true);
            connectorTask = null;
        }

        closeConnection();
    }

    private void closeConnection() {
        logger.debug("Closing connection to the heat pump");

        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }

        if (connector != null) {
            connector.removeEventListener(this);
            connector.disconnect();
        }
    }

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {

            if (!configuration.enableReadCommands) {
                logger.trace("All read commands denied, skip polling!");
                return;
            }

            List<Integer> items;
            synchronized (itemsToPoll) {
                items = new ArrayList<>(itemsToPoll);
            }

            for (int item : items) {
                if (connector != null && connector.isConnected()
                        && getThing().getStatusInfo().getStatus() == ThingStatus.ONLINE) {

                    CacheObject oldValue = stateMap.get(item);
                    if (oldValue == null
                            || (oldValue.lastUpdateTime + refreshIntervalMillis()) < System.currentTimeMillis()) {

                        // it's time to refresh data
                        logger.debug("Time to refresh variable '{}' data", item);

                        ModbusReadRequestMessage request = new ModbusReadRequestMessage.MessageBuilder()
                                .coilAddress(item).build();

                        try {
                            readResult = sendMessageToNibe(request);
                            ModbusReadResponseMessage result = (ModbusReadResponseMessage) readResult.get(TIMEOUT,
                                    TimeUnit.MILLISECONDS);
                            if (result != null) {
                                if (request.getCoilAddress() != result.getCoilAddress()) {
                                    logger.debug("Data from wrong register '{}' received, expected '{}'",
                                            result.getCoilAddress(), request.getCoilAddress());
                                }
                                // update variable anyway
                                handleVariableUpdate(pumpModel, result.getValueAsModbusValue());
                            }
                        } catch (TimeoutException e) {
                            logger.debug("Message sending to heat pump failed, no response");
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                        } catch (InterruptedException e) {
                            logger.debug("Message sending to heat pump failed, sending interrupted");
                        } catch (NibeHeatPumpException e) {
                            logger.debug("Message sending to heat pump failed, exception {}", e.getMessage());
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                        } finally {
                            readResult = null;
                        }
                    }
                }
            }
        }
    };

    private long refreshIntervalMillis() {
        return configuration.refreshInterval * 1000;
    }

    private int convertStateToNibeValue(Command command) {
        int value;

        if (command instanceof OnOffType) {
            value = command == OnOffType.ON ? 1 : 0;
        } else {
            value = Integer.parseInt(command.toString());
        }

        return value;
    }

    private void parseWriteEnabledItems() throws IllegalArgumentException {
        itemsToEnableWrite.clear();
        if (configuration.enableWriteCommands && configuration.enableWriteCommandsToRegisters != null
                && configuration.enableWriteCommandsToRegisters.length() > 0) {
            String[] items = configuration.enableWriteCommandsToRegisters.replace(" ", "").split(",");
            for (String item : items) {
                try {
                    int coilAddress = Integer.parseInt(item);
                    VariableInformation variableInformation = VariableInformation.getVariableInfo(pumpModel,
                            coilAddress);
                    if (variableInformation == null) {
                        String description = String.format("Unknown register %s", coilAddress);
                        throw new IllegalArgumentException(description);
                    }
                    itemsToEnableWrite.add(coilAddress);
                } catch (NumberFormatException e) {
                    String description = String.format("Illegal register %s", item);
                    throw new IllegalArgumentException(description);
                }
            }
        }
        logger.debug("Enabled registers for write commands: {}", itemsToEnableWrite);
    }

    private State convertNibeValueToState(VariableInformation variableInfo, int value, String acceptedItemType) {
        State state = UnDefType.UNDEF;

        if ("String".equalsIgnoreCase(acceptedItemType)) {
            state = new StringType(String.valueOf((int) value));

        } else if ("Switch".equalsIgnoreCase(acceptedItemType)) {
            state = value == 0 ? OnOffType.OFF : OnOffType.ON;

        } else if ("Number".equalsIgnoreCase(acceptedItemType)) {
            NibeDataType dataType = variableInfo.dataType;
            int decimals = (int)Math.log10(variableInfo.factor);
            byte[] bytes;
            int x;
            switch (dataType) {
                case U8:
                    bytes = ByteBuffer.allocate(1).put((byte)value).array();
                    value = bytes[0];
                    break;
                case U16:
                    bytes = ByteBuffer.allocate(2).putChar((char) value).array();
                    value = ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff);
                    break;
                case U32:
                    bytes = ByteBuffer.allocate(4).putInt(value).array();
                    value = ((bytes[0] & 0xff)<<24) | ((bytes[1] & 0xff)<<16) | ( (bytes[2]&0xff)<<8) | (bytes[3] & 0xff);
                    return new DecimalType(new BigDecimal(Integer.toUnsignedLong(value)).movePointLeft(decimals).setScale(decimals, RoundingMode.HALF_EVEN));
                case S8:
                    bytes = ByteBuffer.allocate(1).put((byte)value).array();
                    x = bytes[0];
                    if ((bytes[0]>>>7&1)==1){
                        x = ((x^0x7f)&0x7f)+1; //2's complement 8 bit
                        x *= -1;
                    }
                    value = x;
                    break;
                case S16:
                    bytes = ByteBuffer.allocate(2).putChar((char) value).array();
                    x = (bytes[0] << 8) | (bytes[1] & 0xff);
                    if ((x>>>15&1)==1){
                        x = ((x^0x7fff)&0x7fff)+1; //2's complement 16 bit
                        x *= -1;
                    }
                    value = x;
                    break;
                case S32:
                    bytes = ByteBuffer.allocate(4).putInt(value).array();
                    x = (bytes[0]<<24) | (bytes[1]<<16) | (bytes[2]<<8) | (bytes[3]);
                    if ((x>>>31&1)==1){
                        x = ((x^0x7fffffff)&0x7fffffff)+1; //2's complement 32 bit
                        x *= -1;
                    }

                    value = x;
                    break;
            }
            state = new DecimalType(new BigDecimal(value).movePointLeft(decimals).setScale(decimals, RoundingMode.HALF_EVEN));
        }

        return state;
    }

    private void clearCache() {
        stateMap.clear();
        lastUpdateTime = 0;
    }

    private void clearCache(int coilAddress) {
        stateMap.put(coilAddress, null);
    }

    private synchronized NibeHeatPumpCommandResult sendMessageToNibe(NibeHeatPumpMessage msg)
            throws NibeHeatPumpException {

        logger.debug("Sending message: {}", msg);
        connector.sendDatagram(msg);
        return new NibeHeatPumpCommandResult();
    }

    @Override
    public void msgReceived(NibeHeatPumpMessage msg) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Received raw data: {}", msg.toHexString());
            }

            logger.debug("Received message: {}", msg);

            updateStatus(ThingStatus.ONLINE);

            if (msg instanceof ModbusReadResponseMessage) {
                handleReadResponseMessage((ModbusReadResponseMessage) msg);
            } else if (msg instanceof ModbusWriteResponseMessage) {
                handleWriteResponseMessage((ModbusWriteResponseMessage) msg);
            } else if (msg instanceof ModbusDataReadOutMessage) {
                handleDataReadOutMessage((ModbusDataReadOutMessage) msg);
            } else {
                logger.debug("Received unknown message: {}", msg.toString());
            }
        } catch (Exception e) {
            logger.debug("Error occurred when parsing received message, reason: {}", e.getMessage());
        }
    }

    @Override
    public void errorOccurred(String error) {
        logger.debug("Error '{}' occurred, re-establish the connection", error);
        reconnectionRequest = true;
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
    }

    private void handleReadResponseMessage(ModbusReadResponseMessage msg) {
        if (readResult != null) {
            readResult.set(msg);
        }
    }

    private void handleWriteResponseMessage(ModbusWriteResponseMessage msg) {
        if (writeResult != null) {
            writeResult.set(msg);
        }
    }

    private void handleDataReadOutMessage(ModbusDataReadOutMessage msg) {
        boolean parse = true;

        logger.debug("Received data read out message");
        if (configuration.throttleTime > 0) {
            if ((lastUpdateTime + configuration.throttleTime) > System.currentTimeMillis()) {
                logger.debug("Skipping data read out message parsing");
                parse = false;
            }
        }

        if (parse) {
            logger.debug("Parsing data read out message");
            lastUpdateTime = System.currentTimeMillis();
            List<ModbusValue> regValues = msg.getValues();

            if (regValues != null) {
                for (ModbusValue val : regValues) {
                    handleVariableUpdate(pumpModel, val);
                }
            }
        }
    }

    private void handleVariableUpdate(PumpModel pumpModel, ModbusValue value) {
        logger.debug("Received variable update: {}", value);
        int coilAddress = value.getCoilAddress();

        VariableInformation variableInfo = VariableInformation.getVariableInfo(pumpModel, coilAddress);

        if (variableInfo != null) {
            logger.trace("Using variable information to register {}: {}", coilAddress, variableInfo);

            int val = value.getValue();
            logger.debug("{} = {}", coilAddress + ":" + variableInfo.variable + "/" + variableInfo.factor, val);

            CacheObject oldValue = stateMap.get(coilAddress);

            if (oldValue != null && val == oldValue.value && (oldValue.lastUpdateTime + refreshIntervalMillis()) >= System.currentTimeMillis()) {
                logger.trace("Value did not change, ignoring update");
            } else {
                final String channelPrefix = (variableInfo.type == Type.SETTING ? "setting#" : "sensor#");
                final String channelId = channelPrefix + String.valueOf(coilAddress);
                final String acceptedItemType = thing.getChannel(channelId).getAcceptedItemType();

                logger.trace("AcceptedItemType for channel {} = {}", channelId, acceptedItemType);
                State state = convertNibeValueToState(variableInfo, val, acceptedItemType);
                stateMap.put(coilAddress, new CacheObject(System.currentTimeMillis(), val));
                updateState(new ChannelUID(getThing().getUID(), channelId), state);
            }
        } else {
            logger.debug("Unknown register {}", coilAddress);
        }
    }
}
