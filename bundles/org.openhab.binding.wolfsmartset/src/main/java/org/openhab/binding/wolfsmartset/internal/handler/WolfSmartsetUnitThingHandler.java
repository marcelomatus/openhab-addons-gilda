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
package org.openhab.binding.wolfsmartset.internal.handler;

import static org.openhab.binding.wolfsmartset.internal.WolfSmartsetBindingConstants.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.wolfsmartset.internal.config.WolfSmartsetUnitConfiguration;
import org.openhab.binding.wolfsmartset.internal.dto.GetParameterValuesDTO;
import org.openhab.binding.wolfsmartset.internal.dto.MenuItemTabViewDTO;
import org.openhab.binding.wolfsmartset.internal.dto.ParameterDescriptorDTO;
import org.openhab.binding.wolfsmartset.internal.dto.SubMenuEntryDTO;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WolfSmartsetUnitThingHandler} is responsible for updating the channels associated
 * with an WolfSmartset unit.
 *
 * @author Bo Biene - Initial contribution
 */
@NonNullByDefault
public class WolfSmartsetUnitThingHandler extends BaseThingHandler {

    public static final String CAPABILITY_ADC = "adc";
    public static final String CAPABILITY_CO2 = "co2";
    public static final String CAPABILITY_DRY_CONTACT = "dryContact";
    public static final String CAPABILITY_HUMIDITY = "humidity";
    public static final String CAPABILITY_OCCUPANCY = "occupancy";
    public static final String CAPABILITY_TEMPERATURE = "temperature";
    public static final String CAPABILITY_UNKNOWN = "unknown";

    private final Logger logger = LoggerFactory.getLogger(WolfSmartsetUnitThingHandler.class);

    private @NonNullByDefault({}) String unitId;
    private @Nullable Instant lastRefreshTime;

    private Map<String, State> stateCache = new ConcurrentHashMap<>();
    private Map<Long, ParameterDescriptorDTO> paramDescriptionMap = new ConcurrentHashMap<>();
    private @Nullable SubMenuEntryDTO submenu;
    private @Nullable MenuItemTabViewDTO tabmenu;

    public WolfSmartsetUnitThingHandler(Thing thing) {
        super(thing);
    }

    public @Nullable SubMenuEntryDTO getSubMenu() {
        return this.submenu;
    }

    public @Nullable MenuItemTabViewDTO getTabMenu() {
        return this.tabmenu;
    }

    public @Nullable Instant getLastRefreshTime() {
        return this.lastRefreshTime;
    }

    @Override
    public void initialize() {
        unitId = getConfigAs(WolfSmartsetUnitConfiguration.class).unitId;
        logger.debug("UnitThing: Initializing unit '{}'", unitId);
        clearSavedState();
        bridgeStatusChanged(getBridge().getStatusInfo());
    }

    @Override
    public void dispose() {
        logger.debug("UnitThing: Disposing unit '{}'", unitId);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            if (this.submenu != null && this.tabmenu != null) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING);
            }

        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    public void updateConfiguration(SubMenuEntryDTO submenu, MenuItemTabViewDTO tabmenu) {
        this.submenu = submenu;
        this.tabmenu = tabmenu;
        bridgeStatusChanged(getBridge().getStatusInfo());
        lastRefreshTime = null;

        ThingBuilder thingBuilder = editThing();
        var thingId = thing.getUID();

        paramDescriptionMap.clear();
        for (var param : tabmenu.ParameterDescriptors) {
            paramDescriptionMap.put(param.ValueId, param);
            var channelId = new ChannelUID(thingId, param.ParameterId.toString()); // "bindingId:type:thingId:1")
            if (thing.getChannel(channelId) == null) {
                logger.info("UnitThing: Create channel '{}'", channelId);
                Channel channel = ChannelBuilder.create(channelId, getItemType(param.ControlType)).withLabel(param.Name)
                        .withType(getChannelType(param)).build();
                thingBuilder.withChannel(channel);
            }
        }

        updateThing(thingBuilder.build());

        for (var param : tabmenu.ParameterDescriptors) {
            var channelId = new ChannelUID(thingId, param.ParameterId.toString());
            setState(channelId, WolfSmartsetUtils.undefOrString(param.Value));
        }
    }

    private void setState(ChannelUID channelId, State state) {
        stateCache.put(channelId.getId(), state);
        updateState(channelId, state);
    }

    private ChannelTypeUID getChannelType(ParameterDescriptorDTO parmeter) {
        if (parmeter.Unit == null || parmeter.Unit.isBlank()) {
            if (parmeter.ControlType == null) {
                return new ChannelTypeUID(BINDING_ID, CH_STRING);
            } else {
                switch (parmeter.ControlType) {
                    case 1:
                    case 3:
                    case 6:
                    case 8:
                        return new ChannelTypeUID(BINDING_ID, CH_NUMBER);
                    case 5:
                        return new ChannelTypeUID(BINDING_ID, CH_CONTACT);
                    case 9:
                    case 10:
                        return new ChannelTypeUID(BINDING_ID, CH_DATETIME);
                    default:
                        return new ChannelTypeUID(BINDING_ID, CH_STRING);
                }
            }
        } else {
            switch (parmeter.Unit) {
                case "bar":
                    return new ChannelTypeUID("barometric-pressure");
                case "%":
                case "Std":
                    return new ChannelTypeUID(BINDING_ID, CH_NUMBER);
                case "°C":
                    return new ChannelTypeUID(BINDING_ID, CH_TEMPERATURE);
                default:
                    return new ChannelTypeUID(BINDING_ID, CH_STRING);
            }
        }
    }

    private String getItemType(Integer controlType) {
        switch (controlType) {
            case 1:

            case 3:
            case 6:
            case 8:
                return "Number";
            case 5:
                return "Contact";
            case 9:
            case 10:
                return "DateTime";
            default:
                return "String";
        }
    }

    public void updateValues(@Nullable GetParameterValuesDTO values) {
        var thingId = thing.getUID();
        if (values != null && values.getValues() != null && values.getValues().size() > 0) {
            if (!values.getIsNewJobCreated())
                lastRefreshTime = Instant.now();

            for (var value : values.getValues()) {
                var param = paramDescriptionMap.get(value.getValueId());
                if (param != null) {
                    var channelId = new ChannelUID(thingId, param.ParameterId.toString());
                    setState(channelId, WolfSmartsetUtils.undefOrString(value.getValue()));
                }
            }
        } else {

        }
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            State state = stateCache.get(channelUID.getId());
            if (state != null) {
                updateState(channelUID.getId(), state);
            }
            return;
        }
    }

    private void clearSavedState() {
        stateCache.clear();
    }
}
