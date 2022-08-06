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
package org.openhab.binding.liqiudcheck.internal.httpClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.liqiudcheck.internal.LiqiudCheckConfiguration;
import org.openhab.binding.liqiudcheck.internal.LiqiudCheckHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LiqiudCheckBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
public class LiquidCheckHttpClient {

    private final Logger logger = LoggerFactory.getLogger(LiqiudCheckHandler.class);
    private final HttpClient client;
    private final LiqiudCheckConfiguration config;

    public boolean isClosed = false;

    public LiquidCheckHttpClient(LiqiudCheckConfiguration config) {
        this.config = config;
        client = new HttpClient();

        try {
            client.setFollowRedirects(false);
            client.setName("LiquidCheckHttpClient");
            client.setIdleTimeout(config.connecionTimeOut);
            client.start();
        } catch (Exception e) {
            logger.error("Couldn't start Client! Exception: " + e.toString());
            return;
        }
    }

    public String pollData() throws InterruptedException, TimeoutException, ExecutionException {
        String uri = "http://" + config.hostname + "/infos.json";
        Request request = client.newRequest(uri);
        request.method(HttpMethod.GET);
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
