/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

import java.util.Arrays;

import net.wimpi.modbus.procimg.SimpleInputRegister;

/**
 * Basic {@link ModbusRegister} implementation
 *
 * @author Sami Salonen
 */
public class ModbusRegisterImpl implements ModbusRegister {

    private SimpleInputRegister wrapped;

    /**
     * Constructs a new instance for bytes
     *
     * @param b1 the first (hi) byte of the word.
     * @param b2 the second (low) byte of the word.
     */
    public ModbusRegisterImpl(byte b1, byte b2) {
        wrapped = new SimpleInputRegister(b1, b2);
    }

    /**
     * Construct register for at
     *
     * @param val value representing register data. The <code>int</code> will be downcasted to <code>short</code>.
     */
    public ModbusRegisterImpl(int val) {
        wrapped = new SimpleInputRegister(val);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public byte[] getBytes() {
        return wrapped.toBytes();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public int getValue() {
        return wrapped.getValue();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public int toUnsignedShort() {
        return wrapped.toUnsignedShort();
    }

    @Override
    public String toString() {
        return new StringBuffer("ModbusRegisterImpl(bytes=").append(Arrays.toString(wrapped.toBytes()))
                .append(",ushort=").append(toUnsignedShort()).append(')').toString();
    }
}
