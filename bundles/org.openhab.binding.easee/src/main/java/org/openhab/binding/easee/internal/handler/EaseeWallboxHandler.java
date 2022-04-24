/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.easee.internal.handler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.easee.internal.AtomicReferenceTrait;
import org.openhab.binding.easee.internal.config.EaseeConfiguration;
import org.openhab.binding.easee.internal.connector.WebInterface;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EaseeWallboxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Friese - initial contribution
 */
@NonNullByDefault
public class EaseeWallboxHandler extends BaseThingHandler implements EaseeHandler, AtomicReferenceTrait {
    private final Logger logger = LoggerFactory.getLogger(EaseeWallboxHandler.class);

    private final long POLLING_INITIAL_DELAY = 1;

    /**
     * Interface object for querying the Easee web interface
     */
    private WebInterface webInterface;

    /**
     * Schedule for polling live data
     */
    private final AtomicReference<@Nullable Future<?>> dataPollingJobReference;

    public EaseeWallboxHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.webInterface = new WebInterface(scheduler, this, httpClient);
        this.dataPollingJobReference = new AtomicReference<>(null);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("command for {}: {}", channelUID, command);
        // write access is not yet supported.
    }

    @Override
    public void initialize() {
        logger.debug("About to initialize Easee");
        EaseeConfiguration config = getConfiguration();
        logger.debug("Easee initialized with configuration: {}", config);

        startPolling();
        webInterface.start();
        updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "waiting for web api login");
    }

    /**
     * Start the polling.
     */
    private void startPolling() {
        updateJobReference(dataPollingJobReference, scheduler.scheduleWithFixedDelay(new EaseeCloudPolling(this),
                POLLING_INITIAL_DELAY, getConfiguration().getDataPollingInterval(), TimeUnit.MINUTES));

    }

    /**
     * Disposes the bridge.
     */
    @Override
    public void dispose() {
        logger.debug("Handler disposed.");

        cancelJobReference(dataPollingJobReference);

        webInterface.dispose();
    }

    @Override
    public WebInterface getWebInterface() {
        return webInterface;
    }

    /**
     * will update all channels provided in the map
     */
    @Override
    public void updateChannelStatus(Map<Channel, State> values) {
        logger.debug("Handling channel update.");

        for (Channel channel : values.keySet()) {
            if (getChannels().contains(channel)) {
                State value = values.get(channel);
                if (value != null) {
                    logger.debug("Channel is to be updated: {}: {}", channel.getUID().getAsString(), value);
                    updateState(channel.getUID(), value);
                } else {
                    logger.debug("Value is null or not provided by Easee Cloud (channel: {})",
                            channel.getUID().getAsString());
                    updateState(channel.getUID(), UnDefType.UNDEF);
                }
            } else {
                logger.debug("Could not identify channel: {} for model {}", channel.getUID().getAsString(),
                        getThing().getThingTypeUID().getAsString());
            }
        }
    }

    @Override
    public void setStatusInfo(ThingStatus status, ThingStatusDetail statusDetail, String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @Override
    public EaseeConfiguration getConfiguration() {
        return this.getConfigAs(EaseeConfiguration.class);
    }

    @Override
    public List<Channel> getChannels() {
        return getThing().getChannels();
    }

    @Override
    public @Nullable Channel getChannel(String groupId, String channelId) {
        ThingUID thingUID = this.getThing().getUID();
        ChannelGroupUID channelGroupUID = new ChannelGroupUID(thingUID, groupId);
        Channel channel = getThing().getChannel(new ChannelUID(channelGroupUID, channelId));
        return channel;
    }
}
