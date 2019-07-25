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
package org.openhab.binding.pjlinkdevice.internal.device.command.input;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.openhab.binding.pjlinkdevice.internal.device.command.ErrorCode;
import org.openhab.binding.pjlinkdevice.internal.device.command.PrefixedResponse;
import org.openhab.binding.pjlinkdevice.internal.device.command.ResponseException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Nils Schnabel - Initial contribution
 */
@NonNullByDefault
public class InputListQueryResponse extends PrefixedResponse<Set<Input>> {
    public InputListQueryResponse(String response) throws ResponseException {
        super("INST=", new HashSet<ErrorCode>(
                Arrays.asList(new ErrorCode[] { ErrorCode.UNAVAILABLE_TIME, ErrorCode.DEVICE_FAILURE })), response);
    }

    @Override
    protected Set<Input> parseResponseWithoutPrefix(String responseWithoutPrefix) throws ResponseException {
        Set<Input> result = new HashSet<Input>();
        int pos = 0;
        while (pos < responseWithoutPrefix.length()) {
            result.add(new Input(responseWithoutPrefix.substring(pos, pos + 2)));
            pos += 3;
        }
        return result;
    }

}
