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
package org.openhab.binding.bluetooth.bluegiga.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main handler class for interacting with the BlueGiga serial API. This class provides conversion of packets from
 * the serial stream into command and response classes.
 *
 * @author Chris Jackson - Initial contribution and API
 * @author Pauli Anttila - Split serial handler and transaction management
 *
 */
public class BlueGigaSerialHandler {

    private static final int BLE_MAX_LENGTH = 64;

    private final Logger logger = LoggerFactory.getLogger(BlueGigaSerialHandler.class);

    /**
     * The event listeners will be notified of any asynchronous events
     */
    private final Set<BlueGigaSerialEventListener> eventListeners = new CopyOnWriteArraySet<>();

    /**
     * The event listeners will be notified of any life-cycle events of the handler.
     */
    private final Set<BlueGigaHandlerListener> handlerListeners = new CopyOnWriteArraySet<>();

    /**
     * Flag reflecting that parser has been closed and parser parserThread
     * should exit.
     */
    private boolean close = false;

    private final OutputStream outputStream;
    private final InputStream inputStream;
    private Thread parserThread = null;

    public BlueGigaSerialHandler(final InputStream inputStream, final OutputStream outputStream) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;

        parserThread = createBlueGigaBLEHandler();
        parserThread.setDaemon(true);
        parserThread.start();
        int tries = 0;

        // wait until the daemon thread kicks off, e.g. when it is ready to receive any commands
        while (parserThread.getState() == Thread.State.NEW) {
            try {
                Thread.sleep(100);
                tries++;
                if (tries > 10) {
                    throw new IllegalStateException("BlueGiga handler thread failed to start");
                }
            } catch (InterruptedException ignore) {
                /* ignore */
            }
        }
    }

    /**
     * Requests parser thread to shutdown. Waits forever while the parser thread is getting shut down.
     */
    public void close() {
        close(0);
    }

    /**
     * Requests parser thread to shutdown. Waits specified milliseconds while the parser thread is getting shut down.
     *
     * @param timeout milliseconds to wait
     */
    public void close(long timeout) {
        close = true;
        try {
            parserThread.interrupt();
            // Give a fair chance to shutdown nicely
            Thread.sleep(50);
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
            parserThread.join(0);
        } catch (InterruptedException e) {
            logger.warn("Interrupted in packet parser thread shutdown join.");
        }

        handlerListeners.clear();
        eventListeners.clear();
        logger.debug("Closed");
    }

    /**
     * Checks if parser thread is alive.
     *
     * @return true if parser thread is alive.
     */
    public boolean isAlive() {
        return parserThread != null && parserThread.isAlive() && !close;
    }

    public void sendFrame(BlueGigaCommand bleFrame) throws IllegalStateException {
        checkIfAlive();

        // Send the data
        logger.debug("sendFrame: {}", bleFrame);
        try {
            int[] payload = bleFrame.serialize();
            if (logger.isTraceEnabled()) {
                logger.trace("BLE TX: {}", printHex(payload, payload.length));
            }
            for (int b : payload) {
                outputStream.write(b);
            }

        } catch (IOException e) {
            throw new BlueGigaException("Error sending BLE frame", e);
        }
    }

    public void addEventListener(BlueGigaSerialEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(BlueGigaSerialEventListener listener) {
        eventListeners.remove(listener);
    }

    public void addHandlerListener(BlueGigaHandlerListener listener) {
        handlerListeners.add(listener);
    }

    public void removeHandlerListener(BlueGigaHandlerListener listener) {
        handlerListeners.remove(listener);
    }

    /**
     * Notify any transaction listeners when we receive a response.
     *
     * @param response the response data received
     */
    private void notifyEventListeners(final BlueGigaResponse response) {
        // Notify the listeners
        for (final BlueGigaSerialEventListener listener : eventListeners) {
            try {
                listener.bluegigaFrameReceived(response);
            } catch (Exception ex) {
                logger.warn("Execution error of a BlueGigaHandlerListener listener.", ex);
            }
        }
    }

    /**
     * Notify handler event listeners that the handler was bluegigaClosed due to an error specified as an argument.
     *
     * @param reason the reason to bluegigaClosed
     */
    private void notifyEventListeners(final Exception reason) {
        // It should be safe enough not to use the NotificationService as this is a fatal error, no any further actions
        // can be done with the handler, a new handler should be re-created
        // There is another reason why NotificationService can't be used - the listeners should be notified immediately
        for (final BlueGigaHandlerListener listener : handlerListeners) {
            try {
                listener.bluegigaClosed(reason);
            } catch (Exception ex) {
                logger.warn("Execution error of a BlueGigaHandlerListener listener.", ex);
            }
        }
    }

    private String printHex(int[] data, int len) {
        StringBuilder builder = new StringBuilder();

        for (int cnt = 0; cnt < len; cnt++) {
            builder.append(String.format("%02X ", data[cnt]));
        }

        return builder.toString();
    }

    private void checkIfAlive() {
        if (!isAlive()) {
            throw new IllegalStateException("Bluegiga handler is dead. Most likely because of IO errors. "
                    + "Re-initialization of the BlueGigaSerialHandler is required.");
        }
    }

    private Thread createBlueGigaBLEHandler() {
        final int framecheckParams[] = new int[] { 0x00, 0x7F, 0xC0, 0xF8, 0xE0 };
        return new Thread("BlueGigaBLEHandler") {
            @Override
            public void run() {
                int exceptionCnt = 0;
                logger.debug("BlueGiga BLE thread started");
                int[] inputBuffer = new int[BLE_MAX_LENGTH];
                int inputCount = 0;
                int inputLength = 0;

                while (!close) {
                    try {
                        int val = inputStream.read();
                        if (val == -1) {
                            continue;
                        }

                        inputBuffer[inputCount++] = val;

                        if (inputCount < 4) {
                            // The BGAPI protocol has no packet framing, and no error detection, so we do a few
                            // sanity checks on the header to try and allow resyncronisation should there be an
                            // error.
                            // Byte 0: Check technology type is bluetooth and high length is 0
                            // Byte 1: Check length is less than 64 bytes
                            // Byte 2: Check class ID is less than 8
                            // Byte 3: Check command ID is less than 16
                            if ((val & framecheckParams[inputCount]) != 0) {
                                logger.debug("BlueGiga framing error byte {} = {}", inputCount, val);
                                inputCount = 0;
                                continue;
                            }
                        } else if (inputCount == 4) {
                            // Process the header to get the length
                            inputLength = inputBuffer[1] + (inputBuffer[0] & 0x02 << 8) + 4;
                            if (inputLength > 64) {
                                logger.error("BLE length larger than 64 bytes ({})", inputLength);
                            }
                        }
                        if (inputCount == inputLength) {
                            // End of packet reached - process
                            BlueGigaResponse responsePacket = BlueGigaResponsePackets.getPacket(inputBuffer);

                            if (logger.isTraceEnabled()) {
                                logger.trace("BLE RX: {}", printHex(inputBuffer, inputLength));
                                logger.trace("BLE RX: {}", responsePacket);
                            }
                            if (responsePacket != null) {
                                notifyEventListeners(responsePacket);
                            }

                            inputCount = 0;
                        }

                    } catch (final IOException e) {
                        logger.error("BlueGiga BLE IOException: ", e);

                        if (exceptionCnt++ > 10) {
                            logger.error("BlueGiga BLE exception count exceeded");
                            close = true;
                            notifyEventListeners(e);
                        }
                    }
                }
                logger.debug("BlueGiga BLE exited.");
            }
        };
    }
}
