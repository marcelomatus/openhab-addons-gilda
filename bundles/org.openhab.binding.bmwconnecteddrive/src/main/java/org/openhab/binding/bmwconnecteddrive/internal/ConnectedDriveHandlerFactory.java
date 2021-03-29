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
package org.openhab.binding.bmwconnecteddrive.internal;

import static org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bmwconnecteddrive.internal.handler.BMWConnectedDriveOptionProvider;
import org.openhab.binding.bmwconnecteddrive.internal.handler.ConnectedDriveBridgeHandler;
import org.openhab.binding.bmwconnecteddrive.internal.handler.VehicleHandler;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ConnectedDriveHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.bmwconnecteddrive", service = ThingHandlerFactory.class)
public class ConnectedDriveHandlerFactory extends BaseThingHandlerFactory {

    private final HttpClientFactory httpClientFactory;
    private final BMWConnectedDriveOptionProvider optionProvider;
    private static final List<VehicleHandler> VEHICLE_HANDLER_REGISTRY = new ArrayList<VehicleHandler>();
    private boolean imperial = false;

    @Activate
    public ConnectedDriveHandlerFactory(final @Reference HttpClientFactory hcf,
            final @Reference BMWConnectedDriveOptionProvider op, final @Reference LocaleProvider lp) {
        httpClientFactory = hcf;
        optionProvider = op;
        imperial = IMPERIAL_COUNTRIES.contains(lp.getLocale().getCountry());
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_SET.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (THING_TYPE_CONNECTED_DRIVE_ACCOUNT.equals(thingTypeUID)) {
            return new ConnectedDriveBridgeHandler((Bridge) thing, httpClientFactory, bundleContext);
        } else if (SUPPORTED_THING_SET.contains(thingTypeUID)) {
            VehicleHandler vh = new VehicleHandler(thing, optionProvider, thingTypeUID.getId(), imperial);
            VEHICLE_HANDLER_REGISTRY.add(vh);
            return vh;
        }
        return null;
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof ConnectedDriveBridgeHandler) {
            // nothing to cleanup
        } else {
            VEHICLE_HANDLER_REGISTRY.remove(thingHandler);
        }
    }

    public static List<VehicleHandler> getHandlerRegistry() {
        return VEHICLE_HANDLER_REGISTRY;
    }
}
