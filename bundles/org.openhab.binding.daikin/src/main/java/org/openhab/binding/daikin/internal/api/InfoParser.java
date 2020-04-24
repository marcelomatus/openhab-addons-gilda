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
package org.openhab.binding.daikin.internal.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;

import org.openhab.binding.daikin.internal.api.Enums.FanMovement;
import org.openhab.binding.daikin.internal.api.Enums.FanSpeed;
import org.openhab.binding.daikin.internal.api.Enums.Mode;

/**
 * Class for parsing the comma separated values returned by the Daikin Controller.
 *
 * @author Jimmy Tanagra - Initial Contribution
 *
 */
@NonNullByDefault
public class InfoParser {
    private InfoParser() {
    }

    public static Map<String, String> parse(String response) {
        return Arrays.asList(response.split(",")).stream().filter(kv -> kv.contains("="))
                .map(kv -> {
                    String[] keyValue = kv.split("=");
                    String key = keyValue[0];
                    String value = keyValue.length > 1 ? keyValue[1] : "";
                    return new String[] { key, value };
                }).collect(Collectors.toMap(x -> x[0], x -> x[1]));
    }

    public static Optional<Double> parseDouble(String value) {
        if ("-".equals(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parseInt(String value) {
        if ("-".equals(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
