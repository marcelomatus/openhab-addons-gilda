/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;

/**
 * A transport layer to execute SSL requests to a given <code>url</code>
 *
 * @author Svilen Valkanov
 */
public interface RestClient {

    public static final String CERTIFICATE_DIR = "SSL";
    public static final String CERTIFICATE_FILE_NAME = "miHome.crt";

    /**
     * The recommended method from the API documentation
     */
    public static final HttpMethod DEFAULT_HTTP_METHOD = HttpMethod.POST;

    /**
     * Required content type for both request and response
     */
    public static final String CONTENT_TYPE = "application/json";

    /**
     * Request timeout in ms
     */
    public static final int DEFAULT_REQUEST_TIMEOUT = 5000;

    /**
     * Mi|Home REST API base URL
     */
    public static final String MIHOME_REST_API_URL = "https://mihome4u.co.uk/api/v1";

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>
     *
     * @param requestPath - path relative to base URL
     * @param httpMethod - the HTTP method to use
     * @param httpHeaders - optional HTTP headers which has to be set on request
     * @param content - the content to be send to the given <code>url</code> or <code>null</code> if no content should
     *            be send.
     * @param contentType - the content type of the given <code>content</code>
     * @return - the response or <code>NULL</code> when the request went wrong
     * @throws IOException - when the request execution failed, timed out or it was interrupted
     */
    public ContentResponse sendRequest(String requestPath, HttpMethod httpMethod, Properties httpHeaders,
            InputStream content, String contentType) throws IOException;

    /**
     * Sets a sslContext
     *
     * @param sslContext - {@link SSLContext} to be used by the client when executing the SSL handshake
     * @throws Exception - if any Exception occurs while trying to set the {@link SSLContext}
     */
    public void setSslContext(SSLContext sslContext) throws Exception;

    public String getBaseURL();

    public void setBaseURL(String baseUrl);

    public int getConnectionTimeout();

    public void setConnectionTimeout(int timeout);
}
