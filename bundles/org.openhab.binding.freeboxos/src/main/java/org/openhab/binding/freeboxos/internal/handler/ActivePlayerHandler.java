/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.action.ActivePlayerActions;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager.Configuration;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager.ForegroundApp;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager.Player;
import org.openhab.binding.freeboxos.internal.api.rest.PlayerManager.Status;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ActivePlayerHandler} is responsible for handling everything associated to Freebox Player
 * with api capabilities.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class ActivePlayerHandler extends PlayerHandler implements FreeDeviceIntf {
    private final Logger logger = LoggerFactory.getLogger(ActivePlayerHandler.class);

    private final ChannelUID eventChannelUID;

    private long uptime = -1;

    public ActivePlayerHandler(Thing thing) {
        super(thing);
        eventChannelUID = new ChannelUID(getThing().getUID(), SYS_INFO, BOX_EVENT);
    }

    @Override
    void initializeProperties(Map<String, String> properties) throws FreeboxException {
        super.initializeProperties(properties);
        Player player = getManager(PlayerManager.class).getDevice(getClientId());
        if (player.reachable()) {
            Configuration config = getManager(PlayerManager.class).getConfig(player.id());
            if (config != null) {
                properties.put(Thing.PROPERTY_SERIAL_NUMBER, config.serial());
                properties.put(Thing.PROPERTY_FIRMWARE_VERSION, config.firmwareVersion());
            }
        }
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        super.internalPoll();
        if (thing.getStatus().equals(ThingStatus.ONLINE)) {
            Player player = getManager(PlayerManager.class).getDevice(getClientId());
            updateStatus(player.reachable() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
            if (player.reachable()) {
                Status status = getManager(PlayerManager.class).getPlayerStatus(getClientId());
                if (status != null) {
                    updateChannelString(PLAYER_STATUS, PLAYER_STATUS, status.powerState().name());
                    ForegroundApp foreground = status.foregroundApp();
                    if (foreground != null) {
                        updateChannelString(PLAYER_STATUS, PACKAGE, foreground._package());
                    }
                }
                Configuration config = getManager(PlayerManager.class).getConfig(getClientId());
                if (config != null) {
                    uptime = checkUptimeAndFirmware(config.uptimeVal(), uptime, config.firmwareVersion());
                } else {
                    uptime = 0;
                }
            }
            updateChannelQuantity(SYS_INFO, UPTIME, uptime, Units.SECOND);
        }
    }

    public void reboot() {
        processReboot(() -> {
            try {
                if (!getManager(PlayerManager.class).reboot(getClientId())) {
                    logger.warn("Unable to reboot the player - probably not reachable");
                }
            } catch (FreeboxException e) {
                logger.warn("Error rebooting: {}", e.getMessage());
            }
        });
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(ActivePlayerActions.class);
    }

    @Override
    public ChannelUID getEventChannelUID() {
        return eventChannelUID;
    }

    @Override
    public void triggerChannel(ChannelUID channelUID, String event) {
        super.triggerChannel(channelUID, event);
    }
}
