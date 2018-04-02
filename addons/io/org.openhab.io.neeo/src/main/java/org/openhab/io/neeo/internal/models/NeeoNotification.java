/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.neeo.internal.models;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.neeo.internal.NeeoUtil;

/**
 * The model representing an NEEO notification (serialize/deserialize json use only).
 *
 * @author Tim Roberts
 */
public class NeeoNotification {

    /** The type of notification */
    private final String type;

    /** The value of the notification */
    private final Object data;

    /**
     * Instantiates a new neeo notification from the key, item name and data.
     *
     * @param deviceKey the non-null, non-empty device key
     * @param itemName the non-null, non-empty item name
     * @param data the possibly null, possibly empty (if a string) data
     */
    public NeeoNotification(String deviceKey, String itemName, @Nullable Object data) {
        NeeoUtil.requireNotEmpty(deviceKey, "deviceKey cannot be empty");
        NeeoUtil.requireNotEmpty(itemName, "itemName cannot be empty");
        this.type = deviceKey + ":" + itemName;
        this.data = data == null || (data instanceof String && StringUtils.isEmpty(data.toString())) ? "-" : data;
    }

    /**
     * Gets the notification type.
     *
     * @return the notification type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "NeeoNotification [type=" + type + ", data=" + data + "]";
    }
}
