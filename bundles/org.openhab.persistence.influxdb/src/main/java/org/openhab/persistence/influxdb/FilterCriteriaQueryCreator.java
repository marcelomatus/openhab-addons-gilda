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
package org.openhab.persistence.influxdb;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.persistence.FilterCriteria;

/**
 * Creates InfluxDB query sentence given a OpenHab persistence {@link FilterCriteria}
 *
 * @author Joan Pujol Espinar - Addon rewrite refactoring code and adding support for InfluxDB 2.0
 */
@NonNullByDefault
public interface FilterCriteriaQueryCreator {
    String createQuery(FilterCriteria criteria, String bucket);
}
