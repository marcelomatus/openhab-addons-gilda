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
package org.openhab.binding.yioremote.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YIOremoteDockWebsocketInterface} is responsible for interfacing the Websocket.
 *
 * @author Michael Loercher - Initial contribution
 */
@NonNullByDefault
public interface YIOremoteDockWebsocketInterface {

    public void onConnect(boolean connected);

    public void onMessage(String decodedmessage);

    public void onError();
}
