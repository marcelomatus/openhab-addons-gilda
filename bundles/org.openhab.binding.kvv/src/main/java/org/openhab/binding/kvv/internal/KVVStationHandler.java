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
package org.openhab.binding.kvv.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KVVStationHandler represents a station and holds information about the trains
 * which will arrive soon.
 *
 * @author Maximilian Hess - Initial contribution
 */
@NonNullByDefault
public class KVVStationHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(KVVStationHandler.class);

    @Nullable
    private ScheduledFuture<?> pollingJob;

    @Nullable
    private KVVBridgeHandler bridgeHandler;

    @Nullable
    private KVVStationConfig config;

    public KVVStationHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        final KVVStationConfig config = getConfigAs(KVVStationConfig.class);
        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Failed to get station configuration");
            return;
        }

        final Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Failed to get bridge");
            return;
        }

        final KVVBridgeHandler bridgeHandler = (KVVBridgeHandler) bridge.getHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR, "Failed to get bridge handler");
            return;
        }

        // creating channels
        final List<Channel> channels = new ArrayList<Channel>();
        for (int i = 0; i < bridgeHandler.getBridgeConfig().maxTrains; i++) {
            channels.add(ChannelBuilder.create(new ChannelUID(this.thing.getUID(), "train" + i + "-name"), "String")
                    .build());
            channels.add(ChannelBuilder
                    .create(new ChannelUID(this.thing.getUID(), "train" + i + "-destination"), "String").build());
            channels.add(
                    ChannelBuilder.create(new ChannelUID(this.thing.getUID(), "train" + i + "-eta"), "String").build());
        }
        this.updateThing(this.editThing().withChannels(channels).build());
        this.pollingJob = this.scheduler.scheduleWithFixedDelay(new UpdateTask(bridgeHandler, config), 0,
                bridgeHandler.getBridgeConfig().updateInterval, TimeUnit.SECONDS);

        this.config = config;
        this.bridgeHandler = bridgeHandler;
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        if (this.pollingJob != null) {
            this.pollingJob.cancel(true);
        }
    }

    /**
     * Updates the local list of departures.
     *
     * @param departures the new list of departures
     */
    private synchronized void setDepartures(final DepartureResult departures, final int maxTrains) {
        int i = 0;
        for (; i < departures.departures.size(); i++) {
            this.updateState(new ChannelUID(this.thing.getUID(), "train" + i + "-name"),
                    new StringType(departures.departures.get(i).route));
            this.updateState(new ChannelUID(this.thing.getUID(), "train" + i + "-destination"),
                    new StringType(departures.departures.get(i).destination));
            String eta = departures.departures.get(i).time;
            if (eta.equals("0")) {
                eta += " min";
            }
            this.updateState(new ChannelUID(this.thing.getUID(), "train" + i + "-eta"), new StringType(eta));
        }
        for (; i < maxTrains; i++) {
            this.updateState(new ChannelUID(this.thing.getUID(), "train" + i + "-name"), new StringType(""));
            this.updateState(new ChannelUID(this.thing.getUID(), "train" + i + "-destination"), new StringType(""));
            this.updateState(new ChannelUID(this.thing.getUID(), "train" + i + "-eta"), new StringType(""));
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (command == RefreshType.REFRESH) {
            final KVVBridgeHandler bridgeHandler = this.bridgeHandler;
            if (bridgeHandler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR, "Failed to get bridge handler");
                return;
            }
            final KVVStationConfig config = this.config;
            if (config == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Failed to get station configuration");
                return;
            }
            new UpdateTask(bridgeHandler, config).run();
        }
    }

    /**
     * Holds a single {@link TimerTask} to fetch the latest departure data.
     *
     * @author Maximilian Hess - Initial contribution
     *
     */
    public class UpdateTask extends TimerTask {

        private final KVVBridgeHandler bridgeHandler;

        private final KVVStationConfig stationConfig;

        public UpdateTask(final KVVBridgeHandler bridgeHandler, final KVVStationConfig stationConfig) {
            this.bridgeHandler = bridgeHandler;
            this.stationConfig = stationConfig;
        }

        @Override
        public void run() {
            final DepartureResult departures = this.bridgeHandler.queryKVV(this.stationConfig);
            if (departures != null) {
                setDepartures(departures, this.bridgeHandler.getBridgeConfig().maxTrains);
            }
        }
    }
}
