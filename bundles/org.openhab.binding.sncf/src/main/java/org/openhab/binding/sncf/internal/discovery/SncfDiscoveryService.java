/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.sncf.internal.discovery;

import static org.openhab.binding.sncf.internal.SncfBindingConstants.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sncf.internal.SncfException;
import org.openhab.binding.sncf.internal.dto.PlaceNearby;
import org.openhab.binding.sncf.internal.handler.SncfBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.i18n.LocationProvider;
import org.openhab.core.library.types.PointType;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SncfDiscoveryService} searches for available
 * station discoverable through API
 *
 * @author Gaël L'hopital - Initial contribution
 */
@Component(service = ThingHandlerService.class)
@NonNullByDefault
public class SncfDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    private static final int SEARCH_TIME = 2;

    private final Logger logger = LoggerFactory.getLogger(SncfDiscoveryService.class);

    private @Nullable LocationProvider locationProvider;
    private @Nullable SncfBridgeHandler bridgeHandler;

    private int searchRange = 1500;

    @Activate
    public SncfDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void startScan() {
        SncfBridgeHandler handler = bridgeHandler;
        LocationProvider provider = locationProvider;
        if (provider != null && handler != null) {
            PointType location = provider.getLocation();
            if (location != null) {
                ThingUID bridgeUID = handler.getThing().getUID();
                searchRange += 500;
                try {
                    List<PlaceNearby> places = handler.discoverNearby(location, searchRange);
                    if (places != null && !places.isEmpty()) {
                        places.forEach(place -> {
                            String placeId = place.id;
                            String thingId = placeId.replace(":", "_").replace("-", "_").replace("stop_point_", "");
                            thingDiscovered(
                                    DiscoveryResultBuilder.create(new ThingUID(STATION_THING_TYPE, bridgeUID, thingId))
                                            .withLabel(place.stopPoint.name).withBridge(bridgeUID)
                                            .withRepresentationProperty(STOP_POINT_ID)
                                            .withProperty(STOP_POINT_ID, placeId).build());
                        });
                    } else {
                        logger.info("No station found in a perimeter of {} m, extending search", searchRange);
                        startScan();
                    }
                } catch (SncfException e) {
                    logger.warn("Error calling Sncf Api : {}", e.getMessage());
                }
            } else {
                logger.info("Please set a system location to enable station discovery");
            }
        }
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof SncfBridgeHandler) {
            this.bridgeHandler = (SncfBridgeHandler) handler;
            this.locationProvider = bridgeHandler.getLocationProvider();
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }
}
