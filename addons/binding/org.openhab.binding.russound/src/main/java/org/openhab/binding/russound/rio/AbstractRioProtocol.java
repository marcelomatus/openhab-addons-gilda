/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rio;

import java.io.IOException;

import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.russound.internal.net.SocketSession;
import org.openhab.binding.russound.internal.net.SocketSessionListener;
import org.openhab.binding.russound.rio.system.RioSystemHandler;

/**
 * Defines the abstract base for a protocol handler. This base provides managment of the {@link SocketSession} and
 * provides helper methods that will callback {@link RioHandlerCallback}
 *
 * @author Tim Roberts
 * @version $Id: $Id
 *
 */
public abstract class AbstractRioProtocol implements SocketSessionListener {
    /**
     * The {@link SocketSession} used by this protocol handler
     */
    private final SocketSession _session;

    /**
     * The {@link RioSystemHandler} to call back to update status and state
     */
    private final RioHandlerCallback _callback;

    /**
     * Constructs the protocol handler from given parameters and will add this handler as a
     * {@link SocketSessionListener} to the specified {@link SocketSession} via
     * {@link SocketSession#addListener(SocketSessionListener)}
     *
     * @param session a non-null {@link SocketSession} (may be connected or disconnected)
     * @param callback a non-null {@link RioHandlerCallback} to update state and status
     */
    protected AbstractRioProtocol(SocketSession session, RioHandlerCallback callback) {

        if (session == null) {
            throw new IllegalArgumentException("session cannot be null");
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        _session = session;
        _session.addListener(this);
        _callback = callback;
    }

    /**
     * Sends the command and puts the thing into {@link ThingStatus#OFFLINE} if an IOException occurs
     *
     * @param command a non-null, non-empty command to send
     */
    protected void sendCommand(String command) {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }
        try {
            _session.sendCommand(command);
        } catch (IOException e) {
            _callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Exception occurred sending command: " + e);
        }
    }

    /**
     * Updates the state via the {@link RioHandlerCallback#stateChanged(String, State)}
     *
     * @param channelId the channel id to update state
     * @param newState the new state
     */
    protected void stateChanged(String channelId, State newState) {
        _callback.stateChanged(channelId, newState);
    }

    /**
     * Updates the status via {@link RioHandlerCallback#statusChanged(ThingStatus, ThingStatusDetail, String)}
     *
     * @param status the new status
     * @param statusDetail the status detail
     * @param msg the status detail message
     */
    protected void statusChanged(ThingStatus status, ThingStatusDetail statusDetail, String msg) {
        _callback.statusChanged(status, statusDetail, msg);
    }

    /**
     * Disposes of the protocol by removing ourselves from listening to the socket via
     * {@link SocketSession#removeListener(SocketSessionListener)}
     */
    public void dispose() {
        _session.removeListener(this);
    }

    /**
     * Implements the {@link SocketSessionListener#responseException(Exception)} to automatically take the thing offline
     * via {@link RioHandlerCallback#statusChanged(ThingStatus, ThingStatusDetail, String)}
     * 
     * @param e the exception
     */
    @Override
    public void responseException(Exception e) {
        _callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Exception occurred reading from the socket: " + e);
    }
}
