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
package org.openhab.binding.freebox.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freebox.internal.api.RequestAnnotation;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link WifiConfig} is the Java class used to map the "WifiGlobalConfig"
 * structure used by the Wifi global configuration API
 * https://dev.freebox.fr/sdk/os/wifi/#
 *
 * @author Laurent Garnier - Initial contribution
 */
@RequestAnnotation(responseClass = WifiConfigResponse.class, relativeUrl = "wifi/config/", retryAuth = true, method = "PUT")
@NonNullByDefault
public class WifiConfig {
    public static enum FilterState {
        UNKNOWN,
        @SerializedName("disabled")
        DISABLED,
        @SerializedName("whitelist")
        WHITELIST,
        @SerializedName("blacklist")
        BLACKLIST;
    }

    private Boolean enabled = Boolean.FALSE;
    private FilterState macFilterState = FilterState.UNKNOWN;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public FilterState getMacFilterState() {
        return macFilterState;
    }

    public void setMacFilterState(FilterState macFilterState) {
        this.macFilterState = macFilterState;
    }
}
