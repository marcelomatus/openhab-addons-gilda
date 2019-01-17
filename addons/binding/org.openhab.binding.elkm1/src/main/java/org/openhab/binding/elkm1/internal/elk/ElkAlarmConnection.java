/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elkm1.internal.elk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.openhab.binding.elkm1.internal.config.ElkAlarmConfig;
import org.openhab.binding.elkm1.internal.elk.message.EthernetModuleTestReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * The connection to the elk, handles the socket and other pieces.
 *
 * @author David Bennett - Initial Contribution
 */
public class ElkAlarmConnection {
    private final Logger logger = LoggerFactory.getLogger(ElkAlarmConnection.class);
    private final ElkAlarmConfig config;
    private final ElkMessageFactory factory;
    private Socket socket;
    private boolean running = false;
    private boolean sentSomethiung = false;
    private Thread elkAlarmThread;
    private List<ElkListener> listeners = Lists.newArrayList();
    private Queue<ElkMessage> toSend = new ArrayBlockingQueue<>(100);

    /**
     * Create the connection to the alarm.
     *
     * @param config The configuration of the elk config
     * @param factory The message factory to use
     */
    public ElkAlarmConnection(ElkAlarmConfig config, ElkMessageFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    /**
     * Initializes the connection by connecting to the elk and verifying we get
     * basic data back.
     *
     * @return true if successfuly initialized.
     */
    public boolean initialize() {
        try {
            socket = new Socket(config.ipAddress, 2101);
            running = true;
            elkAlarmThread = new Thread(new ReadingDataThread());
            elkAlarmThread.start();
        } catch (IOException e) {
            logger.error("Unable to open connection to the elk alarm {}:{}", config.ipAddress, config.port, e);
            return false;
        }

        return socket != null && !socket.isClosed();
    }

    /**
     * Called to shutdown the running threads and close the socket.
     */
    public void shutdown() {
        running = false;
        elkAlarmThread.interrupt();
        elkAlarmThread = null;
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                logger.error("Closing the socket", config.ipAddress, config.port, e);
            }
        }
    }

    /**
     * Sends a specific command to the elk.
     *
     * @param message The message to send.
     */
    public void sendCommand(ElkMessage message) {
        synchronized (toSend) {
            this.toSend.add(message);
        }

        if (!sentSomethiung) {
            sendActualMessage();
        }
    }

    private void sendActualMessage() {
        String sendStr;
        ElkMessage message;
        synchronized (toSend) {
            if (toSend.isEmpty()) {
                sentSomethiung = false;
                return;
            }
            message = toSend.remove();
        }
        sendStr = message.getSendableMessage() + "\r\n";
        try {
            // Try and reopen it.
            if (socket == null || socket.isClosed()) {
                socket = new Socket(config.ipAddress, config.port);
            }
            socket.getOutputStream().write(sendStr.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            logger.debug("Writing {} to alasrm", sendStr);
            sentSomethiung = true;
            if (message instanceof EthernetModuleTestReply) {
                sendActualMessage();
            }
        } catch (IOException e) {
            logger.error("Error writing to elk alarm {}:{}", config.ipAddress, config.port, e);
            running = false;
            try {
                socket.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                logger.error("Error closing socket to elk alarm {}:{}", config.ipAddress, config.port, e);
            }
            socket = null;
        }

    }

    /**
     * Adds the elk listener into the list of things listening for messages.
     */
    public void addElkListener(ElkListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the elk listener into the list of things listening for messages.
     */
    public void removeElkListener(ElkListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    class ReadingDataThread implements Runnable {

        /** The reading thread to get data from the elk. */
        @Override
        public void run() {
            logger.info("Starting to run the reading thread.");
            BufferedReader buff;
            try {
                buff = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            } catch (IOException e1) {
                logger.error("Unable to setup the reader for the elk alarm {}:{}", config.ipAddress, config.port, e1);
                running = false;
                return;
            }
            while (running) {
                try {
                    String line = buff.readLine();
                    logger.debug("Received {} from alarm", line);
                    // Got our line. Yay.
                    ElkMessage message = factory.createMessage(line);
                    if (message != null) {
                        synchronized (listeners) {
                            for (ElkListener listen : listeners) {
                                listen.handleElkMessage(message);
                            }
                        }
                        logger.debug("Dispatched elk message {} {}", message.toString(), line);
                    } else {
                        logger.info("Unknown elk message {}", line);
                    }
                    // See if we need to send a message too.
                    sendActualMessage();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    logger.error("Error reading line from elk alarm {}:{}", config.ipAddress, config.port, e);
                }
            }
        }
    }

    /**
     * Find out if there is a message already being sent of this type in the queue.
     *
     * @param classToLookup The class to look for
     * @return true if it is a sending class.
     */
    public boolean isSendingClass(Class<?> classToLookup) {
        synchronized (toSend) {
            for (ElkMessage message : toSend) {
                if (message.getClass().isAssignableFrom(classToLookup)) {
                    return true;
                }
            }
            return false;
        }
    }
}
