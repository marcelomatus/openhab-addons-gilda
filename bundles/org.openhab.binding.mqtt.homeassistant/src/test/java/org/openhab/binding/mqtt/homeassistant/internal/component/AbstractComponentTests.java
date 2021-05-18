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
package org.openhab.binding.mqtt.homeassistant.internal.component;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.openhab.binding.mqtt.generic.MqttChannelTypeProvider;
import org.openhab.binding.mqtt.generic.TransformationServiceProvider;
import org.openhab.binding.mqtt.generic.values.Value;
import org.openhab.binding.mqtt.homeassistant.internal.AbstractHomeAssistantTests;
import org.openhab.binding.mqtt.homeassistant.internal.HaID;
import org.openhab.binding.mqtt.homeassistant.internal.HandlerConfiguration;
import org.openhab.binding.mqtt.homeassistant.internal.config.AbstractChannelConfiguration;
import org.openhab.binding.mqtt.homeassistant.internal.handler.HomeAssistantThingHandler;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.State;

/**
 * Abstract class for components tests.
 * TODO: need a way to test all channel properties, not only topics.
 *
 * @author Anton Kharuzhy - Initial contribution
 */
@SuppressWarnings({ "ConstantConditions" })
public abstract class AbstractComponentTests extends AbstractHomeAssistantTests {
    private final static int SUBSCRIBE_TIMEOUT = 10000;
    private final static int ATTRIBUTE_RECEIVE_TIMEOUT = 2000;

    private @Mock ThingHandlerCallback callback;
    private LatchThingHandler thingHandler;

    @BeforeEach
    public void setupThingHandler() {
        final var config = haThing.getConfiguration();

        config.put(HandlerConfiguration.PROPERTY_BASETOPIC, HandlerConfiguration.DEFAULT_BASETOPIC);
        config.put(HandlerConfiguration.PROPERTY_TOPICS, getConfigTopics());

        when(callback.getBridge(eq(BRIDGE_UID))).thenReturn(bridgeThing);

        thingHandler = new LatchThingHandler(haThing, channelTypeProvider, transformationServiceProvider,
                SUBSCRIBE_TIMEOUT, ATTRIBUTE_RECEIVE_TIMEOUT);
        thingHandler.setConnection(bridgeConnection);
        thingHandler.setCallback(callback);
        thingHandler = spy(thingHandler);

        thingHandler.initialize();
    }

    @AfterEach
    public void disposeThingHandler() {
        thingHandler.dispose();
    }

    /**
     * {@link org.openhab.binding.mqtt.homeassistant.internal.DiscoverComponents} will wait a config on specified
     * topics.
     * Topics in config must be without prefix and suffix, they can be converted to full with method
     * {@link #configTopicToMqtt(String)}
     *
     * @return config topics
     */
    protected abstract Set<String> getConfigTopics();

    /**
     * Process payload to discover and configure component. Topic should be added to {@link #getConfigTopics()}
     *
     * @param mqttTopic mqtt topic with configuration
     * @param json configuration payload in Json
     * @return discovered component
     */
    protected AbstractComponent<@NonNull ? extends AbstractChannelConfiguration> discoverComponent(String mqttTopic,
            String json) {
        return discoverComponent(mqttTopic, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Process payload to discover and configure component. Topic should be added to {@link #getConfigTopics()}
     *
     * @param mqttTopic mqtt topic with configuration
     * @param jsonPayload configuration payload in Json
     * @return discovered component
     */
    protected AbstractComponent<@NonNull ? extends AbstractChannelConfiguration> discoverComponent(String mqttTopic,
            byte[] jsonPayload) {
        var latch = thingHandler.createWaitForComponentDiscoveredLatch(1);
        assertThat(publishMessage(mqttTopic, jsonPayload), is(true));
        try {
            assert latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            assertThat(e.getMessage(), false);
        }
        var component = thingHandler.getDiscoveredComponent();
        assertThat(component, CoreMatchers.notNullValue());
        return component;
    }

    /**
     * Assert channel topics, label and value class
     *
     * @param component component
     * @param channelId channel
     * @param stateTopic state topic or empty string
     * @param commandTopic command topic or empty string
     * @param label label
     * @param valueClass value class
     */
    @SuppressWarnings("ConstantConditions")
    protected static void assertChannel(AbstractComponent<@NonNull ? extends AbstractChannelConfiguration> component,
            String channelId, String stateTopic, String commandTopic, String label, Class<? extends Value> valueClass) {
        var stateChannel = component.getChannel(channelId);
        assertThat(stateChannel.getChannel().getLabel(), is(label));
        assertThat(stateChannel.getState().getStateTopic(), is(stateTopic));
        assertThat(stateChannel.getState().getCommandTopic(), is(commandTopic));
        assertThat(stateChannel.getState().getCache(), is(instanceOf(valueClass)));
    }

    /**
     * Assert channel state
     *
     * @param component component
     * @param channelId channel
     * @param state expected state
     */
    protected static void assertState(AbstractComponent<@NonNull ? extends AbstractChannelConfiguration> component,
            String channelId, State state) {
        assertThat(component.getChannel(channelId).getState().getCache().getChannelState(), is(state));
    }

    /**
     * Assert that given payload was published on given topic.
     *
     * @param mqttTopic Mqtt topic
     * @param payload payload
     */
    protected void assertPublished(String mqttTopic, String payload) {
        verify(bridgeConnection).publish(eq(mqttTopic), eq(payload.getBytes(StandardCharsets.UTF_8)), anyInt(),
                anyBoolean());
    }

    /**
     * Assert that given payload was not published on given topic.
     *
     * @param mqttTopic Mqtt topic
     * @param payload payload
     */
    protected void assertNotPublished(String mqttTopic, String payload) {
        verify(bridgeConnection, never()).publish(eq(mqttTopic), eq(payload.getBytes(StandardCharsets.UTF_8)), anyInt(),
                anyBoolean());
    }

    /**
     * Publish payload to all subscribers on specified topic.
     *
     * @param mqttTopic Mqtt topic
     * @param payload payload
     * @return true when at least one subscriber found
     */
    protected boolean publishMessage(String mqttTopic, String payload) {
        return publishMessage(mqttTopic, payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Publish payload to all subscribers on specified topic.
     *
     * @param mqttTopic Mqtt topic
     * @param payload payload
     * @return true when at least one subscriber found
     */
    protected boolean publishMessage(String mqttTopic, byte[] payload) {
        final var topicSubscribers = subscriptions.get(mqttTopic);

        if (topicSubscribers != null && !topicSubscribers.isEmpty()) {
            topicSubscribers.forEach(mqttMessageSubscriber -> mqttMessageSubscriber.processMessage(mqttTopic, payload));
            return true;
        }
        return false;
    }

    @NonNullByDefault
    protected static class LatchThingHandler extends HomeAssistantThingHandler {
        private @Nullable CountDownLatch latch;
        private @Nullable AbstractComponent<@NonNull ? extends AbstractChannelConfiguration> discoveredComponent;

        public LatchThingHandler(Thing thing, MqttChannelTypeProvider channelTypeProvider,
                TransformationServiceProvider transformationServiceProvider, int subscribeTimeout,
                int attributeReceiveTimeout) {
            super(thing, channelTypeProvider, transformationServiceProvider, subscribeTimeout, attributeReceiveTimeout);
        }

        public void componentDiscovered(HaID homeAssistantTopicID, AbstractComponent<@NonNull ?> component) {
            accept(List.of(component));
            discoveredComponent = component;
            if (latch != null) {
                latch.countDown();
            }
        }

        public CountDownLatch createWaitForComponentDiscoveredLatch(int count) {
            final var newLatch = new CountDownLatch(count);
            latch = newLatch;
            return newLatch;
        }

        public @Nullable AbstractComponent<@NonNull ? extends AbstractChannelConfiguration> getDiscoveredComponent() {
            return discoveredComponent;
        }
    }
}
