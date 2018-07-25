/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nibeuplink.internal.connector;

import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus.Code;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.nibeuplink.config.NibeUplinkConfiguration;
import org.openhab.binding.nibeuplink.handler.NibeUplinkHandler;
import org.openhab.binding.nibeuplink.internal.AtomicReferenceUtils;
import org.openhab.binding.nibeuplink.internal.command.Login;
import org.openhab.binding.nibeuplink.internal.command.NibeUplinkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles requests to the NibeUplink web interface. It manages authentication and wraps commands.
 *
 * @author Alexander Friese - initial contribution
 */
@NonNullByDefault
public class UplinkWebInterface implements AtomicReferenceUtils {

    private static final long REQUEST_INITIAL_DELAY = 30000;
    private static final long REQUEST_INTERVAL = 5000;

    private final Logger logger = LoggerFactory.getLogger(UplinkWebInterface.class);

    /**
     * Configuration of the bridge from
     * {@link org.openhab.BoxHandler.fritzaha.handler.FritzAhaBridgeHandler}
     */
    private final NibeUplinkConfiguration config;

    /**
     * Bridge thing handler for updating thing status
     */
    private final NibeUplinkHandler uplinkHandler;

    /**
     * holds authentication status
     */
    private boolean authenticated = false;

    /**
     * HTTP client for asynchronous calls
     */
    private final HttpClient httpClient;

    /**
     * the scheduler which periodically sends web requests to the solaredge API. Should be initiated with the thing's
     * existing scheduler instance.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * request executor
     */
    private final WebRequestExecutor requestExecutor;

    /**
     * periodic request executor job
     */
    private AtomicReference<@Nullable Future<?>> requestExecutorJobReference = new AtomicReference<@Nullable Future<?>>(
            null);;

    /**
     * this class is responsible for executing periodic web requests. This ensures that only one request is executed at
     * the same time and there will be a guaranteed minimum delay between subsequent requests.
     *
     * @author afriese - initial contribution
     */
    private class WebRequestExecutor implements Runnable {

        /**
         * queue which holds the commands to execute
         */
        private final Queue<NibeUplinkCommand> commandQueue;

        /**
         * constructor
         */
        WebRequestExecutor() {
            this.commandQueue = new BlockingArrayQueue<>(20);
        }

        /**
         * puts a command into the queue
         *
         * @param command
         */
        void enqueue(NibeUplinkCommand command) {
            commandQueue.add(command);
        }

        /**
         * executes the web request
         */
        @Override
        public void run() {
            if (!isAuthenticated()) {
                authenticate();
            }

            else if (isAuthenticated() && !commandQueue.isEmpty()) {
                StatusUpdateListener statusUpdater = new StatusUpdateListener() {
                    @Override
                    public void update(CommunicationStatus status) {
                        if (Code.SERVICE_UNAVAILABLE.equals(status.getHttpCode())) {
                            uplinkHandler.setStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                                    status.getMessage());
                            setAuthenticated(false);
                        } else if (!Code.OK.equals(status.getHttpCode())) {
                            uplinkHandler.setStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    status.getMessage());
                            setAuthenticated(false);
                        }

                    }
                };

                NibeUplinkCommand command = commandQueue.poll();
                command.setListener(statusUpdater);
                command.performAction(httpClient);
            }
        }

    }

    /**
     * Constructor to set up interface
     *
     * @param config Bridge configuration
     */
    public UplinkWebInterface(NibeUplinkConfiguration config, ScheduledExecutorService scheduler,
            NibeUplinkHandler handler, HttpClient httpClient) {
        this.config = config;
        this.uplinkHandler = handler;
        this.scheduler = scheduler;
        this.requestExecutor = new WebRequestExecutor();
        this.httpClient = httpClient;
    }

    /**
     * starts the periodic request executor job which handles all web requests
     */
    public void start() {
        updateJobReference(requestExecutorJobReference, scheduler.scheduleWithFixedDelay(requestExecutor,
                REQUEST_INITIAL_DELAY, REQUEST_INTERVAL, TimeUnit.MILLISECONDS));
    }

    /**
     * queues any command for execution
     *
     * @param command
     */
    public void enqueueCommand(NibeUplinkCommand command) {
        requestExecutor.enqueue(command);
    }

    /**
     * authenticates with the Nibe Uplink WEB interface
     *
     * @throws UnsupportedEncodingException
     */
    private synchronized void authenticate() {
        setAuthenticated(false);

        if (preCheck()) {

            StatusUpdateListener statusUpdater = new StatusUpdateListener() {

                @Override
                public void update(CommunicationStatus status) {
                    if (Code.FOUND.equals(status.getHttpCode())) {
                        uplinkHandler.setStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "logged in");
                        setAuthenticated(true);
                    } else if (Code.OK.equals(status.getHttpCode())) {
                        uplinkHandler.setStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_ERROR,
                                "invalid username or password");
                        setAuthenticated(false);
                    } else if (Code.SERVICE_UNAVAILABLE.equals(status.getHttpCode())) {
                        uplinkHandler.setStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                                status.getMessage());
                        setAuthenticated(false);
                    } else {
                        uplinkHandler.setStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                status.getMessage());
                        setAuthenticated(false);
                    }

                }
            };

            new Login(uplinkHandler, statusUpdater).performAction(httpClient);
        }
    }

    /**
     * performs some pre cheks on configuration before attempting to login
     *
     * @return error message or SUCCESS
     */
    private boolean preCheck() {
        String preCheckStatusMessage = "";
        if (this.config.getPassword() == null || this.config.getPassword().isEmpty()) {
            preCheckStatusMessage = "please configure password first";
        } else if (this.config.getUser() == null || this.config.getUser().isEmpty()) {
            preCheckStatusMessage = "please configure user first";
        } else if (this.config.getNibeId() == null || this.config.getNibeId().isEmpty()) {
            preCheckStatusMessage = "please configure nibeId first";
        } else {
            return true;
        }

        this.uplinkHandler.setStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                preCheckStatusMessage);
        return false;

    }

    /**
     * will be called by the ThingHandler to abort periodic jobs.
     */
    public void dispose() {
        logger.debug("Webinterface disposed.");
        cancelJobReference(requestExecutorJobReference);
    }

    private boolean isAuthenticated() {
        return authenticated;
    }

    private void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
