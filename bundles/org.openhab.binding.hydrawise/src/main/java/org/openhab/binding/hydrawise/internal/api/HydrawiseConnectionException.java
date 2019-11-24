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
package org.openhab.binding.hydrawise.internal.api;

/**
 * Thrown for connection issues to the Hydrawise controller
 *
 * @author Dan Cunningham - Initial contribution
 */
@SuppressWarnings("serial")
public class HydrawiseConnectionException extends Exception {

    public HydrawiseConnectionException(Exception e) {
        super(e);
    }

    public HydrawiseConnectionException(String message) {
        super(message);
    }
}
