/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.modbus.internal.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.profiles.ProfileTypeBuilder;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfileType;

/**
 * Modbus profile constants.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public interface ModbusProfiles {

    public static final String MODBUS_SCOPE = "modbus";

    ProfileTypeUID BIT = new ProfileTypeUID(MODBUS_SCOPE, "bit");
    ProfileTypeUID GAIN_OFFSET = new ProfileTypeUID(MODBUS_SCOPE, "gainOffset");

    StateProfileType BIT_MASK_TYPE = ProfileTypeBuilder.newState(BIT, "Bit profile").build();
    StateProfileType GAIN_OFFSET_TYPE = ProfileTypeBuilder.newState(GAIN_OFFSET, "Gain-Offset Correction").build();
}
