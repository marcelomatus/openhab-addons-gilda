/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.lgtvserial.internal;

import static org.openhab.binding.lgtvserial.LgTvSerialBindingConstants.*;

import java.util.Collections;
import java.util.Set;

import org.openhab.binding.lgtvserial.handler.LgTvSerialHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

/**
 * The {@link LgTvSerialHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Marius Bjoernstad - Initial contribution
 */
public class LgTvSerialHandlerFactory extends BaseThingHandlerFactory {
    
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_LGTV);
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_LGTV)) {
            return new LgTvSerialHandler(thing);
        }

        return null;
    }
}

