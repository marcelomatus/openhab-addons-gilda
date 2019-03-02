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
package org.openhab.binding.loxone.internal.core;

/**
 * Reasons why Miniserver may be not reachable
 *
 * @author Pawel Pieczul - initial contribution
 *
 */
public enum LxOfflineReason {
    /**
     * No reason at all - should be reachable
     */
    NONE,
    /**
     * User name or password incorrect or user not authorized
     */
    UNAUTHORIZED,
    /**
     * Too many failed login attempts and server's temporary ban of the user
     */
    TOO_MANY_FAILED_LOGIN_ATTEMPTS,
    /**
     * Communication error with the Miniserv
     */
    COMMUNICATION_ERROR,
    /**
     * Timeout of user authentication procedure
     */
    AUTHENTICATION_TIMEOUT,
    /**
     * No activity from Miniserver's client
     */
    IDLE_TIMEOUT,
    /**
     * Internal error, sign of something wrong with the program
     */
    INTERNAL_ERROR,
    /**
     * Connection attempt failed (before authentication)
     */
    CONNECT_FAILED;

    /**
     * Converts Miniserver status code to offline reason
     *
     * @param code
     *                 status code received in message response from the Miniserver
     * @return
     *         converted offline reason
     */
    static LxOfflineReason getReason(int code) {
        switch (code) {
            case 420:
                return LxOfflineReason.AUTHENTICATION_TIMEOUT;
            case 401:
                return LxOfflineReason.UNAUTHORIZED;
            case 4003:
                return LxOfflineReason.TOO_MANY_FAILED_LOGIN_ATTEMPTS;
            case 1001:
                return LxOfflineReason.IDLE_TIMEOUT;
            case 200:
                return LxOfflineReason.NONE;
            default:
                return COMMUNICATION_ERROR;
        }
    }
}
