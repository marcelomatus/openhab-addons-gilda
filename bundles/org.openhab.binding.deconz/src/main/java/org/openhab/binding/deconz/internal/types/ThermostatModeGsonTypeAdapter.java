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
package org.openhab.binding.deconz.internal.types;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Custom (de)serializer for {@link ThermostatMode}
 *
 * @author Lukas Agethen - Initial contribution
 */
public class ThermostatModeGsonTypeAdapter implements JsonDeserializer<ThermostatMode>, JsonSerializer<ThermostatMode> {
    @Override
    public ThermostatMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String s = json.getAsString();
        return s == null ? ThermostatMode.UNKNOWN : ThermostatMode.fromString(s);
    }

    @Override
    public JsonElement serialize(ThermostatMode src, Type typeOfSrc, JsonSerializationContext context)
            throws JsonParseException {
        return new JsonPrimitive(src != ThermostatMode.UNKNOWN ? src.getDeconzValue() : null);
    }
}
