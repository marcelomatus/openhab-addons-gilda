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
 *
 */
package org.openhab.binding.goveelan.internal.model;

/**
 *
 * @param onOff on=1 off=0
 * @param brightness brightness
 * @param color rgb Color
 * @param colorTemInKelvin color in Kelvin
 *
 *            * @author Stefan Höhn - Initial contribution
 */
public record StatusData(int onOff, int brightness, Color color, int colorTemInKelvin) {
}
