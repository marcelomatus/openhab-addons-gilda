/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.yeelight.internal.lib.listeners;

import org.openhab.binding.yeelight.internal.lib.device.DeviceBase;

/**
 * @author Coaster Li - Initial contribution
 */
public interface DeviceListener {
    void onDeviceFound(DeviceBase device);

    void onDeviceLost(DeviceBase device);
}
