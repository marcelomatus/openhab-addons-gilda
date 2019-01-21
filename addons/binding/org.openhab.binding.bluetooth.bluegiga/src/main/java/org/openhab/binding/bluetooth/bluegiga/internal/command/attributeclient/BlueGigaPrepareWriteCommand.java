/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient;

import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>prepareWrite</b>.
 * <p>
 * This command will send a prepare write request to a remote device for queued writes. Queued
 * writes can for example be used to write large attribute values by transmitting the data in
 * chunks using prepare write command. Once the data has been transmitted with multiple
 * prepare write commands the write must then be executed or canceled with Execute Write
 * command, which if acknowledged by the remote device triggers a Procedure Completed event.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class BlueGigaPrepareWriteCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x04;
    public static int COMMAND_METHOD = 0x09;

    /**
     * Connection handle
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int connection;

    /**
     * Attribute handle
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int attHandle;

    /**
     * Offset to write to
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int offset;

    /**
     * Data to write. Maximum amount of data that can be sent in single command is 18 bytes.
     * <p>
     * BlueGiga API type is <i>uint8array</i> - Java type is {@link int[]}
     */
    private int[] data;

    /**
     * Connection handle
     *
     * @param connection the connection to set as {@link int}
     */
    public void setConnection(int connection) {
        this.connection = connection;
    }
    /**
     * Attribute handle
     *
     * @param attHandle the attHandle to set as {@link int}
     */
    public void setAttHandle(int attHandle) {
        this.attHandle = attHandle;
    }
    /**
     * Offset to write to
     *
     * @param offset the offset to set as {@link int}
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }
    /**
     * Data to write. Maximum amount of data that can be sent in single command is 18 bytes.
     *
     * @param data the data to set as {@link int[]}
     */
    public void setData(int[] data) {
        this.data = data;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeUInt8(connection);
        serializeUInt16(attHandle);
        serializeUInt16(offset);
        serializeUInt8Array(data);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaPrepareWriteCommand [connection=");
        builder.append(connection);
        builder.append(", attHandle=");
        builder.append(attHandle);
        builder.append(", offset=");
        builder.append(offset);
        builder.append(", data=");
        for (int c = 0; c < data.length; c++) {
            if (c > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", data[c]));
        }
        builder.append(']');
        return builder.toString();
    }
}
