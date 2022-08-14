/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.liquidcheck.internal.json;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

/**
 * The {@link ResponseTest} .
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
public class ResponseTest {

    @Nullable
    Response response;

    @BeforeEach
    void setUp() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("src/test/resources/Example.json"),
                    StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line);
            }
            String json = sb.toString();
            response = new Gson().fromJson(json, Response.class);
        } catch (Exception e) {
            return;
        }
    }

    @Test
    public void jsonTest() {
        Response response = this.response;
        if (response != null) {
            assertThat(response.header.namespace, is(equalTo("Device")));
            assertThat(response.header.name, is(equalTo("Response")));
            assertThat(response.header.messageId, is(equalTo("499C7D21-F9579A3C")));
            assertThat(response.header.payloadVersion, is(equalTo("1")));
            assertThat(response.header.authorization, is(equalTo("1C9DC262BE70-00038BC8-TX0K103HIXCXVLTBMVKVXFF")));
            assertThat(response.payload.measure.level, is(equalTo(2.23)));
            assertThat(response.payload.measure.content, is(equalTo(9265)));
            assertThat(response.payload.measure.age, is(equalTo(1981)));
            assertThat(response.payload.measure.raw.level, is(equalTo(2.2276)));
            assertThat(response.payload.measure.raw.content, is(equalTo(9255.3193)));
            assertThat(response.payload.expansion.boardType, is(equalTo(-1)));
            assertThat(response.payload.expansion.oneWire, is(nullValue()));
            assertThat(response.payload.expansion.board, is(nullValue()));
            assertThat(response.payload.device.firmware, is(equalTo("1.60")));
            assertThat(response.payload.device.hardware, is(equalTo("B5")));
            assertThat(response.payload.device.name, is(equalTo("Liquid-Check")));
            assertThat(response.payload.device.manufacturer, is(equalTo("SI-Elektronik GmbH")));
            assertThat(response.payload.device.uuid, is(equalTo("0ba64a0c-7a88b168-0001")));
            assertThat(response.payload.device.model.name, is(equalTo("")));
            assertThat(response.payload.device.model.number, is(equalTo(1)));
            assertThat(response.payload.device.security.code, is(equalTo("gkzQ5uGo6ElSdUsDWKQu2A==")));
            assertThat(response.payload.system.error, is(equalTo(0)));
            assertThat(response.payload.system.uptime, is(equalTo(232392)));
            assertThat(response.payload.system.pump.totalRuns, is(equalTo(351)));
            assertThat(response.payload.system.pump.totalRuntime, is(equalTo(1249)));
            assertThat(response.payload.wifi.station.hostname, is(equalTo("Liquid-Check")));
            assertThat(response.payload.wifi.station.ip, is(equalTo("192.168.2.102")));
            assertThat(response.payload.wifi.station.gateway, is(equalTo("192.168.2.1")));
            assertThat(response.payload.wifi.station.netmask, is(equalTo("255.255.255.0")));
            assertThat(response.payload.wifi.station.mac, is(equalTo("1C:9D:C2:62:BE:70")));
            assertThat(response.payload.wifi.accessPoint.ssid, is(equalTo("WLAN-267994")));
            assertThat(response.payload.wifi.accessPoint.bssid, is(equalTo("4C:09:D4:2B:C3:97")));
            assertThat(response.payload.wifi.accessPoint.rssi, is(equalTo(-45)));
        }
    }
}
