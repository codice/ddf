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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;

import javax.cache.Cache;
import javax.cache.CacheException;

import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.internal.MarshalledSubscription;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionIdentifier;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableList;

import ddf.catalog.event.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionContainerImplTest {

    private static final String SOME_ID = "id";

    private static final String SOME_TYPE = "type";

    private static final String SOME_FILTER = "filter";

    private static final String SOME_CALLBACK = "http://localhost8993/test";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private static BundleContext mockBundleContext;

    @Mock
    private static Cache<String, CachedSubscription> mockCache;

    @Mock
    private static SubscriptionFactory mockFactoryA;

    @Mock
    private static SubscriptionFactory mockFactoryB;

    @Mock
    private Subscription mockArgSubscription;

    @Mock
    private SubscriptionIdentifier mockArgIdentifier;

    @Mock
    private MarshalledSubscription mockArgMarshalled;

    private static Map<String, SubscriptionFactory> factories = new HashMap<>();

    private SubscriptionContainerImpl container;

    @Before
    public void setup() {
        when(mockArgIdentifier.getId()).thenReturn(SOME_ID);
        when(mockArgIdentifier.getTypeName()).thenReturn(SOME_TYPE);
        when(mockArgMarshalled.getFilter()).thenReturn(SOME_FILTER);
        when(mockArgMarshalled.getCallbackAddress()).thenReturn(SOME_CALLBACK);

        when(mockFactoryA.getTypeName()).thenReturn("A");
        when(mockFactoryB.getTypeName()).thenReturn("B");

        container = new ContainerUnderTest();

        factories.put("A", mockFactoryA);
        factories.put("B", mockFactoryB);
    }

    @After
    public void cleanup() {
        factories.clear();
    }

    //region UTIL

    @Test
    public void testInit() {
        FactoryUpdateDataGroup group = new FactoryUpdateDataGroup();
        Spliterator spliterator = group.getSpliterator();
        when(mockCache.spliterator()).thenReturn(spliterator);

        container.init();

        verify(mockCache).loadAll(anySet(), eq(false), anyObject());
        verify(group.registeredSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.typeMismatchSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.validSubA).registerSubscription(eq(mockFactoryA));
        verify(group.validSubB).registerSubscription(eq(mockFactoryB));
    }

    @Test
    public void testBindFactory() {
        FactoryUpdateDataGroup group = new FactoryUpdateDataGroup();
        Spliterator spliterator = group.getSpliterator();
        when(mockCache.spliterator()).thenReturn(spliterator);

        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");

        container.bindFactory(factory);

        verify(group.registeredSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.typeMismatchSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.validSubZ).registerSubscription(eq(factory));

        assertThat(factories.containsKey("Z"), is(true));
        assertThat(factories.get("Z"), is(factory));
    }

    @Test
    public void testBindFactoryNullDoesNoOp() {
        Spliterator spliterator = mock(Spliterator.class);
        when(mockCache.spliterator()).thenReturn(spliterator);

        container.bindFactory(null);

        verifyZeroInteractions(spliterator);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testBindFactoryDuplicateThrowsException() {
        Spliterator spliterator = mock(Spliterator.class);
        when(mockCache.spliterator()).thenReturn(spliterator);

        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");
        factories.put("Z", factory);

        container.bindFactory(factory);

        verifyZeroInteractions(spliterator);
    }

    @Test
    public void testUnbindFactory() {
        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");
        factories.put("Z", factory);

        container.unbindFactory(factory);
        assertThat(factories.get("Z"), is(nullValue()));
    }

    @Test
    public void testUnbindFactoryNullDoesNoOp() {
        Map<String, SubscriptionFactory> mockFactoryMap = mock(Map.class);
        container = new ContainerUnderTest() {
            @Override
            Map<String, SubscriptionFactory> createFactoryMap() {
                return mockFactoryMap;
            }
        };
        container.unbindFactory(null);
        verifyZeroInteractions(mockFactoryMap);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testUnbindNonexistentFactoryThrowsException() {
        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");
        container.unbindFactory(factory);
    }

    @Test
    public void testContainsTrue() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.isNotType(SOME_TYPE)).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(mockArgSubscription));

        when(mockCache.get(SOME_ID)).thenReturn(cachedSub);
        assertThat(container.contains(mockArgIdentifier), is(true));
    }

    @Test
    public void testContainsFalse() {
        when(mockCache.get(SOME_ID)).thenReturn(null);
        assertThat(container.contains(mockArgIdentifier), is(false));
    }

    //endregion

    //region GET

    @Test
    public void testGetSubscription() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.isNotType(SOME_TYPE)).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(mockArgSubscription));

        when(mockCache.get(SOME_ID)).thenReturn(cachedSub);
        assertThat(container.get(mockArgIdentifier), is(mockArgSubscription));
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testGetSubscriptionWhenOptionalIsEmpty() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.isNotType(SOME_TYPE)).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);
        when(cachedSub.getSubscription()).thenReturn(Optional.empty());

        when(mockCache.get(SOME_ID)).thenReturn(cachedSub);
        container.get(mockArgIdentifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSubscriptionUsingNullIdentifier() {
        container.get(null);
    }

    @Test
    public void testGetNullSubscription() {
        when(mockCache.get(SOME_ID)).thenReturn(null);
        assertThat(container.get(mockArgIdentifier), is(nullValue()));
    }

    @Test
    public void testGetWrongTypeOfSubscription() {
        CachedSubscription sub = mock(CachedSubscription.class);
        when(sub.isNotType(SOME_TYPE)).thenReturn(true);

        when(mockCache.get(SOME_ID)).thenReturn(sub);
        assertThat(container.get(mockArgIdentifier), is(nullValue()));
    }

    @Test
    public void testGetUnregisteredSubscription() {
        CachedSubscription sub = mock(CachedSubscription.class);
        when(sub.isNotType(SOME_TYPE)).thenReturn(false);
        when(sub.isNotRegistered()).thenReturn(true);

        when(mockCache.get(SOME_ID)).thenReturn(sub);
        assertThat(container.get(mockArgIdentifier), is(nullValue()));
    }

    //endregion

    //region INSERT

    @Test
    public void testInsertSubscription() {
        container.insert(mockArgSubscription, mockArgMarshalled, mockArgIdentifier);

        ArgumentCaptor<CachedSubscription> argCaptor =
                ArgumentCaptor.forClass(CachedSubscription.class);
        verify(mockCache).put(anyString(), argCaptor.capture());

        CachedSubscription arg = argCaptor.getValue();
        SubscriptionMetadata metadata = arg.getMetadata();

        assertThat(metadata.getTypeName(), is(SOME_TYPE));
        assertThat(metadata.getFilter(), is(SOME_FILTER));
        assertThat(metadata.getCallbackAddress(), is(SOME_CALLBACK));

        if (arg.getSubscription()
                .isPresent()) {
            assertThat(arg.isNotRegistered(), is(false));
            assertThat(arg.getSubscription()
                    .get(), is(mockArgSubscription));
        } else {
            fail("Subscription was null on the cache object. ");
        }
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testInsertWhenPersistorThrowsException() {
        doThrow(CacheException.class).when(mockCache)
                .put(anyString(), any(CachedSubscription.class));
        container.insert(mockArgSubscription, mockArgMarshalled, mockArgIdentifier);

        ArgumentCaptor<CachedSubscription> argCaptor =
                ArgumentCaptor.forClass(CachedSubscription.class);
        verify(mockCache).put(anyString(), argCaptor.capture());

        CachedSubscription arg = argCaptor.getValue();
        assertThat(arg.isNotRegistered(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertSubscriptionUsingNullSubscription() {
        container.insert(null, mockArgMarshalled, mockArgIdentifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertSubscriptionUsingNullMarshalledSubscription() {
        container.insert(mockArgSubscription, null, mockArgIdentifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertSubscriptionUsingNullSubscriptionType() {
        container.insert(mockArgSubscription, mockArgMarshalled, null);
    }

    //endregion

    //region UPDATE
    @Test
    public void testUpdate() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(mockArgSubscription));
        when(cachedSub.isNotType(anyString())).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);

        when(mockCache.get(anyString())).thenReturn(cachedSub);

        container.update(mockArgSubscription, mockArgMarshalled, mockArgIdentifier);

        ArgumentCaptor<CachedSubscription> argCaptor =
                ArgumentCaptor.forClass(CachedSubscription.class);
        verify(mockCache).remove(eq(SOME_ID));
        verify(mockCache).put(eq(SOME_ID), argCaptor.capture());

        CachedSubscription arg = argCaptor.getValue();
        SubscriptionMetadata metadata = arg.getMetadata();

        assertThat(metadata.getId(), is(SOME_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSubscriptionUsingNullSubscription() {
        container.update(null, mockArgMarshalled, mockArgIdentifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSubscriptionUsingNullMarshalledSubscription() {
        container.update(mockArgSubscription, null, mockArgIdentifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSubscriptionUsingNullSubscriptionIdentifier() {
        container.update(mockArgSubscription, mockArgMarshalled, null);
    }
    //endregion

    //region DELETE
    @Test
    public void testDelete() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(mockArgSubscription));
        when(cachedSub.isNotType(anyString())).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);

        when(mockCache.get(anyString())).thenReturn(cachedSub);
        container.delete(mockArgIdentifier);

        verify(mockCache).remove(eq(SOME_ID));
        verify(cachedSub).unregisterSubscription();
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testDeleteWhenSubscriptionDoesNotExist() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(mockArgSubscription));
        when(cachedSub.isNotType(anyString())).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);

        when(mockCache.get(anyString())).thenReturn(null);
        container.delete(mockArgIdentifier);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testDeleteWhenCacheThrowsException() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(mockArgSubscription));
        when(cachedSub.isNotType(anyString())).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);

        when(mockCache.get(anyString())).thenReturn(cachedSub);
        doThrow(CacheException.class).when(mockCache)
                .remove(eq(SOME_ID));
        container.delete(mockArgIdentifier);

        verify(cachedSub, never()).unregisterSubscription();
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testDeleteWhenOptionalIsEmpty() {
        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.getSubscription()).thenReturn(Optional.empty());
        when(cachedSub.isNotType(anyString())).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);

        when(mockCache.get(anyString())).thenReturn(cachedSub);
        container.delete(mockArgIdentifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteSubscriptionUsingNullSubscriptionIdentifier() {
        container.delete(null);
    }
    //endregion

    private static class ContainerUnderTest extends SubscriptionContainerImpl {

        public ContainerUnderTest() {
            super(null, null);
        }

        @Override
        Cache<String, CachedSubscription> createCache(SubscriptionCacheLoader cacheLoader,
                SubscriptionCacheWriter cacheWriter) {
            return mockCache;
        }

        @Override
        Map<String, SubscriptionFactory> createFactoryMap() {
            return factories;
        }

        @Override
        CachedSubscription createCachedSubscription(SubscriptionMetadata metadata) {
            return new CachedSubscription(metadata) {
                @Override
                protected BundleContext getBundleContext() {
                    return mockBundleContext;
                }
            };
        }
    }

    private static class FactoryUpdateDataGroup {

        CachedSubscription registeredSub;

        CachedSubscription typeMismatchSub;

        CachedSubscription validSubA;

        CachedSubscription validSubB;

        CachedSubscription validSubZ;

        public FactoryUpdateDataGroup() {
            registeredSub = makeMockForType("donotcare", "id-dnc", false);
            typeMismatchSub = makeMockForType("C", "id-C");
            validSubA = makeMockForType("A", "id-A");
            validSubB = makeMockForType("B", "id-B");
            validSubZ = makeMockForType("Z", "id-Z");

            when(validSubZ.isType(anyString())).thenReturn(true);
        }

        Spliterator getSpliterator() {
            return ImmutableList.of(wrap(registeredSub),
                    wrap(typeMismatchSub),
                    wrap(validSubA),
                    wrap(validSubB),
                    wrap(validSubZ))
                    .spliterator();
        }

        private CachedSubscription makeMockForType(String type, String id) {
            return makeMockForType(type, id, true);
        }

        private CachedSubscription makeMockForType(String type, String id,
                boolean isNotRegistered) {
            CachedSubscription sub = mock(CachedSubscription.class, RETURNS_DEEP_STUBS);
            when(sub.isNotRegistered()).thenReturn(isNotRegistered);
            when(sub.getMetadata()
                    .getTypeName()).thenReturn(type);
            when(sub.getMetadata()
                    .getId()).thenReturn(id);
            return sub;
        }

        private CacheEntryTestImpl wrap(CachedSubscription sub) {
            return new CacheEntryTestImpl(sub);
        }
    }
}
