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
package org.openhab.binding.cul.internal.handler;

import org.openhab.binding.cul.internal.CULManager;
import org.openhab.binding.cul.internal.CULMode;
import org.openhab.binding.cul.internal.config.MaxCULBridgeConfiguration;
import org.openhab.binding.cul.internal.serial.CULSerialConfigFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Goehr (johgoe) - Initial contribution
 */
public class CulMaxBridgeHandler extends CulCunBaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(CulMaxBridgeHandler.class);

    private MaxCULBridgeConfiguration config;

    public CulMaxBridgeHandler(Bridge thing, CULManager manager) {
        super(thing, manager);
    }

    @Override
    public void initialize() {
        config = getConfigAs(MaxCULBridgeConfiguration.class);
        if (config.serialPort.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Serial port is not configured.");
            return;
        }
        updateStatus(ThingStatus.UNKNOWN);
        culConfig = new CULSerialConfigFactory().create("serial", config.serialPort, CULMode.MAX, config.baudrate,
                config.parity);
        logger.debug("Schedule connect to cul");
        scheduler.submit(this::connect);
    }
}
