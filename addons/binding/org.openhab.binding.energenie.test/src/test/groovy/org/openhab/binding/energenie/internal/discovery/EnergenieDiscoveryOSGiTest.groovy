/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.energenie.internal.discovery

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import static org.openhab.binding.energenie.test.AbstractEnergenieOSGiTest.*

import org.eclipse.smarthome.config.discovery.DiscoveryListener
import org.eclipse.smarthome.config.discovery.DiscoveryResult
import org.eclipse.smarthome.config.discovery.DiscoveryService
import org.eclipse.smarthome.core.thing.Bridge
import org.eclipse.smarthome.core.thing.Thing
import org.eclipse.smarthome.core.thing.ThingRegistry
import org.eclipse.smarthome.core.thing.ThingTypeUID
import org.eclipse.smarthome.core.thing.ThingUID
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openhab.binding.energenie.EnergenieBindingConstants
import org.openhab.binding.energenie.handler.EnergenieGatewayHandler
import org.openhab.binding.energenie.handler.EnergenieSubdevicesHandler
import org.openhab.binding.energenie.internal.api.EnergenieDeviceTypes
import org.openhab.binding.energenie.internal.api.JsonDevice
import org.openhab.binding.energenie.internal.api.JsonGateway
import org.openhab.binding.energenie.internal.api.JsonSubdevice
import org.openhab.binding.energenie.internal.api.manager.EnergenieApiConfiguration
import org.openhab.binding.energenie.internal.api.manager.EnergenieApiManager
/**
 * Tests for the {@link EnergenieDiscoveryService}
 *
 * @author Svilen Valkanov
 * @author Mihaela Memova
 *
 */
class EnergenieDiscoveryOSGiTest {
    // MiHome internal API test data
    private static final int TEST_GATEWAY_ID = 4541
    private static final String TEST_GATEWAY_LABEL = "TestGateway"
    private static final int TEST_DEVICE_ID = 51816
    private static final EnergenieDeviceTypes TEST_DEVICE_TYPE = EnergenieDeviceTypes.MOTION_SENSOR

    //ESH test data
    private static final ThingTypeUID TEST_THING_TYPE_UID = EnergenieBindingConstants.THING_TYPE_MOTION_SENSOR
    private static final ThingUID TEST_THING_UID = new ThingUID(TEST_THING_TYPE_UID,Integer.toString(TEST_DEVICE_ID))
    private static final ThingTypeUID TEST_GATEWAY_TYPE_UID = EnergenieBindingConstants.THING_TYPE_GATEWAY
    private static final ThingUID TEST_GATEWAY_UID = new ThingUID(TEST_GATEWAY_TYPE_UID, Integer.toString(TEST_GATEWAY_ID))

    private EnergenieDiscoveryService discoveryService;

    private boolean isResultExpected = false
    private JsonDevice[] listGatewaysResponse
    private JsonDevice[] listSubdevicesResponse
    private List<Thing> registeredThings = new ArrayList<Thing>();


    EnergenieApiManager mockedApiManager = [
        listGateways : {return listGatewaysResponse },
        listSubdevices : { return listSubdevicesResponse },
        getConfiguration : { return new EnergenieApiConfiguration(TEST_USERNAME, TEST_PASSWORD) }
    ] as EnergenieApiManager

    ThingRegistry thingRegistryMock = [
        getAll:{ -> return registeredThings}
    ] as ThingRegistry

    @Before
    public void setUp() {
        discoveryService = new EnergenieDiscoveryService(mockedApiManager, thingRegistryMock)
    }

    @After
    public void tearDown(){
        registeredThings.clear()
        listSubdevicesResponse = null
        listGatewaysResponse = null
        isResultExpected = false
    }
    @Test
    public void 'create result for gateway without a Thing'(){
        // Set up the backend response
        listSubdevicesResponse = new JsonSubdevice[0]
        listGatewaysResponse = new JsonGateway[1];
        listGatewaysResponse[0] = new JsonGateway(TEST_GATEWAY_ID, TEST_GATEWAY_LABEL)

        boolean expectDiscoveryResult = true
        DiscoveryListener discoveryListenerMock = [
            thingDiscovered : { DiscoveryService source, DiscoveryResult result ->
                if(expectDiscoveryResult) {
                    assertGatewayDiscoveryResult(result)
                    // Only one result is expected
                    expectDiscoveryResult = false;
                } else {
                    fail("Unexpected result {}",result.toString())
                }
            }
        ] as DiscoveryListener

        discoveryService.addDiscoveryListener(discoveryListenerMock)
        discoveryService.startScan();

        assertThat expectDiscoveryResult, is(false)
    }

    @Test
    public void 'dont create a result when no gateway is found' () {
        // Set up the backend response
        listSubdevicesResponse = new JsonSubdevice[0]
        listGatewaysResponse = new JsonGateway[0];

        DiscoveryListener discoveryListenerMock = [
            thingDiscovered : { DiscoveryService source, DiscoveryResult result ->
                fail("Unexpected Discovery Result {}", result.toString())
            }
        ] as DiscoveryListener

        discoveryService.addDiscoveryListener(discoveryListenerMock)
        discoveryService.startScan();
    }

    @Test
    public void 'dont create result for gateway with a Thing'() {
        // Add a bridge to the registry
        Bridge bridge = BridgeBuilder.create(TEST_GATEWAY_TYPE_UID,TEST_GATEWAY_UID).
                withProperties([deviceID:Integer.toString(TEST_GATEWAY_ID)]).build();
        registeredThings.add(bridge)

        // Set up the backend response
        JsonGateway gateway = new JsonGateway()
        listGatewaysResponse = new JsonGateway[1];
        listGatewaysResponse[0] = new JsonGateway(TEST_GATEWAY_ID, TEST_GATEWAY_LABEL)

        // Add the listener
        DiscoveryListener discoveryListenerMock = [
            thingDiscovered : { DiscoveryService source, DiscoveryResult result ->
                fail("Unexpected Discovery Result")
            }
        ] as DiscoveryListener

        discoveryService.startScan();
    }

    @Test
    public void 'create result for subdevice without a Thing'(){
        // Add a bridge to the registry
        Bridge bridge = BridgeBuilder.create(TEST_GATEWAY_TYPE_UID,TEST_GATEWAY_UID).
                withProperties([deviceID:Integer.toString(TEST_GATEWAY_ID)]).build();
        registeredThings.add(bridge)

        // Set up the backend response
        listSubdevicesResponse = new JsonSubdevice[1]
        listSubdevicesResponse[0] = new JsonSubdevice(TEST_DEVICE_ID, TEST_GATEWAY_ID, TEST_DEVICE_TYPE)
        listGatewaysResponse = new JsonGateway[0]

        boolean expectDiscoveryResult = true
        DiscoveryListener discoveryListenerMock = [
            thingDiscovered : { DiscoveryService source, DiscoveryResult result ->
                if(expectDiscoveryResult) {
                    assertSubdeviceDiscoveryResult(result)
                    // Only one result is expected
                    expectDiscoveryResult = false;
                } else {
                    fail("Unexpected result {}",result.toString())
                }
            }
        ] as DiscoveryListener

        discoveryService.addDiscoveryListener(discoveryListenerMock)
        discoveryService.startScan();

        assertThat expectDiscoveryResult, is(false)
    }

    @Test
    public void 'dont create a result when no subdevice is found' () {
        // Set up the backend response
        listSubdevicesResponse = new JsonSubdevice[0]
        listGatewaysResponse = new JsonGateway[0]

        DiscoveryListener discoveryListenerMock = [
            thingDiscovered : { DiscoveryService source, DiscoveryResult result ->
                fail("Unexpected Discovery Result {}", result.toString())
            }
        ] as DiscoveryListener

        discoveryService.addDiscoveryListener(discoveryListenerMock)
        discoveryService.startScan();
    }

    @Test
    public void 'dont create result for subdevice with a Thing'() {
        // Add a thing to the registry
        Thing thing = ThingBuilder.create(TEST_THING_TYPE_UID, TEST_THING_UID)
                .withProperties([deviceID:Integer.toString(TEST_DEVICE_ID)]).build();
        registeredThings.add(thing)

        // Set up the backend response
        listGatewaysResponse = new JsonGateway[0]
        listSubdevicesResponse = new JsonSubdevice[1]
        listSubdevicesResponse[0] = new JsonSubdevice(TEST_DEVICE_ID,TEST_GATEWAY_ID,TEST_DEVICE_TYPE)

        // Add the listener
        DiscoveryListener discoveryListenerMock = [
            thingDiscovered : { DiscoveryService source, DiscoveryResult result ->
                fail("Unexpected Discovery Result")
            }
        ] as DiscoveryListener

        discoveryService.startScan();
    }

    private assertGatewayDiscoveryResult(DiscoveryResult result){
        assertThat "DiscoveryResult has incorrect ThingUID", result.getThingUID(), is(TEST_GATEWAY_UID)

        Map<String,Object> properties = result.getProperties()
        assertThat "DiscoveryResult has incorrect ${EnergenieBindingConstants.PROPERTY_DEVICE_ID} property", properties.get(EnergenieBindingConstants.PROPERTY_DEVICE_ID), is(equalTo(TEST_GATEWAY_ID))
        assertThat "DiscoveryResult has incorrect ${EnergenieBindingConstants.PROPERTY_TYPE} property", properties.get(EnergenieBindingConstants.PROPERTY_TYPE), is(equalTo(EnergenieDeviceTypes.GATEWAY.toString()))
        assertThat "DiscoveryResult doesn't contain default required configuration paramter ${EnergenieBindingConstants.CONFIG_UPDATE_INTERVAL}", properties.get(EnergenieBindingConstants.CONFIG_UPDATE_INTERVAL), is(equalTo(EnergenieGatewayHandler.DEFAULT_UPDATE_INTERVAL))
    }

    private assertSubdeviceDiscoveryResult(DiscoveryResult result){
        assertThat "DiscoveryResult has incorrect BridgeUID", result.getBridgeUID(), is(TEST_GATEWAY_UID)

        assertThat "DiscoveryResult has incorrect ThingUID", result.getThingUID(), is(TEST_THING_UID)

        Map<String,Object> properties = result.getProperties()
        assertThat "DiscoveryResult has incorrect ${EnergenieBindingConstants.PROPERTY_DEVICE_ID} property", properties.get(EnergenieBindingConstants.PROPERTY_DEVICE_ID), is(equalTo(TEST_DEVICE_ID))
        assertThat "DiscoveryResult has incorrect ${EnergenieBindingConstants.PROPERTY_GATEWAY_ID} property", properties.get(EnergenieBindingConstants.PROPERTY_GATEWAY_ID), is(equalTo(TEST_GATEWAY_ID))
        assertThat "DiscoveryResult has incorrect ${EnergenieBindingConstants.PROPERTY_TYPE} property", properties.get(EnergenieBindingConstants.PROPERTY_TYPE), is(equalTo(TEST_DEVICE_TYPE))
        assertThat "DiscoveryResult doesn't contain default required configuration paramter ${EnergenieBindingConstants.CONFIG_UPDATE_INTERVAL}", properties.get(EnergenieBindingConstants.CONFIG_UPDATE_INTERVAL), is(equalTo(EnergenieSubdevicesHandler.DEFAULT_UPDATE_INTERVAL))
    }
}
