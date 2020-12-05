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
package org.openhab.binding.withings.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.withings.internal.WithingsBindingConstants;
import org.openhab.binding.withings.internal.api.device.DevicesResponseDTO;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.State;

/**
 * @author Sven Strohschein - Initial contribution
 */
@NonNullByDefault
public class SleepMonitorThingHandler extends AbstractWithingsDeviceThingHandler {

    public SleepMonitorThingHandler(Thing thing) {
        super(thing);
    }

    protected void updateChannels(DevicesResponseDTO.Device device) {
        for (Channel channel : getThing().getChannels()) {

            ChannelUID channelUID = channel.getUID();
            if (isLinked(channelUID)) {

                State state;
                if (WithingsBindingConstants.CHANNEL_SLEEP_MONITOR_LAST_CONNECTION.equals(channelUID.getId())) {
                    state = createDateTimeType(device.getLastSessionDate());
                } else {
                    throw new RuntimeException("Unknown channel \"" + channelUID.getId() + "\"!");
                }
                updateState(channelUID, state);
            }
        }
    }
}
