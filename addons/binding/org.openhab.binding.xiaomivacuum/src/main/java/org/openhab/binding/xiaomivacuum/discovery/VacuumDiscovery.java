/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.xiaomivacuum.discovery;

import static org.openhab.binding.xiaomivacuum.XiaomiVacuumBindingConstants.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.xiaomivacuum.internal.Message;
import org.openhab.binding.xiaomivacuum.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VacuumDiscovery} is responsible for discovering new Xiaomi Robot Vacuums
 * and their token
 *
 * @author Marcel Verpaalen - Initial contribution
 *
 */
public class VacuumDiscovery extends AbstractDiscoveryService {

    /** The refresh interval for background discovery */
    private static final long SEARCH_INTERVAL = 600;
    private static final int TIMEOUT = 10000;
    private ScheduledFuture<?> roboDiscoveryJob;

    private final Logger logger = LoggerFactory.getLogger(VacuumDiscovery.class);

    public VacuumDiscovery() throws IllegalArgumentException {
        super(15);
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Start Xiaomi Robot Vacuum background discovery");
        if (roboDiscoveryJob == null || roboDiscoveryJob.isCancelled()) {
            roboDiscoveryJob = scheduler.scheduleWithFixedDelay(() -> discover(), 0, SEARCH_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stop Xiaomi Robot Vacuum background discovery");
        if (roboDiscoveryJob != null && !roboDiscoveryJob.isCancelled()) {
            roboDiscoveryJob.cancel(true);
            roboDiscoveryJob = null;
        }
    }

    private synchronized void discover() {
        TreeSet<String> broadcastAdresses = getBroadcastAddresses();
        HashMap<String, byte[]> responses = new HashMap<String, byte[]>();
        for (String broadcastAdress : broadcastAdresses) {
            responses.putAll(sendDiscoveryRequest(broadcastAdress));
        }
        for (Entry<String, byte[]> i : responses.entrySet()) {
            logger.trace("Discovery responses from : {}:{}", i.getKey(), Utils.getSpacedHex(i.getValue()));
            Message msg = new Message(i.getValue());
            logger.debug("Mi IO device time stamp: {}, OH time {}, delta {}", msg.getTimestamp(), LocalDateTime.now(),
                    LocalDateTime.now().compareTo(msg.getTimestamp()));
            String token = Utils.getHex(msg.getChecksum());
            String id = Utils.getHex(msg.getDeviceId());

            ThingUID uid = new ThingUID(THING_TYPE_MIIO, id);
            logger.debug("Discovered Mi IO Device {} at {}", id, i.getKey());
            if (IGNORED_TOLKENS.contains(token)) {
                logger.debug(
                        "No token discovered for device {}. To discover token reset your device & connect to it's wireless network and re-run discovery. Read readme for other options.",
                        id);
                thingDiscovered(DiscoveryResultBuilder.create(uid).withProperty(PROPERTY_HOST_IP, i.getKey())
                        .withRepresentationProperty(id).withLabel("Discovered Xiaomi Mi IO Device").build());
            } else {
                logger.debug("Discovered token for device {}: {} ('{}')", id, token, new String(msg.getChecksum()));
                thingDiscovered(DiscoveryResultBuilder.create(uid).withProperty(PROPERTY_HOST_IP, i.getKey())
                        .withProperty(PROPERTY_TOKEN, token).withRepresentationProperty(id)
                        .withLabel("Discovered Xiaomi Mi IO Device").build());
            }
        }
    }

    /**
     * @return broadcast addresses for all interfaces
     */
    private TreeSet<String> getBroadcastAddresses() {
        TreeSet<String> broadcastAddresses = new TreeSet<String>();
        try {
            broadcastAddresses.add("224.0.0.1");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        String address = interfaceAddress.getBroadcast().getHostAddress();
                        if (address != null) {
                            broadcastAddresses.add(address);
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            logger.trace("Error collecting broadcast addresses: {}", e.getMessage(), e);
        }
        return broadcastAddresses;
    }

    public HashMap<String, byte[]> sendDiscoveryRequest(String ipAddress) {
        HashMap<String, byte[]> responses = new HashMap<String, byte[]>();

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setReuseAddress(true);
            clientSocket.setBroadcast(true);
            clientSocket.setSoTimeout(TIMEOUT);
            byte[] sendData = DISCOVER_STRING;
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ipAddress),
                    PORT);
            for (int i = 1; i <= 2; i++) {
                clientSocket.send(sendPacket);
            }
            sendPacket.setData(new byte[256]);
            while (true) {
                clientSocket.receive(sendPacket);
                byte[] messageBuf = Arrays.copyOfRange(sendPacket.getData(), sendPacket.getOffset(),
                        sendPacket.getOffset() + sendPacket.getLength());
                if (logger.isTraceEnabled()) {
                    Message roboResponse = new Message(messageBuf);
                    logger.trace("Discovery response received from {} DeviceID: {}\r\n{}",
                            sendPacket.getAddress().getHostAddress(), Utils.getHex(roboResponse.getDeviceId()),
                            roboResponse.toSting());
                }
                responses.put(sendPacket.getAddress().getHostAddress(), messageBuf);
            }
        } catch (Exception e) {
            logger.trace("Discovery on {} error: {}", ipAddress, e.getMessage());
        }
        return responses;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Collections.singleton(THING_TYPE_MIIO);
    }

    @Override
    protected void startScan() {
        logger.debug("Start Xiaomi Mi IO discovery");
        discover();
        logger.debug("Xiaomi Mi IO discovery done");
    }
}
