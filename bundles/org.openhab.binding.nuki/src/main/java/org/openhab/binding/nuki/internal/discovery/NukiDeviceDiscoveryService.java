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
package org.openhab.binding.nuki.internal.discovery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nuki.internal.constants.NukiBindingConstants;
import org.openhab.binding.nuki.internal.dataexchange.BridgeListResponse;
import org.openhab.binding.nuki.internal.dto.BridgeApiListDeviceDto;
import org.openhab.binding.nuki.internal.handler.NukiBridgeHandler;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service which uses Brige API to find all devices connected to bridges.
 *
 * @author Jan Vybíral - Initial contribution
 */
@NonNullByDefault
public class NukiDeviceDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(NukiDeviceDiscoveryService.class);
    private final ScheduledExecutorService threadPool = ThreadPoolManager.getScheduledPool("discovery");
    @Nullable
    private NukiBridgeHandler bridge;

    public NukiDeviceDiscoveryService() {
        super(new HashSet<>(Arrays.asList(NukiBindingConstants.THING_TYPE_SMARTLOCK)), 5, false);
    }

    @Override
    protected void startScan() {
        if (bridge == null) {
            logger.warn("Cannot start Nuki discovery - no bridge available");
            return;
        }

        threadPool.execute(() -> {
            BridgeListResponse list = bridge.getNukiHttpClient().getList();

            list.getDevices().stream()
                    .filter(device -> NukiBindingConstants.SUPPORTED_DEVICES.contains(device.getDeviceType()))
                    .map(this::createDiscoveryResult).forEach(this::thingDiscovered);
        });
    }

    private DiscoveryResult createDiscoveryResult(BridgeApiListDeviceDto device) {
        return DiscoveryResultBuilder.create(getUid(device.getNukiId(), device.getDeviceType()))
                .withBridge(bridge.getThing().getUID()).withLabel(device.getName())
                .withRepresentationProperty(NukiBindingConstants.PROPERTY_NUKI_ID)
                .withProperty(NukiBindingConstants.PROPERTY_NAME, device.getName())
                .withProperty(NukiBindingConstants.PROPERTY_NUKI_ID, device.getNukiId())
                .withProperty(NukiBindingConstants.PROPERTY_FIRMWARE_VERSION, device.getFirmwareVersion()).build();
    }

    private ThingUID getUid(String nukiId, int deviceType) {
        if (deviceType == NukiBindingConstants.DEVICE_OPENER) {
            return new ThingUID(NukiBindingConstants.THING_TYPE_OPENER, this.bridge.getThing().getUID(), nukiId);
        } else {
            return new ThingUID(NukiBindingConstants.THING_TYPE_SMARTLOCK, this.bridge.getThing().getUID(), nukiId);
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof NukiBridgeHandler) {
            bridge = (NukiBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridge;
    }

    @Override
    public void deactivate() {
    }
}
