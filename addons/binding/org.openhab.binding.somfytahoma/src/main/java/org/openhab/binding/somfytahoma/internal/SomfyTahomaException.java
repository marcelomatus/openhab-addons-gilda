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
package org.openhab.binding.somfytahoma.internal;

/**
 * The {@link SomfyTahomaException} represents an exception in the response of
 * the TahomaLink cloud service.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class SomfyTahomaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SomfyTahomaException(String message) {
        super(message);
    }

    public SomfyTahomaException(final Throwable cause) {
        super(cause);
    }

    public SomfyTahomaException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
