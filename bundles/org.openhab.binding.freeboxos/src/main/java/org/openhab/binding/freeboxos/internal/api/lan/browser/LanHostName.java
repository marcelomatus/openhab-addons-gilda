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
package org.openhab.binding.freeboxos.internal.api.lan.browser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.api.ApiConstants.HostNameSource;

/**
 * The {@link LanHostName} is the Java class used to map the "LanHostName" structure used by the Lan Hosts Browser API
 *
 * https://dev.freebox.fr/sdk/os/lan/#lan-browser
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class LanHostName {
    private @Nullable String name;
    private HostNameSource source = HostNameSource.UNKNOWN;

    public @Nullable String getName() {
        return name;
    }

    public HostNameSource getSource() {
        return source;
    }
}
