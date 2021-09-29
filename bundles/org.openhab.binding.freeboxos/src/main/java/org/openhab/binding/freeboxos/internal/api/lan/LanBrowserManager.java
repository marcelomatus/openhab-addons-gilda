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
package org.openhab.binding.freeboxos.internal.api.lan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.lan.LanHost.LanHostsResponse;
import org.openhab.binding.freeboxos.internal.api.lan.LanInterface.LanInterfaceResponse;
import org.openhab.binding.freeboxos.internal.api.lan.LanInterface.LanInterfacesResponse;
import org.openhab.binding.freeboxos.internal.api.rest.FreeboxOsSession;
import org.openhab.binding.freeboxos.internal.api.rest.ListableRest;

/**
 * The {@link LanBrowserManager} is the Java class used to handle api requests
 * related to lan
 * https://dev.freebox.fr/sdk/os/system/#
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class LanBrowserManager extends ListableRest<LanInterface, LanInterfaceResponse, LanInterfacesResponse> {
    private static final String INTERFACES_SUB_PATH = "interfaces";
    private static final String BROWSER_SUB_PATH = "browser";

    public LanBrowserManager(FreeboxOsSession session) throws FreeboxException {
        super(LanManager.LAN_SUB_PATH + "/" + BROWSER_SUB_PATH, session, LanInterfaceResponse.class,
                LanInterfacesResponse.class);
        listSubPath = INTERFACES_SUB_PATH;
    }

    private List<LanHost> getInterfaceHosts(String lanInterface) throws FreeboxException {
        UriBuilder myBuilder = getUriBuilder().path(lanInterface);
        return getList(LanHostsResponse.class, myBuilder.build());
    }

    private synchronized List<LanHost> getHosts() throws FreeboxException {
        List<LanHost> hosts = new ArrayList<>();

        for (LanInterface intf : getDevices()) {
            String name = intf.getName();
            if (name != null) {
                List<LanHost> intfHosts = getInterfaceHosts(name);
                hosts.addAll(intfHosts);
            }
        }
        return hosts;
    }

    public Map<String, LanHost> getHostsMap() throws FreeboxException {
        Map<String, LanHost> result = new HashMap<>();
        getHosts().stream().forEach(host -> {
            String mac = host.getMac();
            if (mac != null) {
                result.put(mac, host);
            }
        });
        return result;
    }

    public Optional<LanHost> getHost(String mac) throws FreeboxException {
        return Optional.ofNullable(getHostsMap().get(mac));
    }

    public void wakeOnLan(String host) throws FreeboxException {
        WakeOnLineData wol = new WakeOnLineData(host);
        post("wol/" + host, wol);
    }
}
