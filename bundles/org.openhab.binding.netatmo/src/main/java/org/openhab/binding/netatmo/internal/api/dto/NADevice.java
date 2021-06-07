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
package org.openhab.binding.netatmo.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.netatmo.internal.api.ModuleType;
import org.openhab.binding.netatmo.internal.deserialization.NADynamicObjectMap;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */
@NonNullByDefault
public class NADevice extends NAThing {
    private NADynamicObjectMap modules = new NADynamicObjectMap();
    private boolean co2Calibrating;
    private long dateSetup;
    private long lastUpgrade;
    private @Nullable NAPlace place;

    public NADynamicObjectMap getModules() {
        return modules;
    }

    // TODO : faut-il garder ce setter ?
    public void setModules(NADynamicObjectMap modules) {
        this.modules = modules;
    }

    public @Nullable NAThing getModule(String key) {
        return modules.get(key);
    }

    public long getDateSetup() {
        return dateSetup;
    }

    public long getLastUpgrade() {
        return lastUpgrade;
    }

    public @Nullable NAPlace getPlace() {
        return place;
    }

    public void setPlace(@Nullable NAPlace place) {
        this.place = place;
    }

    public boolean isCo2Calibrating() {
        return co2Calibrating;
    }

    public boolean containsModuleType(ModuleType searchedType) {
        return modules.values().stream().anyMatch(module -> module.getType() == searchedType);
    }
}
