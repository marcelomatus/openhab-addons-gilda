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
package org.openhab.binding.wizlighting.internal.entities;

import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * This POJO represents one Wiz Lighting Sync params
 *
 * @author Sriram Balakrishnan - Initial contribution
 *
 */
public class SyncResponseParam implements Param {
    public String mac;
    public boolean state;
    public int sceneId;
    public boolean play;
    public int speed;
    public int r;
    public int g;
    public int b;
    public int c;
    public int w;
    public int dimming;
    public int temp;
    public int rssi;

    public PercentType getTemperatureColor() {
        return new PercentType((temp - 2200) / (6500 - 2200) * 100);
    }
}
