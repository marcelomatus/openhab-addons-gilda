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
package org.openhab.binding.enocean.internal.eep.D2_01;

import static org.openhab.binding.enocean.internal.EnOceanBindingConstants.CHANNEL_REPEATERMODE;

import java.util.Hashtable;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.enocean.internal.EnOceanBindingConstants;
import org.openhab.binding.enocean.internal.messages.ERP1Message;
import org.openhab.binding.enocean.internal.messages.ERP1Message.RORG;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public class D2_01_0F_NodON extends D2_01 {

    public D2_01_0F_NodON() {
        super();
    }

    public D2_01_0F_NodON(ERP1Message packet) {
        super(packet);
    }

    @Override
    protected void convertFromCommandImpl(String channelId, String channelTypeId, Command command,
            Hashtable<String, State> currentState, Configuration config) {

        if (channelId.equalsIgnoreCase(CHANNEL_REPEATERMODE)) {

            if (command instanceof RefreshType) {
                senderId = null; // make this message invalid as we do not support refresh of repeter status
            } else if (command instanceof StringType) {
                switch (((StringType) command).toString()) {
                    case EnOceanBindingConstants.REPEATERMODE_LEVEL_1:
                        setRORG(RORG.MSC).setData((byte) 0x00, (byte) 0x46, (byte) 0x08, (byte) 0x01, (byte) 0x01);
                        break;
                    case EnOceanBindingConstants.REPEATERMODE_LEVEL_2:
                        setRORG(RORG.MSC).setData((byte) 0x00, (byte) 0x46, (byte) 0x08, (byte) 0x01, (byte) 0x02);
                        break;
                    default:
                        setRORG(RORG.MSC).setData((byte) 0x00, (byte) 0x46, (byte) 0x08, (byte) 0x00, (byte) 0x00);
                }
            }
        } else {
            super.convertFromCommandImpl(channelId, channelTypeId, command, currentState, config);
        }
    }
}
