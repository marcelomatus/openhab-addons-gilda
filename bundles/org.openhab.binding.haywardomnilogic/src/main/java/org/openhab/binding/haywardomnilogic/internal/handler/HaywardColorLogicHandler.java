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
import org.eclipse.smarthome.core.library.types.OnOffType;
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
 * The ColorLogic Handler
 *
 * @author Matt Myers - Initial Contribution
 */
@NonNullByDefault
public class HaywardColorLogicHandler extends HaywardThingHandler {
    private final Logger logger = LoggerFactory.getLogger(HaywardColorLogicHandler.class);

    HaywardThingProperties prop = getConfig().as(HaywardThingProperties.class);

    public HaywardColorLogicHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void getTelemetry(String xmlResponse) throws Exception {
        List<String> data = new ArrayList<>();
        List<String> systemIDs = new ArrayList<>();

        @SuppressWarnings("null")
        HaywardBridgeHandler bridgehandler = (HaywardBridgeHandler) getBridge().getHandler();
        if (bridgehandler != null) {
            systemIDs = bridgehandler.evaluateXPath("//ColorLogic-Light/@systemId", xmlResponse);
            String thingSystemID = getThing().getProperties().get(HaywardBindingConstants.PROPERTY_SYSTEM_ID);
            for (int i = 0; i < systemIDs.size(); i++) {
                if (systemIDs.get(i).equals(thingSystemID)) {
                    // Light State
                    data = bridgehandler.evaluateXPath("//ColorLogic-Light/@lightState", xmlResponse);
                    updateData(HaywardBindingConstants.CHANNEL_COLORLOGIC_LIGHTSTATE, data.get(0));

                    if (data.get(0).equals("0")) {
                        updateData(HaywardBindingConstants.CHANNEL_COLORLOGIC_ENABLE, "0");
                    } else {
                        updateData(HaywardBindingConstants.CHANNEL_COLORLOGIC_ENABLE, "1");
                    }

                    // Current Show
                    data = bridgehandler.evaluateXPath("//ColorLogic-Light/@currentShow", xmlResponse);
                    updateData(HaywardBindingConstants.CHANNEL_COLORLOGIC_CURRENTSHOW, data.get(0));
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
                    case HaywardBindingConstants.CHANNEL_COLORLOGIC_ENABLE:
                        if (command == OnOffType.ON) {
                            cmdString = "1";
                        } else {
                            cmdString = "0";
                        }
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
                    case HaywardBindingConstants.CHANNEL_COLORLOGIC_CURRENTSHOW:
                        cmdURL = HaywardBindingConstants.COMMAND_PARAMETERS
                                + "<Name>SetStandAloneLightShow</Name><Parameters>"
                                + "<Parameter name=\"Token\" dataType=\"String\">" + bridgehandler.account.token
                                + "</Parameter>" + "<Parameter name=\"MspSystemID\" dataType=\"int\">"
                                + bridgehandler.account.mspSystemID + "</Parameter>"
                                + "<Parameter name=\"PoolID\" dataType=\"int\">" + prop.poolID + "</Parameter>"
                                + "<Parameter name=\"LightID\" dataType=\"int\">" + prop.systemID + "</Parameter>"
                                + "<Parameter name=\"Show\" dataType=\"int\">" + cmdString + "</Parameter>"
                                + "<Parameter name=\"Speed\" dataType=\"byte\">4</Parameter>"
                                + "<Parameter name=\"Brightness\" dataType=\"byte\">4</Parameter>"
                                + "<Parameter name=\"Reserved\" dataType=\"byte\">0</Parameter>"
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
