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
package org.openhab.binding.solarforecast.internal.forecastsolar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Power;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.json.JSONException;
import org.json.JSONObject;
import org.openhab.binding.solarforecast.internal.actions.SolarForecast;
import org.openhab.binding.solarforecast.internal.utils.Utils;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TimeSeries.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ForecastSolarObject} holds complete data for forecast
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class ForecastSolarObject implements SolarForecast {
    private static final double UNDEF = -1;

    private final Logger logger = LoggerFactory.getLogger(ForecastSolarObject.class);
    private final TreeMap<ZonedDateTime, Double> wattHourMap = new TreeMap<ZonedDateTime, Double>();
    private final TreeMap<ZonedDateTime, Double> wattMap = new TreeMap<ZonedDateTime, Double>();
    private final DateTimeFormatter dateInputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ZoneId zone = ZoneId.systemDefault();
    private Optional<String> rawData = Optional.empty();
    private boolean valid = false;
    private Instant expirationDateTime;

    public ForecastSolarObject() {
        expirationDateTime = Instant.now();
    }

    public ForecastSolarObject(String content, Instant expirationDate) {
        expirationDateTime = expirationDate;
        if (!content.isEmpty()) {
            rawData = Optional.of(content);
            try {
                JSONObject contentJson = new JSONObject(content);
                JSONObject resultJson = contentJson.getJSONObject("result");
                JSONObject wattHourJson = resultJson.getJSONObject("watt_hours");
                JSONObject wattJson = resultJson.getJSONObject("watts");
                String zoneStr = contentJson.getJSONObject("message").getJSONObject("info").getString("timezone");
                zone = ZoneId.of(zoneStr);
                Iterator<String> iter = wattHourJson.keys();
                // put all values of the current day into sorted tree map
                while (iter.hasNext()) {
                    String dateStr = iter.next();
                    // convert date time into machine readable format
                    try {
                        ZonedDateTime zdt = LocalDateTime.parse(dateStr, dateInputFormatter).atZone(zone);
                        wattHourMap.put(zdt, wattHourJson.getDouble(dateStr));
                        wattMap.put(zdt, wattJson.getDouble(dateStr));
                    } catch (DateTimeParseException dtpe) {
                        logger.warn("Exception parsing time {} Reason: {}", dateStr, dtpe.getMessage());
                        return;
                    }
                }
                valid = true;
            } catch (JSONException je) {
                logger.debug("Error parsing JSON response {} - {}", content, je.getMessage());
            }
        }
    }

    public boolean isValid() {
        if (valid) {
            if (!wattHourMap.isEmpty()) {
                if (expirationDateTime.isAfter(Instant.now())) {
                    return true;
                }
            }
        }
        return false;
    }

    public double getActualEnergyValue(ZonedDateTime queryDateTime) {
        if (wattHourMap.isEmpty()) {
            return UNDEF;
        }
        Entry<ZonedDateTime, Double> f = wattHourMap.floorEntry(queryDateTime);
        Entry<ZonedDateTime, Double> c = wattHourMap.ceilingEntry(queryDateTime);
        if (f != null && c == null) {
            // only floor available
            if (f.getKey().toLocalDate().equals(queryDateTime.toLocalDate())) {
                // floor has valid date
                return f.getValue() / 1000.0;
            } else {
                // floor date doesn't fit
                return UNDEF;
            }
        } else if (f == null && c != null) {
            if (c.getKey().toLocalDate().equals(queryDateTime.toLocalDate())) {
                // only ceiling from correct date available - no valid data reached yet
                return 0;
            } else {
                // ceiling date doesn't fit
                return UNDEF;
            }
        } else if (f != null && c != null) {
            // ceiling and floor available
            if (f.getKey().toLocalDate().equals(queryDateTime.toLocalDate())) {
                if (c.getKey().toLocalDate().equals(queryDateTime.toLocalDate())) {
                    // we're during suntime!
                    double production = c.getValue() - f.getValue();
                    int interpolation = queryDateTime.getMinute() - f.getKey().getMinute();
                    double interpolationProduction = production * interpolation / 60;
                    double actualProduction = f.getValue() + interpolationProduction;
                    return actualProduction / 1000.0;
                } else {
                    // ceiling from wrong date, but floor is valid
                    return f.getValue() / 1000.0;
                }
            } else {
                // floor invalid - ceiling not reached
                return 0;
            }
        } // else both null - date time doesn't fit to forecast data
        return UNDEF;
    }

    public TimeSeries getEnergyTimeSeries() {
        TimeSeries ts = new TimeSeries(Policy.REPLACE);
        wattHourMap.forEach((timestamp, energy) -> {
            ts.add(timestamp.toInstant(), Utils.getEnergyState(energy / 1000.0));
        });
        return ts;
    }

    public double getActualPowerValue(ZonedDateTime queryDateTime) {
        if (wattMap.isEmpty()) {
            return UNDEF;
        }
        double actualPowerValue = 0;
        Entry<ZonedDateTime, Double> f = wattMap.floorEntry(queryDateTime);
        Entry<ZonedDateTime, Double> c = wattMap.ceilingEntry(queryDateTime);
        if (f != null && c == null) {
            // only floor available
            if (f.getKey().toLocalDate().equals(queryDateTime.toLocalDate())) {
                // floor has valid date
                return f.getValue() / 1000.0;
            } else {
                // floor date doesn't fit
                return UNDEF;
            }
        } else if (f == null && c != null) {
            if (c.getKey().toLocalDate().equals(queryDateTime.toLocalDate())) {
                // only ceiling from correct date available - no valid data reached yet
                return 0;
            } else {
                // ceiling date doesn't fit
                return UNDEF;
            }
        } else if (f != null && c != null) {
            // we're during suntime!
            double powerCeiling = c.getValue();
            double powerFloor = f.getValue();
            // calculate in minutes from floor to now, e.g. 20 minutes
            // => take 2/3 of floor and 1/3 of ceiling
            double interpolation = (queryDateTime.getMinute() - f.getKey().getMinute()) / 60.0;
            actualPowerValue = ((1 - interpolation) * powerFloor) + (interpolation * powerCeiling);
            return actualPowerValue / 1000.0;
        } // else both null - this shall not happen
        return UNDEF;
    }

    public TimeSeries getPowerTimeSeries() {
        TimeSeries ts = new TimeSeries(Policy.REPLACE);
        wattMap.forEach((timestamp, power) -> {
            ts.add(timestamp.toInstant(), Utils.getPowerState(power / 1000.0));
        });
        return ts;
    }

    public double getDayTotal(LocalDate queryDate) {
        if (rawData.isEmpty()) {
            return UNDEF;
        }
        JSONObject contentJson = new JSONObject(rawData.get());
        JSONObject resultJson = contentJson.getJSONObject("result");
        JSONObject wattsDay = resultJson.getJSONObject("watt_hours_day");

        if (wattsDay.has(queryDate.toString())) {
            return wattsDay.getDouble(queryDate.toString()) / 1000.0;
        }
        return UNDEF;
    }

    public double getRemainingProduction(ZonedDateTime queryDateTime) {
        if (wattHourMap.isEmpty()) {
            return UNDEF;
        }
        double daily = getDayTotal(queryDateTime.toLocalDate());
        double actual = getActualEnergyValue(queryDateTime);
        if (daily < 0 || actual < 0) {
            return UNDEF;
        }
        return daily - actual;
    }

    public ZoneId getZone() {
        return zone;
    }

    @Override
    public String toString() {
        return "Expiration: " + expirationDateTime + ", Valid: " + valid + ", Data:" + wattHourMap;
    }

    /**
     * SolarForecast Interface
     */
    @Override
    public QuantityType<Energy> getDay(LocalDate localDate, String... args) {
        if (args.length > 0) {
            logger.info("ForecastSolar doesn't accept arguments");
            return Utils.getEnergyState(-1);
        }
        double measure = getDayTotal(localDate);
        return Utils.getEnergyState(measure);
    }

    @Override
    public QuantityType<Energy> getEnergy(Instant start, Instant end, String... args) {
        if (args.length > 0) {
            logger.info("ForecastSolar doesn't accept arguments");
            return Utils.getEnergyState(-1);
        }
        LocalDate beginDate = start.atZone(zone).toLocalDate();
        LocalDate endDate = end.atZone(zone).toLocalDate();
        double measure = UNDEF;
        if (beginDate.equals(endDate)) {
            measure = getDayTotal(beginDate) - getActualEnergyValue(start.atZone(zone))
                    - getRemainingProduction(end.atZone(zone));
        } else {
            measure = getRemainingProduction(start.atZone(zone));
            beginDate = beginDate.plusDays(1);
            while (beginDate.isBefore(endDate) && measure >= 0) {
                double day = getDayTotal(beginDate);
                if (day > 0) {
                    measure += day;
                }
                beginDate = beginDate.plusDays(1);
            }
            double lastDay = getActualEnergyValue(end.atZone(zone));
            if (lastDay >= 0) {
                measure += lastDay;
            }
        }
        return Utils.getEnergyState(measure);
    }

    @Override
    public QuantityType<Power> getPower(Instant timestamp, String... args) {
        if (args.length > 0) {
            logger.info("ForecastSolar doesn't accept arguments");
            return Utils.getPowerState(-1);
        }
        double measure = getActualPowerValue(timestamp.atZone(zone));
        return Utils.getPowerState(measure);
    }

    @Override
    public Instant getForecastBegin() {
        if (!wattHourMap.isEmpty()) {
            ZonedDateTime zdt = wattHourMap.firstEntry().getKey();
            return zdt.toInstant();
        }
        return Instant.MAX;
    }

    @Override
    public Instant getForecastEnd() {
        if (!wattHourMap.isEmpty()) {
            ZonedDateTime zdt = wattHourMap.lastEntry().getKey();
            return zdt.toInstant();
        }
        return Instant.MIN;
    }
}
