/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.milight.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Contains a led bulb state including the HSB value, white color temperature and animation values.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class MilightThingState {
    public int animationMode;
    public int colorTemperature; // only for led bulbs which include white leds applicable
    public int brightness;
    public int hue360; // only for rgb(w) leds applicable (v3+)
    public int saturation; // only for rgbww leds applicable (v6+)

    public MilightThingState() {
        reset();
    }

    public void reset() {
        animationMode = 0;
        colorTemperature = 100; // only for led bulbs which include white leds applicable
        brightness = 100;
        hue360 = 180; // only for rgb(w) leds applicable (v3+)
        saturation = 100; // only for rgbww leds applicable (v6+)
    }
}
