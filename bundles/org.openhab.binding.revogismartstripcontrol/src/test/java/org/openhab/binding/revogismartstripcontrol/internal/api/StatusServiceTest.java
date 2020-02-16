/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.revogismartstripcontrol.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.openhab.binding.revogismartstripcontrol.internal.udp.UdpResponse;
import org.openhab.binding.revogismartstripcontrol.internal.udp.UdpSenderService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andi Bräu - Initial contribution
 */
@NonNullByDefault
public class StatusServiceTest {

    private UdpSenderService udpSenderService = mock(UdpSenderService.class);
    private StatusService statusService = new StatusService(udpSenderService);

    @Test
    public void getStatusSuccessfully() {
        // given
        Status status = new Status(true, 200, Arrays.asList(0, 0, 0, 0, 0, 0), Arrays.asList(0, 0, 0, 0, 0, 0), Arrays.asList(0, 0, 0, 0, 0, 0));
        List<UdpResponse> statusString = Collections.singletonList(
                new UdpResponse("V3{\"response\":90,\"code\":200,\"data\":{\"switch\":[0,0,0,0,0,0],\"watt\":[0,0,0,0,0,0],\"amp\":[0,0,0,0,0,0]}}"
                        , "127.0.0.1"));
        when(udpSenderService.broadcastUpdDatagram("V3{\"sn\":\"serial\", \"cmd\": 90}")).thenReturn(statusString);

        // when
        Status statusResponse = statusService.queryStatus("serial");

        // then
        assertEquals(status, statusResponse);
    }

    @Test
    public void invalidUdpResponse() {
        // given
        List<UdpResponse> statusString = Collections.singletonList(new UdpResponse("something invalid", "12345"));
        when(udpSenderService.broadcastUpdDatagram("V3{\"sn\":\"serial\", \"cmd\": 90}")).thenReturn(statusString);

        // when
        Status status = statusService.queryStatus("serial");

        // then
        assertEquals(503, status.getResponseCode());
    }
}