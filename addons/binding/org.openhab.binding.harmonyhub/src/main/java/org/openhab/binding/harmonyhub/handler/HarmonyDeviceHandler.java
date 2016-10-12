/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.harmonyhub.handler;

import static org.openhab.binding.harmonyhub.HarmonyHubBindingConstants.HARMONY_DEVICE_THING_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.harmonyhub.HarmonyHubBindingConstants;
import org.openhab.binding.harmonyhub.config.HarmonyDeviceConfig;
import org.openhab.binding.harmonyhub.internal.HarmonyHubHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.whistlingfish.harmony.config.ControlGroup;
import net.whistlingfish.harmony.config.Device;
import net.whistlingfish.harmony.config.Function;
import net.whistlingfish.harmony.config.HarmonyConfig;

/**
 * The {@link HarmonyDeviceHandler} is responsible for handling commands for Harmony Devices, which are
 * sent to one of the channels. It also is responsible for dynamically creating the button press channel
 * based on the device's available button press functions.
 *
 * @author Dan Cunningham - Initial contribution
 *
 */
public class HarmonyDeviceHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(HarmonyDeviceHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(HARMONY_DEVICE_THING_TYPE);

    HarmonyHubHandler bridge;
    HarmonyHubHandlerFactory factory;
    int id;
    String name;
    String logName;

    public HarmonyDeviceHandler(Thing thing, HarmonyHubHandlerFactory factory) {
        super(thing);
        this.factory = factory;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Command {}  for {}", command, channelUID);
        Channel channel = getThing().getChannel(channelUID.getId());

        if (channel == null) {
            logger.warn("No such channel {] for device {}", channelUID, getThing());
            return;
        }

        if (!(command instanceof StringType)) {
            logger.warn("Command {} is not a String type for channel {] for device {}", command, channelUID,
                    getThing());
            return;
        }

        logger.debug("Pressing button {} on {}", command, id > 0 ? 0 : name);

        if (id > 0) {
            bridge.getClient().pressButton(id, command.toString());
        } else {
            bridge.getClient().pressButton(name, command.toString());
        }

        // may need to ask the list if this can be set here?
        updateState(channelUID, UnDefType.UNDEF);
    }

    @Override
    public void initialize() {
        id = getConfig().as(HarmonyDeviceConfig.class).id;
        name = getConfig().as(HarmonyDeviceConfig.class).name;
        if (!checkConfig()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "A harmony device thing must be configured with a device name OR a postive device id");
        } else {
            logName = id > 0 ? String.valueOf(id) : name;
            logger.debug("initializing {}", logName);
            updateDeviceStatus(getBridge().getStatus());
        }
    };

    @Override
    public void dispose() {
        factory.removeChannelTypesForThing(getThing().getUID());
    }

    @Override
    public void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
        if (thingHandler instanceof HarmonyHubHandler) {
            logger.trace("bridgeHandlerInitialized for device {}", logName);
            this.bridge = (HarmonyHubHandler) thingHandler;
        }
    }

    @Override
    public void bridgeHandlerDisposed(ThingHandler thingHandler, Bridge bridge) {
        logger.debug("bridgeHandlerDisposed for device {}", logName);
        this.bridge = null;
        super.bridgeHandlerDisposed(thingHandler, bridge);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo statusInfo) {
        ThingStatus status = statusInfo.getStatus();
        logger.debug("hubStatusChanged {}  {}", logName, status);
        updateDeviceStatus(status);
    }

    /**
     * updates our local status to online if our bridge is online and we
     * have a valid configuration.
     *
     * @param status
     */
    private void updateDeviceStatus(ThingStatus status) {
        if (checkConfig() && status.equals(ThingStatus.ONLINE)) {
            updateStatus(ThingStatus.ONLINE);
            updateChannel();
        }
    }

    /**
     * Checks if we have a String name or numeric id
     *
     * @return
     */
    private boolean checkConfig() {
        return name != null || id >= 0;
    }

    /**
     * Updates our channel with the available buttons as option states
     */
    private void updateChannel() {
        try {
            logger.debug("updateChannel for device {}", logName);
            if (bridge == null) {
                logger.debug("updateChannel: no bridge for device {}", logName);
                return;
            }

            HarmonyConfig config = bridge.getCachedConfig();
            if (config == null) {
                logger.debug("updateChannel: could not get config from bridge {}", logName);
                return;
            }

            List<StateOption> states = new LinkedList<StateOption>();
            List<Device> devices = config.getDevices();

            // Iterate through button function commands and add them to our state list
            for (Device device : devices) {
                if (device.getId() != id) {
                    continue;
                }
                List<ControlGroup> controlGroups = device.getControlGroup();
                for (ControlGroup controlGroup : controlGroups) {
                    List<Function> functions = controlGroup.getFunction();
                    for (Function function : functions) {
                        states.add(new StateOption(function.getName(), function.getLabel()));
                    }
                }
                break;
            }

            ThingBuilder thingBuilder = editThing();

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(
                    getThing().getUID().getAsString() + ":" + HarmonyHubBindingConstants.CHANNEL_BUTTON_PRESS);

            ChannelType channelType = new ChannelType(channelTypeUID, false, "String", "Send Button Press",
                    "Send a button press to device " + getThing().getLabel(), null, null,
                    new StateDescription(null, null, null, null, false, states), null);

            factory.addChannelType(channelType);

            Channel channel = ChannelBuilder
                    .create(new ChannelUID(getThing().getUID(), HarmonyHubBindingConstants.CHANNEL_BUTTON_PRESS),
                            "String")
                    .withType(channelTypeUID).build();

            // replace existing buttonPress with updated one
            List<Channel> currentChannels = getThing().getChannels();
            List<Channel> newChannels = new ArrayList<Channel>();
            for (Channel c : currentChannels) {
                if (!c.getUID().equals(channel.getUID())) {
                    newChannels.add(c);
                }
            }
            newChannels.add(channel);
            thingBuilder.withChannels(newChannels);

            updateThing(thingBuilder.build());
        } catch (Exception e) {
            logger.debug("Could not add button channels to device " + logName, e);
        }
    }
}
