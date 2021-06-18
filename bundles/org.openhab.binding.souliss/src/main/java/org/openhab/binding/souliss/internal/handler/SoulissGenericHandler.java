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
 */
package org.openhab.binding.souliss.internal.handler;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.SoulissBindingConstants;
import org.openhab.binding.souliss.internal.SoulissProtocolConstants;
import org.openhab.binding.souliss.internal.protocol.CommonCommands;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 *
 *         This class implements the base Souliss Typical All other Typicals derive from
 *         this class
 *
 *         ...from wiki of Dario De Maio
 *         In Souliss the logics that drive your lights, curtains, LED, and
 *         others are pre-configured into so called Typicals. A Typical is a
 *         logic with a predefined set of inputs and outputs and a know
 *         behavior, are used to standardize the user interface and have a
 *         configuration-less behavior.
 *
 */

@NonNullByDefault
public abstract class SoulissGenericHandler extends BaseThingHandler implements TypicalCommonMethods {

    Thing thingGeneric;

    private final CommonCommands soulissCommands = new CommonCommands();

    private int iSlot;
    private int iNode;
    private final Logger logger = LoggerFactory.getLogger(SoulissGenericHandler.class);

    boolean bSecureSend = false; // 0 means that Secure Send is disabled
    boolean bExpectedValueSameAsSet = false; // true means that expected value is setpoint (only for T31, T19 and T6x)

    protected SoulissGenericHandler(Thing thing) {
        super(thing);
        this.thingGeneric = thing;
        int iPosNodeSlot = 2; // if uuid is of type souliss:gateway:[typical]:[node]-[slot] then node/slot is at
                              // position 2

        if (thing.getUID().getAsString().split(":").length > 3) {
            iPosNodeSlot = 3; // else, if uuid is of type souliss:gateway:[bridgeID]:[typical]:[node]-[slot] then
                              // node/slot is at position 3
        }
        try {
            iNode = Integer.parseInt(thing.getUID().toString().split(":")[iPosNodeSlot]
                    .split(SoulissBindingConstants.UUID_NODE_SLOT_SEPARATOR)[0]);
            iSlot = Integer.parseInt(thing.getUID().toString().split(":")[iPosNodeSlot]
                    .split(SoulissBindingConstants.UUID_NODE_SLOT_SEPARATOR)[1]);
        } catch (Exception e) {
            logger.debug("Item Definition Error. Use ex:'souliss:t11:nodeNumber-slotNumber'");
        }
    }

    /**
     * @return the iSlot
     */
    public int getSlot() {
        return iSlot;
    }

    /**
     * @param SoulissNode
     *            the SoulissNodeID to get
     */
    public int getNode() {
        return iNode;
    }

    /**
     * Send a command as hexadecimal, e.g.: SOULISS_T1N_ON_CMD = 0x02; short
     * SOULISS_T1N_OFF_CMD = 0x04;
     *
     * @param command
     */
    public void commandSEND(byte command) {
        soulissCommands.sendFORCEFrame(getGatewayIP(), getGatewayNodeIndex(), getGatewayUserIndex(), this.getNode(),
                this.getSlot(), command);
    }

    public void commandSendRgb(byte command, byte r, byte g, byte b) {
        soulissCommands.sendFORCEFrame(getGatewayIP(), getGatewayNodeIndex(), getGatewayUserIndex(), this.getNode(),
                this.getSlot(), command, r, g, b);
    }

    public void commandSEND(byte command, byte b1, byte b2) {
        soulissCommands.sendFORCEFrameT31SetPoint(getGatewayIP(), getGatewayNodeIndex(), getGatewayUserIndex(),
                this.getNode(), this.getSlot(), command, b1, b2);
    }

    public void commandSEND(byte b1, byte b2) {
        soulissCommands.sendFORCEFrameT61SetPoint(getGatewayIP(), getGatewayNodeIndex(), getGatewayUserIndex(),
                this.getNode(), this.getSlot(), b1, b2);
    }

    /**
     * Create a time stamp as "yyyy-MM-dd'T'HH:mm:ssz"
     *
     * @return String timestamp
     */
    private static String getTimestamp() {
        // Pattern : yyyy-MM-dd'T'HH:mm:ssz
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
        Date n = new Date();
        return sdf.format(n.getTime());
    }

    @Override
    public void thingUpdated(Thing thing) {
        updateThing(thing);
        this.thingGeneric = thing;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.BRIDGE_OFFLINE);
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Nullable
    public String getGatewayIP() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            BridgeHandler bridgeHandler = bridge.getHandler();
            if (bridgeHandler != null) {
                return ((SoulissGatewayHandler) bridgeHandler).ipAddressOnLAN;
            }
        }
        return null;
    }

    @Nullable
    public String getLabel() {
        return thingGeneric.getLabel();
    }

    public byte getGatewayUserIndex() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            SoulissGatewayHandler soulissgwHandler = (SoulissGatewayHandler) bridge.getHandler();
            if (soulissgwHandler != null) {
                return soulissgwHandler.userIndex;
            }
        }
        return 0;
    }

    public byte getGatewayNodeIndex() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            SoulissGatewayHandler soulissgwHandler = (SoulissGatewayHandler) bridge.getHandler();
            if (soulissgwHandler != null) {
                return soulissgwHandler.nodeIndex;
            }
        }
        return 0;
    }

    public void setHealthy(byte shHealthy) {
        this.updateState(SoulissBindingConstants.HEALTHY_CHANNEL, new DecimalType(shHealthy & 0xFF));
    }

    public void setLastStatusStored() {
        this.updateState(SoulissBindingConstants.LASTSTATUSSTORED_CHANNEL, DateTimeType.valueOf(getTimestamp()));
    }

    protected @Nullable OnOffType getOhStateOnOffFromSoulissVal(byte sVal) {
        if (sVal == SoulissProtocolConstants.SOULISS_T1N_ON_COIL) {
            return OnOffType.ON;
        } else if (sVal == SoulissProtocolConstants.SOULISS_T1N_OFF_COIL) {
            return OnOffType.OFF;
        } else if (sVal == SoulissProtocolConstants.SOULISS_T1N_ON_FEEDBACK) {
            return OnOffType.ON;
        } else if (sVal == SoulissProtocolConstants.SOULISS_T1N_OFF_FEEDBACK) {
            return OnOffType.OFF;
        } else if (sVal == SoulissProtocolConstants.SOULISS_T4N_NOT_ARMED) {
            return OnOffType.OFF;
        } else if (sVal == SoulissProtocolConstants.SOULISS_T4N_ARMED) {
            return OnOffType.ON;
        }

        return null;
    }

    @Nullable
    protected OpenClosedType getOhStateOpenCloseFromSoulissVal(byte sVal) {
        if (sVal == SoulissProtocolConstants.SOULISS_T1N_ON_COIL) {
            return OpenClosedType.CLOSED;
        } else if (sVal == SoulissProtocolConstants.SOULISS_T1N_OFF_COIL) {
            return OpenClosedType.OPEN;
        }
        return null;
    }
}
