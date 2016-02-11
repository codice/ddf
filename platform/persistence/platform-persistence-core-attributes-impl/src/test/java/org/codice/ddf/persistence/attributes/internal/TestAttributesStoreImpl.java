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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;

public class TestAttributesStoreImpl {

    private AttributesStoreImpl attributesStore;

    private List<Map<String, Object>> attributesList;

    private PersistentStore persistentStore = mock(PersistentStore.class);

    private static final String USER = "user";

    @Before
    public void setup() {
        attributesStore = new AttributesStoreImpl(persistentStore);
    }

    @Test
    public void testGetDataUsage() throws PersistenceException {
        attributesList = new ArrayList<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("data_usage_lng", 100L);
        attributesList.add(attributes);
        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(attributesList);

        long usage = attributesStore.getCurrentDataUsageByUser(USER);

        assertEquals(100L, usage);
    }

    @Test
    public void testUpdateDataUsage() throws PersistenceException {
        attributesList = new ArrayList<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("data_usage_lng", 100L);
        attributesList.add(attributes);
        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(attributesList);

        attributesStore.updateUserDataUsage(USER, 500L);
    }

    @Test
    public void testSetDataUsage() throws PersistenceException {
        attributesList = new ArrayList<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("data_usage_lng", 100L);
        attributesList.add(attributes);
        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(attributesList);

        attributesStore.setDataUsage(USER, 500L);
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
        attributesStore.updateUserDataUsage(USER, 500L);
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
