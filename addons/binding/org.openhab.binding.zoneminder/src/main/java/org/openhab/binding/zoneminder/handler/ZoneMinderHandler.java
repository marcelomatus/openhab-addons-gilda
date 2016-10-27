/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zoneminder.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;

public interface ZoneMinderHandler {
    String getZoneMinderId();

    void updateAvaliabilityStatus();

    Boolean isOnline();

    Boolean isRunning();

    void updateChannel(ChannelUID channel);

    void onBridgeConnected(ZoneMinderBaseBridgeHandler bridge);

    void onBridgeDisconnected(ZoneMinderBaseBridgeHandler bridge);

}
