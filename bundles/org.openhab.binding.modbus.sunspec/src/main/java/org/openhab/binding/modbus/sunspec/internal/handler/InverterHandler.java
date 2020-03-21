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
package org.openhab.binding.modbus.sunspec.internal.handler;

import static org.openhab.binding.modbus.sunspec.internal.SunSpecConstants.*;

import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.modbus.sunspec.internal.dto.InverterModelBlock;
import org.openhab.binding.modbus.sunspec.internal.parser.InverterModelParser;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link InverterHandler} is responsible for handling commands, which are
 * sent to an inverter and publishing the received values to OpenHAB.
 *
 * @author Nagy Attila Gabor - Initial contribution
 */
@NonNullByDefault
public class InverterHandler extends AbstractSunSpecHandler {

    /**
     * Parser used to convert incoming raw messages into model blocks
     */
    private InverterModelParser parser = new InverterModelParser();

    /**
     * Logger instance
     */
    private final Logger logger = LoggerFactory.getLogger(InverterHandler.class);

    public InverterHandler(Thing thing, Supplier<ModbusManager> managerRef) {
        super(thing, managerRef);
    }

    /**
     *
     */
    @Override
    protected void handlePolledData(ModbusRegisterArray registers) {
        logger.trace("Model block received, size: {}", registers.size());

        InverterModelBlock block = parser.parse(registers);

        // Device information group
        updateState(new ChannelUID(getThing().getUID(), GROUP_DEVICE_INFO, CHANNEL_CABINET_TEMPERATURE),
                getScaled(block.temperatureCabinet, block.temperatureSF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_DEVICE_INFO, CHANNEL_HEATSINK_TEMPERATURE),
                getScaled(block.temperatureHeatsink, Optional.of(block.temperatureSF)));

        updateState(new ChannelUID(getThing().getUID(), GROUP_DEVICE_INFO, CHANNEL_TRANSFORMER_TEMPERATURE),
                getScaled(block.temperatureTransformer, Optional.of(block.temperatureSF)));

        updateState(new ChannelUID(getThing().getUID(), GROUP_DEVICE_INFO, CHANNEL_OTHER_TEMPERATURE),
                getScaled(block.temperatureOther, Optional.of(block.temperatureSF)));

        Integer status = block.status;
        updateState(new ChannelUID(getThing().getUID(), GROUP_DEVICE_INFO, CHANNEL_STATUS),
                status == null ? UnDefType.UNDEF : new DecimalType(status));

        // AC General group
        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_TOTAL_CURRENT),
                getScaled(block.acCurrentTotal, block.acCurrentSF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_POWER),
                getScaled(block.acPower, block.acPowerSF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_FREQUENCY),
                getScaled(block.acFrequency, block.acFrequencySF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_APPARENT_POWER),
                getScaled(block.acApparentPower, block.acApparentPowerSF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_REACTIVE_POWER),
                getScaled(block.acReactivePower, block.acReactivePowerSF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_POWER_FACTOR),
                getScaled(block.acPowerFactor, block.acPowerFactorSF));

        updateState(new ChannelUID(getThing().getUID(), GROUP_AC_GENERAL, CHANNEL_AC_LIFETIME_ENERGY),
                getScaled(block.acEnergyLifetime, block.acEnergyLifetimeSF));

        resetCommunicationError();

    }

}
