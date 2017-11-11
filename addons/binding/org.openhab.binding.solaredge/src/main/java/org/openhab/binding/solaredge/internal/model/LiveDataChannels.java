/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.solaredge.internal.model;

/**
 * list of all available channels
 *
 * @author Alexander Friese - initial contribution
 *
 */
public enum LiveDataChannels implements Channel {

    PRODUCTION("production", "production", ChannelType.LIVE, ChannelGroup.LIVE, Double.class),
    PV_STATUS("pv_status", "PV status", ChannelType.LIVE, ChannelGroup.LIVE, String.class),

    CONSUMPTION("consumption", "consumption", ChannelType.LIVE, ChannelGroup.LIVE, Double.class),
    LOAD_STATUS("load_status", "load status", ChannelType.LIVE, ChannelGroup.LIVE, String.class),

    BATTERY_CHARGE("battery_charge", "battery charge rate", ChannelType.LIVE, ChannelGroup.LIVE, Double.class),
    BATTERY_LEVEL("battery_level", "battery level", ChannelType.LIVE, ChannelGroup.LIVE, Double.class),
    BATTERY_STATUS("battery_status", "battery status", ChannelType.LIVE, ChannelGroup.LIVE, String.class),
    BATTERY_CRITICAL("battery_critical", "battery critical", ChannelType.LIVE, ChannelGroup.LIVE, String.class),

    IMPORT("import", "import", ChannelType.LIVE, ChannelGroup.LIVE, Double.class),
    EXPORT("export", "export", ChannelType.LIVE, ChannelGroup.LIVE, Double.class),
    GRID_STATUS("grid_status", "grid status", ChannelType.LIVE, ChannelGroup.LIVE, String.class),

    /* END */
    ;

    private final String id;
    private final String name;
    private final ChannelType channelType;
    private final ChannelGroup channelGroup;
    private final Class<?> javaType;

    /**
     * Constructor
     *
     * @param id
     * @param name
     * @param type
     */
    LiveDataChannels(String id, String name, ChannelType channelType, ChannelGroup channelGroup, Class<?> javaType) {
        this.id = id;
        this.name = name;
        this.channelType = channelType;
        this.channelGroup = channelGroup;
        this.javaType = javaType;
    }

    public static LiveDataChannels fromFQName(String fqName) {
        for (LiveDataChannels channel : LiveDataChannels.values()) {
            if (channel.getFQName().equals(fqName)) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final ChannelType getChannelType() {
        return channelType;
    }

    @Override
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public final Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public String getFQName() {
        String fQName = getChannelGroup().toString().toLowerCase() + "#" + getId();
        return fQName;
    }
}
