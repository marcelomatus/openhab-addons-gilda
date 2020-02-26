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
package org.openhab.binding.hive.internal.client.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Thrown to indicate a call to the Hive API has failed because the client is not authorised.
 *
 * @author Ross Brown - Initial contribution
 */
@NonNullByDefault
public final class HiveApiNotAuthorisedException extends HiveApiException {
    private static final long serialVersionUID = 1L;

    public HiveApiNotAuthorisedException() {
        super();
    }

    public HiveApiNotAuthorisedException(final String message) {
        super(message);
    }

    public HiveApiNotAuthorisedException(final Throwable cause) {
        super(cause);
    }

    public HiveApiNotAuthorisedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
