/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enocean.internal.eep.F6_02;

import static org.openhab.binding.enocean.internal.EnOceanBindingConstants.*;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.CommonTriggerEvents;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.enocean.internal.config.EnOceanChannelRockerSwitchConfigBase.Channel;
import org.openhab.binding.enocean.internal.config.EnOceanChannelVirtualRockerSwitchConfig;
import org.openhab.binding.enocean.internal.eep.Base._RPSMessage;
import org.openhab.binding.enocean.internal.messages.ERP1Message;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public class F6_02_02 extends _RPSMessage {

    final byte AI = 0;
    final byte A0 = 1;
    final byte BI = 2;
    final byte B0 = 3;
    final byte PRESSED = 16;

    public F6_02_02() {
        super();
    }

    public F6_02_02(ERP1Message packet) {
        super(packet);
    }

    @Override
    protected String convertToEventImpl(String channelId, String lastEvent, Configuration config) {
        if (!isValid()) {
            return null;
        }

        if (t21 && nu) {

            switch (channelId) {
                case CHANNEL_ROCKERSWITCH_CHANNELA:
                    if ((bytes[0] >>> 5) == AI) {
                        return ((bytes[0] & PRESSED) != 0) ? CommonTriggerEvents.DIR1_PRESSED
                                : CommonTriggerEvents.DIR1_RELEASED;
                    } else if ((bytes[0] >>> 5) == A0) {
                        return ((bytes[0] & PRESSED) != 0) ? CommonTriggerEvents.DIR2_PRESSED
                                : CommonTriggerEvents.DIR2_RELEASED;
                    }
                    return null;

                case CHANNEL_ROCKERSWITCH_CHANNELB:
                    if ((bytes[0] >>> 5) == BI) {
                        return ((bytes[0] & PRESSED) != 0) ? CommonTriggerEvents.DIR1_PRESSED
                                : CommonTriggerEvents.DIR1_RELEASED;
                    } else if ((bytes[0] >>> 5) == B0) {
                        return ((bytes[0] & PRESSED) != 0) ? CommonTriggerEvents.DIR2_PRESSED
                                : CommonTriggerEvents.DIR2_RELEASED;
                    }
                    return null;
            }
        } else if (t21 && !nu) {
            if (lastEvent != null && lastEvent.equals(CommonTriggerEvents.DIR1_PRESSED)) {
                return CommonTriggerEvents.DIR1_RELEASED;
            } else if (lastEvent != null && lastEvent.equals(CommonTriggerEvents.DIR2_PRESSED)) {
                return CommonTriggerEvents.DIR2_RELEASED;
            }
        }

        return null;
    }

    @Override
    protected State convertToStateImpl(String channelId, State currentState, Configuration config) {
        if (!isValid()) {
            return UnDefType.UNDEF;
        }

        if (t21 && nu) {
            EnOceanChannelVirtualRockerSwitchConfig c = config.as(EnOceanChannelVirtualRockerSwitchConfig.class);
            if (c != null) {
                byte dir1 = c.getChannel() == Channel.ChannelA ? AI : BI;
                byte dir2 = c.getChannel() == Channel.ChannelB ? A0 : B0;

                // We are just listening on the pressed event here
                switch (c.getSwitchMode()) {
                    case RockerSwitch:
                        if ((bytes[0] >>> 5) == dir1) {
                            if (((bytes[0] & PRESSED) != 0)) {
                                return channelId.equals(CHANNEL_ROCKERSWITCHLISTENERSWITCH) ? OnOffType.ON
                                        : UpDownType.UP;
                            }
                        } else if ((bytes[0] >>> 5) == dir2) {
                            if (((bytes[0] & PRESSED) != 0)) {
                                return channelId.equals(CHANNEL_ROCKERSWITCHLISTENERSWITCH) ? OnOffType.OFF
                                        : UpDownType.DOWN;
                            }
                        }
                    case ToggleDir1:
                        if ((bytes[0] >>> 5) == dir1) {
                            if (((bytes[0] & PRESSED) != 0)) {
                                return channelId.equals(CHANNEL_ROCKERSWITCHLISTENERSWITCH)
                                        ? inverse((OnOffType) currentState)
                                        : inverse((UpDownType) currentState);
                            }
                        }
                    case ToggleDir2:
                        if ((bytes[0] >>> 5) == dir2) {
                            if (((bytes[0] & PRESSED) != 0)) {
                                return channelId.equals(CHANNEL_ROCKERSWITCHLISTENERSWITCH)
                                        ? inverse((OnOffType) currentState)
                                        : inverse((UpDownType) currentState);
                            }
                        }
                    default:
                        break;
                }
            }
        }

        return UnDefType.UNDEF;
    }

    private State inverse(OnOffType currentState) {
        return currentState == OnOffType.ON ? OnOffType.OFF : OnOffType.ON;
    }

    private State inverse(UpDownType currentState) {
        return currentState == UpDownType.UP ? UpDownType.DOWN : UpDownType.UP;
    }
}
