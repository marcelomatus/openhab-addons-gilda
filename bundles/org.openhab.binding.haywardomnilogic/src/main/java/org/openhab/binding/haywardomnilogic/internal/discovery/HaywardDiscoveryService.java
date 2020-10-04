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
package org.openhab.binding.haywardomnilogic.internal.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.haywardomnilogic.internal.HaywardBindingConstants;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardBridgeHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardHandlerListener;
import org.openhab.binding.haywardomnilogic.internal.hayward.HaywardTypeToRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up the discovery results and details
 *
 * @author Matt Myers - Initial Contribution
 */

public class HaywardDiscoveryService extends AbstractDiscoveryService implements HaywardHandlerListener {
    private final Logger logger = LoggerFactory.getLogger(HaywardDiscoveryService.class);

    private HaywardBridgeHandler bridge;

    public HaywardDiscoveryService(HaywardBridgeHandler bridge) throws IllegalArgumentException {
        super(60);
        this.bridge = bridge;
    }

    @Override
    public void activate(Map<String, Object> configProperties) {
        super.activate(configProperties);
        this.bridge.addListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.bridge.removeListener(this);
    }

    @Override
    protected void startScan() {
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    public void onBackyardDiscovered(int systemID, String label) {
        logger.debug("Hayward Backyard {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_BACKYARD, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.BACKYARD);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onBOWDiscovered(int systemID, String label) {
        logger.debug("Hayward BOW {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_BOW, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.BOW);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onChlorinatorDiscovered(int systemID, String label, String bowID, String bowName) {
        logger.debug("Hayward Chlorinator {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_CHLORINATOR, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.CHLORINATOR);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onColorLogicDiscovered(int systemID, String label, String bowID, String bowName) {
        logger.debug("Hayward Color Logic Light {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_COLORLOGIC, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.COLORLOGIC);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onFilterDiscovered(int systemID, String label, String bowID, String bowName, String minPumpSpeed,
            String maxPumpSpeed, String minPumpRpm, String maxPumpRpm) {
        logger.debug("Hayward Filter {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_FILTER, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.FILTER);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        properties.put(HaywardBindingConstants.PROPERTY_FILTER_MINPUMPSPEED, minPumpSpeed);
        properties.put(HaywardBindingConstants.PROPERTY_FILTER_MAXPUMPSPEED, maxPumpSpeed);
        properties.put(HaywardBindingConstants.PROPERTY_FILTER_MINPUMPRPM, minPumpRpm);
        properties.put(HaywardBindingConstants.PROPERTY_FILTER_MAXPUMPRPM, maxPumpRpm);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onHeaterDiscovered(int systemID, String label, String bowID, String bowName) {
        logger.debug("Hayward Heater {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_HEATER, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.HEATER);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onPumpDiscovered(int systemID, String label, String bowID, String bowName, String property1,
            String property2, String property3, String property4) {
        logger.debug("Hayward Pump {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_PUMP, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.PUMP);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        properties.put(HaywardBindingConstants.PROPERTY_PUMP_MINPUMPSPEED, property1);
        properties.put(HaywardBindingConstants.PROPERTY_PUMP_MAXPUMPSPEED, property2);
        properties.put(HaywardBindingConstants.PROPERTY_PUMP_MINPUMPRPM, property3);
        properties.put(HaywardBindingConstants.PROPERTY_PUMP_MAXPUMPRPM, property4);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onRelayDiscovered(int systemID, String label, String bowID, String bowName) {
        logger.debug("Hayward Relay {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_RELAY, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.RELAY);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onSensorDiscovered(int systemID, String label, String bowID, String bowName) {
        logger.debug("Hayward Sensor {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_SENSOR, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.SENSOR);
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }

    @Override
    public void onVirtualHeaterDiscovered(int systemID, String label, String bowID, String bowName) {
        logger.debug("Hayward Virtual Heater {} Discovered: {}", systemID, label);
        ThingUID thingUID = new ThingUID(HaywardBindingConstants.THING_TYPE_VIRTUALHEATER, bridge.getThing().getUID(),
                Integer.toString(systemID));
        Map<String, Object> properties = new HashMap<>();
        properties.put(HaywardBindingConstants.PROPERTY_SYSTEM_ID, systemID);
        properties.put(HaywardBindingConstants.PROPERTY_TYPE, HaywardTypeToRequest.VIRTUALHEATER.toString());
        properties.put(HaywardBindingConstants.PROPERTY_BOWID, bowID);
        properties.put(HaywardBindingConstants.PROPERTY_BOWNAME, bowName);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getThing().getUID())
                .withRepresentationProperty(HaywardBindingConstants.PROPERTY_SYSTEM_ID).withLabel("Hayward " + label)
                .withProperties(properties).build();
        thingDiscovered(result);
    }
}
