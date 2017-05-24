/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.subscriptionstore.common;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common persistence layer for all subscriptions targeting the {@link PersistentStore}.
 * <p>
 * The {@link SubscriptionPersistor} serves as the data-access object for all {@link ddf.catalog.event.Subscription}s
 * across any endpoint. The object that actually gets stored is a {@link SubscriptionMetadata}, but combined
 * with a {@link org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory}, the
 * original {@code Subscription} object can be re-created and registered with the OSGi framework.
 * <p>
 * {@code SubscriptionPersistor} knows how to marshall and unmarshall between {@code SubscriptionMetadata}
 * and {@link PersistentItem}, effectively hiding the persistence details of subscriptions from the rest
 * of the service.
 */
public class SubscriptionPersistor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionPersistor.class);

    private static final String ECQL_FORMAT = "\"%s_txt\" LIKE '%s'";

    private static final String VERSION_LABEL = "version";

    private static final String VERSION = "1.0";

    private static final String SUBSCRIPTION_ID_SOLR = "id";

    private static final String SUBSCRIPTION_TYPE = "type";

    private static final String SUBSCRIPTION_FILTER = "filter";

    private static final String SUBSCRIPTION_CALLBACK = "callback";

    private final PersistentStore persistentStore;

    public SubscriptionPersistor(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
    }

    /**
     * Get all the subscription metadata objects in the persistent store, regardless of type.
     *
     * @return a map of subscription ids to their accompanying metadata.
     * @throws SubscriptionStoreException if a problem occurs during the read.
     */
    public Map<String, SubscriptionMetadata> getSubscriptions() {
        List<Map<String, Object>> results;
        try {
            results = persistentStore.get(PersistentStore.EVENT_SUBSCRIPTIONS_TYPE);
        } catch (PersistenceException e) {
            throw new SubscriptionStoreException("Exception while reading subscriptions from Solr: ",
                    e);
        }

        return results.stream()
                .map(this::formatIfPersistentItem)
                .map(this::mapToMetadata)
                .collect(Collectors.toMap(SubscriptionMetadata::getId, Function.identity()));
    }

    /**
     * Add the subscription metadata to the persistent store using its id for the document key.
     * <p>
     * Insertion operations with the same key overwrite the previous value, which means a duplicate
     * being added is a no-op, minus the network overhead.
     *
     * @param metadata the subscription metadata to add to the persistent store.
     * @throws SubscriptionStoreException if a problem occurs during insert.
     */
    public void insert(SubscriptionMetadata metadata) {
        LOGGER.debug("Adding [{}] to persistence store. ", metadata.getId());
        PersistentItem persistentSubscription = metadataToPersistentItem(metadata);
        try {
            persistentStore.add(PersistentStore.EVENT_SUBSCRIPTIONS_TYPE, persistentSubscription);
        } catch (PersistenceException e) {
            throw new SubscriptionStoreException("Exception while persisting subscription: ", e);
        }
    }

    /**
     * Remove the subscription metadata from the persistent store that corresponds to the given id.
     * <p>
     * Redundant deletes are valid as a no-op to support javax.cache events propogating write-through
     * processing to other cache nodes.
     *
     * @param subscriptionId the unique id of the subscription to delete.
     * @throws SubscriptionStoreException if a problem occurs during delete.
     */
    public void delete(String subscriptionId) {
        LOGGER.debug("Deleting [{}] from persistence store", subscriptionId);
        try {
            persistentStore.delete(PersistentStore.EVENT_SUBSCRIPTIONS_TYPE,
                    getEcqlStringForId(subscriptionId));
        } catch (PersistenceException e) {
            throw new SubscriptionStoreException("Exception while deleting subscription: ", e);
        }
    }

    /**
     * Given subscription metadata, transform it into a persistent item for storage.
     */
    private PersistentItem metadataToPersistentItem(SubscriptionMetadata metadata) {
        notNull(metadata, "subscription metadata cannot be null");

        PersistentItem persistentItem = new PersistentItem();
        persistentItem.addProperty(VERSION_LABEL, VERSION);
        persistentItem.addProperty(SUBSCRIPTION_ID_SOLR, metadata.getId());
        persistentItem.addProperty(SUBSCRIPTION_TYPE, metadata.getTypeName());
        persistentItem.addProperty(SUBSCRIPTION_FILTER, metadata.getFilter());
        persistentItem.addProperty(SUBSCRIPTION_CALLBACK, metadata.getCallbackAddress());

        return persistentItem;
    }

    /**
     * Given a result from the persistent store, transform the map back into valid subscription
     * metadata.
     */
    private SubscriptionMetadata mapToMetadata(Map<String, Object> map) {
        notEmpty(map, "map cannot be null or empty");

        String id = (String) map.get(SUBSCRIPTION_ID_SOLR);
        String type = (String) map.get(SUBSCRIPTION_TYPE);
        String serializedSubscription = (String) map.get(SUBSCRIPTION_FILTER);
        String callback = (String) map.get(SUBSCRIPTION_CALLBACK);

        return new SubscriptionMetadata(type, serializedSubscription, callback, id);
    }

    /**
     * There may be {@link PersistentStore} implementations that do not use suffix-based key labeling.
     * This pass-through method allows conditional formating of the retrieved results and tries to
     * reduce the implementation-dependence of the solution.
     */
    private Map<String, Object> formatIfPersistentItem(Map<String, Object> map) {
        if (map instanceof PersistentItem) {
            return PersistentItem.stripSuffixes(map);
        }
        return map;
    }

    /**
     * Generate the ECQL string used for deletes. It will select the entity with the provided
     * {@code subscriptionId}.
     */
    private String getEcqlStringForId(String subscriptionId) {
        return format(ECQL_FORMAT, SUBSCRIPTION_ID_SOLR, subscriptionId);
    }
}