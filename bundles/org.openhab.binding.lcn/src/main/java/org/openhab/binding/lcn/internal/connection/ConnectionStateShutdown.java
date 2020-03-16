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

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.lcn.internal.common.LcnAddr;

/**
 * This state is entered when the connection shall be shut-down finally. This happens when Thing.dispose() is called.
 *
 * @author Fabian Wolter - Initial Contribution
 */
@NonNullByDefault
public class ConnectionStateShutdown extends AbstractConnectionState {
    public ConnectionStateShutdown(StateContext context, ScheduledExecutorService scheduler) {
        super(context, scheduler);
    }

    @Override
    public void startWorking() {
        closeSocketChannel();

        // end state
    }

    @Override
    public void queue(LcnAddr addr, boolean wantsAck, ByteBuffer data) {
        // nothing
    }

    @Override
    public void onPckMessageReceived(String data) {
        // nothing
    }
}
