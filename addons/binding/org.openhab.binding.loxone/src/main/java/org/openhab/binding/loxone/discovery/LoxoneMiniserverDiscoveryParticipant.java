/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.loxone.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.binding.loxone.LoxoneBindingConstants;
import org.openhab.binding.loxone.handler.LoxoneMiniserverHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LoxoneMiniserverDiscoveryParticipant} class creates Miniserver things.
 * It analyzes UPNP devices discovered by the framework and if Loxone Miniserver is found,
 * a new thing discovery is reported, which in turn will result in creating a {@link Thing}
 * and subsequently a new {@link LoxoneMiniserverHandler} object.
 *
 * @author Pawel Pieczul - Initial contribution
 *
 */
public class LoxoneMiniserverDiscoveryParticipant implements UpnpDiscoveryParticipant {

    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(LoxoneMiniserverDiscoveryParticipant.class);

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return LoxoneMiniserverHandler.SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public DiscoveryResult createResult(RemoteDevice device) {
        ThingUID uid = getThingUID(device);
        if (uid != null) {
            Map<String, Object> properties = new HashMap<>(2);

            DeviceDetails details = device.getDetails();
            String serial = details.getSerialNumber();
            String host = details.getPresentationURI().getHost();
            String label = details.getFriendlyName() + " @ " + host;
            int port = details.getPresentationURI().getPort();

            properties.put(LoxoneBindingConstants.MINISERVER_PARAM_HOST, host);
            properties.put(LoxoneBindingConstants.MINISERVER_PARAM_PORT, port);

            return DiscoveryResultBuilder.create(uid).withProperties(properties).withLabel(label)
                    .withRepresentationProperty(serial).build();
        }
        return null;
    }

    @Override
    public ThingUID getThingUID(RemoteDevice device) {
        if (device != null) {
            String manufacturer = device.getDetails().getManufacturerDetails().getManufacturer();
            if (manufacturer != null && manufacturer.toLowerCase().contains("loxone")) {
                String model = device.getDetails().getModelDetails().getModelName();
                if (model != null && model.toLowerCase().contentEquals("loxone miniserver")) {
                    String serial = device.getDetails().getSerialNumber();
                    if (serial == null) {
                        serial = device.getIdentity().getUdn().getIdentifierString();
                    }
                    if (serial != null) {
                        return new ThingUID(LoxoneBindingConstants.THING_TYPE_MINISERVER, serial);
                    }
                }
            }
        }
        return null;
    }
}
