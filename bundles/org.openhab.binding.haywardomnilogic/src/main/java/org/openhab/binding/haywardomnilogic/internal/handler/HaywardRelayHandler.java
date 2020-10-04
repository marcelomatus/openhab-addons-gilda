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
package org.openhab.binding.haywardomnilogic.internal.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.haywardomnilogic.internal.HaywardBindingConstants;
import org.openhab.binding.haywardomnilogic.internal.HaywardThingHandler;
import org.openhab.binding.haywardomnilogic.internal.HaywardThingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Relay Handler
 *
 * @author Matt Myers - Initial Contribution
 */
@NonNullByDefault
public class HaywardRelayHandler extends HaywardThingHandler {
    private final Logger logger = LoggerFactory.getLogger(HaywardRelayHandler.class);

    HaywardThingProperties prop = getConfig().as(HaywardThingProperties.class);

    public HaywardRelayHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void getTelemetry(String xmlResponse) throws Exception {
        List<String> data = new ArrayList<>();
        List<String> systemIDs = new ArrayList<>();

        @SuppressWarnings("null")
        HaywardBridgeHandler bridgehandler = (HaywardBridgeHandler) getBridge().getHandler();
        if (bridgehandler != null) {
            systemIDs = bridgehandler.evaluateXPath("//Relay/@systemId", xmlResponse);
            data = bridgehandler.evaluateXPath("//Relay/@relayState", xmlResponse);
            String thingSystemID = getThing().getProperties().get(HaywardBindingConstants.PROPERTY_SYSTEM_ID);
            for (int i = 0; i < systemIDs.size(); i++) {
                if (systemIDs.get(i).equals(thingSystemID)) {
                    updateData(HaywardBindingConstants.CHANNEL_RELAY_STATE, data.get(i));
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if ((command instanceof RefreshType)) {
            return;
        }

        prop.systemID = getThing().getProperties().get(HaywardBindingConstants.PROPERTY_SYSTEM_ID);
        prop.poolID = getThing().getProperties().get(HaywardBindingConstants.PROPERTY_BOWID);

        @SuppressWarnings("null")
        HaywardBridgeHandler bridgehandler = (HaywardBridgeHandler) getBridge().getHandler();
        if (bridgehandler != null) {
            String cmdString = this.cmdToString(command);
            String cmdURL = null;
            try {
                switch (channelUID.getId()) {
                    case HaywardBindingConstants.CHANNEL_RELAY_STATE:
                        cmdURL = HaywardBindingConstants.COMMAND_PARAMETERS
                                + "<Name>SetUIEquipmentCmd</Name><Parameters>"
                                + "<Parameter name=\"Token\" dataType=\"String\">" + bridgehandler.account.token
                                + "</Parameter>" + "<Parameter name=\"MspSystemID\" dataType=\"int\">"
                                + bridgehandler.account.mspSystemID + "</Parameter>"
                                + "<Parameter name=\"PoolID\" dataType=\"int\">" + prop.poolID + "</Parameter>"
                                + "<Parameter name=\"EquipmentID\" dataType=\"int\">" + prop.systemID + "</Parameter>"
                                + "<Parameter name=\"IsOn\" dataType=\"int\">" + cmdString + "</Parameter>"
                                + HaywardBindingConstants.COMMAND_SCHEDULE + "</Parameters></Request>";
                        break;
                    default:
                        logger.error("haywardCommand Unsupported type {}", channelUID);
                        return;
                }

                // *****Send Command to Hayward server
                String xmlResponse = bridgehandler.httpXmlResponse(cmdURL);
                String status = bridgehandler.evaluateXPath("//Parameter[@name='Status']/text()", xmlResponse).get(0);

                if (!(status.equals("0"))) {
                    logger.error("haywardCommand XML response: {}", xmlResponse);
                    return;
                }
            } catch (Exception e) {
                logger.debug("Unable to send command to Hayward's server {}:{}", bridgehandler.config.hostname,
                        bridgehandler.config.username);
            }
        }
    }
}
