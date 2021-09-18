/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.freeboxos.internal.api.system;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.FreeboxOsSession;
import org.openhab.binding.freeboxos.internal.api.Response;
import org.openhab.binding.freeboxos.internal.api.RestManager;

/**
 * The {@link SystemManager} is the Java class used to handle api requests
 * related to system
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class SystemManager extends RestManager {

    private static final String REBOOT_SUB_PATH = "reboot";
    private static final String SYSTEM_PATH = "system";

    public SystemManager(FreeboxOsSession session) {
        super(SYSTEM_PATH, session);
    }

    public SystemConf getConfig() throws FreeboxException {
        return get(null, SystemConfigurationResponse.class, true);
    }

    public void reboot() throws FreeboxException {
        post(REBOOT_SUB_PATH);
    }

    // Response classes and validity evaluations
    private static class SystemConfigurationResponse extends Response<SystemConf> {
    }
}
