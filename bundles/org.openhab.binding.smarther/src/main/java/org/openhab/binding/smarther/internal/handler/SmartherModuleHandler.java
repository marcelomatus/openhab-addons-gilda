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
package org.openhab.binding.smarther.internal.handler;

import static org.openhab.binding.smarther.internal.SmartherBindingConstants.*;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.scheduler.CronScheduler;
import org.eclipse.smarthome.core.scheduler.ScheduledCompletableFuture;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.smarther.internal.api.dto.Chronothermostat;
import org.openhab.binding.smarther.internal.api.dto.Enums.BoostTime;
import org.openhab.binding.smarther.internal.api.dto.Enums.Mode;
import org.openhab.binding.smarther.internal.api.dto.ModuleStatus;
import org.openhab.binding.smarther.internal.api.dto.Notification;
import org.openhab.binding.smarther.internal.api.dto.Program;
import org.openhab.binding.smarther.internal.api.exception.SmartherGatewayException;
import org.openhab.binding.smarther.internal.api.exception.SmartherSubscriptionAlreadyExistsException;
import org.openhab.binding.smarther.internal.config.SmartherModuleConfiguration;
import org.openhab.binding.smarther.internal.model.ModuleSettings;
import org.openhab.binding.smarther.internal.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code SmartherModuleHandler} class is responsible of a single Smarther Chronothermostat, handling the commands
 * that are sent to one of its channels.
 * Each Smarther Chronothermostat communicates with the Smarther API via its assigned {@code SmartherBridgeHandler}.
 *
 * @author Fabio Possieri - Initial contribution
 */
@NonNullByDefault
public class SmartherModuleHandler extends BaseThingHandler {

    private static final String DAILY_MIDNIGHT = "1 0 0 * * ? *";
    private static final long POLL_INITIAL_DELAY = 5;

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CronScheduler cronScheduler;
    private final SmartherDynamicStateDescriptionProvider dynamicStateDescriptionProvider;
    private final ChannelUID programChannelUID;
    private final ChannelUID endDateChannelUID;

    // Field members assigned in initialize method
    private @NonNullByDefault({}) ScheduledCompletableFuture<Void> jobFuture;
    private @NonNullByDefault({}) Future<?> pollFuture;
    private @NonNullByDefault({}) SmartherBridgeHandler bridgeHandler;
    private @NonNullByDefault({}) SmartherModuleConfiguration config;

    // Chronothermostat local status
    private @NonNullByDefault({}) ExpiringCache<List<Program>> programCache;
    private @NonNullByDefault({}) Chronothermostat chronothermostat;
    private @NonNullByDefault({}) ModuleSettings moduleSettings;

    /**
     * Constructs a {@code SmartherModuleHandler} for the given thing, scheduler and dynamic state description provider.
     *
     * @param thing
     *            the {@link Thing} thing to be used
     * @param scheduler
     *            the {@link CronScheduler} periodic job scheduler to be used
     * @param provider
     *            the {@link SmartherDynamicStateDescriptionProvider} dynamic state description provider to be used
     */
    public SmartherModuleHandler(Thing thing, CronScheduler scheduler,
            SmartherDynamicStateDescriptionProvider provider) {
        super(thing);
        this.cronScheduler = scheduler;
        this.dynamicStateDescriptionProvider = provider;
        this.programChannelUID = new ChannelUID(thing.getUID(), CHANNEL_SETTINGS_PROGRAM);
        this.endDateChannelUID = new ChannelUID(thing.getUID(), CHANNEL_SETTINGS_ENDDATE);
    }

    // ===========================================================================
    //
    // Chronothermostat thing lifecycle management methods
    //
    // ===========================================================================

    @Override
    public void initialize() {
        logger.debug("Module[{}] Initialize handler", thing.getUID());

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            return;
        }

        bridgeHandler = (SmartherBridgeHandler) bridge.getHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, String.format(
                    "Missing configuration from the Smarther Bridge (UID:%s). Fix configuration or report if this problem remains.",
                    bridge.getBridgeUID()));
            return;
        }

        config = getConfigAs(SmartherModuleConfiguration.class);
        if (StringUtil.isBlank(config.getPlantId())) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The 'Plant Id' property is not set or empty. If you have an older thing please recreate it.");
            return;
        }
        if (StringUtil.isBlank(config.getModuleId())) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The 'Module Id' property is not set or empty. If you have an older thing please recreate it.");
            return;
        }
        if (config.getProgramsRefreshPeriod() <= 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The 'Programs Refresh Period' must be > 0. If you have an older thing please recreate it.");
            return;
        }
        if (config.getStatusRefreshPeriod() <= 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The 'Module Status Refresh Period' must be > 0. If you have an older thing please recreate it.");
            return;
        }

        programCache = new ExpiringCache<>(Duration.ofHours(config.getProgramsRefreshPeriod()),
                this::programCacheAction);
        moduleSettings = new ModuleSettings(config.getPlantId(), config.getModuleId());

        updateStatus(ThingStatus.UNKNOWN);

        scheduleJob();
        schedulePoll();

        logger.debug("Module[{}] Finished initializing!", thing.getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            handleCommandInternal(channelUID, command);

            updateModuleStatus();
        } catch (SmartherGatewayException ex) {
            // catch exceptions and handle it in your binding
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        }
    }

    /**
     * Handles the command sent to a given Channel of this Chronothermostat.
     *
     * @param channelUID
     *            the identifier of the Channel
     * @param command
     *            the command sent to the given Channel
     *
     * @throws {@link SmartherGatewayException}
     *             in case of communication issues with the Smarther API
     */
    private void handleCommandInternal(ChannelUID channelUID, Command command) throws SmartherGatewayException {
        switch (channelUID.getId()) {
            case CHANNEL_SETTINGS_MODE:
                if (command instanceof StringType) {
                    moduleSettings.setMode(Mode.fromValue(command.toString()));
                    return;
                }
                break;
            case CHANNEL_SETTINGS_TEMPERATURE:
                if (changeTemperature(command)) {
                    return;
                }
                break;
            case CHANNEL_SETTINGS_PROGRAM:
                if (command instanceof DecimalType) {
                    moduleSettings.setProgram(((DecimalType) command).intValue());
                    return;
                }
                break;
            case CHANNEL_SETTINGS_BOOSTTIME:
                if (command instanceof DecimalType) {
                    moduleSettings.setBoostTime(BoostTime.fromValue(((DecimalType) command).intValue()));
                    return;
                }
                break;
            case CHANNEL_SETTINGS_ENDDATE:
                if (command instanceof StringType) {
                    moduleSettings.setEndDate(command.toString());
                    return;
                }
                break;
            case CHANNEL_SETTINGS_ENDHOUR:
                if (changeTimeHour(command)) {
                    return;
                }
                break;
            case CHANNEL_SETTINGS_ENDMINUTE:
                if (changeTimeMinute(command)) {
                    return;
                }
                break;
            case CHANNEL_SETTINGS_POWER:
                if (command instanceof OnOffType) {
                    if (OnOffType.ON.equals(command)) {
                        // Send change to the remote module
                        applyModuleSettings();
                        updateChannelState(CHANNEL_SETTINGS_POWER, OnOffType.OFF);
                    }
                    return;
                }
                break;
            case CHANNEL_FETCH_CONFIG:
                if (command instanceof OnOffType) {
                    if (OnOffType.ON.equals(command)) {
                        logger.debug("Module[{}] Manually triggered channel to refresh the Module config",
                                thing.getUID());
                        expireCache();
                        programCache.getValue();
                        updateChannelState(CHANNEL_FETCH_CONFIG, OnOffType.OFF);
                    }
                    return;
                }
                break;
        }

        if (command instanceof RefreshType) {
            // Avoid logging wrong command when refresh command is sent
            return;
        }

        logger.debug("Module[{}] Received command {} of wrong type {} on channel {}", thing.getUID(), command,
                command.getClass().getTypeName(), channelUID.getId());
    }

    /**
     * Remotely applies the new settings to the Chronothermostat associated to this handler.
     *
     * @throws {@link SmartherGatewayException}
     *             in case of communication issues with the Smarther API
     */
    private void applyModuleSettings() throws SmartherGatewayException {
        // Send change to the remote module
        if (bridgeHandler.setModuleStatus(moduleSettings)) {
            // Change applied, update module status
            logger.debug("Module[{}] New settings applied!", thing.getUID());
        }
    }

    /**
     * Changes the "temperature" in module settings, based on the received Command.
     * The new value is checked against the temperature limits allowed by the device.
     *
     * @param command
     *            the command received on temperature Channel
     *
     * @return {@code true} if the change succeeded, {@code false} otherwise
     */
    private boolean changeTemperature(Command command) {
        if (!(command instanceof QuantityType)) {
            return false;
        }

        QuantityType<?> quantity = (QuantityType<?>) command;
        QuantityType<?> newMeasure = quantity.toUnit(SIUnits.CELSIUS);

        // Check remote device temperature limits
        if (newMeasure != null && newMeasure.doubleValue() >= 7.1 && newMeasure.doubleValue() <= 40.0) {
            // Only tenth degree increments are allowed
            double newTemperature = Math.round(newMeasure.doubleValue() * 10) / 10.0;

            moduleSettings.setSetPointTemperature(QuantityType.valueOf(newTemperature, SIUnits.CELSIUS));
            return true;
        }
        return false;
    }

    /**
     * Changes the "end hour" for manual mode in module settings, based on the received Command.
     * The new value is checked against the 24-hours clock allowed range.
     *
     * @param command
     *            the command received on end hour Channel
     *
     * @return {@code true} if the change succeeded, {@code false} otherwise
     */
    private boolean changeTimeHour(Command command) {
        if (command instanceof DecimalType) {
            int endHour = ((DecimalType) command).intValue();
            if (endHour >= 0 && endHour <= 23) {
                moduleSettings.setEndHour(endHour);
                return true;
            }
        }
        return false;
    }

    /**
     * Changes the "end minute" for manual mode in module settings, based on the received Command.
     * The new value is modified to match a 15 min step increment.
     *
     * @param command
     *            the command received on end minute Channel
     *
     * @return {@code true} if the change succeeded, {@code false} otherwise
     */
    private boolean changeTimeMinute(Command command) {
        if (command instanceof DecimalType) {
            int endMinute = ((DecimalType) command).intValue();
            if (endMinute >= 0 && endMinute <= 59) {
                // Only 15 min increments are allowed
                endMinute = Math.round(endMinute / 15) * 15;
                moduleSettings.setEndMinute(endMinute);
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the notification dispatched to this Chronothermostat from the reference Smarther Bridge.
     *
     * @param notification
     *            the notification to handle
     */
    public void handleNotification(Notification notification) {
        chronothermostat = notification.getData().toChronothermostat();
        if (config.isSettingsAutoupdate()) {
            moduleSettings.updateFromChronothermostat(chronothermostat);
        }
        logger.debug("Module[{}] Handle notification: [{}]", thing.getUID(), chronothermostat);

        updateModuleStatus();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() != ThingStatus.ONLINE) {
            // Put module offline when the parent bridge goes offline
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Smarther Bridge Offline");
            logger.debug("Module[{}] Bridge switched {}", thing.getUID(), bridgeStatusInfo.getStatus());
        } else {
            // Update the module status when the parent bridge return online
            logger.debug("Module[{}] Bridge is back ONLINE", thing.getUID());
            // Restart polling to collect module data
            schedulePoll();
        }
    }

    @Override
    public void handleRemoval() {
        super.handleRemoval();
        stopPoll(true);
        stopJob(true);
    }

    @Override
    public void dispose() {
        logger.debug("Module[{}] Dispose handler", thing.getUID());
        stopPoll(true);
        stopJob(true);
        try {
            bridgeHandler.unregisterNotification(config.getPlantId());
        } catch (SmartherGatewayException e) {
            logger.warn("Module[{}] API Gateway error during disposing: {}", thing.getUID(), e.getMessage());
        }
        logger.debug("Module[{}] Finished disposing!", thing.getUID());
    }

    // ===========================================================================
    //
    // Chronothermostat data cache management methods
    //
    // ===========================================================================

    /**
     * Returns the available automatic mode programs to be cached for this Chronothermostat.
     *
     * @return the available programs to be cached for this Chronothermostat, or {@code null} if the list of available
     *         programs cannot be retrieved
     */
    private @Nullable List<Program> programCacheAction() {
        try {
            final List<Program> programs = bridgeHandler.getModulePrograms(config.getPlantId(), config.getModuleId());
            logger.debug("Module[{}] Available programs: {}", thing.getUID(), programs);

            return programs;

        } catch (SmartherGatewayException e) {
            logger.warn("Module[{}] Cannot retrieve available programs: {}", thing.getUID(), e.getMessage());
            return null;
        }
    }

    /**
     * Sets all the cache to "expired" for this Chronothermostat.
     */
    private void expireCache() {
        logger.debug("Module[{}] Invalidating program cache", thing.getUID());
        programCache.invalidateValue();
    }

    // ===========================================================================
    //
    // Chronothermostat job scheduler methods
    //
    // ===========================================================================

    /**
     * Starts a new cron scheduler to execute the internal recurring jobs.
     */
    private synchronized void scheduleJob() {
        stopJob(false);
        // Schedule daily job to start daily, at midnight
        jobFuture = cronScheduler.schedule(this::dailyJob, DAILY_MIDNIGHT);
        logger.debug("Module[{}] Scheduled recurring job {} to start at midnight", thing.getUID(),
                Integer.toHexString(jobFuture.hashCode()));
        // Execute daily job immediately at startup
        this.dailyJob();
    }

    /**
     * Cancels all running jobs.
     *
     * @param mayInterruptIfRunning
     *            {@code true} if the thread executing this task should be interrupted, {@code false} if the in-progress
     *            tasks are allowed to complete
     */
    private synchronized void stopJob(boolean mayInterruptIfRunning) {
        if (jobFuture != null && !jobFuture.isCancelled()) {
            jobFuture.cancel(mayInterruptIfRunning);
            jobFuture = null;
        }
    }

    /**
     * Action to be executed by the daily job: refresh the end dates list for "manual" mode.
     */
    private void dailyJob() {
        logger.debug("Module[{}] Daily job, refreshing the end dates list for \"manual\" mode", thing.getUID());
        // Refresh the end dates list for "manual" mode
        dynamicStateDescriptionProvider.setEndDates(endDateChannelUID, config.getNumberOfEndDays());
        // If expired, update EndDate in module settings
        if (moduleSettings != null && moduleSettings.isEndDateExpired()) {
            moduleSettings.refreshEndDate();
            updateChannelState(CHANNEL_SETTINGS_ENDDATE, new StringType(moduleSettings.getEndDate()));
        }
    }

    // ===========================================================================
    //
    // Chronothermostat status polling mechanism methods
    //
    // ===========================================================================

    /**
     * Starts a new scheduler to periodically poll and update this Chronothermostat status.
     */
    private void schedulePoll() {
        stopPoll(false);
        // Schedule poll to start after POLL_INITIAL_DELAY sec and run periodically based on status refresh period
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, POLL_INITIAL_DELAY,
                config.getStatusRefreshPeriod() * 60, TimeUnit.SECONDS);
        logger.debug("Module[{}] Scheduled poll for {} sec out, then every {} min", thing.getUID(), POLL_INITIAL_DELAY,
                config.getStatusRefreshPeriod());
    }

    /**
     * Cancels all running poll schedulers.
     *
     * @param mayInterruptIfRunning
     *            {@code true} if the thread executing this task should be interrupted, {@code false} if the in-progress
     *            tasks are allowed to complete
     */
    private synchronized void stopPoll(boolean mayInterruptIfRunning) {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(mayInterruptIfRunning);
            pollFuture = null;
        }
    }

    /**
     * Polls to update this Chronothermostat status.
     *
     * @return {@code true} if the method completes without errors, {@code false} otherwise
     */
    private synchronized boolean poll() {
        try {
            final Bridge bridge = getBridge();
            if (bridge != null) {
                final ThingStatusInfo bridgeStatusInfo = bridge.getStatusInfo();
                if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
                    ModuleStatus moduleStatus = bridgeHandler.getModuleStatus(config.getPlantId(),
                            config.getModuleId());
                    if (!moduleStatus.hasChronothermostat()) {
                        throw new SmartherGatewayException("No chronothermostat data found");
                    }

                    boolean isFirstRemoteUpdate = (chronothermostat == null);
                    chronothermostat = moduleStatus.toChronothermostat();
                    if (isFirstRemoteUpdate || config.isSettingsAutoupdate()) {
                        moduleSettings.updateFromChronothermostat(chronothermostat);
                    }
                    logger.debug("Module[{}] Status: [{}]", thing.getUID(), chronothermostat);

                    // Refresh the programs list for "automatic" mode
                    final List<Program> programs = programCache.getValue();
                    if (programs != null) {
                        dynamicStateDescriptionProvider.setPrograms(programChannelUID, programs);
                    }

                    updateModuleStatus();

                    bridgeHandler.registerNotification(config.getPlantId());

                    // Everything is ok > set the Thing state to Online
                    updateStatus(ThingStatus.ONLINE);
                    return true;
                } else if (thing.getStatus() != ThingStatus.OFFLINE) {
                    logger.debug("Module[{}] Switched {} as Bridge is not online", thing.getUID(),
                            bridgeStatusInfo.getStatus());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Smarther Bridge Offline");
                }
            }
            return false;
        } catch (SmartherSubscriptionAlreadyExistsException e) {
            logger.debug("Module[{}] Subscription error during polling: {}", thing.getUID(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
            schedulePoll();
            return false;
        } catch (SmartherGatewayException e) {
            logger.warn("Module[{}] API Gateway error during polling: {}", thing.getUID(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            schedulePoll();
            return false;
        } catch (RuntimeException e) {
            // All other exceptions apart from Subscription and Gateway issues
            logger.warn("Module[{}] Unexpected error during polling, please report if this keeps occurring: ",
                    thing.getUID(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
            schedulePoll();
            return false;
        }
    }

    // ===========================================================================
    //
    // Chronothermostat convenience methods
    //
    // ===========================================================================

    /**
     * Returns this Chronothermostat plant identifier
     *
     * @return a string containing the plant identifier
     */
    public String getPlantId() {
        return config.getPlantId();
    }

    /**
     * Returns this Chronothermostat module identifier
     *
     * @return a string containing the module identifier
     */
    public String getModuleId() {
        return config.getModuleId();
    }

    /**
     * Checks whether this Chronothermostat matches with the given plant and module identifiers.
     *
     * @param plantId
     *            the plant identifier to match to
     * @param moduleId
     *            the module identifier to match to
     *
     * @return {@code true} if the Chronothermostat matches the given plant and module identifiers, {@code false}
     *         otherwise
     */
    public boolean isLinkedTo(String plantId, String moduleId) {
        return (config.getPlantId().equals(plantId) && config.getModuleId().equals(moduleId));
    }

    /**
     * Convenience method to update the given Channel state "only" if the Channel is linked.
     *
     * @param channelId
     *            the identifier of the Channel to be updated
     * @param state
     *            the new state to be applied to the given Channel
     */
    private void updateChannelState(String channelId, State state) {
        final Channel channel = thing.getChannel(channelId);

        if (channel != null && isLinked(channel.getUID())) {
            updateState(channel.getUID(), state);
        }
    }

    /**
     * Convenience method to update the whole status of the Chronothermostat associated to this handler.
     * Channels are updated based on the local {@code chronothermostat} and {@code moduleSettings} objects.
     */
    private void updateModuleStatus() {
        if (chronothermostat != null) {
            // Update the Measures channels
            updateChannelState(CHANNEL_MEASURES_TEMPERATURE, chronothermostat.getThermometer().toState());
            updateChannelState(CHANNEL_MEASURES_HUMIDITY, chronothermostat.getHygrometer().toState());
            // Update the Status channels
            updateChannelState(CHANNEL_STATUS_STATE, (chronothermostat.isActive() ? OnOffType.ON : OnOffType.OFF));
            updateChannelState(CHANNEL_STATUS_FUNCTION,
                    new StringType(StringUtil.capitalize(chronothermostat.getFunction().toLowerCase())));
            updateChannelState(CHANNEL_STATUS_MODE,
                    new StringType(StringUtil.capitalize(chronothermostat.getMode().toLowerCase())));
            updateChannelState(CHANNEL_STATUS_TEMPERATURE, chronothermostat.getSetPointTemperature().toState());
            updateChannelState(CHANNEL_STATUS_PROGRAM, new StringType("" + chronothermostat.getProgram().getNumber()));
            updateChannelState(CHANNEL_STATUS_ENDTIME, new StringType(chronothermostat.getActivationTimeLabel()));
            updateChannelState(CHANNEL_STATUS_TEMP_FORMAT, new StringType(chronothermostat.getTemperatureFormat()));
        }
        if (moduleSettings != null) {
            // Update the Settings channels
            updateChannelState(CHANNEL_SETTINGS_MODE, new StringType(moduleSettings.getMode().getValue()));
            updateChannelState(CHANNEL_SETTINGS_TEMPERATURE, moduleSettings.getSetPointTemperature());
            updateChannelState(CHANNEL_SETTINGS_PROGRAM, new DecimalType(moduleSettings.getProgram()));
            updateChannelState(CHANNEL_SETTINGS_BOOSTTIME, new DecimalType(moduleSettings.getBoostTime().getValue()));
            updateChannelState(CHANNEL_SETTINGS_ENDDATE, new StringType(moduleSettings.getEndDate()));
            updateChannelState(CHANNEL_SETTINGS_ENDHOUR, new DecimalType(moduleSettings.getEndHour()));
            updateChannelState(CHANNEL_SETTINGS_ENDMINUTE, new DecimalType(moduleSettings.getEndMinute()));
        }
        updateChannelState(CHANNEL_SETTINGS_POWER, OnOffType.OFF);
    }

}
