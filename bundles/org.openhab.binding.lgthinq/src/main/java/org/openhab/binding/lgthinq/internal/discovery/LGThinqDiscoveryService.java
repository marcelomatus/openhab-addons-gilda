/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.lgthinq.internal.discovery;

import static org.openhab.binding.lgthinq.handler.LGAirConditionerHandler.THING_TYPE_AIR_CONDITIONER;
import static org.openhab.binding.lgthinq.internal.LGThinqBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lgthinq.errors.LGThinqException;
import org.openhab.binding.lgthinq.handler.LGAirConditionerHandler;
import org.openhab.binding.lgthinq.handler.LGBridgeHandler;
import org.openhab.binding.lgthinq.lgapi.LGApiClientService;
import org.openhab.binding.lgthinq.lgapi.LGApiV1ClientServiceImpl;
import org.openhab.binding.lgthinq.lgapi.LGApiV2ClientServiceImpl;
import org.openhab.binding.lgthinq.lgapi.model.LGDevice;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LGThinqDiscoveryService}
 *
 * @author Nemer Daud - Initial contribution
 */
@NonNullByDefault
public class LGThinqDiscoveryService extends AbstractDiscoveryService implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(LGThinqDiscoveryService.class);
    private @Nullable LGBridgeHandler bridgeHandler;
    private @Nullable ThingUID bridgeHandlerUID;
    private final LGApiClientService lgApiV1ClientService, lgApiV2ClientService;

    public LGThinqDiscoveryService() {
        super(LGAirConditionerHandler.SUPPORTED_THING_TYPES, SEARCH_TIME);
        lgApiV1ClientService = LGApiV1ClientServiceImpl.getInstance();
        lgApiV2ClientService = LGApiV2ClientServiceImpl.getInstance();
    }

    @Override
    protected void startScan() {
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof LGBridgeHandler) {
            bridgeHandler = (LGBridgeHandler) handler;
            bridgeHandlerUID = handler.getThing().getUID();
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void activate() {
        if (bridgeHandler != null) {
            bridgeHandler.registerDiscoveryListener(this);
        }
    }

    @Override
    public void deactivate() {
        ThingHandlerService.super.deactivate();
    }

    public void removeLgDeviceDiscovery(LGDevice device) {
        try {
            ThingUID thingUID = getThingUID(device);
            thingRemoved(thingUID);
        } catch (LGThinqException e) {
            logger.error("Error getting Thing UID");
        }
    }

    public void addLgDeviceDiscovery(String bridgeName, LGDevice device) {
        String modelId = device.getModelName();
        ThingUID thingUID;
        ThingTypeUID thingTypeUID;
        try {
            thingUID = getThingUID(device);
            thingTypeUID = getThingTypeUID(device);
        } catch (LGThinqException e) {
            logger.debug("Discovered unsupported LG device of type '{}' and model '{}' with id {}",
                    device.getDeviceType(), modelId, device.getDeviceId());
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(DEVICE_ID, device.getDeviceId());
        properties.put(DEVICE_ALIAS, device.getAlias());
        properties.put(MODEL_URL_INFO, device.getModelJsonUri());
        properties.put(PLATFORM_TYPE, device.getPlatformType());
        try {
            // registry the capabilities of the thing
            if (PLATFORM_TYPE_V1.equals(device.getPlatformType())) {
                lgApiV1ClientService.getDeviceCapability(bridgeName, device.getModelJsonUri(), true);
            } else {
                lgApiV2ClientService.getDeviceCapability(bridgeName, device.getModelJsonUri(), true);
            }

        } catch (Exception ex) {
            logger.error(
                    "Error trying to get device capabilities in discovery service. Fallback to the defaults values",
                    ex);
        }
        properties.put(Thing.PROPERTY_MODEL_ID, modelId);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                .withProperties(properties).withBridge(bridgeHandlerUID).withRepresentationProperty(DEVICE_ID)
                .withLabel(device.getAlias()).build();

        thingDiscovered(discoveryResult);
    }

    private ThingUID getThingUID(LGDevice device) throws LGThinqException {
        ThingTypeUID thingTypeUID = getThingTypeUID(device);
        return new ThingUID(thingTypeUID,
                Objects.requireNonNull(bridgeHandlerUID, "bridgeHandleUid should never be null here"),
                device.getDeviceId());
    }

    private ThingTypeUID getThingTypeUID(LGDevice device) throws LGThinqException {
        // Short switch, but is here because it is going to be increase after new LG Devices were added
        switch (device.getDeviceType()) {
            case AIR_CONDITIONER:
                return THING_TYPE_AIR_CONDITIONER;
            default:
                throw new LGThinqException(String.format("device type [%s] not supported", device.getDeviceType()));
        }
    }
}
