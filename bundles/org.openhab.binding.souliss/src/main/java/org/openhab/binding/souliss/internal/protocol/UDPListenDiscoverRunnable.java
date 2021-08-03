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
package org.openhab.binding.souliss.internal.protocol;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.discovery.DiscoverResult;
import org.openhab.binding.souliss.internal.handler.SoulissGatewayHandler;
import org.openhab.core.thing.Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provide receive packet from network
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 * @author Alessandro Del Pex - Souliss App
 */
@NonNullByDefault
public class UDPListenDiscoverRunnable implements Runnable {

    protected boolean bExit = false;
    @Nullable
    UDPDecoder decoder = null;

    private final Logger logger = LoggerFactory.getLogger(UDPListenDiscoverRunnable.class);

    @Nullable
    private SoulissGatewayHandler gwHandler;

    public UDPListenDiscoverRunnable(Bridge bridge, @Nullable DiscoverResult pDiscoverResult) {
        this.gwHandler = (SoulissGatewayHandler) bridge.getHandler();
        decoder = new UDPDecoder(bridge, pDiscoverResult);
    }

    @Override
    public void run() {
        DatagramSocket socket = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // open socket for listening...
                var channel = DatagramChannel.open();
                socket = channel.socket();

                socket.setReuseAddress(true);
                socket.setBroadcast(true);

                var localGwHandler = this.gwHandler;
                if (localGwHandler != null) {
                    var sa = new InetSocketAddress(localGwHandler.gwConfig.preferredLocalPortNumber);
                    socket.bind(sa);

                    var buf = new byte[200];
                    // receive request
                    final var packet = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(60000);
                    socket.receive(packet);
                    buf = packet.getData();

                    // **************** DECODER ********************
                    logger.debug("Packet received (port {}) {}", socket.getLocalPort(), macacoToString(buf));

                    var localDecoder = this.decoder;
                    if (localDecoder != null) {
                        localDecoder.decodeVNetDatagram(packet);
                    }
                }

            } catch (BindException e) {
                logger.error("UDP Port busy, Souliss already listening? {} ", e.getLocalizedMessage());
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e1) {
                    logger.error("UDP socket close failed: {} ", e1.getLocalizedMessage());
                }
            } catch (SocketTimeoutException e2) {
                logger.warn("UDP SocketTimeoutException close: {}", e2.getLocalizedMessage());
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ee) {
                logger.error("Exception receiving-decoding message: {} ", ee.getLocalizedMessage());
                ee.printStackTrace();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }

    private String macacoToString(byte[] frame) {
        var sb = new StringBuilder();
        sb.append("HEX: [");
        for (byte b : frame) {
            sb.append(String.format("%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }
}
