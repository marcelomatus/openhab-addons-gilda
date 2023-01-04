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
package org.openhab.binding.warmup.internal.model.query;

import java.util.List;

/**
 * @author James Melville - Initial contribution
 */
public class LocationDTO {

    private int id;
    private String name;
    private List<RoomDTO> rooms;

    public String getId() {
        return String.valueOf(id);
    }

    public String getName() {
        return name;
    }

    public List<RoomDTO> getRooms() {
        return rooms;
    }
}
