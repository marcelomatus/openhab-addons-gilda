/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fronius.internal.model;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Access object for storage information.
 *
 * @author Gerrit Beine
 */
public class StorageRealtimeData {

    private final Logger logger = LoggerFactory.getLogger(StorageRealtimeData.class);

    // Device info
    private DecimalType capacity = DecimalType.ZERO;
    private DecimalType current = DecimalType.ZERO;
    private DecimalType voltage = DecimalType.ZERO;
    private DecimalType temperature = DecimalType.ZERO;
    private DecimalType charge = DecimalType.ZERO;
    private DecimalType designedCapacity = DecimalType.ZERO;
    private StringType manufacturer = StringType.EMPTY;
    private StringType model = StringType.EMPTY;
    private StringType serial = StringType.EMPTY;
    private DecimalType enable = DecimalType.ZERO;
    private DecimalType maxVoltage = DecimalType.ZERO;
    private DecimalType minVoltage = DecimalType.ZERO;

    // Request info
    private DecimalType code = DecimalType.ZERO;
    private DateTimeType timestamp = new DateTimeType();
    private boolean empty = false;

    public static StorageRealtimeData createStorageRealtimeData(final JsonObject json) {
        final StorageRealtimeData srd = new StorageRealtimeData();
        srd.createFromJson(json);
        return srd;
    }

    private StorageRealtimeData() {
        super();
    }

    public boolean isEmpty() {
        return empty;
    }

    public DecimalType getCapacity() {
        return capacity;
    }

    public DecimalType getCurrent() {
        return current;
    }

    public DecimalType getVoltage() {
        return voltage;
    }

    public DecimalType getTemperature() {
        return temperature;
    }

    public DecimalType getCharge() {
        return charge;
    }

    public DecimalType getCode() {
        return code;
    }

    public DateTimeType getTimestamp() {
        return timestamp;
    }

    public DecimalType getDesignedCapacity() {
        return designedCapacity;
    }

    public StringType getManufacturer() {
        return manufacturer;
    }

    public StringType getModel() {
        return model;
    }

    public StringType getSerial() {
        return serial;
    }

    public DecimalType getEnable() {
        return enable;
    }

    public DecimalType getMaxVoltage() {
        return maxVoltage;
    }

    public DecimalType getMinVoltage() {
        return minVoltage;
    }

    private synchronized void createFromJson(final JsonObject json) {
        try {
            if (json.has("Body")) {
                final JsonObject body = json.get("Body").getAsJsonObject();
                logger.trace("{}", body.toString());
                if (body.has("Data")) {
                    final JsonObject data = body.get("Data").getAsJsonObject();
                    logger.trace("{}", data.toString());
                    if (data.has("Controller")) {
                        final JsonObject controller = data.get("Controller").getAsJsonObject();
                        logger.trace("{}", controller.toString());
                        if (controller.has("Capacity_Maximum")) {
                            capacity = new DecimalType(controller.get("Capacity_Maximum").getAsString());
                            logger.debug("Capacity: {}", capacity);
                        }
                        if (controller.has("Current_DC")) {
                            current = new DecimalType(controller.get("Current_DC").getAsString());
                            logger.debug("Current: {}", current);
                        }
                        if (controller.has("Voltage_DC")) {
                            voltage = new DecimalType(controller.get("Voltage_DC").getAsString());
                            logger.debug("Voltage: {}", voltage);
                        }
                        if (controller.has("Temperature_Cell")) {
                            temperature = new DecimalType(controller.get("Temperature_Cell").getAsString());
                            logger.debug("Temperature: {}", temperature);
                        }
                        if (controller.has("StateOfCharge_Relative")) {
                            charge = new DecimalType(controller.get("StateOfCharge_Relative").getAsString());
                            logger.debug("Charge: {}", charge);
                        }
                        if (controller.has("DesignedCapacity")) {
                            designedCapacity = new DecimalType(controller.get("DesignedCapacity").getAsString());
                            logger.debug("Designed Capacity: {}", designedCapacity);
                        }
                        if (controller.has("Enable")) {
                            enable = new DecimalType(controller.get("Enable").getAsString());
                            logger.debug("Enable: {}", enable);
                        }
                        if (controller.has("Voltage_DC_Maximum_Cell")) {
                            maxVoltage = new DecimalType(controller.get("Voltage_DC_Maximum_Cell").getAsString());
                            logger.debug("Voltage Maximum: {}", maxVoltage);
                        }
                        if (controller.has("Voltage_DC_Minimum_Cell")) {
                            minVoltage = new DecimalType(controller.get("Voltage_DC_Minimum_Cell").getAsString());
                            logger.debug("Voltage Minimum: {}", minVoltage);
                        }
                        if (controller.has("Details")) {
                            final JsonObject details = controller.get("Details").getAsJsonObject();
                            logger.trace("{}", details.toString());
                            if (details.has("Manufacturer")) {
                                manufacturer = new StringType(details.get("Manufacturer").getAsString());
                                logger.debug("Manufacturer: {}", manufacturer);
                            }
                            if (details.has("Model")) {
                                model = new StringType(details.get("Model").getAsString());
                                logger.debug("Model: {}", model);
                            }
                            if (details.has("Serial")) {
                                serial = new StringType(details.get("Serial").getAsString());
                                logger.debug("Serial: {}", serial);
                            }
                        }
                    }
                    empty = false;
                } else {
                    empty = true;
                }

            }
            if (json.has("Head")) {
                final JsonObject head = json.get("Head").getAsJsonObject();
                logger.trace("{}", head.toString());
                if (head.has("Status")) {
                    final JsonObject status = head.get("Status").getAsJsonObject();
                    logger.trace("{}", status.toString());
                    if (status.has("Code")) {
                        code = new DecimalType(status.get("Code").getAsString());
                        logger.debug("Status Code: {}", code);
                    }
                }
                if (head.has("Timestamp")) {
                    timestamp = new DateTimeType(head.get("Timestamp").getAsString());
                    logger.debug("Timestamp: {}", timestamp);
                }
            }
        } catch (Exception e) {
            logger.warn("{}", e.toString());
        }
    }
}
