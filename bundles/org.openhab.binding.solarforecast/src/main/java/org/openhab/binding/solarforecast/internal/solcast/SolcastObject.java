/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.solarforecast.internal.solcast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Power;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openhab.binding.solarforecast.internal.actions.SolarForecast;
import org.openhab.binding.solarforecast.internal.utils.Utils;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TimeSeries.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SolcastObject} holds complete data for forecast
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class SolcastObject implements SolarForecast {
    private static final double UNDEF = -1;
    private static final TreeMap<ZonedDateTime, Double> EMPTY_MAP = new TreeMap<ZonedDateTime, Double>();

    private final Logger logger = LoggerFactory.getLogger(SolcastObject.class);
    private final TreeMap<ZonedDateTime, Double> estimationDataMap = new TreeMap<ZonedDateTime, Double>();
    private final TreeMap<ZonedDateTime, Double> optimisticDataMap = new TreeMap<ZonedDateTime, Double>();
    private final TreeMap<ZonedDateTime, Double> pessimisticDataMap = new TreeMap<ZonedDateTime, Double>();
    private final TimeZoneProvider timeZoneProvider;

    private Optional<JSONObject> rawData = Optional.of(new JSONObject());
    private Instant expirationDateTime;
    private boolean valid = false;

    public enum QueryMode {
        Estimation("Estimation"),
        Optimistic(SolarForecast.OPTIMISTIC),
        Pessimistic(SolarForecast.PESSIMISTIC),
        Error("Error");

        String modeDescirption;

        QueryMode(String description) {
            modeDescirption = description;
        }

        @Override
        public String toString() {
            return modeDescirption;
        }
    }

    public SolcastObject(TimeZoneProvider tzp) {
        // invalid forecast object
        timeZoneProvider = tzp;
        expirationDateTime = Instant.now();
    }

    public SolcastObject(String content, Instant expiration, TimeZoneProvider tzp) {
        expirationDateTime = expiration;
        timeZoneProvider = tzp;
        add(content);
    }

    public void join(String content) {
        add(content);
    }

    private void add(String content) {
        if (!content.isEmpty()) {
            valid = true;
            JSONObject contentJson = new JSONObject(content);
            JSONArray resultJsonArray;

            // prepare data for raw channel
            if (contentJson.has("forecasts")) {
                resultJsonArray = contentJson.getJSONArray("forecasts");
                addJSONArray(resultJsonArray);
                rawData.get().put("forecasts", resultJsonArray);
            }
            if (contentJson.has("estimated_actuals")) {
                resultJsonArray = contentJson.getJSONArray("estimated_actuals");
                addJSONArray(resultJsonArray);
                rawData.get().put("estimated_actuals", resultJsonArray);
            }
        }
    }

    private void addJSONArray(JSONArray resultJsonArray) {
        // sort data into TreeMaps
        for (int i = 0; i < resultJsonArray.length(); i++) {
            JSONObject jo = resultJsonArray.getJSONObject(i);
            String periodEnd = jo.getString("period_end");
            ZonedDateTime periadEndZdt = getZdtFromUTC(periodEnd);
            if (periadEndZdt == null) {
                return;
            }
            estimationDataMap.put(periadEndZdt, jo.getDouble("pv_estimate"));

            // fill pessimistic values
            if (jo.has("pv_estimate10")) {
                pessimisticDataMap.put(periadEndZdt, jo.getDouble("pv_estimate10"));
            } else {
                pessimisticDataMap.put(periadEndZdt, jo.getDouble("pv_estimate"));
            }

            // fill optimistic values
            if (jo.has("pv_estimate90")) {
                optimisticDataMap.put(periadEndZdt, jo.getDouble("pv_estimate90"));
            } else {
                optimisticDataMap.put(periadEndZdt, jo.getDouble("pv_estimate"));
            }
        }
    }

    public boolean isValid() {
        if (valid) {
            if (!estimationDataMap.isEmpty()) {
                if (expirationDateTime.isAfter(Instant.now())) {
                    return true;
                }
            }
        }
        return false;
    }

    public double getActualEnergyValue(ZonedDateTime query, QueryMode mode) {
        ZonedDateTime iterationDateTime = query.withHour(0).withMinute(0).withSecond(0);
        TreeMap<ZonedDateTime, Double> dtm = getDataMap(mode);
        Entry<ZonedDateTime, Double> nextEntry = dtm.higherEntry(iterationDateTime);
        if (nextEntry == null) {
            return UNDEF;
        }
        double forecastValue = 0;
        double previousEstimate = 0;
        while (nextEntry.getKey().isBefore(query) || nextEntry.getKey().isEqual(query)) {
            // value are reported in PT30M = 30 minutes interval with kw value
            // for kw/h it's half the value
            Double endValue = nextEntry.getValue();
            // production during period is half of previous and next value
            double addedValue = (endValue.doubleValue() + previousEstimate) / 2.0 / 2.0;
            forecastValue += addedValue;
            previousEstimate = endValue.doubleValue();
            iterationDateTime = nextEntry.getKey();
            nextEntry = dtm.higherEntry(iterationDateTime);
            if (nextEntry == null) {
                break;
            }
        }
        Entry<ZonedDateTime, Double> f = dtm.floorEntry(query);
        Entry<ZonedDateTime, Double> c = dtm.ceilingEntry(query);
        if (f != null) {
            if (c != null) {
                if (c.getValue() > 0) {
                    int interpolation = query.getMinute() - f.getKey().getMinute();
                    double interpolationProduction = getActualPowerValue(query, mode) * interpolation / 30.0 / 2.0;
                    forecastValue += interpolationProduction;
                    return forecastValue;
                } else {
                    // if ceiling value is 0 there's no further production in this period
                    return forecastValue;
                }
            } else {
                // if ceiling is null we're at the very end of the day
                return forecastValue;
            }
        } else {
            // if floor is null we're at the very beginning of the day => 0
            return 0;
        }
    }

    public TimeSeries getEnergyTimeSeries(QueryMode mode) {
        TreeMap<ZonedDateTime, Double> dtm = getDataMap(mode);
        TimeSeries ts = new TimeSeries(Policy.REPLACE);
        dtm.forEach((timestamp, energy) -> {
            ts.add(timestamp.toInstant(), Utils.getEnergyState(getActualEnergyValue(timestamp, mode)));
        });
        return ts;
    }

    /**
     * Get power values
     */
    public double getActualPowerValue(ZonedDateTime query, QueryMode mode) {
        if (query.toInstant().isBefore(getForecastBegin()) || query.toInstant().isAfter(getForecastEnd())) {
            return UNDEF;
        }
        TreeMap<ZonedDateTime, Double> dtm = getDataMap(mode);
        double actualPowerValue = 0;
        Entry<ZonedDateTime, Double> f = dtm.floorEntry(query);
        Entry<ZonedDateTime, Double> c = dtm.ceilingEntry(query);
        if (f != null) {
            if (c != null) {
                double powerCeiling = c.getValue();
                if (powerCeiling > 0) {
                    double powerFloor = f.getValue();
                    // calculate in minutes from floor to now, e.g. 20 minutes from PT30M 30 minutes
                    // => take 1/3 of floor and 2/3 of ceiling
                    double interpolation = (query.getMinute() - f.getKey().getMinute()) / 30.0;
                    actualPowerValue = ((1 - interpolation) * powerFloor) + (interpolation * powerCeiling);
                    return actualPowerValue;
                } else {
                    // if power ceiling == 0 there's no production in this period
                    return 0;
                }
            } else {
                // if ceiling is null we're at the very end of this day => 0
                return 0;
            }
        } else {
            // if floor is null we're at the very beginning of this day => 0
            return 0;
        }
    }

    public TimeSeries getPowerTimeSeries(QueryMode mode) {
        TreeMap<ZonedDateTime, Double> dtm = getDataMap(mode);
        TimeSeries ts = new TimeSeries(Policy.REPLACE);
        dtm.forEach((timestamp, power) -> {
            ts.add(timestamp.toInstant(), Utils.getPowerState(power));
        });
        return ts;
    }

    /**
     * Daily totals
     */
    public double getDayTotal(LocalDate query, QueryMode mode) {
        TreeMap<ZonedDateTime, Double> dtm = getDataMap(mode);
        ZonedDateTime iterationDateTime = query.atStartOfDay(timeZoneProvider.getTimeZone());
        Entry<ZonedDateTime, Double> nextEntry = dtm.higherEntry(iterationDateTime);
        if (nextEntry == null) {
            return UNDEF;
        }
        ZonedDateTime endDateTime = query.atTime(23, 59, 59).atZone(timeZoneProvider.getTimeZone());
        double forecastValue = 0;
        double previousEstimate = 0;
        while (nextEntry.getKey().isBefore(endDateTime) || nextEntry.getKey().isEqual(endDateTime)) {
            // value are reported in PT30M = 30 minutes interval with kw value
            // for kw/h it's half the value
            Double endValue = nextEntry.getValue();
            // production during period is half of previous and next value
            double addedValue = (endValue.doubleValue() + previousEstimate) / 2.0 / 2.0;
            forecastValue += addedValue;
            previousEstimate = endValue.doubleValue();
            iterationDateTime = nextEntry.getKey();
            nextEntry = dtm.higherEntry(iterationDateTime);
            if (nextEntry == null) {
                break;
            }
        }
        return forecastValue;
    }

    public double getRemainingProduction(ZonedDateTime query, QueryMode mode) {
        return getDayTotal(query.toLocalDate(), mode) - getActualEnergyValue(query, mode);
    }

    @Override
    public String toString() {
        return "Expiration: " + expirationDateTime + ", Valid: " + valid + ", Data:" + estimationDataMap;
    }

    public String getRaw() {
        return rawData.get().toString();
    }

    private TreeMap<ZonedDateTime, Double> getDataMap(QueryMode mode) {
        TreeMap<ZonedDateTime, Double> returnMap = EMPTY_MAP;
        switch (mode) {
            case Estimation:
                returnMap = estimationDataMap;
                break;
            case Optimistic:
                returnMap = optimisticDataMap;
                break;
            case Pessimistic:
                returnMap = pessimisticDataMap;
                break;
            case Error:
                // nothing to do
                break;
            default:
                // nothing to do
                break;
        }
        return returnMap;
    }

    public @Nullable ZonedDateTime getZdtFromUTC(String utc) {
        try {
            Instant timestamp = Instant.parse(utc);
            return timestamp.atZone(timeZoneProvider.getTimeZone());
        } catch (DateTimeParseException dtpe) {
            logger.warn("Exception parsing time {} Reason: {}", utc, dtpe.getMessage());
        }
        return null;
    }

    /**
     * SolarForecast Interface
     */
    @Override
    public QuantityType<Energy> getDay(LocalDate date, String... args) {
        QueryMode mode = evalArguments(args);
        if (mode.equals(QueryMode.Error)) {
            return Utils.getEnergyState(-1);
        } else if (mode.equals(QueryMode.Optimistic) || mode.equals(QueryMode.Pessimistic)) {
            if (date.isBefore(LocalDate.now())) {
                logger.info("{} forecasts only available for future", mode);
                return Utils.getEnergyState(-1);
            }
        }
        double measure = getDayTotal(date, mode);
        return Utils.getEnergyState(measure);
    }

    @Override
    public QuantityType<Energy> getEnergy(Instant start, Instant end, String... args) {
        if (end.isBefore(start)) {
            logger.info("End {} defined before Start {}", end, start);
            return Utils.getEnergyState(-1);
        }
        QueryMode mode = evalArguments(args);
        if (mode.equals(QueryMode.Error)) {
            return Utils.getEnergyState(-1);
        } else if (mode.equals(QueryMode.Optimistic) || mode.equals(QueryMode.Pessimistic)) {
            if (end.isBefore(Instant.now())) {
                logger.info("{} forecasts only available for future", mode);
                return Utils.getEnergyState(-1);
            }
        }
        LocalDate beginDate = start.atZone(timeZoneProvider.getTimeZone()).toLocalDate();
        LocalDate endDate = end.atZone(timeZoneProvider.getTimeZone()).toLocalDate();
        double measure = UNDEF;
        if (beginDate.isEqual(endDate)) {
            measure = getDayTotal(beginDate, mode)
                    - getActualEnergyValue(start.atZone(timeZoneProvider.getTimeZone()), mode)
                    - getRemainingProduction(end.atZone(timeZoneProvider.getTimeZone()), mode);
        } else {
            measure = getRemainingProduction(start.atZone(timeZoneProvider.getTimeZone()), mode);
            beginDate = beginDate.plusDays(1);
            while (beginDate.isBefore(endDate) && measure >= 0) {
                double day = getDayTotal(beginDate, mode);
                if (day > 0) {
                    measure += day;
                }
                beginDate = beginDate.plusDays(1);
            }
            double lastDay = getActualEnergyValue(end.atZone(timeZoneProvider.getTimeZone()), mode);
            if (lastDay >= 0) {
                measure += lastDay;
            }
        }
        return Utils.getEnergyState(measure);
    }

    @Override
    public QuantityType<Power> getPower(Instant timestamp, String... args) {
        // eliminate error cases and return immediately
        QueryMode mode = evalArguments(args);
        if (mode.equals(QueryMode.Error)) {
            return Utils.getPowerState(-1);
        } else if (mode.equals(QueryMode.Optimistic) || mode.equals(QueryMode.Pessimistic)) {
            if (timestamp.isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))) {
                logger.info("{} forecasts only available for future", mode);
                return Utils.getPowerState(-1);
            }
        }
        double measure = getActualPowerValue(ZonedDateTime.ofInstant(timestamp, timeZoneProvider.getTimeZone()), mode);
        return Utils.getPowerState(measure);
    }

    @Override
    public Instant getForecastBegin() {
        if (!estimationDataMap.isEmpty()) {
            return estimationDataMap.firstEntry().getKey().toInstant();
        }
        return Instant.MAX;
    }

    @Override
    public Instant getForecastEnd() {
        if (!estimationDataMap.isEmpty()) {
            return estimationDataMap.lastEntry().getKey().toInstant();
        }
        return Instant.MIN;
    }

    private QueryMode evalArguments(String[] args) {
        if (args.length > 0) {
            if (args.length > 1) {
                logger.info("Too many arguments {}", Arrays.toString(args));
                return QueryMode.Error;
            }

            if (SolarForecast.OPTIMISTIC.equals(args[0])) {
                return QueryMode.Optimistic;
            } else if (SolarForecast.PESSIMISTIC.equals(args[0])) {
                return QueryMode.Pessimistic;
            } else {
                logger.info("Argument {} not supported", args[0]);
                return QueryMode.Error;
            }
        } else {
            return QueryMode.Estimation;
        }
    }
}
