/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.timer;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link TimerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Neil Renaud - Initial contribution
 */
public class TimerBindingConstants {

    private static final String BINDING_ID = "timer";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "sample");

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

}
