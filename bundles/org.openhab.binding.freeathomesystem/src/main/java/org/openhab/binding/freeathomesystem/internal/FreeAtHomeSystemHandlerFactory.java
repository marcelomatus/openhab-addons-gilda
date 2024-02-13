/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.freeathomesystem.internal;

import static org.openhab.binding.freeathomesystem.internal.FreeAtHomeSystemBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.freeathomesystem.internal.handler.FreeAtHomeBridgeHandler;
import org.openhab.binding.freeathomesystem.internal.handler.FreeAtHomeDeviceHandler;
import org.openhab.binding.freeathomesystem.internal.type.FreeAtHomeChannelTypeProvider;
import org.openhab.binding.freeathomesystem.internal.type.FreeAtHomeThingTypeProvider;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link FreeAtHomeSystemHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Andras Uhrin - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.freeathomesystem", service = ThingHandlerFactory.class)
public class FreeAtHomeSystemHandlerFactory extends BaseThingHandlerFactory {

    private final HttpClient httpClient;
    private final FreeAtHomeChannelTypeProvider channelTypeProvider;
    private final TranslationProvider i18nProvider;
    private final LocaleProvider localeProvider;

    @Activate
    public FreeAtHomeSystemHandlerFactory(@Reference FreeAtHomeThingTypeProvider thingTypeProvider,
            @Reference FreeAtHomeChannelTypeProvider channelTypeProvider, @Reference TranslationProvider i18nProvider,
            @Reference LocaleProvider localeProvider, @Reference HttpClientFactory httpClientFactory,
            ComponentContext componentContext) {
        super.activate(componentContext);
        this.channelTypeProvider = channelTypeProvider;
        this.httpClient = httpClientFactory.createHttpClient(FreeAtHomeSystemBindingConstants.BINDING_ID);
        this.i18nProvider = i18nProvider;
        this.localeProvider = localeProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (BRIDGE_TYPE_UID.equals(thingTypeUID)) {
            return new FreeAtHomeBridgeHandler((Bridge) thing, httpClient);
        } else if (FREEATHOMEDEVICE_TYPE_UID.equals(thingTypeUID)) {
            return new FreeAtHomeDeviceHandler(thing, channelTypeProvider, i18nProvider, localeProvider);
        }

        return null;
    }
}
