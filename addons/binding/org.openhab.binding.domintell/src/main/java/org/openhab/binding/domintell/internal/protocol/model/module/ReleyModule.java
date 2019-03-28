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
package org.openhab.binding.domintell.internal.protocol.model.module;

import org.openhab.binding.domintell.internal.protocol.DomintellConnection;
import org.openhab.binding.domintell.internal.protocol.message.ActionMessageBuilder;
import org.openhab.binding.domintell.internal.protocol.message.StatusMessage;
import org.openhab.binding.domintell.internal.protocol.model.SerialNumber;
import org.openhab.binding.domintell.internal.protocol.model.type.ActionType;
import org.openhab.binding.domintell.internal.protocol.model.type.ItemType;
import org.openhab.binding.domintell.internal.protocol.model.type.ModuleType;

/**
* The {@link ReleyModule} class is a base class for all relay type Domintell modules
*
* @author Gabor Bicskei - Initial contribution
*/
public abstract class ReleyModule extends Module {
    ReleyModule(DomintellConnection connection, ModuleType type, SerialNumber serialNumber, int inputNum) {
        super(connection, type, serialNumber);

        //add channels
        for (int i = 0; i < inputNum; i++) {
            addItem(i + 1, ItemType.output, Boolean.class);
        }
    }

    protected void updateItems(StatusMessage message) {
        updateBooleanItems(message);
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    /**
     * Set boolean value
     *
     * @param idx Output index
     */
    public void setOutput(int idx) {
        getConnection().sendCommand(ActionMessageBuilder.create()
                .withModuleKey(getModuleKey())
                .withIONumber(idx)
                .withAction(ActionType.SET_OUTPUT)
                .build());
    }

    /**
     * Reset boolean output
     *
     * @param idx Output index
     */
    public void resetOutput(int idx) {
        getConnection().sendCommand(ActionMessageBuilder.create()
                .withModuleKey(getModuleKey())
                .withIONumber(idx)
                .withAction(ActionType.RESET_OUTPUT)
                .build());
    }
}
