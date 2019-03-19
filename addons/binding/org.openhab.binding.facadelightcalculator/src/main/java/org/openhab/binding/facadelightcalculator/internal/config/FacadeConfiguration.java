/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.facadelightcalculator.internal.config;

/**
 * The {@link FacadeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class FacadeConfiguration {
    private Integer orientation;
    private Integer noffset;
    private Integer poffset;
    private Integer margin;

    /**
     * Returns the orientation.
     */
    public Integer getOrientation() {
        return orientation;
    }

    /**
     * Returns the Negative Offset.
     */
    public Integer getNegativeOffset() {
        return noffset;
    }

    /**
     * Returns the Positive Offset.
     */
    public Integer getPositiveOffset() {
        return poffset;
    }

    /**
     * Returns the Margin.
     */
    public Integer getMargin() {
        return margin;
    }
}
