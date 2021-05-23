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
package org.openhab.binding.wolfsmartset.internal.handler;

import static org.openhab.binding.wolfsmartset.internal.WolfSmartsetBindingConstants.CONFIG_SYSTEM_ID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.wolfsmartset.internal.api.WolfSmartsetApi;
import org.openhab.binding.wolfsmartset.internal.api.WolfSmartsetCloudException;
import org.openhab.binding.wolfsmartset.internal.config.WolfSmartsetAccountConfiguration;
import org.openhab.binding.wolfsmartset.internal.discovery.WolfSmartsetAccountDiscoveryService;
import org.openhab.binding.wolfsmartset.internal.dto.*;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WolfSmartsetAccountBridgeHandler} is responsible for managing
 * communication with the WolfSmartset API.
 *
 * @author Bo Biene - Initial contribution
 */
@NonNullByDefault
public class WolfSmartsetAccountBridgeHandler extends BaseBridgeHandler {
    private static final int REFRESH_STARTUP_DELAY_SECONDS = 3;
    private static final int REFRESH_INTERVAL_SECONDS = 1;
    private static final int DEFAULT_REFRESH_INTERVAL_CONFIGURATION_MINUTES = 10;
    private static final int DEFAULT_REFRESH_INTERVAL_VALUES_SECONDS = 15;

    private final Logger logger = LoggerFactory.getLogger(WolfSmartsetAccountBridgeHandler.class);

    private final HttpClient httpClient;

    private @NonNullByDefault({}) WolfSmartsetApi api;
    private @NonNullByDefault({}) String apiKey;
    private int refreshIntervalConfigurationMinutes;
    private int refreshIntervalValuesSeconds;
    private boolean discoveryEnabled;

    private final Map<String, WolfSmartsetSystemBridgeHandler> systemHandlers = new ConcurrentHashMap<>();
    private final Set<String> systemIds = new CopyOnWriteArraySet<>();

    private @Nullable Future<?> refreshSystemsJob;
    private final AtomicInteger refreshConfigurationCounter = new AtomicInteger(REFRESH_STARTUP_DELAY_SECONDS);
    private final AtomicInteger refreshValuesCounter = new AtomicInteger(REFRESH_STARTUP_DELAY_SECONDS);

    public WolfSmartsetAccountBridgeHandler(final Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        logger.debug("AccountBridge: Initializing");

        WolfSmartsetAccountConfiguration config = getConfigAs(WolfSmartsetAccountConfiguration.class);

        Integer value;
        value = config.refreshIntervalConfiguration;
        refreshIntervalConfigurationMinutes = value == null ? DEFAULT_REFRESH_INTERVAL_CONFIGURATION_MINUTES : value;

        value = config.refreshIntervalValues;
        refreshIntervalValuesSeconds = value == null ? DEFAULT_REFRESH_INTERVAL_VALUES_SECONDS : value;

        String username = config.username;
        String password = config.password;
        username = username == null ? "" : username;
        password = password == null ? "" : password;

        Boolean booleanValue = config.discoveryEnabled;
        discoveryEnabled = booleanValue == null ? false : booleanValue.booleanValue();
        logger.debug("AccountBridge: System and unit discovery is {}", discoveryEnabled ? "enabled" : "disabled");

        try {
            api = new WolfSmartsetApi(username, password, httpClient, scheduler);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Checking authorization");
            scheduleRefreshJob();
        } catch (WolfSmartsetCloudException e) {
            logger.error("unbale to create wolf smartset api {}", e);
        }
    }

    @Override
    public void dispose() {
        cancelRefreshJob();
        logger.debug("AccountBridge: Disposing");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(WolfSmartsetAccountDiscoveryService.class);
    }

    @Override
    public void childHandlerInitialized(ThingHandler systemHandler, Thing systemThing) {
        String systemId = (String) systemThing.getConfiguration().get(CONFIG_SYSTEM_ID);
        systemHandlers.put(systemId, (WolfSmartsetSystemBridgeHandler) systemHandler);
        systemIds.add(systemId);
        scheduleRefreshJob();
        logger.debug("AccountBridge: Adding system handler for {} with id {}", systemThing.getUID(), systemId);
    }

    @Override
    public void childHandlerDisposed(ThingHandler systemHandler, Thing systemThing) {
        String systemId = (String) systemThing.getConfiguration().get(CONFIG_SYSTEM_ID);
        systemHandlers.remove(systemId);
        systemIds.remove(systemId);
        logger.debug("AccountBridge: Removing system handler for {} with id {}", systemThing.getUID(), systemId);
    }

    public boolean isBackgroundDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void updateBridgeStatus(ThingStatus status) {
        updateStatus(status);
    }

    public void updateBridgeStatus(ThingStatus status, ThingStatusDetail statusDetail, String statusMessage) {
        updateStatus(status, statusDetail, statusMessage);
    }

    public void markOnline() {
        updateStatus(ThingStatus.ONLINE);
    }

    public List<GetSystemListDTO> getRegisteredSystems() {
        return api.getSystems();
    }

    /*
     * The refresh job updates the system channels on the refresh interval set in the system thing config.
     * The system update process involves first running a system summary transaction to
     * determine if any system data has changed since the last summary. If any change is detected,
     * a full query of the systems is performed.
     */
    private void refresh() {
        refreshSystems();
    }

    private void refreshSystems() {
        try {
            if (refreshConfigurationCounter.getAndDecrement() == 0) {

                refreshConfigurationCounter.set(refreshIntervalConfigurationMinutes * 60);
                if (api.login()) {
                    logger.info("AccountBridge: refreshing configuration");
                    updateStatus(ThingStatus.ONLINE);
                    for (GetSystemListDTO system : api.getSystems()) {
                        WolfSmartsetSystemBridgeHandler handler = systemHandlers.get(system.getId().toString());
                        if (handler != null) {

                            GetGuiDescriptionForGatewayDTO systemDescription = api.getSystemDescription(system.getId(),
                                    system.getGatewayId());
                            handler.updateConfiguration(system, systemDescription);
                        }
                    }
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Authorization failed");
                }
            }

            if (refreshValuesCounter.getAndDecrement() == 0) {
                refreshValuesCounter.set(refreshIntervalValuesSeconds);
                if (api.login()) {
                    logger.info("AccountBridge: refreshing values");
                    updateStatus(ThingStatus.ONLINE);

                    var systemConfigs = systemHandlers.values().stream().map(s -> s.getSystemConfig())
                            .filter(s -> s != null).collect(Collectors.toSet());
                    if (systemConfigs != null && systemConfigs.size() > 0) {
                        var systemStates = api.getSystemState(systemConfigs);
                        if (systemStates != null) {
                            for (var systemState : systemStates) {
                                var systemHandler = systemHandlers.get(systemState.getSystemId().toString());
                                if (systemHandler != null) {
                                    systemHandler.updateSystemState(systemState);
                                }
                            }
                        } else {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "Failed to update system states");
                        }

                        for (var systemHandler : systemHandlers.values()) {
                            var systemConfig = systemHandler.getSystemConfig();

                            if (systemConfig != null) {
                                var faultMessages = api.getFaultMessages(systemConfig.getId(),
                                        systemConfig.getGatewayId());

                                systemHandler.updateFaultMessages(faultMessages);

                                for (var unitHandler : systemHandler.getUnitHandler()) {
                                    var tabmenu = unitHandler.getTabMenu();
                                    if (tabmenu != null) {
                                        var lastRefreshTime = unitHandler.getLastRefreshTime();
                                        var valueIds = tabmenu.ParameterDescriptors.stream().filter(p -> p.ValueId > 0)
                                                .map(p -> p.ValueId).collect(Collectors.toList());
                                        var paramValues = api.getGetParameterValues(systemConfig.getId(),
                                                systemConfig.getGatewayId(), tabmenu.BundleId, valueIds,
                                                lastRefreshTime);

                                        unitHandler.updateValues(paramValues);
                                    }
                                }
                            } else {
                                // waiting for config.
                                systemHandler.updateSystemState(null);
                            }
                        }

                    } else {
                        // no systems
                    }

                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Authorization failed");
                }
            }
        } catch (Throwable e) {
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.COMMUNICATION_ERROR, "Error updating things");
            logger.error("Error updating things {}", e);
        }
    }

    public void scheduleRefreshJob() {
        logger.debug("AccountBridge: Scheduling system refresh job");
        cancelRefreshJob();
        refreshConfigurationCounter.set(0);
        refreshValuesCounter.set(0);
        refreshSystemsJob = scheduler.scheduleWithFixedDelay(this::refresh, REFRESH_STARTUP_DELAY_SECONDS,
                REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelRefreshJob() {
        Future<?> localRefreshSystemsJob = refreshSystemsJob;
        if (localRefreshSystemsJob != null) {
            localRefreshSystemsJob.cancel(true);
            logger.debug("AccountBridge: Canceling system refresh job");
        }
    }
}
