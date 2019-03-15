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
package org.openhab.binding.ihc.internal.converters;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.ihc.internal.ws.exeptions.ConversionException;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSIntegerValue;

/**
 * DecimalType <-> WSIntegerValue converter.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class DecimalTypeWSIntegerValueConverter implements Converter<WSIntegerValue, DecimalType> {

    @Override
    public DecimalType convertFromResourceValue(@NonNull WSIntegerValue from,
            @NonNull ConverterAdditionalInfo convertData) throws ConversionException {
        return new DecimalType(from.getInteger());
    }

    @Override
    public WSIntegerValue convertFromOHType(@NonNull DecimalType from, @NonNull WSIntegerValue value,
            @NonNull ConverterAdditionalInfo convertData) throws ConversionException {
        if (from.intValue() >= value.getMinimumValue() && from.intValue() <= value.getMaximumValue()) {
            WSIntegerValue v = new WSIntegerValue(value);
            v.setInteger(from.intValue());
            return v;
        } else {
            throw new ConversionException("Value is not between acceptable limits (min=" + value.getMinimumValue()
                    + ", max=" + value.getMaximumValue() + ")");
        }
    }
}
