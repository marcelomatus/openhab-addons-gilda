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

package org.openhab.binding.qbus.internal.protocol;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.qbus.internal.handler.QbusCO2Handler;

/**
 * The {@link QbusCO2} class represents the action Qbus CO2 output.
 *
 * @author Koen Schockaert - Initial Contribution
 */

@NonNullByDefault
public final class QbusCO2 {

    @Nullable
    private Integer state;

    @Nullable
    private QbusCO2Handler thingHandler;

    /**
     * This method should be called if the ThingHandler for the thing corresponding to this CO2 is initialized.
     * It keeps a record of the thing handler in this object so the thing can be updated when
     * the CO2 output receives an update from the Qbus IP-interface.
     *
     * @param handler
     */
    public void setThingHandler(QbusCO2Handler handler) {
        this.thingHandler = handler;
    }

    /**
     * Get state of CO2.
     *
     * @return CO2 state
     */
    public @Nullable Integer getState() {
        if (this.state != null) {
            return this.state;
        } else {
            return null;
        }
    }

    /**
     * Sets state of CO2.
     *
     * @param CO2 state
     */
    public void setState(Integer co2) {
        this.state = co2;
        QbusCO2Handler handler = thingHandler;
        if (handler != null) {
            handler.handleStateUpdate(this);
        }
    }
}
