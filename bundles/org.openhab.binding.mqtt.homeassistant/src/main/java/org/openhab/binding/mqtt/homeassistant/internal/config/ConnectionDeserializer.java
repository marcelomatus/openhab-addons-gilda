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
package org.openhab.binding.mqtt.homeassistant.internal.config;

import java.lang.reflect.Type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.Connection;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * The {@link ConnectionDeserializer} will de-serialize a connection-list
 *
 * see: https://www.home-assistant.io/integrations/sensor.mqtt/#connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ConnectionDeserializer implements JsonDeserializer<Connection> {
    public @Nullable Connection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonArray list;
        if (json == null) {
            throw new JsonParseException("JSON element is null");
        }
        try {
            list = json.getAsJsonArray();
        } catch (IllegalStateException e) {
            throw new JsonParseException("Cannot parse JSON array", e);
        }
        if (list.size() != 2) {
            throw new JsonParseException(
                    "Connection information must be a tuple, but has " + list.size() + " elements!");
        }
        return new Connection(list.get(0).getAsString(), list.get(1).getAsString());
    }
}
