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
package org.openhab.binding.jablotron.internal.handler;

import static org.openhab.binding.jablotron.JablotronBindingConstants.CACHE_TIMEOUT_MS;
import static org.openhab.binding.jablotron.JablotronBindingConstants.CHANNEL_LAST_CHECK_TIME;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.model.JablotronControlResponse;
import org.openhab.binding.jablotron.internal.model.JablotronServiceDetailSegment;
import org.openhab.binding.jablotron.internal.model.JablotronServiceDetailSegmentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JablotronJa100Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronJa100Handler extends JablotronAlarmHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronJa100Handler.class);

    public JablotronJa100Handler(Thing thing, String alarmName) {
        super(thing, alarmName);
        dataCache = new ExpiringCache<>(CACHE_TIMEOUT_MS, this::sendGetStatusRequest);
        eventCache = new ExpiringCache<>(CACHE_TIMEOUT_MS, this::sendGetEventHistory);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH.equals(command)) {
            logger.debug("refreshing channel: {}", channelUID.getId());
            updateChannel(channelUID.getId());
        } else {
            if (channelUID.getId().startsWith("STATE_") && command instanceof StringType) {
                scheduler.execute(() -> {
                    controlSTATESection(channelUID.getId(), command.toString());
                });
            }

            if (channelUID.getId().startsWith("PGM_") && command instanceof OnOffType) {
                scheduler.execute(() -> {
                    controlPGMSection(channelUID.getId(), command.equals(OnOffType.ON) ? "set" : "unset");
                });
            }
        }
    }

    private void updateChannel(String channel) {
        if (channel.startsWith("STATE_") || channel.startsWith("PGM_") || channel.startsWith("THERMOMETER_")
                || channel.startsWith("THERMOSTAT_")) {
            updateSegmentStatus(channel, dataCache.getValue());
        } else if (CHANNEL_LAST_CHECK_TIME.equals(channel)) {
            // not updating
        } else {
            updateEventChannel(channel);
        }
    }

    private void createChannel(JablotronServiceDetailSegment section) {
        if (section.getSegmentId().startsWith("PGM_")) {
            createPGMChannel(section.getSegmentId(), section.getSegmentName());
        } else {
            createStateChannel(section.getSegmentId(), section.getSegmentName());
        }
    }

    private void createTempChannel(String name, String label) {
        ChannelTypeUID temperature = new ChannelTypeUID("jablotron", "temperature");
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), "Number:Temperature")
                .withLabel(label).withType(temperature).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    private void createThermostatChannel(String name, String label) {
        ChannelTypeUID temperature = new ChannelTypeUID("jablotron", "thermostat");
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), "Number:Temperature")
                .withLabel(label).withType(temperature).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    private void createPGMChannel(String name, String label) {
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), "Switch").withLabel(label)
                .build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    private void createStateChannel(String name, String label) {
        ChannelTypeUID alarmStatus = new ChannelTypeUID("jablotron", "alarm_state");
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), "String").withLabel(label)
                .withType(alarmStatus).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    @Override
    protected void updateSegmentStatus(JablotronServiceDetailSegment segment) {
        logger.debug("Segment id: {} and status: {}", segment.getSegmentId(), segment.getSegmentState());
        String segmentId = segment.getSegmentId();

        if (segmentId.startsWith("STATE_") || segmentId.startsWith("PGM_")) {
            processSection(segment);
        } else if (segmentId.startsWith("THERMOMETER_")) {
            processThermometer(segment);
        } else if (segmentId.startsWith("THERMOSTAT_")) {
            processThermostat(segment);
        } else {
            logger.debug("Unknown segment received: {} with state: {}", segment.getSegmentId(),
                    segment.getSegmentState());
        }
    }

    private void processSection(JablotronServiceDetailSegment segment) {
        String segmentId = segment.getSegmentId();
        Channel channel = getThing().getChannel(segmentId);
        if (channel == null) {
            logger.debug("Creating a new channel: {}", segmentId);
            createChannel(segment);
        }
        channel = getThing().getChannel(segmentId);
        if (channel != null) {
            logger.debug("Updating channel: {} to value: {}", channel.getUID(), segment.getSegmentState());
            State newState;
            if (segmentId.startsWith("PGM_")) {
                newState = "unset".equals(segment.getSegmentState()) ? OnOffType.OFF : OnOffType.ON;
            } else {
                newState = new StringType(segment.getSegmentState());
            }
            updateState(channel.getUID(), newState);
        } else {
            logger.debug("The channel: {} still doesn't exist!", segmentId);
        }
    }

    private void processThermometer(JablotronServiceDetailSegment segment) {
        String segmentId = segment.getSegmentId();
        Channel channel = getThing().getChannel(segmentId);
        if (channel == null) {
            logger.debug("Creating a new temperature channel: {}", segmentId);
            createTempChannel(segmentId, segment.getSegmentName());
        }
        updateTemperatureChannel(channel, segment);
    }

    private void processThermostat(JablotronServiceDetailSegment segment) {
        String segmentId = segment.getSegmentId();
        Channel channel = getThing().getChannel(segmentId);
        if (channel == null) {
            logger.debug("Creating a new thermostat channel: {}", segmentId);
            createThermostatChannel(segmentId, segment.getSegmentName());
        }
        updateTemperatureChannel(channel, segment);
    }

    private void updateTemperatureChannel(Channel channel, JablotronServiceDetailSegment segment) {
        String segmentId = segment.getSegmentId();
        if (channel != null) {
            List<JablotronServiceDetailSegmentInfo> infos = segment.getSegmentInfos();
            if (infos.size() > 0) {
                logger.debug("Found value: {} and type: {}", infos.get(0).getValue(), infos.get(0).getType());
                DecimalType newState = new DecimalType(infos.get(0).getValue());
                updateState(channel.getUID(), newState);
            } else {
                logger.debug("No segment information received");
            }
        } else {
            logger.debug("The channel: {} still doesn't exist!", segmentId);
        }
    }

    public synchronized void controlPGMSection(String section, String status) {
        logger.debug("Controlling section: {} with status: {}", section, status);
        JablotronControlResponse response = sendUserCode(section, section.toLowerCase(), status, thingConfig.getCode());

        updateAlarmStatus();
        if (response == null) {
            logger.debug("null response/status received during the control of PGM section: {}", section);
        }
    }

    public synchronized void controlSTATESection(String section, String status) {
        logger.debug("Controlling section: {} with status: {}", section, status);
        JablotronControlResponse response = sendUserCode(section, section.toLowerCase().replace("state", "section"),
                status, thingConfig.getCode());

        updateAlarmStatus();
        if (response == null) {
            logger.debug("null response/status received during the control of STATE section: {}", section);
        }
    }
}
