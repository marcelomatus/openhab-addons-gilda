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
package org.openhab.binding.modbus.e3dc.internal.dto;

import static org.openhab.binding.modbus.e3dc.internal.modbus.E3DCModbusConstans.*;

import java.nio.ByteBuffer;
import java.util.BitSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.binding.modbus.e3dc.internal.modbus.Data;

/**
 * The {@link EmergencyBlock} Data object for E3DC Info Block
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class EmergencyBlock implements Data {
    public StringType epStatus = EP_UNKOWN;
    public OnOffType batteryChargingLocked = OnOffType.OFF;
    public OnOffType batteryDischargingLocked = OnOffType.OFF;
    public OnOffType epPossible = OnOffType.OFF;
    public OnOffType weatherPredictedCharging = OnOffType.OFF;
    public OnOffType regulationStatus = OnOffType.OFF;
    public OnOffType chargeLockTime = OnOffType.OFF;
    public OnOffType dischargeLockTime = OnOffType.OFF;

    // Possible Status definitions according to chapter 3.1.2, Register 40084, page 14 & 15
    public static final StringType EP_NOT_SUPPORTED = new StringType("EP not supported");
    public static final StringType EP_ACTIVE = new StringType("EP active");
    public static final StringType EP_NOT_ACTIVE = new StringType("EP not active");
    public static final StringType EP_POSSIBLE = new StringType("EP possible");
    public static final StringType EP_SWITCH = new StringType("EP Switch in wrong position");
    public static final StringType EP_UNKOWN = new StringType("EP Status unknown");
    public static final StringType[] EP_STATUS_ARRAY = new StringType[] { EP_NOT_SUPPORTED, EP_ACTIVE, EP_NOT_ACTIVE,
            EP_POSSIBLE, EP_SWITCH };

    /**
     * For decoding see Modbus Register Mapping Chapter 3.1.2 page 14 & 15
     *
     * @param bArray - Modbus Registers as bytes from 40084 to 40085
     */
    public EmergencyBlock(byte[] bArray) {
        // uint16 status register 40084 - possible Status Strings are defined in Constants above
        int status = DataConverter.getUInt16Value(ByteBuffer.wrap(bArray));
        if (status >= 0 && status < 5) {
            epStatus = EP_STATUS_ARRAY[status];
        } else {
            epStatus = EP_UNKOWN;
        }

        // uint16 status register 40085 shall be handled as Bits - check cahpter 3.1.3 page 17
        byte[] emsStatusBytes = new byte[] { bArray[3], bArray[2] };
        BitSet bs = BitSet.valueOf(emsStatusBytes);
        batteryChargingLocked = bs.get(EMS_CHARGING_LOCK_BIT) ? OnOffType.ON : OnOffType.OFF;
        batteryDischargingLocked = bs.get(EMS_DISCHARGING_LOCK_BIT) ? OnOffType.ON : OnOffType.OFF;
        epPossible = bs.get(EMS_DISCHARGING_LOCK_BIT) ? OnOffType.ON : OnOffType.OFF;
        weatherPredictedCharging = bs.get(EMS_WEATHER_CHARGING_BIT) ? OnOffType.ON : OnOffType.OFF;
        regulationStatus = bs.get(EMS_REGULATION_BIT) ? OnOffType.ON : OnOffType.OFF;
        chargeLockTime = bs.get(EMS_CHARGE_LOCKTIME_BIT) ? OnOffType.ON : OnOffType.OFF;
        dischargeLockTime = bs.get(EMS_DISCHARGE_LOCKTIME_BIT) ? OnOffType.ON : OnOffType.OFF;
    }
}
