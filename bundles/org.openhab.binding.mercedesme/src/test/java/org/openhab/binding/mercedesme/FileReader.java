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
package org.openhab.binding.mercedesme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mercedesme.internal.Constants;

/**
 * The {@link FileReader} Helper Util to read test resource files
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class FileReader {

    public static String readFileInString(String filename) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));) {
            StringBuilder buf = new StringBuilder();
            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                buf.append(sCurrentLine);
            }
            return buf.toString();
        } catch (IOException e) {
            // fail if file cannot be read
            assertEquals(filename, Constants.EMPTY, "Read failute " + filename);
        }
        return Constants.EMPTY;
    }

    public static byte[] readFileInBytes(String filename) {
        File file = new File(filename);
        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(file.toPath());
            return fileContent;
        } catch (IOException e) {
            // fail if file cannot be read
            assertEquals(filename, Constants.EMPTY, "Read failute " + filename);
        }
        return new byte[] { 'a' };
    }
}
