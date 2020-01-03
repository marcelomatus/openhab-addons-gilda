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

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;

/**
 * Implements a location value.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class LocationValue extends Value {
    public LocationValue() {
        super(CoreItemFactory.LOCATION, Stream.of(PointType.class, StringType.class).collect(Collectors.toList()));
    }

    @Override
    public @NonNull String getMQTTpublishValue(@Nullable String pattern) {
        String formatPattern = pattern;

        if (formatPattern == null || "%s".equals(formatPattern)) {
            formatPattern = "%1$f,%2$f";
        }
        PointType point = ((PointType) state);
        return String.format(Locale.ROOT, formatPattern, point.getLatitude().toBigDecimal(),
                point.getLongitude().toBigDecimal(), point.getAltitude().toBigDecimal());
    }

    @Override
    public void update(Command command) throws IllegalArgumentException {
        if (command instanceof PointType) {
            state = ((PointType) command);
        } else {
            state = PointType.valueOf(command.toString());
        }
    }
}
