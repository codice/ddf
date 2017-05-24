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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;

import javax.cache.integration.CacheWriterException;

import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionPersistor;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionCacheWriterTest {

    @Mock
    private SubscriptionMetadata mockMetadata;

    @Mock
    private SubscriptionPersistor mockPersistor;

    private SubscriptionCacheWriter cacheWriter;

    @Before
    public void setup() {
        when(mockMetadata.getId()).thenReturn("id");
        when(mockMetadata.getTypeName()).thenReturn("type");
        when(mockMetadata.getFilter()).thenReturn("filter");
        when(mockMetadata.getCallbackAddress()).thenReturn("callback");
        cacheWriter = new SubscriptionCacheWriter(mockPersistor);
    }

    @Test
    public void testWrite() {
        cacheWriter.write(new CacheEntryTestImpl(mockMetadata));
        verify(mockPersistor).insert(mockMetadata);
        verifyNoMoreInteractions(mockPersistor);
    }

    @Test
    public void testWriteAll() {
        Collection entries = ImmutableList.of(new CacheEntryTestImpl(mockMetadata),
                new CacheEntryTestImpl(mockMetadata));
        cacheWriter.writeAll(entries);
        verify(mockPersistor, times(2)).insert(mockMetadata);
        verifyNoMoreInteractions(mockPersistor);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testOnWritePersistorExceptionNotSurpressed() {
        doThrow(SubscriptionStoreException.class).when(mockPersistor)
                .insert(anyObject());
        cacheWriter.write(new CacheEntryTestImpl(mockMetadata));
    }

    @Test
    public void testDelete() {
        String key = "key";
        cacheWriter.delete(key);
        verify(mockPersistor).delete(key);
        verifyNoMoreInteractions(mockPersistor);
    }

    @Test(expected = CacheWriterException.class)
    public void testDeleteWithNonStringKey() {
        cacheWriter.delete(5);
    }

    @Test
    public void testDeleteAll() {
        Collection entries = ImmutableList.of("key1", "key2");
        cacheWriter.deleteAll(entries);
        verify(mockPersistor).delete("key1");
        verify(mockPersistor).delete("key2");
        verifyNoMoreInteractions(mockPersistor);
    }

    @Test(expected = CacheWriterException.class)
    public void testDeleteAllWhenSomeKeyIsntString() {
        Collection entries = ImmutableList.of("key1", "key2", 5, "key3");
        cacheWriter.deleteAll(entries);
        verifyZeroInteractions(mockPersistor);
    }
}
