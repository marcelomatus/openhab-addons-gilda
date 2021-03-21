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
package org.openhab.binding.tapocontrol.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TapoControlConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Wild - Initial contribution
 */

@NonNullByDefault
public final class TapoControlConfiguration {
    /**
     * thing configuration parameter.
     */
    public String ipAddress = "";
    public Integer pollingInterval = 30;
}
