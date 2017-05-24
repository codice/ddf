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
import static org.apache.commons.lang.Validate.notNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionPersistor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Tests for the cache loader. Note that for {@link SubscriptionCacheLoader#loadAll(Iterable)}, the
 * set of keys does not matter. The backing store is the single source of truth. And the tests reflect
 * this.
 *
 * @see SubscriptionCacheLoader#loadAll(Iterable)
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionCacheLoaderTest {

    @Mock
    private SubscriptionPersistor mockPersistor;

    @Mock
    private SubscriptionMetadata mockMetadata;

    private SubscriptionCacheLoader cacheLoader;

    @Before
    public void setup() {
        cacheLoader = new SubscriptionCacheLoader(mockPersistor);
    }

    @Test
    public void testLoad() {
        Map<String, SubscriptionMetadata> results = ImmutableMap.of("id", mockMetadata);
        when(mockPersistor.getSubscriptions()).thenReturn(results);
        CachedSubscription cachedSubscription = cacheLoader.load("id");
        assertThat(cachedSubscription.getMetadata(), is(mockMetadata));
    }

    @Test
    public void testLoadReturnsNull() {
        Map<String, SubscriptionMetadata> results = ImmutableMap.of("id", mockMetadata);
        when(mockPersistor.getSubscriptions()).thenReturn(results);
        CachedSubscription cachedSubscription = cacheLoader.load("not_id");
        assertThat(cachedSubscription, is(nullValue()));
    }

    @Test
    public void testLoadAll() {
        Map<String, SubscriptionMetadata> results = generatePersistorLoadAllMap();
        when(mockPersistor.getSubscriptions()).thenReturn(results);
        Map<String, CachedSubscription> map = cacheLoader.loadAll(Collections.emptySet());
        validateLoadAllResults(map);
    }

    @Test
    public void testLoadAllWithNonEmptyKeySet() {
        Map<String, SubscriptionMetadata> results = generatePersistorLoadAllMap();
        when(mockPersistor.getSubscriptions()).thenReturn(results);
        Map<String, CachedSubscription> map = cacheLoader.loadAll(ImmutableSet.of("id1", "id2"));
        validateLoadAllResults(map);
    }

    private Map generatePersistorLoadAllMap() {
        return ImmutableMap.builder()
                .put("id1", new SubscriptionMetadata("t1", "f1", asValidUrl("cb1"), "id1"))
                .put("id2", new SubscriptionMetadata("t2", "f2", asValidUrl("cb2"), "id2"))
                .put("id3", new SubscriptionMetadata("t3", "f3", asValidUrl("cb3"), "id3"))
                .build();
    }

    private void validateLoadAllResults(Map<String, CachedSubscription> map) {
        validateSubscriptionMetadata(map.get("id1")
                .getMetadata(), "t1", "f1", asValidUrl("cb1"), "id1");
        validateSubscriptionMetadata(map.get("id2")
                .getMetadata(), "t2", "f2", asValidUrl("cb2"), "id2");
        validateSubscriptionMetadata(map.get("id3")
                .getMetadata(), "t3", "f3", asValidUrl("cb3"), "id3");
    }

    private void validateSubscriptionMetadata(SubscriptionMetadata metadata, String type,
            String filter, String callback, String id) {
        notNull(metadata);
        assertThat(metadata.getId(), is(id));
        assertThat(metadata.getTypeName(), is(type));
        assertThat(metadata.getFilter(), is(filter));
        assertThat(metadata.getCallbackAddress(), is(callback));
    }

    private String asValidUrl(String variation) {
        return format("http://localhost:8993/%s", variation);
    }
}
