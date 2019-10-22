/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.doorbird.internal.handler;

import static org.openhab.binding.doorbird.internal.DoorbirdBindingConstants.*;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.doorbird.internal.api.DoorbirdAPI;
import org.openhab.binding.doorbird.internal.api.DoorbirdInfo;
import org.openhab.binding.doorbird.internal.config.ControllerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ControllerHandler} is responsible for handling commands
 * to the A1081 Controller.
 *
 * @author Mark Hilbush - Initial contribution
 */
@NonNullByDefault
public class ControllerHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ControllerHandler.class);

    private @Nullable String controllerId;

    private DoorbirdAPI api = new DoorbirdAPI();

    public ControllerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        String host = getConfigAs(ControllerConfiguration.class).doorbirdHost;
        if (StringUtils.isEmpty(host)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Doorbird host not provided");
            return;
        }
        String user = getConfigAs(ControllerConfiguration.class).userId;
        if (StringUtils.isEmpty(user)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "User ID not provided");
            return;
        }
        String password = getConfigAs(ControllerConfiguration.class).userPassword;
        if (StringUtils.isEmpty(password)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "User password not provided");
            return;
        }

        api.setAuthorization(host, user, password);

        // Get the Id of the controller for use in the open door API
        controllerId = getControllerId();
        if (controllerId != null) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Doorbird not configured with a Controller");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Got command {} for channel {} of thing {}", command, channelUID, getThing().getUID());

        switch (channelUID.getId()) {
            case CHANNEL_OPENDOOR1:
                handleOpenDoor(command, "1");
                break;
            case CHANNEL_OPENDOOR2:
                handleOpenDoor(command, "2");
                break;
            case CHANNEL_OPENDOOR3:
                handleOpenDoor(command, "3");
                break;
        }
    }

    private void handleOpenDoor(Command command, String doorNumber) {
        String id = controllerId;
        if (id == null) {
            logger.debug("Unable to handle open door command because controller ID is not set");
            return;
        }
        if (command instanceof OnOffType && command.equals(OnOffType.ON)) {
            api.openDoorController(id, doorNumber);
        }
    }

    private @Nullable String getControllerId() {
        DoorbirdInfo info = api.getDoorbirdInfo();
        return info == null ? null : info.getControllerId();
    }
}
