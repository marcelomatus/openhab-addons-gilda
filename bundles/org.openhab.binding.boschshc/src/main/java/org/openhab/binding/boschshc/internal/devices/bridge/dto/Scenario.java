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
package org.openhab.binding.boschshc.internal.devices.bridge.dto;

import org.openhab.binding.boschshc.internal.services.dto.BoschSHCServiceState;

/**
 * A scenario as represented by the controller.
 *
 * Json example:
 * {
 * "@type": "scenarioTriggered",
 * "name": "My scenario",
 * "id": "509bd737-eed0-40b7-8caa-e8686a714399",
 * "lastTimeTriggered": "1693758693032"
 * }
 *
 * @author Patrick Gell - Initial contribution
 */
public class Scenario extends BoschSHCServiceState {

    public String name;
    public String id;
    public String lastTimeTriggered;

    public Scenario() {
        super("scenarioTriggered");
    }
}
