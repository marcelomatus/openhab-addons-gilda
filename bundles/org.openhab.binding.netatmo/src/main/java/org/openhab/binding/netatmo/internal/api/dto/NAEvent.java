/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.netatmo.internal.api.dto;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.netatmo.internal.api.EventSubType;
import org.openhab.binding.netatmo.internal.api.EventType;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */

@NonNullByDefault
public abstract class NAEvent extends NAObject {
    protected EventType type = EventType.UNKNOWN;
    private @NonNullByDefault({}) String cameraId;
    private @NonNullByDefault({}) String message;
    protected int subType = -1;

    public EventType getEventType() {
        return type;
    }

    public String getCameraId() {
        return cameraId;
    }

    public String getMessage() {
        return message.replace("<b>", "").replace("</b>", "");
    }

    public abstract ZonedDateTime getTime();

    public abstract @Nullable String getPersonId();

    public abstract @Nullable NASnapshot getSnapshot();

    public Optional<EventSubType> getSubTypeDescription() {
        return Stream.of(EventSubType.values()).filter(v -> v.getType() == getEventType() && v.getSubType() == subType)
                .findFirst();
    }

    public void setEventType(EventType type) {
        this.type = type;
    }
}
