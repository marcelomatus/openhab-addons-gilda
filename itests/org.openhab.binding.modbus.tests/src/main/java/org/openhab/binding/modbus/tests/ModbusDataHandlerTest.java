/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.modbus.tests;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.openhab.binding.modbus.internal.ModbusBindingConstantsInternal.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandler;
import org.openhab.binding.modbus.internal.handler.ModbusDataThingHandler;
import org.openhab.binding.modbus.internal.handler.ModbusTcpThingHandler;
import org.openhab.io.transport.modbus.AsyncModbusFailure;
import org.openhab.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.io.transport.modbus.AsyncModbusWriteResult;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusConstants;
import org.openhab.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusResponse;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.PollTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author Sami Salonen - Initial contribution
 */
@RunWith(MockitoJUnitRunner.class)
public class ModbusDataHandlerTest extends AbstractModbusOSGiTest {

    private final class MultiplyTransformation implements TransformationService {
        @Override
        public String transform(String function, String source) throws TransformationException {
            return String.valueOf(Integer.parseInt(function) * Integer.parseInt(source));
        }
    }

    private static final Map<String, String> CHANNEL_TO_ACCEPTED_TYPE = new HashMap<>();
    static {
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_SWITCH, "Switch");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_CONTACT, "Contact");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_DATETIME, "DateTime");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_DIMMER, "Dimmer");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_NUMBER, "Number");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_STRING, "String");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_ROLLERSHUTTER, "Rollershutter");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_LAST_READ_SUCCESS, "DateTime");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_LAST_WRITE_SUCCESS, "DateTime");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_LAST_WRITE_ERROR, "DateTime");
        CHANNEL_TO_ACCEPTED_TYPE.put(CHANNEL_LAST_READ_ERROR, "DateTime");
    }
    private List<ModbusWriteRequestBlueprint> writeRequests = new ArrayList<>();

    @After
    public void tearDown() {
        writeRequests.clear();
    }

    private void captureModbusWrites() {
        Mockito.when(comms.submitOneTimeWrite(any(), any(), any())).then(invocation -> {
            ModbusWriteRequestBlueprint task = (ModbusWriteRequestBlueprint) invocation.getArgument(0);
            writeRequests.add(task);
            return Mockito.mock(ScheduledFuture.class);
        });
    }

    private Bridge createPollerMock(String pollerId, PollTask task) {
        final Bridge poller;
        ThingUID thingUID = new ThingUID(THING_TYPE_MODBUS_POLLER, pollerId);
        BridgeBuilder builder = BridgeBuilder.create(THING_TYPE_MODBUS_POLLER, thingUID)
                .withLabel("label for " + pollerId);
        for (Entry<String, String> entry : CHANNEL_TO_ACCEPTED_TYPE.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            builder = builder.withChannel(
                    ChannelBuilder.create(new ChannelUID(thingUID, channelId), channelAcceptedType).build());
        }
        poller = builder.build();
        poller.setStatusInfo(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, ""));

        ModbusPollerThingHandler mockHandler = Mockito.mock(ModbusPollerThingHandler.class);
        doReturn(task.getRequest()).when(mockHandler).getRequest();
        assert comms != null;
        doReturn(comms).when(mockHandler).getCommunicationInterface();
        doReturn(task.getEndpoint()).when(comms).getEndpoint();
        poller.setHandler(mockHandler);
        assertSame(poller.getHandler(), mockHandler);
        assertSame(((ModbusPollerThingHandler) poller.getHandler()).getCommunicationInterface().getEndpoint(),
                task.getEndpoint());
        assertSame(((ModbusPollerThingHandler) poller.getHandler()).getRequest(), task.getRequest());

        addThing(poller);
        return poller;
    }

    private Bridge createTcpMock() {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);
        Bridge tcpBridge = ModbusPollerThingHandlerTest.createTcpThingBuilder("tcp1").build();
        ModbusTcpThingHandler tcpThingHandler = Mockito.mock(ModbusTcpThingHandler.class);
        tcpBridge.setStatusInfo(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, ""));
        tcpBridge.setHandler(tcpThingHandler);
        doReturn(comms).when(tcpThingHandler).getCommunicationInterface();
        doReturn(0).when(tcpThingHandler).getSlaveId();
        tcpThingHandler.initialize();
        assertThat(tcpBridge.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        return tcpBridge;
    }

    private ModbusDataThingHandler createDataHandler(String id, Bridge bridge,
            Function<ThingBuilder, ThingBuilder> builderConfigurator) {
        return createDataHandler(id, bridge, builderConfigurator, null);
    }

    private ModbusDataThingHandler createDataHandler(String id, Bridge bridge,
            Function<ThingBuilder, ThingBuilder> builderConfigurator, BundleContext context) {
        return createDataHandler(id, bridge, builderConfigurator, context, true);
    }

    private ModbusDataThingHandler createDataHandler(String id, Bridge bridge,
            Function<ThingBuilder, ThingBuilder> builderConfigurator, BundleContext context,
            boolean autoCreateItemsAndLinkToChannels) {
        ThingUID thingUID = new ThingUID(THING_TYPE_MODBUS_DATA, id);
        ThingBuilder builder = ThingBuilder.create(THING_TYPE_MODBUS_DATA, thingUID).withLabel("label for " + id);
        Map<String, ChannelUID> toBeLinked = new HashMap<>();
        for (Entry<String, String> entry : CHANNEL_TO_ACCEPTED_TYPE.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            ChannelUID channelUID = new ChannelUID(thingUID, channelId);
            builder = builder.withChannel(ChannelBuilder.create(channelUID, channelAcceptedType).build());

            if (autoCreateItemsAndLinkToChannels) {
                // Create item of correct type and link it to channel
                String itemName = getItemName(channelUID);
                final GenericItem item;
                if (channelId.startsWith("last") || channelId.equals("datetime")) {
                    item = new DateTimeItem(itemName);
                } else {
                    item = coreItemFactory.createItem(StringUtils.capitalize(channelId), itemName);
                }
                assertThat(String.format("Could not determine correct item type for %s", channelId), item,
                        is(notNullValue()));
                assertNotNull(item);
                addItem(item);
                toBeLinked.put(itemName, channelUID);
            }
        }
        if (builderConfigurator != null) {
            builder = builderConfigurator.apply(builder);
        }

        Thing dataThing = builder.withBridge(bridge.getUID()).build();
        addThing(dataThing);

        // Link after the things and items have been created
        for (Entry<String, ChannelUID> entry : toBeLinked.entrySet()) {
            linkItem(entry.getKey(), entry.getValue());
        }
        return (ModbusDataThingHandler) dataThing.getHandler();
    }

    private String getItemName(ChannelUID channelUID) {
        return channelUID.toString().replace(':', '_') + "_item";
    }

    private void assertSingleStateUpdate(ModbusDataThingHandler handler, String channel, Matcher<State> matcher) {
        waitForAssert(() -> {
            ChannelUID channelUID = new ChannelUID(handler.getThing().getUID(), channel);
            String itemName = getItemName(channelUID);
            Item item = itemRegistry.get(itemName);
            assertThat(String.format("Item %s is not available from item registry", itemName), item,
                    is(notNullValue()));
            assertNotNull(item);
            List<State> updates = getStateUpdates(itemName);
            if (updates != null) {
                assertThat(
                        String.format("Many updates found, expected one: %s", Arrays.deepToString(updates.toArray())),
                        updates.size(), is(equalTo(1)));
            }
            State state = updates == null ? null : updates.get(0);
            assertThat(String.format("%s %s, state %s of type %s", item.getClass().getSimpleName(), itemName, state,
                    state == null ? null : state.getClass().getSimpleName()), state, is(matcher));
        });
    }

    private void assertSingleStateUpdate(ModbusDataThingHandler handler, String channel, State state) {
        assertSingleStateUpdate(handler, channel, is(equalTo(state)));
    }

    private void testOutOfBoundsGeneric(int pollStart, int pollLength, String start,
            ModbusReadFunctionCode functionCode, ValueType valueType, ThingStatus expectedStatus) {
        testOutOfBoundsGeneric(pollStart, pollLength, start, functionCode, valueType, expectedStatus, null);
    }

    private void testOutOfBoundsGeneric(int pollStart, int pollLength, String start,
            ModbusReadFunctionCode functionCode, ValueType valueType, ThingStatus expectedStatus,
            BundleContext context) {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        doReturn(pollStart).when(request).getReference();
        doReturn(pollLength).when(request).getDataLength();
        doReturn(functionCode).when(request).getFunctionCode();

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Bridge pollerThing = createPollerMock("poller1", task);

        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", start);
        dataConfig.put("readTransform", "default");
        dataConfig.put("readValueType", valueType.getConfigValue());
        ModbusDataThingHandler dataHandler = createDataHandler("data1", pollerThing,
                builder -> builder.withConfiguration(dataConfig), context);
        assertThat(dataHandler.getThing().getStatusInfo().getDescription(), dataHandler.getThing().getStatus(),
                is(equalTo(expectedStatus)));
    }

    @Test
    public void testInitCoilsOutOfIndex() {
        testOutOfBoundsGeneric(4, 3, "8", ModbusReadFunctionCode.READ_COILS, ModbusConstants.ValueType.BIT,
                ThingStatus.OFFLINE);
    }

    @Test
    public void testInitCoilsOK() {
        testOutOfBoundsGeneric(4, 3, "6", ModbusReadFunctionCode.READ_COILS, ModbusConstants.ValueType.BIT,
                ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithBitOutOfIndex() {
        testOutOfBoundsGeneric(4, 3, "8.0", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.BIT, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithBitOutOfIndex2() {
        testOutOfBoundsGeneric(4, 3, "7.16", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.BIT, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithBitOK() {
        testOutOfBoundsGeneric(4, 3, "6.0", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.BIT, ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithBitOK2() {
        testOutOfBoundsGeneric(4, 3, "6.15", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.BIT, ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithInt8OutOfIndex() {
        testOutOfBoundsGeneric(4, 3, "8.0", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT8, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithInt8OutOfIndex2() {
        testOutOfBoundsGeneric(4, 3, "7.2", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT8, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithInt8OK() {
        testOutOfBoundsGeneric(4, 3, "6.0", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT8, ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithInt8OK2() {
        testOutOfBoundsGeneric(4, 3, "6.1", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT8, ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithInt16OK() {
        testOutOfBoundsGeneric(4, 3, "6", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT16, ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithInt16OutOfBounds() {
        testOutOfBoundsGeneric(4, 3, "8", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT16, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithInt16NoDecimalFormatAllowed() {
        testOutOfBoundsGeneric(4, 3, "7.0", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT16, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithInt32OK() {
        testOutOfBoundsGeneric(4, 3, "5", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT32, ThingStatus.ONLINE);
    }

    @Test
    public void testInitRegistersWithInt32OutOfBounds() {
        testOutOfBoundsGeneric(4, 3, "6", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT32, ThingStatus.OFFLINE);
    }

    @Test
    public void testInitRegistersWithInt32AtTheEdge() {
        testOutOfBoundsGeneric(4, 3, "5", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusConstants.ValueType.INT32, ThingStatus.ONLINE);
    }

    private ModbusDataThingHandler testReadHandlingGeneric(ModbusReadFunctionCode functionCode, String start,
            String transform, ValueType valueType, BitArray bits, ModbusRegisterArray registers, Exception error) {
        return testReadHandlingGeneric(functionCode, start, transform, valueType, bits, registers, error, null);
    }

    private ModbusDataThingHandler testReadHandlingGeneric(ModbusReadFunctionCode functionCode, String start,
            String transform, ValueType valueType, BitArray bits, ModbusRegisterArray registers, Exception error,
            BundleContext context) {
        return testReadHandlingGeneric(functionCode, start, transform, valueType, bits, registers, error, context,
                true);
    }

    @SuppressWarnings({ "null" })
    private ModbusDataThingHandler testReadHandlingGeneric(ModbusReadFunctionCode functionCode, String start,
            String transform, ValueType valueType, BitArray bits, ModbusRegisterArray registers, Exception error,
            BundleContext context, boolean autoCreateItemsAndLinkToChannels) {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        int pollLength = 3;

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        doReturn(pollLength).when(request).getDataLength();
        doReturn(functionCode).when(request).getFunctionCode();

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Bridge poller = createPollerMock("poller1", task);

        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", start);
        dataConfig.put("readTransform", transform);
        dataConfig.put("readValueType", valueType.getConfigValue());

        String thingId = "read1";
        ModbusDataThingHandler dataHandler = createDataHandler(thingId, poller,
                builder -> builder.withConfiguration(dataConfig), context, autoCreateItemsAndLinkToChannels);

        assertThat(dataHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));

        // call callbacks
        if (bits != null) {
            assertNull(registers);
            assertNull(error);
            AsyncModbusReadResult result = new AsyncModbusReadResult(request, bits);
            dataHandler.handle(result);
        } else if (registers != null) {
            assertNull(bits);
            assertNull(error);
            AsyncModbusReadResult result = new AsyncModbusReadResult(request, registers);
            dataHandler.handle(result);
        } else {
            assertNull(bits);
            assertNull(registers);
            assertNotNull(error);
            AsyncModbusFailure<ModbusReadRequestBlueprint> result = new AsyncModbusFailure<ModbusReadRequestBlueprint>(
                    request, error);
            dataHandler.handleReadError(result);
        }
        return dataHandler;
    }

    @SuppressWarnings({ "null" })
    private ModbusDataThingHandler testWriteHandlingGeneric(String start, String transform, ValueType valueType,
            String writeType, ModbusWriteFunctionCode successFC, String channel, Command command, Exception error,
            BundleContext context) {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Bridge poller = createPollerMock("poller1", task);

        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "");
        dataConfig.put("writeStart", start);
        dataConfig.put("writeTransform", transform);
        dataConfig.put("writeValueType", valueType.getConfigValue());
        dataConfig.put("writeType", writeType);

        String thingId = "write";

        ModbusDataThingHandler dataHandler = createDataHandler(thingId, poller,
                builder -> builder.withConfiguration(dataConfig), context);

        assertThat(dataHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));

        dataHandler.handleCommand(new ChannelUID(dataHandler.getThing().getUID(), channel), command);

        if (error != null) {
            dataHandler.handleReadError(new AsyncModbusFailure<ModbusReadRequestBlueprint>(request, error));
        } else {
            ModbusResponse resp = new ModbusResponse() {

                @Override
                public int getFunctionCode() {
                    return successFC.getFunctionCode();
                }
            };
            dataHandler
                    .onWriteResponse(new AsyncModbusWriteResult(Mockito.mock(ModbusWriteRequestBlueprint.class), resp));
        }
        return dataHandler;
    }

    @Test
    public void testOnError() {
        ModbusDataThingHandler dataHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                "0.0", "default", ModbusConstants.ValueType.BIT, null, null, new Exception("fooerror"));

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_ERROR, is(notNullValue(State.class)));
    }

    @Test
    public void testOnRegistersInt16StaticTransformation() {
        ModbusDataThingHandler dataHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                "0", "-3", ModbusConstants.ValueType.INT16, null,
                new ModbusRegisterArray(new ModbusRegister[] { new ModbusRegister((byte) 0xff, (byte) 0xfd) }), null);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_ERROR, is(nullValue(State.class)));

        // -3 converts to "true"
        assertSingleStateUpdate(dataHandler, CHANNEL_CONTACT, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_SWITCH, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_DIMMER, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_NUMBER, new DecimalType(-3));
        // roller shutter fails since -3 is invalid value (not between 0...100)
        // assertThatStateContains(state, CHANNEL_ROLLERSHUTTER, new PercentType(1));
        assertSingleStateUpdate(dataHandler, CHANNEL_STRING, new StringType("-3"));
        // no datetime, conversion not possible without transformation
    }

    @Test
    public void testOnRegistersRealTransformation() {
        mockTransformation("MULTIPLY", new MultiplyTransformation());
        ModbusDataThingHandler dataHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                "0", "MULTIPLY(10)", ModbusConstants.ValueType.INT16, null,
                new ModbusRegisterArray(new ModbusRegister[] { new ModbusRegister((byte) 0xff, (byte) 0xfd) }), null,
                bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_ERROR, is(nullValue(State.class)));

        // transformation output (-30) is not valid for contact or switch
        assertSingleStateUpdate(dataHandler, CHANNEL_CONTACT, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_SWITCH, is(nullValue(State.class)));
        // -30 is not valid value for Dimmer (PercentType) (not between 0...100)
        assertSingleStateUpdate(dataHandler, CHANNEL_DIMMER, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_NUMBER, new DecimalType(-30));
        // roller shutter fails since -3 is invalid value (not between 0...100)
        assertSingleStateUpdate(dataHandler, CHANNEL_ROLLERSHUTTER, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_STRING, new StringType("-30"));
        // no datetime, conversion not possible without transformation
    }

    @Test
    public void testOnRegistersNaNFloatInRegisters() throws InvalidSyntaxException {
        ModbusDataThingHandler dataHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                "0", "default", ModbusConstants.ValueType.FLOAT32, null, new ModbusRegisterArray(
                        // equivalent of floating point NaN
                        new ModbusRegister[] { new ModbusRegister((byte) 0x7f, (byte) 0xc0),
                                new ModbusRegister((byte) 0x00, (byte) 0x00) }),
                null, bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_ERROR, is(nullValue(State.class)));

        // UNDEF is treated as "boolean true" (OPEN/ON) since it is != 0.
        assertSingleStateUpdate(dataHandler, CHANNEL_CONTACT, OpenClosedType.OPEN);
        assertSingleStateUpdate(dataHandler, CHANNEL_SWITCH, OnOffType.ON);
        assertSingleStateUpdate(dataHandler, CHANNEL_DIMMER, OnOffType.ON);
        assertSingleStateUpdate(dataHandler, CHANNEL_NUMBER, UnDefType.UNDEF);
        assertSingleStateUpdate(dataHandler, CHANNEL_ROLLERSHUTTER, UnDefType.UNDEF);
        assertSingleStateUpdate(dataHandler, CHANNEL_STRING, UnDefType.UNDEF);
    }

    @Test
    public void testOnRegistersRealTransformation2() throws InvalidSyntaxException {
        mockTransformation("ONOFF", new TransformationService() {

            @Override
            public String transform(String function, String source) throws TransformationException {
                return Integer.parseInt(source) != 0 ? "ON" : "OFF";
            }
        });
        ModbusDataThingHandler dataHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                "0", "ONOFF(10)", ModbusConstants.ValueType.INT16, null,
                new ModbusRegisterArray(new ModbusRegister[] { new ModbusRegister((byte) 0xff, (byte) 0xfd) }), null,
                bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_READ_ERROR, is(nullValue(State.class)));

        assertSingleStateUpdate(dataHandler, CHANNEL_CONTACT, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_SWITCH, is(equalTo(OnOffType.ON)));
        assertSingleStateUpdate(dataHandler, CHANNEL_DIMMER, is(equalTo(OnOffType.ON)));
        assertSingleStateUpdate(dataHandler, CHANNEL_NUMBER, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_ROLLERSHUTTER, is(nullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_STRING, is(equalTo(new StringType("ON"))));
    }

    @Test
    public void testWriteRealTransformation() throws InvalidSyntaxException {
        captureModbusWrites();
        mockTransformation("MULTIPLY", new MultiplyTransformation());
        ModbusDataThingHandler dataHandler = testWriteHandlingGeneric("50", "MULTIPLY(10)",
                ModbusConstants.ValueType.BIT, "coil", ModbusWriteFunctionCode.WRITE_COIL, "number",
                new DecimalType("2"), null, bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_ERROR, is(nullValue(State.class)));
        assertThat(writeRequests.size(), is(equalTo(1)));
        ModbusWriteRequestBlueprint writeRequest = writeRequests.get(0);
        assertThat(writeRequest.getFunctionCode(), is(equalTo(ModbusWriteFunctionCode.WRITE_COIL)));
        assertThat(writeRequest.getReference(), is(equalTo(50)));
        assertThat(((ModbusWriteCoilRequestBlueprint) writeRequest).getCoils().size(), is(equalTo(1)));
        // Since transform output is non-zero, it is mapped as "true"
        assertThat(((ModbusWriteCoilRequestBlueprint) writeRequest).getCoils().getBit(0), is(equalTo(true)));
    }

    @Test
    public void testWriteRealTransformation2() throws InvalidSyntaxException {
        captureModbusWrites();
        mockTransformation("ZERO", new TransformationService() {

            @Override
            public String transform(String function, String source) throws TransformationException {
                return "0";
            }
        });
        ModbusDataThingHandler dataHandler = testWriteHandlingGeneric("50", "ZERO(foobar)",
                ModbusConstants.ValueType.BIT, "coil", ModbusWriteFunctionCode.WRITE_COIL, "number",
                new DecimalType("2"), null, bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_ERROR, is(nullValue(State.class)));
        assertThat(writeRequests.size(), is(equalTo(1)));
        ModbusWriteRequestBlueprint writeRequest = writeRequests.get(0);
        assertThat(writeRequest.getFunctionCode(), is(equalTo(ModbusWriteFunctionCode.WRITE_COIL)));
        assertThat(writeRequest.getReference(), is(equalTo(50)));
        assertThat(((ModbusWriteCoilRequestBlueprint) writeRequest).getCoils().size(), is(equalTo(1)));
        // Since transform output is zero, it is mapped as "false"
        assertThat(((ModbusWriteCoilRequestBlueprint) writeRequest).getCoils().getBit(0), is(equalTo(false)));
    }

    @Test
    public void testWriteRealTransformation3() throws InvalidSyntaxException {
        captureModbusWrites();
        mockTransformation("RANDOM", new TransformationService() {

            @Override
            public String transform(String function, String source) throws TransformationException {
                return "5";
            }
        });
        ModbusDataThingHandler dataHandler = testWriteHandlingGeneric("50", "RANDOM(foobar)",
                ModbusConstants.ValueType.INT16, "holding", ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER, "number",
                new DecimalType("2"), null, bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_ERROR, is(nullValue(State.class)));
        assertThat(writeRequests.size(), is(equalTo(1)));
        ModbusWriteRequestBlueprint writeRequest = writeRequests.get(0);
        assertThat(writeRequest.getFunctionCode(), is(equalTo(ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER)));
        assertThat(writeRequest.getReference(), is(equalTo(50)));
        assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().size(), is(equalTo(1)));
        assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().getRegister(0).getValue(),
                is(equalTo(5)));
    }

    @Test
    public void testWriteRealTransformation4() throws InvalidSyntaxException {
        captureModbusWrites();
        mockTransformation("JSON", new TransformationService() {

            @Override
            public String transform(String function, String source) throws TransformationException {
                return "[{"//
                        + "\"functionCode\": 16,"//
                        + "\"address\": 5412,"//
                        + "\"value\": [1, 0, 5]"//
                        + "},"//
                        + "{"//
                        + "\"functionCode\": 6,"//
                        + "\"address\": 555,"//
                        + "\"value\": [3]"//
                        + "}]";
            }
        });
        ModbusDataThingHandler dataHandler = testWriteHandlingGeneric("50", "JSON(foobar)",
                ModbusConstants.ValueType.INT16, "holding", ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS, "number",
                new DecimalType("2"), null, bundleContext);

        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_SUCCESS, is(notNullValue(State.class)));
        assertSingleStateUpdate(dataHandler, CHANNEL_LAST_WRITE_ERROR, is(nullValue(State.class)));
        assertThat(writeRequests.size(), is(equalTo(2)));
        {
            ModbusWriteRequestBlueprint writeRequest = writeRequests.get(0);
            assertThat(writeRequest.getFunctionCode(), is(equalTo(ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS)));
            assertThat(writeRequest.getReference(), is(equalTo(5412)));
            assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().size(), is(equalTo(3)));
            assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().getRegister(0).getValue(),
                    is(equalTo(1)));
            assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().getRegister(1).getValue(),
                    is(equalTo(0)));
            assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().getRegister(2).getValue(),
                    is(equalTo(5)));
        }
        {
            ModbusWriteRequestBlueprint writeRequest = writeRequests.get(1);
            assertThat(writeRequest.getFunctionCode(), is(equalTo(ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER)));
            assertThat(writeRequest.getReference(), is(equalTo(555)));
            assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().size(), is(equalTo(1)));
            assertThat(((ModbusWriteRegisterRequestBlueprint) writeRequest).getRegisters().getRegister(0).getValue(),
                    is(equalTo(3)));
        }
    }

    private void testValueTypeGeneric(ModbusReadFunctionCode functionCode, ValueType valueType,
            ThingStatus expectedStatus) {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        doReturn(3).when(request).getDataLength();
        doReturn(functionCode).when(request).getFunctionCode();

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Bridge poller = createPollerMock("poller1", task);

        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "1");
        dataConfig.put("readTransform", "default");
        dataConfig.put("readValueType", valueType.getConfigValue());
        ModbusDataThingHandler dataHandler = createDataHandler("data1", poller,
                builder -> builder.withConfiguration(dataConfig));
        assertThat(dataHandler.getThing().getStatus(), is(equalTo(expectedStatus)));
    }

    @Test
    public void testCoilDoesNotAcceptFloat32ValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_COILS, ModbusConstants.ValueType.FLOAT32, ThingStatus.OFFLINE);
    }

    @Test
    public void testCoilAcceptsBitValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_COILS, ModbusConstants.ValueType.BIT, ThingStatus.ONLINE);
    }

    @Test
    public void testDiscreteInputDoesNotAcceptFloat32ValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_INPUT_DISCRETES, ModbusConstants.ValueType.FLOAT32,
                ThingStatus.OFFLINE);
    }

    @Test
    public void testDiscreteInputAcceptsBitValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_INPUT_DISCRETES, ModbusConstants.ValueType.BIT,
                ThingStatus.ONLINE);
    }

    @Test
    public void testRefreshOnData() throws InterruptedException {
        ModbusReadFunctionCode functionCode = ModbusReadFunctionCode.READ_COILS;

        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        int pollLength = 3;

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        doReturn(pollLength).when(request).getDataLength();
        doReturn(functionCode).when(request).getFunctionCode();

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Bridge poller = createPollerMock("poller1", task);

        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0");
        dataConfig.put("readTransform", "default");
        dataConfig.put("readValueType", "bit");

        String thingId = "read1";

        ModbusDataThingHandler dataHandler = createDataHandler(thingId, poller,
                builder -> builder.withConfiguration(dataConfig), bundleContext);
        assertThat(dataHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verify(comms, never()).submitOneTimePoll(request, notNull(), notNull());
        // Reset initial REFRESH commands to data thing channels from the Core
        reset(poller.getHandler());
        dataHandler.handleCommand(Mockito.mock(ChannelUID.class), RefreshType.REFRESH);

        // data handler asynchronously calls the poller.refresh() -- it might take some time
        // We check that refresh is finally called
        waitForAssert(() -> verify((ModbusPollerThingHandler) poller.getHandler()).refresh(), 2500, 50);
    }

    /**
     *
     * @param pollerFunctionCode poller function code. Use null if you want to have data thing direct child of endpoint
     *            thing
     * @param config thing config
     * @param statusConsumer assertion method for data thingstatus
     */
    private void testInitGeneric(ModbusReadFunctionCode pollerFunctionCode, Configuration config,
            Consumer<ThingStatusInfo> statusConsumer) {
        int pollLength = 3;

        Bridge parent;
        if (pollerFunctionCode == null) {
            parent = createTcpMock();
            ThingHandler foo = parent.getHandler();
            addThing(parent);
        } else {
            ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

            // Minimally mocked request
            ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
            doReturn(pollLength).when(request).getDataLength();
            doReturn(pollerFunctionCode).when(request).getFunctionCode();

            PollTask task = Mockito.mock(PollTask.class);
            doReturn(endpoint).when(task).getEndpoint();
            doReturn(request).when(task).getRequest();

            parent = createPollerMock("poller1", task);
        }

        String thingId = "read1";

        ModbusDataThingHandler dataHandler = createDataHandler(thingId, parent,
                builder -> builder.withConfiguration(config), bundleContext);

        statusConsumer.accept(dataHandler.getThing().getStatusInfo());
    }

    @Test
    public void testReadOnlyData() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0");
        dataConfig.put("readValueType", "bit");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig,
                status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    /**
     * readValueType=bit should be assumed with coils, so it's ok to skip it
     */
    @Test
    public void testReadOnlyDataMissingValueTypeWithCoils() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0");
        // missing value type
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig,
                status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testReadOnlyDataInvalidValueType() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0");
        dataConfig.put("readValueType", "foobar");
        testInitGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    /**
     * We do not assume value type with registers, not ok to skip it
     */
    @Test
    public void testReadOnlyDataMissingValueTypeWithRegisters() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0");
        testInitGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteOnlyData() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeValueType", "bit");
        dataConfig.put("writeType", "coil");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig,
                status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testWriteHoldingInt16Data() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeValueType", "int16");
        dataConfig.put("writeType", "holding");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig,
                status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testWriteHoldingInt8Data() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeValueType", "int8");
        dataConfig.put("writeType", "holding");
        testInitGeneric(null, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteHoldingBitData() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeValueType", "bit");
        dataConfig.put("writeType", "holding");
        testInitGeneric(null, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteOnlyDataChildOfEndpoint() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeValueType", "bit");
        dataConfig.put("writeType", "coil");
        testInitGeneric(null, dataConfig, status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testWriteOnlyDataMissingOneParameter() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeValueType", "bit");
        // missing writeType --> error
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
            assertThat(status.getDescription(), is(not(equalTo(null))));
        });
    }

    /**
     * OK to omit writeValueType with coils since bit is assumed
     */
    @Test
    public void testWriteOnlyDataMissingValueTypeWithCoilParameter() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeType", "coil");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig,
                status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testWriteOnlyIllegalValueType() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeType", "coil");
        dataConfig.put("writeValueType", "foobar");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteInvalidType() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0");
        dataConfig.put("writeType", "foobar");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteCoilBadStart() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0.4");
        dataConfig.put("writeType", "coil");
        testInitGeneric(null, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteHoldingBadStart() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("writeStart", "0.4");
        dataConfig.put("writeType", "holding");
        testInitGeneric(null, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testReadHoldingBadStart() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0.0");
        dataConfig.put("readValueType", "int16");
        testInitGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testReadHoldingBadStart2() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0.0");
        dataConfig.put("readValueType", "bit");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testReadHoldingOKStart() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0.0");
        dataConfig.put("readType", "holding");
        dataConfig.put("readValueType", "bit");
        testInitGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, dataConfig,
                status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testReadValueTypeIllegal() {
        Configuration dataConfig = new Configuration();
        dataConfig.put("readStart", "0.0");
        dataConfig.put("readType", "holding");
        dataConfig.put("readValueType", "foobar");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteOnlyTransform() {
        Configuration dataConfig = new Configuration();
        // no need to have start, JSON output of transformation defines everything
        dataConfig.put("writeTransform", "JS(myJsonTransform.js)");
        testInitGeneric(null, dataConfig, status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }

    @Test
    public void testWriteTransformAndStart() {
        Configuration dataConfig = new Configuration();
        // It's illegal to have start and transform. Just have transform or have all
        dataConfig.put("writeStart", "3");
        dataConfig.put("writeTransform", "JS(myJsonTransform.js)");
        testInitGeneric(ModbusReadFunctionCode.READ_COILS, dataConfig, status -> {
            assertThat(status.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
            assertThat(status.getStatusDetail(), is(equalTo(ThingStatusDetail.CONFIGURATION_ERROR)));
        });
    }

    @Test
    public void testWriteTransformAndNecessary() {
        Configuration dataConfig = new Configuration();
        // It's illegal to have start and transform. Just have transform or have all
        dataConfig.put("writeStart", "3");
        dataConfig.put("writeType", "holding");
        dataConfig.put("writeValueType", "int16");
        dataConfig.put("writeTransform", "JS(myJsonTransform.js)");
        testInitGeneric(null, dataConfig, status -> assertThat(status.getStatus(), is(equalTo(ThingStatus.ONLINE))));
    }
}
