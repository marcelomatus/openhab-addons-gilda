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
package org.openhab.binding.pjlinkdevice.internal.device.command.errorstatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.openhab.binding.pjlinkdevice.internal.device.command.ErrorCode;
import org.openhab.binding.pjlinkdevice.internal.device.command.PrefixedResponse;
import org.openhab.binding.pjlinkdevice.internal.device.command.ResponseException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Nils Schnabel - Initial contribution
 */
@NonNullByDefault
public class ErrorStatusQueryResponse extends PrefixedResponse<Map<ErrorStatusQueryResponse.ErrorStatusDevicePart, ErrorStatusQueryResponse.ErrorStatusQueryResponseState>> {

    public enum ErrorStatusQueryResponseState {
        OK_UNKOWN("OK/no failure detection", "0"),
        WARNING("Warning", "1"),
        ERROR("Error", "2");

        private String text;
        private String code;

        private ErrorStatusQueryResponseState(String text, String code) {
            this.text = text;
            this.code = code;
        }

        public String getText() {
            return this.text;
        }

        public static ErrorStatusQueryResponseState parseString(String code) throws ResponseException {
            for (ErrorStatusQueryResponseState result : ErrorStatusQueryResponseState.values()) {
                if (result.code.equals(code)) {
                    return result;
                }
            }

            throw new ResponseException("Cannot understand status: " + code);
        }
    }

    public enum ErrorStatusDevicePart {
        FAN("Fan error", "FanError", 0),
        LAMP("Lamp error", "LampError", 1),
        TEMPERATURE("Temperature error", "TemperatureError", 2),
        COVER_OPEN("Cover open error", "CoverOpenError", 3),
        FILTER("Filter error", "FilterError", 4),
        OTHER("Other errors", "OtherErrors", 5);

        private String text;
        private String camelCaseText;
        private int positionInResponse;

        private ErrorStatusDevicePart(String text, String camelCaseText, int positionInResponse) {
            this.text = text;
            this.camelCaseText = camelCaseText;
            this.positionInResponse = positionInResponse;
        }

        public String getText() {
            return this.text;
        }

        public String getCamelCaseText() {
            return this.camelCaseText;
        }

        public static ErrorStatusDevicePart getDevicePartByResponsePosition(int pos) {
            for (ErrorStatusDevicePart result : ErrorStatusDevicePart.values()) {
                if (result.positionInResponse == pos) {
                    return result;
                }
            }

            return OTHER;
        }
    }

    public ErrorStatusQueryResponse(String response) throws ResponseException {
        super("ERST=", new HashSet<ErrorCode>(
                Arrays.asList(new ErrorCode[] { ErrorCode.UNAVAILABLE_TIME, ErrorCode.DEVICE_FAILURE })), response);
    }

    @Override
    protected Map<ErrorStatusDevicePart, ErrorStatusQueryResponseState> parse0(String responseWithoutPrefix) throws ResponseException {
        Map<ErrorStatusDevicePart, ErrorStatusQueryResponseState> result = new HashMap<ErrorStatusDevicePart, ErrorStatusQueryResponseState>();
        for (int i = 0; i < ErrorStatusDevicePart.values().length; i++) {
            result.put(ErrorStatusDevicePart.getDevicePartByResponsePosition(i),
                    ErrorStatusQueryResponseState.parseString(responseWithoutPrefix.substring(i, i + 1)));
        }
        return result;
    }

}
