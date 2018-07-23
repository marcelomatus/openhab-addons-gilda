/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.souliss.internal.protocol;

import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.souliss.internal.discovery.SoulissDiscoverThread.DiscoverResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contain parameter of Souliss Network.
 * Those are loaded at startup from SoulissBinding.updated(), from file openhab.cfg
 * and used by SoulissBinding.execute(), SoulissCommGate.send(), UDPServerThread, decodeDBStructRequest.decodeMacaco
 *
 * @author Tonino Fazio
 * @since 1.7.0
 */
public class SoulissBindingNetworkParameters {

    public static short defaultNodeIndex = 130;
    public static short defaultUserIndex = 70;
    public static int presetTime = 1000;
    public static int SEND_DELAY = presetTime;
    public static int SEND_MIN_DELAY = presetTime;
    public static long SECURE_SEND_TIMEOUT_TO_REQUEUE = presetTime;
    public static long SECURE_SEND_TIMEOUT_TO_REMOVE_PACKET = presetTime;
    private static Logger logger = LoggerFactory.getLogger(SoulissBindingNetworkParameters.class);

    private static ConcurrentHashMap<Short, Thing> hashTableGateways = new ConcurrentHashMap<Short, Thing>();
    private static ConcurrentHashMap<String, Thing> hashTableTopics = new ConcurrentHashMap<String, Thing>();

    private static DatagramSocket datagramSocket;
    public static DiscoverResult discoverResult;
    private static SoulissBindingUDPServerThread UDP_Server;

    public static DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    public static void closeDatagramSocket() {
        datagramSocket.close();
        datagramSocket = null;
    }

    public static void setDatagramSocket(DatagramSocket datagramSocket) {
        SoulissBindingNetworkParameters.datagramSocket = datagramSocket;
    }

    public static void addGateway(short lastByteGatewayIP, Thing thing) {
        hashTableGateways.put(lastByteGatewayIP, thing);
    }

    public static void addTopics(String sUID, Thing thing) {
        hashTableTopics.put(sUID, thing);
    }

    public static ConcurrentHashMap<Short, Thing> getHashTableGateways() {
        return hashTableGateways;
    }

    public static ConcurrentHashMap<String, Thing> getHashTableTopics() {
        return hashTableTopics;
    }

    public static Thing getTopic(String sUID) {
        return hashTableTopics.get(sUID);
    }

    public static Bridge getGateway(short lastByteGatewayIP) {
        return (Bridge) hashTableGateways.get(lastByteGatewayIP);
    }

    public static void removeGateway(short lastByteGatewayIP) {
        hashTableGateways.remove(lastByteGatewayIP);
    }

    public static void removeTopic(String sUID) {
        hashTableTopics.remove(sUID);
    }

    public static void setUDPServer(SoulissBindingUDPServerThread _UDP_Server) {
        UDP_Server = _UDP_Server;

    }

    public static SoulissBindingUDPServerThread getUDPServer() {
        return UDP_Server;
    }
}
