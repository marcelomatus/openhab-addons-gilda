/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.internal.config;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.DATE_TIME_ITEM;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link PLCDateTimeConfiguration} holds configuration of Siemens LOGO! PLC
 * analog input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
@NonNullByDefault
public class PLCDateTimeConfiguration extends PLCCommonConfiguration {

    private String block;
    private String type;

    /**
     * Get configured Siemens LOGO! block name.
     *
     * @return Configured Siemens LOGO! block name
     */
    public String getBlockName() {
        return block;
    }

    /**
     * Set Siemens LOGO! block name.
     *
     * @param name Siemens LOGO! block name
     */
    public void setBlockName(final String name) {
        this.block = name.trim();
    }

    /**
     * Get configured Siemens LOGO! block name.
     *
     * @return Configured Siemens LOGO! block name
     */
    public String getBlockType() {
        return type;
    }

    /**
     * Set Siemens LOGO! block name.
     *
     * @param name Siemens LOGO! output block name
     */
    public void setBlockType(final String type) {
        this.type = type.trim();
    }

    @Override
    public String getChannelType() {
        return DATE_TIME_ITEM;
    }

    @Override
    public String getBlockKind() {
        return block.substring(0, 2);
    }

}
