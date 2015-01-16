/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.max.internal.message;


/**
* Base class for configuration provided by the MAX!Cube C_Message. 
* 
* @author Andreas Heil (info@aheil.de)
* @since 1.4.0
*/
public final class DeviceConfiguration {
	
	private DeviceType deviceType = null;
	private String rfAddress = null;
	private String serialNumber = null;
	private String name = null;
	private int roomId = -1;
	
	private DeviceConfiguration() {
	}
	
	public static DeviceConfiguration create(Message message) {	
		DeviceConfiguration configuration = new DeviceConfiguration();
		configuration.setValues((C_Message) message);
		
		return configuration;
	}
	
	public static DeviceConfiguration create(DeviceInformation di) {
		DeviceConfiguration configuration = new DeviceConfiguration();
		configuration.setValues(di.getRFAddress(), di.getDeviceType(), di.getSerialNumber(), di.getName());
		return configuration;
	}
	

	public void setValues(C_Message message) {
		setValues(message.getRFAddress(), message.getDeviceType(), message.getSerialNumber());
	}
	
	private void setValues(String rfAddress, DeviceType deviceType, String serialNumber, String name) {
		setValues(rfAddress, deviceType, serialNumber);
		this.name = name;
	}
	
	private void setValues(String rfAddress, DeviceType deviceType, String serialNumber) {
		this.rfAddress = rfAddress;
		this.deviceType = deviceType;
		this.serialNumber = serialNumber;
	}
	
	public String getRFAddress() {
		return rfAddress;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public String getSerialNumber() {
		return serialNumber;
	}
	
	public String getName() {
		return name;
	}

	public int getRoomId() {
		return roomId;
	}
	
	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}	
}
