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
package org.openhab.binding.hue.internal.console;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.openhab.binding.hue.internal.handler.HueBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link HueCommandExtension} is responsible for handling console commands
 *
 * @author Laurent Garnier - Initial contribution
 */

@NonNullByDefault
@Component(service = ConsoleCommandExtension.class)
public class HueCommandExtension extends AbstractConsoleCommandExtension {

    private static final String USER_NAME = "username";

    private final ThingRegistry thingRegistry;

    @Activate
    public HueCommandExtension(final @Reference ThingRegistry thingRegistry) {
        super("hue", "Interact with the hue binding.");
        this.thingRegistry = thingRegistry;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length == 2) {
            HueBridgeHandler bridgeHandler = null;
            try {
                ThingUID thingUID = new ThingUID(args[0]);
                Thing thing = thingRegistry.get(thingUID);
                if (thing != null) {
                    ThingHandler thingHandler = thing.getHandler();
                    if (thingHandler instanceof HueBridgeHandler) {
                        bridgeHandler = (HueBridgeHandler) thingHandler;
                    }
                }
            } catch (IllegalArgumentException e) {
                bridgeHandler = null;
            }
            if (bridgeHandler == null) {
                console.println("Bad bridge id '" + args[0] + "'");
                printUsage(console);
            } else {
                switch (args[1]) {
                    case USER_NAME:
                        String userName = bridgeHandler.getUserName();
                        console.println("Your user name is " + (userName != null ? userName : "undefined"));
                        break;
                    default:
                        printUsage(console);
                        break;
                }
            }
        } else {
            printUsage(console);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(buildCommandUsage("<bridgeUID> " + USER_NAME, "show the user name"));
    }
}
