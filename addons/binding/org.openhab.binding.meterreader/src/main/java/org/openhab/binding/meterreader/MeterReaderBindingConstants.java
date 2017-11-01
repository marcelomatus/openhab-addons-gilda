/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.meterreader;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link SmlReaderBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Matthias Steigenberger - Initial contribution
 */
public class MeterReaderBindingConstants {

    public static final String BINDING_ID = "meterreader";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SMLREADER = new ThingTypeUID(BINDING_ID, "meter");

    public static final String CONFIGURATION_PORT = "port";

    public static final String CONFIGURATION_SERIAL_MODE = "mode";

    public static final String CONFIGURATION_BAUDRATE = "baudrate";

    public static final String CONFIGURATION_INIT_MESSAGE = "initMessage";

    public static final String CONFIGURATION_CONVERSION = "conversionRatio";

    public static final String CHANNEL_PROPERTY_OBIS = "obis";

    /** Obis format */
    public static final String OBIS_FORMAT = "%d-%d:%d.%d.%d";

    public static final String CHANNEL_TYPE_NUMBER = "NumberChannel";
    public static final String CHANNEL_TYPE_STRING = "StringChannel";
}
