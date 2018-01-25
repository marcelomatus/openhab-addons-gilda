/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.neeo.internal.discovery;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.io.transport.mdns.MDNSClient;
import org.openhab.io.neeo.NeeoConstants;
import org.openhab.io.neeo.internal.MdnsHelper;
import org.openhab.io.neeo.internal.NeeoApi;
import org.openhab.io.neeo.internal.NeeoUtil;
import org.openhab.io.neeo.internal.ServiceContext;
import org.openhab.io.neeo.internal.models.NeeoSystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * An implementations of {@link BrainDiscovery} that will discovery brains from the MDNS/Zeroconf/Bonjour service
 * announcements
 *
 * @author Tim Roberts - initial contribution
 */
public class MdnsBrainDiscovery extends AbstractBrainDiscovery {

    /** The logger */
    private final Logger logger = LoggerFactory.getLogger(MdnsBrainDiscovery.class);

    /** The lock that controls access to the {@link #systems} set */
    private final Lock systemsLock = new ReentrantLock();

    /** The set of {@link NeeoSystemInfo} that has been discovered */
    private final Map<NeeoSystemInfo, InetAddress> systems = new HashMap<>();

    /** The MDNS listener used. */
    private final ServiceListener mdnsListener = new ServiceListener() {

        @Override
        public void serviceAdded(ServiceEvent event) {
            considerService(event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            removeService(event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            considerService(event.getInfo());
        }

    };

    /** The service context */
    private final ServiceContext context;

    /** The scheduler used to schedule tasks */
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(NeeoConstants.THREAD_POOL_NAME);

    private final Gson gson = new Gson();

    /** The file we store definitions in */
    private final File file = new File(NeeoConstants.FILENAME_DISCOVEREDBRAINS);

    /**
     * Creates the MDNS brain discovery from the given {@link ServiceContext}
     *
     * @param context the non-null service context
     */
    public MdnsBrainDiscovery(ServiceContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        this.context = context;
    }

    /**
     * Starts discovery by
     * <ol>
     * <li>Listening to future service announcements from the {@link MDNSClient}</li>
     * <li>Getting a list of all current announcements</li>
     * </ol>
     *
     * @see org.openhab.io.neeo.internal.BrainDiscovery#startDiscovery()
     */
    @Override
    public void startDiscovery() {
        logger.debug("Starting NEEO Brain MDNS Listener");
        context.getMdnsClient().addServiceListener(NeeoConstants.NEEO_MDNS_TYPE, mdnsListener);

        scheduler.execute(() -> {
            if (file.exists()) {
                try {
                    logger.debug("Reading contents of {}", file.getAbsolutePath());
                    final byte[] contents = Files.readAllBytes(file.toPath());
                    final String json = new String(contents, StandardCharsets.UTF_8);
                    final String[] ipAddresses = gson.fromJson(json, String[].class);
                    if (ipAddresses != null) {
                        for (String ipAddress : ipAddresses) {
                            if (StringUtils.isNotBlank(ipAddress)) {
                                addDiscovered(ipAddress, false);
                            }
                        }
                    }
                } catch (JsonParseException | UnsupportedOperationException e) {
                    logger.debug("JsonParseException reading {}: {}", file.toPath(), e.getMessage(), e);
                } catch (IOException e) {
                    logger.debug("IOException reading {}: {}", file.toPath(), e.getMessage(), e);
                }
            }

            try {
                MdnsHelper.sendQuery();
            } catch (IOException e) {
                logger.error("Exception sending query: {}", e.getMessage(), e);
            }

            for (ServiceInfo info : context.getMdnsClient().list(NeeoConstants.NEEO_MDNS_TYPE)) {
                considerService(info);
            }
        });
    }

    /**
     * Return the brain ID and {@link InetAddress} from the {@link ServiceInfo}
     *
     * @param info the non-null {@link ServiceInfo}
     * @return an {@link Entry} that represents the brain ID and the associated IP address
     */
    private Entry<String, InetAddress> getNeeoBrainInfo(ServiceInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        if (!StringUtils.equals("neeo", info.getApplication())) {
            logger.debug("A non-neeo application was found for the NEEO MDNS: {}", info);
            return null;
        }

        final InetAddress ipAddress = getIpAddress(info);
        if (ipAddress == null) {
            logger.debug("Got a NEEO lookup without an IP address (scheduling a list): {}", info);
            return null;
        }

        String model = info.getPropertyString("hon"); // model
        if (model == null) {
            final String server = info.getServer(); // NEEO-xxxxx.local.
            if (server != null) {
                final int idx = server.indexOf(".");
                if (idx >= 0) {
                    model = server.substring(0, idx);
                }
            }
        }
        if (model == null || model.length() <= 5 || !model.toLowerCase().startsWith("neeo")) {
            logger.debug("No HON or server found to retrieve the model # from: {}", info);
            return null;
        }

        return new AbstractMap.SimpleImmutableEntry<String, InetAddress>(model, ipAddress);
    }

    private void considerService(ServiceInfo info) {
        considerService(info, 1);
    }

    /**
     * Consider whether the {@link ServiceInfo} is for a NEEO brain. We first get the info via
     * {@link #getNeeoBrainInfo(ServiceInfo)} and then attempt to connect to it to retrieve the {@link NeeoSystemInfo}.
     * If successful and the brain has not been already discovered, a
     * {@link #fireDiscovered(NeeoSystemInfo, InetAddress)} is issued.
     *
     * @param info the non-null {@link ServiceInfo}
     */
    private void considerService(ServiceInfo info, int attempts) {
        Objects.requireNonNull(info, "info cannot be null");

        final Entry<String, InetAddress> brainInfo = getNeeoBrainInfo(info);
        if (brainInfo == null) {
            logger.debug("BrainInfo null (ignoring): {}", info);
            return;
        }

        logger.debug("NEEO Brain Found: {} (attempt #{} to get information)", brainInfo.getKey(), attempts);

        if (attempts > 120) {
            logger.debug("NEEO Brain found but couldn't retrieve the system information for {} at {} - giving up!",
                    brainInfo.getKey(), brainInfo.getValue());
            return;
        }

        NeeoSystemInfo sysInfo;
        try {
            sysInfo = NeeoApi.getSystemInfo(brainInfo.getValue().toString());
        } catch (IOException e) {
            // We can get an MDNS notification BEFORE the brain is ready to process.
            // if that happens, we'll get an IOException (usually bad gateway message), schedule another attempt to get
            // the info (rather than lose the notification)
            scheduler.schedule(() -> {
                considerService(info, attempts + 1);
            }, 1, TimeUnit.SECONDS);
            return;
        }

        systemsLock.lock();
        try {
            final InetAddress oldAddr = systems.get(sysInfo);
            final InetAddress newAddr = brainInfo.getValue();
            if (oldAddr == null) {
                systems.put(sysInfo, newAddr);
                fireDiscovered(sysInfo, newAddr);
                save();
            } else if (!oldAddr.equals(newAddr)) {
                fireRemoved(sysInfo);
                systems.put(sysInfo, newAddr);
                fireUpdated(sysInfo, oldAddr, newAddr);
                save();
            } else {
                logger.debug("NEEO Brain {} already registered", brainInfo.getValue());
            }
        } finally {
            systemsLock.unlock();
        }
    }

    @Override
    public boolean addDiscovered(String ipAddress) {
        return addDiscovered(ipAddress, true);
    }

    /**
     * Adds a discovered IP address and optionally saving it to the brain's discovered file
     *
     * @param ipAddress a non-null, non-empty IP address
     * @param save true to save changes, false otherwise
     * @return true if discovered, false otherwise
     */
    private boolean addDiscovered(String ipAddress, boolean save) {
        NeeoUtil.requireNotEmpty(ipAddress, "ipAddress cannot be empty");

        try {
            final InetAddress addr = InetAddress.getByName(ipAddress);
            final NeeoSystemInfo sysInfo = NeeoApi.getSystemInfo(ipAddress);
            logger.debug("Manually adding brain ({}) with system information: {}", ipAddress, sysInfo);

            systemsLock.lock();
            try {
                final InetAddress oldAddr = systems.get(sysInfo);

                systems.put(sysInfo, addr);

                if (oldAddr == null) {
                    fireDiscovered(sysInfo, addr);
                } else {
                    fireUpdated(sysInfo, oldAddr, addr);
                }
                if (save) {
                    save();
                }
            } finally {
                systemsLock.unlock();
            }

            return true;
        } catch (IOException e) {
            logger.debug("Tried to manually add a brain ({}) but an exception occurred: {}", ipAddress, e.getMessage(),
                    e);
            return false;
        }
    }

    @Override
    public boolean removeDiscovered(NeeoSystemInfo sysInfo) {
        Objects.requireNonNull(sysInfo, "sysInfo cannot be null");
        systemsLock.lock();
        try {
            if (systems.containsKey(sysInfo)) {
                systems.remove(sysInfo);
                fireRemoved(sysInfo);
                save();
                return true;
            } else {
                logger.debug("Cannot remove NEEO Brain {} - unknown", sysInfo);
                return false;
            }
        } finally {
            systemsLock.unlock();
        }
    }

    /**
     * Removes the service. If the info represents a brain we already discovered, a {@link #fireRemoved(NeeoSystemInfo)}
     * is issued.
     *
     * @param info the non-null {@link ServiceInfo}
     */
    private void removeService(ServiceInfo info) {
        Objects.requireNonNull(info, "info cannot be null");

        final Entry<String, InetAddress> brainInfo = getNeeoBrainInfo(info);
        if (brainInfo == null) {
            return;
        }

        systemsLock.lock();
        try {
            NeeoSystemInfo foundInfo = null;
            for (NeeoSystemInfo existingSysInfo : systems.keySet()) {
                if (StringUtils.equals(existingSysInfo.getHostname(), brainInfo.getKey())) {
                    foundInfo = existingSysInfo;
                    break;
                }
            }
            if (foundInfo != null) {
                fireRemoved(foundInfo);
                systems.remove(foundInfo);
                save();
            }
        } finally {
            systemsLock.unlock();
        }
    }

    /**
     * Saves the current brains to the {@link #file}. Any {@link IOException} will be logged and ignored.
     */
    private void save() {
        logger.debug("Saving brain's discovered to {}", file.toPath());

        systemsLock.lock();
        try {
            // ensure full path exists
            file.getParentFile().mkdirs();

            final List<String> ipAddresses = systems.values().stream().map(e -> e.getHostAddress())
                    .collect(Collectors.toList());

            final String json = gson.toJson(ipAddresses);
            final byte[] contents = json.getBytes(StandardCharsets.UTF_8);
            Files.write(file.toPath(), contents);
        } catch (IOException e) {
            logger.debug("IOException writing {}: {}", file.toPath(), e.getMessage(), e);
        } finally {
            systemsLock.unlock();
        }
    }

    /**
     * Get's the IP address from the given service
     *
     * @param service the non-null {@link ServiceInfo}
     * @return the ip address of the service or null if not found
     */
    private InetAddress getIpAddress(ServiceInfo service) {
        Objects.requireNonNull(service, "service cannot be null");

        for (String addr : service.getHostAddresses()) {
            try {
                return InetAddress.getByName(addr);
            } catch (UnknownHostException e) {
                // ignore
            }
        }

        InetAddress address = null;
        for (InetAddress addr : service.getInet4Addresses()) {
            return addr;
        }
        // Fallback for Inet6addresses
        for (InetAddress addr : service.getInet6Addresses()) {
            return addr;
        }
        return address;
    }

    /**
     * Simply unregisters from the {@link MDNSClient}
     *
     * @see org.openhab.io.neeo.internal.BrainDiscovery#close()
     */
    @Override
    public void close() {
        context.getMdnsClient().unregisterAllServices();
        save();
        systems.clear();
        logger.debug("Stopped NEEO Brain MDNS Listener");
    }

}
