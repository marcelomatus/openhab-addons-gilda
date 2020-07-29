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
package org.openhab.binding.automower.internal.things;

import static org.openhab.binding.automower.internal.AutomowerBindingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.joda.time.DateTime;
import org.openhab.binding.automower.internal.AutomowerBindingConstants;
import org.openhab.binding.automower.internal.actions.AutomowerActions;
import org.openhab.binding.automower.internal.bridge.AutomowerBridge;
import org.openhab.binding.automower.internal.bridge.AutomowerBridgeHandler;
import org.openhab.binding.automower.internal.rest.api.automowerconnect.dto.Mower;
import org.openhab.binding.automower.internal.rest.exceptions.AutomowerCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AutomowerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Markus Pfleger - Initial contribution
 */
@NonNullByDefault
public class AutomowerHandler extends BaseThingHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_AUTOMOWER);
    private static final String NO_ID = "NO_ID";
    private static final long DEFAULT_COMMAND_DURATION_MIN = 60;
    private static final long DEFAULT_POLLING_INTERVAL_S = TimeUnit.MINUTES.toSeconds(10);

    private final Logger logger = LoggerFactory.getLogger(AutomowerHandler.class);
    private AtomicReference<String> automowerId = new AtomicReference<String>(NO_ID);
    private long lastQueryTimeMs = 0L;

    private @Nullable ScheduledFuture<?> automowerPollingJob;
    private long maxQueryFrequencyNanos = TimeUnit.MINUTES.toNanos(1);

    public AutomowerHandler(Thing thing) {
        super(thing);
    }

    class AutomowerPollingRunnable implements Runnable {

        @Override
        public void run() {
            Bridge bridge = getBridge();
            if (bridge != null && bridge.getStatus() == ThingStatus.ONLINE) {
                updateAutomowerState();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannels(channelUID);
        }
    }

    private void refreshChannels(ChannelUID channelUID) {
        updateAutomowerState();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(AutomowerActions.class);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            AutomowerConfiguration currentConfig = getConfigAs(AutomowerConfiguration.class);
            final String configMowerId = currentConfig.getMowerId();
            final Integer pollingIntervalS = currentConfig.getPollingInterval();

            if (configMowerId == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "@text/conf-error-no-mower-id");
            } else if (pollingIntervalS != null && pollingIntervalS < 1) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "@text/conf-error-invalid-polling-interval");
            } else {
                automowerId.set(configMowerId);
                startAutomowerPolling(pollingIntervalS);
            }

        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        }
    }

    @Nullable
    private AutomowerBridge getAutomowerBridge() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof AutomowerBridgeHandler) {
                AutomowerBridgeHandler bridgeHandler = (AutomowerBridgeHandler) handler;
                return bridgeHandler.getAutomowerBridge();
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        if (!automowerId.get().equals(NO_ID)) {
            stopAutomowerPolling();
            automowerId.set(NO_ID);
        }
    }

    private void startAutomowerPolling(@Nullable Integer pollingIntervalS) {
        if (automowerPollingJob == null) {
            final long pollingIntervalToUse = pollingIntervalS == null ? DEFAULT_POLLING_INTERVAL_S : pollingIntervalS;
            automowerPollingJob = scheduler.scheduleWithFixedDelay(new AutomowerPollingRunnable(), 1,
                    pollingIntervalToUse, TimeUnit.SECONDS);
        }
    }

    private void stopAutomowerPolling() {
        if (automowerPollingJob != null) {
            automowerPollingJob.cancel(true);
            automowerPollingJob = null;
        }
    }

    private boolean isValidResult(Mower mower) {
        return mower.getAttributes() != null && mower.getAttributes().getMetadata() != null
                && mower.getAttributes().getBattery() != null && mower.getAttributes().getSystem() != null;
    }

    private boolean isConnected(Mower mower) {
        return mower.getAttributes() != null && mower.getAttributes().getMetadata() != null
                && mower.getAttributes().getMetadata().isConnected();
    }

    private synchronized void updateAutomowerState() {
        if (System.nanoTime() - lastQueryTimeMs > maxQueryFrequencyNanos) {
            lastQueryTimeMs = System.nanoTime();
            String id = automowerId.get();
            try {
                AutomowerBridge automowerBridge = getAutomowerBridge();
                if (automowerBridge != null) {
                    Mower mower = automowerBridge.getAutomowerStatus(id);

                    if (isValidResult(mower)) {
                        initializeProperties(mower);

                        updateChannelState(mower);

                        if (isConnected(mower)) {
                            updateStatus(ThingStatus.ONLINE);
                        } else {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "@text/comm-error-mower-not-connected-to-cloud");
                        }
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "@text/comm-error-query-mower-failed");
                    }
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "@text/conf-error-no-bridge");
                }
            } catch (AutomowerCommunicationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "@text/comm-error-query-mower-failed");
                logger.warn("Unable to query automower status for:  {}. Error: {}", id, e.getMessage());
            }
        }
    }

    /**
     * Sends a command to the automower with the default duration of 60min
     *
     * @param command The command that should be sent. Valid values are: "Start", "ResumeSchedule", "Pause", "Park",
     *            "ParkUntilNextSchedule", "ParkUntilFurtherNotice"
     *
     * @param command
     */
    public void sendAutomowerCommand(AutomowerCommand command) {
        sendAutomowerCommand(command, DEFAULT_COMMAND_DURATION_MIN);
    }

    /**
     * Sends a command to the automower with the given duration
     *
     * @param command The command that should be sent. Valid values are: "Start", "ResumeSchedule", "Pause", "Park",
     *            "ParkUntilNextSchedule", "ParkUntilFurtherNotice"
     * @param commandDurationMinutes The duration of the command in minutes. This is only evaluated for "Start" and
     *            "Park" commands
     */
    public void sendAutomowerCommand(AutomowerCommand command, long commandDurationMinutes) {
        String id = automowerId.get();
        try {
            AutomowerBridge automowerBridge = getAutomowerBridge();
            if (automowerBridge != null) {
                automowerBridge.sendAutomowerCommand(id, command, commandDurationMinutes);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "@text/conf-error-no-bridge");
            }
        } catch (AutomowerCommunicationException e) {
            logger.warn("Unable to send command to automower: {}, Error: {}", id, e.getMessage());
        }

        updateAutomowerState();
    }

    private void updateChannelState(Mower mower) {
        if (isValidResult(mower)) {
            updateState(CHANNEL_MOWER_NAME, new StringType(mower.getAttributes().getSystem().getName()));

            updateState(CHANNEL_STATUS_MODE, new StringType(mower.getAttributes().getMower().getMode().name()));
            updateState(CHANNEL_STATUS_ACTIVITY, new StringType(mower.getAttributes().getMower().getActivity().name()));
            updateState(CHANNEL_STATUS_STATE, new StringType(mower.getAttributes().getMower().getState().name()));

            updateState(CHANNEL_STATUS_LAST_UPDATE, new DateTimeType(
                    new DateTime(mower.getAttributes().getMetadata().getStatusTimestamp()).toString()));
            updateState(CHANNEL_STATUS_BATTERY,
                    new DecimalType(mower.getAttributes().getBattery().getBatteryPercent()));

            updateState(CHANNEL_STATUS_ERROR_CODE, new DecimalType(mower.getAttributes().getMower().getErrorCode()));
            updateState(CHANNEL_STATUS_ERROR_TIMESTAMP, new DateTimeType(
                    new DateTime(mower.getAttributes().getMower().getErrorCodeTimestamp()).toString()));

        }
    }

    private void initializeProperties(Mower mower) {
        Map<String, String> properties = editProperties();

        properties.put(AutomowerBindingConstants.AUTOMOWER_ID, mower.getId());

        if (mower.getAttributes() != null && mower.getAttributes().getSystem() != null) {
            properties.put(AutomowerBindingConstants.AUTOMOWER_SERIAL_NUMBER,
                    mower.getAttributes().getSystem().getSerialNumber());
            properties.put(AutomowerBindingConstants.AUTOMOWER_MODEL, mower.getAttributes().getSystem().getModel());
            properties.put(AutomowerBindingConstants.AUTOMOWER_NAME, mower.getAttributes().getSystem().getName());
        }

        updateProperties(properties);
    }
}
