/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enocean.internal.eep.D2_05;

import static org.openhab.binding.enocean.EnOceanBindingConstants.*;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.enocean.internal.eep.Base._VLDMessage;
import org.openhab.binding.enocean.internal.messages.ERP1Message;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public class D2_05_00 extends _VLDMessage {

    protected final byte cmdMask = 0x0f;
    protected final byte outputValueMask = 0x7f;
    protected final byte outputChannelMask = 0x1f;

    protected final byte CMD_ACTUATOR_SET_POSITION = 0x01;
    protected final byte CMD_ACTUATOR_STOP = 0x02;
    protected final byte CMD_ACTUATOR_POSITION_QUERY = 0x03;
    protected final byte CMD_ACTUATOR_POSITION_RESPONE = 0x04;

    protected final byte AllChannels_Mask = 0x1e;
    protected final byte ChannelA_Mask = 0x00;

    protected final byte DOWN = 0x64; // 100%
    protected final byte UP = 0x00; // 0%

    public D2_05_00() {
        super();
    }

    public D2_05_00(ERP1Message packet) {
        super(packet);
    }

    protected byte getCMD() {
        return (byte) (bytes[bytes.length - 1] & cmdMask);
    }

    protected void setPositionData(Command command, byte outputChannel) {

        if (command instanceof UpDownType) {
            if (command == UpDownType.DOWN) {
                setData(DOWN, (byte) 0x00, (byte) 0x00, (byte) (outputChannel + CMD_ACTUATOR_SET_POSITION));
            } else {
                setData(UP, (byte) 0x00, (byte) 0x00, (byte) (outputChannel + CMD_ACTUATOR_SET_POSITION));
            }
        } else if (command instanceof StopMoveType) {
            if (command == StopMoveType.STOP) {
                setData((byte) (outputChannel + CMD_ACTUATOR_STOP));
            }
        } else if (command instanceof PercentType) {
            setData((byte) (((PercentType) command).intValue()), (byte) 0x00, (byte) 0x00,
                    (byte) (outputChannel + CMD_ACTUATOR_SET_POSITION));
        }
    }

    protected void setPositionQueryData(byte outputChannel) {
        setData((byte) (outputChannel + CMD_ACTUATOR_POSITION_QUERY));
    }

    protected State getPositionData() {
        if (getCMD() == CMD_ACTUATOR_POSITION_RESPONE) {
            if (bytes[0] != 127) {
                return new PercentType(bytes[0] & 0x7f);
            }
        }

        return UnDefType.UNDEF;
    }

    protected byte getChannel() {
        return (byte) (bytes[1] & outputChannelMask);
    }

    @Override
    public void addConfigPropertiesTo(DiscoveryResultBuilder discoveredThingResultBuilder) {
        discoveredThingResultBuilder.withProperty(PARAMETER_EEPID, getEEPType().getId());
    }

    @Override
    protected void convertFromCommandImpl(Command command, String channelId, State currentState, Configuration config) {
        if (!getEEPType().GetChannelIds().contains(channelId)) {
            return;
        }

        if (channelId.equals(CHANNEL_ROLLERSHUTTER)) {
            if (command == RefreshType.REFRESH) {
                setPositionQueryData(ChannelA_Mask);
            } else {
                setPositionData(command, ChannelA_Mask);
            }
        }
    }

    @Override
    protected State convertToStateImpl(String channelId, State currentState, Configuration config) {

        switch (channelId) {
            case CHANNEL_ROLLERSHUTTER:
                return getPositionData();
        }

        return UnDefType.UNDEF;
    }

}
