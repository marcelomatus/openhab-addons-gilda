/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.loxone.internal.core;

import java.util.HashSet;
import java.util.Set;

import org.openhab.binding.loxone.internal.controls.LxControl;

/**
 * Container on Loxone Miniserver that groups {@link LxControl} objects.
 * <p>
 * Examples of containers are rooms and categories.
 *
 * @author Pawel Pieczul - initial contribution
 *
 */
public class LxContainer {
    private final LxUuid uuid;
    private final Set<LxControl> controls = new HashSet<>();
    private String name;

    /**
     * Create a new container with given uuid and name
     *
     * @param uuid
     *                 UUID of the container as received from the Miniserver
     * @param name
     *                 name of the container as received from the Miniserver
     */
    public LxContainer(LxUuid uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    /**
     * Obtain container's current name
     *
     * @return
     *         container's current name
     */
    public String getName() {
        return name;
    }

    /**
     * Obtain container's UUID (assigned by Loxone Miniserver)
     *
     * @return
     *         container's UUID
     */
    public LxUuid getUuid() {
        return uuid;
    }

    /**
     * Update container's name
     *
     * @param name
     *                 a new name of the container
     */
    public void setName(String name) {
        this.name = name;
        uuid.setUpdate(true);
    }

    /**
     * Add a new control to this container or mark existing control's and container's UUIDs as updated.
     *
     * @param control control to be added or updated
     */
    public void addOrUpdateControl(LxControl control) {
        uuid.setUpdate(true);
        for (LxControl c : controls) {
            if (control.equals(c)) {
                c.getUuid().setUpdate(true);
                return;
            }
        }
        controls.add(control);
        control.getUuid().setUpdate(true);
    }

    /**
     * Removes a control from the container
     *
     * @param control control object to remove from the container
     * @return true if control object existed in the container and was removed
     */
    public boolean removeControl(LxControl control) {
        return controls.remove(control);
    }
}
