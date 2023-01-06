/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.powermax.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Used to map messages to be sent to the Visonic alarm panel to an ENUM value
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public enum PowermaxSendType {

    INIT(new byte[] { (byte) 0xAB, 0x0A, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null, null),
    ZONESNAME(new byte[] { (byte) 0xA3, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.ZONESNAME),
    ZONESTYPE(new byte[] { (byte) 0xA6, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.ZONESTYPE),
    X10NAMES(new byte[] { (byte) 0xAC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null, null),
    RESTORE(new byte[] { (byte) 0xAB, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.STATUS),
    ENROLL(new byte[] { (byte) 0xAB, 0x0A, 0x00, 0x00, 'O', 'H', 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null, null),
    EVENTLOG(new byte[] { (byte) 0xA0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, 4,
            PowermaxReceiveType.EVENT_LOG),
    ARM(new byte[] { (byte) 0xA1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, 3, null),
    STATUS(new byte[] { (byte) 0xA2, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.STATUS),
    BYPASSTAT(new byte[] { (byte) 0xA2, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.STATUS),
    X10PGM(new byte[] { (byte) 0xA4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, 6, null),
    BYPASS(new byte[] { (byte) 0xAA, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43 }, 1, null),
    DOWNLOAD(new byte[] { 0x24, 0x00, 0x00, 'O', 'H', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.INFO),
    SETTIME(new byte[] { 0x46, (byte) 0xF8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF }, 3,
            null),
    DL_TIME(new byte[] { 0x3E, (byte) 0xF8, 0x00, 0x06, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_COMMDEF(new byte[] { 0x3E, 0x01, 0x01, 0x1E, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_PHONENRS(new byte[] { 0x3E, 0x36, 0x01, 0x20, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_PINCODES(new byte[] { 0x3E, (byte) 0xFA, 0x01, 0x10, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_PGMX10(new byte[] { 0x3E, 0x14, 0x02, (byte) 0xD5, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_PARTITIONS(new byte[] { 0x3E, 0x00, 0x03, (byte) 0xF0, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_PANELFW(new byte[] { 0x3E, 0x00, 0x04, 0x20, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_SERIAL(new byte[] { 0x3E, 0x30, 0x04, 0x08, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_ZONES(new byte[] { 0x3E, 0x00, 0x09, 0x78, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_KEYFOBS(new byte[] { 0x3E, 0x78, 0x09, 0x40, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_2WKEYPAD(new byte[] { 0x3E, 0x00, 0x0A, 0x08, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_1WKEYPAD(new byte[] { 0x3E, 0x20, 0x0A, 0x40, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_SIRENS(new byte[] { 0x3E, 0x60, 0x0A, 0x08, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_X10NAMES(new byte[] { 0x3E, 0x30, 0x0B, 0x10, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_ZONENAMES(new byte[] { 0x3E, 0x40, 0x0B, 0x1E, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_EVENTLOG(new byte[] { 0x3E, (byte) 0xDF, 0x04, 0x28, 0x03, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_ZONESTR(new byte[] { 0x3E, 0x00, 0x19, 0x00, 0x02, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_ZONECUSTOM(new byte[] { 0x3E, (byte) 0xA0, 0x1A, 0x50, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_MR_ZONENAMES(new byte[] { 0x3E, 0x60, 0x09, 0x40, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_MR_PINCODES(new byte[] { 0x3E, (byte) 0x98, 0x0A, 0x60, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_MR_SIRENS(new byte[] { 0x3E, (byte) 0xE2, (byte) 0xB6, 0x50, 0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 },
            null, PowermaxReceiveType.SETTINGS_ITEM),
    DL_MR_KEYPADS(new byte[] { 0x3E, 0x32, (byte) 0xB7, 0x40, 0x01, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_MR_ZONES(new byte[] { 0x3E, 0x72, (byte) 0xB8, (byte) 0x80, 0x02, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 },
            null, PowermaxReceiveType.SETTINGS_ITEM),
    DL_MR_SIRKEYZON(
            new byte[] { 0x3E, (byte) 0xE2, (byte) 0xB6, 0x10, 0x04, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    DL_ALL(new byte[] { 0x3E, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS_ITEM),
    SER_TYPE(new byte[] { 0x5A, 0x30, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, null,
            PowermaxReceiveType.SETTINGS),
    START(new byte[] { 0x0A }, null, PowermaxReceiveType.SETTINGS),
    EXIT(new byte[] { 0x0F }, null, null),
    POWERMASTER_ZONE_STAT1(
            new byte[] { (byte) 0xB0, 0x01, 0x04, 0x06, 0x02, (byte) 0xFF, 0x08, 0x03, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.POWERMASTER),
    POWERMASTER_ZONE_STAT2(
            new byte[] { (byte) 0xB0, 0x01, 0x07, 0x06, 0x02, (byte) 0xFF, 0x08, 0x03, 0x00, 0x00, 0x43 }, null,
            PowermaxReceiveType.POWERMASTER),
    POWERMASTER_ZONE_NAME(new byte[] { (byte) 0xB0, 0x01, 0x21, 0x02, 0x05, 0x00, 0x43 }, null,
            PowermaxReceiveType.POWERMASTER),
    POWERMASTER_ZONE_TYPE1(new byte[] { (byte) 0xB0, 0x01, 0x2D, 0x02, 0x05, 0x00, 0x43 }, null,
            PowermaxReceiveType.POWERMASTER);

    private final byte[] message;
    private final @Nullable Integer paramPosition;
    private final @Nullable PowermaxReceiveType expectedResponseType;

    private PowermaxSendType(byte[] message, @Nullable Integer paramPosition,
            @Nullable PowermaxReceiveType expectedResponseType) {
        this.message = message;
        this.paramPosition = paramPosition;
        this.expectedResponseType = expectedResponseType;
    }

    /**
     * @return the message buffer, preamble and postamble not included
     */
    public byte[] getMessage() {
        return message;
    }

    /**
     * @return the position of the parameter in the message buffer
     */
    public @Nullable Integer getParamPosition() {
        return paramPosition;
    }

    /**
     * @return the ENUM value of the expected message as response
     */
    public @Nullable PowermaxReceiveType getExpectedResponseType() {
        return expectedResponseType;
    }
}
