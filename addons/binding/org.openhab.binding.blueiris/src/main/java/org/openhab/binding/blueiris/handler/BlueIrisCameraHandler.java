/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.blueiris.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.blueiris.internal.control.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BlueIrisCameraHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Bennett - Initial contribution
 */
public class BlueIrisCameraHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(BlueIrisCameraHandler.class);
    private Connection connection;

    public BlueIrisCameraHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
    }
}
