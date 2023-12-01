/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.opensmartcity.internal.discovery;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.opensmartcity.internal.OpenSmartCityCityHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.LocationProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.library.types.PointType;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenSmartCityWeatherDiscoveryService} creates things based on the configured location.
 *
 * @author Kai Kreuzer - Initial Contribution
 */
@NonNullByDefault
public class OpenSmartCityWeatherDiscoveryService extends AbstractDiscoveryService {
    private static final int DISCOVERY_TIMEOUT_SECONDS = 2;
    private static final int DISCOVERY_INTERVAL_SECONDS = 60;
    private @Nullable ScheduledFuture<?> discoveryJob;
    private final LocationProvider locationProvider;
    private @Nullable PointType previousLocation;

    private final Logger logger = LoggerFactory.getLogger(OpenSmartCityWeatherDiscoveryService.class);

    private final OpenSmartCityCityHandler bridgeHandler;

    /**
     * Creates an OpenSmartCityWeatherDiscoveryService.
     */
    public OpenSmartCityWeatherDiscoveryService(OpenSmartCityCityHandler bridgeHandler,
            LocationProvider locationProvider, LocaleProvider localeProvider, TranslationProvider i18nProvider) {
        super(OpenSmartCityCityHandler.SUPPORTED_THING_TYPES, DISCOVERY_TIMEOUT_SECONDS);
        this.bridgeHandler = bridgeHandler;
        this.locationProvider = locationProvider;
        this.localeProvider = localeProvider;
        this.i18nProvider = i18nProvider;
        activate(null);
    }

    @Override
    protected void activate(@Nullable Map<String, Object> configProperties) {
        super.activate(configProperties);
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime(), bridgeHandler.getThing().getUID());
        super.deactivate();
    }

    @Override
    protected void startScan() {
        logger.debug("Start manual location discovery scan.");
        scanForNewLocation(false);
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("Stop manual location discovery scan.");
        super.stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob == null || localDiscoveryJob.isCancelled()) {
            logger.debug("Start location background discovery job at interval {} s.", DISCOVERY_INTERVAL_SECONDS);
            localDiscoveryJob = scheduler.scheduleWithFixedDelay(() -> {
                scanForNewLocation(true);
            }, 0, DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob != null && !localDiscoveryJob.isCancelled()) {
            logger.debug("Stop location background discovery job.");
            if (localDiscoveryJob.cancel(true)) {
                discoveryJob = null;
            }
        }
    }

    private void scanForNewLocation(boolean updateOnlyIfNewLocation) {
        PointType currentLocation = locationProvider.getLocation();
        if (currentLocation == null) {
            logger.debug("Location is not set -> Will not provide any discovery results.");
        } else if (!Objects.equals(currentLocation, previousLocation)) {
            logger.debug("Location has been changed from {} to {} -> Creating new discovery results.", previousLocation,
                    currentLocation);
            createResults(currentLocation);
            previousLocation = currentLocation;
        } else if (!updateOnlyIfNewLocation) {
            createResults(currentLocation);
        }
    }

    private void createResults(PointType location) {
        String locationString = location.toFullString();
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        createWeatherResult(locationString, bridgeUID);
    }

    private void createWeatherResult(String location, ThingUID bridgeUID) {
        thingDiscovered(DiscoveryResultBuilder
                .create(new ThingUID(
                        org.openhab.binding.opensmartcity.internal.OpenSmartCityBindingConstants.THING_TYPE_WEATHER,
                        bridgeUID, org.openhab.binding.opensmartcity.internal.OpenSmartCityBindingConstants.LOCAL))
                .withLabel("Local Weather Data")
                .withProperty(org.openhab.binding.opensmartcity.internal.OpenSmartCityBindingConstants.CONFIG_LOCATION,
                        location)
                .withRepresentationProperty(
                        org.openhab.binding.opensmartcity.internal.OpenSmartCityBindingConstants.CONFIG_LOCATION)
                .withBridge(bridgeUID).build());
    }
}
