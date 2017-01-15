/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homie.handler;

import static org.openhab.binding.homie.HomieBindingConstants.*;
import static org.openhab.binding.homie.internal.conventionv200.HomieConventions.*;

import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.homie.internal.MqttConnection;
import org.openhab.binding.homie.internal.conventionv200.HomieTopic;
import org.openhab.binding.homie.internal.conventionv200.TopicParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomieDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Kolb - Initial contribution
 */
public class HomieDeviceHandler extends BaseBridgeHandler implements IMqttMessageListener {

    private Logger logger = LoggerFactory.getLogger(HomieDeviceHandler.class);
    private final MqttConnection mqttconnection;
    private final TopicParser topicParser;

    /**
     * Constructor
     *
     * @param thing The Bridge that will be handled
     */
    public HomieDeviceHandler(Bridge thing, MqttConnection connection) {
        super(thing);
        this.mqttconnection = connection;
        topicParser = new TopicParser(mqttconnection.getBasetopic());

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Channel channel = getThing().getChannel(channelUID.getId());
        if (channel != null) {
            if (command == RefreshType.REFRESH) {
                // reconnect to mqtt to receive the retained messages once more
                mqttconnection.subscribeChannel(channel, this);
            }
        }
    }

    @Override
    public void initialize() {
        try {
            mqttconnection.subscribe(thing, this);
            updateStatus(ThingStatus.ONLINE);
        } catch (MqttException e) {
            logger.error("Error subscribing for MQTT topics", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error subscribing MQTT" + e.toString());
        }
    }

    @Override
    public void dispose() {
        mqttconnection.disconnect();
        super.dispose();
    }

    long map(long x, long in_min, long in_max, long out_min, long out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String message = mqttMessage.toString();
        try {
            HomieTopic ht = topicParser.parse(topic);
            if (ht.isDeviceProperty()) {
                String prop = ht.getCombinedInternalPropertyName();

                if (StringUtils.equals(prop, STATS_UPTIME_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_STATS_UPTIME);
                    updateState(channel, new DecimalType(message));
                } else if (StringUtils.equals(prop, ONLINE_TOPIC_SUFFIX)) {
                    boolean isOnline = StringUtils.equalsIgnoreCase(message, "true");
                    updateStatus(isOnline ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_ONLINE);
                    updateState(channel, isOnline ? OnOffType.ON : OnOffType.OFF);
                } else if (StringUtils.equals(prop, NAME_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_NAME);
                    updateState(channel, new StringType(message));
                } else if (StringUtils.equals(prop, LOCALIP_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_LOCALIP);
                    updateState(channel, new StringType(message));
                } else if (StringUtils.equals(prop, MAC_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_MAC);
                    updateState(channel, new StringType(message));
                } else if (StringUtils.equals(prop, STATS_SIGNAL_TOPIC_SUFFIX)) {
                    // Homie Channel
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_STATS_SIGNAL);
                    updateState(channel, new DecimalType(message));

                    // Eclipse smart home system channel
                    ChannelUID channelesh = new ChannelUID(getThing().getUID(), CHANNEL_STATS_SIGNAL_ESH);
                    int val = Integer.parseInt(message);
                    val = (int) map(val, 0, 100, 0, 4); // Scale percent (0-9) to ESH scale (0-4)
                    updateState(channelesh, new DecimalType(val));
                } else if (StringUtils.equals(prop, STATS_INTERVAL_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_STATS_INTERVAL);
                    updateState(channel, new DecimalType(message));
                } else if (StringUtils.equals(prop, FIRMWARE_NAME_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_FIRMWARE_NAME);
                    updateState(channel, new StringType(message));
                } else if (StringUtils.equals(prop, FIRMWARE_VERSION_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_FIRMWARE_VERSION);
                    updateState(channel, new StringType(message));
                } else if (StringUtils.equals(prop, FIRMWARE_CHECKSUM_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_FIRMWARE_CHECKSUM);
                    updateState(channel, new StringType(message));
                } else if (StringUtils.equals(prop, IMPLEMENTATION_TOPIC_SUFFIX)) {
                    ChannelUID channel = new ChannelUID(getThing().getUID(), CHANNEL_IMPLEMENTATION);
                    updateState(channel, new StringType(message));
                }
            }

        } catch (ParseException e) {
            logger.error("Topic cannot be handled", e);
        }

    }

}
