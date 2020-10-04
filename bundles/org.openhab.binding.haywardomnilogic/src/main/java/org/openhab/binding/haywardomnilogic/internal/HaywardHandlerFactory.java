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
package org.openhab.binding.haywardomnilogic.internal;

import static org.openhab.binding.haywardomnilogic.internal.HaywardBindingConstants.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardBackyardHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardBowHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardBridgeHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardChlorinatorHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardColorLogicHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardFilterHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardHeaterHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardRelayHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardSensorHandler;
import org.openhab.binding.haywardomnilogic.internal.handler.HaywardVirtualHeaterHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HaywardHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Matt Myers - Initial Contribution
 */

@Component(service = ThingHandlerFactory.class, configurationPid = "binding.haywardomnilogic")
@NonNullByDefault
public class HaywardHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(HaywardHandlerFactory.class);
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.concat(BRIDGE_THING_TYPES_UIDS.stream(), THING_TYPES_UIDS.stream()).collect(Collectors.toSet()));

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    private final HttpClient httpClient;

    @Activate
    public HaywardHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    /**
     * Creates the specific handler for this thing.
     */
    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_BRIDGE)) {
            return new HaywardBridgeHandler((Bridge) thing, httpClient);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_BACKYARD)) {
            return new HaywardBackyardHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_BOW)) {
            return new HaywardBowHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_CHLORINATOR)) {
            return new HaywardChlorinatorHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_COLORLOGIC)) {
            return new HaywardColorLogicHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_FILTER)) {
            return new HaywardFilterHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_HEATER)) {
            return new HaywardHeaterHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_RELAY)) {
            return new HaywardRelayHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_SENSOR)) {
            return new HaywardSensorHandler(thing);
        }
        if (thingTypeUID.equals(HaywardBindingConstants.THING_TYPE_VIRTUALHEATER)) {
            return new HaywardVirtualHeaterHandler(thing);
        }
        return null;
    }
}
