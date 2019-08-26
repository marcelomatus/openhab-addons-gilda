/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.snmp.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.junit.Test;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

/**
 * Tests cases for {@link SnmpTargetHandler}.
 *
 * @author Jan N. Klug - Initial contribution
 */
public class SwitchChannelTest extends AbstractSnmpTargetHandlerTest {

    @Test
    public void testCommandsAreProperlyHandledBySwitchChannel() throws IOException {
        VariableBinding variable;

        variable = handleCommandSwitchChannel(SnmpDatatype.STRING, OnOffType.ON, "on", "off", true);
        assertEquals(new OID(TEST_OID), variable.getOid());
        assertTrue(variable.getVariable() instanceof OctetString);
        assertEquals("on", ((OctetString) variable.getVariable()).toString());

        variable = handleCommandSwitchChannel(SnmpDatatype.STRING, OnOffType.OFF, "on", "off", true);
        assertEquals(new OID(TEST_OID), variable.getOid());
        assertTrue(variable.getVariable() instanceof OctetString);
        assertEquals("off", ((OctetString) variable.getVariable()).toString());

        variable = handleCommandSwitchChannel(SnmpDatatype.STRING, OnOffType.OFF, "on", null, false);
        assertNull(variable);
    }

    @Test
    public void testSwitchChannelsProperlyUpdatingOnValue() throws IOException {
        setup(SnmpBindingConstants.CHANNEL_TYPE_UID_SWITCH, SnmpChannelMode.READ, SnmpDatatype.STRING, "on", "off");
        PDU responsePDU = new PDU(PDU.RESPONSE,
                Collections.singletonList(new VariableBinding(new OID(TEST_OID), new OctetString("on"))));
        ResponseEvent event = new ResponseEvent("test", null, null, responsePDU, null);
        thingHandler.onResponse(event);
        verify(thingHandlerCallback, atLeast(1)).stateUpdated(eq(CHANNEL_UID), eq(OnOffType.ON));
    }

    @Test
    public void testSwitchChannelsProperlyUpdatingOffValue() throws IOException {
        setup(SnmpBindingConstants.CHANNEL_TYPE_UID_SWITCH, SnmpChannelMode.READ, SnmpDatatype.INT32, "0", "3");
        PDU responsePDU = new PDU(PDU.RESPONSE,
                Collections.singletonList(new VariableBinding(new OID(TEST_OID), new Integer32(3))));
        ResponseEvent event = new ResponseEvent("test", null, null, responsePDU, null);
        thingHandler.onResponse(event);
        verify(thingHandlerCallback, atLeast(1)).stateUpdated(eq(CHANNEL_UID), eq(OnOffType.OFF));
    }

    @Test
    public void testSwitchChannelsProperlyUpdatingHexValue() throws IOException {
        setup(SnmpBindingConstants.CHANNEL_TYPE_UID_SWITCH, SnmpChannelMode.READ, SnmpDatatype.HEXSTRING, "AA bb 11",
                "cc ba 1d");
        PDU responsePDU = new PDU(PDU.RESPONSE, Collections
                .singletonList(new VariableBinding(new OID(TEST_OID), OctetString.fromHexStringPairs("aabb11"))));
        ResponseEvent event = new ResponseEvent("test", null, null, responsePDU, null);
        thingHandler.onResponse(event);
        verify(thingHandlerCallback, atLeast(1)).stateUpdated(eq(CHANNEL_UID), eq(OnOffType.ON));
    }

    @Test
    public void testSwitchChannelsIgnoresArbitraryValue() throws IOException {
        setup(SnmpBindingConstants.CHANNEL_TYPE_UID_SWITCH, SnmpChannelMode.READ, SnmpDatatype.COUNTER64, "0", "12223");
        PDU responsePDU = new PDU(PDU.RESPONSE,
                Collections.singletonList(new VariableBinding(new OID(TEST_OID), new Counter64(17))));
        ResponseEvent event = new ResponseEvent("test", null, null, responsePDU, null);
        thingHandler.onResponse(event);
        verify(thingHandlerCallback, never()).stateUpdated(eq(CHANNEL_UID), any());
    }

    @Test
    public void testSwitchChannelSendsUndefExceptionValue() throws IOException {
        setup(SnmpBindingConstants.CHANNEL_TYPE_UID_SWITCH, SnmpChannelMode.READ, SnmpDatatype.COUNTER64, "0", "12223");
        PDU responsePDU = new PDU(PDU.RESPONSE,
                Collections.singletonList(new VariableBinding(new OID(TEST_OID), Null.noSuchInstance)));
        ResponseEvent event = new ResponseEvent("test", null, null, responsePDU, null);
        thingHandler.onResponse(event);
        verify(thingHandlerCallback, atLeast(1)).stateUpdated(eq(CHANNEL_UID), eq(UnDefType.UNDEF));
    }

    @Test
    public void testSwitchChannelSendsConfiguredExceptionValue() throws IOException {
        setup(SnmpBindingConstants.CHANNEL_TYPE_UID_SWITCH, SnmpChannelMode.READ, SnmpDatatype.COUNTER64, "0", "12223",
                "OFF");
        PDU responsePDU = new PDU(PDU.RESPONSE,
                Collections.singletonList(new VariableBinding(new OID(TEST_OID), Null.noSuchInstance)));
        ResponseEvent event = new ResponseEvent("test", null, null, responsePDU, null);
        thingHandler.onResponse(event);
        verify(thingHandlerCallback, atLeast(1)).stateUpdated(eq(CHANNEL_UID), eq(OnOffType.OFF));
    }
}
