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
package org.openhab.binding.kaleidescape.internal.discovery;

import static org.openhab.binding.kaleidescape.internal.KaleidescapeBindingConstants.*;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.util.SubnetUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link KaleidescapeDiscoveryService} class allow manual discovery of Kaleidescape components.
 *
 * @author Chris Graham - Initial contribution
 * @author Michael Lobstein - Adapted for the Kaleidescape binding
 * 
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.kaleidescape")
public class KaleidescapeDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(KaleidescapeDiscoveryService.class);
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(
            Arrays.asList(THING_TYPE_PlAYER_ZONE));

    private @Nullable ExecutorService discoverySearchPool;

    @Activate
    public KaleidescapeDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVERY_DEFAULT_TIMEOUT_RATE, DISCOVERY_DEFAULT_AUTO_DISCOVER);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting discovery of Kaleidescape components.");

        try {
            List<String> ipList = getIpAddressScanList();

            discoverySearchPool = Executors.newFixedThreadPool(DISCOVERY_THREAD_POOL_SIZE);

            for (String ip : ipList) {
                discoverySearchPool.execute(new KaleidescapeDiscoveryJob(this, ip));
            }

            discoverySearchPool.shutdown();
        } catch (Exception exp) {
            logger.debug("Kaleidescape discovery service encountered an error while scanning for components: {}",
                    exp.getMessage());
        }

        logger.debug("Completed discovery of Kaleidescape components.");
    }

    /**
     * Create a new Thing with an IP address and Component type given. Uses default port.
     *
     * @param ip IP address of the Kaleidescape component as a string.
     * @param componentType Type of Kaleidescape component as a string.
     * @param friendlyName Name of Kaleidescape component as a string.
     * @param serialNumber Serial Number of Kaleidescape component as a string.
     */
    public void submitDiscoveryResults(String ip, String componentType, String friendlyName, String serialNumber) {
        ThingUID uid = new ThingUID(THING_TYPE_PlAYER_ZONE, serialNumber);

        HashMap<String, Object> properties = new HashMap<>();

        properties.put("host", ip);
        properties.put("port", DEFAULT_API_PORT);
        properties.put("componentType", componentType);

        thingDiscovered(
                DiscoveryResultBuilder.create(uid).withProperties(properties).withLabel(componentType + " (" + friendlyName + ")").build());
    }

    /**
     * Provide a string list of all the IP addresses associated with the network interfaces on
     * this machine.
     *
     * @return String list of IP addresses.
     * @throws UnknownHostException
     * @throws SocketException
     */
    private List<String> getIpAddressScanList() throws UnknownHostException, SocketException {
        List<String> results = new ArrayList<>();

        InetAddress localHost = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);

        for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            InetAddress ipAddress = address.getAddress();

            String cidrSubnet = ipAddress.getHostAddress() + "/" + DISCOVERY_SUBNET_MASK;

            /* Apache Subnet Utils only supports IP v4 for creating string list of IP's */
            if (ipAddress instanceof Inet4Address) {
                logger.info("Found interface IPv4 address to scan: {}", cidrSubnet);

                SubnetUtils utils = new SubnetUtils(cidrSubnet);

                results.addAll(Arrays.asList(utils.getInfo().getAllAddresses()));
            } else if (ipAddress instanceof Inet6Address) {
                logger.info("Found interface IPv6 address to scan: {}", cidrSubnet);
            } else {
                logger.info("Found interface unknown IP type address to scan: {}", cidrSubnet);
            }
        }

        return results;
    }
}
