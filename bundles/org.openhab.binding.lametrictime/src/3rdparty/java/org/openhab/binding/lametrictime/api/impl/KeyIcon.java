/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.lametrictime.api.impl;

import org.openhab.binding.lametrictime.api.model.Icon;

public class KeyIcon implements Icon
{
    private final String key;

    public KeyIcon(String key)
    {
        this.key = key;
    }

    @Override
    public String toRaw()
    {
        return key;
    }
}
