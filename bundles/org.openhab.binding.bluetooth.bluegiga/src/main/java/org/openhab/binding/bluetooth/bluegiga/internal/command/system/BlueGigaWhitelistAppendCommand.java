/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>whitelistAppend</b>.
 * <p>
 * Add an entry to the running white list. By the white list you can define for example the remote
 * devices which are allowed to establish a connection. See also Set Filtering Connect
 * Selective and (if the white list is empty they will not be active). Do not use this command
 * while advertising, scanning, or while being connected. The current list is discarded upon
 * reset or power-cycle.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaWhitelistAppendCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x00;
    public static int COMMAND_METHOD = 0x0A;

    /**
     * Bluetooth device address to add to the running white list. Maximum of 8 can be stored before
     * you must clear or remove entries.
     * <p>
     * BlueGiga API type is <i>bd_addr</i> - Java type is {@link String}
     */
    private String address = "";

    /**
     * Bluetooth device address to add to the running white list. Maximum of 8 can be stored before
     * you must clear or remove entries.
     *
     * @param address the address to set as {@link String}
     */
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeAddress(address);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaWhitelistAppendCommand [address=");
        builder.append(address);
        builder.append(']');
        return builder.toString();
    }
}
