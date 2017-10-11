/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fronius;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link FroniusBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Thomas Rokohl - Initial contribution
 */
public class FroniusBindingConstants {

    private static final String BINDING_ID = "powerinverter";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "fronius");

    // List of all Channel ids
    public static final String InverterDataChannelDayEnergy = "inverterdatachanneldayenergy";
    public static final String InverterDataChannelPac = "inverterdatachannelpac";
    public static final String InverterDataChannelTotal = "inverterdatachanneltotal";
    public static final String InverterDataChannelYear = "inverterdatachannelyear";
    public static final String InverterDataChannelFac = "inverterdatachannelfac";
    public static final String InverterDataChannelIac = "inverterdatachanneliac";
    public static final String InverterDataChannelIdc = "inverterdatachannelidc";
    public static final String InverterDataChannelUac = "inverterdatachanneluac";
    public static final String InverterDataChannelUdc = "inverterdatachanneludc";
    public static final String PowerFlowpGrid = "powerflowchannelpgrid";
    public static final String PowerFlowpLoad = "powerflowchannelpload";
    public static final String PowerFlowpAkku = "powerflowchannelpakku";

}
