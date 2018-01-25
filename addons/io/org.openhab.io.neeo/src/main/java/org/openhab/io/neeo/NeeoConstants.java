/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.neeo;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.ConfigConstants;

/**
 * The constants class for the NEEO Transport
 *
 * @author Tim Roberts - Initial contribution
 */
public class NeeoConstants {

    /** Binding ID/Thing Types for transport created things - must match app.js */
    public static final @NonNull String NEEOBINDING_BINDING_ID = "neeo";
    public static final @NonNull String NEEOBINDING_DEVICE_ID = NEEOBINDING_BINDING_ID + ":device";
    public static final String NEEOIO_BINDING_ID = "neeo_io";
    public static final String VIRTUAL_THING_TYPE = "virtual";

    /** Constant used to identify thread pool name */
    public static final String THREAD_POOL_NAME = "neeoio";

    /** Constants used for the Web APP */
    public static final String WEBAPP_PREFIX = "/neeo";
    static final String WEBAPP_STATUS_PREFIX = "/neeostatus";

    /** The MDNS type for neeo */
    public static final String NEEO_MDNS_TYPE = "_neeo._tcp.local.";

    /** The constants used for configuration */
    public static final String CFG_EXPOSE_ALL = "exposeAll";
    public static final String CFG_EXPOSENEEOBINDING = "exposeNeeoBinding";
    public static final String CFG_CHECKSTATUSINTERVAL = "checkStatusInterval";
    public static final String CFG_LOCALIPADDRESS = "localIpAddress";
    public static final String CFG_SEARCHLIMIT = "searchLimit";

    /** The name of the adapter */
    public static final String ADAPTER_NAME = "openHAB";

    /** The default port the brain listens on. */
    public static final int DEFAULT_BRAIN_PORT = 3000;

    /** The default protocol for the brain. */
    public static final String PROTOCOL = "http://";

    /** The device definitions file name */
    public static final String FILENAME_DEVICEDEFINITIONS = ConfigConstants.getUserDataFolder() + File.separator
            + "neeo" + File.separator + "neeodefinitions.json";
    public static final String FILENAME_DISCOVEREDBRAINS = ConfigConstants.getUserDataFolder() + File.separator + "neeo"
            + File.separator + "discoveredbrains.json";

    /** The search threshold value */
    public static final double SEARCH_MATCHFACTOR = 0.5;

    /** Various brain URLs */
    private static final String NEEO_VERSION = "/v1";
    public static final String REGISTER_SDK_ADAPTER = NEEO_VERSION + "/api/registerSdkDeviceAdapter";
    public static final String UNREGISTER_SDK_ADAPTER = NEEO_VERSION + "/api/unregisterSdkDeviceAdapter";
    public static final String PROJECTS_HOME = NEEO_VERSION + "/projects/home";
    public static final String RECIPES = NEEO_VERSION + "/api/recipes";
    public static final String SYSTEMINFO = NEEO_VERSION + "/systeminfo";
    public static final String IDENTBRAIN = SYSTEMINFO + "/identbrain";
    public static final String NOTIFICATION = NEEO_VERSION + "/notifications";
    public static final String CAPABILITY_PATH_PREFIX = "/device";
}
