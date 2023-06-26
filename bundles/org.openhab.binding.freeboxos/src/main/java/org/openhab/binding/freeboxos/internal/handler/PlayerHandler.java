/**
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.action.PlayerActions;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.rest.LanBrowserManager.LanHost;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager.Player;
import org.openhab.binding.freeboxos.internal.config.PlayerConfiguration;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import inet.ipaddr.IPAddress;

/**
 * The {@link PlayerHandler} is responsible for handling everything associated to any Freebox Player thing type.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class PlayerHandler extends HostHandler {
    private static final List<String> VALID_REMOTE_KEYS = Arrays.asList("red", "green", "blue", "yellow", "power",
            "list", "tv", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "vol_inc", "vol_dec", "mute", "prgm_inc",
            "prgm_dec", "prev", "bwd", "play", "rec", "fwd", "next", "up", "right", "down", "left", "back", "swap",
            "info", "epg", "mail", "media", "help", "options", "pip", "ok", "home");

    private final Logger logger = LoggerFactory.getLogger(PlayerHandler.class);
    private @Nullable IPAddress ipAddress;

    public PlayerHandler(Thing thing) {
        super(thing);
    }

    @Override
    void initializeProperties(Map<String, String> properties) throws FreeboxException {
        super.initializeProperties(properties);
        Player player = getManager(PlayerManager.class).getDevice(getClientId());
        properties.put(Thing.PROPERTY_MODEL_ID, player.deviceModel().name());
    }

    @Override
    protected boolean internalHandleCommand(String channelId, Command command) throws FreeboxException {
        if (KEY_CODE.equals(channelId) && command instanceof StringType) {
            sendKey(command.toString(), false, 1);
            return true;
        }

        return super.internalHandleCommand(channelId, command);
    }

    @Override
    public void updateConnectivityChannels(LanHost host) {
        super.updateConnectivityChannels(host);
        ipAddress = host.getIpv4();
    }

    public void sendKey(String key, boolean longPress, int count) {
        String aKey = key.toLowerCase();
        IPAddress ip = ipAddress;
        if (ip == null) {
            logger.info("Player IP is unknown");
        } else if (VALID_REMOTE_KEYS.contains(aKey)) {
            String remoteCode = (String) getConfig().get(PlayerConfiguration.REMOTE_CODE);
            if (remoteCode != null) {
                try {
                    getManager(PlayerManager.class).sendKey(ip.toCanonicalString(), remoteCode, aKey, longPress, count);
                } catch (FreeboxException e) {
                    logger.info("Error sending key : {}", e.getMessage());
                }
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
=======
 * Copyright (c) 2010-2022 Contributors to the openHAB project
=======
 * Copyright (c) 2010-2023 Contributors to the openHAB project
>>>>>>> 006a813 Saving work before instroduction of ArrayListDeserializer
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.action.PlayerActions;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.lan.browser.LanHost;
import org.openhab.binding.freeboxos.internal.api.player.Player;
import org.openhab.binding.freeboxos.internal.api.player.PlayerManager;
import org.openhab.binding.freeboxos.internal.api.system.SystemConfig;
import org.openhab.binding.freeboxos.internal.config.PlayerConfiguration;
import org.openhab.core.audio.AudioHTTPServer;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.osgi.framework.BundleContext;
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
public class PlayerHandler extends FreeDeviceHandler2Del implements FreeClientIntf {
    private static final List<String> VALID_REMOTE_KEYS = Arrays.asList("red", "green", "blue", "yellow", "power",
            "list", "tv", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "vol_inc", "vol_dec", "mute", "prgm_inc",
            "prgm_dec", "prev", "bwd", "play", "rec", "fwd", "next", "up", "right", "down", "left", "back", "swap",
            "info", "epg", "mail", "media", "help", "options", "pip", "ok", "home");

    private final Logger logger = LoggerFactory.getLogger(PlayerHandler.class);
    private @Nullable String ipAddress;

    public PlayerHandler(Thing thing, AudioHTTPServer audioHTTPServer, String ipAddress, BundleContext bundleContext) {
        super(thing, audioHTTPServer, ipAddress, bundleContext);
    }

    @Override
    void initializeProperties(Map<String, String> properties) throws FreeboxException {
        super.initializeProperties(properties);
        Player player = getManager(PlayerManager.class).getDevice(getClientId());
        properties.put(Thing.PROPERTY_MODEL_ID, player.getDeviceModel().name());
    }

    // private String getPassword() {
    // return (String) getConfig().get(PlayerConfiguration.PASSWORD);
    // }

    @Override
    protected boolean internalHandleCommand(String channelId, Command command) throws FreeboxException {
        if (KEY_CODE.equals(channelId) && command instanceof StringType) {
            sendKey(command.toString(), false, 1);
            return true;
        }

        return super.internalHandleCommand(channelId, command);
    }

    public void sendKey(String key, boolean longPress, int count) {
        String aKey = key.toLowerCase();
        String ip = getIpAddress();
        if (ip == null) {
            logger.info("Player IP is unknown");
        } else if (VALID_REMOTE_KEYS.contains(aKey)) {
            String remoteCode = (String) getConfig().get(PlayerConfiguration.REMOTE_CODE);
            if (remoteCode != null) {
                try {
                    getManager(PlayerManager.class).sendKey(ip, remoteCode, aKey, longPress, count);
                } catch (FreeboxException e) {
                    logger.info("Error sending key : {}", e.getMessage());
                }
            } else {
                logger.warn("A remote code must be configured in the on the player thing.");
            }
        } else {
            logger.info("Key '{}' is not a valid key expression", key);
        }
    }

    @Override
    public void updateConnectivityChannels(LanHost host) {
        super.updateConnectivityChannels(host);
        ipAddress = host.getIpv4();
    }

    public void sendMultipleKeys(String keys) {
        String[] keyChain = keys.split(",");
        Arrays.stream(keyChain).forEach(key -> {
            sendKey(key, false, 1);
        });
    }

    public @Nullable String getIpAddress() {
        return ipAddress;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(PlayerActions.class);
    }

    @Override
    protected Optional<SystemConfig> getDeviceConfig() throws FreeboxException {
        return Optional.empty();
    }

    @Override
    protected void internalCallReboot() throws FreeboxException {
>>>>>>> 46dadb1 SAT warnings handling
    }
}
