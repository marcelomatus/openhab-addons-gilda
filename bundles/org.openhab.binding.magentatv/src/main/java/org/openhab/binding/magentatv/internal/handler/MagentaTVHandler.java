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
package org.openhab.binding.magentatv.internal.handler;

import static org.openhab.binding.magentatv.internal.MagentaTVBindingConstants.*;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Unit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.magentatv.internal.MagentaTVConfiguration;
import org.openhab.binding.magentatv.internal.MagentaTVException;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRPayEvent;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRPayEventInstanceCreator;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRProgramInfoEvent;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRProgramInfoEventInstanceCreator;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRProgramStatus;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRProgramStatusInstanceCreator;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRShortProgramInfo;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.MRShortProgramInfoInstanceCreator;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.OAuthAutenhicateResponse;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.OAuthTokenResponse;
import org.openhab.binding.magentatv.internal.MagentaTVGsonDTO.OauthCredentials;
import org.openhab.binding.magentatv.internal.MagentaTVHandlerFactory;
import org.openhab.binding.magentatv.internal.network.MagentaTVNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link MagentaTVHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class MagentaTVHandler extends BaseThingHandler implements MagentaTVListener {
    private final Logger logger = LoggerFactory.getLogger(MagentaTVHandler.class);
    private static final String EMPTY_CRED = "***";
    private static final DecimalType ZERO = new DecimalType(0);

    protected final MagentaTVConfiguration thingConfig = new MagentaTVConfiguration();
    private final Gson gson;
    protected final MagentaTVNetwork network;
    protected final MagentaTVHandlerFactory handlerFactory;
    protected MagentaTVControl control = new MagentaTVControl();

    private String thingId = "";
    private volatile int idRefresh = 0;
    private @Nullable ScheduledFuture<?> initializeJob;
    private @Nullable ScheduledFuture<?> pairingWatchdogJob;
    private @Nullable ScheduledFuture<?> renewEventJob;

    /**
     * Constructor, save bindingConfig (services as default for thingConfig)
     *
     * @param thing
     * @param bindingConfig
     */
    public MagentaTVHandler(MagentaTVHandlerFactory handlerFactory, Thing thing, MagentaTVNetwork network) {
        super(thing);
        this.handlerFactory = handlerFactory;
        this.network = network;
        gson = new GsonBuilder().registerTypeAdapter(OauthCredentials.class, new MRProgramInfoEventInstanceCreator())
                .registerTypeAdapter(OAuthTokenResponse.class, new MRProgramStatusInstanceCreator())
                .registerTypeAdapter(OAuthAutenhicateResponse.class, new MRShortProgramInfoInstanceCreator())
                .registerTypeAdapter(OAuthAutenhicateResponse.class, new MRPayEventInstanceCreator()).create();
    }

    /**
     * Thing initialization:
     * - initialize thing status from UPnP discovery, thing config, local network settings
     * - perform Oath if userID is not configured and credentials are available
     * - wait for NotifyServlet to initialize (solves timing issues on fast startup)
     */
    @Override
    public void initialize() {
        // The framework requires you to return from this method quickly. For that the initialization itself is executed
        // asynchronously
        String label = getThing().getLabel();
        thingId = label != null ? label : "";
        resetEventChannels();
        updateStatus(ThingStatus.UNKNOWN);

        thingConfig.fromProperties(getThing().getProperties());
        thingConfig.initializeConfig(getConfig().getProperties());
        thingId = thingConfig.getFriendlyName();

        try {
            initializeJob = scheduler.schedule(() -> {
                initializeThing();
            }, 5, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            logger.warn("Unable to schedule thing initialization", e);
        }
    }

    private void initializeThing() {
        String errorMessage = "";
        try {
            if (thingConfig.getUDN().isEmpty()) {
                // get UDN from device name
                String uid = this.getThing().getUID().getAsString();
                thingConfig.setUDN(StringUtils.substringAfterLast(uid, ":"));
            }
            if (thingConfig.getMacAddress().isEmpty()) {
                // get MAC address from UDN (last 12 digits)
                String macAddress = StringUtils.substringAfterLast(thingConfig.getUDN(), "_");
                if (macAddress.isEmpty()) {
                    macAddress = StringUtils.substringAfterLast(thingConfig.getUDN(), "-");
                }
                thingConfig.setMacAddress(macAddress);
            }
            control = new MagentaTVControl(thingConfig, network);

            // Check for emoty credentials (e.g. missing in .things file)
            String account = thingConfig.getAccountName();
            if (thingConfig.getUserID().isEmpty()
                    && (thingConfig.getAccountName().isEmpty() || account.equals(EMPTY_CRED))) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Missing credentials for login!");
                return;
            }

            logger.info("{}: Authenticate account {}", thingId, account);
            thingConfig.updateConfig(control.getConfig().getProperties()); // get network parameters from control
            authenticateUser();
            connectReceiver(); // throws MagentaTVException on error

            // setup background device check
            renewEventJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    renewEventSubscription();
                }
            }, 2, 5, TimeUnit.MINUTES);

            // change to ThingStatus.ONLINE will be done when the pairing result is received
            // (see onPairingResult())
        } catch (MagentaTVException e) {
            errorMessage = e.toString();
        } catch (RuntimeException e) {
            logger.warn("{}: Exception on initialization", thingId, e);
        } finally {
            if (!errorMessage.isEmpty()) {
                logger.debug("{}: {}", thingId, errorMessage);
                setOnlineState(ThingStatus.OFFLINE, errorMessage);
            }
        }
    }

    /**
     * This routine is called every time the Thing configuration has been changed (e.g. PaperUI)
     */
    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        logger.debug("{}: Thing config updated, re-initialize", thingId);
        cancelAllJobs();
        if (configurationParameters.containsKey(PROPERTY_ACCT_NAME)) {
            String newAccount = (String) configurationParameters.get(PROPERTY_ACCT_NAME);
            if (!newAccount.equals(EMPTY_CRED)) {
                // new account info, need to renew userId
                thingConfig.setUserID("");
            }
        }

        super.handleConfigurationUpdate(configurationParameters);

        // initializeThing();
    }

    /**
     * Handle channel commands
     *
     * @param channelUID - the channel, which received the command
     * @param command - the actual command (could be instance of StringType,
     *            DecimalType or OnOffType)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH) {
            // currently no channels to be refreshed
            return;
        }

        try {
            if (!isOnline() || command.toString().equalsIgnoreCase("PAIR")) {
                logger.debug("{}: Receiver {} is offline, try to (re-)connect", thingId, deviceName());
                connectReceiver(); // reconnect to MR, throws an exception if this fails
            }

            logger.debug("{}: Channel command for device {}: {} for channel {}", thingId, thingConfig.getFriendlyName(),
                    command, channelUID.getId());
            switch (channelUID.getId()) {
                case CHANNEL_POWER: // toggle power
                    logger.debug("{}: Toggle power, new state={}", thingId, command);
                    control.sendKey("POWER");
                    break;
                case CHANNEL_PLAYER:
                    logger.debug("{}: Player command: {}", thingId, command);
                    if (command instanceof OnOffType) {
                        control.sendKey("POWER");
                    } else if (command instanceof PlayPauseType) {
                        if (command == PlayPauseType.PLAY) {
                            control.sendKey("PLAY");
                        } else if (command == PlayPauseType.PAUSE) {
                            control.sendKey("PAUSE");
                        }
                    } else if (command instanceof NextPreviousType) {
                        if (command == NextPreviousType.NEXT) {
                            control.sendKey("NEXTCH");
                        } else if (command == NextPreviousType.PREVIOUS) {
                            control.sendKey("PREVCH");
                        }
                    } else if (command instanceof RewindFastforwardType) {
                        if (command == RewindFastforwardType.FASTFORWARD) {
                            control.sendKey("FORWARD");
                        } else if (command == RewindFastforwardType.REWIND) {
                            control.sendKey("REWIND");
                        }
                    } else {
                        logger.debug("{}: Unknown media command: {}", thingId, command);
                    }
                    break;
                case CHANNEL_CHANNEL:
                    String chan = command.toString();
                    control.selectChannel(chan);
                    break;
                case CHANNEL_MUTE:
                    if (command == OnOffType.ON) {
                        control.sendKey("MUTE");
                    } else {
                        control.sendKey("VOLUP");
                    }
                    break;
                case CHANNEL_KEY:
                    if (command.toString().equalsIgnoreCase("PAIR")) { // special key to re-pair receiver (already done
                                                                       // above)
                        logger.debug("{}: PAIRing key received, reconnect receiver {}", thingId, deviceName());
                    } else {
                        control.sendKey(command.toString());
                        mapKeyToMediateState(command.toString());
                    }
                    break;
                default:
                    logger.debug("{}: Command {} for unknown channel {}", thingId, command, channelUID.getAsString());
            }
        } catch (MagentaTVException e) {
            String errorMessage = MessageFormat.format("Channel operation failed (command={0}, value={1}): {2}",
                    command, channelUID.getId(), e.toString());
            logger.debug("{}: {}", thingId, errorMessage);
            setOnlineState(ThingStatus.OFFLINE, errorMessage);
        }
    }

    private void mapKeyToMediateState(String key) {
        State state = null;
        switch (key.toUpperCase()) {
            case "PLAY":
                state = PlayPauseType.PLAY;
                break;
            case "PAUSE":
                state = PlayPauseType.PAUSE;
                break;
            case "FORWARD":
                state = RewindFastforwardType.FASTFORWARD;
                break;
            case "REWIND":
                updateState(CHANNEL_PLAYER, RewindFastforwardType.REWIND);
                break;
        }
        if (state != null) {
            logger.debug("{}: Setting Player state to {}", thingId, state);
            updateState(CHANNEL_PLAYER, state);
        }
    }

    /**
     * Connect to the receiver
     *
     * @throws MagentaTVException something failed
     */
    protected void connectReceiver() throws MagentaTVException {
        if (control.checkDev()) {
            thingConfig.updateConfig(control.getConfig().getProperties()); // get description data
            updateThingProperties(thingConfig.getProperties());
            handlerFactory.registerDevice(thingConfig.getUDN(), thingConfig.getTerminalID(), thingConfig.getIpAddress(),
                    this);
            control.subscribeEventChannel();
            control.sendPairingRequest();
            updateThingProperties(thingConfig.getProperties());

            // check for pairing timeout
            final int iRefresh = ++idRefresh;
            pairingWatchdogJob = scheduler.schedule(() -> {
                if (iRefresh == idRefresh) { // Make a best effort to not run multiple deferred refresh
                    if (thingConfig.getVerificationCode().isEmpty()) {
                        setOnlineState(ThingStatus.OFFLINE, "Timeout on pairing request!");
                    }
                }
            }, 15, TimeUnit.SECONDS);
        }
    }

    /**
     * If userID is empty and credentials are given the Telekom OAuth service is
     * used to query the userID
     *
     * @throws MagentaTVException
     */
    private void authenticateUser() throws MagentaTVException {
        String userId = thingConfig.getUserID();
        if (userId.isEmpty()) {
            // run OAuth authentication, this finally provides the userID
            userId = control.authenticateUser(thingConfig.getAccountName(), thingConfig.getAccountPassword());

            // Update thing configuration (persistent) - remove credentials, add userID
            Configuration configuration = this.getConfig();
            configuration.remove(PROPERTY_ACCT_NAME);
            configuration.remove(PROPERTY_ACCT_PWD);
            configuration.remove(PROPERTY_USERID);
            configuration.put(PROPERTY_ACCT_NAME, EMPTY_CRED);
            configuration.put(PROPERTY_ACCT_PWD, EMPTY_CRED);
            configuration.put(PROPERTY_USERID, userId);
            this.updateConfiguration(configuration);
            thingConfig.setAccountName(EMPTY_CRED);
            thingConfig.setAccountPassword(EMPTY_CRED);
        } else {
            logger.debug("{}: Skip OAuth, use existing userID {}", thingId, thingConfig.getUserID());
        }
        if (!userId.isEmpty()) {
            thingConfig.setUserID(userId);
        } else {
            logger.warn("{}: Unable to obtain userId from OAuth", thingId);
        }
    }

    /**
     * Update thing status
     *
     * @param mode new thing status
     * @return ON = power on, OFF=power off
     */
    public void setOnlineState(ThingStatus newStatus, String errorMessage) {
        ThingStatus status = this.getThing().getStatus();
        if (status != newStatus) {
            if (newStatus == ThingStatus.INITIALIZING) {
                logger.debug("{}: Invalid new thing state: {}", thingId, newStatus);
            }
            if (newStatus == ThingStatus.ONLINE) {
                updateStatus(newStatus);
                updateState(CHANNEL_POWER, OnOffType.ON);
            } else {
                if (!errorMessage.isEmpty()) {
                    logger.debug("{}: Communication Error - {}, switch Thing offline", thingId, errorMessage);
                    updateStatus(newStatus, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
                } else {
                    updateStatus(newStatus);
                }
                updateState(CHANNEL_POWER, OnOffType.OFF);
            }
        }
    }

    /**
     * A wakeup of the MR was detected (e.g. UPnP received)
     *
     * @throws MagentaTVException
     */
    @Override
    public void onWakeup(Map<String, String> discoveredProperties) throws MagentaTVException {
        if ((this.getThing().getStatus() == ThingStatus.OFFLINE) || thingConfig.getVerificationCode().isEmpty()) {
            // Device sent a UPnP discovery information, trigger to reconnect
            connectReceiver();
        } else {
            logger.debug("{}: Refesh device status for {} (UDN={}", thingId, deviceName(), thingConfig.getUDN());
            setOnlineState(ThingStatus.ONLINE, "");
        }
    }

    /**
     * The pairing result has been received. The pairing code will be used to generate the verification code and
     * complete pairing with the MR. Finally if pairing was completed successful the thing status will change to ONLINE
     *
     * @param pairingCode pairing code received from MR (NOTIFY event data)
     * @throws MagentaTVException
     */
    @Override
    public void onPairingResult(String pairingCode) throws MagentaTVException {
        if (control.isInitialized()) {
            thingConfig.updateConfig(control.getConfig().getProperties()); // get description data

            if (control.generateVerificationCode(pairingCode)) {
                thingConfig.setPairingCode(pairingCode);
                logger.debug(
                        "{}: Pairing code received (UDN {}, terminalID {}, pairingCode={}, verificationCode={}, userID={})",
                        thingId, thingConfig.getUDN(), thingConfig.getTerminalID(), thingConfig.getPairingCode(),
                        thingConfig.getVerificationCode(), thingConfig.getUserID());

                // verify pairing completes the pairing process
                if (control.verifyPairing()) {
                    logger.debug("{}: Pairing completed for device {} ({}), Thing now ONLINE", thingId,
                            thingConfig.getFriendlyName(), thingConfig.getTerminalID());
                    setOnlineState(ThingStatus.ONLINE, "");
                    cancelPairingCheck(); // stop timeout check
                }
            }
        } else {
            logger.debug("{}: control not yet initialized!", thingId);
        }
    }

    @Override
    public void onMREvent(String jsonInput) {
        logger.trace("{}: Process MR event for device {}, json={}", thingId, deviceName(), jsonInput);
        boolean flUpdatePower = false;
        String jsonEvent = fixEventJson(jsonInput);
        if (jsonEvent.contains(MR_EVENT_EIT_CHANGE)) {
            logger.debug("{}: EVENT_EIT_CHANGE event received.", thingId);

            MRProgramInfoEvent pinfo = gson.fromJson(jsonEvent, MRProgramInfoEvent.class);
            if (!pinfo.channelNum.isEmpty()) {
                logger.debug("{}: EVENT_EIT_CHANGE for channel {}/{}", thingId, pinfo.channelNum, pinfo.channelCode);
                updateState(CHANNEL_CHANNEL, new DecimalType(pinfo.channelNum));
                updateState(CHANNEL_CHANNEL_CODE, new DecimalType(pinfo.channelCode));
            }
            if (pinfo.programInfo != null) {
                int i = 0;
                for (MRProgramStatus ps : pinfo.programInfo) {
                    if ((ps.startTime == null) || ps.startTime.isEmpty()) {
                        logger.debug("{}: EVENT_EIT_CHANGE: empty event data = {}", thingId, jsonEvent);
                        continue; // empty program_info
                    }
                    updateState(CHANNEL_RUN_STATUS, new StringType(control.getRunStatus(ps.runningStatus)));

                    if (ps.shortEvent != null) {
                        for (MRShortProgramInfo se : ps.shortEvent) {
                            if ((ps.startTime == null) || ps.startTime.isEmpty()) {
                                logger.debug("{}: EVENT_EIT_CHANGE: empty program info", thingId);
                                continue;
                            }
                            // Convert UTC to local time
                            // 2018/11/04 21:45:00 -> "2018-11-04T10:15:30.00Z"
                            String tsLocal = ps.startTime.replace('/', '-').replace(" ", "T") + "Z";
                            Instant timestamp = Instant.parse(tsLocal);
                            ZonedDateTime localTime = timestamp.atZone(ZoneId.of("Europe/Berlin"));
                            String ts = localTime.toString();
                            tsLocal = StringUtils.substringBeforeLast(localTime.toString(), "[");
                            tsLocal = StringUtils.substringBefore(tsLocal.replace('-', '/').replace('T', ' '), "+");

                            logger.debug("{}: Info for channel {} / {} - {} {}.{}, start time={}, duration={}", thingId,
                                    pinfo.channelNum, pinfo.channelCode, control.getRunStatus(ps.runningStatus),
                                    se.eventName, se.textChar, tsLocal, ps.duration);
                            if (ps.runningStatus != EV_EITCHG_RUNNING_NOT_RUNNING) {
                                updateState(CHANNEL_PROG_TITLE, new StringType(se.eventName));
                                updateState(CHANNEL_PROG_TEXT, new StringType(se.textChar));
                                updateState(CHANNEL_PROG_START, new DateTimeType(localTime));

                                try {
                                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    Date date = dateFormat.parse(ps.duration);
                                    long minutes = date.getTime() / 1000L / 60l;
                                    updateState(CHANNEL_PROG_DURATION, toQuantityType(minutes, SmartHomeUnits.MINUTE));
                                } catch (ParseException e) {
                                    logger.debug("{}: Unable to parse programDuration: {}", thingId, ps.duration);
                                }

                                if (i++ == 0) {
                                    flUpdatePower = true;
                                }
                            }
                        }
                    }
                }
            }
        } else if (jsonEvent.contains("new_play_mode")) {
            MRPayEvent event = gson.fromJson(jsonEvent, MRPayEvent.class);
            if (event.duration == null) {
                event.duration = -1;
            }
            if (event.playPostion == null) {
                event.playPostion = -1;
            }
            logger.debug("{}: STB event playContent: playMode={}, duration={}, playPosition={}", thingId,
                    control.getPlayStatus(event.newPlayMode), event.duration, event.playPostion);

            // If we get a playConfig event there MR must be online. However it also sends a
            // plyMode stop before powering off the device, so we filter this.
            if ((event.newPlayMode != EV_PLAYCHG_STOP) && this.isInitialized()) {
                flUpdatePower = true;
            }
            if (event.newPlayMode != -1) {
                String playMode = control.getPlayStatus(event.newPlayMode);
                updateState(CHANNEL_PLAY_MODE, new StringType(playMode));
                mapPlayModeToMediaControl(playMode);
            }
            if (event.duration > 0) {
                updateState(CHANNEL_PROG_DURATION, new StringType(event.duration.toString()));
            }
            if (event.playPostion != -1) {
                updateState(CHANNEL_PROG_POS, toQuantityType(event.playPostion / 6, SmartHomeUnits.MINUTE));
            }
        } else {
            logger.debug("{}: Unknown MR event, JSON={}", thingId, jsonEvent);
        }
        if (flUpdatePower) {
            // We received a non-stopped event -> MR must be on
            updateState(CHANNEL_POWER, OnOffType.ON);
        }
    }

    private void mapPlayModeToMediaControl(String playMode) {
        switch (playMode) {
            case "playing":
            case "playing (MC)":
            case "playing (UC)":
            case "buffering":
                logger.debug("{}: Setting Player state to PLAY", thingId);
                updateState(CHANNEL_PLAYER, PlayPauseType.PLAY);
            case "paused":
            case "stopped":
                logger.debug("{}: Setting Player state to PAUSE", thingId);
                updateState(CHANNEL_PLAYER, PlayPauseType.PAUSE);
                break;
        }
    }

    /**
     * When the MR powers off it send a UPnP message, which is catched by the binding.
     */
    @Override
    public void onPowerOff() throws MagentaTVException {
        logger.debug("{}: Power-Off received for device {}", thingId, deviceName());
        // MR was powered off -> update pwoer status, reset items
        resetEventChannels();
    }

    private void resetEventChannels() {
        updateState(CHANNEL_POWER, OnOffType.OFF);
        updateState(CHANNEL_PROG_TITLE, StringType.EMPTY);
        updateState(CHANNEL_PROG_TEXT, StringType.EMPTY);
        updateState(CHANNEL_PROG_START, StringType.EMPTY);
        updateState(CHANNEL_PROG_DURATION, ZERO);
        updateState(CHANNEL_PROG_POS, ZERO);
        updateState(CHANNEL_CHANNEL, ZERO);
        updateState(CHANNEL_CHANNEL_CODE, ZERO);
    }

    private String fixEventJson(String jsonEvent) {
        // MR401: channel_num is a string -> ok
        // MR201: channel_num is an int -> fix JSON formatting to String
        if (jsonEvent.contains(MR_EVENT_CHAN_TAG) && !jsonEvent.contains(MR_EVENT_CHAN_TAG + "\"")) {
            // hack: reformat the JSON string to make it compatible with the GSON parsing
            logger.trace("{}: malformed JSON->fix channel_num", thingId);
            String start = StringUtils.substringBefore(jsonEvent, MR_EVENT_CHAN_TAG); // up to "channel_num":
            String end = StringUtils.substringAfter(jsonEvent, MR_EVENT_CHAN_TAG); // behind "channel_num":
            String chan = StringUtils.substringBetween(jsonEvent, MR_EVENT_CHAN_TAG, ",");
            chan = StringUtils.trim(chan);
            return start + "\"channel_num\":" + "\"" + chan + "\"" + end;
        }
        return jsonEvent;
    }

    private boolean isOnline() {
        return this.getThing().getStatus() == ThingStatus.ONLINE;
    }

    /**
     * Check device status, if pairing
     * events (if callbackUrl is configured)
     */
    private void renewEventSubscription() {
        if (!control.isInitialized()) {
            return;
        }
        logger.debug("{}: Check receiver status, current state  {}/{}", thingId,
                this.getThing().getStatusInfo().getStatus().toString(),
                this.getThing().getStatusInfo().getStatusDetail());

        try {
            // when pairing is completed re-new event channel subscription
            if ((this.getThing().getStatus() != ThingStatus.OFFLINE) && !thingConfig.getVerificationCode().isEmpty()) {
                logger.debug("{}: Renew MR event subscription for device {}", thingId, deviceName());
                control.subscribeEventChannel();
            }
        } catch (MagentaTVException e) {
            logger.warn("{}: Re-new event subscription failed: {}", deviceName(), e.toString());
        }

        // another try: if the above SUBSCRIBE fails, try a re-connect immediatly
        try {
            if ((this.getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.COMMUNICATION_ERROR)
                    && !thingConfig.getUserID().isEmpty()) {
                // if we have no userID the OAuth is not completed or pairing process got stuck
                logger.debug("{}: Reconnect media receiver", deviceName());
                connectReceiver(); // throws MagentaTVException on error
            }
        } catch (MagentaTVException | RuntimeException e) {
            logger.debug("{}: Re-connect to receiver failed: {}", deviceName(), e.toString());
        }
    }

    public void updateThingProperties(Map<String, String> properties) {
        // copy all attributes except thos, which begin with $
        Map<String, String> map = new HashMap<String, String>();
        for (String key : properties.keySet()) {
            if ((key.charAt(0) != '$') && !key.contains("component.")) {
                String prop = properties.get(key);
                map.put(key, prop != null ? prop : "");
            }
        }
        this.updateProperties(map);
    }

    public static State toQuantityType(@Nullable Number value, Unit<?> unit) {
        return value == null ? UnDefType.NULL : new QuantityType<>(value, unit);
    }

    private String deviceName() {
        return thingConfig.getFriendlyName() + "(" + thingConfig.getTerminalID() + ")";
    }

    private void cancelJob(@Nullable ScheduledFuture<?> job) {
        if ((job != null) && !job.isCancelled()) {
            job.cancel(true);
        }
    }

    protected void cancelInitialize() {
        cancelJob(initializeJob);
    }

    protected void cancelPairingCheck() {
        cancelJob(pairingWatchdogJob);
    }

    protected void cancelRenewEvent() {
        cancelJob(renewEventJob);
    }

    private void cancelAllJobs() {
        cancelInitialize();
        cancelPairingCheck();
        cancelRenewEvent();
    }

    @Override
    public void dispose() {
        cancelAllJobs();
        handlerFactory.removeDevice(thingConfig.getTerminalID());
        super.dispose();
    }
}
