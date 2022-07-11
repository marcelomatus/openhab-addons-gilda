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
package org.openhab.binding.vesync.internal.handler.requests;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.openhab.binding.vesync.internal.exceptions.AuthenticationException;
import org.openhab.binding.vesync.internal.VeSyncConstants;
import org.openhab.binding.vesync.internal.dto.requests.VesyncLoginCredentials;
import org.openhab.binding.vesync.internal.dto.requests.VesyncRequestManagedDevicesPage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@link VesyncRequestManagedDevicesPageTest} class implements unit test case for
 * {@link VesyncRequestManagedDevicesPage}
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class VesyncRequestManagedDevicesPageTest {

    @Test
    public void checkBaseFieldsExist() {
        String content = VeSyncConstants.GSON.toJson(new VesyncLoginCredentials("username", "passmd5"));

        assertEquals(true, content.contains("\"timeZone\": \"America/New_York\""));
        assertEquals(true, content.contains("\"acceptLanguage\": \"en\""));

        assertEquals(true, content.contains("\"appVersion\": \"2.5.1\""));
        assertEquals(true, content.contains("\"phoneBrand\": \"SM N9005\""));
        assertEquals(true, content.contains("\"phoneOS\": \"Android\""));

        Pattern p = Pattern.compile("\"traceId\": \"\\d+\"");
        Matcher m = p.matcher(content);
        assertEquals(true, m.find());
    }

    @Test
    public void checkRequestDevicesFields() {

        String content = "";
        try {
            content = VeSyncConstants.GSON
                    .toJson(new VesyncRequestManagedDevicesPage(org.openhab.binding.vesync.internal.handler.requests.VesyncAuthenticatedRequestTest.testUser, 1, 100));
        } catch (AuthenticationException ae) {

        }


        assertEquals(true, content.contains("\"method\": \"devices\""));
        assertEquals(true, content.contains("\"pageNo\": \"1\""));
        assertEquals(true, content.contains("\"pageSize\": \"100\""));
        assertEquals(true, content.contains("\"token\": \"AccessTokenString=\""));
        assertEquals(true, content.contains("\"accountID\": \"5328043\""));
    }
}
