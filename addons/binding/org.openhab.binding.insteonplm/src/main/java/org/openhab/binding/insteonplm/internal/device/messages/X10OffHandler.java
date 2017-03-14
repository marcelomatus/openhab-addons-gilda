package org.openhab.binding.insteonplm.internal.device.messages;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.openhab.binding.insteonplm.handler.InsteonThingHandler;
import org.openhab.binding.insteonplm.internal.device.InsteonAddress;
import org.openhab.binding.insteonplm.internal.device.X10DeviceFeature;
import org.openhab.binding.insteonplm.internal.device.X10MessageHandler;
import org.openhab.binding.insteonplm.internal.message.modem.X10MessageReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Pfrommer
 * @author Bernd Pfrommer
 */
public class X10OffHandler extends X10MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(X10OffHandler.class);

    X10OffHandler(X10DeviceFeature p) {
        super(p);
    }

    @Override
    public void handleMessage(InsteonThingHandler handler, X10MessageReceived msg, Channel f) {
        InsteonAddress a = handler.getAddress();
        logger.info("{}: set X10 device {} to OFF", nm(), a);
        handler.updateFeatureState(f, OnOffType.OFF);
    }
}
