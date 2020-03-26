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
package org.openhab.binding.hue.internal;

import java.util.Arrays;

/**
 * Current state of light.
 *
 * @author Q42 - Initial contribution
 * @author Denis Dudnik - moved Jue library source code inside the smarthome Hue binding
 */
public class State {
    private boolean on;
    int bri;
    int hue;
    int sat;
    private float[] xy;
    private int ct;
    private String alert;
    private String effect;
    String colormode;
    private boolean reachable;

    State() {
    }

    /**
     * Color modes of a light.
     */
    public enum ColorMode {
        /**
         * CIE color space coordinates
         */
        XY,

        /**
         * Hue and saturation
         */
        HS,

        /**
         * Color temperature in mirek
         */
        CT
    }

    /**
     * Alert modes of a light.
     */
    public enum AlertMode {
        /**
         * Light is not performing alert effect
         */
        NONE,

        /**
         * Light is performing one breathe cycle
         */
        SELECT,

        /**
         * Light is performing breathe cycles for 30 seconds (unless cancelled)
         */
        LSELECT
    }

    /**
     * Effects possible for a light.
     */
    public enum Effect {
        /**
         * No effect
         */
        NONE,

        /**
         * Cycle through all hues with current saturation and brightness
         */
        COLORLOOP
    }

    /**
     * Returns the on state.
     *
     * @return true if the light is on, false if it isn't
     */
    public boolean isOn() {
        return on;
    }

    /**
     * Returns the brightness.
     *
     * @return brightness
     */
    public int getBrightness() {
        return bri;
    }

    /**
     * Returns the hue.
     *
     * @return hue
     */
    public int getHue() {
        return hue;
    }

    /**
     * Returns the saturation.
     *
     * @return saturation
     */
    public int getSaturation() {
        return sat;
    }

    /**
     * Returns the coordinates in CIE color space.
     *
     * @return cie color spaces coordinates
     */
    public float[] getXY() {
        return xy;
    }

    /**
     * Returns the color temperature.
     *
     * @return color temperature
     */
    public int getColorTemperature() {
        return ct;
    }

    /**
     * Returns the last alert mode set.
     * Future firmware updates may change this to actually report the current alert mode.
     *
     * @return last alert mode
     */
    public AlertMode getAlertMode() {
        if (alert == null) {
            return null;
        }
        return AlertMode.valueOf(alert.toUpperCase());
    }

    /**
     * Returns the current color mode.
     *
     * @return current color mode
     */
    public ColorMode getColorMode() {
        if (colormode == null) {
            return null;
        }
        return ColorMode.valueOf(colormode.toUpperCase());
    }

    /**
     * Returns the current active effect.
     *
     * @return current active effect
     */
    public Effect getEffect() {
        if (effect == null) {
            return null;
        }
        return Effect.valueOf(effect.toUpperCase());
    }

    /**
     * Returns reachability.
     *
     * @return true if reachable, false if it isn't
     */
    public boolean isReachable() {
        return reachable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        State other = (State) obj;
        if (alert == null) {
            if (other.alert != null)
                return false;
        } else if (!alert.equals(other.alert))
            return false;
        if (bri != other.bri)
            return false;
        if (colormode == null) {
            if (other.colormode != null)
                return false;
        } else if (!colormode.equals(other.colormode))
            return false;
        if (ct != other.ct)
            return false;
        if (effect == null) {
            if (other.effect != null)
                return false;
        } else if (!effect.equals(other.effect))
            return false;
        if (hue != other.hue)
            return false;
        if (on != other.on)
            return false;
        if (reachable != other.reachable)
            return false;
        if (sat != other.sat)
            return false;
        if (!Arrays.equals(xy, other.xy))
            return false;
        return true;
    }
}
