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
package org.openhab.binding.lifx.internal.protocol;

import java.nio.ByteBuffer;

import org.openhab.binding.lifx.internal.fields.Field;
import org.openhab.binding.lifx.internal.fields.StringField;

/**
 * @author Tim Buckley - Initial Contribution
 * @author Karel Goderis - Enhancement for the V2 LIFX Firmware and LAN Protocol Specification
 */
public class StateLabelResponse extends Packet {

    public static final int TYPE = 0x19;

    public static final Field<String> FIELD_LABEL = new StringField(32).utf8();

    private String label;

    public static int getType() {
        return TYPE;
    }

    public static Field<String> getFieldLabel() {
        return FIELD_LABEL;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int packetType() {
        return TYPE;
    }

    @Override
    protected int packetLength() {
        return 32;
    }

    @Override
    protected void parsePacket(ByteBuffer bytes) {
        label = FIELD_LABEL.value(bytes);
    }

    @Override
    protected ByteBuffer packetBytes() {
        return FIELD_LABEL.bytes(label);
    }

    @Override
    public int[] expectedResponses() {
        return new int[] {};
    }

}
