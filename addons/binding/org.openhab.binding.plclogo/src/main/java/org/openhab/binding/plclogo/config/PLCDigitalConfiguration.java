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

/**
 * The {@link PLCDigitalConfiguration} is a base class for configuration
 * of Siemens LOGO! PLC digital input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCDigitalConfiguration extends PLCCommonConfiguration {

    private String kind;

    @Override
    public @NonNull String getBlockKind() {
        return kind;
    }

    /**
     * Set Siemens LOGO! blocks kind.
     * Can be I, Q, M, NI or NQ for digital blocks and
     * AI, AM, AQ, NAI or NAQ for analog
     *
     * @param kind Siemens LOGO! blocks kind
     */
    public void setBlockKind(final @NonNull String kind) {
        Objects.requireNonNull(kind, "PLCDigitalConfiguration: Block name may not be null.");
        this.kind = kind.trim();
    }

    @Override
    public @NonNull String getChannelType() {
        return (kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("NI")) ? "Contact" : "Switch";
    }

}
