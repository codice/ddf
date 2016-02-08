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
package org.codice.ddf.persistence.attributes.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestAttributesStoreImpl {

    AttributesStoreImpl attributesStore;

    List<Map<String, Object>> attributesList;

    PersistentStore persistentStore = mock(PersistentStore.class);

    @Before
    public void setup() {

        attributesStore = new AttributesStoreImpl(persistentStore);

    }

    @Test
    public void testNormal() throws PersistenceException {
        attributesList = new ArrayList<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("data_usage_lng", 100L);
        attributesList.add(attributes);
        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(attributesList);

        long usage = attributesStore.getCurrentDataUsageByUser("user");

        assertEquals(100L, usage);
        attributesStore.updateUserDataUsage("user", 500L);

    }

    @Test
    public void testPersistenceStoreReturnsNull() throws PersistenceException {

        when(persistentStore.get(any(String.class), any(String.class))).thenReturn(null);

        assertEquals(0L, attributesStore.getCurrentDataUsageByUser("user"));
        attributesStore.updateUserDataUsage("user", 500L);

    }

    @Test
    public void testPersistenceStoreReturnsEmptyList() throws PersistenceException {

        when(persistentStore.get(any(String.class),
                any(String.class))).thenReturn(new ArrayList<Map<String, Object>>());

        assertEquals(0L, attributesStore.getCurrentDataUsageByUser("user"));
        attributesStore.updateUserDataUsage("user", 500L);

    }

    @Test
    public void testPersistenceStoreThrowsException() throws PersistenceException {

        when(persistentStore.get(any(String.class),
                any(String.class))).thenThrow(new PersistenceException());

        assertEquals(0L, attributesStore.getCurrentDataUsageByUser("user"));

        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws PersistenceException {
                throw new PersistenceException();
            }
        })
                .when(persistentStore)
                .add(anyString(), anyMap());

        attributesStore.updateUserDataUsage("user", 500L);

    }

}
