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
package org.openhab.binding.shieldtv.internal.protocol.shieldtv;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface defining callback routines used by LeapMessageParser
 *
 * @author Ben Rosenblum - Initial contribution
 */
@NonNullByDefault
public interface ShieldTVMessageParserCallbacks {

    public void validMessageReceived();

    public void checkInitialized();

    public void setHostName(String hostName);

    public String getHostName();

    public void setCurrentApp(String currentApp);

    public String getCurrentApp();

    public void setLoggedIn(boolean isLoggedIn);

    public void setKeys(String privKey, String cert);

    public boolean getLoggedIn();
}
