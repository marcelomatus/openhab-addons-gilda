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
package org.openhab.binding.fineoffsetweatherstation.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link FineOffsetWeatherStationBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Andreas Berger - Initial contribution
 */
@NonNullByDefault
public class FineOffsetWeatherStationBindingConstants {

    public static final String BINDING_ID = "fineoffsetweatherstation";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_GATEWAY = new ThingTypeUID(BINDING_ID, "gateway");
    public static final ThingTypeUID THING_TYPE_SENSOR = new ThingTypeUID(BINDING_ID, "sensor");

    public static final ChannelTypeUID CHANNEL_TEMPERATURE = new ChannelTypeUID(BINDING_ID, "temperature");
    public static final ChannelTypeUID CHANNEL_HUMIDITY = new ChannelTypeUID(BINDING_ID, "humidity");
    public static final ChannelTypeUID CHANNEL_WIND_SPEED = new ChannelTypeUID(BINDING_ID, "wind-speed");
    public static final ChannelTypeUID CHANNEL_RAIN = new ChannelTypeUID(BINDING_ID, "rain");
    public static final ChannelTypeUID CHANNEL_PRESSURE = new ChannelTypeUID(BINDING_ID, "pressure");
    public static final ChannelTypeUID CHANNEL_ILLUMINATION = new ChannelTypeUID(BINDING_ID, "illumination");
    public static final ChannelTypeUID CHANNEL_UV_INDEX = new ChannelTypeUID(BINDING_ID, "uv-index");
    public static final ChannelTypeUID CHANNEL_UV_RADIATION = new ChannelTypeUID(BINDING_ID, "uv-radiation");
    public static final ChannelTypeUID CHANNEL_RAIN_RATE = new ChannelTypeUID(BINDING_ID, "rain-rate");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_GATEWAY, THING_TYPE_SENSOR);

    public static final String SENSOR_CHANNEL_SIGNAL = "signal";
    public static final String SENSOR_CHANNEL_BATTERY_LEVEL = "batteryLevel";
    public static final String SENSOR_CHANNEL_LOW_BATTERY = "lowBattery";
}
