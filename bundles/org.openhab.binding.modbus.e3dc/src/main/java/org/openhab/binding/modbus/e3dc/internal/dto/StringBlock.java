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

import java.nio.ByteBuffer;

import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Power;

import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.openhab.binding.modbus.e3dc.internal.modbus.Data;

/**
 * The {@link StringBlock} Data object for E3DC Info Block
 *
 * @author Bernd Weymann - Initial contribution
 */
public class StringBlock implements Data {
    public QuantityType<ElectricPotential> string1Volt;
    public QuantityType<ElectricPotential> string2Volt;
    public QuantityType<ElectricPotential> string3Volt;
    public QuantityType<ElectricCurrent> string1Ampere;
    public QuantityType<ElectricCurrent> string2Ampere;
    public QuantityType<ElectricCurrent> string3Ampere;
    public QuantityType<Power> string1Watt;
    public QuantityType<Power> string2Watt;
    public QuantityType<Power> string3Watt;

    /**
     * For decoding see Modbus Register Mapping Chapter 3.1.2 page 14-16
     *
     * @param bArray - Modbus Registers as bytes from 40096 to 40104
     */
    public StringBlock(byte[] bArray) {
        ByteBuffer wrap = ByteBuffer.wrap(bArray);
        // straight forward - for each String the values Volt, Ampere and then Watt. All unt16 = 2 bytes values
        string1Volt = new QuantityType<ElectricPotential>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.VOLT);
        string2Volt = new QuantityType<ElectricPotential>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.VOLT);
        string3Volt = new QuantityType<ElectricPotential>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.VOLT);
        string1Ampere = new QuantityType<ElectricCurrent>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.AMPERE);
        string2Ampere = new QuantityType<ElectricCurrent>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.AMPERE);
        string3Ampere = new QuantityType<ElectricCurrent>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.AMPERE);
        string1Watt = new QuantityType<Power>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.WATT);
        string2Watt = new QuantityType<Power>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.WATT);
        string3Watt = new QuantityType<Power>(DataConverter.getUIntt16Value(wrap), SmartHomeUnits.WATT);
    }
}
