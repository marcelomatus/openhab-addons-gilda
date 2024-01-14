/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.boschshc.internal.devices.plug;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.boschshc.internal.devices.AbstractPowerSwitchHandler;
import org.openhab.core.thing.Thing;

/**
 * A handler for compact smart plugs.
 *
 * @author David Pace - Initial contribution
 */
@NonNullByDefault
public class PlugHandler extends AbstractPowerSwitchHandler {

    public PlugHandler(Thing thing) {
        super(thing);
    }
}
