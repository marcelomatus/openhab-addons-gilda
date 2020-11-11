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
package org.openhab.binding.epsonprojector.internal.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.io.IOUtils;
import org.openhab.binding.epsonprojector.internal.EpsonProjectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for TCP communication.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class EpsonProjectorTcpConnector implements EpsonProjectorConnector {

    private final Logger logger = LoggerFactory.getLogger(EpsonProjectorTcpConnector.class);

    private final String ip;
    private final int port;
    private Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;

    public EpsonProjectorTcpConnector(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void connect() throws EpsonProjectorException {
        logger.debug("Open connection to address'{}:{}'", ip, port);

        try {
            socket = new Socket(ip, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            throw new EpsonProjectorException(e);
        }
    }

    @Override
    public void disconnect() throws EpsonProjectorException {
        if (out != null) {
            logger.debug("Close tcp out stream");
            IOUtils.closeQuietly(out);
        }
        if (in != null) {
            logger.debug("Close tcp in stream");
            IOUtils.closeQuietly(in);
        }
        if (socket != null) {
            logger.debug("Closing socket");
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Error occurred when closing tcp socket", e);
            }
        }

        socket = null;
        out = null;
        in = null;

        logger.debug("Closed");
    }

    @Override
    public String sendMessage(String data, int timeout) throws EpsonProjectorException {
        if (in == null || out == null) {
            connect();
        }

        try {
            // flush input stream
            if (in.markSupported()) {
                in.reset();
            } else {
                while (in.available() > 0) {
                    int availableBytes = in.available();

                    if (availableBytes > 0) {
                        byte[] tmpData = new byte[availableBytes];
                        in.read(tmpData, 0, availableBytes);
                    }
                }
            }

            return sendMmsg(data, timeout);
        } catch (IOException e) {
            logger.debug("IO error occurred...reconnect and resend ones");
            disconnect();
            connect();

            try {
                return sendMmsg(data, timeout);
            } catch (IOException e1) {
                throw new EpsonProjectorException(e);
            }
        } catch (Exception e) {
            throw new EpsonProjectorException(e);
        }
    }

    private String sendMmsg(String data, int timeout) throws IOException, EpsonProjectorException {
        out.write(data.getBytes());
        out.write("\r\n".getBytes());
        out.flush();

        String resp = "";

        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        while (elapsedTime < timeout) {
            int availableBytes = in.available();
            if (availableBytes > 0) {
                byte[] tmpData = new byte[availableBytes];
                int readBytes = in.read(tmpData, 0, availableBytes);
                resp = resp.concat(new String(tmpData, 0, readBytes));

                if (resp.contains(":")) {
                    return resp;
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new EpsonProjectorException(e);
                }
            }

            elapsedTime = System.currentTimeMillis() - startTime;
        }

        return resp;
    }
}
