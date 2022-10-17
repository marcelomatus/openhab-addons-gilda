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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private @Nullable ScheduledFuture<?> chargeStateJob;
    private @Nullable ScheduledFuture<?> vehicleStateJob;

    public SAICiSMARTHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // no commands supported yet
    }

    protected @Nullable SAICiSMARTBridgeHandler getBridgeHandler() {
        return (SAICiSMARTBridgeHandler) super.getBridge().getHandler();
    }

    @Override
    public void initialize() {
        config = getConfigAs(SAICiSMARTVehicleConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        chargeStateJob = scheduler.scheduleWithFixedDelay(new ChargeStateUpdater(this), 10, 30, TimeUnit.SECONDS);
        vehicleStateJob = scheduler.scheduleWithFixedDelay(new VehicleStateUpdater(this), 10, 30, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = chargeStateJob;
        if (job != null) {
            job.cancel(true);
            chargeStateJob = null;
        }

        job = vehicleStateJob;
        if (job != null) {
            job.cancel(true);
            vehicleStateJob = null;
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
