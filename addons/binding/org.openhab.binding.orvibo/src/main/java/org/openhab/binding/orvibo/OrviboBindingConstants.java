/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.orvibo;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link OrviboBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Daniel Walters - Initial contribution
 */
public class OrviboBindingConstants {

    public static final String BINDING_ID = "orvibo";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_S20 = new ThingTypeUID(BINDING_ID, "s20");
	public final static ThingTypeUID THING_TYPE_ALLONE = new ThingTypeUID(BINDING_ID, "allone");

    // List of all Channel ids
    public final static String CHANNEL_S20_SWITCH = "power";
	public final static String CHANNEL_ALLONE_LEARN = "learn";
    public final static String CHANNEL_ALLONE_LEARN_NAME = "learnName";
    public final static String CHANNEL_ALLONE_EMIT = "emit";

    // List of all Config properties
    public static final String CONFIG_PROPERTY_DEVICE_ID = "deviceId";
	public final static String CONFIG_PROPERTY_ROOT = "rootFolder";
	
}
