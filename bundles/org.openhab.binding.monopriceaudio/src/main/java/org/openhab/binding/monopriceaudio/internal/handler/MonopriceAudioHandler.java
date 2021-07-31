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
package org.openhab.binding.monopriceaudio.internal.handler;

import static org.openhab.binding.monopriceaudio.internal.MonopriceAudioBindingConstants.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.monopriceaudio.internal.MonopriceAudioException;
import org.openhab.binding.monopriceaudio.internal.MonopriceAudioStateDescriptionOptionProvider;
import org.openhab.binding.monopriceaudio.internal.communication.AmplifierModel;
import org.openhab.binding.monopriceaudio.internal.communication.MonopriceAudioConnector;
import org.openhab.binding.monopriceaudio.internal.communication.MonopriceAudioDefaultConnector;
import org.openhab.binding.monopriceaudio.internal.communication.MonopriceAudioIpConnector;
import org.openhab.binding.monopriceaudio.internal.communication.MonopriceAudioMessageEvent;
import org.openhab.binding.monopriceaudio.internal.communication.MonopriceAudioMessageEventListener;
import org.openhab.binding.monopriceaudio.internal.communication.MonopriceAudioSerialConnector;
import org.openhab.binding.monopriceaudio.internal.configuration.MonopriceAudioThingConfiguration;
import org.openhab.binding.monopriceaudio.internal.dto.MonopriceAudioZoneDTO;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MonopriceAudioHandler} is responsible for handling commands, which are sent to one of the channels.
 *
 * Based on the Rotel binding by Laurent Garnier
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class MonopriceAudioHandler extends BaseThingHandler implements MonopriceAudioMessageEventListener {
    private static final long RECON_POLLING_INTERVAL_SEC = 60;
    private static final long INITIAL_POLLING_DELAY_SEC = 5;

    private static final String ZONE = "zone";
    private static final String ALL = "all";
    private static final String CHANNEL_DELIMIT = "#";

    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int MIN_VOLUME = 0;

    private final Logger logger = LoggerFactory.getLogger(MonopriceAudioHandler.class);
    private final MonopriceAudioStateDescriptionOptionProvider stateDescriptionProvider;
    private final SerialPortManager serialPortManager;

    private @Nullable ScheduledFuture<?> reconnectJob;
    private @Nullable ScheduledFuture<?> pollingJob;

    private AmplifierModel amp = AmplifierModel.AMPLIFIER;
    private MonopriceAudioConnector connector = new MonopriceAudioDefaultConnector();

    private Map<String, MonopriceAudioZoneDTO> zoneDataMap = Map.of(ZONE, new MonopriceAudioZoneDTO());
    private Set<String> ignoreZones = new HashSet<>();
    private long lastPollingUpdate = System.currentTimeMillis();
    private long pollingInterval = ZERO;
    private int numZones = ZERO;
    private int allVolume = ONE;
    private int initialAllVolume = ZERO;
    private Object sequenceLock = new Object();

    public MonopriceAudioHandler(Thing thing, MonopriceAudioStateDescriptionOptionProvider stateDescriptionProvider,
            SerialPortManager serialPortManager) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        final String uid = this.getThing().getUID().getAsString();
        MonopriceAudioThingConfiguration config = getConfigAs(MonopriceAudioThingConfiguration.class);
        final String serialPort = config.serialPort;
        final String host = config.host;
        final Integer port = config.port;
        numZones = config.numZones;
        final String ignoreZonesConfig = config.ignoreZones;
        amp = AmplifierModel.valueOf(thing.getThingTypeUID().getId().toUpperCase());

        // build a Map with a MonopriceAudioZoneDTO for each zoneId
        zoneDataMap = amp.getZoneIds().stream().limit(numZones)
                .collect(Collectors.toMap(s -> s, s -> new MonopriceAudioZoneDTO()));

        // Check configuration settings
        String configError = null;
        if ((serialPort == null || serialPort.isEmpty()) && (host == null || host.isEmpty())) {
            configError = "undefined serialPort and host configuration settings; please set one of them";
        } else if (serialPort != null && (host == null || host.isEmpty())) {
            if (serialPort.toLowerCase().startsWith("rfc2217")) {
                configError = "use host and port configuration settings for a serial over IP connection";
            }
        } else {
            if (port == null) {
                configError = "undefined port configuration setting";
            } else if (port <= ZERO) {
                configError = "invalid port configuration setting";
            }
        }

        if (configError != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, configError);
            return;
        }

        if (serialPort != null) {
            connector = new MonopriceAudioSerialConnector(serialPortManager, serialPort, uid);
        } else if (port != null) {
            connector = new MonopriceAudioIpConnector(host, port, uid);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Either Serial port or Host & Port must be specifed");
            return;
        }
        connector.setAmplifierModel(amp);

        pollingInterval = config.pollingInterval;
        initialAllVolume = config.initialAllVolume;

        // If zones were specified to be ignored by the 'all*' commands, use the specified binding
        // zone ids to get the amplifier's internal zone ids and save those to a list
        if (ignoreZonesConfig != null) {
            for (String zone : ignoreZonesConfig.split(",")) {
                try {
                    int zoneInt = Integer.parseInt(zone);
                    if (zoneInt >= ONE && zoneInt <= amp.getMaxZones()) {
                        ignoreZones.add(ZONE + zoneInt);
                    } else {
                        logger.warn("Invalid ignore zone value: {}, value must be between {} and {}", zone, ONE,
                                amp.getMaxZones());
                    }
                } catch (NumberFormatException nfe) {
                    logger.warn("Invalid ignore zone value: {}", zone);
                }
            }
        }

        // Put the source labels on all active zones
        List<Integer> activeZones = IntStream.range(1, numZones + 1).boxed().collect(Collectors.toList());

        List<StateOption> sourceLabels = amp.getSourceLabels(config);
        stateDescriptionProvider.setStateOptions(
                new ChannelUID(getThing().getUID(), ALL + CHANNEL_DELIMIT + CHANNEL_TYPE_ALLSOURCE), sourceLabels);
        activeZones.forEach(zoneNum -> {
            stateDescriptionProvider.setStateOptions(
                    new ChannelUID(getThing().getUID(), ZONE + zoneNum + CHANNEL_DELIMIT + CHANNEL_TYPE_SOURCE),
                    sourceLabels);
        });

        // remove the channels for the zones we are not using
        if (numZones < amp.getMaxZones()) {
            List<Channel> channels = new ArrayList<>(this.getThing().getChannels());

            List<Integer> zonesToRemove = IntStream.range(numZones + 1, amp.getMaxZones() + 1).boxed()
                    .collect(Collectors.toList());

            zonesToRemove.forEach(zone -> {
                channels.removeIf(c -> (c.getUID().getId().contains(ZONE + zone)));
            });
            updateThing(editThing().withChannels(channels).build());
        }

        // initialize the all volume state
        allVolume = initialAllVolume;
        long allVolumePct = Math
                .round((initialAllVolume - MIN_VOLUME) / (double) (amp.getMaxVol() - MIN_VOLUME) * 100.0);
        updateState(ALL + CHANNEL_DELIMIT + CHANNEL_TYPE_ALLVOLUME, new PercentType(BigDecimal.valueOf(allVolumePct)));

        scheduleReconnectJob();
        schedulePollingJob();

        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void dispose() {
        cancelReconnectJob();
        cancelPollingJob();
        closeConnection();
        ignoreZones.clear();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();
        String[] channelSplit = channel.split(CHANNEL_DELIMIT);
        String channelType = channelSplit[1];
        String zoneName = channelSplit[0];
        String zoneId = amp.getZoneIdFromZoneName(zoneName);

        if (getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Thing is not ONLINE; command {} from channel {} is ignored", command, channel);
            return;
        }

        boolean success = true;
        synchronized (sequenceLock) {
            if (!connector.isConnected()) {
                logger.debug("Command {} from channel {} is ignored: connection not established", command, channel);
                return;
            }

            if (command instanceof RefreshType) {
                MonopriceAudioZoneDTO zoneDTO = zoneDataMap.get(zoneId);
                if (zoneDTO != null) {
                    updateChannelState(zoneName, channelType, zoneDTO);
                } else {
                    logger.info("Could not execute REFRESH command for zone {}: null", zoneId);
                }
                return;
            }

            Stream<String> zoneStream = amp.getZoneIds().stream().limit(numZones);
            try {
                switch (channelType) {
                    case CHANNEL_TYPE_POWER:
                        if (command instanceof OnOffType) {
                            connector.sendCommand(zoneId, amp.getPowerCmd(), command == OnOffType.ON ? ONE : ZERO);
                            zoneDataMap.get(zoneId)
                                    .setPower(command == OnOffType.ON ? amp.getOnStr() : amp.getOffStr());
                        }
                        break;
                    case CHANNEL_TYPE_SOURCE:
                        if (command instanceof DecimalType) {
                            int value = ((DecimalType) command).intValue();
                            if (value >= ONE && value <= amp.getMaxSrc()) {
                                logger.debug("Got source command {} zone {}", value, zoneId);
                                connector.sendCommand(zoneId, amp.getSourceCmd(), value);
                                zoneDataMap.get(zoneId).setSource(String.format("%02d", value));
                            }
                        }
                        break;
                    case CHANNEL_TYPE_VOLUME:
                        if (command instanceof PercentType) {
                            int value = (int) Math.round(
                                    ((PercentType) command).doubleValue() / 100.0 * (amp.getMaxVol() - MIN_VOLUME))
                                    + MIN_VOLUME;
                            logger.debug("Got volume command {} zone {}", value, zoneId);
                            connector.sendCommand(zoneId, amp.getVolumeCmd(), value);
                            zoneDataMap.get(zoneId).setVolume(value);
                        }
                        break;
                    case CHANNEL_TYPE_MUTE:
                        if (command instanceof OnOffType) {
                            connector.sendCommand(zoneId, amp.getMuteCmd(), command == OnOffType.ON ? ONE : ZERO);
                            zoneDataMap.get(zoneId).setMute(command == OnOffType.ON ? amp.getOnStr() : amp.getOffStr());
                        }
                        break;
                    case CHANNEL_TYPE_TREBLE:
                        if (command instanceof DecimalType) {
                            int value = ((DecimalType) command).intValue();
                            if (value >= amp.getMinTone() && value <= amp.getMaxTone()) {
                                logger.debug("Got treble command {} zone {}", value, zoneId);
                                connector.sendCommand(zoneId, amp.getTrebleCmd(), value + amp.getToneOffset());
                                zoneDataMap.get(zoneId).setTreble(value + amp.getToneOffset());
                            }
                        }
                        break;
                    case CHANNEL_TYPE_BASS:
                        if (command instanceof DecimalType) {
                            int value = ((DecimalType) command).intValue();
                            if (value >= amp.getMinTone() && value <= amp.getMaxTone()) {
                                logger.debug("Got bass command {} zone {}", value, zoneId);
                                connector.sendCommand(zoneId, amp.getBassCmd(), value + amp.getToneOffset());
                                zoneDataMap.get(zoneId).setBass(value + amp.getToneOffset());
                            }
                        }
                        break;
                    case CHANNEL_TYPE_BALANCE:
                        if (command instanceof DecimalType) {
                            int value = ((DecimalType) command).intValue();
                            if (value >= amp.getMinBal() && value <= amp.getMaxBal()) {
                                logger.debug("Got balance command {} zone {}", value, zoneId);
                                connector.sendCommand(zoneId, amp.getBalanceCmd(), value + amp.getBalOffset());
                                zoneDataMap.get(zoneId).setBalance(value + amp.getBalOffset());
                            }
                        }
                        break;
                    case CHANNEL_TYPE_DND:
                        if (command instanceof OnOffType) {
                            connector.sendCommand(zoneId, amp.getDndCmd(), command == OnOffType.ON ? ONE : ZERO);
                            zoneDataMap.get(zoneId).setDnd(command == OnOffType.ON ? amp.getOnStr() : amp.getOffStr());
                        }
                        break;
                    case CHANNEL_TYPE_ALLPOWER:
                        if (command instanceof OnOffType) {
                            zoneStream.forEach((streamZoneId) -> {
                                if (command == OnOffType.OFF || !ignoreZones.contains(amp.getZoneName(streamZoneId))) {
                                    try {
                                        connector.sendCommand(streamZoneId, amp.getPowerCmd(),
                                                command == OnOffType.ON ? ONE : ZERO);
                                        if (command == OnOffType.ON) {
                                            // reset the volume of each zone to allVolume
                                            connector.sendCommand(streamZoneId, amp.getVolumeCmd(), allVolume);
                                        }
                                    } catch (MonopriceAudioException e) {
                                        logger.warn("Error Turning All Zones On: {}", e.getMessage());
                                    }
                                }

                            });
                        }
                        break;
                    case CHANNEL_TYPE_ALLSOURCE:
                        if (command instanceof DecimalType) {
                            int value = ((DecimalType) command).intValue();
                            if (value >= ONE && value <= amp.getMaxSrc()) {
                                zoneStream.forEach((streamZoneId) -> {
                                    if (!ignoreZones.contains(amp.getZoneName(streamZoneId))) {
                                        try {
                                            connector.sendCommand(streamZoneId, amp.getSourceCmd(), value);
                                        } catch (MonopriceAudioException e) {
                                            logger.warn("Error Setting Source for  All Zones: {}", e.getMessage());
                                        }
                                    }
                                });
                            }
                        }
                        break;
                    case CHANNEL_TYPE_ALLVOLUME:
                        if (command instanceof PercentType) {
                            int value = (int) Math.round(
                                    ((PercentType) command).doubleValue() / 100.0 * (amp.getMaxVol() - MIN_VOLUME))
                                    + MIN_VOLUME;
                            allVolume = value;
                            zoneStream.forEach((streamZoneId) -> {
                                if (!ignoreZones.contains(amp.getZoneName(streamZoneId))) {
                                    try {
                                        connector.sendCommand(streamZoneId, amp.getVolumeCmd(), value);
                                    } catch (MonopriceAudioException e) {
                                        logger.warn("Error Setting Volume for All Zones: {}", e.getMessage());
                                    }
                                }
                            });
                        }
                        break;
                    case CHANNEL_TYPE_ALLMUTE:
                        if (command instanceof OnOffType) {
                            int cmd = command == OnOffType.ON ? ONE : ZERO;
                            zoneStream.forEach((streamZoneId) -> {
                                if (!ignoreZones.contains(amp.getZoneName(streamZoneId))) {
                                    try {
                                        connector.sendCommand(streamZoneId, amp.getMuteCmd(), cmd);
                                    } catch (MonopriceAudioException e) {
                                        logger.warn("Error Setting Mute for All Zones: {}", e.getMessage());
                                    }
                                }
                            });
                        }
                        break;
                    default:
                        success = false;
                        logger.debug("Command {} from channel {} failed: unexpected command", command, channel);
                        break;
                }

                if (success) {
                    logger.trace("Command {} from channel {} succeeded", command, channel);
                }
            } catch (MonopriceAudioException e) {
                logger.warn("Command {} from channel {} failed: {}", command, channel, e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Sending command failed");
                closeConnection();
                scheduleReconnectJob();
            }
        }
    }

    /**
     * Open the connection with the MonopriceAudio device
     *
     * @return true if the connection is opened successfully or false if not
     */
    private synchronized boolean openConnection() {
        connector.addEventListener(this);
        try {
            connector.open();
        } catch (MonopriceAudioException e) {
            logger.debug("openConnection() failed: {}", e.getMessage());
        }
        logger.debug("openConnection(): {}", connector.isConnected() ? "connected" : "disconnected");
        return connector.isConnected();
    }

    /**
     * Close the connection with the MonopriceAudio device
     */
    private synchronized void closeConnection() {
        if (connector.isConnected()) {
            connector.close();
            connector.removeEventListener(this);
            logger.debug("closeConnection(): disconnected");
        }
    }

    @Override
    public void onNewMessageEvent(MonopriceAudioMessageEvent evt) {
        String key = evt.getKey();
        String updateData = evt.getValue().trim();
        if (!MonopriceAudioConnector.KEY_ERROR.equals(key)) {
            updateStatus(ThingStatus.ONLINE);
        }
        try {
            switch (key) {
                case MonopriceAudioConnector.KEY_ERROR:
                    logger.debug("Reading feedback message failed");
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Reading thread ended");
                    closeConnection();
                    break;

                case MonopriceAudioConnector.KEY_ZONE_UPDATE:
                    MonopriceAudioZoneDTO newZoneData = amp.getZoneData(updateData);
                    String zoneId = newZoneData.getZone();
                    MonopriceAudioZoneDTO zoneData = zoneDataMap.get(zoneId);
                    String zoneName = amp.getZoneName(zoneId);
                    if (amp.getZoneIds().contains(zoneId) && zoneData != null) {
                        processZoneUpdate(zoneName, zoneData, newZoneData);
                    } else {
                        logger.warn("invalid event: {} for key: {} or zone data null", evt.getValue(), key);
                    }
                    break;
                default:
                    logger.debug("onNewMessageEvent: unhandled key {}", key);
                    break;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid value {} for key {}", updateData, key);
        }
    }

    /**
     * Schedule the reconnection job
     */
    private void scheduleReconnectJob() {
        logger.debug("Schedule reconnect job");
        cancelReconnectJob();
        reconnectJob = scheduler.scheduleWithFixedDelay(() -> {
            synchronized (sequenceLock) {
                if (!connector.isConnected()) {
                    logger.debug("Trying to reconnect...");
                    closeConnection();
                    String error = null;

                    if (openConnection()) {
                        try {
                            long prevUpdateTime = lastPollingUpdate;

                            if (amp == AmplifierModel.MONOPRICE70V) {
                                // for 31028 query treble, bass & balance for all zones once per reconnect
                                amp.getZoneIds().stream().limit(numZones).forEach((streamZoneId) -> {
                                    try {
                                        connector.queryBalanceTone(streamZoneId);
                                    } catch (MonopriceAudioException e) {
                                        logger.warn("Polling error: {}", e.getMessage());
                                    }
                                });
                            } else if (amp == AmplifierModel.XANTECH44 || amp == AmplifierModel.XANTECH88) {
                                // for xantech send the commands to enable unsolicited updates
                                connector.sendCommand("", "ZA1", null);
                                connector.sendCommand("", "ZP1", null);
                            } else {
                                // for 10761, just query zone 1 to see if the amp responds
                                connector.queryZone(amp.getZoneIds().get(0));
                            }

                            // prevUpdateTime should have changed if a zone update was received
                            if (lastPollingUpdate == prevUpdateTime) {
                                error = "Amplifier not responding to status requests";
                            }

                        } catch (MonopriceAudioException e) {
                            error = "First command after connection failed";
                            logger.warn("{}: {}", error, e.getMessage());
                            closeConnection();
                        }
                    } else {
                        error = "Reconnection failed";
                    }
                    if (error != null) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
                    } else {
                        updateStatus(ThingStatus.ONLINE);
                        lastPollingUpdate = System.currentTimeMillis();
                    }
                }
            }
        }, 1, RECON_POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /**
     * Cancel the reconnection job
     */
    private void cancelReconnectJob() {
        ScheduledFuture<?> reconnectJob = this.reconnectJob;
        if (reconnectJob != null) {
            reconnectJob.cancel(true);
            this.reconnectJob = null;
        }
    }

    /**
     * Schedule the polling job
     */
    private void schedulePollingJob() {
        logger.debug("Schedule polling job");
        cancelPollingJob();

        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            synchronized (sequenceLock) {
                if (connector.isConnected()) {
                    logger.debug("Polling the amplifier for updated status...");

                    // poll each zone up to the number of zones specified in the configuration
                    amp.getZoneIds().stream().limit(numZones).forEach((zoneIdStream) -> {
                        try {
                            connector.queryZone(zoneIdStream);
                        } catch (MonopriceAudioException e) {
                            logger.warn("Polling error: {}", e.getMessage());
                        }
                    });

                    // if the last successful polling update was more than 2.25 intervals ago, the amplifier
                    // is either switched off or not responding even though the connection is still good
                    if ((System.currentTimeMillis() - lastPollingUpdate) > (pollingInterval * 2.25 * 1000)) {
                        logger.warn("Amplifier not responding to status requests");
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "Amplifier not responding to status requests");
                        closeConnection();
                        scheduleReconnectJob();
                    }
                }
            }
        }, INITIAL_POLLING_DELAY_SEC, pollingInterval, TimeUnit.SECONDS);
    }

    /**
     * Cancel the polling job
     */
    private void cancelPollingJob() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null) {
            pollingJob.cancel(true);
            this.pollingJob = null;
        }
    }

    /**
     * Update the state of a channel
     *
     * @param channel the channel
     */
    private void updateChannelState(String zoneName, String channelType, MonopriceAudioZoneDTO zoneData) {
        String channel = zoneName + CHANNEL_DELIMIT + channelType;

        if (!isLinked(channel)) {
            return;
        }

        State state = UnDefType.UNDEF;
        switch (channelType) {
            case CHANNEL_TYPE_POWER:
                state = zoneData.isPowerOn() ? OnOffType.ON : OnOffType.OFF;
                break;
            case CHANNEL_TYPE_SOURCE:
                state = new DecimalType(zoneData.getSource());
                break;
            case CHANNEL_TYPE_VOLUME:
                long volumePct = Math
                        .round((zoneData.getVolume() - MIN_VOLUME) / (double) (amp.getMaxVol() - MIN_VOLUME) * 100.0);
                state = new PercentType(BigDecimal.valueOf(volumePct));
                break;
            case CHANNEL_TYPE_MUTE:
                state = zoneData.isMuted() ? OnOffType.ON : OnOffType.OFF;
                break;
            case CHANNEL_TYPE_TREBLE:
                state = new DecimalType(BigDecimal.valueOf(zoneData.getTreble() - amp.getToneOffset()));
                break;
            case CHANNEL_TYPE_BASS:
                state = new DecimalType(BigDecimal.valueOf(zoneData.getBass() - amp.getToneOffset()));
                break;
            case CHANNEL_TYPE_BALANCE:
                state = new DecimalType(BigDecimal.valueOf(zoneData.getBalance() - amp.getBalOffset()));
                break;
            case CHANNEL_TYPE_DND:
                state = zoneData.isDndOn() ? OnOffType.ON : OnOffType.OFF;
                break;
            case CHANNEL_TYPE_PAGE:
                state = zoneData.isPageActive() ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
                break;
            case CHANNEL_TYPE_KEYPAD:
                state = zoneData.isKeypadActive() ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
                break;
            default:
                break;
        }
        updateState(channel, state);
    }

    private void processZoneUpdate(String zoneName, MonopriceAudioZoneDTO zoneData, MonopriceAudioZoneDTO newZoneData) {
        // only process the update if something actually changed in this zone since the last time through
        if (!newZoneData.toString().equals(zoneData.toString())) {
            zoneData.setZone(newZoneData.getZone());

            if (!newZoneData.getPage().equals(zoneData.getPage())) {
                zoneData.setPage(newZoneData.getPage());
                updateChannelState(zoneName, CHANNEL_TYPE_PAGE, zoneData);
            }

            if (!newZoneData.getPower().equals(zoneData.getPower())) {
                zoneData.setPower(newZoneData.getPower());
                updateChannelState(zoneName, CHANNEL_TYPE_POWER, zoneData);
            }

            if (!newZoneData.getMute().equals(zoneData.getMute())) {
                zoneData.setMute(newZoneData.getMute());
                updateChannelState(zoneName, CHANNEL_TYPE_MUTE, zoneData);
            }

            if (!newZoneData.getDnd().equals(zoneData.getDnd())) {
                zoneData.setDnd(newZoneData.getDnd());
                updateChannelState(zoneName, CHANNEL_TYPE_DND, zoneData);
            }

            if (newZoneData.getVolume() != zoneData.getVolume()) {
                zoneData.setVolume(newZoneData.getVolume());
                updateChannelState(zoneName, CHANNEL_TYPE_VOLUME, zoneData);
            }

            // check for a real value since the 31028's normal polling does not populate Treble, Bass and Balance
            if (newZoneData.getTreble() != NIL && newZoneData.getTreble() != zoneData.getTreble()) {
                zoneData.setTreble(newZoneData.getTreble());
                updateChannelState(zoneName, CHANNEL_TYPE_TREBLE, zoneData);
            }

            if (newZoneData.getBass() != NIL && newZoneData.getBass() != zoneData.getBass()) {
                zoneData.setBass(newZoneData.getBass());
                updateChannelState(zoneName, CHANNEL_TYPE_BASS, zoneData);
            }

            if (newZoneData.getBalance() != NIL && newZoneData.getBalance() != zoneData.getBalance()) {
                zoneData.setBalance(newZoneData.getBalance());
                updateChannelState(zoneName, CHANNEL_TYPE_BALANCE, zoneData);
            }

            if (!newZoneData.getSource().equals(zoneData.getSource())) {
                zoneData.setSource(newZoneData.getSource());
                updateChannelState(zoneName, CHANNEL_TYPE_SOURCE, zoneData);
            }

            if (!newZoneData.getKeypad().equals(zoneData.getKeypad())) {
                zoneData.setKeypad(newZoneData.getKeypad());
                updateChannelState(zoneName, CHANNEL_TYPE_KEYPAD, zoneData);
            }

        }
        lastPollingUpdate = System.currentTimeMillis();
    }
}
