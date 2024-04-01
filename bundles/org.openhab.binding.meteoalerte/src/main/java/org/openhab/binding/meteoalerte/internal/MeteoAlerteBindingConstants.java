/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.meteoalerte.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link MeteoAlerteBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class MeteoAlerteBindingConstants {
    public static final String BINDING_ID = "meteoalerte";

    // List of Bridge Type UIDs
    public static final ThingTypeUID BRIDGE_TYPE_API = new ThingTypeUID(BINDING_ID, "api");

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_DEPARTEMENT = new ThingTypeUID(BINDING_ID, "department");

    // List of all Channel IDs

    public static final String OBSERVATION_TIME = "observation-time";
    public static final String END_TIME = "end-time";
    public static final String COMMENT = "comment";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(BRIDGE_TYPE_API, THING_TYPE_DEPARTEMENT);
}
