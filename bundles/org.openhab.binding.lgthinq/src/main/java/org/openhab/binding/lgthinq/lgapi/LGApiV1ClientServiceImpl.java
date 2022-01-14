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
package org.openhab.binding.lgthinq.lgapi;

import static org.openhab.binding.lgthinq.internal.LGThinqBindingConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lgthinq.api.RestResult;
import org.openhab.binding.lgthinq.api.RestUtils;
import org.openhab.binding.lgthinq.api.TokenManager;
import org.openhab.binding.lgthinq.api.TokenResult;
import org.openhab.binding.lgthinq.errors.LGApiException;
import org.openhab.binding.lgthinq.errors.LGDeviceV1OfflineException;
import org.openhab.binding.lgthinq.errors.RefreshTokenException;
import org.openhab.binding.lgthinq.lgapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link LGApiV1ClientServiceImpl}
 *
 * @author Nemer Daud - Initial contribution
 */
public class LGApiV1ClientServiceImpl extends LGApiClientServiceImpl {
    private static final LGApiClientService instance;
    private static final Logger logger = LoggerFactory.getLogger(LGApiV1ClientServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenManager tokenManager;

    static {
        instance = new LGApiV1ClientServiceImpl();
    }

    private LGApiV1ClientServiceImpl() {
        tokenManager = TokenManager.getInstance();
    }

    public static LGApiClientService getInstance() {
        return instance;
    }

    @Override
    protected TokenManager getTokenManager() {
        return tokenManager;
    }

    /**
     * Get snapshot data from the device.
     * <b>It works only for API V2 device versions!</b>
     * 
     * @param deviceId device ID for de desired V2 LG Thinq.
     * @return return map containing metamodel of settings and snapshot
     * @throws LGApiException if some communication error occur.
     */
    @Override
    public ACSnapShot getAcDeviceData(String bridgeName, String deviceId) throws LGApiException {
        throw new UnsupportedOperationException("Method not supported in V1 API device.");
    }

    public RestResult sendControlCommands(String bridgeName, String deviceId, String keyName, int value)
            throws Exception {
        TokenResult token = tokenManager.getValidRegisteredToken(bridgeName);
        UriBuilder builder = UriBuilder.fromUri(token.getGatewayInfo().getApiRootV1()).path(V1_CONTROL_OP);
        Map<String, String> headers = getCommonHeaders(token.getGatewayInfo().getLanguage(),
                token.getGatewayInfo().getCountry(), token.getAccessToken(), token.getUserInfo().getUserNumber());

        String payload = String.format(
                "{\n" + "   \"lgedmRoot\":{\n" + "      \"cmd\": \"Control\"," + "      \"cmdOpt\": \"Set\","
                        + "      \"value\": {\"%s\": \"%d\"}," + "      \"deviceId\": \"%s\","
                        + "      \"workId\": \"%s\"," + "      \"data\": \"\"" + "   }\n" + "}",
                keyName, value, deviceId, UUID.randomUUID().toString());
        return RestUtils.postCall(builder.build().toURL().toString(), headers, payload);
    }

    @Override
    public boolean turnDevicePower(String bridgeName, String deviceId, DevicePowerState newPowerState)
            throws LGApiException {
        try {
            RestResult resp = sendControlCommands(bridgeName, deviceId, "Operation", newPowerState.commandValue());

            handleV1GenericErrorResult(resp);
        } catch (Exception e) {
            throw new LGApiException("Error adjusting device power", e);
        }
        return true;
    }

    @Override
    public boolean changeOperationMode(String bridgeName, String deviceId, int newOpMode) throws LGApiException {
        try {
            RestResult resp = sendControlCommands(bridgeName, deviceId, "OpMode", newOpMode);

            handleV1GenericErrorResult(resp);
        } catch (Exception e) {
            throw new LGApiException("Error adjusting operation mode", e);
        }
        return true;
    }

    @Override
    public boolean changeFanSpeed(String bridgeName, String deviceId, int newFanSpeed) throws LGApiException {
        try {
            RestResult resp = sendControlCommands(bridgeName, deviceId, "WindStrength", newFanSpeed);

            handleV1GenericErrorResult(resp);
        } catch (Exception e) {
            throw new LGApiException("Error adjusting fan speed", e);
        }
        return true;
    }

    @Override
    public boolean changeTargetTemperature(String bridgeName, String deviceId, ACTargetTmp newTargetTemp)
            throws LGApiException {
        try {
            RestResult resp = sendControlCommands(bridgeName, deviceId, "TempCfg", newTargetTemp.commandValue());

            handleV1GenericErrorResult(resp);
        } catch (Exception e) {
            throw new LGApiException("Error adjusting target temperature", e);
        }
        return true;
    }

    /**
     * Start monitor data form specific device. This is old one, <b>works only on V1 API supported devices</b>.
     * 
     * @param deviceId Device ID
     * @return Work1 to be uses to grab data during monitoring.
     * @throws LGApiException If some communication error occur.
     */
    @Override
    public String startMonitor(String bridgeName, String deviceId)
            throws LGApiException, LGDeviceV1OfflineException, IOException {
        TokenResult token = tokenManager.getValidRegisteredToken(bridgeName);
        UriBuilder builder = UriBuilder.fromUri(token.getGatewayInfo().getApiRootV1()).path(V1_START_MON_PATH);
        Map<String, String> headers = getCommonHeaders(token.getGatewayInfo().getLanguage(),
                token.getGatewayInfo().getCountry(), token.getAccessToken(), token.getUserInfo().getUserNumber());
        String workerId = UUID.randomUUID().toString();
        String jsonData = String.format(" { \"lgedmRoot\" : {" + "\"cmd\": \"Mon\"," + "\"cmdOpt\": \"Start\","
                + "\"deviceId\": \"%s\"," + "\"workId\": \"%s\"" + "} }", deviceId, workerId);
        RestResult resp = RestUtils.postCall(builder.build().toURL().toString(), headers, jsonData);
        return (String) handleV1GenericErrorResult(resp).get("workId");
    }

    @NonNull
    private Map<String, Object> handleV1GenericErrorResult(@Nullable RestResult resp)
            throws LGApiException, LGDeviceV1OfflineException {
        Map<String, Object> metaResult;
        Map<String, Object> envelope = Collections.EMPTY_MAP;
        if (resp == null) {
            return envelope;
        }
        if (resp.getStatusCode() != 200) {
            logger.error("Error returned by LG Server API. The reason is:{}", resp.getJsonResponse());
            throw new LGApiException(
                    String.format("Error returned by LG Server API. The reason is:%s", resp.getJsonResponse()));
        } else {
            try {
                metaResult = objectMapper.readValue(resp.getJsonResponse(), new TypeReference<Map<String, Object>>() {
                });
                envelope = (Map<String, Object>) metaResult.get("lgedmRoot");
                if (envelope == null) {
                    throw new LGApiException(String.format(
                            "Unexpected json body returned (without root node lgedmRoot): %s", resp.getJsonResponse()));
                } else if (!"0000".equals(envelope.get("returnCd"))) {
                    if ("0106".equals(envelope.get("returnCd")) || "D".equals(envelope.get("deviceState"))) {
                        // Disconnected Device
                        throw new LGDeviceV1OfflineException("Device is offline. No data available");
                    }
                    throw new LGApiException(
                            String.format("Status error executing endpoint. resultCode must be 0000, but was:%s",
                                    metaResult.get("returnCd")));
                }
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unknown error occurred deserializing json stream", e);
            }
        }
        return envelope;
    }

    private Map<String, Object> handleV2GenericErrorResult(@Nullable RestResult resp) throws LGApiException {
        Map<String, Object> metaResult;
        if (resp == null) {
            return null;
        }
        if (resp.getStatusCode() != 200) {
            logger.error("Error returned by LG Server API. The reason is:{}", resp.getJsonResponse());
            throw new LGApiException(
                    String.format("Error returned by LG Server API. The reason is:%s", resp.getJsonResponse()));
        } else {
            try {
                metaResult = objectMapper.readValue(resp.getJsonResponse(), new TypeReference<Map<String, Object>>() {
                });
                if (!"0000".equals(metaResult.get("resultCode"))) {
                    throw new LGApiException(
                            String.format("Status error executing endpoint. resultCode must be 0000, but was:%s",
                                    metaResult.get("resultCode")));
                }
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unknown error occurred deserializing json stream", e);
            }

        }
        return (Map<String, Object>) metaResult.get("result");
    }

    @Override
    public void stopMonitor(String bridgeName, String deviceId, String workId)
            throws LGApiException, RefreshTokenException, IOException, LGDeviceV1OfflineException {
        TokenResult token = tokenManager.getValidRegisteredToken(bridgeName);
        UriBuilder builder = UriBuilder.fromUri(token.getGatewayInfo().getApiRootV1()).path(V1_START_MON_PATH);
        Map<String, String> headers = getCommonHeaders(token.getGatewayInfo().getLanguage(),
                token.getGatewayInfo().getCountry(), token.getAccessToken(), token.getUserInfo().getUserNumber());
        String jsonData = String.format(" { \"lgedmRoot\" : {" + "\"cmd\": \"Mon\"," + "\"cmdOpt\": \"Stop\","
                + "\"deviceId\": \"%s\"," + "\"workId\": \"%s\"" + "} }", deviceId, workId);
        RestResult resp = RestUtils.postCall(builder.build().toURL().toString(), headers, jsonData);
        handleV1GenericErrorResult(resp);
    }

    @Override
    public ACSnapShot getMonitorData(String bridgeName, String deviceId, String workId)
            throws LGApiException, IOException {
        TokenResult token = tokenManager.getValidRegisteredToken(bridgeName);
        UriBuilder builder = UriBuilder.fromUri(token.getGatewayInfo().getApiRootV1()).path(V1_POOL_MON_PATH);
        Map<String, String> headers = getCommonHeaders(token.getGatewayInfo().getLanguage(),
                token.getGatewayInfo().getCountry(), token.getAccessToken(), token.getUserInfo().getUserNumber());
        String jsonData = String.format("{\n" + "   \"lgedmRoot\":{\n" + "      \"workList\":[\n" + "         {\n"
                + "            \"deviceId\":\"%s\",\n" + "            \"workId\":\"%s\"\n" + "         }\n"
                + "      ]\n" + "   }\n" + "}", deviceId, workId);
        RestResult resp = RestUtils.postCall(builder.build().toURL().toString(), headers, jsonData);
        Map<String, Object> envelop = null;
        // to unify the same behaviour then V2, this method handle Offline Exception and return a dummy shot with
        // offline flag.
        try {
            envelop = handleV1GenericErrorResult(resp);
        } catch (LGDeviceV1OfflineException e) {
            ACSnapShot shot = new ACSnapShotV2();
            shot.setOnline(false);
            return shot;
        }
        if (envelop.get("workList") != null
                && ((Map<String, Object>) envelop.get("workList")).get("returnData") != null) {
            Map<String, Object> workList = ((Map<String, Object>) envelop.get("workList"));
            String jsonMonDataB64 = (String) workList.get("returnData");
            String jsonMon = new String(Base64.getDecoder().decode(jsonMonDataB64));
            ACSnapShot shot = objectMapper.readValue(jsonMon, ACSnapShotV1.class);
            shot.setOnline("E".equals(workList.get("deviceState")));
            return shot;
        } else {
            // no data available yet
            return null;
        }
    }

    private File getCapFileForDevice(String deviceId) {
        return new File(String.format(BASE_CAP_CONFIG_DATA_FILE, deviceId));
    }

    /**
     * Get capability em registry/cache on file for next consult
     * 
     * @param deviceId ID of the device
     * @param uri URI of the config capanility
     * @return return simplified capability
     * @throws LGApiException If some error occurr
     */
    @Override
    public ACCapability getDeviceCapability(String deviceId, String uri, boolean forceRecreate) throws LGApiException {
        try {
            File regFile = getCapFileForDevice(deviceId);
            ACCapability acCap = new ACCapability();
            Map<String, Object> mapper;
            if (regFile.isFile() && !forceRecreate) {
                // reg exists. Retrieve from it
                mapper = objectMapper.readValue(regFile, new TypeReference<Map<String, Object>>() {
                });
            } else {
                RestResult res = RestUtils.getCall(uri, null, null);
                mapper = objectMapper.readValue(res.getJsonResponse(), new TypeReference<Map<String, Object>>() {
                });
                // try save file
                objectMapper.writeValue(getCapFileForDevice(deviceId), mapper);
            }
            Map<String, Object> cap = (Map<String, Object>) mapper.get("Value");
            if (cap == null) {
                throw new LGApiException("Error extracting capabilities supported by the device");
            }

            Map<String, Object> opModes = (Map<String, Object>) cap.get("OpMode");
            if (opModes == null) {
                throw new LGApiException("Error extracting opModes supported by the device");
            } else {
                Map<String, String> modes = new HashMap<String, String>();
                ((Map<String, String>) opModes.get("option")).forEach((k, v) -> {
                    modes.put(v, k);
                });
                acCap.setOpMod(modes);
            }
            Map<String, Object> fanSpeed = (Map<String, Object>) cap.get("WindStrength");
            if (fanSpeed == null) {
                throw new LGApiException("Error extracting fanSpeed supported by the device");
            } else {
                Map<String, String> fanModes = new HashMap<String, String>();
                ((Map<String, String>) fanSpeed.get("option")).forEach((k, v) -> {
                    fanModes.put(v, k);
                });
                acCap.setFanSpeed(fanModes);

            }
            // Set supported modes for the device

            Map<String, Map<String, String>> supOpModes = (Map<String, Map<String, String>>) cap.get("SupportOpMode");
            acCap.setSupportedOpMode(new ArrayList<>(supOpModes.get("option").values()));
            acCap.getSupportedOpMode().remove("@NON");
            Map<String, Map<String, String>> supFanSpeeds = (Map<String, Map<String, String>>) cap
                    .get("SupportWindStrength");
            acCap.setSupportedFanSpeed(new ArrayList<>(supFanSpeeds.get("option").values()));
            acCap.getSupportedFanSpeed().remove("@NON");

            return acCap;
        } catch (IOException e) {
            throw new LGApiException("Error reading IO interface", e);
        }
    }
}
