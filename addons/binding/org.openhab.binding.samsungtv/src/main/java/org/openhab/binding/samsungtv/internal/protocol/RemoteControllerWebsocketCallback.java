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
package org.openhab.binding.samsungtv.internal.protocol;

import java.util.List;

/**
 * Callback from the websocket remote controller
 *
 * @author Arjan Mels - Initial contribution
 */

public interface RemoteControllerWebsocketCallback {

    void appsUpdated(List<String> apps);

    void currentAppUpdated(String app);

    void powerUpdated(boolean on, boolean artmode);

    void connectionError(Throwable error);
}
