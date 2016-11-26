/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.edimax;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link EdimaxBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Falk Harnisch - Initial contribution
 */
public class EdimaxBindingConstants {

    public static final String BINDING_ID = "edimax";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SP1101W = new ThingTypeUID(BINDING_ID, "sp1101w");
    public static final ThingTypeUID THING_TYPE_SP2101W = new ThingTypeUID(BINDING_ID, "sp2101w");

    // List of all Channel ids
    public static final String SWITCH = "switch";
    public static final String CURRENT = "current";
    public static final String POWER = "power";

}
