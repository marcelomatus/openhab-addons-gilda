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
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.openwebnet.OpenWebNetBindingConstants;
import org.openhab.binding.openwebnet.handler.OpenWebNetBridgeHandler;
import org.openwebnet.OpenError;
import org.openwebnet.OpenGatewayZigBee;
import org.openwebnet.OpenListener;
import org.openwebnet.OpenWebNet;
import org.openwebnet.message.GatewayManagement;
import org.openwebnet.message.OpenMessage;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZigBeeGatewayDiscoveryService} is a {@link DiscoveryService} implementation responsible for discovering
 * OpenWebNet ZigBee gateways in the network.
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.openwebent")
public class ZigBeeGatewayDiscoveryService extends AbstractDiscoveryService
        implements OpenListener, ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(ZigBeeGatewayDiscoveryService.class);

    private final static int DISCOVERY_TIMEOUT = 30; // seconds

    // TODO support multiple gateways at the same time
    private @Nullable OpenGatewayZigBee zbgateway;
    private int gatewayZigBeeId = 0;
    private @Nullable OpenWebNetBridgeHandler bridgeHandler;

    private @Nullable ThingUID gatewayUID = null;

    public ZigBeeGatewayDiscoveryService() {
        super(OpenWebNetBindingConstants.BRIDGE_SUPPORTED_THING_TYPES, DISCOVERY_TIMEOUT, false);
    }

    public ZigBeeGatewayDiscoveryService(int timeout) throws IllegalArgumentException {
        super(timeout);
    }

    @Override
    protected void startScan() {
        logger.info("------ startScan() - SEARCHING for bridges...");
        startZigBeeScan();
    }

    /**
     * OWN ZigBee gw discovery
     */
    private void startZigBeeScan() {
        if (zbgateway == null) {
            logger.debug("Gateway NULL, creating a new one ...");
            zbgateway = OpenWebNet.gatewayZigBeeAsSingleton();
            zbgateway.subscribe(this);
        }
        if (!zbgateway.isConnected()) {
            logger.debug("... trying to connect gateway ...");
            zbgateway.connect();
        } else { // gw is already connected
            logger.debug("... gateway is already connected ...");
            if (gatewayZigBeeId != 0) {
                // a gw was already discovered, notify new gateway thing to inbox
                logger.debug("... gateway ZigBeeId is: {}", gatewayZigBeeId);
                notifyNewZBGatewayThing(gatewayZigBeeId);
            } else {
                logger.debug("... requesting again MACAddress ...");
                zbgateway.send(GatewayManagement.requestMACAddress());
            }
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return OpenWebNetBindingConstants.BRIDGE_SUPPORTED_THING_TYPES;
    }

    /**
     * Create and notify to Inbox a new ZigBee USB Gateway thing has been discovered
     *
     * @param gatewayZigBeeId the discovered gateway ZigBee ID
     */
    private void notifyNewZBGatewayThing(int gatewayZigBeeId) {
        ThingUID gwThingUID = new ThingUID(OpenWebNetBindingConstants.THING_TYPE_ZB_GATEWAY,
                Integer.toString(gatewayZigBeeId));
        gatewayUID = gwThingUID;
        Map<String, Object> gwProperties = new HashMap<>(3);
        gwProperties.put(OpenWebNetBindingConstants.CONFIG_PROPERTY_SERIAL_PORT, zbgateway.getConnectedPort());
        gwProperties.put(OpenWebNetBindingConstants.PROPERTY_FIRMWARE_VERSION, zbgateway.getFirmwareVersion());
        gwProperties.put(OpenWebNetBindingConstants.PROPERTY_ZIGBEEID, String.valueOf(gatewayZigBeeId));

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(gwThingUID).withProperties(gwProperties)
                .withLabel(OpenWebNetBindingConstants.THING_LABEL_ZB_GATEWAY + " (" + zbgateway.getConnectedPort()
                        + ", " + zbgateway.getFirmwareVersion() + ")")
                .withRepresentationProperty(OpenWebNetBindingConstants.PROPERTY_ZIGBEEID).build();
        logger.info("--- ZIGBEE USB GATEWAY thing discovered: {}", discoveryResult.getLabel());
        thingDiscovered(discoveryResult);
    }

    @Override
    public void onConnected() {
        logger.info("onConnected() FOUND ZIGBEE USB GATEWAY: CONNECTED port={}", zbgateway.getConnectedPort());
        gatewayZigBeeId = 0; // reset gatewayZigBeeId
        zbgateway.send(GatewayManagement.requestFirmwareVersion());
        zbgateway.send(GatewayManagement.requestMACAddress());
    }

    @Override
    public void onConnectionError(OpenError error, String errMsg) {
        if (error == OpenError.NO_SERIAL_PORTS_ERROR) {
            logger.info("No serial ports found");
        } else {
            logger.warn("onConnectionError() - CONNECTION ERROR: {} - {}", error, errMsg);
        }
        stopScan();
        // TODO handle other gw connection problems
    }

    @Override
    public void onConnectionClosed() {
        logger.debug("recevied onConnectionClosed()");
        stopScan();
    }

    @Override
    public void onDisconnected() {
        logger.warn("received onDisconnected()");
        stopScan();
    }

    @Override
    public void onReconnected() {
        logger.warn("received onReconnected()");
    }

    @Override
    public void onMessage(OpenMessage msg) {
        // TODO change this to listen to response to MACddress request session with timeout
        // and not to all messages that arrive here
        if (gatewayZigBeeId == 0) { // we do not know the discovered ZigBeeID yet, check if it was discovered with this
            // message
            int zbid = zbgateway.getZigBeeIdAsDecimal();
            if (zbid != 0) {
                // a gw was discovered, notify new gw thing to inbox
                gatewayZigBeeId = zbid;
                logger.debug("==OWN:BridgeDiscovery== GW ZigBeeID is set: {}", gatewayZigBeeId);
                notifyNewZBGatewayThing(gatewayZigBeeId);
            }
        } else {
            logger.trace("==OWN:BridgeDiscovery== onReceiveFrame() ZigBeeID != 0 : ignoring (msg={})", msg);
        }

    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof OpenWebNetBridgeHandler) {
            bridgeHandler = (OpenWebNetBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

}
