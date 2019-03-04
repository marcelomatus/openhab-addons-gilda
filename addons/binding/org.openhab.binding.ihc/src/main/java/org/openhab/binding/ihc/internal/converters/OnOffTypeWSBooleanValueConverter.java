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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSBooleanValue;

/**
 * OnOffType <-> WSBooleanValue converter.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class OnOffTypeWSBooleanValueConverter implements Converter<WSBooleanValue, OnOffType> {

    @Override
    public OnOffType convertFromResourceValue(WSBooleanValue from, ConverterAdditionalInfo convertData)
            throws NumberFormatException {
        return from.booleanValue() ^ convertData.getInverted() ? OnOffType.ON : OnOffType.OFF;
    }

    @Override
    public WSBooleanValue convertFromOHType(OnOffType from, WSBooleanValue value, ConverterAdditionalInfo convertData)
            throws NumberFormatException {
        value.setValue(from == OnOffType.ON ^ convertData.getInverted());
        return value;
    }
}
