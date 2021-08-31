/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.myq.internal.handler;

import static org.openhab.binding.myq.internal.MyQBindingConstants.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openhab.binding.myq.internal.MyQDiscoveryService;
import org.openhab.binding.myq.internal.config.MyQAccountConfiguration;
import org.openhab.binding.myq.internal.dto.AccountDTO;
import org.openhab.binding.myq.internal.dto.ActionDTO;
import org.openhab.binding.myq.internal.dto.DevicesDTO;
import org.openhab.core.auth.client.oauth2.AccessTokenRefreshListener;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link MyQAccountHandler} is responsible for communicating with the MyQ API based on an account.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class MyQAccountHandler extends BaseBridgeHandler implements AccessTokenRefreshListener {
    /*
     * MyQ oAuth relate fields
     */
    private static final String CLIENT_SECRET = "VUQ0RFhuS3lQV3EyNUJTdw==";
    private static final String CLIENT_ID = "IOS_CGI_MYQ";
    private static final String REDIRECT_URI = "com.myqops://ios";
    private static final String SCOPE = "MyQ_Residential offline_access";
    /*
     * MyQ authentication API endpoints
     */
    private static final String LOGIN_BASE_URL = "https://partner-identity.myq-cloud.com";
    private static final String LOGIN_AUTHORIZE_URL = LOGIN_BASE_URL + "/connect/authorize";
    private static final String LOGIN_TOKEN_URL = LOGIN_BASE_URL + "/connect/token";
    /*
     * MyQ device and account API endpoint
     */
    private static final String BASE_URL = "https://api.myqdevice.com/api";
    private static final Integer RAPID_REFRESH_SECONDS = 5;
    private final Logger logger = LoggerFactory.getLogger(MyQAccountHandler.class);
    private final Gson gsonUpperCase = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();
    private final Gson gsonLowerCase = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private @Nullable Future<?> normalPollFuture;
    private @Nullable Future<?> rapidPollFuture;
    private @Nullable AccountDTO account;
    private @Nullable DevicesDTO devicesCache;
    private Integer normalRefreshSeconds = 60;
    private HttpClient httpClient;
    private String username = "";
    private String password = "";
    private String userAgent = "";

    private final OAuthClientService oAuthService;

    public MyQAccountHandler(Bridge bridge, HttpClient httpClient, final OAuthFactory oAuthFactory) {
        super(bridge);
        this.httpClient = httpClient;
        this.oAuthService = oAuthFactory.createOAuthClientService(getThing().toString(), LOGIN_TOKEN_URL,
                LOGIN_AUTHORIZE_URL, CLIENT_ID, CLIENT_SECRET, SCOPE, false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        MyQAccountConfiguration config = getConfigAs(MyQAccountConfiguration.class);
        normalRefreshSeconds = config.refreshInterval;
        username = config.username;
        password = config.password;
        // MyQ can get picky about blocking user agents apparently
        userAgent = MyQAccountHandler.randomString(5);
        updateStatus(ThingStatus.UNKNOWN);
        restartPolls(false);
    }

    @Override
    public void dispose() {
        stopPolls();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(MyQDiscoveryService.class);
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        DevicesDTO localDeviceCaches = devicesCache;
        if (localDeviceCaches != null && childHandler instanceof MyQDeviceHandler) {
            MyQDeviceHandler handler = (MyQDeviceHandler) childHandler;
            localDeviceCaches.items.stream()
                    .filter(d -> ((MyQDeviceHandler) childHandler).getSerialNumber().equalsIgnoreCase(d.serialNumber))
                    .findFirst().ifPresent(handler::handleDeviceUpdate);
        }
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        logger.debug("Auth Token Refreshed, expires in {}", tokenResponse.getExpiresIn());
    }

    /**
     * Sends an action to the MyQ API
     *
     * @param serialNumber
     * @param action
     */
    public void sendAction(String serialNumber, String action) {
        AccountDTO localAccount = account;
        if (localAccount != null) {
            try {
                ContentResponse response = sendRequest(
                        String.format("%s/v5.1/Accounts/%s/Devices/%s/actions", BASE_URL, localAccount.account.id,
                                serialNumber),
                        HttpMethod.PUT, new StringContentProvider(gsonLowerCase.toJson(new ActionDTO(action))),
                        "application/json");
                if (HttpStatus.isSuccess(response.getStatus())) {
                    restartPolls(true);
                } else {
                    logger.debug("Failed to send action {} : {}", action, response.getContentAsString());
                }
            } catch (InterruptedException | IOException | OAuthException | ExecutionException
                    | OAuthResponseException e) {
                logger.debug("Could not send action", e);
            }
        }
    }

    /**
     * Last known state of MyQ Devices
     *
     * @return cached MyQ devices
     */
    public @Nullable DevicesDTO devicesCache() {
        return devicesCache;
    }

    private void stopPolls() {
        stopNormalPoll();
        stopRapidPoll();
    }

    private synchronized void stopNormalPoll() {
        stopFuture(normalPollFuture);
        normalPollFuture = null;
    }

    private synchronized void stopRapidPoll() {
        stopFuture(rapidPollFuture);
        rapidPollFuture = null;
    }

    private void stopFuture(@Nullable Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private synchronized void restartPolls(boolean rapid) {
        stopPolls();
        if (rapid) {
            normalPollFuture = scheduler.scheduleWithFixedDelay(this::normalPoll, 35, normalRefreshSeconds,
                    TimeUnit.SECONDS);
            rapidPollFuture = scheduler.scheduleWithFixedDelay(this::rapidPoll, 3, RAPID_REFRESH_SECONDS,
                    TimeUnit.SECONDS);
        } else {
            normalPollFuture = scheduler.scheduleWithFixedDelay(this::normalPoll, 0, normalRefreshSeconds,
                    TimeUnit.SECONDS);
        }
    }

    private void normalPoll() {
        stopRapidPoll();
        fetchData();
    }

    private void rapidPoll() {
        fetchData();
    }

    private synchronized void fetchData() {
        boolean validToken = false;
        try {
            validToken = oAuthService.getAccessTokenResponse() != null;
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.debug("error with oAuth service, attempting login again", e);
        }

        try {
            if (!validToken) {
                login();
            }
            if (account == null) {
                getAccount();
            }
            getDevices();
        } catch (TimeoutException | IOException | OAuthException | ExecutionException | OAuthResponseException e) {
            logger.debug("MyQ communication error", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (MyQAuthenticationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            stopPolls();
        } catch (InterruptedException e) {
            // we were shut down, ignore
        }
    }

    private void login() throws InterruptedException, IOException, OAuthException, ExecutionException,
            OAuthResponseException, TimeoutException, MyQAuthenticationException {

        // make sure we have a fresh session
        httpClient.getCookieStore().removeAll();

        String codeVerifier = generateCodeVerifier();

        ContentResponse loginPageResponse = getLoginPage(codeVerifier);

        // load the login page to get cookies and form parameters
        Document loginPage = Jsoup.parse(loginPageResponse.getContentAsString());
        Element form = loginPage.select("form").first();
        Element requestToken = loginPage.select("input[name=__RequestVerificationToken]").first();
        Element returnURL = loginPage.select("input[name=ReturnUrl]").first();

        if (form == null || requestToken == null) {
            throw new IOException("Coul not load login page");
        }

        String action = LOGIN_BASE_URL + form.attr("action");

        // post our user name and password along with elements from the scraped form
        String location = postLogin(action, requestToken.attr("value"), returnURL.attr("value"));
        if (location == null) {
            throw new MyQAuthenticationException("Could not login with credentials");
        }

        logger.debug("Login Response URI {}", location);

        // finally complete the oAuth flow and retrieve a JSON oAuth token response
        ContentResponse tokenResponse = getLoginToken(location, codeVerifier);
        String loginToken = tokenResponse.getContentAsString();
        logger.debug("Login Token response {}", loginToken);

        AccessTokenResponse accessTokenResponse = gsonLowerCase.fromJson(loginToken, AccessTokenResponse.class);
        if (accessTokenResponse == null) {
            throw new MyQAuthenticationException("Could not parse token response");
        }
        oAuthService.importAccessTokenResponse(accessTokenResponse);
    }

    private void getAccount()
            throws InterruptedException, IOException, OAuthException, ExecutionException, OAuthResponseException {
        ContentResponse response = sendRequest(BASE_URL + "/v5/My?expand=account", HttpMethod.GET, null, null);
        account = parseResultAndUpdateStatus(response, gsonUpperCase, AccountDTO.class);
    }

    private void getDevices()
            throws InterruptedException, IOException, OAuthException, ExecutionException, OAuthResponseException {
        AccountDTO localAccount = account;
        if (localAccount == null) {
            return;
        }
        ContentResponse response = sendRequest(
                String.format("%s/v5.1/Accounts/%s/Devices", BASE_URL, localAccount.account.id), HttpMethod.GET, null,
                null);
        DevicesDTO devices = parseResultAndUpdateStatus(response, gsonLowerCase, DevicesDTO.class);
        if (devices != null) {
            devicesCache = devices;
            devices.items.forEach(device -> {
                ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, device.deviceFamily);
                if (SUPPORTED_DISCOVERY_THING_TYPES_UIDS.contains(thingTypeUID)) {
                    for (Thing thing : getThing().getThings()) {
                        ThingHandler handler = thing.getHandler();
                        if (handler != null && ((MyQDeviceHandler) handler).getSerialNumber()
                                .equalsIgnoreCase(device.serialNumber)) {
                            ((MyQDeviceHandler) handler).handleDeviceUpdate(device);
                        }
                    }
                }
            });
        }
    }

    private synchronized ContentResponse sendRequest(String url, HttpMethod method, @Nullable ContentProvider content,
            @Nullable String contentType)
            throws InterruptedException, IOException, OAuthException, ExecutionException, OAuthResponseException {
        AccessTokenResponse tokenResponse = oAuthService.getAccessTokenResponse();
        if (tokenResponse == null) {
            throw new OAuthException("unable to get accessToken");
        }
        Request request = httpClient.newRequest(url).method(method).agent(userAgent).timeout(10, TimeUnit.SECONDS)
                .header("Authorization", tokenResponse.getTokenType() + " " + tokenResponse.getAccessToken());
        if (content != null & contentType != null) {
            request = request.content(content, contentType);
        }
        // use asyc jetty as the API service will response with a 401 error when credentials are wrong,
        // but not a WWW-Authenticate header which causes Jetty to throw a generic execution exception which
        // prevents us from knowing the response code
        logger.trace("Sending {} to {}", request.getMethod(), request.getURI());
        final CompletableFuture<ContentResponse> futureResult = new CompletableFuture<>();
        request.send(new BufferingResponseListener() {
            @NonNullByDefault({})
            @Override
            public void onComplete(Result result) {
                Response response = result.getResponse();
                futureResult.complete(new HttpContentResponse(response, getContent(), getMediaType(), getEncoding()));
            }
        });
        ContentResponse result = futureResult.get();
        logger.trace("Account Response - status: {} content: {}", result.getStatus(), result.getContentAsString());
        return result;
    }

    @Nullable
    private <T> T parseResultAndUpdateStatus(ContentResponse response, Gson parser, Class<T> classOfT) {
        if (HttpStatus.isSuccess(response.getStatus())) {
            try {
                T responseObject = parser.fromJson(response.getContentAsString(), classOfT);
                if (responseObject != null) {
                    if (getThing().getStatus() != ThingStatus.ONLINE) {
                        updateStatus(ThingStatus.ONLINE);
                    }
                    return responseObject;
                }
            } catch (JsonSyntaxException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Invalid JSON Response " + response.getContentAsString());
            }
        } else if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Unauthorized - Check Credentials");
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Invalid Response Code " + response.getStatus() + " : " + response.getContentAsString());
        }
        return null;
    }

    private ContentResponse getLoginPage(String codeVerifier)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            /*
             * this returns the login page, so we can grab cookies to log in with
             */
            Request request = httpClient.newRequest(LOGIN_AUTHORIZE_URL) //
                    .param("client_id", CLIENT_ID) //
                    .param("code_challenge", generateCodeChallange(codeVerifier)) //
                    .param("code_challenge_method", "S256") //
                    .param("redirect_uri", REDIRECT_URI) //
                    .param("response_type", "code") //
                    .param("scope", SCOPE) //
                    .agent("null").followRedirects(true);
            logger.debug("Sending {} to {}", request.getMethod(), request.getURI());
            ContentResponse response = request.send();
            logger.debug("Login Code {} Response {}", response.getStatus(), response.getContentAsString());
            return response;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new ExecutionException(e.getCause());
        }
    }

    @Nullable
    private String postLogin(String url, String requestToken, String returnURL)
            throws InterruptedException, ExecutionException, TimeoutException {
        /*
         * on a successful post to this page we will get several redirects, and a final 301 to:
         * com.myqops://ios?code=0123456789&scope=MyQ_Residential%20offline_access&iss=https%3A%2F%2Fpartner-identity.
         * myq-cloud.com
         *
         * We can then take the parameters out of this location and continue the process
         */
        Fields fields = new Fields();
        fields.add("Email", username);
        fields.add("Password", password);
        fields.add("__RequestVerificationToken", requestToken);
        fields.add("ReturnUrl", returnURL);

        Request request = httpClient.newRequest(url).method(HttpMethod.POST) //
                .content(new FormContentProvider(fields)) //
                .agent(userAgent) //
                .followRedirects(false);
        setCookies(request);

        logger.debug("Posting Login to {}", url);
        ContentResponse response = request.send();

        String location = null;

        // follow redirects until we match our REDIRECT_URI
        while (HttpStatus.isRedirection(response.getStatus())) {
            logger.debug("Redirect Login: Code {} Response {}", response.getStatus(), response.getContentAsString());
            String loc = response.getHeaders().get("location");
            logger.debug("location string {}", loc);
            if (loc == null) {
                logger.debug("No location value");
                break;
            }
            if (loc.indexOf(REDIRECT_URI) == 0) {
                location = loc;
                break;
            }
            request = httpClient.newRequest(LOGIN_BASE_URL + loc).agent(userAgent).followRedirects(false);
            setCookies(request);
            response = request.send();
        }
        return location;
    }

    private ContentResponse getLoginToken(String redirectLocation, String codeVerifier)
            throws InterruptedException, ExecutionException, TimeoutException {
        /*
         * this returns the login page, so we can grab cookies to log in with
         */
        try {
            Map<String, String> params = parseLocationQuery(redirectLocation);

            Fields fields = new Fields();
            fields.add("client_id", CLIENT_ID);
            fields.add("client_secret", Base64.getEncoder().encodeToString(CLIENT_SECRET.getBytes()));
            fields.add("code", params.get("code"));
            fields.add("code_verifier", codeVerifier);
            fields.add("grant_type", "authorization_code");
            fields.add("redirect_uri", REDIRECT_URI);
            fields.add("scope", params.get("scope"));

            Request request = httpClient.newRequest(LOGIN_TOKEN_URL) //
                    .content(new FormContentProvider(fields)) //
                    .method(HttpMethod.POST) //
                    .agent(userAgent).followRedirects(true);
            setCookies(request);

            ContentResponse response = request.send();
            logger.debug("Login Code {} Response {}", response.getStatus(), response.getContentAsString());
            return response;
        } catch (URISyntaxException e) {
            throw new ExecutionException(e.getCause());
        }
    }

    private static String randomString(int length) {
        int low = 97; // a-z
        int high = 122; // A-Z
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append((char) (low + (int) (random.nextFloat() * (high - low + 1))));
        }
        return sb.toString();
    }

    private String generateCodeVerifier() throws UnsupportedEncodingException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallange(String codeVerifier)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes("US-ASCII");
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private Map<String, String> parseLocationQuery(String location) throws URISyntaxException {
        URI uri = new URI(location);
        return Arrays.stream(uri.getQuery().split("&")).map(str -> str.split("="))
                .collect(Collectors.toMap(str -> str[0], str -> str[1]));
    }

    private void setCookies(Request request) {
        for (HttpCookie c : httpClient.getCookieStore().getCookies()) {
            request.cookie(c);
        }
    }

    class MyQAuthenticationException extends Exception {
        private static final long serialVersionUID = 1L;

        public MyQAuthenticationException(String message) {
            super(message);
        }
    }
}
