/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.etherrain.discovery;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.etherrain.EtherRainBindingConstants;
import org.openhab.binding.etherrain.internal.api.EtherRainCommunication;
import org.openhab.binding.etherrain.internal.api.EtherRainUdpResponse;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EtherrainDiscoveryService} class discovers Etherrain Device(s) and places them in the inbox.
 *
 * @author Joe Inkenbrandt - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.etherrain")
public class EtherrainDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(EtherrainDiscoveryService.class);

    private static final int TIMEOUT = 15;

    public EtherrainDiscoveryService() {
        super(EtherRainBindingConstants.SUPPORTED_THING_TYPES_UIDS, TIMEOUT, true);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return EtherRainBindingConstants.SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void startScan() {
        EtherRainUdpResponse rdp = EtherRainCommunication.autoDiscover();

        if (rdp != null) {
            ThingUID uid = new ThingUID(EtherRainBindingConstants.ETHERRAIN_THING,
                    rdp.getAddress().replaceAll("[^A-Za-z0-9\\-_]", ""));
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
                    .withLabel("Etherrain " + rdp.getType() + " " + rdp.getUnqiueName())
                    .withProperty("hostname", rdp.getAddress()).withProperty("port", rdp.getPort()).build();
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("Nothing responded to request");
        }

    }

}
