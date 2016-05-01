/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.serialmessage;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveSerialMessageException;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInclusionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller
 *
 * @author Chris Jackson
 */
public class AddNodeMessageClass extends ZWaveCommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AddNodeMessageClass.class);

    private final int ADD_NODE_ANY = 0x01;
    private final int ADD_NODE_CONTROLLER = 0x02;
    private final int ADD_NODE_SLAVE = 0x03;
    private final int ADD_NODE_EXISTING = 0x04;
    private final int ADD_NODE_STOP = 0x05;
    private final int ADD_NODE_STOP_FAILED = 0x06;

    private final int ADD_NODE_STATUS_LEARN_READY = 0x01;
    private final int ADD_NODE_STATUS_NODE_FOUND = 0x02;
    private final int ADD_NODE_STATUS_ADDING_SLAVE = 0x03;
    private final int ADD_NODE_STATUS_ADDING_CONTROLLER = 0x04;
    private final int ADD_NODE_STATUS_PROTOCOL_DONE = 0x05;
    private final int ADD_NODE_STATUS_DONE = 0x06;
    private final int ADD_NODE_STATUS_FAILED = 0x07;

    private final int OPTION_HIGH_POWER = 0x80;
    private final int OPTION_NETWORK_WIDE = 0x40;

    public SerialMessage doRequestStart(boolean highPower, boolean networkWide) {
        logger.debug("Setting controller into INCLUSION mode, highPower:{} networkWide:{}.", highPower, networkWide);

        // Queue the request
        SerialMessage newMessage = new SerialMessage(SerialMessage.SerialMessageClass.AddNodeToNetwork,
                SerialMessage.SerialMessageType.Request, SerialMessage.SerialMessageClass.AddNodeToNetwork,
                SerialMessage.SerialMessagePriority.High);
        byte[] newPayload = { (byte) ADD_NODE_ANY, (byte) 255 };
        if (highPower == true) {
            newPayload[0] |= OPTION_HIGH_POWER;
        }
        if (networkWide == true) {
            newPayload[0] |= OPTION_NETWORK_WIDE;
        }

        newMessage.setMessagePayload(newPayload);
        return newMessage;
    }

    public SerialMessage doRequestStop() {
        logger.debug("Ending INCLUSION mode.");

        // Queue the request
        SerialMessage newMessage = new SerialMessage(SerialMessage.SerialMessageClass.AddNodeToNetwork,
                SerialMessage.SerialMessageType.Request, SerialMessage.SerialMessageClass.AddNodeToNetwork,
                SerialMessage.SerialMessagePriority.High);
        byte[] newPayload = { (byte) ADD_NODE_STOP };

        newMessage.setMessagePayload(newPayload);
        return newMessage;
    }

    @Override
    public boolean handleRequest(ZWaveController zController, SerialMessage lastSentMessage,
            SerialMessage incomingMessage) {
        try {
            switch (incomingMessage.getMessagePayloadByte(1)) {
                case ADD_NODE_STATUS_LEARN_READY:
                    logger.debug("Add Node: Learn ready.");
                    zController.notifyEventListeners(new ZWaveInclusionEvent(ZWaveInclusionEvent.Type.IncludeStart));
                    break;
                case ADD_NODE_STATUS_NODE_FOUND:
                    logger.debug("Add Node: New node found.");
                    break;
                case ADD_NODE_STATUS_ADDING_SLAVE:
                    logger.debug("NODE {}: Adding slave.", incomingMessage.getMessagePayloadByte(2));
                    zController.notifyEventListeners(new ZWaveInclusionEvent(ZWaveInclusionEvent.Type.IncludeSlaveFound,
                            incomingMessage.getMessagePayloadByte(2)));
                    break;
                case ADD_NODE_STATUS_ADDING_CONTROLLER:
                    logger.debug("NODE {}: Adding controller.", incomingMessage.getMessagePayloadByte(2));
                    zController.notifyEventListeners(new ZWaveInclusionEvent(
                            ZWaveInclusionEvent.Type.IncludeControllerFound, incomingMessage.getMessagePayloadByte(2)));
                    break;
                case ADD_NODE_STATUS_PROTOCOL_DONE:
                    logger.debug("Add Node: Protocol done.");
                    break;
                case ADD_NODE_STATUS_DONE:
                    logger.debug("Add Node: Done.");
                    // If the node ID is 0, ignore!
                    if (incomingMessage.getMessagePayloadByte(2) > 0
                            && incomingMessage.getMessagePayloadByte(2) <= 232) {
                        zController.notifyEventListeners(new ZWaveInclusionEvent(ZWaveInclusionEvent.Type.IncludeDone,
                                incomingMessage.getMessagePayloadByte(2)));
                    }
                    break;
                case ADD_NODE_STATUS_FAILED:
                    logger.debug("Add Node: Failed.");
                    zController.notifyEventListeners(new ZWaveInclusionEvent(ZWaveInclusionEvent.Type.IncludeFail));
                    break;
                default:
                    logger.debug("Unknown request ({}).", incomingMessage.getMessagePayloadByte(1));
                    break;
            }
        } catch (ZWaveSerialMessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        checkTransactionComplete(lastSentMessage, incomingMessage);

        return transactionComplete;
    }
}
