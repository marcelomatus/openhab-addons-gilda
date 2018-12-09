/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradoxalarm.internal.communication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openhab.binding.paradoxalarm.internal.communication.messages.EpromRequestPayload;
import org.openhab.binding.paradoxalarm.internal.communication.messages.HeaderMessageType;
import org.openhab.binding.paradoxalarm.internal.communication.messages.IPPacketPayload;
import org.openhab.binding.paradoxalarm.internal.communication.messages.ParadoxIPPacket;
import org.openhab.binding.paradoxalarm.internal.communication.messages.RamRequestPayload;
import org.openhab.binding.paradoxalarm.internal.model.ZoneStateFlags;
import org.openhab.binding.paradoxalarm.internal.util.ParadoxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EvoCommunicator} is responsible for handling communication to Evo192 alarm system via IP150 interface.
 *
 * @author Konstantin_Polihronov - Initial contribution
 */
public class EvoCommunicator extends GenericCommunicator implements IParadoxCommunicator {

    protected static Logger logger = LoggerFactory.getLogger(EvoCommunicator.class);

    private MemoryMap memoryMap;

    public EvoCommunicator(String ipAddress, int tcpPort, String ip150Password, String pcPassword) throws Exception {
        super(ipAddress, tcpPort, ip150Password, pcPassword);
        initializeMemoryMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see mainApp.ParadoxAdapter#readPartitions()
     */
    @Override
    public List<String> readPartitionLabels() {
        List<String> result = new ArrayList<>();

        try {
            for (int i = 1; i <= 8; i++) {
                result.add(readPartitionLabel(i));
            }
        } catch (Exception e) {
            logger.debug("Unable to retrieve partition labels.\nException: " + e.getMessage());
        }
        return result;
    }

    public String readPartitionLabel(int partitionNo) throws Exception {
        logger.debug("Reading partition label: " + partitionNo);
        if (partitionNo < 1 || partitionNo > 8) {
            throw new Exception("Invalid partition number. Valid values are 1-8.");
        }

        int address = 0x3A6B + (partitionNo - 1) * 107;
        byte labelLength = 16;

        byte[] payloadResult = readEepromMemory(address, labelLength);

        String result = createString(payloadResult);
        logger.debug("Partition label: {}", result);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see mainApp.ParadoxAdapter#readZones()
     */
    @Override
    public List<String> readZoneLabels() {
        List<String> result = new ArrayList<>();

        try {
            for (int i = 1; i <= 60; i++) {
                result.add(readZoneLabel(i));
            }
        } catch (Exception e) {
            logger.debug("Unable to retrieve zone labels.\nException: " + e.getMessage());
        }
        return result;
    }

    public String readZoneLabel(int zoneNumber) throws Exception {
        logger.debug("Reading zone label: " + zoneNumber);
        if (zoneNumber < 1 || zoneNumber > 192) {
            throw new Exception("Invalid zone number. Valid values are 1-192.");
        }

        byte labelLength = 16;

        int address;
        if (zoneNumber <= 96) {
            address = 0x430 + (zoneNumber - 1) * 16;
        } else {
            address = 0x62F7 + (zoneNumber - 97) * 16;
        }

        byte[] payloadResult = readEepromMemory(address, labelLength);

        String result = createString(payloadResult);
        logger.debug("Zone label: " + result);
        return result;
    }

    @Override
    public List<byte[]> readPartitionFlags() {
        List<byte[]> result = new ArrayList<byte[]>();

        byte[] element = memoryMap.getElement(2);
        byte[] firstBlock = Arrays.copyOfRange(element, 32, 64);

        element = memoryMap.getElement(3);
        byte[] secondBlock = Arrays.copyOfRange(element, 0, 16);
        byte[] mergeByteArrays = ParadoxUtil.mergeByteArrays(firstBlock, secondBlock);
        for (int i = 0; i < mergeByteArrays.length; i += 6) {
            result.add(Arrays.copyOfRange(mergeByteArrays, i, i + 6));
        }

        return result;
    }

    @Override
    public ZoneStateFlags readZoneStateFlags() {
        ZoneStateFlags result = new ZoneStateFlags();

        byte[] firstPage = memoryMap.getElement(0);
        byte[] secondPage = memoryMap.getElement(8);

        byte[] firstBlock = Arrays.copyOfRange(firstPage, 28, 40);
        byte[] secondBlock = Arrays.copyOfRange(secondPage, 0, 22);
        byte[] zonesOpened = ParadoxUtil.mergeByteArrays(firstBlock, secondBlock);
        result.setZonesOpened(zonesOpened);

        firstBlock = Arrays.copyOfRange(firstPage, 40, 52);
        secondBlock = Arrays.copyOfRange(secondPage, 22, 34);
        byte[] zonesTampered = ParadoxUtil.mergeByteArrays(firstBlock, secondBlock);
        result.setZonesTampered(zonesTampered);

        firstBlock = Arrays.copyOfRange(firstPage, 52, 52);
        secondBlock = Arrays.copyOfRange(secondPage, 34, 64);
        byte[] zonesLowBattery = ParadoxUtil.mergeByteArrays(firstBlock, secondBlock);
        result.setZonesLowBattery(zonesLowBattery);

        return result;
    }

    public void initializeMemoryMap() throws Exception {
        List<byte[]> ramCache = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            logger.debug("Reading memory page number: {}", i);
            ramCache.add(readRAMBlock(i));
        }
        ramCache.add(readRAMBlock(0x10));
        memoryMap = new MemoryMap(ramCache);
    }

    @Override
    public void refreshMemoryMap() throws Exception {
        if (isOnline()) {
            for (int i = 1, j = 0; i <= 16; i++, j++) {
                logger.trace("Reading memory page number: {}", i);
                memoryMap.updateElement(j, readRAMBlock(i));
            }
        }
    }

    public byte[] readRAMBlock(int blockNo) throws Exception {
        if (isOnline) {
            return readRAM(blockNo, (byte) 64);
        } else {
            return new byte[0];
        }
    }

    public byte[] readRAM(int blockNo, byte bytesToRead) throws Exception {
        IPPacketPayload payload = new RamRequestPayload(blockNo, bytesToRead);
        return readMemory(payload);
    }

    private byte[] readEepromMemory(int address, byte bytesToRead) throws Exception {
        if (bytesToRead < 1 || bytesToRead > 64) {
            throw new Exception("Invalid bytes to read. Valid values are 1 to 64.");
        }

        IPPacketPayload payload = new EpromRequestPayload(address, bytesToRead);
        return readMemory(payload);
    }

    private byte[] readMemory(IPPacketPayload payload) throws Exception {
        ParadoxIPPacket readEpromIPPacket = new ParadoxIPPacket(payload)
                .setMessageType(HeaderMessageType.SERIAL_PASSTHRU_REQUEST).setUnknown0((byte) 0x14);

        sendPacket(readEpromIPPacket);
        return receivePacket((byte) 0x5);
    }

    /// <summary>
    /// This method reads data from the IP150 module. It can return multiple
    /// responses
    /// e.g. a live event is combined with another response.
    /// </summary>
    /// <param name="networkStream">The open active TCP/IP stream.</param>
    /// <param name="command">A panel command, e.g. 0x5 (read memory)</param>
    /// <returns>An array of an array of the raw bytes received from the TCP/IP
    /// stream.</returns>
    private byte[] receivePacket(byte command) throws IOException, InterruptedException {
        if (command > 0xF) {
            command = ParadoxUtil.getHighNibble(command);
        }

        byte retryCounter = 0;

        // We might enter this too early, meaning the panel has not yet had time to
        // respond
        // to our command. We add a retry counter that will wait and retry.
        while (retryCounter < 3) {
            byte[] packetResponse = receivePacket();
            List<byte[]> responses = splitResponsePackets(packetResponse);
            for (byte[] response : responses) {
                // Message too short
                if (response.length < 17) {
                    continue;
                }

                // Response command (after header) is not related to reading memory
                if (ParadoxUtil.getHighNibble(response[16]) != command) {
                    continue;
                }

                return Arrays.copyOfRange(response, 22, response.length - 1);
            }

            // Give the panel time to send us a response
            Thread.sleep(100);

            retryCounter++;
        }

        logger.error("Failed to receive data for command 0x{0:X}", command);
        return null;
    }

    private List<byte[]> splitResponsePackets(byte[] response) {
        List<byte[]> packets = new ArrayList<byte[]>();
        byte[] responseCopy = Arrays.copyOf(response, response.length);
        try {
            int totalLength = responseCopy.length;
            while (responseCopy.length > 0) {
                if (responseCopy.length < 16 || responseCopy[0] != (byte) 0xAA) {
                    // throw new Exception("No 16 byte header found");
                    logger.debug("No 16 byte header found");
                }

                byte[] header = Arrays.copyOfRange(responseCopy, 0, 16);
                byte messageLength = header[1];

                // Remove the header
                responseCopy = Arrays.copyOfRange(responseCopy, 16, totalLength);

                if (responseCopy.length < messageLength) {
                    throw new Exception("Unexpected end of data");
                }

                // Check if there's padding bytes (0xEE)
                if (responseCopy.length > messageLength) {
                    for (int i = messageLength; i < responseCopy.length; i++) {
                        if (responseCopy[i] == 0xEE) {
                            messageLength++;
                        } else {
                            break;
                        }
                    }
                }

                byte[] message = Arrays.copyOfRange(responseCopy, 0, messageLength);

                responseCopy = Arrays.copyOfRange(responseCopy, messageLength, responseCopy.length);

                packets.add(ParadoxUtil.mergeByteArrays(header, message));
            }
        } catch (Exception ex) {
            logger.error("Exception occurred: {}", ex.getMessage());
        }

        return packets;
    }

    private String createString(byte[] payloadResult) throws UnsupportedEncodingException {
        return new String(payloadResult, "US-ASCII");
    }

    @Override
    public void executeCommand(String command) {
        try {
            IP150Command ip150Command = IP150Command.valueOf(command);
            switch (ip150Command) {
                case LOGIN:
                    loginSequence();
                    return;
                case LOGOUT:
                    logoutSequence();
                    return;
                case RESET:
                    close();
                    loginSequence();
                    return;
                default:
                    logger.error("Command {} not implemented.", command);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error while executing command {}. Exception:{}", command, e);
        }
    }
}
