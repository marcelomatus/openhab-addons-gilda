/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.neeo.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.neeo.NeeoConstants;
import org.openhab.binding.neeo.NeeoUtil;
import org.openhab.binding.neeo.internal.NeeoBrainApi;
import org.openhab.binding.neeo.internal.NeeoHandlerCallback;
import org.openhab.binding.neeo.internal.NeeoRoomConfig;
import org.openhab.binding.neeo.internal.NeeoRoomProtocol;
import org.openhab.binding.neeo.internal.models.NeeoAction;
import org.openhab.binding.neeo.internal.models.NeeoRoom;
import org.openhab.binding.neeo.internal.type.UidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A subclass of {@link AbstractBridgeHandler} that is responsible for handling commands for a room
 *
 * @author Tim Roberts - Initial contribution
 */
public class NeeoRoomHandler extends AbstractBridgeHandler {

    /** The logger */
    private final Logger logger = LoggerFactory.getLogger(NeeoRoomHandler.class);

    /**
     * The initialization task (null until set by {@link #initializeTask()} and set back to null in {@link #dispose()}
     */
    private final AtomicReference<Future<?>> initializationTask = new AtomicReference<>(null);

    /**
     * The refresh task (null until set by {@link #initializeTask()} and set back to null in {@link #dispose()}
     */
    private final AtomicReference<ScheduledFuture<?>> refreshTask = new AtomicReference<>(null);

    /** The {@link NeeoRoomProtocol} (null until set by {@link #initializationTask}) */
    private final AtomicReference<NeeoRoomProtocol> roomProtocol = new AtomicReference<>();

    /**
     * Instantiates a new neeo room handler.
     *
     * @param bridge the non-null bridge
     */
    NeeoRoomHandler(Bridge bridge) {
        super(bridge);
        Objects.requireNonNull(bridge, "bridge cannot be null");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Objects.requireNonNull(channelUID, "channelUID cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        final String[] groupIds = UidUtils.parseGroupId(channelUID);
        if (groupIds.length == 0) {
            logger.debug("Bad group declaration: {}", channelUID);
            return;
        }

        final NeeoRoomProtocol protocol = roomProtocol.get();
        if (protocol == null) {
            logger.debug("Protocol is null - ignoring update: {}", channelUID);
            return;
        }

        final String channelSection = groupIds[0];
        final String channelKey = groupIds.length > 1 ? groupIds[1] : "";
        final String channelId = channelUID.getIdWithoutGroup();

        if (command instanceof RefreshType) {
            refreshChannel(protocol, channelSection, channelKey, channelId);
        } else {
            switch (channelSection) {
                case NeeoConstants.ROOM_CHANNEL_GROUP_RECIPEID:
                    switch (channelId) {
                        case NeeoConstants.ROOM_CHANNEL_STATUS:
                            // Ignore OFF status updates
                            if (command == OnOffType.ON) {
                                protocol.startRecipe(channelKey);
                            }
                            break;
                    }
                    break;
                case NeeoConstants.ROOM_CHANNEL_GROUP_SCENARIOID:
                    switch (channelId) {
                        case NeeoConstants.ROOM_CHANNEL_STATUS:
                            if (command instanceof OnOffType) {
                                protocol.setScenarioStatus(channelKey, command == OnOffType.ON);
                            }
                            break;
                    }
                    break;
                default:
                    logger.debug("Unknown channel to set: {}", channelUID);
                    break;
            }
        }
    }

    /**
     * Refresh the specified channel section, key and id using the specified protocol
     *
     * @param protocol a non-null protocol to use
     * @param channelSection the non-empty channel section
     * @param channelKey the non-empty channel key
     * @param channelId the non-empty channel id
     */
    private void refreshChannel(NeeoRoomProtocol protocol, String channelSection, String channelKey, String channelId) {
        Objects.requireNonNull(protocol, "protocol cannot be null");
        NeeoUtil.requireNotEmpty(channelSection, "channelSection must not be empty");
        NeeoUtil.requireNotEmpty(channelId, "channelId must not be empty");

        switch (channelSection) {
            case NeeoConstants.ROOM_CHANNEL_GROUP_RECIPEID:
                NeeoUtil.requireNotEmpty(channelKey, "channelKey must not be empty");
                switch (channelId) {
                    case NeeoConstants.ROOM_CHANNEL_NAME:
                        protocol.refreshRecipeName(channelKey);
                        break;
                    case NeeoConstants.ROOM_CHANNEL_TYPE:
                        protocol.refreshRecipeType(channelKey);
                        break;
                    case NeeoConstants.ROOM_CHANNEL_ENABLED:
                        protocol.refreshRecipeEnabled(channelKey);
                        break;
                    case NeeoConstants.ROOM_CHANNEL_STATUS:
                        protocol.refreshRecipeStatus(channelKey);
                        break;
                }
                break;
            case NeeoConstants.ROOM_CHANNEL_GROUP_SCENARIOID:
                NeeoUtil.requireNotEmpty(channelKey, "channelKey must not be empty");
                switch (channelId) {
                    case NeeoConstants.ROOM_CHANNEL_NAME:
                        protocol.refreshScenarioName(channelKey);
                        break;
                    case NeeoConstants.ROOM_CHANNEL_CONFIGURED:
                        protocol.refreshScenarioConfigured(channelKey);
                        break;
                    case NeeoConstants.ROOM_CHANNEL_STATUS:
                        protocol.refreshScenarioStatus(channelKey);
                        break;
                }
                break;
        }
    }

    @Override
    public void initialize() {
        NeeoUtil.cancel(initializationTask.getAndSet(scheduler.submit(() -> {
            initializeTask();
        })));
    }

    /**
     * Initializes the task be creating the {@link NeeoRoomProtocol}, going online and then scheduling the refresh task.
     */
    private void initializeTask() {
        final NeeoRoomConfig config = getConfigAs(NeeoRoomConfig.class);
        try {
            NeeoUtil.checkInterrupt();
            final NeeoBrainApi brainApi = getNeeoBrainApi();
            if (brainApi == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                return;
            }

            final NeeoRoom room = brainApi.getRoom(config.getRoomKey());
            if (room == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Room (" + config.getRoomKey() + ") was not found");
                return;
            }

            final Map<String, String> properties = new HashMap<>();
            properties.put("Key", room.getKey());
            updateProperties(properties);

            NeeoUtil.checkInterrupt();
            final NeeoRoomProtocol protocol = new NeeoRoomProtocol(new NeeoHandlerCallback() {

                @Override
                public void statusChanged(ThingStatus status, ThingStatusDetail detail, String msg) {
                    updateStatus(status, detail, msg);
                }

                @Override
                public void stateChanged(String channelId, State state) {
                    updateState(channelId, state);
                }

                @Override
                public void setProperty(String propertyName, String propertyValue) {
                    getThing().setProperty(propertyName, propertyValue);
                }

                @Override
                public void scheduleTask(Runnable task, long milliSeconds) {
                    scheduler.schedule(task, milliSeconds, TimeUnit.MILLISECONDS);
                }

                @Override
                public void triggerEvent(String channelID, String event) {
                    triggerChannel(channelID, event);
                }

                @Override
                public NeeoBrainApi getApi() {
                    return getNeeoBrainApi();
                }
            }, config.getRoomKey());
            roomProtocol.getAndSet(protocol);

            NeeoUtil.checkInterrupt();
            updateStatus(ThingStatus.ONLINE);

            if (config.getRefreshPolling() > 0) {
                NeeoUtil.checkInterrupt();
                NeeoUtil.cancel(refreshTask.getAndSet(scheduler.scheduleWithFixedDelay(() -> {
                    try {
                        refreshState();
                    } catch (InterruptedException e) {
                        logger.debug("Refresh State was interrupted", e);
                    }
                }, 0, config.getRefreshPolling(), TimeUnit.SECONDS)));
            }
        } catch (IOException e) {
            logger.debug("IOException during initialization", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Room " + config.getRoomKey() + " couldn't be found");
        } catch (InterruptedException e) {
            logger.debug("Initialization was interrupted", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                    "Initialization was interrupted");
        }
    }

    /**
     * Processes the action if it applies to this room
     *
     * @param action a non-null action to process
     */
    void processAction(NeeoAction action) {
        Objects.requireNonNull(action, "action cannot be null");
        final NeeoRoomProtocol protocol = roomProtocol.get();
        if (protocol != null) {
            protocol.processAction(action);
        }
    }

    /**
     * Refreshes the state of the room by calling {@link NeeoRoomProtocol#refreshState()}
     *
     * @throws InterruptedException
     */
    private void refreshState() throws InterruptedException {
        NeeoUtil.checkInterrupt();
        final NeeoRoomProtocol protocol = roomProtocol.get();
        if (protocol != null) {
            NeeoUtil.checkInterrupt();
            protocol.refreshState();
        }
    }

    @Override
    public void dispose() {
        NeeoUtil.cancel(initializationTask.getAndSet(null));
        NeeoUtil.cancel(refreshTask.getAndSet(null));
        roomProtocol.getAndSet(null);
    }
}
