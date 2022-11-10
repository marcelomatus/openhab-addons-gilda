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
package org.openhab.binding.icloud.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.openhab.binding.icloud.internal.json.response.ICloudDeviceInformation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * This class gives access to the find my iPhone (FMIP) service.
 *
 * @author Simon Spielmann Initial Contribution.
 */
public class FindMyIPhoneServiceManager {

    private ICloudSession session;

    private URI fmipRefreshUrl;

    private URI fmipSoundUrl;

    private final static String FMIP_ENDPOINT = "/fmipservice/client/web";

    private final Gson gson = new GsonBuilder().create();

    /**
     * The constructor.
     *
     * @param session {@link ICloudSession} to use for API calls.
     * @param serviceRoot Root URL for FMIP service.
     */
    public FindMyIPhoneServiceManager(ICloudSession session, String serviceRoot) {

        this.session = session;
        this.fmipRefreshUrl = URI.create(serviceRoot + FMIP_ENDPOINT + "/refreshClient");
        this.fmipSoundUrl = URI.create(serviceRoot + FMIP_ENDPOINT + "/playSound");
    }

    /**
     * Receive client informations as JSON.
     *
     * @return Information about all clients as JSON {@link ICloudDeviceInformation}.
     *
     * @throws IOException if I/O error occurred
     * @throws InterruptedException if this blocking request was interrupted
     *
     */
    public String refreshClient() throws IOException, InterruptedException {

        Map<String, Object> request = Map.of("clientContext",
                Map.of("fmly", true, "shouldLocate", true, "selectedDevice", "All", "deviceListVersion", 1));

        return this.session.post(this.fmipRefreshUrl.toString(), this.gson.toJson(request), null);
    }

    /**
     * Play sound (find my iPhone) on given device.
     *
     * @param deviceId ID of the device to play sound on
     * @throws IOException if I/O error occurred
     * @throws InterruptedException if this blocking request was interrupted
     */
    public void playSound(String deviceId) throws IOException, InterruptedException {

        Map<String, Object> request = Map.of("device", deviceId, "fmyl", true);
        this.session.post(this.fmipSoundUrl.toString(), this.gson.toJson(request), null);
    }
}
