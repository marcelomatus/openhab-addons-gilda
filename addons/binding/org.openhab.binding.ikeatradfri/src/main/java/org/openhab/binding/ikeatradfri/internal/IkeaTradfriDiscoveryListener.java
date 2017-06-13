/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ikeatradfri.internal;

import org.eclipse.smarthome.core.thing.ThingUID;

import com.google.gson.JsonObject;

/**
 * The {@link IkeaTradfriDiscoveryListener} is notified by the bridge handler
 * with updated data from the Tradfri gateway server.
 *
 * @author Daniel Sundberg - Initial contribution
 * @author Kai Kreuzer - refactorings
 */
public interface IkeaTradfriDiscoveryListener {

    /**
     * This method is called just after the bridge thing handler fetched new data
     * from the IKEA Tradfri gateway.
     *
     * @param bridge The IKEA Tradfri gateway bridge.
     * @param data The Json object describing the new device.
     */
    public void onDeviceFound(ThingUID bridge, JsonObject data);
}
