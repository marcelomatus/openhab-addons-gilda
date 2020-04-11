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
package org.openhab.binding.smarther.internal.api.model;

import java.util.Optional;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.smarther.internal.api.model.Enums.MeasureUnit;

import com.google.gson.annotations.SerializedName;

import tec.uom.se.unit.Units;

/**
 * Smarther API Measure DTO class.
 *
 * @author Fabio Possieri - Initial contribution
 */
public class Measure {

    @SerializedName("timeStamp")
    private String timestamp;
    private String value;
    private String unit;

    public String getTimestamp() {
        return timestamp;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public MeasureUnit getMeasureUnit() {
        return MeasureUnit.fromValue(unit);
    }

    public State toState() {
        State state = UnDefType.UNDEF;
        final Optional<Double> optValue = (StringUtils.isBlank(value)) ? Optional.empty()
                : Optional.of(Double.parseDouble(value));

        switch (MeasureUnit.fromValue(unit)) {
            case CELSIUS:
                state = optValue.<State>map(t -> new QuantityType<Temperature>(new DecimalType(t), SIUnits.CELSIUS))
                        .orElse(UnDefType.UNDEF);
                break;
            case FAHRENHEIT:
                state = optValue
                        .<State>map(t -> new QuantityType<Temperature>(new DecimalType(t), ImperialUnits.FAHRENHEIT))
                        .orElse(UnDefType.UNDEF);
                break;
            case PERCENTAGE:
                state = optValue.<State>map(t -> new QuantityType<Dimensionless>(new DecimalType(t), Units.PERCENT))
                        .orElse(UnDefType.UNDEF);
                break;
            case DIMENSIONLESS:
                state = optValue.<State>map(t -> new DecimalType(t)).orElse(UnDefType.UNDEF);
        }

        return state;
    }

    @Override
    public String toString() {
        return (StringUtils.isBlank(timestamp)) ? String.format("value=%s, unit=%s", value, unit)
                : String.format("value=%s, unit=%s, timestamp=%s", value, unit, timestamp);
    }

}
