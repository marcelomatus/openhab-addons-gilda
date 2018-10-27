/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nuki.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.nuki.NukiBindingConstants;
import org.openhab.binding.nuki.internal.converter.LockActionConverter;
import org.openhab.binding.nuki.internal.dataexchange.BridgeLockActionResponse;
import org.openhab.binding.nuki.internal.dataexchange.BridgeLockStateResponse;
import org.openhab.binding.nuki.internal.dataexchange.NukiBaseResponse;
import org.openhab.binding.nuki.internal.dataexchange.NukiHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NukiSmartLockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Markus Katter - Initial contribution
 */
public class NukiSmartLockHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(NukiSmartLockHandler.class);
    private static final int RETRYJOB_INTERVAL = 60;

    private NukiHttpClient nukiHttpClient;
    private ScheduledFuture<?> retryJob;
    private String nukiId;

    public NukiSmartLockHandler(Thing thing) {
        super(thing);
        logger.debug("Instantiating NukiSmartLockHandler({})", thing);
    }

    @Override
    public void initialize() {
        logger.debug("NukiSmartLockHandler:initialize()");
        scheduler.execute(() -> initializeHandler());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("NukiSmartLockHandler:handleCommand({}, {})", channelUID, command);
        String nukiId = (String) getConfig().get(NukiBindingConstants.CONFIG_NUKI_ID);
        if (command instanceof RefreshType) {
            scheduler.execute(() -> handleCommandRefreshType(channelUID, command, nukiId));
        } else if (command instanceof OnOffType) {
            scheduler.execute(() -> handleCommandOnOffType(channelUID, command, nukiId));
        } else if (command instanceof DecimalType) {
            scheduler.execute(() -> handleCommandDecimalType(channelUID, command, nukiId));
        } else {
            logger.debug("NukiSmartLockHandler:handleCommand({}, {}) not implemented!", channelUID, command);
        }
    }

    @Override
    public void dispose() {
        logger.debug("NukiSmartLockHandler:dispose()");
        cancelRetryJob();
    }

    private void initializeHandler() {
        logger.debug("NukiSmartLockHandler:initializeHandler()");
        nukiId = (String) getConfig().get(NukiBindingConstants.CONFIG_NUKI_ID);
        if (nukiId == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }
        if (getNukiHttpClient() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            scheduleRetryJob();
            return;
        }
        BridgeLockStateResponse bridgeLockStateResponse = nukiHttpClient.getBridgeLockState(nukiId);
        if (handleResponse(bridgeLockStateResponse, null, null)) {
            updateStatus(ThingStatus.ONLINE);
            if (retryJob != null) {
                for (Channel channel : thing.getChannels()) {
                    handleCommand(channel.getUID(), RefreshType.REFRESH);
                }
            }
            cancelRetryJob();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    bridgeLockStateResponse.getMessage());
        }
    }

    private NukiHttpClient getNukiHttpClient() {
        logger.debug("NukiSmartLockHandler:getNukiHttpClient()");
        Bridge bridge = this.getBridge();
        if (bridge == null) {
            logger.debug("Setting Smart Lock[{}] offline because Bridge is offline!", nukiId);
            return null;
        }
        NukiBridgeHandler nbh = (NukiBridgeHandler) bridge.getHandler();
        if (nbh == null) {
            logger.debug("Setting Smart Lock[{}] offline because NukiBridgeHandler is null!", nukiId);
            return null;
        }
        nukiHttpClient = nbh.getNukiHttpClient();
        if (nukiHttpClient == null) {
            logger.debug("Setting Smart Lock[{}] offline because NukiBridgeHandler returned null for nukiHttpClient!",
                    nukiId);
            return null;
        }
        return nukiHttpClient;
    }

    private void handleCommandRefreshType(ChannelUID channelUID, Command command, String nukiId) {
        logger.debug("NukiSmartLockHandler:handleCommandRefreshType({}, {}, {})", channelUID, command, nukiId);
        if (nukiHttpClient == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            scheduleRetryJob();
            return;
        }
        BridgeLockStateResponse bridgeLockStateResponse = nukiHttpClient.getBridgeLockState(nukiId);
        if (handleResponse(bridgeLockStateResponse, channelUID.getAsString(), command.toString())) {
            int lockState = bridgeLockStateResponse.getState();
            State state;
            switch (channelUID.getId()) {
                case NukiBindingConstants.CHANNEL_SMARTLOCK_LOCK:
                    if (lockState == NukiBindingConstants.LOCK_STATES_LOCKED) {
                        state = OnOffType.ON;
                    } else if (lockState == NukiBindingConstants.LOCK_STATES_UNLOCKED) {
                        state = OnOffType.OFF;
                    } else {
                        logger.warn(
                                "Smart Lock returned lockState[{}]. Intentionally setting possibly wrong value 'OFF' for channel 'smartlockLock'!",
                                lockState);
                        state = OnOffType.OFF;
                    }
                    break;
                case NukiBindingConstants.CHANNEL_SMARTLOCK_STATE:
                    state = new DecimalType(lockState);
                    break;
                case NukiBindingConstants.CHANNEL_SMARTLOCK_LOW_BATTERY:
                    state = bridgeLockStateResponse.isBatteryCritical() ? OnOffType.ON : OnOffType.OFF;
                    break;
                default:
                    logger.debug("Command[{}] for channelUID[{}] not implemented!", command, channelUID);
                    return;
            }
            updateState(channelUID, state);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    bridgeLockStateResponse.getMessage());
        }
    }

    private void handleCommandOnOffType(ChannelUID channelUID, Command command, String nukiId) {
        logger.debug("NukiSmartLockHandler:handleCommandOnOffType({}, {}, {})", channelUID, command, nukiId);
        if (!channelUID.getId().equals(NukiBindingConstants.CHANNEL_SMARTLOCK_LOCK)) {
            logger.debug("Command[{}] for channelUID[{}] not implemented!", command, channelUID);
            return;
        }
        int lockAction;
        boolean unlatch = (Boolean) getConfig().get(NukiBindingConstants.CONFIG_UNLATCH);
        if (unlatch) {
            lockAction = (command == OnOffType.OFF ? NukiBindingConstants.LOCK_ACTIONS_UNLATCH
                    : NukiBindingConstants.LOCK_ACTIONS_LOCK);
        } else {
            lockAction = (command == OnOffType.OFF ? NukiBindingConstants.LOCK_ACTIONS_UNLOCK
                    : NukiBindingConstants.LOCK_ACTIONS_LOCK);
        }
        Channel channelLockState = thing.getChannel(NukiBindingConstants.CHANNEL_SMARTLOCK_STATE);
        if (channelLockState != null) {
            updateState(channelLockState.getUID(), new DecimalType(LockActionConverter.getLockStateFor(lockAction)));
        }
        BridgeLockActionResponse bridgeLockActionResponse = nukiHttpClient.getBridgeLockAction(nukiId, lockAction);
        handleResponse(bridgeLockActionResponse, channelUID.getAsString(), command.toString());
    }

    private void handleCommandDecimalType(ChannelUID channelUID, Command command, String nukiId) {
        logger.debug("NukiSmartLockHandler:handleCommandDecimalType({}, {}, {})", channelUID, command, nukiId);
        if (!channelUID.getId().equals(NukiBindingConstants.CHANNEL_SMARTLOCK_STATE)) {
            logger.debug("Command[{}] for channelUID[{}] not implemented!", command, channelUID);
            return;
        }
        int lockAction;
        lockAction = (command instanceof DecimalType) ? ((DecimalType) command).intValue() : 0;
        lockAction = LockActionConverter.getLockActionFor(lockAction);
        updateState(channelUID, new DecimalType(LockActionConverter.getLockStateFor(lockAction)));
        BridgeLockActionResponse bridgeLockActionResponse = nukiHttpClient.getBridgeLockAction(nukiId, lockAction);
        handleResponse(bridgeLockActionResponse, channelUID.getAsString(), command.toString());
    }

    private boolean handleResponse(NukiBaseResponse nukiBaseResponse, String channelUID, String command) {
        if (nukiBaseResponse.getStatus() == 200 && nukiBaseResponse.isSuccess()) {
            logger.debug("Command[{}] succeeded for channelUID[{}] on nukiId[{}]!", command, channelUID, nukiId);
            return true;
        } else if (nukiBaseResponse.getStatus() != 200) {
            logger.debug("Request to Bridge failed! status[{}] - message[{}]", nukiBaseResponse.getStatus(),
                    nukiBaseResponse.getMessage());
        } else if (!nukiBaseResponse.isSuccess()) {
            logger.debug(
                    "Request from Bridge to Smart Lock failed! status[{}] - message[{}] - isSuccess[{}]. Check if Nuki Smart Lock is powered on!",
                    nukiBaseResponse.getStatus(), nukiBaseResponse.getMessage(), nukiBaseResponse.isSuccess());
        }
        logger.debug("Could not handle command[{}] for channelUID[{}] on nukiId[{}]!", command, channelUID, nukiId);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, nukiBaseResponse.getMessage());
        Channel channelLock = thing.getChannel(NukiBindingConstants.CHANNEL_SMARTLOCK_LOCK);
        if (channelLock != null) {
            updateState(channelLock.getUID(), OnOffType.OFF);
        }
        Channel channelLockState = thing.getChannel(NukiBindingConstants.CHANNEL_SMARTLOCK_STATE);
        if (channelLockState != null) {
            updateState(channelLockState.getUID(), new DecimalType(NukiBindingConstants.LOCK_STATES_UNDEFINED));
        }
        scheduleRetryJob();
        return false;
    }

    private void scheduleRetryJob() {
        logger.trace("NukiSmartLockHandler:scheduleRetryJob():Scheduling retryJob in {}secs for Smart Lock[{}].",
                RETRYJOB_INTERVAL, nukiId);
        if (retryJob != null && !retryJob.isDone()) {
            logger.trace("NukiSmartLockHandler:scheduleRetryJob():Already scheduled for Smart Lock[{}].", nukiId);
            return;
        }
        retryJob = null;
        retryJob = scheduler.schedule(() -> {
            initialize();
        }, RETRYJOB_INTERVAL, TimeUnit.SECONDS);
    }

    private void cancelRetryJob() {
        logger.trace("NukiSmartLockHandler:cancelRetryJob():Canceling retryJob for Smart Lock[{}].", nukiId);
        if (retryJob != null) {
            retryJob.cancel(true);
            retryJob = null;
        }
    }

    public void handleApiServletUpdate(ChannelUID channelUID, State newState) {
        logger.trace("NukiSmartLockHandler:handleApiServletUpdate({}, {})", channelUID, newState);
        updateState(channelUID, newState);
    }

}
