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
package org.openhab.binding.oppo.internal.handler;

import static org.openhab.binding.oppo.internal.OppoBindingConstants.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.oppo.internal.OppoException;
import org.openhab.binding.oppo.internal.OppoStateDescriptionOptionProvider;
import org.openhab.binding.oppo.internal.communication.OppoCommand;
import org.openhab.binding.oppo.internal.communication.OppoConnector;
import org.openhab.binding.oppo.internal.communication.OppoIpConnector;
import org.openhab.binding.oppo.internal.communication.OppoMessageEvent;
import org.openhab.binding.oppo.internal.communication.OppoMessageEventListener;
import org.openhab.binding.oppo.internal.communication.OppoSerialConnector;
import org.openhab.binding.oppo.internal.communication.OppoStatusCodes;
import org.openhab.binding.oppo.internal.configuration.OppoThingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OppoHandler} is responsible for handling commands, which are sent to one of the channels.
 *
 * Based on the Rotel binding by Laurent Garnier
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class OppoHandler extends BaseThingHandler implements OppoMessageEventListener {

    private final Logger logger = LoggerFactory.getLogger(OppoHandler.class);

    private static final long RECON_POLLING_INTERVAL = TimeUnit.SECONDS.toSeconds(60);
    private static final long POLLING_INTERVAL = TimeUnit.SECONDS.toSeconds(30);
    private static final long INITIAL_POLLING_DELAY = TimeUnit.SECONDS.toSeconds(10);
    private static final long SLEEP_BETWEEN_CMD = TimeUnit.MILLISECONDS.toMillis(250);

    private static final String ON = "ON";
    private static final String OFF = "OFF";
    private static final String UNDEF = "UNDEF";

    private @Nullable ScheduledFuture<?> reconnectJob;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> clockSyncJob;

    private OppoStateDescriptionOptionProvider stateDescriptionProvider;
    private SerialPortManager serialPortManager;

    private @Nullable OppoConnector connector;
    
    List<StateOption> inputSourceOptions = new ArrayList<>();
    List<StateOption> hdmiModeOptions = new ArrayList<>();
    
    private Long lastEventReceived = System.currentTimeMillis();
    private Object sequenceLock = new Object();
    private String versionString = "";
    private String verboseMode = "2";
    private Boolean is203205 = false;
    private String currentChapter = "";
    private String currentTimeMode = "T";
    private String currentPlayMode = "";
    
    /**
     * Constructor
     */
    public OppoHandler(Thing thing, OppoStateDescriptionOptionProvider stateDescriptionProvider,
            SerialPortManager serialPortManager) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing handler for thing {}", getThing().getUID());

        OppoThingConfiguration config = getConfigAs(OppoThingConfiguration.class);

        // Check configuration settings
        String configError = null;
        String model = "";
        Boolean override = false;
        
        if (config.model == null) {
            configError = "player model must be specified";
        } else {
            model = config.model.toString();
            if ((config.serialPort == null || config.serialPort.isEmpty())
                    && (config.host == null || config.host.isEmpty())) {
                configError = "undefined serialPort and host configuration settings; please set one of them";
            } else if (config.host == null || config.host.isEmpty()) {
                if (config.serialPort.toLowerCase().startsWith("rfc2217")) {
                    configError = "use host and port configuration settings for a serial over IP connection";
                }
            } else {
                if (config.port == null) {
                    if ((MODEL83).equals(model)) {
                        config.port = BDP83_PORT;
                        override = true;
                    } else if ((MODEL103).equals(model) || (MODEL105).equals(model)) {
                        config.port = BDP10X_PORT;
                        override = true;
                    } else {
                        config.port = BDP20X_PORT;
                    }
                } else if (config.port <= 0) {
                    configError = "invalid port configuration setting";
                }
            }
        }

        if (configError != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, configError);
        } else {
            if (config.serialPort != null) {
                connector = new OppoSerialConnector(serialPortManager, config.serialPort);
            } else {
                connector = new OppoIpConnector(config.host, config.port);
                connector.overrideCmdPreamble(override);
            }

            this.verboseMode = config.verboseMode.toString();
            
            if (MODEL203.equals(model) || MODEL205.equals(model)) {
                this.is203205 = true;
            }
            
            this.buildOptionDropdowns(model);
            stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_SOURCE), inputSourceOptions);
            stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_HDMI_MODE), hdmiModeOptions);
            
            // remove channels not needed for this model
            List<Channel> channels = new ArrayList<>(this.getThing().getChannels());
            
            if (MODEL83.equals(model)) {
                channels.removeIf(c -> (c.getUID().getId().equals(CHANNEL_SUB_SHIFT) || c.getUID().getId().equals(CHANNEL_OSD_POSITION)));
            }
            
            if (MODEL83.equals(model) || MODEL103.equals(model) || MODEL105.equals(model)) {
                channels.removeIf(c -> (c.getUID().getId().equals(CHANNEL_ASPECT_RATIO) || c.getUID().getId().equals(CHANNEL_HDR_MODE)));
            }
            
            // no query for this, set the default value at startup
            updateChannelState(CHANNEL_TIME_MODE, currentTimeMode);
            
            updateThing(editThing().withChannels(channels).build());
                  
            updateStatus(ThingStatus.UNKNOWN);
            scheduleReconnectJob();
            schedulePollingJob();
        }

        logger.debug("Finished initializing!");
    }

    @Override
    public void dispose() {
        logger.debug("Disposing handler for thing {}", getThing().getUID());
        cancelReconnectJob();
        cancelPollingJob();
        closeConnection();
        super.dispose();
    }

    /**
     * Handle a command the UI
     *
     * @param channelUID the channel sending the command
     * @param command the command received
     * 
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();

        if (getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Thing is not ONLINE; command {} from channel {} is ignored", command, channel);
            return;
        }

        if (!connector.isConnected()) {
            logger.debug("Command {} from channel {} is ignored: connection not established", command, channel);
            return;
        }

        synchronized (sequenceLock) {
            try {
                switch (channel) {
                    case CHANNEL_POWER:
                        if (command instanceof OnOffType && command == OnOffType.ON) {
                            connector.sendCommand(OppoCommand.POWER_ON);
                            Thread.sleep(3000); // wait 3 seconds and set verbose mode  again mode just be sure
                            connector.sendCommand(OppoCommand.SET_VERBOSE_MODE, this.verboseMode);
                        } else if (command instanceof OnOffType && command == OnOffType.OFF) {
                            connector.sendCommand(OppoCommand.POWER_OFF);
                            currentPlayMode = "";
                        }
                        break;
                    case CHANNEL_VOLUME:
                        if (command instanceof PercentType) {
                            connector.sendCommand(OppoCommand.SET_VOLUME_LEVEL, command.toString());
                        }
                        break;
                    case CHANNEL_MUTE:
                        if (command instanceof OnOffType && command == OnOffType.ON) {
                            connector.sendCommand(OppoCommand.SET_VOLUME_LEVEL, "MUTE");
                        } else if (command instanceof OnOffType && command == OnOffType.OFF) {
                            connector.sendCommand(OppoCommand.MUTE);
                        }
                        break;
                    case CHANNEL_SOURCE:
                        if (command instanceof DecimalType) {
                            Integer value = ((DecimalType) command).intValue();
                            connector.sendCommand(OppoCommand.SET_INPUT_SOURCE, value.toString());
                        }
                        break;
                    case CHANNEL_CONTROL:
                        this.handleControlCommand(command);
                        break;
                    case CHANNEL_TIME_MODE:
                        if (command instanceof StringType) {
                            connector.sendCommand(OppoCommand.SET_TIME_DISPLAY, command.toString());
                            currentTimeMode = command.toString();
                        }
                        break;
                    case CHANNEL_REPEAT_MODE:
                        if (command instanceof StringType) {
                            // this one is lame, the response code when querying repeat mode is two digits,
                            // but setting it is a 2-3 letter code. 
                            connector.sendCommand(OppoCommand.SET_REPEAT, OppoStatusCodes.repeatMode.get(command.toString()));
                        }
                        break;
                    case CHANNEL_ZOOM_MODE:
                        if (command instanceof StringType) {
                            // again why could't they make the query code and set code the same?
                            connector.sendCommand(OppoCommand.SET_ZOOM_RATIO, OppoStatusCodes.zoomMode.get(command.toString()));
                        }
                        break;
                    case CHANNEL_SUB_SHIFT:
                        if (command instanceof DecimalType) {
                            Integer value = ((DecimalType) command).intValue();
                            connector.sendCommand(OppoCommand.SET_SUBTITLE_SHIFT, value.toString());
                        }
                        break;
                    case CHANNEL_OSD_POSITION:
                        if (command instanceof DecimalType) {
                            Integer value = ((DecimalType) command).intValue();
                            connector.sendCommand(OppoCommand.SET_OSD_POSITION, value.toString());
                        }
                        break;
                    case CHANNEL_HDMI_MODE:
                        if (command instanceof StringType) {
                            connector.sendCommand(OppoCommand.SET_HDMI_MODE, command.toString());
                        }
                        break;
                    case CHANNEL_HDR_MODE:
                        if (command instanceof StringType) {
                            connector.sendCommand(OppoCommand.SET_HDR_MODE, command.toString());
                        }
                        break;
                    case CHANNEL_REMOTE_BUTTON:
                        if (command instanceof StringType) {
                            connector.sendCommand(command.toString());
                        }
                        break;
                }
            } catch (OppoException | InterruptedException e) {
                logger.error("Command {} from channel {} failed: {}", command, channel, e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Sending command failed");
                closeConnection();
                scheduleReconnectJob();
            }
        }                    
    }

    /**
     * Open the connection with the Oppo player
     *
     * @return true if the connection is opened successfully or false if not
     */
    private synchronized boolean openConnection() {
        connector.addEventListener(this);
        try {
            connector.open();
        } catch (OppoException e) {
            logger.debug("openConnection() failed: {}", e.getMessage());
        }
        logger.debug("openConnection(): {}", connector.isConnected() ? "connected" : "disconnected");
        return connector.isConnected();
    }

    /**
     * Close the connection with the Oppo player
     */
    private synchronized void closeConnection() {
        if (connector !=null && connector.isConnected()) {
            connector.close();
            connector.removeEventListener(this);
            logger.debug("closeConnection(): disconnected");
        }
    }

    /**
     * Handle an event received from the Oppo player
     *
     * @param event the event to process
     */
    @Override
    public void onNewMessageEvent(EventObject event) {

        OppoMessageEvent evt = (OppoMessageEvent) event;
        logger.debug("onNewMessageEvent: key {} = {}", evt.getKey(), evt.getValue());
        lastEventReceived = System.currentTimeMillis();

        String key = evt.getKey();
        String updateData = evt.getValue().trim();
        if (this.getThing().getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, this.versionString);
        }
        Pattern p;
        
        try {
            switch (key) {
                case "NOP": // ignore
                    break;
                case "UTC":
                    // Player sent a time code update ie: 000 000 T 00:00:01
                    // g1 = title(movie only; cd always 000), g2 = chapter(movie)/track(cd), g3 = time display code, g4 = time
                    p = Pattern.compile("^(\\d{3}) (\\d{3}) ([A-Z]{1}) (\\d{2}:\\d{2}:\\d{2})$");
                    
                    try {
                        Matcher matcher=p.matcher(updateData);
                        matcher.find();
                        
                        // only update these when chapter/track changes to prevent spamming the channels with unnecessary updates
                        if (!currentChapter.equals(matcher.group(2))) {
                            currentChapter = matcher.group(2);
                            connector.sendCommand(OppoCommand.QUERY_TITLE_TRACK); // for CDs this will get track 1/x also
                            Thread.sleep(SLEEP_BETWEEN_CMD);
                            connector.sendCommand(OppoCommand.QUERY_CHAPTER); // for movies shows chapter 1/x; always 0/0 for CDs
                        }
                        
                        if (!currentTimeMode.equals(matcher.group(3))) {
                            currentTimeMode = matcher.group(3);
                            updateChannelState(CHANNEL_TIME_MODE, currentTimeMode);
                        }
                        
                        updateChannelState(CHANNEL_TIME_DISPLAY, matcher.group(4));
                    } catch (IllegalStateException | InterruptedException e){
                        logger.debug("no match on message: {}", updateData);
                    }
                    break;
                case "QTE":
                case "QTR":
                case "QCE":
                case "QCR":
                    // these are used with verbose mode 2
                    updateChannelState(CHANNEL_TIME_DISPLAY, updateData);
                    break;                    
                case "QVR":
                    this.versionString = updateData;
                    break;
                case "QPW":
                    updateChannelState(CHANNEL_POWER, updateData);
                    if (OFF.equals(updateData)) {
                        currentPlayMode = "";
                    }
                    break;
                case "UPW":
                    updateChannelState(CHANNEL_POWER, "1".equals(updateData) ? ON :OFF);
                    if ("0".equals(updateData)) {
                        currentPlayMode = "";
                    }
                    break;
                case "QVL":
                case "UVL":
                case "VUP":
                case "VDN":
                    if ("MUTE".equals(updateData) || "MUT".equals(updateData)) { // query sends MUTE, update sends MUT
                        updateChannelState(CHANNEL_MUTE, ON);
                    } else if ("UMT".equals(updateData)) {
                        updateChannelState(CHANNEL_MUTE, OFF);
                    } else {
                        updateChannelState(CHANNEL_VOLUME, updateData); 
                        updateChannelState(CHANNEL_MUTE, OFF);
                    }
                    break;
                case "QIS":
                case "UIS":
                    // example: 0 BD-PLAYER, split off just the number
                    updateChannelState(CHANNEL_SOURCE, updateData.split(" ")[0]);
                    break;
                case "UPL":                    
                    // we got the playback status update, throw it away and call the query because the text output is better
                    connector.sendCommand(OppoCommand.QUERY_PLAYBACK_STATUS); 
                    break;
                case "QTK":
                    // example: 02/10, split off both numbers
                    updateChannelState(CHANNEL_CURRENT_TITLE, updateData.split("/")[0]);
                    updateChannelState(CHANNEL_TOTAL_TITLE, updateData.split("/")[1]);
                    break;
                case "QCH":
                    // example: 03/03, split off the both numbers
                    updateChannelState(CHANNEL_CURRENT_CHAPTER, updateData.split("/")[0]);
                    updateChannelState(CHANNEL_TOTAL_CHAPTER, updateData.split("/")[1]);
                    break;
                case "QPL":
                    // if playback has stopped, we have to zero out Time, Title and Track info manually
                    if ("NO DISC".equals(updateData) || "LOADING".equals(updateData) || "OPEN".equals(updateData) || "CLOSE".equals(updateData) || "STOP".equals(updateData)) {
                        updateChannelState(CHANNEL_CURRENT_TITLE, UNDEF);
                        updateChannelState(CHANNEL_TOTAL_TITLE, UNDEF);
                        updateChannelState(CHANNEL_CURRENT_CHAPTER, UNDEF);
                        updateChannelState(CHANNEL_TOTAL_CHAPTER, UNDEF);
                        updateChannelState(CHANNEL_TIME_DISPLAY, UNDEF);
                        updateChannelState(CHANNEL_AUDIO_TYPE, UNDEF);
                        updateChannelState(CHANNEL_SUBTITLE_TYPE, UNDEF);
                    }
                    updateChannelState(CHANNEL_PLAY_MODE, updateData);
                    
                    // if switching to play mode then query the subtitle type...
                    // because if subtitles were on when playback stopped, they got nulled out above
                    // and the subtitle update message ("UST") is not sent when play starts like it is for audio
                    if ("PLAY".equals(updateData)) {
                        connector.sendCommand(OppoCommand.QUERY_SUBTITLE_TYPE);
                    }
                    currentPlayMode = updateData;
                    break;
                case "QRP":
                    updateChannelState(CHANNEL_REPEAT_MODE, updateData);
                    break;
                case "QZM":
                    updateChannelState(CHANNEL_ZOOM_MODE, updateData);
                    break;
                case "UDT":
                     // we got the disc type status update, throw it away and call the query because the text output is better
                    connector.sendCommand(OppoCommand.QUERY_DISC_TYPE);
                case "QDT":
                    updateChannelState(CHANNEL_DISC_TYPE, updateData);
                    break;
                case "UAT":
                    // we got the audio type status update, throw it away and call the query because the text output is better
                    connector.sendCommand(OppoCommand.QUERY_AUDIO_TYPE);
                    break;
                case "QAT":
                    updateChannelState(CHANNEL_AUDIO_TYPE, updateData);
                    break;
                case "UST":
                    // we got the subtitle type status update, throw it away and call the query because the text output is better
                    connector.sendCommand(OppoCommand.QUERY_SUBTITLE_TYPE);
                    break;
                case "QST":
                    updateChannelState(CHANNEL_SUBTITLE_TYPE, updateData);
                    break;
                case "UAR": // 203 & 205 only
                    updateChannelState(CHANNEL_ASPECT_RATIO, updateData);
                    break;
                case "UVO":
                    // example: _480I60 1080P60 - 1st source res, 2nd output res
                    updateChannelState(CHANNEL_SOURCE_RESOLUTION, updateData.replace("_", "").split(" ")[0]);
                    updateChannelState(CHANNEL_OUTPUT_RESOLUTION, updateData.replace("_", "").split(" ")[1]);
                    break;
                case "U3D":
                    updateChannelState(CHANNEL_3D_INDICATOR, updateData);
                    break;
                case "QSH":
                    updateChannelState(CHANNEL_SUB_SHIFT, updateData);
                    break;
                case "QOP":
                    updateChannelState(CHANNEL_OSD_POSITION, updateData);
                    break;
                case "QHD":
                    if (this.is203205) {
                        updateChannelState(CHANNEL_HDMI_MODE, updateData);
                    } else {
                        handleHdmiModeUpdate(updateData);
                    }
                    break;
                case "QHR": // 203 & 205 only
                    updateChannelState(CHANNEL_HDR_MODE, updateData);
                    break;
                default:
                    logger.debug("onNewMessageEvent: unhandled key {}, value: {}", key, updateData);
                    break;
            }
        } catch (OppoException e) {
            logger.debug("Exception sending initial commands: {}", e.getMessage());
        }
    }

    /**
     * Schedule the reconnection job
     */
    private void scheduleReconnectJob() {
        logger.debug("Schedule reconnect job");
        cancelReconnectJob();
        
        reconnectJob = scheduler.scheduleWithFixedDelay(() -> {
            if (!connector.isConnected()) {
                logger.debug("Trying to reconnect...");
                closeConnection();
                String error = null;
                if (openConnection()) {
                    synchronized (sequenceLock) {
                        try {
                            Long prevUpdateTime = lastEventReceived;
                            
                            // if the player is off most of these won't really do much...
                            connector.sendCommand(OppoCommand.SET_VERBOSE_MODE, this.verboseMode);
                            Thread.sleep(SLEEP_BETWEEN_CMD);
                            
                            OppoCommand.initialCommands.forEach(cmd -> {
                                try {
                                    connector.sendCommand(cmd);
                                    Thread.sleep(SLEEP_BETWEEN_CMD);
                                } catch (OppoException | InterruptedException e) {
                                    logger.debug("Exception sending initial commands: {}", e.getMessage());
                                }
                                
                            });
                            
                            // prevUpdateTime should have changed if a message was received from the player
                            if (prevUpdateTime.equals(lastEventReceived)) {
                                error = "Controller not responding to status requests";
                            }
                        } catch (OppoException | InterruptedException e) {
                            error = "First command after connection failed";
                            logger.debug("{}: {}", error, e.getMessage());
                        }
                    }
                } else {
                    error = "Reconnection failed";
                }
                if (error != null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
                    closeConnection();
                } else {
                    updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, this.versionString);
                }
            }
        }, 1, RECON_POLLING_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Cancel the reconnection job
     */
    private void cancelReconnectJob() {
        ScheduledFuture<?> reconnectJob = this.reconnectJob;
        if (reconnectJob != null && !reconnectJob.isCancelled()) {
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

        // when the Oppo is off, this will keep the connection (esp Serial over IP) alive and detect if the connection goes down
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            if (connector.isConnected()) {
                logger.debug("Polling the component for updated status...");

                synchronized (sequenceLock) {
                    try {
                        // for Verbose mode 2 get the current play back time if we are playing, otherwise just do NO_OP
                        if ("2".equals(this.verboseMode) && "PLAY".equals(currentPlayMode)) {
                            switch (currentTimeMode) {
                                case "T":
                                    connector.sendCommand(OppoCommand.QUERY_TITLE_ELAPSED);
                                    break;
                                case "X":
                                    connector.sendCommand(OppoCommand.QUERY_TITLE_REMAIN);
                                    break;
                                case "C":
                                    connector.sendCommand(OppoCommand.QUERY_CHAPTER_ELAPSED);
                                    break;
                                case "K":
                                    connector.sendCommand(OppoCommand.QUERY_CHAPTER_REMAIN);
                                    break;
                            }
                            Thread.sleep(SLEEP_BETWEEN_CMD);
                            
                            // make queries to refresh total number of titles/tracks & chapters
                            connector.sendCommand(OppoCommand.QUERY_TITLE_TRACK);
                            Thread.sleep(SLEEP_BETWEEN_CMD);
                            connector.sendCommand(OppoCommand.QUERY_CHAPTER);
                        } else {
                            // verbose mode 3 or off
                            connector.sendCommand(OppoCommand.NO_OP);
                        }
                        
                    } catch (OppoException | InterruptedException e) {
                        logger.debug("Polling error: {}", e.getMessage());
                    }

                    // if the last event received was more than 1.25 intervals ago,
                    // the component is not responding even though the connection is still good
                    if ((System.currentTimeMillis() - lastEventReceived) > (POLLING_INTERVAL * 1.25 * 1000)) {
                        logger.debug("Component not responding to status requests");
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Component not responding to status requests");
                        closeConnection();
                        scheduleReconnectJob();
                    } 
                }
            }
        }, INITIAL_POLLING_DELAY, POLLING_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Cancel the polling job
     */
    private void cancelPollingJob() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            this.pollingJob = null;
        }
    }

    /**
     * Update the state of a channel
     *
     * @param channel the channel
     * @param value the value to be updated
     */
    private void updateChannelState(String channel, String value) {
        if (!isLinked(channel)) {
            return;
        }
        
        if (UNDEF.equals(value)) {
            updateState(channel, UnDefType.UNDEF);
            return;
        }

        State state = UnDefType.UNDEF;
        
        switch (channel) {
            case CHANNEL_POWER:
            case CHANNEL_MUTE:
                state = ON.equals(value) ? OnOffType.ON : OnOffType.OFF;
                break;
            case CHANNEL_SOURCE:
            case CHANNEL_SUB_SHIFT:
            case CHANNEL_OSD_POSITION:
            case CHANNEL_CURRENT_TITLE:
            case CHANNEL_TOTAL_TITLE:
            case CHANNEL_CURRENT_CHAPTER:
            case CHANNEL_TOTAL_CHAPTER:
                state = new DecimalType(value);
                break;
            case CHANNEL_VOLUME:
                state = new PercentType(BigDecimal.valueOf(Integer.parseInt(value)));
                break;
            case CHANNEL_PLAY_MODE:
            case CHANNEL_TIME_MODE:
            case CHANNEL_TIME_DISPLAY:
            case CHANNEL_REPEAT_MODE:
            case CHANNEL_ZOOM_MODE:
            case CHANNEL_DISC_TYPE:
            case CHANNEL_AUDIO_TYPE:
            case CHANNEL_SUBTITLE_TYPE:
            case CHANNEL_ASPECT_RATIO:
            case CHANNEL_SOURCE_RESOLUTION:
            case CHANNEL_OUTPUT_RESOLUTION:
            case CHANNEL_3D_INDICATOR:
            case CHANNEL_HDMI_MODE:
            case CHANNEL_HDR_MODE:
                state = new StringType(value);
                break;
            default:
                break;
        }
        updateState(channel, state);
        
    }
    
    /**
     * Handle a button press from a UI Player item
     *
     * @param command the control button press command received
     */
    private void handleControlCommand(Command command) throws OppoException {
        if (command instanceof PlayPauseType) {
            if (command == PlayPauseType.PLAY) {
                connector.sendCommand(OppoCommand.PLAY);
            } else if (command == PlayPauseType.PAUSE) {
                connector.sendCommand(OppoCommand.PAUSE);
            }
        } else if (command instanceof NextPreviousType) {
            if (command == NextPreviousType.NEXT) {
                connector.sendCommand(OppoCommand.NEXT);
            } else if (command == NextPreviousType.PREVIOUS) {
                connector.sendCommand(OppoCommand.PREV);
            }
        } else if (command instanceof RewindFastforwardType) {
            if (command == RewindFastforwardType.FASTFORWARD) {
                connector.sendCommand(OppoCommand.FFORWARD);
            } else if (command == RewindFastforwardType.REWIND) {
                connector.sendCommand(OppoCommand.REWIND);
            }
        } else {
            logger.debug("Unknown control command: {}", command);
        }
    }
    
    private void buildOptionDropdowns(String model) {
        if (model.equals(MODEL83) || model.equals(MODEL103) || model.equals(MODEL105)) {
            hdmiModeOptions.add(new StateOption("AUTO","Auto"));
            hdmiModeOptions.add(new StateOption("SRC","Source Direct"));
            if (!model.equals(MODEL83)) {
                hdmiModeOptions.add(new StateOption("4K2K","4K*2K"));
            }
            hdmiModeOptions.add(new StateOption("1080P","1080P"));
            hdmiModeOptions.add(new StateOption("1080I","1080I"));
            hdmiModeOptions.add(new StateOption("720P","720P"));
            hdmiModeOptions.add(new StateOption("SDP","480P"));
            hdmiModeOptions.add(new StateOption("SDI","480I"));
        }
        
        if (model.equals(MODEL103) || model.equals(MODEL105)) {
            inputSourceOptions.add(new StateOption("0","Blu-Ray Player"));
            inputSourceOptions.add(new StateOption("1","HDMI/MHL IN-Front"));
            inputSourceOptions.add(new StateOption("2","HDMI IN-Back"));
            inputSourceOptions.add(new StateOption("3","ARC"));
            
            if (model.equals(MODEL105)) {
                inputSourceOptions.add(new StateOption("4","Optical In"));
                inputSourceOptions.add(new StateOption("5","Coaxial In"));
                inputSourceOptions.add(new StateOption("6","USB Audio In")); 
            }
        }
            
        if (model.equals(MODEL203) || model.equals(MODEL205)) {
            hdmiModeOptions.add(new StateOption("AUTO","Auto"));
            hdmiModeOptions.add(new StateOption("SRC","Source Direct"));
            hdmiModeOptions.add(new StateOption("UHD_AUTO","UHD Auto"));
            hdmiModeOptions.add(new StateOption("UHD24","UHD24"));
            hdmiModeOptions.add(new StateOption("UHD50","UHD50"));
            hdmiModeOptions.add(new StateOption("UHD60","UHD60"));
            hdmiModeOptions.add(new StateOption("1080P_AUTO","1080P Auto"));
            hdmiModeOptions.add(new StateOption("1080P24","1080P24"));
            hdmiModeOptions.add(new StateOption("1080P50","1080P50"));
            hdmiModeOptions.add(new StateOption("1080P60","1080P60"));
            hdmiModeOptions.add(new StateOption("1080I50","1080I50"));
            hdmiModeOptions.add(new StateOption("1080I60","1080I60"));
            hdmiModeOptions.add(new StateOption("720P50","720P50"));
            hdmiModeOptions.add(new StateOption("720P60","720P60"));
            hdmiModeOptions.add(new StateOption("576P","567P"));
            hdmiModeOptions.add(new StateOption("576I","567I"));
            hdmiModeOptions.add(new StateOption("480P","480P"));
            hdmiModeOptions.add(new StateOption("480I","480I"));
               
            inputSourceOptions.add(new StateOption("0","Blu-Ray Player"));
            inputSourceOptions.add(new StateOption("1","HDMI IN"));
            inputSourceOptions.add(new StateOption("2","ARC"));
            
            if (model.equals(MODEL205)) {
                inputSourceOptions.add(new StateOption("3","Optical In"));
                inputSourceOptions.add(new StateOption("4","Coaxial In"));
                inputSourceOptions.add(new StateOption("5","USB Audio In")); 
            }   
        }
    }
    
    private void handleHdmiModeUpdate(String updateData) {
        // ugly... a couple of the query hdmi mode response codes on the earlier models don't match the code to set it
        // some of this protocol is weird like that...
        if ("480I".equals(updateData)) {
            updateChannelState(CHANNEL_HDMI_MODE, "SDI");
        } else if ("480P".equals(updateData)) {
            updateChannelState(CHANNEL_HDMI_MODE, "SDP");
        } else if ("4K*2K".equals(updateData)) {
            updateChannelState(CHANNEL_HDMI_MODE, "4K2K");
        } else {
            updateChannelState(CHANNEL_HDMI_MODE, updateData);
        }
    }
}
