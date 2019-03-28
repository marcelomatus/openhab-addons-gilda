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
import org.openhab.binding.domintell.internal.protocol.message.StatusMessage;
import org.openhab.binding.domintell.internal.protocol.model.Item;
import org.openhab.binding.domintell.internal.protocol.model.SerialNumber;
import org.openhab.binding.domintell.internal.protocol.model.group.ItemGroup;
import org.openhab.binding.domintell.internal.protocol.model.group.ItemGroupType;
import org.openhab.binding.domintell.internal.protocol.model.type.ItemType;
import org.openhab.binding.domintell.internal.protocol.model.type.ModuleType;

/**
* The {@link VARModule} class is the model class for Domintell variables. It doesn't make the variables discoverable
 * one-by-one but it organizes all variables into a variable type item group.
*
* @author Gabor Bicskei - Initial contribution
*/
public class VARModule extends Module {
    /**
     * Item representing the Domintell variable
     */
    private Item<Boolean> item;

    public VARModule(DomintellConnection connection, SerialNumber serialNumber) {
        super(connection, ModuleType.VAR, serialNumber);

        //add item
        ItemGroup cg = connection.getRegistry().getItemGroup(ItemGroupType.variable);
        item = addItem((Integer)null, ItemType.booleanVar, Boolean.class);
        cg.addItem(item);
        cg.notifyItemsChanged(item);
    }

    protected void updateItems(StatusMessage message) {
        String data = message.getData();
        Integer state = Integer.parseInt(data, 16);
        item.setValue(state == 1);
    }
}
