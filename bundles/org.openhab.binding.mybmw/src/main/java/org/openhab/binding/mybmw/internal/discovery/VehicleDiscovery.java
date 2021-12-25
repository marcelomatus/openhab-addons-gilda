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
package org.openhab.binding.mybmw.internal.discovery;

import static org.openhab.binding.mybmw.internal.MyBMWConstants.SUPPORTED_THING_SET;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mybmw.internal.MyBMWConstants;
import org.openhab.binding.mybmw.internal.dto.vehicle.Vehicle;
import org.openhab.binding.mybmw.internal.handler.MyBMWBridgeHandler;
import org.openhab.binding.mybmw.internal.utils.Constants;
import org.openhab.binding.mybmw.internal.utils.Converter;
import org.openhab.binding.mybmw.internal.utils.VehicleStatusUtils;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VehicleDiscovery} requests data from ConnectedDrive and is identifying the Vehicles after response
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class VehicleDiscovery extends AbstractDiscoveryService implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(VehicleDiscovery.class);
    private static final int DISCOVERY_TIMEOUT = 10;
    private Optional<MyBMWBridgeHandler> bridgeHandler = Optional.empty();

    public VehicleDiscovery() {
        super(SUPPORTED_THING_SET, DISCOVERY_TIMEOUT, false);
    }

    public void onResponse(List<Vehicle> vehicleList) {
        bridgeHandler.ifPresent(bridge -> {
            final ThingUID bridgeUID = bridge.getThing().getUID();
            vehicleList.forEach(vehicle -> {
                // the DriveTrain field in the delivered json is defining the Vehicle Type
                String vehicleType = VehicleStatusUtils.vehicleType(vehicle.driveTrain, vehicle.model).toString();
                SUPPORTED_THING_SET.forEach(entry -> {
                    if (entry.getId().equals(vehicleType)) {
                        ThingUID uid = new ThingUID(entry, vehicle.vin, bridgeUID.getId());
                        Map<String, String> properties = new HashMap<>();
                        /**
                         * [todo] evaluate right properties
                         * // Dealer
                         * if (vehicle.dealer != null) {
                         * properties.put("dealer", vehicle.dealer.name);
                         * properties.put("dealerAddress", vehicle.dealer.street + " " + vehicle.dealer.country + " "
                         * + vehicle.dealer.postalCode + " " + vehicle.dealer.city);
                         * properties.put("dealerPhone", vehicle.dealer.phone);
                         * }
                         *
                         * // Services & Support
                         * properties.put("servicesActivated", getObject(vehicle, Constants.ACTIVATED));
                         * String servicesSupported = getObject(vehicle, Constants.SUPPORTED);
                         * String servicesNotSupported = getObject(vehicle, Constants.NOT_SUPPORTED);
                         * if (vehicle.statisticsAvailable) {
                         * servicesSupported += Constants.STATISTICS;
                         * } else {
                         * servicesNotSupported += Constants.STATISTICS;
                         * }
                         * properties.put(Constants.SERVICES_SUPPORTED, servicesSupported);
                         * properties.put("servicesNotSupported", servicesNotSupported);
                         * properties.put("supportBreakdownNumber", vehicle.breakdownNumber);
                         *
                         * // Vehicle Properties
                         * if (vehicle.supportedChargingModes != null) {
                         * properties.put("vehicleChargeModes",
                         * String.join(Constants.SPACE, vehicle.supportedChargingModes));
                         * }
                         * if (vehicle.hasAlarmSystem) {
                         * properties.put("vehicleAlarmSystem", "Available");
                         * } else {
                         * properties.put("vehicleAlarmSystem", "Not Available");
                         * }
                         */
                        properties.put("vehicleBrand", vehicle.brand);
                        properties.put("vehicleBodytype", vehicle.bodyType);
                        properties.put("vehicleConstructionYear", Integer.toString(vehicle.year));
                        properties.put("vehicleDriveTrain", vehicle.driveTrain);
                        properties.put("vehicleModel", vehicle.model);

                        // Update Properties for already created Things
                        bridge.getThing().getThings().forEach(vehicleThing -> {
                            Configuration c = vehicleThing.getConfiguration();
                            if (c.containsKey(MyBMWConstants.VIN)) {
                                String thingVIN = c.get(MyBMWConstants.VIN).toString();
                                if (vehicle.vin.equals(thingVIN)) {
                                    vehicleThing.setProperties(properties);
                                }
                            }
                        });

                        // Properties needed for functional Thing
                        properties.put(MyBMWConstants.VIN, vehicle.vin);
                        properties.put("brand", vehicle.brand);
                        properties.put("refreshInterval",
                                Integer.toString(MyBMWConstants.DEFAULT_REFRESH_INTERVAL_MINUTES));
                        properties.put("imageSize", Integer.toString(MyBMWConstants.DEFAULT_IMAGE_SIZE_PX));
                        properties.put("imageViewport", MyBMWConstants.DEFAULT_IMAGE_VIEWPORT);

                        String vehicleLabel = vehicle.brand + " " + vehicle.model;
                        Map<String, Object> convertedProperties = new HashMap<String, Object>(properties);
                        thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                                .withRepresentationProperty(MyBMWConstants.VIN).withLabel(vehicleLabel)
                                .withProperties(convertedProperties).build());
                    }
                });
            });
        });
    }

    /**
     * Get all field names from a DTO with a specific value
     * Used to get e.g. all services which are "ACTIVATED"
     *
     * @param DTO Object
     * @param compare String which needs to map with the value
     * @return String with all field names matching this value separated with Spaces
     */
    public String getObject(Object dto, String compare) {
        StringBuilder buf = new StringBuilder();
        for (Field field : dto.getClass().getDeclaredFields()) {
            try {
                Object value = field.get(dto);
                if (compare.equals(value)) {
                    buf.append(Converter.capitalizeFirst(field.getName()) + Constants.SPACE);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logger.debug("Field {} not found {}", compare, e.getMessage());
            }
        }
        return buf.toString();
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof MyBMWBridgeHandler) {
            bridgeHandler = Optional.of((MyBMWBridgeHandler) handler);
            bridgeHandler.get().setDiscoveryService(this);
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler.orElse(null);
    }

    @Override
    protected void startScan() {
        bridgeHandler.ifPresent(MyBMWBridgeHandler::requestVehicles);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }
}
