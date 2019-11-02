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
package org.openhab.binding.ism8.server;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link DataPointChangedEvent} is an event container for data point changes
 *
 * @author Hans-Reiner Hoffmann - Initial contribution
 */
@NonNullByDefault
public class DataPointChangedEvent {
    protected IDataPoint dataPoint;

    public DataPointChangedEvent(Object source, IDataPoint dataPoint) {
        this.dataPoint = dataPoint;
    }

    public IDataPoint getDataPoint() {
        return dataPoint;
    }
}
