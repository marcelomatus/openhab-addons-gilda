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
package org.openhab.binding.smarther.internal.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Smarther API Subscription DTO class.
 *
 * @author Fabio Possieri - Initial contribution
 */
public class Subscription {

    @SerializedName("plantId")
    private String plantId;
    @SerializedName("subscriptionId")
    private String subscriptionId;
    @SerializedName("EndPointUrl")
    private String endpointUrl;

    public String getPlantId() {
        return plantId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    @Override
    public String toString() {
        return String.format("plantId=%s, id=%s, endpoint=%s", plantId, subscriptionId, endpointUrl);
    }

}
