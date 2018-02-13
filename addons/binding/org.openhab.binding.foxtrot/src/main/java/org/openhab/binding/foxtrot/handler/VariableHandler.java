/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.foxtrot.handler;

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
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.foxtrot.internal.RefreshGroup;
import org.openhab.binding.foxtrot.internal.config.VariableConfiguration;
import org.openhab.binding.foxtrot.internal.model.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FoxtrotNumberHandler.
 *
 * @author Radovan Sninsky
 * @since 2018-02-10 23:56
 */
public class VariableHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(VariableHandler.class);

    private String variableName;
    private RefreshGroup group;

    public VariableHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void initialize() {
        VariableConfiguration config = getConfigAs(VariableConfiguration.class);

        try {
            variableName = config.variableName;
            group = RefreshGroup.valueOf(config.refreshGroup.toUpperCase());

            group.add(new Variable(variableName, value -> {
                updateState("number", isNumber(value) ? new DecimalType(value) : UnDefType.UNDEF);

                updateState("string", new StringType(value));

                if (isBool(value)) {
                    updateState("bool", "1".equals(value) ? OnOffType.ON : OnOffType.OFF);
                }
            }));

            updateStatus(ThingStatus.ONLINE);
        } catch (IllegalArgumentException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "");
        }
    }

    @Override
    public void dispose() {
        if (group != null) {
            group.remove(variableName);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("FoxtrotNumber handle: thing: {}, channel: {}, command: {}", thing, channelUID, command);
    }

    private boolean isNumber(String value) {
        if (value == null) {
            return false;
        }

        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBool(String value) {
        return "1".equals(value) || "0".equals(value);
    }
}
