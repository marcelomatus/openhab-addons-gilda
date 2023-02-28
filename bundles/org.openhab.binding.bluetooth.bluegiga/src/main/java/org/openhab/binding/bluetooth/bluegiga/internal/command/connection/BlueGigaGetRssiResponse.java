/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.bluetooth.bluegiga.internal.command.connection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaDeviceResponse;

/**
 * Class to implement the BlueGiga command <b>getRssi</b>.
 * <p>
 * This command disconnects an active connection. Bluetooth When link is disconnected a
 * Disconnected event is produced.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaGetRssiResponse extends BlueGigaDeviceResponse {
    public static int COMMAND_CLASS = 0x03;
    public static int COMMAND_METHOD = 0x01;

    /**
     * RSSI value of the connection in dBm. Range: -103 to -38
     * <p>
     * BlueGiga API type is <i>int8</i> - Java type is {@link int}
     */
    private int rssi;

    /**
     * Response constructor
     */
    public BlueGigaGetRssiResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        connection = deserializeUInt8();
        rssi = deserializeInt8();
    }

    /**
     * RSSI value of the connection in dBm. Range: -103 to -38
     * <p>
     * BlueGiga API type is <i>int8</i> - Java type is {@link int}
     *
     * @return the current rssi as {@link int}
     */
    public int getRssi() {
        return rssi;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaGetRssiResponse [connection=");
        builder.append(connection);
        builder.append(", rssi=");
        builder.append(rssi);
        builder.append(']');
        return builder.toString();
    }
}
