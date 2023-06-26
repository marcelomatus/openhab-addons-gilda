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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.api.ApiConstants.Permission;

/**
 * The {@link Session} is the Java class used to map the structure used by the response of the open session API
 *
 * https://dev.freebox.fr/sdk/os/login/#
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class Session {
    private Map<Permission, @Nullable Boolean> permissions = Map.of();
    private @Nullable String sessionToken;

    public @Nullable String getSessionToken() {
        return sessionToken;
    }

    public boolean hasPermission(Permission checked) {
        return Boolean.TRUE.equals(permissions.get(checked));
    }
}
