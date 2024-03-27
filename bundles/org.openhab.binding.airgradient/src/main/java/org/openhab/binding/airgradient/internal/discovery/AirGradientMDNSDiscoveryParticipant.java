/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.airgradient.internal.discovery;

import static org.openhab.binding.airgradient.internal.AirGradientBindingConstants.CONFIG_API_HOST_NAME;
import static org.openhab.binding.airgradient.internal.AirGradientBindingConstants.CONFIG_API_REFRESH_INTERVAL;
import static org.openhab.binding.airgradient.internal.AirGradientBindingConstants.CONFIG_API_TOKEN;
import static org.openhab.binding.airgradient.internal.AirGradientBindingConstants.DEFAULT_POLL_INTERVAL_LOCAL;
import static org.openhab.binding.airgradient.internal.AirGradientBindingConstants.THING_TYPE_API;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AirGradientMDNSDiscoveryParticipant} is responsible for discovering new and removed AirGradient sensors.
 * It uses the
 * central {@link org.openhab.core.config.discovery.mdns.internal.MDNSDiscoveryService}.
 *
 * @author Jørgen Austvik - Initial contribution
 */
@Component(configurationPid = "discovery.airgradient")
@NonNullByDefault
public class AirGradientMDNSDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private static final String SERVICE_TYPE = "_airgradient._tcp.local.";

    private final Logger logger = LoggerFactory.getLogger(AirGradientMDNSDiscoveryParticipant.class);
    protected final ThingRegistry thingRegistry;

    @Activate
    public AirGradientMDNSDiscoveryParticipant(final @Reference ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Set.of(THING_TYPE_API);
    }

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo si) {
        logger.debug("Discovered {} at {}:\n{}", si.getQualifiedName(), si.getURLs(), si.getNiceTextString());

        String urls[] = si.getURLs();
        if (urls == null || urls.length < 1) {
            logger.debug("Not able to find URLs for {}, not autodetecting", si.getQualifiedName());
            return null;
        }

        String serialNo = si.getPropertyString("serialno");
        String hostName = urls[0] + "/measures/current";
        String model = si.getPropertyString("model");

        Map<String, Object> properties = new HashMap<>(1);
        properties.put(CONFIG_API_TOKEN, "");
        properties.put(CONFIG_API_HOST_NAME, hostName);
        properties.put(CONFIG_API_REFRESH_INTERVAL, DEFAULT_POLL_INTERVAL_LOCAL);

        ThingUID thingUID = new ThingUID(THING_TYPE_API, serialNo);

        logger.debug("Autodiscovered API {} with id {} with host name {}. It is a {}", si.getName(), thingUID, hostName,
                model);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                .withLabel(si.getName()).withRepresentationProperty(CONFIG_API_HOST_NAME).build();

        return discoveryResult;
    }

    private static <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) {
        return () -> iterator;
    }

    private String toString(Enumeration<String> strs) {
        return String.join(", ", getIterableFromIterator(strs.asIterator()));
    }

    private String toString(String strs[]) {
        return String.join(", ", strs);
    }

    private String toString(InetAddress strs[]) {
        return String.join(", ", Stream.of(strs).map((a) -> a.toString()).toList());
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo si) {
        logger.debug("Getting thing ID for: App: {} Host: {} Name: {} Port: {} Serial: {}", si.getApplication(),
                si.getHostAddresses(), si.getName(), si.getPort(), si.getPropertyString("serialno"));
        return new ThingUID(THING_TYPE_API, si.getPropertyString("serialno"));
    }
}
