/**
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
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
package org.openhab.binding.freeboxos.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.handler.HostHandler;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {HostActions} class is responsible to call corresponding actions on a given lan host
 *
 * @author Gaël L'hopital - Initial contribution
 */
@ThingActionsScope(name = "freeboxos")
@NonNullByDefault
public class HostActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(HostActions.class);
    private @Nullable HostHandler handler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof HostHandler hostHandler) {
            this.handler = hostHandler;
<<<<<<< Upstream, based on origin/main
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.handler;
    }

    @RuleAction(label = "wol host", description = "Awakes a lan host")
    public void wol() {
        logger.debug("Host WOL called");
        HostHandler hostHandler = this.handler;
        if (hostHandler != null) {
            hostHandler.wol();
        } else {
            logger.warn("LanHost Action service ThingHandler is null");
=======
 * Copyright (c) 2010-2022 Contributors to the openHAB project
=======
 * Copyright (c) 2010-2023 Contributors to the openHAB project
>>>>>>> 006a813 Saving work before instroduction of ArrayListDeserializer
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
package org.openhab.binding.freeboxos.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.handler.HostHandler;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {HostActions} class is responsible to call corresponding actions on a given lan host
 *
 * @author Gaël L'hopital - Initial contribution
 */
@ThingActionsScope(name = "freeboxos")
@NonNullByDefault
public class HostActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(HostActions.class);
    private @Nullable HostHandler handler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof HostHandler) {
            this.handler = (HostHandler) handler;
=======
>>>>>>> 9aef877 Rebooting Home Node part
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.handler;
    }

    @RuleAction(label = "wol host", description = "Awakes a lan host")
    public void wol() {
        logger.debug("Host WOL called");
        HostHandler hostHandler = this.handler;
        if (hostHandler != null) {
            hostHandler.wol();
        } else {
<<<<<<< Upstream, based on origin/main
            logger.warn("LanHost Action service ThingHandler is null!");
>>>>>>> 46dadb1 SAT warnings handling
=======
            logger.warn("LanHost Action service ThingHandler is null");
>>>>>>> 089708c Switching to addons.xml, headers updated
        }
    }
}
