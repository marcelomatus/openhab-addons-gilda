/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.foxtrot.handler;

import java.io.IOException;
import java.math.BigDecimal;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.foxtrot.internal.*;
import org.openhab.binding.foxtrot.internal.config.VariableConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.foxtrot.FoxtrotBindingConstants.CHANNEL_NUMBER;

/**
 * FoxtrotNumberHandler.
 *
 * @author Radovan Sninsky
 * @since 2018-02-10 23:56
 */
public class NumberHandler extends BaseThingHandler implements RefreshableHandler {

    private final Logger logger = LoggerFactory.getLogger(NumberHandler.class);

    private String variableName;
    private RefreshGroup group;

    public NumberHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void initialize() {
        logger.debug("Initializing Number handler ...");
        VariableConfiguration config = getConfigAs(VariableConfiguration.class);

        try {
            variableName = config.var;
            group = ((FoxtrotBridgeHandler)getBridge().getHandler()).findByName(config.refreshGroup);

            logger.debug("Adding Variable handler {} into refresh group {}", this, group.getName());
            group.addHandler(this);

            updateStatus(ThingStatus.ONLINE);
        } catch (IllegalArgumentException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Unknown refresh group: "+config.refreshGroup.toUpperCase());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Variable handler resources ...");
        if (group != null) {
            logger.debug("Removing Variable handler {} from refresh group {} ...", this, group.getName());
            group.removeHandler(this);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Handling command: {} for channel: {}", command, channelUID);

        CommandExecutor ce = CommandExecutor.get();
        if (OnOffType.ON.equals(command)) {
            ce.execCommand(variableName, Boolean.TRUE);
        } else if (OnOffType.OFF.equals(command)) {
            ce.execCommand(variableName, Boolean.FALSE);
        } else if (command instanceof DecimalType || command instanceof StringType) {
            ce.execCommand(variableName, command.toFullString());
        }
    }

    @Override
    public void refreshFromPlc(PlcComSClient plcClient) {
        State newState = UnDefType.UNDEF;
        try {
            BigDecimal newValue = plcClient.getNumber(variableName);

            if (newValue != null) {
                newState = new DecimalType(newValue);
            }
        } catch (PlcComSEception e) {
            logger.warn("PLCComS returned {} while getting variable '{}' value: {}: {}", e.getType(), variableName, e.getCode(), e.getMessage());
        } catch (IOException e) {
            logger.warn("Communication with PLCComS failed while getting variable '{}' value: {}", variableName, e.getMessage());
        } finally {
            updateState(CHANNEL_NUMBER, newState);
        }
    }

    @Override
    @SuppressWarnings("StringBufferReplaceableByString")
    public String toString() {
        final StringBuilder sb = new StringBuilder("NumberHandler{");
        sb.append("'").append(variableName).append('\'');
        sb.append(", ").append(group);
        sb.append('}');
        return sb.toString();
    }
}
