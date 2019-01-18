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
package org.openhab.binding.lutron.internal.hw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.TooManyListenersException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 *
 * This is the main handler for HomeWorks RS232 Processors.
 *
 * @author Andrew Shilliday - Initial contribution
 *
 */
public class HwSerialBridgeHandler extends BaseBridgeHandler implements SerialPortEventListener {
    private final Logger logger = LoggerFactory.getLogger(HwSerialBridgeHandler.class);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String serialPortName;
    private int baudRate;
    private Boolean updateTime;
    private ScheduledFuture<?> updateTimeJob;

    private SerialPort serialPort;
    private OutputStreamWriter serialOutput;
    private BufferedReader serialInput;

    private HwDiscoveryService discoveryService;
    private ServiceRegistration<DiscoveryService> discoveryRegistration;

    public HwSerialBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing the Lutron HomeWorks RS232 bridge handler");
        HwSerialBridgeConfig configuration = getConfigAs(HwSerialBridgeConfig.class);
        serialPortName = configuration.getSerialPort();
        updateTime = configuration.getUpdateTime();
        if (configuration.getBaudRate() == null) {
            baudRate = HwSerialBridgeConfig.DEFAULT_BAUD;
        } else {
            baudRate = configuration.getBaudRate().intValue();
        }

        this.discoveryService = new HwDiscoveryService(this);
        this.discoveryRegistration = bundleContext.registerService(DiscoveryService.class, discoveryService, null);

        if (serialPortName == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Serial port not specified");
            return;
        }

        logger.debug("Lutron HomeWorks RS232 Bridge Handler Initializing.");
        logger.debug("   Serial Port: {},", serialPortName);
        logger.debug("   Baud:        {},", baudRate);

        scheduler.execute(() -> openConnection());
    }

    private void openConnection() {
        try {
            logger.info("Connecting to Lutron HomeWorks Processor using {}.", serialPortName);
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
            serialPort = portIdentifier.open(this.getClass().getName(), 2000);

            logger.debug("Connection established using {}.  Configuring IO parameters. ", serialPortName);

            int db = SerialPort.DATABITS_8, sb = SerialPort.STOPBITS_1, p = SerialPort.PARITY_NONE;
            serialPort.setSerialPortParams(baudRate, db, sb, p);
            serialPort.enableReceiveThreshold(1);
            serialPort.disableReceiveTimeout();
            serialOutput = new OutputStreamWriter(serialPort.getOutputStream(), "US-ASCII");
            serialInput = new BufferedReader(new InputStreamReader(serialPort.getInputStream(), "US-ASCII"));

            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            logger.debug("Sending monitoring commands.");
            sendCommand("PROMPTOFF");
            sendCommand("KBMOFF");
            sendCommand("KLMOFF");
            sendCommand("GSMOFF");
            sendCommand("DLMON"); // Turn on dimmer monitoring

            updateStatus(ThingStatus.ONLINE);

            if (updateTime) {
                startUpdateProcessorTimeJob();
            }

        } catch (NoSuchPortException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Invalid port: " + serialPortName);
        } catch (PortInUseException portInUseException) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Port in use: " + serialPortName);
        } catch (UnsupportedCommOperationException | IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Communication error");
        } catch (TooManyListenersException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Too many listeners to serial port.");
        }
    }

    private void startUpdateProcessorTimeJob() {
        if (updateTimeJob != null) {
            logger.debug("Canceling old scheduled job");
            updateTimeJob.cancel(false);
            updateTimeJob = null;
        }

        updateTimeJob = scheduler.scheduleWithFixedDelay(() -> updateProcessorTime(), 0, 1, TimeUnit.DAYS);
    }

    private void updateProcessorTime() {
        LocalDate date = LocalDate.now();
        String dateString = date.format(dateFormat);
        String timeString = date.format(timeFormat);
        logger.debug("Updating HomeWorks processor date and time to {} {}", dateString, timeString);

        if (!this.getBridge().getStatus().equals(ThingStatus.ONLINE)) {
            logger.warn("HomeWorks Bridge is offline and cannot update time on HomeWorks processor.");
            if (updateTimeJob != null) {
                updateTimeJob.cancel(false);
                updateTimeJob = null;
            }
            return;
        }

        sendCommand("SD, " + dateString);
        sendCommand("ST, " + timeString);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Unexpected command for HomeWorks Bridge: {} - {}", channelUID, command);
    }

    private void handleIncomingMessage(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        logger.debug("Received message from HomeWorks processor: {}", line);
        String[] data = line.replaceAll("\\s", "").toUpperCase().split(",");
        if ("DL".equals(data[0])) {
            try {
                String address = data[1];
                Integer level = Integer.parseInt(data[2]);
                HwDimmerHandler handler = findHandler(address);
                if (handler == null) {
                    discoveryService.declareUnknownDimmer(address);
                } else {
                    handler.handleLevelChange(level);
                }
            } catch (RuntimeException e) {
                logger.error("Error parsing incoming message", e);
            }
        }

    }

    private HwDimmerHandler findHandler(String address) {
        for (Thing thing : getThing().getThings()) {
            if (thing.getHandler() instanceof HwDimmerHandler) {
                HwDimmerHandler handler = (HwDimmerHandler) thing.getHandler();
                if (address.equals(handler.getAddress())) {
                    return handler;
                }
            }
        }
        return null;
    }

    /**
     * Receives Serial Port Events and reads Serial Port Data.
     *
     * @param serialPortEvent
     */
    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                while (true) {
                    String messageLine = serialInput.readLine();
                    if (messageLine == null) {
                        break;
                    }
                    handleIncomingMessage(messageLine);
                }
            } catch (IOException e) {
                logger.debug("Error reading from serial port: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Error reading from port");
            }
        }
    }

    public void sendCommand(String command) {
        try {
            logger.debug("HomeWorks bridge sending command: {}", command);
            serialOutput.write(command + "\r");
            serialOutput.flush();
        } catch (IOException e) {
            logger.debug("Error writing to serial port: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Error writing to port.");
        }
    }

    @Override
    public void dispose() {
        logger.info("HomeWorks bridge being disposed.");
        if (serialPort != null) {
            serialPort.close();
        }

        serialPort = null;
        serialInput = null;
        serialOutput = null;

        if (updateTimeJob != null) {
            updateTimeJob.cancel(false);
        }

        if (this.discoveryRegistration != null) {
            this.discoveryRegistration.unregister();
            this.discoveryRegistration = null;
        }
        logger.debug("Finished disposing bridge.");
    }

}
