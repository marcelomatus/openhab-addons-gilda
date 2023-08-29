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
package org.openhab.binding.solax.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link SinglePhaseInverterData} interface should be implemented for any particular bean that returns the parsed
 * data in a human readable code and format for a single-phased inverter.
 *
 * @author Konstantin Polihronov - Initial contribution
 */
@NonNullByDefault
public interface SinglePhaseInverterData extends InverterData {

    double getInverterVoltage();

    double getInverterCurrent();

    short getInverterOutputPower();

    double getInverterFrequency();
}
