/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.freebox.internal.api.model;

import org.openhab.binding.freebox.internal.api.RequestAnnotation;

/**
 * The {@link LcdConfig} is the Java class used to map the "LcdConfig"
 * structure used by the LCD configuration API
 * https://dev.freebox.fr/sdk/os/lcd/#
 *
 * @author Laurent Garnier - Initial contribution
 */
@RequestAnnotation(responseClass = LcdConfigResponse.class, relativeUrl = "lcd/config/", retryAuth = true, method = "PUT")
public class LcdConfig {
    private int brightness;
    private boolean orientationForced;
    private int orientation;

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = Math.max(0, Math.min(100, brightness));
    }

    public boolean isOrientationForced() {
        return orientationForced;
    }

    public void setOrientationForced(boolean orientationForced) {
        this.orientationForced = orientationForced;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        int newValue = Math.min(360, orientation);
        newValue = Math.max(newValue, 0);
        this.orientation = newValue;
        this.orientationForced = true;
    }
}
