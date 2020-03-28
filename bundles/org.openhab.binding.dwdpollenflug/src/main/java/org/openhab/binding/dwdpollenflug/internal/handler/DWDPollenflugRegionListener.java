/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.dwdpollenflug.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dwdpollenflug.internal.dto.DWDPollenflug;

/**
 * The {@link DWDPollenflugRegionListener} is the interface for communication
 * from bridge to region handler
 *
 * @author Johannes DerOetzi Ott - Initial contribution
 */
@NonNullByDefault
public interface DWDPollenflugRegionListener {
    public void notifyOnUpdate(DWDPollenflug pollenflug);
}