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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Custom (de)serializer for {@link ThermostatMode}
 *
 * @author Lukas Agethen - Initial contribution
 */
@NonNullByDefault
public class ThermostatModeGsonTypeAdapter implements JsonDeserializer<ThermostatMode>, JsonSerializer<ThermostatMode> {
    @Override
    public ThermostatMode deserialize(@Nullable JsonElement json, @Nullable Type typeOfT,
            @Nullable JsonDeserializationContext context) throws JsonParseException {
        JsonElement jsonLocal = json;
        if (jsonLocal != null) {
            String s = jsonLocal.getAsString();
            return s == null ? ThermostatMode.UNKNOWN : ThermostatMode.fromString(s);
        }
        return ThermostatMode.UNKNOWN;
    }

    @Override
    public JsonElement serialize(ThermostatMode src, @Nullable Type typeOfSrc,
            @Nullable JsonSerializationContext context) throws JsonParseException {
        return src != ThermostatMode.UNKNOWN ? new JsonPrimitive(src.getDeconzValue()) : JsonNull.INSTANCE;

    }
}
