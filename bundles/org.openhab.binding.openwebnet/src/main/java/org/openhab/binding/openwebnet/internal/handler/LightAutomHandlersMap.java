/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.openwebnet.internal.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openwebnet4j.message.WhereLightAutom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A map to store handlers for lights and automations. The map is organised by
 * Area.
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
public class LightAutomHandlersMap {

    private final Logger logger = LoggerFactory.getLogger(LightAutomHandlersMap.class);

    private Map<Integer, Map<String, OpenWebNetThingHandler>> hndlrsMap;
    private @Nullable OpenWebNetThingHandler oneHandler = null;

    protected LightAutomHandlersMap() {
        hndlrsMap = new ConcurrentHashMap<>();
    }

    protected void add(int area, OpenWebNetThingHandler handler) {
        if (!hndlrsMap.containsKey(area)) {
            hndlrsMap.put(area, new ConcurrentHashMap<>());
        }
        Map<String, OpenWebNetThingHandler> areaHndlrs = hndlrsMap.get(Integer.valueOf(area));
        final String handlerOwnId = handler.ownId;
        if (areaHndlrs != null && handlerOwnId != null) {
            areaHndlrs.put(handlerOwnId, handler);
            if (oneHandler == null) {
                oneHandler = handler;
            }
            logger.debug("Added handler {} to Area {}", handlerOwnId, area);
            logger.debug("Map: {}", this.toString());
        }
    }

    protected void remove(int area, OpenWebNetThingHandler handler) {
        if (hndlrsMap.containsKey(area)) {
            Map<String, OpenWebNetThingHandler> areaHndlrs = hndlrsMap.get(Integer.valueOf(area));
            if (areaHndlrs != null) {
                boolean removed = areaHndlrs.remove(handler.ownId, handler);
                OpenWebNetThingHandler oneHandler = this.oneHandler;
                // if the removed handler was linked by oneHandler, find another one
                if (removed && oneHandler != null && oneHandler.equals(handler)) {
                    this.oneHandler = getFirst();
                }
                logger.debug("Removed handler {} from Area {}", handler.ownId,
                        area);
                logger.debug("Map: {}", this.toString());
            }
        }
    }

    protected @Nullable List<OpenWebNetThingHandler> getAreaHandlers(int area) {
        Map<String, OpenWebNetThingHandler> areaHndlrs = hndlrsMap.get(area);
        if (areaHndlrs != null) {
            List<OpenWebNetThingHandler> list = new ArrayList<OpenWebNetThingHandler>(areaHndlrs.values());
            return list;
        } else {
            return null;
        }
    }

    protected @Nullable List<OpenWebNetThingHandler> getAllHandlers() {
        List<OpenWebNetThingHandler> list = new ArrayList<OpenWebNetThingHandler>();
        for (Map.Entry<Integer, Map<String, OpenWebNetThingHandler>> entry : hndlrsMap.entrySet()) {
            Map<String, OpenWebNetThingHandler> innerMap = entry.getValue();
            for (Map.Entry<String, OpenWebNetThingHandler> innerEntry : innerMap.entrySet()) {
                OpenWebNetThingHandler hndlr = innerEntry.getValue();
                if (hndlr != null) {
                    list.add(hndlr);
                }
            }
        }
        return list;
    }

    protected boolean isEmpty() {
        return oneHandler == null;
    }

    protected @Nullable OpenWebNetThingHandler getOne() {
        if (oneHandler == null) {
            oneHandler = getFirst();
        }
        return oneHandler;
    }

    private @Nullable OpenWebNetThingHandler getFirst() {
        for (Map.Entry<Integer, Map<String, OpenWebNetThingHandler>> entry : hndlrsMap.entrySet()) {
            Map<String, OpenWebNetThingHandler> innerMap = entry.getValue();
            for (Map.Entry<String, OpenWebNetThingHandler> innerEntry : innerMap.entrySet()) {
                OpenWebNetThingHandler thingHandler = innerEntry.getValue();
                if (thingHandler != null) {
                    WhereLightAutom whereLightAutom = (WhereLightAutom) thingHandler.deviceWhere;
                    if (whereLightAutom != null && whereLightAutom.isAPL()) {
                        return thingHandler;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String log = "\n---- LightAutomHandlersMap ----";
        for (Map.Entry<Integer, Map<String, OpenWebNetThingHandler>> entry : hndlrsMap.entrySet()) {
            log += "\n- Area: " + entry.getKey() + "\n   -";
            Map<String, OpenWebNetThingHandler> innerMap = entry.getValue();
            for (Map.Entry<String, OpenWebNetThingHandler> innerEntry : innerMap.entrySet()) {
                OpenWebNetThingHandler thingHandler = innerEntry.getValue();
                if (thingHandler != null) {
                    log += " " + thingHandler.ownId;
                }
            }
        }
        log += "\n# getAllHandlers: ";
        List<OpenWebNetThingHandler> allHandlers = getAllHandlers();
        if (allHandlers != null) {
            for (OpenWebNetThingHandler singleHandler : allHandlers) {
                log += " " + singleHandler.ownId;
            }
        }
        OpenWebNetThingHandler one = this.getOne();
        log += "\n# getOne() = " + (one == null ? "null" : one.ownId);
        log += "\n-------------------------------";

        return log;
    }
}
