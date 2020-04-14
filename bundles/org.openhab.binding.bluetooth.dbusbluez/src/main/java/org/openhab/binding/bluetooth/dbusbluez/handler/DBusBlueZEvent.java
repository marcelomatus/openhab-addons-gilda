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
package org.openhab.binding.bluetooth.dbusbluez.handler;

import org.openhab.binding.bluetooth.BluetoothAddress;

/**
 *
 * @author blafois
 *
 */
public class DBusBlueZEvent {

    public enum EVENT_TYPE {
        RSSI_UPDATE,
        CHARACTERISTIC_NOTIFY,
        MANUFACTURER_DATA,
        CONNECTED,
        TXPOWER,
        NAME,
        SERVICES_RESOLVED,
    }

    private EVENT_TYPE eventType;
    private BluetoothAddress device;

    public DBusBlueZEvent(EVENT_TYPE eventType, BluetoothAddress device) {
        this.eventType = eventType;
        this.device = device;
    }

    public EVENT_TYPE getEventType() {
        return eventType;
    }

    public BluetoothAddress getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return "EventType: " + eventType + ", Device: " + device;
    }

}
