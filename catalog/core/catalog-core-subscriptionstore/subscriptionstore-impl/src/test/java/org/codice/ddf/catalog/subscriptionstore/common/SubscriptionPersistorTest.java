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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionPersistorTest {

    private static final String ECQL_FOR_ID = "\"id_txt\" LIKE 'df-87-6g-11'";

    private static final String SUBSCRIPTION_ID = "df-87-6g-11";

    private static final String SUBSCRIPTION_TYPE = "csw";

    private static final String SUBSCRIPTION_CALLBACK = "https://localhost:8993/test";

    private static final String SUBSCRIPTION_FILTER = "<xml>filter here</xml>";

    @Mock
    private PersistentStore mockPersistentStore;

    private SubscriptionPersistor persistor;

    private SubscriptionMetadata metadata;

    private Map<String, Object> nonPersistentItemResponse;

    @Before
    public void setup() throws Exception {
        metadata = new SubscriptionMetadata(SUBSCRIPTION_TYPE,
                SUBSCRIPTION_FILTER,
                SUBSCRIPTION_CALLBACK,
                SUBSCRIPTION_ID);

        nonPersistentItemResponse = new HashMap<>();
        nonPersistentItemResponse.put("id", SUBSCRIPTION_ID);
        nonPersistentItemResponse.put("type", SUBSCRIPTION_TYPE);
        nonPersistentItemResponse.put("callback", SUBSCRIPTION_CALLBACK);
        nonPersistentItemResponse.put("filter", SUBSCRIPTION_FILTER);

        persistor = new SubscriptionPersistor(mockPersistentStore);
    }

    @Test
    public void testValidInsert() throws Exception {
        persistor.insert(metadata);
        verify(mockPersistentStore).add(eq(PersistentStore.EVENT_SUBSCRIPTIONS_TYPE), anyMap());
        verifyNoMoreInteractions(mockPersistentStore);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testInsertThrowsException() throws Exception {
        doThrow(PersistenceException.class).when(mockPersistentStore)
                .add(anyString(), any(Map.class));
        persistor.insert(metadata);
    }

    @Test
    public void testValidDelete() throws Exception {
        persistor.delete(SUBSCRIPTION_ID);
        verify(mockPersistentStore).delete(eq(PersistentStore.EVENT_SUBSCRIPTIONS_TYPE),
                eq(ECQL_FOR_ID));
        verifyNoMoreInteractions(mockPersistentStore);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testDeleteThrowsException() throws Exception {
        doThrow(PersistenceException.class).when(mockPersistentStore)
                .delete(anyString(), anyString());
        persistor.delete(SUBSCRIPTION_ID);
    }

    @Test
    public void testInsertAndGetForPersistentItemRoundTrip() {
        persistor = new SubscriptionPersistor(new SingleItemPersistentStore());
        persistor.insert(metadata);

        Map<String, SubscriptionMetadata> results = persistor.getSubscriptions();
        SubscriptionMetadata resultMetadata = results.get(SUBSCRIPTION_ID);
        verifyIfMetadataIsCorrect(resultMetadata);
    }

    @Test
    public void testGetWhenResultIsNotPersistentItem() throws Exception {
        when(mockPersistentStore.get(eq(PersistentStore.EVENT_SUBSCRIPTIONS_TYPE))).thenReturn(
                Collections.singletonList(nonPersistentItemResponse));
        Map<String, SubscriptionMetadata> results = persistor.getSubscriptions();
        SubscriptionMetadata resultMetadata = results.get(SUBSCRIPTION_ID);
        verifyIfMetadataIsCorrect(resultMetadata);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testGetThrowsException() throws Exception {
        doThrow(PersistenceException.class).when(mockPersistentStore)
                .get(anyString());
        persistor.getSubscriptions();
    }

    private void verifyIfMetadataIsCorrect(SubscriptionMetadata resultMetadata) {
        assertThat("Metadata should not be null. ", resultMetadata != null);

        assertThat("Metadata IDs should match. ",
                metadata.getId()
                        .equals(resultMetadata.getId()));
        assertThat("Metadata types should match. ",
                metadata.getTypeName()
                        .equals(resultMetadata.getTypeName()));
        assertThat("Metadata callbacks should match. ",
                metadata.getCallbackAddress()
                        .equals(resultMetadata.getCallbackAddress()));
        assertThat("Metadata filters should match. ",
                metadata.getFilter()
                        .equals(resultMetadata.getFilter()));
    }

    /**
     * Keep a reference to the last item added to the persistent store.
     */
    private static class SingleItemPersistentStore implements PersistentStore {
        private Map<String, Object> data;

        public SingleItemPersistentStore() {
        }

        @Override
        public void add(String type, Map<String, Object> properties) throws PersistenceException {
            assertThat("Version should be present",
                    properties.containsKey("version_txt"),
                    is(true));
            data = properties;
        }

        @Override
        public void add(String type, Collection<Map<String, Object>> items)
                throws PersistenceException {
            for (Map<String, Object> item : items) {
                add(type, item);
            }
        }

        @Override
        public List<Map<String, Object>> get(String type) throws PersistenceException {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data);
        }

        @Override
        public List<Map<String, Object>> get(String type, String ecql) throws PersistenceException {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data);
        }

        @Override
        public int delete(String type, String ecql) throws PersistenceException {
            if (data == null) {
                return 0;
            }
            data = null;
            return 1;
        }
    }
}
