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
package org.openhab.persistence.mongodb.internal;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.*;
import org.openhab.core.library.types.*;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.*;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

/**
 * This is the implementation of the MongoDB {@link PersistenceService}.
 *
 * @author Thorsten Hoeger - Initial contribution
 * @author Stephan Brunner - Query fixes, Cleanup
 * @author René Ulbricht - Fixes type handling, driver update and cleanup
 */
@NonNullByDefault
@Component(service = { PersistenceService.class,
        QueryablePersistenceService.class }, configurationPid = "org.openhab.mongodb", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MongoDBPersistenceService implements QueryablePersistenceService {

    private final Logger logger = LoggerFactory.getLogger(MongoDBPersistenceService.class);

    private String url = "";
    private String db = "";
    private String collection = "";
    private boolean collectionPerItem;

    private boolean initialized = false;

    protected final ItemRegistry itemRegistry;

    private @Nullable MongoClient cl;

    @Activate
    public MongoDBPersistenceService(final @Reference ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Activate
    public void activate(final BundleContext bundleContext, final Map<String, Object> config) {
        @Nullable
        String configUrl = (String) config.get("url");
        logger.debug("MongoDB URL {}", configUrl);
        if (configUrl == null || configUrl.isBlank()) {
            logger.warn("The MongoDB database URL is missing - please configure the mongodb:url parameter.");
            return;
        }
        url = configUrl;

        @Nullable
        String configDb = (String) config.get("database");
        logger.debug("MongoDB database {}", configDb);
        if (configDb == null || configDb.isBlank()) {
            logger.warn("The MongoDB database name is missing - please configure the mongodb:database parameter.");
            return;
        }
        db = configDb;

        @Nullable
        String dbCollection = (String) config.get("collection");
        logger.debug("MongoDB collection {}", dbCollection);
        collection = dbCollection == null ? "" : dbCollection;
        collectionPerItem = dbCollection == null || dbCollection.isBlank();

        if (!tryConnectToDatabase()) {
            logger.warn("Failed to connect to MongoDB server. Trying to reconnect later.");
        }

        initialized = true;
    }

    @Deactivate
    public void deactivate(final int reason) {
        logger.debug("MongoDB persistence bundle stopping. Disconnecting from database.");
        disconnectFromDatabase();
    }

    @Override
    public String getId() {
        return "mongodb";
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "MongoDB";
    }

    @Override
    public void store(Item item, @Nullable String alias) {
        // Don't log undefined/uninitialized data
        if (item.getState() instanceof UnDefType) {
            return;
        }

        // If we've not initialized the bundle, then return
        if (!initialized) {
            logger.warn("MongoDB not initialized");
            return;
        }

        // Connect to mongodb server if we're not already connected
        // If we can't connect, log.
        if (!tryConnectToDatabase()) {
            logger.warn(
                    "mongodb: No connection to database. Cannot persist item '{}'! Will retry connecting to database next time.",
                    item);
            return;
        }

        String realItemName = item.getName();
        String collectionName = collectionPerItem ? realItemName : this.collection;

        @Nullable
        MongoCollection<Document> collection = connectToCollection(collectionName);

        if (collection == null) {
            // Logging is done in connectToCollection()
            return;
        }

        String name = (alias != null) ? alias : realItemName;
        State state = item.getState();
        Object value = MongoDBTypeConversions.convertValue(state);

        Document obj = new Document();
        obj.put(MongoDBFields.FIELD_ID, new ObjectId());
        obj.put(MongoDBFields.FIELD_ITEM, name);
        obj.put(MongoDBFields.FIELD_REALNAME, realItemName);
        obj.put(MongoDBFields.FIELD_TIMESTAMP, new Date());
        obj.put(MongoDBFields.FIELD_VALUE, value);
        if (item instanceof NumberItem && state instanceof QuantityType<?>) {
            obj.put(MongoDBFields.FIELD_UNIT, ((QuantityType<?>) state).getUnit().toString());
        }
        try {
            collection.insertOne(obj);
        } catch (org.bson.BsonMaximumSizeExceededException e) {
            logger.error("Document size exceeds maximum size of 16MB. Item {} not persisted.", name);
            throw e;
        }
        logger.debug("MongoDB save {}={}", name, value);
    }

    @Override
    public void store(Item item) {
        store(item, null);
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        return Collections.emptySet();
    }

    /**
     * Checks if we have a database connection.
     * Also tests if communication with the MongoDB-Server is available.
     *
     * @return true if connection has been established, false otherwise
     */
    private synchronized boolean isConnected() {
        if (cl == null) {
            return false;
        }

        // Also check if the connection is valid.
        // Network problems may cause failure sometimes,
        // even if the connection object was successfully created before.
        try {
            cl.listDatabaseNames().first();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * (Re)connects to the database
     *
     * @return True, if the connection was successfully established.
     */
    private synchronized boolean tryConnectToDatabase() {
        if (isConnected()) {
            return true;
        }

        try {
            logger.debug("Connect MongoDB");
            disconnectFromDatabase();

            this.cl = MongoClients.create(this.url);

            // The MongoDB driver always succeeds in creating the connection.
            // We have to actually force it to test the connection to try to connect to the server.
            cl.listDatabaseNames().first();

            logger.debug("Connect MongoDB ... done");
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to database {}: {}", this.url, e.getMessage(), e);
            disconnectFromDatabase();
            return false;
        }
    }

    /**
     * Fetches the currently valid database.
     *
     * @return The database object
     */
    private synchronized @Nullable MongoClient getDatabase() {
        return cl;
    }

    /**
     * Connects to the Collection
     *
     * @return The collection object when collection creation was successful. Null otherwise.
     */
    private @Nullable MongoCollection<Document> connectToCollection(String collectionName) {
        try {
            @Nullable
            MongoClient db = getDatabase();

            if (db == null) {
                logger.error("Failed to connect to collection {}: Connection not ready", collectionName);
                return null;
            }

            MongoCollection<Document> mongoCollection = db.getDatabase(this.db).getCollection(collectionName);

            Document idx = new Document();
            idx.append(MongoDBFields.FIELD_ITEM, 1).append(MongoDBFields.FIELD_TIMESTAMP, 1);
            mongoCollection.createIndex(idx);

            return mongoCollection;
        } catch (Exception e) {
            logger.error("Failed to connect to collection {}: {}", collectionName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Disconnects from the database
     */
    private synchronized void disconnectFromDatabase() {
        if (this.cl != null) {
            this.cl.close();
        }

        cl = null;
    }

    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        if (!initialized) {
            return Collections.emptyList();
        }

        if (!tryConnectToDatabase()) {
            return Collections.emptyList();
        }

        String realItemName = filter.getItemName();
        if (realItemName == null) {
            logger.warn("Item name is missing in filter {}", filter);
            return List.of();
        }

        String collectionName = collectionPerItem ? realItemName : this.collection;
        @Nullable
        MongoCollection<Document> collection = connectToCollection(collectionName);

        // If collection creation failed, return nothing.
        if (collection == null) {
            // Logging is done in connectToCollection()
            return Collections.emptyList();
        }

        @Nullable
        Item item = getItem(realItemName);

        if (item == null) {
            logger.warn("Item {} not found", realItemName);
            return Collections.emptyList();
        }

        List<HistoricItem> items = new ArrayList<>();
        Document query = new Document();
        if (filter.getItemName() != null) {
            query.put(MongoDBFields.FIELD_ITEM, filter.getItemName());
        }
        State filterState = filter.getState();
        if (filterState != null && filter.getOperator() != null) {
            @Nullable
            String op = MongoDBTypeConversions.convertOperator(filter.getOperator());

            if (op == null) {
                logger.error("Failed to convert operator {} to MongoDB operator", filter.getOperator());
                return Collections.emptyList();
            }

            Object value = MongoDBTypeConversions.convertValue(filterState);
            query.put(MongoDBFields.FIELD_VALUE, new Document(op, value));
        }

        Document dateQueries = new Document();
        if (filter.getBeginDate() != null) {
            dateQueries.put("$gte", Date.from(filter.getBeginDate().toInstant()));
        }
        if (filter.getEndDate() != null) {
            dateQueries.put("$lte", Date.from(filter.getEndDate().toInstant()));
        }
        if (!dateQueries.isEmpty()) {
            query.put(MongoDBFields.FIELD_TIMESTAMP, dateQueries);
        }

        logger.debug("Query: {}", query);

        Integer sortDir = (filter.getOrdering() == Ordering.ASCENDING) ? 1 : -1;
        MongoCursor<Document> cursor = null;
        try {
            cursor = collection.find(query).sort(new Document(MongoDBFields.FIELD_TIMESTAMP, sortDir))
                    .skip(filter.getPageNumber() * filter.getPageSize()).limit(filter.getPageSize()).iterator();

            while (cursor.hasNext()) {
                Document obj = cursor.next();

                final State state = MongoDBTypeConversions.getStateFromDocument(item, obj);

                items.add(new MongoDBItem(realItemName, state, ZonedDateTime
                        .ofInstant(obj.getDate(MongoDBFields.FIELD_TIMESTAMP).toInstant(), ZoneId.systemDefault())));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return items;
    }

    private @Nullable Item getItem(String itemName) {
        try {
            return itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e1) {
            logger.error("Unable to get item type for {}", itemName);
        }
        return null;
    }

    @Override
    public List<PersistenceStrategy> getDefaultStrategies() {
        return Collections.emptyList();
    }
}
