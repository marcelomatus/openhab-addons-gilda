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
package org.openhab.binding.doorbird.internal.api;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.doorbird.internal.model.DoorbirdInfoDTO;
import org.openhab.binding.doorbird.internal.model.DoorbirdInfoDTO.DoorbirdInfoBha;
import org.openhab.binding.doorbird.internal.model.DoorbirdInfoDTO.DoorbirdInfoBha.DoorbirdInfoArray;

import com.google.gson.JsonSyntaxException;

/**
 * The {@link DoorbirdInfo} holds information about the Doorbird.
 *
 * @author Mark Hilbush - Initial contribution
 */
@NonNullByDefault
public class DoorbirdInfo {
    private @Nullable String returnCode;
    private @Nullable String firmwareVersion;
    private @Nullable String buildNumber;
    private @Nullable String primaryMacAddress;
    private @Nullable String wifiMacAddress;
    private @Nullable String deviceType;
    private @Nullable String controllerId;
    private ArrayList<String> relays = new ArrayList<>();

    @SuppressWarnings("null")
    public DoorbirdInfo(String infoJson) throws JsonSyntaxException {
        DoorbirdInfoDTO info = DoorbirdAPI.fromJson(infoJson, DoorbirdInfoDTO.class);
        if (info != null) {
            DoorbirdInfoBha bha = info.bha;
            returnCode = bha.returnCode;
            if (bha.doorbirdInfoArray.length == 1) {
                DoorbirdInfoArray doorbirdInfo = bha.doorbirdInfoArray[0];
                firmwareVersion = doorbirdInfo.firmwareVersion;
                buildNumber = doorbirdInfo.buildNumber;
                primaryMacAddress = doorbirdInfo.primaryMacAddress;
                wifiMacAddress = doorbirdInfo.wifiMacAddress;
                deviceType = doorbirdInfo.deviceType;
                for (String relay : doorbirdInfo.relays) {
                    relays.add(relay);
                    String[] parts = relay.split("@");
                    if (parts.length == 2) {
                        controllerId = parts[0];
                    }
                }
            }
        }
    }

    public @Nullable String getReturnCode() {
        return returnCode;
    }

    public @Nullable String getFirmwareVersion() {
        return firmwareVersion;
    }

    public @Nullable String getBuildNumber() {
        return buildNumber;
    }

    public @Nullable String getPrimaryMacAddress() {
        return primaryMacAddress;
    }

    public @Nullable String getWifiMacAddress() {
        return wifiMacAddress;
    }

    public @Nullable String getDeviceType() {
        return deviceType;
    }

    public @Nullable String getControllerId() {
        return controllerId;
    }

    public ArrayList<String> getRelays() {
        return relays;
    }

    public void addRelay(String relay) {
        relays.add(relay);
    }
}
