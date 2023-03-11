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
package org.openhab.binding.broadlink.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.broadlink.internal.BroadlinkRemoteDynamicCommandDescriptionProvider;
import org.openhab.core.thing.Thing;

/**
 * Supports quirks in V44057 firmware.
 * 
 * @author Stewart Cossey - Initial contribution
 */
@NonNullByDefault
public class BroadlinkRemoteModel3V44057Handler extends BroadlinkRemoteModel4Handler {

    public BroadlinkRemoteModel3V44057Handler(Thing thing,
            BroadlinkRemoteDynamicCommandDescriptionProvider commandDescriptionProvider) {
        super(thing, commandDescriptionProvider);
    }

    protected boolean getStatusFromDevice() {
        return true;
    }
}
