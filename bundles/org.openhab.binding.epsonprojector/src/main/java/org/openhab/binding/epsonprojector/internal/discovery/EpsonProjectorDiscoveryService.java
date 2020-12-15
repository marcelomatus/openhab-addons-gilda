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
package org.openhab.binding.epsonprojector.internal.discovery;

import static org.openhab.binding.epsonprojector.internal.EpsonProjectorBindingConstants.*;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EpsonProjectoreDiscoveryService} class implements a service
 * for discovering Epson projectors using the AMX Device Discovery protocol.
 *
 * @author Mark Hilbush - Initial contribution
 * @author Michael Lobstein - Adapted for the Epson Projector binding
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.epsonprojector")
public class EpsonProjectorDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(EpsonProjectorDiscoveryService.class);

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private @Nullable ScheduledFuture<?> epsonDiscoveryJob;

    // Discovery parameters
    public static final boolean BACKGROUND_DISCOVERY_ENABLED = true;
    public static final int BACKGROUND_DISCOVERY_DELAY = 10;

    private @Nullable NetworkAddressService networkAddressService;

    private boolean terminate = false;

    public EpsonProjectorDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 0, BACKGROUND_DISCOVERY_ENABLED);
        epsonDiscoveryJob = null;
        terminate = false;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void activate(@Nullable Map<String, Object> configProperties) {
        logger.debug("Epson Projector discovery service activated");
        super.activate(configProperties);
    }

    @Override
    protected void deactivate() {
        logger.debug("Epson projector discovery service deactivated");
        stopBackgroundDiscovery();
        super.deactivate();
    }

    @Override
    @Modified
    protected void modified(@Nullable Map<String, Object> configProperties) {
        super.modified(configProperties);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (epsonDiscoveryJob == null) {
            terminate = false;
            logger.debug("Starting background discovery job in {} seconds", BACKGROUND_DISCOVERY_DELAY);
            epsonDiscoveryJob = scheduledExecutorService.schedule(this::discover, BACKGROUND_DISCOVERY_DELAY,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> epsonDiscoveryJob = this.epsonDiscoveryJob;
        if (epsonDiscoveryJob != null) {
            logger.debug("Canceling background discovery job");
            terminate = true;
            epsonDiscoveryJob.cancel(false);
            this.epsonDiscoveryJob = null;
        }
    }

    @Override
    public void startScan() {
    }

    @Override
    public void stopScan() {
    }

    private synchronized void discover() {
        logger.debug("Discovery job is running");

        MulticastListener epsonMulticastListener;
        NetworkAddressService networkAddressService = this.networkAddressService;

        if (networkAddressService != null) {
            String local = "127.0.0.1";
            try {
                String ip = networkAddressService.getPrimaryIpv4HostAddress();
                epsonMulticastListener = new MulticastListener((ip != null ? ip : local));
            } catch (SocketException se) {
                logger.error("Discovery job got Socket exception creating multicast socket: {}", se.getMessage());
                return;
            } catch (IOException ioe) {
                logger.error("Discovery job got IO exception creating multicast socket: {}", ioe.getMessage());
                return;
            }

            while (!terminate) {
                boolean beaconReceived;
                try {
                    // Wait for a discovery beacon.
                    beaconReceived = epsonMulticastListener.waitForBeacon();
                } catch (IOException ioe) {
                    logger.debug("Discovery job got exception waiting for beacon: {}", ioe.getMessage());
                    beaconReceived = false;
                }

                if (beaconReceived) {
                    // We got a discovery beacon. Process it as a potential new thing
                    Map<String, Object> properties = new HashMap<>();

                    properties.put(THING_PROPERTY_HOST, epsonMulticastListener.getIPAddress());
                    properties.put(THING_PROPERTY_PORT, DEFAULT_PORT);

                    logger.trace("Projector with UID {} discovered at IP: {}", epsonMulticastListener.getUID(),
                            epsonMulticastListener.getIPAddress());

                    ThingUID uid = new ThingUID(THING_TYPE_PROJECTOR_TCP, epsonMulticastListener.getUID());
                    logger.trace("Creating epson projector discovery result for: {}, IP={}", uid,
                            epsonMulticastListener.getIPAddress());
                    thingDiscovered(DiscoveryResultBuilder.create(uid).withProperties(properties)
                            .withLabel("Epson Projector " + epsonMulticastListener.getUID()).build());
                }
            }
            epsonMulticastListener.shutdown();
            logger.debug("Discovery job is exiting");
        }
    }

    @Reference
    protected void setNetworkAddressService(NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
    }

    protected void unsetNetworkAddressService(NetworkAddressService networkAddressService) {
        this.networkAddressService = null;
    }
}
