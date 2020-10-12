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
package org.openhab.binding.deconz.internal.handler;

import static org.openhab.binding.deconz.internal.BindingConstants.*;
import static org.openhab.binding.deconz.internal.Util.buildUrl;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.deconz.internal.Util;
import org.openhab.binding.deconz.internal.dto.DeconzBaseMessage;
import org.openhab.binding.deconz.internal.dto.GroupAction;
import org.openhab.binding.deconz.internal.dto.GroupMessage;
import org.openhab.binding.deconz.internal.dto.GroupState;
import org.openhab.binding.deconz.internal.netutils.AsyncHttpClient;
import org.openhab.binding.deconz.internal.types.ResourceType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This light thing doesn't establish any connections, that is done by the bridge Thing.
 *
 * It waits for the bridge to come online, grab the websocket connection and bridge configuration
 * and registers to the websocket connection as a listener.
 *
 * A REST API call is made to get the initial light/rollershutter state.
 *
 * Every light and rollershutter is supported by this Thing, because a unified state is kept
 * in {@link #groupStateCache}. Every field that got received by the REST API for this specific
 * sensor is published to the framework.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GroupThingHandler extends DeconzBaseThingHandler<GroupMessage> {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Set.of(THING_TYPE_LIGHTGROUP);

    private static final double HUE_FACTOR = 65535 / 360.0;
    private static final double BRIGHTNESS_FACTOR = 2.54;

    private final Logger logger = LoggerFactory.getLogger(GroupThingHandler.class);

    private long lastCommandExpireTimestamp = 0;
    private boolean needsPropertyUpdate = false;

    /**
     * The group state.
     */
    private GroupState groupStateCache = new GroupState();

    public GroupThingHandler(Thing thing, Gson gson) {
        super(thing, gson, ResourceType.GROUPS);
    }

    @Override
    protected void requestState() {
        requestState("groups");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelId = channelUID.getId();

        GroupAction newGroupAction = new GroupAction();
        switch (channelId) {
            case CHANNEL_ALL_ON:
            case CHANNEL_ANY_ON:
                if (command instanceof RefreshType) {
                    valueUpdated(channelUID.getId(), groupStateCache);
                    return;
                }
                break;
            case CHANNEL_ALERT:
                if (command instanceof OnOffType) {
                    newGroupAction.alert = command == OnOffType.ON ? "alert" : "none";
                } else {
                    return;
                }
                break;
            case CHANNEL_COLOR:
                if (command instanceof HSBType) {
                    HSBType hsbCommand = (HSBType) command;
                    newGroupAction.bri = Util.fromPercentType(hsbCommand.getBrightness());
                    if (newGroupAction.bri > 0) {
                        newGroupAction.hue = (int) (hsbCommand.getHue().doubleValue() * HUE_FACTOR);
                        newGroupAction.sat = Util.fromPercentType(hsbCommand.getSaturation());
                    }
                } else if (command instanceof PercentType) {
                    newGroupAction.bri = Util.fromPercentType((PercentType) command);
                } else if (command instanceof DecimalType) {
                    newGroupAction.bri = ((DecimalType) command).intValue();
                } else if (command instanceof OnOffType) {
                    newGroupAction.on = OnOffType.ON.equals(command);
                } else {
                    return;
                }
                break;
            case CHANNEL_COLOR_TEMPERATURE:
                if (command instanceof DecimalType) {
                    int miredValue = Util.kelvinToMired(((DecimalType) command).intValue());
                    newGroupAction.ct = Util.constrainToRange(miredValue, ZCL_CT_MIN, ZCL_CT_MAX);
                } else {
                    return;
                }
                break;
            default:
                return;
        }

        if (newGroupAction.bri != null && newGroupAction.bri > 0) {
            newGroupAction.on = true;
        }

        AsyncHttpClient asyncHttpClient = http;
        if (asyncHttpClient == null) {
            return;
        }
        String url = buildUrl(bridgeConfig.host, bridgeConfig.httpPort, bridgeConfig.apikey, "groups", config.id,
                "action");

        String json = gson.toJson(newGroupAction);
        logger.trace("Sending {} to group {} via {}", json, config.id, url);

        asyncHttpClient.put(url, json, bridgeConfig.timeout).thenAccept(v -> {
            logger.trace("Result code={}, body={}", v.getResponseCode(), v.getBody());
        }).exceptionally(e -> {
            logger.debug("Sending command {} to channel {} failed:", command, channelUID, e);
            return null;
        });
    }

    @Override
    protected @Nullable GroupMessage parseStateResponse(AsyncHttpClient.Result r) {
        if (r.getResponseCode() == 403) {
            return null;
        } else if (r.getResponseCode() == 200) {
            GroupMessage groupMessage = gson.fromJson(r.getBody(), GroupMessage.class);
            return groupMessage;
        } else {
            throw new IllegalStateException("Unknown status code " + r.getResponseCode() + " for full state request");
        }
    }

    @Override
    protected void processStateResponse(@Nullable GroupMessage stateResponse) {
        if (stateResponse == null) {
            return;
        }
        messageReceived(config.id, stateResponse);
    }

    private void valueUpdated(String channelId, GroupState newState) {
        switch (channelId) {
            case CHANNEL_ALL_ON:
                updateState(channelId, OnOffType.from(newState.all_on));
                break;
            case CHANNEL_ANY_ON:
                updateState(channelId, OnOffType.from(newState.any_on));
                break;
            default:
        }
    }

    @Override
    public void messageReceived(String sensorID, DeconzBaseMessage message) {
        if (message instanceof GroupMessage) {
            GroupMessage groupMessage = (GroupMessage) message;
            logger.trace("{} received {}", thing.getUID(), groupMessage);
            GroupState groupState = groupMessage.state;
            if (groupState != null) {
                updateStatus(ThingStatus.ONLINE);
                thing.getChannels().stream().map(c -> c.getUID().getId()).forEach(c -> valueUpdated(c, groupState));
            }
        }
    }
}
