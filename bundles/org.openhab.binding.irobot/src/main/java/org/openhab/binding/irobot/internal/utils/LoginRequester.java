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
package org.openhab.binding.irobot.internal.utils;

import static org.openhab.binding.irobot.internal.IRobotBindingConstants.MQTT_PORT;
import static org.openhab.binding.irobot.internal.IRobotBindingConstants.TRUST_MANAGERS;
import static org.openhab.binding.irobot.internal.IRobotBindingConstants.UDP_PORT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;

/**
 * Helper functions to get blid and password. Seems pretty much reinventing a bicycle,
 * but it looks like HiveMq doesn't provide for sending and receiving custom packets.
 * The {@link LoginRequester#getBlid} and {@link IRobotDiscoveryService} are heavily
 * related to each other.
 *
 * @author Pavel Fedin - Initial contribution
 * @author Alexander Falkenstern - Fix password fetching
 *
 */
@NonNullByDefault
public class LoginRequester {
    public static @Nullable String getBlid(final String ip) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(1000); // One second
        socket.setReuseAddress(true);

        final byte[] bRequest = "irobotmcs".getBytes(StandardCharsets.UTF_8);
        DatagramPacket request = new DatagramPacket(bRequest, bRequest.length, InetAddress.getByName(ip), UDP_PORT);
        socket.send(request);

        byte[] reply = new byte[1024];
        try {
            DatagramPacket packet = new DatagramPacket(reply, reply.length);
            socket.receive(packet);
            reply = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength());
        } catch (IOException exception) {
        } finally {
            socket.close();
        }

        Configuration.ConfigurationBuilder builder = Configuration.builder();
        builder = builder.jsonProvider(new GsonJsonProvider());
        builder = builder.mappingProvider(new GsonMappingProvider());
        builder = builder.options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS);

        final String json = new String(reply, 0, reply.length, StandardCharsets.UTF_8);
        DocumentContext document = JsonPath.using(builder.build()).parse(json);

        @Nullable
        String blid = document.read("$.robotid", String.class);
        final @Nullable String hostname = document.read("$.hostname", String.class);
        if (((blid == null) || blid.isEmpty()) && ((hostname != null) && !hostname.isEmpty())) {
            String[] parts = hostname.split("-");
            if (parts.length == 2) {
                blid = parts[1];
            }
        }

        return blid;
    }

    public static @Nullable String getPassword(final String ip)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        String password = null;

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, TRUST_MANAGERS, new java.security.SecureRandom());

        Socket socket = context.getSocketFactory().createSocket(ip, MQTT_PORT);
        socket.setSoTimeout(3000);

        // 1st byte: MQTT reserved message: 0xF0
        // 2nd byte: Data length: 0x05
        // from 3d byte magic packet data: 0xEFCC3B2900
        final byte[] request = { (byte) 0xF0, (byte) 0x05, (byte) 0xEF, (byte) 0xCC, (byte) 0x3B, (byte) 0x29, 0x00 };
        socket.getOutputStream().write(request);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            socket.getInputStream().transferTo(buffer);
        } catch (IOException exception) {
            // Roomba 980 send no properly EOF, so eat the exception
        } finally {
            socket.close();
            buffer.flush();
        }

        final byte[] reply = buffer.toByteArray();
        if ((reply.length > request.length) && (reply.length == reply[1] + 2)) { // Add 2 bytes, see request doc above
            reply[1] = request[1]; // Hack, that we can find request packet in reply
            if (Arrays.equals(request, 0, request.length, reply, 0, request.length)) {
                password = new String(Arrays.copyOfRange(reply, request.length, reply.length), StandardCharsets.UTF_8);
            }
        }

        return password;
    }
}
