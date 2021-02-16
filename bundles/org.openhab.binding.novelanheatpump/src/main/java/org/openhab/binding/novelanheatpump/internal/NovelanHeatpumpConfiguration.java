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
package org.openhab.binding.novelanheatpump.internal;

/**
 * The {@link NovelanHeatpumpConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Jan-Philipp Bolle - Initial contribution
 */
public class NovelanHeatpumpConfiguration {

    /**
     * Port of the Heatpump API
     */
    public Integer port;
    /**
     * Heatpump data fetches interval (in seconds)
     */
    public Integer refresh;
    /**
     * The Network Address of the Heatpump
     */
    public String address;
}
