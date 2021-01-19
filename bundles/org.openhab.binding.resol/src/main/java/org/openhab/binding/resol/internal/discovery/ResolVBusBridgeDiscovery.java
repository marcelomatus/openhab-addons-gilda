/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.resol.internal.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.resol.internal.ResolBindingConstants;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.resol.vbus.TcpDataSource;
import de.resol.vbus.TcpDataSourceProvider;

/**
 * The {@link ResolVBusBridgeDiscovery} class provides the DiscoverySerivce to
 * discover Resol VBus-LAN adapters
 *
 * @author Raphael Mack - Initial contribution
 */
@Component(service = DiscoveryService.class)
@NonNullByDefault
public class ResolVBusBridgeDiscovery extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(ResolVBusBridgeDiscovery.class);

    public ResolVBusBridgeDiscovery() throws IllegalArgumentException {
        super(ResolBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS, 35, false);
    }

    @Override
    protected void startScan() {
        scheduler.execute(this::searchRunnable);
    }

    /*
     * The runnable for the search routine.
     */
    public void searchRunnable() {
        try {
            InetAddress broadcastAddress = InetAddress
                    .getByAddress(new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 });

            TcpDataSource[] dataSources = TcpDataSourceProvider.discoverDataSources(broadcastAddress, 3, 500, false);

            HashMap<String, TcpDataSource> currentDataSourceById = new HashMap<String, TcpDataSource>();
            for (TcpDataSource ds : dataSources) {
                InetAddress address = ds.getAddress();
                String addressId = address.getHostAddress();
                TcpDataSource dsWithInfo;
                try {
                    dsWithInfo = TcpDataSourceProvider.fetchInformation(address, 1500);
                    logger.trace("Discovered Resol VBus-LAN interface @{} {} ({})", addressId,
                            dsWithInfo.getDeviceName(), dsWithInfo.getSerial());

                    currentDataSourceById.put(addressId, dsWithInfo);
                    addAdapter(addressId, dsWithInfo);
                    // here we can add the detection of Multi-Channel interfaces like DL3
                } catch (IOException ex) {
                    /* address is no valid adapter */
                }

            }
        } catch (UnknownHostException e) {
            logger.debug("Could not resolve IPv4 broadcast address");
        }
    }

    private void addAdapter(String remoteIP, TcpDataSource dsWithInfo) {
        String adapterSerial = dsWithInfo.getSerial();
        Map<String, Object> properties = new HashMap<>(3);
        properties.put("ipAddress", remoteIP);
        properties.put("port", dsWithInfo.getLivePort());
        properties.put("adapterSerial", adapterSerial);

        ThingUID uid = new ThingUID(ResolBindingConstants.THING_TYPE_UID_BRIDGE, adapterSerial);
        thingDiscovered(DiscoveryResultBuilder.create(uid).withRepresentationProperty("ipAddress")
                .withProperties(properties).withLabel(dsWithInfo.getName()).build());
    }
}
