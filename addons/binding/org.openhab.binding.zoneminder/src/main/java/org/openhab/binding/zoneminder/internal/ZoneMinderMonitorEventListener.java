/**
 * Copyright (c) 2014-2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zoneminder.internal;

import java.util.List;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import name.eskildsen.zoneminder.api.ZoneMinderData;

public interface ZoneMinderMonitorEventListener {

    void notifyZoneMinderApiDataUpdated(ThingTypeUID thingTypeUID, String ZoneMinderId, List<ZoneMinderData> arrData);

    // TODO:: FIXME void notifyZoneMinderEvent(ZoneMinderEvent event);
}
