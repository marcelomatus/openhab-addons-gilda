/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.airquality.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link Pollutant} enum lists people categories
 * than can be affected by a given pollutant.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public enum SensitiveGroup {
    RESPIRATORY,
    HEART,
    ELDERLY,
    CHILDREN,
    ASTHMA;
}
