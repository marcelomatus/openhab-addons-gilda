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
package org.openhab.binding.netatmo.internal.api;

import static org.openhab.binding.netatmo.internal.api.NetatmoConstants.*;

import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.netatmo.internal.api.NetatmoConstants.MeasureLimit;
import org.openhab.binding.netatmo.internal.api.NetatmoConstants.MeasureScale;
import org.openhab.binding.netatmo.internal.api.NetatmoConstants.MeasureType;
import org.openhab.binding.netatmo.internal.api.dto.NADeviceDataBody;
import org.openhab.binding.netatmo.internal.api.dto.NAMain;
import org.openhab.binding.netatmo.internal.api.dto.NAMeasureBodyElem;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */

@NonNullByDefault
public class WeatherApi extends RestManager {
    public class NAStationDataResponse extends ApiResponse<NADeviceDataBody<NAMain>> {
    }

    private class NAMeasuresResponse extends ApiResponse<List<NAMeasureBodyElem>> {
    }

    public WeatherApi(ApiBridge apiClient) {
        super(apiClient, NetatmoConstants.WEATHER_SCOPES);
    }

    /**
     *
     * The method getstationsdata Returns data from a user&#x27;s Weather Stations (measures and device specific
     * data).
     *
     * @param deviceId Id of the device you want to retrieve information of (optional)
     * @param getFavorites Whether to include the user&#x27;s favorite Weather Stations in addition to the user&#x27;s
     *            own Weather Stations (optional, default to false)
     * @return NAStationDataResponse
     * @throws NetatmoException If fail to call the API, e.g. server error or cannot deserialize the
     *             response body
     */
    public NAStationDataResponse getStationsData(@Nullable String deviceId, boolean getFavorites)
            throws NetatmoException {
        UriBuilder uriBuilder = getApiUriBuilder().path(NA_GETSTATION_SPATH);
        if (deviceId != null) {
            uriBuilder.queryParam(NA_DEVICEID_PARAM, deviceId);
        }
        uriBuilder.queryParam("get_favorites", getFavorites);
        NAStationDataResponse response = get(uriBuilder, NAStationDataResponse.class);
        return response;
    }

    public NAMain getStationData(String deviceId) throws NetatmoException {
        NADeviceDataBody<NAMain> answer = getStationsData(deviceId, true).getBody();
        NAMain station = answer.getDevice(deviceId);
        if (station != null) {
            return station;
        }
        throw new NetatmoException(String.format("Unexpected answer cherching device '%s' : not found.", deviceId));
    }

    public double getMeasurements(String deviceId, @Nullable String moduleId, MeasureScale scale, MeasureType type,
            MeasureLimit limit) throws NetatmoException {
        List<NAMeasureBodyElem> result = getmeasure(deviceId, moduleId, scale,
                (limit.toString() + "_" + type.toString()).toLowerCase(), 0, 0);
        return result.size() > 0 ? result.get(0).getSingleValue() : Double.NaN;
    }

    public double getMeasurements(String deviceId, @Nullable String moduleId, MeasureScale scale, MeasureType type)
            throws NetatmoException {
        List<NAMeasureBodyElem> result = getmeasure(deviceId, moduleId, scale, type.toString().toLowerCase(), 0, 0);
        return result.size() > 0 ? result.get(0).getSingleValue() : Double.NaN;
    }

    private List<NAMeasureBodyElem> getmeasure(String deviceId, @Nullable String moduleId, MeasureScale scale,
            String measureType, long dateBegin, long dateEnd) throws NetatmoException {
        UriBuilder uriBuilder = getApiUriBuilder().path(NA_GETMEASURE_SPATH).queryParam(NA_DEVICEID_PARAM, deviceId)
                .queryParam("scale", scale.getDescriptor()).queryParam("real_time", true)
                .queryParam("date_end", (dateEnd > 0 && dateEnd > dateBegin) ? dateEnd : "last")
                .queryParam("optimize", true) // NAMeasuresResponse is not designed for optimize=false
                .queryParam("type", measureType);

        if (moduleId != null) {
            uriBuilder.queryParam(NA_MODULEID_PARAM, moduleId);
        }
        if (dateBegin > 0) {
            uriBuilder.queryParam("date_begin", dateBegin);
        }
        NAMeasuresResponse response = get(uriBuilder, NAMeasuresResponse.class);
        return response.getBody();
    }
}
