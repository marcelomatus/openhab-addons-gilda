/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.phc.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.phc.PHCBindingConstants;
import org.openhab.binding.phc.internal.PHCHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link PHCBridgeHandler} is responsible for handling the serial Communication to and from the PHC Modules.
 *
 * @author Jonas Hohaus - Initial contribution
 */

public class PHCBridgeHandler extends BaseBridgeHandler implements SerialPortEventListener {

    private Logger logger = LoggerFactory.getLogger(PHCBridgeHandler.class);

    private static final int BAUD = 19200;
    private static final int SEND_RETRY_COUNT = 15; // max count to send the same message
    private static final int SEND_RETRY_TIME = 80; // time to wait for an acknowledge before send the message again

    private InputStream serialIn;
    private OutputStream serialOut;
    short lastReceivedCrc;
    byte[] messageFragment;
    RXTXPort serialPort;

    private Map<String, Boolean> toggleMap = new HashMap<String, Boolean>();
    private ConcurrentSkipListMap<Long, QueueObject> queue = new ConcurrentSkipListMap<Long, QueueObject>();

    private byte emLedOutputState[] = new byte[32];
    private byte amOutputState[] = new byte[32];

    private List<Byte> modules = new ArrayList<Byte>();

    public PHCBridgeHandler(Bridge phcBridge) {
        super(phcBridge);
    }

    @Override
    public void initialize() {
        String port = ((String) getConfig().get(PHCBindingConstants.PORT));

        try {
            CommPortIdentifier portId = null;

            // parse ports and if the default port is found, initialized the
            // reader
            @SuppressWarnings("rawtypes")
            Enumeration portList = CommPortIdentifier.getPortIdentifiers();
            while (portList.hasMoreElements()) {
                CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
                if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    if (id.getName().equals(port)) {
                        logger.debug("Serial port '{}' has been found.", port);
                        portId = id;
                    }
                }
            }

            if (portId == null) {
                StringBuilder sb = new StringBuilder();
                portList = CommPortIdentifier.getPortIdentifiers();
                while (portList.hasMoreElements()) {
                    CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
                    if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                        sb.append(id.getName() + "\n");
                    }
                }
                logger.error("Serial port '{}' could not be found. Available ports are:\n {}", port, sb);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Serial port '" + port + "' could not be found.");

            } else {
                // initialize serial port
                serialPort = portId.open("openHAB", 2000); // owner, timeout
                serialIn = serialPort.getInputStream();
                // set port parameters
                serialPort.setSerialPortParams(BAUD, SerialPort.DATABITS_8, SerialPort.STOPBITS_2,
                        SerialPort.PARITY_NONE);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

                serialPort.addEventListener(this);

                // activate the DATA_AVAILABLE notifier
                serialPort.notifyOnDataAvailable(true);
                serialPort.setRTS(true);

                // get the output stream
                serialOut = serialPort.getOutputStream();

                sendPorBroadcast((byte) 0xFF);

                byte[] b = { 0x01 };
                for (int j = 0; j <= 0x1F; j++) {
                    serialWrite(buildMessage((byte) j, 0, b, false));

                }
                updateStatus(ThingStatus.ONLINE);

                scheduler.submit(new Runnable() {

                    @Override
                    public void run() {
                        processQueueLoop();
                    }

                });

            }

        } catch (PortInUseException | TooManyListenersException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Could not open serial port " + serialPort + ": " + e.getMessage());

        } catch (UnsupportedCommOperationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Could not configure serial port " + serialPort + ": " + e.getMessage());
        }
    }

    /**
     * Put the available data on serial port into a buffer and transfer it to the processing method.
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != SerialPortEvent.DATA_AVAILABLE) {
            return;
        }
        StringBuilder binaryBuffer = new StringBuilder();
        StringBuilder hexBuffer = new StringBuilder();
        byte[] buffer;

        try {
            if (serialIn.available() <= 0) {
                return;
            }

            Thread.sleep(5);
            buffer = new byte[serialIn.available()];
            serialIn.read(buffer);

            Byte[] result;
            if (messageFragment != null) {
                result = processInputStream(ArrayUtils.addAll(messageFragment, buffer)); // result needed?
            } else {
                result = processInputStream(buffer); // result needed?
            }

            if (logger.isDebugEnabled()) {
                for (Byte b : result) {
                    binaryBuffer.append(PHCHelper.byteToBinaryString(b));
                    hexBuffer.append(PHCHelper.byteToHexString(b));
                }

                logger.debug("Incoming Byte: {}", binaryBuffer);
                logger.debug("Incoming Hex: {}", hexBuffer);
            }
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

    }

    /**
     * Split and process the given buffer to single messages.
     *
     * @param buffer
     * @return the whole buffer for logging.
     * @throws IOException
     */
    private Byte[] processInputStream(byte[] buffer) throws IOException {
        List<Byte> result = new ArrayList<Byte>();
        List<String> repeat = new ArrayList<String>();
        int pos = 0;
        messageFragment = null;
        Byte moduleAddress = null;

        while (pos < buffer.length) {
            int messageStart = 0;

            for (byte b : buffer) {
                result.add(b);
                if (modules.contains(b)) {
                    moduleAddress = b;
                    messageStart = pos;
                    break;
                }
                pos++;
            }

            if (moduleAddress == null) {
                return result.toArray(new Byte[result.size()]);
            }

            if (buffer.length - 1 - pos < 3) {
                messageFragment = new byte[buffer.length - pos + 1];
                System.arraycopy(buffer, messageStart, messageFragment, 0, messageFragment.length);
            }

            byte size = 0;
            result.add(buffer[pos]);
            size = (byte) (buffer[pos] & 0x7F);

            if (size < 4) {
                if (buffer.length - 1 - pos < size + 2) {
                    messageFragment = new byte[buffer.length - pos + 1];
                    System.arraycopy(buffer, messageStart, messageFragment, 0, messageFragment.length);
                }

                byte[] message = new byte[size + 4];
                message[0] = moduleAddress;
                message[1] = buffer[pos++];

                for (int i = 2; i < size + 4; i++) {
                    message[i] = buffer[pos++];
                    result.add(message[i]);
                }

                if (!repeat.contains(Arrays.toString(message))) { // avoids multiple processing of the same message
                    processIncomingMessage(message, size);
                    repeat.add(Arrays.toString(message));
                }
            }
        }
        return result.toArray(new Byte[result.size()]);
    }

    private void processQueueLoop() {
        while (true) {
            long limit = System.currentTimeMillis();

            Map<Long, QueueObject> subQueue = queue.subMap(0L, true, limit, true);

            for (Long key : subQueue.keySet()) {
                QueueObject qo = subQueue.get(key);
                queue.remove(key);
                if (qo.getCounter() < SEND_RETRY_COUNT && !qo.getCommand()
                        .equals(isChannelOutputStateSet(qo.getModuleType(), qo.getModuleAddress(), qo.getChannel()))) {
                    qo.increaseCounter();

                    switch (qo.getModuleType()) {
                        case PHCBindingConstants.CHANNELS_AM:
                            sendAm(qo.getModuleAddress(), qo.getChannel(), qo.getCommand());
                            break;
                        case PHCBindingConstants.CHANNELS_EM_LED:
                            sendEm(qo.getModuleAddress(), qo.getChannel(), qo.getCommand());
                            break;
                        case PHCBindingConstants.CHANNELS_JRM:
                            sendJRM(qo.getModuleAddress(), qo.getChannel(), qo.getCommand());
                            break;
                    }

                    queue.put(System.currentTimeMillis() + SEND_RETRY_TIME, qo);

                } else if (qo.getCounter() >= SEND_RETRY_COUNT
                        && !qo.getModuleType().equals(PHCBindingConstants.CHANNELS_JRM)) {
                    // Can´t process the acknowledgement of JRM yet.
                    // TODO: If no acknowledge from the module received, perhaps set the status of the appropriate thing
                    // to offline. The communication isn't yet enough reliable.
                    logger.info("No acknowlgdge from the module {} received.", qo.getModuleAddress());
                }
            }

        }
    }

    private void setModuleOutputState(String moduleType, byte moduleAddress, byte state) {
        if (moduleType == PHCBindingConstants.CHANNELS_EM_LED) {
            emLedOutputState[moduleAddress] = state;
        } else if (moduleType == PHCBindingConstants.CHANNELS_AM) {
            amOutputState[moduleAddress & 0x1F] = state;
        } else if (moduleType == PHCBindingConstants.CHANNELS_JRM) {
            // not implemented yet
        }
    }

    private State isChannelOutputStateSet(String moduleType, byte moduleAddress, byte channel) {
        State set = null;

        if (moduleType.equals(PHCBindingConstants.CHANNELS_EM_LED)) {
            set = ((emLedOutputState[moduleAddress] >> channel) & 0x01) == 1 ? OnOffType.ON : OnOffType.OFF;
        } else if (moduleType.equals(PHCBindingConstants.CHANNELS_AM)) {
            set = ((amOutputState[moduleAddress & 0x1F] >> channel) & 0x01) == 1 ? OnOffType.ON : OnOffType.OFF;
        } else if (moduleType.equals(PHCBindingConstants.CHANNELS_JRM)) {
            set = ((amOutputState[moduleAddress & 0x1F] >> channel) & 0x01) == 1 ? OnOffType.ON : OnOffType.OFF;
        }

        return set;
    }

    private boolean toggleChannel(byte moduleAddress, byte channel) {
        String key = new String(new byte[] { moduleAddress, channel });
        boolean toggle = false;
        if (toggleMap.containsKey(key)) {
            toggle = toggleMap.get(key);
        }
        toggleMap.put(key, !toggle);

        return toggle;
    }

    /**
     * Put the given command into the queue to send.
     *
     * @param moduleType
     * @param moduleAddress
     * @param channel
     * @param command
     */
    public void send(String moduleType, String moduleAddress, String channel, Command command) {
        if (command instanceof OnOffType || command instanceof UpDownType || command.equals(StopMoveType.STOP)) {
            if (moduleType.equals(PHCBindingConstants.CHANNELS_JRM)) { // can´t process acknowledge yet
                queue.put(System.currentTimeMillis(), new QueueObject(moduleType, moduleAddress, channel, command, 14));
            } else {
                queue.put(System.currentTimeMillis(), new QueueObject(moduleType, moduleAddress, channel, command));
            }
        }
    }

    private void sendAm(byte moduleAddress, byte channel, Command command) {
        moduleAddress |= 0x40;
        byte[] cmd = { (byte) (channel << 5) };

        if (command.equals(OnOffType.ON)) {
            cmd[0] |= 2;
        } else {
            cmd[0] |= 3;
        }
        serialWrite(buildMessage(moduleAddress, channel, cmd, toggleChannel(moduleAddress, channel)));
    }

    private void sendEm(byte moduleAddress, byte channel, Command command) {
        byte[] cmd = { (byte) (channel << 4) };

        if (command.equals(OnOffType.ON)) {
            cmd[0] |= 2;
        } else {
            cmd[0] |= 3;
        }
        serialWrite(buildMessage(moduleAddress, channel, cmd, toggleChannel(moduleAddress, channel)));
    }

    private void sendJRM(byte moduleAddress, byte channel, Command command) {
        // The up and the down message needs two additional bytes for the time.
        int size = (command == StopMoveType.STOP) ? 2 : 4;
        byte[] cmd = new byte[size];

        moduleAddress |= 0x40;

        cmd[0] = (byte) (channel << 5);
        cmd[1] = 0x3F;

        switch (command.toString()) {
            case "UP":
                cmd[0] |= 5;
                cmd[2] = 0x70;// ZEIT 1/10 Sek. LSB
                cmd[3] = 0x17; // 1/10 Sek. MSB
                break;
            case "DOWN":
                cmd[0] |= 6;
                cmd[2] = 0x70;// ZEIT 1/10 Sek. LSB
                cmd[3] = 0x17; // 1/10 Sek. MSB
                break;
            case "STOP":
                cmd[0] |= 2;
                break;
        }

        serialWrite(buildMessage(moduleAddress, channel, cmd, toggleChannel(moduleAddress, channel)));
    }

    private void sendPorBroadcast(byte b) {
        byte[] cmd = { 0 };
        writeMsg(buildMessage((byte) 0xFF, 0, cmd, false), 20); // 20 times needed?
    }

    private void sendAmConfig(byte moduleAddress) {
        byte[] cmd = new byte[3];

        cmd[0] = (byte) 0xFE;
        cmd[1] = 0;
        cmd[2] = (byte) 0xFF;

        serialWrite(buildMessage(moduleAddress, 0, cmd, false));
    }

    private void sendEmConfig(byte moduleAddress) {
        byte[] cmd = new byte[52];
        int pos = 0;

        cmd[pos++] = (byte) 0xFE;
        cmd[pos++] = (byte) 0x00; // POR

        cmd[pos++] = 0x00;
        cmd[pos++] = 0x00;

        for (int i = 0; i < 16; i++) { // 16 inputs
            cmd[pos++] = (byte) ((i << 4) | 0x02);
            cmd[pos++] = (byte) ((i << 4) | 0x03);
            cmd[pos++] = (byte) ((i << 4) | 0x05);
        }

        serialWrite(buildMessage(moduleAddress, 0, cmd, false));
    }

    private void sendEmAcknowledge(byte module, boolean toggle) {
        byte[] cmd = { 0 };
        writeMsg(buildMessage(module, 0, cmd, toggle), 2); // 2 times needed?
    }

    /**
     * Build a serial message from the given parameters.
     *
     * @param modulAddr
     * @param channel
     * @param cmd
     * @param toggle
     * @return
     */
    private byte[] buildMessage(byte modulAddr, int channel, byte[] cmd, boolean toggle) {
        int len = cmd.length;
        byte[] buffer = new byte[len + 4];

        buffer[0] = modulAddr;
        buffer[1] = (byte) (toggle ? (len | 0x80) : len); // 0x80: 1000 0000

        for (int i = 0; i < len; i++) {
            buffer[2 + i] = cmd[i];
        }

        short crc = (short) 0xFFFF;

        for (int i = 0; i < (2 + len); i++) {
            crc = crc16Update(crc, buffer[i]);
        }
        crc ^= 0xFFFF;

        buffer[2 + len] = (byte) (crc & 0xFF);
        buffer[3 + len] = (byte) ((crc >> 8) & 0xFF);

        return buffer;
    }

    /**
     * Calculate/update the 16 bit crc of the message.
     *
     * @param crc
     * @param data
     * @return
     */
    private short crc16Update(short crc, byte data) {
        data ^= crc & 0xFF;
        data ^= data << 4;
        short data16 = data;

        crc = (short) (((data16 << 8) | (((crc >> 8) & 0xFF) & 0xFF)) ^ ((data >> 4) & 0xF)
                ^ ((data16 << 3) & 0b11111111111));
        return crc;
    }

    /**
     * Process the incoming message and start the reaction.
     *
     * @param inByte
     * @param size
     */
    private void processIncomingMessage(byte[] inByte, int size) {
        short calcCrc = (short) 0xFFFF;
        short rcvCrc = (short) (inByte[2 + size] & 0xFF);
        rcvCrc |= (inByte[size + 3] << 8);
        boolean toggleIn = false;

        if (logger.isDebugEnabled()) {
            StringBuilder logMessage = new StringBuilder();
            for (int i = 0; i < inByte.length; i++) {
                logMessage.append(PHCHelper.byteToBinaryString(inByte[i]));
            }
            logMessage.append(", " + inByte.length);
            logger.debug("received: {}", logMessage);
        }

        if ((inByte[1] & 0x80) == 0x80) {
            toggleIn = true;
        }

        for (int i = 0; i < (size + 2); i++) {
            calcCrc = crc16Update(calcCrc, inByte[i]);
        }
        calcCrc ^= 0xFFFF;

        if (rcvCrc == calcCrc) {

            byte moduleAddress = inByte[0];

            byte[] cmd = new byte[size];
            for (int i = 0; i < cmd.length; i++) {
                cmd[i] = inByte[2 + i];
            }

            int[] channel = new int[size];
            for (int i = 0; i < cmd.length; i++) {
                channel[i] = (inByte[2 + i] >> 4) & 0xF;
            }

            if ((moduleAddress & 0xE0) != 0xE0) {
                if (cmd[0] == 0) {
                    if ((((moduleAddress & 0xE0) == 0x40) || ((moduleAddress & 0xE0) == 0x00)) && (cmd.length == 2)) {
                        String moduleType = ((moduleAddress & 0xE0) == 0x40) ? PHCBindingConstants.CHANNELS_AM
                                : PHCBindingConstants.CHANNELS_EM_LED;
                        setModuleOutputState(moduleType, moduleAddress, cmd[1]);
                    }
                } else if (cmd[0] == (byte) 0xFF) {
                    if ((moduleAddress & 0xE0) == 0x00) { // EM
                        sendEmConfig(moduleAddress);
                    }
                    if ((moduleAddress & 0xE0) == 0x40) { // AM and JRM
                        sendAmConfig(moduleAddress);
                    }

                } else if (cmd[0] != 0x01) { // I'm not sure what the command 0x01 means. It isn't relevant for normal
                                             // functionality.
                    if ((moduleAddress & 0xE0) == 0) {
                        if (rcvCrc == lastReceivedCrc) {
                            sendEmAcknowledge(moduleAddress, toggleIn); // Just send the acknowledge message again, when
                                                                        // PHC didn't recognize it.
                        } else {
                            sendEmAcknowledge(moduleAddress, toggleIn);
                            handleIncomingCommand(moduleAddress, channel[0], cmd, rcvCrc);

                        }
                    }
                }
            }
        }
    }

    /**
     * Send the incoming command to the appropriate handler and channel.
     *
     * @param moduleAddress
     * @param channel
     * @param cmd
     * @param rcvCrc
     */
    private void handleIncomingCommand(byte moduleAddress, int channel, byte[] cmd, short rcvCrc) {
        ThingUID uid = PHCHelper.getThingUIDreverse(PHCBindingConstants.THING_TYPE_EM, moduleAddress);
        Thing thing = getThingByUID(uid);
        String channelId = "em#" + StringUtils.leftPad(Integer.toString(channel), 2, '0');

        if (thing != null) {
            OnOffType state = OnOffType.OFF;
            if ((cmd[0] & 0x0F) == 2) {
                state = OnOffType.ON;
            }

            logger.debug("{}, {}", thing, state);
            ((PHCHandler) thing.getHandler()).handleIncoming(channelId, state);

        } else {
            logger.info("No Thing with UID {} available.", uid.getAsString());
        }
    }

    /**
     * Send message to to serial port x times.
     *
     * @param msg
     * @param sendCnt
     */
    private void writeMsg(byte[] msg, int sendCnt) {
        while (0 < sendCnt) {
            serialWrite(msg);
            sendCnt--;
        }
    }

    private void serialWrite(byte[] msg) {
        if (logger.isDebugEnabled()) {
            StringBuilder log = new StringBuilder();
            for (byte b : msg) {
                log.append(PHCHelper.byteToBinaryString(b));
                log.append(' ');
            }
            logger.debug("send: {}", log);
        }

        try {
            // write to serial port
            serialPort.setRTS(false);
            serialOut.write(msg);
            serialOut.flush();
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error writing '" + msg + "' to serial port : " + e.getMessage());
        } finally {
            serialPort.setRTS(true);
        }
    }

    /**
     * Adds the given address to the module list.
     *
     * @param module
     */
    public void addModule(byte module) {
        modules.add(module);
    }

    /**
     * Object to save a whole message.
     *
     * @author Jonas Hohaus
     */
    class QueueObject {
        private String moduleType;
        private byte moduleAddress;
        private byte channel;
        private Command command;

        private int counter;

        public QueueObject(String moduleType, String moduleAddress, String channel, Command command) {
            this.moduleType = moduleType;
            this.moduleAddress = Byte.parseByte(moduleAddress, 2);
            this.channel = Byte.parseByte(channel);
            this.command = command;
        }

        public QueueObject(String moduleType, String moduleAddress, String channel, Command command, int counter) {
            this.moduleType = moduleType;
            this.moduleAddress = Byte.parseByte(moduleAddress, 2);
            this.channel = Byte.parseByte(channel);
            this.command = command;
            this.counter = counter;
        }

        public String getModuleType() {
            return moduleType;
        }

        public byte getModuleAddress() {
            return moduleAddress;
        }

        public byte getChannel() {
            return channel;
        }

        public Command getCommand() {
            return command;
        }

        public void increaseCounter() {
            counter++;
        }

        public int getCounter() {
            return counter;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // unnecessary
    }
}
