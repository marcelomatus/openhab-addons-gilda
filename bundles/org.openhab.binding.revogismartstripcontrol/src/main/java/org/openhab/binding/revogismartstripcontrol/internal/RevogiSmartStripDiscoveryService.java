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
package org.openhab.binding.revogismartstripcontrol.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.revogismartstripcontrol.internal.api.DiscoveryResponse;
import org.openhab.binding.revogismartstripcontrol.internal.api.DiscoveryService;
import org.openhab.binding.revogismartstripcontrol.internal.udp.UdpSenderService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openhab.binding.revogismartstripcontrol.internal.RevogiSmartStripControlBindingConstants.SMART_STRIP_THING_TYPE;

@NonNullByDefault
public class RevogiSmartStripDiscoveryService extends AbstractDiscoveryService {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<>(Collections.singleton(SMART_STRIP_THING_TYPE));

    private final DiscoveryService discoveryService;
    private final RevogiSmartStripControlHandler revogiSmartStripControlHandler;

    private static final int SEARCH_TIME = 10;

    public RevogiSmartStripDiscoveryService(UdpSenderService udpSenderService, RevogiSmartStripControlHandler revogiSmartStripControlHandler) throws IllegalArgumentException {
        super(SEARCH_TIME);
        discoveryService = new DiscoveryService(udpSenderService);
        this.revogiSmartStripControlHandler = revogiSmartStripControlHandler;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES;
    }

    @Override
    protected void startScan() {
        List<DiscoveryResponse> discoveryResponses = discoveryService.discoverSmartStrips();
        discoveryResponses.forEach(response -> {
            ThingUID thingUID = getThingUID(response);

                }
        );
    }

    private @Nullable ThingUID getThingUID(DiscoveryResponse response) {
        ThingTypeUID thingTypeUID = revogiSmartStripControlHandler.getThing().getThingTypeUID();

        if (thingTypeUID != null && getSupportedThingTypes().contains(thingTypeUID)) {
            return new ThingUID(thingTypeUID, response.getSerialNumber());
        } else {
            return null;
        }
    }
}
