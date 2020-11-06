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
package org.openhab.binding.systeminfo.test;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.binding.systeminfo.internal.SysteminfoBindingConstants;
import org.openhab.binding.systeminfo.internal.SysteminfoHandlerFactory;
import org.openhab.binding.systeminfo.internal.discovery.SysteminfoDiscoveryService;
import org.openhab.binding.systeminfo.internal.handler.SysteminfoHandler;
import org.openhab.binding.systeminfo.internal.model.SysteminfoInterface;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.config.discovery.inbox.InboxPredicates;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * OSGi tests for the {@link SysteminfoHandler}
 *
 * @author Svilen Valkanov - Initial contribution
 * @author Lyubomir Papazov - Created a mock systeminfo object. This way, access to the user's OS will not be required,
 *         but mock data will be used instead, avoiding potential errors from the OS queries.
 * @author Wouter Born - Migrate Groovy to Java tests
 */
public class SysteminfoOSGiTest extends JavaOSGiTest {
    private static final String DEFAULT_TEST_BRIDGE_NAME = "computer";
    private static final String DEFAULT_TEST_PROCESS_NAME = "process";

    private static final String DEFAULT_TEST_ITEM_NAME = "test";
    private static final String DEFAULT_TEST_GROUP_ID = SysteminfoBindingConstants.CPU_GROUP_ID;
    private static final String DEFAULT_TEST_CHANNEL_ID = SysteminfoBindingConstants.CHANNEL_CPU_LOAD;
    private static final String DEFAULT_TEST_CHANNEL_PRIORITY = SysteminfoBindingConstants.HIGH_PRIOIRITY;
    private static final int DEFAULT_DEVICE_INDEX = 0;

    /**
     * Refresh time in seconds for tasks with priority High.
     * Default value for the parameter interval_high in the thing configuration
     */
    private static final int DEFAULT_TEST_INTERVAL_HIGH = 1;

    /**
     * Refresh time in seconds for tasks with priority Medium.
     */
    private static final int DEFAULT_TEST_INTERVAL_MEDIUM = 3;

    private Thing processInfoThing;
    private Bridge systemInfoBridge;

    private GenericItem testItem;

    private SysteminfoInterface mockedSystemInfo;
    private ManagedThingProvider managedThingProvider;
    private ThingRegistry thingRegistry;
    private ItemRegistry itemRegistry;
    private SysteminfoHandlerFactory systeminfoHandlerFactory;

    @BeforeEach
    public void setUp() {
        VolatileStorageService volatileStorageService = new VolatileStorageService();
        registerService(volatileStorageService);

        // Preparing the mock with OS properties, that are used in the initialize method of SysteminfoHandler
        mockedSystemInfo = mock(SysteminfoInterface.class);
        when(mockedSystemInfo.getCpuLogicalCores()).thenReturn(BigDecimal.valueOf(2));
        when(mockedSystemInfo.getCpuPhysicalCores()).thenReturn(BigDecimal.valueOf(2));
        when(mockedSystemInfo.getOsFamily()).thenReturn("Mock OS");
        when(mockedSystemInfo.getOsManufacturer()).thenReturn("Mock OS Manufacturer");
        when(mockedSystemInfo.getOsVersion()).thenReturn("Mock Os Version");

        systeminfoHandlerFactory = getService(ThingHandlerFactory.class, SysteminfoHandlerFactory.class);

        // Unbind oshiSystemInfo service and bind the mock service to make the systeminfobinding tests
        // independent of the external OSHI library
        SysteminfoInterface oshiSystemInfo = getService(SysteminfoInterface.class);
        if (oshiSystemInfo != null) {
            systeminfoHandlerFactory.unbindSystemInfo(oshiSystemInfo);
            oshiSystemInfo = null;
        }
        systeminfoHandlerFactory.bindSystemInfo(mockedSystemInfo);

        managedThingProvider = getService(ThingProvider.class, ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        itemRegistry = getService(ItemRegistry.class);
        assertThat(itemRegistry, is(notNullValue()));
    }

    @AfterEach
    public void tearDown() {
        if (processInfoThing != null) {
            // Remove the processinfo thing. The handler will be also disposed automatically
            Thing removedThing = thingRegistry.forceRemove(processInfoThing.getUID());
            assertThat("The processinfo thing cannot be deleted", removedThing, is(notNullValue()));
            waitForAssert(() -> {
                assertThat(processInfoThing.getHandler(), is(nullValue()));
            });
            processInfoThing = null;
        }

        if (systemInfoBridge != null) {
            // Remove the systeminfo thing. The handler will be also disposed automatically
            Thing removedThing = thingRegistry.forceRemove(systemInfoBridge.getUID());
            assertThat("The systeminfo thing cannot be deleted", removedThing, is(notNullValue()));
            waitForAssert(() -> {
                assertThat(systemInfoBridge.getHandler(), is(nullValue()));
            });
            systemInfoBridge = null;
        }

        if (testItem != null) {
            itemRegistry.remove(DEFAULT_TEST_ITEM_NAME);
            testItem = null;
        }
    }

    private void initializeProzessThing(String channelID, String acceptedItemType, int pid) {
        ThingTypeUID bridgeTypeUID = SysteminfoBindingConstants.THING_TYPE_COMPUTER;
        ThingUID bridgeUID = new ThingUID(bridgeTypeUID, DEFAULT_TEST_BRIDGE_NAME);
        systemInfoBridge = BridgeBuilder.create(bridgeTypeUID, bridgeUID).build();
        managedThingProvider.add(systemInfoBridge);

        ThingTypeUID thingTypeUID = SysteminfoBindingConstants.THING_TYPE_PROCESS;
        ThingUID thingUID = new ThingUID(thingTypeUID, DEFAULT_TEST_PROCESS_NAME);

        ChannelUID channelUID = new ChannelUID(thingUID, channelID);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(SysteminfoBindingConstants.BINDING_ID, channelUID.getId());

        Configuration channelConfig = new Configuration();
        channelConfig.put(SysteminfoBindingConstants.PARAMETER_PRIOIRITY, DEFAULT_TEST_CHANNEL_PRIORITY);
        Channel channel = ChannelBuilder.create(channelUID, acceptedItemType).withType(channelTypeUID)
                .withKind(ChannelKind.STATE).withConfiguration(channelConfig).build();

        Configuration configuration = new Configuration();
        configuration.put(SysteminfoBindingConstants.PROCESS_ID, new BigDecimal(pid));
        processInfoThing = ThingBuilder.create(thingTypeUID, thingUID).withBridge(systemInfoBridge.getUID())
                .withConfiguration(configuration).withChannel(channel).build();
        managedThingProvider.add(processInfoThing);

        waitForAssert(() -> {
            assertThat(processInfoThing.getHandler(), is(notNullValue()));
        });

        waitForAssert(() -> {
            assertThat("Thing is not initilized, before an Item is created", processInfoThing.getStatus(),
                    anyOf(equalTo(ThingStatus.OFFLINE), equalTo(ThingStatus.ONLINE)));
        });

        intializeItem(channelUID, DEFAULT_TEST_ITEM_NAME, acceptedItemType);
    }

    private void initializeThingWithChannel(String groupID, String channelID, String acceptedItemType) {
        Configuration configuration = new Configuration();
        configuration.put(SysteminfoBindingConstants.HIGH_PRIORITY_REFRESH_TIME,
                new BigDecimal(DEFAULT_TEST_INTERVAL_HIGH));
        configuration.put(SysteminfoBindingConstants.MEDIUM_PRIORITY_REFRESH_TIME,
                new BigDecimal(DEFAULT_TEST_INTERVAL_MEDIUM));

        initializeBridge(configuration, groupID, channelID, acceptedItemType, DEFAULT_TEST_CHANNEL_PRIORITY);
    }

    private void initializeBridge(Configuration thingConfiguration, String groupID, String channelID,
            String acceptedItemType, String priority) {
        ThingTypeUID thingTypeUID = SysteminfoBindingConstants.THING_TYPE_COMPUTER;
        ThingUID thingUID = new ThingUID(thingTypeUID, DEFAULT_TEST_BRIDGE_NAME);

        ChannelUID channelUID = new ChannelUID(new ChannelGroupUID(thingUID, groupID), channelID);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(SysteminfoBindingConstants.BINDING_ID, channelID);
        Configuration channelConfig = new Configuration();
        channelConfig.put(SysteminfoBindingConstants.PARAMETER_PRIOIRITY, priority);
        Channel channel = ChannelBuilder.create(channelUID, acceptedItemType).withType(channelTypeUID)
                .withKind(ChannelKind.STATE).withConfiguration(channelConfig).build();

        systemInfoBridge = BridgeBuilder.create(thingTypeUID, thingUID).withConfiguration(thingConfiguration)
                .withChannel(channel).build();
        waitForAssert(() -> {
            assertThat(systemInfoBridge, is(notNullValue()));
        });

        managedThingProvider.add(systemInfoBridge);
        waitForAssert(() -> {
            assertThat(systemInfoBridge.getHandler(), is(notNullValue()));
        });

        waitForAssert(() -> {
            assertThat("Thing is not initilized, before an Item is created", systemInfoBridge.getStatus(),
                    anyOf(equalTo(ThingStatus.OFFLINE), equalTo(ThingStatus.ONLINE)));
        });

        intializeItem(channelUID, DEFAULT_TEST_ITEM_NAME, acceptedItemType);
    }

    private void assertThingOnline(final Thing thing) {
        waitForAssert(() -> {
            final ThingStatusInfo statusInfo = thing.getStatusInfo();

            String description = statusInfo.getDescription();
            ThingStatusDetail thingStatusDetail = statusInfo.getStatusDetail();
            assertThat("Thing status detail is " + thingStatusDetail + " with description " + description,
                    thing.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        });
    }

    private void assertItemState(String acceptedItemType, String itemName, String priority, State expectedState) {
        // The binding starts all refresh tasks in SysteminfoHandler.scheduleUpdates() after this delay
        try {
            sleep(SysteminfoHandler.WAIT_TIME_CHANNEL_ITEM_LINK_INIT * 1000);
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted while sleeping");
        }

        GenericItem item;
        try {
            item = (GenericItem) itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            throw new AssertionError("Item not found in registry");
        }

        int waitTime;
        if (priority.equals(SysteminfoBindingConstants.HIGH_PRIOIRITY)) {
            waitTime = DEFAULT_TEST_INTERVAL_HIGH * 1000;
        } else if (priority.equals(SysteminfoBindingConstants.MEDIUM_PRIOIRITY)) {
            waitTime = DEFAULT_TEST_INTERVAL_MEDIUM * 1000;
        } else {
            waitTime = 100;
        }

        waitForAssert(() -> {
            assertThat(item.getType(), is(equalTo(acceptedItemType)));
            assertThat(item.getState(), is(equalTo(expectedState)));
        }, waitTime, DFL_SLEEP_TIME);
    }

    private void intializeItem(ChannelUID channelUID, String itemName, String acceptedItemType) {
        if (acceptedItemType.equals("Number")) {
            testItem = new NumberItem(itemName);
        } else if (acceptedItemType.equals("String")) {
            testItem = new StringItem(itemName);
        }
        itemRegistry.add(testItem);

        ManagedItemChannelLinkProvider itemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertThat(itemChannelLinkProvider, is(notNullValue()));

        itemChannelLinkProvider.add(new ItemChannelLink(itemName, channelUID));
    }

    @Test
    public void assertInvalidThingConfigurationValuesAreHandled() {
        Configuration configuration = new Configuration();

        // invalid value - must be positive
        int refreshIntervalHigh = -5;
        configuration.put(SysteminfoBindingConstants.HIGH_PRIORITY_REFRESH_TIME, new BigDecimal(refreshIntervalHigh));

        int refreshIntervalMedium = 3;
        configuration.put(SysteminfoBindingConstants.MEDIUM_PRIORITY_REFRESH_TIME,
                new BigDecimal(refreshIntervalMedium));

        String acceptedItemType = "Number";
        initializeBridge(configuration, DEFAULT_TEST_GROUP_ID, DEFAULT_TEST_CHANNEL_ID, acceptedItemType,
                DEFAULT_TEST_CHANNEL_PRIORITY);

        waitForAssert(() -> {
            final ThingStatus status = systemInfoBridge.getStatus();
            assertThat("Invalid configuration is used!", status, is(equalTo(ThingStatus.OFFLINE)));

            final ThingStatusInfo statusInfo = systemInfoBridge.getStatusInfo();
            assertThat(statusInfo.getStatusDetail(), is(equalTo(ThingStatusDetail.HANDLER_INITIALIZING_ERROR)));
            assertThat(statusInfo.getDescription(), is(equalTo("Thing cannot be initialized.")));
        });
    }

    @Test
    public void assertThingStatusIsUninitializedWhenThereIsNoSysteminfoServiceProvided() {
        // Unbind the mock service to verify the systeminfo thing will not be initialized
        // when no systeminfo service is provided
        systeminfoHandlerFactory.unbindSystemInfo(mockedSystemInfo);

        ThingTypeUID thingTypeUID = SysteminfoBindingConstants.THING_TYPE_COMPUTER;
        ThingUID thingUID = new ThingUID(thingTypeUID, DEFAULT_TEST_BRIDGE_NAME);

        systemInfoBridge = BridgeBuilder.create(thingTypeUID, thingUID).build();
        managedThingProvider.add(systemInfoBridge);

        waitForAssert(() -> {
            assertThat("The thing status is uninitialized when systeminfo service is missing",
                    systemInfoBridge.getStatus(), equalTo(ThingStatus.UNINITIALIZED));
        });
    }

    @Test
    public void assertMediumPriorityChannelIsUpdated() {
        String acceptedItemType = "Number";
        String priority = SysteminfoBindingConstants.MEDIUM_PRIOIRITY;

        Configuration configuration = new Configuration();
        configuration.put(SysteminfoBindingConstants.HIGH_PRIORITY_REFRESH_TIME,
                new BigDecimal(DEFAULT_TEST_INTERVAL_HIGH));
        configuration.put(SysteminfoBindingConstants.MEDIUM_PRIORITY_REFRESH_TIME,
                new BigDecimal(DEFAULT_TEST_INTERVAL_MEDIUM));
        initializeBridge(configuration, DEFAULT_TEST_GROUP_ID, DEFAULT_TEST_CHANNEL_ID, acceptedItemType, priority);

        assertThingOnline(systemInfoBridge);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, priority, UnDefType.UNDEF);
    }

    @Test
    public void assertStateOfSecondDeviceIsUpdated() {
        // This test assumes that at least 2 network interfaces are present on the test platform
        ThingTypeUID thingTypeUID = SysteminfoBindingConstants.THING_TYPE_COMPUTER;
        ThingUID thingUID = new ThingUID(thingTypeUID, DEFAULT_TEST_BRIDGE_NAME);

        int deviceIndex = 1;
        String acceptedItemType = "String";
        String channelID = SysteminfoBindingConstants.NETWORK_GROUP_ID + String.valueOf(deviceIndex) + "#mac";

        Configuration channelConfig = new Configuration();
        channelConfig.put(SysteminfoBindingConstants.PARAMETER_PRIOIRITY, DEFAULT_TEST_CHANNEL_PRIORITY);

        ChannelUID channelUID = new ChannelUID(thingUID, channelID);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(SysteminfoBindingConstants.BINDING_ID,
                channelUID.getIdWithoutGroup());
        Channel channel = ChannelBuilder.create(channelUID, acceptedItemType).withType(channelTypeUID)
                .withKind(ChannelKind.STATE).withConfiguration(channelConfig).build();

        Configuration configuration = new Configuration();
        configuration.put(SysteminfoBindingConstants.HIGH_PRIORITY_REFRESH_TIME,
                new BigDecimal(DEFAULT_TEST_INTERVAL_HIGH));
        configuration.put(SysteminfoBindingConstants.MEDIUM_PRIORITY_REFRESH_TIME,
                new BigDecimal(DEFAULT_TEST_INTERVAL_MEDIUM));

        systemInfoBridge = BridgeBuilder.create(thingTypeUID, thingUID).withConfiguration(configuration)
                .withChannel(channel).build();
        waitForAssert(() -> {
            assertThat(systemInfoBridge, is(notNullValue()));
        });

        managedThingProvider.add(systemInfoBridge);
        waitForAssert(() -> {
            assertThat(systemInfoBridge.getHandler(), is(notNullValue()));
        });

        waitForAssert(() -> {
            assertThat("Thing is not initilized, before an Item is created", systemInfoBridge.getStatus(),
                    anyOf(equalTo(ThingStatus.OFFLINE), equalTo(ThingStatus.ONLINE)));
        });

        intializeItem(channelUID, DEFAULT_TEST_ITEM_NAME, acceptedItemType);

        assertThingOnline(systemInfoBridge);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, UnDefType.UNDEF);
    }

    @Test
    public void assertChannelCpuLoadIsUpdated() {
        BigDecimal mockedCpuLoadValue = BigDecimal.valueOf(10.5);
        when(mockedSystemInfo.getCpuLoad()).thenReturn(mockedCpuLoadValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID, SysteminfoBindingConstants.CHANNEL_CPU_LOAD,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedCpuLoadValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuLoad1IsUpdated() {
        BigDecimal mockedCpuLoad1Value = BigDecimal.valueOf(1.1);
        when(mockedSystemInfo.getCpuLoad1()).thenReturn(mockedCpuLoad1Value);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_CPU_LOAD_1, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedCpuLoad1Value);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuLoad5IsUpdated() {
        BigDecimal mockedCpuLoad5Value = BigDecimal.valueOf(5.5);
        when(mockedSystemInfo.getCpuLoad5()).thenReturn(mockedCpuLoad5Value);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_CPU_LOAD_5, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedCpuLoad5Value);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuLoad15IsUpdated() {
        BigDecimal mockedCpuLoad15Value = BigDecimal.valueOf(15.15);
        when(mockedSystemInfo.getCpuLoad15()).thenReturn(mockedCpuLoad15Value);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_CPU_LOAD_15, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedCpuLoad15Value);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuThreadsIsUpdated() {
        BigDecimal mockedCpuThreadsValue = BigDecimal.valueOf(16);
        when(mockedSystemInfo.getCpuThreads()).thenReturn(mockedCpuThreadsValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID, SysteminfoBindingConstants.CHANNEL_THREADS,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedCpuThreadsValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuUptimeIsUpdated() {
        BigDecimal mockedCpuUptimeValue = BigDecimal.valueOf(100);
        when(mockedSystemInfo.getCpuUptime()).thenReturn(mockedCpuUptimeValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_CPU_UPTIME, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedCpuUptimeValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuDescriptionIsUpdated() {
        String mockedCpuDescriptionValue = "Mocked Cpu Description";
        when(mockedSystemInfo.getCpuDescription()).thenReturn(mockedCpuDescriptionValue);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_DESCRIPTION, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedCpuDescriptionValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelCpuNameIsUpdated() {
        String mockedCpuNameValue = "Mocked Cpu Name";
        when(mockedSystemInfo.getCpuName()).thenReturn(mockedCpuNameValue);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID, SysteminfoBindingConstants.CHANNEL_NAME,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedCpuNameValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Disabled
    // There is a bug opened for this issue - https://github.com/dblock/oshi/issues/185
    @Test
    public void assertChannelSensorsCpuTempIsUpdated() {
        BigDecimal mockedSensorsCpuTemperatureValue = BigDecimal.valueOf(60);
        when(mockedSystemInfo.getSensorsCpuTemperature()).thenReturn(mockedSensorsCpuTemperatureValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_CPU_TEMPERATURE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSensorsCpuTemperatureValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelSensorsCpuVoltageIsUpdated() {
        BigDecimal mockedSensorsCpuVoltageValue = BigDecimal.valueOf(1000);
        when(mockedSystemInfo.getSensorsCpuVoltage()).thenReturn(mockedSensorsCpuVoltageValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.CPU_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_CPU_VOLTAGE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSensorsCpuVoltageValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelMemoryAvailableIsUpdated() {
        BigDecimal mockedMemoryAvailableValue = BigDecimal.valueOf(1000);
        when(mockedSystemInfo.getMemoryAvailable()).thenReturn(mockedMemoryAvailableValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.MEMORY_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_AVAILABLE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedMemoryAvailableValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelMemoryUsedIsUpdated() {
        BigDecimal mockedMemoryUsedValue = BigDecimal.valueOf(24);
        when(mockedSystemInfo.getMemoryUsed()).thenReturn(mockedMemoryUsedValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.MEMORY_GROUP_ID, SysteminfoBindingConstants.CHANNEL_USED,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedMemoryUsedValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelMemoryTotalIsUpdated() {
        BigDecimal mockedMemoryTotalValue = BigDecimal.valueOf(1024);
        when(mockedSystemInfo.getMemoryTotal()).thenReturn(mockedMemoryTotalValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.MEMORY_GROUP_ID, SysteminfoBindingConstants.CHANNEL_TOTAL,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedMemoryTotalValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelMemoryAvailablePercentIsUpdated() {
        BigDecimal mockedMemoryAvailablePercentValue = BigDecimal.valueOf(97);
        when(mockedSystemInfo.getMemoryAvailablePercent()).thenReturn(mockedMemoryAvailablePercentValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.MEMORY_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_AVAILABLE_PERCENT, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedMemoryAvailablePercentValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelSwapAvailableIsUpdated() {
        BigDecimal mockedSwapAvailableValue = BigDecimal.valueOf(482);
        when(mockedSystemInfo.getSwapAvailable()).thenReturn(mockedSwapAvailableValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.SWAP_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_AVAILABLE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSwapAvailableValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelSwapUsedIsUpdated() {
        BigDecimal mockedSwapUsedValue = BigDecimal.valueOf(30);
        when(mockedSystemInfo.getSwapUsed()).thenReturn(mockedSwapUsedValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.SWAP_GROUP_ID, SysteminfoBindingConstants.CHANNEL_USED,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSwapUsedValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelSwapTotalIsUpdated() {
        BigDecimal mockedSwapTotalValue = BigDecimal.valueOf(512);
        when(mockedSystemInfo.getSwapTotal()).thenReturn(mockedSwapTotalValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.SWAP_GROUP_ID, SysteminfoBindingConstants.CHANNEL_TOTAL,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSwapTotalValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelSwapAvailablePercentIsUpdated() {
        BigDecimal mockedSwapAvailablePercentValue = BigDecimal.valueOf(94);
        when(mockedSystemInfo.getSwapAvailablePercent()).thenReturn(mockedSwapAvailablePercentValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.SWAP_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_AVAILABLE_PERCENT, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSwapAvailablePercentValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageNameIsUpdated() throws IllegalArgumentException {
        String mockedStorageName = "Mocked Storage Name";
        when(mockedSystemInfo.getStorageName(DEFAULT_DEVICE_INDEX)).thenReturn(mockedStorageName);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID, SysteminfoBindingConstants.CHANNEL_NAME,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedStorageName);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageTypeIsUpdated() throws IllegalArgumentException {
        String mockedStorageType = "Mocked Storage Type";
        when(mockedSystemInfo.getStorageType(DEFAULT_DEVICE_INDEX)).thenReturn(mockedStorageType);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_STORAGE_TYPE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedStorageType);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageDescriptionIsUpdated() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.STORAGE_GROUP_ID + String.valueOf(DEFAULT_DEVICE_INDEX);
        channelID = channelID + "#" + SysteminfoBindingConstants.CHANNEL_DESCRIPTION;

        String mockedStorageDescription = "Mocked Storage Description";
        when(mockedSystemInfo.getStorageDescription(DEFAULT_DEVICE_INDEX)).thenReturn(mockedStorageDescription);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_DESCRIPTION, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedStorageDescription);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageAvailableIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedStorageAvailableValue = BigDecimal.valueOf(2000);
        when(mockedSystemInfo.getStorageAvailable(DEFAULT_DEVICE_INDEX)).thenReturn(mockedStorageAvailableValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_AVAILABLE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedStorageAvailableValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageUsedIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedStorageUsedValue = BigDecimal.valueOf(500);
        when(mockedSystemInfo.getStorageUsed(DEFAULT_DEVICE_INDEX)).thenReturn(mockedStorageUsedValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID, SysteminfoBindingConstants.CHANNEL_USED,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedStorageUsedValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageTotalIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedStorageTotalValue = BigDecimal.valueOf(2500);
        when(mockedSystemInfo.getStorageTotal(DEFAULT_DEVICE_INDEX)).thenReturn(mockedStorageTotalValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_TOTAL, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedStorageTotalValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelStorageAvailablePercentIsUpdated() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.STORAGE_GROUP_ID + String.valueOf(DEFAULT_DEVICE_INDEX);
        channelID = channelID + "#" + SysteminfoBindingConstants.CHANNEL_AVAILABLE_PERCENT;

        BigDecimal mockedStorageAvailablePercent = BigDecimal.valueOf(20);
        when(mockedSystemInfo.getStorageAvailablePercent(DEFAULT_DEVICE_INDEX))
                .thenReturn(mockedStorageAvailablePercent);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.STORAGE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_AVAILABLE_PERCENT, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedStorageAvailablePercent);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelDriveNameIsUpdated() throws IllegalArgumentException {
        String mockedDriveNameValue = "Mocked Drive Name";
        when(mockedSystemInfo.getDriveName(DEFAULT_DEVICE_INDEX)).thenReturn(mockedDriveNameValue);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.DRIVE_GROUP_ID, SysteminfoBindingConstants.CHANNEL_NAME,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedDriveNameValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelDriveModelIsUpdated() throws IllegalArgumentException {
        String mockedDriveModelValue = "Mocked Drive Model";
        when(mockedSystemInfo.getDriveModel(DEFAULT_DEVICE_INDEX)).thenReturn(mockedDriveModelValue);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.DRIVE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_DRIVE_MODEL, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedDriveModelValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Disabled
    // There is a bug opened for this issue - https://github.com/dblock/oshi/issues/185
    @Test
    public void assertChannelDriveSerialIsUpdated() throws IllegalArgumentException {
        String mockedDriveSerialNumber = "Mocked Drive Serial Number";
        when(mockedSystemInfo.getDriveSerialNumber(DEFAULT_DEVICE_INDEX)).thenReturn(mockedDriveSerialNumber);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.DRIVE_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_DRIVE_SERIAL, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedDriveSerialNumber);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelSensorsFanSpeedIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedSensorsCpuFanSpeedValue = BigDecimal.valueOf(180);
        when(mockedSystemInfo.getSensorsFanSpeed(DEFAULT_DEVICE_INDEX)).thenReturn(mockedSensorsCpuFanSpeedValue);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.FANS_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_FAN_SPEED, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedSensorsCpuFanSpeedValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelBatteryNameIsUpdated() throws IllegalArgumentException {
        String mockedBatteryName = "Mocked Battery Name";
        when(mockedSystemInfo.getBatteryName(DEFAULT_DEVICE_INDEX)).thenReturn(mockedBatteryName);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.BATTERY_GROUP_ID, SysteminfoBindingConstants.CHANNEL_NAME,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedBatteryName);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelBatteryRemainingCapacityIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedBatteryRemainingCapacity = BigDecimal.valueOf(200);
        when(mockedSystemInfo.getBatteryRemainingCapacity(DEFAULT_DEVICE_INDEX))
                .thenReturn(mockedBatteryRemainingCapacity);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.BATTERY_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_BATTERY_REMAINING_CAPACITY, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedBatteryRemainingCapacity);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelBatteryRemainingTimeIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedBatteryRemainingTime = BigDecimal.valueOf(3600);
        when(mockedSystemInfo.getBatteryRemainingTime(DEFAULT_DEVICE_INDEX)).thenReturn(mockedBatteryRemainingTime);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.BATTERY_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_BATTERY_REMAINING_TIME, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedBatteryRemainingTime);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelDisplayInformationIsUpdated() throws IllegalArgumentException {
        String mockedDisplayInfo = "Mocked Display Information";
        when(mockedSystemInfo.getDisplayInformation(DEFAULT_DEVICE_INDEX)).thenReturn(mockedDisplayInfo);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.DISPLAY_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_DISPLAY_INFORMATION, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedDisplayInfo);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkIpIsUpdated() throws IllegalArgumentException {
        String mockedNetworkIp = "192.168.1.0";
        when(mockedSystemInfo.getNetworkIp(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkIp);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_IP, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedNetworkIp);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkMacIsUpdated() throws IllegalArgumentException {
        String mockedNetworkMacValue = "AB-10-11-12-13-14";
        when(mockedSystemInfo.getNetworkMac(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkMacValue);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_MAC, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedNetworkMacValue);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkDataSentIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedNetworkDataSent = BigDecimal.valueOf(1000);
        when(mockedSystemInfo.getNetworkDataSent(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkDataSent);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_DATA_SENT, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedNetworkDataSent);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkDataReceivedIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedNetworkDataReceiveed = BigDecimal.valueOf(800);
        when(mockedSystemInfo.getNetworkDataReceived(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkDataReceiveed);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_DATA_RECEIVED, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedNetworkDataReceiveed);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkPacketsSentIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedNetworkPacketsSent = BigDecimal.valueOf(50);
        when(mockedSystemInfo.getNetworkPacketsSent(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkPacketsSent);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_PACKETS_SENT, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedNetworkPacketsSent);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkPacketsReceivedIsUpdated() throws IllegalArgumentException {
        BigDecimal mockedNetworkPacketsReceived = BigDecimal.valueOf(48);
        when(mockedSystemInfo.getNetworkPacketsReceived(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkPacketsReceived);

        String acceptedItemType = "Number";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_PACKETS_RECEIVED, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        DecimalType state = new DecimalType(mockedNetworkPacketsReceived);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkNetworkNameIsUpdated() throws IllegalArgumentException {
        String mockedNetworkName = "MockN-AQ34";
        when(mockedSystemInfo.getNetworkName(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkName);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID, SysteminfoBindingConstants.CHANNEL_NAME,
                acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedNetworkName);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelNetworkNetworkDisplayNameIsUpdated() throws IllegalArgumentException {
        String mockedNetworkAdapterName = "Mocked Network Adapter Name";
        when(mockedSystemInfo.getNetworkDisplayName(DEFAULT_DEVICE_INDEX)).thenReturn(mockedNetworkAdapterName);

        String acceptedItemType = "String";
        initializeThingWithChannel(SysteminfoBindingConstants.NETWORK_GROUP_ID,
                SysteminfoBindingConstants.CHANNEL_NETWORK_INTERFACE, acceptedItemType);
        assertThingOnline(systemInfoBridge);

        StringType state = new StringType(mockedNetworkAdapterName);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    class SysteminfoDiscoveryServiceMock extends SysteminfoDiscoveryService {
        String hostname;

        SysteminfoDiscoveryServiceMock(String hostname) {
            super();
            this.hostname = hostname;
        }

        @Override
        protected String getHostName() throws UnknownHostException {
            if (hostname.equals("unresolved")) {
                throw new UnknownHostException();
            }
            return hostname;
        }

        @Override
        public void startScan() {
            super.startScan();
        }
    }

    @Test
    public void testDiscoveryWithInvalidHostname() {
        String hostname = "Hilo.fritz.box";
        String expectedHostname = "Hilo_fritz_box";

        testDiscoveryService(expectedHostname, hostname);
    }

    @Test
    public void testDiscoveryWithValidHostname() {
        String hostname = "MyComputer";
        String expectedHostname = "MyComputer";

        testDiscoveryService(expectedHostname, hostname);
    }

    @Test
    public void testDiscoveryWithUnresolvedHostname() {
        String hostname = "unresolved";
        String expectedHostname = SysteminfoDiscoveryService.DEFAULT_THING_ID;

        testDiscoveryService(expectedHostname, hostname);
    }

    @Test
    public void testDiscoveryWithEmptyHostnameString() {
        String hostname = "";
        String expectedHostname = SysteminfoDiscoveryService.DEFAULT_THING_ID;

        testDiscoveryService(expectedHostname, hostname);
    }

    private void testDiscoveryService(String expectedHostname, String hostname) {
        SysteminfoDiscoveryService discoveryService = getService(DiscoveryService.class,
                SysteminfoDiscoveryService.class);
        waitForAssert(() -> {
            assertThat(discoveryService, is(notNullValue()));
        });
        SysteminfoDiscoveryServiceMock discoveryServiceMock = new SysteminfoDiscoveryServiceMock(hostname);
        if (discoveryService != null) {
            unregisterService(DiscoveryService.class);
        }
        registerService(discoveryServiceMock, DiscoveryService.class.getName(), new Hashtable<>());

        ThingTypeUID computerType = SysteminfoBindingConstants.THING_TYPE_COMPUTER;
        ThingUID computerUID = new ThingUID(computerType, expectedHostname);

        discoveryServiceMock.startScan();

        Inbox inbox = getService(Inbox.class);
        assertThat(inbox, is(notNullValue()));

        waitForAssert(() -> {
            List<DiscoveryResult> results = inbox.stream().filter(InboxPredicates.forThingUID(computerUID))
                    .collect(toList());
            assertFalse(results.isEmpty(), "No Thing with UID " + computerUID.getAsString() + " in inbox");
        });

        inbox.approve(computerUID, SysteminfoDiscoveryService.DEFAULT_THING_LABEL, null);

        waitForAssert(() -> {
            systemInfoBridge = (Bridge) thingRegistry.get(computerUID);
            assertThat(systemInfoBridge, is(notNullValue()));
        });

        waitForAssert(() -> {
            assertThat("Thing is not initialized.", systemInfoBridge.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        });
    }

    @Test
    public void assertChannelProcessThreadsIsUpdatedWithPIDse() throws IllegalArgumentException {
        String acceptedItemType = "Number";
        String channelID = SysteminfoBindingConstants.CHANNEL_PROCESS_THREADS;

        // The pid of the System idle process in Windows
        int pid = 0;

        BigDecimal mockedProcessThreadsCount = BigDecimal.valueOf(4);
        when(mockedSystemInfo.getProcessThreads(pid)).thenReturn(mockedProcessThreadsCount);

        initializeProzessThing(channelID, acceptedItemType, pid);
        assertThingOnline(processInfoThing);

        DecimalType state = new DecimalType(mockedProcessThreadsCount);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelProcessPathIsUpdatedWithPIDset() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.CHANNEL_PROCESS_PATH;
        String acceptedItemType = "String";
        // The pid of the System idle process in Windows
        int pid = 0;

        String mockedProcessPath = "C:\\Users\\MockedUser\\Process";
        when(mockedSystemInfo.getProcessPath(pid)).thenReturn(mockedProcessPath);

        initializeProzessThing(channelID, acceptedItemType, pid);
        assertThingOnline(processInfoThing);

        StringType state = new StringType(mockedProcessPath);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelProcessNameIsUpdatedWithPIDset() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.CHANNEL_NAME;
        String acceptedItemType = "String";
        // The pid of the System idle process in Windows
        int pid = 0;

        String mockedProcessName = "MockedProcess.exe";
        when(mockedSystemInfo.getProcessName(pid)).thenReturn(mockedProcessName);

        initializeProzessThing(channelID, acceptedItemType, pid);
        assertThingOnline(processInfoThing);

        StringType state = new StringType(mockedProcessName);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelProcessResidentMemoryIsUpdatedWithPIDset() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.CHANNEL_PROCESS_RESIDENT_MEMORY;
        String acceptedItemType = "Number";
        // The pid of the System idle process in Windows
        int pid = 0;

        BigDecimal mockedProcessMemory = BigDecimal.valueOf(450);
        when(mockedSystemInfo.getProcessResidentMemory(pid)).thenReturn(mockedProcessMemory);

        initializeProzessThing(channelID, acceptedItemType, pid);
        assertThingOnline(processInfoThing);

        DecimalType state = new DecimalType(mockedProcessMemory);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelProcessVirtualtMemoryIsUpdatedWithPIDset() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.CHANNEL_PROCESS_VIRTUAL_MEMORY;
        String acceptedItemType = "Number";
        // The pid of the System idle process in Windows
        int pid = 0;

        BigDecimal mockedProcessMemory = BigDecimal.valueOf(450);
        when(mockedSystemInfo.getProcessVirtualMemory(pid)).thenReturn(mockedProcessMemory);

        initializeProzessThing(channelID, acceptedItemType, pid);
        assertThingOnline(processInfoThing);

        DecimalType state = new DecimalType(mockedProcessMemory);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void assertChannelProcessLoadIsUpdatedWithPIDset() throws IllegalArgumentException {
        String channelID = SysteminfoBindingConstants.CHANNEL_PROCESS_LOAD;
        String acceptedItemType = "Number";
        // The pid of the System idle process in Windows
        int pid = 0;

        BigDecimal mockedProcessLoad = BigDecimal.valueOf(3);
        when(mockedSystemInfo.getProcessCpuUsage(pid)).thenReturn(mockedProcessLoad);

        initializeProzessThing(channelID, acceptedItemType, pid);
        assertThingOnline(processInfoThing);

        DecimalType state = new DecimalType(mockedProcessLoad);
        assertItemState(acceptedItemType, DEFAULT_TEST_ITEM_NAME, DEFAULT_TEST_CHANNEL_PRIORITY, state);
    }

    @Test
    public void testThingHandlesChannelPriorityChange() {
        BigDecimal mockedCpuLoadValue = BigDecimal.valueOf(10.5);
        when(mockedSystemInfo.getCpuLoad()).thenReturn(mockedCpuLoadValue);

        String priorityKey = SysteminfoBindingConstants.PARAMETER_PRIOIRITY;
        String initialPriority = DEFAULT_TEST_CHANNEL_PRIORITY; // Evaluates to High
        String newPriority = SysteminfoBindingConstants.LOW_PRIOIRITY;

        String acceptedItemType = "Number";
        initializeThingWithChannel(DEFAULT_TEST_GROUP_ID, DEFAULT_TEST_CHANNEL_ID, acceptedItemType);

        ChannelGroupUID channelGroupUID = new ChannelGroupUID(systemInfoBridge.getUID(), DEFAULT_TEST_GROUP_ID);
        ChannelUID channelUID = new ChannelUID(channelGroupUID, DEFAULT_TEST_CHANNEL_ID);
        SysteminfoHandler systemInfoHandler = (SysteminfoHandler) systemInfoBridge.getHandler();
        if (systemInfoHandler == null) {
            throw new AssertionError("SystemInfoHandler '" + systemInfoBridge.getUID() + "' is null");
        }

        waitForAssert(() -> {
            Channel channel = systemInfoBridge.getChannel(channelUID);
            assertThat(channel, is(notNullValue()));

            Configuration config = channel.getConfiguration();
            assertThat("The initial priority of channel " + channel.getUID() + " is not as expected.",
                    config.get(priorityKey), is(equalTo(initialPriority)));

            Set<ChannelUID> channels = systemInfoHandler.getHighPriorityChannels();
            assertThat(channels, is(notNullValue()));
            assertThat(channels.contains(channel.getUID()), is(true));
        });

        // Change the priority of a channel
        Configuration updatedConfig = new Configuration();
        updatedConfig.put(priorityKey, newPriority);
        Channel updatedCannel = systemInfoBridge.getChannel(channelUID);
        updatedCannel = ChannelBuilder.create(updatedCannel).withConfiguration(updatedConfig).build();

        systemInfoBridge = BridgeBuilder.create(systemInfoBridge.getThingTypeUID(), systemInfoBridge.getUID())
                .withConfiguration(systemInfoBridge.getConfiguration()).withChannel(updatedCannel).build();
        systemInfoHandler.thingUpdated(systemInfoBridge);

        waitForAssert(() -> {
            Channel channel = systemInfoBridge.getChannel(channelUID);
            assertThat(channel, is(notNullValue()));

            Configuration config = channel.getConfiguration();
            assertThat("The prority of the channel was not updated: ", config.get(priorityKey),
                    is(equalTo(newPriority)));

            Set<ChannelUID> channels = systemInfoHandler.getLowPriorityChannels();
            assertThat(channels, is(notNullValue()));
            assertThat(channels.contains(channel.getUID()), is(true));
        });
    }
}
