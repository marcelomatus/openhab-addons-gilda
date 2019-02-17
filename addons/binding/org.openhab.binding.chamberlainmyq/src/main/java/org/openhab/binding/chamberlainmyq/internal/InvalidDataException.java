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
package org.openhab.binding.chamberlainmyq.internal;

import java.io.IOException;

/**
 * Throw if the data we are parsing in not what we are expecting for input.
 *
 * @author Dan Cunningham - Initial contribution
 * @author Scott Hanson - 2.x Binding
 *
 */
public class InvalidDataException extends IOException {

    private static final long serialVersionUID = 1L;

    public InvalidDataException(String message) {
        super(message);
    }
}
