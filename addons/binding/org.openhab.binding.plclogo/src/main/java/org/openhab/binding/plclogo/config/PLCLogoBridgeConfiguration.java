/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.config;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.plclogo.PLCLogoBindingConstants;

/**
 * The {@link PLCLogoBridgeConfiguration} hold configuration of Siemens LOGO! PLCs.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoBridgeConfiguration {

    private String address;
    private String family;
    private @NonNull String localTSAP = "0x3000";
    private @NonNull String remoteTSAP = "0x2000";
    private @NonNull Integer refresh = 100;

    /**
     * Get configured Siemens LOGO! device IP address.
     *
     * @return Configured Siemens LOGO! IP address
     */
    public @NonNull String getAddress() {
        return address;
    }

    /**
     * Set IP address for Siemens LOGO! device.
     *
     * @param address IP address of Siemens LOGO! device
     */
    public void setAddress(final @NonNull String address) {
        Objects.requireNonNull(address, "IP may not be null");
        this.address = address.trim();
    }

    /**
     * Get configured Siemens LOGO! device family.
     *
     * @see PLCLogoBindingConstants#LOGO_0BA7
     * @see PLCLogoBindingConstants#LOGO_0BA8
     * @return Configured Siemens LOGO! device family
     */
    public @NonNull String getFamily() {
        return family;
    }

    /**
     * Set Siemens LOGO! device family.
     *
     * @param family Family of Siemens LOGO! device
     * @see PLCLogoBindingConstants#LOGO_0BA7
     * @see PLCLogoBindingConstants#LOGO_0BA8
     */
    public void setFamily(final @NonNull String family) {
        Objects.requireNonNull(family, "Family may not be null");
        this.family = family.trim();
    }

    /**
     * Get configured local TSAP of Siemens LOGO! device.
     *
     * @return Configured local TSAP of Siemens LOGO!
     */
    public @Nullable Integer getLocalTSAP() {
        Integer result = null;
        if (localTSAP.startsWith("0x")) {
            result = Integer.decode(localTSAP);
        }
        return result;
    }

    /**
     * Set local TSAP of Siemens LOGO! device.
     *
     * @param tsap Local TSAP of Siemens LOGO! device
     */
    public void setLocalTSAP(final @NonNull String tsap) {
        Objects.requireNonNull(tsap, "LocalTSAP may not be null");
        this.localTSAP = tsap.trim();
    }

    /**
     * Get configured remote TSAP of Siemens LOGO! device.
     *
     * @return Configured local TSAP of Siemens LOGO!
     */
    public @Nullable Integer getRemoteTSAP() {
        Integer result = null;
        if (remoteTSAP.startsWith("0x")) {
            result = Integer.decode(remoteTSAP);
        }
        return result;
    }

    /**
     * Set remote TSAP of Siemens LOGO! device.
     *
     * @param tsap Remote TSAP of Siemens LOGO! device
     */
    public void setRemoteTSAP(final @NonNull String tsap) {
        Objects.requireNonNull(tsap, "RemoteTSAP may not be null");
        this.remoteTSAP = tsap.trim();
    }

    /**
     * Get configured refresh rate of Siemens LOGO! device blocks in milliseconds.
     *
     * @return Configured refresh rate of Siemens LOGO! device blocks
     */
    public @NonNull Integer getRefreshRate() {
        return refresh;
    }

    /**
     * Set refresh rate of Siemens LOGO! device blocks in milliseconds.
     *
     * @param rate Refresh rate of Siemens LOGO! device blocks
     */
    public void setRefreshRate(final @NonNull Integer rate) {
        Objects.requireNonNull(rate, "Refresh rate may not be null");
        this.refresh = rate;
    }

}
