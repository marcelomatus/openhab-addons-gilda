/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.onkyo.internal.eiscp;

/**
 * @author Thomas.Eichstaedt-Engelen
 * @since 1.3.0
 */
public enum EiscpCommandRef {

    POWER_OFF(1),
    POWER_ON(2),
    POWER_QUERY(3),

    UNMUTE(4),
    MUTE(5),
    MUTE_QUERY(6),

    VOLUME_UP(7),
    VOLUME_DOWN(8),
    VOLUME_QUERY(9),
    VOLUME_SET(10),

    SOURCE_DVR(11),
    SOURCE_SATELLITE(12),
    SOURCE_GAME(13),
    SOURCE_AUX(14),
    SOURCE_VIDEO5(15),
    SOURCE_COMPUTER(16),
    SOURCE_BLURAY(17),
    SOURCE_TAPE1(18),
    SOURCE_TAPE2(19),
    SOURCE_PHONO(20),
    SOURCE_CD(21),
    SOURCE_FM(22),
    SOURCE_AM(23),
    SOURCE_TUNER(24),
    SOURCE_MUSICSERVER(25),
    SOURCE_INTERETRADIO(26),
    SOURCE_USB(27),
    SOURCE_USB_BACK(28),
    SOURCE_NETWORK(29),
    SOURCE_MULTICH(30),
    SOURCE_SIRIUS(31),
    SOURCE_UP(32),
    SOURCE_DOWN(33),
    SOURCE_QUERY(34),

    VIDEO_WIDE_AUTO(35),
    VIDEO_WIDE_43(36),
    VIDEO_WIDE_FULL(37),
    VIDEO_WIDE_ZOOM(38),
    VIDEO_WIDE_WIDEZOOM(39),
    VIDEO_WIDE_SMARTZOOM(40),
    VIDEO_WIDE_NEXT(41),
    VIDEO_WIDE_QUERY(42),

    /*
     * LISTEN_MODE_STEREO(43),
     * LISTEN_MODE_ALCHANSTEREO(44),
     * LISTEN_MODE_AUDYSSEY_DSX(45),
     * LISTEN_MODE_PLII_MOVIE_DSX(46),
     * LISTEN_MODE_PLII_MUSIC_DSX(47),
     * LISTEN_MODE_PLII_GAME_DSX(48),
     * LISTEN_MODE_NEO_CINEMA_DSX(49),
     * LISTEN_MODE_NEO_MUSIC_DSX(50),
     * LISTEN_MODE_NEURAL_SURROUND_DSX(51),
     * LISTEN_MODE_NEURAL_DIGITAL_DSX(52),
     */
    LISTEN_MODE_SET(52),
    LISTEN_MODE_QUERY(53),

    ZONE2_POWER_ON(54),
    ZONE2_POWER_SBY(55),
    ZONE2_POWER_QUERY(56),
    ZONE2_SOURCE_DVR(57),
    ZONE2_SOURCE_SATELLITE(58),
    ZONE2_SOURCE_GAME(59),
    ZONE2_SOURCE_AUX(60),
    ZONE2_SOURCE_VIDEO5(61),
    ZONE2_SOURCE_COMPUTER(62),
    ZONE2_SOURCE_BLURAY(63),
    ZONE2_SOURCE_QUERY(64),

    NETUSB_OP_PLAY(65),
    NETUSB_OP_STOP(66),
    NETUSB_OP_PAUSE(67),
    NETUSB_OP_TRACKUP(68),
    NETUSB_OP_TRACKDWN(69),
    NETUSB_OP_FF(70),
    NETUSB_OP_REW(71),
    NETUSB_OP_REPEAT(72),
    NETUSB_OP_RANDOM(73),
    NETUSB_OP_DISPLAY(74),
    NETUSB_OP_RIGHT(75),
    NETUSB_OP_LEFT(76),
    NETUSB_OP_UP(77),
    NETUSB_OP_DOWN(78),
    NETUSB_OP_SELECT(79),
    NETUSB_OP_1(80),
    NETUSB_OP_2(81),
    NETUSB_OP_3(82),
    NETUSB_OP_4(83),
    NETUSB_OP_5(84),
    NETUSB_OP_6(85),
    NETUSB_OP_7(86),
    NETUSB_OP_8(87),
    NETUSB_OP_9(88),
    NETUSB_OP_0(89),
    NETUSB_OP_DELETE(90),
    NETUSB_OP_CAPS(91),
    NETUSB_OP_SETUP(92),
    NETUSB_OP_RETURN(93),
    NETUSB_OP_CHANUP(94),
    NETUSB_OP_CHANDWN(95),
    NETUSB_OP_MENU(96),
    NETUSB_OP_TOPMENU(97),

    NETUSB_SONG_ARTIST_QUERY(98),
    NETUSB_SONG_ALBUM_QUERY(99),
    NETUSB_SONG_TITLE_QUERY(100),
    NETUSB_SONG_ELAPSEDTIME_QUERY(101), // NET/USB Time Info (Elapsed time/Track Time Max 99:59).
    NETUSB_SONG_TRACK_QUERY(102), // NET/USB Track Info (Current Track/Toral Track Max 9999).

    LISTEN_MODE_UP(103),
    LISTEN_MODE_DOWN(104),

    ZONE2_UNMUTE(105),
    ZONE2_MUTE(106),
    ZONE2_MUTE_QUERY(107),
    ZONE2_VOLUME_UP(108),
    ZONE2_VOLUME_DOWN(109),
    ZONE2_VOLUME_QUERY(110),
    ZONE2_VOLUME_SET(111),
    ZONE2_SOURCE_TAPE1(112),
    ZONE2_SOURCE_TAPE2(113),
    ZONE2_SOURCE_PHONO(114),
    ZONE2_SOURCE_CD(115),
    ZONE2_SOURCE_FM(116),
    ZONE2_SOURCE_AM(117),
    ZONE2_SOURCE_TUNER(118),
    ZONE2_SOURCE_MUSICSERVER(119),
    ZONE2_SOURCE_INTERETRADIO(120),
    ZONE2_SOURCE_USB(121),
    ZONE2_SOURCE_USB_BACK(122),
    ZONE2_SOURCE_NETWORK(123),
    ZONE2_SOURCE_MULTICH(124),
    ZONE2_SOURCE_SIRIUS(125),
    ZONE2_SOURCE_UP(126),
    ZONE2_SOURCE_DOWN(127),

    ZONE3_POWER_ON(128),
    ZONE3_POWER_SBY(129),
    ZONE3_POWER_QUERY(130),
    ZONE3_SOURCE_DVR(131),
    ZONE3_SOURCE_SATELLITE(132),
    ZONE3_SOURCE_GAME(133),
    ZONE3_SOURCE_AUX(134),
    ZONE3_SOURCE_VIDEO5(135),
    ZONE3_SOURCE_COMPUTER(136),
    ZONE3_SOURCE_BLURAY(137),
    ZONE3_SOURCE_QUERY(138),
    ZONE3_UNMUTE(139),
    ZONE3_MUTE(140),
    ZONE3_MUTE_QUERY(141),
    ZONE3_VOLUME_UP(142),
    ZONE3_VOLUME_DOWN(143),
    ZONE3_VOLUME_QUERY(144),
    ZONE3_VOLUME_SET(145),
    ZONE3_SOURCE_TAPE1(146),
    ZONE3_SOURCE_TAPE2(147),
    ZONE3_SOURCE_PHONO(148),
    ZONE3_SOURCE_CD(149),
    ZONE3_SOURCE_FM(150),
    ZONE3_SOURCE_AM(151),
    ZONE3_SOURCE_TUNER(152),
    ZONE3_SOURCE_MUSICSERVER(153),
    ZONE3_SOURCE_INTERETRADIO(154),
    ZONE3_SOURCE_USB(155),
    ZONE3_SOURCE_USB_BACK(156),
    ZONE3_SOURCE_NETWORK(157),
    ZONE3_SOURCE_MULTICH(158),
    ZONE3_SOURCE_SIRIUS(159),
    ZONE3_SOURCE_UP(160),
    ZONE3_SOURCE_DOWN(161),

    SOURCE_SET(162),
    ZONE2_SOURCE_SET(163),
    ZONE3_SOURCE_SET(163),
    SOURCE_GAME2(164),

    /**
     * NET/USB Play Status QUERY (3 letters - PRS).
     * <UL>
     * <LI>p -> Play Status: "S": STOP, "P": Play, "p": Pause, "F": FF, "R":
     * FREW</LI>
     * <LI>r -> Repeat Status: "-": Off, "R": All, "F": Folder, "1": Repeat 1</LI>
     * <LI>s -> Shuffle Status: "-": Off, "S": All , "A": Album, "F": Folder</LI>
     * </UL>
     **/
    NETUSB_PLAY_STATUS_QUERY(0);

    private int command;

    private EiscpCommandRef(int command) {
        this.command = command;
    }

    public int getCommand() {
        return command;
    }

}