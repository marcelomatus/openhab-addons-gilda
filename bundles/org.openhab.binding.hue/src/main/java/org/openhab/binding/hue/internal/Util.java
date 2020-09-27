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
package org.openhab.binding.hue.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Q42 - Initial contribution
 * @author Denis Dudnik - moved Jue library source code inside the smarthome Hue binding
 */
@NonNullByDefault
class Util {
    private Util() {
    }

    // This is used to check what byte size strings have, because the bridge doesn't natively support UTF-8
    public static int stringSize(String str) {
        return str.getBytes(StandardCharsets.UTF_8).length;
    }

    public static List<HueObject> idsToLights(List<String> ids) {
        List<HueObject> lights = new ArrayList<>();

        for (String id : ids) {
            HueObject light = new HueObject();
            light.setId(id);
            lights.add(light);
        }

        return lights;
    }

    public static List<String> lightsToIds(List<HueObject> lights) {
        List<String> ids = new ArrayList<>();

        for (HueObject light : lights) {
            ids.add(light.getId());
        }

        return ids;
    }

    public static @Nullable String quickMatch(String needle, String haystack) {
        Matcher m = Pattern.compile(needle).matcher(haystack);
        m.find();
        return m.group(1);
    }
}
