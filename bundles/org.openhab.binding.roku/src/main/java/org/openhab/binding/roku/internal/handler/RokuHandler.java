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
package org.openhab.binding.roku.internal.handler;

import static org.openhab.binding.roku.internal.RokuBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.roku.internal.RokuConfiguration;
import org.openhab.binding.roku.internal.RokuHttpException;
import org.openhab.binding.roku.internal.RokuStateDescriptionOptionProvider;
import org.openhab.binding.roku.internal.communication.RokuCommunicator;
import org.openhab.binding.roku.internal.dto.ActiveApp;
import org.openhab.binding.roku.internal.dto.Apps.App;
import org.openhab.binding.roku.internal.dto.DeviceInfo;
import org.openhab.binding.roku.internal.dto.Player;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RokuHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RokuHandler extends BaseThingHandler {
    private static final int DEFAULT_REFRESH_PERIOD_SEC = 10;

    private final Logger logger = LoggerFactory.getLogger(RokuHandler.class);
    private final HttpClient httpClient;
    private final RokuStateDescriptionOptionProvider stateDescriptionProvider;

    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable ScheduledFuture<?> appListJob;

    private RokuCommunicator communicator;
    private DeviceInfo deviceInfo = new DeviceInfo();
    private int refreshInterval = DEFAULT_REFRESH_PERIOD_SEC;

    private Object sequenceLock = new Object();

    public RokuHandler(Thing thing, HttpClient httpClient,
            RokuStateDescriptionOptionProvider stateDescriptionProvider) {
        super(thing);
        this.httpClient = httpClient;
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.communicator = new RokuCommunicator(httpClient, EMPTY, -1);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Roku handler");
        RokuConfiguration config = getConfigAs(RokuConfiguration.class);

        final @Nullable String host = config.hostName;

        if (host != null && !host.equals(EMPTY)) {
            this.communicator = new RokuCommunicator(httpClient, host, config.port);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Host Name must be specified");
            return;
        }

        if (config.refresh >= 10)
            refreshInterval = config.refresh;

        updateStatus(ThingStatus.UNKNOWN);

        try {
            deviceInfo = communicator.getDeviceInfo();
            thing.setProperty(PROPERTY_MODEL_NAME, deviceInfo.getModelName());
            thing.setProperty(PROPERTY_MODEL_NUMBER, deviceInfo.getModelNumber());
            thing.setProperty(PROPERTY_DEVICE_LOCAITON, deviceInfo.getUserDeviceLocation());
            thing.setProperty(PROPERTY_SERIAL_NUMBER, deviceInfo.getSerialNumber());
            thing.setProperty(PROPERTY_DEVICE_ID, deviceInfo.getDeviceId());
            thing.setProperty(PROPERTY_SOFTWARE_VERSION, deviceInfo.getSoftwareVersion());
            updateStatus(ThingStatus.ONLINE);
        } catch (RokuHttpException e) {
            logger.debug("Unable to retrieve Roku device-info. Exception: {}", e.getMessage(), e);
        }
        startAutomaticRefresh();
        startAppListRefresh();
    }

    /**
     * Start the job to periodically get status updates from the Roku
     */
    private void startAutomaticRefresh() {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob == null || refreshJob.isCancelled()) {
            Runnable runnable = () -> {
                synchronized (sequenceLock) {
                    try {
                        ActiveApp activeApp = communicator.getActiveApp();
                        updateState(ACTIVE_APP, new StringType(activeApp.getApp().getId()));
                        updateStatus(ThingStatus.ONLINE);
                    } catch (RokuHttpException e) {
                        logger.debug("Unable to retrieve Roku active-app info. Exception: {}", e.getMessage(), e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                    }

                    try {
                        Player playerInfo = communicator.getPlayerInfo();
                        // When nothing playing, 'close' is reported, replace with 'stop'
                        updateState(PLAY_MODE, new StringType(playerInfo.getState().replaceAll(CLOSE, STOP)));

                        // Remove non-numeric from string, ie: ' ms'
                        String position = playerInfo.getPosition().replaceAll(NON_DIGIT_PATTERN, EMPTY);
                        if (!EMPTY.equals(position)) {
                            updateState(TIME_ELAPSED,
                                    new QuantityType<>(Integer.parseInt(position) / 1000, API_SECONDS_UNIT));
                        } else {
                            updateState(TIME_ELAPSED, UnDefType.UNDEF);
                        }

                        String duration = playerInfo.getDuration().replaceAll(NON_DIGIT_PATTERN, EMPTY);
                        if (!EMPTY.equals(duration)) {
                            updateState(TIME_TOTAL,
                                    new QuantityType<>(Integer.parseInt(duration) / 1000, API_SECONDS_UNIT));
                        } else {
                            updateState(TIME_TOTAL, UnDefType.UNDEF);
                        }
                    } catch (RokuHttpException | NumberFormatException e) {
                        logger.debug("Unable to retrieve Roku media-player info. Exception: {}", e.getMessage(), e);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                    }
                }
            };
            this.refreshJob = scheduler.scheduleWithFixedDelay(runnable, 0, refreshInterval, TimeUnit.SECONDS);
        }
    }

    /**
     * Start the job to periodically update list of apps installed on the the Roku
     */
    private void startAppListRefresh() {
        ScheduledFuture<?> appListJob = this.appListJob;
        if (appListJob == null || appListJob.isCancelled()) {
            Runnable runnable = () -> {
                synchronized (sequenceLock) {
                    try {
                        List<App> appList = communicator.getAppList();

                        List<StateOption> appListOptions = new ArrayList<>();
                        // Roku Home will be selected in the drop-down any time an app is not running.
                        appListOptions.add(new StateOption(ROKU_HOME_ID, ROKU_HOME));

                        appList.forEach(app -> {
                            appListOptions.add(new StateOption(app.getId(), app.getValue()));
                        });

                        stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), ACTIVE_APP),
                                appListOptions);

                    } catch (RokuHttpException e) {
                        logger.debug("Unable to retrieve Roku installed app-list. Exception: {}", e.getMessage(), e);
                    }
                }
            };
            this.appListJob = scheduler.scheduleWithFixedDelay(runnable, 10, 600, TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> refreshJob = this.refreshJob;
        if (refreshJob != null) {
            refreshJob.cancel(true);
            this.refreshJob = null;
        }

        ScheduledFuture<?> appListJob = this.appListJob;
        if (appListJob != null) {
            appListJob.cancel(true);
            this.appListJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Unsupported refresh command: {}", command.toString());
        } else if (channelUID.getId().equals(BUTTON)) {
            synchronized (sequenceLock) {
                try {
                    communicator.keyPress(command.toString());
                } catch (RokuHttpException e) {
                    logger.debug("Unable to send keypress to Roku, key: {}, Exception: {}", command.toString(),
                            e.getMessage());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                }
            }
        } else if (channelUID.getId().equals(ACTIVE_APP)) {
            synchronized (sequenceLock) {
                try {
                    String appId = command.toString();
                    // Roku Home(-1) is not a real appId, just press the home button instead
                    if (!ROKU_HOME_ID.equals(appId)) {
                        communicator.launchApp(appId);
                    } else {
                        communicator.keyPress(ROKU_HOME_BUTTON);
                    }
                } catch (RokuHttpException e) {
                    logger.debug("Unable to launch app on Roku, appId: {}, Exception: {}", command.toString(),
                            e.getMessage());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                }
            }
        } else {
            logger.debug("Unsupported command: {}", command.toString());
        }
    }
}
