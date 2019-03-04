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

import org.eclipse.smarthome.core.library.types.UpDownType;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSBooleanValue;

/**
 * UpDownType <-> WSBooleanValue converter.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class UpDownTypeWSBooleanValueConverter implements Converter<WSBooleanValue, UpDownType> {

    @Override
    public UpDownType convertFromResourceValue(WSBooleanValue from, ConverterAdditionalInfo convertData)
            throws NumberFormatException {
        return from.booleanValue() ^ convertData.getInverted() ? UpDownType.UP : UpDownType.DOWN;
    }

    @Override
    public WSBooleanValue convertFromOHType(UpDownType from, WSBooleanValue value, ConverterAdditionalInfo convertData)
            throws NumberFormatException {
        value.setValue(from == UpDownType.UP ^ convertData.getInverted());
        return value;
    }
}
