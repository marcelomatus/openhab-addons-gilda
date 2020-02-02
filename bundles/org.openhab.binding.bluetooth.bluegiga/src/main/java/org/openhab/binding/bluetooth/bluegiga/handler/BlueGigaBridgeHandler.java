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
package org.openhab.binding.bluetooth.bluegiga.handler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.bluetooth.BluetoothAdapter;
import org.openhab.binding.bluetooth.BluetoothAddress;
import org.openhab.binding.bluetooth.BluetoothBindingConstants;
import org.openhab.binding.bluetooth.BluetoothDevice;
import org.openhab.binding.bluetooth.BluetoothDevice.ConnectionState;
import org.openhab.binding.bluetooth.BluetoothDeviceListener;
import org.openhab.binding.bluetooth.BluetoothDiscoveryListener;
import org.openhab.binding.bluetooth.bluegiga.BlueGigaAdapterConstants;
import org.openhab.binding.bluetooth.bluegiga.BlueGigaBluetoothDevice;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaConfiguration;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaEventListener;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaException;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaHandlerListener;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaSerialHandler;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaTransactionManager;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaAttributeWriteCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaAttributeWriteResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaFindInformationCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaFindInformationResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaReadByGroupTypeCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaReadByGroupTypeResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaReadByHandleCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient.BlueGigaReadByHandleResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.connection.BlueGigaConnectionStatusEvent;
import org.openhab.binding.bluetooth.bluegiga.internal.command.connection.BlueGigaDisconnectCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.connection.BlueGigaDisconnectResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.connection.BlueGigaDisconnectedEvent;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaConnectDirectCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaConnectDirectResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaDiscoverCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaDiscoverResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaEndProcedureCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaEndProcedureResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaScanResponseEvent;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaSetModeCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaSetModeResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaSetScanParametersCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.gap.BlueGigaSetScanParametersResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.system.BlueGigaAddressGetCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.system.BlueGigaAddressGetResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.system.BlueGigaGetConnectionsCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.system.BlueGigaGetConnectionsResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.command.system.BlueGigaGetInfoCommand;
import org.openhab.binding.bluetooth.bluegiga.internal.command.system.BlueGigaGetInfoResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.BgApiResponse;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.BluetoothAddressType;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.GapConnectableMode;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.GapDiscoverMode;
import org.openhab.binding.bluetooth.bluegiga.internal.enumeration.GapDiscoverableMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BlueGigaBridgeHandler} is responsible for interfacing to the BlueGiga Bluetooth adapter.
 * It provides a private interface for {@link BlueGigaBluetoothDevice}s to access the dongle and provides top
 * level adaptor functionality for scanning and arbitration.
 * <p>
 * The handler provides the serial interface to the dongle via the BlueGiga BG-API library.
 * <p>
 * In the BlueGiga dongle, we leave scanning enabled most of the time. Normally, it's just passive scanning, and active
 * scanning is enabled when we want to include new devices. Passive scanning is enough for us to receive beacons etc
 * that are transmitted periodically, and active scanning will get more information which may be useful when we are
 * including new devices.
 *
 * @author Chris Jackson - Initial contribution
 * @author Kai Kreuzer - Made handler implement BlueGigaHandlerListener
 * @author Pauli Anttila - Many improvements
 */
@NonNullByDefault
public class BlueGigaBridgeHandler extends BaseBridgeHandler
        implements BluetoothAdapter, BlueGigaEventListener, BlueGigaHandlerListener {

    private final Logger logger = LoggerFactory.getLogger(BlueGigaBridgeHandler.class);

    private final int COMMAND_TIMEOUT_MS = 5000;

    private final SerialPortManager serialPortManager;

    private final ScheduledExecutorService executor = ThreadPoolManager.getScheduledPool("BlueGiga");

    // The serial port.
    @Nullable
    private SerialPort serialPort;

    private @NonNullByDefault({}) BlueGigaConfiguration configuration;

    // The serial port input stream.
    @Nullable
    private InputStream inputStream;

    // The serial port output stream.
    @Nullable
    private OutputStream outputStream;

    // The BlueGiga API handler
    private @NonNullByDefault({}) BlueGigaSerialHandler serialHandler;

    // The BlueGiga API handler
    private @NonNullByDefault({}) BlueGigaTransactionManager transactionManager;

    // The maximum number of connections this interface supports
    private int maxConnections = 0;

    // Our BT address
    @Nullable
    private BluetoothAddress address;

    // Map of Bluetooth devices known to this bridge.
    // This is all devices we have heard on the network - not just things bound to the bridge
    private final Map<BluetoothAddress, BlueGigaBluetoothDevice> devices = new ConcurrentHashMap<>();

    // Map of open connections
    private final Map<Integer, BluetoothAddress> connections = new ConcurrentHashMap<>();

    // Set of discovery listeners
    protected final Set<BluetoothDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();

    // List of device listeners
    protected final ConcurrentHashMap<BluetoothAddress, BluetoothDeviceListener> deviceListeners = new ConcurrentHashMap<>();

    private boolean initComplete = false;

    private @NonNullByDefault({}) ScheduledFuture<?> removeInactiveDevicesTask;

    private volatile boolean activeScanEnabled = false;

    private @NonNullByDefault({}) Future<?> passiveScanIdleTimer;

    public BlueGigaBridgeHandler(Bridge bridge, SerialPortManager serialPortManager) {
        super(bridge);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public ThingUID getUID() {
        // being a BluetoothAdapter, we use the UID of our bridge
        return getThing().getUID();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // No commands supported for the bridge
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(BlueGigaConfiguration.class);
        logger.debug("Using configuration: {}", configuration);

        if (openSerialPort(configuration.port, 115200)) {
            serialHandler = new BlueGigaSerialHandler(inputStream, outputStream);
            transactionManager = new BlueGigaTransactionManager(serialHandler, executor);
            serialHandler.addHandlerListener(this);
            transactionManager.addEventListener(this);

            updateStatus(ThingStatus.UNKNOWN);

            try {
                // Stop any procedures that are running
                bgEndProcedure();

                // Set mode to non-discoverable etc.
                bgSetMode();

                // Get maximum parallel connections
                maxConnections = readMaxConnections().getMaxconn();

                // Close all connections so we start from a known position
                for (int connection = 0; connection < maxConnections; connection++) {
                    bgDisconnect(connection);
                }

                // Get our Bluetooth address
                address = new BluetoothAddress(readAddress().getAddress());

                updateThingProperties();

                initComplete = true;
                updateStatus(ThingStatus.ONLINE);
                startScheduledTasks();
            } catch (BlueGigaException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                        "Command send failed to BlueGiga controller");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Failed opening serial port.");
        }
    }

    @Override
    public void dispose() {
        try {
            stopScheduledTasks();
            transactionManager.removeEventListener(this);
            serialHandler.removeHandlerListener(this);
            transactionManager.close();
            serialHandler.close();
            initComplete = false;
            devices.clear();
            connections.clear();
        } catch (IllegalStateException e) {
            // ignore if handler wasn't set at all
        }
        closeSerialPort();
    }

    private void schedulePassiveScan() {
        cancelScheduledPassiveScan();
        passiveScanIdleTimer = executor.schedule(() -> {
            if (!activeScanEnabled) {
                logger.debug("Activate passive scan");
                bgEndProcedure();
                bgStartScanning(false, configuration.passiveScanInterval, configuration.passiveScanWindow);
            } else {
                logger.debug("Ignore passive scan activation as active scan is active");
            }
        }, configuration.passiveScanIdleTime, TimeUnit.MILLISECONDS);
    }

    private void cancelScheduledPassiveScan() {
        if (passiveScanIdleTimer != null) {
            passiveScanIdleTimer.cancel(true);
        }
    }

    private void startScheduledTasks() {
        schedulePassiveScan();
        logger.debug("Start scheduled task to remove inactive devices");
        removeInactiveDevicesTask = scheduler.scheduleWithFixedDelay(this::removeInactiveDevices, 1, 1,
                TimeUnit.MINUTES);
    }

    private void stopScheduledTasks() {
        cancelScheduledPassiveScan();
        removeInactiveDevicesTask.cancel(true);
    }

    private void removeInactiveDevices() {
        logger.debug("Check inactive devices, count {}", devices.size());
        devices.forEach((address, device) -> {
            if (shouldRemove(device)) {
                logger.debug("Removing device '{}' due to inactivity, last seen: {}", address,
                        device.getLastSeenTime());
                device.dispose();
                devices.remove(address);
            }
        });
    }

    private boolean shouldRemove(BlueGigaBluetoothDevice device) {
        // we can't remove devices with listeners since that means they have a handler.
        if (device.hasListeners()) {
            return false;
        }
        // devices that are connected won't receive any scan notifications so we can't remove them for being idle
        if (device.getConnectionState() == ConnectionState.CONNECTED) {
            return false;
        }

        return device.getLastSeenTime().plusMinutes(5).isBeforeNow();
    }

    private BlueGigaGetConnectionsResponse readMaxConnections() throws BlueGigaException {
        BlueGigaCommand command = new BlueGigaGetConnectionsCommand();
        return transactionManager.sendTransaction(command, BlueGigaGetConnectionsResponse.class, COMMAND_TIMEOUT_MS);
    }

    private BlueGigaAddressGetResponse readAddress() throws BlueGigaException {
        BlueGigaAddressGetCommand command = new BlueGigaAddressGetCommand();
        return transactionManager.sendTransaction(command, BlueGigaAddressGetResponse.class, COMMAND_TIMEOUT_MS);
    }

    private BlueGigaGetInfoResponse readInfo() throws BlueGigaException {
        BlueGigaGetInfoCommand command = new BlueGigaGetInfoCommand();
        return transactionManager.sendTransaction(command, BlueGigaGetInfoResponse.class, COMMAND_TIMEOUT_MS);
    }

    private void updateThingProperties() throws BlueGigaException {
        BlueGigaGetInfoResponse infoResponse = readInfo();

        Map<String, String> properties = editProperties();
        properties.put(BluetoothBindingConstants.PROPERTY_MAXCONNECTIONS, Integer.toString(maxConnections));
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION,
                String.format("%d.%d", infoResponse.getMajor(), infoResponse.getMinor()));
        properties.put(Thing.PROPERTY_HARDWARE_VERSION, Integer.toString(infoResponse.getHardware()));
        properties.put(BlueGigaAdapterConstants.PROPERTY_PROTOCOL, Integer.toString(infoResponse.getProtocolVersion()));
        properties.put(BlueGigaAdapterConstants.PROPERTY_LINKLAYER, Integer.toString(infoResponse.getLlVersion()));
        updateProperties(properties);
    }

    private boolean openSerialPort(final String serialPortName, int baudRate) {
        logger.debug("Connecting to serial port '{}'", serialPortName);
        try {
            SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(serialPortName);
            if (portIdentifier == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Port does not exist");
                return false;
            }
            SerialPort sp = portIdentifier.open("org.openhab.binding.bluetooth.bluegiga", 2000);
            sp.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            sp.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);
            sp.enableReceiveThreshold(1);
            sp.enableReceiveTimeout(2000);

            // RXTX serial port library causes high CPU load
            // Start event listener, which will just sleep and slow down event loop
            sp.notifyOnDataAvailable(true);

            logger.info("Connected to serial port '{}'.", serialPortName);

            try {
                inputStream = new BufferedInputStream(sp.getInputStream());
                outputStream = sp.getOutputStream();
            } catch (IOException e) {
                logger.error("Error getting serial streams", e);
                return false;
            }
            this.serialPort = sp;
            return true;
        } catch (PortInUseException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Serial Error: Port in use");
            return false;
        } catch (UnsupportedCommOperationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Serial Error: Unsupported operation");
            return false;
        }
    }

    private void closeSerialPort() {
        if (serialPort != null) {
            SerialPort sp = serialPort;
            try {
                sp.disableReceiveTimeout();
                sp.removeEventListener();
                IOUtils.closeQuietly(outputStream);
                IOUtils.closeQuietly(inputStream);
            } finally {
                sp.close();
                logger.debug("Closed serial port.");
                serialPort = null;
                inputStream = null;
                outputStream = null;
            }
        }
    }

    @Override
    public void scanStart() {
        logger.debug("Start active scan");
        activeScanEnabled = true;
        // Stop the passive scan
        cancelScheduledPassiveScan();
        bgEndProcedure();

        // Start a active scan
        bgStartScanning(true, configuration.activeScanInterval, configuration.activeScanWindow);

        for (BluetoothDevice device : devices.values()) {
            deviceDiscovered(device);
        }
    }

    @Override
    public void scanStop() {
        logger.debug("Stop active scan");
        activeScanEnabled = false;

        // Stop the active scan
        bgEndProcedure();

        // Start a passive scan after idle delay
        schedulePassiveScan();
    }

    @Override
    public BluetoothAddress getAddress() {
        BluetoothAddress addr = address;
        if (addr != null) {
            return addr;
        } else {
            throw new IllegalStateException("Adapter has not been initialized yet!");
        }
    }

    @SuppressWarnings({ "null", "unused" })
    @Override
    public BluetoothDevice getDevice(BluetoothAddress address) {
        BlueGigaBluetoothDevice device = devices.get(address);
        if (device == null) {
            // This method always needs to return a device, even if we don't currently know about it.
            device = new BlueGigaBluetoothDevice(this, address, BluetoothAddressType.UNKNOWN);
            devices.put(address, device);
        }
        return device;
    }

<<<<<<< Upstream, based on origin/2.5.x
    @Override
    public boolean hasDevice(BluetoothAddress address) {
        return devices.containsKey(address);
    }

    /*
     * The following methods provide adaptor level functions for the BlueGiga interface. Typically these methods
     * are used by the device but are provided in the adapter to allow common knowledge and to support conflict
     * resolution.
     */

    public BlueGigaSerialHandler getBgHandler() {
        BlueGigaSerialHandler handler = bgHandler;
        if (handler != null) {
            return handler;
        } else {
            throw new IllegalStateException("bgHandler must not be null at that point!");
        }
    }

    public void setBgHandler(BlueGigaSerialHandler bgHandler) {
        this.bgHandler = bgHandler;
    }

=======
>>>>>>> 68f3371 [bluetooth.bluegiga] Introduced transaction manager
    /**
     * Connects to a device.
     * <p>
     * If the device is already connected, or the attempt to connect failed, then we return false. If we have reached
     * the maximum number of connections supported by this dongle, then we return false.
     *
     * @param address the device {@link BluetoothAddress} to connect to
     * @param addressType the {@link BluetoothAddressType} of the device
     * @return true if the connection was started
     */
    public boolean bgConnect(BluetoothAddress address, BluetoothAddressType addressType) {
        // Check the connection to make sure we're not already connected to this device
        if (connections.containsValue(address)) {
            return false;
        }

        // FIXME: When getting here, I always found all connections to be already taken and thus the code never
        // proceeded. Relaxing this condition did not do any obvious harm, but now guaranteed that the services are
        // queried from the device.
        if (connections.size() == maxConnections + 1) {
            logger.debug("BlueGiga: Attempt to connect to {} but no connections available.", address);
            return false;
        }

        logger.debug("BlueGiga Connect: address {}.", address);
        cancelScheduledPassiveScan();

        // Connect...
        BlueGigaConnectDirectCommand connect = new BlueGigaConnectDirectCommand();
        connect.setAddress(address.toString());
        connect.setAddrType(addressType);
        connect.setConnIntervalMin(configuration.connIntervalMin);
        connect.setConnIntervalMax(configuration.connIntervalMax);
        connect.setLatency(configuration.connLatency);
        connect.setTimeout(configuration.connTimeout);

        try {
            BlueGigaConnectDirectResponse connectResponse = transactionManager.sendTransaction(connect,
                    BlueGigaConnectDirectResponse.class, COMMAND_TIMEOUT_MS);
            if (connectResponse.getResult() != BgApiResponse.SUCCESS) {
                return false;
            }
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending connect command to device {}, reason: {}.", address,
                    e.getMessage());
            return false;
        } finally {
            schedulePassiveScan();
        }
        return true;
    }

    /**
     * Close a connection using {@link BlueGigaDisconnectCommand}
     *
     * @param connectionHandle
     * @return
     */
    public boolean bgDisconnect(int connectionHandle) {
        logger.debug("BlueGiga Disconnect: connection {}", connectionHandle);
        cancelScheduledPassiveScan();
        BlueGigaDisconnectCommand command = new BlueGigaDisconnectCommand();
        command.setConnection(connectionHandle);

        try {
            BlueGigaDisconnectResponse response = transactionManager.sendTransaction(command,
                    BlueGigaDisconnectResponse.class, COMMAND_TIMEOUT_MS);
            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending disconnect command to device {}, reason: {}.", address,
                    e.getMessage());
            return false;
        } finally {
            schedulePassiveScan();
        }
    }

    /**
     * Device discovered. This simply passes the discover information to the discovery service for processing.
     */
    public void deviceDiscovered(BluetoothDevice device) {
        if (configuration.discovery || activeScanEnabled) {
            for (BluetoothDiscoveryListener listener : discoveryListeners) {
                listener.deviceDiscovered(device);
            }
        }
    }

    /**
     * Start a read of all primary services using {@link BlueGigaReadByGroupTypeCommand}
     *
     * @param connectionHandle
     * @return true if successful
     */
    public boolean bgFindPrimaryServices(int connectionHandle) {
        logger.debug("BlueGiga FindPrimary: connection {}", connectionHandle);
        cancelScheduledPassiveScan();
        BlueGigaReadByGroupTypeCommand command = new BlueGigaReadByGroupTypeCommand();
        command.setConnection(connectionHandle);
        command.setStart(1);
        command.setEnd(65535);
        command.setUuid(UUID.fromString("00002800-0000-0000-0000-000000000000"));

        try {
            BlueGigaReadByGroupTypeResponse response = transactionManager.sendTransaction(command,
                    BlueGigaReadByGroupTypeResponse.class, COMMAND_TIMEOUT_MS);
            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending read primary services command to device {}, reason: {}.", address,
                    e.getMessage());
            return false;
        } finally {
            schedulePassiveScan();
        }
    }

    /**
     * Start a read of all characteristics using {@link BlueGigaFindInformationCommand}
     *
     * @param connectionHandle
     * @return true if successful
     */
    public boolean bgFindCharacteristics(int connectionHandle) {
        logger.debug("BlueGiga Find: connection {}", connectionHandle);
        cancelScheduledPassiveScan();
        BlueGigaFindInformationCommand command = new BlueGigaFindInformationCommand();
        command.setConnection(connectionHandle);
        command.setStart(1);
        command.setEnd(65535);

        try {
            BlueGigaFindInformationResponse response = transactionManager.sendTransaction(command,
                    BlueGigaFindInformationResponse.class, COMMAND_TIMEOUT_MS);
            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending read characteristics command to device {}, reason: {}.", address,
                    e.getMessage());
            return false;
        } finally {
            schedulePassiveScan();
        }
    }

    /**
     * Read a characteristic using {@link BlueGigaReadByHandleCommand}
     *
     * @param connectionHandle
     * @param handle
     * @return true if successful
     */
    public boolean bgReadCharacteristic(int connectionHandle, int handle) {
        logger.debug("BlueGiga Read: connection {}, handle {}", connectionHandle, handle);
        cancelScheduledPassiveScan();
        BlueGigaReadByHandleCommand command = new BlueGigaReadByHandleCommand();
        command.setConnection(connectionHandle);
        command.setChrHandle(handle);

        try {
            BlueGigaReadByHandleResponse response = transactionManager.sendTransaction(command,
                    BlueGigaReadByHandleResponse.class, COMMAND_TIMEOUT_MS);
            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending read characteristics command to device {}, reason: {}.", address,
                    e.getMessage());
            return false;
        } finally {
            schedulePassiveScan();
        }
    }

    /**
     * Write a characteristic using {@link BlueGigaAttributeWriteCommand}
     *
     * @param connectionHandle
     * @param handle
     * @param value
     * @return true if successful
     */
    public boolean bgWriteCharacteristic(int connectionHandle, int handle, int[] value) {
        logger.debug("BlueGiga Write: connection {}, handle {}", connectionHandle, handle);
        cancelScheduledPassiveScan();
        BlueGigaAttributeWriteCommand command = new BlueGigaAttributeWriteCommand();
        command.setConnection(connectionHandle);
        command.setAttHandle(handle);
        command.setData(value);

        try {
            BlueGigaAttributeWriteResponse response = transactionManager.sendTransaction(command,
                    BlueGigaAttributeWriteResponse.class, COMMAND_TIMEOUT_MS);
            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending write characteristics command to device {}, reason: {}.", address,
                    e.getMessage());
            return false;
        } finally {
            schedulePassiveScan();
        }
    }

    /*
     * The following methods are private methods for handling the BlueGiga protocol
     */
    private boolean bgEndProcedure() {
        try {
            BlueGigaCommand command = new BlueGigaEndProcedureCommand();
            BlueGigaEndProcedureResponse response = transactionManager.sendTransaction(command,
                    BlueGigaEndProcedureResponse.class, COMMAND_TIMEOUT_MS);

            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending end procedure command.");
            return false;
        }
    }

    private boolean bgSetMode() {
        try {
            BlueGigaSetModeCommand command = new BlueGigaSetModeCommand();
            command.setConnect(GapConnectableMode.GAP_NON_CONNECTABLE);
            command.setDiscover(GapDiscoverableMode.GAP_NON_DISCOVERABLE);
            BlueGigaSetModeResponse response = transactionManager.sendTransaction(command,
                    BlueGigaSetModeResponse.class, COMMAND_TIMEOUT_MS);
            return response.getResult() == BgApiResponse.SUCCESS;
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending set mode command, reason: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Starts scanning on the dongle
     *
     * @param active true for active scanning
     */
    private boolean bgStartScanning(boolean active, int interval, int window) {
        try {
            BlueGigaSetScanParametersCommand scanCommand = new BlueGigaSetScanParametersCommand();
            scanCommand.setActiveScanning(active);
            scanCommand.setScanInterval(interval);
            scanCommand.setScanWindow(window);
            BlueGigaSetScanParametersResponse scanParametersResponse = transactionManager.sendTransaction(scanCommand,
                    BlueGigaSetScanParametersResponse.class, COMMAND_TIMEOUT_MS);
            if (scanParametersResponse.getResult() == BgApiResponse.SUCCESS) {
                BlueGigaDiscoverCommand discoverCommand = new BlueGigaDiscoverCommand();
                discoverCommand.setMode(GapDiscoverMode.GAP_DISCOVER_OBSERVATION);
                BlueGigaDiscoverResponse discoverResponse = transactionManager.sendTransaction(discoverCommand,
                        BlueGigaDiscoverResponse.class, COMMAND_TIMEOUT_MS);
                if (discoverResponse.getResult() == BgApiResponse.SUCCESS) {
                    logger.debug("{} scanning succesfully started.", active ? "Active" : "Passive");
                    return true;
                }
            }
        } catch (BlueGigaException e) {
            logger.debug("Error occured when sending start scan command, reason: {}", e.getMessage());
        }
        logger.debug("Scan start failed.");
        return false;
    }

    /**
     * Add an event listener for the BlueGiga events
     *
     * @param listener the {@link BlueGigaEventListener} to add
     */
    public void addEventListener(BlueGigaEventListener listener) {
        transactionManager.addEventListener(listener);
    }

    /**
     * Remove an event listener for the BlueGiga events
     *
     * @param listener the {@link BlueGigaEventListener} to remove
     */
    public void removeEventListener(BlueGigaEventListener listener) {
        transactionManager.removeEventListener(listener);
    }

    @Override
    public void addDiscoveryListener(BluetoothDiscoveryListener listener) {
        discoveryListeners.add(listener);
    }

    @Override
    public void removeDiscoveryListener(@Nullable BluetoothDiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    @Override
    public void bluegigaEventReceived(@Nullable BlueGigaResponse event) {
        if (event instanceof BlueGigaScanResponseEvent) {
            if (initComplete) {
                BlueGigaScanResponseEvent scanEvent = (BlueGigaScanResponseEvent) event;

                // We use the scan event to add any devices we hear to the devices list
                // The device gets created, and then manages itself for discovery etc.
                BluetoothAddress sender = new BluetoothAddress(scanEvent.getSender());
                if (!devices.containsKey(sender)) {
                    BlueGigaBluetoothDevice device;
                    logger.debug("BlueGiga adding new device to adaptor {}: {}", address, sender);
                    device = new BlueGigaBluetoothDevice(this, new BluetoothAddress(scanEvent.getSender()),
                            scanEvent.getAddressType());
                    devices.put(sender, device);
                    deviceDiscovered(device);
                }
            } else {
                logger.trace("Ignore BlueGigaScanResponseEvent as initialization is not complete");
            }
            return;
        }

        if (event instanceof BlueGigaConnectionStatusEvent) {
            BlueGigaConnectionStatusEvent connectionEvent = (BlueGigaConnectionStatusEvent) event;
            connections.put(connectionEvent.getConnection(), new BluetoothAddress(connectionEvent.getAddress()));
        }

        if (event instanceof BlueGigaDisconnectedEvent) {
            BlueGigaDisconnectedEvent disconnectedEvent = (BlueGigaDisconnectedEvent) event;
            connections.remove(disconnectedEvent.getConnection());
        }
    }

    @Override
    public void bluegigaClosed(Exception reason) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, reason.getMessage());
    }
}
