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
package org.openhab.binding.publictransportswitzerland.internal.stationboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.openhab.binding.publictransportswitzerland.internal.PublicTransportSwitzerlandBindingConstants.*;

/**
 * The {@link PublicTransportSwitzerlandStationboardHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jeremy Stucki - Initial contribution
 */
@NonNullByDefault
public class PublicTransportSwitzerlandStationboardHandler extends BaseThingHandler {

    // Limit the API response to the necessary fields
    private static final String fieldFilters = createFilterForFields(
            "stationboard/to",
            "stationboard/category",
            "stationboard/number",
            "stationboard/stop/departureTimestamp",
            "stationboard/stop/delay",
            "stationboard/stop/platform");

    private final ChannelGroupUID dynamicChannelGroupUID = new ChannelGroupUID(getThing().getUID(), "departures");

    private final Logger logger = LoggerFactory.getLogger(PublicTransportSwitzerlandStationboardHandler.class);

    private @Nullable ScheduledFuture<?> updateDataJob;

    public PublicTransportSwitzerlandStationboardHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // This handler does not support any commands
    }

    @Override
    public void initialize() {
        PublicTransportSwitzerlandStationboardConfiguration config = getConfigAs(PublicTransportSwitzerlandStationboardConfiguration.class);

        if (config.station == null || config.station.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        } else {
            updateStatus(ThingStatus.UNKNOWN);
            updateDataJob = scheduler.scheduleWithFixedDelay(this::updateData, 0, 60, TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        if (updateDataJob != null && !updateDataJob.isCancelled()) {
            updateDataJob.cancel(true);
        }
    }

    public void updateData() {
        PublicTransportSwitzerlandStationboardConfiguration config = getConfigAs(PublicTransportSwitzerlandStationboardConfiguration.class);

        try {
            String escapedStation = URLEncoder.encode(config.station, StandardCharsets.UTF_8.name());
            String requestUrl = BASE_URL + "stationboard?station=" + escapedStation + fieldFilters;

            String response = HttpUtil.executeUrl("GET", requestUrl, 10_000);
            logger.debug("Got response from API: {}", response);

            JsonElement jsonObject = new JsonParser().parse(response);

            updateChannels(jsonObject);
            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.warn("Unable to fetch stationboard data", e);

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            updateState(CHANNEL_TSV, new StringType("No data available"));
        }
    }

    private static String createFilterForFields(String... fields) {
        return Arrays.stream(fields).map((field) -> "&fields[]=" + field).collect(Collectors.joining());
    }

    private void updateChannels(JsonElement jsonObject) throws Exception {
        JsonArray stationboard = jsonObject.getAsJsonObject().get("stationboard").getAsJsonArray();
        createDynamicChannels(stationboard.size());

        List<String> tsvRows = new ArrayList<>();

        for (int i = 0; i < stationboard.size(); i++) {
            JsonElement jsonElement = stationboard.get(i);

            JsonObject departureObject = jsonElement.getAsJsonObject();
            JsonObject stopObject = departureObject.get("stop").getAsJsonObject();

            String category = departureObject.get("category").getAsString();
            String number = departureObject.get("number").getAsString();
            String destination = departureObject.get("to").getAsString();

            JsonElement delayElement = departureObject.get("delay");
            String delay = "";
            if (delayElement != null) {
                delay = delayElement.getAsString();
            }

            Long departureTime = stopObject.get("departureTimestamp").getAsLong();
            String track = stopObject.get("platform").getAsString();

            updateState(getChannelUIDForPosition(i), new StringType(formatDeparture(category, number, departureTime, destination, track, delay)));
            tsvRows.add(String.join("\t", category, number, departureTime.toString(), destination, track, delay));
        }

        updateState(CHANNEL_TSV, new StringType(String.join("\n", tsvRows)));
    }

    private String formatDeparture(String category, String number, Long departureTimestamp, String destination, String track, @Nullable String delay) {
        Date departureDate = new Date(departureTimestamp * 1000);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String formattedDate = timeFormat.format(departureDate);

        String train = number.startsWith(category) ? category : category + number;
        String result = String.format("%s - %s %s - Pl. %s", formattedDate, train, destination, track);

        if (delay != null && !delay.isEmpty()) {
            result += String.format(" (%s' late)", delay);
        }

        return result;
    }

    private void createDynamicChannels(int numberOfChannels) {
        List<Channel> existingChannels = getThing().getChannelsOfGroup(dynamicChannelGroupUID.getId());

        ThingBuilder thingBuilder = editThing();

        for (int i = existingChannels.size(); i < numberOfChannels; i++) {
            Channel channel = ChannelBuilder
                    .create(getChannelUIDForPosition(i), "String")
                    .withLabel("Departure " + (i + 1))
                    .build();
            thingBuilder.withChannel(channel);
        }

        updateThing(thingBuilder.build());
    }

    private ChannelUID getChannelUIDForPosition(int position) {
        return new ChannelUID(dynamicChannelGroupUID, String.valueOf(position + 1));
    }

}
