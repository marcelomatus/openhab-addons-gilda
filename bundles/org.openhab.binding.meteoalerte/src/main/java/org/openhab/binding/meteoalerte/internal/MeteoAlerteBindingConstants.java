/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.meteoalerte.internal;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.meteoalerte.internal.json.ResponseFieldDTO.AlertLevel;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.State;

/**
 * The {@link MeteoAlerteBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class MeteoAlerteBindingConstants {
    public static final String BINDING_ID = "meteoalerte";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_METEO_ALERT = new ThingTypeUID(BINDING_ID, "department");

    // List of all Channel id's
    public static final String WAVE = "vague-submersion";
    public static final String AVALANCHE = "avalanches";
    public static final String HEAT = "canicule";
    public static final String FREEZE = "grand-froid";
    public static final String FLOOD = "inondation";
    public static final String SNOW = "neige";
    public static final String STORM = "orage";
    public static final String RAIN = "pluie-inondation";
    public static final String WIND = "vent";
    public static final String OBSERVATION_TIME = "observation-time";
    public static final String END_TIME = "end-time";
    public static final String COMMENT = "comment";

    public static final String UNKNOWN_COLOR = "b3b3b3";

    public static final Map<AlertLevel, String> ALERT_COLORS = Map.of(AlertLevel.GREEN, "00ff00", AlertLevel.YELLOW,
            "ffff00", AlertLevel.ORANGE, "ff6600", AlertLevel.RED, "ff0000", AlertLevel.UNKNOWN, UNKNOWN_COLOR);

    public static final Map<AlertLevel, State> ALERT_LEVELS = Map.of(AlertLevel.GREEN, DecimalType.ZERO,
            AlertLevel.YELLOW, new DecimalType(1), AlertLevel.ORANGE, new DecimalType(2), AlertLevel.RED,
            new DecimalType(3));

}
