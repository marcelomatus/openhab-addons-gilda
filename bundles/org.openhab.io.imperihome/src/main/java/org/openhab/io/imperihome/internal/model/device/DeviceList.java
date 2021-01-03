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
package org.openhab.io.imperihome.internal.model.device;

import java.util.Collection;

/**
 * Device list holder.
 *
 * @author Pepijn de Geus - Initial contribution
 */
public class DeviceList {

    private Collection<AbstractDevice> devices;

    public Collection<AbstractDevice> getDevices() {
        return devices;
    }

    public void setDevices(Collection<AbstractDevice> devices) {
        this.devices = devices;
    }

    @Override
    public String toString() {
        return "DeviceList{" + "devices=" + devices + '}';
    }
}
