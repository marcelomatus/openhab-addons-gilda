/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
=======
 * Copyright (c) 2014-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.souliss.handler;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.SoulissBindingConstants;
import org.openhab.binding.souliss.SoulissBindingProtocolConstants;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SoulissT11Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */

@NonNullByDefault
public class SoulissT11Handler extends SoulissGenericHandler {

    private @NonNullByDefault({}) Configuration gwConfigurationMap;
    private Logger logger = LoggerFactory.getLogger(SoulissT11Handler.class);
    byte t1nRawState;
    byte xSleepTime = 0;

    public SoulissT11Handler(Thing thing) {
        super(thing);
    }

    // called on every status change or change request

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            switch (channelUID.getId()) {
                case SoulissBindingConstants.ONOFF_CHANNEL:
                    @Nullable
                    OnOffType val = getOhStateOnOffFromSoulissVal(t1nRawState);
                    if (val != null) {
                        updateState(channelUID, val);
                    }
                    break;
                default:
                    logger.debug("Unknown channel for T11 thing: {}", channelUID);
            }
        } else {
            switch (channelUID.getId()) {
                case SoulissBindingConstants.ONOFF_CHANNEL:
                    if (command instanceof OnOffType) {
                        if (command.equals(OnOffType.ON)) {
                            commandSEND(SoulissBindingProtocolConstants.SOULISS_T1N_ON_CMD);
                        } else if (command.equals(OnOffType.OFF)) {
                            commandSEND(SoulissBindingProtocolConstants.SOULISS_T1N_OFF_CMD);
                        }
                    }
                    break;

                case SoulissBindingConstants.SLEEP_CHANNEL:
                    if ((command instanceof OnOffType) && (command.equals(OnOffType.ON))) {
                        commandSEND((byte) (SoulissBindingProtocolConstants.SOULISS_T1N_TIMED + xSleepTime));
                        // set Off
                        updateState(channelUID, OnOffType.OFF);

                    }
                    break;
                default:
                    logger.debug("Unknown channel for T11 thing: {}", channelUID);

            }
        }
    }

    @Override
    public void initialize() {
        // Long running initialization should be done asynchronously in background.
        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thingGeneric does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        updateStatus(ThingStatus.ONLINE);

        gwConfigurationMap = thingGeneric.getConfiguration();
        if (gwConfigurationMap.get(SoulissBindingConstants.SLEEP_CHANNEL) != null) {
            xSleepTime = ((BigDecimal) gwConfigurationMap.get(SoulissBindingConstants.SLEEP_CHANNEL)).byteValue();
        }
        if (gwConfigurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND) != null) {
            bSecureSend = ((Boolean) gwConfigurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND)).booleanValue();
        }
    }

    @Override
    public byte getExpectedRawState(byte bCmd) {
        if (bSecureSend) {
            if (bCmd == SoulissBindingProtocolConstants.SOULISS_T1N_ON_CMD) {
                return SoulissBindingProtocolConstants.SOULISS_T1N_ON_COIL;
            } else if (bCmd == SoulissBindingProtocolConstants.SOULISS_T1N_OFF_CMD) {
                return SoulissBindingProtocolConstants.SOULISS_T1N_OFF_COIL;
            } else if (bCmd >= SoulissBindingProtocolConstants.SOULISS_T1N_TIMED) {
                // SLEEP
                return SoulissBindingProtocolConstants.SOULISS_T1N_ON_COIL;
            }
        }
        return -1;
    }

    void setState(@Nullable PrimitiveType state) {
        if (state != null) {
            updateState(SoulissBindingConstants.SLEEP_CHANNEL, OnOffType.OFF);
            this.updateState(SoulissBindingConstants.ONOFF_CHANNEL, (OnOffType) state);
        }
    }

    @Override
    public void setRawState(byte rawState) {
        // update Last Status stored time
        super.setLastStatusStored();
        // update item state only if it is different from previous
        if (t1nRawState != rawState) {
            this.setState(getOhStateOnOffFromSoulissVal(rawState));
        }
        t1nRawState = rawState;
    }

    @Override
    public byte getRawState() {
        return t1nRawState;
    }
}
