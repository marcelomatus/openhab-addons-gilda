/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nikohomecontrol.internal.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.binding.nikohomecontrol.handler.NikoHomeControlBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * The {@link NikoHomeControlCommunication} class is able to do the following tasks with Niko Home Control
 * systems:
 * <ul>
 * <li>Start and stop TCP socket connection with Niko Home Control IP-interface.
 * <li>Read all setup and status information from the Niko Home Control Controller.
 * <li>Execute Niko Home Control commands.
 * <li>Listen to events from Niko Home Control.
 * </ul>
 *
 * Only switch, dimmer and rollershutter actions are currently implemented.
 *
 * A class instance is instantiated from the {@link NikoHomeControlBridgeHandler} class initialization.
 *
 * @author Mark Herwege
 */
public final class NikoHomeControlCommunication {

    private Logger logger = LoggerFactory.getLogger(NikoHomeControlCommunication.class);

    private boolean fixedIp;
    private InetAddress nhcAddress;
    private int nhcPort;
    private InetAddress broadcastAddr;

    private Socket nhcSocket;
    private PrintWriter nhcOut;
    private BufferedReader nhcIn;

    private boolean listenerStopped;
    private boolean nhcEventsRunning;

    // We keep only 2 gson adapters used to serialize and deserialize all messages sent and received
    private Gson gsonOut = new Gson();
    private Gson gsonIn;

    private final NhcSystemInfo systemInfo = new NhcSystemInfo();
    private final Map<Integer, NhcLocation> locations = new HashMap<>();
    private final Map<Integer, NhcAction> actions = new HashMap<>();

    private NikoHomeControlBridgeHandler bridgeCallBack;

    /**
     * Constructor for Niko Home Control communication object, manages communication with
     * Niko Home Control IP-interface.
     *
     * @param addr Can be null or omitted, will attempt to discover IP address if omitted.
     *
     * @throws IOException when Niko Home Control IP-interface cannot be found
     */
    public NikoHomeControlCommunication(InetAddress addr, InetAddress broadcastAddr) throws IOException {

        this.broadcastAddr = broadcastAddr;

        if (addr == null) {
            NikoHomeControlDiscover nhcDiscover = new NikoHomeControlDiscover(broadcastAddr);
            this.nhcAddress = nhcDiscover.getAddr();
            this.fixedIp = false;
        } else {
            this.nhcAddress = addr;
            this.fixedIp = true;
        }

        // When we set up this object, we also want to get the proper gson adapter set up once
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(NhcMessageBase.class, new NikoHomeControlMessageDeserializer());
        this.gsonIn = gsonBuilder.create();
    }

    public NikoHomeControlCommunication() throws IOException {

        this(null, null);
    }

    /**
     * Start communication with Niko Home Control IP-interface, run through initialization and start thread listening
     * to all messages coming from Niko Home Control.
     *
     */
    public void startCommunication(int port) {

        try {
            for (int i = 1; nhcEventsRunning && (i <= 5); i++) {
                // the events listener thread did not finish yet, so wait max 5000ms before restarting
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new IOException();
                }
            }
            if (nhcEventsRunning) {
                logger.error(
                        "Niko Home Control: starting from thread {}, but previous connection still active after 5000ms",
                        Thread.currentThread().getId());
                throw new IOException();
            }

            this.nhcPort = port;
            this.nhcSocket = new Socket(this.nhcAddress, this.nhcPort);
            this.nhcOut = new PrintWriter(this.nhcSocket.getOutputStream(), true);
            this.nhcIn = new BufferedReader(new InputStreamReader(this.nhcSocket.getInputStream()));
            logger.info("Niko Home Control: connected from thread {}", Thread.currentThread().getId());

            // initialize all info in local fields
            initialize();

            // Start Niko Home Control event listener. This listener will act on all messages coming from
            // IP-interface.
            (new Thread(nhcEvents)).start();

        } catch (IOException e) {
            // if the error occurs in the initialization, don't try to restart
            logger.warn("Niko Home Control: error initializing communication from thread {}",
                    Thread.currentThread().getId());
            stopCommunication();
            // take bridge offline
            this.bridgeCallBack.bridgeOffline();
        }

    }

    /**
     * Cleanup socket when the communication with Niko Home Control IP-interface is closed.
     *
     */
    public synchronized void stopCommunication() {
        this.listenerStopped = true;

        if (this.nhcSocket != null) {
            try {
                this.nhcSocket.close();
            } catch (IOException ignore) {
                // ignore IO Error when trying to close the socket if the intention is to close it anyway
            }
            this.nhcSocket = null;
        }
        logger.warn("Niko Home Control: communication stopped from thread {}", Thread.currentThread().getId());

    }

    /**
     * Close and restart communication with Niko Home Control IP-interface.
     *
     */
    public void restartCommunication() {
        stopCommunication();

        logger.info("Niko Home Control: restart communication from thread {}", Thread.currentThread().getId());
        if (!this.fixedIp) {
            try {
                NikoHomeControlDiscover nhcDiscover = new NikoHomeControlDiscover(broadcastAddr);
                this.nhcAddress = nhcDiscover.getAddr();
            } catch (IOException e) {
                // if the error occurs in the initialization, don't try to restart
                logger.warn("Niko Home Control: cannot find IP-interface");
                // take bridge offline
                this.bridgeCallBack.bridgeOffline();
                return;
            }
        }
        startCommunication(this.nhcPort);

    }

    /**
     * Method to check if communication with Niko Home Control IP-interface is active
     *
     * @return True if active
     */
    public boolean communicationActive() {
        return (this.nhcSocket != null);
    }

    /**
     * Runnable that handles inbound communication from Niko Home Control.
     * <p>
     * The thread listens to the TCP socket opened at instantiation of the {@link NikoHomeControlCommunication} class
     * and interprets all inbound json messages. It triggers state updates for active channels linked to the Niko Home
     * Control actions. It is started after initialization of the communication.
     *
     */
    private Runnable nhcEvents = new Runnable() {

        @Override
        public void run() {
            String nhcMessage;

            logger.debug("Niko Home Control: listening for events on thread {}", Thread.currentThread().getId());
            listenerStopped = false;
            nhcEventsRunning = true;

            try {
                while (!listenerStopped & ((nhcMessage = nhcIn.readLine()) != null)) {
                    readMessage(nhcMessage);
                }
            } catch (IOException e) {
                if (!listenerStopped) {
                    nhcEventsRunning = false;
                    // this is not an communication stop triggered from outside this runnable
                    logger.warn("Niko Home Control: IO error in listener on thread {}", Thread.currentThread().getId());
                    // the IO has stopped working, so we need to close cleanly and try to restart
                    restartCommunication();
                    return;
                }
            }

            nhcEventsRunning = false;
            // this is a stop from outside the runnable, so just log it and stop
            logger.debug("Niko Home Control: event listener thread stopped on thread {}",
                    Thread.currentThread().getId());
        }

    };

    /**
     * Method that interprets all feedback from Niko Home Control and calls appropriate handling methods.
     *
     * @param nhcMessage message read from Niko Home Control.
     */
    private void readMessage(String nhcMessage) {

        logger.debug("Niko Home Control: received json {} on thread {}", nhcMessage, Thread.currentThread().getId());

        try {
            NhcMessageBase nhcMessageGson = this.gsonIn.fromJson(nhcMessage, NhcMessageBase.class);

            String cmd = nhcMessageGson.getCmd();
            String event = nhcMessageGson.getEvent();

            if ("systeminfo".equals(cmd)) {
                cmdSystemInfo(((NhcMessageMap) nhcMessageGson).getData());
            } else if ("startevents".equals(cmd)) {
                cmdStartEvents(((NhcMessageMap) nhcMessageGson).getData());
            } else if ("listlocations".equals(cmd)) {
                cmdListLocations(((NhcMessageListMap) nhcMessageGson).getData());
            } else if ("listactions".equals(cmd)) {
                cmdListActions(((NhcMessageListMap) nhcMessageGson).getData());
            } else if ("executeactions".equals(cmd)) {
                cmdExecuteActions(((NhcMessageMap) nhcMessageGson).getData());
            } else if ("listactions".equals(event)) {
                eventListActions(((NhcMessageListMap) nhcMessageGson).getData());
            } else {
                logger.debug("Niko Home Control: not acted on json {}", nhcMessage);
            }
        } catch (JsonParseException e) {
            logger.debug("Niko Home Control: not acted on unsupported json {}", nhcMessage);
        }

    }

    /**
     * After setting up the communication with the Niko Home Control IP-interface, send all initialization messages.
     * <p>
     * Only at first initialization, also set the return values. Otherwise use the runnable to get updated values.
     * While communication is set up for thermostats, tariff data and alarms, only info from locations and actions
     * is used beyond this point in openHAB. All other elements are for future extensions.
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        sendAndReadMessage("systeminfo");
        sendAndReadMessage("startevents");
        sendAndReadMessage("listlocations");
        sendAndReadMessage("listactions");
        sendAndReadMessage("listthermostat");
        sendAndReadMessage("listthermostatHVAC");
        sendAndReadMessage("readtariffdata");
        sendAndReadMessage("getalarms");

    }

    private void sendAndReadMessage(String command) throws IOException {
        sendMessage(new NhcMessageCmd(command));
        readMessage(this.nhcIn.readLine());
    }

    private void cmdSystemInfo(Map<String, String> data) {

        logger.debug("Niko Home Control: systeminfo");

        if (data.containsKey("swversion")) {
            this.systemInfo.setSwVersion(data.get("swversion"));
        }
        if (data.containsKey("api")) {
            this.systemInfo.setApi(data.get("api"));
        }
        if (data.containsKey("time")) {
            this.systemInfo.setTime(data.get("time"));
        }
        if (data.containsKey("language")) {
            this.systemInfo.setLanguage(data.get("language"));
        }
        if (data.containsKey("currency")) {
            this.systemInfo.setCurrency(data.get("currency"));
        }
        if (data.containsKey("units")) {
            this.systemInfo.setUnits(data.get("units"));
        }
        if (data.containsKey("DST")) {
            this.systemInfo.setDst(data.get("DST"));
        }
        if (data.containsKey("TZ")) {
            this.systemInfo.setTz(data.get("TZ"));
        }
        if (data.containsKey("lastenergyerase")) {
            this.systemInfo.setLastEnergyErase(data.get("lastenergyerase"));
        }
        if (data.containsKey("lastconfig")) {
            this.systemInfo.setLastConfig(data.get("lastconfig"));
        }
    }

    private void cmdStartEvents(Map<String, String> data) {

        Integer errorCode = Integer.valueOf(data.get("error"));

        if (errorCode.equals(0)) {
            logger.debug("Niko Home Control: start events success");
        } else {
            logger.warn("Niko Home Control: error code {} returned on start events", errorCode);
        }
    }

    private void cmdListLocations(List<HashMap<String, String>> data) {

        logger.debug("Niko Home Control: list locations");

        this.locations.clear();

        for (HashMap<String, String> location : data) {
            int id = Integer.valueOf(location.get("id"));
            String name = location.get("name");
            NhcLocation nhcLocation = new NhcLocation(name);
            this.locations.put(id, nhcLocation);
        }
    }

    private void cmdListActions(List<HashMap<String, String>> data) {

        logger.debug("Niko Home Control: list actions");

        for (HashMap<String, String> action : data) {

            int id = Integer.valueOf(action.get("id"));
            Integer state = Integer.valueOf(action.get("value1"));

            if (!this.actions.containsKey(id)) {
                // Initial instantiation of NhcAction class for action object
                String name = action.get("name");
                Integer type = Integer.valueOf(action.get("type"));
                Integer locationId = Integer.valueOf(action.get("location"));
                String location = null;
                if (locationId != null) {
                    location = this.locations.get(locationId).getName();
                }
                NhcAction nhcAction = new NhcAction(id, name, type, location);
                nhcAction.setState(state);
                nhcAction.setNhcComm(this);
                this.actions.put(id, nhcAction);
            } else {
                // Action object already exists, so only update state.
                // If we would re-instantiate action, we would lose pointer back from action to thing handler that was
                // set in thing handler initialize().
                this.actions.get(id).setState(state);
            }
        }
    }

    private void cmdExecuteActions(Map<String, String> data) {

        Integer errorCode = Integer.valueOf(data.get("error"));
        if (errorCode.equals(0)) {
            logger.debug("Niko Home Control: execute action success");
        } else {
            logger.warn("Niko Home Control: error code {} returned on command execution", errorCode);
        }
    }

    private void eventListActions(List<HashMap<String, String>> data) {

        for (HashMap<String, String> action : data) {
            int id = Integer.valueOf(action.get("id"));
            if (!this.actions.containsKey(id)) {
                logger.warn("Niko Home Control: action in controller not known to openHab {}", id);
                return;
            }
            Integer state = Integer.valueOf(action.get("value1"));
            logger.debug("Niko Home Control: event execute action {} with state {}", id, state);
            try {
                this.actions.get(id).setState(state);
            } catch (Exception e) {
                logger.error("Niko Home Control: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Called by other methods to send json cmd to Niko Home Control.
     *
     * @param nhcMessage
     */
    public void sendMessage(Object nhcMessage) {
        String json = gsonOut.toJson(nhcMessage);
        logger.debug("Niko Home Control: send json {}", json);
        this.nhcOut.println(json);
        if (this.nhcOut.checkError()) {
            logger.warn("Niko Home Control: error sending message, trying to restart communication from thread {}",
                    Thread.currentThread().getId());
            restartCommunication();
            // retry sending after restart
            this.nhcOut.println(json);
        }
    }

    /**
     * Return IP address of Niko Home Control IP-interface.
     *
     * @return IP address
     */
    public InetAddress getAddr() {
        return this.nhcAddress;
    }

    /**
     * Return socket of Niko Home Control IP-interface.
     *
     * @return port
     */
    public int getPort() {
        return this.nhcPort;
    }

    /**
     * Return the object with system info as read from the Niko Home Control controller.
     *
     * @return the systemInfo
     */
    public NhcSystemInfo getSystemInfo() {
        return this.systemInfo;
    }

    /**
     * Return all actions in the Niko Home Control Controller.
     *
     * @return <code>Map&ltInteger, {@link NhcAction}></code>
     */
    public Map<Integer, NhcAction> getActions() {
        return this.actions;
    }

    /**
     * @param bridgeCallBack the bridgeCallBack to set
     */
    public void setBridgeCallBack(NikoHomeControlBridgeHandler bridgeCallBack) {
        this.bridgeCallBack = bridgeCallBack;
    }

}
