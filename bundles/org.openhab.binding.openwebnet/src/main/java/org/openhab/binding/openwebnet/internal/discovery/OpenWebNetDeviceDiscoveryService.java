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
package org.openhab.binding.openwebnet.internal.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.openwebnet.OpenWebNetBindingConstants;
import org.openhab.binding.openwebnet.handler.OpenWebNetBridgeHandler;
import org.openwebnet.OpenDeviceType;
import org.openwebnet.OpenNewDeviceListener;
import org.openwebnet.message.BaseOpenMessage;
import org.openwebnet.message.OpenMessageFactory;
import org.openwebnet.message.Who;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenWebNetDeviceDiscoveryService} is responsible for discovering OpenWebNet devices connected to a
 * bridge/gateway
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
public class OpenWebNetDeviceDiscoveryService extends AbstractDiscoveryService implements OpenNewDeviceListener {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = OpenWebNetBindingConstants.DEVICE_SUPPORTED_THING_TYPES;

    private final static int SEARCH_TIME = 60;

    private final Logger logger = LoggerFactory.getLogger(OpenWebNetDeviceDiscoveryService.class);
    private final OpenWebNetBridgeHandler bridgeHandler;
    private final ThingUID bridgeUID;

    public OpenWebNetDeviceDiscoveryService(OpenWebNetBridgeHandler handler) {
        super(SEARCH_TIME);
        bridgeHandler = handler;
        bridgeUID = handler.getThing().getUID();
        logger.debug("Constructor for bridge: '{}'", bridgeUID);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return OpenWebNetDeviceDiscoveryService.SUPPORTED_THING_TYPES;
    }

    @Override
    protected void startScan() {
        logger.info("------ startScan() - SEARCHING for DEVICES on bridge '{}' ({}) ...",
                bridgeHandler.getThing().getLabel(), bridgeUID);
        bridgeHandler.searchDevices(this);
    }

    @Override
    protected void stopScan() {
        logger.debug("------ stopScan() on bridge '{}'", bridgeUID);
        bridgeHandler.scanStopped();
    }

    @Override
    public void abortScan() {
        logger.debug("------ abortScan() on bridge '{}'", bridgeUID);
        bridgeHandler.scanStopped();
    }

    @Override
    public void onNewDevice(@Nullable String where, @Nullable OpenDeviceType deviceType,
            @Nullable BaseOpenMessage msg) {
        try {
            if (where != null && deviceType != null) {
                newDiscoveryResult(where, deviceType, msg);
            } else {
                logger.warn("onNewDevice with null where/deviceType msg={}", msg);
            }
        } catch (Exception e) {
            logger.warn("Exception while discovering new device WHERE={}, deviceType={}: {}", where, deviceType,
                    e.getMessage());
        }
    }

    /**
     * Create and notify to Inbox a new DiscoveryResult based on where, OpenDeviceType and BaseOpenMessage
     *
     * @param where the discovered device's address (WHERE)
     * @param deviceType {@link OpenDeviceType} of the discovered device
     * @param message the OWN message received that identified the device (optional)
     */
    public void newDiscoveryResult(String where, OpenDeviceType deviceType, @Nullable BaseOpenMessage baseMsg) {
        logger.info("newDiscoveryResult() WHERE={}, deviceType={}", where, deviceType);
        ThingTypeUID thingTypeUID = OpenWebNetBindingConstants.THING_TYPE_DEVICE; // generic device
        String thingLabel = OpenWebNetBindingConstants.THING_LABEL_DEVICE;
        Who deviceWho = Who.DEVICE_DIAGNOSTIC; // TODO change to another Who (unknown?)
        switch (deviceType) {
            case ZIGBEE_ON_OFF_SWITCH: {
                thingTypeUID = OpenWebNetBindingConstants.THING_TYPE_ZB_ON_OFF_SWITCH;
                thingLabel = OpenWebNetBindingConstants.THING_LABEL_ZB_ON_OFF_SWITCH;
                deviceWho = Who.LIGHTING;
                break;
            }
            case ZIGBEE_DIMMER_SWITCH: {
                thingTypeUID = OpenWebNetBindingConstants.THING_TYPE_ZB_DIMMER;
                thingLabel = OpenWebNetBindingConstants.THING_LABEL_ZB_DIMMER;
                deviceWho = Who.LIGHTING;
                break;
            }
            case SCS_ON_OFF_SWITCH: {
                thingTypeUID = OpenWebNetBindingConstants.THING_TYPE_BUS_ON_OFF_SWITCH;
                thingLabel = OpenWebNetBindingConstants.THING_LABEL_BUS_ON_OFF_SWITCH;
                deviceWho = Who.LIGHTING;
                break;
            }
            case SCS_DIMMER_SWITCH: {
                thingTypeUID = OpenWebNetBindingConstants.THING_TYPE_BUS_DIMMER;
                thingLabel = OpenWebNetBindingConstants.THING_LABEL_BUS_DIMMER;
                deviceWho = Who.LIGHTING;
                break;
            }
            default:
                logger.warn("Device type {} is not supported, default to generic device (WHERE={})", deviceType, where);
        }
        String tId = bridgeHandler.thingIdFromWhere(where);
        ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, tId);

        DiscoveryResult discoveryResult = null;

        String whereLabel = where;
        if (BaseOpenMessage.UNIT_02.equals(OpenMessageFactory.getUnit(where))) {
            logger.debug("UNIT=02 found (WHERE={})", where);
            logger.debug("will remove previous result if exists");
            thingRemoved(thingUID); // remove previously discovered thing
            // re-create thingUID with new type
            thingTypeUID = OpenWebNetBindingConstants.THING_TYPE_ZB_ON_OFF_SWITCH_2UNITS;
            thingLabel = OpenWebNetBindingConstants.THING_LABEL_ZB_ON_OFF_SWITCH_2UNITS;
            thingUID = new ThingUID(thingTypeUID, bridgeUID, tId);
            whereLabel = whereLabel.replace("02#", "00#"); // replace unit '02' with all unit '00'
            logger.debug("UNIT=02, switching type from {} to {}",
                    OpenWebNetBindingConstants.THING_TYPE_ZB_ON_OFF_SWITCH,
                    OpenWebNetBindingConstants.THING_TYPE_ZB_ON_OFF_SWITCH_2UNITS);
        }
        Map<String, Object> properties = new HashMap<>(2);
        properties.put(OpenWebNetBindingConstants.CONFIG_PROPERTY_WHERE, bridgeHandler.normalizeWhere(where));
        properties.put(OpenWebNetBindingConstants.PROPERTY_OWNID,
                bridgeHandler.ownIdFromWhoWhere(where, deviceWho.value().toString()));
        if (thingTypeUID == OpenWebNetBindingConstants.THING_TYPE_DEVICE && baseMsg != null) {
            // generic thing, let's specify the WHO
            thingLabel = thingLabel + " (WHO=" + baseMsg.getWho() + ", WHERE=" + whereLabel + ")";
        } else {
            thingLabel = thingLabel + " (WHERE=" + whereLabel + ")";
        }
        discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID).withProperties(properties)
                .withRepresentationProperty(OpenWebNetBindingConstants.PROPERTY_OWNID).withBridge(bridgeUID)
                .withLabel(thingLabel).build();
        thingDiscovered(discoveryResult);
    }

}
