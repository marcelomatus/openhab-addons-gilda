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
package org.openhab.binding.bluetooth.discovery.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.bluetooth.BluetoothAddress;
import org.openhab.binding.bluetooth.BluetoothBindingConstants;
import org.openhab.binding.bluetooth.BluetoothCharacteristic;
import org.openhab.binding.bluetooth.BluetoothCharacteristic.GattCharacteristic;
import org.openhab.binding.bluetooth.BluetoothCompanyIdentifiers;
import org.openhab.binding.bluetooth.BluetoothCompletionStatus;
import org.openhab.binding.bluetooth.BluetoothDescriptor;
import org.openhab.binding.bluetooth.BluetoothDevice;
import org.openhab.binding.bluetooth.BluetoothDevice.ConnectionState;
import org.openhab.binding.bluetooth.BluetoothDeviceListener;
import org.openhab.binding.bluetooth.discovery.BluetoothDiscoveryParticipant;
import org.openhab.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.openhab.binding.bluetooth.notification.BluetoothScanNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BluetoothDiscoveryProcess} does the work of creating a DiscoveryResult from a set of
 * {@link BluetoothDisocveryParticipant}s
 *
 * @author Connor Petty - Initial Contribution
 */
@NonNullByDefault
public class BluetoothDiscoveryProcess implements Supplier<DiscoveryResult>, BluetoothDeviceListener {

    private static final int DISCOVERY_TTL = 300;

    private final Logger logger = LoggerFactory.getLogger(BluetoothDiscoveryProcess.class);

    private final Lock serviceDiscoveryLock = new ReentrantLock();
    private final Condition connectionCondition = serviceDiscoveryLock.newCondition();
    private final Condition serviceDiscoveryCondition = serviceDiscoveryLock.newCondition();
    private final Condition nameDiscoveryCondition = serviceDiscoveryLock.newCondition();

    private final Collection<BluetoothDiscoveryParticipant> participants;
    private final BluetoothDevice device;

    private volatile boolean servicesDiscovered = false;

    public BluetoothDiscoveryProcess(BluetoothDevice device, Collection<BluetoothDiscoveryParticipant> participants) {
        this.participants = participants;
        this.device = device;
    }

    @Override
    public DiscoveryResult get() {
        // first see if any of the participants that don't require a connection recognize this device
        List<BluetoothDiscoveryParticipant> connectionParticipants = new ArrayList<>();
        for (BluetoothDiscoveryParticipant participant : participants) {
            if (participant.requiresConnection(device)) {
                connectionParticipants.add(participant);
                continue;
            }
            try {
                DiscoveryResult result = participant.createResult(device);
                if (result != null) {
                    return result;
                }
            } catch (RuntimeException e) {
                logger.warn("Participant '{}' threw an exception", participant.getClass().getName(), e);
            }
        }

        // Since we couldn't find a result, lets try the connection based participants
        DiscoveryResult result = null;
        if (!connectionParticipants.isEmpty()) {
            BluetoothAddress address = device.getAddress();
            try {
                BluetoothAddressLocker.lock(address);
                if (!device.hasListeners()) {
                    result = findConnectionResult(connectionParticipants);
                    // make sure to disconnect before letting go of the device
                    if (device.getConnectionState() == ConnectionState.CONNECTED) {
                        try {
                            if (!device.disconnect()) {
                                logger.debug("Failed to disconnect from device {}", address);
                            }
                        } catch (RuntimeException ex) {
                            logger.warn("Error occurred during bluetooth discovery for device {} on adapter {}",
                                    address, device.getAdapter().getAddress(), ex);
                        }
                    }
                }
            } finally {
                BluetoothAddressLocker.unlock(address);
            }
        }
        if (result == null) {
            result = createDefaultResult(device);
        }
        return result;
    }

    private DiscoveryResult createDefaultResult(BluetoothDevice device) {
        // We did not find a thing type for this device, so let's treat it as a generic one
        String label = device.getName();
        if (label == null || label.length() == 0 || label.equals(device.getAddress().toString().replace(':', '-'))) {
            label = "Bluetooth Device";
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(BluetoothBindingConstants.CONFIGURATION_ADDRESS, device.getAddress().toString());
        Integer txPower = device.getTxPower();
        if (txPower != null && txPower > 0) {
            properties.put(BluetoothBindingConstants.PROPERTY_TXPOWER, Integer.toString(txPower));
        }
        String manufacturer = BluetoothCompanyIdentifiers.get(device.getManufacturerId());
        if (manufacturer == null) {
            logger.debug("Unknown manufacturer Id ({}) found on bluetooth device.", device.getManufacturerId());
        } else {
            properties.put(Thing.PROPERTY_VENDOR, manufacturer);
            label += " (" + manufacturer + ")";
        }

        ThingUID thingUID = new ThingUID(BluetoothBindingConstants.THING_TYPE_BEACON, device.getAdapter().getUID(),
                device.getAddress().toString().toLowerCase().replace(":", ""));

        // Create the discovery result and add to the inbox
        return DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                .withRepresentationProperty(BluetoothBindingConstants.CONFIGURATION_ADDRESS).withTTL(DISCOVERY_TTL)
                .withBridge(device.getAdapter().getUID()).withLabel(label).build();
    }

    private @Nullable DiscoveryResult findConnectionResult(List<BluetoothDiscoveryParticipant> connectionParticipants) {
        try {
            device.addListener(this);
            for (BluetoothDiscoveryParticipant participant : connectionParticipants) {
                // we call this every time just in case a participant somehow closes the connection
                if (device.getConnectionState() != ConnectionState.CONNECTED) {
                    if (device.getConnectionState() != ConnectionState.CONNECTING && !device.connect()) {
                        logger.debug("Connection attempt failed to start for device {}", device.getAddress());
                        // something failed, so we abandon connection discovery
                        return null;
                    }
                    if (!awaitConnection(1, TimeUnit.SECONDS)) {
                        logger.debug("Connection to device {} timed out", device.getAddress());
                        return null;
                    }
                    if (!servicesDiscovered) {
                        device.discoverServices();
                        if (!awaitServiceDiscovery(1, TimeUnit.SECONDS)) {
                            logger.debug("Service discovery for device {} timed out", device.getAddress());
                            // something failed, so we abandon connection discovery
                            return null;
                        }
                    }
                    tryToDiscoverNameIfMissing();
                }

                try {
                    DiscoveryResult result = participant.createResult(device);
                    if (result != null) {
                        return result;
                    }
                } catch (RuntimeException e) {
                    logger.warn("Participant '{}' threw an exception", participant.getClass().getName(), e);
                }
            }
        } catch (InterruptedException e) {
            // do nothing
        } finally {
            device.removeListener(this);
        }
        return null;
    }

    @Override
    public void onScanRecordReceived(BluetoothScanNotification scanNotification) {
    }

    @Override
    public void onConnectionStateChange(BluetoothConnectionStatusNotification connectionNotification) {
        if (connectionNotification.getConnectionState() == ConnectionState.CONNECTED) {
            serviceDiscoveryLock.lock();
            try {
                connectionCondition.signal();
            } finally {
                serviceDiscoveryLock.unlock();
            }
            // a failure here would just result in a timeout, which is fine
            device.discoverServices();
        }
    }

    private void tryToDiscoverNameIfMissing() throws InterruptedException {
        BluetoothCharacteristic characteristic = device.getCharacteristic(GattCharacteristic.DEVICE_NAME.getUUID());
        if (characteristic == null || device.getName() != null) {
            return;
        }
        if (!device.readCharacteristic(characteristic)) {
            logger.debug("Failed to aquire name for device {}", device.getAddress());
            return;
        }
        if (!awaitNameDiscovery(1, TimeUnit.SECONDS)) {
            logger.debug("Name discovery for device {} timed out", device.getAddress());
        }
    }

    private boolean awaitConnection(long timeout, TimeUnit unit) throws InterruptedException {
        serviceDiscoveryLock.lock();
        try {
            long nanosTimeout = unit.toNanos(timeout);
            while (device.getConnectionState() != ConnectionState.CONNECTED) {
                if (nanosTimeout <= 0L) {
                    return false;
                }
                nanosTimeout = connectionCondition.awaitNanos(nanosTimeout);
            }
        } finally {
            serviceDiscoveryLock.unlock();
        }
        return true;
    }

    private boolean awaitNameDiscovery(long timeout, TimeUnit unit) throws InterruptedException {
        serviceDiscoveryLock.lock();
        try {
            long nanosTimeout = unit.toNanos(timeout);
            while (device.getName() == null) {
                if (nanosTimeout <= 0L) {
                    return false;
                }
                nanosTimeout = nameDiscoveryCondition.awaitNanos(nanosTimeout);
            }
        } finally {
            serviceDiscoveryLock.unlock();
        }
        return true;
    }

    private boolean awaitServiceDiscovery(long timeout, TimeUnit unit) throws InterruptedException {
        serviceDiscoveryLock.lock();
        try {
            long nanosTimeout = unit.toNanos(timeout);
            while (!servicesDiscovered) {
                if (nanosTimeout <= 0L) {
                    return false;
                }
                nanosTimeout = serviceDiscoveryCondition.awaitNanos(nanosTimeout);
            }
        } finally {
            serviceDiscoveryLock.unlock();
        }
        return true;
    }

    @Override
    public void onServicesDiscovered() {
        serviceDiscoveryLock.lock();
        try {
            servicesDiscovered = true;
            serviceDiscoveryCondition.signal();
        } finally {
            serviceDiscoveryLock.unlock();
        }
    }

    @Override
    public void onCharacteristicReadComplete(BluetoothCharacteristic characteristic, BluetoothCompletionStatus status) {
        serviceDiscoveryLock.lock();
        try {
            if (characteristic.getGattCharacteristic() == GattCharacteristic.DEVICE_NAME) {
                device.setName(characteristic.getStringValue(0));
                nameDiscoveryCondition.signal();
            }
        } finally {
            serviceDiscoveryLock.unlock();
        }
    }

    @Override
    public void onCharacteristicWriteComplete(BluetoothCharacteristic characteristic,
            BluetoothCompletionStatus status) {
    }

    @Override
    public void onCharacteristicUpdate(BluetoothCharacteristic characteristic) {
    }

    @Override
    public void onDescriptorUpdate(BluetoothDescriptor bluetoothDescriptor) {
    }

}
