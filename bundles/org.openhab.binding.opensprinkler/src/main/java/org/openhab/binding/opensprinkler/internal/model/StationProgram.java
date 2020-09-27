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
package org.openhab.binding.opensprinkler.internal.model;

/**
 * The {@link StationProgram} class corresponds to the program set in the station.
 *
 * @author Florian Schmidt - Initial contribution
 */
public class StationProgram {
    public final long remainingWaterTime;

    public StationProgram(int remainingWaterTime) {
        this.remainingWaterTime = remainingWaterTime;
    }
}
