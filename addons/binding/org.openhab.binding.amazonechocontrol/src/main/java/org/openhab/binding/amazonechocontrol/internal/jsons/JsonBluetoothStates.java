/**
 * Copyright (c) 2014-2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.amazonechocontrol.internal.jsons;

import org.openhab.binding.amazonechocontrol.internal.jsons.JsonDevices.Device;

/**
 * The {@link JsonBluetoothStates} encapsulate the GSON data of bluetooth state
 *
 * @author Michael Geramb - Initial contribution
 */
public class JsonBluetoothStates {

    public BluetoothState findStateByDevice(Device device) {
        if (device == null) {
            return null;
        }
        if (bluetoothStates == null) {
            return null;
        }
        for (BluetoothState state : bluetoothStates) {
            if (state.deviceSerialNumber != null && state.deviceSerialNumber.equals(device.serialNumber)) {
                return state;
            }
        }
        return null;
    }

    public BluetoothState[] bluetoothStates;

    public class PairedDevice {
        public String address;
        public boolean connected;
        public String deviceClass;
        public String friendlyName;
        // "profiles":[
        // "AVRCP",
        // "A2DP-SINK"
        // ]
    }

    public class BluetoothState {
        public String deviceSerialNumber;
        public String deviceType;
        public String friendlyName;
        public boolean gadgetPaired;
        public boolean online;
        public PairedDevice[] pairedDeviceList;

    }
}
