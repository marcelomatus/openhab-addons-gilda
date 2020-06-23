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
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.comfoair.internal.ComfoAirCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle boolean values which are handled as decimal 0/1 states
 *
 * @author Holger Hees - Initial Contribution
 * @author Hans Böhm - Refactoring
 */
@NonNullByDefault
public class DataTypeBoolean implements ComfoAirDataType {
    private static DataTypeBoolean SINGLETON_INSTANCE = new DataTypeBoolean();

    private DataTypeBoolean() {
    }

    private final Logger logger = LoggerFactory.getLogger(DataTypeBoolean.class);

    public static DataTypeBoolean getInstance() {
        return SINGLETON_INSTANCE;
    }

    @Override
    public State convertToState(int @Nullable [] data, ComfoAirCommandType commandType) {
        if (data == null) {
            logger.trace("\"DataTypeBoolean\" class \"convertToState\" method parameter: null");
            return UnDefType.NULL;
        } else {
            int[] get_reply_data_pos = commandType.getGetReplyDataPos();
            int get_reply_data_bits = commandType.getGetReplyDataBits();
            int read_command = commandType.getReadCommand();
            boolean result;

            if (get_reply_data_pos != null && get_reply_data_pos[0] < data.length) {
                if (get_reply_data_bits == 0) {
                    result = data[get_reply_data_pos[0]] == 1;
                } else {
                    result = (data[get_reply_data_pos[0]] & get_reply_data_bits) == get_reply_data_bits;
                }
                return OnOffType.from(result);
            } else if (read_command == 0) {
                return OnOffType.OFF; // handle write-only commands (resets)
            } else {
                return UnDefType.NULL;
            }
        }
    }

    @Override
    public int @Nullable [] convertFromState(State value, ComfoAirCommandType commandType) {
        if (value instanceof UnDefType) {
            logger.trace("\"DataTypeBoolean\" class \"convertFromState\" undefined state");
            return null;
        } else {
            DecimalType decimalValue = value.as(DecimalType.class);
            int[] possible_values = commandType.getPossibleValues();
            int returnValue = 0x01;

            if (possible_values != null) {
                returnValue = possible_values[0];
            }

            if (decimalValue != null) {
                int[] template = commandType.getChangeDataTemplate();

                template[commandType.getChangeDataPos()] = decimalValue.intValue() == 1 ? returnValue : 0x00;

                return template;
            } else {
                logger.trace(
                        "\"DataTypeBoolean\" class \"convertFromState\" method: State value conversion returned null");
                return null;
            }
        }
    }
}
