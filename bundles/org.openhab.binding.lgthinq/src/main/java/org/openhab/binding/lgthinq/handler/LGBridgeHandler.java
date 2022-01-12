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
package org.openhab.binding.lgthinq.handler;

import static org.openhab.binding.lgthinq.internal.LGThinqBindingConstants.THINQ_USER_DATA_FOLDER;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lgthinq.api.TokenManager;
import org.openhab.binding.lgthinq.errors.LGThinqException;
import org.openhab.binding.lgthinq.errors.RefreshTokenException;
import org.openhab.binding.lgthinq.internal.LGDeviceThing;
import org.openhab.binding.lgthinq.internal.LGThinqBindingConstants;
import org.openhab.binding.lgthinq.internal.LGThinqConfiguration;
import org.openhab.binding.lgthinq.internal.discovery.LGThinqDiscoveryService;
import org.openhab.binding.lgthinq.lgapi.LGApiClientService;
import org.openhab.binding.lgthinq.lgapi.LGApiV1ClientServiceImpl;
import org.openhab.binding.lgthinq.lgapi.model.LGDevice;
import org.openhab.core.config.core.status.ConfigStatusMessage;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.ConfigStatusBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LGBridgeHandler}
 *
 * @author Nemer Daud - Initial contribution
 */
public class LGBridgeHandler extends ConfigStatusBridgeHandler implements LGBridge {
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(LGThinqBindingConstants.BINDING_ID, "bridge");

    private Map<String, LGDeviceThing> lGDeviceRegister = new ConcurrentHashMap<>();
    private Map<String, LGDevice> lastDevicesDiscovered = new ConcurrentHashMap<>();

    static {
        var logger = LoggerFactory.getLogger(LGBridgeHandler.class);
        try {
            File directory = new File(THINQ_USER_DATA_FOLDER);
            if (!directory.exists()) {
                directory.mkdir();
            }
        } catch (Exception e) {
            logger.warn("Unable to setup thinq userdata directory: {}", e.getMessage());
        }
    }
    private final Logger logger = LoggerFactory.getLogger(LGBridgeHandler.class);
    private LGThinqConfiguration lgthinqConfig;
    private TokenManager tokenManager;
    private LGThinqDiscoveryService discoveryService;
    private LGApiClientService lgApiClient;
    private @Nullable Future<?> initJob;
    private @Nullable ScheduledFuture<?> devicePollingJob;

    public LGBridgeHandler(Bridge bridge) {
        super(bridge);
        tokenManager = TokenManager.getInstance();
        lgApiClient = LGApiV1ClientServiceImpl.getInstance();
    }

    final ReentrantLock pollingLock = new ReentrantLock();

    /**
     * Abstract Runnable Pooling Class to schedule sincronization status of the Bridge Thing Kinds !
     */
    abstract class PollingRunnable implements Runnable {
        @Override
        public void run() {
            try {
                pollingLock.lock();
                // check if configuration file already exists
                if (tokenManager.isOauthTokenRegistered()) {
                    logger.debug(
                            "Token authentication process has been already done. Skip first authentication process.");
                    try {
                        // Dummy - if token is expired, then provide the refresh
                        tokenManager.getValidRegisteredToken();
                    } catch (IOException e) {
                        logger.error("Error reading LGThinq TokenFile", e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                                "@text/error.toke-file-corrupted");
                        return;
                    } catch (RefreshTokenException e) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                                "@text/error.toke-refresh");
                        return;
                    }
                } else {
                    try {
                        tokenManager.oauthFirstRegistration(lgthinqConfig.getLanguage(), lgthinqConfig.getCountry(),
                                lgthinqConfig.getUsername(), lgthinqConfig.getPassword());
                        if (tokenManager.getValidRegisteredToken() != null) {

                        }
                    } catch (IOException e) {
                        logger.debug(
                                "I/O error accessing json token configuration file. Updating Bridge Status to OFFLINE.",
                                e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                "@text/error.toke-file-access-error");
                        return;
                    } catch (LGThinqException e) {
                        logger.debug("Error accessing LG API. Updating Bridge Status to OFFLINE.", e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "@text/error.lgapi-communication-error");
                        return;
                    }
                }
                if (thing.getStatus() != ThingStatus.ONLINE) {
                    updateStatus(ThingStatus.ONLINE);
                }

                try {
                    doConnectedRun();
                } catch (Exception e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "@text/error.lgapi-getting-devices");
                }

            } finally {
                pollingLock.unlock();
            }
        }

        protected abstract void doConnectedRun() throws IOException, LGThinqException;
    }

    @Override
    public boolean registerDiscoveryListener(LGThinqDiscoveryService listener) {
        if (discoveryService == null) {
            discoveryService = listener;
            return true;
        }
        return false;
    }

    /**
     * Registry the OSGi services used by this Bridge.
     * Eventually, the Discovery Service will be activated with this bridge as argument.
     * 
     * @return Services to be registered to OSGi.
     */
    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LGThinqDiscoveryService.class);
    }

    @Override
    public void registryListenerThing(LGDeviceThing thing) {
        if (lGDeviceRegister.get(thing.getDeviceId()) == null) {
            lGDeviceRegister.put(thing.getDeviceId(), thing);
            // remove device from discovery list, if exists.
            LGDevice device = lastDevicesDiscovered.get(thing.getDeviceId());
            if (device != null) {
                discoveryService.removeLgDeviceDiscovery(device);
            }
        }
    }

    @Override
    public void unRegistryListenerThing(LGDeviceThing thing) {
        lGDeviceRegister.remove(thing.getDeviceId());
    }

    @Override
    public LGDeviceThing getThingByDeviceId(String deviceId) {
        return lGDeviceRegister.get(deviceId);
    }

    private final Runnable lgDevicePollingRunnable = new PollingRunnable() {
        @Override
        protected void doConnectedRun() throws LGThinqException {
            Map<String, LGDevice> lastDevicesDiscoveredCopy = new HashMap<>(lastDevicesDiscovered);
            for (final LGDevice device : lgApiClient.listAccountDevices()) {
                String deviceId = device.getDeviceId();

                if (lGDeviceRegister.get(deviceId) == null) {
                    logger.debug("Adding new LG Device to things registry with id:{}", deviceId);
                    if (discoveryService != null) {
                        discoveryService.addLgDeviceDiscovery(device);
                    }
                }
                lastDevicesDiscovered.put(deviceId, device);
                lastDevicesDiscoveredCopy.remove(deviceId);
            }
            // the rest in lastDevicesDiscoveredCopy is not more registered in LG API. Remove from discovery
            lastDevicesDiscoveredCopy.forEach((deviceId, device) -> {
                logger.trace("LG Device '{}' removed.", deviceId);
                lastDevicesDiscovered.remove(deviceId);

                LGDeviceThing deviceThing = lGDeviceRegister.get(deviceId);
                if (deviceThing != null) {
                    deviceThing.onDeviceRemoved();
                }
                if (discoveryService != null && deviceThing != null) {
                    discoveryService.removeLgDeviceDiscovery(device);
                }
            });
        }
    };

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        List<ConfigStatusMessage> resultList = new ArrayList<>();
        if (lgthinqConfig.username.isEmpty()) {
            resultList.add(ConfigStatusMessage.Builder.error("USERNAME").withMessageKeySuffix("missing field")
                    .withArguments("username").build());
        }
        if (lgthinqConfig.password.isEmpty()) {
            resultList.add(ConfigStatusMessage.Builder.error("PASSWORD").withMessageKeySuffix("missing field")
                    .withArguments("password").build());
        }
        if (lgthinqConfig.language.isEmpty()) {
            resultList.add(ConfigStatusMessage.Builder.error("LANGUAGE").withMessageKeySuffix("missing field")
                    .withArguments("language").build());
        }
        if (lgthinqConfig.country.isEmpty()) {
            resultList.add(ConfigStatusMessage.Builder.error("COUNTRY").withMessageKeySuffix("missing field")
                    .withArguments("country").build());

        }
        return resultList;
    }

    @Override
    public void handleRemoval() {
        if (devicePollingJob != null)
            devicePollingJob.cancel(true);
        tokenManager.cleanupTokenRegistry();
        super.handleRemoval();
    }

    @Override
    public void dispose() {
        if (devicePollingJob != null) {
            devicePollingJob.cancel(true);
            devicePollingJob = null;
        }
    }

    @Override
    public <T> T getConfigAs(Class<T> configurationClass) {
        return super.getConfigAs(configurationClass);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing LGThinq bridge handler.");
        lgthinqConfig = getConfigAs(LGThinqConfiguration.class);

        if (lgthinqConfig.username.isEmpty() || lgthinqConfig.password.isEmpty() || lgthinqConfig.language.isEmpty()
                || lgthinqConfig.country.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/error.mandotory-fields-missing");
        } else {
            updateStatus(ThingStatus.UNKNOWN);
            startLGDevicePolling();
        }
    }

    private void startLGDevicePolling() {
        if (devicePollingJob == null || devicePollingJob.isDone()) {
            long pollingInterval;
            int configPollingInterval = lgthinqConfig.getPoolingIntervalSec();
            // It's not recommended to pool for resources in LG API short intervals to do not enter in BlackList
            if (configPollingInterval < 300) {
                pollingInterval = TimeUnit.SECONDS.toSeconds(300);
                logger.info("Wrong configuration value for polling interval. Using default value: {}s",
                        pollingInterval);
            } else {
                pollingInterval = configPollingInterval;
            }
            // Delay the first execution to give a chance to have all light and group things registered
            devicePollingJob = scheduler.scheduleWithFixedDelay(lgDevicePollingRunnable, 10, pollingInterval,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
}
