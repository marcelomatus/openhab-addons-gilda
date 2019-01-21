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
package org.openhab.binding.tradfri.handler;

import static org.openhab.binding.tradfri.TradfriBindingConstants.CHANNEL_POWER;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tradfri.internal.model.TradfriPlugData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * The {@link TradfriPlugHandler} is responsible for handling commands for individual plugs.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class TradfriPlugHandler extends TradfriThingHandler {

    private final Logger logger = LoggerFactory.getLogger(TradfriPlugHandler.class);

    public TradfriPlugHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void onUpdate(JsonElement data) {
        if (active && !(data.isJsonNull())) {
            TradfriPlugData state = new TradfriPlugData(data);
            updateStatus(state.getReachabilityStatus() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

            updateState(CHANNEL_POWER, state.getOnOffState() ? OnOffType.ON : OnOffType.OFF);
            updateDeviceProperties(state);
        }
    }

    private void setState(OnOffType onOff) {
        TradfriPlugData data = new TradfriPlugData();
        data.setOnOffState(onOff == OnOffType.ON);
        set(data.getJsonString());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (active) {
            if (command instanceof RefreshType) {
                logger.debug("Refreshing channel {}", channelUID);
                coapClient.asyncGet(this);
                return;
            }

            switch (channelUID.getId()) {
                case CHANNEL_POWER:
                    if (command instanceof OnOffType) {
                        setState(((OnOffType) command));
                    } else {
                        logger.debug("Cannot handle command '{}' for channel '{}'", command, CHANNEL_POWER);
                    }
                    break;
                default:
                    logger.error("Unknown channel UID {}", channelUID);
            }
        }
    }

}
