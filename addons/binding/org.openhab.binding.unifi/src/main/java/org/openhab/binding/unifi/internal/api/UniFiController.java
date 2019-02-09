/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.unifi.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.unifi.internal.api.model.UniFiClient;
import org.openhab.binding.unifi.internal.api.model.UniFiDevice;
import org.openhab.binding.unifi.internal.api.model.UniFiSite;
import org.openhab.binding.unifi.internal.api.util.UniFiClientDeserializer;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link UniFiController} is the main communication point with an external instance of the Ubiquiti Networks
 * Controller Software.
 *
 * @author Matthew Bowman - Initial contribution
 */
@NonNullByDefault
public class UniFiController {

    private final HttpClient httpClient;

    private final String host;

    private final int port;

    private final String username;

    private final String password;

    private final Gson gson;

    public UniFiController(HttpClient httpClient, String host, int port, String username, String password) {
        this.httpClient = httpClient;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(UniFiClient.class, new UniFiClientDeserializer()).create();
    }

    // Public API

    public void start() throws UniFiException {
        login();
    }

    public void stop() throws UniFiException {
        logout();
    }

    public void login() throws UniFiException {
        UniFiControllerRequest<Void> req = newRequest(Void.class);
        req.setPath("/api/login");
        req.setBodyParameter("username", username);
        req.setBodyParameter("password", password);
        // scurb: Changed to make blocking feature work.
        req.setBodyParameter("strict", false);
        req.setBodyParameter("remember", false);
        executeRequest(req);
    }

    public UniFiSite[] getSites() throws UniFiException {
        UniFiControllerRequest<UniFiSite[]> req = newRequest(UniFiSite[].class);
        req.setPath("/api/self/sites");
        return executeRequest(req);
    }

    public UniFiDevice[] getDevices(UniFiSite site) throws UniFiException {
        UniFiControllerRequest<UniFiDevice[]> req = newRequest(UniFiDevice[].class);
        req.setPath("/api/s/" + site.getName() + "/stat/device");
        return executeRequest(req);
    }

    public UniFiClient[] getClients(UniFiSite site) throws UniFiException {
        UniFiControllerRequest<UniFiClient[]> req = newRequest(UniFiClient[].class);
        req.setPath("/api/s/" + site.getName() + "/stat/sta");
        return executeRequest(req);
    }

    public UniFiClient[] getInsights(UniFiSite site) throws UniFiException {
        UniFiControllerRequest<UniFiClient[]> req = newRequest(UniFiClient[].class);
        req.setPath("/api/s/" + site.getName() + "/stat/alluser");
        req.setQueryParameter("within", 168); // scurb: Changed to 7 days.
        return executeRequest(req);
    }

    public void logout() throws UniFiException {
        UniFiControllerRequest<Void> req = newRequest(Void.class);
        req.setPath("/logout");
        executeRequest(req);
    }

    public UniFiClient[] blockStation(UniFiClient client) throws UniFiException {
        UniFiControllerRequest<UniFiClient[]> req = newRequest(UniFiClient[].class);

        String siteName = "";
        if (client.getDevice() == null) {
            UniFiSite[] sites = getSites();
            for (UniFiSite site : sites) {
                if (site.getId().equalsIgnoreCase(client.getSiteId())) {
                    siteName = site.getName();
                }
            }
        } else {
            siteName = client.getDevice().getSite().getName();
        }

        String url = "/api/s/" + siteName + "/cmd/stamgr";
        req.setPath(url);
        req.setBodyParameter("cmd", "block-sta");
        req.setBodyParameter("mac", client.getMac());
        return executeRequest(req);
    }

    public UniFiClient @Nullable [] unblockStation(UniFiClient client) throws UniFiException {
        UniFiControllerRequest<UniFiClient[]> req = newRequest(UniFiClient[].class);

        String siteName = "";
        if (client.getDevice() == null) {
            UniFiSite[] sites = getSites();
            for (UniFiSite site : sites) {
                if (site.getId().equalsIgnoreCase(client.getSiteId())) {
                    siteName = site.getName();
                }
            }
        } else {
            siteName = client.getDevice().getSite().getName();
        }

        if (siteName.equalsIgnoreCase("")) {
            return null;
        }

        String url = "/api/s/" + siteName + "/cmd/stamgr";
        req.setPath(url);
        req.setBodyParameter("cmd", "unblock-sta");
        req.setBodyParameter("mac", client.getMac());
        return executeRequest(req);
    }

    // Private API

    private <T> UniFiControllerRequest<T> newRequest(Class<T> responseType) {
        return new UniFiControllerRequest<T>(responseType, gson, httpClient, host, port);
    }

    private <T> T executeRequest(UniFiControllerRequest<T> request) throws UniFiException {
        T result;
        try {
            result = request.execute();
        } catch (UniFiExpiredSessionException e) {
            login();
            result = executeRequest(request);
        }
        return result;
    }

}
