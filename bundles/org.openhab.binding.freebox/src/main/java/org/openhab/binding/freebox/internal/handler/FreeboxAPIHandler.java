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
package org.openhab.binding.freebox.internal.handler;

import static org.openhab.binding.freebox.internal.FreeboxBindingConstants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.freebox.internal.api.FreeboxException;
import org.openhab.binding.freebox.internal.api.RelativePath;
import org.openhab.binding.freebox.internal.api.RequestAnnotation;
import org.openhab.binding.freebox.internal.api.model.AuthorizationStatus;
import org.openhab.binding.freebox.internal.api.model.AuthorizationStatus.Status;
import org.openhab.binding.freebox.internal.api.model.AuthorizationStatusResponse;
import org.openhab.binding.freebox.internal.api.model.AuthorizeRequest;
import org.openhab.binding.freebox.internal.api.model.AuthorizeResponse;
import org.openhab.binding.freebox.internal.api.model.AuthorizeResult;
import org.openhab.binding.freebox.internal.api.model.DiscoveryResponse;
import org.openhab.binding.freebox.internal.api.model.EmptyResponse;
import org.openhab.binding.freebox.internal.api.model.FreeboxResponse;
import org.openhab.binding.freebox.internal.api.model.LanHost;
import org.openhab.binding.freebox.internal.api.model.LanHostsResponse;
import org.openhab.binding.freebox.internal.api.model.LanInterface;
import org.openhab.binding.freebox.internal.api.model.LanInterfacesResponse;
import org.openhab.binding.freebox.internal.api.model.LoginResponse;
import org.openhab.binding.freebox.internal.api.model.LogoutResponse;
import org.openhab.binding.freebox.internal.api.model.OpenSessionRequest;
import org.openhab.binding.freebox.internal.api.model.OpenSessionResult;
import org.openhab.binding.freebox.internal.config.FreeboxAPIConfiguration;
import org.openhab.binding.freebox.internal.discovery.FreeboxDiscoveryService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link FreeboxAPIHandler} is responsible for the communication with the Freebox.
 * It implements the different HTTP API calls provided by the Freebox
 *
 * @author Laurent Garnier - Initial contribution
 */

@NonNullByDefault
public class FreeboxAPIHandler extends BaseBridgeHandler {
    private static final int HTTP_CALL_DEFAULT_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);
    private static final Bundle BUNDLE = FrameworkUtil.getBundle(FreeboxAPIHandler.class);
    private static final String appId = BUNDLE.getSymbolicName();

    private static final String HTTP_CALL_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String AUTH_HEADER = "X-Fbx-App-Auth";

    private final Logger logger = LoggerFactory.getLogger(FreeboxAPIHandler.class);
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private @NonNullByDefault({}) ScheduledFuture<?> authorizeJob;

    private @Nullable String baseAddress;
    private @NonNullByDefault({}) String appToken;
    private @Nullable String sessionToken;

    public FreeboxAPIHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Freebox Server handler for thing {}.", getThing().getUID());

        updateStatus(ThingStatus.UNKNOWN);

        logger.debug("Binding will schedule a job to establish a connection...");
        if (authorizeJob == null || authorizeJob.isCancelled()) {
            authorizeJob = scheduler.schedule(this::authorize, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("Binding does not handle commands");
    }

    public Map<String, String> discoverAttributes(DiscoveryResponse result) throws FreeboxException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(API_BASE_URL, result.getApiBaseUrl());

        if (result.getApiVersion() != null) {
            properties.put(API_VERSION, result.getApiVersion());
        }

        properties.put(Thing.PROPERTY_VENDOR, "Freebox SAS");
        updateProperties(properties);

        return properties;
    }

    private void authorize() {
        logger.debug("Authorize job...");

        FreeboxAPIConfiguration configuration = getConfigAs(FreeboxAPIConfiguration.class);
        String hostAddress = configuration.hostAddress + ":" + Long.toString(configuration.remoteHttpsPort);
        DiscoveryResponse result = null;
        boolean httpsRequestOk = false;
        if (configuration.httpsAvailable) {
            result = checkApi(hostAddress, true);
            httpsRequestOk = (result != null);
        }
        if (!httpsRequestOk) {
            result = checkApi(hostAddress, false);
        }
        boolean useHttps = false;
        String errorMsg = null;
        if (result == null) {
            errorMsg = "Can't connect to " + hostAddress;
        } else if (StringUtils.isEmpty(result.getApiBaseUrl())) {
            errorMsg = hostAddress + " does not deliver any API base URL";
        } else if (StringUtils.isEmpty(result.getApiVersion())) {
            errorMsg = hostAddress + " does not deliver any API version";
        } else if (result.isHttpsAvailable()) {
            if (result.getHttpsPort() == -1 || StringUtils.isEmpty(result.getApiDomain())) {
                if (httpsRequestOk) {
                    useHttps = true;
                } else {
                    logger.debug("{} does not deliver API domain or HTTPS port; use HTTP API", hostAddress);
                }
            } else if (checkApi(String.format("%s:%d", result.getApiDomain(), result.getHttpsPort()), true) != null) {
                useHttps = true;
                hostAddress = String.format("%s:%d", result.getApiDomain(), result.getHttpsPort());
            }
        }

        if (errorMsg != null) {
            logger.debug("Thing {}: bad configuration: {}", getThing().getUID(), errorMsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errorMsg);
        } else if (!authorize2(useHttps, hostAddress, result.getApiBaseUrl(), result.getApiVersion(),
                configuration.appToken)) {
            if (StringUtils.isEmpty(configuration.appToken)) {
                errorMsg = "App token not set in the thing configuration";
            } else {
                errorMsg = "Check your app token in the thing configuration; opening session with " + hostAddress
                        + " using " + (useHttps ? "HTTPS" : "HTTP") + " API version " + result.getApiVersion()
                        + " failed";
            }
            logger.debug("Thing {}: {}", getThing().getUID(), errorMsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errorMsg);
        } else {
            updateStatus(ThingStatus.ONLINE);
            logger.debug("Thing {}: session opened with {} using {} API version {}", getThing().getUID(), hostAddress,
                    (useHttps ? "HTTPS" : "HTTP"), result.getApiVersion());
        }

        if (thing.getProperties().isEmpty() && result != null) {
            try {
                discoverAttributes(result);
            } catch (FreeboxException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Error getting Freebox Server configuration items");
            }

        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Freebox Server handler for thing {}", getThing().getUID());
        if (authorizeJob != null && !authorizeJob.isCancelled()) {
            authorizeJob.cancel(true);
            authorizeJob = null;
        }
        closeSession();
        super.dispose();
    }

    public void logCommandException(FreeboxException e, ChannelUID channelUID, Command command) {
        if (e.isMissingRights()) {
            logger.debug("Thing {}: missing right {} while handling command {} from channel {}", getThing().getUID(),
                    e.getResponse().getMissingRight(), command, channelUID.getId());
        } else {
            logger.debug("Thing {}: error while handling command {} from channel {}", getThing().getUID(), command,
                    channelUID.getId(), e);
        }
    }

    public @Nullable DiscoveryResponse checkApi(String fqdn, boolean secureHttp) {
        String url = String.format("%s://%s/api_version", secureHttp ? "https" : "http", fqdn);
        try {
            String jsonResponse = HttpUtil.executeUrl("GET", url, HTTP_CALL_DEFAULT_TIMEOUT_MS);
            return gson.fromJson(jsonResponse, DiscoveryResponse.class);
        } catch (IOException | JsonSyntaxException e) {
            logger.debug("checkApi with {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    public boolean authorize2(boolean useHttps, String fqdn, String apiBaseUrl, String apiVersion, String appToken) {
        String[] versionSplit = apiVersion.split("\\.");
        String majorVersion = "5";
        if (versionSplit.length > 0) {
            majorVersion = versionSplit[0];
        }
        this.baseAddress = (useHttps ? "https://" : "http://") + fqdn + apiBaseUrl + "v" + majorVersion + "/";

        boolean granted = false;
        try {
            String token = appToken;
            if (StringUtils.isEmpty(token)) {
                AuthorizeRequest request = new AuthorizeRequest(appId, FrameworkUtil.getBundle(getClass()));
                AuthorizeResult response = executePost(AuthorizeResponse.class, null, request);
                token = response.getAppToken();

                logger.info("####################################################################");
                logger.info("# Please accept activation request directly on your freebox        #");
                logger.info("# Once done, record Apptoken in the Freebox thing configuration    #");
                logger.info("# {} #", token);
                logger.info("####################################################################");

                AuthorizationStatus result;
                do {
                    Thread.sleep(2000);
                    result = executeGet(AuthorizationStatusResponse.class, response.getTrackId().toString());
                } while (result.getStatus() == Status.PENDING);
                granted = result.getStatus() == Status.GRANTED;
            } else {
                granted = true;
            }
            if (!granted) {
                return false;
            }

            this.appToken = token;
            openSession();
            return true;
        } catch (FreeboxException | InterruptedException e) {
            logger.debug("Error while opening a session", e);
            return false;
        }
    }

    private synchronized void openSession() throws FreeboxException {
        String challenge = executeGet(LoginResponse.class, null).getChallenge();
        OpenSessionResult loginResult = execute(new OpenSessionRequest(appId, appToken, challenge), null);
        sessionToken = loginResult.getSessionToken();
    }

    public synchronized void closeSession() {
        if (sessionToken != null) {
            try {
                executePost(LogoutResponse.class, null, null);
            } catch (FreeboxException e) {
                logger.warn("Error closing session : {}", e.getMessage());
            }
            sessionToken = null;
        }
    }

    public <T extends FreeboxResponse<F>, F> F executeGet(Class<T> responseClass, @Nullable String request)
            throws FreeboxException {
        Annotation[] annotations = responseClass.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof RelativePath) {
                RelativePath myAnnotation = (RelativePath) annotation;
                String relativeUrl = myAnnotation.relativeUrl();
                if (request != null) {
                    relativeUrl += encodeUrl(request) + "/";
                }
                return executeUrl("GET", relativeUrl, null, responseClass, myAnnotation.retryAuth(), false, false);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends FreeboxResponse<F>, F> F execute(Object request, @Nullable String requestUrl)
            throws FreeboxException {
        Annotation[] annotations = request.getClass().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestAnnotation) {
                RequestAnnotation myAnnotation = (RequestAnnotation) annotation;
                Class<T> answerClass = (Class<T>) myAnnotation.responseClass();
                String relativeUrl = myAnnotation.relativeUrl();
                if (requestUrl != null) {
                    relativeUrl += encodeUrl(requestUrl) + "/";
                }
                return executeUrl(myAnnotation.method(), relativeUrl, gson.toJson(request), answerClass,
                        myAnnotation.retryAuth(), false, false);
            }
        }
        return null;
    }

    protected <T extends FreeboxResponse<F>, F> F executePost(Class<T> responseClass, @Nullable String request,
            @Nullable Object content) throws FreeboxException {
        Annotation[] annotations = responseClass.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof RelativePath) {
                RelativePath myAnnotation = (RelativePath) annotation;
                String relativeUrl = myAnnotation.relativeUrl();
                if (request != null) {
                    relativeUrl += encodeUrl(request) + "/";
                }
                return executeUrl("POST", relativeUrl, content != null ? gson.toJson(content) : null, responseClass,
                        myAnnotation.retryAuth(), false, false);
            }
        }
        return null;
    }

    private <T extends FreeboxResponse<F>, F> F executeUrl(String httpMethod, @Nullable String relativeUrl,
            @Nullable String requestContent, Class<T> responseClass, boolean retryAuth, boolean patchTableReponse,
            boolean doNotLogData) throws FreeboxException {
        try {
            Properties headers = null;
            String token = sessionToken;
            if (token != null) {
                headers = new Properties();
                headers.setProperty(AUTH_HEADER, token);
            }
            InputStream stream = null;
            String contentType = null;
            if (requestContent != null) {
                stream = new ByteArrayInputStream(requestContent.getBytes(StandardCharsets.UTF_8));
                contentType = HTTP_CALL_CONTENT_TYPE;
            }
            logger.debug("executeUrl {} {} requestContent {}", httpMethod, relativeUrl,
                    doNotLogData ? "***" : requestContent);
            String jsonResponse = HttpUtil.executeUrl(httpMethod, baseAddress + relativeUrl, headers, stream,
                    contentType, HTTP_CALL_DEFAULT_TIMEOUT_MS);
            if (stream != null) {
                stream.close();
                stream = null;
            }

            if (patchTableReponse) {
                // Replace empty result by an empty table result
                jsonResponse = jsonResponse.replace("\"result\":{}", "\"result\":[]");
            }

            return evaluateJsonResponse(jsonResponse, responseClass, doNotLogData);
        } catch (FreeboxException e) {
            if (retryAuth && e.isAuthRequired()) {
                logger.debug("Authentication required: open a new session and retry the request");
                openSession();
                return executeUrl(httpMethod, relativeUrl, requestContent, responseClass, false, patchTableReponse,
                        doNotLogData);
            }
            throw e;
        } catch (IOException e) {
            throw new FreeboxException(httpMethod + " request " + relativeUrl + ": execution failed: " + e.getMessage(),
                    e);
        } catch (JsonSyntaxException e) {
            throw new FreeboxException(
                    httpMethod + " request " + relativeUrl + ": response parsing failed: " + e.getMessage(), e);
        }
    }

    private <T extends FreeboxResponse<F>, F> F evaluateJsonResponse(String jsonResponse, Class<T> responseClass,
            boolean doNotLogData) throws JsonSyntaxException, FreeboxException {
        logger.debug("evaluateJsonReesponse Json {}", doNotLogData ? "***" : jsonResponse);
        // First check only if the result is successful
        FreeboxResponse<Object> partialResponse = gson.fromJson(jsonResponse, EmptyResponse.class);
        partialResponse.evaluate();
        // Parse the full response in case of success
        T fullResponse = gson.fromJson(jsonResponse, responseClass);
        fullResponse.evaluate();
        F result = fullResponse.getResult();
        return result;
    }

    private String encodeUrl(String url) throws FreeboxException {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new FreeboxException("Encoding the URL \"" + url + "\" in UTF-8 failed", e);
        }
    }

    public List<LanHost> getLanHosts() throws FreeboxException {
        List<LanHost> hosts = new ArrayList<>();
        List<LanInterface> lans = executeGet(LanInterfacesResponse.class, null);
        lans.stream().filter(LanInterface::hasHosts).forEach(lan -> {
            List<LanHost> lanHosts;
            try {
                lanHosts = executeGet(LanHostsResponse.class, lan.getName());
                hosts.addAll(lanHosts);
            } catch (FreeboxException e) {
                logger.warn("Error getting hosts for interface {}", lan.getName());
            }
        });
        return hosts;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(FreeboxDiscoveryService.class);
    }

    public FreeboxAPIConfiguration getConfiguration() {
        return getConfigAs(FreeboxAPIConfiguration.class);
    }
}
