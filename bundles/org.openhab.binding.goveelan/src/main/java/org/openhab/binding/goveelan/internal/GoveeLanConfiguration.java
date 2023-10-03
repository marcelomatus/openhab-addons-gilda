/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.goveelan.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link GoveeLanConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Stefan Höhn - Initial contribution
 */
@NonNullByDefault
public class GoveeLanConfiguration {
    public static final String IPADDRESS = "hostname";
    public static final String MAC_ADDRESS = "macaddress";
    public static final String DEVICETYPE = "devicetype";
    public static final String PRODUCTNAME = "productname";
    public int refreshInterval = 3; // in seconds
}
