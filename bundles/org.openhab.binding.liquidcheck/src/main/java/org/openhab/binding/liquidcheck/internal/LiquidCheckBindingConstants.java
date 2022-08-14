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
package org.openhab.binding.liquidcheck.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link LiquidCheckBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
public class LiquidCheckBindingConstants {

    private static final String BINDING_ID = "liquidcheck";

    // List of all Thing Type UIDs
<<<<<<< HEAD
    public static final ThingTypeUID THING_TYPE_LIQUID_CHEK = new ThingTypeUID(BINDING_ID, "liquidCheckDevice");
=======
    public static final ThingTypeUID THING_TYPE_LIQUID_CHECK = new ThingTypeUID(BINDING_ID, "liquidCheckDevice");
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d

    // List of all Channel ids
    public static final String CONTENT_CHANNEL = "content";
    public static final String RAW_CONTENT_CHANNEL = "raw-content";
    public static final String LEVEL_CHANNEL = "level";
    public static final String RAW_LEVEL_CHANNEL = "raw-level";
    public static final String PUMP_TOTAL_RUNS_CHANNEL = "pump-runs";
    public static final String PUMP_TOTAL_RUNTIME_CHANNEL = "pump-runtime";
    public static final String MEASURE_CHANNEL = "measure";

    // List of all Property ids
    public static final String CONFIG_ID_FIRMWARE = "firmware";
    public static final String CONFIG_ID_HARDWARE = "hardware";
    public static final String CONFIG_ID_NAME = "name";
    public static final String CONFIG_ID_MANUFACTURER = "manufacturer";
    public static final String CONFIG_ID_UUID = "uuid";
    public static final String CONFIG_ID_SECURITY_CODE = "securityCode";
    public static final String CONFIG_ID_IP = "ip";
    public static final String CONFIG_ID_MAC = "mac";
    public static final String CONFIG_ID_SSID = "ssid";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
}
