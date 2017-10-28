/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.icloud;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Patrik Gfeller - Initial contribution
 */
public class BindingConstants {

    private static final String BINDING_ID = "icloud";

    private static final String BRIDGE_ID = "account";
    private static final String DEVICE_ID = "device";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ICLOUD = new ThingTypeUID(BINDING_ID, BRIDGE_ID);
    public static final ThingTypeUID THING_TYPE_ICLOUDDEVICE = new ThingTypeUID(BINDING_ID, DEVICE_ID);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_ICLOUD,
            THING_TYPE_ICLOUDDEVICE);

    // List of all Channel IDs
    public static final String NUMBEROFDEVICES = "numberOfDevices";
    public static final String OWNER = "owner";
    public static final String REFRESH = "refresh";
    public static final String ENABLEADDRESSLOOKUP = "enableAddressLookup";

    public static final String BATTERYSTATUS = "batteryStatus";
    public static final String BATTERYLEVEL = "batteryLevel";
    public static final String FINDMYPHONE = "findMyPhone";
    public static final String LOCATION = "location";
    public static final String LOCATIONACCURACY = "locationAccuracy";
    public static final String LOCATIONLASTUPDATE = "locationLastUpdate";
    public static final String DISTANCEFROMHOME = "distanceFromHome";
    public static final String ADDRESSSTREET = "addressStreet";
    public static final String ADDRESSCITY = "addressCity";
    public static final String ADDRESSCOUNTRY = "addressCountry";
    public static final String FORMATTEDADDRESS = "formattedAddress";
    public static final String DEVICENAME = "deviceName";

    // Properties
    public static final String IDPROPERTY = "id";
}
