/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.liquidcheck.internal.json;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link Wifi} contains the data about the wireless connection.
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
public class Wifi {
    public Station station = new Station();
    public AccessPoint accessPoint = new AccessPoint();
}
