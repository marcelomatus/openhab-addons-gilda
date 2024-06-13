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
package org.openhab.binding.rainsoft.internal.errors;

/**
 * IllegalDeviceClassException will be thrown if an device is retrieved
 * from the RainSoftDeviceRegistry and the class is not as expected.
 *
 * @author Ben Rosenblum - Initial contribution
 */

public class IllegalDeviceClassException extends Exception {

    private static final long serialVersionUID = -4010587859949508962L;

    public IllegalDeviceClassException(String message) {
        super(message);
    }
}
