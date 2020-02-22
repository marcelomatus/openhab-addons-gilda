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
package org.openhab.binding.bluetooth.bluegiga.internal.command.system;

import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>addressGet</b>.
 * <p>
 * This command reads the local device's public Bluetooth address.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class BlueGigaAddressGetResponse extends BlueGigaResponse {
    public static int COMMAND_CLASS = 0x00;
    public static int COMMAND_METHOD = 0x02;

    /**
     * Bluetooth address of the local device
     * <p>
     * BlueGiga API type is <i>bd_addr</i> - Java type is {@link String}
     */
    private String address;

    /**
     * Response constructor
     */
    public BlueGigaAddressGetResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        address = deserializeAddress();
    }

    /**
     * Bluetooth address of the local device
     * <p>
     * BlueGiga API type is <i>bd_addr</i> - Java type is {@link String}
     *
     * @return the current address as {@link String}
     */
    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaAddressGetResponse [address=");
        builder.append(address);
        builder.append(']');
        return builder.toString();
    }
}
