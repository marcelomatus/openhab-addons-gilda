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
package org.openhab.binding.boschindego.internal;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.boschindego.internal.dto.DeviceCommand;
import org.openhab.binding.boschindego.internal.dto.PredictiveAdjustment;
import org.openhab.binding.boschindego.internal.dto.PredictiveStatus;
import org.openhab.binding.boschindego.internal.dto.request.AuthenticationRequest;
import org.openhab.binding.boschindego.internal.dto.request.SetStateRequest;
import org.openhab.binding.boschindego.internal.dto.response.AuthenticationResponse;
import org.openhab.binding.boschindego.internal.dto.response.DeviceCalendarResponse;
import org.openhab.binding.boschindego.internal.dto.response.DeviceStateResponse;
import org.openhab.binding.boschindego.internal.dto.response.LocationWeatherResponse;
import org.openhab.binding.boschindego.internal.dto.response.PredictiveCuttingTimeResponse;
import org.openhab.binding.boschindego.internal.exceptions.IndegoAuthenticationException;
import org.openhab.binding.boschindego.internal.exceptions.IndegoException;
import org.openhab.binding.boschindego.internal.exceptions.IndegoInvalidCommandException;
import org.openhab.binding.boschindego.internal.exceptions.IndegoInvalidResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Controller for communicating with a Bosch Indego device through Bosch services.
 * This class provides methods for retrieving state information as well as controlling
 * the device.
 * 
 * The implementation is based on zazaz-de/iot-device-bosch-indego-controller, but
 * rewritten from scratch to use Jetty HTTP client for HTTP communication and GSON for
 * JSON parsing. Thanks to Oliver Schünemann for providing the original implementation.
 * 
 * @author Jacob Laursen - Initial contribution
 */
@NonNullByDefault
public class IndegoController {

    private static final String BASE_URL = "https://api.indego.iot.bosch-si.com/api/v1/";
    private static final String SERIAL_NUMBER_SUBPATH = "alms/";
    private static final String CONTEXT_HEADER_NAME = "x-im-context-id";
    private static final String CONTENT_TYPE_HEADER = "application/json";

    private final Logger logger = LoggerFactory.getLogger(IndegoController.class);
    private final String basicAuthenticationHeader;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    private IndegoSession session = new IndegoSession();

    /**
     * Initialize the controller instance.
     * 
     * @param username the username for authenticating
     * @param password the password
     */
    public IndegoController(HttpClient httpClient, String username, String password) {
        this.httpClient = httpClient;
        basicAuthenticationHeader = "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    /**
     * Authenticate with server and store session context and serial number.
     * 
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    private void authenticate() throws IndegoAuthenticationException, IndegoException {
        try {
            Request request = httpClient.newRequest(BASE_URL + "authenticate").method(HttpMethod.POST)
                    .header(HttpHeader.AUTHORIZATION, basicAuthenticationHeader);

            AuthenticationRequest authRequest = new AuthenticationRequest();
            authRequest.device = "";
            authRequest.osType = "Android";
            authRequest.osVersion = "4.0";
            authRequest.deviceManufacturer = "unknown";
            authRequest.deviceType = "unknown";
            String json = gson.toJson(authRequest);
            request.content(new StringContentProvider(json));
            request.header(HttpHeader.CONTENT_TYPE, CONTENT_TYPE_HEADER);

            if (logger.isTraceEnabled()) {
                logger.trace("POST request for {}", BASE_URL + "authenticate");
            }

            ContentResponse response = sendRequest(request);
            int status = response.getStatus();
            if (status == HttpStatus.UNAUTHORIZED_401) {
                throw new IndegoAuthenticationException("Authentication was rejected");
            }
            if (!HttpStatus.isSuccess(status)) {
                throw new IndegoAuthenticationException("The request failed with HTTP error: " + status);
            }

            String jsonResponse = response.getContentAsString();
            if (jsonResponse.isEmpty()) {
                throw new IndegoInvalidResponseException("No content returned");
            }
            logger.trace("JSON response: '{}'", jsonResponse);

            AuthenticationResponse authenticationResponse = gson.fromJson(jsonResponse, AuthenticationResponse.class);
            if (authenticationResponse == null) {
                throw new IndegoInvalidResponseException("Response could not be parsed as AuthenticationResponse");
            }
            session = new IndegoSession(authenticationResponse.contextId, authenticationResponse.serialNumber,
                    extractContextExpirationTime(response));
            logger.debug("Initialized session {}", session);
        } catch (JsonParseException e) {
            throw new IndegoInvalidResponseException("Error parsing AuthenticationResponse", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IndegoException(e);
        } catch (TimeoutException | ExecutionException e) {
            throw new IndegoException(e);
        }
    }

    /**
     * Extracts expiration time from response "Set-Cookie" header.
     * 
     * @param response
     * @return expiration time as {@link Instant}
     */
    private Instant extractContextExpirationTime(ContentResponse response) {
        List<HttpField> setCookieHeaders = response.getHeaders().getFields(HttpHeader.SET_COOKIE);
        for (HttpField setCookieHeader : setCookieHeaders) {
            try {
                var setCookie = new HttpCookie(setCookieHeader.toString());
                long maxAge = setCookie.getMaxAge();
                if (maxAge > 0) {
                    logger.trace("Accepted {}", setCookieHeader);
                    return Instant.now().plusSeconds(maxAge);
                } else {
                    logger.trace("Skipped {}", setCookieHeader);
                }
            } catch (Exception e) {
                logger.debug("Error parsing {}", setCookieHeader);
                return Instant.MIN;
            }
        }
        logger.trace("{} not found", HttpHeader.SET_COOKIE.asString());

        return Instant.MIN;
    }

    /**
     * Wraps {@link #getRequest(String, Class)} into an authenticated session.
     *
     * @param path the relative path to which the request should be sent
     * @param dtoClass the DTO class to which the JSON result should be deserialized
     * @return the deserialized DTO from the JSON response
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    private <T> T getRequestWithAuthentication(String path, Class<? extends T> dtoClass)
            throws IndegoAuthenticationException, IndegoException {
        if (!session.isValid()) {
            authenticate();
        }
        try {
            logger.debug("Session {} valid, skipping authentication", session);
            return getRequest(path, dtoClass);
        } catch (IndegoAuthenticationException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Context rejected", e);
            } else {
                logger.debug("Context rejected: {}", e.getMessage());
            }
            session.invalidate();
            authenticate();
            return getRequest(path, dtoClass);
        }
    }

    /**
     * Sends a GET request to the server and returns the deserialized JSON response.
     * 
     * @param path the relative path to which the request should be sent
     * @param dtoClass the DTO class to which the JSON result should be deserialized
     * @return the deserialized DTO from the JSON response
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    private <T> T getRequest(String path, Class<? extends T> dtoClass)
            throws IndegoAuthenticationException, IndegoException {
        try {
            Request request = httpClient.newRequest(BASE_URL + path).method(HttpMethod.GET).header(CONTEXT_HEADER_NAME,
                    session.getContextId());
            if (logger.isTraceEnabled()) {
                logger.trace("GET request for {}", BASE_URL + path);
            }
            ContentResponse response = sendRequest(request);
            int status = response.getStatus();
            if (status == HttpStatus.UNAUTHORIZED_401) {
                // This will currently not happen because "WWW-Authenticate" header is missing; see below.
                throw new IndegoAuthenticationException("Context rejected");
            }
            if (!HttpStatus.isSuccess(status)) {
                throw new IndegoAuthenticationException("The request failed with HTTP error: " + status);
            }
            String jsonResponse = response.getContentAsString();
            if (jsonResponse.isEmpty()) {
                throw new IndegoInvalidResponseException("No content returned");
            }
            logger.trace("JSON response: '{}'", jsonResponse);

            @Nullable
            T result = gson.fromJson(jsonResponse, dtoClass);
            if (result == null) {
                throw new IndegoInvalidResponseException("Parsed response is null");
            }
            return result;
        } catch (JsonParseException e) {
            throw new IndegoInvalidResponseException("Error parsing response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IndegoException(e);
        } catch (TimeoutException e) {
            throw new IndegoException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof HttpResponseException) {
                Response response = ((HttpResponseException) cause).getResponse();
                if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                    /*
                     * When contextId is not valid, the service will respond with HTTP code 401 without
                     * any "WWW-Authenticate" header, violating RFC 7235. Jetty will then throw
                     * HttpResponseException. We need to handle this in order to attempt
                     * reauthentication.
                     */
                    throw new IndegoAuthenticationException("Context rejected", e);
                }
            }
            throw new IndegoException(e);
        }
    }

    /**
     * Wraps {@link #putRequest(String, Object)} into an authenticated session.
     * 
     * @param path the relative path to which the request should be sent
     * @param requestDto the DTO which should be sent to the server as JSON
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    private void putRequestWithAuthentication(String path, Object requestDto)
            throws IndegoAuthenticationException, IndegoException {
        if (!session.isValid()) {
            authenticate();
        }
        try {
            logger.debug("Session {} valid, skipping authentication", session);
            putRequest(path, requestDto);
        } catch (IndegoAuthenticationException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Context rejected", e);
            } else {
                logger.debug("Context rejected: {}", e.getMessage());
            }
            session.invalidate();
            authenticate();
            putRequest(path, requestDto);
        }
    }

    /**
     * Sends a PUT request to the server.
     * 
     * @param path the relative path to which the request should be sent
     * @param requestDto the DTO which should be sent to the server as JSON
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    private void putRequest(String path, Object requestDto) throws IndegoAuthenticationException, IndegoException {
        try {
            Request request = httpClient.newRequest(BASE_URL + path).method(HttpMethod.PUT)
                    .header(CONTEXT_HEADER_NAME, session.getContextId())
                    .header(HttpHeader.CONTENT_TYPE, CONTENT_TYPE_HEADER);
            String payload = gson.toJson(requestDto);
            request.content(new StringContentProvider(payload));
            if (logger.isTraceEnabled()) {
                logger.trace("PUT request for {} with payload '{}'", BASE_URL + path, payload);
            }
            ContentResponse response = sendRequest(request);
            int status = response.getStatus();
            if (status == HttpStatus.UNAUTHORIZED_401) {
                // This will currently not happen because "WWW-Authenticate" header is missing; see below.
                throw new IndegoAuthenticationException("Context rejected");
            }
            if (status == HttpStatus.INTERNAL_SERVER_ERROR_500) {
                throw new IndegoInvalidCommandException("The request failed with HTTP error: " + status);
            }
            if (!HttpStatus.isSuccess(status)) {
                throw new IndegoException("The request failed with error: " + status);
            }
        } catch (JsonParseException e) {
            throw new IndegoInvalidResponseException("Error parsing response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IndegoException(e);
        } catch (TimeoutException e) {
            throw new IndegoException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof HttpResponseException) {
                Response response = ((HttpResponseException) cause).getResponse();
                if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                    /*
                     * When contextId is not valid, the service will respond with HTTP code 401 without
                     * any "WWW-Authenticate" header, violating RFC 7235. Jetty will then throw
                     * HttpResponseException. We need to handle this in order to attempt
                     * reauthentication.
                     */
                    throw new IndegoAuthenticationException("Context rejected", e);
                }
            }
            throw new IndegoException(e);
        }
    }

    private synchronized ContentResponse sendRequest(Request request)
            throws InterruptedException, TimeoutException, ExecutionException {
        return request.send();
    }

    /**
     * Gets serial number of the associated Indego device
     *
     * @return the serial number of the device
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    public String getSerialNumber() throws IndegoAuthenticationException, IndegoException {
        if (!session.isInitialized()) {
            logger.debug("Session not yet initialized when serial number was requested; authenticating...");
            authenticate();
        }
        return session.getSerialNumber();
    }

    /**
     * Queries the device state from the server.
     * 
     * @return the device state
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    public DeviceStateResponse getState() throws IndegoAuthenticationException, IndegoException {
        return getRequestWithAuthentication(SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/state",
                DeviceStateResponse.class);
    }

    public DeviceCalendarResponse getCalendar() throws IndegoException {
        DeviceCalendarResponse calendar = getRequestWithAuthentication(
                SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/calendar", DeviceCalendarResponse.class);
        return calendar;
    }

    /**
     * Sends a command to the Indego device.
     * 
     * @param command the control command to send to the device
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoInvalidCommandException if the command was not processed correctly
     * @throws IndegoException if any communication or parsing error occurred
     */
    public void sendCommand(DeviceCommand command)
            throws IndegoAuthenticationException, IndegoInvalidCommandException, IndegoException {
        SetStateRequest request = new SetStateRequest();
        request.state = command.getActionCode();
        putRequestWithAuthentication(SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/state", request);
    }

    /**
     * Queries the predictive weather forecast.
     * 
     * @return the weather forecast DTO
     * @throws IndegoAuthenticationException if request was rejected as unauthorized
     * @throws IndegoException if any communication or parsing error occurred
     */
    public LocationWeatherResponse getWeather() throws IndegoAuthenticationException, IndegoException {
        return getRequestWithAuthentication(SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive/weather",
                LocationWeatherResponse.class);
    }

    /**
     * Queries the predictive adjustment.
     * 
     * @return the predictive adjustment
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public int getPredictiveAdjustment() throws IndegoAuthenticationException, IndegoException {
        return getRequestWithAuthentication(
                SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive/useradjustment",
                PredictiveAdjustment.class).adjustment;
    }

    /**
     * Sets the predictive adjustment.
     * 
     * @param adjust the predictive adjustment
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public void setPredictiveAdjustment(final int adjust) throws IndegoAuthenticationException, IndegoException {
        final PredictiveAdjustment adjustment = new PredictiveAdjustment();
        adjustment.adjustment = adjust;
        putRequestWithAuthentication(SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive/useradjustment",
                adjustment);
    }

    /**
     * Queries predictive moving.
     * 
     * @return predictive moving
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public boolean getPredictiveMoving() throws IndegoAuthenticationException, IndegoException {
        final PredictiveStatus status = getRequestWithAuthentication(
                SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive", PredictiveStatus.class);
        return status.enabled;
    }

    /**
     * Sets predictive moving.
     * 
     * @param enable
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public void setPredictiveMoving(final boolean enable) throws IndegoAuthenticationException, IndegoException {
        final PredictiveStatus status = new PredictiveStatus();
        status.enabled = enable;
        putRequestWithAuthentication(SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive", status);
    }

    /**
     * Queries predictive next cutting as {@link Instant}.
     * 
     * @return predictive next cutting
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public Instant getPredictiveNextCutting() throws IndegoAuthenticationException, IndegoException {
        final PredictiveCuttingTimeResponse nextCutting = getRequestWithAuthentication(
                SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive/nextcutting",
                PredictiveCuttingTimeResponse.class);
        return nextCutting.getNextCutting();
    }

    /**
     * Queries predictive exclusion time.
     * 
     * @return predictive exclusion time DTO
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public DeviceCalendarResponse getPredictiveExclusionTime() throws IndegoAuthenticationException, IndegoException {
        final DeviceCalendarResponse calendar = getRequestWithAuthentication(
                SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive/calendar", DeviceCalendarResponse.class);
        return calendar;
    }

    /**
     * Sets predictive exclusion time.
     * 
     * @param calendar calendar DTO
     * @throws IndegoAuthenticationException
     * @throws IndegoException
     */
    public void setPredictiveExclusionTime(final DeviceCalendarResponse calendar)
            throws IndegoAuthenticationException, IndegoException {
        putRequestWithAuthentication(SERIAL_NUMBER_SUBPATH + this.getSerialNumber() + "/predictive/calendar", calendar);
    }
}
