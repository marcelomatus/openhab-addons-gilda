/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.intesis.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.binding.intesis.internal.IntesisConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IntesisHomeHttpApi} wraps the IntesisHome REST API and provides various low level function to access the
 * device api (not
 * cloud api).
 *
 * @author Hans-Jörg Merk - Initial contribution
 */
@NonNullByDefault
public class IntesisHomeHttpApi {
    public static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

    private final Logger logger = LoggerFactory.getLogger(IntesisHomeHttpApi.class);
    private final HttpClient httpClient;

    public IntesisHomeHttpApi(IntesisConfiguration config, HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Used to post a request to the device
     *
     * @param ipAddress of the device
     * @param content string
     * @return JSON string as response
     */
    @Nullable
    public String postRequest(String ipAddress, String contentString) {

        String url = "http://" + ipAddress + "/api.cgi";
        try {
            Request request = httpClient.POST(url);
            request.header(HttpHeader.CONTENT_TYPE, "application/json");
            request.content(new StringContentProvider(contentString), "application/json");

            // Do request and get response
            ContentResponse contentResponse = request.send();

            String response = contentResponse.getContentAsString().replace("\t", "").replace("\r\n", "").trim();
            logger.trace("HTTP Response for getInfo {}: {}", contentResponse.getStatus(), response);

            if (response != null && !response.isEmpty()) {
                return response;
            }
        } catch (Exception e) {
        }
        return null;
    }
}
