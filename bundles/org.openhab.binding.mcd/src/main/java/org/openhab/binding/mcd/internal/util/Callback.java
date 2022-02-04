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
package org.openhab.binding.mcd.internal.util;

import com.google.gson.JsonElement;

/**
 * This interface is used for callback events.
 * 
 * @author Simon Dengler - Initial contribution
 */
public interface Callback {
    void jsonElementTypeCallback(JsonElement jsonObject);
}
