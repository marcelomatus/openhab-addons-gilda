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
package org.openhab.binding.luxtronicheatpump.internal.enums;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents all heat pump operation modes
 *
 * @author Stefan Giehl - Initial contribution
 */
@NonNullByDefault
public enum HeatpumpOperationMode {
    AUTOMATIC(0),
    OFF(4),
    PARTY(2),
    HOLIDAY(3),
    AUXILIARY_HEATER(1);

    private int value;

    private HeatpumpOperationMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static HeatpumpOperationMode fromValue(int value) {
        for (HeatpumpOperationMode mode : HeatpumpOperationMode.values()) {
            if (mode.value == value) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Invalid heat pump operation mode: '" + value + "'");
    }
}
