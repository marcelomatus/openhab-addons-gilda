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
package org.openhab.binding.nibeuplink.internal.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.nibeuplink.internal.AtomicReferenceTrait;
import org.openhab.binding.nibeuplink.internal.command.UpdateSetting;
import org.openhab.binding.nibeuplink.internal.config.NibeUplinkConfiguration;
import org.openhab.binding.nibeuplink.internal.connector.UplinkWebInterface;
import org.openhab.binding.nibeuplink.internal.model.ChannelList;
import org.openhab.binding.nibeuplink.internal.model.CustomChannels;
import org.openhab.binding.nibeuplink.internal.model.NibeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UplinkBaseHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Friese - initial contribution
 */
@NonNullByDefault
public abstract class UplinkBaseHandler extends BaseThingHandler implements NibeUplinkHandler, AtomicReferenceTrait {
    private final Logger logger = LoggerFactory.getLogger(UplinkBaseHandler.class);

    private final long POLLING_INITIAL_DELAY = 30;
    private final long HOUSE_KEEPING_INITIAL_DELAY = 300;

    private Set<NibeChannel> deadChannels = new HashSet<>(100);
    private final CustomChannels customChannelList = new CustomChannels();

    /**
     * Interface object for querying the NibeUplink web interface
     */
    private UplinkWebInterface webInterface;

    /**
     * Schedule for polling
     */
    private final AtomicReference<@Nullable Future<?>> pollingJobReference;

    /**
     * Schedule for periodic cleaning dead channel list
     */
    private final AtomicReference<@Nullable Future<?>> deadChannelHouseKeepingReference;

    public UplinkBaseHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.webInterface = new UplinkWebInterface(scheduler, this, httpClient);
        this.pollingJobReference = new AtomicReference<>(null);
        this.deadChannelHouseKeepingReference = new AtomicReference<>(null);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!(command instanceof RefreshType)) {
            logger.debug("command for {}: {}", channelUID.getIdWithoutGroup(), command.toString());
            NibeChannel channel = getSpecificChannel(channelUID.getIdWithoutGroup());
            if (channel != null && !channel.isReadOnly()) {
                webInterface.enqueueCommand(new UpdateSetting(this, channel, command));
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("About to initialize NibeUplink");
        NibeUplinkConfiguration config = getConfiguration();

        logger.debug("NibeUplink initialized with configuration: {}", config);

        setupCustomChannels();

        startPolling();
        webInterface.start();
        updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "waiting for web api login");
    }

    /**
     * initialize the custom channels out of the configuration
     *
     * @param config the active configuration
     */
    private void setupCustomChannels() {
        customChannelList.clearList();
        for (Channel channel : getThing().getChannels()) {
            if (CustomChannels.isCustomChannel(channel)) {
                customChannelList.registerCustomChannel(channel);
            }
        }
    }

    /**
     * Start the polling.
     */
    private void startPolling() {
        updateJobReference(pollingJobReference, scheduler.scheduleWithFixedDelay(new UplinkPolling(this),
                POLLING_INITIAL_DELAY, getConfiguration().getPollingInterval(), TimeUnit.SECONDS));
        updateJobReference(deadChannelHouseKeepingReference, scheduler.scheduleWithFixedDelay(deadChannels::clear,
                HOUSE_KEEPING_INITIAL_DELAY, getConfiguration().getHouseKeepingInterval(), TimeUnit.SECONDS));
    }

    /**
     * Disposes the bridge.
     */
    @Override
    public void dispose() {
        logger.debug("Handler disposed.");

        cancelJobReference(pollingJobReference);
        cancelJobReference(deadChannelHouseKeepingReference);

        // the webinterface also makes use of the scheduler and must stop it's jobs
        webInterface.dispose();
    }

    @Override
    public UplinkWebInterface getWebInterface() {
        return webInterface;
    }

    /**
     * will update all channels provided in the map
     *
     * @param values map containing the data updates
     */
    @Override
    public void updateChannelStatus(Map<NibeChannel, State> values) {
        logger.debug("Handling channel update. ({} Channels)", values.size());

        for (NibeChannel channel : values.keySet()) {
            if (getChannels().contains(channel)) {
                State value = values.get(channel);
                logger.debug("Channel is to be updated: {}: {}", channel.getFQName(), value);
                updateState(channel.getFQName(), value);
            } else {
                logger.debug("Could not identify channel: {} for model {}", channel.getFQName(),
                        getThing().getThingTypeUID().getAsString());
            }
        }
    }

    @Override
    public Set<NibeChannel> getDeadChannels() {
        return deadChannels;
    }

    @Override
    public void setStatusInfo(ThingStatus status, ThingStatusDetail statusDetail, String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @Override
    public NibeUplinkConfiguration getConfiguration() {
        return this.getConfigAs(NibeUplinkConfiguration.class);
    }
    protected ChannelList getCustomChannels() {
        return customChannelList;
    }

}
