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
package org.openhab.binding.etherrain.internal.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.openhab.binding.etherrain.internal.EtherRainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EtherRainCommunication} handles communication with EtherRain
 * controllers so that the API is all in one place
 *
 * @author Joe Inkenbrandt - Initial contribution
 */
public class EtherRainCommunication {

    private static final String BROADCAST_DISCOVERY_MESSAGE = "eviro_id_request1";
    private static final int BROADCAST_DISCOVERY_PORT = 8089;

    private static final String ETHERRAIN_USERNAME = "admin";

    private static final int HTTP_TIMEOUT = 3;

    private static final int BROADCAST_TIMEOUT = 80;

    private static final String RESPONSE_STATUS_PATTERN = "^\\s*(un|ma|ac|os|cs|rz|ri|rn):\\s*([a-zA-Z0-9\\.]*)(\\s*<br>)?";
    private static final String BROADCAST_RESPONSE_DISCOVER_PATTERN = "eviro t=(\\S*) n=(\\S*) p=(\\S*) a=(\\S*)";

    private final Logger logger = LoggerFactory.getLogger(EtherRainCommunication.class);
    private final HttpClient httpClient;

    private final String address;
    private final int port;
    private final String password;

    public EtherRainCommunication(String address, int port, String password, HttpClient httpClient) {
        this.address = address;
        this.port = port;
        this.password = password;
        this.httpClient = httpClient;
    }

    public static EtherRainUdpResponse autoDiscover() {
        return updBroadcast();
    }

    public EtherRainStatusResponse commandStatus() throws EtherRainException, IOException {
        if (!commandLogin()) {
            throw new EtherRainException("could not login");
        }

        List<String> responseList = null;

        responseList = sendGet("result.cgi?xs");

        if (responseList == null) {
            throw new EtherRainException("Empty Response");
        }

        Iterator<String> i = responseList.iterator();

        EtherRainStatusResponse response = new EtherRainStatusResponse();

        while (i.hasNext()) {
            String line = i.next();

            if (line.matches(RESPONSE_STATUS_PATTERN)) {
                String command = line.replaceFirst(RESPONSE_STATUS_PATTERN, "$1");
                String status = line.replaceFirst(RESPONSE_STATUS_PATTERN, "$2");

                switch (command) {
                    case "un":
                        response.setUniqueName(status);
                        break;
                    case "ma":
                        response.setMacAddress(status);
                        break;
                    case "ac":
                        response.setServiceAccount(status);
                        break;
                    case "os":
                        response.setOperatingStatus(EtherRainOperatingStatus.fromString(status));
                        break;
                    case "cs":
                        response.setLastCommandStatus(EtherRainCommandStatus.fromString(status));
                        break;
                    case "rz":
                        response.setLastCommandResult(EtherRainCommandResult.fromString(status));
                        break;
                    case "ri":
                        response.setLastActiveValue(Integer.parseInt(status));
                        break;
                    case "rn":
                        response.setRainSensor(Integer.parseInt(status) == 1 ? true : false);
                        break;
                    default:
                        logger.debug("Unknown response: {}", command);
                }
            }
        }

        return response;
    }

    public boolean commandIrrigate(int delay, int zone1, int zone2, int zone3, int zone4, int zone5, int zone6,
            int zone7, int zone8) {
        try {
            if (sendGet("result.cgi?xi=" + delay + ":" + zone1 + ":" + zone2 + ":" + zone3 + ":" + zone4 + ":" + zone5
                    + ":" + zone6 + ":" + zone7 + ":" + zone8) == null) {
                return false;
            }
        } catch (IOException e) {
            logger.warn("Could not send irrigate command to etherrain: {}", e.getMessage());
            return false;
        }

        return true;
    }

    public boolean commandClear() {
        try {
            if (sendGet("/result.cgi?xr") == null) {
                return false;
            }
        } catch (IOException e) {
            logger.warn("Could not send clear command to etherrain: {}", e.getMessage());
            return false;
        }

        return true;
    }

    public boolean commandLogin() throws IOException {
        return sendGet("/ergetcfg.cgi?lu=" + ETHERRAIN_USERNAME + "&lp=" + password) != null;
    }

    public boolean commandLogout() {
        try {
            if (sendGet("/ergetcfg.cgi?m=o") == null) {
                return false;
            }
        } catch (IOException e) {
            logger.warn("Could not send logout command to etherrain: {}", e.getMessage());
            return false;
        }

        return true;
    }

    private List<String> sendGet(String command) throws IOException {
        String url = "http://" + address + ":" + port + "/" + command;

        ContentResponse response;

        try {
            response = httpClient.newRequest(url).timeout(HTTP_TIMEOUT, TimeUnit.SECONDS).send();
            if (response.getStatus() != HttpURLConnection.HTTP_OK) {
                logger.warn("Etherrain return status other than HTTP_OK : {}", response.getStatus());
                return null;
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.warn("Could not connect to Etherrain with exception: {}", e.getMessage());
            return null;
        }

        List<String> rVal = new BufferedReader(new StringReader(response.getContentAsString())).lines()
                .collect(Collectors.toList());

        return rVal;
    }

    private static EtherRainUdpResponse updBroadcast() {
        DatagramSocket c;

        // Find the server using UDP broadcast
        try {
            // Open a random port to send the package
            c = new DatagramSocket();
            c.setSoTimeout(BROADCAST_TIMEOUT);
            c.setBroadcast(true);

            byte[] sendData = BROADCAST_DISCOVERY_MESSAGE.getBytes();

            // Try the 255.255.255.255 first
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                    InetAddress.getByName("255.255.255.255"), BROADCAST_DISCOVERY_PORT);
            c.send(sendPacket);

            // Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket;
            try {
                receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                c.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                c.close();
                return null;
            }

            // Check if the message is correct
            String message = new String(receivePacket.getData()).trim();

            if (message.length() == 0) {
                c.close();
                return null;
            }

            String addressBC = receivePacket.getAddress().getHostAddress();
            String deviceTypeBC = message.replaceAll(BROADCAST_RESPONSE_DISCOVER_PATTERN, "$1");
            String unqiueNameBC = message.replaceAll(BROADCAST_RESPONSE_DISCOVER_PATTERN, "$2");
            int portBC = Integer.parseInt(message.replaceAll(BROADCAST_RESPONSE_DISCOVER_PATTERN, "$3"));

            c.close();

            // NOTE: Version 3.77 of API states that Additional Parameters (a=) are not used
            // on EtherRain
            return new EtherRainUdpResponse(deviceTypeBC, addressBC, portBC, unqiueNameBC, null);
        } catch (IOException ex) {
            return null;
        }
    }
}
