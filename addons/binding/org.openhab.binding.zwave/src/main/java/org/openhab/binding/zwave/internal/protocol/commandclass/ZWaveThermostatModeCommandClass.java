/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageType;
import org.openhab.binding.zwave.internal.protocol.ZWaveSerialMessageException;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Handles the Thermostat Mode command class.
 *
 * @author Chris Jackson
 * @author Dan Cunningham
 */
@XStreamAlias("thermostatModeCommandClass")
public class ZWaveThermostatModeCommandClass extends ZWaveCommandClass
        implements ZWaveBasicCommands, ZWaveCommandClassInitialization, ZWaveCommandClassDynamicState {

    @XStreamOmitField
    private static final Logger logger = LoggerFactory.getLogger(ZWaveThermostatModeCommandClass.class);

    private static final byte THERMOSTAT_MODE_SET = 0x1;
    private static final byte THERMOSTAT_MODE_GET = 0x2;
    private static final byte THERMOSTAT_MODE_REPORT = 0x3;
    private static final byte THERMOSTAT_MODE_SUPPORTED_GET = 0x4;
    private static final byte THERMOSTAT_MODE_SUPPORTED_REPORT = 0x5;

    private final Set<ModeType> modeTypes = new HashSet<ModeType>();

    @XStreamOmitField
    private boolean initialiseDone = false;
    @XStreamOmitField
    private boolean dynamicDone = false;

    private boolean isGetSupported = true;

    /**
     * Creates a new instance of the ZWaveThermostatModeCommandClass class.
     *
     * @param node the node this command class belongs to
     * @param controller the controller to use
     * @param endpoint the endpoint this Command class belongs to
     */
    public ZWaveThermostatModeCommandClass(ZWaveNode node, ZWaveController controller, ZWaveEndpoint endpoint) {
        super(node, controller, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandClass getCommandClass() {
        return CommandClass.THERMOSTAT_MODE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxVersion() {
        return 2;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ZWaveSerialMessageException
     */
    @Override
    public void handleApplicationCommandRequest(SerialMessage serialMessage, int offset, int endpoint)
            throws ZWaveSerialMessageException {
        logger.debug("NODE {}: Received Thermostat Mode Request", this.getNode().getNodeId());
        int command = serialMessage.getMessagePayloadByte(offset);
        switch (command) {
            case THERMOSTAT_MODE_SET:
                processThermostatModeReport(serialMessage, offset, endpoint);
                break;
            case THERMOSTAT_MODE_SUPPORTED_REPORT:
                logger.debug("NODE {}: Process Thermostat Supported Mode Report", this.getNode().getNodeId());

                int payloadLength = serialMessage.getMessagePayload().length;

                for (int i = offset + 1; i < payloadLength; ++i) {
                    int bitMask = serialMessage.getMessagePayloadByte(i);
                    for (int bit = 0; bit < 8; ++bit) {
                        if ((bitMask & (1 << bit)) == 0) {
                            continue;
                        }

                        int index = ((i - (offset + 1)) * 8) + bit;

                        // (n)th bit is set. n is the index for the mode type enumeration.
                        ModeType modeTypeToAdd = ModeType.getModeType(index);
                        if (modeTypeToAdd != null) {
                            this.modeTypes.add(modeTypeToAdd);
                            logger.debug("NODE {}: Added mode type {} ({})", this.getNode().getNodeId(),
                                    modeTypeToAdd.getLabel(), index);
                        } else {
                            logger.warn("NODE {}: Unknown mode type {}", this.getNode().getNodeId(), index);
                        }
                    }
                }

                initialiseDone = true;
                break;
            case THERMOSTAT_MODE_REPORT:
                processThermostatModeReport(serialMessage, offset, endpoint);
                break;
            default:
                logger.warn("NODE {}: Unsupported Command {} for command class {} ({}).", this.getNode().getNodeId(),
                        command, this.getCommandClass().getLabel(), this.getCommandClass().getKey());
        }
    }

    /**
     * Processes a THERMOSTAT_MODE_REPORT message.
     *
     * @param serialMessage the incoming message to process.
     * @param offset the offset position from which to start message processing.
     * @param endpoint the endpoint or instance number this message is meant for.
     * @throws ZWaveSerialMessageException
     */
    protected void processThermostatModeReport(SerialMessage serialMessage, int offset, int endpoint)
            throws ZWaveSerialMessageException {

        int value = serialMessage.getMessagePayloadByte(offset + 1);

        logger.debug("NODE {}: Thermostat Mode report, value = {}", this.getNode().getNodeId(), value);

        ModeType modeType = ModeType.getModeType(value);

        if (modeType == null) {
            logger.error("NODE {}: Unknown Mode Type = {}, ignoring report.", this.getNode().getNodeId(), value);
            return;
        }

        // mode type seems to be supported, add it to the list.
        if (!modeTypes.contains(modeType)) {
            modeTypes.add(modeType);
        }

        dynamicDone = true;

        logger.debug("NODE {}: Thermostat Mode Report, value = {}", this.getNode().getNodeId(), modeType.getLabel());
        ZWaveCommandClassValueEvent zEvent = new ZWaveCommandClassValueEvent(this.getNode().getNodeId(), endpoint,
                this.getCommandClass(), value);
        this.getController().notifyEventListeners(zEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SerialMessage> initialize(boolean refresh) {
        ArrayList<SerialMessage> result = new ArrayList<SerialMessage>();
        if (refresh == true || initialiseDone == false) {
            result.add(this.getSupportedMessage());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SerialMessage> getDynamicValues(boolean refresh) {
        ArrayList<SerialMessage> result = new ArrayList<SerialMessage>();
        if (refresh == true || dynamicDone == false) {
            result.add(getValueMessage());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SerialMessage getValueMessage() {
        if (isGetSupported == false) {
            logger.debug("NODE {}: Node doesn't support get requests", this.getNode().getNodeId());
            return null;
        }

        logger.debug("NODE {}: Creating new message for application command THERMOSTAT_MODE_GET",
                this.getNode().getNodeId());
        SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData,
                SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler, SerialMessagePriority.Get);
        byte[] payload = { (byte) this.getNode().getNodeId(), 2, (byte) getCommandClass().getKey(),
                THERMOSTAT_MODE_GET };
        result.setMessagePayload(payload);
        return result;
    }

    @Override
    public boolean setOptions(Map<String, String> options) {
        if ("false".equals(options.get("getSupported"))) {
            isGetSupported = false;
        }

        return true;
    }

    /**
     * Gets a SerialMessage with the THERMOSTAT_MODE_SUPPORTED_GET command
     *
     * @return the serial message, or null if the supported command is not supported.
     */
    public SerialMessage getSupportedMessage() {
        logger.debug("NODE {}: Creating new message for application command THERMOSTAT_MODE_SUPPORTED_GET",
                this.getNode().getNodeId());

        SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData,
                SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler, SerialMessagePriority.Config);
        byte[] newPayload = { (byte) this.getNode().getNodeId(), 2, (byte) getCommandClass().getKey(),
                THERMOSTAT_MODE_SUPPORTED_GET };
        result.setMessagePayload(newPayload);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SerialMessage setValueMessage(int value) {

        logger.debug("NODE {}: setValueMessage {}, modeType empty {}", this.getNode().getNodeId(), value,
                modeTypes.isEmpty());

        // if we do not have any mode types yet, get them
        if (modeTypes.isEmpty()) {
            logger.warn("NODE {}: requesting mode types, set request ignored (try again later)",
                    this.getNode().getNodeId());
            return this.getSupportedMessage();
        }

        if (!modeTypes.contains(ModeType.getModeType(value))) {
            logger.error("NODE {}: Unsupported mode type {}", this.getNode().getNodeId(), value);
            return null;
        }

        logger.debug("NODE {}: Creating new message for application command THERMOSTAT_MODE_SET",
                this.getNode().getNodeId());

        SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData,
                SerialMessageType.Request, SerialMessageClass.SendData, SerialMessagePriority.Set);
        byte[] newPayload = { (byte) this.getNode().getNodeId(), 3, (byte) getCommandClass().getKey(),
                THERMOSTAT_MODE_SET, (byte) value };
        result.setMessagePayload(newPayload);
        return result;
    }

    /**
     * Z-Wave ModeType enumeration. The mode type indicates the type of mode that is reported.
     *
     * @author Dan Cunningham
     */
    @XStreamAlias("modeType")
    public enum ModeType {

        OFF(0, "Off"),
        HEAT(1, "Heat"),
        COOL(2, "Cool"),
        AUTO(3, "Auto"),
        AUX_HEAT(4, "Aux Heat"),
        RESUME(5, "Resume"),
        FAN_ONLY(6, "Fan Only"),
        FURNANCE(7, "Furnace"),
        DRY_AIR(8, "Dry Air"),
        MOIST_AIR(9, "Moist Air"),
        AUTO_CHANGEOVER(10, "Auto Changeover"),
        HEAT_ECON(11, "Heat Econ"),
        COOL_ECON(12, "Cool Econ"),
        AWAY(13, "Away"),
        MANUAL(31, "Manual");

        /**
         * A mapping between the integer code and its corresponding mode type to facilitate lookup by code.
         */
        private static Map<Integer, ModeType> codeToModeTypeMapping;

        private int key;
        private String label;

        private ModeType(int key, String label) {
            this.key = key;
            this.label = label;
        }

        private static void initMapping() {
            codeToModeTypeMapping = new HashMap<Integer, ModeType>();
            for (ModeType s : values()) {
                codeToModeTypeMapping.put(s.key, s);
            }
        }

        /**
         * Lookup function based on the mode type code.
         * Returns null if the code does not exist.
         *
         * @param i the code to lookup
         * @return enumeration value of the mode type.
         */
        public static ModeType getModeType(int i) {
            if (codeToModeTypeMapping == null) {
                initMapping();
            }
            return codeToModeTypeMapping.get(i);
        }

        /**
         * @return the key
         */
        public int getKey() {
            return key;
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }
    }
}
