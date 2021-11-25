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
package org.openhab.binding.hdpowerview.internal.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.hdpowerview.internal.HDPowerViewBindingConstants;
import org.openhab.binding.hdpowerview.internal.HDPowerViewTranslationProvider;
import org.openhab.binding.hdpowerview.internal.HDPowerViewWebTargets;
import org.openhab.binding.hdpowerview.internal.HubMaintenanceException;
import org.openhab.binding.hdpowerview.internal.HubProcessingException;
import org.openhab.binding.hdpowerview.internal.api.responses.SceneCollections;
import org.openhab.binding.hdpowerview.internal.api.responses.SceneCollections.SceneCollection;
import org.openhab.binding.hdpowerview.internal.api.responses.Scenes;
import org.openhab.binding.hdpowerview.internal.api.responses.Scenes.Scene;
import org.openhab.binding.hdpowerview.internal.api.responses.ScheduledEvents;
import org.openhab.binding.hdpowerview.internal.api.responses.ScheduledEvents.ScheduledEvent;
import org.openhab.binding.hdpowerview.internal.api.responses.Shades;
import org.openhab.binding.hdpowerview.internal.api.responses.Shades.ShadeData;
import org.openhab.binding.hdpowerview.internal.config.HDPowerViewHubConfiguration;
import org.openhab.binding.hdpowerview.internal.config.HDPowerViewShadeConfiguration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

/**
 * The {@link HDPowerViewHubHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * @author Andy Lintner - Initial contribution
 * @author Andrew Fiddian-Green - Added support for secondary rail positions
 * @author Jacob Laursen - Add support for scene groups and automations
 */
@NonNullByDefault
public class HDPowerViewHubHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(HDPowerViewHubHandler.class);
    private final HttpClient httpClient;
    private final HDPowerViewTranslationProvider translationProvider;

    private long refreshInterval;
    private long hardRefreshPositionInterval;
    private long hardRefreshBatteryLevelInterval;

    private @Nullable HDPowerViewWebTargets webTargets;
    private @Nullable ScheduledFuture<?> pollFuture;
    private @Nullable ScheduledFuture<?> hardRefreshPositionFuture;
    private @Nullable ScheduledFuture<?> hardRefreshBatteryLevelFuture;

    private final ChannelTypeUID sceneChannelTypeUID = new ChannelTypeUID(HDPowerViewBindingConstants.BINDING_ID,
            HDPowerViewBindingConstants.CHANNELTYPE_SCENE_ACTIVATE);

    private final ChannelTypeUID sceneGroupChannelTypeUID = new ChannelTypeUID(HDPowerViewBindingConstants.BINDING_ID,
            HDPowerViewBindingConstants.CHANNELTYPE_SCENE_GROUP_ACTIVATE);

    private final ChannelTypeUID automationChannelTypeUID = new ChannelTypeUID(HDPowerViewBindingConstants.BINDING_ID,
            HDPowerViewBindingConstants.CHANNELTYPE_AUTOMATION_ENABLED);

    public HDPowerViewHubHandler(Bridge bridge, HttpClient httpClient,
            HDPowerViewTranslationProvider translationProvider) {
        super(bridge);
        this.httpClient = httpClient;
        this.translationProvider = translationProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH.equals(command)) {
            requestRefreshShadePositions();
            return;
        }

        Channel channel = getThing().getChannel(channelUID.getId());
        if (channel == null) {
            return;
        }

        try {
            HDPowerViewWebTargets webTargets = this.webTargets;
            if (webTargets == null) {
                throw new ProcessingException("Web targets not initialized");
            }
            int id = Integer.parseInt(channelUID.getIdWithoutGroup());
            if (sceneChannelTypeUID.equals(channel.getChannelTypeUID()) && OnOffType.ON.equals(command)) {
                webTargets.activateScene(id);
            } else if (sceneGroupChannelTypeUID.equals(channel.getChannelTypeUID()) && OnOffType.ON.equals(command)) {
                webTargets.activateSceneCollection(id);
            } else if (automationChannelTypeUID.equals(channel.getChannelTypeUID())) {
                webTargets.enableScheduledEvent(id, OnOffType.ON.equals(command));
            }
        } catch (HubMaintenanceException e) {
            // exceptions are logged in HDPowerViewWebTargets
        } catch (NumberFormatException | HubProcessingException e) {
            logger.debug("Unexpected error {}", e.getMessage());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing hub");
        HDPowerViewHubConfiguration config = getConfigAs(HDPowerViewHubConfiguration.class);
        String host = config.host;

        if (host == null || host.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.conf-error.no-host-address");
            return;
        }

        webTargets = new HDPowerViewWebTargets(httpClient, host);
        refreshInterval = config.refresh;
        hardRefreshPositionInterval = config.hardRefresh;
        hardRefreshBatteryLevelInterval = config.hardRefreshBatteryLevel;
        schedulePoll();
    }

    public @Nullable HDPowerViewWebTargets getWebTargets() {
        return webTargets;
    }

    @Override
    public void handleRemoval() {
        super.handleRemoval();
        stopPoll();
    }

    @Override
    public void dispose() {
        super.dispose();
        stopPoll();
    }

    private void schedulePoll() {
        ScheduledFuture<?> future = this.pollFuture;
        if (future != null) {
            future.cancel(false);
        }
        logger.debug("Scheduling poll for 5000ms out, then every {}ms", refreshInterval);
        this.pollFuture = scheduler.scheduleWithFixedDelay(this::poll, 5000, refreshInterval, TimeUnit.MILLISECONDS);

        future = this.hardRefreshPositionFuture;
        if (future != null) {
            future.cancel(false);
        }
        if (hardRefreshPositionInterval > 0) {
            logger.debug("Scheduling hard position refresh every {} minutes", hardRefreshPositionInterval);
            this.hardRefreshPositionFuture = scheduler.scheduleWithFixedDelay(this::requestRefreshShadePositions, 1,
                    hardRefreshPositionInterval, TimeUnit.MINUTES);
        }

        future = this.hardRefreshBatteryLevelFuture;
        if (future != null) {
            future.cancel(false);
        }
        if (hardRefreshBatteryLevelInterval > 0) {
            logger.debug("Scheduling hard battery level refresh every {} hours", hardRefreshBatteryLevelInterval);
            this.hardRefreshBatteryLevelFuture = scheduler.scheduleWithFixedDelay(
                    this::requestRefreshShadeBatteryLevels, 1, hardRefreshBatteryLevelInterval, TimeUnit.HOURS);
        }
    }

    private synchronized void stopPoll() {
        ScheduledFuture<?> future = this.pollFuture;
        if (future != null) {
            future.cancel(true);
        }
        this.pollFuture = null;

        future = this.hardRefreshPositionFuture;
        if (future != null) {
            future.cancel(true);
        }
        this.hardRefreshPositionFuture = null;

        future = this.hardRefreshBatteryLevelFuture;
        if (future != null) {
            future.cancel(true);
        }
        this.hardRefreshBatteryLevelFuture = null;
    }

    private synchronized void poll() {
        try {
            logger.debug("Polling for state");
            pollShades();
            List<Scene> scenes = pollScenes();
            List<SceneCollection> sceneCollections = pollSceneCollections();
            List<ScheduledEvent> scheduledEvents = pollScheduledEvents();
            updateScheduledEventChannels(scenes, sceneCollections, scheduledEvents);
            updateScheduledEventStates(scheduledEvents);
        } catch (JsonParseException e) {
            logger.warn("Bridge returned a bad JSON response: {}", e.getMessage());
        } catch (HubProcessingException e) {
            logger.warn("Error connecting to bridge: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, e.getMessage());
        } catch (HubMaintenanceException e) {
            // exceptions are logged in HDPowerViewWebTargets
        }
    }

    private void pollShades() throws JsonParseException, HubProcessingException, HubMaintenanceException {
        HDPowerViewWebTargets webTargets = this.webTargets;
        if (webTargets == null) {
            throw new ProcessingException("Web targets not initialized");
        }

        Shades shades = webTargets.getShades();
        if (shades == null) {
            throw new JsonParseException("Missing 'shades' element");
        }

        List<ShadeData> shadesData = shades.shadeData;
        if (shadesData == null) {
            throw new JsonParseException("Missing 'shades.shadeData' element");
        }

        updateStatus(ThingStatus.ONLINE);
        logger.debug("Received data for {} shades", shadesData.size());

        Map<String, ShadeData> idShadeDataMap = getIdShadeDataMap(shadesData);
        Map<Thing, String> thingIdMap = getThingIdMap();
        for (Entry<Thing, String> item : thingIdMap.entrySet()) {
            Thing thing = item.getKey();
            String shadeId = item.getValue();
            ShadeData shadeData = idShadeDataMap.get(shadeId);
            updateShadeThing(shadeId, thing, shadeData);
        }
    }

    private void updateShadeThing(String shadeId, Thing thing, @Nullable ShadeData shadeData) {
        HDPowerViewShadeHandler thingHandler = ((HDPowerViewShadeHandler) thing.getHandler());
        if (thingHandler == null) {
            logger.debug("Shade '{}' handler not initialized", shadeId);
            return;
        }
        if (shadeData == null) {
            logger.debug("Shade '{}' has no data in hub", shadeId);
        } else {
            logger.debug("Updating shade '{}'", shadeId);
        }
        thingHandler.onReceiveUpdate(shadeData);
    }

    private List<Scene> pollScenes() throws JsonParseException, HubProcessingException, HubMaintenanceException {
        HDPowerViewWebTargets webTargets = this.webTargets;
        if (webTargets == null) {
            throw new ProcessingException("Web targets not initialized");
        }

        Scenes scenes = webTargets.getScenes();
        if (scenes == null) {
            throw new JsonParseException("Missing 'scenes' element");
        }

        List<Scene> sceneData = scenes.sceneData;
        if (sceneData == null) {
            throw new JsonParseException("Missing 'scenes.sceneData' element");
        }
        logger.debug("Received data for {} scenes", sceneData.size());

        Map<String, Channel> idChannelMap = getIdChannelMap(HDPowerViewBindingConstants.CHANNEL_GROUP_SCENES);
        List<Channel> allChannels = new ArrayList<>(getThing().getChannels());
        ChannelGroupUID channelGroupUid = new ChannelGroupUID(thing.getUID(),
                HDPowerViewBindingConstants.CHANNEL_GROUP_SCENES);
        boolean isChannelListChanged = false;
        for (Scene scene : sceneData) {
            ChannelUID channelUid = new ChannelUID(channelGroupUid, Integer.toString(scene.id));
            String channelId = channelUid.getId();
            // remove existing scene channel from the map
            if (idChannelMap.containsKey(channelId)) {
                idChannelMap.remove(channelId);
                logger.debug("Keeping channel for existing scene '{}'", scene.id);
            } else {
                // create a new scene channel
                String description = translationProvider.getText("dynamic-channel.scene-activate.description",
                        scene.getName());
                Channel channel = ChannelBuilder.create(channelUid, CoreItemFactory.SWITCH)
                        .withType(sceneChannelTypeUID).withLabel(scene.getName()).withDescription(description).build();
                allChannels.add(channel);
                isChannelListChanged = true;
                logger.debug("Creating new channel for scene '{}'", scene.id);
            }
        }

        // remove any previously created channels that no longer exist
        if (!idChannelMap.isEmpty()) {
            logger.debug("Removing {} orphan scene channels", idChannelMap.size());
            allChannels.removeAll(idChannelMap.values());
            isChannelListChanged = true;
        }

        if (isChannelListChanged) {
            updateThing(editThing().withChannels(allChannels).build());
        }

        return sceneData;
    }

    private List<SceneCollection> pollSceneCollections()
            throws JsonParseException, HubProcessingException, HubMaintenanceException {
        HDPowerViewWebTargets webTargets = this.webTargets;
        if (webTargets == null) {
            throw new ProcessingException("Web targets not initialized");
        }

        SceneCollections sceneCollections = webTargets.getSceneCollections();
        if (sceneCollections == null) {
            throw new JsonParseException("Missing 'sceneCollections' element");
        }

        List<SceneCollection> sceneCollectionData = sceneCollections.sceneCollectionData;
        if (sceneCollectionData == null) {
            throw new JsonParseException("Missing 'sceneCollections.sceneCollectionData' element");
        }
        logger.debug("Received data for {} sceneCollections", sceneCollectionData.size());

        Map<String, Channel> idChannelMap = getIdChannelMap(HDPowerViewBindingConstants.CHANNEL_GROUP_SCENE_GROUPS);
        List<Channel> allChannels = new ArrayList<>(getThing().getChannels());
        ChannelGroupUID channelGroupUid = new ChannelGroupUID(thing.getUID(),
                HDPowerViewBindingConstants.CHANNEL_GROUP_SCENE_GROUPS);
        boolean isChannelListChanged = false;
        for (SceneCollection sceneCollection : sceneCollectionData) {
            ChannelUID channelUid = new ChannelUID(channelGroupUid, Integer.toString(sceneCollection.id));
            String channelId = channelUid.getId();
            // remove existing scene collection channel from the map
            if (idChannelMap.containsKey(channelId)) {
                idChannelMap.remove(channelId);
                logger.debug("Keeping channel for existing scene collection '{}'", sceneCollection.id);
            } else {
                // create a new scene collection channel
                String description = translationProvider.getText("dynamic-channel.scene-group-activate.description",
                        sceneCollection.getName());
                Channel channel = ChannelBuilder.create(channelUid, CoreItemFactory.SWITCH)
                        .withType(sceneGroupChannelTypeUID).withLabel(sceneCollection.getName())
                        .withDescription(description).build();
                allChannels.add(channel);
                isChannelListChanged = true;
                logger.debug("Creating new channel for scene collection '{}'", sceneCollection.id);
            }
        }

        // remove any previously created channels that no longer exist
        if (!idChannelMap.isEmpty()) {
            logger.debug("Removing {} orphan scene collection channels", idChannelMap.size());
            allChannels.removeAll(idChannelMap.values());
            isChannelListChanged = true;
        }

        if (isChannelListChanged) {
            updateThing(editThing().withChannels(allChannels).build());
        }

        return sceneCollectionData;
    }

    private List<ScheduledEvent> pollScheduledEvents()
            throws JsonParseException, HubProcessingException, HubMaintenanceException {
        HDPowerViewWebTargets webTargets = this.webTargets;
        if (webTargets == null) {
            throw new ProcessingException("Web targets not initialized");
        }

        ScheduledEvents scheduledEvents = webTargets.getScheduledEvents();
        if (scheduledEvents == null) {
            throw new JsonParseException("Missing 'scheduledEvents' element");
        }

        List<ScheduledEvent> scheduledEventData = scheduledEvents.scheduledEventData;
        if (scheduledEventData == null) {
            throw new JsonParseException("Missing 'scheduledEvents.scheduledEventData' element");
        }
        logger.debug("Received data for {} scheduledEvents", scheduledEventData.size());

        return scheduledEventData;
    }

    private void updateScheduledEventChannels(List<Scene> scenes, List<SceneCollection> sceneCollections,
            List<ScheduledEvent> scheduledEvents) {
        Map<String, Channel> idChannelMap = getIdChannelMap(HDPowerViewBindingConstants.CHANNEL_GROUP_AUTOMATIONS);
        List<Channel> allChannels = new ArrayList<>(getThing().getChannels());
        ChannelGroupUID channelGroupUid = new ChannelGroupUID(thing.getUID(),
                HDPowerViewBindingConstants.CHANNEL_GROUP_AUTOMATIONS);
        boolean isChannelListChanged = false;
        for (ScheduledEvent scheduledEvent : scheduledEvents) {
            ChannelUID channelUid = new ChannelUID(channelGroupUid, Integer.toString(scheduledEvent.id));
            String channelId = channelUid.getId();
            // remove existing scheduled event channel from the map
            if (idChannelMap.containsKey(channelId)) {
                idChannelMap.remove(channelId);
                logger.debug("Keeping channel for existing scheduled event '{}'", scheduledEvent.id);
            } else {
                // create a new scheduled event channel
                String name = null;
                if (scheduledEvent.sceneId > 0) {
                    for (Scene scene : scenes) {
                        if (scene.id == scheduledEvent.sceneId) {
                            name = scene.getName();
                            break;
                        }
                    }
                    if (name == null) {
                        logger.error("sceneId {} was not found for scheduledEventId {}", scheduledEvent.sceneId,
                                scheduledEvent.id);
                        continue;
                    }
                } else if (scheduledEvent.sceneCollectionId > 0) {
                    for (SceneCollection sceneCollection : sceneCollections) {
                        if (sceneCollection.id == scheduledEvent.sceneCollectionId) {
                            name = sceneCollection.getName();
                            break;
                        }
                    }
                    if (name == null) {
                        logger.error("sceneCollectionId {} was not found for scheduledEventId {}",
                                scheduledEvent.sceneCollectionId, scheduledEvent.id);
                        continue;
                    }
                } else {
                    logger.error("ScheduledEventId {} not related to any scene or scene collection", scheduledEvent.id);
                    continue;
                }

                String description = translationProvider.getText("dynamic-channel.automation-enabled.description",
                        name);
                Channel channel = ChannelBuilder.create(channelUid, CoreItemFactory.SWITCH)
                        .withType(automationChannelTypeUID).withLabel(name).withDescription(description).build();
                allChannels.add(channel);
                isChannelListChanged = true;
                logger.debug("Creating new channel for scheduled event '{}'", scheduledEvent.id);
            }
        }

        // remove any previously created channels that no longer exist
        if (!idChannelMap.isEmpty()) {
            logger.debug("Removing {} orphan scheduled event channels", idChannelMap.size());
            allChannels.removeAll(idChannelMap.values());
            isChannelListChanged = true;
        }

        if (isChannelListChanged) {
            updateThing(editThing().withChannels(allChannels).build());
        }
    }

    private void updateScheduledEventStates(List<ScheduledEvent> scheduledEvents) {
        ChannelGroupUID channelGroupUid = new ChannelGroupUID(thing.getUID(),
                HDPowerViewBindingConstants.CHANNEL_GROUP_AUTOMATIONS);
        for (ScheduledEvent scheduledEvent : scheduledEvents) {
            String scheduledEventId = Integer.toString(scheduledEvent.id);
            ChannelUID channelUid = new ChannelUID(channelGroupUid, scheduledEventId);
            updateState(channelUid, scheduledEvent.enabled ? OnOffType.ON : OnOffType.OFF);
        }
    }

    private Map<Thing, String> getThingIdMap() {
        Map<Thing, String> ret = new HashMap<>();
        for (Thing thing : getThing().getThings()) {
            String id = thing.getConfiguration().as(HDPowerViewShadeConfiguration.class).id;
            if (id != null && !id.isEmpty()) {
                ret.put(thing, id);
            }
        }
        return ret;
    }

    private Map<String, ShadeData> getIdShadeDataMap(List<ShadeData> shadeData) {
        Map<String, ShadeData> ret = new HashMap<>();
        for (ShadeData shade : shadeData) {
            if (shade.id != 0) {
                ret.put(Integer.toString(shade.id), shade);
            }
        }
        return ret;
    }

    private Map<String, Channel> getIdChannelMap(String channelGroupId) {
        Map<String, Channel> ret = new HashMap<>();
        for (Channel channel : getThing().getChannelsOfGroup(channelGroupId)) {
            ret.put(channel.getUID().getId(), channel);
        }
        return ret;
    }

    private void requestRefreshShadePositions() {
        Map<Thing, String> thingIdMap = getThingIdMap();
        for (Entry<Thing, String> item : thingIdMap.entrySet()) {
            Thing thing = item.getKey();
            ThingHandler handler = thing.getHandler();
            if (handler instanceof HDPowerViewShadeHandler) {
                ((HDPowerViewShadeHandler) handler).requestRefreshShadePosition();
            } else {
                String shadeId = item.getValue();
                logger.debug("Shade '{}' handler not initialized", shadeId);
            }
        }
    }

    private void requestRefreshShadeBatteryLevels() {
        Map<Thing, String> thingIdMap = getThingIdMap();
        for (Entry<Thing, String> item : thingIdMap.entrySet()) {
            Thing thing = item.getKey();
            ThingHandler handler = thing.getHandler();
            if (handler instanceof HDPowerViewShadeHandler) {
                ((HDPowerViewShadeHandler) handler).requestRefreshShadeBatteryLevel();
            } else {
                String shadeId = item.getValue();
                logger.debug("Shade '{}' handler not initialized", shadeId);
            }
        }
    }
}
