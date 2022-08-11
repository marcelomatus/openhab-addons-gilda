/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.liquidcheck.internal;

import static org.openhab.binding.liquidcheck.internal.LiquidCheckBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.liquidcheck.internal.httpClient.LiquidCheckHttpClient;
import org.openhab.binding.liquidcheck.internal.json.Response;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link LiquidCheckHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
public class LiquidCheckHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(LiquidCheckHandler.class);

    private Map<String, String> oldProps;

    private LiquidCheckConfiguration config = getConfigAs(LiquidCheckConfiguration.class);

    private @Nullable ScheduledFuture<?> polling;

    public LiquidCheckHandler(Thing thing) {
        super(thing);
        oldProps = thing.getProperties();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (MEASURE_CHANNEL.equals(channelUID.getAsString())) {
            LiquidCheckHttpClient client = new LiquidCheckHttpClient(config);
            String response = client.measureCommand();
            logger.debug("This is the response: {}", response);
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(LiquidCheckConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {

            LiquidCheckHttpClient httpClient = new LiquidCheckHttpClient(config);
            boolean thingReachable = httpClient.isConnected();
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
                PollingForData pollingRunnable = new PollingForData(httpClient);
                polling = scheduler.scheduleWithFixedDelay(pollingRunnable, 0, config.refreshInterval,
                        TimeUnit.SECONDS);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    private Map<String, String> createPropertyMap(Response response) {
        Map<String, String> properties = new HashMap<>();
        properties.put(CONFIG_ID_FIRMWARE, response.payload.device.firmware);
        properties.put(CONFIG_ID_HARDWARE, response.payload.device.hardware);
        properties.put(CONFIG_ID_NAME, response.payload.device.name);
        properties.put(CONFIG_ID_MANUFACTURER, response.payload.device.manufacturer);
        properties.put(CONFIG_ID_UUID, response.payload.device.uuid);
        properties.put(CONFIG_ID_SECURITY_CODE, response.payload.device.security.code);
        properties.put(CONFIG_ID_IP, response.payload.wifi.station.ip);
        properties.put(CONFIG_ID_MAC, response.payload.wifi.station.mac);
        properties.put(CONFIG_ID_SSID, response.payload.wifi.accessPoint.ssid);
        return properties;
    }

    @Override
    public void dispose() {
        if (null != polling) {
            polling.cancel(true);
        }
    }

    private class PollingForData implements Runnable {

        private final LiquidCheckHttpClient client;

        public PollingForData(LiquidCheckHttpClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                String jsonString = client.pollData();
                Response response = new Gson().fromJson(jsonString, Response.class);
                if (null != response) {
                    Map<String, String> properties = createPropertyMap(response);
                    if (!oldProps.equals(properties)) {
                        oldProps = properties;
                        updateProperties(properties);
                    }
                    updateState(CONTENT_CHANNEL, new QuantityType<>(response.payload.measure.content, Units.LITRE));
                    updateState(LEVEL_CHANNEL, new QuantityType<>(response.payload.measure.level, SIUnits.METRE));
                    updateState(RAW_CONTENT_CHANNEL,
                            new QuantityType<>(response.payload.measure.raw.content, Units.LITRE));
                    updateState(RAW_LEVEL_CHANNEL,
                            new QuantityType<>(response.payload.measure.raw.level, SIUnits.METRE));
                    updateState(PUMP_TOTAL_RUNS_CHANNEL, new DecimalType(response.payload.system.pump.totalRuns));
                    updateState(PUMP_TOTAl_RUNTIME_CHANNEL,
                            new QuantityType<>(response.payload.system.pump.totalRuntime, Units.SECOND));
                } else {
                    logger.debug("Json is null");
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                logger.error("This went wrong: ", e);
            }
        }
    }
}
