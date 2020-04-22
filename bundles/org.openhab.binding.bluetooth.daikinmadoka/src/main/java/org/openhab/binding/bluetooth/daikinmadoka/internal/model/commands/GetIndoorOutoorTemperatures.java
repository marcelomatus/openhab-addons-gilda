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
package org.openhab.binding.bluetooth.daikinmadoka.internal.model.commands;

import java.util.concurrent.Executor;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.bluetooth.daikinmadoka.internal.model.MadokaMessage;
import org.openhab.binding.bluetooth.daikinmadoka.internal.model.MadokaParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Benjamin Lafois
 *
 */
@NonNullByDefault
public class GetIndoorOutoorTemperatures extends BRC1HCommand {

    private final Logger logger = LoggerFactory.getLogger(GetIndoorOutoorTemperatures.class);

    private @Nullable DecimalType indoorTemperature;
    private @Nullable DecimalType outdoorTemperature;

    @Override
    public byte[] getRequest() {
        return MadokaMessage.createRequest(this);
    }

    @Override
    public boolean handleResponse(Executor executor, ResponseListener listener, MadokaMessage mm)
            throws MadokaParsingException {
        try {
            Integer iIndoorTemperature = Integer.valueOf(mm.getValues().get(0x40).getRawValue()[0]);
            Integer iOutdoorTemperature = Integer.valueOf(mm.getValues().get(0x41).getRawValue()[0]);

            if (iOutdoorTemperature == -1) {
                iOutdoorTemperature = null;
            } else {
                if (iOutdoorTemperature < 0) {
                    iOutdoorTemperature = ((iOutdoorTemperature + 256) - 128) * -1;
                }
            }

            if (iIndoorTemperature != null) {
                indoorTemperature = new DecimalType(iIndoorTemperature);
            }

            if (iOutdoorTemperature != null) {
                outdoorTemperature = new DecimalType(iOutdoorTemperature);
            }

            logger.debug("Indoor Temp: {}", indoorTemperature);
            logger.debug("Outdoor Temp: {}", outdoorTemperature);

            setState(State.SUCCEEDED);
            executor.execute(() -> listener.receivedResponse(this));

            return true;
        } catch (Exception e) {
            setState(State.FAILED);
            throw new MadokaParsingException(e);
        }
    }

    public @Nullable DecimalType getIndoorTemperature() {
        return indoorTemperature;
    }

    public @Nullable DecimalType getOutdoorTemperature() {
        return outdoorTemperature;
    }

    @Override
    public int getCommandId() {
        return 272;
    }

}
