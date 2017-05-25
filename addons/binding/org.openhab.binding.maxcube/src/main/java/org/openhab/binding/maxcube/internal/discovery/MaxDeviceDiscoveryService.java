/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.maxcube.internal.discovery;


import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.maxcube.MaxCubeBinding;
import org.openhab.binding.maxcube.internal.MaxCube;
import org.openhab.binding.maxcube.internal.handler.DeviceStatusListener;
import org.openhab.binding.maxcube.internal.handler.MaxCubeBridgeHandler;
import org.openhab.binding.maxcube.internal.message.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MaxDeviceDiscoveryService} class is used to discover MAX! Cube devices that  
 * are connected to the Lan gateway. 
 * 
 * @author Marcel Verpaalen - Initial contribution
 */
public class MaxDeviceDiscoveryService  extends AbstractDiscoveryService implements DeviceStatusListener {

	private final static Logger logger = LoggerFactory.getLogger(MaxDeviceDiscoveryService.class);

	private MaxCubeBridgeHandler maxCubeBridgeHandler;

	public MaxDeviceDiscoveryService( MaxCubeBridgeHandler maxCubeBridgeHandler) {
		super(MaxCubeBinding.SUPPORTED_DEVICE_THING_TYPES_UIDS, 10,true);
		this.maxCubeBridgeHandler = maxCubeBridgeHandler;
	}

	public void activate() {
		maxCubeBridgeHandler.registerDeviceStatusListener(this);
	}

	public void deactivate() {
		maxCubeBridgeHandler.unregisterDeviceStatusListener(this);
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return MaxCubeBinding.SUPPORTED_DEVICE_THING_TYPES_UIDS;
	}

	@Override
	public void onDeviceAdded(MaxCube bridge, Device device) {
		logger.trace("Adding new MAX! {} with id '{}' to smarthome inbox", device.getType(), device.getSerialNumber());
		ThingUID thingUID = null;
		switch (device.getType()) {
		case WallMountedThermostat:
			thingUID = new ThingUID(MaxCubeBinding.WALLTHERMOSTAT_THING_TYPE,device.getSerialNumber());
			break;
		case HeatingThermostat:
			thingUID = new ThingUID(MaxCubeBinding.HEATINGTHERMOSTAT_THING_TYPE,device.getSerialNumber());
			break;
		case HeatingThermostatPlus:
			thingUID = new ThingUID(MaxCubeBinding.HEATINGTHERMOSTATPLUS_THING_TYPE,device.getSerialNumber());
			break;
		case ShutterContact:
			thingUID = new ThingUID(MaxCubeBinding.SHUTTERCONTACT_THING_TYPE, device.getSerialNumber() );
			break;
		case EcoSwitch:
			thingUID = new ThingUID(MaxCubeBinding.ECOSWITCH_THING_TYPE, device.getSerialNumber() );
			break;
		default:
			break;
		}
		if(thingUID!=null) {
			ThingUID bridgeUID = maxCubeBridgeHandler.getThing().getUID();
			DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
					.withProperty(MaxCubeBinding.SERIAL_NUMBER, device.getSerialNumber())
					.withBridge(bridgeUID)
					.withLabel( device.getType() + ": " + device.getName() + " (" + device.getSerialNumber() +")")
					.build();
			thingDiscovered(discoveryResult);
		} else {
			logger.debug("Discovered MAX! device is unsupported: type '{}' with id '{}'", device.getType(), device.getSerialNumber());
		}
	}

	@Override
	protected void startScan() {
		//this can be ignored here as we discover via the bridge
	}

	@Override
	public void onDeviceStateChanged(ThingUID bridge, Device device) {
		//this can be ignored here
	}

	@Override
	public void onDeviceRemoved(MaxCube bridge, Device device) {
		//this can be ignored here
	}
}
