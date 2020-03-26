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
package org.openhab.binding.bluetooth.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.bluetooth.BeaconBluetoothHandler;
import org.openhab.binding.bluetooth.BluetoothAdapter;
import org.openhab.binding.bluetooth.BluetoothBindingConstants;
import org.openhab.binding.bluetooth.ConnectedBluetoothHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link BluetoothHandlerFactory} is responsible for creating things and thing handlers.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.bluetooth")
public class BluetoothHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
    static {
        SUPPORTED_THING_TYPES_UIDS.add(BluetoothBindingConstants.THING_TYPE_BEACON);
        SUPPORTED_THING_TYPES_UIDS.add(BluetoothBindingConstants.THING_TYPE_CONNECTED);
        SUPPORTED_THING_TYPES_UIDS.add(BluetoothBindingConstants.THING_TYPE_ROAMING);
    }

    private final Map<ThingUID, @Nullable ServiceRegistration<?>> serviceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_BEACON)) {
            return new BeaconBluetoothHandler(thing);
        } else if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_CONNECTED)) {
            return new ConnectedBluetoothHandler(thing);
        } else if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_ROAMING)) {
            RoamingBluetoothBridgeHandler handler = new RoamingBluetoothBridgeHandler((Bridge) thing);
            registerRoamingBluetoothAdapter(handler);
            return handler;
        }
        return null;
    }

    private synchronized void registerRoamingBluetoothAdapter(RoamingBluetoothAdapter adapter) {
        this.serviceRegs.put(adapter.getUID(), bundleContext.registerService(RoamingBluetoothAdapter.class.getName(),
                adapter, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof RoamingBluetoothAdapter) {
            UID uid = ((BluetoothAdapter) thingHandler).getUID();
            ServiceRegistration<?> serviceReg = this.serviceRegs.remove(uid);
            if (serviceReg != null) {
                serviceReg.unregister();
            }
        }
    }
}
