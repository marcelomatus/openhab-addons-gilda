/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.qbus.internal.handler;

import static org.openhab.binding.qbus.internal.QbusBindingConstants.CHANNEL_SCENE;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.qbus.internal.QbusBridgeHandler;
import org.openhab.binding.qbus.internal.protocol.QbusCommunication;
import org.openhab.binding.qbus.internal.protocol.QbusScene;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;

/**
 * The {@link QbusSceneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Koen Schockaert - Initial Contribution
 */

@NonNullByDefault
public class QbusSceneHandler extends QbusGlobalHandler {

    public QbusSceneHandler(Thing thing) {
        super(thing);
    }

    protected @NonNullByDefault({}) QbusThingsConfig config;

    int sceneId;

    @Nullable
    String sn;

    /**
     * Main initialization
     */
    @Override
    public void initialize() {

        setConfig();
        sceneId = getId();

        QbusCommunication QComm = getCommunication("Scene", sceneId);
        if (QComm == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "No communication with Qbus Bridge!");
            return;
        }

        QbusBridgeHandler QBridgeHandler = getBridgeHandler("Scene", sceneId);
        if (QBridgeHandler == null) {
            return;
        }

        setSN();

        Map<Integer, QbusScene> sceneComm = QComm.getScene();

        if (sceneComm != null) {
            QbusScene QScene = sceneComm.get(sceneId);
            if (QScene != null) {
                QScene.setThingHandler(this);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                        "Error while initializing the thing.");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Error while initializing the thing.");
        }
    }

    /**
     * Returns the serial number of the controller
     *
     * @return the serial nr
     */
    public @Nullable String getSN() {
        return this.sn;
    }

    /**
     * Sets the serial number of the controller
     */
    public void setSN() {
        QbusBridgeHandler QBridgeHandler = getBridgeHandler("Scene", sceneId);
        if (QBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "No communication with Qbus Bridge!");
            return;
        }
        this.sn = QBridgeHandler.getSn();
        ;
    }

    /**
     * Handle the status update from the thing
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        QbusCommunication QComm = getCommunication("Scene", sceneId);

        if (QComm == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Bridge communication not initialized when trying to execute command for Scene " + sceneId);
            return;
        }

        Map<Integer, QbusScene> sceneComm = QComm.getScene();

        if (sceneComm != null) {
            QbusScene QScene = sceneComm.get(sceneId);
            if (QScene == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Bridge communication not initialized when trying to execute command for Scene " + sceneId);
                return;
            } else {

                scheduler.submit(() -> {
                    if (!QComm.communicationActive()) {
                        restartCommunication(QComm, "Scene", sceneId);
                    }

                    if (QComm.communicationActive()) {

                        switch (channelUID.getId()) {
                            case CHANNEL_SCENE:
                                handleSwitchCommand(QScene, channelUID, command);
                                updateStatus(ThingStatus.ONLINE);
                                break;

                            default:
                                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                        "Channel unknown " + channelUID.getId());
                        }
                    }
                });
            }
        }
    }

    /**
     * Method to update state of channel, called from Qbus Scene.
     */
    public void handleStateUpdate(QbusScene QScene) {

        Integer sceneState = QScene.getState();
        if (sceneState != null) {
            updateState(CHANNEL_SCENE, (sceneState == 0) ? OnOffType.OFF : OnOffType.ON);
            updateStatus(ThingStatus.ONLINE);
        }
    }

    /**
     *
     * @param message
     */
    public void thingOffline(String message) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, message);
    }

    /**
     * Executes the scene command
     */
    private void handleSwitchCommand(QbusScene QScene, ChannelUID channelUID, Command command) {
        @Nullable
        String snr = getSN();
        if (command instanceof OnOffType) {
            OnOffType s = (OnOffType) command;
            if (s == OnOffType.OFF) {
                if (snr != null) {
                    QScene.execute(0, snr);
                } else {
                    thingOffline("No serial number configured for  " + sceneId);
                }
            } else {
                if (snr != null) {
                    QScene.execute(100, snr);
                } else {
                    thingOffline("No serial number configured for  " + sceneId);
                }
            }
        }
    }

    /**
     * Read the configuration
     */
    protected synchronized void setConfig() {
        config = getConfig().as(QbusThingsConfig.class);
    }

    /**
     * Returns the Id from the configuration
     *
     * @return sceneId
     */
    public int getId() {
        if (config != null) {
            return config.sceneId;
        } else {
            return 0;
        }
    }
}
