/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.wled.internal.handlers;

import static org.openhab.binding.wled.internal.WLedBindingConstants.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.wled.internal.WLedActions;
import org.openhab.binding.wled.internal.WLedConfiguration;
import org.openhab.binding.wled.internal.WledDynamicStateDescriptionProvider;
import org.openhab.binding.wled.internal.api.ApiException;
import org.openhab.binding.wled.internal.api.WledApi;
import org.openhab.binding.wled.internal.api.WledApiFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WLedBridgeHandler} is responsible for talking and parsing data to/from the WLED device.
 *
 * @author Matthew Skinner - Initial contribution
 */

@NonNullByDefault
public class WLedBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public final WledDynamicStateDescriptionProvider stateDescriptionProvider;
    private Map<Integer, ThingHandler> segmentHandlers = new HashMap<Integer, ThingHandler>();
    private WledApiFactory apiFactory;
    public boolean hasWhite = false;
    @Nullable
    WledApi api;
    private @Nullable ScheduledFuture<?> pollingFuture = null;
    public WLedConfiguration config = new WLedConfiguration();

    public WLedBridgeHandler(Bridge bridge, WledApiFactory apiFactory,
            WledDynamicStateDescriptionProvider stateDescriptionProvider) {
        super(bridge);
        this.apiFactory = apiFactory;
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    public void savePreset(int position, String presetName) {
        try {
            if (api != null) {
                api.savePreset(position, presetName);
            }
        } catch (ApiException e) {
        }
    }

    public void removeChannels(ArrayList<Channel> removeChannels) {
        // if (!removeChannels.isEmpty()) {
        // ThingBuilder thingBuilder = editThing();
        // thingBuilder.withoutChannels(removeChannels);
        // updateThing(thingBuilder.build());
        // }
    }

    public void update(int segmentIndex, String channelID, State state) {
        WLedSegmentHandler segmentHandler = (WLedSegmentHandler) segmentHandlers.get(segmentIndex);
        if (segmentHandler != null) {
            segmentHandler.update(channelID, state);
        }
    }

    @Override
    public void childHandlerInitialized(final ThingHandler childHandler, final Thing childThing) {
        BigDecimal segmentIndex = (BigDecimal) childThing.getConfiguration().get(CONFIG_SEGMENT_INDEX);
        segmentHandlers.put(segmentIndex.intValue(), childHandler);
    }

    @Override
    public void childHandlerDisposed(final ThingHandler childHandler, final Thing childThing) {
        BigDecimal segmentIndex = (BigDecimal) childThing.getConfiguration().get(CONFIG_SEGMENT_INDEX);
        segmentHandlers.remove(segmentIndex.intValue());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        WledApi localApi = api;
        if (localApi == null) {
            return;
        }
        try {
            switch (channelUID.getId()) {
                case CHANNEL_SEGMENT_BRIGHTNESS:
                    if (command instanceof OnOffType) {
                        localApi.setGlobalOn(OnOffType.ON.equals(command));
                    } else if (command instanceof PercentType) {
                        if (PercentType.ZERO.equals(command)) {
                            localApi.setGlobalOn(false);
                            return;
                        }
                        localApi.setGlobalBrightness((PercentType) command);
                    }
                    break;
                case CHANNEL_SLEEP:
                    localApi.setSleep(OnOffType.ON.equals(command));
                    break;
                case CHANNEL_PLAYLISTS:
                    localApi.setPreset(command.toString());
                    break;
                case CHANNEL_SYNC_SEND:
                    localApi.setUdpSend(OnOffType.ON.equals(command));
                    break;
                case CHANNEL_SYNC_RECEIVE:
                    localApi.setUdpRecieve(OnOffType.ON.equals(command));
                    break;
                case CHANNEL_LIVE_OVERRIDE:
                    localApi.setLiveOverride(command.toString());
                    break;
            }
        } catch (ApiException e) {
            logger.debug("Exception occured:{}", e.getMessage());
        }
    }

    private void pollState() {
        WledApi localApi = api;
        try {
            if (localApi == null) {
                api = apiFactory.getApi(this);
                api.initialize();
            }
            if (localApi == null) {
                return;
            }
            localApi.update();
            updateStatus(ThingStatus.ONLINE);
        } catch (ApiException e) {
            api = null;// Firmware may be updated so need to check next connect
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(WLedConfiguration.class);
        if (!config.address.contains("://")) {
            logger.debug("Address was not entered in correct format, it may be the raw IP so adding http:// to start");
            config.address = "http://" + config.address;
        }
        pollingFuture = scheduler.scheduleWithFixedDelay(this::pollState, 0, config.pollTime, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        Future<?> future = pollingFuture;
        if (future != null) {
            future.cancel(true);
            pollingFuture = null;
        }
        api = null; // re-initialize api after configuration change
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(WLedActions.class);
    }
}
