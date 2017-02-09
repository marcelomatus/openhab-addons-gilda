/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.avmfritz;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

/**
 * This class defines common constants, which are used
 * across the whole binding.
 *
 * @author Robert Bausdorf
 *
 */
public class BindingConstants {

    public static final String BINDING_ID = "avmfritz";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String BRIDGE_FRITZBOX = "fritzbox";
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String DEVICE_ID = "deviceId";
    public static final String BRIDGE_MODEL_NAME = "FRITZ!Box";
    public static final String POWERLINE_MODEL_NAME = "FRITZ!Powerline";
    public static final String THING_AIN = "ain";

    // List of main device types
    public static final String DEVICE_DECT210 = "FRITZ_DECT_210";
    public static final String DEVICE_DECT200 = "FRITZ_DECT_200";
    public static final String DEVICE_DECT100 = "FRITZ_DECT_Repeater_100";
    public static final String DEVICE_PL546E = "FRITZ_Powerline_546E";
    public static final String DEVICE_PL1240E_STANDALONE = "FRITZ_Powerline_1240E_Solo";
    public static final String DEVICE_PL1220E_STANDALONE = "FRITZ_Powerline_1220E_Solo";
    public static final String DEVICE_PL1000E_STANDALONE = "FRITZ_Powerline_100E_Solo";
    public static final String DEVICE_PL546E_STANDALONE = "FRITZ_Powerline_546E_Solo";
    public static final String DEVICE_PL540E_STANDALONE = "FRITZ_Powerline_540E_Solo";
    public static final String DEVICE_PL530E_STANDALONE = "FRITZ_Powerline_530E_Solo";
    public static final String DEVICE_PL510E_STANDALONE = "FRITZ_Powerline_510E_Solo";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID BRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, BRIDGE_FRITZBOX);
    public final static ThingTypeUID POWERLINE_THING_TYPE = new ThingTypeUID(BINDING_ID, "Powerline_Solo");

    public final static ThingTypeUID DECT210_THING_TYPE = new ThingTypeUID(BINDING_ID, DEVICE_DECT210);
    public final static ThingTypeUID DECT200_THING_TYPE = new ThingTypeUID(BINDING_ID, DEVICE_DECT200);
    public final static ThingTypeUID DECT100_THING_TYPE = new ThingTypeUID(BINDING_ID, DEVICE_DECT100);
    public final static ThingTypeUID PL546E_THING_TYPE = new ThingTypeUID(BINDING_ID, DEVICE_PL546E);
    public final static ThingTypeUID PL1240E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
    		DEVICE_PL1240E_STANDALONE);
    public final static ThingTypeUID PL1220E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
    		DEVICE_PL1220E_STANDALONE);
    public final static ThingTypeUID PL1000E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
    		DEVICE_PL1000E_STANDALONE);
    public final static ThingTypeUID PL546E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
            DEVICE_PL546E_STANDALONE);
    public final static ThingTypeUID PL540E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
    		DEVICE_PL540E_STANDALONE);
    public final static ThingTypeUID PL530E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
    		DEVICE_PL530E_STANDALONE);
    public final static ThingTypeUID PL510E_STANDALONE_THING_TYPE = new ThingTypeUID(BINDING_ID,
    		DEVICE_PL510E_STANDALONE);

    // List of all Channel ids
    public final static String CHANNEL_TEMP = "temperature";
    public final static String CHANNEL_ENERGY = "energy";
    public final static String CHANNEL_POWER = "power";
    public final static String CHANNEL_SWITCH = "outlet";

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(DECT100_THING_TYPE,
            DECT200_THING_TYPE, DECT210_THING_TYPE, PL546E_THING_TYPE, BRIDGE_THING_TYPE,
            PL1240E_STANDALONE_THING_TYPE, PL1220E_STANDALONE_THING_TYPE, PL1000E_STANDALONE_THING_TYPE,
            PL546E_STANDALONE_THING_TYPE, PL540E_STANDALONE_THING_TYPE, PL530E_STANDALONE_THING_TYPE,
            PL510E_STANDALONE_THING_TYPE);

    public final static Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = ImmutableSet.of(DECT100_THING_TYPE,
            DECT200_THING_TYPE, DECT210_THING_TYPE, PL546E_THING_TYPE);

    public final static Set<ThingTypeUID> SUPPORTED_POWERLINE_THING_TYPES_UIDS = ImmutableSet.of(
    		PL1240E_STANDALONE_THING_TYPE, PL1220E_STANDALONE_THING_TYPE, PL1000E_STANDALONE_THING_TYPE,
    		PL546E_STANDALONE_THING_TYPE, PL540E_STANDALONE_THING_TYPE, PL530E_STANDALONE_THING_TYPE,
    		PL510E_STANDALONE_THING_TYPE);
    
    public final static Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = ImmutableSet.of(BRIDGE_THING_TYPE,
    		PL1240E_STANDALONE_THING_TYPE, PL1220E_STANDALONE_THING_TYPE, PL1000E_STANDALONE_THING_TYPE,
    		PL546E_STANDALONE_THING_TYPE, PL540E_STANDALONE_THING_TYPE, PL530E_STANDALONE_THING_TYPE,
    		PL510E_STANDALONE_THING_TYPE);

}
