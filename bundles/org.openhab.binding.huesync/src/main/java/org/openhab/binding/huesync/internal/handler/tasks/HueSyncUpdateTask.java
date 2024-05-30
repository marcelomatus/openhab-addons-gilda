/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.huesync.internal.handler.tasks;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.huesync.internal.api.dto.device.HueSyncDeviceDto;
import org.openhab.binding.huesync.internal.connection.HueSyncDeviceConnection;
import org.openhab.binding.huesync.internal.log.HueSyncLogFactory;
import org.slf4j.Logger;

/**
 * Task to handle device information update.
 * 
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
public class HueSyncUpdateTask implements Runnable {

    private final Logger logger = HueSyncLogFactory.getLogger(HueSyncUpdateTask.class);

    private HueSyncDeviceConnection connection;
    private HueSyncDeviceDto deviceInfo;

    private Consumer<@Nullable HueSyncUpdateInfo> action;

    public HueSyncUpdateTask(HueSyncDeviceConnection connection, HueSyncDeviceDto deviceInfo,
            Consumer<@Nullable HueSyncUpdateInfo> action) {

        this.connection = connection;
        this.deviceInfo = deviceInfo;

        this.action = action;
    }

    @Override
    public void run() {
        try {
            if (this.connection.isRegistered()) {
                this.logger.trace("Status update query for {} {}:{}", this.deviceInfo.name, this.deviceInfo.deviceType,
                        this.deviceInfo.uniqueId);

                HueSyncUpdateInfo updateInfo = new HueSyncUpdateInfo();

                updateInfo.deviceStatus = this.connection.getDetailedDeviceInfo();
                updateInfo.hdmiStatus = this.connection.getHdmiInfo();
                updateInfo.execution = this.connection.getExecutionInfo();

                this.action.accept(updateInfo);
            }
        } catch (Exception e) {
            this.logger.debug("{}", e.getMessage());
        }
    }
}
