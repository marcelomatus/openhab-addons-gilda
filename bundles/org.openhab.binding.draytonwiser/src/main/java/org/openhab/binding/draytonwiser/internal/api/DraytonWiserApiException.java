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
package org.openhab.binding.draytonwiser.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception thrown in case of api problems.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class DraytonWiserApiException extends RuntimeException {

    public DraytonWiserApiException(final String message) {
        super(message);
    }

    public DraytonWiserApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
