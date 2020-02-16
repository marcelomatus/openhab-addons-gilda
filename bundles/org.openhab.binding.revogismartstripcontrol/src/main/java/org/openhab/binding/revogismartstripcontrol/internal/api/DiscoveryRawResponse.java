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
package org.openhab.binding.revogismartstripcontrol.internal.api;

import java.util.Objects;

/**
 * @author Andi Bräu - Initial contribution
 */
public class DiscoveryRawResponse {

    private int response;
    private DiscoveryResponse data;

    public DiscoveryRawResponse() {
    }

    public DiscoveryRawResponse(int response, DiscoveryResponse data) {
        this.response = response;
        this.data = data;
    }

    public int getResponse() {
        return response;
    }

    public DiscoveryResponse getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveryRawResponse that = (DiscoveryRawResponse) o;
        return response == that.response &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response, data);
    }
}