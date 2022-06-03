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
package org.openhab.binding.easee.internal;

import static org.openhab.binding.easee.internal.EaseeBindingConstants.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.easee.internal.model.ConfigurationException;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.type.ChannelTypeUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * trait class which contains useful helper methods. Thus, the interface can be implemented and methods are available
 * within the class.
 *
 * @author Alexander Friese - initial contribution
 */
@NonNullByDefault
public interface UtilsTrait {

    /**
     * returns a date in a readable format
     *
     * @param date
     * @return
     */
    default String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * get element as String.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    default @Nullable JsonObject getAsJsonObject(@Nullable JsonObject jsonObject, String key) {
        JsonElement json = jsonObject == null ? null : jsonObject.get(key);
        return json == null ? null : json.getAsJsonObject();
    }

    /**
     * get element as String.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    default @Nullable String getAsString(@Nullable JsonObject jsonObject, String key) {
        JsonElement json = jsonObject == null ? null : jsonObject.get(key);
        return json == null ? null : json.getAsString();
    }

    /**
     * get element as int.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    default int getAsInt(@Nullable JsonObject jsonObject, String key) {
        JsonElement json = jsonObject == null ? null : jsonObject.get(key);
        return json == null ? 0 : json.getAsInt();
    }

    /**
     * get element as boolean.
     *
     * @param jsonObject
     * @param key
     * @return
     */
    default @Nullable Boolean getAsBool(@Nullable JsonObject jsonObject, String key) {
        JsonElement json = jsonObject == null ? null : jsonObject.get(key);
        return json == null ? null : json.getAsBoolean();
    }

    /**
     * retrieves typeID of a channel.
     *
     * @param channel
     * @return typeID or empty string if typeUID is null.
     */
    default String getChannelTypeId(Channel channel) {
        ChannelTypeUID typeUID = channel.getChannelTypeUID();
        if (typeUID == null) {
            return "";
        }
        return typeUID.getId();
    }

    /**
     * retrieves the validation expression which is assigned to this channel, fallback to a default, if no validation
     * is
     * defined.
     *
     * @param channel
     * @return the validation expression
     */
    default String getValidationExpression(Channel channel) {
        String expr = getPropertyOrParameter(channel, PARAMETER_NAME_VALIDATION_REGEXP);
        if (expr == null) {
            // logger.warn("Channel {} does not have a validation expression configured", channel.getUID().getId());
            throw new ConfigurationException(
                    "channel (" + channel.getUID().getId() + ") does not have a validation expression configured");
        }
        return expr;
    }

    /**
     * retrieves the write API url suffix which is assigned to this channel.
     *
     * @param channel
     * @return the url suffix
     */
    default String getWriteCommand(Channel channel) {
        String command = getPropertyOrParameter(channel, PARAMETER_NAME_WRITE_COMMAND);
        if (command == null) {
            // logger.warn("channel {} does not have a write command configured", channel.getUID().getId());
            throw new ConfigurationException(
                    "channel (" + channel.getUID().getId() + ") does not have a write command configured");
        }
        return command;
    }

    /**
     * internal utiliy method which returns a property (if found) or a config parameter (if found) otherwise null
     *
     * @param channel
     * @param name
     * @return
     */
    default @Nullable String getPropertyOrParameter(Channel channel, String name) {
        String value = channel.getProperties().get(name);
        // also eclipse says this cannot be null, it definitely can!
        if (value == null || value.isEmpty()) {
            Object obj = channel.getConfiguration().get(name);
            value = obj == null ? null : obj.toString();
        }
        return value;
    }
}
