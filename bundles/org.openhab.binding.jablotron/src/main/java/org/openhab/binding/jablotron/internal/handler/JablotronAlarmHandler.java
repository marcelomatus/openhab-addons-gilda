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
package org.openhab.binding.jablotron.internal.handler;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.config.JablotronDeviceConfig;
import org.openhab.binding.jablotron.internal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link JablotronAlarmHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public abstract class JablotronAlarmHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Gson gson = new Gson();

    protected JablotronDeviceConfig thingConfig = new JablotronDeviceConfig();

    private String lastWarningTime = "";

    protected String alarmName = "";

    private boolean inService = false;

    protected @Nullable ScheduledFuture<?> future = null;

    protected @Nullable ExpiringCache<JablotronDataUpdateResponse> dataCache;
    protected @Nullable ExpiringCache<List<JablotronHistoryDataEvent>> eventCache;

    public JablotronAlarmHandler(Thing thing, String alarmName) {
        super(thing);
        this.alarmName = alarmName;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        if (ThingStatus.UNINITIALIZED == bridgeStatusInfo.getStatus()) {
            cleanup();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        cleanup();
    }

    @Override
    public void initialize() {
        thingConfig = getConfigAs(JablotronDeviceConfig.class);
        future = scheduler.scheduleWithFixedDelay(this::updateAlarmStatus, 1, thingConfig.getRefresh(),
                TimeUnit.SECONDS);
        updateStatus(ThingStatus.ONLINE);
    }

    public boolean isInService() {
        return inService;
    }

    public String getAlarmName() {
        return alarmName;
    }

    protected abstract void updateSegmentStatus(JablotronServiceDetailSegment segment);

    protected void updateSegmentStatus(String segmentName, @Nullable JablotronDataUpdateResponse dataUpdate) {
        if (dataUpdate == null || !dataUpdate.isStatus()) {
            return;
        }
        List<JablotronServiceData> serviceData = dataUpdate.getData().getServiceData();
        for (JablotronServiceData data : serviceData) {
            if (!thingConfig.getServiceId().equals(data.getServiceId())) {
                continue;
            }
            List<JablotronService> services = data.getData();
            for (JablotronService service : services) {
                JablotronServiceDetail detail = service.getData();
                for (JablotronServiceDetailSegment segment : detail.getSegments()) {
                    if (segmentName.equals(segment.getSegmentId())) {
                        updateSegmentStatus(segment);
                    }
                }
            }
        }
    }

    private void cleanup() {
        logger.debug("doing cleanup...");
        if (future != null) {
            future.cancel(true);
        }
    }

    protected State getCheckTime() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        return new DateTimeType(zdt);
    }

    protected synchronized @Nullable JablotronDataUpdateResponse sendGetStatusRequest() {
        JablotronBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            return handler.sendGetStatusRequest(getThing());
        }
        return null;
    }

    protected synchronized boolean updateAlarmStatus() {
        JablotronDataUpdateResponse dataUpdate = sendGetStatusRequest();
        if (dataUpdate == null) {
            return false;
        }

        if (dataUpdate.isStatus()) {
            updateState(CHANNEL_LAST_CHECK_TIME, getCheckTime());
            List<JablotronServiceData> serviceData = dataUpdate.getData().getServiceData();
            for (JablotronServiceData data : serviceData) {
                if (!thingConfig.getServiceId().equals(data.getServiceId())) {
                    continue;
                }
                List<JablotronService> services = data.getData();
                for (JablotronService service : services) {
                    JablotronServiceDetail detail = service.getData();
                    for (JablotronServiceDetailSegment segment : detail.getSegments()) {
                        updateSegmentStatus(segment);
                    }
                }

            }
        } else {
            logger.debug("Error during alarm status update: {}", dataUpdate.getErrorMessage());
        }

        List<JablotronHistoryDataEvent> events = sendGetEventHistory();
        if (events != null && events.size() > 0) {
            JablotronHistoryDataEvent event = events.get(0);
            updateLastEvent(event);
        }

        return true;
    }

    protected @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory() {
        return sendGetEventHistory(alarmName);
    }

    private @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory(String alarm) {
        JablotronBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            return handler.sendGetEventHistory(getThing(), alarm);
        }
        return null;
    }

    protected void updateLastEvent(JablotronHistoryDataEvent event) {
        updateState(CHANNEL_LAST_EVENT_TIME, new DateTimeType(getZonedDateTime(event.getDate())));
        updateState(CHANNEL_LAST_EVENT, new StringType(event.getEventText()));
        updateState(CHANNEL_LAST_EVENT_CLASS, new StringType(event.getIconType()));
        updateState(CHANNEL_LAST_EVENT_INVOKER, new StringType(event.getInvokerName()));

        // oasis does not have sections
        if (getThing().getChannel(CHANNEL_LAST_EVENT_SECTION) != null) {
            updateState(CHANNEL_LAST_EVENT_SECTION, new StringType(event.getSectionName()));
        }
    }

    protected void updateEventChannel(String channel) {
        List<JablotronHistoryDataEvent> events = eventCache != null ? eventCache.getValue() : null;
        if (events != null && events.size() > 0) {
            JablotronHistoryDataEvent event = events.get(0);
            switch (channel) {
                case CHANNEL_LAST_EVENT_TIME:
                    updateState(CHANNEL_LAST_EVENT_TIME, new DateTimeType(getZonedDateTime(event.getDate())));
                    break;
                case CHANNEL_LAST_EVENT:
                    updateState(CHANNEL_LAST_EVENT, new StringType(event.getEventText()));
                    break;
                case CHANNEL_LAST_EVENT_CLASS:
                    updateState(CHANNEL_LAST_EVENT_CLASS, new StringType(event.getIconType()));
                    break;
                case CHANNEL_LAST_EVENT_INVOKER:
                    updateState(CHANNEL_LAST_EVENT_INVOKER, new StringType(event.getInvokerName()));
                    break;
                case CHANNEL_LAST_EVENT_SECTION:
                    updateState(CHANNEL_LAST_EVENT_SECTION, new StringType(event.getSectionName()));
                    break;
            }
        }
    }

    public ZonedDateTime getZonedDateTime(String date) {
        return ZonedDateTime.parse(date.substring(0, 22) + ":" + date.substring(22, 24),
                DateTimeFormatter.ISO_DATE_TIME);
    }

    protected @Nullable JablotronControlResponse sendUserCode(String section, String key, String status, String code) {
        JablotronControlResponse response = null;
        JablotronBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            try {
                response = handler.sendUserCode(getThing(), section, key, status, code);
            } catch (SecurityException se) {
                response = handler.sendUserCode(getThing(), section, key, status, code);
            }
        }
        return response;
    }

    protected @Nullable JablotronBridgeHandler getBridgeHandler() {
        Bridge br = getBridge();
        if (br != null && br.getHandler() != null) {
            return (JablotronBridgeHandler) br.getHandler();
        }
        return null;
    }

    public void setStatus(ThingStatus status, ThingStatusDetail detail, String message) {
        updateStatus(status, detail, message);
    }

    public void triggerAlarm(JablotronDiscoveredService service) {
        if (!service.getWarningTime().equals(lastWarningTime)) {
            logger.debug("Service id: {} is triggering an alarm: {}", thing.getUID().getId(), service.getWarning());
            lastWarningTime = service.getWarningTime();
            triggerChannel(CHANNEL_ALARM, service.getWarning());
        }
    }

    public void setInService(boolean inService) {
        this.inService = inService;
    }
}
