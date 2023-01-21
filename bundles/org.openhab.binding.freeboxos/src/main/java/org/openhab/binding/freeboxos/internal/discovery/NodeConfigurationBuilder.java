/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.freeboxos.internal.discovery;

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.*;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.rest.HomeManager.HomeNode;
import org.openhab.binding.freeboxos.internal.config.BasicShutterConfiguration;
import org.openhab.binding.freeboxos.internal.config.ClientConfiguration;
import org.openhab.binding.freeboxos.internal.config.ShutterConfiguration;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NodeConfigurationBuilder} is responsible for holding configuration informations associated to a Freebox
 * Home thing type
 *
 * @author ben12 - Initial contribution
 */
@NonNullByDefault
public class NodeConfigurationBuilder {
    private static final NodeConfigurationBuilder BUILDER_INSTANCE = new NodeConfigurationBuilder();

    private final Logger logger = LoggerFactory.getLogger(NodeConfigurationBuilder.class);

    private NodeConfigurationBuilder() {
    }

    public static NodeConfigurationBuilder getInstance() {
        return BUILDER_INSTANCE;
    }

    public Optional<DiscoveryResultBuilder> configure(ThingUID bridgeUID, HomeNode node) {
        DiscoveryResultBuilder discoveryResultBuilder = null;
        try {
            if (THING_BASIC_SHUTTER.equals(node.category())) {
                ThingUID basicShutterUID = new ThingUID(THING_TYPE_BASIC_SHUTTER, bridgeUID,
                        Integer.toString(node.id()));
                discoveryResultBuilder = DiscoveryResultBuilder.create(basicShutterUID);
                BasicShutterConfiguration.configure(discoveryResultBuilder, node);
            } else if (THING_SHUTTER.equals(node.category())) {
                ThingUID shutterUID = new ThingUID(THING_TYPE_SHUTTER, bridgeUID, Integer.toString(node.id()));
                discoveryResultBuilder = DiscoveryResultBuilder.create(shutterUID);
                ShutterConfiguration.configure(discoveryResultBuilder, node);
            }
        } catch (FreeboxException e) {
            logger.warn("Error while requesting data for home things discovery : {}", e.getMessage());
            discoveryResultBuilder = null;
        }
        if (discoveryResultBuilder != null) {
            discoveryResultBuilder.withProperty(ClientConfiguration.ID, node.id()).withLabel(node.label())
                    .withRepresentationProperty(ClientConfiguration.ID).withBridge(bridgeUID);
        }
        return Optional.ofNullable(discoveryResultBuilder);
    }
}
