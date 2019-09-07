/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.pixometer.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * The {@link PixometerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jerome Luckenbach - Initial contribution
 */
@NonNullByDefault
public class PixometerBindingConstants {

    private static final String BINDING_ID = "pixometer";

    // List of all Thing Type UIDs
    public static final ThingTypeUID BRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, "apiservice");
    public static final ThingTypeUID THING_TYPE_ENERGYMETER = new ThingTypeUID(BINDING_ID, "energymeter");
    public static final ThingTypeUID THING_TYPE_GASMETER = new ThingTypeUID(BINDING_ID, "gasmeter");
    public static final ThingTypeUID THING_TYPE_WATERMETER = new ThingTypeUID(BINDING_ID, "watermeter");

    public static final Set<ThingTypeUID> BRIDGE_THING_TYPES_UIDS = Collections.singleton(BRIDGE_THING_TYPE);
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet(THING_TYPE_ENERGYMETER,
            THING_TYPE_GASMETER, THING_TYPE_WATERMETER);

    // List of all Channel ids
    public static final String CHANNEL_LAST_READING_VALUE = "last_reading_value";
    public static final String CHANNEL_LAST_READING_DATE = "last_reading_date";
    public static final String CHANNEL_LAST_REFRESH_DATE = "last_refresh_date";

    // List of all config ids
    public static final String CONFIG_BRIDGE_AUTH_TOKEN = "access_token";
    public static final String CONFIG_BRIDGE_PASSWORD = "password";
    public static final String CONFIG_BRIDGE_REFRESH = "refresh";
    public static final String CONFIG_BRIDGE_SCOPE = "scope";
    public static final String CONFIG_BRIDGE_USER = "user";

    public static final String CONFIG_THING_RESSOURCE_ID = "resource_id";
}
