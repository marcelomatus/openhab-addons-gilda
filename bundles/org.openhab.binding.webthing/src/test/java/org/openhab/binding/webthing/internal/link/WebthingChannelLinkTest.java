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
package org.openhab.binding.webthing.internal.link;

import static org.junit.jupiter.api.Assertions.*;

import org.openhab.core.library.types.*;
import org.openhab.core.thing.*;

/**
 * Mapping test.
 *
 * Please consider that changes of 'ItemType<->PropertyType mapping' validated by this test
 * will break the compatibility to former releases.
 *
 * @author Gregor Roth - Initial contribution
 */
public class WebthingChannelLinkTest {

    /*
     * @Test
     * public void testChannelToProperty() throws Exception {
     * HttpClientMock httpClientMock = new HttpClientMock();
     * httpClientMock.onGet("http://example.org:8090/0").doReturn(load("/awning_response.json"));
     * httpClientMock.onGet("http://example.org:8090/0/properties/target_position")
     * .doReturn(load("/awning_property.json"));
     * httpClientMock.onPut("http://example.org:8090/0/properties/target_position")
     * .withBody(is("{\"target_position\":10}")).doReturnStatus(200);
     * httpClientMock.onPut("http://example.org:8090/0/properties/target_position")
     * .withBody(is("{\"target_position\":130}")).doReturnStatus(500);
     * 
     * var thingUID = new ThingUID("webthing", "anwing");
     * var channelUID = Channels.createChannelUID(thingUID, "target_position");
     * 
     * var webthing = WebthingTest.createTestWebthing("http://example.org:8090/0", null);
     * var channel = Channels.createChannel(thingUID, "target_position",
     * Objects.requireNonNull(webthing.getPropertyDescription("target_position")));
     * 
     * var testWebthingThingHandler = new TestWebthingThingHandler();
     * ChannelToPropertyLink.establish(testWebthingThingHandler, channel, webthing, "target_position");
     * 
     * testWebthingThingHandler.listeners.get(channelUID).onItemStateChanged(channelUID, new DecimalType(10));
     * 
     * testWebthingThingHandler.listeners.get(channelUID).onItemStateChanged(channelUID, new DecimalType(130));
     * }
     * 
     * @Test
     * public void testPropertyToChannel() throws Exception {
     * HttpClientMock httpClientMock = new HttpClientMock();
     * httpClientMock.onGet("http://example.org:8090/0").doReturn(load("/awning_response.json"));
     * httpClientMock.onGet("http://example.org:8090/0/properties/target_position")
     * .doReturn(load("/awning_property.json"));
     * httpClientMock.onPut("http://example.org:8090/0/properties/target_position")
     * .withBody(is("{\"target_position\":10}")).doReturnStatus(200);
     * 
     * var thingUID = new ThingUID("webthing", "anwing");
     * var channelUID = Channels.createChannelUID(thingUID, "target_position");
     * 
     * var errorHandler = new WebthingTest.ErrorHandler();
     * var websocketConnectionFactory = new WebthingTest.TestWebsocketConnectionFactory();
     * var webthing = WebthingTest.createTestWebthing("http://example.org:8090/0", null, errorHandler,
     * websocketConnectionFactory);
     * var channel = Channels.createChannel(thingUID, "target_position",
     * Objects.requireNonNull(webthing.getPropertyDescription("target_position")));
     * 
     * var testWebthingThingHandler = new TestWebthingThingHandler();
     * PropertyToChannelLink.establish(webthing, "target_position", testWebthingThingHandler, channel);
     * 
     * var message = new PropertyStatusMessage();
     * message.messageType = "propertyStatus";
     * message.data = Map.of("target_position", 77);
     * websocketConnectionFactory.webSocketRef.get().sendToClient(message);
     * 
     * assertEquals(new DecimalType(77), testWebthingThingHandler.itemState.get(channelUID));
     * }
     * 
     * private static final class TestConsumer implements BiConsumer<ChannelUID, Command> {
     * public final AtomicReference<ChannelUID> channelUidRef = new AtomicReference<>();
     * public final AtomicReference<Command> commandRef = new AtomicReference<>();
     * 
     * @Override
     * public void accept(ChannelUID channelUID, Command command) {
     * channelUidRef.set(channelUID);
     * commandRef.set(command);
     * }
     * }
     * 
     * @Test
     * public void testDataTypeMapping() throws Exception {
     * 
     * performDataTypeMappingTest("level_prop", 56.5, new DecimalType(56.5), 3.5, new DecimalType(3.5));
     * performDataTypeMappingTest("level_unit_prop", 10, new PercentType(10), 90, new PercentType(90));
     * performDataTypeMappingTest("thermo_prop", "off", new StringType("off"), "auto", new StringType("auto"));
     * performDataTypeMappingTest("temp_prop", 18.6, new DecimalType(18.6), 23.2, new DecimalType(23.2));
     * performDataTypeMappingTest("targettemp_prop", 18.6, new DecimalType(18.6), 23.2, new DecimalType(23.2));
     * performDataTypeMappingTest("open_prop", true, OpenClosedType.OPEN, false, OpenClosedType.CLOSED);
     * performDataTypeMappingTest("colortemp_prop", 10, new PercentType(10), 60, new PercentType(60));
     * performDataTypeMappingTest("color_prop", "#f2fe00", new HSBType("62,100,99"), "#ff0000",
     * new HSBType("0.0,100.0,100.0"));
     * performDataTypeMappingTest("colormode_prop", "color", new StringType("color"), "temperature",
     * new StringType("temperature"));
     * performDataTypeMappingTest("brightness_prop", 33, new PercentType(33), 65, new PercentType(65));
     * performDataTypeMappingTest("voltage_prop", 4.5, new DecimalType(4.5), 30.2, new DecimalType(30.2));
     * performDataTypeMappingTest("heating_prop", "off", new StringType("off"), "cooling", new StringType("cooling"));
     * performDataTypeMappingTest("onoff_prop", true, OnOffType.ON, false, OnOffType.OFF);
     * performDataTypeMappingTest("string_prop", "initial", new StringType("initial"), "updated",
     * new StringType("updated"));
     * performDataTypeMappingTest("number_prop", 80.5, new DecimalType(80.5), 60.9, new DecimalType(60.9));
     * performDataTypeMappingTest("integer_prop", 11, new DecimalType(11), 77, new DecimalType(77));
     * performDataTypeMappingTest("boolean_prop", true, OnOffType.ON, false, OnOffType.OFF);
     * }
     * 
     * private void performDataTypeMappingTest(String propertyName, Object initialValue, State initialState,
     * Object updatedValue, State updatedState) throws Exception {
     * HttpClientMock httpClientMock = new HttpClientMock();
     * httpClientMock.onGet("http://example.org:8090/").doReturn(load("/datatypes_test_response.json"));
     * httpClientMock.onGet("http://example.org:8090/properties/" + propertyName)
     * .doReturn(new Gson().toJson(Map.of(propertyName, initialValue)));
     * var json = new Gson().toJson(Map.of(propertyName, updatedValue));
     * httpClientMock.onPut("http://example.org:8090/properties/" + propertyName).withBody(is(json))
     * .doReturnStatus(200);
     * 
     * var thingUID = new ThingUID("webthing", "test");
     * var channelUID = Channels.createChannelUID(thingUID, propertyName);
     * 
     * var errorHandler = new WebthingTest.ErrorHandler();
     * var websocketConnectionFactory = new WebthingTest.TestWebsocketConnectionFactory();
     * var webthing = WebthingTest.createTestWebthing("http://example.org:8090/", null, errorHandler,
     * websocketConnectionFactory);
     * var channel = Channels.createChannel(thingUID, propertyName,
     * Objects.requireNonNull(webthing.getPropertyDescription(propertyName)));
     * 
     * var testWebthingThingHandler = new TestWebthingThingHandler();
     * 
     * PropertyToChannelLink.establish(webthing, propertyName, testWebthingThingHandler, channel);
     * 
     * var message = new PropertyStatusMessage();
     * message.messageType = "propertyStatus";
     * message.data = Map.of(propertyName, initialValue);
     * websocketConnectionFactory.webSocketRef.get().sendToClient(message);
     * 
     * assertEquals(initialState, testWebthingThingHandler.itemState.get(channelUID));
     * 
     * ChannelToPropertyLink.establish(testWebthingThingHandler, channel, webthing, propertyName);
     * testWebthingThingHandler.listeners.get(channelUID).onItemStateChanged(channelUID, updatedState);
     * }
     * 
     * public static String load(String name) throws Exception {
     * return new String(Files.readAllBytes(Paths.get(WebthingTest.class.getResource(name).toURI())));
     * }
     * 
     * private static class TestWebthingThingHandler implements ChannelHandler {
     * public final Map<ChannelUID, ItemChangedListener> listeners = new ConcurrentHashMap<>();
     * public final Map<ChannelUID, Command> itemState = new ConcurrentHashMap<>();
     * 
     * @Override
     * public void observeChannel(ChannelUID channelUID, ItemChangedListener listener) {
     * listeners.put(channelUID, listener);
     * }
     * 
     * @Override
     * public void updateItemState(ChannelUID channelUID, Command command) {
     * itemState.put(channelUID, command);
     * }
     * }
     */
}
