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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.netatmo.config.NetatmoDeviceConfiguration;

import io.swagger.client.model.NADevice;
import io.swagger.client.model.NAPlace;

/**
 * {@link NetatmoDeviceHandler} is the handler for a given
 * device accessed through the Netatmo Bridge
 *
 * @author Gaël L'hopital - Initial contribution OH2 version
 *
 */
public abstract class NetatmoDeviceHandler extends AbstractNetatmoThingHandler {
    protected NADevice device;
    private NetatmoDeviceConfiguration configuration;
    private ScheduledFuture<?> refreshJob;

    public NetatmoDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        this.configuration = this.getConfigAs(NetatmoDeviceConfiguration.class);

        refreshJob = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateChannels();
            }
        }, 1, configuration.refreshInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    @Override
    protected void updateChannels() {
        dashboard = device.getDashboardData();
        super.updateChannels();

        updateConnectedModules();
    }

    @Override
    protected State getNAThingProperty(String chanelId) {
        switch (chanelId) {
            case CHANNEL_LAST_STATUS_STORE:
                return new DateTimeType(timestampToCalendar(device.getLastStatusStore()));
            case CHANNEL_LOCATION:
                NAPlace place = device.getPlace();
                return new PointType(new DecimalType(place.getLocation().get(1)),
                        new DecimalType(place.getLocation().get(0)), new DecimalType(place.getAltitude()));
            case CHANNEL_WIFI_STATUS:
                Integer wifiStatus = device.getWifiStatus();
                return new DecimalType(getSignalStrength(wifiStatus));
            default:
                return super.getNAThingProperty(chanelId);
        }
    }

    protected String getId() {
        return configuration.getEquipmentId();
    }

    private void updateConnectedModules() {
        for (Thing handler : bridgeHandler.getThing().getThings()) {
            ThingHandler thingHandler = handler.getHandler();
            if (thingHandler instanceof NetatmoModuleHandler) {
                NetatmoModuleHandler moduleHandler = (NetatmoModuleHandler) thingHandler;
                String parentId = moduleHandler.getParentId();
                if (parentId != null && parentId.equals(getId())) {
                    moduleHandler.updateChannels();
                }
            }
        }
    }

}