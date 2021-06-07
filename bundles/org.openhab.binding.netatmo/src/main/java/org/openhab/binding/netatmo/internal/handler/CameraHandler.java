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
package org.openhab.binding.netatmo.internal.handler;

import static org.openhab.binding.netatmo.internal.NetatmoBindingConstants.*;
import static org.openhab.binding.netatmo.internal.utils.ChannelTypeUtils.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.netatmo.internal.NetatmoDescriptionProvider;
import org.openhab.binding.netatmo.internal.api.ApiBridge;
import org.openhab.binding.netatmo.internal.api.NetatmoException;
import org.openhab.binding.netatmo.internal.api.dto.NAEvent;
import org.openhab.binding.netatmo.internal.api.dto.NAHomeEvent;
import org.openhab.binding.netatmo.internal.api.dto.NASnapshot;
import org.openhab.binding.netatmo.internal.api.dto.NAThing;
import org.openhab.binding.netatmo.internal.api.dto.NAWelcome;
import org.openhab.binding.netatmo.internal.channelhelper.AbstractChannelHelper;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CameraHandler} is the class used to handle Camera Data
 *
 * @author Sven Strohschein - Initial contribution (partly moved code from NAWelcomeCameraHandler to introduce
 *         inheritance, see NAWelcomeCameraHandler)
 *
 */
@NonNullByDefault
public class CameraHandler extends NetatmoDeviceHandler {
    private final Logger logger = LoggerFactory.getLogger(CameraHandler.class);
    private @Nullable CameraAddress cameraAddress;
    private @Nullable String vpnUrl;
    private boolean isLocal;
    private ZonedDateTime maxEventTime;

    public CameraHandler(Bridge bridge, List<AbstractChannelHelper> channelHelpers, ApiBridge apiBridge,
            NetatmoDescriptionProvider descriptionProvider) {
        super(bridge, channelHelpers, apiBridge, descriptionProvider);
        String lastEvent = editProperties().get(PROPERTY_MAX_EVENT_TIME);
        maxEventTime = lastEvent != null ? ZonedDateTime.parse(lastEvent) : Instant.EPOCH.atZone(ZoneOffset.UTC);
    }

    private @Nullable HomeSecurityHandler getHomeHandler() {
        NetatmoDeviceHandler handler = super.getBridgeHandler(getBridge());
        return handler != null ? (HomeSecurityHandler) handler : null;
    }

    @Override
    public void setNAThing(NAThing naModule) {
        super.setNAThing(naModule);
        NAWelcome camera = (NAWelcome) naModule;
        this.vpnUrl = camera.getVpnUrl();
        this.isLocal = camera.isLocal();
        HomeSecurityHandler homeHandler = getHomeHandler();
        if (homeHandler != null) {
            descriptionProvider.setStateOptions(
                    new ChannelUID(getThing().getUID(), GROUP_WELCOME_EVENT, CHANNEL_EVENT_PERSON_ID),
                    homeHandler.getKnownPersons().stream().map(p -> new StateOption(p.getId(), p.getName()))
                            .collect(Collectors.toList()));
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if ((command instanceof OnOffType) && (CHANNEL_CAMERA_IS_MONITORING.equals(channelUID.getIdWithoutGroup()))) {
            String localCameraURL = getLocalCameraURL();
            if (localCameraURL != null) {
                tryApiCall(() -> apiBridge.getHomeApi().changeStatus(localCameraURL, command == OnOffType.ON));
            }
        } else {
            super.handleCommand(channelUID, command);
        }
    }

    protected @Nullable String getLocalCameraURL() {
        CameraAddress address = cameraAddress;
        String vpn = vpnUrl;
        if (vpn != null) {
            // The local address is (re-)requested when it wasn't already determined or when
            // the vpn address was changed.
            if (address == null || address.isVpnURLChanged(vpn)) {
                String localUrl = pingVpnUrl(vpn);
                if (localUrl != null) {
                    address = new CameraAddress(vpn, localUrl);
                    cameraAddress = address;
                    return address.getLocalURL();
                }
            } else {
                return address.getLocalURL();
            }
        }
        return null;
    }

    @Override
    public void setEvent(NAEvent event) {
        if (event.getTime().isAfter(maxEventTime)) {
            maxEventTime = event.getTime();
            updateProperty(PROPERTY_MAX_EVENT_TIME, maxEventTime.toString());

            logger.debug("Updating camera with event : {}", event.toString());
            updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_TYPE, toStringType(event.getEventType()));
            updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_MESSAGE, toStringType(event.getMessage()));
            updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_TIME, new DateTimeType(event.getTime()));
            updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_PERSON_ID, toStringType(event.getPersonId()));
            updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_SUBTYPE,
                    event.getSubTypeDescription().map(d -> toStringType(d)).orElse(UnDefType.NULL));

            NASnapshot snapshot = event.getSnapshot();
            if (snapshot != null) {
                String url = snapshot.getUrl();
                updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_SNAPSHOT, toRawType(url));
                updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_SNAPSHOT_URL, toStringType(url));
            }
            if (event instanceof NAHomeEvent) {
                NAHomeEvent homeEvent = (NAHomeEvent) event;
                updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_VIDEO_STATUS,
                        toStringType(homeEvent.getVideoStatus()));
                updateIfLinked(GROUP_WELCOME_EVENT, CHANNEL_EVENT_VIDEO_URL,
                        toStringType(getStreamURL(homeEvent.getVideoId())));
            }
        }
    }

    private @Nullable String getStreamURL(@Nullable String videoId) {
        if (videoId != null && vpnUrl != null) {
            StringBuilder result = new StringBuilder(String.format("%s/vod/%s/index", vpnUrl, videoId));
            if (isLocal) {
                result.append("_local");
            }
            result.append(".m3u8");
            return result.toString();
        }
        return null;
    }

    private @Nullable String pingVpnUrl(String vpnUrl) {
        try {
            return apiBridge.getHomeApi().ping(vpnUrl);
        } catch (NetatmoException e) {
            logger.warn("Error pinging camera : {}", e.getMessage());
            return null;
        }
    }
}
