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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.measure.quantity.Power;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.bluetooth.BluetoothDevice.ConnectionState;
import org.openhab.binding.bluetooth.internal.RoamingBluetoothAdapter;
import org.openhab.binding.bluetooth.notification.BluetoothConnectionStatusNotification;
import org.openhab.binding.bluetooth.notification.BluetoothScanNotification;

/**
 * This is a handler for generic Bluetooth devices in beacon-mode (i.e. not connected), which at the same time can be
 * used as a base implementation for more specific thing handlers.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@NonNullByDefault
public class BeaconBluetoothHandler extends BaseThingHandler implements BluetoothDeviceListener {

    @NonNullByDefault({} /* non-null if initialized */)
    protected BluetoothAdapter adapter;

    @NonNullByDefault({} /* non-null if initialized */)
    protected BluetoothAddress address;

    @NonNullByDefault({} /* non-null if initialized */)
    protected BluetoothDevice device;

    protected final ReentrantLock deviceLock;

    private @Nullable ZonedDateTime lastActivityTime;

    public BeaconBluetoothHandler(Thing thing) {
        super(thing);
        deviceLock = new ReentrantLock();
    }

    @Override
    public void initialize() {
        try {
            address = new BluetoothAddress(getConfig().get(BluetoothBindingConstants.CONFIGURATION_ADDRESS).toString());
        } catch (IllegalArgumentException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getLocalizedMessage());
            return;
        }

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Not associated with any bridge");
            return;
        }

        BridgeHandler bridgeHandler = bridge.getHandler();
        if (!(bridgeHandler instanceof BluetoothAdapter)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Associated with an unsupported bridge");
            return;
        }

        adapter = (BluetoothAdapter) bridgeHandler;

        try {
            deviceLock.lock();
            device = adapter.getDevice(address);
            device.addListener(this);
        } finally {
            deviceLock.unlock();
        }

        ThingBuilder builder = editThing();
        for (Channel channel : createDynamicChannels()) {
            // we only want to add each channel, not replace all of them
            builder.withChannel(channel);
        }
        updateThing(builder.build());

        updateStatus(ThingStatus.UNKNOWN);
    }

    private Channel buildChannel(String channelType, String itemType) {
        return ChannelBuilder.create(new ChannelUID(getThing().getUID(), channelType), itemType).build();
    }

    protected List<Channel> createDynamicChannels() {
        List<Channel> channels = new ArrayList<>();
        channels.add(buildChannel(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, "Number:Power"));
        channels.add(buildChannel(BluetoothBindingConstants.CHANNEL_TYPE_LAST_ACTIVITY_TIME, "DateTime"));
        if (adapter instanceof RoamingBluetoothAdapter) {
            channels.add(buildChannel(BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER, "String"));
            channels.add(buildChannel(BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER_LOCATION, "String"));
        }
        return channels;
    }

    @Override
    public void dispose() {
        try {
            deviceLock.lock();
            if (device != null) {
                device.removeListener(this);
                device.disconnect();
                device = null;
            }
        } finally {
            deviceLock.unlock();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH) {
            switch (channelUID.getId()) {
                case BluetoothBindingConstants.CHANNEL_TYPE_RSSI:
                    updateRSSI();
                    break;
                case BluetoothBindingConstants.CHANNEL_TYPE_LAST_ACTIVITY_TIME:
                    updateLastActivityTime();
                    break;
                case BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER:
                    updateAdapter();
                    break;
                case BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER_LOCATION:
                    updateAdapterLocation();
                    break;
            }
        }
    }

    /**
     * Updates the RSSI channel and the Thing status according to the new received rssi value
     */
    protected void updateRSSI() {
        if (device != null) {
            updateRSSI(device.getRssi());
        }
    }

    private void updateRSSI(@Nullable Integer rssi) {
        if (rssi != null && rssi != 0) {
            QuantityType<Power> quantity = new QuantityType<>(rssi, SmartHomeUnits.DECIBEL_MILLIWATTS);
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, quantity);
            updateStatusBasedOnRssi(true);
        } else {
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_RSSI, UnDefType.NULL);
            updateStatusBasedOnRssi(false);
        }
    }

    /**
     * Updates the LastActivityTime channel
     */
    protected void updateLastActivityTime() {
        ZonedDateTime activityTime = this.lastActivityTime;
        if (activityTime != null) {
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_LAST_ACTIVITY_TIME, new DateTimeType(activityTime));
        } else {
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_LAST_ACTIVITY_TIME, UnDefType.NULL);
        }
    }

    protected void updateAdapter() {
        if (device != null && adapter instanceof RoamingBluetoothAdapter) {
            BluetoothAdapter adapter = device.getAdapter();
            updateState(BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER, new StringType(adapter.getUID().getId()));
        }
    }

    protected void updateAdapterLocation() {
        if (device != null && adapter instanceof RoamingBluetoothAdapter) {
            BluetoothAdapter adapter = device.getAdapter();
            String location = adapter.getLocation();
            if (location != null || StringUtils.isBlank(location)) {
                updateState(BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER_LOCATION, new StringType(location));
            } else {
                updateState(BluetoothBindingConstants.CHANNEL_TYPE_ADAPTER_LOCATION, UnDefType.NULL);
            }
        }
    }

    /**
     * This method sets the Thing status based on whether or not we can receive a signal from it.
     * This is the best logic for beacons, but connected devices might want to deactivate this by overriding the method.
     *
     * @param receivedSignal true, if the device is in reach
     */
    protected void updateStatusBasedOnRssi(boolean receivedSignal) {
        if (receivedSignal) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    private void onActivity() {
        this.lastActivityTime = ZonedDateTime.now();
        updateLastActivityTime();
    }

    @Override
    public void onScanRecordReceived(BluetoothScanNotification scanNotification) {
        onActivity();
        int rssi = scanNotification.getRssi();
        if (rssi != Integer.MIN_VALUE) {
            updateRSSI(rssi);
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothConnectionStatusNotification connectionNotification) {
        // a disconnection doesn't count as activity
        if (connectionNotification.getConnectionState() != ConnectionState.DISCONNECTED) {
            onActivity();
        }
    }

    @Override
    public void onServicesDiscovered() {
        onActivity();
    }

    @Override
    public void onCharacteristicReadComplete(BluetoothCharacteristic characteristic, BluetoothCompletionStatus status) {
        if (status == BluetoothCompletionStatus.SUCCESS) {
            onActivity();
        }
    }

    @Override
    public void onCharacteristicWriteComplete(BluetoothCharacteristic characteristic,
            BluetoothCompletionStatus status) {
        if (status == BluetoothCompletionStatus.SUCCESS) {
            onActivity();
        }
    }

    @Override
    public void onCharacteristicUpdate(BluetoothCharacteristic characteristic) {
        onActivity();
    }

    @Override
    public void onDescriptorUpdate(BluetoothDescriptor bluetoothDescriptor) {
        onActivity();
    }

    @Override
    public void onAdapterChanged(BluetoothAdapter adapter) {
        updateAdapter();
        updateAdapterLocation();
    }
}
