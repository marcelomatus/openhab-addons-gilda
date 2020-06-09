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
package org.openhab.binding.lcn.internal.connection;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lcn.internal.common.LcnAddr;
import org.openhab.binding.lcn.internal.common.LcnDefs;
import org.openhab.binding.lcn.internal.common.LcnException;

/**
 * This state waits for the status answer of the LCN-PCK gateway after connection establishment, rather the LCN bus is
 * connected.
 *
 * @author Fabian Wolter - Initial Contribution
 */
@NonNullByDefault
public class ConnectionStateWaitForLcnBusConnected extends AbstractConnectionState {
    private @Nullable ScheduledFuture<?> legacyTimer;

    public ConnectionStateWaitForLcnBusConnected(StateContext context, ScheduledExecutorService scheduler) {
        super(context, scheduler);
    }

    @Override
    public void startWorking() {
        // Legacy support for LCN-PCHK 2.2 and earlier:
        // There was no explicit "LCN connected" notification after successful authentication.
        // Only "LCN disconnected" would be reported immediately. That means "LCN connected" used to be the default.
        ScheduledFuture<?> localLegacyTimer = legacyTimer = scheduler.schedule(() -> {
            connection.getCallback().onOnline();
            nextState(ConnectionStateSendDimMode.class);
        }, connection.getSettings().getTimeout(), TimeUnit.MILLISECONDS);
        addTimer(localLegacyTimer);
    }

    @Override
    public void queue(LcnAddr addr, boolean wantsAck, byte[] data) {
        connection.queueOffline(addr, wantsAck, data);
    }

    @Override
    public void onPckMessageReceived(String data) {
        ScheduledFuture<?> localLegacyTimer = legacyTimer;
        if (data.equals(LcnDefs.LCNCONNSTATE_DISCONNECTED)) {
            if (localLegacyTimer != null) {
                localLegacyTimer.cancel(true);
            }
            connection.getCallback().onOffline("LCN bus not connected to LCN-PCHK/PKE");
        } else if (data.equals(LcnDefs.LCNCONNSTATE_CONNECTED)) {
            if (localLegacyTimer != null) {
                localLegacyTimer.cancel(true);
            }
            connection.getCallback().onOnline();
            nextState(ConnectionStateSendDimMode.class);
        } else if (data.equals(LcnDefs.INSUFFICIENT_LICENSES)) {
            context.handleConnectionFailed(
                    new LcnException("LCN-PCHK/PKE has not enough licenses to handle this connection"));
        }
    }
}
