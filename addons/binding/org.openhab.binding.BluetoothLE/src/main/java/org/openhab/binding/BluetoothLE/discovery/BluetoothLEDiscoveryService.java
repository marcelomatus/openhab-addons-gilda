/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.BluetoothLE.discovery;

import org.openhab.binding.BluetoothLE.protocol.ScanResult;

public interface BluetoothLEDiscoveryService {
	
	public ScanResult getScanResult(String device);

}
