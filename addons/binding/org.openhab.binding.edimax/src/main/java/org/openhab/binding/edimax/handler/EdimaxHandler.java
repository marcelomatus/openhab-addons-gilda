/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.edimax.handler;

import static org.openhab.binding.edimax.EdimaxBindingConstants.*;

import java.io.IOException;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.edimax.config.EdimaxConfiguration;
import org.openhab.binding.edimax.internal.HTTPSend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EdimaxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Falk Harnisch - Initial contribution
 */
public class EdimaxHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(EdimaxHandler.class);

    public EdimaxHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(SWITCH_CHANNEL)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        } else if (channelUID.getId().equals(ENERGY_CHANNEL)) {
            // TODO: handle command

        } else if (channelUID.getId().equals(POWER_CHANNEL)) {
            // TODO: handle command

        }
    }

    @Override
    public void initialize() {
        Thing thing = this.getThing();
        EdimaxConfiguration config = this.getConfigAs(EdimaxConfiguration.class);
        config.getIpAddress();
        try {
            Channel channel = thing.getChannel(SWITCH_CHANNEL);
            Boolean state = createSender(config).getState(config.getIpAddress());
            if (state.equals(Boolean.TRUE)) {
                this.updateState(channel.getUID(), OnOffType.ON);
            } else if (state.equals(Boolean.FALSE)) {
                this.updateState(channel.getUID(), OnOffType.OFF);
            } else {
                logger.warn("unknown state " + state + " for channel " + channel.getUID());
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * Creates sender based on the configured password.
     *
     * @param config
     * @return
     */
    private HTTPSend createSender(EdimaxConfiguration config) {
        String password = config.getPassword();
        if (password == null) {
            return new HTTPSend();
        } else {
            return new HTTPSend(password);
        }
    }
}
