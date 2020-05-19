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
package org.openhab.binding.modbus.stiebeleltron.internal.handler;

import static org.eclipse.smarthome.core.library.unit.SIUnits.CELSIUS;
import static org.eclipse.smarthome.core.library.unit.SmartHomeUnits.KILOWATT_HOUR;
import static org.eclipse.smarthome.core.library.unit.SmartHomeUnits.PERCENT;
import static org.openhab.binding.modbus.stiebeleltron.internal.StiebelEltronBindingConstants.*;

import java.math.BigDecimal;
import java.util.Optional;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.modbus.handler.EndpointNotInitializedException;
import org.openhab.binding.modbus.handler.ModbusEndpointThingHandler;
import org.openhab.binding.modbus.stiebeleltron.internal.StiebelEltronConfiguration;
import org.openhab.binding.modbus.stiebeleltron.internal.dto.EnergyBlock;
import org.openhab.binding.modbus.stiebeleltron.internal.dto.SystemBlock;
import org.openhab.binding.modbus.stiebeleltron.internal.dto.SystemStateBlock;
import org.openhab.binding.modbus.stiebeleltron.internal.parser.EnergyBlockParser;
import org.openhab.binding.modbus.stiebeleltron.internal.parser.SystemBlockParser;
import org.openhab.binding.modbus.stiebeleltron.internal.parser.SystemStateBlockParser;
import org.openhab.io.transport.modbus.BasicModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.BasicPollTaskImpl;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.PollTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Modbus.StiebelEltronHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 *
 * @author Paul Frank - Initial contribution
 */
@NonNullByDefault
public class StiebelEltronHandler extends BaseThingHandler {

    @NonNullByDefault
    public abstract class AbstractBasePoller {
        /**
         * Logger instance
         */
        private final Logger logger = LoggerFactory.getLogger(StiebelEltronHandler.class);

        private volatile @Nullable PollTask pollTask;

        public AbstractBasePoller() {
        }

        public synchronized void unregisterPollTask() {
            @Nullable
            PollTask task = pollTask;
            if (task == null) {
                return;
            }
            logger.debug("Unregistering polling from ModbusManager");
            StiebelEltronHandler.this.managerRef.unregisterRegularPoll(task);

            pollTask = null;

        }

        /**
         * Register poll task This is where we set up our regular poller
         */
        public synchronized void registerPollTask(int address, int length) {

            logger.debug("Setting up regular polling");

            @Nullable
            ModbusSlaveEndpoint myendpoint = StiebelEltronHandler.this.endpoint;
            @Nullable
            StiebelEltronConfiguration myconfig = StiebelEltronHandler.this.config;
            if (myconfig == null || myendpoint == null) {
                throw new IllegalStateException("registerPollTask called without proper configuration");
            }

            logger.debug("Setting up regular polling");

            BasicModbusReadRequestBlueprint request = new BasicModbusReadRequestBlueprint(getSlaveId(),
                    ModbusReadFunctionCode.READ_INPUT_REGISTERS, address, length, myconfig.getMaxTries());

            pollTask = new BasicPollTaskImpl(myendpoint, request, new ModbusReadCallback() {

                @Override
                public void onRegisters(@Nullable ModbusReadRequestBlueprint request,
                        @Nullable ModbusRegisterArray registers) {
                    if (registers == null) {
                        logger.info("Received empty register array on poll");
                        return;
                    }
                    handlePolledData(registers);

                    if (StiebelEltronHandler.this.getThing().getStatus() != ThingStatus.ONLINE) {
                        updateStatus(ThingStatus.ONLINE);
                    }
                }

                @Override
                public void onError(@Nullable ModbusReadRequestBlueprint request, @Nullable Exception error) {
                    StiebelEltronHandler.this.handleError(error);
                }

                @Override
                public void onBits(@Nullable ModbusReadRequestBlueprint request, @Nullable BitArray bits) {
                    // don't care, we don't expect this result
                }
            });

            long refreshMillis = myconfig.getRefreshMillis();
            @Nullable
            PollTask task = pollTask;
            if (task != null) {
                StiebelEltronHandler.this.managerRef.registerRegularPoll(task, refreshMillis, 1000);
            }
        }

        protected abstract void handlePolledData(ModbusRegisterArray registers);

    }

    /**
     * Logger instance
     */
    private final Logger logger = LoggerFactory.getLogger(StiebelEltronHandler.class);

    /**
     * Configuration instance
     */
    protected @Nullable StiebelEltronConfiguration config = null;
    /**
     * Parser used to convert incoming raw messages into system blocks
     */
    private final SystemBlockParser systemBlockParser = new SystemBlockParser();
    /**
     * Parser used to convert incoming raw messages into system state blocks
     */
    private final SystemStateBlockParser systemstateBlockParser = new SystemStateBlockParser();
    /**
     * Parser used to convert incoming raw messages into model blocks
     */
    private final EnergyBlockParser energyBlockParser = new EnergyBlockParser();
    /**
     * This is the task used to poll the device
     */
    private volatile @Nullable AbstractBasePoller systemPoller = null;
    /**
     * This is the task used to poll the device
     */
    private volatile @Nullable AbstractBasePoller energyPoller = null;
    /**
     * This is the task used to poll the device
     */
    private volatile @Nullable AbstractBasePoller systemStatePoller = null;
    /**
     * This is the slave endpoint we're connecting to
     */
    protected volatile @Nullable ModbusSlaveEndpoint endpoint = null;

    /**
     * This is the slave id, we store this once initialization is complete
     */
    private volatile int slaveId;

    /**
     * Reference to the modbus manager
     */
    protected ModbusManager managerRef;

    /**
     * Instances of this handler should get a reference to the modbus manager
     *
     * @param thing      the thing to handle
     * @param managerRef the modbus manager
     */
    public StiebelEltronHandler(Thing thing, ModbusManager managerRef) {
        super(thing);
        this.managerRef = managerRef;
    }

    /**
     * Handle incoming commands. This binding is read-only by default
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Currently we do not support any commands
    }

    /**
     * Initialization: Load the config object of the block Connect to the slave
     * bridge Start the periodic polling
     */
    @Override
    public void initialize() {
        config = getConfigAs(StiebelEltronConfiguration.class);
        logger.debug("Initializing thing with properties: {}", thing.getProperties());

        startUp();
    }

    /*
     * This method starts the operation of this handler Load the config object of
     * the block Connect to the slave bridge Start the periodic polling
     */
    private void startUp() {

        connectEndpoint();

        if (endpoint == null || config == null) {
            logger.debug("Invalid endpoint/config/manager ref for sunspec handler");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        if (systemPoller == null) {
            systemPoller = new AbstractBasePoller() {
                @Override
                protected void handlePolledData(ModbusRegisterArray registers) {
                    handlePolledSystemData(registers);
                }

            };
            systemPoller.registerPollTask(500, 36);
        }
        if (energyPoller == null) {
            energyPoller = new AbstractBasePoller() {
                @Override
                protected void handlePolledData(ModbusRegisterArray registers) {
                    handlePolledEnergyData(registers);
                }

            };
            energyPoller.registerPollTask(3500, 16);
        }
        if (systemStatePoller == null) {
            systemPoller = new AbstractBasePoller() {
                @Override
                protected void handlePolledData(ModbusRegisterArray registers) {
                    handlePolledSystemStateData(registers);
                }

            };
            systemPoller.registerPollTask(2500, 2);
        }
    }

    /**
     * Dispose the binding correctly
     */
    @Override
    public void dispose() {
        tearDown();
    }

    /**
     * Unregister the poll task and release the endpoint reference
     */
    private void tearDown() {
        logger.trace("unregisterPollTasks");
        if (systemPoller != null) {
            logger.debug("Unregistering polling from ModbusManager");
            systemPoller.unregisterPollTask();

            systemPoller = null;
        }
        if (energyPoller != null) {
            logger.debug("Unregistering polling from ModbusManager");
            energyPoller.unregisterPollTask();

            energyPoller = null;
        }
        if (systemStatePoller != null) {
            logger.debug("Unregistering polling from ModbusManager");
            systemStatePoller.unregisterPollTask();

            systemPoller = null;
        }

        unregisterEndpoint();
    }

    /**
     * Returns the current slave id from the bridge
     */
    public int getSlaveId() {
        return slaveId;
    }

    /**
     * Get the endpoint handler from the bridge this handler is connected to Checks
     * that we're connected to the right type of bridge
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
            throw new IllegalStateException();
        }
    }

    /**
     * Get a reference to the modbus endpoint
     */
    private void connectEndpoint() {
        if (endpoint != null) {
            return;
        }

        ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
        if (slaveEndpointThingHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format("Bridge '%s' is offline",
                    Optional.ofNullable(getBridge()).map(b -> b.getLabel()).orElse("<null>")));
            logger.debug("No bridge handler available -- aborting init for {}", this);
            return;
        }

        try {
            slaveId = slaveEndpointThingHandler.getSlaveId();

            endpoint = slaveEndpointThingHandler.asSlaveEndpoint();
        } catch (EndpointNotInitializedException e) {
            // this will be handled below as endpoint remains null
        }

        if (endpoint == null) {
            @SuppressWarnings("null")
            String label = Optional.ofNullable(getBridge()).map(b -> b.getLabel()).orElse("<null>");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' not completely initialized", label));
            logger.debug("Bridge not initialized fully (no endpoint) -- aborting init for {}", this);
            return;
        }
    }

    /**
     * Remove the endpoint if exists
     */
    private synchronized void unregisterEndpoint() {
        endpoint = null;
    }

    /**
     * Register poll task This is where we set up our regular poller
     * 
     * private synchronized @Nullable PollTask registerPollTask(int address, int
     * length) { if (config == null || endpoint == null) { throw new
     * IllegalStateException("registerPollTask called without proper
     * configuration"); }
     * 
     * logger.debug("Setting up regular polling");
     * 
     * BasicModbusReadRequestBlueprint request = new
     * BasicModbusReadRequestBlueprint(getSlaveId(),
     * ModbusReadFunctionCode.READ_INPUT_REGISTERS, address, length, //
     * READ_MULTIPLE_REGISTERS config.getMaxTries());
     * 
     * PollTask pollTask = new BasicPollTaskImpl(endpoint, request, new
     * ModbusReadCallback() {
     * 
     * @Override public void onRegisters(@Nullable ModbusReadRequestBlueprint
     *           request,
     * @Nullable ModbusRegisterArray registers) { if (registers == null) {
     *           logger.info("Received empty register array on poll"); return; }
     *           switch(address){ case 500: handlePolledSystemData(registers);
     *           break; case 3500: handlePolledEnergyData(registers); break; }
     * 
     *           if (getThing().getStatus() != ThingStatus.ONLINE) {
     *           updateStatus(ThingStatus.ONLINE); } }
     * 
     * @Override public void onError(@Nullable ModbusReadRequestBlueprint
     *           request, @Nullable Exception error) { handleError(error); }
     * 
     * @Override public void onBits(@Nullable ModbusReadRequestBlueprint
     *           request, @Nullable BitArray bits) { // don't care, we don't expect
     *           this result } });
     * 
     *           managerRef.registerRegularPoll(pollTask, config.getRefreshMillis(),
     *           1000); return pollTask; }
     */

    /**
     * Returns value divided by the 10
     *
     * @param value       the value to alter
     * @param scaleFactor the scale factor to use (may be negative)
     * @return the scaled value as a DecimalType
     */
    protected State getScaled(Number value, Unit<?> unit) {
        return new QuantityType<>(BigDecimal.valueOf(value.longValue(), 1), unit);
    }

    /**
     * Returns high value + low value / 1000
     *
     * @param value       the value to alter
     * @param scaleFactor the scale factor to use (may be negative)
     * @return the scaled value as a DecimalType
     */
    protected State getEnergyQuantity(int high, int low) {
        double value = high * 1000 + low;
        return new QuantityType<>(value, KILOWATT_HOUR);
    }

    /**
     * This method is called each time new data has been polled from the modbus
     * slave The register array is first parsed, then each of the channels are
     * updated to the new values
     *
     * @param registers byte array read from the modbus slave
     */
    protected void handlePolledSystemData(ModbusRegisterArray registers) {
        logger.trace("System block received, size: {}", registers.size());

        SystemBlock block = systemBlockParser.parse(registers);

        // System information group
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_FEK_TEMPERATURE), getScaled(block.temperature_fek, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_FEK_TEMPERATURE_SETPOINT),
                getScaled(block.temperature_fek_setpoint, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_FEK_HUMIDITY), getScaled(block.humidity_ffk, PERCENT));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_FEK_DEWPOINT), getScaled(block.dewpoint_ffk, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_OUTDOOR_TEMPERATURE),
                getScaled(block.temperature_outdoor, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_HK1_TEMPERATURE), getScaled(block.temperature_hk1, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_HK1_TEMPERATURE_SETPOINT),
                getScaled(block.temperature_hk1_setpoint, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_SUPPLY_TEMPERATURE),
                getScaled(block.temperature_supply, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_RETURN_TEMPERATURE),
                getScaled(block.temperature_return, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_SOURCE_TEMPERATURE),
                getScaled(block.temperature_source, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_WATER_TEMPERATURE),
                getScaled(block.temperature_water, CELSIUS));
        updateState(channelUID(GROUP_SYSTEM_INFO, CHANNEL_WATER_TEMPERATURE_SETPOINT),
                getScaled(block.temperature_water_setpoint, CELSIUS));
    }

    /**
     * This method is called each time new data has been polled from the modbus
     * slave The register array is first parsed, then each of the channels are
     * updated to the new values
     *
     * @param registers byte array read from the modbus slave
     */
    protected void handlePolledEnergyData(ModbusRegisterArray registers) {
        logger.trace("Energy block received, size: {}", registers.size());

        EnergyBlock block = energyBlockParser.parse(registers);

        // Energy information group
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_PRODUCTION_HEAT_TODAY),
                new QuantityType<>(block.production_heat_today, KILOWATT_HOUR));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_PRODUCTION_HEAT_TOTAL),
                getEnergyQuantity(block.production_heat_total_high, block.production_heat_total_low));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_PRODUCTION_WATER_TODAY),
                new QuantityType<>(block.production_water_today, KILOWATT_HOUR));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_PRODUCTION_WATER_TOTAL),
                getEnergyQuantity(block.production_water_total_high, block.production_water_total_low));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_CONSUMPTION_HEAT_TODAY),
                new QuantityType<>(block.consumption_heat_today, KILOWATT_HOUR));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_CONSUMPTION_HEAT_TOTAL),
                getEnergyQuantity(block.consumption_heat_total_high, block.consumption_heat_total_low));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_CONSUMPTION_WATER_TODAY),
                new QuantityType<>(block.consumption_water_today, KILOWATT_HOUR));
        updateState(channelUID(GROUP_ENERGY_INFO, CHANNEL_CONSUMPTION_WATER_TOTAL),
                getEnergyQuantity(block.consumption_water_total_high, block.consumption_water_total_low));
    }

    protected void handlePolledSystemStateData(ModbusRegisterArray registers) {
        logger.trace("System state block received, size: {}", registers.size());

        SystemStateBlock block = systemstateBlockParser.parse(registers);
        boolean is_heating = (block.state & 16) != 0;
        updateState(channelUID(GROUP_SYSTEM_STATE, CHANNEL_IS_HEATING),
        is_heating ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
        updateState(channelUID(GROUP_SYSTEM_STATE, CHANNEL_IS_HEATING_WATER),
                (block.state & 32) != 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
        updateState(channelUID(GROUP_SYSTEM_STATE, CHANNEL_IS_COOLING),
                (block.state & 256) != 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
        updateState(channelUID(GROUP_SYSTEM_STATE, CHANNEL_IS_SUMMER),
                (block.state & 128) != 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
        updateState(channelUID(GROUP_SYSTEM_STATE, CHANNEL_IS_PUMPING),
                (block.state & 1) != 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN);

    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);

        logger.debug("Thing status changed to {}", this.getThing().getStatus().name());
        if (getThing().getStatus() == ThingStatus.ONLINE) {
            startUp();
        } else if (getThing().getStatus() == ThingStatus.OFFLINE) {
            tearDown();
        }
    }

    /**
     * Handle errors received during communication
     */
    protected void handleError(@Nullable Exception error) {
        // Ignore all incoming data and errors if configuration is not correct
        if (hasConfigurationError() || getThing().getStatus() == ThingStatus.OFFLINE) {
            return;
        }
        String msg = "";
        String cls = "";
        if (error != null) {
            cls = error.getClass().getName();
            msg = error.getMessage();
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                String.format("Error with read: %s: %s", cls, msg));
    }

    /**
     * Returns true, if we're in a CONFIGURATION_ERROR state
     *
     * @return
     */
    protected boolean hasConfigurationError() {
        ThingStatusInfo statusInfo = getThing().getStatusInfo();
        return statusInfo.getStatus() == ThingStatus.OFFLINE
                && statusInfo.getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR;
    }

    /**
     * Reset communication status to ONLINE if we're in an OFFLINE state
     */
    protected void resetCommunicationError() {
        ThingStatusInfo statusInfo = thing.getStatusInfo();
        if (ThingStatus.OFFLINE.equals(statusInfo.getStatus())
                && ThingStatusDetail.COMMUNICATION_ERROR.equals(statusInfo.getStatusDetail())) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    /**
     * Returns the channel UID for the specified group and channel id
     *
     * @param string the channel group
     * @param string the channel id in that group
     * @return the globally unique channel uid
     */
    ChannelUID channelUID(String group, String id) {
        return new ChannelUID(getThing().getUID(), group, id);
    }

}
