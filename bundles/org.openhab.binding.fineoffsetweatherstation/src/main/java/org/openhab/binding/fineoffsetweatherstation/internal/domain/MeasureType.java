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
package org.openhab.binding.fineoffsetweatherstation.internal.domain;

import static javax.measure.MetricPrefix.HECTO;
import static javax.measure.MetricPrefix.KILO;
import static javax.measure.MetricPrefix.MILLI;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_HUMIDITY;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_ILLUMINATION;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_PM25;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_PRESSURE;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_RAIN;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_RAIN_RATE;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_TEMPERATURE;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_UV_RADIATION;
import static org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetWeatherStationBindingConstants.CHANNEL_WIND_SPEED;
import static org.openhab.binding.fineoffsetweatherstation.internal.Utils.toInt16;
import static org.openhab.binding.fineoffsetweatherstation.internal.Utils.toUInt16;
import static org.openhab.binding.fineoffsetweatherstation.internal.Utils.toUInt32;
import static org.openhab.binding.fineoffsetweatherstation.internal.Utils.toUInt8;
import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.core.library.unit.SIUnits.METRE;
import static org.openhab.core.library.unit.SIUnits.PASCAL;
import static org.openhab.core.library.unit.Units.DEGREE_ANGLE;
import static org.openhab.core.library.unit.Units.METRE_PER_SECOND;
import static org.openhab.core.library.unit.Units.MICROGRAM_PER_CUBICMETRE;
import static org.openhab.core.library.unit.Units.MILLIMETRE_PER_HOUR;
import static org.openhab.core.library.unit.Units.PERCENT;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.BiFunction;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.fineoffsetweatherstation.internal.Utils;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.State;

/**
 * Represents the measured type with conversion form the sensors' bytes to the openHAB state.
 *
 * @author Andreas Berger - Initial contribution
 */
@NonNullByDefault
public enum MeasureType {

    TEMPERATURE(CELSIUS, 2, CHANNEL_TEMPERATURE, (data, offset) -> toInt16(data, offset) / 10.),

    PERCENTAGE(PERCENT, 1, CHANNEL_HUMIDITY, (data, offset) -> toUInt8(data[offset])),

    PRESSURE(HECTO(PASCAL), 2, CHANNEL_PRESSURE, Utils::toUInt16),

    DEGREE(DEGREE_ANGLE, 2, null, Utils::toUInt16),

    SPEED(METRE_PER_SECOND, 2, CHANNEL_WIND_SPEED, (data, offset) -> toUInt16(data, offset) / 10.),

    HEIGHT(MILLI(METRE), 2, CHANNEL_RAIN, (data, offset) -> toUInt16(data, offset) / 10.),

    HEIGHT_BIG(MILLI(METRE), 4, CHANNEL_RAIN, (data, offset) -> toUInt32(data, offset) / 10.),

    HEIGHT_PER_HOUR(MILLIMETRE_PER_HOUR, 2, CHANNEL_RAIN_RATE, (data, offset) -> toUInt16(data, offset) / 10.),

    LUX(Units.LUX, 4, CHANNEL_ILLUMINATION, (data, offset) -> toUInt32(data, offset) / 10.),

    PM25(MICROGRAM_PER_CUBICMETRE, 2, CHANNEL_PM25, (data, offset) -> toUInt16(data, offset) / 10.),

    BOOLEAN(1, null, (data, offset) -> toUInt8(data[offset]) != 0 ? OnOffType.ON : OnOffType.OFF),

    KILOMETER(KILO(METRE), 1, null, (data, offset) -> toUInt8(data[offset])),

    INT(4, null, (data, offset) -> new DecimalType(toUInt32(data, offset))),

    MICROWATT_PER_SQUARE_CENTIMETRE(Units.MICROWATT_PER_SQUARE_CENTIMETRE, 2, CHANNEL_UV_RADIATION, Utils::toUInt16),

    BYTE(1, null, (data, offset) -> new DecimalType(toUInt8(data[offset]))),

    DATE_TIME(4, null,
            (data, offset) -> new DateTimeType(
                    ZonedDateTime.ofInstant(Instant.ofEpochSecond(toUInt32(data, offset)), ZoneOffset.UTC))),

    DATE_TIME2(6, null, (data, offset) -> new DateTimeType(
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(toUInt32(data, offset)), ZoneOffset.UTC)));

    private final int byteSize;
    private final @Nullable ChannelTypeUID channelTypeUID;
    private final BiFunction<byte[], Integer, @Nullable State> stateConverter;

    /**
     * @param unit the unit
     * @param byteSize the size in the sensors' payload
     * @param channelTypeUID the channel type
     * @param valueExtractor a function to extract the sensor data into a number of the dimension defined by the unit
     */
    MeasureType(Unit<?> unit, int byteSize, @Nullable ChannelTypeUID channelTypeUID,
            BiFunction<byte[], Integer, @Nullable Number> valueExtractor) {
        this(byteSize, channelTypeUID, (bytes, integer) -> {
            Number value = valueExtractor.apply(bytes, integer);
            return value == null ? null : new QuantityType<>(value, unit);
        });
    }

    /**
     * @param byteSize the size in the sensors' payload
     * @param channelTypeUID the channel type
     * @param stateConverter a function to extract the sensor data into the openHAB's state
     */
    MeasureType(int byteSize, @Nullable ChannelTypeUID channelTypeUID,
            BiFunction<byte[], Integer, @Nullable State> stateConverter) {
        this.byteSize = byteSize;
        this.channelTypeUID = channelTypeUID;
        this.stateConverter = stateConverter;
    }

    public int getByteSize() {
        return byteSize;
    }

    public @Nullable ChannelTypeUID getChannelTypeId() {
        return channelTypeUID;
    }

    public @Nullable State toState(byte[] data, int offset) {
        return stateConverter.apply(data, offset);
    }
}
