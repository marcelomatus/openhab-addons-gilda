/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.loxone;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * Common constants used across the whole binding.
 *
 * @author Pawel Pieczul - Initial contribution
 */
public class LoxoneBindingConstants {

    public static final String BINDING_ID = "loxone";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_MINISERVER = new ThingTypeUID(BINDING_ID, "miniserver");

    // List of all Channel ids
    public static final String CHANNEL_SWITCH_COMMAND = "command";

    // Miniserver properties and parameters
    public static final String MINISERVER_PARAM_HOST = "host";
    public static final String MINISERVER_PARAM_PORT = "port";
    public static final String MINISERVER_PROPERTY_SERIAL = "Serial number";
    public static final String MINISERVER_PROPERTY_MINISERVER_NAME = "Miniserver name";
    public static final String MINISERVER_PROPERTY_PROJECT_NAME = "Project name";
    public static final String MINISERVER_PROPERTY_CLOUD_ADDRESS = "Cloud address";

}
