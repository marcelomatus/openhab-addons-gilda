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
package org.openhab.binding.freeboxos.internal.api.player;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FbxDevice;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class Player extends FbxDevice {
    private boolean apiAvailable;
    private boolean reachable;

    public boolean isApiAvailable() {
        return apiAvailable;
    }

    public boolean isReachable() {
        return reachable;
    }
}
