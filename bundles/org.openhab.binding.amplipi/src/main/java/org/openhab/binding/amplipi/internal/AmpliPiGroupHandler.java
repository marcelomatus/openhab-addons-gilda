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
package org.openhab.binding.amplipi.internal;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.amplipi.internal.model.Group;
import org.openhab.binding.amplipi.internal.model.GroupUpdate;
import org.openhab.binding.amplipi.internal.model.Status;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link AmpliPiGroupHandler} is responsible for handling commands, which are
 * sent to one of the AmpliPi Groups.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class AmpliPiGroupHandler extends BaseThingHandler implements AmpliPiStatusChangeListener {

    private final Logger logger = LoggerFactory.getLogger(AmpliPiGroupHandler.class);

    private final HttpClient httpClient;
    private final Gson gson;

    private @Nullable AmpliPiHandler bridgeHandler;

    public AmpliPiGroupHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
        this.gson = new Gson();
    }

    private int getId(Thing thing) {
        return Integer.valueOf(thing.getConfiguration().get(AmpliPiBindingConstants.CFG_PARAM_ID).toString());
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            bridgeHandler = (AmpliPiHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                bridgeHandler.addStatusChangeListener(this);
            } else {
                throw new IllegalStateException("Bridge handler must not be null here!");
            }
            if (bridge.getStatus() == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            throw new IllegalStateException("Bridge must not be null here!");
        }
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (command == RefreshType.REFRESH) {
            // do nothing - we just wait for the next automatic refresh
            return;
        }
        GroupUpdate update = new GroupUpdate();
        switch (channelUID.getId()) {
            case AmpliPiBindingConstants.CHANNEL_MUTE:
                if (command instanceof OnOffType) {
                    update.setMute(command == OnOffType.ON);
                }
                break;
            case AmpliPiBindingConstants.CHANNEL_VOLUME:
                if (command instanceof PercentType) {
                    update.setVolDelta(AmpliPiUtils.percentTypeToVolume((PercentType) command));
                }
                break;
            case AmpliPiBindingConstants.CHANNEL_SOURCE:
                if (command instanceof DecimalType) {
                    update.setSourceId(((DecimalType) command).intValue());
                }
                break;
        }
        if (bridgeHandler != null) {
            String url = bridgeHandler.getUrl() + "/api/groups/" + getId(thing);
            ;
            StringContentProvider contentProvider = new StringContentProvider(gson.toJson(update));
            try {
                ContentResponse response = httpClient.newRequest(url).method(HttpMethod.PATCH)
                        .content(contentProvider, "application/json").send();
                if (response.getStatus() != HttpStatus.OK_200) {
                    logger.error("AmpliPi API returned HTTP status {}.", response.getStatus());
                    logger.debug("Content: {}", response.getContentAsString());
                } else {
                    updateStatus(ThingStatus.ONLINE);
                }
            } catch (InterruptedException | TimeoutException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "AmpliPi request failed: " + e.getMessage());
            } catch (ExecutionException e) {
                logger.error("HTTP request to AmpliPi failed: {}.", e.getMessage());
                logger.debug("{}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void receive(@NonNull Status status) {
        int id = getId(thing);
        Optional<Group> group = status.getGroups().stream().filter(z -> z.getId().equals(id)).findFirst();
        if (group.isPresent()) {
            Boolean mute = group.get().getMute();
            Integer volume = group.get().getVolDelta();
            Integer source = group.get().getSourceId();
            updateState(AmpliPiBindingConstants.CHANNEL_MUTE, mute ? OnOffType.ON : OnOffType.OFF);
            updateState(AmpliPiBindingConstants.CHANNEL_VOLUME, AmpliPiUtils.volumeToPercentType(volume));
            updateState(AmpliPiBindingConstants.CHANNEL_SOURCE, new DecimalType(source));
        }
    }
}
