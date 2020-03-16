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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lcn.internal.common.LcnAddr;
import org.openhab.binding.lcn.internal.common.LcnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This state is active during the socket creation, host name resolving and waiting for the TCP connection to become
 * established.
 *
 * @author Fabian Wolter - Initial Contribution
 */
@NonNullByDefault
public class ConnectionStateConnecting extends AbstractConnectionState {
    private final Logger logger = LoggerFactory.getLogger(ConnectionStateConnecting.class);

    public ConnectionStateConnecting(StateContext context, ScheduledExecutorService scheduler) {
        super(context, scheduler);
    }

    @Override
    public void startWorking() {
        connection.clearRuntimeData();

        logger.debug("Connecting to {}:{} ...", connection.getSettings().getAddress(),
                connection.getSettings().getPort());

        try {
            // Open Channel by using the system-wide default AynchronousChannelGroup.
            // So, Threads are used or re-used on demand.
            AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
            // Do not wait until some buffer is filled, send PCK commands immediately
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            connection.setSocketChannel(channel);

            InetSocketAddress address = new InetSocketAddress(connection.getSettings().getAddress(),
                    connection.getSettings().getPort());

            if (address.isUnresolved()) {
                throw new LcnException("Could not resolve hostname");
            }

            channel.connect(address, null, new CompletionHandler<@Nullable Void, @Nullable Void>() {
                @Override
                public void completed(@Nullable Void result, @Nullable Void attachment) {
                    connection.readAndProcess();
                    nextState(ConnectionStateSendUsername.class);
                }

                @Override
                public void failed(@Nullable Throwable e, @Nullable Void attachment) {
                    handleConnectionFailure(e);
                }
            });
        } catch (IOException | LcnException e) {
            handleConnectionFailure(e);
        }
    }

    private void handleConnectionFailure(@Nullable Throwable e) {
        String message;
        if (e != null) {
            logger.warn("Could not connect to {}:{}: {}", connection.getSettings().getAddress(),
                    connection.getSettings().getPort(), e.getMessage());
            message = e.getMessage();
        } else {
            message = "";
        }
        connection.getCallback().onOffline(message);
        context.handleConnectionFailed(e);
    }

    @Override
    public void queue(LcnAddr addr, boolean wantsAck, ByteBuffer data) {
        connection.queueOffline(addr, wantsAck, data);
    }

    @Override
    public void onPckMessageReceived(String data) {
        // nothing
    }
}
