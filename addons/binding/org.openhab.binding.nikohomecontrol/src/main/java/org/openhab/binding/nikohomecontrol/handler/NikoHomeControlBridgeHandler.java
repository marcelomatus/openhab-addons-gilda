/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nikohomecontrol.handler;

import static org.openhab.binding.nikohomecontrol.NikoHomeControlBindingConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.nikohomecontrol.internal.discovery.NikoHomeControlDiscoveryService;
import org.openhab.binding.nikohomecontrol.internal.protocol.NikoHomeControlCommunication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NikoHomeControlBridgeHandler} is the handler for a Niko Home Control Gateway and connects it to
 * the framework.
 *
 * @author Mark Herwege
 */
public class NikoHomeControlBridgeHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(NikoHomeControlBridgeHandler.class);

    private NikoHomeControlCommunication nhcComm;

    private ScheduledFuture<?> refreshTimer;

    private InetAddress addr;
    private Integer port;

    private NikoHomeControlDiscoveryService nhcDiscovery;

    public NikoHomeControlBridgeHandler(Bridge nikoHomeControlBridge) {
        super(nikoHomeControlBridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // There is nothing to handle in the bridge handler
    }

    @Override
    public void initialize() {

        logger.debug("Niko Home Control: initializing bridge handler");

        Configuration config = this.getConfig();

        port = ((Number) config.get(CONFIG_PORT)).intValue();
        logger.debug("Niko Home Control: bridge handler port {}", port);

        try {
            // If hostname or address was provided in the configuration, try to use this to for bridge and give error
            // when hostname parameter was not valid.
            // No hostname provided is a valid configuration, therefore allow null addr to pass through.
            if (config.get(CONFIG_HOST_NAME) != null) {
                logger.debug("Niko Home Control: bridge handler host {}", config.get(CONFIG_HOST_NAME));
                addr = InetAddress.getByName((String) config.get(CONFIG_HOST_NAME));
            }
        } catch (UnknownHostException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    "Niko Home Control: cannot resolve bridge IP with hostname " + config.get(CONFIG_HOST_NAME));
            return;
        }

        Integer refreshInterval = ((Number) config.get(CONFIG_REFRESH)).intValue();

        createCommunicationObject();
        setupRefreshTimer(refreshInterval);

    }

    /**
     * Create communication object to Niko Home Control gateway and start communication.
     * Trigger discovery when communication setup is successful.
     *
     * @param nhcAddr IP address or subnet for gateway
     * @param port
     */
    private void createCommunicationObject() {

        nhcDiscovery = new NikoHomeControlDiscoveryService(thing.getUID(), this);
        nhcDiscovery.start(bundleContext);

        scheduler.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        // try connecting max 3 times
                        nhcComm = new NikoHomeControlCommunication(addr, port);
                        if (nhcComm.communicationActive()) {
                            break;
                        }
                        if (i == 2) {
                            throw new IOException("Niko Home Control: communication socket error");
                        }
                    }

                    addr = nhcComm.getAddr();
                    // keep the discovered host ip in the configuration to reuse when restarting
                    thing.getConfiguration().put(CONFIG_HOST_NAME, addr.getHostAddress());

                    updateProperties();

                    updateStatus(ThingStatus.ONLINE);

                    nhcDiscovery.discoverDevices();

                } catch (IOException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                            "Niko Home Control: error starting bridge connection");
                }
            }
        });

    }

    /**
     * Schedule future communication refresh.
     *
     * @param interval_config Time before refresh in minutes.
     */
    private void setupRefreshTimer(Integer refreshInterval) {

        if (refreshTimer != null) {
            refreshTimer.cancel(true);
            refreshTimer = null;
        }

        if ((refreshInterval == null) || (refreshInterval == 0)) {
            return;
        }

        // This timer will restart the bridge connection periodically
        logger.debug("Niko Home Control: restart bridge connection every {} min", refreshInterval);
        refreshTimer = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                logger.debug("Niko Home Control: restart communication at scheduled time");

                updateStatus(ThingStatus.OFFLINE);

                try {
                    for (int i = 0; i < 3; i++) {
                        // try restarting max 3 times
                        nhcComm.restartCommunication();
                        if (nhcComm.communicationActive()) {
                            break;
                        }
                        if (i == 2) {
                            throw new IOException("Niko Home Control: communication socket error");
                        }
                    }
                    addr = nhcComm.getAddr();
                    updateProperties();

                    updateStatus(ThingStatus.ONLINE);

                } catch (IOException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                            "Niko Home Control: error restarting bridge connection");
                }
            }
        }, refreshInterval, refreshInterval, TimeUnit.MINUTES);
    }

    /**
     * Update bridge properties with properties returned from Niko Home Control Controller, so they can be made visible
     * in PaperUI.
     *
     */
    private void updateProperties() {

        HashMap<String, String> properties = new HashMap<>();

        properties.put("ipAddress", addr.getHostAddress());
        properties.put("port", Integer.toString(port));
        properties.put("softwareVersion", nhcComm.getSwVersion());
        properties.put("apiVersion", nhcComm.getApi());
        properties.put("language", nhcComm.getLanguage());
        properties.put("currency", nhcComm.getCurrency());
        properties.put("units", nhcComm.getUnits());
        properties.put("tzOffset", nhcComm.getTz());
        properties.put("dstOffset", nhcComm.getDst());
        properties.put("configDate", nhcComm.getLastConfig());
        properties.put("energyEraseDate", nhcComm.getLastEnergyErase());
        properties.put("connectionStartDate", nhcComm.getTime());

        thing.setProperties(properties);

    }

    @Override
    public void dispose() {
        nhcComm.stopCommunication();
        nhcComm = null;

        nhcDiscovery.stop();
    }

    /**
     * Get the Niko Home Control communication object.
     *
     * @return Niko Home Control communication object
     */
    public NikoHomeControlCommunication getCommunication() {
        return nhcComm;
    }

}
