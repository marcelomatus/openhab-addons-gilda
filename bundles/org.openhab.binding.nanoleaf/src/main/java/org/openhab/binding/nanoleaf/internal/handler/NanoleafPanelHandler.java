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
package org.openhab.binding.nanoleaf.internal.handler;

import static org.openhab.binding.nanoleaf.internal.NanoleafBindingConstants.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.nanoleaf.internal.NanoleafBindingConstants;
import org.openhab.binding.nanoleaf.internal.NanoleafException;
import org.openhab.binding.nanoleaf.internal.NanoleafUnauthorizedException;
import org.openhab.binding.nanoleaf.internal.OpenAPIUtils;
import org.openhab.binding.nanoleaf.internal.config.NanoleafControllerConfig;
import org.openhab.binding.nanoleaf.internal.model.Effects;
import org.openhab.binding.nanoleaf.internal.model.Write;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link NanoleafPanelHandler} is responsible for handling commands to the controller which
 * affect an individual panels
 *
 * @author Martin Raepple - Initial contribution
 */
public class NanoleafPanelHandler extends BaseThingHandler {

    private final static PercentType MIN_PANEL_BRIGHTNESS = PercentType.ZERO;
    private final static PercentType MAX_PANEL_BRIGHTNESS = PercentType.HUNDRED;

    private final Logger logger = LoggerFactory.getLogger(NanoleafPanelHandler.class);

    private HttpClient httpClient;
    // JSON parser for API responses
    private final Gson gson = new Gson();

    // holds current color data per panel
    private Map<String, HSBType> panelInfo = new HashMap<>();

    private @NonNullByDefault({}) ScheduledFuture<?> singleTapJob;
    private @NonNullByDefault({}) ScheduledFuture<?> doubleTapJob;

    public NanoleafPanelHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing handler for panel {}", getThing().getUID());
        updateStatus(ThingStatus.OFFLINE);
        Bridge controller = getBridge();
        if (controller == null) {
            initializePanel(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, ""));
        } else if (controller.getStatus().equals(ThingStatus.OFFLINE)) {
            initializePanel(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "@text/error.nanoleaf.panel.controllerOffline"));
        } else {
            initializePanel(controller.getStatusInfo());
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo controllerStatusInfo) {
        logger.debug("Controller status changed to {}", controllerStatusInfo);
        if (controllerStatusInfo.getStatus().equals(ThingStatus.OFFLINE)) {
            initializePanel(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "@text/error.nanoleaf.panel.controllerOffline"));
        } else {
            initializePanel(controllerStatusInfo);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        try {
            switch (channelUID.getId()) {
                case CHANNEL_PANEL_COLOR:
                    sendRenderedEffectCommand(command);
                    break;
                default:
                    logger.warn("Channel with id {} not handled", channelUID.getId());
                    break;
            }
        } catch (NanoleafUnauthorizedException nae) {
            logger.warn("Authorization for command {} for channelUID {} failed: {}", command, channelUID,
                    nae.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/error.nanoleaf.controller.invalidToken");
        } catch (NanoleafException ne) {
            logger.warn("Handling command {} for channelUID {} failed: {}", command, channelUID, ne.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/error.nanoleaf.controller.communication");
        }
    }

    @Override
    public void handleRemoval() {
        logger.debug("Nanoleaf panel {} removed", getThing().getUID());
        super.handleRemoval();
    }

    @Override
    public void dispose() {
        logger.debug("Disposing handler for Nanoleaf panel {}", getThing().getUID());
        stopAllJobs();
        super.dispose();
    }

    private void stopAllJobs() {
        if (singleTapJob != null && !singleTapJob.isCancelled()) {
            logger.debug("Stop single touch job");
            singleTapJob.cancel(true);
            this.singleTapJob = null;
        }
        if (doubleTapJob != null && !doubleTapJob.isCancelled()) {
            logger.debug("Stop double touch job");
            doubleTapJob.cancel(true);
            this.doubleTapJob = null;
        }
    }

    private void initializePanel(ThingStatusInfo panelStatus) {
        updateStatus(panelStatus.getStatus(), panelStatus.getStatusDetail());
        logger.debug("Panel {} status changed to {}-{}", this.getThing().getUID(), panelStatus.getStatus(),
                panelStatus.getStatusDetail());
    }

    private void sendRenderedEffectCommand(Command command) throws NanoleafException {
        HSBType currentPanelColor = getPanelColor();
        HSBType newPanelColor = new HSBType();
        if (command instanceof HSBType) {
            newPanelColor = (HSBType) command;
        } else if (command instanceof OnOffType) {
            if (OnOffType.ON.equals(command)) {
                newPanelColor = new HSBType(currentPanelColor.getHue(), currentPanelColor.getSaturation(),
                        MAX_PANEL_BRIGHTNESS);
            } else {
                newPanelColor = new HSBType(currentPanelColor.getHue(), currentPanelColor.getSaturation(),
                        MIN_PANEL_BRIGHTNESS);
            }
        } else if (command instanceof PercentType) {
            PercentType brightness = new PercentType(
                    Math.max(MIN_PANEL_BRIGHTNESS.intValue(), ((PercentType) command).intValue()));
            newPanelColor = new HSBType(currentPanelColor.getHue(), currentPanelColor.getSaturation(), brightness);
        } else if (command instanceof IncreaseDecreaseType) {
            int brightness = currentPanelColor.getBrightness().intValue();
            if (command.equals(IncreaseDecreaseType.INCREASE)) {
                brightness = Math.min(MAX_PANEL_BRIGHTNESS.intValue(), brightness + BRIGHTNESS_STEP_SIZE);
            } else {
                brightness = Math.max(MIN_PANEL_BRIGHTNESS.intValue(), brightness - BRIGHTNESS_STEP_SIZE);
            }
            newPanelColor = new HSBType(currentPanelColor.getHue(), currentPanelColor.getSaturation(),
                    new PercentType(brightness));
        } else if (command instanceof RefreshType) {
            logger.debug("Refresh command received");
            return;
        } else {
            logger.warn("Unhandled command type: {}", command.getClass().getName());
            return;
        }
        // store panel's new HSB value
        panelInfo.put(getThing().getConfiguration().get(NanoleafBindingConstants.CONFIG_PANEL_ID).toString(),
                newPanelColor);
        // transform to RGB
        PercentType[] rgbPercent = newPanelColor.toRGB();
        int red = rgbPercent[0].toBigDecimal().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(255)).intValue();
        int green = rgbPercent[1].toBigDecimal().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(255)).intValue();
        int blue = rgbPercent[2].toBigDecimal().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(255)).intValue();

        Bridge bridge = getBridge();
        if (bridge != null) {
            Effects effects = new Effects();
            Write write = new Write();
            write.setCommand("display");
            write.setAnimType("static");
            String panelID = this.thing.getConfiguration().get(NanoleafBindingConstants.CONFIG_PANEL_ID).toString();
            NanoleafControllerConfig config = ((NanoleafControllerHandler) bridge.getHandler()).getControllerConfig();
            // Light Panels and Canvas use different stream commands
            if (config.deviceType.equals(NanoleafBindingConstants.CONFIG_DEVICE_TYPE_LIGHTPANELS)) {
                write.setAnimData(String.format("1 %s 1 %d %d %d 0 10", panelID, red, green, blue));
            } else {
                int quotient = Integer.divideUnsigned(Integer.valueOf(panelID), 256);
                int remainder = Integer.remainderUnsigned(Integer.valueOf(panelID), 256);
                write.setAnimData(String.format("0 1 %d %d %d %d %d 0 0 10", quotient, remainder, red, green, blue));
            }            
            write.setLoop(false);
            effects.setWrite(write);
            Request setNewRenderedEffectRequest = OpenAPIUtils.requestBuilder(httpClient, config, API_EFFECT,
                    HttpMethod.PUT);
            setNewRenderedEffectRequest.content(new StringContentProvider(gson.toJson(effects)), "application/json");
            OpenAPIUtils.sendOpenAPIRequest(setNewRenderedEffectRequest);
        }
    }

    public void updatePanelColorChannel() {
        updateState(CHANNEL_PANEL_COLOR, getPanelColor());
    }

    /**
     * Apply the gesture to the panel
     *
     * @param gesture Only 0=single tap and 1=double tap are supported
     */
    public void updatePanelGesture(int gesture) {
        switch (gesture){
            case 0:
                updateState(CHANNEL_PANEL_SINGLE_TAP, OnOffType.ON);
                singleTapJob = scheduler.schedule(this::resetSingleTap, 1, TimeUnit.SECONDS);
                logger.debug("Asserting single tap of panel {} to ON",getPanelID());
                break;
            case 1:
                updateState(CHANNEL_PANEL_DOUBLE_TAP, OnOffType.ON);
                doubleTapJob = scheduler.schedule(this::resetDoubleTap, 1, TimeUnit.SECONDS);
                logger.debug("Asserting double tap of panel {} to ON",getPanelID());
                break;
        }
    }

    private void resetSingleTap() {
        updateState(CHANNEL_PANEL_SINGLE_TAP, OnOffType.OFF);
        logger.debug("Resetting single tap of panel {} to OFF",getPanelID());
    }

    private void resetDoubleTap() {
        updateState(CHANNEL_PANEL_DOUBLE_TAP, OnOffType.OFF);
        logger.debug("Resetting double tap of panel {} to OFF",getPanelID());
    }

    private synchronized void stopTouchJob() {
        if (singleTapJob != null && !singleTapJob.isCancelled()) {
            logger.debug("Stop single tap job");
            singleTapJob.cancel(true);
            this.singleTapJob = null;
        }
        if (doubleTapJob != null && !doubleTapJob.isCancelled()) {
            logger.debug("Stop double tap job");
            doubleTapJob.cancel(true);
            this.doubleTapJob = null;
        }
    }


    public String getPanelID() {
        String panelID = getThing().getConfiguration().get(NanoleafBindingConstants.CONFIG_PANEL_ID).toString();
        return panelID;
    }

    private HSBType getPanelColor() {
        String panelID = getPanelID();
        if (panelInfo.get(panelID) == null) {
            // get panel color data from controller
            try {
                Effects effects = new Effects();
                Write write = new Write();
                write.setCommand("request");
                write.setAnimName("*Static*");
                effects.setWrite(write);
                Bridge bridge = getBridge();
                if (bridge != null) {
                    NanoleafControllerConfig config = ((NanoleafControllerHandler) bridge.getHandler())
                            .getControllerConfig();
                    Request setPanelUpdateRequest = OpenAPIUtils.requestBuilder(httpClient, config, API_EFFECT,
                            HttpMethod.PUT);
                    setPanelUpdateRequest.content(new StringContentProvider(gson.toJson(effects)), "application/json");
                    ContentResponse panelData = OpenAPIUtils.sendOpenAPIRequest(setPanelUpdateRequest);
                    // parse panel data
                    Write response = gson.fromJson(panelData.getContentAsString(), Write.class);
                    // panelData is in format (numPanels, (PanelId, 1, R, G, B, W, TransitionTime) * numPanel)
                    String[] tokennizedData = response.getAnimData().split(" ");
                    if (config.deviceType.equals(NanoleafBindingConstants.CONFIG_DEVICE_TYPE_LIGHTPANELS)) {
                        // panelData is in format (numPanels (PanelId 1 R G B W TransitionTime) * numPanel)
                        String[] panelDataPoints = Arrays.copyOfRange(tokennizedData, 1, tokennizedData.length);
                        for (int i = 0; i < panelDataPoints.length; i++) {
                            if ((i % 7) == 0) {
                                String id = panelDataPoints[i];
                                if (id.equals(panelID)) {
                                    // found panel data - store it
                                    panelInfo.put(panelID,
                                            HSBType.fromRGB(Integer.parseInt(panelDataPoints[i + 2]),
                                                    Integer.parseInt(panelDataPoints[i + 3]),
                                                    Integer.parseInt(panelDataPoints[i + 4])));
                                }
                            }
                        }
                    } else {
                        // panelData is in format (0 numPanels (quotient(panelID) remainder(panelID) R G B W 0
                        // quotient(TransitionTime) remainder(TransitionTime)) * numPanel)
                        String[] panelDataPoints = Arrays.copyOfRange(tokennizedData, 2, tokennizedData.length);
                        for (int i = 0; i < panelDataPoints.length; i++) {
                            if ((i % 8) == 0) {
                                String idQuotient = panelDataPoints[i];
                                String idRemainder = panelDataPoints[i + 1];
                                Integer idNum = (Integer.valueOf(idQuotient) * 256) + Integer.valueOf(idRemainder);
                                if (String.valueOf(idNum).equals(panelID)) {
                                    // found panel data - store it
                                    panelInfo.put(panelID,
                                            HSBType.fromRGB(Integer.parseInt(panelDataPoints[i + 3]),
                                                    Integer.parseInt(panelDataPoints[i + 4]),
                                                    Integer.parseInt(panelDataPoints[i + 5])));
                                }
                            }
                        }
                    }
                }
            } catch (NanoleafException nue) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "@text/error.nanoleaf.panel.communication");
                logger.warn("Panel data could not be retrieved: {}", nue.getMessage());
            }
        }

        return panelInfo.get(panelID);
    }
}
