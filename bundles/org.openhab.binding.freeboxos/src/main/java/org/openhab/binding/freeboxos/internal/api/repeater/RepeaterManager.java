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
package org.openhab.binding.freeboxos.internal.api.repeater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.FreeboxOsSession;
import org.openhab.binding.freeboxos.internal.api.ListResponse;
import org.openhab.binding.freeboxos.internal.api.Response;
import org.openhab.binding.freeboxos.internal.api.RestManager;
import org.openhab.binding.freeboxos.internal.api.lan.LanHost;

/**
 * The {@link RepeaterManager} is the Java class used to handle api requests
 * related to repeater
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class RepeaterManager extends RestManager {
    private static String REPEATER_URL = "repeater";
    private List<Repeater> repeaters = new ArrayList<>();

    public RepeaterManager(FreeboxOsSession session) throws FreeboxException {
        super(REPEATER_URL, session);
    }

    public synchronized List<Repeater> getRepeaters() throws FreeboxException {
        if (repeaters.isEmpty()) {
            repeaters.addAll(getList(RepeatersResponse.class, true));
        }
        return repeaters;
    }

    public List<LanHost> getRepeaterHosts(int id) throws FreeboxException {
        UriBuilder myBuilder = getUriBuilder();
        myBuilder.path(Integer.toString(id)).path("host");
        return getList(myBuilder.build(), AccessPointHostsResponse.class, true);
    }

    private synchronized List<LanHost> getHosts() throws FreeboxException {
        List<LanHost> hosts = new ArrayList<>();
        for (Repeater rep : getRepeaters()) {
            List<LanHost> repHosts = getRepeaterHosts(rep.getId());
            hosts.addAll(repHosts);
        }
        return hosts;
    }

    public Map<String, @Nullable LanHost> getHostsMap() throws FreeboxException {
        Map<String, @Nullable LanHost> result = new HashMap<>();
        getHosts().stream().forEach(host -> {
            String mac = host.getMac();
            if (mac != null) {
                result.put(mac, host);
            }
        });
        return result;
    }

    public Repeater getRepeater(int id) throws FreeboxException {
        return get(String.format("%d", id), RepeaterResponse.class, true);
    }

    // Response classes
    private static class RepeatersResponse extends ListResponse<Repeater> {
    }

    private static class RepeaterResponse extends Response<Repeater> {
    }

    private static class AccessPointHostsResponse extends ListResponse<LanHost> {
    }
}
