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
package org.openhab.binding.homematic.internal.type;

import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;

/**
 * Extends the ChannelGroupTypeProvider to manually add a ChannelGroupType.
 *
 * @author Michael Reitler - Initial contribution
 */
public interface HomematicChannelGroupTypeProvider extends ChannelGroupTypeProvider {

    /**
     * Adds the ChannelGroupType to this provider.
     */
    public void addChannelGroupType(ChannelGroupType channelGroupType);

    /**
     * Use this method to lookup a ChannelGroupType which was generated by the
     * homematic binding. Other than {@link #getChannelGroupType(ChannelGroupTypeUID, Locale)}
     * of this provider, it will return also those {@link ChannelGroupType}s
     * which are excluded by {@link HomematicThingTypeExcluder}
     *
     * @param channelGroupTypeUID
     *            e.g. <i>homematic:HM-WDS40-TH-I-2_0</i>
     * @return ChannelGroupType that was added to HomematicChannelGroupTypeProvider, identified
     *         by its config-description-uri<br>
     *         <i>null</i> if no ChannelGroupType with the given UID was added
     *         before
     */
    public ChannelGroupType getInternalChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID);
}
