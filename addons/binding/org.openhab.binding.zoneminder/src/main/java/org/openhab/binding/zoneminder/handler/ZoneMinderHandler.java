/**
 * Copyright (c) 2014-2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zoneminder.handler;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.eclipse.smarthome.core.thing.ChannelUID;

import name.eskildsen.zoneminder.exception.ZoneMinderUrlNotFoundException;
import name.eskildsen.zoneminder.interfaces.IZoneMinderConnectionInfo;

/**
 * Interface for ZoneMinder handlers.
 *
 * @author Martin S. Eskildsen
 */
public interface ZoneMinderHandler {
    String getZoneMinderId();

    void updateAvaliabilityStatus(IZoneMinderConnectionInfo connection);

    void updateChannel(ChannelUID channel);

    void onBridgeConnected(ZoneMinderServerBridgeHandler bridge, IZoneMinderConnectionInfo connection)
            throws IllegalArgumentException, GeneralSecurityException, IOException, ZoneMinderUrlNotFoundException;

    void onBridgeDisconnected(ZoneMinderServerBridgeHandler bridge);

}
