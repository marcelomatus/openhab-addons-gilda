/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.iotawatt.internal.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.iotawatt.internal.model.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Encapsulates the communication with the IoTaWatt device.
 *
 * @author Peter Rosenberg - Initial contribution
 */
@NonNullByDefault
public class IoTaWattClient {
    private static final String REQUEST_URL = "http://%s/status?state=&inputs=";

    private final Logger logger = LoggerFactory.getLogger(IoTaWattClient.class);

    public final String hostname;
    private final HttpClient httpClient;
    private final Gson gson;

    public IoTaWattClient(String hostname, HttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.hostname = hostname;
        this.gson = gson;
    }

    /**
     * Fetch the current status from the device.
     * The errors are handled by the caller to update the Thing status accordingly.
     */
    public Optional<StatusResponse> fetchStatus()
            throws ExecutionException, InterruptedException, TimeoutException, URISyntaxException {
        final URI uri = new URI(String.format(REQUEST_URL, hostname));
        final Request request = httpClient.newRequest(uri).method(HttpMethod.GET);
        final ContentResponse response = request.send();
        final String content = response.getContentAsString();
        @Nullable
        final StatusResponse statusResponse = gson.fromJson(content, StatusResponse.class);
        logger.trace("statusResponse: {}", statusResponse);
        // noinspection ConstantConditions
        return Optional.ofNullable(statusResponse);
    }
}
