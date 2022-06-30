/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.mercedesme.internal.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mercedesme.internal.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Utils} class defines an HTTP Server for authentication callbacks
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final List<Integer> PORTS = new ArrayList<Integer>();
    private static int port = 8090;

    /**
     * Get free port without other Thread interference
     *
     * @return
     */
    public static synchronized int getFreePort() {
        while (PORTS.contains(port)) {
            port++;
        }
        PORTS.add(port);
        return port;
    }

    public static synchronized void addPort(int portNr) {
        if (PORTS.contains(portNr)) {
            LOGGER.warn("Port {} already occupied", portNr);
        }
        PORTS.add(portNr);
    }

    public static synchronized void removePort(int portNr) {
        PORTS.remove(Integer.valueOf(portNr));
    }

    public static String getCallbackIP() {
        // https://stackoverflow.com/questions/1062041/ip-address-not-obtained-in-java
        String ip = Constants.NOT_SET;
        try {
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces
                    .hasMoreElements();) {
                NetworkInterface iface = ifaces.nextElement();
                // guess IP address, not loopback!
                if (!Constants.LOOPBACK_ADDRESS.equals(iface.getName())) {
                    for (Enumeration<InetAddress> addresses = iface.getInetAddresses(); addresses.hasMoreElements();) {
                        InetAddress address = addresses.nextElement();
                        ip = address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.trace("Autodetect IP failed {}", e.getMessage());
        }
        return ip;
    }

    public static String getCallbackAddress(String callbackIP, int callbackPort) {
        return Constants.HTTP + callbackIP + Constants.COLON + callbackPort + Constants.CALLBACK_ENDPOINT;
    }

    public static Object fromString(String s) {
        try {
            byte[] data = Base64.getDecoder().decode(s);
            ObjectInputStream ois;
            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.debug("Exception Token deserialization {}", e.getMessage());
        }
        return Constants.EMPTY;
    }

    public static String toString(Serializable o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            LOGGER.debug("Exception Token serialization {}", e.getMessage());
        }
        return Constants.EMPTY;
    }
}
