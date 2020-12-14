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
package org.openhab.binding.webthing.internal.client;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.openhab.binding.webthing.internal.client.dto.PropertyStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The WebsocketConnection implementation
 *
 * @author Gregor Roth - Initial contribution
 */
class WebSocketConnectionImpl implements WebSocketConnection, WebSocket.Listener {
    private static final PropertyChangedListener EMPTY_PROPERTY_CHANGED_LISTENER = new PropertyChangedListener() {
    };
    private final Logger logger = LoggerFactory.getLogger(WebSocketConnectionImpl.class);
    private final Consumer<String> errorHandler;
    private final Map<String, PropertyChangedListener> propertyChangedListeners = new HashMap<>();
    private final AtomicReference<Instant> lastTimeReceived = new AtomicReference<>(Instant.now());
    private final AtomicReference<String> receivedTextbufferRef = new AtomicReference<>("");
    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>(null);

    /**
     * constructor
     *
     * @param errorHandler the errorHandler
     * @param pingPeriod the period pings should be sent
     */
    WebSocketConnectionImpl(Consumer<String> errorHandler, Duration pingPeriod) {
        this.errorHandler = errorHandler;
        new ConnectionWatchDog(pingPeriod).start();
    }

    @Override
    public void close() {
        var webSocket = webSocketRef.getAndSet(null);
        if (webSocket != null) {
            webSocket.abort();
        }
    }

    @Override
    public void observeProperty(@NotNull String propertyName, @NotNull PropertyChangedListener listener) {
        propertyChangedListeners.put(propertyName, listener);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocketRef.set(webSocket); // save websocket ref to be able to send ping
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        lastTimeReceived.set(Instant.now());
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence event, boolean last) {
        try {
            lastTimeReceived.set(Instant.now());

            var message = receivedTextbufferRef.accumulateAndGet(event.toString(),
                    (received, current) -> received + current);
            if (last) {
                receivedTextbufferRef.set("");
                var propertyStatus = new Gson().fromJson(message, PropertyStatusMessage.class);
                if (propertyStatus.messageType.equals("propertyStatus")) {
                    for (var propertyEntry : propertyStatus.data.entrySet()) {
                        propertyChangedListeners.getOrDefault(propertyEntry.getKey(), EMPTY_PROPERTY_CHANGED_LISTENER)
                                .onPropertyValueChanged(propertyEntry.getKey(), propertyEntry.getValue());
                    }
                } else {
                    logger.debug("Ignoring received message of unknown type: {}", event.toString());
                }
            }
        } catch (JsonSyntaxException se) {
            logger.warn("received invalid message: {}", event.toString());
        }

        return WebSocket.Listener.super.onText(webSocket, event, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        onError(webSocket, new IOException("websocket closed by peer. " + reason));
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        onError(error.getMessage());
    }

    private void onError(String message) {
        try {
            errorHandler.accept(message);
        } catch (Exception e) {
            logger.warn("error occurred by performing on disconnect", e);
        }
    }

    private final class ConnectionWatchDog extends Thread {
        private final Duration pingPeriod;

        ConnectionWatchDog(Duration pingPeriod) {
            this.pingPeriod = pingPeriod;
            setDaemon(true);
        }

        @Override
        public void run() {
            // initial pause
            pause(pingPeriod.dividedBy(2));

            while (webSocketRef.get() != null) {
                // check if connection is alive (message has been received recently)
                var elapsedSinceLast = Duration.between(lastTimeReceived.get(), Instant.now());
                var remainingTime = elapsedSinceLast.minus(pingPeriod);
                if (remainingTime.toMillis() > 0) {
                    onError("connection seems to be broken (last message received at " + lastTimeReceived.get() + ", "
                            + elapsedSinceLast.getSeconds() + " sec ago)");
                    return;
                }
                pause(pingPeriod.dividedBy(2));

                // send ping
                var webSocket = webSocketRef.get();
                if (webSocket != null) {
                    try {
                        webSocket.sendPing(ByteBuffer.wrap(Instant.now().toString().getBytes()));
                    } catch (Exception e) {
                        onError("could not send ping " + e.getMessage());
                        return;
                    }
                }
                pause(pingPeriod.dividedBy(2));
            }
        }

        private void pause(Duration delay) {
            if (webSocketRef.get() != null) {
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ignore) {
                }
            }
        }
    }
}
