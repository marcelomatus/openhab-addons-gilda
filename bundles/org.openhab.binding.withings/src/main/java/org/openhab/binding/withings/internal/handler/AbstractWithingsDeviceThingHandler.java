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
package org.openhab.binding.withings.internal.handler;

import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.withings.internal.WithingsBindingConstants;
import org.openhab.binding.withings.internal.api.WithingsDataModel;
import org.openhab.binding.withings.internal.api.device.DevicesResponse;

/**
 * @author Sven Strohschein - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractWithingsDeviceThingHandler extends AbstractWithingsThingHandler {

    public AbstractWithingsDeviceThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public final boolean updateThingData(WithingsDataModel model) {
        final String thingId = getThing().getUID().getId();

        Optional<DevicesResponse.Device> deviceOptional = model.getDevice(thingId);
        if (deviceOptional.isPresent()) {
            DevicesResponse.Device device = deviceOptional.get();
            updateProperties(device);
            updateChannels(device);
            return true;
        }
        return false;
    }

    private void updateProperties(DevicesResponse.Device device) {
        Map<String, String> properties = editProperties();
        @Nullable
        String deviceId = device.getDeviceId();
        if (deviceId != null) {
            properties.put(WithingsBindingConstants.PROPERTY_DEVICE_ID, deviceId);
        }
        @Nullable
        Integer modelId = device.getModelId();
        if (modelId != null) {
            properties.put(Thing.PROPERTY_MODEL_ID, String.valueOf(modelId));
        }
        @Nullable
        String model = device.getModel();
        if (model != null) {
            properties.put(WithingsBindingConstants.PROPERTY_DEVICE_MODEL, model);
        }
        updateProperties(properties);
    }

    protected abstract void updateChannels(DevicesResponse.Device device);
}
