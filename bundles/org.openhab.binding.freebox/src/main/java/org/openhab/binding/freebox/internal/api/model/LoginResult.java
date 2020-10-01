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
package org.openhab.binding.freebox.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LoginResult} is the Java class used to map the
 * structure used by the response of the login API
 * https://dev.freebox.fr/sdk/os/login/#
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class LoginResult {
    private boolean loggedIn;
    private String challenge = "";

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getChallenge() {
        return challenge;
    }
}
