/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.luxom.internal.handler.config;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link LuxomBridgeConfig} is the general config class for Luxom Bridges.
 *
 * @author Kris Jespers - Initial contribution
 */
@NonNullByDefault
public class LuxomBridgeConfig {
    public @Nullable String ipAddress;
    public int port;

    /**
     * reconnect afte X minutes when disconnected
     */
    public int reconnectInterval;
    public int aliveCheckInterval;
    public int delay = 0;

    /**
     * if true, on communication error the devices will NOT go offline...
     * if false, they will go offline. In both instances they will get (re)pinged after reconnect.
     *
     */
    public boolean useFastReconnect = false;

    public boolean sameConnectionParameters(LuxomBridgeConfig config) {
        return StringUtils.equals(ipAddress, config.ipAddress) && config.port == port
                && (reconnectInterval == config.reconnectInterval) && (aliveCheckInterval == config.aliveCheckInterval)
                && (delay == config.delay);
    }
}
