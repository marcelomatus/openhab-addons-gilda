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
package org.openhab.binding.netatmo.internal.channelhelper;

import static org.openhab.binding.netatmo.internal.NetatmoBindingConstants.*;
import static org.openhab.binding.netatmo.internal.utils.ChannelTypeUtils.toStringType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.netatmo.internal.api.dto.NAHome;
import org.openhab.binding.netatmo.internal.api.dto.NAHomeSecurity;
import org.openhab.binding.netatmo.internal.api.dto.NAPerson;
import org.openhab.binding.netatmo.internal.api.dto.NAPlace;
import org.openhab.binding.netatmo.internal.api.dto.NAThing;
import org.openhab.binding.netatmo.internal.deserialization.NAObjectMap;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeSecurityChannelHelper} handle specific behavior
 * of modules using batteries
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */
@NonNullByDefault
public class HomeSecurityChannelHelper extends AbstractChannelHelper {
    private final Logger logger = LoggerFactory.getLogger(HomeSecurityChannelHelper.class);

    private int persons = -1;
    private int unknowns = -1;

    public HomeSecurityChannelHelper(Thing thing, TimeZoneProvider timeZoneProvider) {
        super(thing, timeZoneProvider, Set.of(GROUP_HOME_SECURITY));
    }

    @Override
    public void setNewData(NAThing naThing) {
        super.setNewData(naThing);
        if (naThing instanceof NAHomeSecurity) {
            NAHomeSecurity home = (NAHomeSecurity) naThing;

            persons = 0;
            unknowns = 0;

            logger.debug("welcome home '{}' counts Persons at home", home.getId());

            NAObjectMap<NAPerson> personList = home.getPersons();
            // if (personList != null) {
            List<NAPerson> present = personList.values().stream().filter(p -> !p.isOutOfSight())
                    .collect(Collectors.toList());
            persons = present.size();
            present = present.stream().filter(p -> p.getName() != null).collect(Collectors.toList());
            unknowns = persons - present.size();
            // }

        }
    }

    @Override
    protected @Nullable State internalGetProperty(NAThing naThing, String channelId) {
        if (CHANNEL_HOME_PERSONCOUNT.equals(channelId)) {
            return persons != -1 ? new DecimalType(persons) : UnDefType.UNDEF;
        } else if (CHANNEL_HOME_UNKNOWNCOUNT.equals(channelId)) {
            return unknowns != -1 ? new DecimalType(unknowns) : UnDefType.UNDEF;
        }
        NAHome localThing = (NAHome) naThing;
        NAPlace place = localThing.getPlace();
        return place == null ? null
                : CHANNEL_HOME_CITY.equals(channelId) ? toStringType(place.getCity())
                        : CHANNEL_HOME_COUNTRY.equals(channelId) ? toStringType(place.getCountry())
                                : CHANNEL_HOME_TIMEZONE.equals(channelId) ? toStringType(place.getTimezone()) : null;
    }
}
