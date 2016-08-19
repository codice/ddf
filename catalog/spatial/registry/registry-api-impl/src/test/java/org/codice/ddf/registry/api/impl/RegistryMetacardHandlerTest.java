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
package org.codice.ddf.registry.api.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.event.EventProcessor;

public class RegistryMetacardHandlerTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private FederationAdminService federationAdmin;

    private RegistryMetacardHandler rmh;

    private MetacardImpl mcardInternal;

    private MetacardImpl mcard;

    private Event event;

    private Dictionary<String, Object> eventProperties;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        rmh = new RegistryMetacardHandler(executorService, federationAdmin);
        mcardInternal = new MetacardImpl();
        mcardInternal.setModifiedDate(new Date());
        mcardInternal.setId("internalId");
        mcardInternal.setAttribute(Metacard.TAGS, RegistryConstants.REGISTRY_TAG_INTERNAL);
        mcardInternal.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                "testRegId"));
        mcard = new MetacardImpl();
        mcard.setId("id");
        mcard.setModifiedDate(new Date());
        mcard.setAttribute(Metacard.TAGS, RegistryConstants.REGISTRY_TAG);
        mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "testRegId"));
        eventProperties = new Hashtable<>();
        eventProperties.put("ddf.catalog.event.metacard", mcardInternal);
        event = new Event("myevent", eventProperties);
    }

    @Test
    public void testNullMetacard() {
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        Event event = new Event("myevent", eventProperties);
        rmh.handleEvent(event);
        verify(executorService, never()).execute(any(Runnable.class));
    }

    @Test
    public void testNonRegistryInternalMetacard() {
        mcardInternal.setAttribute(Metacard.TAGS, Metacard.DEFAULT_TAG);
        rmh.handleEvent(event);
        verify(executorService, never()).execute(any(Runnable.class));
    }

    @Test
    public void testCreateEvent() throws Exception {
        event = new Event(EventProcessor.EVENTS_TOPIC_CREATED, eventProperties);
        setupSerialExecutor();
        when(federationAdmin.getRegistryMetacardsByRegistryIds(any())).thenReturn(Collections.emptyList());
        rmh.handleEvent(event);
        verify(federationAdmin).addRegistryEntry(mcardInternal);
        assertThat(mcardInternal.getTags()
                .contains(RegistryConstants.REGISTRY_TAG), is(true));
    }

    @Test
    public void testUpdateEventNoNewData() throws Exception {
        event = new Event(EventProcessor.EVENTS_TOPIC_UPDATED, eventProperties);
        setupSerialExecutor();
        when(federationAdmin.getRegistryMetacardsByRegistryIds(any())).thenReturn(Collections.singletonList(
                mcard));
        rmh.handleEvent(event);
        verify(federationAdmin, never()).addRegistryEntry(mcardInternal);
        verify(federationAdmin, never()).updateRegistryEntry(mcardInternal);
    }

    @Test
    public void testUpdateEvent() throws Exception {
        event = new Event(EventProcessor.EVENTS_TOPIC_UPDATED, eventProperties);
        setupSerialExecutor();
        mcard.setModifiedDate(new Date(mcardInternal.getModifiedDate()
                .getTime() - 1000));
        when(federationAdmin.getRegistryMetacardsByRegistryIds(any())).thenReturn(Collections.singletonList(
                mcard));
        rmh.handleEvent(event);
        verify(federationAdmin, never()).addRegistryEntry(mcardInternal);
        verify(federationAdmin).updateRegistryEntry(mcardInternal);
        assertThat(mcardInternal.getTags()
                .contains(RegistryConstants.REGISTRY_TAG), is(true));
    }

    @Test
    public void testDeleteEvent() throws Exception {
        event = new Event(EventProcessor.EVENTS_TOPIC_DELETED, eventProperties);
        setupSerialExecutor();
        when(federationAdmin.getRegistryMetacardsByRegistryIds(any())).thenReturn(Collections.singletonList(
                mcard));
        when(federationAdmin.getInternalRegistryMetacardsByRegistryId(any())).thenReturn(Collections.emptyList());
        rmh.handleEvent(event);
        verify(federationAdmin).deleteRegistryEntriesByRegistryIds(any());
    }

    @Test
    public void testDestroy() throws Exception {
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        rmh.destroy();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(0)).shutdownNow();
    }

    @Test
    public void testDestroyTerminateTasks() throws Exception {
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
        rmh.destroy();
        verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void testDestroyInterupt() throws Exception {
        when(executorService.awaitTermination(anyLong(),
                any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));
        rmh.destroy();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(1)).shutdownNow();
    }

    private void setupSerialExecutor() {
        doAnswer((args) -> {
            ((Runnable) args.getArguments()[0]).run();
            return null;
        }).when(executorService)
                .execute(any());
    }
}
