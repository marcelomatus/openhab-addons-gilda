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
package org.openhab.binding.somfytahoma.internal.model;

import java.util.ArrayList;

/**
 * The {@link SomfyTahomaDeviceWithState} holds information about a device
 * with state bound to TahomaLink account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class SomfyTahomaDeviceWithState {

    private ArrayList<SomfyTahomaState> states;

    public boolean hasStates() {
        return states != null && states.size() > 0;
    }

    public ArrayList<SomfyTahomaState> getStates() {
        return states;
    }
}
