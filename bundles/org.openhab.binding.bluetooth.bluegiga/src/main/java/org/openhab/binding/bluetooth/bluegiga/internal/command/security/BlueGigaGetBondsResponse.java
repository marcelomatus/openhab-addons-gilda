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
package org.openhab.binding.bluetooth.bluegiga.internal.command.security;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>getBonds</b>.
 * <p>
 * This command lists all bonded devices. There can be a maximum of 8 bonded devices. The
 * information related to the bonded devices is stored in the Flash memory, so it is persistent
 * across resets and power-cycles.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaGetBondsResponse extends BlueGigaResponse {
    public static final int COMMAND_CLASS = 0x05;
    public static final int COMMAND_METHOD = 0x05;

    /**
     * Num of currently bonded devices
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int bonds;

    /**
     * Response constructor
     */
    public BlueGigaGetBondsResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        bonds = deserializeUInt8();
    }

    /**
     * Num of currently bonded devices
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current bonds as {@link int}
     */
    public int getBonds() {
        return bonds;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaGetBondsResponse [bonds=");
        builder.append(bonds);
        builder.append(']');
        return builder.toString();
    }
}
