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
package org.openhab.binding.nuki.internal.dataexchange;

import java.util.Collections;
import java.util.List;

import org.openhab.binding.nuki.internal.dto.BridgeApiListDeviceDto;

/**
 * The {@link BridgeListResponse} class wraps {@link BridgeApiListDeviceDto} class.
 *
 * @author Jan Vybíral - Initial contribution
 */
public class BridgeListResponse extends NukiBaseResponse {

    private final List<BridgeApiListDeviceDto> devices;

    public BridgeListResponse(int status, String message, List<BridgeApiListDeviceDto> devices) {
        super(status, message);
        setSuccess(devices != null);
        this.devices = devices == null ? Collections.emptyList() : Collections.unmodifiableList(devices);
    }

    public BridgeListResponse(NukiBaseResponse nukiBaseResponse) {
        this(nukiBaseResponse.getStatus(), nukiBaseResponse.getMessage(), null);
    }

    public List<BridgeApiListDeviceDto> getDevices() {
        return devices;
    }

    public BridgeApiListDeviceDto getDevice(String nukiId) {
        for (BridgeApiListDeviceDto device : this.devices) {
            if (device.getNukiId().equals(nukiId)) {
                return device;
            }
        }
        return null;
    }
}
