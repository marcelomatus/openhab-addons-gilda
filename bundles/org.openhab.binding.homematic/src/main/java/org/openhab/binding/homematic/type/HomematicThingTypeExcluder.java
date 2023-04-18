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
package org.openhab.binding.homematic.type;

import java.net.URI;
import java.util.Set;

import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;

/**
 * Allows external definition of
 * {@link org.openhab.core.thing.type.ThingType}s for this binding by
 * suppressing those ThingTypes which are generated by
 * {@link org.openhab.binding.homematic.internal.type.HomematicThingTypeProvider}
 *
 * @author Michael Reitler - Initial contribution
 */
public interface HomematicThingTypeExcluder {

    /**
     * Returns list of ThingTypes to be excluded. Clients which implement this
     * are henceforth responsible to ...
     * <li>provide any excluded ThingType on their own - e.g. in a custom
     * {@link org.openhab.core.thing.binding.ThingTypeProvider} or by
     * defining those {@link ThingType}s in XML.</li>
     * <li>provide {@link org.openhab.core.thing.type.ChannelType}s
     * which are introduced by the provided thing-types</li>
     * <li>ensure compatibility and completeness of those thing-types (for any
     * {@link org.openhab.binding.homematic.internal.model.HmDatapoint}
     * there must be a matching
     * {@link org.openhab.core.thing.Channel}) which can be handled by
     * the binding (see
     * {@link org.openhab.binding.homematic.internal.handler.HomematicThingHandler})</li>
     *
     * @return {@link ThingTypeUID}s of ThingTypes that are supposed to be
     *         excluded from the binding's thing-type generation
     */
    Set<ThingTypeUID> getExcludedThingTypes();

    /**
     * Check for the given {@link ThingTypeUID} whether it is excluded by this
     * {@link HomematicThingTypeExcluder} or not
     *
     * @param thingType a specific ThingType, specified by its {@link ThingTypeUID}
     * @return <i>true</i>, if the {@link ThingType} is excluded
     */
    boolean isThingTypeExcluded(ThingTypeUID thingType);

    /**
     * Check for the given {@link ChannelTypeUID} whether it is excluded by this
     * {@link HomematicThingTypeExcluder} or not
     *
     * @param channelType a specific ChannelType, specified by its {@link ChannelTypeUID}
     * @return <i>true</i>, if the {@link org.openhab.core.thing.type.ChannelType} is
     *         excluded
     */
    boolean isChannelTypeExcluded(ChannelTypeUID channelType);

    /**
     * Check for the given {@link ChannelGroupTypeUID} whether it is excluded by
     * this {@link HomematicThingTypeExcluder} or not
     *
     * @param channelGroupType a specific ChannelGroupType, specified by its {@link ChannelGroupTypeUID}
     * @return <i>true</i>, if the
     *         {@link org.openhab.core.thing.type.ChannelGroupType} is
     *         excluded
     */
    boolean isChannelGroupTypeExcluded(ChannelGroupTypeUID channelGroupType);

    /**
     * Check for the given config-description-{@link URI} whether it is excluded by
     * this {@link HomematicThingTypeExcluder} or not
     *
     * @param configDescriptionURI a specific ConfigDescription, specified by a unique {@link URI}
     * @return <i>true</i>, if the {@link org.openhab.core.config.core.ConfigDescription} is
     *         excluded
     */
    boolean isConfigDescriptionExcluded(URI configDescriptionURI);
}
