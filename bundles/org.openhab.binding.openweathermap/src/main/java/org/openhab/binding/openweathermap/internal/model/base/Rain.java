/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.openweathermap.internal.model.base;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Generated Plain Old Java Objects class for {@link Rain} from JSON.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class Rain {
    @SerializedName("1h")
    private @Nullable Double oneHour;
    @SerializedName("3h")
    private @Nullable Double threeHours;

    public @Nullable Double get1h() {
        return oneHour;
    }

    public void set1h(Double oneHour) {
        this.oneHour = oneHour;
    }

    public @Nullable Double get3h() {
        return threeHours;
    }

    public void set3h(Double threeHours) {
        this.threeHours = threeHours;
    }

    public Double getVolume() {
        return oneHour != null ? oneHour : threeHours != null ? threeHours / 3 : 0;
    }
}
