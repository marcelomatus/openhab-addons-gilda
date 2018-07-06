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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus.Code;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.nibeuplink.config.NibeUplinkConfiguration;
import org.openhab.binding.nibeuplink.handler.NibeUplinkHandler;
import org.openhab.binding.nibeuplink.internal.command.Login;
import org.openhab.binding.nibeuplink.internal.command.NibeUplinkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles requests to the NibeUplink web interface. It manages authentication and wraps commands.
 *
 * @author Alexander Friese - initial contribution
 *
 */
public class UplinkWebInterface {

    private static final long LOGIN_WAIT_TIME = 1000;

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
     * Constructor to set up interface
     *
     * @param config Bridge configuration
     */
    public UplinkWebInterface(NibeUplinkConfiguration config, NibeUplinkHandler handler, HttpClient httpClient) {
        this.config = config;
        this.uplinkHandler = handler;
        this.httpClient = httpClient;
    }

    /**
     * executes any command provided by parameter
     *
     * @param command
     */
    public void executeCommand(NibeUplinkCommand command) {
        if (!isAuthenticated()) {
            authenticate();
            try {
                Thread.sleep(LOGIN_WAIT_TIME);
            } catch (InterruptedException e) {
            }
        }

        if (isAuthenticated()) {

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

            command.setListener(statusUpdater);
            command.performAction(httpClient);
        }

    }

    /**
     * authenticates with the Nibe Uplink WEB interface
     *
     * @throws UnsupportedEncodingException
     */
    public synchronized void authenticate() {
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

    private synchronized boolean isAuthenticated() {
        return authenticated;
    }

    private synchronized void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
