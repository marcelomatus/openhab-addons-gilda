/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ikeatradfri.internal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant;
import org.openhab.binding.ikeatradfri.IkeaTradfriBindingConstants;
import org.openhab.binding.ikeatradfri.IkeaTradfriGatewayConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IkeaTradfriGatewayDiscoveryParticipant} is responsible for discovering
 * the IKEA Tradfri gateway
 *
 * @author Daniel Sundberg - Initial contribution
 * @author Kai Kreuzer - refactorings
 */

public class IkeaTradfriGatewayDiscoveryParticipant implements MDNSDiscoveryParticipant {
    private Logger logger = LoggerFactory.getLogger(IkeaTradfriGatewayDiscoveryParticipant.class);

    private static final String SERVICE_TYPE = "_coap._udp.local.";

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return IkeaTradfriBindingConstants.SUPPORTED_GATEWAY_TYPES_UIDS;
    }

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    @Override
    public ThingUID getThingUID(ServiceInfo service) {
        if (service != null) {
            String name = service.getName();
            if ((service.getType() != null) && service.getType().equals(getServiceType())
                    && (name.matches("gw:([a-f0-9]{2}[-]?){6}"))) {
                return new ThingUID(IkeaTradfriBindingConstants.THING_TYPE_GATEWAY,
                        name.replaceAll("[^A-Za-z0-9_]", ""));
            }
        }
        return null;
    }

    @Override
    public DiscoveryResult createResult(ServiceInfo service) {
        logger.debug("IKEA Tradfri Gateway discovery result: {}", service.toString());
        DiscoveryResult result = null;
        String ip = null;

        if (service.getHostAddresses() != null && service.getHostAddresses().length > 0
                && !service.getHostAddresses()[0].isEmpty()) {
            ip = service.getHostAddresses()[0];
        }

        ThingUID thingUID = getThingUID(service);
        if (thingUID != null && ip != null) {
            logger.debug("Created a DiscoveryResult for Ikea Trådfri Gateway {} on IP {}", thingUID, ip);

            Enumeration<String> props = service.getPropertyNames();
            Map<String, Object> properties = new HashMap<>(1);
            props = service.getPropertyNames();
            while (props.hasMoreElements()) {
                String s = props.nextElement();
                properties.put(s, service.getPropertyString(s));
            }

            properties.put(IkeaTradfriGatewayConfiguration.HOST, ip);
            properties.put(IkeaTradfriGatewayConfiguration.PORT, service.getPort());
            result = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withLabel("IKEA Trådfri Gateway").withRepresentationProperty(IkeaTradfriGatewayConfiguration.HOST)
                    .build();
        }
        return result;
    }
}
