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
package org.openhab.binding.siemenshvac.internal.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.osgi.service.component.annotations.Component;

/**
 * Provides all ChannelGroupTypes from all Homematic bridges.
 *
 * @author Laurent Arnal - Initial contribution
 */
@Component(service = { SiemensHvacChannelGroupTypeProvider.class, ChannelGroupTypeProvider.class })
public class SiemensHvacChannelGroupTypeProviderImpl implements SiemensHvacChannelGroupTypeProvider {

    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypesByUID = new HashMap<>();

    //

    @Override
    public ChannelGroupType getInternalChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID) {
        return channelGroupTypesByUID.get(channelGroupTypeUID);
    }

    @Override
    public void addChannelGroupType(ChannelGroupType channelGroupType) {
        channelGroupTypesByUID.put(channelGroupType.getUID(), channelGroupType);
    }

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, @Nullable Locale locale) {
        return channelGroupTypesByUID.get(channelGroupTypeUID);
    }

    /**
     *
     * @see ChannelTypeRegistry#getChannelGroupTypes(Locale)
     *
     */
    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        Collection<ChannelGroupType> result = new ArrayList<>();
        for (ChannelGroupTypeUID uid : channelGroupTypesByUID.keySet()) {
            result.add(channelGroupTypesByUID.get(uid));
        }
        return result;

    }
}
