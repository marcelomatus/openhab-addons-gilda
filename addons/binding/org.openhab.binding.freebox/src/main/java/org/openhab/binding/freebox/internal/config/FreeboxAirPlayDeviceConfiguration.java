/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.freebox.internal.config;

/**
 * The {@link FreeboxAirPlayDeviceConfiguration} is responsible for holding
 * configuration informations associated to a Freebox AirPlay Device thing type
 *
 * @author Laurent Garnier - Initial contribution
 */
public class FreeboxAirPlayDeviceConfiguration {

    public static final String NAME = "name";
    public static final String PASSWORD = "password";
    public static final String ACCEPT_ALL_MP3 = "acceptAllMp3";

    public String name;
    public String password;
    public Boolean acceptAllMp3;

}
