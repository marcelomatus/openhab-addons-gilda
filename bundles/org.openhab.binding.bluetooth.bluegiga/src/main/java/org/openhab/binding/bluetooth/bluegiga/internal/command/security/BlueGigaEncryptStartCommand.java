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
package org.openhab.binding.bluetooth.bluegiga.internal.command.security;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>encryptStart</b>.
 * <p>
 * This command starts the encryption for a given connection.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaEncryptStartCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x05;
    public static int COMMAND_METHOD = 0x00;

    /**
     * Bonding handle of a device. This handle can be obtained for example from events like: Scan
     * Response Status - If handle is 0xFF, all bondings will be deleted
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int handle;

    /**
     * Create bonding if devices are not already bonded. 0: Do not create bonding. 1: Creating
     * bonding
     * <p>
     * BlueGiga API type is <i>boolean</i> - Java type is {@link boolean}
     */
    private boolean bonding;

    /**
     * Bonding handle of a device. This handle can be obtained for example from events like: Scan
     * Response Status - If handle is 0xFF, all bondings will be deleted
     *
     * @param handle the handle to set as {@link int}
     */
    public void setHandle(int handle) {
        this.handle = handle;
    }

    /**
     * Create bonding if devices are not already bonded. 0: Do not create bonding. 1: Creating
     * bonding
     *
     * @param bonding the bonding to set as {@link boolean}
     */
    public void setBonding(boolean bonding) {
        this.bonding = bonding;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeUInt8(handle);
        serializeBoolean(bonding);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaEncryptStartCommand [handle=");
        builder.append(handle);
        builder.append(", bonding=");
        builder.append(bonding);
        builder.append(']');
        return builder.toString();
    }
}
