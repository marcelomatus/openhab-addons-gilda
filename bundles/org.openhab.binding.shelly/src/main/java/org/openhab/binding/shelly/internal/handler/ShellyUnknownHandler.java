/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.shelly.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.shelly.internal.ShellyHandlerFactory;
import org.openhab.binding.shelly.internal.coap.ShellyCoapServer;
import org.openhab.binding.shelly.internal.config.ShellyBindingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ShellyUnknownHandler} implements a dummy handler for password protected or unknown devices.
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class ShellyUnknownHandler extends ShellyBaseHandler {
    private final Logger logger = LoggerFactory.getLogger(ShellyUnknownHandler.class);

    public ShellyUnknownHandler(Thing thing, ShellyHandlerFactory handlerFactory,
            ShellyBindingConfiguration bindingConfig, @Nullable ShellyCoapServer coapServer) {
        super(thing, handlerFactory, bindingConfig, coapServer);
    }

    @Override
    public void initialize() {
        logger.debug("Thing is using class {}", this.getClass());
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "Userid / password missing or thing type is unknown!");
    }
}
