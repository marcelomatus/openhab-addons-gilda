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
package org.openhab.binding.freeboxos.internal.handler;

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.home.HomeManager;
import org.openhab.binding.freeboxos.internal.api.home.HomeNodeEndpointState;
import org.openhab.binding.freeboxos.internal.api.home.HomeNodeEndpointState.ValueType;
import org.openhab.binding.freeboxos.internal.config.ShutterConfiguration;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;

/**
 * The {@link ShutterHandler} is responsible for handling everything associated to any Freebox Home shutter thing type.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class ShutterHandler extends BasicShutterHandler {

    public ShutterHandler(Thing thing) {
        super(thing);
    }

    @Override
    void internalGetProperties(Map<String, String> properties) throws FreeboxException {
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        ShutterConfiguration config = getConfiguration();
        HomeNodeEndpointState state = getManager(HomeManager.class).getEndpointsState(config.id, config.stateSignalId);
        HomeNodeEndpointState position = getManager(HomeManager.class).getEndpointsState(config.id,
                config.positionSignalId);
        Double percent = null;
        if (state != null) {
            if (ValueType.BOOL.equals(position.getValueType())) {
                percent = Boolean.TRUE.equals(position.asBoolean()) ? 1.0 : 0.0;
            } else if (ValueType.INT.equals(position.getValueType())) {
                Integer inValue = position.asInt();
                if (inValue != null) {
                    percent = inValue.doubleValue() / 100.0;
                }
            }
        }
        updateChannelDecimal(BASIC_SHUTTER, BASIC_SHUTTER_CMD, percent);
    }

    private ShutterConfiguration getConfiguration() {
        return getConfigAs(ShutterConfiguration.class);
    }

    @Override
    protected boolean internalHandleCommand(ChannelUID channelUID, Command command) throws FreeboxException {
        if (BASIC_SHUTTER_CMD.equals(channelUID.getIdWithoutGroup())) {
            ShutterConfiguration config = getConfiguration();
            // if (UpDownType.UP.equals(command)) {
            // getManager(HomeManager.class).putCommand(config.id, config.upSlotId, true);
            // return true;
            // } else if (UpDownType.DOWN.equals(command)) {
            // getManager(HomeManager.class).putCommand(config.id, config.downSlotId, true);
            // return true;
            // } else if (StopMoveType.STOP.equals(command)) {
            // getManager(HomeManager.class).putCommand(config.id, config.stopSlotId, true);
            // return true;
            // }
        }
        return super.internalHandleCommand(channelUID, command);
    }
}
