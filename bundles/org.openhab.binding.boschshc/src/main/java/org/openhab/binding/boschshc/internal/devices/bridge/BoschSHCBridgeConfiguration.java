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
package org.openhab.binding.boschshc.internal.devices.bridge;

/**
 * The {@link BoschSHCBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Stefan Kästle - Initial contribution
 */
public class BoschSHCBridgeConfiguration {

    /**
     * IP address of the Bosch Smart Home Controller
     */
    public String ipAddress;

    /**
     * Password of the Bosch Smart Home Controller. Set during initialization via the Bosch app.
     */
    public String password;
}
