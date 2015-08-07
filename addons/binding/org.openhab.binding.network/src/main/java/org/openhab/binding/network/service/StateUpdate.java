/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.network.service;

/**
 * Callback for the automatic Refresh
 * @author Marc Mettke - Initial contribution
 */
public interface StateUpdate {
	public void newState(boolean state);
    public void invalidConfig();
}