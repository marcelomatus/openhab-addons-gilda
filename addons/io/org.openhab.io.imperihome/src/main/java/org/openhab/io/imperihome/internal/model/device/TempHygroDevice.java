/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.imperihome.internal.model.device;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.imperihome.internal.model.param.NumericValueParam;
import org.openhab.io.imperihome.internal.model.param.ParamType;
import org.openhab.io.imperihome.internal.processor.ItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combined temperature/hygro sensor device. Can be specified on either a temp or hygro Item, with a link to the other one.
 * The linked value will be retrieved in {@link #updateParams()}.
 * @author Pepijn de Geus - Initial contribution
 */
public class TempHygroDevice extends AbstractNumericValueDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempHygroDevice.class);

    private static final String LINK_HYGRO = "hygro";
    private static final String LINK_TEMP = "temp";

    public TempHygroDevice(Item item) {
        super(DeviceType.TEMP_HYGRO, item, null);
    }

    @Override
    public void addLink(String linkType, String deviceId) {
        super.addLink(linkType, deviceId);

        if (getLinks().containsKey(LINK_HYGRO)) {
            setUnit("°C");
        } else if (getLinks().containsKey(LINK_TEMP)) {
            setUnit("%");
        }
    }

    @Override
    public void stateUpdated(Item item, State newState) {
        super.stateUpdated(item, newState);

        DecimalType value = (DecimalType) item.getStateAs(DecimalType.class);

        if (getLinks().containsKey(LINK_HYGRO)) {
            addParam(new NumericValueParam(ParamType.TEMPERATURE_DUAL, getUnit(), value));
        } else if (getLinks().containsKey(LINK_TEMP)) {
            addParam(new NumericValueParam(ParamType.HYGROMETRY_DUAL, getUnit(), value));
        }
    }

    @Override
    public void updateParams() {
        super.updateParams();

        boolean foundLink = false;

        if (getLinks().containsKey(LINK_HYGRO)) {
            String deviceName = getLinks().get(LINK_HYGRO);
            String deviceId = ItemProcessor.getDeviceId(deviceName);
            AbstractDevice hygroDevice = getDeviceRegistry().getDevice(deviceId);
            if (hygroDevice == null) {
                LOGGER.error("Couldn't resolve linked hygro device '{}', make sure the Item has iss tags", deviceName);
            } else {
                setHygroParam(hygroDevice);
                foundLink = true;
            }
        }

        if (getLinks().containsKey(LINK_TEMP)) {
            String deviceName = getLinks().get(LINK_TEMP);
            String deviceId = ItemProcessor.getDeviceId(deviceName);
            AbstractDevice tempDevice = getDeviceRegistry().getDevice(deviceId);
            if (tempDevice == null) {
                LOGGER.error("Couldn't resolve linked temp device '{}', make sure the Item has iss tags", deviceName);
            } else {
                setTempParam(tempDevice);
                foundLink = true;
            }
        }

        if (!foundLink) {
            LOGGER.warn("DevTempHygro device contains no valid 'hygro' or 'temp' link. Add a link to another item using 'iss:link:<type>:<item>'");
        }
    }

    private void setHygroParam(AbstractDevice device) {
        NumericValueParam valueParam = (NumericValueParam) device.getParams().get(ParamType.HYGROMETRY_VALUE);
        if (valueParam == null) {
            LOGGER.warn("Linked Hygro device has no Value parameter: {}", device);
            return;
        }

        NumericValueParam hygroParam = new NumericValueParam(ParamType.HYGROMETRY_DUAL, valueParam.getUnit(), null);
        hygroParam.setValue(valueParam.getValue());
        addParam(hygroParam);
    }

    private void setTempParam(AbstractDevice device) {
        NumericValueParam valueParam = (NumericValueParam) device.getParams().get(ParamType.TEMPERATURE_VALUE);
        if (valueParam == null) {
            LOGGER.warn("Linked Temperature device has no Value parameter: {}", device);
            return;
        }

        NumericValueParam tempParam = new NumericValueParam(ParamType.TEMPERATURE_DUAL, valueParam.getUnit(), null);
        tempParam.setValue(valueParam.getValue());
        addParam(tempParam);
    }

}
