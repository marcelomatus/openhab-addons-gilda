/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fronius.internal.api;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link PowerFlowRealtimeResponse} is responsible for storing
 * the response from the powerflowrealtime api
 *
 * @author Thomas Rokohl - Initial contribution
 */
public class PowerFlowRealtimeResponse implements IBaseFroniusResponse {
    @SerializedName("Head")
    private Head head;

    @SerializedName("Body")
    private PowerFlowRealtimeBody body;

    @Override
    public Head getHead() {
        if (head == null) {
            head = new Head();
        }
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public PowerFlowRealtimeBody getBody() {
        if (body == null) {
            body = new PowerFlowRealtimeBody();
        }
        return body;
    }

    public void setBody(PowerFlowRealtimeBody body) {
        this.body = body;
    }

}
