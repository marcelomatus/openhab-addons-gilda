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
package org.openhab.binding.nuvo.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.core.thing.binding.ThingActionsScope;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.nuvo.internal.handler.NuvoHandler;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some automation actions to be used with a {@link NuvoThingActions}
 *
 * @author Michael Lobstein - initial contribution
 *
 */
@ThingActionsScope(name = "nuvo")
@NonNullByDefault
public class NuvoThingActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(NuvoThingActions.class);

    private @Nullable NuvoHandler handler;

    @RuleAction(label = "sendNuvoCommand", description = "Action that sends raw command to the amplifer")
    public void sendNuvoCommand(@ActionInput(name = "sendNuvoCommand") String rawCommand) {
        NuvoHandler localHandler = handler;
        if (localHandler != null) {
            localHandler.handleRawCommand(rawCommand);
            logger.debug("sendNuvoCommand called with raw command: {}", rawCommand);
        } else {
            logger.warn("unable to send command, NuvoHandler was null");
        }
    }

    public static void sendRawCommand(@Nullable ThingActions actions, String rawCommand)
            throws IllegalArgumentException {
        invokeMethodOf(actions).sendNuvoCommand(rawCommand);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        this.handler = (NuvoHandler) handler;
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.handler;
    }

    private static NuvoThingActions invokeMethodOf(@Nullable ThingActions actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions cannot be null");
        }
        if (actions.getClass().getName().equals(NuvoThingActions.class.getName())) {
            if (actions instanceof NuvoThingActions) {
                return (NuvoThingActions) actions;
            } else {
                return (NuvoThingActions) Proxy.newProxyInstance(NuvoThingActions.class.getClassLoader(),
                        new Class[] { NuvoThingActions.class }, (Object proxy, Method method, Object[] args) -> {
                            Method m = actions.getClass().getDeclaredMethod(method.getName(),
                                    method.getParameterTypes());
                            return m.invoke(actions, args);
                        });
            }
        }
        throw new IllegalArgumentException("Actions is not an instance of NuvoThingActions");
    }
}
