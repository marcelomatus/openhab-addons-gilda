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
package org.openhab.binding.lgthinq.lgapi.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link DevicePowerState}
 *
 * @author Nemer Daud - Initial contribution
 */
@NonNullByDefault
public enum DevicePowerState {
    DV_POWER_ON(1.0),
    DV_POWER_OFF(0.0),
    DV_POWER_UNK(-1);

    private final double powerState;

    public double getValue() {
        return powerState;
    }

    DevicePowerState(double i) {
        powerState = i;
    }

    public static DevicePowerState statusOf(double value) {
        switch ((int) value) {
            case 1:
                return DV_POWER_ON;
            case 0:
                return DV_POWER_OFF;
            default:
                return DV_POWER_UNK;
        }
    }

    public static double valueOf(DevicePowerState dps) {
        return dps.powerState;
    }

    /**
     * Value of command (not state, but command to change the state of device)
     * 
     * @return value of the command to reach the state
     */
    public int commandValue() {
        switch (this) {
            case DV_POWER_ON:
                return 257;// "@AC_MAIN_OPERATION_ALL_ON_W"
            case DV_POWER_OFF:
                return 0; // "@AC_MAIN_OPERATION_OFF_W"
            default:
                throw new IllegalArgumentException("Enum not accepted for command:" + this);
        }
    }
}
