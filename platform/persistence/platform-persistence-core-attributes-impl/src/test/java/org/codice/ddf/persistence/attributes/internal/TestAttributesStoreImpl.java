/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.persistence.attributes.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestAttributesStoreImpl {

    private AttributesStoreImpl attributesStore;

    private List<Map<String, Object>> attributesList;

    private PersistentStore persistentStore = mock(PersistentStore.class);

    private static final String USER = "user";

    private static final String CQL = String.format("%s = '%s'",
            AttributesStoreImpl.USER_KEY,
            USER);

    private static final String DATA_USAGE_LONG =
            AttributesStore.DATA_USAGE_KEY + PersistentItem.LONG_SUFFIX;

    @Before
    public void setup() {
        attributesStore = new AttributesStoreImpl(persistentStore);
    }

    @Test
    public void testGetDataUsage() throws PersistenceException {
        attributesList = new ArrayList<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(DATA_USAGE_LONG, 100L);
        attributesList.add(attributes);
        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(attributesList);

        long usage = attributesStore.getCurrentDataUsageByUser(USER);

        assertEquals(100L, usage);
    }

    @Test
    public void testUpdateDataUsage() throws PersistenceException {
        ArgumentCaptor<String> keyArg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyArg2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cqlArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);

        attributesList = new ArrayList<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(DATA_USAGE_LONG, 100L);
        attributesList.add(attributes);
        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(attributesList);

        attributesStore.updateUserDataUsage(USER, 500L);

        verify(persistentStore).get(keyArg1.capture(), cqlArg.capture());
        verify(persistentStore).add(keyArg2.capture(), itemArg.capture());

        assertEquals(PersistentStore.USER_ATTRIBUTE_TYPE, keyArg1.getValue());
        assertEquals(PersistentStore.USER_ATTRIBUTE_TYPE, keyArg2.getValue());

        assertEquals(600L,
                (long) itemArg.getValue()
                        .getLongProperty(AttributesStore.DATA_USAGE_KEY));

        assertEquals(CQL, cqlArg.getValue());
    }

    @Test
    public void testSetDataUsage() throws PersistenceException {

        long dataUsage = 500L;
        ArgumentCaptor<String> keyArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);
        attributesStore.setDataUsage(USER, dataUsage);
        verify(persistentStore).add(keyArg.capture(), itemArg.capture());
        assertEquals(PersistentStore.USER_ATTRIBUTE_TYPE, keyArg.getValue());
        assertEquals(dataUsage,
                (long) itemArg.getValue()
                        .getLongProperty(AttributesStore.DATA_USAGE_KEY));

    }

    @Test
    public void testSetDataUsageSizeLessThanZero() throws PersistenceException {

        long dataUsage = -1L;
        attributesStore.setDataUsage(USER, dataUsage);
        verify(persistentStore, never()).add(anyString(), anyMap());
    }

    @Test
    public void testUpdateDataUsageSizeLessThanZero() throws PersistenceException {

        long dataUsage = -1L;
        attributesStore.updateUserDataUsage(USER, dataUsage);
        verify(persistentStore, never()).add(anyString(), anyMap());
    }

    @Test(expected = PersistenceException.class)
    public void testSetDataUsageNullUsername() throws PersistenceException {

        attributesStore.setDataUsage(null, 500L);
    }

    @Test(expected = PersistenceException.class)
    public void testUpdateDataUsageNullUsername() throws PersistenceException {

        attributesStore.updateUserDataUsage(null, 500L);
    }

    @Test(expected = PersistenceException.class)
    public void testGetDataUsageNullUsername() throws PersistenceException {

        attributesStore.getCurrentDataUsageByUser(null);
    }

    @Test
    public void testPersistenceStoreReturnsNull() throws PersistenceException {

        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(null);

        assertEquals(0L, attributesStore.getCurrentDataUsageByUser(USER));
    }

    @Test
    public void testPersistenceStoreReturnsEmptyList() throws PersistenceException {

        when(persistentStore.get(any(String.class),
                any(String.class))).thenReturn(new ArrayList<Map<String, Object>>());

        assertEquals(0L, attributesStore.getCurrentDataUsageByUser(USER));
    }

    @Test(expected = PersistenceException.class)
    public void testPersistenceStoreThrowsExceptionOnGet() throws PersistenceException {

        when(persistentStore.get(any(String.class),
                any(String.class))).thenThrow(new PersistenceException());

        attributesStore.updateUserDataUsage(USER, 500L);

    }

}
