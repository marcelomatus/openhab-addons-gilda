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
package org.openhab.binding.liquidcheck.internal.httpclient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.liquidcheck.internal.LiquidCheckConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LiquidCheckHttpClient} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
public class LiquidCheckHttpClient {

    private final Logger logger = LoggerFactory.getLogger(LiquidCheckHttpClient.class);
    private final HttpClient client;
    private final LiquidCheckConfiguration config;

    public boolean isClosed = false;

    /**
     * Sets up the client that polls the data from the device
     * 
     * @param config
     */
    public LiquidCheckHttpClient(LiquidCheckConfiguration config) {
        this.config = config;
        client = new HttpClient();

        try {
            client.setFollowRedirects(false);
            client.setName("LiquidCheckHttpClient");
            client.setIdleTimeout(config.connecionTimeOut * 1000);
            client.start();
        } catch (Exception e) {
            logger.error("Couldn't start Client! Exception: {}", e.toString());
            return;
        }
    }

    /**
     * 
     * This method gets the data from the device
     * 
     * @return the response string
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    public String pollData() throws InterruptedException, TimeoutException, ExecutionException {
        String uri = "http://" + config.ip + "/infos.json";
        Request request = client.newRequest(uri);
        request.method(HttpMethod.GET);
        ContentResponse response = request.send();
        return response.getContentAsString();
    }

    /**
     * 
     * This method sends the command to start the measurement
     * 
     * @return the string of the response
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    public String measureCommand() throws InterruptedException, TimeoutException, ExecutionException {
        String uri = "http://" + config.ip + "/command";
        Request request = client.newRequest(uri);
        request.method(HttpMethod.POST);
        request.header(HttpHeader.CONTENT_TYPE, "applicaton/json");
        request.content(new StringContentProvider(
                "{\"header\":{\"namespace\":\"Device.Control\",\"name\":\"StartMeasure\",\"messageId\":\"1\",\"payloadVersion\":\"1\"},\"payload\":null}"));
        ContentResponse response = request.send();
        return response.getContentAsString();
    }

    /**
     * 
     * @return true when the client is connected
     */
    public boolean isConnected() {
        String state = this.client.getState();
        return "STARTED".equals(state) ? true : false;
    }

    /**
     * This method closes the client
     */
    public void close() {
        this.isClosed = true;
        try {
            this.client.stop();
        } catch (Exception e) {
            logger.error("Couldn't close HttpClient! Exception: {}", e.toString());
        }
    }
}
