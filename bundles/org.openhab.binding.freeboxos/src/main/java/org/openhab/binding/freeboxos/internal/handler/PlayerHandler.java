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

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.KEY_CODE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.action.PlayerActions;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.airmedia.AirMediaManager;
import org.openhab.binding.freeboxos.internal.api.lan.LanManager;
import org.openhab.binding.freeboxos.internal.api.lan.NameSource;
import org.openhab.binding.freeboxos.internal.api.player.Player;
import org.openhab.binding.freeboxos.internal.api.player.PlayerManager;
import org.openhab.binding.freeboxos.internal.api.system.DeviceConfig;
import org.openhab.binding.freeboxos.internal.config.PlayerConfiguration;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.library.types.StringType;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
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
public class PlayerHandler extends FreeDeviceHandler {
    private static final List<String> VALID_REMOTE_KEYS = Arrays.asList("red", "green", "blue", "yellow", "power",
            "list", "tv", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "vol_inc", "vol_dec", "mute", "prgm_inc",
            "prgm_dec", "prev", "bwd", "play", "rec", "fwd", "next", "up", "right", "down", "left", "back", "swap",
            "info", "epg", "mail", "media", "help", "options", "pip", "ok", "home");

    private final Logger logger = LoggerFactory.getLogger(PlayerHandler.class);
    private final ServiceRegistration<AudioSink> reg;

    private final AirMediaSink audioSink;

    @SuppressWarnings("unchecked")
    public PlayerHandler(Thing thing, AudioHTTPServer audioHTTPServer, NetworkAddressService networkAddressService,
            BundleContext bundleContext) {
        super(thing);
        this.audioSink = new AirMediaSink(thing, audioHTTPServer, networkAddressService, bundleContext);
        reg = (ServiceRegistration<AudioSink>) bundleContext.registerService(AudioSink.class.getName(), audioSink,
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
                getManager(LanManager.class).getHost(getMac()).ifPresent(
                        host -> properties.put(NameSource.UPNP.name(), host.getPrimaryName().orElse("Freebox Player")));
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        PlayerConfiguration config = getConfigAs(PlayerConfiguration.class);
        try {
            audioSink.initialize(config.callBackUrl, editProperties().get(NameSource.UPNP.name()),
                    getManager(AirMediaManager.class));
        } catch (FreeboxException e) {
            logger.warn("Error.");
        }
    }

    // private String getPassword() {
    // return (String) getConfig().get(PlayerConfiguration.PASSWORD);
    // }

    @Override
    public void dispose() {
        reg.unregister();
        super.dispose();
    }

    @Override
    protected boolean internalHandleCommand(ChannelUID channelUID, Command command) throws FreeboxException {
        if (KEY_CODE.equals(channelUID.getIdWithoutGroup()) && command instanceof StringType) {
            sendKey(command.toString(), false, 1);
            return true;
        }

        return super.internalHandleCommand(channelUID, command);
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
