/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.lgthinq.internal.handler;

import static org.openhab.binding.lgthinq.internal.LGThinQBindingConstants.*;

import java.util.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.lgthinq.internal.LGThinQStateDescriptionProvider;
import org.openhab.binding.lgthinq.internal.errors.LGThinqApiException;
import org.openhab.binding.lgthinq.internal.type.ThinqChannelGroupTypeProvider;
import org.openhab.binding.lgthinq.internal.type.ThinqChannelTypeProvider;
import org.openhab.binding.lgthinq.lgservices.LGThinQApiClientService;
import org.openhab.binding.lgthinq.lgservices.LGThinQApiClientServiceFactory;
import org.openhab.binding.lgthinq.lgservices.LGThinQDishWasherApiClientService;
import org.openhab.binding.lgthinq.lgservices.model.*;
import org.openhab.binding.lgthinq.lgservices.model.devices.dishwasher.DishWasherCapability;
import org.openhab.binding.lgthinq.lgservices.model.devices.dishwasher.DishWasherSnapshot;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LGThinQDishWasherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nemer Daud - Initial contribution
 */
@NonNullByDefault
public class LGThinQDishWasherHandler extends LGThinQAbstractDeviceHandler<DishWasherCapability, DishWasherSnapshot> {

    private final LGThinQStateDescriptionProvider stateDescriptionProvider;
    private final ChannelUID courseChannelUID;
    private final ChannelUID remainTimeChannelUID;
    private final ChannelUID stateChannelUID;
    private final ChannelUID processStateChannelUID;
    private final ChannelUID doorLockChannelUID;

    public final ChannelGroupUID channelGroupDashboardUID;

    private final Logger logger = LoggerFactory.getLogger(LGThinQDishWasherHandler.class);
    @NonNullByDefault
    private final LGThinQDishWasherApiClientService lgThinqDishWasherApiClientService;

    public LGThinQDishWasherHandler(Thing thing, LGThinQStateDescriptionProvider stateDescriptionProvider,
            ThinqChannelTypeProvider channelTypeProvider, ThinqChannelGroupTypeProvider channelGroupTypeProvider,
            ItemChannelLinkRegistry itemChannelLinkRegistry, HttpClientFactory httpClientFactory) {
        super(thing, stateDescriptionProvider, itemChannelLinkRegistry);
        this.thinqChannelGroupProvider = channelGroupTypeProvider;
        this.thinqChannelProvider = channelTypeProvider;
        this.stateDescriptionProvider = stateDescriptionProvider;
        lgThinqDishWasherApiClientService = LGThinQApiClientServiceFactory.newDishWasherApiClientService(lgPlatformType,
                httpClientFactory);
        channelGroupDashboardUID = new ChannelGroupUID(getThing().getUID(), CHANNEL_DASHBOARD_GRP_ID);
        courseChannelUID = new ChannelUID(channelGroupDashboardUID, WM_CHANNEL_COURSE_ID);
        stateChannelUID = new ChannelUID(channelGroupDashboardUID, WM_CHANNEL_STATE_ID);
        processStateChannelUID = new ChannelUID(channelGroupDashboardUID, WM_CHANNEL_PROCESS_STATE_ID);
        remainTimeChannelUID = new ChannelUID(channelGroupDashboardUID, WM_CHANNEL_REMAIN_TIME_ID);
        doorLockChannelUID = new ChannelUID(channelGroupDashboardUID, WM_CHANNEL_DOOR_LOCK_ID);
    }

    private void loadOptionsCourse(DishWasherCapability cap, ChannelUID courseChannel) {
        List<StateOption> optionsCourses = new ArrayList<>();
        cap.getCourses().forEach((k, v) -> optionsCourses.add(new StateOption(k, emptyIfNull(v.getCourseName()))));
        stateDescriptionProvider.setStateOptions(courseChannel, optionsCourses);
    }

    @Override
    public void updateChannelDynStateDescription() throws LGThinqApiException {
        DishWasherCapability dwCap = getCapabilities();

        List<StateOption> options = new ArrayList<>();
        dwCap.getStateFeat().getValuesMapping()
                .forEach((k, v) -> options.add(new StateOption(k, keyIfValueNotFound(CAP_DW_STATE, v))));
        stateDescriptionProvider.setStateOptions(stateChannelUID, options);

        loadOptionsCourse(dwCap, courseChannelUID);

        List<StateOption> optionsDoor = new ArrayList<>();
        dwCap.getDoorStateFeat().getValuesMapping()
                .forEach((k, v) -> optionsDoor.add(new StateOption(k, keyIfValueNotFound(CAP_DW_DOOR_STATE, v))));
        stateDescriptionProvider.setStateOptions(doorLockChannelUID, optionsDoor);

        List<StateOption> optionsPre = new ArrayList<>();
        dwCap.getProcessState().getValuesMapping()
                .forEach((k, v) -> optionsPre.add(new StateOption(k, keyIfValueNotFound(CAP_DW_PROCESS_STATE, v))));
        stateDescriptionProvider.setStateOptions(processStateChannelUID, optionsPre);
    }

    @Override
    public LGThinQApiClientService<DishWasherCapability, DishWasherSnapshot> getLgThinQAPIClientService() {
        return lgThinqDishWasherApiClientService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void updateDeviceChannels(DishWasherSnapshot shot) {
        DishWasherSnapshot lastShot = getLastShot();
        updateState("dashboard#" + CHANNEL_POWER_ID,
                (DevicePowerState.DV_POWER_ON.equals(shot.getPowerStatus()) ? OnOffType.ON : OnOffType.OFF));
        updateState(stateChannelUID, new StringType(shot.getState()));
        updateState(processStateChannelUID, new StringType(shot.getProcessState()));
        updateState(courseChannelUID, new StringType(shot.getCourse()));
        updateState(doorLockChannelUID, new StringType(shot.getDoorLock()));
        updateState(remainTimeChannelUID, new StringType(shot.getRemainingTime()));
        final List<Channel> dynChannels = new ArrayList<>();
        // only can have remote start channel is the WM is not in sleep mode, and remote start is enabled.
    }

    @Override
    protected DeviceTypes getDeviceType() {
        if (THING_TYPE_WASHING_MACHINE.equals(getThing().getThingTypeUID())) {
            return DeviceTypes.WASHERDRYER_MACHINE;
        } else if (THING_TYPE_WASHING_TOWER.equals(getThing().getThingTypeUID())) {
            return DeviceTypes.WASHING_TOWER;
        } else {
            throw new IllegalArgumentException(
                    "DeviceTypeUuid [" + getThing().getThingTypeUID() + "] not expected for WashingTower/Machine");
        }
    }

    @Override
    protected void processCommand(AsyncCommandParams params) throws LGThinqApiException {
        logger.error("Command {} to the channel {} not supported. Ignored.", params.command, params.channelUID);
    }

    @Override
    public void onDeviceAdded(LGDevice device) {
        // TODO - handle it. Think if it's needed
    }

    @Override
    public String getDeviceAlias() {
        return emptyIfNull(getThing().getProperties().get(DEVICE_ALIAS));
    }

    @Override
    public String getDeviceUriJsonConfig() {
        return emptyIfNull(getThing().getProperties().get(MODEL_URL_INFO));
    }

    @Override
    public void onDeviceRemoved() {
        // TODO - HANDLE IT, Think if it's needed
    }

    /**
     * Put the channels in default state if the device is disconnected or gone.
     */
    @Override
    public void onDeviceDisconnected() {
        updateState(CHANNEL_POWER_ID, OnOffType.OFF);
        updateState(WM_CHANNEL_STATE_ID, new StringType(WM_POWER_OFF_VALUE));
        updateState(WM_CHANNEL_COURSE_ID, new StringType("NOT_SELECTED"));
        updateState(WM_CHANNEL_SMART_COURSE_ID, new StringType("NOT_SELECTED"));
        updateState(WM_CHANNEL_DOOR_LOCK_ID, new StringType("DOOR_LOCK_OFF"));
        updateState(WM_CHANNEL_REMAIN_TIME_ID, new StringType("00:00"));
    }
}
