/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.mqtt.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.eclipse.smarthome.io.transport.mqtt.MqttConnectionObserver;
import org.eclipse.smarthome.io.transport.mqtt.MqttConnectionState;
import org.eclipse.smarthome.io.transport.mqtt.MqttService;
import org.openhab.binding.mqtt.action.MQTTActions;
import org.openhab.binding.mqtt.discovery.MQTTTopicDiscoveryParticipant;
import org.openhab.binding.mqtt.discovery.TopicSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * This base implementation handles connection changes of the {@link MqttBrokerConnection}
 * and puts the Thing on or offline. It also handles adding/removing notifications of the
 * {@link MqttService} and provides a basic dispose() implementation.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractBrokerHandler extends BaseBridgeHandler implements MqttConnectionObserver {
    public static final int TIMEOUT_DEFAULT = 1200; /* timeout in milliseconds */
    private final Logger logger = LoggerFactory.getLogger(AbstractBrokerHandler.class);

    final Map<ChannelUID, PublishTriggerChannel> channelStateByChannelUID = new HashMap<>();
    private final Map<String, @Nullable Map<MQTTTopicDiscoveryParticipant, @Nullable TopicSubscribe>> discoveryTopics = new HashMap<>();

    protected @Nullable MqttBrokerConnection connection;
    protected CompletableFuture<MqttBrokerConnection> connectionFuture = new CompletableFuture<>();

    public AbstractBrokerHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(MQTTActions.class);
    }

    /**
     * Returns the underlying {@link MqttBrokerConnection} either immediately or after {@link #initialize()} has
     * performed.
     */
    public CompletableFuture<MqttBrokerConnection> getConnectionAsync() {
        return connectionFuture;
    }

    /**
     * Returns the underlying {@link MqttBrokerConnection}.
     */
    public @Nullable MqttBrokerConnection getConnection() {
        return connection;
    }

    /**
     * Does nothing in the base implementation.
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // No commands to handle
    }

    /**
     * Registers a connection status listener and attempts a connection if there is none so far.
     */
    @Override
    public void initialize() {
        final MqttBrokerConnection connection = this.connection;
        if (connection == null) {
            logger.warn("Trying to initialize {} but connection is null. This is most likely a bug.", thing.getUID());
            return;
        }
        for (Channel channel : thing.getChannels()) {
            final PublishTriggerChannelConfig channelConfig = channel.getConfiguration()
                    .as(PublishTriggerChannelConfig.class);
            PublishTriggerChannel c = new PublishTriggerChannel(channelConfig, channel.getUID(), connection, this);
            channelStateByChannelUID.put(channel.getUID(), c);
        }

        connection.addConnectionObserver(this);

        connection.start().exceptionally(e -> {
            connectionStateChanged(MqttConnectionState.DISCONNECTED, e);
            return false;
        }).thenAccept(v -> {
            if (!v) {
                connectionStateChanged(MqttConnectionState.DISCONNECTED, new TimeoutException("Timeout"));
            } else {
                connectionStateChanged(MqttConnectionState.CONNECTED, null);
            }
        });
        connectionFuture.complete(connection);

        discoveryTopics.forEach((topic, listenerMap) -> {
            listenerMap.replaceAll((listener, oldTopicSubscribe) -> {
                TopicSubscribe topicSubscribe = new TopicSubscribe(connection, topic, listener, thing.getUID());
                topicSubscribe.start().handle((result, ex) -> {
                    if (ex != null) {
                        logger.warn("Failed to subscribe {} to discovery topic {} on broker {}", listener, topic,
                                thing.getUID());
                    } else {
                        logger.trace("Subscribed {} to discovery topic {} on broker {}", listener, topic,
                                thing.getUID());
                    }
                    return null;
                });
                return topicSubscribe;
            });
        });
    }

    @Override
    public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error) {
        if (state == MqttConnectionState.CONNECTED) {
            updateStatus(ThingStatus.ONLINE);
            channelStateByChannelUID.values().forEach(c -> c.start());
        } else {
            channelStateByChannelUID.values().forEach(c -> c.stop());
            if (error == null) {
                updateStatus(ThingStatus.OFFLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error.getMessage());
            }
        }
    }

    @Override
    protected void triggerChannel(ChannelUID channelUID, String event) {
        super.triggerChannel(channelUID, event);
    }

    /**
     * Removes listeners to the {@link MqttBrokerConnection}.
     */
    @Override
    public void dispose() {
        channelStateByChannelUID.values().forEach(c -> c.stop());
        channelStateByChannelUID.clear();

        // keep topics, but stop subscriptions
        discoveryTopics.forEach((topic, listenerMap) -> {
            listenerMap.forEach((listener, topicSubscribe) -> {
                topicSubscribe.stop();
            });
        });

        if (connection != null) {
            connection.removeConnectionObserver(this);
        } else {
            logger.warn("Trying to dispose handler {} but connection is already null. Most likely this is a bug.",
                    thing.getUID());
        }
        this.connection = null;
        connectionFuture = new CompletableFuture<>();
        super.dispose();
    }

    /**
     * register a discovery listener to a specified topic on this broker (used by the handler factory)
     *
     * @param listener the discovery participant that wishes to be notified about this topic
     * @param topic the topic (wildcards supported)
     */
    public final void registerDiscoveryListener(MQTTTopicDiscoveryParticipant listener, String topic) {
        Map<MQTTTopicDiscoveryParticipant, @Nullable TopicSubscribe> topicListeners = discoveryTopics
                .computeIfAbsent(topic, t -> new HashMap<>());
        topicListeners.compute(listener, (k, v) -> {
            if (v != null) {
                logger.warn("Duplicate subscription for {} to discovery topic {} on broker {}. Check discovery logic!",
                        listener, topic, thing.getUID());
                v.stop();
            }

            TopicSubscribe topicSubscribe = new TopicSubscribe(connection, topic, listener, thing.getUID());
            topicSubscribe.start().handle((result, ex) -> {
                if (ex != null) {
                    logger.warn("Failed to subscribe {} to discovery topic {} on broker {}", listener, topic,
                            thing.getUID());
                } else {
                    logger.trace("Subscribed {} to discovery topic {} on broker {}", listener, topic, thing.getUID());
                }
                return null;
            });
            return topicSubscribe;
        });
    }

    /**
     * unregisters a discovery listener from a specified topic on this broker (used by the handler factory)
     *
     * @param listener the discovery participant that wishes no notifications about this topic
     * @param topic the topic (as specified during registration)
     */
    public final void unregisterDiscoveryListener(MQTTTopicDiscoveryParticipant listener, String topic) {
        Map<MQTTTopicDiscoveryParticipant, @Nullable TopicSubscribe> topicListeners = discoveryTopics
                .compute(topic, (k, v) -> {
                    if (v == null) {
                        logger.warn(
                                "Tried to unsubscribe {} from  discovery topic {} on broker {} but topic not registered at all. Check discovery logic!",
                                listener, topic, thing.getUID());
                        return null;
                    }
                    v.compute(listener, (l, w) -> {
                        if (w == null) {
                            logger.warn(
                                    "Tried to unsubscribe {} from  discovery topic {} on broker {} but topic not registered for listener. Check discovery logic!",
                                    listener, topic, thing.getUID());
                        } else {
                            w.stop();
                            logger.trace("Unsubscribed {} from discovery topic {} on broker {}", listener, topic,
                                    thing.getUID());
                        }
                        return null;
                    });
                    return v.isEmpty() ? null : v;
                });
    }
}
