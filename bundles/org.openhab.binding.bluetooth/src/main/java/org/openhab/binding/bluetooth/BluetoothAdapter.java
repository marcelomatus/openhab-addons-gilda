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
package org.openhab.binding.bluetooth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.registry.Identifiable;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * The {@link BluetoothAdapter} class defines the standard adapter API that must be implemented by bridge handlers,
 * which are then required to be registered as an BluetoothAdapter OSGi service.
 * <p>
 * <b>Scanning</b>
 * The API assumes that the adapter is "always" scanning to enable beacons to be received.
 * The bridge must decide to enable and disable scanning as it needs. This design choice avoids interaction between
 * higher layers where a binding may want to enable scanning while another needs to disable scanning for a specific
 * function (e.g. to connect to a device). The bridge should disable scanning only for the period that is needed.
 *
 * @author Chris Jackson - Initial contribution
 * @author Kai Kreuzer - renamed it, made it identifiable and added listener support
 */
@NonNullByDefault
public interface BluetoothAdapter extends Identifiable<ThingUID> {

    /**
     * Adds a {@link BluetoothDiscoveryListener} to the adapter
     *
     * @param listener the listener to add
     */
    void addDiscoveryListener(BluetoothDiscoveryListener listener);

    /**
     * Removes a {@link BluetoothDiscoveryListener} from the adapter
     *
     * @param listener the listener to remove
     */
    void removeDiscoveryListener(@Nullable BluetoothDiscoveryListener listener);

    /**
     * Starts an active scan on the Bluetooth interface.
     */
    void scanStart();

    /**
     * Stops an active scan on the Bluetooth interface
     */
    void scanStop();

    /**
     * Gets the {@link BluetoothAddress} of the adapter
     *
     * @return the {@link BluetoothAddress} of the adapter
     * @throws IllegalStateException if the adapter is not initialized
     */
    BluetoothAddress getAddress();

    /**
     * Gets the {@link BluetoothDevice} given the {@link BluetoothAddress}.
     * A {@link BluetoothDevice} will always be returned for a valid hardware address, even if this adapter has never
     * seen that device.
     *
     * @param address the {@link BluetoothAddress} to retrieve
     * @return the {@link BluetoothDevice}
     * @throws IllegalArgumentException if the address is no valid hardware address
     */
    BluetoothDevice getDevice(BluetoothAddress address);

}
