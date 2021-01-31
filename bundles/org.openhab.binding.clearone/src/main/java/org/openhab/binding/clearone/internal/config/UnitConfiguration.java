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
package org.openhab.binding.clearone.internal.config;

/**
 * Configuration class for the ClearOne Unit Thing.
 *
 * @author Garry Mitchell - Initial contribution
 */

public class UnitConfiguration {

    // Unit Thing constants
    public static final String UNIT_NUMBER = "deviceId";
    public static final String UNIT_TYPE = "typeId";

    public String deviceId;
    public String typeId;
}
