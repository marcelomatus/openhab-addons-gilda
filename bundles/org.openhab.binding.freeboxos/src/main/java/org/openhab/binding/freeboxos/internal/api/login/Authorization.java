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
package org.openhab.binding.freeboxos.internal.api.login;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Authorization} is used to map the structure used by the response of the request authorization API
 *
 * https://dev.freebox.fr/sdk/os/login/#
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
class Authorization {
    private @Nullable String appToken;
    private int trackId;

    public String getAppToken() {
        return Objects.requireNonNull(appToken);
    }

    public int getTrackId() {
        return trackId;
    }
}
