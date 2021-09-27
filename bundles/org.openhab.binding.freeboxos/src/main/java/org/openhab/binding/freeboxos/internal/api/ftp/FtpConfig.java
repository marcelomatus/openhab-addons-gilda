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
package org.openhab.binding.freeboxos.internal.api.ftp;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.Response;

/**
 * The {@link FtpConfig} is the Java class used to map the "FtpConfig"
 * structure used by the FTP configuration API
 * https://dev.freebox.fr/sdk/os/ftp/#
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class FtpConfig {
    // Response classes
    public static class FtpConfigResponse extends Response<FtpConfig> {
    }

    protected boolean enabled;
    protected boolean allowAnonymous;
    protected boolean allowAnonymousWrite;
    protected String password = "";
    protected boolean allowRemoteAccess;
    protected boolean weakPassword;
    protected int portCtrl;
    protected int portData;
    protected String remoteDomain = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
