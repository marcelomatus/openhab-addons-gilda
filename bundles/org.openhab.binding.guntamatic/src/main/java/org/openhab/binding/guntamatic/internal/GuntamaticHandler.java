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
package org.openhab.binding.guntamatic.internal;

import static org.openhab.binding.guntamatic.internal.GuntamaticBindingConstants.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;
import javax.measure.quantity.Volume;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The {@link GuntamaticHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Weger Michael - Initial contribution
 */
@NonNullByDefault
public class GuntamaticHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(GuntamaticHandler.class);
    private final HttpClient httpClient;

    private @Nullable ScheduledFuture<?> pollingFuture = null;

    private GuntamaticConfiguration config = new GuntamaticConfiguration();
    private Boolean channelsInitialized = false;
    private GuntamaticChannelTypeProvider guntamaticChannelTypeProvider;
    private Map<Integer, String> channels = new HashMap<>();
    private Map<Integer, String> types = new HashMap<>();
    private Map<Integer, String> units = new HashMap<>();

    public GuntamaticHandler(Thing thing, HttpClient httpClient,
            GuntamaticChannelTypeProvider guntamaticChannelTypeProvider) {
        super(thing);
        this.httpClient = httpClient;
        this.guntamaticChannelTypeProvider = guntamaticChannelTypeProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!(command instanceof RefreshType)) {
            if (!config.key.isBlank()) {
                String param;
                String channelID = channelUID.getId();
                switch (channelID) {
                    case CHANNEL_CONTROLBOILERAPPROVAL:
                        param = getThing().getProperties().get(PARAMETER_BOILERAPPROVAL);
                        break;
                    case CHANNEL_CONTROLPROGRAM:
                        param = getThing().getProperties().get(PARAMETER_PROGRAM);
                        break;
                    case CHANNEL_CONTROLHEATCIRCPROGRAM0:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM1:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM2:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM3:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM4:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM5:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM6:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM7:
                    case CHANNEL_CONTROLHEATCIRCPROGRAM8:
                        param = getThing().getProperties().get(PARAMETER_HEATCIRCPROGRAM).replace("x",
                                channelID.substring(channelID.length() - 1));
                        break;
                    case CHANNEL_CONTROLWWHEAT0:
                    case CHANNEL_CONTROLWWHEAT1:
                    case CHANNEL_CONTROLWWHEAT2:
                        param = getThing().getProperties().get(PARAMETER_WWHEAT).replace("x",
                                channelID.substring(channelID.length() - 1));
                        break;
                    case CHANNEL_CONTROLEXTRAWWHEAT0:
                    case CHANNEL_CONTROLEXTRAWWHEAT1:
                    case CHANNEL_CONTROLEXTRAWWHEAT2:
                        param = getThing().getProperties().get(PARAMETER_EXTRAWWHEAT).replace("x",
                                channelID.substring(channelID.length() - 1));
                        break;
                    default:
                        return;
                }
                String response = sendGetRequest(PARSET_URL, "syn=" + param, "value=" + command.toString());
                if (response != null) {
                    State newState = new StringType(response);
                    updateState(channelID, newState);
                }
            } else {
                logger.warn("A 'key' needs to be configured in order to control the Guntamatic Heating System");
            }
        }
    }

    private void parseAndUpdate(String html) {
        String[] daqdata = html.split("\\n");

        for (Integer i : channels.keySet()) {
            String channel = channels.get(i);
            String unit = units.get(i);
            if ((channel != null) && (unit != null) && (i < daqdata.length)) {
                String value = daqdata[i];
                Channel chn = thing.getChannel(channel);
                if ((chn != null) && (value != null)) {
                    value = value.trim();
                    String typeName = chn.getAcceptedItemType();
                    if ("Switch".equals(typeName)) {
                        // Guntamatic uses German OnOff when configured to German and English OnOff for all other
                        // languages
                        if ("ON".equals(value) || "EIN".equals(value)) {
                            updateState(channel, OnOffType.ON);
                        } else if ("OFF".equals(value) || "AUS".equals(value)) {
                            updateState(channel, OnOffType.OFF);
                        }
                    } else {
                        try {
                            State newState = null;
                            if ("Number".equals(typeName)) {
                                if (unit.isBlank()) {
                                    newState = new DecimalType(value);
                                }
                            } else if ("Number:Dimensionless".equals(typeName)) {
                                if ("%".equals(unit)) {
                                    newState = new QuantityType<Dimensionless>(Double.parseDouble(value),
                                            Units.PERCENT);
                                }
                            } else if ("Number:Temperature".equals(typeName)) {
                                if ("°C".equals(unit)) {
                                    newState = new QuantityType<Temperature>(Double.parseDouble(value),
                                            SIUnits.CELSIUS);
                                } else if ("°F".equals(unit)) {
                                    newState = new QuantityType<Temperature>(Double.parseDouble(value),
                                            ImperialUnits.FAHRENHEIT);
                                }
                            } else if ("Number:Volume".equals(typeName)) {
                                if ("m³".equals(unit)) {
                                    newState = new QuantityType<Volume>(Double.parseDouble(value), SIUnits.CUBIC_METRE);
                                }
                            } else if ("Number:Time".equals(typeName)) {
                                if ("d".equals(unit)) {
                                    newState = new QuantityType<Time>(Double.parseDouble(value), Units.DAY);
                                } else if ("h".equals(unit)) {
                                    newState = new QuantityType<Time>(Double.parseDouble(value), Units.HOUR);
                                }
                            } else if ("String".equals(typeName)) {
                                if (unit.isBlank()) {
                                    newState = new StringType(value);
                                }
                            }

                            if (newState != null) {
                                updateState(channel, newState);
                            } else {
                                logger.warn("Data for unknown typeName '{}' or unit '{}' received", typeName, unit);
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("NumberFormatException: {}", ((e.getMessage() != null) ? e.getMessage() : ""));
                        }
                    }
                }
            } else {
                logger.warn("Data for not intialized ChannelId '{}' received", i);
            }
        }
    }

    private void parseAndJsonInit(String html) {
        try {
            // remove non JSON compliant, empty element ",,"
            JsonArray json = JsonParser.parseString(html.replace(",,", ",")).getAsJsonArray();
            for (int i = 1; i < json.size(); i++) {
                JsonObject points = json.get(i).getAsJsonObject();
                if (points.has("id")) {
                    int id = points.get("id").getAsInt();
                    if (points.has("type")) {
                        String type = points.get("type").getAsString();
                        types.put(id, type);
                    }
                }
            }
        } catch (JsonParseException e) {
            logger.warn("Invalid JSON data will be ignored: '{}'", html.replace(",,", ","));
        }
    }

    private void parseAndInit(String html) {
        String[] daqdesc = html.split("\\n");
        List<Channel> channelList = new ArrayList<>();

        for (String channelID : CHANNELIDS) {
            Channel channel = thing.getChannel(channelID);
            if (channel == null) {
                logger.warn("Static Channel '{}' is not present: remove and re-add Thing", channelID);
            } else {
                channelList.add(channel);
            }
        }

        for (int i = 0; i < daqdesc.length; i++) {
            String[] param = daqdesc[i].split(";");
            String label = param[0].replace("C02", "CO2");

            if (!"reserved".equals(label)) {
                String channel = toLowerCamelCase(replaceUmlaut(label));
                label = label.substring(0, 1).toUpperCase() + label.substring(1);

                String unit;
                if ((param.length == 1) || (param[1].isBlank())) {
                    unit = "";
                } else {
                    unit = param[1].trim().replace("m3", "m³");
                }

                boolean channelInitialized = channels.containsValue(channel);
                if (!channelInitialized) {
                    String itemType;
                    String pattern;
                    String type = types.get(i);
                    if (type == null) {
                        type = "";
                    }

                    if ("boolean".equals(type)) {
                        itemType = "Switch";
                        pattern = "";
                    } else if ("integer".equals(type)) {
                        pattern = "%d";
                        if (unit.isBlank()) {
                            itemType = "Number";
                        } else {
                            itemType = guessQuantityType(unit);
                            pattern += " %unit%";
                        }
                    } else if ("float".equals(type)) {
                        pattern = "%.2f";
                        if (unit.isBlank()) {
                            itemType = "Number";
                        } else {
                            itemType = guessQuantityType(unit);
                            pattern += " %unit%";
                        }
                    } else if ("string".equals(type)) {
                        itemType = "String";
                        pattern = "%s";
                    } else {
                        if (unit.isBlank()) {
                            itemType = "String";
                            pattern = "%s";
                        } else {
                            itemType = guessQuantityType(unit);
                            pattern = "%.2f %unit%";
                        }
                    }

                    ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, channel);
                    guntamaticChannelTypeProvider.addChannelType(channelTypeUID, channel, itemType,
                            "Guntamatic " + label, false, pattern);
                    Channel newChannel = ChannelBuilder.create(new ChannelUID(thing.getUID(), channel), itemType)
                            .withType(channelTypeUID).withKind(ChannelKind.STATE).withLabel(label).build();
                    channelList.add(newChannel);
                    channels.put(i, channel);
                    units.put(i, unit);

                    logger.debug(
                            "Supported Channel: Idx: '{}', Name: '{}'/'{}', Type: '{}'/'{}', Unit: '{}', Pattern '{}' ",
                            String.format("%03d", i), label, channel, type, itemType, unit, pattern);
                }
            }
        }
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channelList);
        updateThing(thingBuilder.build());
        channelsInitialized = true;
    }

    private String guessQuantityType(String unit) {
        String quantityType = "Number";

        if ("%".equals(unit)) {
            quantityType += ":Dimensionless";
        } else if ("°C".equals(unit) || "°F".equals(unit)) {
            quantityType += ":Temperature";
        } else if ("m³".equals(unit)) {
            quantityType += ":Volume";
        } else if ("d".equals(unit) || "h".equals(unit)) {
            quantityType += ":Time";
        } else {
            logger.warn("Unsupported unit '{}' detected: using native '{}' type", unit, quantityType);
        }

        return quantityType;
    }

    private static String replaceUmlaut(String input) {

        // replace all lower Umlauts
        String output = input.replace("ü", "ue").replace("ö", "oe").replace("ä", "ae").replace("ß", "ss");

        // first replace all capital umlaute in a non-capitalized context (e.g. Übung)
        output = output.replaceAll("Ü(?=[a-zäöüß ])", "Ue").replaceAll("Ö(?=[a-zäöüß ])", "Oe")
                .replaceAll("Ä(?=[a-zäöüß ])", "Ae");

        // now replace all the other capital umlaute
        output = output.replace("Ü", "UE").replace("Ö", "OE").replace("Ä", "AE");

        return output;
    }

    private String toLowerCamelCase(String input) {
        char delimiter = ' ';
        String output = input.replace("´", "").replaceAll("[^\\w]", String.valueOf(delimiter));

        StringBuilder builder = new StringBuilder();
        boolean nextCharLow = true;

        for (int i = 0; i < output.length(); i++) {
            char currentChar = output.charAt(i);
            if (delimiter == currentChar) {
                nextCharLow = false;
            } else if (nextCharLow) {
                builder.append(Character.toLowerCase(currentChar));
            } else {
                builder.append(Character.toUpperCase(currentChar));
                nextCharLow = true;
            }
        }
        return builder.toString();
    }

    private @Nullable String sendGetRequest(String url, String... params) {
        String errorReason = "";
        String req = "http://" + config.hostname + url;

        if (!config.key.isBlank()) {
            req += "?key=" + config.key;
        }

        for (int i = 0; i < params.length; i++) {
            if ((i == 0) && config.key.isBlank()) {
                req += "?";
            } else {
                req += "&";
            }
            req += params[i];
        }

        Request request = httpClient.newRequest(req);
        request.method(HttpMethod.GET).timeout(30, TimeUnit.SECONDS).header(HttpHeader.ACCEPT_ENCODING, "gzip");

        try {
            ContentResponse contentResponse = request.send();
            if (HttpStatus.OK_200 == contentResponse.getStatus()) {
                if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                    updateStatus(ThingStatus.ONLINE);
                }
                try {
                    String response = new String(contentResponse.getContent(), Charset.forName(config.encoding));
                    if (url.equals(DAQEXTDESC_URL)) {
                        parseAndJsonInit(response);
                    } else if (url.equals(DAQDATA_URL)) {
                        parseAndUpdate(response);
                    } else if (url.equals(DAQDESC_URL)) {
                        parseAndInit(response);
                    } else if (url.equals(PARSET_URL)) {
                        // via return
                    }
                    return response;
                } catch (IllegalArgumentException e) {
                    errorReason = String.format("IllegalArgumentException: %s",
                            ((e.getMessage() != null) ? e.getMessage() : ""));
                }
            } else {
                errorReason = String.format("Guntamatic request failed with %d: %s", contentResponse.getStatus(),
                        ((contentResponse.getReason() != null) ? contentResponse.getReason() : ""));
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: Guntamatic was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", ((e.getMessage() != null) ? e.getMessage() : ""));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorReason = String.format("InterruptedException: %s", ((e.getMessage() != null) ? e.getMessage() : ""));
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorReason);
        return null;
    }

    private void pollGuntamatic() {
        if (channelsInitialized == false) {
            if (!config.key.isBlank()) {
                sendGetRequest(DAQEXTDESC_URL);
            }
            sendGetRequest(DAQDESC_URL);
        } else {
            sendGetRequest(DAQDATA_URL);
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(GuntamaticConfiguration.class);
        if (config.hostname.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid hostname configuration");
        } else {
            updateStatus(ThingStatus.UNKNOWN);
            pollingFuture = scheduler.scheduleWithFixedDelay(this::pollGuntamatic, 1, config.refreshInterval,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        final ScheduledFuture<?> job = pollingFuture;
        if (job != null) {
            job.cancel(true);
            pollingFuture = null;
        }
        channelsInitialized = false;
    }
}
