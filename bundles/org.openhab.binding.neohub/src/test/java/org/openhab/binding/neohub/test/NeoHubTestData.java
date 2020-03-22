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
package org.openhab.binding.neohub.test;

/**
 * The {@link NeoHubTestData} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
public class NeoHubTestData {

    public static final String NEOHUB_JSON_TEST_STRING = 
        "{\"devices\":[" + 

            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":23,\"CURRENT_SET_TEMPERATURE\":\"22.0\",\"CURRENT_TEMPERATURE\":\"22.2\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":21,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"22.0\",\"MIN_TEMPERATURE\":\"21.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"6 days 23:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"2:42\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AND_FLOOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":0,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Dining Room\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":127,\"CURRENT_SET_TEMPERATURE\":\"22.0\",\"CURRENT_TEMPERATURE\":\"22.6\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":23,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"24.0\",\"MIN_TEMPERATURE\":\"23.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"6 days 23:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"255:255\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"REMOTE_AIR_SENSOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":4,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Shower Room\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":26,\"CURRENT_SET_TEMPERATURE\":\"22.0\",\"CURRENT_TEMPERATURE\":\"23.3\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":16,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"25.0\",\"MIN_TEMPERATURE\":\"20.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"6 days 23:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"3:48\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AND_FLOOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":0,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Conservatory\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":28,\"CURRENT_SET_TEMPERATURE\":\"22.0\",\"CURRENT_TEMPERATURE\":\"22.6\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":21,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"24.0\",\"MIN_TEMPERATURE\":\"19.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"6 days 23:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"3:16\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AND_FLOOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":2,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Living Room\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":23,\"CURRENT_SET_TEMPERATURE\":\"22.0\",\"CURRENT_TEMPERATURE\":\"23.6\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":21,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"23.0\",\"MIN_TEMPERATURE\":\"20.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"6 days 23:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"2:08\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AND_FLOOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":6,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Kitchen\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":17,\"CURRENT_SET_TEMPERATURE\":\"20.0\",\"CURRENT_TEMPERATURE\":\"20.1\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":21,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"21.0\",\"MIN_TEMPERATURE\":\"20.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"7 days 00:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"255:255\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AND_FLOOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":0,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Hallway\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":23,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":127,\"CURRENT_SET_TEMPERATURE\":\"10.0\",\"CURRENT_TEMPERATURE\":\"17.3\",\"DEMAND\":false,\"DEVICE_TYPE\":12,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":10,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"19.0\",\"MIN_TEMPERATURE\":\"16.0\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"7 days 08:00\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":5,\"PREHEAT\":false,\"PREHEAT_TIME\":\"255:255\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AIR_SENSOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"THERMOSTAT\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":20,\"WRITE_COUNT\":0,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Shed\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":0,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":127,\"CURRENT_SET_TEMPERATURE\":\"0.0\",\"CURRENT_TEMPERATURE\":\"255.255\",\"DEMAND\":false,\"DEVICE_TYPE\":6,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":1,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"255.255\",\"MIN_TEMPERATURE\":\"255.255\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"255 days 255:255\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"255:255\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AIR_SENSOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_ON\":true,\"TIMECLOCK\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":79,\"WRITE_COUNT\":11,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Plug South\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":0,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":127,\"CURRENT_SET_TEMPERATURE\":\"0.0\",\"CURRENT_TEMPERATURE\":\"255.255\",\"DEMAND\":false,\"DEVICE_TYPE\":6,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":0,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"255.255\",\"MIN_TEMPERATURE\":\"255.255\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"255 days 255:255\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"255:255\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AIR_SENSOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_ON\":true,\"TIMECLOCK\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":true,\"TIME_CLOCK_OVERIDE_BIT\":true,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":79,\"WRITE_COUNT\":7,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Plug North\"}," + 
            "{\"AWAY\":false,\"COOLING\":false,\"COOLING_ENABLED\":false,\"COOLING_TEMPERATURE_IN_WHOLE_DEGREES\":0,\"COOL_INP\":false,\"COUNT_DOWN_TIME\":\"0:00\",\"CRADLE_PAIRED_TO_REMOTE_SENSOR\":false,\"CRADLE_PAIRED_TO_STAT\":false,\"CURRENT_FLOOR_TEMPERATURE\":127,\"CURRENT_SET_TEMPERATURE\":\"0.0\",\"CURRENT_TEMPERATURE\":\"255.255\",\"DEMAND\":false,\"DEVICE_TYPE\":6,\"ENABLE_BOILER\":false,\"ENABLE_COOLING\":false,\"ENABLE_PUMP\":false,\"ENABLE_VALVE\":false,\"ENABLE_ZONE\":false,\"FAILSAFE_STATE\":false,\"FAIL_SAFE_ENABLED\":false,\"FLOOR_LIMIT\":false,\"FULL/PARTIAL_LOCK_AVAILABLE\":false,\"HEAT/COOL_MODE\":false,\"HEATING\":false,\"HOLD_TEMPERATURE\":1,\"HOLD_TIME\":\"0:00\",\"HOLIDAY\":false,\"HOLIDAY_DAYS\":0,\"HUMIDITY\":0,\"LOCK\":false,\"LOCK_PIN_NUMBER\":\"0000\",\"LOW_BATTERY\":false,\"MAX_TEMPERATURE\":\"255.255\",\"MIN_TEMPERATURE\":\"255.255\",\"MODULATION_LEVEL\":0,\"NEXT_ON_TIME\":\"255 days 255:255\",\"OFFLINE\":false,\"OUPUT_DELAY\":false,\"OUTPUT_DELAY\":0,\"PREHEAT\":false,\"PREHEAT_TIME\":\"255:255\",\"PROGRAM_MODE\":\"24HOURSFIXED\",\"PUMP_DELAY\":false,\"RADIATORS_OR_UNDERFLOOR\":false,\"SENSOR_SELECTION\":\"BUILT_IN_AIR_SENSOR\",\"SET_COUNTDOWN_TIME\":0,\"STANDBY\":false,\"STAT_MODE\":{\"4_HEAT_LEVELS\":true,\"MANUAL_OFF\":true,\"TIMECLOCK\":true},\"TEMPERATURE_FORMAT\":false,\"TEMP_HOLD\":false,\"TIMECLOCK_MODE\":false,\"TIMER\":false,\"TIME_CLOCK_OVERIDE_BIT\":false,\"ULTRA_VERSION\":0,\"VERSION_NUMBER\":79,\"WRITE_COUNT\":28,\"ZONE_1PAIRED_TO_MULTILINK\":true,\"ZONE_1_OR_2\":false,\"ZONE_2_PAIRED_TO_MULTILINK\":false,\"device\":\"Watering System\"}" +
            
        "]}";
}
