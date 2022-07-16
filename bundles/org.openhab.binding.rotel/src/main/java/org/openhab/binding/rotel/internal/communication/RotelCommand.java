/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.rotel.internal.communication;

import static org.openhab.binding.rotel.internal.RotelBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rotel.internal.RotelException;

/**
 * Represents the different kinds of commands
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public enum RotelCommand {

    POWER_TOGGLE("Power Toggle", PRIMARY_CMD, (byte) 0x0A, "power_toggle", "power_toggle"),
    POWER_OFF("Power Off", PRIMARY_CMD, (byte) 0x4A, "power_off", "power_off"),
    POWER_ON("Power On", PRIMARY_CMD, (byte) 0x4B, "power_on", "power_on"),
    POWER("Request current power status", "get_current_power", "power?"),
    ZONE_SELECT("Zone Select", PRIMARY_CMD, (byte) 0x23),
    MAIN_ZONE_POWER_TOGGLE("Main Zone Power Toggle", MAIN_ZONE_CMD, (byte) 0x0A),
    MAIN_ZONE_POWER_OFF("Main Zone Power Off", MAIN_ZONE_CMD, (byte) 0x4A),
    MAIN_ZONE_POWER_ON("Main Zone Power On", MAIN_ZONE_CMD, (byte) 0x4B),
    ZONE2_POWER_TOGGLE("Zone 2 Power Toggle", ZONE2_CMD, (byte) 0x0A),
    ZONE2_POWER_OFF("Zone 2 Power Off", ZONE2_CMD, (byte) 0x4A),
    ZONE2_POWER_ON("Zone 2 Power On", ZONE2_CMD, (byte) 0x4B),
    ZONE3_POWER_TOGGLE("Zone 3 Power Toggle", ZONE3_CMD, (byte) 0x0A),
    ZONE3_POWER_OFF("Zone 3 Power Off", ZONE3_CMD, (byte) 0x4A),
    ZONE3_POWER_ON("Zone 3 Power On", ZONE3_CMD, (byte) 0x4B),
    ZONE4_POWER_TOGGLE("Zone 4 Power Toggle", ZONE4_CMD, (byte) 0x0A),
    ZONE4_POWER_OFF("Zone 4 Power Off", ZONE4_CMD, (byte) 0x4A),
    ZONE4_POWER_ON("Zone 4 Power On", ZONE4_CMD, (byte) 0x4B),
    VOLUME_UP("Volume Up", PRIMARY_CMD, (byte) 0x0B, "volume_up", "vol_up"),
    VOLUME_DOWN("Volume Down", PRIMARY_CMD, (byte) 0x0C, "volume_down", "vol_dwn"),
    VOLUME_SET("Set Volume to level", VOLUME_CMD, (byte) 0, "volume_", "vol_"),
    VOLUME_GET("Request current volume level", "get_volume", "volume?"),
    VOLUME_GET_MIN("Request Min volume level", "get_volume_min", null),
    VOLUME_GET_MAX("Request Max volume level", "get_volume_max", null),
    MUTE_TOGGLE("Mute Toggle", PRIMARY_CMD, (byte) 0x1E, "mute", "mute"),
    MUTE_ON("Mute On", "mute_on", "mute_on"),
    MUTE_OFF("Mute Off", "mute_off", "mute_off"),
    MUTE("Request current mute status", "get_mute_status", "mute?"),
    MAIN_ZONE_VOLUME_UP("Main Zone Volume Up", MAIN_ZONE_CMD, (byte) 0),
    MAIN_ZONE_VOLUME_DOWN("Main Zone Volume Down", MAIN_ZONE_CMD, (byte) 1),
    MAIN_ZONE_MUTE_TOGGLE("Main Zone Mute Toggle", MAIN_ZONE_CMD, (byte) 0x1E),
    MAIN_ZONE_MUTE_ON("Main Zone Mute On", MAIN_ZONE_CMD, (byte) 0x6C),
    MAIN_ZONE_MUTE_OFF("Main Zone Mute Off", MAIN_ZONE_CMD, (byte) 0x6D),
    ZONE1_VOLUME_UP("Zone 1 Volume Up", null, "z1:vol_up"),
    ZONE1_VOLUME_DOWN("Zone 1 Volume Down", null, "z1:vol_dwn"),
    ZONE1_VOLUME_SET("Set Zone 1 Volume to level", null, "z1:vol_"),
    ZONE1_MUTE_TOGGLE("Zone 1 Mute Toggle", null, "z1:mute"),
    ZONE1_MUTE_ON("Zone 1 Mute On", null, "z1:mute_on"),
    ZONE1_MUTE_OFF("Zone 1 Mute Off", null, "z1:mute_off"),
    ZONE2_VOLUME_UP("Zone 2 Volume Up", ZONE2_CMD, (byte) 0, null, "z2:vol_up"),
    ZONE2_VOLUME_DOWN("Zone 2 Volume Down", ZONE2_CMD, (byte) 1, null, "z2:vol_dwn"),
    ZONE2_VOLUME_SET("Set Zone 2 Volume to level", ZONE2_VOLUME_CMD, (byte) 0, null, "z2:vol_"),
    ZONE2_MUTE_TOGGLE("Zone 2 Mute Toggle", ZONE2_CMD, (byte) 0x1E, null, "z2:mute"),
    ZONE2_MUTE_ON("Zone 2 Mute On", ZONE2_CMD, (byte) 0x6C, null, "z2:mute_on"),
    ZONE2_MUTE_OFF("Zone 2 Mute Off", ZONE2_CMD, (byte) 0x6D, null, "z2:mute_off"),
    ZONE3_VOLUME_UP("Zone 3 Volume Up", ZONE3_CMD, (byte) 0, null, "z3:vol_up"),
    ZONE3_VOLUME_DOWN("Zone 3 Volume Down", ZONE3_CMD, (byte) 1, null, "z3:vol_dwn"),
    ZONE3_VOLUME_SET("Set Zone 3 Volume to level", ZONE3_VOLUME_CMD, (byte) 0, null, "z3:vol_"),
    ZONE3_MUTE_TOGGLE("Zone 3 Mute Toggle", ZONE3_CMD, (byte) 0x1E, null, "z3:mute"),
    ZONE3_MUTE_ON("Zone 3 Mute On", ZONE3_CMD, (byte) 0x6C, null, "z3:mute_on"),
    ZONE3_MUTE_OFF("Zone 3 Mute Off", ZONE3_CMD, (byte) 0x6D, null, "z3:mute_off"),
    ZONE4_VOLUME_UP("Zone 4 Volume Up", ZONE4_CMD, (byte) 0, null, "z4:vol_up"),
    ZONE4_VOLUME_DOWN("Zone 4 Volume Down", ZONE4_CMD, (byte) 1, null, "z4:vol_dwn"),
    ZONE4_VOLUME_SET("Set Zone 4 Volume to level", ZONE4_VOLUME_CMD, (byte) 0, null, "z4:vol_"),
    ZONE4_MUTE_TOGGLE("Zone 4 Mute Toggle", ZONE4_CMD, (byte) 0x1E, null, "z4:mute"),
    ZONE4_MUTE_ON("Zone 4 Mute On", ZONE4_CMD, (byte) 0x6C, null, "z4:mute_on"),
    ZONE4_MUTE_OFF("Zone 4 Mute Off", ZONE4_CMD, (byte) 0x6D, null, "z4:mute_off"),
    SOURCE_CD("Source CD", PRIMARY_CMD, (byte) 0x02, "cd", "cd"),
    SOURCE_TUNER("Source Tuner", PRIMARY_CMD, (byte) 0x03, "tuner", "tuner"),
    SOURCE_TAPE("Source Tape", PRIMARY_CMD, (byte) 0x04, "tape", "tape"),
    SOURCE_VIDEO1("Source Video 1", PRIMARY_CMD, (byte) 0x05, "video1", "video1"),
    SOURCE_VIDEO2("Source Video 2", PRIMARY_CMD, (byte) 0x06, "video2", "video2"),
    SOURCE_VIDEO3("Source Video 3", PRIMARY_CMD, (byte) 0x07, "video3", "video3"),
    SOURCE_VIDEO4("Source Video 4", PRIMARY_CMD, (byte) 0x08, "video4", "video4"),
    SOURCE_VIDEO5("Source Video 5", PRIMARY_CMD, (byte) 0x09, "video5", "video5"),
    SOURCE_VIDEO6("Source Video 6", PRIMARY_CMD, (byte) 0x94, "video6", "video6"),
    SOURCE_VIDEO7("Source Video 7", "video7", "video7"),
    SOURCE_VIDEO8("Source Video 8", "video8", "video8"),
    SOURCE_PHONO("Source Phono", PRIMARY_CMD, (byte) 0x35, "phono", "phono"),
    SOURCE_USB("Source Front USB", PRIMARY_CMD, (byte) 0x8E, "usb", "usb"),
    SOURCE_PCUSB("Source PC USB", "pc_usb", "pcusb"),
    SOURCE_MULTI_INPUT("Source Multi Input", PRIMARY_CMD, (byte) 0x15, "multi_input", "multi_input"),
    SOURCE_AUX("Source Aux", "aux", "aux"),
    SOURCE_AUX1("Source Aux 1", "aux1", "aux1"),
    SOURCE_AUX2("Source Aux 2", "aux2", "aux2"),
    SOURCE_AUX1_COAX("Source Aux 1 Coax", "aux1_coax", "aux1_coax"),
    SOURCE_AUX1_OPT("Source Aux 1 Optical", "aux1_opt", "aux1_opt"),
    SOURCE_COAX1("Source Coax 1", "coax1", "coax1"),
    SOURCE_COAX2("Source Coax 2", "coax2", "coax2"),
    SOURCE_COAX3("Source Coax 3", "coax3", "coax3"),
    SOURCE_OPT1("Source Optical 1", "opt1", "opt1"),
    SOURCE_OPT2("Source Optical 2", "opt2", "opt2"),
    SOURCE_OPT3("Source Optical 3", "opt3", "opt3"),
    SOURCE_BLUETOOTH("Source Bluetooth", "bluetooth", "bluetooth"),
    SOURCE_ROTEL_CD("Source Rotel CD", "rcd", null),
    SOURCE_XLR("Source XLR", "bal_xlr", "bal_xlr"),
    SOURCE_XLR1("Source XLR 1", "bal_xlr1", "bal_xlr1"),
    SOURCE_XLR2("Source XLR 2", "bal_xlr2", "bal_xlr2"),
    SOURCE_FM("Source FM", "fm", "fm"),
    SOURCE_DAB("Source DAB", "dab", "dab"),
    SOURCE_PLAYFI("Source PlayFi", "playfi", "playfi"),
    SOURCE_IRADIO("Source iRadio", "iradio", "iradio"),
    SOURCE_NETWORK("Source Network", "network", "network"),
    SOURCE_INPUT_A("Source Input A", null, "input_a"),
    SOURCE_INPUT_B("Source Input B", null, "input_b"),
    SOURCE_INPUT_C("Source Input C", null, "input_c"),
    SOURCE_INPUT_D("Source Input D", null, "input_d"),
    SOURCE("Request current source", "get_current_source", "source?"),
    INPUT("Request current source", null, "input?"),
    MAIN_ZONE_SOURCE_CD("Main Zone Source CD", MAIN_ZONE_CMD, (byte) 0x02, "main_zone_cd", "main_zone_cd"),
    MAIN_ZONE_SOURCE_TUNER("Main Zone Source Tuner", MAIN_ZONE_CMD, (byte) 0x03, "main_zone_tuner", "main_zone_tuner"),
    MAIN_ZONE_SOURCE_TAPE("Main Zone Source Tape", MAIN_ZONE_CMD, (byte) 0x04, "main_zone_tape", "main_zone_tape"),
    MAIN_ZONE_SOURCE_VIDEO1("Main Zone Source Video 1", MAIN_ZONE_CMD, (byte) 0x05, "main_zone_video1",
            "main_zone_video1"),
    MAIN_ZONE_SOURCE_VIDEO2("Main Zone Source Video 2", MAIN_ZONE_CMD, (byte) 0x06, "main_zone_video2",
            "main_zone_video2"),
    MAIN_ZONE_SOURCE_VIDEO3("Main Zone Source Video 3", MAIN_ZONE_CMD, (byte) 0x07, "main_zone_video3",
            "main_zone_video3"),
    MAIN_ZONE_SOURCE_VIDEO4("Main Zone Source Video 4", MAIN_ZONE_CMD, (byte) 0x08, "main_zone_video4",
            "main_zone_video4"),
    MAIN_ZONE_SOURCE_VIDEO5("Main Zone Source Video 5", MAIN_ZONE_CMD, (byte) 0x09, "main_zone_video5",
            "main_zone_video5"),
    MAIN_ZONE_SOURCE_VIDEO6("Main Zone Source Video 6", MAIN_ZONE_CMD, (byte) 0x94, "main_zone_video6",
            "main_zone_video6"),
    MAIN_ZONE_SOURCE_USB("Main Zone Source Front USB", MAIN_ZONE_CMD, (byte) 0x8E, "main_zone_usb", "main_zone_usb"),
    MAIN_ZONE_SOURCE_MULTI_INPUT("Main Zone Source Multi Input", MAIN_ZONE_CMD, (byte) 0x15, "main_zone_multi_input",
            "main_zone_multi_input"),
    ZONE1_SOURCE_INPUT_A("Zone 1 Source Input A", null, "z1:input_a"),
    ZONE1_SOURCE_INPUT_B("Zone 1 Source Input B", null, "z1:input_b"),
    ZONE1_SOURCE_INPUT_C("Zone 1 Source Input C", null, "z1:input_c"),
    ZONE1_SOURCE_INPUT_D("Zone 1 Source Input D", null, "z1:input_d"),
    RECORD_SOURCE_CD("Record Source CD", RECORD_SRC_CMD, (byte) 0x02, "record_cd", "record_cd"),
    RECORD_SOURCE_TUNER("Record Source Tuner", RECORD_SRC_CMD, (byte) 0x03, "record_tuner", "record_tuner"),
    RECORD_SOURCE_TAPE("Record Source Tape", RECORD_SRC_CMD, (byte) 0x04, "record_tape", "record_tape"),
    RECORD_SOURCE_VIDEO1("Record Source Video 1", RECORD_SRC_CMD, (byte) 0x05, "record_video1", "record_video1"),
    RECORD_SOURCE_VIDEO2("Record Source Video 2", RECORD_SRC_CMD, (byte) 0x06, "record_video2", "record_video2"),
    RECORD_SOURCE_VIDEO3("Record Source Video 3", RECORD_SRC_CMD, (byte) 0x07, "record_video3", "record_video3"),
    RECORD_SOURCE_VIDEO4("Record Source Video 4", RECORD_SRC_CMD, (byte) 0x08, "record_video4", "record_video4"),
    RECORD_SOURCE_VIDEO5("Record Source Video 5", RECORD_SRC_CMD, (byte) 0x09, "record_video5", "record_video5"),
    RECORD_SOURCE_VIDEO6("Record Source Video 6", RECORD_SRC_CMD, (byte) 0x94, "record_video6", "record_video6"),
    RECORD_SOURCE_USB("Record Source Front USB", RECORD_SRC_CMD, (byte) 0x8E, "record_usb", "record_usb"),
    RECORD_SOURCE_MAIN("Record Follow Main Zone Source", RECORD_SRC_CMD, (byte) 0x6B, "record_follow_main",
            "record_follow_main"),
    ZONE2_SOURCE_CD("Zone 2 Source CD", ZONE2_CMD, (byte) 0x02, "zone2_cd", "zone2_cd"),
    ZONE2_SOURCE_TUNER("Zone 2 Source Tuner", ZONE2_CMD, (byte) 0x03, "zone2_tuner", "zone2_tuner"),
    ZONE2_SOURCE_TAPE("Zone 2 Source Tape", ZONE2_CMD, (byte) 0x04, "zone2_tape", "zone2_tape"),
    ZONE2_SOURCE_VIDEO1("Zone 2 Source Video 1", ZONE2_CMD, (byte) 0x05, "zone2_video1", "zone2_video1"),
    ZONE2_SOURCE_VIDEO2("Zone 2 Source Video 2", ZONE2_CMD, (byte) 0x06, "zone2_video2", "zone2_video2"),
    ZONE2_SOURCE_VIDEO3("Zone 2 Source Video 3", ZONE2_CMD, (byte) 0x07, "zone2_video3", "zone2_video3"),
    ZONE2_SOURCE_VIDEO4("Zone 2 Source Video 4", ZONE2_CMD, (byte) 0x08, "zone2_video4", "zone2_video4"),
    ZONE2_SOURCE_VIDEO5("Zone 2 Source Video 5", ZONE2_CMD, (byte) 0x09, "zone2_video5", "zone2_video5"),
    ZONE2_SOURCE_VIDEO6("Zone 2 Source Video 6", ZONE2_CMD, (byte) 0x94, "zone2_video6", "zone2_video6"),
    ZONE2_SOURCE_USB("Zone 2 Source Front USB", ZONE2_CMD, (byte) 0x8E, "zone2_usb", "zone2_usb"),
    ZONE2_SOURCE_MAIN("Zone 2 Follow Main Zone Source", ZONE2_CMD, (byte) 0x6B, "zone2_follow_main",
            "zone2_follow_main"),
    ZONE2_SOURCE_INPUT_A("Zone 2 Source Input A", null, "z2:input_a"),
    ZONE2_SOURCE_INPUT_B("Zone 2 Source Input B", null, "z2:input_b"),
    ZONE2_SOURCE_INPUT_C("Zone 2 Source Input C", null, "z2:input_c"),
    ZONE2_SOURCE_INPUT_D("Zone 2 Source Input D", null, "z2:input_d"),
    ZONE3_SOURCE_CD("Zone 3 Source CD", ZONE3_CMD, (byte) 0x02, "zone3_cd", "zone3_cd"),
    ZONE3_SOURCE_TUNER("Zone 3 Source Tuner", ZONE3_CMD, (byte) 0x03, "zone3_tuner", "zone3_tuner"),
    ZONE3_SOURCE_TAPE("Zone 3 Source Tape", ZONE3_CMD, (byte) 0x04, "zone3_tape", "zone3_tape"),
    ZONE3_SOURCE_VIDEO1("Zone 3 Source Video 1", ZONE3_CMD, (byte) 0x05, "zone3_video1", "zone3_video1"),
    ZONE3_SOURCE_VIDEO2("Zone 3 Source Video 2", ZONE3_CMD, (byte) 0x06, "zone3_video2", "zone3_video2"),
    ZONE3_SOURCE_VIDEO3("Zone 3 Source Video 3", ZONE3_CMD, (byte) 0x07, "zone3_video3", "zone3_video3"),
    ZONE3_SOURCE_VIDEO4("Zone 3 Source Video 4", ZONE3_CMD, (byte) 0x08, "zone3_video4", "zone3_video4"),
    ZONE3_SOURCE_VIDEO5("Zone 3 Source Video 5", ZONE3_CMD, (byte) 0x09, "zone3_video5", "zone3_video5"),
    ZONE3_SOURCE_VIDEO6("Zone 3 Source Video 6", ZONE3_CMD, (byte) 0x94, "zone3_video6", "zone3_video6"),
    ZONE3_SOURCE_USB("Zone 3 Source Front USB", ZONE3_CMD, (byte) 0x8E, "zone3_usb", "zone3_usb"),
    ZONE3_SOURCE_MAIN("Zone 3 Follow Main Zone Source", ZONE3_CMD, (byte) 0x6B, "zone3_follow_main",
            "zone3_follow_main"),
    ZONE3_SOURCE_INPUT_A("Zone 3 Source Input A", null, "z3:input_a"),
    ZONE3_SOURCE_INPUT_B("Zone 3 Source Input B", null, "z3:input_b"),
    ZONE3_SOURCE_INPUT_C("Zone 3 Source Input C", null, "z3:input_c"),
    ZONE3_SOURCE_INPUT_D("Zone 3 Source Input D", null, "z3:input_d"),
    ZONE4_SOURCE_CD("Zone 4 Source CD", ZONE4_CMD, (byte) 0x02, "zone4_cd", "zone4_cd"),
    ZONE4_SOURCE_TUNER("Zone 4 Source Tuner", ZONE4_CMD, (byte) 0x03, "zone4_tuner", "zone4_tuner"),
    ZONE4_SOURCE_TAPE("Zone 4 Source Tape", ZONE4_CMD, (byte) 0x04, "zone4_tape", "zone4_tape"),
    ZONE4_SOURCE_VIDEO1("Zone 4 Source Video 1", ZONE4_CMD, (byte) 0x05, "zone4_video1", "zone4_video1"),
    ZONE4_SOURCE_VIDEO2("Zone 4 Source Video 2", ZONE4_CMD, (byte) 0x06, "zone4_video2", "zone4_video2"),
    ZONE4_SOURCE_VIDEO3("Zone 4 Source Video 3", ZONE4_CMD, (byte) 0x07, "zone4_video3", "zone4_video3"),
    ZONE4_SOURCE_VIDEO4("Zone 4 Source Video 4", ZONE4_CMD, (byte) 0x08, "zone4_video4", "zone4_video4"),
    ZONE4_SOURCE_VIDEO5("Zone 4 Source Video 5", ZONE4_CMD, (byte) 0x09, "zone4_video5", "zone4_video5"),
    ZONE4_SOURCE_VIDEO6("Zone 4 Source Video 6", ZONE4_CMD, (byte) 0x94, "zone4_video6", "zone4_video6"),
    ZONE4_SOURCE_USB("Zone 4 Source Front USB", ZONE4_CMD, (byte) 0x8E, "zone4_usb", "zone4_usb"),
    ZONE4_SOURCE_MAIN("Zone 4 Follow Main Zone Source", ZONE4_CMD, (byte) 0x6B, "zone4_follow_main",
            "zone4_follow_main"),
    ZONE4_SOURCE_INPUT_A("Zone 4 Source Input A", null, "z4:input_a"),
    ZONE4_SOURCE_INPUT_B("Zone 4 Source Input B", null, "z4:input_b"),
    ZONE4_SOURCE_INPUT_C("Zone 4 Source Input C", null, "z4:input_c"),
    ZONE4_SOURCE_INPUT_D("Zone 4 Source Input D", null, "z4:input_d"),
    STEREO("Stereo", PRIMARY_CMD, (byte) 0x11, "2channel", "2channel"),
    STEREO3("Dolby 3 Stereo ", PRIMARY_CMD, (byte) 0x12, "3channel", "3channel"),
    STEREO5("5 Channel Stereo", PRIMARY_CMD, (byte) 0x5B, "5channel", "5channel"),
    STEREO7("7 Channel Stereo", PRIMARY_CMD, (byte) 0x5C, "7channel", "7channel"),
    STEREO9("9 Channel Stereo", "9channel", "9channel"),
    STEREO11("11 Channel Stereo", "11channel", "11channel"),
    DSP1("DSP 1", PRIMARY_CMD, (byte) 0x57),
    DSP2("DSP 2", PRIMARY_CMD, (byte) 0x58),
    DSP3("DSP 3", PRIMARY_CMD, (byte) 0x59),
    DSP4("DSP 4", PRIMARY_CMD, (byte) 0x5A),
    PROLOGIC("Dolby Pro Logic", PRIMARY_CMD, (byte) 0x5F),
    PLII_CINEMA("Dolby PLII Cinema", PRIMARY_CMD, (byte) 0x5D, "prologic_movie", "prologic_movie"),
    PLII_MUSIC("Dolby PLII Music", PRIMARY_CMD, (byte) 0x5E, "prologic_music", "prologic_music"),
    PLII_GAME("Dolby PLII Game", PRIMARY_CMD, (byte) 0x74, "prologic_game", "prologic_game"),
    PLIIZ("Dolby PLIIz", PRIMARY_CMD, (byte) 0x92, "prologic_iiz", "prologic_iiz"),
    NEO6_MUSIC("dts Neo:6 Music", PRIMARY_CMD, (byte) 0x60, "neo6_music", "neo6_music"),
    NEO6_CINEMA("dts Neo:6 Cinema", PRIMARY_CMD, (byte) 0x61, "neo6_cinema", "neo6_cinema"),
    ATMOS("Dolby Atmos", "dolby_atmos", "dolby_atmos"),
    NEURAL_X("dts Neural:X", "dts_neural", "dts_neural"),
    BYPASS("Analog Bypass", PRIMARY_CMD, (byte) 0x11, "bypass", "bypass"),
    DSP_MODE("Request current DSP mode", "get_dsp_mode", "dsp_mode"),
    TONE_MAX("Request Max tone level", "get_tone_max", null),
    TONE_CONTROL_SELECT("Tone Control Select", PRIMARY_CMD, (byte) 0x67),
    TREBLE_UP("Treble Up", PRIMARY_CMD, (byte) 0x0D, "treble_up", "treble_up"),
    TREBLE_DOWN("Treble Down", PRIMARY_CMD, (byte) 0x0E, "treble_down", "treble_down"),
    TREBLE_SET("Set Treble to level", "treble_", "treble_"),
    TREBLE("Request current treble level", "get_treble", "treble?"),
    BASS_UP("Bass Up", PRIMARY_CMD, (byte) 0x0F, "bass_up", "bass_up"),
    BASS_DOWN("Bass Down", PRIMARY_CMD, (byte) 0x10, "bass_down", "bass_down"),
    BASS_SET("Set Bass to level", "bass_", "bass_"),
    BASS("Request current bass level", "get_bass", "bass?"),
    ZONE1_TREBLE_UP("Zone 1 Treble Up", null, "z1:treble_up"),
    ZONE1_TREBLE_DOWN("Zone 1 Treble Down", null, "z1:treble_down"),
    ZONE1_TREBLE_SET("Set Zone 1 Treble to level", null, "z1:treble_"),
    ZONE1_BASS_UP("Zone 1 Bass Up", null, "z1:bass_up"),
    ZONE1_BASS_DOWN("Zone 1 Bass Down", null, "z1:bass_down"),
    ZONE1_BASS_SET("Set Zone 1 Bass to level", null, "z1:bass_"),
    ZONE2_TREBLE_UP("Zone 2 Treble Up", null, "z2:treble_up"),
    ZONE2_TREBLE_DOWN("Zone 2 Treble Down", null, "z2:treble_down"),
    ZONE2_TREBLE_SET("Set Zone 2 Treble to level", null, "z2:treble_"),
    ZONE2_BASS_UP("Zone 2 Bass Up", null, "z2:bass_up"),
    ZONE2_BASS_DOWN("Zone 2 Bass Down", null, "z2:bass_down"),
    ZONE2_BASS_SET("Set Zone 2 Bass to level", null, "z2:bass_"),
    ZONE3_TREBLE_UP("Zone 3 Treble Up", null, "z3:treble_up"),
    ZONE3_TREBLE_DOWN("Zone 3 Treble Down", null, "z3:treble_down"),
    ZONE3_TREBLE_SET("Set Zone 3 Treble to level", null, "z3:treble_"),
    ZONE3_BASS_UP("Zone 3 Bass Up", null, "z3:bass_up"),
    ZONE3_BASS_DOWN("Zone 3 Bass Down", null, "z3:bass_down"),
    ZONE3_BASS_SET("Set Zone 3 Bass to level", null, "z3:bass_"),
    ZONE4_TREBLE_UP("Zone 4 Treble Up", null, "z4:treble_up"),
    ZONE4_TREBLE_DOWN("Zone 4 Treble Down", null, "z4:treble_down"),
    ZONE4_TREBLE_SET("Set Zone 4 Treble to level", null, "z4:treble_"),
    ZONE4_BASS_UP("Zone 4 Bass Up", null, "z4:bass_up"),
    ZONE4_BASS_DOWN("Zone 4 Bass Down", null, "z4:bass_down"),
    ZONE4_BASS_SET("Set Zone 4 Bass to level", null, "z4:bass_"),
    RECORD_FONCTION_SELECT("Record Function Select", PRIMARY_CMD, (byte) 0x17),
    PLAY("Play Source", PRIMARY_CMD, (byte) 0x04, "play", "play"),
    STOP("Stop Source", PRIMARY_CMD, (byte) 0x06, "stop", "stop"),
    PAUSE("Pause Source", PRIMARY_CMD, (byte) 0x05, "pause", "pause"),
    CD_PLAY_STATUS("Request CD play status", "get_cd_play_status", null),
    PLAY_STATUS("Request source play status", "get_play_status", "status"),
    TRACK_FORWARD("Track Forward", PRIMARY_CMD, (byte) 0x09, "track_fwd", "trkf"),
    TRACK_BACKWORD("Track Backward", PRIMARY_CMD, (byte) 0x08, "track_back", "trkb"),
    TRACK("Request current CD track number", null, "track"),
    FREQUENCY("Request current frequency for digital source input", "get_current_freq", "freq?"),
    DISPLAY_REFRESH("Display Refresh", PRIMARY_CMD, (byte) 0xFF),
    DIMMER_LEVEL_GET("Request current front display dimmer level", "get_current_dimmer", "dimmer?"),
    DIMMER_LEVEL_SET("Set front display dimmer to level", "dimmer_", "dimmer_"),
    UPDATE_AUTO("Set Update to Auto", "display_update_auto", "rs232_update_on"),
    UPDATE_MANUAL("Set Update to Manual", "display_update_manual", "rs232_update_off"),
    TONE_CONTROLS_ON("Tone Controls On", "tone_on", null),
    TONE_CONTROLS_OFF("Tone Controls Off", "tone_off", null),
    TONE_CONTROLS("Request current tone control state", "get_tone", null),
    TCBYPASS_ON("Bypass On", null, "bypass_on"),
    TCBYPASS_OFF("Bypass Off", null, "bypass_off"),
    TCBYPASS("Request current tone bypass state", null, "bypass?"),
    BALANCE_RIGHT("Balance Right", "balance_right", "balance_r"),
    BALANCE_LEFT("Balance Left", "balance_left", "balance_l"),
    BALANCE_SET("Set Balance to level", "balance_", "balance_"),
    ZONE1_BALANCE_RIGHT("Zone 1 Balance Right", null, "z1:balance_r"),
    ZONE1_BALANCE_LEFT("Zone 1 Balance Left", null, "z1:balance_l"),
    ZONE1_BALANCE_SET("Set Zone 1 Balance to level", null, "z1:balance_"),
    ZONE2_BALANCE_RIGHT("Zone 2 Balance Right", null, "z2:balance_r"),
    ZONE2_BALANCE_LEFT("Zone 2 Balance Left", null, "z2:balance_l"),
    ZONE2_BALANCE_SET("Set Zone 2 Balance to level", null, "z2:balance_"),
    ZONE3_BALANCE_RIGHT("Zone 3 Balance Right", null, "z3:balance_r"),
    ZONE3_BALANCE_LEFT("Zone 3 Balance Left", null, "z3:balance_l"),
    ZONE3_BALANCE_SET("Set Zone 3 Balance to level", null, "z3:balance_"),
    ZONE4_BALANCE_RIGHT("Zone 4 Balance Right", null, "z4:balance_r"),
    ZONE4_BALANCE_LEFT("Zone 4 Balance Left", null, "z4:balance_l"),
    ZONE4_BALANCE_SET("Set Zone 4 Balance to level", null, "z4:balance_"),
    BALANCE("Request current balance setting", "get_balance", "balance?"),
    SPEAKER_A_TOGGLE("Toggle Speaker A Output", PRIMARY_CMD, (byte) 0x50, "speaker_a", "speaker_a"),
    SPEAKER_A_ON("Set Speaker A Output", "speaker_a_on", "speaker_a_on"),
    SPEAKER_A_OFF("Unset Speaker A Output", "speaker_a_off", "speaker_a_off"),
    SPEAKER_B_TOGGLE("Toggle Speaker B Output", PRIMARY_CMD, (byte) 0x51, "speaker_b", "speaker_b"),
    SPEAKER_B_ON("Set Speaker B Output", "speaker_b_on", "speaker_b_on"),
    SPEAKER_B_OFF("Unset Speaker B Output", "speaker_b_off", "speaker_b_off"),
    SPEAKER("Request current active speaker outputs", "get_current_speaker", "speaker?");

    public static final byte PRIMARY_COMMAND = (byte) 0x10;

    private String name;
    private byte hexType;
    private byte hexKey;
    private @Nullable String asciiCommandV1;
    private @Nullable String asciiCommandV2;

    /**
     * Constructor when the textual commands are undefined
     *
     * @param name the command name
     * @param hexType the the command type (HEX protocol)
     * @param hexKey the the command key (HEX protocol)
     */
    private RotelCommand(String name, byte hexType, byte hexKey) {
        this(name, hexType, hexKey, null, null);
    }

    /**
     * Constructor when the HEX command is undefined
     *
     * @param name the command name
     * @param asciiCommandV1 the textual command (ASCII protocol V1)
     * @param asciiCommandV2 the textual command (ASCII protocol V2)
     */
    private RotelCommand(String name, @Nullable String asciiCommandV1, @Nullable String asciiCommandV2) {
        this(name, (byte) 0, (byte) 0, asciiCommandV1, asciiCommandV2);
    }

    /**
     * Constructor
     *
     * @param name the command name
     * @param hexType the the command type (HEX protocol)
     * @param hexKey the the command key (HEX protocol)
     * @param asciiCommandV1 the textual command (ASCII protocol V1)
     * @param asciiCommandV2 the textual command (ASCII protocol V2)
     */
    private RotelCommand(String name, byte hexType, byte hexKey, @Nullable String asciiCommandV1,
            @Nullable String asciiCommandV2) {
        this.name = name;
        this.hexType = hexType;
        this.hexKey = hexKey;
        this.asciiCommandV1 = asciiCommandV1;
        this.asciiCommandV2 = asciiCommandV2;
    }

    /**
     * Get the command name
     *
     * @return the command name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the command type (HEX protocol)
     *
     * @return the command type
     */
    public byte getHexType() {
        return hexType;
    }

    /**
     * Get the command key (HEX protocol)
     *
     * @return the command key
     */
    public byte getHexKey() {
        return hexKey;
    }

    /**
     * Get the textual command (ASCII protocol V1)
     *
     * @return the textual command
     */
    public @Nullable String getAsciiCommandV1() {
        return asciiCommandV1;
    }

    /**
     * Get the textual command (ASCII protocol V2)
     *
     * @return the textual command
     */
    public @Nullable String getAsciiCommandV2() {
        return asciiCommandV2;
    }

    /**
     * Get the command associated to a textual command
     *
     * @param text the textual command used to identify the command
     *
     * @return the command associated to the searched textual command
     *
     * @throws RotelException - If no command is associated to the searched textual command
     */
    public static RotelCommand getFromAsciiCommand(String text) throws RotelException {
        for (RotelCommand value : RotelCommand.values()) {
            if (text.equals(value.getAsciiCommandV1()) || text.equals(value.getAsciiCommandV2())) {
                return value;
            }
        }
        throw new RotelException("Invalid textual command: " + text);
    }
}
