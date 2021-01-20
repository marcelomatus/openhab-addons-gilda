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
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>getCounters</b>.
 * <p>
 * Read packet counters and resets them, also returns available packet buffers.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaGetCountersResponse extends BlueGigaResponse {
    public static int COMMAND_CLASS = 0x00;
    public static int COMMAND_METHOD = 0x05;

    /**
     * Number of transmitted packets
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int txok;

    /**
     * Number of retransmitted packets
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int txretry;

    /**
     * Number of received packets where CRC was OK
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int rxok;

    /**
     * Number of received packets with CRC error
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int rxfail;

    /**
     * Number of available packet buffers
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int mbuf;

    /**
     * Response constructor
     */
    public BlueGigaGetCountersResponse(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        txok = deserializeUInt8();
        txretry = deserializeUInt8();
        rxok = deserializeUInt8();
        rxfail = deserializeUInt8();
        mbuf = deserializeUInt8();
    }

    /**
     * Number of transmitted packets
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current txok as {@link int}
     */
    public int getTxok() {
        return txok;
    }

    /**
     * Number of retransmitted packets
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current txretry as {@link int}
     */
    public int getTxretry() {
        return txretry;
    }

    /**
     * Number of received packets where CRC was OK
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current rxok as {@link int}
     */
    public int getRxok() {
        return rxok;
    }

    /**
     * Number of received packets with CRC error
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current rxfail as {@link int}
     */
    public int getRxfail() {
        return rxfail;
    }

    /**
     * Number of available packet buffers
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current mbuf as {@link int}
     */
    public int getMbuf() {
        return mbuf;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaGetCountersResponse [txok=");
        builder.append(txok);
        builder.append(", txretry=");
        builder.append(txretry);
        builder.append(", rxok=");
        builder.append(rxok);
        builder.append(", rxfail=");
        builder.append(rxfail);
        builder.append(", mbuf=");
        builder.append(mbuf);
        builder.append(']');
        return builder.toString();
    }
}
