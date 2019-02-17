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
package org.openhab.binding.chamberlainmyq.internal;

/**
 * Enum of error codes from Chamberlain the MyQ API
 *
 * @see http
 *      ://chamberlain.custhelp.com/app/answers/detail/a_id/4922/~/what-are-the
 *      -myq-error-codes%3F
 * @author Dan Cunningham - Initial contribution
 * @author Scott Hanson - 2.x Binding
 *
 */
public enum ChamberlainMyQResponseCode {

    OK(0, "OK"),
    LOGIN_ERROR(-3333, "Login error. Please login again."), // token has expired
    ACCOUNT_INVALID(203, "The username or password you entered is incorrect. Try again."),
    ACCOUNT_NOT_FOUND(204, "The username was not found or is locked out."),
    ACCOUNT_LOCKED_PENDING(205, "This user will be locked out."),
    ACCOUNT_LOCKED(207, "This user is locked out."),
    GATEWAY_OFFLINE(223, "Gateway is Offline."),
    GATEWAY_LEARNMODE(224, "Gateway is in learn mode."),
    DEVICE_LEARNMODE(305, "The device is currently in Learn Mode."),
    DEVICE_NOT_RESPONDING(308, "The device is not responding. Please check that the device is powered and in range."),
    DEVICE_OFFLINE(309, "The gateway or hub is offline. Please check the power and network connections."),
    UNKNOWN(-1, "Unknow resonse");

    private int code;
    private String desc;

    /**
     * Creates a new Response code object from the numeric code
     *
     * @param code
     * @param desc
     */
    private ChamberlainMyQResponseCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * The human readable description of the error
     *
     * @return
     */
    public String getDesc() {
        return desc;
    }

    /**
     * The error code number from the API.
     *
     * @return
     */
    public int getCode() {
        return code;
    }

    /**
     * Return a MyQResponseCode from a given code number
     *
     * @param code
     * @return
     */
    public static ChamberlainMyQResponseCode fromCode(int code) {
        for (ChamberlainMyQResponseCode rc : values()) {
            if (rc.getCode() == code) {
                return rc;
            }
        }
        return UNKNOWN;
    }
}
