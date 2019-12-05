/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.somfymylink.internal.handler;

import static org.openhab.binding.somfymylink.internal.SomfyMyLinkBindingConstants.CHANNEL_SHADELEVEL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SomfyMyLinkBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Chris Johnson - Initial contribution
 */
@NonNullByDefault
public class SomfyShadeHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SomfyShadeHandler.class);

    public SomfyShadeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        initDeviceState();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("Bridge status changed to {} updating {}", bridgeStatusInfo.getStatus(), getThing().getLabel());

        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE
                && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            initDeviceState();
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    public void initDeviceState() {
        Bridge bridge = getBridge();

        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge configured");
            logger.debug("Initialized device state for shade {} {}", ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR);
        } else if (bridge.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            logger.debug("Initialized device state for shade {}", ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            logger.debug("Initialized device state for shade {} {}", ThingStatus.OFFLINE,
                    ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (CHANNEL_SHADELEVEL.equals(channelUID.getId())) {
                String targetId = channelUID.getThingUID().getId();

                if (command instanceof RefreshType) {
                    return;
                }

                if (CHANNEL_SHADELEVEL.equals(channelUID.getId()) && command instanceof UpDownType) {
                    if (command.equals(UpDownType.DOWN)) {
                        getBridgeHandler().commandShadeDown(targetId);
                    } else {
                        getBridgeHandler().commandShadeUp(targetId);
                    }
                }

                if (CHANNEL_SHADELEVEL.equals(channelUID.getId()) && command instanceof StopMoveType) {
                    getBridgeHandler().commandShadeStop(targetId);
                }
            }
        } catch (SomfyMyLinkException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    protected SomfyMyLinkBridgeHandler getBridgeHandler() {
        Bridge bridge = this.getBridge();
        if (bridge == null)
            throw new SomfyMyLinkException("No bridge was found");

        BridgeHandler handler = bridge.getHandler();
        if (handler == null)
            throw new SomfyMyLinkException("No handler was found");

        return (SomfyMyLinkBridgeHandler) handler;
    }
}
