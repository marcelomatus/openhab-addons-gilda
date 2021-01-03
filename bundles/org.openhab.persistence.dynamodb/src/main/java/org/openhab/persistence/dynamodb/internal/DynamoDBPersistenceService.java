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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * This is the implementation of the DynamoDB {@link PersistenceService}. It persists item values
 * using the <a href="https://aws.amazon.com/dynamodb/">Amazon DynamoDB</a> database. The states (
 * {@link State}) of an {@link Item} are persisted in DynamoDB tables.
 *
 * The service creates tables automatically, one for numbers, and one for strings.
 *
 * @see AbstractDynamoDBItem.fromState for details how different items are persisted
 *
 * @author Sami Salonen - Initial contribution
 * @author Kai Kreuzer - Migration to 3.x
 *
 */
@NonNullByDefault
@Component(service = { PersistenceService.class, QueryablePersistenceService.class,
        ModifiablePersistenceService.class }, configurationPid = "org.openhab.dynamodb", //
        property = Constants.SERVICE_PID + "=org.openhab.dynamodb")
@ConfigurableService(category = "persistence", label = "DynamoDB Persistence Service", description_uri = DynamoDBPersistenceService.CONFIG_URI)
public class DynamoDBPersistenceService implements QueryablePersistenceService {

    protected static final String CONFIG_URI = "persistence:dynamodb";

    private class CredentialsProvider implements AwsCredentialsProvider {

        @Override
        public AwsCredentials resolveCredentials() {
            if (dbConfig == null) {
                logger.error("Dynamodb config is not ready, should not happen!");
                throw new IllegalStateException();
            }
            return dbConfig.getCredentials();
        }
    }

    private static final String DYNAMODB_THREADPOOL_NAME = "dynamodbPersistenceService";

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) DynamoDbEnhancedAsyncClient client;
    @NonNullByDefault({})
    DynamoDbAsyncClient lowLevelClient;
    private final Logger logger = LoggerFactory.getLogger(DynamoDBPersistenceService.class);
    private boolean isProperlyConfigured;
    @NonNullByDefault({})
    DynamoDBConfig dbConfig;
    @NonNullByDefault({})
    DynamoDBTableNameResolver tableNameResolver;
    final ExecutorService executor = ThreadPoolManager.getPool(DYNAMODB_THREADPOOL_NAME);
    private static final Duration TIMEOUT_API_CALL = Duration.ofSeconds(60);
    private static final Duration TIMEOUT_API_CALL_ATTEMPT = Duration.ofSeconds(5);
    @SuppressWarnings("rawtypes")
    private Map<Class<? extends DynamoDBItem>, DynamoDbAsyncTable<DynamoDBItem>> tableCache = new ConcurrentHashMap<>(
            2);
    private AwsCredentialsProvider credentialsProvider = new CredentialsProvider();

    @Nullable
    URI endpointOverride;

    void overrideConfig(AwsRequestOverrideConfiguration.Builder config) {
        config.apiCallAttemptTimeout(TIMEOUT_API_CALL_ATTEMPT).apiCallTimeout(TIMEOUT_API_CALL)
                .credentialsProvider(credentialsProvider);
    }

    void overrideConfig(ClientOverrideConfiguration.Builder config) {
        config.apiCallAttemptTimeout(TIMEOUT_API_CALL_ATTEMPT).apiCallTimeout(TIMEOUT_API_CALL)
                .retryPolicy(dbConfig.getRetryPolicy());
    }

    @Activate
    public DynamoDBPersistenceService(final @Reference ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * For tests
     */
    DynamoDBPersistenceService(final @Reference ItemRegistry itemRegistry, @Nullable URI endpointOverride) {
        this.itemRegistry = itemRegistry;
        this.endpointOverride = endpointOverride;
    }

    /**
     * For testing. Allows access to underlying DynamoDbEnhancedAsyncClient.
     *
     */
    DynamoDbEnhancedAsyncClient getClient() {
        return client;
    }

    /**
     * For testing. Allows access to underlying DynamoDbAsyncClient.
     *
     */
    DynamoDbAsyncClient getLowLevelClient() {
        return lowLevelClient;
    }

    @Activate
    public void activate(final @Nullable BundleContext bundleContext, final Map<String, Object> config) {
        disconnect();
        dbConfig = DynamoDBConfig.fromConfig(config);
        if (dbConfig == null) {
            // Configuration was invalid. Abort service activation.
            // Error is already logger in fromConfig.
            return;
        }
        tableNameResolver = new DynamoDBTableNameResolver(dbConfig.getTableRevision(), dbConfig.getTable(),
                dbConfig.getTablePrefixLegacy());
        try {
            if (!ensureClient()) {
                logger.error("Error creating dynamodb database client. Aborting service activation.");
                return;
            }
        } catch (Exception e) {
            logger.error("Error constructing dynamodb client", e);
            return;
        }

        isProperlyConfigured = true;
        logger.debug("dynamodb persistence service activated");
    }

    @Deactivate
    public void deactivate() {
        logger.debug("dynamodb persistence service deactivated");
        logIfManyQueuedTasks();
        disconnect();
    }

    /**
     * Initializes Dynamo DB client and determines schema
     *
     * If construction fails, error is logged and false is returned.
     *
     * @return whether initialization was successful.
     */
    private boolean ensureClient() {
        if (dbConfig == null) {
            return false;
        }
        if (client == null) {
            try {
                synchronized (this) {
                    if (this.client != null) {
                        return true;
                    }
                    DynamoDbAsyncClientBuilder lowlevelClientBuilder = DynamoDbAsyncClient.builder()
                            .credentialsProvider(StaticCredentialsProvider.create(dbConfig.getCredentials()))
                            .asyncConfiguration(ClientAsyncConfiguration.builder()
                                    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executor)
                                    .build())
                            .overrideConfiguration(this::overrideConfig).region(dbConfig.getRegion());
                    if (endpointOverride != null) {
                        logger.info("DynamoDB has been overriden to {}", endpointOverride);
                        lowlevelClientBuilder.endpointOverride(endpointOverride);
                    }
                    DynamoDbAsyncClient lowlevelClient = lowlevelClientBuilder.build();
                    client = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(lowlevelClient).build();
                    this.lowLevelClient = lowlevelClient;
                }
            } catch (Exception e) {
                logger.error("Error constructing dynamodb client", e);
                return false;
            }
        }
        return true;
    }

    private CompletableFuture<Boolean> resolveTableSchema() {
        if (tableNameResolver.isFullyResolved()) {
            return CompletableFuture.completedFuture(true);
        } else {
            synchronized (tableNameResolver) {
                if (tableNameResolver.isFullyResolved()) {
                    return CompletableFuture.completedFuture(true);
                }
                return tableNameResolver
                        .resolveSchema(lowLevelClient, b -> b.overrideConfiguration(this::overrideConfig), executor)
                        .thenApplyAsync(resolved -> {
                            if (resolved && tableNameResolver.getTableSchema() == ExpectedTableSchema.LEGACY) {
                                logger.warn(
                                        "Using legacy table format. Is it recommended to migrate to the new table format: specify the 'table' parameter and unset the old 'tablePrefix' parameter.");
                            }
                            return resolved;
                        }, executor);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private DynamoDbAsyncTable<DynamoDBItem> getTable(Class<? extends DynamoDBItem> dtoClass) {
        DynamoDbEnhancedAsyncClient localClient = client;
        if (!ensureClient() || localClient == null) {
            throw new IllegalStateException();
        }
        ExpectedTableSchema expectedTableSchemaRevision = tableNameResolver.getTableSchema();
        String tableName = tableNameResolver.fromClass(dtoClass);
        final TableSchema<? extends DynamoDBItem> schema = getDynamoDBTableSchema(dtoClass,
                expectedTableSchemaRevision);
        DynamoDbAsyncTable<DynamoDBItem> table = tableCache.computeIfAbsent(dtoClass, clz -> {
            return (DynamoDbAsyncTable<DynamoDBItem>) localClient.table(tableName, schema);
        });
        assert table != null; // Invariant
        return table;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static TableSchema<DynamoDBItem> getDynamoDBTableSchema(Class<? extends DynamoDBItem> dtoClass,
            ExpectedTableSchema expectedTableSchemaRevision) {
        final TableSchema<? extends DynamoDBItem> schema;
        if (dtoClass.equals(DynamoDBBigDecimalItem.class)) {
            schema = expectedTableSchemaRevision == ExpectedTableSchema.NEW ? DynamoDBBigDecimalItem.TABLE_SCHEMA_NEW
                    : DynamoDBBigDecimalItem.TABLE_SCHEMA_LEGACY;
        } else if (dtoClass.equals(DynamoDBStringItem.class)) {
            schema = expectedTableSchemaRevision == ExpectedTableSchema.NEW ? DynamoDBStringItem.TABLE_SCHEMA_NEW
                    : DynamoDBStringItem.TABLE_SCHEMA_LEGACY;
        } else {
            throw new IllegalStateException("Unknown DTO class. Bug");
        }
        return (TableSchema<DynamoDBItem>) schema;
    }

    private void disconnect() {
        if (client == null || lowLevelClient == null) {
            return;
        }
        lowLevelClient.close();
        lowLevelClient = null;
        client = null;
        dbConfig = null;
        tableNameResolver = null;
        isProperlyConfigured = false;
        tableCache.clear();
    }

    protected boolean isReadyToStore() {
        return isProperlyConfigured && ensureClient();
    }

    @Override
    public String getId() {
        return "dynamodb";
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "DynamoDB";
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        return Collections.emptySet();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        logIfManyQueuedTasks();
        try {
            Boolean resolved = resolveTableSchema().get();
            if (!resolved) {
                logger.warn("Table schema not resolved, cannot query data.");
                return Collections.<HistoricItem>emptyList();
            }
        } catch (InterruptedException e) {
            logger.warn("Table schema resolution interrupted, cannot query data");
            return Collections.<HistoricItem>emptyList();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            logger.warn("Table schema resolution errored, cannot query data: {} {}",
                    cause == null ? e.getClass().getSimpleName() : cause.getClass().getSimpleName(),
                    cause == null ? e.getMessage() : cause.getMessage());
            return Collections.<HistoricItem>emptyList();
        }

        Instant start = Instant.now();
        String filterDescription = filterToString(filter);
        logger.trace("Got a query with filter {}", filterDescription);
        DynamoDbEnhancedAsyncClient localClient = client;
        if (!isProperlyConfigured) {
            logger.debug("Configuration for dynamodb not yet loaded or broken. Not storing item.");
            return Collections.<HistoricItem>emptyList();
        }
        if (!ensureClient() || localClient == null) {
            logger.warn("DynamoDB not connected. Not storing item.");
            return Collections.<HistoricItem>emptyList();
        }

        String itemName = filter.getItemName();
        Item item = getItemFromRegistry(itemName);
        if (item == null) {
            logger.warn("Could not get item {} from registry!", itemName);
            return Collections.<HistoricItem>emptyList();
        }
        boolean legacy = tableNameResolver.getTableSchema() == ExpectedTableSchema.LEGACY;
        Class<DynamoDBItem<?>> dtoClass = AbstractDynamoDBItem.getDynamoItemClass(item.getClass(), legacy);
        String tableName = tableNameResolver.fromClass(dtoClass);
        DynamoDbAsyncTable<DynamoDBItem> table = getTable(dtoClass);
        logger.debug("Item {} (of type {}) will be tried to query using DTO class {} from table {}", itemName,
                item.getClass().getSimpleName(), dtoClass.getSimpleName(), tableName);

        QueryEnhancedRequest queryExpression = DynamoDBQueryUtils.createQueryExpression(dtoClass,
                tableNameResolver.getTableSchema(), item, filter);

        CompletableFuture<List<DynamoDBItem>> itemsFuture = new CompletableFuture<>();
        final SdkPublisher<DynamoDBItem> itemPublisher = table.query(queryExpression).items();
        Subscriber<DynamoDBItem> pageSubscriber = new PageOfInterestSubscriber<DynamoDBItem>(itemsFuture,
                filter.getPageNumber(), filter.getPageSize());
        itemPublisher.subscribe(pageSubscriber);

        try {
            @SuppressWarnings("null")
            List<HistoricItem> results = itemsFuture.get().stream().map(dynamoItem -> {
                HistoricItem historicItem = dynamoItem.asHistoricItem(item);
                if (historicItem == null) {
                    logger.warn(
                            "Dynamo item {} serialized state '{}' cannot be converted to item {} {}. Item type changed since persistence. Ignoring",
                            dynamoItem.getClass().getSimpleName(), dynamoItem.getState(),
                            item.getClass().getSimpleName(), item.getName());
                    return null;
                }
                logger.trace("Dynamo item {} converted to historic item: {}", item, historicItem);
                return historicItem;
            }).filter(value -> value != null).collect(Collectors.toList());
            logger.debug("Query completed in {} ms. Filter was {}", Duration.between(start, Instant.now()).toMillis(),
                    filterDescription);
            return results;
        } catch (InterruptedException e) {
            logger.warn("Query interrupted. Filter was {}", filterDescription);
            return Collections.<HistoricItem>emptyList();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ResourceNotFoundException) {
                logger.trace("Query failed since the DynamoDB table '{}' does not exist. Filter was {}", tableName,
                        filterDescription);
            } else if (logger.isTraceEnabled()) {
                logger.trace("Query failed. Filter was {}", filterDescription, e);
            } else {
                logger.warn("Query failed {} {}. Filter was {}",
                        cause == null ? e.getClass().getSimpleName() : cause.getClass().getSimpleName(),
                        cause == null ? e.getMessage() : cause.getMessage(), filterDescription);
            }
            return Collections.<HistoricItem>emptyList();
        }
    }

    /**
     * Retrieves the item for the given name from the item registry
     *
     * @param itemName
     * @return item with the given name, or null if no such item exists in item registry.
     */
    private @Nullable Item getItemFromRegistry(String itemName) {
        if (itemRegistry == null) {
            return null;
        }
        Item item = null;
        try {
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e1) {
            logger.error("Unable to get item {} from registry", itemName);
        }
        return item;
    }

    @Override
    public List<PersistenceStrategy> getDefaultStrategies() {
        return List.of(PersistenceStrategy.Globals.RESTORE, PersistenceStrategy.Globals.CHANGE);
    }

    @Override
    public void store(Item item) {
        store(item, null);
    }

    @Override
    public void store(Item item, @Nullable String alias) {
        // Timestamp and capture state immediately as rest of the store is asynchronous (state might change in between)
        ZonedDateTime time = ZonedDateTime.now();

        logIfManyQueuedTasks();
        if (!(item instanceof GenericItem)) {
            return;
        }
        if (item.getState() instanceof UnDefType) {
            logger.debug("Undefined item state received. Not storing item {}.", item.getName());
            return;
        }
        if (!isReadyToStore()) {
            logger.warn("Not ready to store (config error?), not storing item {}.", item.getName());
            return;
        }

        String effectiveName = (alias != null) ? alias : item.getName();

        // item.state is not reliable in async context below since item state can change. We 'copy' the item.
        final GenericItem copiedItem = copyItem(item, effectiveName, null);

        resolveTableSchema().thenAcceptAsync(resolved -> {
            if (!resolved) {
                logger.warn("Table schema not resolved, not storing item {}.", copiedItem.getName());
                return;
            }

            DynamoDbEnhancedAsyncClient localClient = client;
            DynamoDbAsyncClient localLowlevelClient = lowLevelClient;
            if (localClient == null || localLowlevelClient == null) {
                return;
            }

            final DynamoDBItem<?> dto;
            switch (tableNameResolver.getTableSchema()) {
                case NEW:
                    dto = AbstractDynamoDBItem.fromStateNew(copiedItem, time);
                    break;
                case LEGACY:
                    dto = AbstractDynamoDBItem.fromStateLegacy(copiedItem, time);
                    break;
                default:
                    throw new IllegalStateException("Unexpected. Bug");
            }
            logger.trace("store() called with item {} {} '{}', which was converted to DTO {}",
                    copiedItem.getClass().getSimpleName(), effectiveName, copiedItem.getState(), dto);
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class<DynamoDBItem> dtoClass = (Class<DynamoDBItem>) dto.getClass();

            @SuppressWarnings("rawtypes")
            DynamoDbAsyncTable<DynamoDBItem> table = getTable(dtoClass);
            new TableCreatingPutItem(this, dto, table).putItemAsync();
        }, executor).exceptionally(e -> {
            logger.error("Unexcepted error", e);
            return null;
        });
    }

    static GenericItem copyItem(Item item, @Nullable String nameOverride, @Nullable State stateOverride) {
        GenericItem copiedItem;
        try {
            copiedItem = (GenericItem) item.getClass().getDeclaredConstructor(String.class)
                    .newInstance(nameOverride == null ? item.getName() : nameOverride);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            LoggerFactory.getLogger(DynamoDBPersistenceService.class).error("Could not copy item of type {}. Bug",
                    item.getClass().getSimpleName());
            throw new IllegalArgumentException();
        }
        copiedItem.setState(stateOverride == null ? item.getState() : stateOverride);
        return copiedItem;
    }

    private void logIfManyQueuedTasks() {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor localExecutor = (ThreadPoolExecutor) executor;
            logger.trace("executor queue size: {}, remaining space {}. Active threads {}",
                    localExecutor.getQueue().size(), localExecutor.getQueue().remainingCapacity(),
                    localExecutor.getActiveCount());
            if (localExecutor.getQueue().size() >= 50) {
                logger.warn(
                        "Many ({}) tasks queued in executor! This might be sign of bad design or bug in the addon code.",
                        localExecutor.getQueue().size());
            }
        }
    }

    private String filterToString(FilterCriteria filter) {
        return String.format(
                "FilterCriteria@%s(item=%s, pageNumber=%d, pageSize=%d, time=[%s, %s, %s], state=[%s, %s of %s] )",
                System.identityHashCode(filter), filter.getItemName(), filter.getPageNumber(), filter.getPageSize(),
                filter.getBeginDate(), filter.getEndDate(), filter.getOrdering(), filter.getOperator(),
                filter.getState(), filter.getState() == null ? "null" : filter.getState().getClass().getSimpleName());
    }
}
