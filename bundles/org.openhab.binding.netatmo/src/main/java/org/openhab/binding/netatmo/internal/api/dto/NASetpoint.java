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
package org.openhab.binding.netatmo.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.netatmo.internal.api.NetatmoConstants.SetpointMode;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */

@NonNullByDefault
public class NASetpoint {
    private double thermSetpointTemperature;
    private long setpointEndtime;
    private SetpointMode setpointMode = SetpointMode.UNKNOWN;

    public double getSetpointTemperature() {
        return thermSetpointTemperature;
    }

    public long getSetpointEndtime() {
        return setpointEndtime;
    }

    public SetpointMode getMode() {
        return setpointMode;
    }
}
