/**
 * Copyright (c) 2010-2018 Contributors to the openHAB project
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
package org.openhab.binding.ihc.internal.converters;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSBooleanValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSDateValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSEnumValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSFloatingPointValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSIntegerValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSResourceValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSTimeValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSTimerValue;
import org.openhab.binding.ihc.internal.ws.resourcevalues.WSWeekdayValue;

/**
 * IHC / ELKO <-> openHAB data type converter factory.
 *
 * @author Pauli Anttila - Initial contribution
 */
public enum ConverterFactory {
    INSTANCE;

    private static final String ITEM_TYPE_NUMBER = "Number";
    private static final String ITEM_TYPE_SWITCH = "Switch";
    private static final String ITEM_TYPE_CONTACT = "Contact";
    private static final String ITEM_TYPE_DIMMER = "Dimmer";
    private static final String ITEM_TYPE_DATETIME = "DateTime";
    private static final String ITEM_TYPE_STRING = "String";
    private static final String ITEM_TYPE_ROLLERSHUTTER = "Rollershutter";

    private class Key {
        Class<? extends WSResourceValue> ihcType;
        Class<? extends Type> openhabType;

        Key(Class<? extends WSResourceValue> ihcType, Class<? extends Type> openhabType) {
            this.ihcType = ihcType;
            this.openhabType = openhabType;
        }

        @Override
        public int hashCode() {
            return new String(ihcType.getClass().toString() + openhabType.getClass().toString()).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;

            return key.ihcType.equals(ihcType) && key.openhabType.equals(openhabType);
        }
    }

    private Map<Key, Converter<? extends WSResourceValue, ? extends Type>> converters;

    private ConverterFactory() {
        converters = new HashMap<Key, Converter<? extends WSResourceValue, ? extends Type>>();

        converters.put(new Key(WSDateValue.class, DateTimeType.class), new DateTimeTypeWSDateValueConverter());
        converters.put(new Key(WSTimeValue.class, DateTimeType.class), new DateTimeTypeWSTimeValueConverter());
        converters.put(new Key(WSBooleanValue.class, DecimalType.class), new DecimalTypeWSBooleanValueConverter());
        converters.put(new Key(WSEnumValue.class, DecimalType.class), new DecimalTypeWSEnumValueConverter());
        converters.put(new Key(WSFloatingPointValue.class, DecimalType.class),
                new DecimalTypeWSFloatingPointValueConverter());
        converters.put(new Key(WSIntegerValue.class, DecimalType.class), new DecimalTypeWSIntegerValueConverter());
        converters.put(new Key(WSTimerValue.class, DecimalType.class), new DecimalTypeWSTimerValueConverter());
        converters.put(new Key(WSWeekdayValue.class, DecimalType.class), new DecimalTypeWSWeekdayValueConverter());
        converters.put(new Key(WSBooleanValue.class, OnOffType.class), new OnOffTypeWSBooleanValueConverter());
        converters.put(new Key(WSIntegerValue.class, OnOffType.class), new OnOffTypeWSIntegerValueConverter());
        converters.put(new Key(WSBooleanValue.class, OpenClosedType.class),
                new OpenClosedTypeWSBooleanValueConverter());
        converters.put(new Key(WSIntegerValue.class, OpenClosedType.class),
                new OpenClosedTypeWSIntegerValueConverter());
        converters.put(new Key(WSIntegerValue.class, PercentType.class), new PercentTypeWSIntegerValueConverter());
        converters.put(new Key(WSEnumValue.class, StringType.class), new StringTypeWSEnumValueConverter());
        converters.put(new Key(WSBooleanValue.class, UpDownType.class), new UpDownTypeWSBooleanValueConverter());
        converters.put(new Key(WSIntegerValue.class, UpDownType.class), new UpDownTypeWSIntegerValueConverter());
    }

    public static ConverterFactory getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public Converter<WSResourceValue, Type> getConverter(Class<? extends WSResourceValue> resourceValueType,
            Class<? extends Type> type) {
        return (Converter<WSResourceValue, Type>) converters.get(new Key(resourceValueType, type));
    }

    @SuppressWarnings("unchecked")
    public Converter<WSResourceValue, Type> getConverter(Class<? extends WSResourceValue> resourceValueType,
            String itemType) {
        Class<? extends Type> type = null;
        switch (itemType) {
            case ITEM_TYPE_SWITCH:
                type = OnOffType.class;
                break;
            case ITEM_TYPE_ROLLERSHUTTER:
            case ITEM_TYPE_DIMMER:
                type = PercentType.class;
                break;
            case ITEM_TYPE_CONTACT:
                type = OpenClosedType.class;
                break;
            case ITEM_TYPE_STRING:
                type = StringType.class;
                break;
            case ITEM_TYPE_NUMBER:
                type = DecimalType.class;
                break;
            case ITEM_TYPE_DATETIME:
                type = DateTimeType.class;
                break;
        }
        return (Converter<WSResourceValue, Type>) converters.get(new Key(resourceValueType, type));
    }
}
