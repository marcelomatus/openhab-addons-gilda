/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.loxone.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A Light Controller type of control on Loxone Miniserver.
 * <p>
 * According to Loxone API documentation, a light controller is one of following functional blocks:
 * <ul>
 * <li>Lighting Controller
 * <li>Hotel Lighting Controller
 * </ul>
 *
 * @author Pawel Pieczul - initial commit
 *
 */
public class LxControlLightController extends LxControl implements LxControlStateListener {

    /**
     * A name by which Miniserver refers to light controller controls
     */
    public static final String TYPE_NAME = "lightcontroller";

    /**
     * Current active scene number (0-9)
     */
    private static final String STATE_ACTIVE_SCENE = "activescene";
    /**
     * List of available scenes
     */
    private static final String STATE_SCENE_LIST = "scenelist";
    /**
     * Command string used to set control's state to ON
     */
    private static final String CMD_ON = "On";
    /**
     * Command string used to set control's state to OFF
     */
    private static final String CMD_OFF = "Off";
    /**
     * Command string used to go to the next scene
     */
    private static final String CMD_NEXT_SCENE = "plus";
    /**
     * Command string used to go to the previous scene
     */
    private static final String CMD_PREVIOUS_SCENE = "minus";

    public static final int NUM_OF_SCENES = 10;
    private Map<String, String> sceneNames = new HashMap<String, String>();
    private boolean newSceneNames = false;

    /**
     * Create lighting controller object.
     *
     * @param client
     *            communication client used to send commands to the Miniserver
     * @param uuid
     *            controller's UUID
     * @param name
     *            controller's name
     * @param room
     *            room to which controller belongs
     * @param category
     *            category to which controller belongs
     * @param states
     *            controller's states and their names
     * @param movementScene
     *            scene number (0-9) that is designated as a 'movement' scene
     */
    LxControlLightController(LxWsClient client, LxUuid uuid, String name, LxContainer room, LxCategory category,
            Map<String, LxControlState> states, int movementScene) {
        super(client, uuid, name, room, category, states);

        LxControlState sceneListState = getState(STATE_SCENE_LIST);
        if (sceneListState != null) {
            sceneListState.addListener(this);
        }

    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Set all outputs to ON.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void allOn() throws IOException {
        socketClient.sendAction(uuid, CMD_ON);
    }

    /**
     * Set all outputs to OFF.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void allOff() throws IOException {
        socketClient.sendAction(uuid, CMD_OFF);
    }

    /**
     * Select next lighting scene.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void nextScene() throws IOException {
        socketClient.sendAction(uuid, CMD_NEXT_SCENE);
    }

    /**
     * Select previous lighting scene.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void previousScene() throws IOException {
        socketClient.sendAction(uuid, CMD_PREVIOUS_SCENE);
    }

    /**
     * Set provided scene.
     *
     * @param scene
     *            scene number to set (0-9)
     * @throws IOException
     *             when something went wrong with communication
     */
    public void setScene(int scene) throws IOException {
        if (scene >= 0 && scene < NUM_OF_SCENES) {
            socketClient.sendAction(uuid, Long.toString(scene));
        }
    }

    /**
     * Get current active scene
     *
     * @return
     *         number of the active scene (0-9, 0-all off, 9-all on) or -1 if error
     */
    public int getCurrentScene() {
        LxControlState state = getState(STATE_ACTIVE_SCENE);
        if (state != null) {
            return (int) state.getValue();
        }
        return -1;
    }

    /**
     * Return an array with names of all scenes, where index is scene number
     *
     * @return
     *         an array with scene names indexed by scene number
     */
    public Map<String, String> getSceneNames() {
        return sceneNames;
    }

    /**
     * Check if scene names were updated since last check.
     * 
     * @return
     *         true if there are new scene names
     */
    public boolean sceneNamesUpdated() {
        boolean ret = newSceneNames;
        newSceneNames = false;
        return ret;
    }

    /**
     * Get scene names from new state value received from the Miniserver
     */
    @Override
    public void onStateChange(LxControlState state) {
        String scenesText = state.getTextValue();
        if (scenesText != null) {
            sceneNames.clear();
            String[] scenes = scenesText.split(",");
            for (String line : scenes) {
                line = line.replaceAll("\"", "");
                String[] params = line.split("=");
                if (params.length == 2) {
                    sceneNames.put(params[0], params[1]);
                }
            }
            newSceneNames = true;
        }
    }
}
