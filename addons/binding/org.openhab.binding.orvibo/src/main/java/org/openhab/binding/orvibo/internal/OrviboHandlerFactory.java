/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.orvibo.internal;

import static org.openhab.binding.orvibo.OrviboBindingConstants.THING_TYPE_S20;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.orvibo.handler.S20Handler;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tavalin.s20.S20Client;

/**
 * The {@link OrviboHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Daniel Walters - Initial contribution
 */
public class OrviboHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(OrviboHandlerFactory.class);
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_S20);
    private S20Client s20Client;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        try {
            s20Client = S20Client.getInstance();
            s20Client.connect();
        } catch (Exception e) {
            logger.error("Error activating OrviboHandlerFactory: " + e.getMessage(), e);
        }
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        s20Client.disconnect();
        super.deactivate(componentContext);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_S20)) {
            return new S20Handler(thing);
        }
        return null;
    }

}
