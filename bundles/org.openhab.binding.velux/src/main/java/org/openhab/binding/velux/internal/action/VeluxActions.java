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
package org.openhab.binding.velux.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.velux.internal.handler.VeluxBridgeHandler;
import org.openhab.binding.velux.internal.things.VeluxProduct.ProductBridgeIndex;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VeluxActions} implementation of the rule action for rebooting the bridge
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@ThingActionsScope(name = "velux")
@NonNullByDefault
public class VeluxActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(VeluxActions.class);

    private @Nullable VeluxBridgeHandler bridgeHandler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof VeluxBridgeHandler) {
            this.bridgeHandler = (VeluxBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.bridgeHandler;
    }

    @RuleAction(label = "@text/action.reboot.label", description = "@text/action.reboot.descr")
    public @ActionOutput(name = "running", type = "java.lang.Boolean", label = "@text/action.run.label", description = "@text/action.run.descr") Boolean rebootBridge()
            throws IllegalStateException {
        logger.trace("rebootBridge(): action called");
        VeluxBridgeHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            throw new IllegalStateException("Bridge instance is null");
        }
        return bridgeHandler.rebootBridge();
    }

    @RuleAction(label = "@text/action.moveRelative.label", description = "@text/action.moveRelative.descr")
    public @ActionOutput(name = "running", type = "java.lang.Boolean", label = "@text/action.run.label", description = "@text/action.run.descr") Boolean moveRelative(
            @ActionInput(name = "nodeId", label = "@text/action.node.label", description = "@text/action.node.descr") String nodeId,
            @ActionInput(name = "relativePercent", label = "@text/action.relative.label", description = "@text/action.relative.descr") String relativePercent)
            throws NumberFormatException, IllegalStateException {
        logger.trace("moveRelative(): action called");
        VeluxBridgeHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            throw new IllegalStateException("Bridge instance is null");
        }
        int node = Integer.parseInt(nodeId);
        if (node < 0 || node > 200) {
            throw new NumberFormatException("Node Id out of range");
        }
        int relPct = Integer.parseInt(relativePercent);
        if (Math.abs(relPct) > 100) {
            throw new NumberFormatException("Relative Percent out of range");
        }
        return bridgeHandler.moveRelative(node, relPct);
    }

    /**
     * Static method to send a reboot command to a Velux Bridge
     *
     * @param actions ThingActions from the caller
     * @return true if the command was sent
     * @throws IllegalArgumentException if actions is invalid
     * @throws IllegalStateException if anything else is wrong
     */
    public static Boolean rebootBridge(ThingActions actions) throws IllegalArgumentException, IllegalStateException {
        return ((VeluxActions) actions).rebootBridge();
    }

    /**
     * Static method to send a relative move command to a Velux actuator
     *
     * @param actions ThingActions from the caller
     * @param nodeId the node Id in the bridge
     * @param relativePercent the target position relative to its current position (-100% <= relativePercent <= +100%)
     * @return true if the command was sent
     * @throws IllegalArgumentException if actions is invalid
     * @throws IllegalStateException if anything else is wrong
     * @throws NumberFormatException if either of nodeId or relativePercent is not an integer, or out of range
     */
    public static Boolean moveRelative(ThingActions actions, String nodeId, String relativePercent)
            throws IllegalArgumentException, NumberFormatException, IllegalStateException {
        return ((VeluxActions) actions).moveRelative(nodeId, relativePercent);
    }

    @RuleAction(label = "@text/action.moveMainAndVane.label", description = "@text/action.moveMainAndVane.descr")
    public @ActionOutput(name = "running", type = "java.lang.Boolean", label = "@text/action.run.label", description = "@text/action.run.descr") Boolean moveMainAndVane(
            @ActionInput(name = "thingName", label = "@text/action.thing.label", description = "@text/action.thing.descr") String thingName,
            @ActionInput(name = "mainPercent", label = "@text/action.main.label", description = "@text/action.main.descr") Integer mainPercent,
            @ActionInput(name = "vanePercent", label = "@text/action.vane.label", description = "@text/action.vane.descr") Integer vanePercent)
            throws NumberFormatException, IllegalArgumentException, IllegalStateException {
        logger.trace("moveMainAndVane(thingName:'{}', mainPercent:{}, vanePercent:{}) action called", thingName,
                mainPercent, vanePercent);
        VeluxBridgeHandler bridgeHandler = this.bridgeHandler;
        if (bridgeHandler == null) {
            throw new IllegalStateException("Bridge instance is null");
        }
        ProductBridgeIndex node = bridgeHandler.getProductBridgeIndex(thingName);
        if (ProductBridgeIndex.UNKNOWN.equals(node)) {
            throw new IllegalArgumentException("Bridge does not contain a thing with name " + thingName);
        }
        PercentType mainPercentType = new PercentType(mainPercent);
        PercentType vanePercenType = new PercentType(vanePercent);
        return bridgeHandler.moveMainAndVane(node, mainPercentType, vanePercenType);
    }

    /**
     * Action to simultaneously move the shade main position and vane positions.
     *
     *
     * @param actions ThingActions from the caller
     * @param thingName the name of the thing to be moved (e.g. 'velux:rollershutter:hubid:thingid')
     * @param mainPercent the desired main position (range 0..100)
     * @param vanePercent the desired vane position (range 0..100)
     * @return true if the command was sent
     * @throws NumberFormatException if any of the arguments are not an integer
     * @throws IllegalArgumentException if any of the arguments are invalid
     * @throws IllegalStateException if anything else is wrong
     */
    public static Boolean moveMainAndVane(ThingActions actions, String thingName, Integer mainPercent,
            Integer vanePercent) throws NumberFormatException, IllegalArgumentException, IllegalStateException {
        return ((VeluxActions) actions).moveMainAndVane(thingName, mainPercent, vanePercent);
    }
}
