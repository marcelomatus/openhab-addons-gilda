/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.smarther.internal.api.dto;

/**
 * The {@code Program} class defines the dto for Smarther API program object.
 *
 * @author Fabio Possieri - Initial contribution
 */
public class Program {

    private static final String DEFAULT_PROGRAM = "Default";

    private int number;
    private String name;

    public int getNumber() {
        return number;
    }

    public String getName() {
        return (number == 0) ? DEFAULT_PROGRAM : name;
    }

    @Override
    public String toString() {
        return String.format("number=%d, name=%s", number, name);
    }

}
