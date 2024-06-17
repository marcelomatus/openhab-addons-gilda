/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.thekeys.internal.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for /lockers endpoint
 *
 * @author Jordan Martin - Initial contribution
 */
public class LockersDTO {

    private List<LockerDTO> devices = new ArrayList<>();

    public List<LockerDTO> getDevices() {
        return devices;
    }
}
