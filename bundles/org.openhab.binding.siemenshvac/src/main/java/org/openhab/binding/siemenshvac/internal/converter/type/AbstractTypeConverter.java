/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.siemenshvac.internal.converter.type;

import org.openhab.binding.siemenshvac.internal.converter.ConverterException;
import org.openhab.binding.siemenshvac.internal.converter.ConverterTypeException;
import org.openhab.binding.siemenshvac.internal.converter.TypeConverter;
import org.openhab.core.types.Command;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Base class for all Converters with common methods.
 *
 * @author Laurent Arnal - Initial contribution
 */
public abstract class AbstractTypeConverter implements TypeConverter {
    private final Logger logger = LoggerFactory.getLogger(AbstractTypeConverter.class);

    @Override
    public Object convertToBinding(Type type) throws ConverterException {

        if (type == UnDefType.NULL) {
            return null;
        } else if (type.getClass().isEnum()) {
            return commandToBinding((Command) type);
        } else if (!toBindingValidation(type)) {
            String errorMessage = String.format("Can't convert type %s with value '%s' to %s value",
                    type.getClass().getSimpleName(), type.toString(), this.getClass().getSimpleName());
            throw new ConverterTypeException(errorMessage);
        }

        return toBinding(type);
    }

    @Override
    public Type convertFromBinding(JsonObject dp) throws ConverterException {

        String type = null;
        JsonElement value = null;

        if (dp.has("Type")) {
            type = dp.get("Type").getAsString().trim();
        }
        if (dp.has("Value")) {
            value = dp.get("Value");
        }
        if (dp.has("EnumValue")) {
            value = dp.get("EnumValue");
        }

        if (value == null) {
            return UnDefType.NULL;
        }

        if (type == null) {
            logger.debug("siemensHvac:ReadDP:null type {}", dp);
            return UnDefType.NULL;
        }

        if (!fromBindingValidation(value, type)) {
            logger.debug("Can't convert {} value '{}' with {} for '{}'", type, value, this.getClass().getSimpleName(),
                    dp);
            return UnDefType.NULL;
        }

        return fromBinding(value, type);
    }

    /**
     * Converts an openHAB command to a SiemensHvacValue value.
     */
    protected Object commandToBinding(Command command) throws ConverterException {
        throw new ConverterException("Unsupported command " + command.getClass().getSimpleName() + " for "
                + this.getClass().getSimpleName());
    }

    /**
     * Returns true, if the conversion from openHAB to the binding is possible.
     */
    protected abstract boolean toBindingValidation(Type type);

    /**
     * Converts the type to a datapoint value.
     */
    protected abstract Object toBinding(Type type) throws ConverterException;

    /**
     * Returns true, if the conversion from the binding to openHAB is possible.
     */
    protected abstract boolean fromBindingValidation(JsonElement value, String type);

    /**
     * Converts the datapoint value to an openHAB type.
     */
    protected abstract Type fromBinding(JsonElement value, String type) throws ConverterException;

    /**
     * get underlying channel type to construct channel type UID
     *
     */
    @Override
    public abstract String getChannelType(boolean writeAccess);

    /**
     * get underlying item type on openhab side for this SiemensHvac type
     *
     */
    @Override
    public abstract String getItemType(boolean writeAccess);

    /**
     * tell if this type have different subvariant or not
     *
     */
    @Override
    public abstract boolean hasVariant();
}
