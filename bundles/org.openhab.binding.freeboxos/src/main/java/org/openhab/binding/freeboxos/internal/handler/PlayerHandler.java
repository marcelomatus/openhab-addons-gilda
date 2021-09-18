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
package org.openhab.binding.freeboxos.internal.handler;

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.*;
import static org.openhab.core.audio.AudioFormat.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.action.PlayerActions;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.airmedia.AirMediaActionData.MediaAction;
import org.openhab.binding.freeboxos.internal.api.airmedia.AirMediaActionData.MediaType;
import org.openhab.binding.freeboxos.internal.api.airmedia.AirMediaConfig;
import org.openhab.binding.freeboxos.internal.api.airmedia.AirMediaManager;
import org.openhab.binding.freeboxos.internal.api.lan.LanConfig.NetworkMode;
import org.openhab.binding.freeboxos.internal.api.lan.LanHost;
import org.openhab.binding.freeboxos.internal.api.lan.LanManager;
import org.openhab.binding.freeboxos.internal.api.lan.NameSource;
import org.openhab.binding.freeboxos.internal.api.player.Player;
import org.openhab.binding.freeboxos.internal.api.player.PlayerManager;
import org.openhab.binding.freeboxos.internal.api.system.DeviceConfig;
import org.openhab.binding.freeboxos.internal.config.PlayerConfiguration;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.audio.URLAudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.net.HttpServiceUtil;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PlayerHandler} is responsible for handling everything associated to
 * any Freebox Player thing type.
 *
 * @author Gaël L'hopital - Initial contribution
 *         https://github.com/betonniere/freeteuse/
 *         https://github.com/MaximeCheramy/remotefreebox/blob/16e2a42ed7cfcfd1ab303184280564eeace77919/remotefreebox/fbx_descriptor.py
 *         https://dev.freebox.fr/sdk/freebox_player_1.1.4_codes.html
 *         http://192.168.0.98/pub/remote_control?code=78952520&key=1&long=true
 */
@NonNullByDefault
public class PlayerHandler extends FreeDeviceHandler implements AudioSink {
    private static final Set<Class<? extends AudioStream>> SUPPORTED_STREAMS = Collections.singleton(AudioStream.class);
    private static final Set<AudioFormat> BASIC_FORMATS = Set.of(WAV, OGG);
    private static final Set<AudioFormat> ALL_MP3_FORMATS = Set.of(
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 96000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 112000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 128000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 160000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 192000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 224000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 256000, null),
            new AudioFormat(CONTAINER_NONE, CODEC_MP3, null, null, 320000, null));
    private static final List<String> VALID_REMOTE_KEYS = Arrays.asList("red", "green", "blue", "yellow", "power",
            "list", "tv", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "vol_inc", "vol_dec", "mute", "prgm_inc",
            "prgm_dec", "prev", "bwd", "play", "rec", "fwd", "next", "up", "right", "down", "left", "back", "swap",
            "info", "epg", "mail", "media", "help", "options", "pip", "ok", "home");

    private final Logger logger = LoggerFactory.getLogger(PlayerHandler.class);
    private final AudioHTTPServer audioHTTPServer;
    private @Nullable String callbackUrl;
    private final Set<AudioFormat> SUPPORTED_FORMATS = new HashSet<>();
    private final ServiceRegistration<AudioSink> reg;
    private final NetworkAddressService networkAddressService;
    private final BundleContext bundleContext;

    @SuppressWarnings("unchecked")
    public PlayerHandler(Thing thing, AudioHTTPServer audioHTTPServer, NetworkAddressService networkAddressService,
            BundleContext bundleContext) {
        super(thing);
        this.audioHTTPServer = audioHTTPServer;
        this.networkAddressService = networkAddressService;
        this.bundleContext = bundleContext;
        reg = (ServiceRegistration<AudioSink>) bundleContext.registerService(AudioSink.class.getName(), this,
                new Hashtable<>());
    }

    @Override
    void internalGetProperties(Map<String, String> properties) throws FreeboxException {
        super.internalGetProperties(properties);
        for (Player player : getManager(PlayerManager.class).getPlayers()) {
            if (player.getMac().equals(getMac())) {
                properties.put(Thing.PROPERTY_MODEL_ID, player.getModel());
                if (player.isApiAvailable() && player.isReachable()) {
                    DeviceConfig config = getManager(PlayerManager.class).getConfig(player.getId());
                    properties.put(Thing.PROPERTY_SERIAL_NUMBER, config.getSerial());
                    properties.put(Thing.PROPERTY_FIRMWARE_VERSION, config.getFirmwareVersion());
                }
                LanHost lanhost = getManager(LanManager.class).getHostsMap().get(getMac());
                if (lanhost != null) {
                    properties.put(NameSource.UPNP.name(), lanhost.getPrimaryName().orElse("Freebox Player"));
                }
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        PlayerConfiguration config = getConfigAs(PlayerConfiguration.class);
        if (!config.callBackUrl.isEmpty()) {
            callbackUrl = config.callBackUrl;
        } else {
            String ipAddress = networkAddressService.getPrimaryIpv4HostAddress();
            int port = HttpServiceUtil.getHttpServicePort(bundleContext);
            if (port != -1 && ipAddress != null) {
                // we do not use SSL as it can cause certificate validation issues.
                callbackUrl = String.format("http://%s:%d", ipAddress, port);
            } else {
                logger.warn("No network interface could be found or cannot find port of the http service.");
            }
        }
    }

    private String getPassword() {
        return (String) getConfig().get(PlayerConfiguration.PASSWORD);
    }

    @Override
    public void dispose() {
        reg.unregister();
        super.dispose();
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        super.internalPoll();
        fetchAirMediaStatus();
    }

    private void fetchAirMediaStatus() throws FreeboxException {
        Boolean airMediaStatus = false;
        if (getManager(LanManager.class).getNetworkMode() != NetworkMode.BRIDGE) {
            AirMediaConfig response = getManager(AirMediaManager.class).getConfig();
            airMediaStatus = response.isEnabled();
        }
        updateChannelOnOff(PLAYER_ACTIONS, AIRMEDIA_STATUS, airMediaStatus);
    }

    @Override
    protected boolean internalHandleCommand(ChannelUID channelUID, Command command) throws FreeboxException {
        if (AIRMEDIA_STATUS.equals(channelUID.getIdWithoutGroup()) && (ON_OFF_CLASSES.contains(command.getClass()))) {
            boolean enable = TRUE_COMMANDS.contains(command);
            updateState(new ChannelUID(getThing().getUID(), PLAYER_ACTIONS, AIRMEDIA_STATUS),
                    OnOffType.from(enableAirMedia(enable)));
            return true;
        } else if (KEY_CODE.equals(channelUID.getIdWithoutGroup()) && command instanceof StringType) {
            sendKey(command.toString(), false, 1);
            return true;
        }

        return super.internalHandleCommand(channelUID, command);
    }

    @Override
    public void process(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        String playerName = editProperties().get(NameSource.UPNP.name());

        if (getThing().getStatus() == ThingStatus.ONLINE && playerName != null) {
            if (audioStream == null) {
                try {
                    getManager(AirMediaManager.class).sendToReceiver(playerName, getPassword(), MediaAction.STOP,
                            MediaType.VIDEO);
                } catch (FreeboxException e) {
                    logger.warn("Exception while stopping audio stream playback: {}", e.getMessage());
                }
            } else {
                String url = null;
                if (audioStream instanceof URLAudioStream) {
                    // it is an external URL, we can access it directly
                    URLAudioStream urlAudioStream = (URLAudioStream) audioStream;
                    url = urlAudioStream.getURL();
                } else {
                    // we serve it on our own HTTP server
                    String relativeUrl = "";
                    if (audioStream instanceof FixedLengthAudioStream) {
                        relativeUrl = audioHTTPServer.serve((FixedLengthAudioStream) audioStream, 20);
                    } else {
                        relativeUrl = audioHTTPServer.serve(audioStream);
                    }
                    url = callbackUrl + relativeUrl;
                }
                try {
                    audioStream.close();
                    try {
                        logger.debug("AirPlay audio sink: process url {}", url);
                        getManager(AirMediaManager.class).sendToReceiver(playerName, getPassword(), MediaAction.START,
                                MediaType.VIDEO, url);
                    } catch (FreeboxException e) {
                        logger.warn("Audio stream playback failed: {}", e.getMessage());
                    }
                } catch (IOException e) {
                    logger.warn("Exception while closing audioStream : {}", e.getMessage());
                }
            }
        }
    }

    private boolean enableAirMedia(boolean enable) throws FreeboxException {
        AirMediaConfig config = getManager(AirMediaManager.class).getConfig();
        config.setEnable(enable);
        config = getManager(AirMediaManager.class).setConfig(config);
        return config.isEnabled();
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        if (SUPPORTED_FORMATS.isEmpty()) {
            SUPPORTED_FORMATS.addAll(BASIC_FORMATS);
            if (getConfigAs(PlayerConfiguration.class).acceptAllMp3) {
                SUPPORTED_FORMATS.addAll(ALL_MP3_FORMATS);
            } else { // Only accept MP3 bitrates >= 96 kbps
                SUPPORTED_FORMATS.add(MP3);
            }
        }
        return SUPPORTED_FORMATS;
    }

    public void sendKey(String key, boolean longPress, int count) {
        String aKey = key.toLowerCase();
        String ip = getIpAddress();
        if (ip == null) {
            logger.info("Player IP is unknown");
        } else if (VALID_REMOTE_KEYS.contains(aKey)) {
            String remoteCode = (String) getConfig().get(PlayerConfiguration.REMOTE_CODE);
            if (remoteCode != null) {
                UriBuilder uriBuilder = UriBuilder.fromPath("pub").scheme("http").host(ip).path("remote_control");
                uriBuilder.queryParam("code", remoteCode).queryParam("key", aKey);
                if (longPress) {
                    uriBuilder.queryParam("long", true);
                }
                if (count > 1) {
                    uriBuilder.queryParam("repeat", count);
                }
                // try {
                // TODO : s Correct this
                // getApi().execute(uriBuilder.build(), HttpMethod.GET, null, null, false);
                // } catch (FreeboxException e) {
                // logger.warn("Error calling Player url : {}", e.getMessage());
                // }
            } else {
                logger.warn("A remote code must be configured in the on the player thing.");
            }
        } else {
            logger.info("Key '{}' is not a valid key expression", key);
        }
    }

    public void sendMultipleKeys(String keys) {
        String[] keyChain = keys.split(",");
        Arrays.stream(keyChain).forEach(key -> {
            sendKey(key, false, 1);
        });
    }

    @Override
    public String getId() {
        return getThing().getUID().toString();
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return getThing().getLabel();
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_STREAMS;
    }

    @Override
    public PercentType getVolume() throws IOException {
        logger.info("getVolume received but AirMedia does not have the capability - returning 100%.");
        return PercentType.HUNDRED;
    }

    @Override
    public void setVolume(PercentType volume) throws IOException {
        logger.info("setVolume received but AirMedia does not have the capability - ignoring it.");
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(PlayerActions.class);
    }

    @Override
    protected Optional<DeviceConfig> getDeviceConfig() throws FreeboxException {
        return Optional.empty();
    }

    @Override
    protected void internalCallReboot() throws FreeboxException {
    }
}
