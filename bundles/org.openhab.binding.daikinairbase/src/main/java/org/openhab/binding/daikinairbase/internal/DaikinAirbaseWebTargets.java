/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.daikinairbase.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.daikinairbase.internal.api.ControlInfo;
import org.openhab.binding.daikinairbase.internal.api.SensorInfo;
import org.openhab.binding.daikinairbase.internal.api.BasicInfo;
import org.openhab.binding.daikinairbase.internal.api.ModelInfo;
import org.openhab.binding.daikinairbase.internal.api.ZoneInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles performing the actual HTTP requests for communicating with Daikin air conditioning units.
 *
 * @author Tim Waterhouse - Initial Contribution
 * @author Paul Smedley - Modifications for Daikin Airbase module for Australian Daikin Ducted Systems
 *
 */
public class DaikinAirbaseWebTargets {
    private static final int TIMEOUT_MS = 30000;

    private String setControlInfoUri;
    private String getControlInfoUri;
    private String getSensorInfoUri;
    private String BasicInfoUri;
    private String getModelInfoUri;
    private String getZoneInfoUri;
    private String setZoneInfoUri;
    private Logger logger = LoggerFactory.getLogger(DaikinAirbaseWebTargets.class);

    public DaikinAirbaseWebTargets(String ipAddress) {
        String baseUri = "http://" + ipAddress + "/";
        setControlInfoUri = baseUri + "skyfi/aircon/set_control_info";
        getControlInfoUri = baseUri + "skyfi/aircon/get_control_info";
        getSensorInfoUri = baseUri + "skyfi/aircon/get_sensor_info";
        BasicInfoUri = baseUri + "skyfi/common/basic_info";
        getModelInfoUri = baseUri + "skyfi/aircon/get_model_info";
        getZoneInfoUri = baseUri + "skyfi/aircon/get_zone_setting";
        setZoneInfoUri = baseUri + "skyfi/aircon/set_zone_setting";
    }

    public ControlInfo getControlInfo() throws DaikinAirbaseCommunicationException {
        String response = invoke(getControlInfoUri);
        return ControlInfo.parse(response);
    }

    public void setControlInfo(ControlInfo info) throws DaikinAirbaseCommunicationException {
        Map<String, String> queryParams = info.getParamString();
        invoke(setControlInfoUri, queryParams);
    }

    public SensorInfo getSensorInfo() throws DaikinAirbaseCommunicationException {
        String response = invoke(getSensorInfoUri);
        return SensorInfo.parse(response);
    }

    public BasicInfo BasicInfo() throws DaikinAirbaseCommunicationException {
        String response = invoke(BasicInfoUri);
        return BasicInfo.parse(response);
    }

    public ModelInfo getModelInfo() throws DaikinAirbaseCommunicationException {
        String response = invoke(getModelInfoUri);
        return ModelInfo.parse(response);
    }

    public ZoneInfo getZoneInfo() throws DaikinAirbaseCommunicationException {
        String response = invoke(getZoneInfoUri);
        return ZoneInfo.parse(response);
    }

    public void setZoneInfo(ZoneInfo zoneinfo, ModelInfo modelinfo) throws DaikinAirbaseCommunicationException {
        int count=0;
        count = (zoneinfo.zone1 ? 1 : 0) + (zoneinfo.zone2 ? 1 : 0) + (zoneinfo.zone3 ? 1 : 0) + (zoneinfo.zone4 ? 1 : 0) + (zoneinfo.zone5 ? 1 : 0) + (zoneinfo.zone6 ? 1 : 0) + (zoneinfo.zone7 ? 1 : 0) + (zoneinfo.zone8 ? 1 : 0) + modelinfo.commonzone;
        logger.debug("Number of open zones: \"{}\"", count);

        Map<String, String> queryParams = zoneinfo.getParamString();
        if (count >= 1) invoke(setZoneInfoUri, queryParams);
    }

    private String invoke(String uri) throws DaikinAirbaseCommunicationException {
        return invoke(uri, new HashMap<>());
    }

    private String invoke(String uri, Map<String, String> params) throws DaikinAirbaseCommunicationException {
        String uriWithParams = uri + paramsToQueryString(params);
        logger.debug("Calling url: {}", uriWithParams);
        String response;
        synchronized (this) {
            try {
                response = HttpUtil.executeUrl("GET", uriWithParams, TIMEOUT_MS);
            } catch (IOException ex) {
                // Response will also be set to null if parsing in executeUrl fails so we use null here to make the
                // error check below consistent.
                response = null;
            }
        }

        if (response == null) {
            throw new DaikinAirbaseCommunicationException(
                    String.format("Daikin Airbase controller returned error while invoking %s", uriWithParams));
        }

        return response;
    }

    private String paramsToQueryString(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }

        return "?" + params.entrySet().stream().map(param -> param.getKey() + "=" + param.getValue())
                .collect(Collectors.joining("&"));
    }
}
