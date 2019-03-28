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
package org.openhab.binding.domintell.internal.protocol.model;

/**
* The {@link Description} class is handles information received from Domintell system
*
* @author Gabor Bicskei - Initial contribution
*/
public class Description {
    /**
     * Item/module label in domintell system
     */
    private String name;

    /**
     * Item/module location in domintell system
     */
    private String location;

    /**
     * Item/module extra information
     */
    private String extra;

    public Description(String name, String location, String extra) {
        this.name = name;
        this.location = location;
        this.extra = extra;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getExtra() {
        return extra;
    }

    public static Description parseInfo(String info) {
        int nameEnd = info.indexOf('[');
        String name = info.substring(0, nameEnd);
        int locationEnd = info.indexOf(']', nameEnd);
        String location = info.substring(nameEnd + 1, locationEnd);
        int extraStart = info.indexOf('[', locationEnd);
        String extra = extraStart != -1 ? info.substring(extraStart + 1, info.length() - 1) : null;
        return new Description(name, location, extra);
    }

    @Override
    public String toString() {
        return "ItemInfo{" +
                "name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", extra='" + extra + '\'' +
                '}';
    }
}
