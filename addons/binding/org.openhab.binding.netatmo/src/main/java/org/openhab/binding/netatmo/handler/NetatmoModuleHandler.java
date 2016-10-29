/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.netatmo.handler;

import static org.openhab.binding.netatmo.NetatmoBindingConstants.*;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.netatmo.config.NetatmoModuleConfiguration;
import org.openhab.binding.netatmo.internal.NAModuleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NetatmoModuleHandler} is the handler for a given
 * module device accessed through the Netatmo Device
 *
 * @author Gaël L'hopital - Initial contribution OH2 version
 *
 */
public abstract class NetatmoModuleHandler<X extends NetatmoModuleConfiguration>
        extends AbstractNetatmoThingHandler<X> {
    private static Logger logger = LoggerFactory.getLogger(NetatmoModuleHandler.class);
    // Initialize with default values to avoid div 0 if properties not correctly loaded
    private int batteryMin = 0;
    private int batteryLow = 0;
    private int batteryMax = 1;
    protected NAModuleAdapter module;

    protected NetatmoModuleHandler(Thing thing, Class<X> configurationClass) {
        super(thing, configurationClass);
        try {
            this.batteryMax = Integer.parseInt(getProperty(PROPERTY_BATTERY_MAX));
            this.batteryMin = Integer.parseInt(getProperty(PROPERTY_BATTERY_MIN));
            this.batteryLow = Integer.parseInt(getProperty(PROPERTY_BATTERY_LOW));
        } catch (NumberFormatException e) {
            logger.warn("Battery levels were not correctly initialised - reported values will be inconsistent");
        }
    }

    private int getBatteryPercent() {
        // when batteries are freshly changed, API may return a value superior to batteryMax !
        int correctedVp = Math.min(module.getBatteryVp(), batteryMax);
        return (100 * (correctedVp - batteryMin) / (batteryMax - batteryMin));
    }

    @Override
    protected State getNAThingProperty(String channelId) {
        switch (channelId) {
            case CHANNEL_BATTERY_LEVEL:
                return toDecimalType(getBatteryPercent());
            case CHANNEL_LOW_BATTERY:
                return module.getBatteryVp() < batteryLow ? OnOffType.ON : OnOffType.OFF;
            case CHANNEL_LAST_MESSAGE:
                return toDateTimeType(module.getLastMessage());
            case CHANNEL_RF_STATUS:
                Integer rfStatus = module.getRfStatus();
                return toDecimalType(getSignalStrength(rfStatus));
            default:
                return super.getNAThingProperty(channelId);
        }
    }

    public void updateChannels(NetatmoBridgeHandler bridgeHandler, NAModuleAdapter module) {
        this.module = module;
        super.updateChannels(configuration.getParentId());
    }

}
