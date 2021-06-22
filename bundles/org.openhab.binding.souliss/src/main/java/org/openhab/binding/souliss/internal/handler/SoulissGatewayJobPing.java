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
package org.openhab.binding.souliss.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.protocol.CommonCommands;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.binding.BridgeHandler;

/**
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */

@NonNullByDefault
public class SoulissGatewayJobPing implements Runnable {

    private final CommonCommands soulissCommands = new CommonCommands();

    @Nullable
    private SoulissGatewayHandler gwHandler;

    @SuppressWarnings("null")
    public SoulissGatewayJobPing(Bridge bridge) {
        BridgeHandler bridgeHandler = bridge.getHandler();
        if (bridgeHandler != null) {
            gwHandler = (SoulissGatewayHandler) bridgeHandler;
        }
    }

    @Override
    public void run() {
        sendPing();
        @Nullable
        SoulissGatewayHandler localGwHandler = this.gwHandler;
        if (localGwHandler != null) {
            localGwHandler.pingSent();
        }
    }

    private void sendPing() {
        // sending ping packet

        if (this.gwHandler != null && this.gwHandler.gwConfig.gatewayIpAddress != null
                && this.gwHandler.gwConfig.gatewayIpAddress.length() > 0) {
            soulissCommands.sendPing(this.gwHandler.gwConfig.gatewayIpAddress, (byte) this.gwHandler.gwConfig.nodeIndex,
                    (byte) this.gwHandler.gwConfig.userIndex, (byte) 0, (byte) 0);
            // ping packet sent
        }
    }

    public int getPingRefreshInterval() {
        if (this.gwHandler != null) {
            return this.gwHandler.gwConfig.pingInterval;
        }
        return -1;
    }
}
