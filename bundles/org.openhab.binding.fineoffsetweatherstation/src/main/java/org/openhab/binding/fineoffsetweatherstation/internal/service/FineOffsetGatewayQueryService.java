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
package org.openhab.binding.fineoffsetweatherstation.internal.service;

import static org.openhab.binding.fineoffsetweatherstation.internal.Utils.toUInt16;
import static org.openhab.binding.fineoffsetweatherstation.internal.Utils.toUInt32;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.fineoffsetweatherstation.internal.FineOffsetGatewayConfiguration;
import org.openhab.binding.fineoffsetweatherstation.internal.Utils;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.Command;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.Measurand;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.MeasureType;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.Sensor;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.response.BatteryStatus;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.response.MeasuredValue;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.response.SensorDevice;
import org.openhab.binding.fineoffsetweatherstation.internal.domain.response.SystemInfo;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to query the gateway device.
 *
 * @author Andreas Berger - Initial contribution
 */
@NonNullByDefault
public class FineOffsetGatewayQueryService implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(FineOffsetGatewayQueryService.class);

    private final Socket socket;

    public FineOffsetGatewayQueryService(FineOffsetGatewayConfiguration config) throws IOException {
        this.socket = new Socket(config.ip, config.port);
        this.socket.setSoTimeout(5000);
    }

    public @Nullable String getFirmwareVersion() {
        var data = executeCommand(Command.CMD_READ_FIRMWARE_VERSION);
        if (null != data && data.length > 0) {
            return new String(data, 5, data[4]);
        }
        return null;
    }

    public Map<Sensor, SensorDevice> getRegisteredSensors() {
        var data = executeCommand(Command.CMD_READ_SENSOR_ID_NEW);
        if (null == data) {
            return Map.of();
        }

        /*
         * Pos | Length | Description
         * -------------------------------------------------
         * 0 | 2 | fixed header (0xffff)
         * 2 | 1 | command (0x3c)
         * 3 | 2 | size
         * -------------------------------------------------
         * (n * 7) + 5 | 1 | index of sensor n
         * (n * 7) + 6 | 4 | id of sensor n
         * (n * 7) + 10 | 1 | battery status of sensor n
         * (n * 7) + 11 | 1 | signal of sensor n
         * -------------------------------------------------
         * (n * 7) + 12 | 1 | checksum
         */

        Map<Sensor, SensorDevice> result = new HashMap<>();
        var len = toUInt16(data, 3);
        int entry = 0;
        int entrySize = 7;
        while (entry * entrySize + 11 <= len) {
            int idx = entry++ * entrySize + 5;
            int id = toUInt32(data, idx + 1);
            List<Sensor> sensorCandidates = Sensor.forIndex(data[idx]);
            if (sensorCandidates == null || sensorCandidates.isEmpty()) {
                logger.debug("unknown sensor (id={}) for index {}", id, data[idx]);
                continue;
            }
            Sensor sensor = null;
            if (sensorCandidates.size() == 1) {
                sensor = sensorCandidates.get(0);
            } else if (sensorCandidates.size() == 2 && data[idx] == 0) {
                @Nullable
                SystemInfo systemInfo = fetchSystemInfo();
                if (systemInfo != null) {
                    sensor = systemInfo.isUseWh24() ? Sensor.WH24 : Sensor.WH65;
                }
            }
            if (sensor == null) {
                logger.debug("to many sensor candidates for (id={}) and index {}: {}", id, data[idx], sensorCandidates);
                continue;
            }
            switch (id) {
                case 0xFFFFFFFE:
                    logger.trace("sensor {} = disabled", sensor);
                    continue;
                case 0xFFFFFFFF:
                    logger.trace("sensor {} = registering", sensor);
                    continue;
            }

            BatteryStatus batteryStatus = sensor.getBatteryStatus(data[idx + 5]);
            int signal = Utils.toUInt8(data[idx + 6]);

            result.put(sensor, new SensorDevice(id, sensor, batteryStatus, signal));
        }
        return result;
    }

    public @Nullable SystemInfo fetchSystemInfo() {

        var data = executeCommand(Command.CMD_READ_SSSS);

        // expected response
        // 0 - 0xff - header
        // 1 - 0xff - header
        // 2 - 0x30 - system info
        // 3 - 0x?? - size of response
        // 4 - frequency - 0=433, 1=868MHz, 2=915MHz, 3=920MHz
        // 5 - sensor type - 0=WH24, 1=WH65
        // 6-9 - UTC time
        // 10 - time zone index (?)
        // 11 - DST 0-1 - false/true
        // 12 - 0x?? - checksum

        if (data == null) {
            logger.info("Unexpected response to System Info!");
            return null;
        }

        Integer frequency = null;
        switch (data[4]) {
            case 0:
                frequency = 433;
                break;
            case 1:
                frequency = 868;
                break;
            case 2:
                frequency = 915;
                break;
            case 3:
                frequency = 920;
                break;

        }
        boolean useWh24 = data[5] == 0;
        var unix = toUInt32(data, 6);
        var date = LocalDateTime.ofEpochSecond(unix, 0, ZoneOffset.UTC);
        var dst = data[11] != 0;
        return new SystemInfo(frequency, date, dst, useWh24);
    }

    public List<MeasuredValue> getLiveData() {
        byte[] data = executeCommand(Command.CMD_GW1000_LIVEDATA);
        if (data == null) {
            return Collections.emptyList();
        }
        /*
         * Pos| Length | Description
         * -------------------------------------------------
         * 0 | 2 | fixed header (0xffff)
         * 2 | 1 | command (0x27)
         * 3 | 2 | size
         * -------------------------------------------------
         * 5 | 1 | code of item (item defines n)
         * 6 | n | value of item
         * -------------------------------------------------
         * 6 + n | 1 | code of item (item defines m)
         * 7 + n | m | value of item
         * -------------------------------------------------
         * ...
         * -------------------------------------------------
         *
         * | 1 | checksum
         */
        var idx = 5;
        var size = toUInt16(data, 3);
        List<MeasuredValue> result = new ArrayList<>();
        while (idx < size) {
            byte code = data[idx++];
            Measurand measurand = Measurand.getByCode(code);
            if (measurand == null) {
                logger.warn("failed to get measurand 0x{}", Integer.toHexString(code));
                return result;
            }
            MeasureType measureType = measurand.getMeasureType();
            State state = measureType.toState(data, idx);
            if (state != null) {
                result.add(new MeasuredValue(measurand, state));
            }
            idx += measureType.getByteSize();
        }
        return result;
    }

    private synchronized byte @Nullable [] executeCommand(Command command) {
        byte[] buffer = new byte[2028];
        int bytesRead;
        byte[] request = command.getPayload();

        try {
            InputStream in = socket.getInputStream();
            socket.getOutputStream().write(request);
            if ((bytesRead = in.read(buffer)) == -1) {
                return null;
            }
            if (!command.isValidateResponse(buffer)) {
                if (bytesRead > 0) {
                    logger.debug("executeCommand({}), invalid response: {}", command,
                            Utils.toHexString(buffer, bytesRead, ""));
                } else {
                    logger.debug("executeCommand({}): no response", command);
                }
                return null;
            }

        } catch (Exception ex) {
            logger.warn("executeCommand({})", command, ex);
            return null;
        }

        var data = Arrays.copyOfRange(buffer, 0, bytesRead);
        logger.trace("executeCommand({}): received: {}", command, Utils.toHexString(data, data.length, ""));
        return data;
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }
}
