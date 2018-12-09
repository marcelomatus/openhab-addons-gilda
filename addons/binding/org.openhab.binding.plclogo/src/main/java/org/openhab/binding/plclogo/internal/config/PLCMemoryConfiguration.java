/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.internal.config;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link PLCMemoryConfiguration} is a class for configuration
 * of Siemens LOGO! PLC memory input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
@NonNullByDefault
public class PLCMemoryConfiguration extends PLCCommonConfiguration {

    private String block = "";
    private Integer threshold = 0;

    /**
     * Get configured Siemens LOGO! memory block name.
     *
     * @return Configured Siemens LOGO! memory block name
     */
    public String getBlockName() {
        return block;
    }

    /**
     * Set Siemens LOGO! memory block name.
     *
     * @param name Siemens LOGO! memory block name
     */
    public void setBlockName(final String name) {
        this.block = name.trim();
    }

    /**
     * Get Siemens LOGO! blocks update threshold.
     *
     * @return Configured Siemens LOGO! update threshold
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * Set Siemens LOGO! blocks update threshold.
     *
     * @param force Force update of Siemens LOGO! blocks
     */
    public void setThreshold(final Integer threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getChannelType() {
        final String kind = getBlockKind();
        return kind.equalsIgnoreCase("VB") && block.contains(".") ? DIGITAL_OUTPUT_ITEM : ANALOG_ITEM;
    }

    @Override
    public String getBlockKind() {
        return getBlockKind(block);
    }

    protected static String getBlockKind(final String name) {
        String kind = "Unknown";
        if (Character.isDigit(name.charAt(1))) {
            kind = name.substring(0, 1);
        } else if (Character.isDigit(name.charAt(2))) {
            kind = name.substring(0, 2);
        } else if (Character.isDigit(name.charAt(3))) {
            kind = name.substring(0, 3);
        }
        return kind;
    }
}
