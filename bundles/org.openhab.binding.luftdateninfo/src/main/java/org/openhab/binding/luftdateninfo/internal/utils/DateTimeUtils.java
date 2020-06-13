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
package org.openhab.binding.luftdateninfo.internal.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link DateTimeUtils} class provides helpers for converting Dates and Times.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class DateTimeUtils {
    private static SimpleDateFormat sdf = new SimpleDateFormat("YYYY-mm-dd hh:mm:ss");

    public static @Nullable Date toDate(String dateTime) {
        try {
            return sdf.parse(dateTime);
        } catch (ParseException | NumberFormatException e) {
            return null;
        }
    }
}
