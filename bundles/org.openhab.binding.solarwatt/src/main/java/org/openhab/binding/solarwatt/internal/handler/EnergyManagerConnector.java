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
package org.openhab.binding.solarwatt.internal.handler;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.solarwatt.internal.SolarwattConfiguration;
import org.openhab.binding.solarwatt.internal.domain.EnergyManagerCollection;
import org.openhab.binding.solarwatt.internal.domain.dto.EnergyManagerDTO;
import org.openhab.binding.solarwatt.internal.exception.SolarwattConnectionException;
import org.openhab.binding.solarwatt.internal.factory.EnergyManagerDevicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Class to talk to the energy anager via HTTP and return the concrete device instances.
 *
 * @author Sven Carstens - Initial contribution
 */
@NonNullByDefault
public class EnergyManagerConnector {
    private static final String PROTOCOL = "http://";
    private static final String WIZARD_DEVICES_URL = "/rest/kiwigrid/wizard/devices";
    private static final long CONNECT_TIMEOUT_SECONDS = 3;

    private final Logger logger = LoggerFactory.getLogger(EnergyManagerConnector.class);
    private final Gson gson = new GsonBuilder().create();
    private final HttpClient httpClient;
    private String hostname = "";
    private @Nullable URI energyManagerURI;

    public EnergyManagerConnector(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Pass in the configuration to know which host to talk to.
     *
     * @param configuration containing the hostname.
     */
    public void setConfiguration(final @Nullable SolarwattConfiguration configuration) {
        if (configuration != null) {
            this.hostname = configuration.hostname;

            if (!"".equals(this.hostname)) {
                this.energyManagerURI = URI.create(PROTOCOL + this.hostname + WIZARD_DEVICES_URL);
            } else {
                this.logger.debug("Hostname is empty");
            }

        } else {
            this.logger.debug("Config is empty");
        }
    }

    /**
     * Get the collection of devices represented by the energy manager.
     *
     * Read the JSON and transform everything into concrete instances.
     *
     * @return wrapping the devices
     * @throws SolarwattConnectionException on any communication error
     */
    public EnergyManagerCollection retrieveDevices() throws SolarwattConnectionException {
        try {
            final Request request = this.httpClient.newRequest(this.energyManagerURI).timeout(CONNECT_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            final ContentResponse response = request.send();
            final String content = response.getContentAsString();

            this.logger.trace("Energymanager return with status {}: {} bytes", response.getStatus(), content.length());
            try {
                if (response.getStatus() == HttpStatus.OK_200) {
                    EnergyManagerDTO energyManagerDTO = this.gson.fromJson(content, EnergyManagerDTO.class);
                    if (energyManagerDTO == null) {
                        throw new SolarwattConnectionException("No data received");
                    }
                    return EnergyManagerDevicesFactory.getEnergyManagerCollection(energyManagerDTO);
                } else {
                    this.logger.debug("Energymanager returned an error: {}", response.getReason());
                    throw new SolarwattConnectionException(response.getReason());
                }
            } catch (final JsonSyntaxException e) {
                this.logger.debug("Error parsing json: {}", content, e);
                throw new SolarwattConnectionException(e.getMessage());
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SolarwattConnectionException("Interrupted");
        } catch (TimeoutException | ExecutionException e) {
            throw new SolarwattConnectionException("Connection problem", e);
        }
    }
}
