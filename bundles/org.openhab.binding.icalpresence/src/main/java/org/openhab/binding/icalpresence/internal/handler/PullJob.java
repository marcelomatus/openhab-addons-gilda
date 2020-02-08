/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

package org.openhab.binding.icalpresence.internal.handler;

import static org.openhab.binding.icalpresence.internal.ICalPresenceBindingConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.icalpresence.internal.logic.AbstractPresentableCalendar;
import org.openhab.binding.icalpresence.internal.logic.CalendarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

/**
 * The Job for pulling an update of a calendar. Fires
 * {@link CalendarUpdateListener#onCalendarUpdated()} after successful update.
 *
 * @author Michael Wodniok - Initial contribution
 */
@NonNullByDefault
class PullJob implements Runnable {

    private final static int K = 1024;
    private final static String TMP_FILE_PREFIX = "icalpresencedld";
    private final Logger logger = LoggerFactory.getLogger(PullJob.class);
    private HttpClient httpClient;
    private URI sourceURI;
    private Authentication.@Nullable Result authentication;
    private File destination;
    private @Nullable CalendarUpdateListener listener;

    /**
     * Constructor of PullJob for creating a single pull of a calendar.
     *
     * @param httpClient A HttpClient for getting the source
     * @param sourceURI The source as URI
     * @param username Optional username for basic auth. Must be set together with a password.
     * @param password Optional password for basic auth. Must be set together with an username.
     * @param destination The destination the downloaded calendar should be saved to.
     * @param listener The listener that should be fired when update succeed.
     */
    public PullJob(HttpClient httpClient, URI sourceURI, @Nullable String username, @Nullable String password,
            File destination, @Nullable CalendarUpdateListener listener) {
        this.httpClient = httpClient;
        this.sourceURI = sourceURI;
        if (username != null && password != null) {
            this.authentication = new BasicAuthentication.BasicResult(this.sourceURI, username, password);
        } else {
            this.authentication = null;
        }
        this.destination = destination;
        this.listener = listener;
    }

    @Override
    public void run() {
        Request request = httpClient.newRequest(this.sourceURI).followRedirects(true).method(HttpMethod.GET);
        Authentication.@Nullable Result currentAuthentication = this.authentication;
        if (currentAuthentication != null) {
            currentAuthentication.apply(request);
        }

        InputStreamResponseListener asyncListener = new InputStreamResponseListener();
        request.send(asyncListener);

        Response response;
        try {
            response = asyncListener.get(HTTP_TIMEOUT_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e1) {
            logger.warn("Response for calendar request could not be retrieved.", e1);
            return;
        }

        if (response.getStatus() != HttpStatus.OK_200) {
            logger.warn("Response status for getting \"{}\" was {} instead of 200. Ignoring it.", sourceURI.toString(),
                    response.getStatus());
            return;
        }

        String responseLength = response.getHeaders().get(HttpHeader.CONTENT_LENGTH);
        if (responseLength != null) {
            try {
                if (Integer.parseInt(responseLength) > HTTP_MAX_CALENDAR_SIZE_K * K) {
                    logger.warn("Calendar is too big, aborting request");
                    response.abort(new ResponseTooBigException());
                    return;
                }
            } catch (NumberFormatException e) {
                logger.warn(
                        "While requesting calendar Content-Length was set, but is malformed. Falling back to read-loop.",
                        e);
            }
        }

        File tmpTargetFile;
        FileOutputStream tmpOutStream = null;
        try {
            tmpTargetFile = File.createTempFile(TMP_FILE_PREFIX, null);
            tmpOutStream = new FileOutputStream(tmpTargetFile);
            byte[] buffer = new byte[K];
            int readBytesTotal = 0;
            InputStream httpInputStream = asyncListener.getInputStream();
            int currentReadBytes = -1;
            while ((currentReadBytes = httpInputStream.read(buffer)) > -1) {
                readBytesTotal += currentReadBytes;
                if (readBytesTotal > HTTP_MAX_CALENDAR_SIZE_K * K) {
                    logger.warn("Calendar is too big. Stopping receiving calendar.");
                    response.abort(new ResponseTooBigException());
                    return;
                }
                tmpOutStream.write(buffer, 0, currentReadBytes);
            }
        } catch (IOException e) {
            logger.warn("Not able to write temporary file with downloaded iCal.", e);
            return;
        } finally {
            if (tmpOutStream != null) {
                try {
                    tmpOutStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        try {
            AbstractPresentableCalendar.create(new FileInputStream(tmpTargetFile));
        } catch (IOException | CalendarException e) {
            logger.warn("Not able to read downloaded iCal.", e);
            return;
        }

        try {
            Files.move(tmpTargetFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warn("Failed to replace iCal-file", e);
            return;
        }

        CalendarUpdateListener currentUpdateListener = this.listener;
        if (currentUpdateListener != null) {
            try {
                currentUpdateListener.onCalendarUpdated();
            } catch (Exception e) {
                logger.warn("An Exception was thrown while calling back", e);
            }
        }
    }

    /**
     * Interface for calling back when the update succeed.
     */
    public static interface CalendarUpdateListener {
        /**
         * Callback when update was successful and result was placed onto target file.
         */
        public void onCalendarUpdated();
    }

    /**
     * Exception for failure if size of the response is greater than allowed.
     */
    public static class ResponseTooBigException extends Exception {

        /**
         * The only local definition. Rest of implementation is taken from Exception or is default.
         */
        private static final long serialVersionUID = 7033851403473533793L;

    }
}
