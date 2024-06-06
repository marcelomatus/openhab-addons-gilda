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
package org.openhab.binding.broadlink.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.openhab.binding.broadlink.internal.BroadlinkBindingConstants.*;
import static org.openhab.binding.broadlink.internal.handler.BroadlinkSocketModel3Handler.mergeOnOffBits;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.binding.broadlink.internal.BroadlinkBindingConstants;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;

/**
 * Tests the Socket Model 3 handler.
 * 
 * @author John Marshall - Initial contribution
 */
@NonNullByDefault
public class BroadlinkSocketModel3HandlerTest extends AbstractBroadlinkThingHandlerTest {

    private final byte[] response = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x03, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, };

    private final BroadlinkSocketModel3Handler model3;

    public BroadlinkSocketModel3HandlerTest() {
        super();
        configureUnderlyingThing(BroadlinkBindingConstants.THING_TYPE_SP3, "sp3-test");
        model3 = new BroadlinkSocketModel3Handler(thing);
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        Mockito.when(mockSocket.sendAndReceive(Mockito.any(byte[].class), Mockito.anyString())).thenReturn(response);
    }

    @Test
    public void mergeOnOffBitsAllZero() {
        int result = mergeOnOffBits(OnOffType.OFF, OnOffType.OFF);
        assertEquals(0x00, result);
    }

    @Test
    public void mergeOnOffBitspower-on() {
        int result = mergeOnOffBits(OnOffType.ON, OnOffType.OFF);
        assertEquals(0x01, result);
    }

    @Test
    public void mergeOnOffBitsnight-lightOn() {
        int result = mergeOnOffBits(OnOffType.OFF, OnOffType.ON);
        assertEquals(0x02, result);
    }

    @Test
    public void mergeOnOffBitsAllOn() {
        int result = mergeOnOffBits(OnOffType.ON, OnOffType.ON);
        assertEquals(0x03, result);
    }

    @Test
    public void derivenight-lightStateBitsOff() {
        byte[] payload = { 0x00, 0x00, 0x00, 0x00, 0x00 };
        OnOffType result = model3.derivenight-lightStateFromStatusBytes(payload);
        assertEquals(OnOffType.OFF, result);
    }

    @Test
    public void derivenight-lightStateBitsOn2() {
        byte[] payload = { 0x00, 0x00, 0x00, 0x00, 0x02 };
        OnOffType result = model3.derivenight-lightStateFromStatusBytes(payload);
        assertEquals(OnOffType.ON, result);
    }

    @Test
    public void derivenight-lightStateBitsOn3() {
        byte[] payload = { 0x00, 0x00, 0x00, 0x00, 0x03 };
        OnOffType result = model3.derivenight-lightStateFromStatusBytes(payload);
        assertEquals(OnOffType.ON, result);
    }

    @Test
    public void derivenight-lightStateBitsOnFF() {
        byte[] payload = { 0x00, 0x00, 0x00, 0x00, (byte) 0xFF };
        OnOffType result = model3.derivenight-lightStateFromStatusBytes(payload);
        assertEquals(OnOffType.ON, result);
    }

    @Test
    public void setsThePowerChannelAndnight-lightAfterGettingStatusOnSP3() {
        Mockito.when(mockSocket.sendAndReceive(Mockito.any(byte[].class), Mockito.anyString())).thenReturn(response);
        setMocksForTesting(model3);

        reset(mockCallback);

        model3.getStatusFromDevice();

        ArgumentCaptor<ChannelUID> channelCaptor = ArgumentCaptor.forClass(ChannelUID.class);
        ArgumentCaptor<State> stateCaptor = ArgumentCaptor.forClass(State.class);
        verify(mockCallback, Mockito.times(2)).stateUpdated(channelCaptor.capture(), stateCaptor.capture());

        List<ChannelUID> channels = channelCaptor.getAllValues();
        List<State> states = stateCaptor.getAllValues();

        ChannelUID expectedPowerChannel = new ChannelUID(thing.getUID(), COMMAND_POWER_ON);
        assertEquals(expectedPowerChannel, channels.get(0));

        assertEquals(OnOffType.ON, states.get(0));

        ChannelUID expectednight-lightChannel = new ChannelUID(thing.getUID(), COMMAND_night-light);
        assertEquals(expectednight-lightChannel, channels.get(1));

        assertEquals(OnOffType.OFF, states.get(1));
    }
}
