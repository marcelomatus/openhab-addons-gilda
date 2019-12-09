/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.internal.kostal.inverter.secondgeneration;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link SecondGenerationDxsEntriesConfiguration} class defines dxsEntries, which are
 * used in the second generation part of the binding.
 *
 * @author Örjan Backsell - Initial contribution Piko1020, Piko New Generation
 */
public class SecondGenerationDxsEntriesConfiguration {

    public static String[] getDxsEntriesConfiguration() {
        List<String> dxsEntriesConfigurationList = new ArrayList<>(24);
        dxsEntriesConfigurationList.add("ChargeTimeEnd");
        dxsEntriesConfigurationList.add("33556236");
        dxsEntriesConfigurationList.add("BatteryType");
        dxsEntriesConfigurationList.add("33556252");
        dxsEntriesConfigurationList.add("BatteryUsageConsumption");
        dxsEntriesConfigurationList.add("33556249");
        dxsEntriesConfigurationList.add("BatteryUsageStrategy");
        dxsEntriesConfigurationList.add("83888896");
        dxsEntriesConfigurationList.add("SmartBatteryControl");
        dxsEntriesConfigurationList.add("33556484");
        dxsEntriesConfigurationList.add("SmartBatteryControl_Text");
        dxsEntriesConfigurationList.add("33556484");
        dxsEntriesConfigurationList.add("BatteryChargeTimeFrom");
        dxsEntriesConfigurationList.add("33556239");
        dxsEntriesConfigurationList.add("BatteryChargeTimeTo");
        dxsEntriesConfigurationList.add("33556240");
        dxsEntriesConfigurationList.add("MaxDepthOfDischarge");
        dxsEntriesConfigurationList.add("33556247");
        dxsEntriesConfigurationList.add("ShadowManagement");
        dxsEntriesConfigurationList.add("33556483");
        dxsEntriesConfigurationList.add("ExternalModuleControl");
        dxsEntriesConfigurationList.add("33556482");
        dxsEntriesConfigurationList.add("InverterName");
        dxsEntriesConfigurationList.add("16777984");

        // Create an dxsEntries array
        String[] dxsEntriesConfiguration = new String[24];

        return dxsEntriesConfiguration;
    }
}
