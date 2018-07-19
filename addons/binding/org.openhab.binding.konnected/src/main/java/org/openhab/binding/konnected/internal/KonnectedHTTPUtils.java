/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.konnected.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.konnected.internal.handler.KonnectedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Get and Put reqeust class.
 *
 * @author Zachary Christiansen - Initial contribution
 */
public class KonnectedHTTPUtils {
    private final Logger logger = LoggerFactory.getLogger(KonnectedHandler.class);
    private static final int REQUEST_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

    public KonnectedHTTPUtils() {

    }

    /**
     * Sends a {@link doPut} request with a timeout of 30 seconds
     *
     * @param urlAddress the address to send the request
     *
     * @param payload the json payload to include with the request
     *
     */
    // TO DO: for some reason here on my production machine i get an error on the second attempt to doPut
    public String doPut(String urlAddress, String payload) throws IOException {
        logger.debug("The String url we want to put is : {}", urlAddress);
        logger.debug("The payload we want to put is: {}", payload);
        ByteArrayInputStream input = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
        String test = HttpUtil.executeUrl("PUT", urlAddress, getHttpHeaders(), input, "application/json",
                KonnectedHTTPUtils.REQUEST_TIMEOUT);
        logger.debug(test);
        return test;
    }

    protected Properties getHttpHeaders() {
        Properties httpHeaders = new Properties();
        httpHeaders.put("Content-Type", "application/json");
        return httpHeaders;
    }

    /**
     * Sends a {@link doGet} request with a timeout of 30 seconds
     *
     * @param urlAddress the address to send the request
     */

    public synchronized String doGet(String urlAddress) throws IOException {
        logger.debug("The String url we want to get is : {}", urlAddress);
        String test = HttpUtil.executeUrl("GET", urlAddress, KonnectedHTTPUtils.REQUEST_TIMEOUT);
        logger.debug(test);
        return test;
    }

    public String getHostAddresses() {
        Set<String> HostAddresses = new HashSet<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp() && ni.getHardwareAddress() != null) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        if (ia.getBroadcast() != null) { // If limited to IPV4
                            HostAddresses.add(ia.getAddress().getHostAddress());
                        }
                    }
                }
            }
        } catch (SocketException e) {
        }

        return HostAddresses.toArray(new String[0]).toString();
    }

}
