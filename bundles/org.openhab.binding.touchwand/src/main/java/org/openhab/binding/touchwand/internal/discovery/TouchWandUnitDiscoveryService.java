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

package org.openhab.binding.touchwand.internal.discovery;

import static org.openhab.binding.touchwand.internal.TouchWandBindingConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.touchwand.internal.TouchWandBridgeHandler;
import org.openhab.binding.touchwand.internal.TouchWandUnitStatusUpdateListener;
import org.openhab.binding.touchwand.internal.data.TouchWandShutterSwitchUnitData;
import org.openhab.binding.touchwand.internal.data.TouchWandUnitData;
import org.openhab.binding.touchwand.internal.data.TouchWandUnitDataWallController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link TouchWandUnitDiscoveryService} Discovery service for TouchWand units.
 *
 * @author Roie Geron - Initial contribution
 */
public class TouchWandUnitDiscoveryService extends AbstractDiscoveryService {

    private static final int SEARCH_TIME = 10;
    private static final int SCAN_INTERVAL = 60;
    private static final int LINK_DISCOVERY_SERVICE_INITIAL_DELAY = 5;
    private static final String[] CONNECTIVITY_OPTIONS = { "zwave", "knx" };

    private ScheduledFuture<?> scanningJob;

    private final TouchWandUnitScan scanningRunnable;

    private final Logger logger = LoggerFactory.getLogger(TouchWandUnitDiscoveryService.class);

    private List<TouchWandUnitStatusUpdateListener> listeners = new ArrayList<>();

    private final TouchWandBridgeHandler touchWandBridgeHandler;

    public TouchWandUnitDiscoveryService(TouchWandBridgeHandler touchWandBridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME, true);
        this.touchWandBridgeHandler = touchWandBridgeHandler;
        removeOlderResults(getTimestampOfLastScan(), touchWandBridgeHandler.getThing().getUID());
        this.scanningRunnable = new TouchWandUnitScan();
        this.activate();
    }

    @Override
    protected void startScan() {
        if (touchWandBridgeHandler.touchWandClient == null) {
            logger.warn("Could not scan units without bridge handler {}");
            return;
        }

        if (touchWandBridgeHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            logger.warn("Could not scan units while bridge offline");
            return;
        }

        logger.trace("Starting TouchWand discovery on bridge {}", touchWandBridgeHandler.getThing().getUID());
        String response = touchWandBridgeHandler.touchWandClient.cmdListUnits();
        if (response == null) {
            return;
        }

        JsonParser jsonParser = new JsonParser();
        try {
            JsonArray jsonArray = jsonParser.parse(response).getAsJsonArray();
            if (jsonArray.isJsonArray()) {
                try {
                    for (JsonElement unit : jsonArray) {
                        Gson gson = new Gson();
                        JsonObject unitObj = unit.getAsJsonObject();
                        TouchWandUnitData touchWandUnit;
                        String type = unitObj.get("type").getAsString();
                        if (!Arrays.asList(SUPPORTED_TOCUHWAND_TYPES).contains(type)) {
                            logger.debug("Unit discovery skipping unsupported unit type : {} ", type);
                            continue;
                        }

                        if (!unitObj.has("currStatus") || (unitObj.get("currStatus") == null)) {
                            logger.warn("Unit discovery unit currStatus is null : {}", response);
                            continue;
                        }

                        if (type.equals("WallController")) {
                            touchWandUnit = gson.fromJson(unitObj, TouchWandUnitDataWallController.class);
                        } else {
                            touchWandUnit = gson.fromJson(unitObj, TouchWandShutterSwitchUnitData.class);
                        }

                        if (!touchWandBridgeHandler.isAddSecondaryControllerUnits()) {
                            if (!Arrays.asList(CONNECTIVITY_OPTIONS).contains(touchWandUnit.getConnectivity())) {
                                continue;
                            }
                        }

                        String unitType = touchWandUnit.getType();

                        if (unitType.equals("Switch")) {
                            addDeviceDiscoveryResult(touchWandUnit, THING_TYPE_SWITCH);
                        } else if (unitType.equals("shutter")) {
                            addDeviceDiscoveryResult(touchWandUnit, THING_TYPE_SHUTTER);
                        } else if (unitType.equals("WallController")) {
                            addDeviceDiscoveryResult(touchWandUnit, THING_TYPE_WALLCONTROLLER);
                        } else if (unitType.equals("dimmer")) {
                            addDeviceDiscoveryResult(touchWandUnit, THING_TYPE_DIMMER);
                        }

                        /* sometimes current status received null , no point update listeners */
                        if (touchWandUnit.getCurrStatus() == null) {
                            // logger.warn("Unit id {} name {} CurrStatus is null", touchWandUnit.getId(),
                            // touchWandUnit.getName());
                            continue;
                        }

                        if (unitType.equals("Switch") || unitType.equals("shutter") || unitType.equals("dimmer")) {
                            norifyListeners(touchWandUnit);
                        }
                    }
                } catch (JsonSyntaxException e) {
                    logger.warn("Could not parse unit {}", e.getMessage());
                }
            }
        } catch (JsonSyntaxException msg) {
            logger.warn("Could not parse list units response {}", msg);
        }
    }

    private void norifyListeners(TouchWandUnitData touchWandUnit) {
        for (TouchWandUnitStatusUpdateListener listener : listeners) {
            listener.onDataReceived(touchWandUnit);
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    public void activate() {
        super.activate(null);
        removeOlderResults(new Date().getTime(), touchWandBridgeHandler.getThing().getUID());
        logger.trace("Activate TouchWand units discovery service");
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime(), touchWandBridgeHandler.getThing().getUID());
        super.deactivate();
        logger.trace("deactivate TouchWand units discovery services");
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.trace("Start TouchWand units background discovery");
        if (scanningJob == null || scanningJob.isCancelled()) {
            scanningJob = scheduler.scheduleWithFixedDelay(scanningRunnable, LINK_DISCOVERY_SERVICE_INITIAL_DELAY,
                    SCAN_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.trace("Stop TouchWand device units discovery");
        if (scanningJob != null && !scanningJob.isCancelled()) {
            scanningJob.cancel(true);
            scanningJob = null;
        }
    }

    public synchronized void registerListener(TouchWandUnitStatusUpdateListener listener) {
        if (!listeners.contains(listener)) {
            logger.debug("Adding TouchWandWebSocket listener {}", listener);
            listeners.add(listener);
        }
    }

    public synchronized void unregisterListener(TouchWandUnitStatusUpdateListener listener) {
        logger.debug("Removing TouchWandWebSocket listener {}", listener);
        listeners.remove(listener);
    }

    @NonNullByDefault
    public class TouchWandUnitScan implements Runnable {
        @Override
        public void run() {
            startScan();
        }
    }

    @Override
    public int getScanTimeout() {
        return SEARCH_TIME;
    }

    private void addDeviceDiscoveryResult(TouchWandUnitData unit, ThingTypeUID typeUID) {
        ThingUID bridgeUID = touchWandBridgeHandler.getThing().getUID();
        ThingUID thingUID = new ThingUID(typeUID, bridgeUID, unit.getId().toString());
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", unit.getId().toString());
        properties.put("name", unit.getName());
        // @formatter:off
        thingDiscovered(DiscoveryResultBuilder.create(thingUID)
                .withThingType(typeUID)
                .withLabel(unit.getName())
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withRepresentationProperty(unit.getId().toString())
                .build()
        );
        // @formatter:on
    }

}
