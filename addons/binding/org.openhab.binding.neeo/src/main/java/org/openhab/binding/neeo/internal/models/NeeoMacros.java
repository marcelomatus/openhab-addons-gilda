/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.neeo.internal.models;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The model representing Neeo Macros (serialize/deserialize json use only).
 *
 * @author Tim Roberts - Initial contribution
 */
public class NeeoMacros {

    /** The macros. */
    private final NeeoMacro @Nullable [] macros;

    /**
     * Instantiates a new neeo macros.
     *
     * @param macros the macros
     */
    public NeeoMacros(NeeoMacro[] macros) {
        Objects.requireNonNull(macros, "macros cannot be null");
        this.macros = macros;
    }

    /**
     * Gets the macros.
     *
     * @return the macros
     */
    public NeeoMacro[] getMacros() {
        final NeeoMacro @Nullable [] localMacros = macros;
        return localMacros == null ? new NeeoMacro[0] : localMacros;
    }

    /**
     * Gets the macro.
     *
     * @param key the key
     * @return the macro
     */
    @Nullable
    public NeeoMacro getMacro(String key) {
        for (NeeoMacro macro : getMacros()) {
            if (StringUtils.equalsIgnoreCase(key, macro.getKey())) {
                return macro;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "NeeoMacro [macros=" + Arrays.toString(macros) + "]";
    }
}
