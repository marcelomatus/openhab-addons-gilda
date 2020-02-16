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
package org.openhab.binding.revogismartstripcontrol.internal.udp;

import java.util.Objects;

/**
 * The class {@link UdpResponse} represents udp reponse we expect
 *
 * @author Andi Bräu - Initial contribution
 */
public class UdpResponse {
    private String answer;
    private String ipAddress;

    public UdpResponse(String answer, String ipAddress) {
        this.answer = answer;
        this.ipAddress = ipAddress;
    }

    public String getAnswer() {
        return answer;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UdpResponse that = (UdpResponse) o;
        return answer.equals(that.answer) &&
                ipAddress.equals(that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer, ipAddress);
    }
}
