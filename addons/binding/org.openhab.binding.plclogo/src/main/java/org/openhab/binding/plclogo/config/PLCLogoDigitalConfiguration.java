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
 * The {@link PLCLogoDigitalConfiguration} holds configuration of Siemens LOGO! PLC
 * digital input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoDigitalConfiguration extends PLCLogoBlockConfiguration {

    @Override
    public boolean isBlockValid(final @NonNull String name) {
        Objects.requireNonNull(name, "PLCLogoDigitalConfiguration: Block name may not be null.");

        boolean valid = false;
        if (name.length() >= 2) {
            valid = valid || name.startsWith("I") || name.startsWith("NI"); // Inputs
            valid = valid || name.startsWith("Q") || name.startsWith("NQ"); // Outputs
            valid = valid || name.startsWith("M"); // Markers
            if (!valid && name.startsWith("VB")) { // Memory block
                final String[] parts = name.split("\\.");
                if (parts.length == 2) {
                    final int bit = Integer.parseInt(parts[1]);
                    valid = (0 <= bit) && (bit <= 7);
                    if (valid && Character.isDigit(parts[0].charAt(2))) {
                        final int address = Integer.parseInt(parts[0].substring(2));
                        valid = (0 <= address) && (address <= 850);
                    }
                }
            }
        }
        return valid;
    }

    @Override
    public boolean isInputBlock(final @NonNull String name) {
        final String kind = getBlockKind(name);
        return kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("NI");
    }

    @Override
    public @NonNull String getItemType() {
        return getItemType(getBlockName());
    }

    /**
     * Return supported item type for given block.
     *
     * @param name Name of the LOGO! block
     * @return Supported item type
     */
    protected @NonNull String getItemType(final @NonNull String name) {
        return isInputBlock(name) ? "Contact" : "Switch";
    }

}
