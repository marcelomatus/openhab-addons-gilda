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
package org.openhab.binding.freebox.internal.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.freebox.internal.api.model.AuthorizationStatus;
import org.openhab.binding.freebox.internal.api.model.AuthorizationStatus.Status;
import org.openhab.binding.freebox.internal.api.model.AuthorizeResult;
import org.openhab.binding.freebox.internal.api.model.DiscoveryResponse;
import org.openhab.binding.freebox.internal.api.model.LoginResult;
import org.openhab.binding.freebox.internal.api.model.OpenSessionResult;
import org.openhab.binding.freebox.internal.config.ServerConfiguration;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link FreeboxApiManager} is responsible for the communication with the Freebox.
 * It implements the different HTTP API calls provided by the Freebox
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ApiManager {
    private final Logger logger = LoggerFactory.getLogger(ApiManager.class);
    private static final int HTTP_CALL_DEFAULT_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);
    // private static final String THREADPOOL_NAME = "freeboxApiManager";
    private static final String APP_ID = FrameworkUtil.getBundle(ApiManager.class).getSymbolicName();
    private static final String AUTH_HEADER = "X-Fbx-App-Auth";
    private static final String HTTP_CALL_CONTENT_TYPE = "application/json; charset=utf-8";

    private final Properties headers = new Properties();
    // private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREADPOOL_NAME);

    private @NonNullByDefault({}) String baseAddress;
    private @Nullable String appToken;
    private final Gson gson;

    public ApiManager(ServerConfiguration configuration, Gson gson) throws FreeboxException {
        this.gson = gson;
        logger.debug("Discover how to access the server...");
        baseAddress = String.format("%s://%s/", "http", configuration.hostAddress);
        DiscoveryResponse discovery = null;
        try {
            // First let's try simple
            discovery = execute(new APIRequests.checkAPI());
        } catch (FreeboxException e1) {
            if (configuration.remoteHttpsPort != -1) {
                // Then let's try with port
                baseAddress = String.format("%s://%s:%d/", "http", configuration.hostAddress,
                        configuration.remoteHttpsPort);
                try {
                    discovery = execute(new APIRequests.checkAPI());
                } catch (FreeboxException e2) {
                    if (configuration.httpsAvailable) {
                        // Then let's try with https
                        baseAddress = String.format("%s://%s:%d/", "https", configuration.hostAddress,
                                configuration.remoteHttpsPort);
                        discovery = execute(new APIRequests.checkAPI());
                    }
                }
            }
        }
        if (discovery != null) {
            this.appToken = configuration.appToken;
            authorize(discovery);
        } else {
            throw new FreeboxException(String.format("Can't connect to '%s'", configuration.hostAddress));
        }
    }

    private void authorize(DiscoveryResponse discovery) throws FreeboxException {
        logger.debug("Getting authorization on '{}'", baseAddress);
        if (!discovery.getApiBaseUrl().isEmpty() && !discovery.getApiVersion().isEmpty()) {
            String[] versionSplit = discovery.getApiVersion().split("\\.");
            String majorVersion = (versionSplit.length > 0) ? majorVersion = versionSplit[0] : "5";

            this.baseAddress = String.format("%s%sv%s/", baseAddress, discovery.getApiBaseUrl().substring(1),
                    majorVersion);

            try {
                if (appToken == null || appToken.isEmpty()) {
                    AuthorizeResult response = execute(
                            new APIRequests.Authorize(APP_ID, FrameworkUtil.getBundle(getClass())));
                    appToken = response.getAppToken();
                    AuthorizationStatus authorization;
                    do {
                        Thread.sleep(2000);
                        authorization = execute(new APIRequests.AuthorizationStatus(response.getTrackId()));
                    } while (authorization.getStatus() == Status.PENDING);
                    if (authorization.getStatus() != Status.GRANTED) {
                        throw new FreeboxException(String.format("Unable to grant session"));
                    }
                }
                openSession();
            } catch (InterruptedException e) {
                throw new FreeboxException("Granting process interrupted", e);
            }
        } else {
            throw new FreeboxException(
                    String.format("Missing API version or base URL on '%s' : '%s'", baseAddress, discovery.toString()));
        }
    }

    private synchronized void openSession() throws FreeboxException {
        LoginResult loginState = execute(new APIRequests.Login());
        if (!loginState.isLoggedIn()) {
            try {
                OpenSessionResult openSession = execute(
                        new APIRequests.OpenSession(APP_ID, appToken, loginState.getChallenge()));
                headers.setProperty(AUTH_HEADER, openSession.getSessionToken());
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                throw new FreeboxException("Error opening session : {}", e);
            }
        }
    }

    public synchronized void closeSession() {
        if (headers.getProperty(AUTH_HEADER) != null) {
            try {
                execute(new APIRequests.Logout());
            } catch (FreeboxException e) {
                logger.warn("Error closing session : {}", e.getMessage());
            }
            headers.remove(AUTH_HEADER);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends FreeboxResponse<F>, F> F execute(APIAction action) throws FreeboxException {
        String payload = action.getPayload() != null ? gson.toJson(action.getPayload()) : null;
        String url = baseAddress + action.getUrl();
        String jsonResponse = "";
        try {
            InputStream stream = payload != null ? new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8))
                    : null;

            logger.debug("executeUrl {} {} ", action.getMethod(), action.getUrl());
            jsonResponse = HttpUtil.executeUrl(action.getMethod(), url, headers, stream, HTTP_CALL_CONTENT_TYPE,
                    HTTP_CALL_DEFAULT_TIMEOUT_MS);

            if (stream != null) {
                stream.close();
                stream = null;
            }

            if (action instanceof APIRequests.checkAPI) {
                F fullResponse = gson.fromJson(jsonResponse, (Class<F>) action.getResponseClass());
                return fullResponse;
            } else {
                EmptyResponse partialResponse = gson.fromJson(jsonResponse, EmptyResponse.class);
                partialResponse.evaluate();
                T fullResponse = gson.fromJson(jsonResponse, (Class<T>) action.getResponseClass());
                if (action.getResponseClass() != EmptyResponse.class) {
                    fullResponse.evaluate();
                }
                return fullResponse.getResult();
            }
        } catch (FreeboxException | JsonSyntaxException | IOException e) {
            if (e instanceof FreeboxException && ((FreeboxException) e).isAuthRequired()) {
                openSession();
            }
            if (action.retriesLeft()) {
                logger.debug("Retry the request");
                return execute(action);
            }
            throw new FreeboxException(action.getMethod() + " request " + action.getUrl() + ": failed:" + e.getMessage()
                    + "||url :" + url + "||response : " + jsonResponse, e);
        }
    }

    public @Nullable String getAppToken() {
        return this.appToken;
    }
}
