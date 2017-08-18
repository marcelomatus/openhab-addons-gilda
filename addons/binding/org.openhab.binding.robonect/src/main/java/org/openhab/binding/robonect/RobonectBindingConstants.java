/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.robonect;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link RobonectBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marco Meyer - Initial contribution
 */
public class RobonectBindingConstants {

    private static final String BINDING_ID = "robonect";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_AUTOMOWER = new ThingTypeUID(BINDING_ID, "mower");

    // List of all Channel ids
    public static final String CHANNEL_MOWER_NAME = "mowerInfo#name";
    public static final String CHANNEL_STATUS_BATTERY = "mowerStatus#battery";
    public static final String CHANNEL_STATUS_DURATION = "mowerStatus#duration";
    public static final String CHANNEL_STATUS_HOURS = "mowerStatus#hours";
    public static final String CHANNEL_STATUS_MODE = "mowerStatus#mode";
    public static final String CHANNEL_STATUS = "mowerStatus#status";
    public static final String CHANNEL_MOWER_STATUS_STARTED = "mowerStatus#started";
    public static final String CHANNEL_MOWER_STATUS_OFFLINE_TRIGGER = "mowerStatus#offlineTrigger";
    public static final String CHANNEL_TIMER_STATUS = "timer#status";
    public static final String CHANNEL_TIMER_NEXT_TIMER = "timer#nextTimer";
    public static final String CHANNEL_WLAN_SIGNAL = "wlan#signal";
    
    public static final String CHANNEL_VERSION_SERIAL = "version#serial";
    public static final String CHANNEL_VERSION_VERSION = "version#version";
    public static final String CHANNEL_VERSION_COMPILED = "version#compiled";
    public static final String CHANNEL_VERSION_COMMENT = "version#comment";
    
    public static final String CHANNEL_JOB_REMOTE_START = "job#remoteStart";
    public static final String CHANNEL_JOB_AFTER_MODE = "job#afterMode";
    public static final String CHANNEL_JOB_START = "job#start";
    public static final String CHANNEL_JOB_END = "job#end";
    
    public static final String CHANNEL_ERROR_CODE = "error#code";
    public static final String CHANNEL_ERROR_MESSAGE = "error#message";
    public static final String CHANNEL_ERROR_DATE = "error#date";
    
    public static final String CHANNEL_LAST_ERROR_CODE = "lastError#code";
    public static final String CHANNEL_LAST_ERROR_MESSAGE = "lastError#message";
    public static final String CHANNEL_LAST_ERROR_DATE = "lastError#date";
    
    public static final String CHANNEL_HEALTH_TEMP = "health#temperature";
    public static final String CHANNEL_HEALTH_HUM = "health#humidity";

}
