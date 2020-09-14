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
package org.openhab.binding.playstation.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link PlayStationBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Fredrik Ahlström - Initial contribution
 */
@NonNullByDefault
public class PlayStationBindingConstants {

    private static final String BINDING_ID = "playstation";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_PS3 = new ThingTypeUID(BINDING_ID, "PS3");
    public static final ThingTypeUID THING_TYPE_PS4 = new ThingTypeUID(BINDING_ID, "PS4");
    public static final ThingTypeUID THING_TYPE_PS5 = new ThingTypeUID(BINDING_ID, "PS5");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_PS3, THING_TYPE_PS4, THING_TYPE_PS5).collect(Collectors.toSet()));

    // List of all Channel ids
    static final String CHANNEL_POWER = "power";
    static final String CHANNEL_APPLICATION_NAME = "applicationName";
    static final String CHANNEL_APPLICATION_ID = "applicationId";
    static final String CHANNEL_APPLICATION_IMAGE = "applicationImage";
    static final String CHANNEL_OSK_TEXT = "oskText";
    static final String CHANNEL_KEY_UP = "keyUp";
    static final String CHANNEL_KEY_DOWN = "keyDown";
    static final String CHANNEL_KEY_RIGHT = "keyRight";
    static final String CHANNEL_KEY_LEFT = "keyLeft";
    static final String CHANNEL_KEY_ENTER = "keyEnter";
    static final String CHANNEL_KEY_BACK = "keyBack";
    static final String CHANNEL_KEY_OPTION = "keyOption";
    static final String CHANNEL_KEY_PS = "keyPS";
    static final String CHANNEL_DISCONNECT = "disconnect";
    static final String CHANNEL_2ND_SCREEN = "secondScreen";
    static final String CHANNEL_LOG_OUT = "logOut";
    static final String CHANNEL_SCREEN_SHOT = "screenShot";

    // List of all properties in the response from the PS4
    public static final String RESPONSE_HOST_ID = "host-id";
    public static final String RESPONSE_HOST_TYPE = "host-type";
    public static final String RESPONSE_HOST_NAME = "host-name";
    public static final String RESPONSE_HOST_REQUEST_PORT = "host-request-port";
    public static final String RESPONSE_DEVICE_DISCOVERY_PROTOCOL_VERSION = "device_discovery_protocol-version";
    public static final String RESPONSE_SYSTEM_VERSION = "system-version";
    public static final String RESPONSE_RUNNING_APP_NAME = "running-app-name";
    public static final String RESPONSE_RUNNING_APP_TITLEID = "running-app-titleid";

    public static final String PS3HW_CECHXXXX = "CECHXXXX";
    public static final String PS3HW_CECHA00 = "CECHA00";
    public static final String PS3HW_CECHB00 = "CECHB00";
    public static final String PS3HW_CECHC00 = "CECHC00";
    public static final String PS3HW_CECHE00 = "CECHE00";
    public static final String PS3HW_CECHG00 = "CECHG00";
    public static final String PS3HW_CECHH00 = "CECHH00";
    public static final String PS3HW_CECHJ00 = "CECHJ00";
    public static final String PS3HW_CECHK00 = "CECHK00";
    public static final String PS3HW_CECHL00 = "CECHL00";
    public static final String PS3HW_CECHM00 = "CECHM00";
    public static final String PS3HW_CECHP00 = "CECHP00";
    public static final String PS3HW_CECHQ00 = "CECHQ00";
    public static final String PS3HW_CECH2000 = "CECH-2000";
    public static final String PS3HW_CECH2100 = "CECH-2100";
    public static final String PS3HW_CECH2500 = "CECH-2500";
    public static final String PS3HW_CECH3000 = "CECH-3000";
    public static final String PS3HW_CECH4000 = "CECH-4000";
    public static final String PS3HW_CECH4200 = "CECH-4200";
    public static final String PS3HW_CECH4300 = "CECH-4300";

    public static final String PS4HW_CUHXXXX = "CUH-XXXX";
    public static final String PS4HW_CUH1000 = "CUH-1000";
    public static final String PS4HW_CUH1100 = "CUH-1100";
    public static final String PS4HW_CUH1200 = "CUH-1200";
    public static final String PS4HW_CUH2000 = "CUH-2000";
    public static final String PS4HW_CUH2100 = "CUH-2100";
    public static final String PS4HW_CUH2200 = "CUH-2200";
    public static final String PS4HW_CUH7000 = "CUH-7000";
    public static final String PS4HW_CUH7100 = "CUH-7100";

    static final int PS4_KEY_UP = 1 << 0;
    static final int PS4_KEY_DOWN = 1 << 1;
    static final int PS4_KEY_RIGHT = 1 << 2;
    static final int PS4_KEY_LEFT = 1 << 3;
    static final int PS4_KEY_ENTER = 1 << 4;
    static final int PS4_KEY_BACK = 1 << 5;
    static final int PS4_KEY_OPTION = 1 << 6;
    static final int PS4_KEY_PS = 1 << 7;
    static final int PS4_KEY_OFF = 1 << 8;
    static final int PS4_KEY_CANCEL = 1 << 9;
    static final int PS4_KEY_OPEN_RC = 1 << 10;
    static final int PS4_KEY_CLOSE_RC = 1 << 11;

    /**
     * Default port numbers PS4 uses.
     */
    public static final int DEFAULT_BROADCAST_PORT = 987;
    public static final int DEFAULT_COMMUNICATION_PORT = 997;

    /** Default port for PS3. */
    public static final int DEFAULT_PS3_BROADCAST_PORT = 9293;
    public static final int DEFAULT_PS3_UNKNOWN_PORT = 9309;
    public static final int DEFAULT_PS3_UNKNOWN_PORT1 = 56235;
    public static final int DEFAULT_PS3_UNKNOWN_PORT2 = 56259;
    public static final int DEFAULT_PS3_COMMUNICATION_PORT = 5223;

    private PlayStationBindingConstants() {
        // No need to instance this class.
    }

}
