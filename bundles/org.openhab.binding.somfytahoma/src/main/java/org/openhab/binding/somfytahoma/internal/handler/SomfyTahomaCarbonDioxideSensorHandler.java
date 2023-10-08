/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.somfytahoma.internal.handler;

import static org.openhab.binding.somfytahoma.internal.SomfyTahomaBindingConstants.CO2_CONCENTRATION;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;

/**
 * The {@link SomfyTahomaCarbonDioxideSensorHandler} is responsible for handling commands,
 * which are sent to one of the channels of the carbon dioxide sensor thing.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class SomfyTahomaCarbonDioxideSensorHandler extends SomfyTahomaBaseThingHandler {

    public SomfyTahomaCarbonDioxideSensorHandler(Thing thing) {
        super(thing);
        stateNames.put(CO2_CONCENTRATION, "core:CO2ConcentrationState");
    }
}
