/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.unifi.internal.handler;

import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_AP;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_BLOCKED;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_ESSID;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_GUEST;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_IP_ADDRESS;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_LAST_SEEN;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_MAC_ADDRESS;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_ONLINE;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_RECONNECT;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_RSSI;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_SITE;
import static org.openhab.binding.unifi.internal.UniFiBindingConstants.CHANNEL_UPTIME;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.unifi.internal.UniFiClientThingConfig;
import org.openhab.binding.unifi.internal.api.UniFiController;
import org.openhab.binding.unifi.internal.api.UniFiException;
import org.openhab.binding.unifi.internal.api.model.UniFiClient;
import org.openhab.binding.unifi.internal.api.model.UniFiDevice;
import org.openhab.binding.unifi.internal.api.model.UniFiSite;
import org.openhab.binding.unifi.internal.api.model.UniFiWiredClient;
import org.openhab.binding.unifi.internal.api.model.UniFiWirelessClient;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UniFiClientThingHandler} is responsible for handling commands and status
 * updates for UniFi Wireless Devices.
 *
 * @author Matthew Bowman - Initial contribution
 * @author Patrik Wimnell - Blocking / Unblocking client support
 */
@NonNullByDefault
public class UniFiClientThingHandler extends UniFiBaseThingHandler<UniFiClient, UniFiClientThingConfig> {

    private final Logger logger = LoggerFactory.getLogger(UniFiClientThingHandler.class);

    private UniFiClientThingConfig config = new UniFiClientThingConfig();

    public UniFiClientThingHandler(final Thing thing) {
        super(thing);
    }

    @Override
    protected boolean initialize(final UniFiClientThingConfig config) {
        // mgb: called when the config changes
        logger.debug("Initializing the UniFi Client Handler with config = {}", config);
        if (!config.isValid()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "@text/error.thing.client.offline.configuration_error");
            return false;
        }
        this.config = config;
        return true;
    }

    private static boolean belongsToSite(final UniFiClient client, final String siteName) {
        boolean result = true; // mgb: assume true = proof by contradiction
        if (!siteName.isEmpty()) {
            final UniFiSite site = client.getSite();
            // mgb: if the 'site' can't be found or the name doesn't match...
            if (site == null || !site.matchesName(siteName)) {
                // mgb: ... then the client doesn't belong to this thing's configured 'site' and we 'filter' it
                result = false;
            }
        }
        return result;
    }

    @Override
    protected synchronized @Nullable UniFiClient getEntity(final UniFiController controller) {
        final UniFiClient client = controller.getClient(config.getClientID());
        // mgb: short circuit
        if (client == null || !belongsToSite(client, config.getSite())) {
            return null;
        }
        return client;
    }

    private State getDefaultState(final String channelID) {
        final State state;
        switch (channelID) {
            case CHANNEL_SITE:
            case CHANNEL_AP:
            case CHANNEL_ESSID:
            case CHANNEL_RSSI:
            case CHANNEL_MAC_ADDRESS:
            case CHANNEL_IP_ADDRESS:
            case CHANNEL_BLOCKED:
                state = UnDefType.UNDEF;
                break;
            case CHANNEL_UPTIME:
                // mgb: uptime should default to 0 seconds
                state = DecimalType.ZERO;
                break;
            case CHANNEL_LAST_SEEN:
                // mgb: lastSeen should keep the last state no matter what
                state = UnDefType.NULL;
                break;
            case CHANNEL_RECONNECT:
                state = OnOffType.OFF;
                break;
            default:
                state = UnDefType.NULL;
                break;
        }
        return state;
    }

    private synchronized boolean isClientHome(final UniFiClient client) {
        final boolean online;

        final ZonedDateTime lastSeen = client.getLastSeen();
        if (lastSeen == null) {
            online = false;
            logger.warn("Could not determine if client is online: cid = {}, lastSeen = null", config.getClientID());
        } else {
            final Instant considerHomeExpiry = lastSeen.toInstant().plusSeconds(config.getConsiderHome());
            online = Instant.now().isBefore(considerHomeExpiry);
        }
        return online;
    }

    @Override
    protected void refreshChannel(final UniFiClient client, final ChannelUID channelUID) {
        final boolean clientHome = isClientHome(client);
        final UniFiDevice device = client.getDevice();
        final UniFiSite site = (device == null ? null : device.getSite());
        final String channelID = channelUID.getIdWithoutGroup();
        State state = getDefaultState(channelID);
        switch (channelID) {
            // mgb: common wired + wireless client channels

            // :online
            case CHANNEL_ONLINE:
                state = OnOffType.from(clientHome);
                break;

            // :site
            case CHANNEL_SITE:
                if (site != null && site.getDescription() != null && !site.getDescription().isBlank()) {
                    state = StringType.valueOf(site.getDescription());
                }
                break;

            // :macAddress
            case CHANNEL_MAC_ADDRESS:
                if (client.getMac() != null && !client.getMac().isBlank()) {
                    state = StringType.valueOf(client.getMac());
                }
                break;

            // :ipAddress
            case CHANNEL_IP_ADDRESS:
                if (client.getIp() != null && !client.getIp().isBlank()) {
                    state = StringType.valueOf(client.getIp());
                }
                break;

            // :uptime
            case CHANNEL_UPTIME:
                if (client.getUptime() != null) {
                    state = new DecimalType(client.getUptime());
                }
                break;

            // :lastSeen
            case CHANNEL_LAST_SEEN:
                // mgb: we don't check clientOnline as lastSeen is also included in the Insights data
                if (client.getLastSeen() != null) {
                    state = new DateTimeType(
                            ZonedDateTime.ofInstant(client.getLastSeen().toInstant(), ZoneId.systemDefault()));
                }
                break;

            // :blocked
            case CHANNEL_BLOCKED:
                state = OnOffType.from(client.isBlocked());
                break;

            // :guest
            case CHANNEL_GUEST:
                state = OnOffType.from(client.isGuest());
                break;

            default:
                // mgb: additional wired client channels
                if (client.isWired() && (client instanceof UniFiWiredClient)) {
                    state = getWiredChannelState((UniFiWiredClient) client, channelID, state);
                }

                // mgb: additional wireless client channels
                else if (client.isWireless() && (client instanceof UniFiWirelessClient)) {
                    state = getWirelessChannelState((UniFiWirelessClient) client, channelID, state);
                }
                break;
        }
        // mgb: only non null states get updates
        if (state != UnDefType.NULL) {
            updateState(channelID, state);
        }
    }

    private State getWiredChannelState(final UniFiWiredClient client, final String channelID,
            final State defaultState) {
        return defaultState;
    }

    private State getWirelessChannelState(final UniFiWirelessClient client, final String channelID,
            final State defaultState) {
        State state = defaultState;
        switch (channelID) {
            // :ap
            case CHANNEL_AP:
                final UniFiDevice device = client.getDevice();
                if (device != null && device.getName() != null && !device.getName().isBlank()) {
                    state = StringType.valueOf(device.getName());
                }
                break;

            // :essid
            case CHANNEL_ESSID:
                if (client.getEssid() != null && !client.getEssid().isBlank()) {
                    state = StringType.valueOf(client.getEssid());
                }
                break;

            // :rssi
            case CHANNEL_RSSI:
                if (client.getRssi() != null) {
                    state = new DecimalType(client.getRssi());
                }
                break;

            // :reconnect
            case CHANNEL_RECONNECT:
                // nop - trigger channel so it's always OFF by default
                state = OnOffType.OFF;
                break;
        }
        return state;
    }

    @Override
    protected boolean handleCommand(final UniFiController controller, final UniFiClient client,
            final ChannelUID channelUID, final Command command) throws UniFiException {
        final String channelID = channelUID.getIdWithoutGroup();
        switch (channelID) {
            case CHANNEL_BLOCKED:
                return handleBlockedCommand(controller, client, channelUID, command);
            case CHANNEL_RECONNECT:
                return handleReconnectCommand(controller, client, channelUID, command);
            default:
                return false;
        }
    }

    private boolean handleBlockedCommand(final UniFiController controller, final UniFiClient client,
            final ChannelUID channelUID, final Command command) throws UniFiException {
        if (command instanceof OnOffType) {
            controller.block(client, command == OnOffType.ON);
            refresh();
            return true;
        }
        return false;
    }

    private boolean handleReconnectCommand(final UniFiController controller, final UniFiClient client,
            final ChannelUID channelUID, final Command command) throws UniFiException {
        if (command instanceof OnOffType && command == OnOffType.ON) {
            controller.reconnect(client);
            updateState(channelUID, OnOffType.OFF);
            refresh();
            return true;
        }
        return false;
    }
}
