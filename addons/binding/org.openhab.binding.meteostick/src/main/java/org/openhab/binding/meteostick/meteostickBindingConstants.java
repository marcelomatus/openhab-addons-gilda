/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.meteostick;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link meteostickBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Chris Jackson - Initial contribution
 */
public class meteostickBindingConstants {

    public static final String BINDING_ID = "meteostick";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_FINEOFFSET = new ThingTypeUID(BINDING_ID, "meteostick_fineoffset");
    public final static ThingTypeUID THING_TYPE_DAVIS = new ThingTypeUID(BINDING_ID, "meteostick_davis");

    // List of all Channel ids
    public final static String CHANNEL_TEMPERATURE = "temperature";
    public final static String CHANNEL_HUMIDITY = "humidity";
    public final static String CHANNEL_PRESSURE = "pressure";

}
