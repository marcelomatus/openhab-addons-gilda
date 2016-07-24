/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hdpowerview.discovery;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.hdpowerview.HDPowerViewBindingConstants;
import org.openhab.binding.hdpowerview.config.HDPowerViewHubConfiguration;

import jcifs.netbios.NbtAddress;

/**
 * Discovers the HD Power View HUB by searching for a host advertised with the NetBIOS name PDBU-Hub3.0
 *
 * @author Andy Lintner
 */
public class HDPowerViewHubDiscoveryService extends AbstractDiscoveryService {

    private final Runnable scanner;
    private ScheduledFuture<?> backgroundFuture;

    public HDPowerViewHubDiscoveryService() {
        super(Collections.singleton(HDPowerViewBindingConstants.THING_TYPE_HUB), 600, true);
        scanner = createScanner();
    }

    @Override
    protected void startScan() {
        scheduler.execute(scanner);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (backgroundFuture != null && !backgroundFuture.isDone()) {
            backgroundFuture.cancel(true);
            backgroundFuture = null;
        }
        backgroundFuture = scheduler.scheduleAtFixedRate(scanner, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        if (backgroundFuture != null && !backgroundFuture.isDone()) {
            backgroundFuture.cancel(true);
            backgroundFuture = null;
        }
        super.stopBackgroundDiscovery();
    }

    private Runnable createScanner() {
        return () -> {
            try {
                NbtAddress address = NbtAddress.getByName(HDPowerViewBindingConstants.NETBIOS_NAME);
                if (address != null) {
                    String ip = address.getInetAddress().getHostAddress();
                    ThingUID thingUID = new ThingUID(HDPowerViewBindingConstants.THING_TYPE_HUB, ip.replace('.', '_'));
                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID)
                            .withProperty(HDPowerViewHubConfiguration.IP_ADDRESS, ip)
                            .withLabel("PowerView Hub (" + ip + ")").build();
                    thingDiscovered(result);
                }
            } catch (UnknownHostException e) {
                // Nothing to do here - the host couldn't be found, likely because it doesn't exist
            }
        };
    }

}
