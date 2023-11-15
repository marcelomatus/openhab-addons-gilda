/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.siemenshvac.internal.network;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.siemenshvac.internal.handler.SiemensHvacBridgeBaseThingHandler;
import org.openhab.binding.siemenshvac.internal.handler.SiemensHvacBridgeConfig;
import org.openhab.binding.siemenshvac.internal.metadata.RuntimeTypeAdapterFactory;
import org.openhab.binding.siemenshvac.internal.metadata.SiemensHvacMetadata;
import org.openhab.binding.siemenshvac.internal.metadata.SiemensHvacMetadataDataPoint;
import org.openhab.binding.siemenshvac.internal.metadata.SiemensHvacMetadataMenu;
import org.openhab.binding.siemenshvac.internal.type.SiemensHvacException;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.types.Type;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class SiemensHvacConnectorImpl implements SiemensHvacConnector {

    private static final Logger logger = LoggerFactory.getLogger(SiemensHvacConnectorImpl.class);

    private final static Gson gson;
    private final static Gson gsonWithAdpter;

    private @Nullable String sessionId = null;
    private @Nullable String sessionIdHttp = null;
    private @Nullable SiemensHvacBridgeConfig config = null;

    protected final HttpClientFactory httpClientFactory;

    protected HttpClient httpClient;

    private static int startedRequest = 0;
    private static int completedRequest = 0;
    private Lock lockObj = new ReentrantLock();

    private Map<String, Type> updateCommand;

    private @Nullable SiemensHvacBridgeBaseThingHandler hvacBridgeBaseThingHandler;

    static {
        GsonBuilder builder = new GsonBuilder();
        gson = builder.setPrettyPrinting().create();

        RuntimeTypeAdapterFactory<SiemensHvacMetadata> adapter = RuntimeTypeAdapterFactory
                .of(SiemensHvacMetadata.class);
        adapter.registerSubtype(SiemensHvacMetadataMenu.class);
        adapter.registerSubtype(SiemensHvacMetadataDataPoint.class);

        gsonWithAdpter = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();

    }

    @Activate
    public SiemensHvacConnectorImpl(@Reference HttpClientFactory httpClientFactory) {
        this.updateCommand = new Hashtable<String, Type>();
        this.httpClientFactory = httpClientFactory;

        SslContextFactory ctxFactory = new SslContextFactory.Client(true);
        ctxFactory.setRenegotiationAllowed(false);
        ctxFactory.setEnableCRLDP(false);
        ctxFactory.setEnableOCSP(false);
        ctxFactory.setTrustAll(true);
        ctxFactory.setValidateCerts(false);
        ctxFactory.setValidatePeerCerts(false);
        ctxFactory.setEndpointIdentificationAlgorithm(null);

        this.httpClient = new HttpClient(ctxFactory);
        this.httpClient.setMaxConnectionsPerDestination(10);
        this.httpClient.setMaxRequestsQueuedPerDestination(10000);
        this.httpClient.setConnectTimeout(10000);
        this.httpClient.setFollowRedirects(false);

        try {
            this.httpClient.start();
        } catch (Exception e) {
            logger.error("Failed to start http client: {}", e.getMessage());
        }
    }

    @Override
    public void setSiemensHvacBridgeBaseThingHandler(
            @Nullable SiemensHvacBridgeBaseThingHandler hvacBridgeBaseThingHandler) {
        this.hvacBridgeBaseThingHandler = hvacBridgeBaseThingHandler;
    }

    public void unsetSiemensHvacBridgeBaseThingHandler(SiemensHvacBridgeBaseThingHandler hvacBridgeBaseThingHandler) {
        this.hvacBridgeBaseThingHandler = null;
    }

    @Override
    public void onComplete(@Nullable Request request) {
        lockObj.lock();
        try {
            logger.debug("OnComplete");
            completedRequest++;
        } finally {
            lockObj.unlock();
        }
    }

    @Override
    public void onError(@Nullable Request request, @Nullable SiemensHvacCallback cb) throws Exception {
        lockObj.lock();
        try {
            logger.debug("OnError");
            completedRequest++;
        } finally {
            lockObj.unlock();
        }

        if (cb == null || request == null) {
            return;
        }

        try {
            final Request retryRequest = httpClient.newRequest(request.getURI());
            request.method(HttpMethod.GET);

            if (retryRequest != null) {
                executeRequest(retryRequest, cb);
            }
        } catch (Exception ex) {
            logger.debug("Error during gateway request:", ex);
            throw ex;
        }
    }

    private @Nullable ContentResponse executeRequest(final Request request, @Nullable SiemensHvacCallback callback)
            throws Exception {
        // Give a high timeout because we queue a lot of async request,
        // so enqueued them will take some times ...
        request.timeout(240, TimeUnit.SECONDS);

        ContentResponse response = null;

        @Nullable
        SiemensHvacRequestListener requestListener = null;
        if (callback != null) {
            requestListener = new SiemensHvacRequestListener(callback, this);
        }

        try {
            if (requestListener != null) {
                lockObj.lock();
                try {
                    startedRequest++;
                    logger.trace("StartedRequest: {}", startedRequest - completedRequest);

                } finally {
                    lockObj.unlock();
                }

                request.send(requestListener);
            } else {
                response = request.send();
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new SiemensHvacException("siemensHvac:Exception by executing request: " + request.getQuery() + " ; "
                    + e.getLocalizedMessage());
        }
        return response;
    }

    private void initConfig() throws Exception {
        SiemensHvacBridgeBaseThingHandler lcHvacBridgeBaseThingHandler = hvacBridgeBaseThingHandler;

        if (lcHvacBridgeBaseThingHandler != null) {
            config = lcHvacBridgeBaseThingHandler.getBridgeConfiguration();
        } else {
            throw new SiemensHvacException(
                    "siemensHvac:Exception unable to get config because hvacBridgeBaseThingHandler is null");
        }
    }

    @Override
    public @Nullable SiemensHvacBridgeConfig getBridgeConfiguration() {
        return config;
    }

    private void doAuth(boolean http) throws Exception {
        logger.debug("siemensHvac:doAuth()");

        initConfig();

        SiemensHvacBridgeConfig config = this.config;
        if (config == null) {
            throw new SiemensHvacException("Missing SiemensHvacOZW672 Bridge configuration");
        }

        String baseUri = config.baseUrl;
        String uri = "";

        if (http) {
            uri = "main.app";
        } else {
            uri = String.format("api/auth/login.json?user=%s&pwd=%s", config.userName, config.userPassword);
        }

        final Request request = httpClient.newRequest(baseUri + uri);
        if (http) {
            request.method(HttpMethod.POST).param("user", config.userName).param("pwd", config.userPassword);
        } else {
            request.method(HttpMethod.GET);
        }

        logger.debug("siemensHvac:doAuth:connect()");

        try {
            ContentResponse response = executeRequest(request, null);
            if (response != null) {
                int statusCode = response.getStatus();

                if (statusCode == HttpStatus.OK_200) {
                    String result = response.getContentAsString();

                    if (http) {
                        CookieStore cookieStore = httpClient.getCookieStore();
                        List<HttpCookie> cookies = cookieStore.getCookies();

                        for (HttpCookie httpCookie : cookies) {
                            if (httpCookie.getName().equals("SessionId")) {
                                sessionIdHttp = httpCookie.getValue();
                            }

                        }

                        if (sessionIdHttp == null) {
                            logger.debug("Session request auth was unsuccessful in _doAuth()");
                        }
                    } else {
                        if (result != null) {
                            JsonObject resultObj = getGson().fromJson(result, JsonObject.class);

                            if (resultObj != null && resultObj.has("Result")) {
                                JsonElement resultVal = resultObj.get("Result");
                                JsonObject resultObj2 = resultVal.getAsJsonObject();

                                if (resultObj2.has("Success")) {
                                    boolean successVal = resultObj2.get("Success").getAsBoolean();

                                    if (successVal) {

                                        if (resultObj.has("SessionId")) {
                                            sessionId = resultObj.get("SessionId").getAsString();
                                            logger.debug("Have new SessionId: {} ", sessionId);
                                        }

                                    }

                                }
                            }

                            logger.debug("siemensHvac:doAuth:decodeResponse:()");

                            if (sessionId == null) {
                                logger.debug("Session request auth was unsuccessful in _doAuth()");
                            }

                        }

                    }
                }
            }

            logger.trace("siemensHvac:doAuth:connect()");

        } catch (Exception ex) {
            logger.debug("siemensHvac:doAuth:error() {}", ex.getLocalizedMessage());
        } finally {
        }
    }

    @Override
    public @Nullable String DoBasicRequest(String uri) throws Exception {
        return DoBasicRequest(uri, null);
    }

    public @Nullable String DoBasicRequestAsync(String uri, @Nullable SiemensHvacCallback callback) throws Exception {
        return DoBasicRequest(uri, callback);
    }

    public @Nullable String DoBasicRequest(String uri, @Nullable SiemensHvacCallback callback) throws Exception {
        if (sessionIdHttp == null) {
            doAuth(true);
        }

        if (sessionId == null) {
            doAuth(false);
        }

        SiemensHvacBridgeConfig config = this.config;
        if (config == null) {
            throw new SiemensHvacException("Missing SiemensHvacOZW672 Bridge configuration");
        }

        String baseUri = config.baseUrl;

        String mUri = uri;
        if (!mUri.endsWith("?")) {
            mUri = mUri + "&";
        }
        if (mUri.indexOf("main.app") >= 0) {
            mUri = mUri + "SessionId=" + sessionIdHttp;
        } else {
            mUri = mUri + "SessionId=" + sessionId;
        }

        logger.debug("Execute request: {}", uri);
        CookieStore c = httpClient.getCookieStore();
        java.net.HttpCookie cookie = new HttpCookie("SessionId", sessionIdHttp);
        cookie.setPath("/");
        cookie.setVersion(0);

        c.add(new URI(baseUri), cookie);

        logger.debug("Execute request: {}", uri);
        final Request request = httpClient.newRequest(baseUri + mUri);
        request.method(HttpMethod.GET);

        ContentResponse response = executeRequest(request, callback);
        if (callback == null && response != null) {
            int statusCode = response.getStatus();

            if (statusCode == HttpStatus.OK_200) {
                String result = response.getContentAsString();

                return result;
            }
        }

        return null;
    }

    @Override
    public @Nullable JsonObject doRequest(String req) {
        return doRequest(req, null);
    }

    @Override
    public @Nullable JsonObject doRequest(String req, @Nullable SiemensHvacCallback callback) {
        try {
            String response = DoBasicRequest(req, callback);

            if (response != null) {
                JsonObject resultObj = getGson().fromJson(response, JsonObject.class);

                if (resultObj != null && resultObj.has("Result")) {
                    JsonObject subResultObj = resultObj.getAsJsonObject("Result");

                    if (subResultObj.has("Success")) {
                        boolean result = subResultObj.get("Success").getAsBoolean();
                        if (result) {
                            return resultObj;
                        }
                    }

                }

                return null;
            }
        } catch (Exception e) {
            logger.warn("siemensHvac:DoRequest:Exception by executing jsonRequest: {} ; {} ", req,
                    e.getLocalizedMessage());
        }

        return null;
    }

    @Override
    public void DisplayRequestStats() {
        logger.info("DisplayRequestStats : {} ({}/{})", (startedRequest - completedRequest), startedRequest,
                completedRequest);
    }

    @Override
    public void WaitAllPendingRequest() {
        logger.debug("WaitAllPendingRequest:start");
        try {
            Thread.sleep(1000);
            boolean allRequestDone = false;

            while (!allRequestDone) {
                int idx = 0;

                allRequestDone = true;
                while (idx < 5 && allRequestDone) {
                    logger.debug("WaitAllPendingRequest:waitAllRequestDone {} : {} ({}/{})", idx,
                            (startedRequest - completedRequest), startedRequest, completedRequest);
                    if (startedRequest != completedRequest) {
                        allRequestDone = false;
                    }
                    Thread.sleep(1000);
                    idx++;
                }
            }
        } catch (InterruptedException ex) {
            logger.debug("WaitAllPendingRequest:interrupted in WaitAllRequest");
        }

        logger.debug("WaitAllPendingRequest:end WaitAllPendingRequest");
    }

    @Override
    public void WaitNoNewRequest() {
        logger.debug("WaitNoNewRequest:start");
        try {
            int lastRequest = startedRequest;
            Thread.sleep(5000);
            while (lastRequest != startedRequest) {
                logger.debug("waitNoNewRequest  {}/{})", startedRequest, lastRequest);
                Thread.sleep(5000);
                lastRequest = startedRequest;
            }
        } catch (InterruptedException ex) {
            logger.debug("WaitAllPendingRequest:interrupted in WaitAllRequest");
        }

        logger.debug("WaitNoNewRequest:end WaitAllStartingRequest");
    }

    public static Gson getGson() {
        return gson;
    }

    public static Gson getGsonWithAdapter() {
        return gsonWithAdpter;
    }

    public void AddDpUpdate(String itemName, Type dp) {
        synchronized (updateCommand) {
            updateCommand.put(itemName, dp);
        }
    }

    @Override
    public void ResetSessionId(boolean web) {
        if (web) {
            sessionIdHttp = null;
        } else {
            sessionId = null;
        }
    }
}
