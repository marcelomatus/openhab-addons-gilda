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
package org.openhab.binding.venstarthermostat.internal.dto;

import java.util.List;

/**
 * The {@link VenstarSensorData} represents sensor data returned from the REST API.
 *
 * @author Matthew Davies - Initial contribution
 */
public class VenstarRuntimeData {
    List<VenstarRuntime> runtimes;

    public VenstarRuntimeData() {
        super();
    }

    public VenstarRuntimeData(List<VenstarRuntime> runtimes) {
        super();
        this.runtimes = runtimes;
    }

    public List<VenstarRuntime> getRuntimes() {
        return runtimes;
    }

    public void setRuntimes(List<VenstarRuntime> runtimes) {
        this.runtimes = runtimes;
    }
}
