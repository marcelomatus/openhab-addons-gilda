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
package org.openhab.binding.modbus.stiebeleltron.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.modbus.ModbusBindingConstants;

/**
 * The {@link Modbus.StiebelEltronBindingConstants} class defines common
 * constants, which are used across the whole binding.
 *
 * @author Paul Frank - Initial contribution
 */
@NonNullByDefault
public class StiebelEltronBindingConstants {

    private static final String BINDING_ID = ModbusBindingConstants.BINDING_ID;

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "heatpump");


    // Channel group ids
    public static final String GROUP_SYSTEM_STATE = "systemState";
    public static final String GROUP_SYSTEM_INFO = "systemInformation";
    public static final String GROUP_ENERGY_INFO = "energyInformation";

    // List of all Channel ids in device information group
    public static final String CHANNEL_FEK_TEMPERATURE = "fek-temperature";
    public static final String CHANNEL_FEK_TEMPERATURE_SETPOINT = "fek-temperature-setpoint";
    public static final String CHANNEL_FEK_HUMIDITY = "fek-humidity";
    public static final String CHANNEL_FEK_DEWPOINT = "fek-dewpoint";
    public static final String CHANNEL_OUTDOOR_TEMPERATURE = "outdoor-temperature";
    public static final String CHANNEL_HK1_TEMPERATURE = "hk1-temperature";
    public static final String CHANNEL_HK1_TEMPERATURE_SETPOINT = "hk1-temperature-setpoint";
    public static final String CHANNEL_SUPPLY_TEMPERATURE = "supply-temperature";
    public static final String CHANNEL_RETURN_TEMPERATURE = "return-temperature";
    public static final String CHANNEL_SOURCE_TEMPERATURE = "source-temperature";
    public static final String CHANNEL_WATER_TEMPERATURE = "water-temperature";
    public static final String CHANNEL_WATER_TEMPERATURE_SETPOINT = "water-temperature-setpoint";

    public static final String CHANNEL_PRODUCTION_HEAT_TODAY = "production_heat_today";
    public static final String CHANNEL_PRODUCTION_HEAT_TOTAL = "production_heat_total";
    public static final String CHANNEL_PRODUCTION_WATER_TODAY = "production_water_today";
    public static final String CHANNEL_PRODUCTION_WATER_TOTAL = "production_water_total";
    public static final String CHANNEL_CONSUMPTION_HEAT_TODAY = "consumption_heat_today";
    public static final String CHANNEL_CONSUMPTION_HEAT_TOTAL = "consumption_heat_total";
    public static final String CHANNEL_CONSUMPTION_WATER_TODAY = "consumption_water_today";
    public static final String CHANNEL_CONSUMPTION_WATER_TOTAL = "consumption_water_total";

    public static final String CHANNEL_IS_HEATING = "is-heating";
    public static final String CHANNEL_IS_HEATING_WATER = "is-heating-water";
    public static final String CHANNEL_IS_COOLING = "is-cooling";
    public static final String CHANNEL_IS_PUMPING = "is-pumping";
    public static final String CHANNEL_IS_SUMMER = "is-summer";

}
