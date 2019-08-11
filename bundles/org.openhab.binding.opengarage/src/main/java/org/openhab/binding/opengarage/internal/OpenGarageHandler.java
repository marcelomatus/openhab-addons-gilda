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
package org.openhab.binding.opengarage.internal;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.MetricPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.binding.opengarage.internal.OpenGarageWebTargets;
import org.openhab.binding.opengarage.internal.api.ControllerVariables;
import org.openhab.binding.opengarage.internal.OpenGarageConfiguration;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link OpenGarageHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Paul Smedley - Initial contribution
 */

public class OpenGarageHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OpenGarageHandler.class);

    private long refreshInterval;

    private OpenGarageWebTargets webTargets;
    private ScheduledFuture<?> pollFuture;

    public OpenGarageHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            handleCommandInternal(channelUID, command);
        } catch (OpenGarageCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        }
    }

    private void handleCommandInternal(ChannelUID channelUID, Command command) throws OpenGarageCommunicationException {
        logger.warn("Received command {} for thing '{}' on channel {}", command, thing.getUID().getAsString(),
                    channelUID.getId());
        switch (channelUID.getId()) {
            case OpenGarageBindingConstants.CHANNEL_OG_STATUS:
                if (command instanceof OnOffType) {
                    changeStatus(((OnOffType) command).equals(OnOffType.ON));
                    return;
                }
                break;
        }

        if (RefreshType.REFRESH != command) {
            logger.warn("Received command {} of wrong type for thing '{}' on channel {}", command, thing.getUID().getAsString(),
                    channelUID.getId());
        } 
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Opengarage Unit");
        OpenGarageConfiguration config = getConfigAs(OpenGarageConfiguration.class);
        logger.debug("config.host = {}, refresh = {}, password = {}, port = {}", config.host, config.refresh, config.password, config.port);
        if (config.host == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Host address must be set");
        } else {
            webTargets = new OpenGarageWebTargets(config.host, config.port, config.password);
            refreshInterval = config.refresh;

            schedulePoll();
        }
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
        if (pollFuture != null) {
            pollFuture.cancel(false);
        }
        logger.debug("Scheduling poll for 500ms out, then every {} s", refreshInterval);
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, 1, refreshInterval, TimeUnit.SECONDS);
    }

    private synchronized void stopPoll() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }

    private synchronized void poll() {
        try {
            logger.debug("Polling for state");
            pollStatus();
        } catch (IOException e) {
            logger.debug("Could not connect to OpenGarage controller", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Unexpected error connecting to OpenGarage controller", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void pollStatus() throws IOException {
        ControllerVariables controllerVariables = webTargets.getControllerVariables();
        updateStatus(ThingStatus.ONLINE);
        if (controllerVariables != null) {
        logger.debug("Updating Thing/Items");
            updateState(OpenGarageBindingConstants.CHANNEL_OG_DISTANCE, new QuantityType<>(controllerVariables.dist, MetricPrefix.CENTI(SIUnits.METRE)));
            if (controllerVariables.door==0) {
                logger.debug("Door state == closed, {}, {}",controllerVariables.door, false);
                updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS, OnOffType.OFF);
            }
            if (controllerVariables.door==1) {
                logger.debug("Door state == open, {}, {}",controllerVariables.door, true);
                updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS, OnOffType.ON);
            }
            if (controllerVariables.vehicle==0)
                updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE, new StringType("No vehicle detected"));
            if (controllerVariables.vehicle==1)
                updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE, new StringType("Vehicle detected"));
            if (controllerVariables.vehicle==3)
                updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE, new StringType("Vehicle Status Unknown"));
        }
    }
    private void changeStatus(boolean status) throws OpenGarageCommunicationException {
        webTargets.setControllerVariables(status);
    }

}
