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
package org.openhab.binding.bluetooth.dbusbluez.handler.events;

import org.openhab.binding.bluetooth.BluetoothAddress;
import org.openhab.binding.bluetooth.dbusbluez.handler.DBusBlueZEvent;

/**
 *
 * @author blafois
 *
 */
public class ConnectedEvent extends DBusBlueZEvent {

    private boolean connected;

    public ConnectedEvent(BluetoothAddress device, boolean connected) {
        super(EVENT_TYPE.CONNECTED, device);
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }

}
