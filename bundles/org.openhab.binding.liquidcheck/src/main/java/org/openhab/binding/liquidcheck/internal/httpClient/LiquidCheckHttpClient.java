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
package org.openhab.binding.liquidcheck.internal.httpClient;

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
import org.openhab.binding.liquidcheck.internal.LiquidCheckHandler;
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

    private final Logger logger = LoggerFactory.getLogger(LiquidCheckHandler.class);
    private final HttpClient client;
    private final LiquidCheckConfiguration config;

    public boolean isClosed = false;

    /**
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
            logger.error("Couldn't start Client! Exception: " + e.toString());
            return;
        }
    }

    /**
     * 
     * @return
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

    public String measureCommand() {
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
     * @return
     */
    public boolean isConnected() {
        String state = this.client.getState();
        return "STARTED".equals(state) ? true : false;
    }

    /**
     * 
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
