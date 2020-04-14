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
package org.openhab.binding.bluetooth.dbusbluez.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.bluetooth.BluetoothAdapter;
import org.openhab.binding.bluetooth.dbusbluez.DBusBlueZAdapterConstants;
import org.openhab.binding.bluetooth.dbusbluez.handler.DBusBlueZBridgeHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link DBusBlueZHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Benjamin Lafois - Initial contribution and API
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.bluetooth.dbusbluez")
public class DBusBlueZHandlerFactory extends BaseThingHandlerFactory {

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(DBusBlueZAdapterConstants.THING_TYPE_DBUSBLUEZ);

    private final Map<ThingUID, ServiceRegistration<?>> serviceRegs = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(DBusBlueZAdapterConstants.THING_TYPE_DBUSBLUEZ)) {
            DBusBlueZBridgeHandler handler = new DBusBlueZBridgeHandler((Bridge) thing);
            registerBluetoothAdapter(handler);
            return handler;
        } else {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized void registerBluetoothAdapter(BluetoothAdapter adapter) {
        this.serviceRegs.put(adapter.getUID(), bundleContext.registerService(BluetoothAdapter.class.getName(), adapter,
                new Hashtable<String, Object>()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof BluetoothAdapter) {
            UID uid = ((BluetoothAdapter) thingHandler).getUID();
            ServiceRegistration<?> serviceReg = this.serviceRegs.remove(uid);
            if (serviceReg != null) {
                serviceReg.unregister();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

}
