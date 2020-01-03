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
package org.openhab.binding.mqtt.generic.values;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * Implements a datetime value.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class DateTimeValue extends Value {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public DateTimeValue() {
        super(CoreItemFactory.DATETIME, Stream.of(DateTimeType.class, StringType.class).collect(Collectors.toList()));
    }

    @Override
    public void update(Command command) throws IllegalArgumentException {
        if (command instanceof DateTimeType) {
            state = ((DateTimeType) command);
        } else {
            state = DateTimeType.valueOf(command.toString());
        }
    }

    @Override
    public String getMQTTpublishValue(@Nullable String pattern) {
        if (state == UnDefType.UNDEF) {
            return "";
        }
        String formatPattern = pattern;
        if (formatPattern == null || "%s".contentEquals(formatPattern)) {
            formatPattern = DATE_PATTERN;
        }
        return DateTimeFormatter.ofPattern(formatPattern).format(((DateTimeType) state).getZonedDateTime());
    }
}
