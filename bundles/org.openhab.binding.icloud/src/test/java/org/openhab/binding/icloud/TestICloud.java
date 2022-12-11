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
package org.openhab.binding.icloud;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openhab.binding.icloud.internal.ICloudApiResponseException;
import org.openhab.binding.icloud.internal.ICloudDeviceInformationParser;
import org.openhab.binding.icloud.internal.ICloudService;
import org.openhab.binding.icloud.internal.ICloudSession;
import org.openhab.binding.icloud.internal.json.response.ICloudAccountDataResponse;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 *
 * Class to test/experiment with iCloud api.
 *
 * @author Simon Spielmann
 */
@NonNullByDefault
public class TestICloud {

    @Nullable
    private final String E_MAIL = System.getProperty("icloud.test.email");

    @Nullable
    private final String PW = System.getProperty("icloud.test.pw");

    @BeforeEach
    private void setUp() {
        final Logger logger = (Logger) LoggerFactory.getLogger(ICloudSession.class);
        logger.setLevel(Level.DEBUG);
    }

    @Test
    @EnabledIfSystemProperty(named = "icloud.test.email", matches = ".*", disabledReason = "Only for manual execution.")
    public void testAuth() throws IOException, InterruptedException, ICloudApiResponseException, JsonSyntaxException {

        File jsonStorageFile = new File(System.getProperty("user.home"), "openhab.json");
        System.out.println(jsonStorageFile.toString());

        JsonStorage<String> stateStorage = new JsonStorage<String>(jsonStorageFile, TestICloud.class.getClassLoader(),
                0, 1000, 1000, List.of());

        ICloudService service = new ICloudService(this.E_MAIL, this.PW, stateStorage);
        service.authenticate(false);
        if (service.requires2fa()) {
            System.out.print("Code: ");
            String code = new Scanner(System.in).nextLine();
            assertTrue(service.validate2faCode(code));
            if (!service.isTrustedSession()) {
                service.trustSession();
            }
            if (!service.isTrustedSession()) {
                System.err.println("Trust failed!!!");
            }

        }
        ICloudAccountDataResponse deviceInfo = new ICloudDeviceInformationParser()
                .parse(service.getDevices().refreshClient());
        assertNotNull(deviceInfo);
        stateStorage.flush();
    }
}
