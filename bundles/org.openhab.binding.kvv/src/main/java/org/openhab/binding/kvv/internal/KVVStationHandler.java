package org.openhab.binding.kvv.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import com.google.gson.Gson;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVVStationHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(KVVStationHandler.class);

    private final KVVStationConfig config;

    /** the most recent set of departures */
    @Nullable
    private DepartureResult departures;

    public KVVStationHandler(final Thing thing) {
        super(thing);
        logger.info("stationhandler!");
        this.config = getConfigAs(KVVStationConfig.class);
        this.departures = null;
    }

    @Override
    public void initialize() {
        scheduler.execute(() -> {
            logger.info("Starting inital fetch");
            final UpdateTask updateThread = new UpdateTask();
            final DepartureResult departures = updateThread.get();
            if (departures == null) {
                logger.warn("Failed to get departures for '" + this.thing.getUID().getAsString() + "'");
                updateStatus(ThingStatus.OFFLINE);
                return;
            }

            final List<Channel> channels = new ArrayList<Channel>();
            for (int i = 0; i < this.config.maxTrains; i++) {
                channels.add(ChannelBuilder.create(
                    new ChannelUID(this.thing.getUID(), "train" + i + "-name"), "String").build());
                channels.add(ChannelBuilder.create(
                    new ChannelUID(this.thing.getUID(), "train" + i + "-destination"), "String").build());
                channels.add(ChannelBuilder.create(
                    new ChannelUID(this.thing.getUID(), "train" + i + "-eta"), "String").build());
            }
            this.updateThing(this.editThing().withChannels(channels).build());

            logger.info("Listing channels...");
            for (final Channel c : this.getThing().getChannels()) {
                logger.info(c.getUID().getAsString());
            }
            
            this.setDepartures(departures);
            updateStatus(ThingStatus.ONLINE);
        });
    }

    /**
     * Updates the local list of departures.
     *
     * @param departures the new list of departures
     */
    private synchronized void setDepartures(final DepartureResult departures) {
        if (departures == null) {
            return;
        }

        this.departures = departures;
        logger.info(this.departures.toString());
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        logger.info("handleCommand!!!!!!!!!!!!!");
    }

    /**
     * Holds a single {@link TimerTask} to fetch the latest departure data.
     *
     * @author Maximilian Hess - Initial contribution
     *
     */
    public class UpdateTask extends TimerTask {

        /** the url of the KVV API */
        private final String url = KVVBindingConstants.API_URL + "/departures/bystop/" + config.stationId + "?key="
                + KVVBindingConstants.API_KEY + "&maxInfos=" + config.maxTrains;

        /**
         * Returns the latest {@link DepartureResult}.
         *
         * @return the latest {@link DepartureResult}.
         */
        public @Nullable DepartureResult get() {
            try {
                final HttpURLConnection conn = (HttpURLConnection) new URL(this.url.toString()).openConnection();
                conn.setRequestMethod("GET");
                final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                final StringBuilder json = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    json.append(line);
                }

                return new Gson().fromJson(json.toString(), DepartureResult.class);
            } catch (IOException e) {
                logger.error("Failed to connect to '" + this.url + "'", e);
                return null;
            }
        }

        @Override
        public void run() {
            final DepartureResult departures = this.get();
            if (departures != null) {
                setDepartures(departures);
            }
        }

    }
    
}