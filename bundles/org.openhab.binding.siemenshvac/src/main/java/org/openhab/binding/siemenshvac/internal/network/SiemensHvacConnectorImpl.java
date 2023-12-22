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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private final Logger logger = LoggerFactory.getLogger(SiemensHvacConnectorImpl.class);

    private Map<SiemensHvacRequestHandler, SiemensHvacRequestHandler> currentHandlerRegistry = new HashMap<SiemensHvacRequestHandler, SiemensHvacRequestHandler>();
    private Map<SiemensHvacRequestHandler, SiemensHvacRequestHandler> handlerInErrorRegistry = new HashMap<SiemensHvacRequestHandler, SiemensHvacRequestHandler>();
    private final Gson gson;
    private final Gson gsonWithAdapter;

    private @Nullable String sessionId = null;
    private @Nullable String sessionIdHttp = null;
    private @Nullable SiemensHvacBridgeConfig config = null;

    protected final HttpClientFactory httpClientFactory;

    protected HttpClient httpClient;

    private Map<String, Type> updateCommand;

    private int requestCount = 0;
    private int errorCount = 0;
    private int timeout = 10;
    private SiemensHvacRequestListener.ErrorSource errorSource = SiemensHvacRequestListener.ErrorSource.ErrorBridge;

    private @Nullable SiemensHvacBridgeBaseThingHandler hvacBridgeBaseThingHandler;

    @Activate
    public SiemensHvacConnectorImpl(@Reference HttpClientFactory httpClientFactory) {
        GsonBuilder builder = new GsonBuilder();
        gson = builder.setPrettyPrinting().create();

        RuntimeTypeAdapterFactory<SiemensHvacMetadata> adapter = RuntimeTypeAdapterFactory
                .of(SiemensHvacMetadata.class);
        adapter.registerSubtype(SiemensHvacMetadataMenu.class);
        adapter.registerSubtype(SiemensHvacMetadataDataPoint.class);

        gsonWithAdapter = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();

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
    public void onComplete(@Nullable Request request, SiemensHvacRequestHandler reqHandler) throws Exception {
        unregisterRequestHandler(reqHandler);
    }

    @Override
    public void onError(@Nullable Request request, @Nullable SiemensHvacRequestHandler reqHandler,
            SiemensHvacRequestListener.ErrorSource errorSource) throws Exception {
        if (reqHandler == null || request == null) {
            throw new SiemensHvacException("internalError : onError call with reqHandler == null");
        }

        // reqHandler.displayStats();

        if (reqHandler.getRetryCount() >= 1) {
            logger.info("unable to handle request, retryCount>5, cancel it");
            unregisterRequestHandler(reqHandler);
            registerHandlerError(reqHandler);
            errorCount++;
            this.errorSource = errorSource;
            return;
        }

        // Wait one second before retrying the request to avoid flooding the gateway
        Thread.sleep(1000);

        if (sessionIdHttp == null) {
            doAuth(true);
        }

        if (sessionId == null) {
            doAuth(false);
        }

        try {
            final Request retryRequest = httpClient.newRequest(request.getURI());
            request.method(HttpMethod.GET);
            reqHandler.setRequest(retryRequest);
            reqHandler.incrementRetryCount();

            if (retryRequest != null) {
                executeRequest(retryRequest, reqHandler);
            }
        } catch (Exception ex) {
            logger.debug("Error during gateway request:", ex);
            throw ex;
        }
    }

    private @Nullable ContentResponse executeRequest(final Request request) throws Exception {
        return executeRequest(request, (SiemensHvacCallback) null);
    }

    private @Nullable ContentResponse executeRequest(final Request request, @Nullable SiemensHvacCallback callback)
            throws Exception {
        requestCount++;

        // For asynchronous request, we create a RequestHandler that will enable us to follow request state
        SiemensHvacRequestHandler requestHandler = null;
        if (callback != null) {
            requestHandler = new SiemensHvacRequestHandler(callback, this);
            requestHandler.setRequest(request);
            currentHandlerRegistry.put(requestHandler, requestHandler);
        }

        return executeRequest(request, requestHandler);
    }

    private void unregisterRequestHandler(SiemensHvacRequestHandler handler) throws SiemensHvacException {
        if (!currentHandlerRegistry.containsKey(handler)) {
            throw new SiemensHvacException("Internal error, try to unregister not registred handler: " + handler);
        }

        currentHandlerRegistry.remove(handler);
    }

    private void registerHandlerError(SiemensHvacRequestHandler handler) {
        handlerInErrorRegistry.put(handler, handler);
    }

    private @Nullable ContentResponse executeRequest(final Request request,
            @Nullable SiemensHvacRequestHandler requestHandler) throws Exception {
        // Give a high timeout because we queue a lot of async request,
        // so enqueued them will take some times ...
        request.timeout(timeout, TimeUnit.SECONDS);

        ContentResponse response = null;

        try {
            if (requestHandler != null) {
                SiemensHvacRequestListener requestListener = new SiemensHvacRequestListener(requestHandler);
                request.send(requestListener);
            } else {
                response = request.send();
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new SiemensHvacException("siemensHvac:Exception by executing request: " + request.getURI() + " ; "
                    + e.getLocalizedMessage());
        }
        return response;
    }

    private void initConfig() throws SiemensHvacException {
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

    private void doAuth(boolean http) throws SiemensHvacException {
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
            ContentResponse response = executeRequest(request);
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
                                throw new SiemensHvacException(
                                        "Session request auth was unsuccessful in _doAuth(), please verify login parameters");
                            }

                        }

                    }
                }
            }

            logger.trace("siemensHvac:doAuth:connect()");

        } catch (Exception ex) {
            logger.debug("siemensHvac:doAuth:error() {}", ex.getLocalizedMessage());
            throw new SiemensHvacException("Error during authentification", ex);
        }
    }

    @Override
    public @Nullable String doBasicRequest(String uri) throws Exception {
        return doBasicRequest(uri, null);
    }

    public @Nullable String doBasicRequestAsync(String uri, @Nullable SiemensHvacCallback callback) throws Exception {
        return doBasicRequest(uri, callback);
    }

    public @Nullable String doBasicRequest(String uri, @Nullable SiemensHvacCallback callback) throws Exception {
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
            String response = doBasicRequest(req, callback);

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
    public void displayRequestStats() {
        logger.debug("DisplayRequestStats : ");
        logger.debug("    currentRuning   : {}", currentHandlerRegistry.keySet().size());
        logger.debug("    errors          : {}", handlerInErrorRegistry.keySet().size());
    }

    @Override
    public void waitAllPendingRequest() {
        logger.debug("WaitAllPendingRequest:start");
        try {
            boolean allRequestDone = false;
            int idx = 0;

            while (!allRequestDone) {
                allRequestDone = false;
                int currentRequestCount = currentHandlerRegistry.keySet().size();
                logger.debug("WaitAllPendingRequest:waitAllRequestDone {} : {}", idx, currentRequestCount);

                if (currentRequestCount == 0) {
                    allRequestDone = true;
                }
                Thread.sleep(1000);

                if ((idx % 60) == 0) {
                    CheckStaleRequest();
                }
                idx++;
            }
        } catch (InterruptedException ex) {
            logger.debug("WaitAllPendingRequest:interrupted in WaitAllRequest");
        }

        logger.debug("WaitAllPendingRequest:end WaitAllPendingRequest");
    }

    public void CheckStaleRequest() {
        logger.debug("check stale request::begin");
        int staleRequest = 0;
        for (SiemensHvacRequestHandler handler : handlerInErrorRegistry.keySet()) {
            long elapseTime = handler.getElapseTime();
            if (elapseTime > 300) {
                String uri = "";
                Request request = handler.getRequest();
                if (request != null) {
                    uri = request.getURI().toString();
                }
                logger.debug("find stale request: {} {}", elapseTime, uri);
                staleRequest++;

                try {
                    unregisterRequestHandler(handler);
                    registerHandlerError(handler);
                } catch (SiemensHvacException ex) {
                    logger.debug("error unregistring handler: {}", handler);
                }

            }
        }
        logger.debug("check stale request::end : {}", staleRequest);
    }

    @Override
    public void waitNoNewRequest() {
        logger.debug("WaitNoNewRequest:start");
        try {
            int lastRequestCount = currentHandlerRegistry.keySet().size();
            boolean newRequest = true;
            while (newRequest) {
                Thread.sleep(5000);
                int newRequestCount = currentHandlerRegistry.keySet().size();
                if (newRequestCount != lastRequestCount) {
                    logger.debug("waitNoNewRequest  {}/{})", newRequestCount, lastRequestCount);
                    lastRequestCount = newRequestCount;
                } else {
                    newRequest = false;
                }
            }
        } catch (InterruptedException ex) {
            logger.debug("WaitAllPendingRequest:interrupted in WaitAllRequest");
        }

        logger.debug("WaitNoNewRequest:end WaitAllStartingRequest");
    }

    @Override
    public Gson getGson() {
        return gson;
    }

    @Override
    public Gson getGsonWithAdapter() {
        return gsonWithAdapter;
    }

    public void addDpUpdate(String itemName, Type dp) {
        synchronized (updateCommand) {
            updateCommand.put(itemName, dp);
        }
    }

    @Override
    public void resetSessionId(boolean web) {
        if (web) {
            sessionIdHttp = null;
        } else {
            sessionId = null;
        }
    }

    @Override
    public int getRequestCount() {
        return requestCount;
    }

    @Override
    public int getErrorCount() {
        return errorCount;
    }

    @Override
    public SiemensHvacRequestListener.ErrorSource getErrorSource() {
        return errorSource;
    }

    @Override
    public void invalidate() {
        sessionId = null;
        sessionIdHttp = null;
        currentHandlerRegistry.clear();
        handlerInErrorRegistry.clear();
    }

    public void setTimeOut(int timeout) {
        this.timeout = timeout;
    }
}
