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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.action.HostActions;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.lan.ConnectivityData;
import org.openhab.binding.freeboxos.internal.api.lan.LanHost;
import org.openhab.binding.freeboxos.internal.api.lan.LanManager;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HostHandler} is responsible for handling everything associated to
 * any Freebox thing types except the bridge thing type.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class HostHandler extends ApiConsumerHandler {
    private final Logger logger = LoggerFactory.getLogger(HostHandler.class);
    private @Nullable String ipAddress;

    public HostHandler(Thing thing) {
        super(thing);
    }

    @Override
    void internalGetProperties(Map<String, String> properties) throws FreeboxException {
        LanHost lanHost = getManager(LanManager.class).getHostsMap().get(getMac());
        if (lanHost != null) {
            properties.put(Thing.PROPERTY_VENDOR, lanHost.getVendorName().orElse("Unknown"));
        }
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        ConnectivityData lanHost = fetchConnectivity();
        ipAddress = lanHost.getIpv4();
        updateChannelOnOff(CONNECTIVITY, REACHABLE, lanHost.isReachable());
        updateChannelDateTimeState(CONNECTIVITY, LAST_SEEN, lanHost.getLastSeen());
        updateChannelString(CONNECTIVITY, IP_ADDRESS, ipAddress);
        updateStatus(lanHost.isReachable() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    protected ConnectivityData fetchConnectivity() throws FreeboxException {
        LanHost lanHost = getManager(LanManager.class).getHostsMap().get(getMac());
        if (lanHost != null) {
            return lanHost;
        }
        throw new FreeboxException("Host data not found");
    }

    public @Nullable String getIpAddress() {
        return ipAddress;
    }

    public void wol() {
        try {
            getManager(LanManager.class).wakeOnLan(getMac());
        } catch (FreeboxException e) {
            logger.warn("Error waking up host : {}", e.getMessage());
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(HostActions.class);
    }

    protected String getMac() {
        return ((String) getConfig().get(Thing.PROPERTY_MAC_ADDRESS)).toLowerCase();
    }
}
