/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.max.internal.handler;

import static org.openhab.binding.max.MaxBinding.CHANNEL_MODE;
import static org.openhab.binding.max.MaxBinding.CHANNEL_SETTEMP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.io.OutputStreamWriter;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.max.config.MaxCubeBridgeConfiguration;
import org.openhab.binding.max.internal.Utils;
import org.openhab.binding.max.internal.message.C_Message;
import org.openhab.binding.max.internal.message.Device;
import org.openhab.binding.max.internal.message.DeviceConfiguration;
import org.openhab.binding.max.internal.message.DeviceInformation;
import org.openhab.binding.max.internal.message.H_Message;
import org.openhab.binding.max.internal.message.HeatingThermostat;
import org.openhab.binding.max.internal.message.L_Message;
import org.openhab.binding.max.internal.message.M_Message;
import org.openhab.binding.max.internal.message.Message;
import org.openhab.binding.max.internal.message.MessageType;
import org.openhab.binding.max.internal.message.S_Command;
import org.openhab.binding.max.internal.message.S_Message;
import org.openhab.binding.max.internal.message.SendCommand;
import org.openhab.binding.max.internal.message.ThermostatModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MaxCubeBridgeHandler} is the handler for a MAX! Cube and connects it
 * to the framework. All {@link MaxDevicesHandler}s use the
 * {@link MaxCubeBridgeHandler} to execute the actual commands.
 * 
 * @author Marcel Verpaalen - Initial contribution OH2 version
 * @author Andreas Heil (info@aheil.de) - OH1 version
 * @author Bernd Michael Helm (bernd.helm at helmundwalter.de) - Exclusive mode
 * 
 */
public class MaxCubeBridgeHandler extends BaseBridgeHandler {
	// TODO: optional configuration to get the actual temperature on a
	// configured interval by changing the valve / temp setting

	public MaxCubeBridgeHandler(Bridge br) {
		super(br);
	}

	private Logger logger = LoggerFactory.getLogger(MaxCubeBridgeHandler.class);

	/** The refresh interval which is used to poll given MAX!Cube */
	private long refreshInterval = 10000;
	ScheduledFuture<?> refreshJob;

	private ArrayList<Device> devices = new ArrayList<Device>();
	private HashSet<String> lastActiveDevices = new HashSet<String>();

	/** MAX! Thermostat default off temperature */
	private static final DecimalType DEFAULT_OFF_TEMPERATURE = new DecimalType(4.5);

	/** MAX! Thermostat default on temperature */
	private static final DecimalType DEFAULT_ON_TEMPERATURE = new DecimalType(30.5);

	private ArrayList<DeviceConfiguration> configurations = new ArrayList<DeviceConfiguration>();

	/** maximum queue size that we're allowing */
	private static final int MAX_COMMANDS = 50;
	private ArrayBlockingQueue<SendCommand> commandQueue = new ArrayBlockingQueue<SendCommand>(MAX_COMMANDS);

	private boolean connectionEstablished = false;

	private SendCommand lastCommandId = null;

	private String ipAddress;
	private int port;
	private boolean exclusive;
	private int maxRequestsPerConnection;
	private int requestCount = 0;

	/**
	 * Duty cycle of the cube
	 */
	private int dutyCycle = 0;

	/**
	 * The available memory slots of the cube
	 */
	private int freeMemorySlots;

	/**
	 * connection socket and reader/writer for execute method
	 */
	private Socket socket = null;
	private BufferedReader reader = null;
	private OutputStreamWriter writer = null;

	private boolean previousOnline = false;

	private List<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArrayList<>();

	private ScheduledFuture<?> pollingJob;
	private Runnable pollingRunnable = new Runnable() {
		@Override
		public void run() {
			refreshData();
		}
	};
	private ScheduledFuture<?> sendCommandJob;
	private long sendCommandInterval = 5000;
	private Runnable sendCommandRunnable = new Runnable() {
		@Override
		public void run() {
			sendCommands();
		}
	};

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (command instanceof RefreshType) {
			logger.debug("Refresh command received.");
			refreshData();
		} else
			logger.warn("No bridge commands defined.");
	}

	@Override
	public void dispose() {
		logger.debug("Handler disposed.");
		if (pollingJob != null && !pollingJob.isCancelled()) {
			pollingJob.cancel(true);
			pollingJob = null;
		}
		if (sendCommandJob != null && !sendCommandJob.isCancelled()) {
			sendCommandJob.cancel(true);
			sendCommandJob = null;
		}

		clearDeviceList();
		connectionEstablished = false;

		socketClose();
		super.dispose();
	}

	@Override
	public void initialize() {
		logger.debug("Initializing MAX! Cube bridge handler.");

		MaxCubeBridgeConfiguration configuration = getConfigAs(MaxCubeBridgeConfiguration.class);
		port = configuration.port;
		ipAddress = configuration.ipAddress;
		refreshInterval = configuration.refreshInterval;
		exclusive = configuration.exclusive;
		maxRequestsPerConnection = configuration.maxRequestsPerConnection;
		logger.debug("Cube IP         {}.", ipAddress);
		logger.debug("Port            {}.", port);
		logger.debug("RefreshInterval {}.", refreshInterval);
		logger.debug("Exclusive mode  {}.", exclusive);
		logger.debug("Max Requests    {}.", maxRequestsPerConnection);

		startAutomaticRefresh();

		// workaround for issue #92: getHandler() returns NULL after
		// configuration update. :
		getThing().setHandler(this);
	}

	private synchronized void startAutomaticRefresh() {
		if (pollingJob == null || pollingJob.isCancelled()) {
			pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0, refreshInterval, TimeUnit.MILLISECONDS);
		}
		if (sendCommandJob == null || sendCommandJob.isCancelled()) {
			sendCommandJob = scheduler.scheduleAtFixedRate(sendCommandRunnable, 0, sendCommandInterval,
					TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Takes a command from the command queue and send it to
	 * {@link executeCommand} for execution.
	 * 
	 */
	private synchronized void sendCommands() {

		SendCommand sendCommand = commandQueue.poll();
		if (sendCommand != null) {
			executeCommand(sendCommand);
		}
	}

	/**
	 * initiates read data from the maxCube bridge
	 */
	private synchronized void refreshData() {

		try {
			refreshDeviceData();
			if (connectionEstablished) {
				updateStatus(ThingStatus.ONLINE);
				previousOnline = true;
				for (Device di : devices) {
					if (lastActiveDevices != null && lastActiveDevices.contains(di.getSerialNumber())) {
						for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
							try {
								deviceStatusListener.onDeviceStateChanged(getThing().getUID(), di);
							} catch (Exception e) {
								logger.error("An exception occurred while calling the DeviceStatusListener", e);
							}
						}
					}
					// New device, not seen before, pass to Discovery
					else {
						for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
							try {
								deviceStatusListener.onDeviceAdded(getThing(), di);
								di.setUpdated(true);
								deviceStatusListener.onDeviceStateChanged(getThing().getUID(), di);
							} catch (Exception e) {
								logger.error("An exception occurred while calling the DeviceStatusListener", e);
							}
							lastActiveDevices.add(di.getSerialNumber());
						}
					}
				}
			} else if (previousOnline)
				onConnectionLost();

		} catch (Exception e) {
			logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
		}
	}

	public void onConnectionLost() {
		logger.info("Bridge connection lost. Updating thing status to OFFLINE.");
		previousOnline = false;
		updateStatus(ThingStatus.OFFLINE);
	}

	public void onConnection() {
		logger.info("Bridge connected. Updating thing status to ONLINE.");
		updateStatus(ThingStatus.ONLINE);
	}

	public boolean registerDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
		if (deviceStatusListener == null) {
			throw new NullPointerException("It's not allowed to pass a null deviceStatusListener.");
		}
		boolean result = deviceStatusListeners.add(deviceStatusListener);
		if (result) {
			// onUpdate();
		}
		return result;
	}

	public boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
		boolean result = deviceStatusListeners.remove(deviceStatusListener);
		if (result) {
			// onUpdate();
		}
		return result;
	}

	public void clearDeviceList() {
		lastActiveDevices = new HashSet<String>();
	}

	/**
	 * Processes device command and sends it to the MAX!Cube Lan Gateway.
	 * 
	 * @param serialNumber
	 *            the serial number of the device as String
	 * @param channelUID
	 *            the ChannelUID used to send the command
	 * @param command
	 *            the command data
	 */

	/**
	 * Connects to the Max!Cube Lan gateway, reads and decodes the message this
	 * updates device information for each connected Max!Cube device
	 */
	private void refreshDeviceData() {
		Message message;

		for (String raw : getRawMessage()) {

			try {
				logger.trace("message block: '{}'", raw);
				message = processRawMessage(raw);
				if (message != null) {
					message.debug(logger);
					processMessage(message);
				}
			} catch (Exception e) {
				logger.info("Failed to process message received by MAX! protocol.");
				logger.debug(Utils.getStackTrace(e));
			}
		}

	}

	/**
	 * Connects to the Max!Cube Lan gateway and returns the read data
	 * corresponding Message.
	 * 
	 * @return the raw message text as ArrayList of String
	 */
	private ArrayList<String> getRawMessage() {
		synchronized (MaxCubeBridgeHandler.class) {
			ArrayList<String> rawMessage = new ArrayList<String>();

			try {
				String raw = null;
				if (socket == null) {
					this.socketConnect();
				} else {
					if (maxRequestsPerConnection > 0 && requestCount >= maxRequestsPerConnection) {
						logger.debug("maxRequestsPerConnection reached, reconnecting.");
						socket.close();
						this.socketConnect();
						requestCount = 0;
					} else {

						/*
						 * if the connection is already open (this happens in
						 * exclusive mode), just send a "l:\r\n" to get the
						 * latest live informations note that "L:\r\n" or "l:\n"
						 * would not work.
						 */
						logger.debug("Sending state request #{} to MAX! Cube", this.requestCount);
						if (writer == null) {
							logger.warn("Can't write to MAX! Cube");
							this.socketConnect();
						}
						writer.write("l:" + '\r' + '\n');
						writer.flush();
						requestCount++;
					}
				}

				boolean cont = true;
				while (cont) {
					raw = reader.readLine();
					if (raw == null) {
						cont = false;
						continue;
					}
					rawMessage.add(raw);
					if (raw.startsWith("L:")) {
						cont = false;
						connectionEstablished = true;
					}
				}

				if (!exclusive) {
					socketClose();
				}
			} catch (ConnectException e) {
				logger.debug("Connection timed out on {} port {}", ipAddress, port);
				connectionEstablished = false;
				socketClose(); // reconnect on next execution
			} catch (UnknownHostException e) {
				logger.debug("Host error occurred during execution: {}", e.getMessage());
				connectionEstablished = false;
				socketClose(); // reconnect on next execution
			} catch (IOException e) {
				logger.debug("IO error occurred during execution: {}", e.getMessage());
				connectionEstablished = false;
				socketClose(); // reconnect on next execution
			} catch (Exception e) {
				logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
				connectionEstablished = false;
				socketClose(); // reconnect on next execution
			}

			return rawMessage;
		}
	}

	/**
	 * Processes the raw TCP data read from the MAX protocol, returning the
	 * corresponding Message.
	 * 
	 * @param raw
	 *            the raw data line read from the MAX protocol
	 * @return message the @Message for the given raw data
	 */
	private Message processRawMessage(String raw) {

		if (raw.startsWith("H:")) {
			return new H_Message(raw);
		} else if (raw.startsWith("M:")) {
			return new M_Message(raw);
		} else if (raw.startsWith("C:")) {
			return new C_Message(raw);
		} else if (raw.startsWith("L:")) {
			return new L_Message(raw);
		} else if (raw.startsWith("S:")) {
			return new S_Message(raw);
		} else {
			logger.debug("Unknown message block: '{}'", raw);
		}
		return null;
	}

	/**
	 * Processes the message
	 * 
	 * @param Message
	 *            the decoded message data
	 */
	private void processMessage(Message message) {

		if (message != null) {
			message.debug(logger);
			if (message.getType() == MessageType.M) {
				M_Message msg = (M_Message) message;
				for (DeviceInformation di : msg.devices) {
					DeviceConfiguration c = null;
					for (DeviceConfiguration conf : configurations) {
						if (conf.getSerialNumber().equalsIgnoreCase(di.getSerialNumber())) {
							c = conf;
							break;
						}
					}

					if (c != null) {
						configurations.remove(c);
					}

					c = DeviceConfiguration.create(di);
					configurations.add(c);

					c.setRoomId(di.getRoomId());
				}
			} else if (message.getType() == MessageType.C) {
				DeviceConfiguration c = null;
				for (DeviceConfiguration conf : configurations) {
					if (conf.getSerialNumber().equalsIgnoreCase(((C_Message) message).getSerialNumber())) {
						c = conf;
						break;
					}
				}

				if (c == null) {
					configurations.add(DeviceConfiguration.create(message));
				} else {
					c.setValues((C_Message) message);
				}
			} else if (message.getType() == MessageType.L) {
				((L_Message) message).updateDevices(devices, configurations);
				logger.trace("{} devices found.", devices.size());
			} else if (message.getType() == MessageType.S) {
				dutyCycle = ((S_Message) message).getDutyCycle();
				freeMemorySlots = ((S_Message) message).getFreeMemorySlots();
				if (((S_Message) message).isCommandDiscarded()) {
					logger.info("Last Send Command discarded. Duty Cycle: {}, Free Memory Slots: {}", dutyCycle,
							freeMemorySlots);
				} else
					logger.debug("S message. Duty Cycle: {}, Free Memory Slots: {}", dutyCycle, freeMemorySlots);
			}
		}
	}

	private Device getDevice(String serialNumber, ArrayList<Device> devices) {
		for (Device device : devices) {
			if (device.getSerialNumber().toUpperCase().equals(serialNumber)) {
				return device;
			}
		}
		return null;
	}

	/**
	 * Returns the MAX! Device decoded during the last refreshData
	 * 
	 * @param serialNumber
	 *            the serial number of the device as String
	 * @return device the {@link Device} information decoded in last refreshData
	 */

	public Device getDevice(String serialNumber) {
		return getDevice(serialNumber, devices);
	}

	/**
	 * Takes the device command and puts it on the command queue to be processed
	 * by the MAX!Cube Lan Gateway. Note that if multiple commands for the same
	 * item-channel combination are send prior that they are processed by the
	 * Max!Cube, they will be removed from the queue as they would not be
	 * meaningful. This will improve the behavior when using sliders in the GUI.
	 * 
	 * @param SendCommand
	 *            the SendCommand containing the serial number of the device as
	 *            String the channelUID used to send the command and the the
	 *            command data
	 */
	public synchronized void queueCommand(SendCommand sendCommand) {

		if (commandQueue.offer(sendCommand)) {
			if (lastCommandId != null) {
				if (lastCommandId.getKey().equals(sendCommand.getKey())) {
					if (commandQueue.remove(lastCommandId))
						logger.debug("Removed Command id {} ({}) from queue. Superceeded by {}", lastCommandId.getId(),
								lastCommandId.getKey(), sendCommand.getId());
				}
			}
			lastCommandId = sendCommand;
			logger.debug("Command queued id {} ({}).", sendCommand.getId(), sendCommand.getKey());

		} else {
			logger.debug("Command queued full dropping command id {} ({}).", sendCommand.getId(), sendCommand.getKey());
		}

	}

	/**
	 * Processes device command and sends it to the MAX!Cube Lan Gateway.
	 * 
	 * @param SendCommand
	 *            the SendCommand containing the serial number of the device as
	 *            String the channelUID used to send the command and the the
	 *            command data
	 */
	public void executeCommand(SendCommand sendCommand) {

		String serialNumber = sendCommand.getDeviceSerial();
		ChannelUID channelUID = sendCommand.getChannelUID();
		Command command = sendCommand.getCommand();

		// send command to MAX!Cube LAN Gateway
		HeatingThermostat device = (HeatingThermostat) getDevice(serialNumber, devices);

		if (device == null) {
			logger.debug("Cannot send command to device with serial number {}, device not listed.", serialNumber);
			return;
		}

		String rfAddress = device.getRFAddress();
		String commandString = null;

		// Temperature setting
		if (channelUID.getId().equals(CHANNEL_SETTEMP)) {

			if (command instanceof DecimalType || command instanceof OnOffType) {
				DecimalType decimalType = DEFAULT_OFF_TEMPERATURE;
				if (command instanceof DecimalType) {
					decimalType = (DecimalType) command;
				} else if (command instanceof OnOffType) {
					decimalType = OnOffType.ON.equals(command) ? DEFAULT_ON_TEMPERATURE : DEFAULT_OFF_TEMPERATURE;
				}

				S_Command cmd = new S_Command(rfAddress, device.getRoomId(), device.getMode(),
						decimalType.doubleValue());
				commandString = cmd.getCommandString();
			}
			// Mode setting
		} else if (channelUID.getId().equals(CHANNEL_MODE)) {
			if (command instanceof StringType) {
				String commandContent = command.toString().trim().toUpperCase();
				S_Command cmd = null;
				ThermostatModeType commandThermoType = null;
				Double setTemp = Double.parseDouble(device.getTemperatureSetpoint().toString());
				if (commandContent.contentEquals(ThermostatModeType.AUTOMATIC.toString())) {
					commandThermoType = ThermostatModeType.AUTOMATIC;
					cmd = new S_Command(rfAddress, device.getRoomId(), commandThermoType, 0D);
				} else if (commandContent.contentEquals(ThermostatModeType.BOOST.toString())) {
					commandThermoType = ThermostatModeType.BOOST;
					cmd = new S_Command(rfAddress, device.getRoomId(), commandThermoType, setTemp);
				} else if (commandContent.contentEquals(ThermostatModeType.MANUAL.toString())) {
					commandThermoType = ThermostatModeType.MANUAL;
					cmd = new S_Command(rfAddress, device.getRoomId(), commandThermoType, setTemp);
					logger.debug("updates to MANUAL mode with temperature '{}'", setTemp);
				} else {
					logger.debug("Only updates to AUTOMATIC & BOOST & MANUAL supported, received value :'{}'",
							commandContent);
					return;
				}
				commandString = cmd.getCommandString();
			}
		}
		// Actual sending of the data to the Max!Cube Lan Gateway
		synchronized (MaxCubeBridgeHandler.class) {
			if (commandString != null) {
				try {
					if (socket == null) {
						this.socketConnect();
					}
					writer.write(commandString);
					writer.flush();
					String raw = reader.readLine();
					Message message = processRawMessage(raw);
					if (message != null)
						processMessage(message);
					if (!exclusive) {
						socketClose();
					}

				} catch (UnknownHostException e) {
					logger.warn("Cannot establish connection with MAX! Cube lan gateway while sending command to '{}'",
							ipAddress);
					logger.debug(Utils.getStackTrace(e));
					socketClose(); // reconnect on next execution
				} catch (IOException e) {
					logger.warn("Cannot write data from MAX! Cube lan gateway while connecting to '{}'", ipAddress);
					logger.debug(Utils.getStackTrace(e));
					socketClose(); // reconnect on next execution
				}
				logger.debug("Command {} ({}) sent to MAX! Cube at IP: {}", sendCommand.getId(), sendCommand.getKey(),
						ipAddress);
				logger.trace("Command {} content: '{}'", sendCommand.getId(), commandString);
			} else {
				logger.debug("Null Command not sent to {}", ipAddress);
			}
		}
	}

	private boolean socketConnect() throws UnknownHostException, IOException {
		socket = new Socket(ipAddress, port);
		logger.debug("Open new connection... to {} port {}", ipAddress, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new OutputStreamWriter(socket.getOutputStream());
		return true;
	}

	private void socketClose() {
		try {
			socket.close();
		} catch (Exception e) {
		}
		socket = null;
	}

}
