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
package org.codice.ddf.catalog.subscriptionstore;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.NotImplementedException;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.geotools.filter.FilterTransformer;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.event.Subscription;

/**
 * The {@link SubscriptionStore} serves as the data-access object for all {@link Subscription}s across
 * any endpoint or provider. The object that actually gets stored is a {@link SubscriptionMetadata}, but
 * combined with a {@link org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory}, the
 * original subscription object can be re-created and registered with the OSGi framework.
 */
public class SubscriptionStore implements CacheLoader<String, SubscriptionMetadata>,
        CacheWriter<String, SubscriptionMetadata> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionStore.class);

    private static final String SUBSCRIPTION_ID = "subscription-id";

    private static final String SUBSCRIPTION_TYPE = "subscription-type";

    private static final String SUBSCRIPTION_MESSAGE_BODY = "subscription-body";

    private static final String CALLBACK_URL = "callback-url";

    private final PersistentStore persistentStore;

    public SubscriptionStore(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
    }

    @Override
    public SubscriptionMetadata load(String s) throws CacheLoaderException {
        return getSubscriptions().get(s);
    }

    @Override
    public Map<String, SubscriptionMetadata> loadAll(Iterable<? extends String> iterable)
            throws CacheLoaderException {
        return getSubscriptions();
    }

    @Override
    public void write(Cache.Entry<? extends String, ? extends SubscriptionMetadata> entry)
            throws CacheWriterException {
        throw new NotImplementedException();
    }

    @Override
    public void writeAll(
            Collection<Cache.Entry<? extends String, ? extends SubscriptionMetadata>> collection)
            throws CacheWriterException {
        throw new NotImplementedException();
    }

    @Override
    public void delete(Object o) throws CacheWriterException {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAll(Collection<?> collection) throws CacheWriterException {
        throw new NotImplementedException();
    }

    public Map<String, SubscriptionMetadata> getSubscriptions() {
        List<Map<String, Object>> results;
        try {
            results = persistentStore.get(PersistentStore.SUBSCRIBED_QUERY_TYPE);
        } catch (PersistenceException e) {
            LOGGER.debug("Exception while reading subscriptions from Solr: ", e);
            return Collections.emptyMap();
        }

        return results.stream()
                .map(this::formatIfPersistentItem)
                .map(this::mapToMetadata)
                .collect(Collectors.toMap(SubscriptionMetadata::getId, Function.identity()));
    }

    public void insert(SubscriptionMetadata subscription) {
        PersistentItem persistentSubscription = metadataToPersistentItem(subscription);
        try {
            persistentStore.add(PersistentStore.SUBSCRIBED_QUERY_TYPE, persistentSubscription);
        } catch (PersistenceException e) {
            throw new SubscriptionStoreException("Exception while persisting subscription: ", e);
        }
    }

    public void delete(String subscriptionId) {
        try {
            // TODO: Ecql processing - do we need to account for non-stripped suffixes?
            String ecql = format("\"%s_txt\" LIKE '%s'", SUBSCRIPTION_ID, subscriptionId);
            persistentStore.delete(PersistentStore.SUBSCRIBED_QUERY_TYPE, ecql);
        } catch (PersistenceException e) {
            throw new SubscriptionStoreException("Exception while deleting subscription: ", e);
        }
    }

    private PersistentItem metadataToPersistentItem(SubscriptionMetadata metadata) {
        notNull(metadata, "subscription cannot be null");

        PersistentItem persistentItem = new PersistentItem();
        persistentItem.addProperty(SUBSCRIPTION_ID, metadata.getId());
        persistentItem.addProperty(SUBSCRIPTION_TYPE, metadata.getType());
        // TODO: Subscription message body is switching to binary for hash support
        persistentItem.addProperty(SUBSCRIPTION_MESSAGE_BODY, metadata.getMessageBody());
        persistentItem.addProperty(CALLBACK_URL,
                metadata.getCallbackUrl()
                        .toString());

        return persistentItem;
    }

    private SubscriptionMetadata mapToMetadata(Map<String, Object> map) {
        notEmpty(map, "persistentItem cannot be null");

        String id = (String) map.get(SUBSCRIPTION_ID);
        String type = (String) map.get(SUBSCRIPTION_TYPE);
        String messageBody = (String) map.get(SUBSCRIPTION_MESSAGE_BODY);
        String callback = (String) map.get(CALLBACK_URL);

        return new SubscriptionMetadata(type, messageBody, callback, id);
    }

    private Map<String, Object> formatIfPersistentItem(Map<String, Object> map) {
        if (map instanceof PersistentItem) {
            return PersistentItem.stripSuffixes(map);
        }
        return map;
    }

    /**
     * This logic is being retained for reference purposes and may not be used in the final product.
     */
    private String persistPureOgcFilter(Filter filter) {
        FilterTransformer transformer = new FilterTransformer();
        try {
            return transformer.transform(filter);
        } catch (TransformerException e) {
            throw new RuntimeException("Could not serialize subscription filter. ", e);
        }
    }

    /**
     * This logic is being retained for reference purposes and may not be used in the final product.
     */
    private Filter regeneratePureOgcFilter(String filterXml) {
        Parser parser = new Parser(new org.geotools.filter.v1_0.OGCConfiguration());
        StringReader reader = new StringReader(filterXml);
        try {
            return (Filter) parser.parse(reader);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException("Exception occurred while deserializing the filter. ", e);
        }
    }
}