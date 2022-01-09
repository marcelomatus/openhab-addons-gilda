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
package org.openhab.binding.lcn.internal.subhandler;

import static org.mockito.Mockito.verify;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.lcn.internal.common.LcnChannelGroup;
import org.openhab.binding.lcn.internal.common.LcnException;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;

/**
 * Test class.
 *
 * @author Fabian Wolter - Initial contribution
 */
@NonNullByDefault
public class LcnModuleRollershutterRelaySubHandlerTest extends AbstractTestLcnModuleSubHandler {
    private @NonNullByDefault({}) LcnModuleRollershutterRelaySubHandler l;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        l = new LcnModuleRollershutterRelaySubHandler(handler, info);
    }

    @Test
    public void testUp1() throws LcnException {
        l.handleCommandUpDown(UpDownType.UP, LcnChannelGroup.ROLLERSHUTTERRELAY, 0, false);
        verify(handler).sendPck("R810------");
    }

    @Test
    public void testUpInverted() throws LcnException {
        l.handleCommandUpDown(UpDownType.UP, LcnChannelGroup.ROLLERSHUTTERRELAY, 0, true);
        verify(handler).sendPck("R811------");
    }

    @Test
    public void testUp4() throws LcnException {
        l.handleCommandUpDown(UpDownType.UP, LcnChannelGroup.ROLLERSHUTTERRELAY, 3, false);
        verify(handler).sendPck("R8------10");
    }

    @Test
    public void testDown1() throws LcnException {
        l.handleCommandUpDown(UpDownType.DOWN, LcnChannelGroup.ROLLERSHUTTERRELAY, 0, false);
        verify(handler).sendPck("R811------");
    }

    @Test
    public void testDownInverted() throws LcnException {
        l.handleCommandUpDown(UpDownType.DOWN, LcnChannelGroup.ROLLERSHUTTERRELAY, 0, true);
        verify(handler).sendPck("R810------");
    }

    @Test
    public void testDown4() throws LcnException {
        l.handleCommandUpDown(UpDownType.DOWN, LcnChannelGroup.ROLLERSHUTTERRELAY, 3, false);
        verify(handler).sendPck("R8------11");
    }

    @Test
    public void testStop1() throws LcnException {
        l.handleCommandStopMove(StopMoveType.STOP, LcnChannelGroup.ROLLERSHUTTERRELAY, 0);
        verify(handler).sendPck("R80-------");
    }

    @Test
    public void testStop4() throws LcnException {
        l.handleCommandStopMove(StopMoveType.STOP, LcnChannelGroup.ROLLERSHUTTERRELAY, 3);
        verify(handler).sendPck("R8------0-");
    }

    @Test
    public void testMove1() throws LcnException {
        l.handleCommandStopMove(StopMoveType.MOVE, LcnChannelGroup.ROLLERSHUTTERRELAY, 0);
        verify(handler).sendPck("R81-------");
    }

    @Test
    public void testMove4() throws LcnException {
        l.handleCommandStopMove(StopMoveType.MOVE, LcnChannelGroup.ROLLERSHUTTERRELAY, 3);
        verify(handler).sendPck("R8------1-");
    }
}
