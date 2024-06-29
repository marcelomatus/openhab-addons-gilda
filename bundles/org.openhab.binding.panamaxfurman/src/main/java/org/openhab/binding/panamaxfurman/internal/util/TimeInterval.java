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
package org.openhab.binding.panamaxfurman.internal.util;

import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Helper object to wrap data for representing a time interval
 *
 * @author Dave Badia - Initial contribution
 */
@NonNullByDefault
public record TimeInterval(int duration, TimeUnit unit) {
}
