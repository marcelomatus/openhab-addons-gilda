/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.lgthinq.lgapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The {@link ACSnapShot}
 *
 * @author Nemer Daud - Initial contribution
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ACSnapShot {

    private Integer airWindStrength;

    private Double targetTemperature;

    private Double currentTemperature;

    private Integer operationMode;

    private Integer operation;
    @JsonIgnore
    private boolean online;

    @JsonIgnore
    public DevicePowerState getAcPowerStatus() {
        return operation == null ? DevicePowerState.DV_POWER_UNK : DevicePowerState.statusOf(operation);
    }

    @JsonIgnore
    public ACFanSpeed getAcFanSpeed() {

        return airWindStrength == null ? ACFanSpeed.F_UNK : ACFanSpeed.statusOf(airWindStrength);
    }

    public Integer getAirWindStrength() {
        return airWindStrength;
    }

    public void setAirWindStrength(Integer airWindStrength) {
        this.airWindStrength = airWindStrength;
    }

    public Double getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(Double targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public Double getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(Double currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    public Integer getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(Integer operationMode) {
        this.operationMode = operationMode;
    }

    public Integer getOperation() {
        return operation;
    }

    public void setOperation(Integer operation) {
        this.operation = operation;
    }

    @JsonIgnore
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return "ACSnapShot{" + "airWindStrength=" + airWindStrength + ", targetTemperature=" + targetTemperature
                + ", currentTemperature=" + currentTemperature + ", operationMode=" + operationMode + ", operation="
                + operation + ", acPowerStatus=" + getAcPowerStatus() + ", acFanSpeed=" + getAcFanSpeed()
                + ", acOpMode=" + ", online=" + isOnline() + " }";
    }
}
