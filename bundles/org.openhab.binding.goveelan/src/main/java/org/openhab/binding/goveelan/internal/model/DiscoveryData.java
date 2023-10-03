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
 *
 */
package org.openhab.binding.goveelan.internal.model;

/**
 * Govee Message - Device information
 *
 * @param ip IP Address of the device
 * @param device Mac Address
 * @param sku artice number
 * @param bleVersionHard Bluetooth HW version
 * @param bleVersionSoft Bluetooth SW version
 * @param wifiVersionHard Wifi HW version
 * @param wifiVersionSoft Wife SW version
 *
 * @author Stefan Höhn - Initial contribution
 */
public record DiscoveryData(String ip, String device, String sku, String bleVersionHard, String bleVersionSoft,
        String wifiVersionHard, String wifiVersionSoft) {
}
