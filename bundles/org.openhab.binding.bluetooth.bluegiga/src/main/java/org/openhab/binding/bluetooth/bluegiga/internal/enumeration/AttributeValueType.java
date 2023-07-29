/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.bluetooth.bluegiga.internal.enumeration;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class to implement the BlueGiga Enumeration <b>AttributeValueType</b>.
 * <p>
 * These enumerations are in the Attribute Client class
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public enum AttributeValueType {
    /**
     * Default unknown value
     */
    UNKNOWN(-1),

    /**
     * [0] Value was read
     */
    ATTCLIENT_ATTRIBUTE_VALUE_TYPE_READ(0x0000),

    /**
     * [1] Value was notified
     */
    ATTCLIENT_ATTRIBUTE_VALUE_TYPE_NOTIFY(0x0001),

    /**
     * [2] Value was indicated
     */
    ATTCLIENT_ATTRIBUTE_VALUE_TYPE_INDICATE(0x0002),

    /**
     * [3] Value was read
     */
    ATTCLIENT_ATTRIBUTE_VALUE_TYPE_READ_BY_TYPE(0x0003),

    /**
     * [4] Value was part of a long attribute
     */
    ATTCLIENT_ATTRIBUTE_VALUE_TYPE_READ_BLOB(0x0004),

    /**
     * [5] Value was indicated and the remote device is waiting for a confirmation. Indicate
     * Confirm command can be used to send a confirmation.
     */
    ATTCLIENT_ATTRIBUTE_VALUE_TYPE_INDICATE_RSP_REQ(0x0005);

    /**
     * A mapping between the integer code and its corresponding type to
     * facilitate lookup by code.
     */
    private static @Nullable Map<Integer, AttributeValueType> codeMapping;

    private int key;

    private AttributeValueType(int key) {
        this.key = key;
    }

    /**
     * Lookup function based on the type code. Returns {@link UNKNOWN} if the code does not exist.
     *
     * @param attributeValueType
     *            the code to lookup
     * @return enumeration value.
     */
    public static AttributeValueType getAttributeValueType(int attributeValueType) {
        Map<Integer, AttributeValueType> localCodeMapping = codeMapping;
        if (localCodeMapping == null) {
            localCodeMapping = new HashMap<>();
            for (AttributeValueType s : values()) {
                localCodeMapping.put(s.key, s);
            }
            codeMapping = localCodeMapping;
        }

        return localCodeMapping.getOrDefault(attributeValueType, UNKNOWN);
    }

    /**
     * Returns the BlueGiga protocol defined value for this enum
     *
     * @return the BGAPI enumeration key
     */
    public int getKey() {
        return key;
    }
}
