/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.regoheatpump.handler;

import static org.openhab.binding.regoheatpump.RegoHeatPumpBindingConstants.*;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.regoheatpump.internal.protocol.CommandFactory;
import org.openhab.binding.regoheatpump.internal.protocol.RegoConnection;
import org.openhab.binding.regoheatpump.internal.protocol.RegoRegisterMapper;
import org.openhab.binding.regoheatpump.internal.protocol.ResponseParser;
import org.openhab.binding.regoheatpump.internal.protocol.ResponseParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RegoHeatPumpHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Boris Krivonog - Initial contribution
 */
public abstract class RegoHeatPumpHandler extends BaseThingHandler {

    private static final class ChannelDescriptor {
        private Date lastUpdate;
        private State state;

        public State stateIfNotExpired(int refreshTime) {
            if (lastUpdate == null || (lastUpdate.getTime() + refreshTime * 900 < new Date().getTime())) {
                return null;
            }

            return state;
        }

        public void setState(State state) {
            lastUpdate = new Date();
            this.state = state;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(RegoHeatPumpHandler.class);
    private final Map<String, ChannelDescriptor> channelDescriptors = new HashMap<>();
    private int refreshInterval;
    private RegoConnection connection;
    private RegoRegisterMapper mapper;
    private ScheduledFuture<?> scheduledRefreshFuture;

    protected RegoHeatPumpHandler(Thing thing) {
        super(thing);
    }

    protected abstract RegoConnection createConnection();

    @Override
    public void initialize() {
        mapper = RegoRegisterMapper.rego600();
        connection = createConnection();
        refreshInterval = ((Number) getConfig().get(REFRESH_INTERVAL)).intValue();

        scheduledRefreshFuture = scheduler.scheduleWithFixedDelay(this::refresh, 1, refreshInterval, TimeUnit.SECONDS);

        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void dispose() {
        super.dispose();

        connection.close();

        scheduledRefreshFuture.cancel(true);
        scheduledRefreshFuture = null;

        synchronized (channelDescriptors) {
            channelDescriptors.clear();
        }

        connection = null;
        mapper = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduledRefreshFuture.cancel(false);
            scheduledRefreshFuture = scheduler.scheduleWithFixedDelay(this::refresh, 1, refreshInterval,
                    TimeUnit.SECONDS);
        } else {
            logger.debug("Unsupported command {}! Supported commands: REFRESH", command);
        }
    }

    private ChannelDescriptor channelDescriptorForChannel(final String channelIID) {
        synchronized (channelDescriptors) {
            ChannelDescriptor descriptor = channelDescriptors.get(channelIID);
            if (descriptor == null) {
                descriptor = new ChannelDescriptor();
                channelDescriptors.put(channelIID, descriptor);
            }
            return descriptor;
        }
    }

    private void processChannelRequest(final String channelIID) {
        switch (channelIID) {
            case CHANNEL_LAST_ERROR_CODE:
            case CHANNEL_LAST_ERROR_TIMESTAMP:
                readLastError();
                break;

            case CHANNEL_FRONT_PANEL_POWER_LED:
                readFromFrontPanel(channelIID, (short) 0x0012);
                break;

            case CHANNEL_FRONT_PANEL_PUMP_LED:
                readFromFrontPanel(channelIID, (short) 0x0013);
                break;

            case CHANNEL_FRONT_PANEL_ADDITIONAL_HEATING_LED:
                readFromFrontPanel(channelIID, (short) 0x0014);
                break;

            case CHANNEL_FRONT_PANEL_WATER_HEATER_LED:
                readFromFrontPanel(channelIID, (short) 0x0015);
                break;

            case CHANNEL_FRONT_PANEL_ALARM_LED:
                readFromFrontPanel(channelIID, (short) 0x0016);
                break;

            default:
                if (readFromSystemRegister(channelIID) == false) {
                    logger.debug("Unable to handle unknown channel {}", channelIID);
                }
                break;
        }
    }

    private Collection<String> linkedChannels() {
        return thing.getChannels().stream().map(Channel::getUID).map(ChannelUID::getId).filter(this::isLinked)
                .collect(Collectors.toList());
    }

    private void refresh() {
        for (final String channelIID : linkedChannels()) {
            if (Thread.interrupted()) {
                break;
            }

            processChannelRequest(channelIID);

            if (thing.getStatus() != ThingStatus.ONLINE) {
                break;
            }
        }
    }

    private void readLastError() {
        executeCommandAndUpdateState(CHANNEL_LAST_ERROR_CODE, CommandFactory.createReadLastErrorCommand(),
                ResponseParserFactory.ErrorLine, e -> {
                    if (e == null) {
                        updateState(CHANNEL_LAST_ERROR_TIMESTAMP, UnDefType.NULL);
                        return UnDefType.NULL;
                    }

                    try {
                        updateState(CHANNEL_LAST_ERROR_TIMESTAMP, new DateTimeType(e.timestamp()));
                    } catch (RuntimeException ex) {
                        logger.warn("Unable to convert timestamp '{}' to DateTimeType due {}", e.timestampAsString(),
                                ex);
                        updateState(CHANNEL_LAST_ERROR_TIMESTAMP, UnDefType.UNDEF);
                    }

                    return new StringType(Byte.toString(e.error()));
                });
    }

    private void readFromFrontPanel(final String channelIID, short address) {
        final byte[] command = CommandFactory.createReadFromFrontPanelCommand(address);
        executeCommandAndUpdateState(channelIID, command, ResponseParserFactory.Short, v -> {
            return v == 0 ? OnOffType.OFF : OnOffType.ON;
        });
    }

    private boolean readFromSystemRegister(final String channelIID) {
        RegoRegisterMapper.Channel channel = mapper.map(channelIID);
        if (channel == null) {
            return false;
        }

        final byte[] command = CommandFactory.createReadFromSystemRegisterCommand(channel.address());
        executeCommandAndUpdateState(channelIID, command, ResponseParserFactory.Short, channel::convert);

        return true;
    }

    private synchronized <T> void executeCommandAndUpdateState(final String channelIID, final byte[] command,
            final ResponseParser<T> parser, Function<T, State> converter) {

        if (logger.isDebugEnabled()) {
            logger.debug("Reading value for channel '{}' ...", channelIID);
        }

        // CHANNEL_LAST_ERROR_CODE and CHANNEL_LAST_ERROR_TIMESTAMP are read from same
        // register. To prevent accessing same register twice when both channels are linked,
        // use same name for both so only a single fetch will be triggered.
        final String mappedChannelIID = (CHANNEL_LAST_ERROR_CODE.equals(channelIID)
                || CHANNEL_LAST_ERROR_TIMESTAMP.equals(channelIID)) ? CHANNEL_LAST_ERROR : channelIID;

        final ChannelDescriptor descriptor = channelDescriptorForChannel(mappedChannelIID);
        final State lastState = descriptor.stateIfNotExpired(refreshInterval);

        if (lastState != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cache did not yet expire, using cached value for {}", mappedChannelIID);
            }
            updateState(channelIID, lastState);
            return;
        }

        try {
            checkRegoDevice();

            T result = executeCommand(command, parser);

            if (logger.isDebugEnabled()) {
                logger.debug("Got value for '{}' = {}", channelIID, result);
            }

            final State newState = converter.apply(result);
            updateState(channelIID, newState);

            descriptor.setState(newState);

        } catch (IOException e) {

            logger.warn("Accessing value for channel '{}' failed due {}", channelIID, e);

            connection.close();

            synchronized (channelDescriptors) {
                channelDescriptors.clear();
            }

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            linkedChannels().forEach(channel -> updateState(channel, UnDefType.UNDEF));

        } catch (Exception e) {

            logger.warn("Accessing value for channel '{}' failed due {}", channelIID, e);
            updateState(channelIID, UnDefType.UNDEF);
        }
    }

    private void checkRegoDevice() throws IOException {
        if (thing.getStatus() != ThingStatus.ONLINE) {
            logger.debug("Reading Rego device version...");
            final Short regoVersion = executeCommand(CommandFactory.createReadRegoVersionCommand(),
                    ResponseParserFactory.Short);

            updateStatus(ThingStatus.ONLINE);
            logger.info("Connected to Rego version {}.", regoVersion);
        }
    }

    private <T> T executeCommand(final byte[] command, final ResponseParser<T> parser) throws IOException {
        final RegoConnection connection = this.connection;

        if (connection.isConnected() == false) {
            connection.connect();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Sending {}", DatatypeConverter.printHexBinary(command));
        }

        connection.write(command);

        byte[] response = new byte[parser.responseLength()];
        for (int i = 0; i < response.length;) {
            int value = connection.read();

            if (value == -1) {
                throw new EOFException("Connection closed");
            }

            if (i == 0 && value != ResponseParser.ComputerAddress) {
                continue;
            }

            response[i] = (byte) value;
            ++i;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Received {}", DatatypeConverter.printHexBinary(response));
        }

        return parser.parse(response);
    }
}
