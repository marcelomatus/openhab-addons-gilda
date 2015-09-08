/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ModbusBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author vores8 - Initial contribution
 */
public class ModbusBindingConstants {

    public static final String BINDING_ID = "modbus";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_SLAVE = new ThingTypeUID(BINDING_ID, "slave");
    public final static ThingTypeUID THING_TYPE_TCP = new ThingTypeUID(BINDING_ID, "tcp");
    public final static ThingTypeUID THING_TYPE_SERIAL = new ThingTypeUID(BINDING_ID, "serial");
    public final static ThingTypeUID THING_TYPE_ENDPOINT = new ThingTypeUID(BINDING_ID, "endpoint");

    public final static String PROP_READ = "read";
    public final static String PROP_WRITE = "write";
    public final static String PROP_TYPE = "type";

    // List of all Channel ids
    // public final static String CHANNEL_1 = "channel1";

}
