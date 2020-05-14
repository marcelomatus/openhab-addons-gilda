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
package org.openhab.binding.heos.internal.resources;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Telnet} is an Telnet Client which handles the connection
 * to a network via the Telnet interface
 *
 * @author Johannes Einig - Initial contribution
 */
public class Telnet {
    private final Logger logger = LoggerFactory.getLogger(Telnet.class);

    private static final int READ_TIMEOUT = 3000;
    private static final int IS_ALIVE_TIMEOUT = 10000;

    private final HeosStringPropertyChangeListener eolNotifier = new HeosStringPropertyChangeListener();
    private final TelnetClient client = new TelnetClient();

    private String ip;
    private int port;

    // Has to be initialized because value is used later with readResult.concat() function
    private String readResult = "";
    private String readLineResult;
    private final List<String> readResultList = new ArrayList<>();

    private InetAddress address;
    private DataOutputStream outStream;
    private InputStream inputStream;
    private BufferedInputStream bufferedStream;

    /**
     * Connects to a host with the specified IP address and port
     *
     * @param ip IP Address of the host
     * @param port where to be connected
     * @return True if connection was successful
     * @throws SocketException
     * @throws IOException
     */
    public boolean connect(String ip, int port) throws SocketException, IOException {
        this.ip = ip;
        this.port = port;
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            logger.debug("Unknown Host Exception - Message: {}", e.getMessage());
        }
        return openConnection();
    }

    private boolean openConnection() throws IOException {
        client.setConnectTimeout(5000);
        client.connect(ip, port);
        outStream = new DataOutputStream(client.getOutputStream());
        inputStream = client.getInputStream();
        bufferedStream = new BufferedInputStream(inputStream);
        return client.isConnected();
    }

    /**
     * Appends \r\n to the command.
     * For clear send use sendClear
     *
     * @param command The command to be send
     * @return true after the command was send
     * @throws IOException
     */
    public boolean send(String command) throws IOException {
        if (client.isConnected()) {
            sendClear(command + "\r\n");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Send command without additional commands
     *
     * @param command The command to be send
     * @throws IOException
     */
    private void sendClear(String command) throws IOException {
        if (!client.isConnected()) {
            return;
        }

        outStream.writeBytes(command);
        outStream.flush();
    }

    /**
     * Read all commands till an End Of Line is detected
     * I more than one line is read every line is an
     * element in the returned {@code ArrayList<>}
     * Reading timed out after 3000 milliseconds. For another timing
     *
     * @see Telnet.readLine(int timeOut).
     *
     * @return A list with all read commands
     * @throws ReadException
     * @throws IOException
     */
    public List<String> readLine() throws ReadException, IOException {
        return readLine(READ_TIMEOUT);
    }

    /**
     * Read all commands till an End Of Line is detected
     * I more than one line is read every line is an
     * element in the returned {@code ArrayList<>}
     * Reading time out is defined by parameter in
     * milliseconds.
     *
     * @param timeOut the time in millis after reading times out
     * @return A list with all read commands
     * @throws ReadException
     * @throws IOException
     */
    public List<String> readLine(int timeOut) throws ReadException, IOException {
        readResultList.clear();
        long timeZero = System.currentTimeMillis();
        if (client.isConnected()) {
            readLineResult = "";
            boolean done = false;
            while (!done) {
                int available = bufferedStream.available();
                byte[] buffer = new byte[available];
                bufferedStream.read(buffer);
                String str = new String(buffer, StandardCharsets.UTF_8);
                done = concatReadLineResult(str);
                if (System.currentTimeMillis() - timeZero >= timeOut) {
                    throw new ReadException();
                }
            }
        } else {
            readResultList.add(null);
        }
        return readResultList;
    }

    /*
     * It seems to be that sometime a command is still
     * in the reading line without being read out. This
     * shall be prevented with an Map which reads until no
     * End of line is detected. Each element of the list
     * should be a JSON Element
     */
    private synchronized boolean concatReadLineResult(String value) {
        readLineResult = readLineResult.concat(value);
        if (readLineResult.endsWith("\r\n")) {
            readLineResult = readLineResult.trim();
            while (readLineResult.contains("\r\n")) {
                int indexFirstElement = readLineResult.indexOf("\r\n");
                readResultList.add(readLineResult.substring(0, indexFirstElement));
                readLineResult = readLineResult.substring(indexFirstElement);
                readLineResult = readLineResult.trim();
            }
            readResultList.add(readLineResult);
            return true;
        }
        return false;
    }

    /**
     * Disconnect Telnet and close all Streams
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        client.disconnect();
        inputStream = null;
        outStream = null;
    }

    /**
     * Input Listener which fires event if input is detected
     */
    public void startInputListener() {
        logger.debug("Starting input listener");
        client.setReaderThread(true);
        client.registerInputListener(this::inputAvailableRead);
    }

    public void stopInputListener() {
        logger.debug("Stopping input listener");
        client.unregisterInputListener();
    }

    /**
     * Reader for InputListenerOnly which only reads the
     * available data without any check
     *
     */
    private void inputAvailableRead() {
        try {
            int i = bufferedStream.available();
            byte[] buffer = new byte[i];
            bufferedStream.read(buffer);
            String str = new String(buffer, StandardCharsets.UTF_8);
            concatReadResult(str);
        } catch (IOException e) {
            logger.debug("IO Exception, message: {}", e.getMessage());
        }
    }

    /**
     * Read values until end of line is reached.
     * Then fires event for change Listener.
     *
     * @return -1 to indicate that end of line is reached
     *         else returns 0
     */
    private int concatReadResult(String value) {
        readResult = readResult.concat(value);
        if (readResult.contains("\r\n")) {
            eolNotifier.setValue(readResult.trim());
            readResult = "";
            return -1;
        }
        return 0;
    }

    /**
     * Checks if the HEOS system is reachable
     * via the network. This does not check if
     * a Telnet connection is open.
     *
     * @return true if HEOS is reachable
     */
    public boolean isHostReachable() {
        try {
            return address != null && address.isReachable(IS_ALIVE_TIMEOUT);
        } catch (IOException e) {
            logger.debug("IO Exception- Message: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        return "Telnet{" + "ip='" + ip + '\'' + ", port=" + port + '}';
    }

    public HeosStringPropertyChangeListener getReadResultListener() {
        return eolNotifier;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public static class ReadException extends Exception {
        private static final long serialVersionUID = 1L;

        public ReadException() {
            super("Can not read from client");
        }
    }
}
