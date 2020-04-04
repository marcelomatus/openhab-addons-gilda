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

package org.openhab.binding.smhi.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;


/**
 * A class containing a forecast for a specific point in time.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class Forecast implements Comparable<Forecast> {
    protected ZonedDateTime validTime;
    private Map<String, @Nullable BigDecimal> parameters;

    public Forecast(ZonedDateTime validTime, Map<String, @Nullable BigDecimal> parameters) {
        this.validTime = validTime;
        this.parameters = parameters;
    }

    public ZonedDateTime getValidTime() {
        return validTime;
    }

    public Map<String, @Nullable BigDecimal> getParameters() {
        return parameters;
    }

    public @Nullable BigDecimal getParameter(String parameter) { return parameters.get(parameter); }

    @Override
    public int compareTo(Forecast o) {
        return validTime.compareTo(o.validTime);
    }
}
