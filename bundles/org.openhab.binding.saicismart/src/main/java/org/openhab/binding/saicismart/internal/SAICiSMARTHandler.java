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
package org.openhab.binding.saicismart.internal;

import static org.openhab.binding.saicismart.internal.SAICiSMARTBindingConstants.CHANNEL_DISABLE_AC;
import static org.openhab.binding.saicismart.internal.SAICiSMARTBindingConstants.CHANNEL_ENABLE_AC;
import static org.openhab.binding.saicismart.internal.SAICiSMARTBindingConstants.CHANNEL_FORCE_REFRESH;
import static org.openhab.binding.saicismart.internal.SAICiSMARTBindingConstants.CHANNEL_LAST_ACTIVITY;
import static org.openhab.binding.saicismart.internal.SAICiSMARTBindingConstants.CHANNEL_WINDOW_SUN_ROOF;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.saicismart.internal.asn1.Util;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import net.heberling.ismart.asn1.v2_1.entity.OTA_RVCReq;
import net.heberling.ismart.asn1.v2_1.entity.OTA_RVCStatus25857;
import net.heberling.ismart.asn1.v2_1.entity.RvcReqParam;

/**
 * The {@link SAICiSMARTHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Markus Heberling - Initial contribution
 */
@NonNullByDefault
public class SAICiSMARTHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SAICiSMARTHandler.class);

    @Nullable
    SAICiSMARTVehicleConfiguration config;
    private @Nullable Future<?> pollingJob;

    // if the binding is initialized, treat the car as active to get some first data
    private ZonedDateTime lastCarActivity = ZonedDateTime.now();

    public SAICiSMARTHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(SAICiSMARTBindingConstants.CHANNEL_FORCE_REFRESH) && command == OnOffType.ON) {
            // reset channel to off
            updateState(CHANNEL_FORCE_REFRESH, OnOffType.from(false));
            // update internal activity date, to query the car for about a minute
            notifyCarActivity(ZonedDateTime.now().minus(9, ChronoUnit.MINUTES), true);
        } else if (channelUID.getId().equals(CHANNEL_ENABLE_AC) && command == OnOffType.ON) {
            // reset channel to off
            updateState(CHANNEL_ENABLE_AC, OnOffType.from(false));
            new Thread(() -> {
                // enable air conditioning
                try {
                    sendACCommand((byte) 5, (byte) 8);
                } catch (URISyntaxException | ExecutionException | TimeoutException | InterruptedException e) {
                    logger.error("A/C On Command failed", e);
                }
            }).start();
        } else if (channelUID.getId().equals(CHANNEL_DISABLE_AC) && command == OnOffType.ON) {
            // reset channel to off
            updateState(CHANNEL_DISABLE_AC, OnOffType.from(false));
            new Thread(() -> {
                // disable air conditioning
                try {
                    sendACCommand((byte) 0, (byte) 0);
                } catch (URISyntaxException | ExecutionException | TimeoutException | InterruptedException e) {
                    logger.error("A/C Off Command failed", e);
                }
            }).start();
        } else if (channelUID.getId().equals(CHANNEL_LAST_ACTIVITY) && command instanceof DateTimeType) {
            // update internal activity date from external date
            notifyCarActivity(((DateTimeType) command).getZonedDateTime(), true);
        }
    }

    protected @Nullable SAICiSMARTBridgeHandler getBridgeHandler() {
        return (SAICiSMARTBridgeHandler) super.getBridge().getHandler();
    }

    @Override
    public void initialize() {
        config = getConfigAs(SAICiSMARTVehicleConfiguration.class);

        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(thing.getChannels().stream().filter(c -> {
            // TODO: check properties
            return !c.getUID().getId().equals(CHANNEL_WINDOW_SUN_ROOF);
        }).collect(Collectors.toList()));
        updateThing(thingBuilder.build());

        updateStatus(ThingStatus.UNKNOWN);

        // just started, make sure we start querying
        notifyCarActivity(ZonedDateTime.now(), true);

        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            if (lastCarActivity.isAfter(ZonedDateTime.now().minus(10, ChronoUnit.MINUTES))) {

                if (this.getBridgeHandler().getUid() != null && this.getBridgeHandler().getToken() != null) {
                    try {
                        boolean chargeOrRun = new VehicleStateUpdater(this).call();
                        if (chargeOrRun) {
                            // get precise data
                            new ChargeStateUpdater(this).call();
                            notifyCarActivity(ZonedDateTime.now(), true);
                        }
                    } catch (Exception e) {
                        logger.error("Could not refresh car data for {}", config.vin, e);
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void sendACCommand(byte command, byte temperature)
            throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException {
        // we send a command end expect the car to wake up
        notifyCarActivity(ZonedDateTime.now(), false);

        OTA_RVCReq req = new OTA_RVCReq();
        req.setRvcReqType(new byte[] { 6 });
        List<RvcReqParam> params = new ArrayList<>();
        req.setRvcParams(params);
        RvcReqParam param = new RvcReqParam();
        param.setParamId(19);
        param.setParamValue(new byte[] { command });
        params.add(param);
        param = new RvcReqParam();
        param.setParamId(20);
        param.setParamValue(new byte[] { temperature });
        params.add(param);
        param = new RvcReqParam();
        param.setParamId(255);
        param.setParamValue(new byte[] { 0 });
        params.add(param);

        net.heberling.ismart.asn1.v2_1.Message<OTA_RVCReq> enableACRequest = new net.heberling.ismart.asn1.v2_1.Message<>(
                new net.heberling.ismart.asn1.v2_1.MP_DispatcherHeader(), new byte[16],
                new net.heberling.ismart.asn1.v2_1.MP_DispatcherBody(), req);
        Util.fillReserved(enableACRequest.getReserved());

        enableACRequest.getBody().setApplicationID("510");
        enableACRequest.getBody().setTestFlag(2);
        enableACRequest.getBody().setVin(config.vin);
        enableACRequest.getBody().setUid(getBridgeHandler().getUid());
        enableACRequest.getBody().setToken(getBridgeHandler().getToken());
        enableACRequest.getBody().setMessageID(1);
        enableACRequest.getBody().setEventCreationTime((int) Instant.now().getEpochSecond());
        enableACRequest.getBody().setApplicationDataProtocolVersion(25857);
        enableACRequest.getBody().setEventID(0);
        // chargingStatusMessage.getHeader().setProtocolVersion(32);

        String enableACRequestMessage = new net.heberling.ismart.asn1.v2_1.MessageCoder<>(OTA_RVCReq.class)
                .encodeRequest(enableACRequest);

        String enableACResponseMessage = getBridgeHandler().sendRequest(enableACRequestMessage,
                "https://tap-eu.soimt.com/TAP.Web/ota.mpv21");

        net.heberling.ismart.asn1.v2_1.Message<OTA_RVCStatus25857> enableACResponse = new net.heberling.ismart.asn1.v2_1.MessageCoder<>(
                OTA_RVCStatus25857.class).decodeResponse(enableACResponseMessage);

        // ... use that to request the data again, until we have it
        // TODO: check for real errors (result!=0 and/or errorMessagePresent)
        while (enableACResponse.getApplicationData() == null) {
            if (enableACResponse.getBody().isErrorMessagePresent()) {
                if (enableACResponse.getBody().getResult() == 2) {
                    getBridgeHandler().relogin();
                }
                throw new TimeoutException(new String(enableACResponse.getBody().getErrorMessage()));
            }
            Util.fillReserved(enableACRequest.getReserved());

            if (enableACResponse.getBody().getResult() == 0) {
                // we get an eventId back...
                enableACRequest.getBody().setEventID(enableACResponse.getBody().getEventID());
            } else {
                // try a fresh eventId
                enableACRequest.getBody().setEventID(0);
            }

            enableACRequestMessage = new net.heberling.ismart.asn1.v2_1.MessageCoder<>(OTA_RVCReq.class)
                    .encodeRequest(enableACRequest);

            enableACResponseMessage = getBridgeHandler().sendRequest(enableACRequestMessage,
                    "https://tap-eu.soimt.com/TAP.Web/ota.mpv21");

            enableACResponse = new net.heberling.ismart.asn1.v2_1.MessageCoder<>(OTA_RVCStatus25857.class)
                    .decodeResponse(enableACResponseMessage);

        }

        logger.info("Got A/C message: {}", new GsonBuilder().setPrettyPrinting().create().toJson(enableACResponse));
    }

    public void notifyCarActivity(ZonedDateTime now, boolean force) {
        // if the car activity changed, notify the channel
        if (force || lastCarActivity.isBefore(now)) {
            lastCarActivity = now;
            updateState(CHANNEL_LAST_ACTIVITY, new DateTimeType(lastCarActivity));
        }
    }

    @Override
    public void dispose() {
        Future<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
    }

    @Override
    public void updateState(String channelID, State state) {
        super.updateState(channelID, state);
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }
}
