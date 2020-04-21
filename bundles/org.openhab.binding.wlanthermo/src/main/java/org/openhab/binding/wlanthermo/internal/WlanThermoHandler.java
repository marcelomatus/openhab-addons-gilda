/**
 * Copyright (c) 2020-2020 Contributors to the openHAB project
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
package org.openhab.binding.wlanthermo.internal;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

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
import org.openhab.binding.wlanthermo.internal.api.data.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WlanThermoHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Schlipp - Initial contribution
 */
@NonNullByDefault
public class WlanThermoHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(WlanThermoHandler.class);

    private @Nullable WlanThermoConfiguration config;
    private HttpClient httpClient = new HttpClient();
    @Nullable
    private ScheduledFuture<?> pollingScheduler;
    private Gson gson = new Gson();
    Data data = new Data();

    public WlanThermoHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            State s = data.getState(channelUID);
            if (s != null)
                updateState(channelUID, s);
        } else {
            data.setState(channelUID, command);
            push();
        }
    }

    private void push() {
        //TODO: Push changed settings to device
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(WlanThermoConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);
        try {
            httpClient.start();
        
            scheduler.execute(() -> {
                checkConnection();
            });

            logger.debug("Finished initializing!");
        } catch (Exception e) {
            logger.error("Failed to initialize!", e);
        }
    }

    private void checkConnection() {
        try {
            if (httpClient.GET(config.getUri()).getStatus() == 200) {
                updateStatus(ThingStatus.ONLINE);
                pollingScheduler = scheduler.scheduleWithFixedDelay(() -> {
                    update();
                }, 0, config.getPollingInterval(), TimeUnit.SECONDS);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "WlanThermo not found under given address.");
            }
        } catch (InterruptedException|ExecutionException|TimeoutException|URISyntaxException e) {
            logger.debug("Failed to connect.", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
            pollingScheduler = scheduler.schedule(() -> {
                checkConnection();
            }, config.getPollingInterval(), TimeUnit.SECONDS);
        }
    }

    private void update() {
        try {
            String json = httpClient.GET(config.getUri("/data")).getContentAsString();
            data = gson.fromJson(json, Data.class);
            for (Channel channel : thing.getChannels()) {
                ChannelUID channelUID = channel.getUID();
                State s = data.getState(channelUID);
                if (s != null) updateState(channelUID, s);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | URISyntaxException e) {
            logger.debug("Update failed, checking connection", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Update failed, reconnecting...");
            checkConnection();
        }
    }

    @Override
    public void dispose() {
        if (pollingScheduler != null && !pollingScheduler.isCancelled()) {
            pollingScheduler.cancel(true);
        }
        try {
            httpClient.stop();
        } catch (Exception e) {
            logger.error("Failed to stop HttpClient", e);
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE);
    }
}
