/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.bosesoundtouch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link BoseSoundTouchBindinConstantsg} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Christian Niessner - Initial contribution
 * @author Thomas Traunbauer
 */
public class BoseSoundTouchBindingConstants {

    public static final String BINDING_ID = "bosesoundtouch";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");

    // all thing types
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>(
            Arrays.asList(THING_TYPE_DEVICE));

    // List of all Channel ids
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_VOLUME = "volume";
    public static final String CHANNEL_MUTE = "mute";
    public static final String CHANNEL_OPERATIONMODE = "operationMode";
    public static final String CHANNEL_ZONE_INFO = "zoneInfo";
    public static final String CHANNEL_PLAYER_CONTROL = "playerControl";
    public static final String CHANNEL_ZONE_ADD = "zoneAdd";
    public static final String CHANNEL_ZONE_REMOVE = "zoneRemove";
    public static final String CHANNEL_PRESET = "preset";
    public static final String CHANNEL_PRESET_CONTROL = "presetControl";
    public static final String CHANNEL_BASS = "bass";
    public static final String CHANNEL_RATEENABLED = "rateEnabled";
    public static final String CHANNEL_SKIPENABLED = "skipEnabled";
    public static final String CHANNEL_SKIPPREVIOUSENABLED = "skipPreviousEnabled";
    public static final String CHANNEL_SAVE_AS_PRESET = "saveAsPreset";
    public static final String CHANNEL_KEY_CODE = "keyCode";
    public static final String CHANNEL_NOWPLAYING_ALBUM = "nowPlayingAlbum";
    public static final String CHANNEL_NOWPLAYING_ARTWORK = "nowPlayingArtwork";
    public static final String CHANNEL_NOWPLAYING_ARTIST = "nowPlayingArtist";
    public static final String CHANNEL_NOWPLAYING_DESCRIPTION = "nowPlayingDescription";
    public static final String CHANNEL_NOWPLAYING_GENRE = "nowPlayingGenre";
    public static final String CHANNEL_NOWPLAYING_ITEMNAME = "nowPlayingItemName";
    public static final String CHANNEL_NOWPLAYING_STATIONLOCATION = "nowPlayingStationLocation";
    public static final String CHANNEL_NOWPLAYING_STATIONNAME = "nowPlayingStationName";
    public static final String CHANNEL_NOWPLAYING_TRACK = "nowPlayingTrack";

    // Device configuration parameters;
    public static final String DEVICE_PARAMETER_HOST = "DEVICE_HOST";
    public static final String DEVICE_PARAMETER_MAC = "DEVICE_MAC";

    // Device information parameters;
    public static final String DEVICE_INFO_NAME = "INFO_NAME";
    public static final String DEVICE_INFO_TYPE = "INFO_TYPE";
}
