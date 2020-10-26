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
package org.openhab.binding.gardena.internal.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.gardena.internal.GardenaSmart;
import org.openhab.binding.gardena.internal.GardenaSmartEventListener;
import org.openhab.binding.gardena.internal.GardenaSmartImpl;
import org.openhab.binding.gardena.internal.config.GardenaConfig;
import org.openhab.binding.gardena.internal.discovery.GardenaDeviceDiscoveryService;
import org.openhab.binding.gardena.internal.exception.GardenaException;
import org.openhab.binding.gardena.internal.model.Device;
import org.openhab.binding.gardena.internal.util.UidUtils;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GardenaAccountHandler} is the handler for a Gardena smart system access and connects it to the framework.
 *
 * @author Gerhard Riegler - Initial contribution
 */
public class GardenaAccountHandler extends BaseBridgeHandler implements GardenaSmartEventListener {
    private final Logger logger = LoggerFactory.getLogger(GardenaAccountHandler.class);
    private final static long REINITIALIZE_DELAY_SECONDS = 10;

    private GardenaDeviceDiscoveryService discoveryService;

    private GardenaSmart gardenaSmart = new GardenaSmartImpl();
    private GardenaConfig gardenaConfig;
    private HttpClientFactory httpClientFactory;
    private WebSocketFactory webSocketFactory;

    public GardenaAccountHandler(Bridge bridge, HttpClientFactory httpClientFactory,
            WebSocketFactory webSocketFactory) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
        this.webSocketFactory = webSocketFactory;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Gardena account '{}'", getThing().getUID().getId());

        gardenaConfig = getThing().getConfiguration().as(GardenaConfig.class);
        logger.debug("{}", gardenaConfig);

        initializeGardena();
    }

    public void setDiscoveryService(GardenaDeviceDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Initializes the GardenaSmart account.
     */
    private void initializeGardena() {
        final GardenaAccountHandler instance = this;
        scheduler.execute(() -> {
            try {
                String id = getThing().getUID().getId();
                gardenaSmart.init(id, gardenaConfig, instance, scheduler, httpClientFactory, webSocketFactory);
                discoveryService.startScan(null);
                discoveryService.waitForScanFinishing();
                updateStatus(ThingStatus.ONLINE);
            } catch (GardenaException ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
                disposeGardena();
                scheduleReinitialize();
                logger.debug("{}", ex.getMessage(), ex);
            }
        });
    }

    /**
     * Schedules a reinitialization, if Gardena smart system account is not reachable.
     */
    private void scheduleReinitialize() {
        scheduler.schedule(() -> {
            initializeGardena();
        }, REINITIALIZE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        super.dispose();
        disposeGardena();
    }

    /**
     * Disposes the GardenaSmart account.
     */
    private void disposeGardena() {
        logger.debug("Disposing Gardena account '{}'", getThing().getUID().getId());
        discoveryService.stopScan();
        gardenaSmart.dispose();
    }

    /**
     * Returns the Gardena smart system implementation.
     */
    public GardenaSmart getGardenaSmart() {
        return gardenaSmart;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(GardenaDeviceDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH == command) {
            logger.debug("Refreshing Gardena account '{}'", getThing().getUID().getId());
            disposeGardena();
            initializeGardena();
        }
    }

    @Override
    public void onDeviceUpdated(Device device) {
        for (ThingUID thingUID : UidUtils.getThingUIDs(device, getThing())) {
            Thing gardenaThing = getThing().getThing(thingUID);
            try {
                GardenaThingHandler gardenaThingHandler = (GardenaThingHandler) gardenaThing.getHandler();
                gardenaThingHandler.updateProperties(device);
                for (Channel channel : gardenaThing.getChannels()) {
                    gardenaThingHandler.updateChannel(channel.getUID());
                }
                gardenaThingHandler.updateStatus(device);
            } catch (GardenaException ex) {
                logger.error("There is something wrong with your thing '{}', please check or recreate it: {}",
                        gardenaThing.getUID(), ex.getMessage());
                logger.debug("Gardena exception caught on device update.", ex);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
            } catch (AccountHandlerNotAvailableException ignore) {
            }
        }
    }

    @Override
    public void onNewDevice(Device device) {
        if (discoveryService != null) {
            discoveryService.deviceDiscovered(device);
        }
        onDeviceUpdated(device);
    }

    @Override
    public void onError() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Connection lost");
        disposeGardena();
        gardenaSmart = new GardenaSmartImpl();
        scheduleReinitialize();
    }
}
