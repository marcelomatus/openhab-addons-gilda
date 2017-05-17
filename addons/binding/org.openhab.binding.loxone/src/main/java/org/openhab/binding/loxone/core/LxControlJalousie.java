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
import java.util.Map;

/**
 * A jalousie type of control on Loxone Miniserver.
 * <p>
 * According to Loxone API documentation, a jalousie control covers:
 * <ul>
 * <li>Blinds
 * <li>Automatic blinds
 * <li>Automatic blinds integrated
 * </ul>
 *
 * @author Pawel Pieczul - initial commit
 *
 */
public class LxControlJalousie extends LxControl implements LxControlStateListener {
    /**
     * A name by which Miniserver refers to jalousie controls
     */
    public final static String TYPE_NAME = "jalousie";

    /**
     * Jalousie is moving up
     */
    private final static String STATE_UP = "up";
    /**
     * Jalousie is moving down
     */
    private final static String STATE_DOWN = "down";
    /**
     * The position of the Jalousie, a number from 0 to 1
     * Jalousie upper position = 0
     * Jalousie lower position = 1
     */
    private final static String STATE_POSITION = "position";
    /**
     * The shade position of the Jalousie (blinds), a number from 0 to 1
     * Blinds are not shaded = 0
     * Blinds are shaded = 1
     */
    @SuppressWarnings("unused")
    private final static String STATE_SHADE_POSITION = "shadeposition";
    /**
     * Only used by ones with Autopilot, this represents the safety shutdown
     */
    @SuppressWarnings("unused")
    private final static String STATE_SAFETY_ACTIVE = "safetyactive";
    /**
     * Only used by ones with Autopilot
     */
    @SuppressWarnings("unused")
    private final static String STATE_AUTO_ALLOWED = "autoallowed";
    /**
     * Only used by ones with Autopilot
     */
    @SuppressWarnings("unused")
    private final static String STATE_AUTO_ACTIVE = "autoactive";
    /**
     * Only used by ones with Autopilot, this represents the output QI in Loxone Config
     */
    @SuppressWarnings("unused")
    private final static String STATE_LOCKED = "locked";

    /**
     * Command string used to set control's state to Down
     */
    @SuppressWarnings("unused")
    private final static String CMD_DOWN = "Down";
    /**
     * Command string used to set control's state to Up
     */
    @SuppressWarnings("unused")
    private final static String CMD_UP = "Up";
    /**
     * Command string used to set control's state to Full Down
     */
    private final static String CMD_FULL_DOWN = "FullDown";
    /**
     * Command string used to set control's state to Full Up
     */
    private final static String CMD_FULL_UP = "FullUp";
    /**
     * Command string used to stop rollershutter
     */
    private final static String CMD_STOP = "Stop";

    private double targetPosition = -1;

    /**
     * Create jalousie control object.
     *
     * @param client
     *            communication client used to send commands to the Miniserver
     * @param uuid
     *            jalousie's UUID
     * @param name
     *            jalousie's name
     * @param room
     *            room to which jalousie belongs
     * @param category
     *            category to which jalousie belongs
     * @param states
     *            jalousie's states and their names
     */
    public LxControlJalousie(LxWsClient client, LxUuid uuid, String name, LxContainer room, LxCategory category,
            Map<String, LxControlState> states) {
        super(client, uuid, name, room, category, states);

        LxControlState positionState = getState(STATE_POSITION);
        if (positionState != null) {
            positionState.addListener(this);
        }
    }

    /**
     * Set rollershutter (jalousie) to full up position.
     * <p>
     * Sends a command to operate the rollershutter.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void fullUp() throws IOException {
        socketClient.sendAction(uuid, CMD_FULL_UP);
    }

    /**
     * Set rollershutter (jalousie) to full down position.
     * <p>
     * Sends a command to operate the rollershutter.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void fullDown() throws IOException {
        socketClient.sendAction(uuid, CMD_FULL_DOWN);
    }

    /**
     * Stop movement of the rollershutter (jalousie)
     * <p>
     * Sends a command to operate the rollershutter.
     *
     * @throws IOException
     *             when something went wrong with communication
     */
    public void stop() throws IOException {
        socketClient.sendAction(uuid, CMD_STOP);
    }

    /**
     * Move the rollershutter (jalousie) to a desired position.
     * <p>
     * The jalousie will start moving in the desired direction based on the current position. It will stop moving once
     * there is a state update event received with value above/below (depending on direction) or equal to the set
     * position.
     *
     * @param position
     *            end position to move jalousie to, floating point number from 0..1 (0-fully closed to 1-fully open)
     * @throws IOException
     *             when something went wrong with communication
     */
    public void moveToPosition(double position) throws IOException {
        double currentPosition = getPosition();
        if (currentPosition > position) {
            logger.debug("Moving jalousie up from {} to {}", currentPosition, position);
            targetPosition = position;
            fullUp();
        } else if (currentPosition < position) {
            logger.debug("Moving jalousie down from {} to {}", currentPosition, position);
            targetPosition = position;
            fullDown();
        }
    }

    /**
     * Get current position of the rollershutter (jalousie)
     *
     * @return
     *         a floating point number from range 0-fully closed to 1-fully open
     */
    public double getPosition() {
        LxControlState state = getState(LxControlJalousie.STATE_POSITION);
        if (state != null) {
            return state.getValue();
        }
        throw new NullPointerException("Jalousie state 'position' is null");
    }

    /**
     * Monitor jalousie position against desired target position and stop it if target position is reached.
     */
    @Override
    public void onStateChange(LxControlState state) {
        // check position changes
        if (state.getName().equals(STATE_POSITION) && targetPosition > 0 && targetPosition < 1) {
            // see in which direction jalousie is moving
            LxControlState up = getState(STATE_UP);
            LxControlState down = getState(STATE_DOWN);
            if (up != null && down != null) {
                double currentPosition = state.getValue();
                if (((up.getValue() == 1) && (currentPosition < targetPosition))
                        || ((down.getValue() == 1) && (currentPosition > targetPosition))) {
                    targetPosition = -1;
                    try {
                        stop();
                    } catch (IOException e) {
                        logger.debug("Error stopping jalousie when meeting target position.");
                    }

                }
            }
        }
    }
}
