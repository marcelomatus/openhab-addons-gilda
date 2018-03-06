/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.bigassfan.internal.utils;

import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * The {@link BigAssFanConverter} is responsible for converting between
 * Dimmer values and values used for fan speed, light brightness, and
 * light color temperature.
 *
 * @author Mark Hilbush - Initial contribution
 */
public class BigAssFanConverter {
    /*
     * Convert from fan range (0-7) to dimmer range (0-100).
     */
    private static final double SPEED_CONVERSION_FACTOR = 14.2857;

    public static String percentToSpeed(PercentType command) {
        // Dimmer item will produce PercentType value, which is 0-100
        // Convert that value to what the fan expects, which is 0-7
        return String.valueOf((int) Math.round(command.doubleValue() / SPEED_CONVERSION_FACTOR));
    }

    public static PercentType speedToPercent(String speed) {
        // Fan will supply fan speed value in range of 0-7
        // Convert that value to a PercentType in range 0-100, which is what Dimmer item expects
        return new PercentType((int) Math.round(Integer.parseInt(speed) * SPEED_CONVERSION_FACTOR));
    }

    /*
     * Convert from light range (0-16) to dimmer range (0-100).
     */
    private static final double BRIGHTNESS_CONVERSION_FACTOR = 6.25;

    public static String percentToLevel(PercentType command) {
        // Dimmer item will produce PercentType value, which is 0-100
        // Convert that value to what the light expects, which is 0-16
        return String.valueOf((int) Math.round(command.doubleValue() / BRIGHTNESS_CONVERSION_FACTOR));
    }

    public static PercentType levelToPercent(String level) {
        // Light will supply brightness value in range of 0-16
        // Convert that value to a PercentType in range 0-100, which is what Dimmer item expects
        return new PercentType((int) Math.round(Integer.parseInt(level) * BRIGHTNESS_CONVERSION_FACTOR));
    }

    /*
     * Convert from hue range (2200-5000) to dimmer range (0-100).
     */
    private static final double HUE_CONVERSION_FACTOR = 28.0;

    public static String percentToHue(PercentType command) {
        // Dimmer item will produce PercentType value, which is 0-100
        // Convert that value to what the light expects, which is 2200-5000
        return String.valueOf(2200 + (int) Math.round(command.doubleValue() * HUE_CONVERSION_FACTOR));
    }

    public static PercentType hueToPercent(String hue) {
        // Light will supply hue value in range of 2200-5000
        // Convert that value to a PercentType in range 0-100, which is what Dimmer item expects
        return new PercentType((int) Math.round((Integer.parseInt(hue) - 2200) / HUE_CONVERSION_FACTOR));
    }
}
