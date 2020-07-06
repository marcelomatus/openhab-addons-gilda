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
package org.openhab.binding.draytonwiser.internal.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.draytonwiser.internal.model.DeviceDTO;

/**
 *
 * @author Andrew Schofield - Initial contribution
 * @author Hilbrand Bouwkamp - Put all device property setting in a separate class
 */
@NonNullByDefault
public final class DraytonWiserPropertyHelper {

    private DraytonWiserPropertyHelper() {
        // helper class
    }

    public static void setControllerProperties(final DeviceDTO device, final Map<String, Object> properties) {
        setGeneralDeviceProperties(device, properties);
    }

    public static void setPropertiesWithSerialNumber(final DeviceDTO device, final Map<String, Object> properties) {
        properties.put("serialNumber", device.getSerialNumber());
        setGeneralDeviceProperties(device, properties);
    }

    public static <T> void setGeneralDeviceProperties(final DeviceDTO device, final Map<String, T> properties) {
        properties.put("Device Type", (T) device.getProductIdentifier());
        properties.put("Firmware Version", (T) device.getActiveFirmwareVersion());
        properties.put("Manufacturer", (T) device.getManufacturer());
        properties.put("Model", (T) device.getModelIdentifier());
    }
}
