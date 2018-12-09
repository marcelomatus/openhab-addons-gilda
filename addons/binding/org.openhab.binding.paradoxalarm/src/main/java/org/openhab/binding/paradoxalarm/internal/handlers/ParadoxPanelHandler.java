/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.paradoxalarm.internal.handlers;

import static org.openhab.binding.paradoxalarm.internal.handlers.ParadoxAlarmBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.paradoxalarm.internal.exceptions.ParadoxBindingException;
import org.openhab.binding.paradoxalarm.internal.model.ParadoxInformation;
import org.openhab.binding.paradoxalarm.internal.model.ParadoxPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ParadoxPanelHandler} This is the handler that takes care of the panel related stuff.
 *
 * @author Konstantin_Polihronov - Initial contribution
 */
@NonNullByDefault
public class ParadoxPanelHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ParadoxPanelHandler.class);

    @Nullable
    private ParadoxPanelConfiguration config;

    public ParadoxPanelHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateThing();
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        updateStatus(ThingStatus.UNKNOWN);
        try {
            initializeModel();
            updateThing();
            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error initializing panel handler. Exception: " + e);
        }
        logger.debug("Finished initializing!");
    }

    private void initializeModel() throws Exception, ParadoxBindingException {
        config = getConfigAs(ParadoxPanelConfiguration.class);
        ParadoxPanel.getInstance();
    }

    private void refreshModelData() {
        try {
            ParadoxPanel.getInstance().updateEntitiesStates();
        } catch (Exception e) {
            logger.error("Unable to retrieve memory map. {}", e);
        }
    }

    private void updateThing() {
        try {
            refreshModelData();

            ParadoxPanel panel = ParadoxPanel.getInstance();
            StringType panelState = panel.isOnline() ? STATE_ONLINE : STATE_OFFLINE;
            updateState(PANEL_STATE_CHANNEL_UID, panelState);

            ParadoxInformation panelInformation = panel.getPanelInformation();
            if (panelInformation != null) {
                updateState(PANEL_SERIAL_NUMBER_CHANNEL_UID, new StringType(panelInformation.getSerialNumber()));
                updateState(PANEL_TYPE_CHANNEL_UID, new StringType(panelInformation.getPanelType().name()));
                updateState(PANEL_HARDWARE_VERSION_CHANNEL_UID,
                        new StringType(panelInformation.getHardwareVersion().toString()));
                updateState(PANEL_APPLICATION_VERSION_CHANNEL_UID,
                        new StringType(panelInformation.getApplicationVersion().toString()));
                updateState(PANEL_BOOTLOADER_VERSION_CHANNEL_UID,
                        new StringType(panelInformation.getBootLoaderVersion().toString()));
            }
        } catch (ParadoxBindingException e) {
            logger.error("Unable to retrieve ParadoxPanel instance. {}", e);
        }

    }
}
