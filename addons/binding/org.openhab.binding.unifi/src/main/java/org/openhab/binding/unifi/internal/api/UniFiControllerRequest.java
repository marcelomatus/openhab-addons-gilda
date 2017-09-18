/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.unifi.internal.api;

import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link UniFiControllerRequest} encapsulates a request sent by the {@link UniFiController}.
 *
 * @author Matthew Bowman - Initial contribution
 *
 * @param <T> The response type expected as a result of the request's execution
 */
@NonNullByDefault
public class UniFiControllerRequest<T> {

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    private static final String CONTENT_TYPE_APPLICATION_JSON = MimeTypes.Type.APPLICATION_JSON.asString();

    private static final long TIMEOUT_SECONDS = 5;

    private static final String PROPERTY_DATA = "data";

    private final Logger logger = LoggerFactory.getLogger(UniFiControllerRequest.class);

    private Gson gson;

    private HttpClient httpClient;

    private String host = "unifi";

    private int port = 8443;

    private String path = "/";

    private Map<String, String> queryParameters = new HashMap<>();

    private Map<String, String> bodyParameters = new HashMap<>();

    private Class<T> resultType;

    // Public API

    public UniFiControllerRequest(Class<T> resultType, Gson gson, HttpClient httpClient, String host, int port) {
        this.resultType = resultType;
        this.gson = gson;
        this.httpClient = httpClient;
        this.host = host;
        this.port = port;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setBodyParameter(String key, Object value) {
        this.bodyParameters.put(key, String.valueOf(value));
    }

    public void setQueryParameter(String key, Object value) {
        this.queryParameters.put(key, String.valueOf(value));
    }

    public @Nullable T execute() throws UniFiException {
        T result = null;
        String json = getContent();
        // mgb: only try and unmarshall non-void result types
        if (!Void.class.equals(resultType)) {
            JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
            if (jsonObject.has(PROPERTY_DATA) && jsonObject.get(PROPERTY_DATA).isJsonArray()) {
                result = gson.fromJson(jsonObject.getAsJsonArray(PROPERTY_DATA), resultType);
            }
        }
        return result;
    }

    // Private API

    private String getContent() throws UniFiException {
        String content;
        ContentResponse response = getContentResponse();
        int status = response.getStatus();
        switch (status) {
            case HttpStatus.OK_200:
                content = response.getContentAsString();
                if (logger.isTraceEnabled()) {
                    logger.trace("<< {} {} \n{}", status, HttpStatus.getMessage(status), prettyPrintJson(content));
                }
                break;
            case HttpStatus.BAD_REQUEST_400:
                throw new UniFiInvalidCredentialsException("Invalid Credentials");
            case HttpStatus.UNAUTHORIZED_401:
                throw new UniFiExpiredSessionException("Expired Credentials");
            default:
                throw new UniFiException("Unknown HTTP status code " + status + " returned by the controller");
        }
        return content;
    }

    private ContentResponse getContentResponse() throws UniFiException {
        Request request = newRequest();
        logger.trace(">> {} {}", request.getMethod(), request.getURI());
        ContentResponse response;
        try {
            response = request.send();
        } catch (TimeoutException | InterruptedException e) {
            throw new UniFiCommunicationException(e);
        } catch (ExecutionException e) {
            // mgb: unwrap the cause and try to cleanly handle it
            Throwable cause = e.getCause();
            if (cause instanceof ConnectException) {
                throw new UniFiCommunicationException(cause);
            } else if (cause instanceof HttpResponseException
                    && ((HttpResponseException) cause).getResponse() instanceof ContentResponse) {
                // the UniFi controller violates the HTTP protocol
                // - it returns 401 UNAUTHORIZED without the WWW-Authenticate response header
                // - this causes an ExceptionException to be thrown
                // - we unwrap the response from the exception for proper handling of the 401 status code
                response = (ContentResponse) ((HttpResponseException) cause).getResponse();
            } else {
                throw new UniFiException(cause);
            }
        }
        return response;
    }

    private Request newRequest() {
        HttpMethod method = bodyParameters.isEmpty() ? HttpMethod.GET : HttpMethod.POST;
        HttpURI uri = new HttpURI(HttpScheme.HTTPS.asString(), host, port, path);
        Request request = httpClient.newRequest(uri.toString()).timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .method(method);
        for (Entry<String, String> entry : queryParameters.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        if (!bodyParameters.isEmpty()) {
            String jsonBody = getRequestBodyAsJson();
            ContentProvider content = new StringContentProvider(CONTENT_TYPE_APPLICATION_JSON, jsonBody, CHARSET_UTF8);
            request = request.content(content);
        }
        return request;
    }

    private String getRequestBodyAsJson() {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = new JsonObject();
        JsonElement jsonElement = null;
        for (Entry<String, String> entry : bodyParameters.entrySet()) {
            try {
                jsonElement = jsonParser.parse(entry.getValue());
            } catch (JsonSyntaxException e) {
                jsonElement = new JsonPrimitive(entry.getValue());
            }
            jsonObject.add(entry.getKey(), jsonElement);
        }
        return jsonObject.toString();
    }

    private static String prettyPrintJson(String content) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(content).getAsJsonObject();
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        return prettyGson.toJson(json);
    }

}
