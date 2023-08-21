/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.androidtv.internal.protocol.philipstv.service.model.application;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of {@link LaunchAppDto} and {@link LaunchAppDto}
 *
 * @author Benjamin Meyer - Initial contribution
 * @author Ben Rosenblum - Merged into AndroidTV
 */
@NonNullByDefault
public class IntentDto {

    @JsonProperty("component")
    private ComponentDto component;

    @JsonProperty("action")
    private String action = "";

    @JsonProperty("extras")
    private ExtrasDto extras;

    public IntentDto(ComponentDto component, ExtrasDto extras) {
        this.component = component;
        this.extras = extras;
    }

    public void setComponent(ComponentDto component) {
        this.component = component;
    }

    public ComponentDto getComponent() {
        return component;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setExtras(ExtrasDto extras) {
        this.extras = extras;
    }

    public ExtrasDto getExtras() {
        return extras;
    }

    @Override
    public String toString() {
        return "Intent{" + "component = '" + component + '\'' + ",action = '" + action + '\'' + ",extras = '" + extras
                + '\'' + "}";
    }
}
