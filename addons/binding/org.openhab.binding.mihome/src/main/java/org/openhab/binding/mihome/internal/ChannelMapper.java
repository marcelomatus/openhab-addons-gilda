/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.CommonTriggerEvents;

/**
 * Maps the various JSON Strings reported from the devices to Channels
 *
 * @author Dieter Schmidt
 */
public class ChannelMapper {

    private static final Map<String, String> systemButtonMapper = new HashMap<String, String>();

    static {
        // Alphabetical order
        systemButtonMapper.put("CLICK", CommonTriggerEvents.SHORT_PRESSED);
        systemButtonMapper.put("DOUBLE_CLICK", CommonTriggerEvents.DOUBLE_PRESSED);
        systemButtonMapper.put("LONG_CLICK_PRESS", CommonTriggerEvents.LONG_PRESSED);
        systemButtonMapper.put("LONG_CLICK_RELEASE", "LONG_RELEASED");
    }

    public static String getChannelEvent(String reportedString) {
        String ret = systemButtonMapper.get(reportedString);
        if (ret != null) {
            return ret;
        } else {
            return "UNKNOWN_EVENT";
        }
    }
}
