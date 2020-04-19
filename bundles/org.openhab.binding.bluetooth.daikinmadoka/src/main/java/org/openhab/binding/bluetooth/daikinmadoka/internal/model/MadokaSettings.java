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
package org.openhab.binding.bluetooth.daikinmadoka.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openhab.binding.bluetooth.daikinmadoka.internal.model.MadokaProperties.FAN_SPEED;
import org.openhab.binding.bluetooth.daikinmadoka.internal.model.MadokaProperties.OPERATION_MODE;

/**
 *
 * @author blafois
 *
 */
@NonNullByDefault
public class MadokaSettings {

    private @Nullable OnOffType onOffState;

    private @Nullable DecimalType setpoint;

    private @Nullable DecimalType indoorTemperature;
    private @Nullable DecimalType outdoorTemperature;

    private @Nullable FAN_SPEED fanspeed;

    private @Nullable OPERATION_MODE operationMode;

    private @Nullable String homekitCurrentMode;
    private @Nullable String homekitTargetMode;

    private @Nullable String communicationControllerVersion;
    private @Nullable String remoteControllerVersion;

    public @Nullable OnOffType getOnOffState() {
        return onOffState;
    }

    public void setOnOffState(OnOffType onOffState) {
        this.onOffState = onOffState;
    }

    public @Nullable DecimalType getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(DecimalType setpoint) {
        this.setpoint = setpoint;
    }

    public @Nullable DecimalType getIndoorTemperature() {
        return indoorTemperature;
    }

    public void setIndoorTemperature(DecimalType indoorTemperature) {
        this.indoorTemperature = indoorTemperature;
    }

    public @Nullable DecimalType getOutdoorTemperature() {
        return outdoorTemperature;
    }

    public void setOutdoorTemperature(DecimalType outdoorTemperature) {
        this.outdoorTemperature = outdoorTemperature;
    }

    public @Nullable FAN_SPEED getFanspeed() {
        return fanspeed;
    }

    public void setFanspeed(FAN_SPEED fanspeed) {
        this.fanspeed = fanspeed;
    }

    public @Nullable OPERATION_MODE getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(OPERATION_MODE operationMode) {
        this.operationMode = operationMode;
    }

    public @Nullable String getHomekitCurrentMode() {
        return homekitCurrentMode;
    }

    public void setHomekitCurrentMode(String homekitCurrentMode) {
        this.homekitCurrentMode = homekitCurrentMode;
    }

    public @Nullable String getHomekitTargetMode() {
        return homekitTargetMode;
    }

    public void setHomekitTargetMode(String homekitTargetMode) {
        this.homekitTargetMode = homekitTargetMode;
    }

    public @Nullable String getCommunicationControllerVersion() {
        return communicationControllerVersion;
    }

    public void setCommunicationControllerVersion(String communicationControllerVersion) {
        this.communicationControllerVersion = communicationControllerVersion;
    }

    public @Nullable String getRemoteControllerVersion() {
        return remoteControllerVersion;
    }

    public void setRemoteControllerVersion(String remoteControllerVersion) {
        this.remoteControllerVersion = remoteControllerVersion;
    }

}
