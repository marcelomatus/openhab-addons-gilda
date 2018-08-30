/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enocean.internal.eep.D2_01;

import static org.openhab.binding.enocean.EnOceanBindingConstants.*;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.enocean.EnOceanBindingConstants;
import org.openhab.binding.enocean.internal.messages.ERP1Message;
import org.openhab.binding.enocean.internal.messages.ERP1Message.RORG;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public class D2_01_09_Permundo extends D2_01 {

    public D2_01_09_Permundo() {
        super();
    }

    public D2_01_09_Permundo(ERP1Message packet) {
        super(packet);
    }

    @Override
    protected void convertFromCommandImpl(Command command, String channelId, State currentState, Configuration config) {

        if (channelId.equals(CHANNEL_REPEATERMODE)) {
            setRepeaterMode(command);
        } else if (channelId.equals(CHANNEL_ECOMODE)) {
            setEcoMode(command);
        } else {
            super.convertFromCommandImpl(command, channelId, currentState, config);
        }
    }

    private void setRepeaterMode(Command command) {
        if (command == RefreshType.REFRESH) {
            senderId = null; // make this message invalid as we do not support refresh of repeater status
        } else if (command instanceof StringType) {
            switch (((StringType) command).toString()) {
                case EnOceanBindingConstants.REPEATERMODE_LEVEL_1:
                    setRORG(RORG.MSC).setData((byte) 0x03, (byte) 0x35, (byte) 0x01);
                    break;
                case EnOceanBindingConstants.REPEATERMODE_LEVEL_2:
                    setRORG(RORG.MSC).setData((byte) 0x03, (byte) 0x35, (byte) 0x02);
                    break;
                default:
                    setRORG(RORG.MSC).setData((byte) 0x03, (byte) 0x35, (byte) 0x00);
            }
        }
    }

    private void setEcoMode(Command command) {
        if (command == RefreshType.REFRESH) {
            senderId = null; // make this message invalid as we do not support refresh of ecomode status
        } else if (command instanceof OnOffType) {
            if (((OnOffType) command) == OnOffType.ON) {
                setRORG(RORG.MSC).setData((byte) 0x03, (byte) 0x36, (byte) 0x01);
            } else {
                setRORG(RORG.MSC).setData((byte) 0x03, (byte) 0x36, (byte) 0x00);
            }
        }
    }
}
