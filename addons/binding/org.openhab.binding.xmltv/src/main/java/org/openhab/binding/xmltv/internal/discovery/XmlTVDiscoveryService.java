/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.xmltv.internal.discovery;

import static org.openhab.binding.xmltv.XmlTVBindingConstants.XMLTV_CHANNEL_THING_TYPE;

import java.util.logging.Logger;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.xmltv.XmlTVBindingConstants;
import org.openhab.binding.xmltv.handler.XmlTVHandler;
import org.openhab.binding.xmltv.internal.configuration.XmlChannelConfiguration;
import org.openhab.binding.xmltv.internal.jaxb.Tv;
import org.slf4j.LoggerFactory;

/**
 * The {@link XmlTVDiscoveryService} is responsible for discovering all channels
 * declared in the XmlTV file
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class XmlTVDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(XmlTVDiscoveryService.class);

    private static final int SEARCH_TIME = 10;

    private XmlTVHandler bridgeHandler;

    /**
     * Creates a XmlTVDiscoveryService with background discovery disabled.
     */
    public XmlTVDiscoveryService(XmlTVHandler bridgeHandler) {
        super(XmlTVBindingConstants.SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME);
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting XmlTV discovery scan");
        if (bridgeHandler.getThing().getStatus() == ThingStatus.ONLINE) {
            Tv tv = bridgeHandler.getXmlFile();
            if (tv == null) {
                return;
            }
            tv.getMediaChannels().stream().forEach(channel -> {
                String channelId = channel.getId();
                String uid = channelId.replaceAll("[^A-Za-z0-9_]", "_");
                ThingUID thingUID = new ThingUID(XMLTV_CHANNEL_THING_TYPE, bridgeHandler.getThing().getUID(), uid);

                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                        .withBridge(bridgeHandler.getThing().getUID())
                        .withLabel(channel.getDisplayNames().get(0).getValue()).withRepresentationProperty(uid)
                        .withProperty(XmlChannelConfiguration.CHANNEL_ID, channelId).build();

                thingDiscovered(discoveryResult);
            });
        }
    }

}
