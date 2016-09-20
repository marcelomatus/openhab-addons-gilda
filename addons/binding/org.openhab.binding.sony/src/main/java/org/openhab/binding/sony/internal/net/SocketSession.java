/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.sony.internal.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a restartable socket connection to the underlying telnet session. Commands can be sent via
 * {@link #sendCommand(String)} and responses will be received on any {@link SocketSessionListener}
 *
 * @author Tim Roberts
 * @version $Id: $Id
 */
public class SocketSession {
    private Logger _logger = LoggerFactory.getLogger(SocketSession.class);

    /**
     * The host/ip address to connect to
     */
    private final String _host;

    /**
     * The port to connect to
     */
    private final int _port;

    /**
     * The actual socket being used. Will be null if not connected
     */
    private Socket _client;

    /**
     * The writer to the {@link #_client}. Will be null if not connected
     */
    private PrintStream _writer;

    /**
     * The reader from the {@link #_client}. Will be null if not connected
     */
    private BufferedReader _reader;

    /**
     * The {@link ResponseReader} that will be used to read from {@link #_reader}
     */
    private final ResponseReader _responseReader = new ResponseReader();

    /**
     * The responses read from the {@link #_responseReader}
     */
    private final BlockingQueue<Object> _responses = new ArrayBlockingQueue<Object>(50);

    /**
     * The dispatcher of responses from {@link #_responses}
     */
    private final Dispatcher _dispatcher = new Dispatcher();

    /**
     * The {@link SocketSessionListener} that the {@link #_dispatcher} will call
     */
    private List<SocketSessionListener> _listeners = new CopyOnWriteArrayList<SocketSessionListener>();

    /**
     * Creates the socket session from the given host and port
     *
     * @param host a non-null, non-empty host/ip address
     * @param port the port number between 1 and 65535
     */
    public SocketSession(String host, int port) {
        if (host == null || host.trim().length() == 0) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        _host = host;
        _port = port;
    }

    /**
     * Adds a {@link SocketSessionListener} to call when responses/exceptions have been received
     *
     * @param listener a non-null {@link SocketSessionListener} to use
     */
    public void addListener(SocketSessionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        _listeners.add(listener);
    }

    /**
     * Clears all listeners
     */
    public void clearListeners() {
        _listeners.clear();
    }

    /**
     * Removes a {@link SocketSessionListener} from this session
     *
     * @param listener a non-null {@link SocketSessionListener} to remove
     * @return true if removed, false otherwise
     */
    public boolean removeListener(SocketSessionListener listener) {
        return _listeners.remove(listener);
    }

    /**
     * Will attempt to connect to the {@link #_host} on port {@link #_port}. If we are current connected, will
     * {@link #disconnect()} first. Once connected, the {@link #_writer} and {@link #_reader} will be created, the
     * {@link #_dispatcher} and {@link #_responseReader} will be started.
     *
     * @throws java.io.IOException if an exception occurs during the connection attempt
     */
    public void connect() throws IOException {
        disconnect();

        _client = new Socket(_host, _port);
        _client.setKeepAlive(true);
        _client.setSoTimeout(1000); // allow reader to check to see if it should stop every 1 second

        _logger.debug("Connecting to {}:{}", _host, _port);
        _writer = new PrintStream(_client.getOutputStream());
        _reader = new BufferedReader(new InputStreamReader(_client.getInputStream()));

        new Thread(_responseReader).start();
        new Thread(_dispatcher).start();
    }

    /**
     * Disconnects from the {@link #_host} if we are {@link #isConnected()}. The {@link #_writer}, {@link #_reader} and
     * {@link #_client}
     * will be closed and set to null. The {@link #_dispatcher} and {@link #_responseReader} will be stopped, the
     * {@link #_listeners} will be nulled and the {@link #_responses} will be cleared.
     *
     * @throws java.io.IOException if an exception occurs during the disconnect attempt
     */
    public void disconnect() throws IOException {
        if (isConnected()) {
            _logger.debug("Disconnecting from {}:{}", _host, _port);

            _dispatcher.stopRunning();
            _responseReader.stopRunning();

            if (_writer != null) {
                _writer.close();
                _writer = null;
            }

            if (_reader != null) {
                _reader.close();
                _reader = null;
            }

            if (_client != null) {
                _client.close();
                _client = null;
            }

            _responses.clear();
        }
    }

    /**
     * Returns true if we are connected ({@link #_client} is not null and is connected)
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return _client != null && _client.isConnected();
    }

    /**
     * Sends the specified command to the underlying socket
     *
     * @param command a non-null, non-empty command
     * @throws java.io.IOException an exception that occurred while sending
     */
    public synchronized void sendCommand(String command) throws IOException {
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        if (command.trim().length() == 0) {
            throw new IllegalArgumentException("Command cannot be empty");
        }

        if (!isConnected()) {
            throw new IOException("Cannot send message - disconnected");
        }

        _logger.debug("Sending Command: '{}'", command);
        _writer.println(command + "\n"); // as per spec - each command must have a newline
        _writer.flush();

    }

    /**
     * This is the runnable that will read from the socket and add messages to the responses queue (to be processed by
     * the dispatcher)
     *
     * @author Tim Roberts
     *
     */
    private class ResponseReader implements Runnable {

        /**
         * Whether the reader is currently running
         */
        private final AtomicBoolean _isRunning = new AtomicBoolean(false);

        /**
         * Locking to allow proper shutdown of the reader
         */
        private final Lock _lock = new ReentrantLock();
        private final Condition _running = _lock.newCondition();

        /**
         * Stops the reader. Will wait 5 seconds for the runnable to stop (should stop within 1 second based on the
         * setSOTimeout)
         */
        public void stopRunning() {
            _lock.lock();
            try {
                if (_isRunning.getAndSet(false)) {
                    if (!_running.await(5, TimeUnit.SECONDS)) {
                        _logger.warn("Waited too long for dispatcher to finish");
                    }
                }
            } catch (InterruptedException e) {
                // shouldn't happen
            } finally {
                _lock.unlock();
            }
        }

        /**
         * Runs the logic to read from the socket until {@link #_isRunning} is false. A 'response' is anything that ends
         * with a newline ending.
         */
        @Override
        public void run() {
            final StringBuilder sb = new StringBuilder(100);
            int c;

            _isRunning.set(true);
            _responses.clear();

            while (_isRunning.get()) {
                try {
                    // if reader is null, sleep and try again
                    if (_reader == null) {
                        Thread.sleep(250);
                        continue;
                    }

                    c = _reader.read();
                    if (c == -1) {
                        _responses.put(new IOException("server closed connection"));
                        _isRunning.set(false);
                        break;
                    }
                    final char ch = (char) c;
                    sb.append(ch);
                    if (ch == '\n') {
                        final String str = sb.toString();
                        if (str.endsWith("\n")) {
                            sb.setLength(0);
                            final String response = str.substring(0, str.length() - 1);
                            _logger.debug("Received response: {}", response);
                            _responses.put(response);
                        }
                    }
                    // _logger.debug(">>> reading: " + sb + ":" + (int) ch);
                } catch (SocketTimeoutException e) {
                    // do nothing - we expect this (setSOTimeout) to check the _isReading
                } catch (InterruptedException e) {
                    // Do nothing - probably shutting down
                } catch (IOException e) {
                    try {
                        _isRunning.set(false);
                        _responses.put(e);
                    } catch (InterruptedException e1) {
                        // Do nothing - probably shutting down
                    }
                }
            }

            _lock.lock();
            try {
                _running.signalAll();
            } finally {
                _lock.unlock();
            }
        }
    }

    /**
     * The dispatcher runnable is responsible for reading the response queue and dispatching it to the current callable.
     * Since the dispatcher is ONLY started when a callable is set, responses may pile up in the queue and be dispatched
     * when a callable is set. Unlike the socket reader, this can be assigned to another thread (no state outside of the
     * class).
     *
     * @author Tim Roberts
     */
    private class Dispatcher implements Runnable {

        /**
         * Whether the dispatcher is running or not
         */
        private final AtomicBoolean _isRunning = new AtomicBoolean(false);

        /**
         * Locking to allow proper shutdown of the reader
         */
        private final Lock _lock = new ReentrantLock();
        private final Condition _running = _lock.newCondition();

        /**
         * Stops the reader. Will wait 5 seconds for the runnable to stop (should stop within 1 second based on the poll
         * timeout below)
         */
        public void stopRunning() {

            _lock.lock();
            try {
                if (_isRunning.getAndSet(false)) {
                    if (!_running.await(5, TimeUnit.SECONDS)) {
                        _logger.warn("Waited too long for dispatcher to finish");
                    }
                }
            } catch (InterruptedException e) {
                // do nothing
            } finally {
                _lock.unlock();
            }

        }

        /**
         * Runs the logic to dispatch any responses to the current listeners until {@link #_isRunning} is false.
         */
        @Override
        public void run() {
            _isRunning.set(true);
            while (_isRunning.get()) {
                try {
                    final SocketSessionListener[] listeners = _listeners.toArray(new SocketSessionListener[0]);

                    // if no listeners, we don't want to start dispatching yet.
                    if (listeners.length == 0) {
                        Thread.sleep(250);
                        continue;
                    }

                    final Object response = _responses.poll(1, TimeUnit.SECONDS);

                    if (response != null) {
                        if (response instanceof String) {
                            try {
                                _logger.debug("Dispatching response: {}", response);
                                for (SocketSessionListener listener : listeners) {
                                    listener.responseReceived((String) response);
                                }
                            } catch (Exception e) {
                                _logger.warn("Exception occurred processing the response '{}': {}", response, e);
                            }
                        } else if (response instanceof Exception) {
                            _logger.debug("Dispatching exception: {}", response);
                            for (SocketSessionListener listener : listeners) {
                                listener.responseException((Exception) response);
                            }
                        } else {
                            _logger.error("Unknown response class: " + response);
                        }
                    }
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }

            _lock.lock();
            try {
                // Signal that we are done
                _running.signalAll();
            } finally {
                _lock.unlock();
            }
        }
    }
}
