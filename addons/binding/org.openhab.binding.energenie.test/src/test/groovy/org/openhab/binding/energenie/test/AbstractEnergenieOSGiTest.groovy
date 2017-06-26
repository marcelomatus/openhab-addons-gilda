/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.energenie.test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

import java.text.SimpleDateFormat
import java.time.*

import javax.servlet.http.HttpServlet

import org.eclipse.smarthome.config.core.Configuration
import org.eclipse.smarthome.core.thing.Bridge
import org.eclipse.smarthome.core.thing.ManagedThingProvider
import org.eclipse.smarthome.core.thing.Thing
import org.eclipse.smarthome.core.thing.ThingRegistry
import org.eclipse.smarthome.core.thing.ThingTypeUID
import org.eclipse.smarthome.core.thing.ThingUID
import org.eclipse.smarthome.core.thing.binding.BridgeHandler
import org.eclipse.smarthome.core.thing.binding.ThingHandler
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory
import org.eclipse.smarthome.test.OSGiTest
import org.openhab.binding.energenie.EnergenieBindingConstants
import org.openhab.binding.energenie.internal.EnergenieHandlerFactory
import org.openhab.binding.energenie.internal.api.JsonDevice
import org.openhab.binding.energenie.internal.api.JsonGateway
import org.openhab.binding.energenie.internal.api.JsonResponseHandler
import org.openhab.binding.energenie.internal.api.JsonSubdevice
import org.openhab.binding.energenie.internal.api.manager.*
import org.openhab.binding.energenie.internal.rest.RestClient
import org.osgi.service.http.HttpService

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

/**
 * Base class for OSGi Tests
 *
 * @author Svilen Valkanov
 *
 */
public abstract class AbstractEnergenieOSGiTest extends OSGiTest {

    // Server information
    public static final String PROTOCOL = "http"
    public static final String HOST = "localhost"
    public static final int SECURE_PORT = 9090
    public static final String TEST_URL = "${PROTOCOL}://${HOST}:${SECURE_PORT}"

    //Gateway information used for the purposes of the tests
    public static final String TEST_PASSWORD = "pass"
    public static final String TEST_USERNAME = "test@my.com"
    public static final String TEST_GATEWAY_CODE = "FGSG5RSDFS"
    public static final int TEST_UPDATE_INTERVAL = 10

    // Gateway device information
    public static final int TEST_GATEWAY_ID = 4541
    public static final int TEST_USER_ID = 35764;
    public static final String TEST_MAC_ADDRESS = "a0bb3e9013c9";
    public static final String TEST_IP_ADDRESS = "195.24.43.238";
    public static final int TEST_PORT = 49154;
    public static final String TEST_LABEL = "New Gateway";
    public static final String TEST_AUTH_CODE = "a21f913b022d";
    public static final int TEST_FIRMWARE_VERSION = 13;

    /**
     * Test update interval for the subdevices in seconds
     */
    public static final BigDecimal TEST_SUBDEVICE_UPDATE_INTERVAL = 1

    public static final String PATH_CREATE_GATEWAY = "/${EnergenieApiManagerImpl.CONTROLLER_DEVICES}/${EnergenieApiManagerImpl.ACTION_CREATE}"
    public static final String PATH_LIST_GATEWAYS = "/${EnergenieApiManagerImpl.CONTROLLER_DEVICES}/${EnergenieApiManagerImpl.ACTION_LIST}"
    public static final String PATH_DELETE_GATEWAYS = "/${EnergenieApiManagerImpl.CONTROLLER_DEVICES}/${EnergenieApiManagerImpl.ACTION_DELETE}"

    protected ManagedThingProvider managedThingProvider
    protected ThingRegistry thingRegistry
    protected HttpService httpService
    protected EnergenieHandlerFactory handlerFactory

    protected RestClient client
    protected Bridge gatewayThing

    protected Bridge createBridge(ThingRegistry thingRegistry, String apiKey, String userName, String gatewayCode, Integer gatewayID) {
        return createBridge(thingRegistry, apiKey, userName, gatewayCode, gatewayID, null)
    }

    protected Bridge createBridge(ThingRegistry thingRegistry, String apiKey, String userName, String gatewayCode, Integer gatewayID, String authCode) {
        handlerFactory.apiConfig = new EnergenieApiConfiguration(userName, apiKey)

        Map<String,Object> properties = new HashMap<String, Object>()
        properties.put(EnergenieBindingConstants.CONFIG_GATEWAY_CODE, gatewayCode)
        properties.put(EnergenieBindingConstants.PROPERTY_DEVICE_ID, new BigDecimal(gatewayID))
        // We set the auth_code to pretend that the device registration has passed correctly
        if(authCode != null) {
            properties.put(EnergenieBindingConstants.PROPERTY_AUTH_CODE, authCode)
        }
        String bridgeID
        if(gatewayID != null) {
            bridgeID = Integer.toString(gatewayID)
        } else {
            bridgeID = "Unknown"
        }

        Configuration configuration = new Configuration(properties)

        Bridge gateway = thingRegistry.createThingOfType(
                EnergenieBindingConstants.THING_TYPE_GATEWAY,
                new ThingUID(EnergenieBindingConstants.THING_TYPE_GATEWAY, bridgeID),
                null, "Label", configuration)
        return gateway
    }
    protected Thing createThing (ThingRegistry thingRegistry, Bridge bridge, ThingTypeUID thingTypeUID) {
        return createThing(thingRegistry, bridge, thingTypeUID, null)
    }

    protected Thing createThing (ThingRegistry thingRegistry, Bridge bridge, ThingTypeUID thingTypeUID, Integer subdeviceID) {
        Map<String,Object> configMap = new HashMap<String, Object>()
        configMap.put(EnergenieBindingConstants.CONFIG_UPDATE_INTERVAL, TEST_SUBDEVICE_UPDATE_INTERVAL)
        Configuration configuration = new Configuration(configMap)

        Map<String, String> properties = new HashMap<String, String>();
        // We set the deviceID to skiconfigMapdevice registration
        if(subdeviceID != null) {
            properties.put(EnergenieBindingConstants.PROPERTY_DEVICE_ID, Integer.toString(subdeviceID))
        }

        String thingID
        if(subdeviceID != null) {
            thingID = Integer.toString(subdeviceID)
        } else {
            thingID = "Unknown"
        }
        Thing thing = thingRegistry.createThingOfType(
                thingTypeUID,
                new ThingUID(thingTypeUID,thingID),
                bridge.getUID(), JsonSubdevice.DEFAULT_LABEL, configuration)
        thing.setProperties(properties)
        return thing
    }

    protected void removeBridge(ThingRegistry thingRegistry, Bridge gateway) {
        if(gateway !=null) {
            thingRegistry.forceRemove(gateway.getUID())
            waitForAssert {
                BridgeHandler gatewayHandler = gateway.getHandler()
                assertThat "The bridge ${gateway.getUID()} cannot be deleted", gatewayHandler, is(nullValue())
            }
        }
    }

    protected void removeThing(ThingRegistry thingRegistry, Thing thing) {
        if(thing !=null) {
            thingRegistry.forceRemove(thing.getUID())
            waitForAssert {
                ThingHandler thingHandler = thing.getHandler()
                assertThat "The thing ${thing.getUID()} cannot be deleted", thingHandler, is(nullValue())
            }
        }
    }

    public JsonObject generateShowJsonDeviceServerResponse(String status, JsonDevice device) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        ShowDeviceResponse responseObj = new ShowDeviceResponse(status, device)
        String response = gson.toJson(responseObj)
        return JsonResponseHandler.responseStringtoJsonObject(response)
    }

    public JsonObject generateJsonDevicesListServerResponse(String status, JsonDevice ... devices) {
        Gson gson = new Gson();
        ListDevicesResponse responseObj = new ListDevicesResponse(devices, status)
        String response = gson.toJson(responseObj)
        return JsonResponseHandler.responseStringtoJsonObject(response)
    }

    protected void registerServlet(String path, HttpServlet servlet){
        try {
            httpService.registerServlet(path, servlet, null, null)
        } catch (Exception e) {
        }
    }

    protected void unregisterServlet(String path) {
        try {
            httpService.unregister(path)
        } catch (IllegalArgumentException e) {
            // Do nothing, the servlet hasn't been registered
        }
    }

    // Helper classes to build the server response

    /**
     * Represents a response to the Mi|Home REST API for the requests /subdevices/show and /devices/create
     */
    static class ShowDeviceResponse {
        String status
        JsonDevice data

        ShowDeviceResponse(String status, JsonDevice data) {
            this.status = status
            this.data = data
        }
    }

    /**
     * Represents a response to the Mi|Home REST API for the request /subdevices/list and devices/list
     */
    static class ListDevicesResponse {
        String status
        JsonDevice [] data

        ListDevicesResponse(JsonDevice [] data, String status) {
            this.status = status
            this.data = data

        }
    }

    public void setUpServices(){
        registerVolatileStorageService()

        thingRegistry = getService(ThingRegistry)
        assertThat thingRegistry, is(notNullValue())

        httpService = getService(HttpService)
        assertThat httpService, is(notNullValue())

        managedThingProvider = getService(ManagedThingProvider)
        assertThat managedThingProvider, is(notNullValue())

        handlerFactory = getService(ThingHandlerFactory,EnergenieHandlerFactory)
        assertThat handlerFactory, is(notNullValue())

        client = getService(RestClient)
        client.setBaseURL(TEST_URL)
    }

    protected JsonGateway createTestGateway() {
        LocalDateTime curentLocalDateTime = LocalDateTime.now();
        Date currentDate = Date.from(curentLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
        String timeStamp = new SimpleDateFormat(EnergenieBindingConstants.DATE_TIME_PATTERN).format(currentDate);

        return new JsonGateway(TEST_USER_ID, TEST_GATEWAY_ID, TEST_LABEL, TEST_AUTH_CODE, TEST_MAC_ADDRESS, TEST_IP_ADDRESS, TEST_PORT, TEST_FIRMWARE_VERSION, timeStamp)
    }
}
