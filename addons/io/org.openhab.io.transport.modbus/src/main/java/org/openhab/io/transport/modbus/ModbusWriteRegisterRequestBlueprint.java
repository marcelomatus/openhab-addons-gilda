/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

/**
 * Write request for registers
 *
 * @author Sami Salonen
 *
 */
public interface ModbusWriteRegisterRequestBlueprint extends ModbusWriteRequestBlueprint {

    public ModbusRegisterArray getRegisters();

    @Override
    public default void accept(ModbusWriteRequestBlueprintVisitor visitor) {
        visitor.visit(this);
    }
}
