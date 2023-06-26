/**
<<<<<<< Upstream, based on origin/main
 * Copyright (c) 2010-2023 Contributors to the openHAB project
=======
 * Copyright (c) 2010-2022 Contributors to the openHAB project
>>>>>>> 46dadb1 SAT warnings handling
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
package org.openhab.binding.freeboxos.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LandlineConfiguration} is responsible for holding
 * configuration informations associated to a Freebox Phone thing type
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class LandlineConfiguration extends ApiConsumerConfiguration {
    public int id = 1;

    LandlineConfiguration() {
        refreshInterval = 2;
    }
}
