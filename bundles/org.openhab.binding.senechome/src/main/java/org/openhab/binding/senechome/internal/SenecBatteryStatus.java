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
package org.openhab.binding.senechome.internal;

/**
 * The {@link SenecBatteryStatus} class defines available Senec specific
 * battery states.
 *
 * @author Steven Schwarznau - Initial contribution
 *
 */
public enum SenecBatteryStatus {

    INITIALSTATE(0, "INITIAL STATE"),
    ERROR_INVERTER_COMMUNICATION(1, "ERROR INVERTER COMMUNICATION"),
    ERROR_ELECTRICY_METER(2, "ERROR ELECTRICY METER"),
    RIPPLE_CONTROL_RECEIVER(3, "RIPPLE CONTROL RECEIVER"),
    INITIAL_CHARGE(4, "INITIAL CHARGE"),
    MAINTENANCE_CHARGE(5, "MAINTENANCE CHARGE"),
    MAINTENANCE_READY(6, "MAINTENANCE READY"),
    MAINTENANCE_REQUIRED(7, "MAINTENANCE REQUIRED"),
    MAN_SAFETY_CHARGE(8, "MAN. SAFETY CHARGE"),
    SAFETY_CHARGE_READY(9, "SAFETY CHARGE READY"),
    FULL_CHARGE(10, "FULL CHARGE"),
    EQUALIZATION_CHARGE(11, "EQUALIZATION: CHARGE"),
    DESULFATATION_CHARGE(12, "DESULFATATION: CHARGE"),
    BATTERY_FULL(13, "BATTERY FULL"),
    CHARGE(14, "CHARGE"),
    BATTERY_EMPTY(15, "BATTERY EMPTY"),
    DISCHARGE(16, "DISCHARGE"),
    PV_AND_DISCHARGE(17, "PV + DISCHARGE"),
    GRID_AND_DISCHARGE(18, "GRID + DISCHARGE"),
    PASSIVE(19, "PASSIVE"),
    OFF(20, "OFF"),
    OWN_CONSUMPTION(21, "OWN CONSUMPTION"),
    RESTART(22, "RESTART"),
    MAN_EQUALIZATION_CHARGE(23, "MAN. EQUALIZATION: CHARGE"),
    MAN_DESULFATATION_CHARGE(24, "MAN. DESULFATATION: CHARGE"),
    SAFETY_CHARGE(25, "SAFETY CHARGE"),
    BATTERY_PROTECTION_MODE(26, "BATTERY PROTECTION MODE"),
    EG_ERROR(27, "EG ERROR"),
    EG_CHARGE(28, "EG CHARGE"),
    EG_DISCHARGE(29, "EG DISCHARGE"),
    EG_PASSIVE(30, "EG PASSIVE"),
    EG_PROHIBIT_CHARGE(31, "EG PROHIBIT CHARGE"),
    EG_PROHIBIT_DISCHARGE(32, "EG PROHIBIT DISCHARGE"),
    EMERGANCY_CHARGE(33, "EMERGANCY CHARGE"),
    SOFTWARE_UPDATE(34, "SOFTWARE UPDATE"),
    NSP_ERROR(35, "NSP ERROR"),
    NSP_ERROR_GRID(36, "NSP ERROR: GRID"),
    NSP_ERROR_HARDWRE(37, "NSP ERROR: HARDWRE"),
    NO_SERVER_CONNECTION(38, "NO SERVER CONNECTION"),
    BMS_ERROR(39, "BMS ERROR"),
    MAINTENANCE_FILTER(40, "MAINTENANCE: FILTER"),
    SLEEPING_MODE(41, "SLEEPING MODE"),
    WAITING_EXCESS(42, "WAITING EXCESS"),
    CAPACITY_TEST_CHARGE(43, "CAPACITY TEST: CHARGE"),
    CAPACITY_TEST_DISCHARGE(44, "CAPACITY TEST: DISCHARGE"),
    MAN_DESULFATATION_WAIT(45, "MAN. DESULFATATION: WAIT"),
    MAN_DESULFATATION_READY(46, "MAN. DESULFATATION: READY"),
    MAN_DESULFATATION_ERROR(47, "MAN. DESULFATATION: ERROR"),
    EQUALIZATION_WAIT(48, "EQUALIZATION: WAIT"),
    EMERGANCY_CHARGE_ERROR(49, "EMERGANCY CHARGE: ERROR"),
    MAN_EQUALIZATION_WAIT(50, "MAN. EQUALIZATION: WAIT"),
    MAN_EQUALIZATION_ERROR(51, "MAN. EQUALIZATION: ERROR"),
    MAN_EQUALIZATION_READY(52, "MAN: EQUALIZATION: READY"),
    AUTO_DESULFATATION_WAIT(53, "AUTO. DESULFATATION: WAIT"),
    ABSORPTION_PHASE(54, "ABSORPTION PHASE"),
    DCSWITCH_OFF(55, "DC-SWITCH OFF"),
    PEAKSHAVING_WAIT(56, "PEAK-SHAVING: WAIT"),
    ERROR_BATTERY_INVERTER(57, "ERROR BATTERY INVERTER"),
    NPUERROR(58, "NPU-ERROR"),
    BMS_OFFLINE(59, "BMS OFFLINE"),
    MAINTENANCE_CHARGE_ERROR(60, "MAINTENANCE CHARGE ERROR"),
    MAN_SAFETY_CHARGE_ERROR(61, "MAN. SAFETY CHARGE ERROR"),
    SAFETY_CHARGE_ERROR(62, "SAFETY CHARGE ERROR"),
    NO_CONNECTION_TO_MASTER(63, "NO CONNECTION TO MASTER"),
    LITHIUM_SAFE_MODE_ACTIVE(64, "LITHIUM SAFE MODE ACTIVE"),
    LITHIUM_SAFE_MODE_DONE(65, "LITHIUM SAFE MODE DONE"),
    BATTERY_VOLTAGE_ERROR(66, "BATTERY VOLTAGE ERROR"),
    BMS_DC_SWITCHED_OFF(67, "BMS DC SWITCHED OFF"),
    GRID_INITIALIZATION(68, "GRID INITIALIZATION"),
    GRID_STABILIZATION(69, "GRID STABILIZATION"),
    REMOTE_SHUTDOWN(70, "REMOTE SHUTDOWN"),
    OFFPEAKCHARGE(71, "OFFPEAK-CHARGE"),
    ERROR_HALFBRIDGE(72, "ERROR HALFBRIDGE"),
    BMS_ERROR_OPERATING_TEMPERATURE(73, "BMS: ERROR OPERATING TEMPERATURE"),
    FACOTRY_SETTINGS_NOT_FOUND(74, "FACOTRY SETTINGS NOT FOUND"),
    BACKUP_POWER_MODE_ACTIVE(75, "BACKUP POWER MODE - ACTIVE"),
    BACKUP_POWER_MODE_BATTERY_EMPTY(76, "BACKUP POWER MODE - BATTERY EMPTY"),
    BACKUP_POWER_MODE_ERROR(77, "BACKUP POWER MODE ERROR"),
    INITIALISING(78, "INITIALISING"),
    INSTALLATION_MODE(79, "INSTALLATION MODE"),
    GRID_OFFLINE(80, "GRID OFFLINE"),
    BMS_UPDATE_NEEDED(81, "BMS UPDATE NEEDED"),
    BMS_CONFIGURATION_NEEDED(82, "BMS CONFIGURATION NEEDED"),
    INSULATION_TEST(83, "INSULATION TEST"),
    SELFTEST(84, "SELFTEST"),
    EXTERNAL_CONTROL(85, "EXTERNAL CONTROL"),
    ERROR_TEMPERATURESENSOR(86, "ERROR: TEMPERATURESENSOR"),
    GRID_OPERATOR_CHARGE_PROHIBITED(87, "GRID OPERATOR: CHARGE PROHIBITED"),
    GRID_OPERATOR_DISCHARGE_PROHIBITED(88, "GRID OPERATOR: DISCHARGE PROHIBITED"),
    SPARE_CAPACITY(89, "SPARE CAPACITY"),
    SELFTEST_ERROR(90, "SELFTEST ERROR"),
    EARTH_FAULT(91, "EARTH FAULT"),
    UNKNOWN(-1, "UNKNOWN");

    private int code;
    private String description;

    SenecBatteryStatus(int index, String description) {
        this.code = index;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SenecBatteryStatus fromCode(int code) {
        for (SenecBatteryStatus state : SenecBatteryStatus.values()) {
            if (state.code == code) {
                return state;
            }
        }
        return SenecBatteryStatus.UNKNOWN;
    }

    public static String descriptionFromCode(int code) {
        for (SenecBatteryStatus state : SenecBatteryStatus.values()) {
            if (state.code == code) {
                return state.description;
            }
        }
        return SenecBatteryStatus.UNKNOWN.description;
    }
}
