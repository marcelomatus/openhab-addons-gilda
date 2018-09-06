/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpstracker.internal.message;

import org.eclipse.smarthome.core.library.types.PointType;

/**
 * Region POJO
 *
 * @author Gabor Bicskei - Initial contribution
 */
public class Region {
    /**
     * Region name
     */
    private String name;

    /**
     * Location coordinates
     */
    private PointType location;

    /**
     * Region radius
     */
    private Integer radius;

    /**
     * Entering or leaving should trigger an event
     */
    private Boolean triggerEvent;

    /**
     * True for the primary region
     */
    private Boolean primary = false;

    public Region(String name, PointType location, Integer radius, Boolean triggerEvent, Boolean primary) {
        this.name = name;
        this.location = location;
        this.radius = radius;
        this.triggerEvent = triggerEvent;
        this.primary = primary;
    }

    public Region() {
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return name != null ? name.replaceAll("[^a-zA-Z0-9_]", ""): "UnnamedRegion";
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRadius() {
        return this.radius;
    }

    public Boolean getTriggerEvent() {
        return this.triggerEvent;
    }

    public void setTriggerEvent(Boolean triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public Boolean isPrimary() {
        return primary != null && primary;
    }

    public PointType getLocation() {
        return location != null && radius != null ? location: null;
    }
}
