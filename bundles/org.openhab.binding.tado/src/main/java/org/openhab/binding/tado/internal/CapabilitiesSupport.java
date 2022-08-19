/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.tado.internal;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tado.internal.api.model.AcModeCapabilities;
import org.openhab.binding.tado.internal.api.model.AirConditioningCapabilities;
import org.openhab.binding.tado.internal.api.model.GenericZoneCapabilities;
import org.openhab.binding.tado.internal.api.model.TadoSystemType;

/**
 * The {@link CapabilitiesSupport} class checks the given {@link GenericZoneCapabilities} parameter, and iterates over
 * all its mode specific sub-capabilities to check which type of channels are needed in a thing that is to be built
 * around the respective capabilities.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class CapabilitiesSupport {
    private final TadoSystemType type;
    private boolean light;
    private boolean swing;
    private boolean fanLevel;
    private boolean fanSpeed;
    private boolean verticalSwing;
    private boolean horizontalSwing;

    public CapabilitiesSupport(GenericZoneCapabilities capabilities) {
        type = capabilities.getType();
        if (!(capabilities instanceof AirConditioningCapabilities)) {
            return;
        }

        AirConditioningCapabilities acCapabilities = (AirConditioningCapabilities) capabilities;

        // @formatter:off
        Stream<@Nullable AcModeCapabilities> allCapabilities = Stream.of(
               acCapabilities.getCOOL(),
               acCapabilities.getDRY(),
               acCapabilities.getHEAT(),
               acCapabilities.getFAN(),
               acCapabilities.getAUTO());
        // @formatter:on

        // iterate over all mode capability elements and build the superset of their inner capabilities
        allCapabilities.forEach(e -> {
            if (e != null) {
                light |= e.getLight() != null ? e.getLight().size() > 0 : false;
                swing |= e.getSwings() != null ? e.getSwings().size() > 0 : false;
                fanLevel |= e.getFanLevel() != null ? e.getFanLevel().size() > 0 : false;
                fanSpeed |= e.getFanSpeeds() != null ? e.getFanSpeeds().size() > 0 : false;
                verticalSwing |= e.getVerticalSwing() != null ? e.getVerticalSwing().size() > 0 : false;
                horizontalSwing |= e.getHorizontalSwing() != null ? e.getHorizontalSwing().size() > 0 : false;
            }
        });
    }

    public boolean fanLevel() {
        return fanLevel;
    }

    public boolean fanSpeed() {
        return fanSpeed;
    }

    public boolean horizontalSwing() {
        return horizontalSwing;
    }

    public boolean light() {
        return light;
    }

    public boolean swing() {
        return swing;
    }

    public boolean verticalSwing() {
        return verticalSwing;
    }

    public boolean acPower() {
        return type == TadoSystemType.AIR_CONDITIONING;
    }

    public boolean heatingPower() {
        return type == TadoSystemType.HEATING;
    }

    public boolean currentTemperature() {
        return (type == TadoSystemType.AIR_CONDITIONING) || (type == TadoSystemType.HEATING);
    }

    public boolean humidity() {
        return (type == TadoSystemType.AIR_CONDITIONING) || (type == TadoSystemType.HEATING);
    }
}
