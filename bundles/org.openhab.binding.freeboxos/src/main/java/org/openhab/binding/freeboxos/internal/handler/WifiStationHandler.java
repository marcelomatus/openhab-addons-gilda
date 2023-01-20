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
package org.openhab.binding.freeboxos.internal.handler;

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.*;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.rest.APManager;
import org.openhab.binding.freeboxos.internal.api.rest.APManager.LanAccessPoint;
import org.openhab.binding.freeboxos.internal.api.rest.APManager.Station;
import org.openhab.binding.freeboxos.internal.api.rest.LanBrowserManager.LanHost;
import org.openhab.binding.freeboxos.internal.api.rest.RepeaterManager;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.UnDefType;

/**
 * The {@link WifiStationHandler} is responsible for handling everything associated to
 * any Freebox thing types except the bridge thing type.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class WifiStationHandler extends HostHandler {
    private static final String SERVER_HOST = "Server";

    public WifiStationHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        super.internalPoll();

        // Search if the wifi-host is hosted on server access-points
        Optional<Station> station = getManager(APManager.class).getStation(getMac());
        if (station.isPresent()) {
            Station data = station.get();
            updateChannelDateTimeState(CONNECTIVITY, LAST_SEEN, data.getLastSeen());
            updateChannelString(GROUP_WIFI, WIFI_HOST, SERVER_HOST);
            updateWifiStationChannels(data.signal(), data.getSsid(), data.rxRate(), data.txRate());
            return;
        }

        // Search if it is hosted by a repeater
        Optional<LanHost> wifiHost = getManager(RepeaterManager.class).getHost(getMac());
        if (wifiHost.isPresent()) {
            updateChannelDateTimeState(CONNECTIVITY, LAST_SEEN, wifiHost.get().getLastSeen());
            LanAccessPoint lanAp = wifiHost.get().accessPoint();
            if (lanAp != null) {
                updateChannelString(GROUP_WIFI, WIFI_HOST, "%s-%s".formatted(lanAp.type(), lanAp.uid()));
                updateWifiStationChannels(lanAp.getSignal(), lanAp.getSsid(), lanAp.rxRate(), lanAp.txRate());
                return;
            }
        }
        // Not found a wifi repeater/host, so update all wifi channels to NULL
        getThing().getChannelsOfGroup(GROUP_WIFI).stream().map(Channel::getUID).filter(uid -> isLinked(uid))
                .forEach(uid -> updateState(uid, UnDefType.NULL));
    }

    private void updateWifiStationChannels(int rssi, @Nullable String ssid, long rxRate, long txRate) {
        updateChannelString(GROUP_WIFI, SSID, ssid);
        updateChannelQuantity(GROUP_WIFI, RSSI, rssi <= 0 ? new QuantityType<>(rssi, Units.DECIBEL_MILLIWATTS) : null);
        updateChannelDecimal(GROUP_WIFI, WIFI_QUALITY, rssi <= 0 ? toQoS(rssi) : null);
        updateRateChannel(RATE_DOWN, rxRate);
        updateRateChannel(RATE_UP, txRate);
=======
=======
import org.eclipse.jdt.annotation.Nullable;
>>>>>>> e4ef5cc Switching to Java 17 records
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.rest.APManager;
import org.openhab.binding.freeboxos.internal.api.rest.RepeaterManager;
import org.openhab.binding.freeboxos.internal.api.rest.APManager.LanAccessPoint;
import org.openhab.binding.freeboxos.internal.api.rest.APManager.Station;
import org.openhab.binding.freeboxos.internal.api.rest.LanBrowserManager.LanHost;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.UnDefType;

/**
 * The {@link WifiStationHandler} is responsible for handling everything associated to
 * any Freebox thing types except the bridge thing type.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class WifiStationHandler extends HostHandler {
    private static final String SERVER_HOST = "Server";

    public WifiStationHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalPoll() throws FreeboxException {
        super.internalPoll();

        // Search if the wifi-host is hosted on server access-points
        Optional<Station> station = getManager(APManager.class).getStation(getMac());
        if (station.isPresent()) {
            Station data = station.get();
            updateChannelDateTimeState(CONNECTIVITY, LAST_SEEN, data.getLastSeen());
            updateChannelString(GROUP_WIFI, WIFI_HOST, SERVER_HOST);
            updateWifiStationChannels(data.signal(), data.getSsid(), data.rxRate(), data.txRate());
            return;
        }

        // Search if it is hosted by a repeater
        Optional<LanHost> wifiHost = getManager(RepeaterManager.class).getHost(getMac());
        if (wifiHost.isPresent()) {
            updateChannelDateTimeState(CONNECTIVITY, LAST_SEEN, wifiHost.get().getLastSeen());
            LanAccessPoint lanAp = wifiHost.get().accessPoint();
            if (lanAp != null) {
                updateChannelString(GROUP_WIFI, WIFI_HOST, "%s-%s".formatted(lanAp.type(), lanAp.uid()));
                updateWifiStationChannels(lanAp.getSignal(), lanAp.getSsid(), lanAp.rxRate(), lanAp.txRate());
                return;
            }
        }
        // Not found a wifi repeater/host, so update all wifi channels to NULL
        getThing().getChannelsOfGroup(GROUP_WIFI).stream().map(Channel::getUID).filter(uid -> isLinked(uid))
                .forEach(uid -> updateState(uid, UnDefType.NULL));
    }

    private void updateWifiStationChannels(int rssi, @Nullable String ssid, long rxRate, long txRate) {
        updateChannelString(GROUP_WIFI, SSID, ssid);
        updateChannelQuantity(GROUP_WIFI, RSSI, rssi <= 0 ? new QuantityType<>(rssi, Units.DECIBEL_MILLIWATTS) : null);
        updateChannelDecimal(GROUP_WIFI, WIFI_QUALITY, rssi <= 0 ? toQoS(rssi) : null);
<<<<<<< Upstream, based on origin/main
        updateRateChannel(RATE_DOWN, wifidevice.getRxRate());
        updateRateChannel(RATE_UP, wifidevice.getTxRate());
>>>>>>> 006a813 Saving work before instroduction of ArrayListDeserializer
=======
        updateRateChannel(RATE_DOWN, rxRate);
        updateRateChannel(RATE_UP, txRate);
>>>>>>> e4ef5cc Switching to Java 17 records
    }

    private void updateRateChannel(String channel, long rate) {
        QuantityType<?> qtty = rate != -1 ? new QuantityType<>(rate * 8, Units.BIT_PER_SECOND) : null;
        updateChannelQuantity(GROUP_WIFI, channel, qtty);
    }

    private int toQoS(int rssi) {
        return rssi > -50 ? 4 : rssi > -60 ? 3 : rssi > -70 ? 2 : rssi > -85 ? 1 : 0;
    }
}
