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
package org.openhab.binding.deconz.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ThingConfig} class holds the configuration properties of a sensor Thing.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class ThingConfig {
    public String id = "";
    public int lastSeenPolling = 1440;
    public @Nullable Double transitiontime;
    public String colormode = "";
}
