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
package org.openhab.binding.modbus.solaxx3mic.internal;

/**
 * The {@link RegisterBlockFunction} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Stanislaw Wawszczak - Initial contribution
 */
public enum RegisterBlockFunction {
    INPUT_REGISTER_BLOCK(3),
    HOLDING_REGISTER_BLOCK(4);

    private final int code;

    RegisterBlockFunction(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }
}
