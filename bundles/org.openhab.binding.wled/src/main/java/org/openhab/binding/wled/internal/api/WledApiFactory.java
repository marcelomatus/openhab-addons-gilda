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
package org.openhab.binding.wled.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.wled.internal.WLedConfiguration;
import org.openhab.binding.wled.internal.WLedHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WledApiFactory} is responsible for creating an instance of the API that will work with different
 * firmware versions.
 *
 * @author Matthew Skinner - Initial contribution
 */
@Component(service = WledApiFactory.class)
@NonNullByDefault
public class WledApiFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final HttpClient httpClient;

    @Activate
    public WledApiFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    public WledApi getApi(WLedHandler wLedHandler, WLedConfiguration config) throws ApiException {
        WledApi lowestSupportedApi = new WledApiV084(wLedHandler, config, httpClient);
        int version = lowestSupportedApi.getFirmwareVersion();
        logger.debug("Treating firmware as int:{}", version);
        // json api was in ver 0.8.4 but may lack testing until 0.10.0 aka 100
        if (version > 100) {
            return new WledApiV084(wLedHandler, config, httpClient);
        }
        logger.warn("Your WLED firmware is very old, upgrade to at least 0.10.0");
        return lowestSupportedApi;
    }
}
