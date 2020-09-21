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
package org.openhab.binding.miio.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Utils}
 *
 * @author Marcel Verpaalen - Initial contribution
 *
 */
@NonNullByDefault
public class UtilsTest {

    @Test
    public void obfuscateTokenTest() {
        String tokenString = "";
        assertEquals("", Utils.obfuscateToken(tokenString));

        tokenString = "6614";
        assertEquals("6614", Utils.obfuscateToken(tokenString));

        tokenString = "66147986";
        assertEquals("66147986", Utils.obfuscateToken(tokenString));

        tokenString = "6614798643fe781563c1";
        assertEquals("66147986XXXXXXXXXXXX", Utils.obfuscateToken(tokenString));

        tokenString = "6614798643fe781563c1eebe";
        assertEquals("66147986XXXXXXXXXXXXXXXX", Utils.obfuscateToken(tokenString));

        tokenString = "6614798643fe781563c1eebeda22479a";
        assertEquals("66147986XXXXXXXXXXXXXXXXda22479a", Utils.obfuscateToken(tokenString));

        tokenString = "6614798643fe781563c1eebeda22479a6614798643fe781563c1eebeda22479a";
        assertEquals("66147986XXXXXXXXXXXXXXXXda22479a6614798643fe781563c1eebeda22479a",
                Utils.obfuscateToken(tokenString));
    }
}
