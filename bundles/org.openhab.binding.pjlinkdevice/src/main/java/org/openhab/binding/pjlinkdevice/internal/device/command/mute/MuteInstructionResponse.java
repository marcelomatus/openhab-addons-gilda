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
package org.openhab.binding.pjlinkdevice.internal.device.command.mute;

import java.util.Arrays;
import java.util.HashSet;

import org.openhab.binding.pjlinkdevice.internal.device.command.AcknowledgeResponseValue;
import org.openhab.binding.pjlinkdevice.internal.device.command.ErrorCode;
import org.openhab.binding.pjlinkdevice.internal.device.command.PrefixedResponse;
import org.openhab.binding.pjlinkdevice.internal.device.command.ResponseException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The response part of {@link MuteInstructionCommand}
 *
 * @author Nils Schnabel - Initial contribution
 */
@NonNullByDefault
public class MuteInstructionResponse extends PrefixedResponse<AcknowledgeResponseValue> {
  private final static HashSet<ErrorCode> SPECIFIED_ERRORCODES = new HashSet<ErrorCode>(
      Arrays.asList(ErrorCode.OUT_OF_PARAMETER, ErrorCode.UNAVAILABLE_TIME, ErrorCode.DEVICE_FAILURE));

  public MuteInstructionResponse(String response) throws ResponseException {
    super("AVMT=", SPECIFIED_ERRORCODES, response);
  }

  @Override
  protected AcknowledgeResponseValue parseResponseWithoutPrefix(String responseWithoutPrefix) throws ResponseException {
    return AcknowledgeResponseValue.getValueForCode(responseWithoutPrefix);
  }
}
