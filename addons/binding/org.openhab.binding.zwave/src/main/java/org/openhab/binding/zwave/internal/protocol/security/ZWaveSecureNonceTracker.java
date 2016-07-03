/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.security;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageType;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveSecurityCommandClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nonces (one time use tokens) are used quite heavily in zwave secure encapsulated messages.
 * This class is in charge of storing:
 * 1) The nonces we generate and send to the device so it can encapsulate incoming messages
 * 2) The nonces we receive from the device to encapsulate outgoing messages
 *
 * Temporary storage is required in both cases. Each nonce also has suggested timeouts per the
 * zwave spec, those timeouts are also tracked in this class
 *
 * @author Dave Badia
 * @author Chris Jackson
 *
 */
public class ZWaveSecureNonceTracker {
    private static final Logger logger = LoggerFactory.getLogger(ZWaveSecureNonceTracker.class);

    /**
     * Should be set to true
     *
     * The code from which this was based included numerous bad security practices (hardcoded IVs, seeding of PRNG
     * with timestamp).
     *
     * It is unknown as to whether that logic was necessary to work around device defects or if it was just by mistake.
     *
     * Setting this to false will use the bad security practices from the original code. true will use accepted security
     * best practices
     *
     * TODO: Package-protected visible for test case use
     */
    public static boolean USE_SECURE_CRYPTO_PRACTICES = true;

    /**
     * It's a security best practice to periodically re-seed our random number
     * generator
     * http://www.cigital.com/justice-league-blog/2009/08/14/proper-use-of-javas-securerandom/
     */
    private static final long SECURE_RANDOM_RESEED_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(1);

    private final ZWaveNode node;

    /**
     * Nonces generated by us and sent to the device in a {@link ZWaveSecurityCommandClass#SECURITY_NONCE_REPORT}
     * message
     * Used in the decryption process to process incoming SECURITY messages
     */
    private NonceTable ourNonceTable = new NonceTable();

    /**
     * Nonces generated by the device and sent to us in a {@link ZWaveSecurityCommandClass#SECURITY_NONCE_REPORT}
     * message.
     * Used in the encryption process for outgoing SECURITY messages
     */
    private DeviceNonceTable deviceNonceTable = new DeviceNonceTable();

    /**
     * Timer to track time elapsed between sending {@link ZWaveSecurityCommandClass#SECURITY_NONCE_GET} and
     * receiving {@link #SECURITY_NONCE_REPORT}. If too
     * much time elapses we should request a new nonce. This timer is optional
     * but recommended
     */
    private NonceTimer requestNonceTimer = null;

    /**
     * The last nonce request message sent to the device. We implement {@link ZWaveEventListener} since
     * the time from which we generate the nonce request message to the time we actually send it can
     * be a significant delay, especially during secure inclusion
     */
    private SerialMessage requestNonceMessage = null; // TODO: DB what was the point of this?

    private SecureRandom secureRandom = null;

    private long reseedAt = 0L;

    public ZWaveSecureNonceTracker(ZWaveNode node) {
        this.node = node;
    }

    /**
     * @return a useable {@link Nonce} or null if none are available
     */
    public synchronized Nonce getUseableDeviceNonce() {
        Nonce nonce = deviceNonceTable.getDeviceNonceToEncryptMessage();
        logger.debug("NODE {}: getUseableDeviceNonce returning {}", node.getNodeId(), nonce);
        return nonce;
    }

    /**
     * @return true if a nonce has been requested from the node and a reply is pending
     */
    private synchronized boolean hasNonceBeenRequested() {
        logger.debug("NODE {}: getUseableDeviceNonce() requestNonceTimer={}", node.getNodeId(), requestNonceTimer);
        if (requestNonceTimer != null && !requestNonceTimer.isExpired()) {
            return true;
        } else {
            requestNonceTimer = null;
            return false;
        }
    }

    public synchronized SerialMessage buildNonceGetIfNeeded() {
        if (hasNonceBeenRequested()) {
            logger.debug("NODE {}: already waiting for nonce", node.getNodeId());
            return null;
        }
        logger.debug("NODE {}: requesting nonce", node.getNodeId());
        SerialMessage message = new SerialMessage(node.getNodeId(), SerialMessageClass.SendData,
                SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler,
                ZWaveSecurityCommandClass.SECURITY_MESSAGE_PRIORITY);
        byte[] payload = { (byte) node.getNodeId(), 2,
                (byte) ZWaveSecurityCommandClass.getSecurityCommandClass().getKey(),
                ZWaveSecurityCommandClass.SECURITY_NONCE_GET, };
        if (ZWaveSecurityCommandClass.OVERRIDE_DEFAULT_TRANSMIT_OPTIONS) {
            logger.trace("NODE {}: Using custom transmit options", node.getNodeId());
            message.setTransmitOptions(
                    ZWaveController.TRANSMIT_OPTION_ACK | ZWaveController.TRANSMIT_OPTION_AUTO_ROUTE);
        }
        // We only try once as strange things happen with NONCE_GET requests TODO: DB add more detail as to what we are
        // trying to fix here
        message.setMessagePayload(payload);
        if (requestNonceTimer != null) {
            logger.warn("NODE {}: requestNonceTimer != null but generating a new request", node.getNodeId());
        }
        requestNonceTimer = new NonceTimer(NonceTimerType.REQUESTED, node);
        requestNonceMessage = message;
        return message;
    }

    /**
     * Generate a new nonce, then build a SECURITY_NONCE_REPORT
     *
     * TODO: Move the generation of the message to the command class - just deal with the nonce in this class!
     */
    public SerialMessage generateAndBuildNonceReport() {
        Nonce nonce = ourNonceTable.generateNewUniqueNonceForDevice();

        // SECURITY_NONCE_REPORT gets immediate priority
        SerialMessage message = new SerialMessage(node.getNodeId(), SerialMessageClass.SendData,
                SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler,
                SerialMessagePriority.Immediate);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) node.getNodeId());
        baos.write((byte) 10);
        baos.write((byte) ZWaveSecurityCommandClass.getSecurityCommandClass().getKey());
        baos.write(ZWaveSecurityCommandClass.SECURITY_NONCE_REPORT);
        try {
            baos.write(nonce.getNonceBytes());
            message.setMessagePayload(baos.toByteArray());
            if (ZWaveSecurityCommandClass.OVERRIDE_DEFAULT_TRANSMIT_OPTIONS) {
                logger.trace("NODE {}: Using custom transmit options", node.getNodeId());
                message.setTransmitOptions(
                        ZWaveController.TRANSMIT_OPTION_ACK | ZWaveController.TRANSMIT_OPTION_AUTO_ROUTE);
            }
        } catch (IOException e) {
            logger.error("NODE {}: Error during Security sendNonceReport.", node.getNodeId(), e);
            return null;
        }
        return message;
    }

    /**
     * Called by {@link ZWaveSecurityCommandClass} so the nonce tracker is aware that
     * a nonce request is being sent via SECURITY_MESSAGE_ENCAP_NONCE_GET and our timer should be started
     */
    public synchronized void sendingEncapNonceGet(SerialMessage message) {
        // No requestNonceTimer != null check since this will be called multiple times for teh same
        requestNonceTimer = new NonceTimer(NonceTimerType.REQUESTED, node);
        requestNonceMessage = message;
    }

    public synchronized void receivedNonceFromDevice(byte[] nonceBytes) {
        if (requestNonceTimer == null) {
            logger.warn("NODE {}: SECURITY_ERROR Nonce was received, but we have no requestNonceTimer.",
                    node.getNodeId());
        } else if (requestNonceTimer.isExpired()) {
            // The nonce was not received within the alloted time of us sending the nonce request. Send it again
            logger.warn("NODE {}: SECURITY_ERROR Nonce was not received within {}ms, a new one will be requested.",
                    node.getNodeId(), NonceTimerType.REQUESTED.validityInMillis);
            // The ZWaveSecurityEncapsulationThread will request a new one for us
            return;
        } else {
            logger.debug("NODE {}: receivedNonceFromDevice nonce received. Stopping requestNonceTimer",
                    node.getNodeId());
            requestNonceTimer = null;
        }
        deviceNonceTable.addNonceFromDevice(nonceBytes);
    }

    public Nonce getNonceWeGeneratedById(byte nonceId) {
        Nonce nonce = ourNonceTable.getNonceById(nonceId);
        return nonce;
    }

    /**
     * Generates a nonce that isn't stored anywhere
     */
    public byte[] generateNonceForEncapsulationMessage() {
        return generateNonceBytes();
    }

    private byte[] generateNonceBytes() {
        if (!USE_SECURE_CRYPTO_PRACTICES) {
            return Nonce.INSECURE_NONCE_BYTES;
        }
        if (System.currentTimeMillis() > reseedAt) {
            secureRandom = createNewSecureRandom();
            reseedAt = System.currentTimeMillis() + SECURE_RANDOM_RESEED_INTERVAL_MILLIS;
        }
        byte[] nonceBytes = new byte[8];
        secureRandom.nextBytes(nonceBytes);
        return nonceBytes;
    }

    private static SecureRandom createNewSecureRandom() {
        SecureRandom secureRandom = null;
        // SecureRandom advice taken from
        // http://www.cigital.com/justice-league-blog/2009/08/14/proper-use-of-javas-securerandom/
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        } catch (GeneralSecurityException e) {
            secureRandom = new SecureRandom();
        }
        // force an internal seeding
        secureRandom.nextBoolean();
        // Add some entropy of our own to the seed
        secureRandom.setSeed(Runtime.getRuntime().freeMemory());
        for (File root : File.listRoots()) {
            secureRandom.setSeed(root.getUsableSpace());
        }
        return secureRandom;
    }

    /* -------------- Begin inner classes -------------- */

    /**
     * The type of Nonce
     */
    private static enum NonceTimerType {
        /**
         * Optional but recommended, so we implement it.
         * Is triggered when we send a {@link ZWaveSecurityCommandClass#SECURITY_NONCE_GET}
         */
        REQUESTED(TimeUnit.SECONDS.toMillis(20)), // 20 seconds since this is optional anyway

        /**
         * Required and is triggered when we generate a nonce to send to the device via a
         * {@link ZWaveSecurityCommandClass#SECURITY_NONCE_REPORT}.
         * Represents how long the device has to use the nonce we sent from the time we generated it (NOT the time we
         * received the ack). min=3, recommended=10, max=20
         */
        GENERATED(TimeUnit.SECONDS.toMillis(10)),
        /**
         * Is used to estimate if a nonce we received from a device is still useful. We have no way of knowing for sure,
         * as nonces can be valid for as little as 3 but as many as 20 seconds. Also, the devices timer starts when it
         * sends the nonce, not when we get it. So slow transmission time can also cause the nonce to be unusable.
         *
         */
        // TODO: DB track if nonce used are from ENCAP_GET_NONCE, and if those keep failing, disable the use
        // of them since the device has a short timer
        RECEIVED(TimeUnit.SECONDS.toMillis(5)), // 5 seconds is our best guess

        /**
         * No timer required. Typically used when we generate a nonce to include in a
         * {@link ZWaveSecurityCommandClass#SECURITY_MESSAGE_ENCAP} or
         * {@link ZWaveSecurityCommandClass#SECURITY_MESSAGE_ENCAP_NONCE_GET} message
         */
        NONE(Long.MAX_VALUE);

        private final long generatedAt = System.currentTimeMillis();
        private final long validityInMillis;

        private NonceTimerType(long validityInMillis) {
            this.validityInMillis = validityInMillis;
        }

        private long computeExpiresAt() {
            return System.currentTimeMillis() + validityInMillis;
        }
    }

    /**
     * per the spec we must track how long it has been since we sent a nonce and only allow it's use within a specified
     * time period.
     */
    static class NonceTimer {
        private NonceTimerType type;
        private long expiresAt;
        private int nodeId;

        NonceTimer(NonceTimerType type, ZWaveNode node) {
            this.type = type;
            this.nodeId = node.getNodeId();
            reset();
        }

        void reset() {
            expiresAt = type.computeExpiresAt();
        }

        private long getExpiresAt() {
            return expiresAt;
        }

        /**
         * @return ms left before this nonce expires, or a negative number if
         *         it has already expired
         */
        private long getTimeLeft() {
            return expiresAt - System.currentTimeMillis();
        }

        private boolean isExpired() {
            long now = System.currentTimeMillis();
            boolean expired = getTimeLeft() < 0;
            if (logger.isTraceEnabled()) {
                DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                logger.trace("NODE {}: expiresAt={}, now={}, expired={}", nodeId, dateFormatter.format(expiresAt),
                        dateFormatter.format(now), expired);
            }
            return expired;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("NonceTimer [type=").append(type).append(" expired=").append(isExpired())
                    .append(" getTimeLeft=").append(getTimeLeft()).append("]");
            return builder.toString();
        }
    }

    /**
     * Class to hold the nonce itself and it's related data
     */
    public static class Nonce {
        private static final byte[] INSECURE_NONCE_BYTES = new byte[] { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA,
                (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, };
        private byte[] nonceBytes;
        private NonceTimer timer;
        private byte nonceId;

        /**
         * Generates a nonce to be sent to a device in
         * a {@link ZWaveSecurityCommandClass#SECURITY_NONCE_REPORT} message
         *
         * @param nonceBytes
         * @param timer the timer should be used, can be null
         */
        protected Nonce(byte[] nonceBytes, NonceTimer timer) {
            super();
            if (nonceBytes == null || nonceBytes.length != 8) {
                throw new IllegalArgumentException("Invalid nonce length for " + Arrays.toString(nonceBytes));
            }
            this.nonceBytes = nonceBytes;
            this.nonceId = nonceBytes[0];
            this.timer = timer;
        }

        public byte[] getNonceBytes() {
            return nonceBytes;
        }

        /**
         * @return the timer or null if none was used
         */
        private NonceTimer getTimer() {
            return timer;
        }

        private byte getNonceId() {
            return nonceId;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("Nonce ");
            if (timer != null) {
                buf.append(timer.type).append("   ");
            }
            buf.append(SerialMessage.bb2hex(nonceBytes));
            if (timer != null) {
                buf.append("; time left=").append(timer.getTimeLeft());
            }
            return buf.toString();
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(nonceBytes);
            result = prime * result + nonceId;
            result = prime * result + ((timer == null) ? 0 : timer.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Nonce other = (Nonce) obj;
            if (!Arrays.equals(nonceBytes, other.nonceBytes)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Data store to hold the nonces we have generated and
     * provide a method to cleanup old nonces
     *
     */
    private class NonceTable {
        /**
         * Store nonces that we generated but have not been retrieved yet here
         */
        private Map<Byte, Nonce> table = new ConcurrentHashMap<Byte, Nonce>();

        /**
         * Once a nonce is used (that is, we get a {@link ZWaveSecurityCommandClass#SECURITY_MESSAGE_ENCAP}
         * with the nonce id of a nonce) it is removed from {@link #table}. But the nonce's ID (1st byte)
         * is stored here
         */
        private SizeLimitedQueue<Byte> usedNonceIdList = new SizeLimitedQueue(10);

        /**
         * When {@link #cleanup()} finds an expired nonce, it's remove from {@link #table} and
         * it's nonce id gets stored here
         */
        private SizeLimitedQueue<Byte> expiredNonceIdList = new SizeLimitedQueue(10);

        private NonceTable() {
            super();
        }

        private Nonce generateNewUniqueNonceForDevice() {
            byte[] nonceBytes = generateNonceBytes();
            boolean unique = false;
            while (!unique) { // Collision, try again
                nonceBytes = generateNonceBytes();
                // Make sure the id is unique for all nonces in storage
                // Can't have duplicate 1st bytes since that is the nonce ID
                unique = ourNonceTable.getNonceById(nonceBytes[0]) == null && !usedNonceIdList.contains(nonceBytes[0])
                        && !expiredNonceIdList.contains(nonceBytes[0]);
            }
            Nonce nonce = new Nonce(nonceBytes, new NonceTimer(NonceTimerType.GENERATED, node));
            logger.debug("NODE {}: Generated new nonce for device: {}", node.getNodeId(),
                    SerialMessage.bb2hex(nonce.getNonceBytes()));
            table.put(nonce.getNonceId(), nonce);
            return nonce;
        }

        private Nonce getNonceById(byte id) {
            // Nonces can only be used once so remove it
            Nonce nonce = table.remove(id);
            if (nonce != null) {
                usedNonceIdList.add(nonce.getNonceId());
                logger.debug("NODE {}: Device message contained nonce id of id={}, found matching nonce of: {}",
                        node.getNodeId(), id, nonce);
            } else if (expiredNonceIdList.contains(id)) {
                logger.error("NODE {}: SECURITY_ERROR Device message contained expired nonce id={}.", node.getNodeId(),
                        id);
            } else if (usedNonceIdList.contains(id)) {
                logger.error("NODE {}: SECURITY_ERROR Device message contained nonce that was previously used, id={}.",
                        node.getNodeId(), id);
            } else {
                logger.error("NODE {}: SECURITY_ERROR Device message contained nonce that is unknown to us, id={}.",
                        node.getNodeId(), id);
                logger.debug("NODE {}: Nonce id={} table={}, expiredList={}, usedList={}", node.getNodeId(), id, table,
                        expiredNonceIdList, usedNonceIdList);
            }
            cleanup();
            return nonce;
        }

        /**
         * Remove any expired nonces from our table
         */
        private void cleanup() {
            Iterator<Entry<Byte, Nonce>> iter = table.entrySet().iterator();
            while (iter.hasNext()) {
                Nonce nonce = iter.next().getValue();
                if (nonce.getTimer() != null) {
                    // Wait an extra 10 seconds after we send the nonce to the device for
                    // it to come back and be used
                    long removeAt = nonce.getTimer().getExpiresAt() + 10000;
                    if (System.currentTimeMillis() > removeAt) {
                        logger.warn("NODE {}: Expiring nonce with id={}", node.getNodeId(), nonce.getNonceId());
                        iter.remove();
                        expiredNonceIdList.add(nonce.getNonceId());
                    }
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("NonceTable: [");
            for (Nonce nonce : table.values()) {
                buf.append(nonce.toString()).append("    ");
            }
            buf.append("]");
            return buf.toString();
        }
    }

    /**
     * Data store to hold nonces generated by the device and sent to us
     * in a {@link ZWaveSecurityCommandClass#SECURITY_NONCE_REPORT} message.
     * Used in the encryption process for outgoing SECURITY messages
     */
    private class DeviceNonceTable {
        private Map<Byte, Nonce> table = new ConcurrentHashMap<Byte, Nonce>();
        private ConcurrentSkipListMap<Long, Nonce> timeToNonceMap = new ConcurrentSkipListMap<Long, Nonce>();

        private DeviceNonceTable() {
            super();
        }

        private void addNonceFromDevice(byte[] nonceBytes) {
            Nonce deviceNonce = new Nonce(nonceBytes, new NonceTimer(NonceTimerType.RECEIVED, node));
            table.put(deviceNonce.getNonceId(), deviceNonce);
            timeToNonceMap.put(deviceNonce.getTimer().getExpiresAt(), deviceNonce);
        }

        private Nonce getDeviceNonceToEncryptMessage() {
            logger.debug("NODE {}: getDeviceNonceToEncryptMessage start deviceNonceTable={}, timeToNonceMap={}",
                    node.getNodeId(), deviceNonceTable, timeToNonceMap);
            cleanup();
            logger.debug("NODE {}: getDeviceNonceToEncryptMessage post cleanup deviceNonceTable={}, timeToNonceMap={}",
                    node.getNodeId(), deviceNonceTable, timeToNonceMap);
            Iterator<Nonce> iter = timeToNonceMap.values().iterator();
            if (iter.hasNext()) {
                Nonce nonce = iter.next();
                logger.debug("NODE {}: getDeviceNonceToEncryptMessage returning DeviceNonce={}", node.getNodeId(),
                        nonce);
                iter.remove(); // Remove it since we are using it
                return nonce;
            } else {
                return null;
            }
        }

        /**
         * Remove any expired nonces from our table
         */
        private void cleanup() {
            Iterator<Entry<Byte, Nonce>> iter = table.entrySet().iterator();
            while (iter.hasNext()) {
                Nonce nonce = iter.next().getValue();
                if (nonce.getTimer() != null && nonce.getTimer().isExpired()) {
                    logger.warn("NODE {}: Expiring nonce with id={}", node.getNodeId(), nonce.getNonceId());
                    iter.remove();
                    // Remove the nonce from timeToNonceIdMap
                    byte nonceId = nonce.getNonceId();
                    Iterator<Map.Entry<Long, Nonce>> iter2 = timeToNonceMap.entrySet().iterator();
                    while (iter2.hasNext()) {
                        Map.Entry<Long, Nonce> entry = iter2.next();
                        if (nonceId == entry.getValue().getNonceId()) {
                            iter2.remove();
                        }
                    }
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("NonceTable: [");
            for (Nonce nonce : table.values()) {
                buf.append(nonce.toString()).append("    ");
            }
            buf.append("]");
            return buf.toString();
        }
    }

    /**
     * To ease nonce error condition tracking, we keep nonce values after they expire or are used.
     * Limit the number we keep to avoid a memory leak
     *
     * code from https://stackoverflow.com/questions/5498865/size-limited-queue-that-holds-last-n-elements-in-java
     *
     * @author Dave Badia
     */
    private class SizeLimitedQueue<E> extends LinkedList<E> {
        private int limit;

        public SizeLimitedQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(E o) {
            super.add(o);
            while (size() > limit) {
                super.remove();
            }
            return true;
        }
    }
}
