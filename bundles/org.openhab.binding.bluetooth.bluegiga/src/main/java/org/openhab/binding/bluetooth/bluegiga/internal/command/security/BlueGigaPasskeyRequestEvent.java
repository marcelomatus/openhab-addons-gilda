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
package org.openhab.binding.bluetooth.bluegiga.internal.command.security;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>passkeyRequestEvent</b>.
 * <p>
 * This event indicates the Security Manager requests the user to enter passkey. The passkey
 * the user needs to enter is displayed by the remote device. Use Passkey Entry command to
 * respond to request
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaPasskeyRequestEvent extends BlueGigaResponse {
    public static int COMMAND_CLASS = 0x05;
    public static int COMMAND_METHOD = 0x03;

    /**
     * Bluetooth connection handle
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int handle;

    /**
     * Event constructor
     */
    public BlueGigaPasskeyRequestEvent(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        handle = deserializeUInt8();
    }

    /**
     * Bluetooth connection handle
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current handle as {@link int}
     */
    public int getHandle() {
        return handle;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaPasskeyRequestEvent [handle=");
        builder.append(handle);
        builder.append(']');
        return builder.toString();
    }
}
