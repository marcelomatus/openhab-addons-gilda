/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.bosesoundtouch.types;

import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.PrimitiveType;
import org.eclipse.smarthome.core.types.State;

/**
 * The {@link RemoteKeyType} class is holding the Keys on a remote. For simulating key presses
 *
 * @author Christian Niessner - Initial contribution
 */
public enum RemoteKeyType implements PrimitiveType, State, Command {
    PLAY,
    PAUSE,
    STOP,
    PREV_TRACK,
    NEXT_TRACK,
    THUMBS_UP,
    THUMBS_DOWN,
    BOOKMARK,
    POWER,
    MUTE,
    VOLUME_UP,
    VOLUME_DOWN,
    PRESET_1,
    PRESET_2,
    PRESET_3,
    PRESET_4,
    PRESET_5,
    PRESET_6,
    AUX_INPUT,
    SHUFFLE_OFF,
    SHUFFLE_ON,
    REPEAT_OFF,
    REPEAT_ONE,
    REPEAT_ALL,
    PLAY_PAUSE,
    ADD_FAVORITE,
    REMOVE_FAVORITE,
    INVALID_KEY;

    private String name;

    private RemoteKeyType() {
        this.name = name();
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, this.toString());
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String toFullString() {
        return toString();
    }
}