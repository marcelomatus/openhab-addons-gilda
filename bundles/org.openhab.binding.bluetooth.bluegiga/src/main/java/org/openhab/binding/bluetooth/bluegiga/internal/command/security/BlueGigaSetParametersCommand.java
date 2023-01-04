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
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.SmpIoCapabilities;

/**
 * Class to implement the BlueGiga command <b>setParameters</b>.
 * <p>
 * This command is used to configure the local Security Manager and its features
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaSetParametersCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x05;
    public static int COMMAND_METHOD = 0x03;

    /**
     * 1: Man-in-the-middle protection required. 0: No Man-in-the-middle protection. Default:
     * 0
     * <p>
     * BlueGiga API type is <i>boolean</i> - Java type is {@link boolean}
     */
    private boolean requireMitm;

    /**
     * Minimum key size in Bytes. Range: 7-16. Default: 7 (56bits)
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int minKeySize;

    /**
     * Configures the local devices I/O capabilities. See: SMP IO Capabilities for options.
     * Default: No Input and No Output
     * <p>
     * BlueGiga API type is <i>SmpIoCapabilities</i> - Java type is {@link SmpIoCapabilities}
     */
    private SmpIoCapabilities ioCapabilities = SmpIoCapabilities.UNKNOWN;

    /**
     * 1: Man-in-the-middle protection required. 0: No Man-in-the-middle protection. Default:
     * 0
     *
     * @param requireMitm the requireMitm to set as {@link boolean}
     */
    public void setRequireMitm(boolean requireMitm) {
        this.requireMitm = requireMitm;
    }

    /**
     * Minimum key size in Bytes. Range: 7-16. Default: 7 (56bits)
     *
     * @param minKeySize the minKeySize to set as {@link int}
     */
    public void setMinKeySize(int minKeySize) {
        this.minKeySize = minKeySize;
    }

    /**
     * Configures the local devices I/O capabilities. See: SMP IO Capabilities for options.
     * Default: No Input and No Output
     *
     * @param ioCapabilities the ioCapabilities to set as {@link SmpIoCapabilities}
     */
    public void setIoCapabilities(SmpIoCapabilities ioCapabilities) {
        this.ioCapabilities = ioCapabilities;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeBoolean(requireMitm);
        serializeUInt8(minKeySize);
        serializeSmpIoCapabilities(ioCapabilities);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaSetParametersCommand [requireMitm=");
        builder.append(requireMitm);
        builder.append(", minKeySize=");
        builder.append(minKeySize);
        builder.append(", ioCapabilities=");
        builder.append(ioCapabilities);
        builder.append(']');
        return builder.toString();
    }
}
