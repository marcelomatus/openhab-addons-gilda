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
package org.openhab.binding.unifi.internal.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.types.RefreshType.REFRESH;

import java.lang.reflect.ParameterizedType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.unifi.internal.api.UniFiController;
import org.openhab.binding.unifi.internal.api.UniFiException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matthew Bowman - Initial contribution
 *
 * @param <E> entity - the UniFi entity class used by this thing handler
 * @param <C> config - the UniFi config class used by this thing handler
 */
@NonNullByDefault
public abstract class UniFiBaseThingHandler<E, C> extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(UniFiBaseThingHandler.class);

    public UniFiBaseThingHandler(final Thing thing) {
        super(thing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void initialize() {
        final Bridge bridge = getBridge();
        if (bridge == null || bridge.getHandler() == null
                || !(bridge.getHandler() instanceof UniFiControllerThingHandler)) {
            updateStatus(OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/error.thing.offline.configuration_error");
            return;
        }
        // mgb: derive the config class from the generic type
        final Class<?> clazz = (Class<?>) (((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[1]);
        final C config = (C) getConfigAs(clazz);
        if (initialize(config)) {
            if (bridge.getStatus() == OFFLINE) {
                updateStatus(OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "@text/error.thing.offline.bridge_offline");
                return;
            } else {
                updateStatus(ONLINE);
            }
        }
    }

    /**
     * Utility method to access the {@link UniFiController} instance associated with this thing.
     *
     * @return
     */
    @SuppressWarnings("null")
    private final @Nullable UniFiController getController() {
        final Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() != null
                && (bridge.getHandler() instanceof UniFiControllerThingHandler)) {
            return ((UniFiControllerThingHandler) bridge.getHandler()).getController();
        }
        return null;
    }

    private @Nullable E getEntity() {
        final UniFiController controller = getController();
        return controller == null ? null : getEntity(controller);
    }

    @Override
    public final void handleCommand(final ChannelUID channelUID, final Command command) {
        logger.debug("Handling command = {} for channel = {}", command, channelUID);
        // mgb: only handle commands if we're ONLINE
        if (getThing().getStatus() == ONLINE) {
            final E entity = getEntity();
            final UniFiController controller = getController();

            if (entity != null && controller != null) {
                if (command == REFRESH) {
                    refreshChannel(entity, channelUID);
                } else {
                    try {
                        if (!handleCommand(controller, entity, channelUID, command)) {
                            logger.info("Ignoring unsupported command = {} for channel = {}", command, channelUID);
                        }
                    } catch (final UniFiException e) {
                        logger.info("Unexpected error handling command = {} for channel = {} : {}", command, channelUID,
                                e.getMessage());
                    }
                }
            } else {
                // mgb: set the default state if we're online yet cannot find the respective entity
                // updateState(channelUID, getDefaultState(channelUID)State);
            }
        }
    }

    protected final void refresh() {
        // mgb: only refresh if we're ONLINE
        if (getThing().getStatus() == ONLINE) {
            final E entity = getEntity();
            if (entity != null) {
                for (final Channel channel : getThing().getChannels()) {
                    final ChannelUID channelUID = channel.getUID();
                    refreshChannel(entity, channelUID);
                }
            }
        }
    }

    /**
     * Additional sub class specific initialization.
     * If initialization is unsuccessful it should set the thing status and return false.
     * if it was successful it should return true
     *
     * @param config
     * @return true if initialization was successful
     */
    protected abstract boolean initialize(C config);

    protected abstract @Nullable E getEntity(UniFiController controller);

    protected abstract void refreshChannel(E entity, ChannelUID channelUID);

    protected abstract boolean handleCommand(UniFiController controller, E entity, ChannelUID channelUID,
            Command command) throws UniFiException;
}
