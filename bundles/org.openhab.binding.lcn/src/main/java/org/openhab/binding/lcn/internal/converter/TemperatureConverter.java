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
package org.openhab.binding.lcn.internal.converter;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.unit.SIUnits;

/**
 * Converts the native LCN value to temperature.
 *
 * @author Fabian Wolter - Initial Contribution
 */
@NonNullByDefault
public class TemperatureConverter extends AbstractVariableValueConverter {
    @Override
    protected Unit<?> getUnitType() {
        // The user can also configure "°F" instead of "°C" in the item. The value will be converted accordingly.
        return SIUnits.CELSIUS;
    }

    @Override
    public int toNative(double value) {
        return (int) (Math.round(value * 10) + 1000);
    }

    @Override
    public double toHumanReadable(long value) {
        return (value - 1000) / 10d;
    }
}
