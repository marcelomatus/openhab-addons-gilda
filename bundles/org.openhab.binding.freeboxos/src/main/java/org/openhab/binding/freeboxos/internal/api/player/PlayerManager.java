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
package org.openhab.binding.freeboxos.internal.api.player;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.login.Session.Permission;
import org.openhab.binding.freeboxos.internal.api.player.Player.PlayerResponse;
import org.openhab.binding.freeboxos.internal.api.player.Player.PlayersResponse;
import org.openhab.binding.freeboxos.internal.api.player.PlayerStatus.PlayerStatusResponse;
import org.openhab.binding.freeboxos.internal.api.rest.FreeboxOsSession;
import org.openhab.binding.freeboxos.internal.api.rest.ListableRest;
import org.openhab.binding.freeboxos.internal.api.system.DeviceConfig;
import org.openhab.binding.freeboxos.internal.api.system.DeviceConfig.DeviceConfigurationResponse;

/**
 * The {@link PlayerManager} is the Java class used to handle api requests
 * related to player
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class PlayerManager extends ListableRest<Player, PlayerResponse, PlayersResponse> {
    private static final String STATUS_SUB_PATH = "status";
    private static final String SYSTEM_SUB_PATH = "system";
    private static final String PLAYER_SUB_PATH = "player";
    private static final String REBOOT_ACTION = SYSTEM_SUB_PATH + "/" + REBOOT_SUB_PATH;

    private final Map<Integer, String> subPaths = new HashMap<>();

    public PlayerManager(FreeboxOsSession session) throws FreeboxException {
        super(PLAYER_SUB_PATH, session, Permission.PLAYER, PlayerResponse.class, PlayersResponse.class);
        getDevices().forEach(player -> {
            subPaths.put(player.getId(), player.baseUrl());
        });
    }

    public PlayerStatus getPlayerStatus(int id) throws FreeboxException {
        return get(PlayerStatusResponse.class, subPaths.get(id) + STATUS_SUB_PATH);
    }

    public DeviceConfig getConfig(int id) throws FreeboxException {
        return get(DeviceConfigurationResponse.class, subPaths.get(id) + SYSTEM_SUB_PATH);
    }

    public void reboot(int id) throws FreeboxException {
        post(subPaths.get(id) + REBOOT_ACTION);
    }
}
