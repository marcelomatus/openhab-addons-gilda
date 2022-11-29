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
package org.openhab.binding.dbquery.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dbquery.internal.config.JdbcBridgeConfiguration;
import org.openhab.binding.dbquery.internal.dbimpl.jdbc.JdbcClientFacadeImpl;
import org.openhab.binding.dbquery.internal.dbimpl.jdbc.JdbcDatabase;
import org.openhab.binding.dbquery.internal.domain.Database;
import org.openhab.core.thing.Bridge;

/**
 * Concrete implementation of {@link DatabaseBridgeHandler} for Jdbc
 *
 * @author Joan Pujol - Initial contribution
 */
@NonNullByDefault
public class JDBCBridgeHandler extends DatabaseBridgeHandler {
    private JdbcBridgeConfiguration config = new JdbcBridgeConfiguration();

    public JDBCBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    Database createDatabase() {
        return new JdbcDatabase(new JdbcClientFacadeImpl(config));
    }

    @Override
    protected void initConfig() {
        config = getConfig().as(JdbcBridgeConfiguration.class);
    }
}
