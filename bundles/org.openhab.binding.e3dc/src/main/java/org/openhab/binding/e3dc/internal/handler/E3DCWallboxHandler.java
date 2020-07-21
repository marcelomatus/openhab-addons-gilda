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
package org.openhab.binding.e3dc.internal.handler;

import static org.openhab.binding.e3dc.internal.E3DCBindingConstants.*;
import static org.openhab.binding.e3dc.internal.modbus.E3DCModbusConstans.*;

import java.util.BitSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.e3dc.internal.E3DCWallboxConfiguration;
import org.openhab.binding.e3dc.internal.dto.DataConverter;
import org.openhab.binding.e3dc.internal.dto.WallboxArray;
import org.openhab.binding.e3dc.internal.dto.WallboxBlock;
import org.openhab.binding.e3dc.internal.modbus.Data.DataType;
import org.openhab.binding.e3dc.internal.modbus.ModbusDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link E3DCWallboxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class E3DCWallboxHandler extends BaseHandler {
    private final Logger logger = LoggerFactory.getLogger(E3DCWallboxHandler.class);
    private BitSet currentBitSet = new BitSet(16);
    private @Nullable E3DCWallboxConfiguration config;
    private @Nullable E3DCDeviceThingHandler bridgeHandler;

    public E3DCWallboxHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(E3DCWallboxConfiguration.class);
        super.initialize(DataType.DATA);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof OnOffType) {
            logger.info("Command {} CUID {}", command, channelUID);
            logger.info("getId {}", channelUID.getId());
            logger.info("getIdWithoutGroup {}", channelUID.getIdWithoutGroup());
            int writeValue = 0;
            synchronized (this) {
                if (channelUID.getIdWithoutGroup().equals(WB_SUNMODE_CHANNEL)) {
                    currentBitSet.set(WB_SUNMODE_BIT, command.equals(OnOffType.ON));
                } else if (channelUID.getIdWithoutGroup().equals(WB_CHARGING_ABORTED_CHANNEL)) {
                    currentBitSet.set(WB_CHARGING_ABORTED_BIT, command.equals(OnOffType.ON));
                } else if (channelUID.getIdWithoutGroup().equals(WB_SCHUKO_ON_CHANNEL)) {
                    currentBitSet.set(WB_SCHUKO_ON_BIT, command.equals(OnOffType.ON));
                } else if (channelUID.getIdWithoutGroup().equals(WB_3PHASE_CHANNEL)) {
                    currentBitSet.set(WB_3PHASE_BIT, command.equals(OnOffType.ON));
                }
                writeValue = DataConverter.toInt(currentBitSet);
                logger.info("Send {}", writeValue);
            }
            E3DCDeviceThingHandler localBridgeHandler = bridgeHandler;
            if (localBridgeHandler == null) {
                Bridge b = getBridge();
                if (b != null) {
                    localBridgeHandler = (E3DCDeviceThingHandler) b.getHandler();
                    bridgeHandler = localBridgeHandler;
                }
            }
            if (localBridgeHandler != null) {
                int wallboxId = getWallboxId(config);
                if (wallboxId != -1) {
                    localBridgeHandler.wallboxSet(wallboxId, writeValue);
                }
            }
        }
    }

    @Override
    public void dataAvailable(ModbusDataProvider provider) {
        WallboxArray blockArray = (WallboxArray) provider.getData(DataType.WALLBOX);
        if (blockArray != null) {
            int wallboxId = getWallboxId(config);
            if (wallboxId != -1) {
                WallboxBlock block = blockArray.getWallboxBlock(wallboxId);
                if (block != null) {
                    synchronized (this) {
                        currentBitSet = block.getBitSet();
                    }
                    updateState(WB_AVAILABLE_CHANNEL, block.wbAvailable);
                    updateState(WB_SUNMODE_CHANNEL, block.wbSunmode);
                    updateState(WB_CHARGING_ABORTED_CHANNEL, block.wbChargingAborted);
                    updateState(WB_CHARGING_CHANNEL, block.wbCharging);
                    updateState(WB_JACK_LOCKED_CHANNEL, block.wbJackLocked);
                    updateState(WB_JACK_PLUGGED_CHANNEL, block.wbJackPlugged);
                    updateState(WB_SCHUKO_ON_CHANNEL, block.wbSchukoOn);
                    updateState(WB_SCHUKO_PLUGGED_CHANNEL, block.wbSchukoPlugged);
                    updateState(WB_SCHUKO_LOCKED_CHANNEL, block.wbSchukoLocked);
                    updateState(WB_SCHUKO_REALY_16A_CHANNEL, block.wbSchukoRelay16);
                    updateState(WB_REALY_16A_CHANNEL, block.wbRelay16);
                    updateState(WB_RELAY_32A_CHANNEL, block.wbRelay32);
                    updateState(WB_3PHASE_CHANNEL, block.wb3phase);
                } else {
                    logger.debug("Unable to get ID {} from WallboxArray", wallboxId);
                }
            } else {
                logger.debug("Wallbox ID {} not valid", wallboxId);
            }
        } else {
            logger.debug("Unable to get {} from provider {}", DataType.WALLBOX, provider.toString());
        }
    }

    private int getWallboxId(@Nullable E3DCWallboxConfiguration c) {
        if (c != null) {
            return c.wallboxId;
        } else {
            return -1;
        }
    }
}
