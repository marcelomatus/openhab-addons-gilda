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
package org.openhab.binding.shelly.internal.coap;

import static org.openhab.binding.shelly.internal.ShellyBindingConstants.*;
import static org.openhab.binding.shelly.internal.api.ShellyApiJsonDTO.*;
import static org.openhab.binding.shelly.internal.util.ShellyUtils.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.shelly.internal.coap.ShellyCoapJSonDTO.CoIotDescrBlk;
import org.openhab.binding.shelly.internal.coap.ShellyCoapJSonDTO.CoIotDescrSen;
import org.openhab.binding.shelly.internal.coap.ShellyCoapJSonDTO.CoIotSensor;
import org.openhab.binding.shelly.internal.handler.ShellyBaseHandler;
import org.openhab.binding.shelly.internal.handler.ShellyColorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tec.uom.se.unit.Units;

/**
 * The {@link ShellyCoIoTVersion1} implements the parsing for CoIoT version 1
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class ShellyCoIoTVersion1 extends ShellyCoIoTProtocol implements ShellyCoIoTInterface {
    private final Logger logger = LoggerFactory.getLogger(ShellyCoIoTVersion1.class);

    public ShellyCoIoTVersion1(String thingName, ShellyBaseHandler thingHandler, Map<String, CoIotDescrBlk> blkMap,
            Map<String, CoIotDescrSen> sensorMap) {
        super(thingName, thingHandler, blkMap, sensorMap);
    }

    @Override
    public int getVersion() {
        return ShellyCoapJSonDTO.COIOT_VERSION_1;
    }

    /**
     * Process CoIoT status update message. If a status update is received, but the device description has not been
     * received yet a GET is send to query device description.
     *
     * @param devId device id included in the status packet
     * @param payload CoAP payload (Json format), example: {"G":[[0,112,0]]}
     * @param serial Serial for this request. If this the the same as last serial
     *            the update was already sent and processed so this one gets
     *            ignored.
     */
    @Override
    public boolean handleStatusUpdate(List<CoIotSensor> sensorUpdates, CoIotDescrSen sen, CoIotSensor s,
            Map<String, State> updates) {

        // first check the base implementation
        if (super.handleStatusUpdate(sensorUpdates, sen, s, updates)) {
            // process by the base class
            return true;
        }

        // Process status information and convert into channel updates
        Integer rIndex = Integer.parseInt(sen.links) + 1;
        String rGroup = getProfile().numRelays <= 1 ? CHANNEL_GROUP_RELAY_CONTROL
                : CHANNEL_GROUP_RELAY_CONTROL + rIndex;
        switch (sen.type.toLowerCase()) {
            case "t": // Temperature +
                Double value = getDouble(s.value);
                switch (sen.desc.toLowerCase()) {
                    case "temperature": // Sensor Temp
                        if (getString(getProfile().settings.temperatureUnits)
                                .equalsIgnoreCase(SHELLY_TEMP_FAHRENHEIT)) {
                            value = ImperialUnits.FAHRENHEIT.getConverterTo(Units.CELSIUS).convert(getDouble(s.value))
                                    .doubleValue();
                        }
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_TEMP,
                                toQuantityType(value, DIGITS_TEMP, SIUnits.CELSIUS));
                        break;
                    case "temperature f": // Device Temp -> ignore (we use C only)
                        break;
                    case "temperature c": // Device Temü in C ++
                        // Device temperature
                        updateChannel(updates, CHANNEL_GROUP_DEV_STATUS, CHANNEL_DEVST_ITEMP,
                                toQuantityType(value, DIGITS_NONE, SIUnits.CELSIUS));
                        break;
                    case "external temperature f": // Shelly 1/1PM external temp sensors
                        // ignore F, we use C only
                        break;
                    case "external temperature c": // Shelly 1/1PM external temp sensors
                    case "external_temperature":
                        int idx = getExtTempId(sen.id);
                        if (idx > 0) {
                            updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_TEMP + idx,
                                    toQuantityType(value, DIGITS_TEMP, SIUnits.CELSIUS));
                        } else {
                            logger.debug("{}: Unable to get extSensorId {} from {}/{}", thingName, sen.id, sen.type,
                                    sen.desc);
                        }
                        break;
                    default:
                        logger.debug("{}: Unknown temperatur type: {}", thingName, sen.desc);
                }
                break;
            case "p": // Power/Watt
                // 3EM uses 1-based meter IDs, other 0-based
                String mGroup = profile.numMeters == 1 ? CHANNEL_GROUP_METER
                        : CHANNEL_GROUP_METER + (profile.isEMeter ? sen.links : rIndex);
                updateChannel(updates, mGroup, CHANNEL_METER_CURRENTWATTS,
                        toQuantityType(s.value, DIGITS_WATT, SmartHomeUnits.WATT));
                updateChannel(updates, mGroup, CHANNEL_LAST_UPDATE, getTimestamp());
                break;
            case "s" /* CatchAll */:
                switch (sen.desc.toLowerCase()) {
                    case "overtemp":
                        if (s.value == 1) {
                            thingHandler.postEvent(ALARM_TYPE_OVERTEMP, true);
                        }
                        break;
                    case "energy counter 0 [w-min]":
                        updateChannel(updates, rGroup, CHANNEL_METER_LASTMIN1,
                                toQuantityType(s.value, DIGITS_WATT, SmartHomeUnits.WATT));
                        break;
                    case "energy counter 1 [w-min]":
                    case "energy counter 2 [w-min]":
                        // we don't use them
                        break;
                    case "energy counter total [w-h]": // 3EM reports W/h
                    case "energy counter total [w-min]":
                        Double total = profile.isEMeter ? s.value / 1000 : s.value / 60 / 1000;
                        updateChannel(updates, rGroup, CHANNEL_METER_TOTALKWH,
                                toQuantityType(total, DIGITS_KWH, SmartHomeUnits.KILOWATT_HOUR));
                        break;
                    case "voltage":
                        updateChannel(updates, rGroup, CHANNEL_EMETER_VOLTAGE,
                                toQuantityType(getDouble(s.value), DIGITS_VOLT, SmartHomeUnits.VOLT));
                        break;
                    case "current":
                        updateChannel(updates, rGroup, CHANNEL_EMETER_CURRENT,
                                toQuantityType(getDouble(s.value), DIGITS_VOLT, SmartHomeUnits.AMPERE));
                        break;
                    case "pf":
                        updateChannel(updates, rGroup, CHANNEL_EMETER_PFACTOR, getDecimal(s.value));
                        break;
                    case "position":
                        // work around: Roller reports 101% instead max 100
                        double pos = Math.max(SHELLY_MIN_ROLLER_POS, Math.min(s.value, SHELLY_MAX_ROLLER_POS));
                        updateChannel(updates, CHANNEL_GROUP_ROL_CONTROL, CHANNEL_ROL_CONTROL_CONTROL,
                                toQuantityType(SHELLY_MAX_ROLLER_POS - pos, SmartHomeUnits.PERCENT));
                        updateChannel(updates, CHANNEL_GROUP_ROL_CONTROL, CHANNEL_ROL_CONTROL_POS,
                                toQuantityType(pos, SmartHomeUnits.PERCENT));
                        break;
                    case "input event": // Shelly Button 1
                        handleInputEvent(sen, getString(s.valueStr), -1, updates);
                        break;
                    case "input event counter": // Shelly Button 1/ix3
                        handleInputEvent(sen, "", getInteger((int) s.value), updates);
                        break;
                    case "flood":
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_FLOOD,
                                s.value == 1 ? OnOffType.ON : OnOffType.OFF);
                        break;
                    case "tilt": // DW with FW1.6.5+ //+
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_TILT,
                                toQuantityType(s.value, DIGITS_NONE, SmartHomeUnits.DEGREE_ANGLE));
                        break;
                    case "vibration": // DW with FW1.6.5+
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_VIBRATION,
                                s.value == 1 ? OnOffType.ON : OnOffType.OFF);
                        break;
                    case "temp": // Shelly Bulb
                    case "colortemperature": // Shelly Duo
                        updateChannel(updates,
                                profile.inColor ? CHANNEL_GROUP_COLOR_CONTROL : CHANNEL_GROUP_WHITE_CONTROL,
                                CHANNEL_COLOR_TEMP,
                                ShellyColorUtils.toPercent((int) s.value, profile.minTemp, profile.maxTemp));
                        break;
                    case "sensor state": // Shelly Gas
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_SSTATE, getStringType(s.valueStr));
                        break;
                    case "alarm state": // Shelly Gas
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_ALARM_STATE,
                                getStringType(s.valueStr));
                        break;
                    case "self-test state":// Shelly Gas
                        updateChannel(updates, CHANNEL_GROUP_DEV_STATUS, CHANNEL_DEVST_SELFTTEST,
                                getStringType(s.valueStr));
                        break;
                    case "concentration":// Shelly Gas
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_PPM, getDecimal(s.value));
                        break;
                    case "sensorerror":
                        updateChannel(updates, CHANNEL_GROUP_SENSOR, CHANNEL_SENSOR_ERROR, getStringType(s.valueStr));
                        break;
                    default:
                        // Unknown
                        return false;
                }
                break;

            default:
                // Unknown type
                return false;
        }
        return true;
    }

    /**
     *
     * Depending on the device type and firmware release there are significant bugs or incosistencies in the CoIoT
     * Device Description returned by the discovery request. Shelly is even not following it's own speicifcation. All of
     * that has been reported to Shelly and acknowledged. Firmware 1.6 brought significant improvements. However, the
     * old mapping stays in to support older firmware releases.
     *
     * @param sen Sensor description received from device
     * @return fixed Sensor description (sen)
     */
    @Override
    public CoIotDescrSen fixDescription(CoIotDescrSen sen, Map<String, CoIotDescrBlk> blkMap) {
        // Shelly1: reports null descr+type "Switch" -> map to S
        // Shelly1PM: reports null descr+type "Overtemp" -> map to O
        // Shelly1PM: reports null descr+type "W" -> add description
        // Shelly1PM: reports temp senmsors without desc -> add description
        // Shelly Dimmer: sensors are reported without descriptions -> map to S
        // SHelly Sense: multiple issues: Description should not be lower case, invalid type for Motion and Battery
        // Shelly Sense: Battery is reported with Desc "battery", but type "H" instead of "B"
        // Shelly Sense: Motion is reported with Desc "battery", but type "H" instead of "B"
        // Shelly Bulb: Colors are coded with Type="Red" etc. rather than Type="S" and color as Descr
        // Shelly RGBW2 is reporting Brightness, Power, VSwitch for each channel, but all with L=0
        if (sen.desc == null) {
            sen.desc = "";
        }
        String desc = sen.desc.toLowerCase();

        // RGBW2 reports Power_0, Power_1, Power_2, Power_3; same for VSwitch and Brightness, all of them linkted to L:0
        // we break it up to Power with L:0, Power with L:1...
        if (desc.contains("_") && (desc.contains("power") || desc.contains("vswitch") || desc.contains("brightness"))) {
            String newDesc = StringUtils.substringBefore(sen.desc, "_");
            String newLink = StringUtils.substringAfter(sen.desc, "_");
            sen.desc = newDesc;
            sen.links = newLink;
            if (!blkMap.containsKey(sen.links)) {
                // auto-insert a matching blk entry
                CoIotDescrBlk blk = new CoIotDescrBlk();
                CoIotDescrBlk blk0 = blkMap.get("0"); // blk 0 is always there
                blk.id = sen.links;
                blk.desc = blk0.desc + "_" + blk.id;
                blkMap.put(blk.id, blk);
            }
        }

        switch (sen.type.toLowerCase()) {
            case "w": // old devices/firmware releases use "W", new ones "P"
                sen.type = "P";
                sen.desc = "Power";
                break;
            case "tc":
                sen.type = "T";
                sen.desc = "Temperature C";
                break;
            case "tf":
                sen.type = "T";
                sen.desc = "Temperature F";
                break;
            case "overtemp":
                sen.type = "S";
                sen.desc = "Overtemp";
                break;
            case "relay0":
            case "switch":
            case "vswitch":
                sen.type = "S";
                sen.desc = "State";
                break;
        }

        switch (sen.desc.toLowerCase()) {
            case "motion": // fix acc to spec it's T=M
                sen.type = "M";
                sen.desc = "Motion";
                break;
            case "battery": // fix: type is B not H
                sen.type = "B";
                sen.desc = "Battery";
                break;
            case "overtemp":
                sen.type = "S";
                sen.desc = "Overtemp";
                break;
            case "relay0":
            case "switch":
            case "vswitch":
                sen.type = "S";
                sen.desc = "State";
                break;
            case "e cnt 0 [w-min]": // 4 Pro
            case "e cnt 1 [w-min]":
            case "e cnt 2 [w-min]":
            case "e cnt total [w-min]": // 4 Pro
                sen.desc = sen.desc.toLowerCase().replace("e cnt", "energy counter");
                break;

        }

        if (sen.desc.isEmpty()) {
            switch (sen.type.toLowerCase()) {
                case "p":
                    sen.desc = "Power";
                    break;
                case "T":
                    sen.desc = "Temperature";
                    break;
                case "input":
                    sen.type = "S";
                    sen.desc = "Input";
                    break;
                case "output":
                    sen.type = "S";
                    sen.desc = "Output";
                    break;
                case "brightness":
                    sen.type = "S";
                    sen.desc = "Brightness";
                    break;
                case "red":
                case "green":
                case "blue":
                case "white":
                case "gain":
                case "temp": // Bulb: Color temperature
                    sen.desc = sen.type;
                    sen.type = "S";
                    break;
                case "vswitch":
                    // it seems that Shelly tends to break their own spec: T is the description and D is no longer
                    // included -> map D to sen.T and set CatchAll for T
                    sen.desc = sen.type;
                    sen.type = "S";
                    break;
                // Default: set no description
                // (there are no T values defined in the CoIoT spec)
                case "tostate":
                default:
                    sen.desc = "";
            }
        }
        return sen;
    }
}
