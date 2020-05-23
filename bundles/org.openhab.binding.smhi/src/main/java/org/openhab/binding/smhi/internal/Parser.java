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

package org.openhab.binding.smhi.internal;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class with static methods for parsing json strings returned from Smhi
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class Parser {

    private static JsonParser parser = new JsonParser();

    /**
     * Parse a json string received from Smhi containing forecasts.
     *
     * @param json A json string
     * @return A {@link TimeSeries} object
     */
    public static TimeSeries parseTimeSeries(String json) {
        ZonedDateTime referenceTime;
        JsonObject object = parser.parse(json).getAsJsonObject();

        referenceTime = parseApprovedTime(json);
        JsonArray timeSeries = object.get("timeSeries").getAsJsonArray();

        List<Forecast> forecasts = StreamSupport.stream(timeSeries.spliterator(), false)
                        .map(element -> parseForecast(element.getAsJsonObject()))
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList());

        return new TimeSeries(referenceTime, forecasts);
    }

    /**
     * Parse a json string containing the approved time and reference time of the latest forecast
     *
     * @param json A json string
     * @return {@link ZonedDateTime} of the reference time
     */
    public static ZonedDateTime parseApprovedTime(String json) {
        JsonObject timeObj = parser.parse(json).getAsJsonObject();

        return ZonedDateTime.parse(timeObj.get("referenceTime").getAsString());
    }

    /**
     * Parse a single forecast, i.e. a forecast for a specific time.
     *
     * @param object
     * @return
     */
    private static Forecast parseForecast(JsonObject object) {
        ZonedDateTime validTime = ZonedDateTime.parse(object.get("validTime").getAsString());
        Map<String, @Nullable BigDecimal> parameters = new HashMap<>();

        JsonArray par = object.get("parameters").getAsJsonArray();

        par.forEach(element -> {
            JsonObject parObj = element.getAsJsonObject();
            String name = parObj.get("name").getAsString().toLowerCase(Locale.ROOT);
            BigDecimal value = parObj.get("values").getAsJsonArray().get(0).getAsBigDecimal();

            parameters.put(name, value);
        });

        return new Forecast(validTime, parameters);
    }
}
