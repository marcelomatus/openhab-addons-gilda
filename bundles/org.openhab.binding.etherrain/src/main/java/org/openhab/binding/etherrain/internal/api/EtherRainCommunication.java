/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.etherrain.internal.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openhab.binding.etherrain.internal.api.EtherRainStatusResponse.CommandResult;
import org.openhab.binding.etherrain.internal.api.EtherRainStatusResponse.CommandStatus;
import org.openhab.binding.etherrain.internal.api.EtherRainStatusResponse.OperatingStatus;

public class EtherRainCommunication {

    private final static String BROADCAST_DISCOVERY_MESSAGE = "eviro_id_request1";
    private final static int BROADCAST_DISCOVERY_PORT = 8089;

    private final static String ETHERRAIN_USERNAME = "admin";

    private final static int HTTP_TIMEOUT = 1000;

    private final static int BROADCAST_TIMEOUT = 80;

    private String address = null;
    private int port = 80;
    private String password = "pw";

    public EtherRainCommunication() {

    }

    public EtherRainCommunication(String address, int port, String password) {
        this.address = address;
        this.port = port;
        this.password = password;
    }

    public static EtherRainUdpResponse autoDiscover() {
        return updBroadcast();
    }

    private static final String RESPONSE_STATUS_PATTERN = "^\\s*(un|ma|ac|os|cs|rz|ri|rn):\\s*([a-zA-Z0-9\\.]*)(\\s*<br>)?";

    public EtherRainStatusResponse commandStatus() throws Exception {
        if (address == null || port == 0) {
            throw new Exception("address and port not set");
        }

        if (!commandLogin()) {
            throw new Exception("could not login");
        }

        List<String> responseList = null;

        responseList = sendGet("result.cgi?xs");

        if (responseList == null) {
            throw new Exception("Empty Response");
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
                        response.setOperatingStatus(OperatingStatus.fromString(status));
                        break;
                    case "cs":
                        response.setLastCommandStatus(CommandStatus.fromString(status));
                        break;
                    case "rz":
                        response.setLastCommandResult(CommandResult.fromString(status));
                        break;
                    case "ri":
                        response.setLastActiveValue(Integer.parseInt(status));
                        break;
                    case "rn":
                        response.setRainSensor(Integer.parseInt(status) == 1 ? true : false);
                        break;
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
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean commandClear() {
        try {
            if (sendGet("/result.cgi?xr") == null) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean commandLogin() throws Exception {
        return sendGet("/ergetcfg.cgi?lu=" + ETHERRAIN_USERNAME + "&lp=" + password) != null;
    }

    public boolean commandLogout() {

        try {
            if (sendGet("/ergetcfg.cgi?m=o") == null) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private final String USER_AGENT = "Mozilla/5.0";

    private List<String> sendGet(String command) throws Exception {

        String url = "http://" + address + ":" + port + "/" + command;

        List<String> rVal = null;

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");
        con.setReadTimeout(HTTP_TIMEOUT);
        con.setConnectTimeout(HTTP_TIMEOUT);

        // add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        if (con.getResponseCode() != 200) {
            return null;
        }

        rVal = new LinkedList<String>();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            rVal.add(inputLine);
        }
        in.close();

        return rVal;

    }

    private static final String BROADCAST_RESPONSE_DISCOVER_PATTERN = "eviro t=(\\S*) n=(\\S*) p=(\\S*) a=(\\S*)";

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

            // NOTE: Version 3.77 of API states that Additional Parameters (a=) are not used on EtherRain
            return new EtherRainUdpResponse(deviceTypeBC, addressBC, portBC, unqiueNameBC, null);
        } catch (IOException ex) {
            return null;
        }

    }

}