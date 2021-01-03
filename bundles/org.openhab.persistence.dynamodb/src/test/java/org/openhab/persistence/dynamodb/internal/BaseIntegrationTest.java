/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.persistence.dynamodb.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.net.ServerSocket;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.RegistryHook;
import org.openhab.core.library.items.CallItem;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.LocationItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.PlayerItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.test.java.JavaTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;

import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class BaseIntegrationTest extends JavaTest {
    protected static final String TABLE = "dynamodb-integration-tests";
    protected static final String TABLE_PREFIX = "dynamodb-integration-tests-";
    protected static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBPersistenceService.class);
    protected static @Nullable DynamoDBPersistenceService service;
    protected static final Map<String, Item> ITEMS = new HashMap<>();
    protected static @Nullable DynamoDBProxyServer embeddedServer;
    private static @Nullable DynamoDBPersistenceService legacyService;
    private static @Nullable DynamoDBPersistenceService newService;
    private static @Nullable URI endpointOverride;

    /**
     * Whether tests are run in Continuous Integration environment, i.e. Jenkins or Travis CI
     *
     * Travis CI is detected using CI environment variable, see https://docs.travis-ci.com/user/environment-variables/
     * Jenkins CI is detected using JENKINS_HOME environment variable
     *
     * @return
     */
    private static boolean isRunningInCI() {
        return "true".equals(System.getenv("CI")) || StringUtils.isNotBlank(System.getenv("JENKINS_HOME"));
    }

    private static boolean credentialsSet() {
        String access = System.getProperty("DYNAMODBTEST_ACCESS");
        String secret = System.getProperty("DYNAMODBTEST_SECRET");
        return access != null && !access.isBlank() && secret != null && !secret.isBlank();
    }

    private static int findFreeTCPPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int localPort = serverSocket.getLocalPort();
            assertTrue(localPort > 0);
            return localPort;
        } catch (Exception e) {
            fail("Unable to find free tcp port for embedded DynamoDB server");
            return -1; // Make compiler happy
        }
    }

    @Override
    protected void waitForAssert(Runnable runnable) {
        // Longer timeouts and slower polling with real dynamodb
        // Non-CI tests against local server are with lower timeout.
        waitForAssert(runnable, hasFakeServer() ? isRunningInCI() ? 30_000L : 10_000L : 120_000L,
                hasFakeServer() ? 500 : 1000L);
    }

    @BeforeAll
    protected static void populateItems() {
        ITEMS.put("dimmer", new DimmerItem("dimmer"));
        ITEMS.put("number", new NumberItem("number"));
        ITEMS.put("string", new StringItem("string"));
        ITEMS.put("switch", new SwitchItem("switch"));
        ITEMS.put("contact", new ContactItem("contact"));
        ITEMS.put("color", new ColorItem("color"));
        ITEMS.put("rollershutter", new RollershutterItem("rollershutter"));
        ITEMS.put("datetime", new DateTimeItem("datetime"));
        ITEMS.put("call", new CallItem("call"));
        ITEMS.put("location", new LocationItem("location"));
        ITEMS.put("player_playpause", new PlayerItem("player_playpause"));
        ITEMS.put("player_rewindfastforward", new PlayerItem("player_rewindfastforward"));
    }

    @BeforeAll
    public static void initService(TestInfo testInfo) throws InterruptedException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        service = newService(isLegacyTest(testInfo), true, null, null, null);
        clearData();
    }

    /**
     * Create new persistence service. Either pointing to real DynamoDB (given credentials as java properties) or local
     * in-memory server
     *
     * @param legacy whether to create config that implies legacy or new schema. Use null for MAYBE_LEGACY
     * @param cleanLocal when creating local DB, whether to create new DB
     * @param overrideLocalURI URI to use when using local DB
     * @param table
     * @param tablePrefix
     * @return new persistence service
     */
    protected synchronized static DynamoDBPersistenceService newService(@Nullable Boolean legacy, boolean cleanLocal,
            @Nullable URI overrideLocalURI, @Nullable String table, @Nullable String tablePrefix) {
        final DynamoDBPersistenceService service;
        Map<String, Object> config = getConfig(legacy, table, tablePrefix);
        if (cleanLocal && overrideLocalURI != null) {
            throw new IllegalArgumentException("cannot specify both cleanLocal=true and overrideLocalURI");
        }
        if (legacy == null && (table != null || tablePrefix != null)) {
            throw new IllegalArgumentException("cannot specify both legacy=null and unambiguous table configuration");
        }
        URI localEndpointOverride = overrideLocalURI == null ? endpointOverride : overrideLocalURI;
        if (overrideLocalURI == null && !credentialsSet() && (cleanLocal || endpointOverride == null)) {
            // Local server not started yet, start it
            // endpointOverride static field has the URI
            LOGGER.info("Since credentials have not been defined, using embedded local AWS DynamoDB server");
            System.setProperty("sqlite4java.library.path", "src/test/resources/native-libs");
            int port = findFreeTCPPort();
            String endpoint = String.format("http://127.0.0.1:%d", port);
            try {
                localEndpointOverride = new URI(endpoint);
                DynamoDBProxyServer localEmbeddedServer = ServerRunner
                        .createServerFromCommandLineArgs(new String[] { "-inMemory", "-port", String.valueOf(port) });
                localEmbeddedServer.start();
                embeddedServer = localEmbeddedServer;
            } catch (Exception e) {
                fail("Error with embedded DynamoDB server", e);
                throw new IllegalStateException();
            }
        }

        if (endpointOverride == null) {
            endpointOverride = localEndpointOverride;
        }

        service = new DynamoDBPersistenceService(new ItemRegistry() {
            @Override
            public Collection<Item> getItems(String pattern) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Item> getItems() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Item getItemByPattern(String name) throws ItemNotFoundException, ItemNotUniqueException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Item getItem(String name) throws ItemNotFoundException {
                Item item = ITEMS.get(name);
                if (item == null) {
                    throw new ItemNotFoundException(name);
                }
                return item;
            }

            @Override
            public void addRegistryChangeListener(RegistryChangeListener<Item> listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Item> getAll() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Stream<Item> stream() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable Item get(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeRegistryChangeListener(RegistryChangeListener<Item> listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Item add(Item element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable Item update(Item element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable Item remove(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Item> getItemsOfType(String type) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Item> getItemsByTag(String... tags) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Item> getItemsByTagAndType(String type, String... tags) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T extends Item> Collection<T> getItemsByTag(Class<T> typeFilter, String... tags) {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable Item remove(String itemName, boolean recursive) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addRegistryHook(RegistryHook<Item> hook) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeRegistryHook(RegistryHook<Item> hook) {
                throw new UnsupportedOperationException();
            }
        }, localEndpointOverride);

        service.activate(null, config);
        return service;
    }

    private static Map<String, Object> getConfig(@Nullable Boolean legacy, @Nullable String table,
            @Nullable String tablePrefix) {
        Map<String, Object> config = new HashMap<>();
        if (legacy != null) {
            if (legacy.booleanValue()) {
                LOGGER.info("Legacy test");
                config.put("tablePrefix", tablePrefix == null ? TABLE_PREFIX : tablePrefix);

                // Disable buffering
                config.put("bufferSize", "0");
            } else {
                LOGGER.info("Non-legacy test");
                config.put("table", table == null ? TABLE : table);
            }
        }

        if (credentialsSet()) {
            LOGGER.info("Since credentials have been defined, using real AWS DynamoDB");

            String value = System.getProperty("DYNAMODBTEST_REGION");
            config.put("region", value != null ? value : "");
            value = System.getProperty("DYNAMODBTEST_ACCESS");
            config.put("accessKey", value != null ? value : "");
            value = System.getProperty("DYNAMODBTEST_SECRET");
            config.put("secretKey", value != null ? value : "");

            for (Entry<String, Object> entry : config.entrySet()) {
                if (((String) entry.getValue()).isEmpty()) {
                    fail("Expecting " + entry.getKey()
                            + " to have value for integration tests. Integration test will fail");
                    throw new IllegalArgumentException();
                }
            }
        } else {
            // Place some values to pass the configuration validation
            config.put("region", "eu-west-1");
            config.put("accessKey", "dummy-access-key");
            config.put("secretKey", "dummy-secret-key");
        }
        return config;
    }

    protected static boolean isLegacyTest(TestInfo testInfo) {
        try {
            return testInfo.getTestClass().get().getDeclaredField("LEGACY_MODE").getBoolean(null);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            fail("Could not find static boolean LEGACY_MODE from the test class: " + e.getClass().getSimpleName() + " "
                    + e.getMessage());
            throw new IllegalStateException(); // Making compiler happy
        }
    }

    protected boolean hasFakeServer() {
        return embeddedServer != null;
    }

    @AfterAll
    public static void tearDown() {
        try {
            if (embeddedServer != null) {
                embeddedServer.stop();
            }
        } catch (Exception e) {
            fail("Error stopping embedded server", e);
        }
    }

    protected static void clearData() {
        DynamoDBPersistenceService localService = service;
        assert localService != null;
        DynamoDbAsyncClient lowLevelClient = localService.getLowLevelClient();
        assertNotNull(lowLevelClient);
        // Clear data
        for (String table : new String[] { "dynamodb-integration-tests-bigdecimal", "dynamodb-integration-tests-string",
                TABLE }) {
            try {
                try {
                    lowLevelClient.describeTable(req -> req.tableName(table)).get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ResourceNotFoundException) {
                        // Table does not exist, this table does not need cleaning, continue to next table
                        continue;
                    }
                }

                lowLevelClient.deleteTable(req -> req.tableName(table)).get();
                final WaiterResponse<DescribeTableResponse> waiterResponse;
                try {
                    waiterResponse = lowLevelClient.waiter().waitUntilTableNotExists(req -> req.tableName(table)).get();
                } catch (ExecutionException e) {
                    // the waiting might fail with SdkClientException: An exception was thrown and did not match any
                    // waiter acceptors
                    // (the exception being CompletionException of ResourceNotFound)

                    // We check if table has been removed, and continue if it has
                    try {
                        lowLevelClient.describeTable(req -> req.tableName(table)).get();
                    } catch (ExecutionException e2) {
                        if (e2.getCause() instanceof ResourceNotFoundException) {
                            // Table does not exist, this table does not need cleaning, continue to next table
                            continue;
                        }
                    }
                    throw e;
                }
                assertTrue(waiterResponse.matched().exception().isEmpty());
            } catch (ExecutionException | InterruptedException e) {
                fail("Error cleaning up test (deleting table)", e);
            }
        }
    }
}
