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
package org.openhab.binding.comfoair.internal.datatypes;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.comfoair.internal.ComfoAirCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle temperature values
 *
 * @author Holger Hees - Initial Contribution
 * @author Hans Böhm - QuantityTypes
 */
@NonNullByDefault
public class DataTypeTemperature implements ComfoAirDataType {
    private static DataTypeTemperature SINGLETON_INSTANCE = new DataTypeTemperature();

    private DataTypeTemperature() {
    }

    private final Logger logger = LoggerFactory.getLogger(DataTypeTemperature.class);

    public static DataTypeTemperature getInstance() {
        return SINGLETON_INSTANCE;
    }

    @Override
    public State convertToState(int @Nullable [] data, ComfoAirCommandType commandType) {
        if (data == null) {
            logger.trace("\"DataTypeTemperature\" class \"convertToState\" method parameter: null");
            return UnDefType.NULL;
        } else {
            int[] readReplyDataPos = commandType.getReadReplyDataPos();
            if (readReplyDataPos != null && readReplyDataPos[0] < data.length) {
                return new QuantityType<>((((double) data[readReplyDataPos[0]]) / 2) - 20, SIUnits.CELSIUS);
            } else {
                return UnDefType.NULL;
            }
        }
    }

    @Override
    public int @Nullable [] convertFromState(State value, ComfoAirCommandType commandType) {
        int[] template = commandType.getChangeDataTemplate();
        QuantityType<?> celsius = ((QuantityType<?>) value).toUnit(SIUnits.CELSIUS);

        if (celsius != null) {
            template[commandType.getChangeDataPos()] = (int) (celsius.doubleValue() + 20) * 2;
            return template;
        } else {
            logger.trace("\"DataTypeTemperature\" class \"convertFromState\" undefined state");
            return null;
        }
    }
}
