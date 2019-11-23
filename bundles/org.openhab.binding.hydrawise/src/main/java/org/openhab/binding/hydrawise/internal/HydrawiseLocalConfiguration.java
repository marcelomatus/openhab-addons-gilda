/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.hydrawise.internal;

/**
 * The {@link HydrawiseLocalConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Dan Cunningham - Initial contribution
 */
public class HydrawiseLocalConfiguration {

    /**
     * Host or IP for local controller
     */
    public String host;
    /**
     * User name (admin) for local controller
     */
    public String username;
    /**
     * Password for local controller
     */
    public String password;

    /**
     * refresh interval in seconds.
     */
    public int refresh;
}
