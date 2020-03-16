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
package org.openhab.binding.lcn.internal;

import static org.openhab.binding.lcn.internal.LcnBindingConstants.*;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link LcnHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.lcn", service = ThingHandlerFactory.class)
public class LcnHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.of(THING_TYPE_PCK_GATEWAY, THING_TYPE_MODULE, THING_TYPE_GROUP).collect(Collectors.toSet()));

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_GROUP.equals(thingTypeUID)) {
            return new LcnGroupHandler(thing);
        }

        if (THING_TYPE_MODULE.equals(thingTypeUID)) {
            return new LcnModuleHandler(thing);
        }

        if (THING_TYPE_PCK_GATEWAY.equals(thingTypeUID)) {
            PckGatewayHandler handler = new PckGatewayHandler((Bridge) thing);

            LcnModuleDiscoveryService discoveryService = new LcnModuleDiscoveryService(handler);

            bundleContext.registerService(DiscoveryService.class.getName(), discoveryService,
                    new Hashtable<@Nullable String, @Nullable Object>());
            return handler;
        }

        return null;
    }
}
