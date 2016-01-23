/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.orvibo.handler;

import static org.openhab.binding.orvibo.OrviboBindingConstants.CHANNEL_S20_SWITCH;

import java.net.SocketException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tavalin.s20.S20Client;
import com.github.tavalin.s20.entities.Types.PowerState;
import com.github.tavalin.s20.socket.Socket;
import com.github.tavalin.s20.socket.Socket.SocketStateListener;

/**
 * The {@link S20Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Daniel Walters - Initial contribution
 */
public class S20Handler extends BaseThingHandler implements SocketStateListener {

    private Logger logger = LoggerFactory.getLogger(S20Handler.class);
    private Socket socket;
    private S20Client s20Client;
    private ScheduledFuture<?> subscribeHandler;
    private long refreshInterval = 15;
    private Runnable subscribeTask = new Runnable() {
        @Override
        public void run() {
            socket.subscribe();
            socket.getSocketData();
            // socket.getTableData();
        }
    };

    public S20Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        configure();
    }

    @Override
    public void dispose() {
        subscribeHandler.cancel(true);
        socket.removeSocketStateListener(this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_S20_SWITCH)) {
            if (command == OnOffType.ON) {
                socket.on();
            } else if (command == OnOffType.OFF) {
                socket.off();
            }
        }
    }

    private void configure() {
        try {
            s20Client = S20Client.getInstance();
            String deviceId = thing.getUID().getId();
            socket = s20Client.socketWithDeviceID(deviceId);
            socket.addSocketStateListener(this);
            socket.findOnNetwork();
            subscribeHandler = scheduler.scheduleWithFixedDelay(subscribeTask, 0, refreshInterval, TimeUnit.SECONDS);
        } catch (SocketException ex) {
            logger.error("Error occured while initializing S20 handler: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void socketDidChangeLabel(Socket socket, String label) {
        if (!StringUtils.isBlank(label)) {
            logger.debug("Updating thing label to " + label);
            thing.getLinkedItem().setLabel(label);
        }
    }

    @Override
    public void socketDidChangePowerState(Socket socket, PowerState state) {
        logger.debug("Received power state: " + state);
        if (socket.getDeviceId().equals(thing.getUID().getId())) {
            if (state == PowerState.ON) {
                updateState(CHANNEL_S20_SWITCH, OnOffType.ON);
            } else if (state == PowerState.OFF) {
                updateState(CHANNEL_S20_SWITCH, OnOffType.OFF);
            }
        }
    }

    @Override
    public void socketDidInitialisation(Socket socket) {
        if (thing.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }
}
