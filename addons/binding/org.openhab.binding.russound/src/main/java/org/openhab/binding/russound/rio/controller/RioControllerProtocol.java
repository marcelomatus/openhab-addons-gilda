/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.russound.rio.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.binding.russound.internal.net.SocketSession;
import org.openhab.binding.russound.internal.net.SocketSessionListener;
import org.openhab.binding.russound.rio.AbstractRioProtocol;
import org.openhab.binding.russound.rio.RioConstants;
import org.openhab.binding.russound.rio.RioHandlerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the protocol handler for the Russound controller. This handler will issue the protocol commands and will
 * process the responses from the Russound system.
 *
 * @author Tim Roberts
 * @version $Id: $Id
 *
 */
class RioControllerProtocol extends AbstractRioProtocol {
    // logger
    private Logger logger = LoggerFactory.getLogger(RioControllerProtocol.class);

    /**
     * The controller identifier
     */
    private final int _controller;

    // Protocol constants
    private final static String CTL_TYPE = "type";
    private final static String CTL_IPADDRESS = "ipAddress";
    private final static String CTL_MACADDRESS = "macAddress";

    // Response pattners
    private final Pattern RSP_CONTROLLERNOTIFICATION = Pattern.compile("^[SN] C\\[(\\d+)\\]\\.(\\w+)=\"(.*)\"$");

    /**
     * Constructs the protocol handler from given parameters
     *
     * @param controller the controller identifier
     * @param session a non-null {@link SocketSession} (may be connected or disconnected)
     * @param callback a non-null {@link RioHandlerCallback} to callback
     */
    RioControllerProtocol(int controller, SocketSession session, RioHandlerCallback callback) {
        super(session, callback);
        _controller = controller;
    }

    /**
     * Issues a get command for the controller given the keyname
     *
     * @param keyName a non-null, non-empty keyname to get
     * @throws IllegalArgumentException if name is null or an empty string
     */
    private void refreshControllerKey(String keyName) {
        if (keyName == null || keyName.trim().length() == 0) {
            throw new IllegalArgumentException("keyName cannot be null or empty");
        }
        sendCommand("GET C[" + _controller + "]." + keyName);
    }

    /**
     * Refreshes the controller IP address
     */
    void refreshControllerIpAddress() {
        refreshControllerKey(CTL_IPADDRESS);
    }

    /**
     * Refreshes the controller MAC address
     */
    void refreshControllerMacAddress() {
        refreshControllerKey(CTL_MACADDRESS);
    }

    /**
     * Refreshes the controller Model Type
     */
    void refreshControllerType() {
        refreshControllerKey(CTL_TYPE);
    }

    /**
     * Handles any controller notifications returned by the russound system
     *
     * @param m a non-null matcher
     * @param resp a possibly null, possibly empty response
     */
    private void handleControllerNotification(Matcher m, String resp) {
        if (m == null) {
            throw new IllegalArgumentException("m (matcher) cannot be null");
        }
        if (m.groupCount() == 3) {
            try {
                final int controller = Integer.parseInt(m.group(1));
                if (controller != _controller) {
                    return;
                }

                final String key = m.group(2);
                final String value = m.group(3);

                switch (key) {
                    case CTL_TYPE:
                        stateChanged(RioConstants.CHANNEL_CTLTYPE, new StringType(value));
                        break;

                    case CTL_IPADDRESS:
                        stateChanged(RioConstants.CHANNEL_CTLIPADDRESS, new StringType(value));
                        break;

                    case CTL_MACADDRESS:
                        stateChanged(RioConstants.CHANNEL_CTLMACADDRESS, new StringType(value));
                        break;

                    default:
                        logger.warn("Unknown controller notification: '{}'", resp);
                        break;
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid Controller Notification (controller not a parsable integer): '{}')", resp);
            }
        } else {
            logger.error("Invalid Controller Notification response: '{}'", resp);
        }

    }

    /**
     * Implements {@link SocketSessionListener#responseReceived(String)} to try to process the response from the
     * russound system. This response may be for other protocol handler - so ignore if we don't recognize the response.
     *
     * @param a possibly null, possibly empty response
     */
    @Override
    public void responseReceived(String response) {
        if (response == null || response == "") {
            return;
        }

        final Matcher m = RSP_CONTROLLERNOTIFICATION.matcher(response);
        if (m.matches()) {
            handleControllerNotification(m, response);
            return;
        }
    }
}
