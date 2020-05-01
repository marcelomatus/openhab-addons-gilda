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
package org.openhab.binding.surepetcare.internal.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Thing;

/**
 * The {@link SurePetcareDevice} is the Java class used
 * as a DTO to represent a Sure Petcare device, such as a hub, a cat flap, a feeder etc.
 *
 * @author Rene Scherer - Initial contribution
 */
public class SurePetcareDevice extends SurePetcareBaseObject {

    public enum ProductType {

        UNKNOWN(0, "Unknown"),
        HUB(1, "Hub"),
        PET_FLAP(3, "Pet Flap"),
        PET_FEEDER(4, "Pet Feeder"),
        CAT_FLAP(6, "Cat Flap");

        public final Integer id;
        public final String name;

        private ProductType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public static @NonNull ProductType findByTypeId(final int id) {
            return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(UNKNOWN);
        }
    }

    public Integer parentDeviceId;
    public Integer productId;
    public Integer householdId;
    public String name;
    public String serialNumber;
    public String macAddress;
    public Integer index;
    public Date pairingAt;
    public SurePetcareDeviceControl control = new SurePetcareDeviceControl();
    public SurePetcareDevice parent;
    public SurePetcareDeviceStatus status = new SurePetcareDeviceStatus();

    @Override
    public Map<@NonNull String, String> getThingProperties() {
        Map<@NonNull String, String> properties = super.getThingProperties();
        properties.put("householdId", householdId.toString());
        properties.put("productType", productId.toString());
        properties.put("productName", ProductType.findByTypeId(productId).name);
        properties.put(Thing.PROPERTY_MAC_ADDRESS, macAddress);
        properties.put(Thing.PROPERTY_SERIAL_NUMBER, serialNumber);
        if (status.version.device != null) {
            properties.put(Thing.PROPERTY_HARDWARE_VERSION, status.version.device.hardware);
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, status.version.device.firmware);
        }
        if (status.version.lcd != null) {
            properties.put(Thing.PROPERTY_HARDWARE_VERSION, status.version.lcd.hardware);
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, status.version.lcd.firmware);
        }
        if (status.version.rf != null) {
            properties.put("rfHardwareVersion", status.version.rf.hardware);
            properties.put("rfFirmwareVersion", status.version.rf.firmware);
        }
        if (pairingAt != null) {
            properties.put("pairingAt", pairingAt.toString());
        }
        return properties;
    }

    @Override
    public String toString() {
        return "Device [id=" + id + ", name=" + name + ", product=" + ProductType.findByTypeId(productId).name + "]";
    }

    public SurePetcareDevice assign(SurePetcareDevice newdev) {
        super.assign(newdev);
        this.parentDeviceId = newdev.parentDeviceId;
        this.productId = newdev.productId;
        this.householdId = newdev.productId;
        this.name = newdev.name;
        this.serialNumber = newdev.serialNumber;
        this.macAddress = newdev.macAddress;
        this.index = newdev.index;
        this.pairingAt = newdev.pairingAt;
        this.control = newdev.control;
        this.parent = newdev.parent;
        this.status = newdev.status;
        return this;
    }

}
