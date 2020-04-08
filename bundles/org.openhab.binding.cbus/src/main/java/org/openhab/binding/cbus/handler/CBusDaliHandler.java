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
package org.openhab.binding.cbus.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daveoxley.cbus.CGateException;
import com.daveoxley.cbus.Group;

/**
 * The {@link CBusDaliHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
@NonNullByDefault
public class CBusDaliHandler extends CBusGroupHandler {

    private final Logger logger = LoggerFactory.getLogger(CBusDaliHandler.class);

    public CBusDaliHandler(Thing thing) {
        super(thing, CBusBindingConstants.CBUS_APPLICATION_DALI);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Group group = this.group;
        if (group == null) {
            return;
        }
        if (command instanceof RefreshType) {
            try {
                int level = group.getLevel();
                logger.debug("handle RefreshType Command for Chanell {} Group {} level {}", channelUID, groupId, level);
                if (channelUID.getId().equals(CBusBindingConstants.CHANNEL_LEVEL)) {
                    updateState(channelUID, new PercentType((int) (level * 100 / 255.0)));
                }
            } catch (CGateException e) {
                logger.warn("Failed to getLevel for group {}", groupId, e);
            }
        } else {
            if (channelUID.getId().equals(CBusBindingConstants.CHANNEL_LEVEL)) {
                logger.debug("Channel Level command for {}: {}", channelUID, command);
                try {
                    if (command instanceof OnOffType) {
                        if (command.equals(OnOffType.ON)) {
                            group.on();
                        } else if (command.equals(OnOffType.OFF)) {
                            group.off();
                        }
                    } else if (command instanceof PercentType) {
                        PercentType value = (PercentType) command;
                        group.ramp((int) Math.round(value.doubleValue() / 100 * 255), 0);
                    } else if (command instanceof IncreaseDecreaseType) {
                        logger.warn("Increase/Decrease not implemented for {}", channelUID);
                    }
                } catch (CGateException e) {
                    logger.warn("Cannot send command {} to {}", command, group, e);
                }
            }
        }
    }

    public void updateGroup(int updateApplicationId, int updateGroupId, String value) {
        if (updateGroupId == groupId && updateApplicationId == applicationId) {
            Thing thing = getThing();
            Channel channel = thing.getChannel(CBusBindingConstants.CHANNEL_LEVEL);
            if (channel != null) {
                ChannelUID channelUID = channel.getUID();

                if ("on".equalsIgnoreCase(value) || "255".equalsIgnoreCase(value)) {
                    updateState(channelUID, OnOffType.ON);
                    updateState(channelUID, new PercentType(100));
                } else if ("off".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value)) {
                    updateState(channelUID, OnOffType.OFF);
                    updateState(channelUID, new PercentType(0));
                } else {
                    try {
                        int v = Integer.parseInt(value);
                        PercentType perc = new PercentType(Math.round(v * 100 / 255));
                        updateState(channelUID, perc);
                    } catch (NumberFormatException e) {
                        logger.warn(
                                "Invalid value presented to channel {}. Received {}, expected On/Off or decimal value",
                                channelUID, value);
                    }
                }
                logger.debug("Updating CBus Lighting Group {} with value {}", thing.getUID(), value);
            } else {
                logger.debug("Failed to Updat CBus Lighting Group {} with value {}: No Channel", thing.getUID(), value);
            }
        }
    }
}
