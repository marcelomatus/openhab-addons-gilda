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
package org.openhab.binding.somfymylink.internal.discovery;

import static org.openhab.binding.somfymylink.internal.SomfyMyLinkBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.ScanListener;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.somfymylink.internal.SomfyMyLinkHandlerFactory;
import org.openhab.binding.somfymylink.internal.handler.SomfyMyLinkBridgeHandler;
import org.openhab.binding.somfymylink.internal.handler.SomfyMyLinkException;
import org.openhab.binding.somfymylink.internal.model.SomfyMyLinkScene;
import org.openhab.binding.somfymylink.internal.model.SomfyMyLinkShade;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SomfyMyLinkDeviceDiscoveryService} is responsible discovering things connected to the mylink.
 *
 * @author Chris Johnson - Initial contribution
 */
@NonNullByDefault
public class SomfyMyLinkDeviceDiscoveryService extends AbstractDiscoveryService
        implements DiscoveryService, ThingHandlerService {

    private static final int DISCOVERY_REFRESH_SEC = 900;

    private final Logger logger = LoggerFactory.getLogger(SomfyMyLinkDeviceDiscoveryService.class);
    private @NonNullByDefault({}) SomfyMyLinkBridgeHandler mylinkHandler;
    private @Nullable Future<?> scanTask;
    private @Nullable ScheduledFuture<?> discoveryJob;

    public SomfyMyLinkDeviceDiscoveryService() {
        super(SomfyMyLinkHandlerFactory.DISCOVERABLE_DEVICE_TYPES_UIDS, 10);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof SomfyMyLinkBridgeHandler) {
            this.mylinkHandler = (SomfyMyLinkBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return mylinkHandler;
    }

    @Override
    @Activate
    public void activate() {
        super.activate(null);
    }

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Somfy My Link background discovery");

        ScheduledFuture<?> discoveryJob = this.discoveryJob;
        if (discoveryJob == null || discoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::discoverDevices, 10,
                    DISCOVERY_REFRESH_SEC, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Somfy MyLink background discovery");

        ScheduledFuture<?> discoveryJob = this.discoveryJob;
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    @Override
    protected synchronized void startScan() {
        Future<?> scanTask = this.scanTask;
        if (scanTask == null || scanTask.isDone()) {
            logger.debug("Starting somfy mylink discovery scan");
            scanTask = scheduler.submit(this::discoverDevices);
        }
    }

    @Override
    public void stopScan() {
        Future<?> scanTask = this.scanTask;
        if (scanTask != null) {
            logger.debug("Stopping somfy mylink discovery scan");
            scanTask.cancel(true);
        }
        super.stopScan();
    }

    public void waitForScanFinishing() {
        if (scanTask != null) {
            logger.debug("Waiting for finishing somfy mylink device discovery scan");
            try {
                scanTask.get();
                logger.debug("Somfy mylink device discovery scan finished");
            } catch (CancellationException ex) {
                // ignore

            } catch (Exception ex) {
                logger.error("Error waiting for device discovery scan: {}", ex.getMessage(), ex);
            }
        }
    }

    private synchronized void discoverDevices() {
        logger.debug("Starting scanning for things...");

        if (this.mylinkHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Skipping device discover as bridge is {}", this.mylinkHandler.getThing().getStatus());
            return;
        }

        try {
            // get the shade list
            SomfyMyLinkShade[] shades = this.mylinkHandler.getShadeList();

            for (SomfyMyLinkShade shade : shades) {
                String id = shade.getTargetID();
                String label = "Somfy Shade " + shade.getName();

                if (id != null) {
                    logger.info("Adding device {}", id);
                    notifyThingDiscovery(THING_TYPE_SHADE, id, label, TARGET_ID);
                }
            }

            SomfyMyLinkScene[] scenes = this.mylinkHandler.getSceneList();

            for (SomfyMyLinkScene scene : scenes) {
                String id = scene.getTargetID();
                String label = "Somfy Scene " + scene.getName();

                logger.info("Adding device {}", id);
                notifyThingDiscovery(THING_TYPE_SCENE, id, label, SCENE_ID);
            }
        } catch (SomfyMyLinkException e) {
            logger.info("Error scanning for devices: {}", e.getMessage(), e);
            ScanListener scanListener = this.scanListener;
            if (scanListener != null) {
                scanListener.onErrorOccurred(e);
            }
        }
    }

    private void notifyThingDiscovery(ThingTypeUID thingTypeUID, String id, String label, String idType) {
        if (StringUtils.isEmpty(id)) {
            logger.info("Discovered {} with no ID", label);
            return;
        }

        ThingUID bridgeUID = this.mylinkHandler.getThing().getUID();
        ThingUID uid = new ThingUID(thingTypeUID, bridgeUID, id);

        Map<String, Object> properties = new HashMap<>();

        properties.put(idType, id);

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withLabel(label)
                .withProperties(properties).withRepresentationProperty(idType).build();

        thingDiscovered(result);

        logger.debug("Discovered {}", uid);
    }
}
