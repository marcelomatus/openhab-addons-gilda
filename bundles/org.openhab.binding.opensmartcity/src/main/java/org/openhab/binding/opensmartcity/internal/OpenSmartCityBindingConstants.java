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
package org.openhab.binding.opensmartcity.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link OpenSmartCityBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class OpenSmartCityBindingConstants {

    private static final String BINDING_ID = "opensmartcity";

    public static final String CONFIG_LOCATION = "location";

    public static final String LOCAL = "local";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_CITY = new ThingTypeUID(BINDING_ID, "city");
    public static final ThingTypeUID THING_TYPE_WEATHER = new ThingTypeUID(BINDING_ID, "weather");

    // List of all Channel ids
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_HUMIDITY = "humidity";
}
