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
package org.openhab.binding.domintell.internal.protocol.model.module;

import org.openhab.binding.domintell.internal.protocol.model.SerialNumber;
import org.openhab.binding.domintell.internal.protocol.model.type.ModuleType;

import java.util.Objects;

/**
* The {@link ModuleKey} class is a module ID class
*
* @author Gabor Bicskei - Initial contribution
*/
public class ModuleKey {
    /**
     * Module type
     */
    private ModuleType moduleType;

    /**
     * Module serial number
     */
    private SerialNumber serialNumber;

    public ModuleKey(ModuleType moduleType, SerialNumber serialNumber) {
        this.moduleType = moduleType;
        this.serialNumber = serialNumber;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    public SerialNumber getSerialNumber() {
        return serialNumber;
    }

    public String getId() {
        return moduleType + "-" + serialNumber.getAddressInt();
    }

    public String toLabel() {
        return moduleType + " " + serialNumber.toLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleKey moduleKey = (ModuleKey) o;
        return moduleType == moduleKey.moduleType &&
                Objects.equals(serialNumber, moduleKey.serialNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleType, serialNumber);
    }

    @Override
    public String toString() {
        return "ModuleKey{" +
                "moduleType=" + moduleType +
                ", serialNumber=" + serialNumber.toLabel() +
                '}';
    }
}
