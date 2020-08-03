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
package org.openhab.binding.modbus.e3dc.internal.handler;

import static org.openhab.binding.modbus.e3dc.internal.E3DCBindingConstants.*;
import static org.openhab.binding.modbus.e3dc.internal.modbus.E3DCModbusConstans.*;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.e3dc.internal.E3DCConfiguration;
import org.openhab.binding.modbus.e3dc.internal.dto.EmergencyBlock;
import org.openhab.binding.modbus.e3dc.internal.dto.InfoBlock;
import org.openhab.binding.modbus.e3dc.internal.dto.PowerBlock;
import org.openhab.binding.modbus.e3dc.internal.dto.StringBlock;
import org.openhab.binding.modbus.e3dc.internal.modbus.Data.DataType;
import org.openhab.binding.modbus.e3dc.internal.modbus.Parser;
import org.openhab.binding.modbus.handler.EndpointNotInitializedException;
import org.openhab.binding.modbus.handler.ModbusEndpointThingHandler;
import org.openhab.io.transport.modbus.AsyncModbusFailure;
import org.openhab.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.PollTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link E3DCThingHandler} Basic modbus connection towards the E3DC device
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class E3DCThingHandler extends BaseBridgeHandler {
    public enum ReadStatus {
        NOT_RECEIVED,
        READ_SUCCESS,
        READ_FAILED
    }

    static final String INFO_DATA_READ_ERROR = "Information And Data Modbus Read Errors";
    static final String INFO_READ_ERROR = "Information Modbus Read Error";
    static final String DATA_READ_ERROR = "Data Modbus Read Error";

    private final ArrayList<E3DCWallboxThingHandler> listeners = new ArrayList<E3DCWallboxThingHandler>();
    private final Logger logger = LoggerFactory.getLogger(E3DCThingHandler.class);
    private final Parser dataParser = new Parser(DataType.DATA);
    private ReadStatus dataRead = ReadStatus.NOT_RECEIVED;
    private final Parser infoParser = new Parser(DataType.INFO);
    private ReadStatus infoRead = ReadStatus.NOT_RECEIVED;
    private @Nullable PollTask infoPoller;
    private @Nullable PollTask dataPoller;
    private @Nullable E3DCConfiguration config;

    /**
     * Communication interface to the slave endpoint we're connecting to
     */
    protected volatile @Nullable ModbusCommunicationInterface comms = null;
    private int slaveId;

    public E3DCThingHandler(Bridge thing) {
        super(thing);
    }

    public @Nullable ModbusCommunicationInterface getComms() {
        return comms;
    }

    public int getSlaveId() {
        return slaveId;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // no control of E3DC device possible yet
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        scheduler.execute(() -> {
            E3DCConfiguration localConfig = getConfigAs(E3DCConfiguration.class);
            config = localConfig;
            ModbusCommunicationInterface localComms = connectEndpoint();
            if (localComms != null) {
                // register low speed info poller
                ModbusReadRequestBlueprint infoRequest = new ModbusReadRequestBlueprint(slaveId,
                        ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, INFO_REG_START, INFO_REG_SIZE, 3);
                infoPoller = localComms.registerRegularPoll(infoRequest, INFO_POLL_REFRESH_TIME_MS, 0,
                        this::handleInfoResult, this::handleInfoFailure);

                ModbusReadRequestBlueprint dataRequest = new ModbusReadRequestBlueprint(slaveId,
                        ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, POWER_REG_START,
                        REGISTER_LENGTH - INFO_REG_SIZE, 3);
                if (config != null) {
                    dataPoller = localComms.registerRegularPoll(dataRequest, localConfig.refresh, 0,
                            this::handleDataResult, this::handleDataFailure);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "E3DC Configuration missing");
                }
            } // else state handling performed in connectEndPoint function
        });
    }

    /**
     * Get a reference to the modbus endpoint
     */
    private @Nullable ModbusCommunicationInterface connectEndpoint() {
        if (comms != null) {
            return comms;
        }

        ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
        if (slaveEndpointThingHandler == null) {
            @SuppressWarnings("null")
            String label = Optional.ofNullable(getBridge()).map(b -> b.getLabel()).orElse("<null>");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' is offline", label));
            logger.debug("No bridge handler available -- aborting init for {}", label);
            return null;
        }
        try {
            slaveId = slaveEndpointThingHandler.getSlaveId();
            comms = slaveEndpointThingHandler.getCommunicationInterface();
        } catch (EndpointNotInitializedException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Slave Endpoint not initialized"));
            logger.debug("Slave Endpoint not initialized");
            return null;
        }
        if (comms == null) {
            @SuppressWarnings("null")
            String label = Optional.ofNullable(getBridge()).map(b -> b.getLabel()).orElse("<null>");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' not completely initialized", label));
            logger.debug("Bridge not initialized fully (no endpoint) -- aborting init for {}", this);
            return null;
        } else {
            return comms;
        }
    }

    /**
     * Get the endpoint handler from the bridge this handler is connected to
     * Checks that we're connected to the right type of bridge
     *
     * @return the endpoint handler or null if the bridge does not exist
     */
    private @Nullable ModbusEndpointThingHandler getEndpointThingHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Bridge is null");
            return null;
        }
        if (bridge.getStatus() != ThingStatus.ONLINE) {
            logger.debug("Bridge is not online");
            return null;
        }

        ThingHandler handler = bridge.getHandler();
        if (handler == null) {
            logger.debug("Bridge handler is null");
            return null;
        }

        if (handler instanceof ModbusEndpointThingHandler) {
            ModbusEndpointThingHandler slaveEndpoint = (ModbusEndpointThingHandler) handler;
            return slaveEndpoint;
        } else {
            logger.debug("Unexpected bridge handler: {}", handler);
            return null;
        }
    }

    /**
     * Returns the channel UID for the specified group and channel id
     *
     * @param string the channel group
     * @param string the channel id in that group
     * @return the globally unique channel uid
     */
    private ChannelUID channelUID(String group, String id) {
        return new ChannelUID(getThing().getUID(), group, id);
    }

    void handleInfoResult(AsyncModbusReadResult result) {
        if (infoRead != ReadStatus.READ_SUCCESS) {
            // update status only if bit switches
            infoRead = ReadStatus.READ_SUCCESS;
            updateStatus();
        }
        infoParser.handle(result);
        InfoBlock block = (InfoBlock) infoParser.parse(DataType.INFO);
        String group = "info";
        if (block != null) {
            updateState(channelUID(group, MODBUS_ID_CHANNEL), block.modbusId);
            updateState(channelUID(group, MODBUS_FIRMWARE_CHANNEL), block.modbusVersion);
            updateState(channelUID(group, SUPPORTED_REGSITERS_CHANNEL), block.supportedRegisters);
            updateState(channelUID(group, MANUFACTURER_NAME_CHANNEL), block.manufacturer);
            updateState(channelUID(group, MODEL_NAME_CHANNEL), block.modelName);
            updateState(channelUID(group, SERIAL_NUMBER_CHANNEL), block.serialNumber);
            updateState(channelUID(group, FIRMWARE_RELEASE_CHANNEL), block.firmware);
        } else {
            logger.debug("Unable to get {} from provider {}", DataType.INFO, dataParser.toString());
        }
    }

    void handleInfoFailure(AsyncModbusFailure<ModbusReadRequestBlueprint> result) {
        if (infoRead != ReadStatus.READ_FAILED) {
            // update status only if bit switches
            infoRead = ReadStatus.READ_FAILED;
            updateStatus();
        }
    }

    void handleDataResult(AsyncModbusReadResult result) {
        if (dataRead != ReadStatus.READ_SUCCESS) {
            // update status only if bit switches
            dataRead = ReadStatus.READ_SUCCESS;
            updateStatus();
        }
        dataParser.handle(result);
        // Update channels in emergency group
        {
            EmergencyBlock block = (EmergencyBlock) dataParser.parse(DataType.EMERGENCY);
            String group = "emergency";
            if (block != null) {
                updateState(channelUID(group, EMERGENCY_POWER_STATUS), block.epStatus);
                updateState(channelUID(group, BATTERY_LOADING_LOCKED), block.batteryLoadingLocked);
                updateState(channelUID(group, BATTERY_UNLOADING_LOCKED), block.batterUnLoadingLocked);
                updateState(channelUID(group, EMERGENCY_POWER_POSSIBLE), block.epPossible);
                updateState(channelUID(group, WEATHER_PREDICTION_LOADING), block.weatherPredictedLoading);
                updateState(channelUID(group, REGULATION_STATUS), block.regulationStatus);
                updateState(channelUID(group, LOADING_LOCK_TIME), block.loadingLockTime);
                updateState(channelUID(group, UNLOADING_LOCKTIME), block.unloadingLockTime);
            } else {
                logger.debug("Unable to get {} from provider {}", DataType.EMERGENCY, dataParser.toString());
            }
        }

        // Update channels in power group
        {
            PowerBlock block = (PowerBlock) dataParser.parse(DataType.POWER);
            String group = "power";
            if (block != null) {
                updateState(channelUID(group, PV_POWER_SUPPLY_CHANNEL), block.pvPowerSupply);
                updateState(channelUID(group, BATTERY_POWER_SUPPLY_CHANNEL), block.batteryPowerSupply);
                updateState(channelUID(group, BATTERY_POWER_CONSUMPTION), block.batteryPowerConsumption);
                updateState(channelUID(group, HOUSEHOLD_POWER_CONSUMPTION_CHANNEL), block.householdPowerConsumption);
                updateState(channelUID(group, GRID_POWER_CONSUMPTION_CHANNEL), block.gridPowerConsumpition);
                updateState(channelUID(group, GRID_POWER_SUPPLY_CHANNEL), block.gridPowerSupply);
                updateState(channelUID(group, EXTERNAL_POWER_SUPPLY_CHANNEL), block.externalPowerSupply);
                updateState(channelUID(group, WALLBOX_POWER_CONSUMPTION_CHANNEL), block.wallboxPowerConsumption);
                updateState(channelUID(group, WALLBOX_PV_POWER_CONSUMPTION_CHANNEL), block.wallboxPVPowerConsumption);
                updateState(channelUID(group, AUTARKY), block.autarky);
                updateState(channelUID(group, SELF_CONSUMPTION), block.selfConsumption);
                updateState(channelUID(group, BATTERY_STATE_OF_CHARGE_CHANNEL), block.batterySOC);
            } else {
                logger.debug("Unable to get {} from provider {}", DataType.POWER, dataParser.toString());
            }
        }

        // Update channels in strings group
        {
            StringBlock block = (StringBlock) dataParser.parse(DataType.STRINGS);
            String group = "strings";
            if (block != null) {
                updateState(channelUID(group, STRING1_DC_CURRENT_CHANNEL), block.string1Ampere);
                updateState(channelUID(group, STRING1_DC_VOLTAGE_CHANNEL), block.string1Volt);
                updateState(channelUID(group, STRING1_DC_OUTPUT_CHANNEL), block.string1Watt);
                updateState(channelUID(group, STRING2_DC_CURRENT_CHANNEL), block.string2Ampere);
                updateState(channelUID(group, STRING2_DC_VOLTAGE_CHANNEL), block.string2Volt);
                updateState(channelUID(group, STRING2_DC_OUTPUT_CHANNEL), block.string2Watt);
                updateState(channelUID(group, STRING3_DC_CURRENT_CHANNEL), block.string3Ampere);
                updateState(channelUID(group, STRING3_DC_VOLTAGE_CHANNEL), block.string3Volt);
                updateState(channelUID(group, STRING3_DC_OUTPUT_CHANNEL), block.string3Watt);
            } else {
                logger.debug("Unable to get {} from provider {}", DataType.STRINGS, dataParser.toString());
            }
        }

        listeners.forEach(l -> {
            l.handle(result);
        });
    }

    void handleDataFailure(AsyncModbusFailure<ModbusReadRequestBlueprint> result) {
        if (dataRead != ReadStatus.READ_FAILED) {
            // update status only if bit switches
            dataRead = ReadStatus.READ_FAILED;
            updateStatus();
        }
        listeners.forEach(l -> {
            l.handleError(result);
        });
    }

    @Override
    public void dispose() {
        ModbusCommunicationInterface localComms = comms;
        if (localComms != null) {
            PollTask localInfoPoller = infoPoller;
            if (localInfoPoller != null) {
                localComms.unregisterRegularPoll(localInfoPoller);
            }
            PollTask localDataPoller = dataPoller;
            if (localDataPoller != null) {
                localComms.unregisterRegularPoll(localDataPoller);
            }
        }
        // Comms will be close()'d by endpoint thing handler
        comms = null;
    }

    private void updateStatus() {
        logger.debug("Status update: Info {} Data {} ", infoRead, dataRead);
        if (infoRead != ReadStatus.NOT_RECEIVED && dataRead != ReadStatus.NOT_RECEIVED) {
            if (infoRead == dataRead) {
                // both reads are ok or else both failed
                if (infoRead == ReadStatus.READ_SUCCESS) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, INFO_DATA_READ_ERROR);
                }
            } else {
                // either info or data read failed - update status with details
                if (infoRead == ReadStatus.READ_FAILED) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, INFO_READ_ERROR);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, DATA_READ_ERROR);
                }
            }
        } // else - one status isn't received yet - wait until both Modbus polls returns either success or error
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        listeners.add((E3DCWallboxThingHandler) childHandler);
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        listeners.remove(childHandler);
    }
}
