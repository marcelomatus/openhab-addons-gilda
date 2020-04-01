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
import org.openhab.persistence.influxdb.internal.InfluxDBConfiguration;
import org.openhab.persistence.influxdb.internal.InfluxDBRepository;
import org.openhab.persistence.influxdb.internal.UnnexpectedConditionException;
import org.openhab.persistence.influxdb.internal.influx1.Influx1FilterCriteriaQueryCreatorImpl;
import org.openhab.persistence.influxdb.internal.influx1.InfluxDB1RepositoryImpl;
import org.openhab.persistence.influxdb.internal.influx2.Influx2FilterCriteriaQueryCreatorImpl;
import org.openhab.persistence.influxdb.internal.influx2.InfluxDB2RepositoryImpl;

/**
 * Factory that returns {@link InfluxDBRepository} and {@link FilterCriteriaQueryCreator} implementations
 * depending on InfluxDB version
 *
 * @author Joan Pujol Espinar - Addon rewrite refactoring code and adding support for InfluxDB 2.0
 */
@NonNullByDefault
public class RepositoryFactory {

    public static InfluxDBRepository createRepository(InfluxDBConfiguration influxDBConfiguration) {
        switch (influxDBConfiguration.getVersion()) {
            case V1:
                return new InfluxDB1RepositoryImpl(influxDBConfiguration);
            case V2:
                return new InfluxDB2RepositoryImpl(influxDBConfiguration);
            default:
                throw new UnnexpectedConditionException("Not expected version " + influxDBConfiguration.getVersion());
        }
    }

    public static FilterCriteriaQueryCreator createQueryCreator(InfluxDBConfiguration influxDBConfiguration) {
        switch (influxDBConfiguration.getVersion()) {
            case V1:
                return new Influx1FilterCriteriaQueryCreatorImpl(influxDBConfiguration);
            case V2:
                return new Influx2FilterCriteriaQueryCreatorImpl();
            default:
                throw new UnnexpectedConditionException("Not expected version " + influxDBConfiguration.getVersion());
        }
    }
}
