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
package org.openhab.binding.bluetooth.daikinmadoka.internal.model;

import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Benjamin Lafois - Initial contribution
 */
@NonNullByDefault
public class MadokaValue {

    private int id;
    private int size;
    private byte @Nullable [] rawValue;

    public MadokaValue(int id, int size, byte[] rawValue) {
        this.id = id;
        this.size = size;
        this.rawValue = rawValue;
    }

    public MadokaValue() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte @Nullable [] getRawValue() {
        return rawValue;
    }

    public void setRawValue(byte[] rawValue) {
        this.rawValue = rawValue;
    }

    public long getComputedValue() {
        byte[] v = rawValue;
        if (v != null) {
            switch (size) {
                case 1:
                    return v[0];
                case 2:
                    return ByteBuffer.wrap(v, 0, 2).getShort();
                case 4:
                    return ByteBuffer.wrap(v, 0, 4).getInt();
                default:
                    // unsupported
                    break;
            }
        }
        return 0;
    }
}
