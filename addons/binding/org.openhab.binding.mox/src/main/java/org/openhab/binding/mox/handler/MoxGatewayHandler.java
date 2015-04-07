/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mox.handler;

import static org.openhab.binding.mox.MoxBindingConstants.STATE;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.mox.protocol.MoxConnector;
import org.openhab.binding.mox.protocol.MoxMessage;
import org.openhab.binding.mox.protocol.MoxMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MoxGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Thomas Eichstaedt-Engelen (innoQ) - Initial contribution
 * @since 0.8.0
 */
public class MoxGatewayHandler extends BaseThingHandler implements MoxMessageListener {

	private Logger logger = LoggerFactory.getLogger(MoxGatewayHandler.class);

	private MoxConnector connector;

	private List<MoxMessageListener> messageListeners = new CopyOnWriteArrayList<>();
	
	
	public MoxGatewayHandler(Thing thing) {
		super(thing);
	}
	
	
	@Override
	public void initialize() {
		super.initialize();

		connector = new MoxConnector("localhost", 6667);
		connector.setMessageHandler(this);
		try {
			connector.connect();
			connector.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        if (connector != null) {
            try {
				connector.disconnect();
	            connector = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    
	public boolean registerMessageListener(MoxMessageListener messageListener) {
		if (messageListener == null) {
			throw new NullPointerException("It's not allowed to pass a null MessageListener.");
		}
		boolean result = messageListeners.add(messageListener);
		if (result) {
		}
		return result;
	}

	public boolean unregisterLightStatusListener(
			MoxMessageListener messageListener) {
		boolean result = messageListeners.remove(messageListener);
		if (result) {
		}
		return result;
	}
	

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (channelUID.getId().equals(STATE)) {
			// TODO: handle command
		}
	}
	
	@Override
	public void onMessage(MoxMessage message) {
		for (MoxMessageListener listener : messageListeners) {
			listener.onMessage(message);
		}
	}

}
