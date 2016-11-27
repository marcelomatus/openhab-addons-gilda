/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.netatmo.internal;

import static org.openhab.binding.netatmo.NetatmoBindingConstants.*;

import java.util.Hashtable;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.netatmo.config.NetatmoBridgeConfiguration;
import org.openhab.binding.netatmo.config.NetatmoWelcomeBridgeConfiguration;
import org.openhab.binding.netatmo.discovery.NetatmoModuleDiscoveryService;
import org.openhab.binding.netatmo.discovery.NetatmoWelcomeDiscoveryService;
import org.openhab.binding.netatmo.handler.NetatmoBridgeHandler;
import org.openhab.binding.netatmo.handler.NetatmoWelcomeBridgeHandler;
import org.openhab.binding.netatmo.handler.station.NAMainHandler;
import org.openhab.binding.netatmo.handler.station.NAModule1Handler;
import org.openhab.binding.netatmo.handler.station.NAModule2Handler;
import org.openhab.binding.netatmo.handler.station.NAModule3Handler;
import org.openhab.binding.netatmo.handler.station.NAModule4Handler;
import org.openhab.binding.netatmo.handler.thermostat.NAPlugHandler;
import org.openhab.binding.netatmo.handler.thermostat.NATherm1Handler;
import org.openhab.binding.netatmo.handler.welcome.NAWelcomeCameraHandler;
import org.openhab.binding.netatmo.handler.welcome.NAWelcomeEventHandler;
import org.openhab.binding.netatmo.handler.welcome.NAWelcomeHomeHandler;
import org.openhab.binding.netatmo.handler.welcome.NAWelcomePersonHandler;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NetatmoHandlerFactory} is responsible for creating things and
 * thing handlers.
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Ing. Peter Weiss - Welcome camera implementation
 */
public class NetatmoHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(NetatmoHandlerFactory.class);
    private ServiceRegistration<?> discoveryServiceReg;
    private ServiceRegistration<?> discoveryServiceWelcomeReg;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID));
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(APIBRIDGE_THING_TYPE)) {
            NetatmoBridgeHandler<NetatmoBridgeConfiguration> bridgeHandler = new NetatmoBridgeHandler<NetatmoBridgeConfiguration>(
                    (Bridge) thing, NetatmoBridgeConfiguration.class);
            registerDeviceDiscoveryService(bridgeHandler);
            return bridgeHandler;
        } else if (thingTypeUID.equals(WELCOMEBRIDGE_THING_TYPE)) {
            NetatmoWelcomeBridgeHandler<NetatmoWelcomeBridgeConfiguration> bridgeHandler = new NetatmoWelcomeBridgeHandler<NetatmoWelcomeBridgeConfiguration>(
                    (Bridge) thing, NetatmoWelcomeBridgeConfiguration.class);
            registerDeviceDiscoveryService(bridgeHandler);
            return bridgeHandler;
        } else if (thingTypeUID.equals(MODULE1_THING_TYPE)) {
            return new NAModule1Handler(thing);
        } else if (thingTypeUID.equals(MODULE2_THING_TYPE)) {
            return new NAModule2Handler(thing);
        } else if (thingTypeUID.equals(MODULE3_THING_TYPE)) {
            return new NAModule3Handler(thing);
        } else if (thingTypeUID.equals(MODULE4_THING_TYPE)) {
            return new NAModule4Handler(thing);
        } else if (thingTypeUID.equals(MAIN_THING_TYPE)) {
            return new NAMainHandler(thing);
        } else if (thingTypeUID.equals(PLUG_THING_TYPE)) {
            return new NAPlugHandler(thing);
        } else if (thingTypeUID.equals(THERM1_THING_TYPE)) {
            return new NATherm1Handler(thing);
        } else if (thingTypeUID.equals(WELCOME_HOME_THING_TYPE)) {
            return new NAWelcomeHomeHandler(thing);
        } else if (thingTypeUID.equals(WELCOME_CAMERA_THING_TYPE)) {
            return new NAWelcomeCameraHandler(thing);
        } else if (thingTypeUID.equals(WELCOME_PERSON_THING_TYPE)) {
            return new NAWelcomePersonHandler(thing);
        } else if (thingTypeUID.equals(WELCOME_EVENT_THING_TYPE)) {
            return new NAWelcomeEventHandler(thing);
        } else {
            logger.warn("ThingHandler not found for {}", thing.getThingTypeUID());
            return null;
        }
    }

    private void registerDeviceDiscoveryService(NetatmoBridgeHandler<NetatmoBridgeConfiguration> netatmoBridgeHandler) {
        NetatmoModuleDiscoveryService discoveryService = new NetatmoModuleDiscoveryService(netatmoBridgeHandler);
        discoveryServiceReg = bundleContext.registerService(DiscoveryService.class.getName(), discoveryService,
                new Hashtable<String, Object>());
    }

    private void registerDeviceDiscoveryService(
            NetatmoWelcomeBridgeHandler<NetatmoWelcomeBridgeConfiguration> netatmoWelcomeBridgeHandler) {
        NetatmoWelcomeDiscoveryService welcomeDiscoveryService = new NetatmoWelcomeDiscoveryService(
                netatmoWelcomeBridgeHandler);
        discoveryServiceWelcomeReg = bundleContext.registerService(DiscoveryService.class.getName(),
                welcomeDiscoveryService, new Hashtable<String, Object>());

    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (discoveryServiceReg != null && thingHandler.getThing().getThingTypeUID().equals(APIBRIDGE_THING_TYPE)) {
            discoveryServiceReg.unregister();
            discoveryServiceReg = null;
        }

        if (discoveryServiceWelcomeReg != null
                && thingHandler.getThing().getThingTypeUID().equals(WELCOMEBRIDGE_THING_TYPE)) {
            discoveryServiceWelcomeReg.unregister();
            discoveryServiceWelcomeReg = null;
        }

        super.removeHandler(thingHandler);
    }

}
