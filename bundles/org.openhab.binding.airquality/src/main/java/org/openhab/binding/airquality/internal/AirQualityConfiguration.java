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
package org.openhab.binding.airquality.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link AirQualityConfiguration} is the class used to match the
 * thing configuration.
 *
 * @author Kuba Wolanin - Initial contribution
 */
@NonNullByDefault
public class AirQualityConfiguration {
    public static final String LOCATION = "location";
    public static final String STATION_ID = "stationId";

    public String apikey = "";
    public String location = "";
    public int stationId = 0;
    public int refresh = 60;
}
