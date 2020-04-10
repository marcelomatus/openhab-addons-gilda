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
package org.openhab.binding.dwdpollenflug.internal.handler;

import static org.openhab.binding.dwdpollenflug.internal.DWDPollenflugBindingConstants.*;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.dwdpollenflug.internal.config.DWDPollenflugRegionConfiguration;
import org.openhab.binding.dwdpollenflug.internal.dto.DWDPollenflug;
import org.openhab.binding.dwdpollenflug.internal.dto.DWDRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DWDPollenflugRegionHandler} is the handler for bridge thing
 *
 * @author Johannes DerOetzi Ott - Initial contribution
 */
@NonNullByDefault
public class DWDPollenflugRegionHandler extends BaseThingHandler implements DWDPollenflugRegionListener {

    private final Logger logger = LoggerFactory.getLogger(DWDPollenflugRegionHandler.class);

    private DWDPollenflugRegionConfiguration thingConfig = new DWDPollenflugRegionConfiguration();

    private @Nullable Date lastUpdate;

    public DWDPollenflugRegionHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing DWD Pollenflug region handler");
        thingConfig = getConfigAs(DWDPollenflugRegionConfiguration.class);

        if (thingConfig.isValid()) {
            DWDPollenflugBridgeHandler handler = syncToBridge();
            if (handler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge handler missing");
            } else {
                handler.registerRegionListener(this);
                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    private synchronized @Nullable DWDPollenflugBridgeHandler syncToBridge() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof DWDPollenflugBridgeHandler) {
                DWDPollenflugBridgeHandler bridgeHandler = (DWDPollenflugBridgeHandler) handler;
                return bridgeHandler;
            }
        }

        return null;
    }

    @Override
    public void dispose() {
        logger.debug("DWDPollenflug region handler disposes. Unregistering listener.");
        DWDPollenflugBridgeHandler bridgeHandler = syncToBridge();
        if (bridgeHandler != null) {
            bridgeHandler.unregisterRegionListener(this);
        }
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        DWDPollenflugBridgeHandler handler = syncToBridge();
        if (handler != null) {
            DWDPollenflug pollenflug = handler.getPollenflug();
            if (pollenflug != null) {
                notifyOnUpdate(pollenflug);
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void notifyOnUpdate(DWDPollenflug pollenflug) {
        DWDRegion region = pollenflug.getRegion(thingConfig.getRegionID());
        if (region == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Region not found");
            return;
        }

        updateStatus(ThingStatus.ONLINE);

        updateProperties(region.getProperties());

        region.getChannels().forEach((channelID, value) -> {
            logger.debug("Updating channel {} to {}", channelID, value);
            updateState(channelID, value);
        });

        if (lastUpdate == null || !lastUpdate.equals(pollenflug.getLastUpdate())) {
            triggerChannel(CHANNEL_UPDATES + "#" + CHANNEL_UPDATED, TRIGGER_REFRESHED);
            lastUpdate = pollenflug.getLastUpdate();
        }
    }
}
