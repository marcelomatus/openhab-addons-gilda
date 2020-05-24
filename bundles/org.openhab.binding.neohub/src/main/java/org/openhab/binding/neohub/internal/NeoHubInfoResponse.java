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
package org.openhab.binding.neohub.internal;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * A wrapper around the JSON response to the JSON INFO request
 *
 * @author Sebastian Prehn - Initial contribution
 * @author Andrew Fiddian-Green - Refactoring for openHAB v2.x
 * 
 */
@NonNullByDefault
public class NeoHubInfoResponse extends NeoHubAbstractDeviceData {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(NeohubBool.class, new NeohubBoolDeserializer()).create();

    @Nullable
    @SerializedName("devices")
    private List<InfoRecord> deviceRecords;

    @SuppressWarnings("null")
    @NonNullByDefault
    static class StatMode {
        @Nullable
        @SerializedName("MANUAL_OFF")
        private NeohubBool manualOff;
        @Nullable
        @SerializedName("MANUAL_ON")
        private NeohubBool manualOn;

        private boolean stateManualOn() {
            NeohubBool manualOn = this.manualOn;
            return (manualOn == null ? false : manualOn.value);
        }

        private boolean stateManualOff() {
            NeohubBool manualOff = this.manualOff;
            return (manualOff == null ? false : manualOff.value);
        }
    }

    @SuppressWarnings("null")
    @NonNullByDefault
    public static class InfoRecord extends AbstractRecord {
        @Nullable
        @SerializedName("device")
        private String deviceName;
        @Nullable
        @SerializedName("CURRENT_SET_TEMPERATURE")
        private BigDecimal currentSetTemperature;
        @Nullable
        @SerializedName("CURRENT_TEMPERATURE")
        private BigDecimal currentTemperature;
        @Nullable
        @SerializedName("CURRENT_FLOOR_TEMPERATURE")
        private BigDecimal currentFloorTemperature;
        @Nullable
        @SerializedName("COOL_INP")
        private NeohubBool coolInput;
        @Nullable
        @SerializedName("LOW_BATTERY")
        private NeohubBool batteryLow;
        @Nullable
        @SerializedName("STANDBY")
        private NeohubBool standby;
        @Nullable
        @SerializedName("HEATING")
        private NeohubBool heating;
        @Nullable
        @SerializedName("PREHEAT")
        private NeohubBool preHeat;
        @Nullable
        @SerializedName("TIMER")
        private NeohubBool timerOn;
        @Nullable
        @SerializedName("DEVICE_TYPE")
        private BigDecimal deviceType;
        @SerializedName("OFFLINE")
        @Nullable
        private NeohubBool offline;
        @Nullable
        @SerializedName("STAT_MODE")
        private StatMode statMode = new StatMode();

        private boolean safeBoolean(@Nullable NeohubBool value) {
            return (value == null ? false : value.value);
        }

        @Override
        public String getDeviceName() {
            String deviceName = this.deviceName;
            return deviceName != null ? deviceName : "";
        }

        @Override
        public BigDecimal getTargetTemperature() {
            return safeBigDecimal(currentSetTemperature);
        }

        @Override
        public BigDecimal getActualTemperature() {
            return safeBigDecimal(currentTemperature);
        }

        @Override
        public BigDecimal getFloorTemperature() {
            return safeBigDecimal(currentFloorTemperature);
        }

        @Override
        public boolean isStandby() {
            return safeBoolean(standby);
        }

        @Override
        public boolean isHeating() {
            return safeBoolean(heating);
        }

        @Override
        public boolean isPreHeating() {
            return safeBoolean(preHeat);
        }

        @Override
        public boolean isTimerOn() {
            return safeBoolean(timerOn);
        }

        @Override
        public boolean offline() {
            return safeBoolean(offline);
        }

        @Override
        public boolean stateManual() {
            StatMode statMode = this.statMode;
            return (statMode != null && statMode.stateManualOn());
        }

        @Override
        public boolean stateAuto() {
            StatMode statMode = this.statMode;
            return (statMode != null && statMode.stateManualOff());
        }

        @Override
        public boolean isWindowOpen() {
            // legacy API misuses the cool input parameter
            return safeBoolean(coolInput);
        }

        @Override
        public boolean isBatteryLow() {
            return safeBoolean(batteryLow);
        }

        public int getDeviceType() {
            BigDecimal deviceType = this.deviceType;
            return deviceType != null ? deviceType.intValue() : -1;
        }
    }

    /**
     * Create wrapper around a JSON string
     * 
     * @param fromJson the JSON string
     * @return a NeoHubInfoResponse wrapper around the JSON
     * @throws JsonSyntaxException
     * 
     */
    @Nullable
    public static NeoHubInfoResponse createDeviceData(String fromJson) throws JsonSyntaxException {
        return GSON.fromJson(fromJson, NeoHubInfoResponse.class);
    }

    /**
     * returns the device record corresponding to a given device name
     * 
     * @param deviceName the device name
     * @return its respective device record
     */
    @Override
    @Nullable
    public AbstractRecord getDeviceRecord(String deviceName) {
        List<InfoRecord> deviceRecords = this.deviceRecords;
        if (deviceRecords != null) {
            for (AbstractRecord deviceRecord : deviceRecords) {
                if (deviceName.equals(deviceRecord.getDeviceName())) {
                    return deviceRecord;
                }
            }
        }
        return null;
    }

    /**
     * @return the full list of device records
     */
    @Override
    @Nullable
    public List<?> getDevices() {
        return deviceRecords;
    }
}
