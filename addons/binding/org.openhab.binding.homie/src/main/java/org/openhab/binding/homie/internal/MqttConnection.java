package org.openhab.binding.homie.internal;

import static org.openhab.binding.homie.HomieBindingConstants.*;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.homie.handler.HomieDeviceHandler;
import org.openhab.binding.homie.internal.conventionv200.HomieConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttConnection {
    private static final int SUBSCRIBE_QOS = 2;
    private static Logger logger = LoggerFactory.getLogger(MqttConnection.class);

    private final String brokerURL;
    private final String basetopic;
    private MqttAsyncClient client;
    private final String listenDeviceTopic;

    private final String qualifier;

    public String getBasetopic() {
        return basetopic;
    }

    public void disconnect() {
        try {
            client.disconnectForcibly();
        } catch (MqttException e) {
            logger.error("Error on disconnect", e);
        }
    }

    public MqttConnection(String brokerurl, String basetopic, String clientIdentifier) {
        this.brokerURL = brokerurl;
        this.basetopic = basetopic;
        listenDeviceTopic = String.format("%s/#", basetopic);
        qualifier = clientIdentifier;

        connect();
    }

    public static MqttConnection fromConfiguration(HomieConfiguration config, Object consumer) {
        return new MqttConnection(config.getBrokerUrl(), config.getBaseTopic(), MQTT_CLIENTID + "-" + consumer);
    }

    private void connect() {
        try {
            logger.debug("Homie MQTT Connection start");
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            client = new MqttAsyncClient(brokerURL, MQTT_CLIENTID + "-" + qualifier, new MemoryPersistence());
            client.connect(opts);
            logger.debug("Homie MQTT Connection connected");
        } catch (MqttException e) {
            logger.error("MQTT Connect failed", e);
        }
    }

    public void listenForNodeProperties(Bridge device, Thing node, IMqttMessageListener messageListener)
            throws MqttException {
        String topic = String.format("%s/%s/%s/", basetopic, device.getUID().getId(), node.getUID().getId());

        client.subscribe(topic + HomieConventions.HOMIE_NODE_TYPE_ANNOUNCEMENT_TOPIC_SUFFIX, SUBSCRIBE_QOS,
                messageListener);
        client.subscribe(topic + HomieConventions.HOMIE_NODE_PROPERTYLIST_ANNOUNCEMENT_TOPIC_SUFFIX, SUBSCRIBE_QOS,
                messageListener);

    }

    public void subscribe(Thing thing, IMqttMessageListener messageListener) throws MqttException {
        String topic = String.format("%s/%s/#", basetopic, thing.getUID().getId());
        client.subscribe(topic, SUBSCRIBE_QOS, messageListener);
    }

    public void listenForDeviceIds(IMqttMessageListener messageListener) {
        logger.debug("Listening for devices on topic " + listenDeviceTopic);
        try {
            client.unsubscribe(listenDeviceTopic);
            client.subscribe(listenDeviceTopic, SUBSCRIBE_QOS, messageListener);
        } catch (MqttException e) {
            logger.error("Failed to subscribe to topic " + listenDeviceTopic, e);
        }
    }

    public void listenForNodeIds(Bridge bridge, IMqttMessageListener messageListener) throws MqttException {
        subscribe(bridge, messageListener);
    }

    public void unsubscribeListenForDeviceIds() {
        try {
            client.unsubscribe(listenDeviceTopic);
        } catch (MqttException e) {
            logger.error("Failed to unsubscribe from topic " + listenDeviceTopic, e);
        }

    }

    public void unsubscribeListenForNodeIds() {
        try {
            client.unsubscribe(listenDeviceTopic);
        } catch (MqttException e) {
            logger.error("Failed to unsubscribe from topic " + listenDeviceTopic, e);
        }

    }

    /**
     * Subscribe to a channel of a device
     *
     * @param channelUID
     * @param handler
     */
    public void subscribeChannel(Channel channel, HomieDeviceHandler handler) {

        String topic = String.format("%s/%s/%s", basetopic, handler.getThing().getUID().getId(),
                channel.getProperties().get(CHANNELPROPERTY_TOPICSUFFIX));
        logger.debug(
                "(Re-)Subscribing to topic '" + topic + "' to listen for events of channel '" + channel.getUID() + "'");
        try {
            client.unsubscribe(topic);
            client.subscribe(topic, SUBSCRIBE_QOS, handler);
            logger.debug("Subscribed to topic " + topic);
        } catch (MqttException e) {
            logger.error("Error (re)subscribing to channel. topic is " + topic, e);
        }

    }

    public void listenForNode(String deviceId, String nodeId, IMqttMessageListener listener) throws MqttException {
        String topic = String.format("%s/%s/%s/", basetopic, deviceId, nodeId);
        // Subscribe to type and proplist separately, as the notifications have to arrive first at the listener
        client.subscribe(topic + HomieConventions.HOMIE_NODE_TYPE_ANNOUNCEMENT_TOPIC_SUFFIX, SUBSCRIBE_QOS, listener);
        client.subscribe(topic + HomieConventions.HOMIE_NODE_PROPERTYLIST_ANNOUNCEMENT_TOPIC_SUFFIX, SUBSCRIBE_QOS,
                listener);
        client.subscribe(topic + "#", SUBSCRIBE_QOS, listener);
    }

}
