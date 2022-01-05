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
package org.openhab.binding.echonetlite.internal;

import org.openhab.core.types.State;

/**
 * @author Michael Barker - Initial contribution
 */
public interface EchonetMessengerService {
    void newDevice(InstanceKey instanceKey, EchonetDeviceListener echonetDeviceListener);

    void refreshDevice(InstanceKey instanceKey, String channelId);

    void removeDevice(InstanceKey instanceKey);

    void updateDevice(InstanceKey instanceKey, String id, State command);

    void startDiscovery(EchonetDiscoveryListener echonetDiscoveryListener);

    void stopDiscovery();
}
