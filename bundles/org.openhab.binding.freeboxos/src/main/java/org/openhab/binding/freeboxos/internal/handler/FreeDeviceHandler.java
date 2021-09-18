/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.freeboxos.internal.handler;

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.system.DeviceConfig;
import org.openhab.binding.freeboxos.internal.api.system.Sensor;
import org.openhab.binding.freeboxos.internal.api.system.Sensor.SensorKind;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public abstract class FreeDeviceHandler extends HostHandler {
    private final Logger logger = LoggerFactory.getLogger(FreeDeviceHandler.class);
    private long uptime = -1;

    public FreeDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    void internalGetProperties(Map<String, String> properties) throws FreeboxException {
        super.internalGetProperties(properties);
        getDeviceConfig().ifPresent(config -> {
            List<Channel> channels = new ArrayList<>(getThing().getChannels());
            List<Sensor> sensors = config.getAllSensors();
            sensors.forEach(sensor -> {
                ChannelUID sensorId = new ChannelUID(thing.getUID(), GROUP_SENSORS, sensor.getId());
                if (channels.stream().noneMatch(c -> c.getUID().equals(sensorId))) {
                    String channelLabel = sensor.getName().startsWith(sensor.getKind().getLabel()) ? sensor.getName()
                            : String.format("%s %s", sensor.getKind().getLabel(), sensor.getName());
                    ChannelBuilder channelBuilder = ChannelBuilder.create(sensorId).withLabel(channelLabel);
                    if (sensor.getKind() == SensorKind.FAN) {
                        channels.add(channelBuilder.withAcceptedItemType(CoreItemFactory.NUMBER)
                                .withType(new ChannelTypeUID(BINDING_ID + ":fanspeed")).build());
                    } else if (sensor.getKind() == SensorKind.TEMP) {
                        channels.add(channelBuilder.withAcceptedItemType("Number:Temperature")
                                .withType(new ChannelTypeUID(BINDING_ID + ":temperature")).build());
                    }
                }
            });
            updateThing(editThing().withChannels(channels).build());
        });
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        super.internalPoll();
        fetchSystemConfig();
    }

    protected abstract Optional<DeviceConfig> getDeviceConfig() throws FreeboxException;

    protected abstract void internalCallReboot() throws FreeboxException;

    private void fetchSystemConfig() throws FreeboxException {
        getDeviceConfig().ifPresent(config -> {
            List<Sensor> sensors = config.getAllSensors();
            sensors.forEach(sensor -> {
                switch (sensor.getKind()) {
                    case FAN:
                        updateChannelDecimal(GROUP_SENSORS, sensor.getId(), sensor.getValue());
                        break;
                    case TEMP:
                        updateChannelQuantity(GROUP_SENSORS, sensor.getId(), sensor.getValue(), SIUnits.CELSIUS);
                        break;
                    case UNKNOWN:
                        logger.warn("Unknown sensor kind : {}", sensor);
                        break;
                }
            });

            long newUptime = config.getUptimeVal();
            if (newUptime < uptime) {
                triggerChannel(new ChannelUID(getThing().getUID(), SYS_INFO, BOX_EVENT), "restarted");
                Map<String, String> properties = editProperties();
                if (!config.getFirmwareVersion().equals(properties.get(Thing.PROPERTY_FIRMWARE_VERSION))) {
                    properties.put(Thing.PROPERTY_FIRMWARE_VERSION, config.getFirmwareVersion());
                    triggerChannel(new ChannelUID(getThing().getUID(), SYS_INFO, BOX_EVENT), "firmware_updated");
                    updateProperties(properties);
                }
            }
            uptime = newUptime;
            updateChannelQuantity(SYS_INFO, UPTIME, uptime, Units.SECOND);
        });
    }

    public void reboot() {
        try {
            internalCallReboot();
            triggerChannel(new ChannelUID(getThing().getUID(), SYS_INFO, BOX_EVENT), "reboot_requested");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.DUTY_CYCLE, "System rebooting, will wait 2 minutes.");
            stopRefreshJob();
            scheduler.schedule(this::initialize, 2, TimeUnit.MINUTES);
        } catch (FreeboxException e) {
            logger.warn("Error rebooting device : {}", e.getMessage());
        }
    }
}
