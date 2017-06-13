/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ikeatradfri.internal;

import static org.openhab.binding.ikeatradfri.IkeaTradfriBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.ikeatradfri.IkeaTradfriBindingConstants;
import org.openhab.binding.ikeatradfri.IkeaTradfriBulbConfiguration;
import org.openhab.binding.ikeatradfri.handler.IkeaTradfriGatewayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link IkeaTradfriDeviceDiscoveryService} is responsible for discovering all things
 * except the IKEA Tradfri gateway itself
 *
 * @author Daniel Sundberg - Initial contribution
 * @author Kai Kreuzer - refactorings
 */
public class IkeaTradfriDeviceDiscoveryService extends AbstractDiscoveryService
        implements IkeaTradfriDiscoveryListener {

    private final Logger logger = LoggerFactory.getLogger(IkeaTradfriDeviceDiscoveryService.class);

    private static final int SEARCH_TIME = 10;
    private IkeaTradfriGatewayHandler bridgeHandler;

    /**
     * Creates a IkeaTradlosDeviceDiscoveryService with background discovery disabled.
     */
    public IkeaTradfriDeviceDiscoveryService(IkeaTradfriGatewayHandler bridgeHandler) {
        super(IkeaTradfriBindingConstants.SUPPORTED_DEVICE_TYPES_UIDS, SEARCH_TIME, true);
        this.bridgeHandler = bridgeHandler;
    }

    public void activate() {
        bridgeHandler.registerDeviceListener(this);
    }

    @Override
    public void deactivate() {
        bridgeHandler.unregisterDeviceListener(this);
    }

    @Override
    protected void startScan() {
        logger.debug("Starting IKEA Tradfri device scan on gateway.");
    }

    @Override
    public void onDeviceFound(ThingUID bridge, JsonObject data) {
        if (bridge != null && data != null) {
            try {
                if (data.has(TRADFRI_LIGHT) && data.has(TRADFRI_INSTANCE_ID)) {
                    int id = data.get(TRADFRI_INSTANCE_ID).getAsInt();
                    ThingUID thingId;

                    JsonObject state = data.get(TRADFRI_LIGHT).getAsJsonArray().get(0).getAsJsonObject();

                    // White spectrum light
                    if (state.has(TRADFRI_COLOR)) {
                        thingId = new ThingUID(IkeaTradfriBindingConstants.THING_TYPE_WS_BULB, bridge,
                                Integer.toString(id));
                    } else {
                        thingId = new ThingUID(IkeaTradfriBindingConstants.THING_TYPE_WW_BULB, bridge,
                                Integer.toString(id));
                    }

                    String label = "IKEA Trådfri bulb";
                    try {
                        label = data.get(TRADFRI_NAME).getAsString();
                    } catch (JsonSyntaxException e) {
                        logger.error("JSON error: {}", e.getMessage());
                    }

                    Map<String, Object> properties = new HashMap<>(1);
                    properties.put(IkeaTradfriBulbConfiguration.ID, id);
                    logger.debug("Adding Tradfri bulb {} to inbox", thingId);
                    DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingId).withBridge(bridge)
                            .withLabel(label).withProperties(properties)
                            .withRepresentationProperty(IkeaTradfriBulbConfiguration.ID).build();
                    thingDiscovered(discoveryResult);

                }
            } catch (JsonSyntaxException e) {
                logger.error("JSON error: {}", e.getMessage());
            }
        }
    }
}
