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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.internal.RegistryPublicationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;

public class TestRegistryStorePublisher extends RegistryStorePublisher {

    private static final String PUBLISH = "publish";

    private static final String UNPUBLISH = "unpublish";

    private RegistryStorePublisher registryStorePublisher;

    private RegistryPublicationService mockRegPubService;

    private FederationAdminService mockFedAdminService;

    private ScheduledExecutorService executorService;

    private ServiceReference serviceReference;

    private RegistryStore mockRegistryStore;

    private Optional<Metacard> optMetacard;

    private ServiceEvent mockServiceEvent;

    private BundleContext bundleContext;

    private Metacard mockIdentity;

    private Attribute registryId;

    private Event event;

    @Before
    public void setup() {
        registryId = new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "registryId");

        mockRegPubService = mock(RegistryPublicationService.class);
        mockFedAdminService = mock(FederationAdminService.class);
        executorService = mock(ScheduledExecutorService.class);
        serviceReference = mock(ServiceReference.class);
        mockRegistryStore = mock(RegistryStore.class);
        mockServiceEvent = mock(ServiceEvent.class);
        bundleContext = mock(BundleContext.class);
        mockIdentity = mock(Metacard.class);

        optMetacard = Optional.of(mockIdentity);

        registryStorePublisher = spy(new RegistryStorePublisher() {
            @Override
            BundleContext getBundleContext() {
                return bundleContext;
            }
        });

        registryStorePublisher.setExecutor(executorService);
        registryStorePublisher.setFederationAdminService(mockFedAdminService);
        registryStorePublisher.setRegistryPublicationService(mockRegPubService);

        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put(EventConstants.EVENT, mockServiceEvent);
        eventProperties.put(Constants.SERVICE_PID, "servicePid");
        event = new Event("org/osgi/framework/ServiceEvent/MODIFIED", eventProperties);
    }

    @Test
    public void testBindRegistryStoreNoContext() {
        registryStorePublisher = Mockito.spy(new RegistryStorePublisher());
        registryStorePublisher.bindRegistryStore(serviceReference);

        verify(registryStorePublisher, Mockito.times(0)).registryPublish(any(), anyString());
    }

    @Test
    public void testBindThenUnbindRegistryStoreAutoPush() {
        when(bundleContext.getService(any())).thenReturn(mockRegistryStore);
        when(mockRegistryStore.getConfigurationPid()).thenReturn("configPid");
        when(mockRegistryStore.isAvailable(any())).thenReturn(true);
        when(mockRegistryStore.isAutoPush()).thenReturn(true);
        when(mockRegistryStore.getRegistryId()).thenReturn("registryId");

        registryStorePublisher.bindRegistryStore(serviceReference);

        registryStorePublisher.unbindRegistryStore(serviceReference);

        verify(registryStorePublisher, Mockito.times(1)).registryPublish(any(), eq(PUBLISH));
    }

    @Test
    public void testUnbindRegistryStoreNoContext() {
        registryStorePublisher = Mockito.spy(new RegistryStorePublisher());
        registryStorePublisher.unbindRegistryStore(serviceReference);

        verify(registryStorePublisher, Mockito.times(0)).registryPublish(any(), eq(UNPUBLISH));
    }

    @Test
    public void testRegistryPublishFailed() throws Exception {
        RegistryStoreImpl mockRegistryStoreImpl = mock(RegistryStoreImpl.class);
        when(mockRegistryStoreImpl.getRegistryId()).thenReturn("registryId");
        when(mockFedAdminService.getLocalRegistryIdentityMetacard()).thenReturn(optMetacard);

        registryStorePublisher.registryPublish(mockRegistryStoreImpl, PUBLISH);

        verify(mockRegPubService, Mockito.times(0)).publish(any(), any());
    }

    @Test
    public void testRegistryPublish() throws Exception {
        when(mockRegistryStore.getRegistryId()).thenReturn("registryId");
        when(mockFedAdminService.getLocalRegistryIdentityMetacard()).thenReturn(optMetacard);
        when(mockIdentity.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)).thenReturn(
                registryId);

        setupSerialExecutor();

        registryStorePublisher.registryPublish(mockRegistryStore, PUBLISH);

        verify(mockRegPubService, Mockito.times(1)).publish(any(), any());
    }

    @Test
    public void testRegistryUnpublishFailed() throws Exception {
        RegistryStoreImpl mockRegistryStoreImpl = mock(RegistryStoreImpl.class);
        when(mockRegistryStoreImpl.getRegistryId()).thenReturn("registryId");
        when(mockFedAdminService.getLocalRegistryIdentityMetacard()).thenReturn(optMetacard);

        registryStorePublisher.registryPublish(mockRegistryStoreImpl, UNPUBLISH);

        verify(mockRegPubService, Mockito.times(0)).unpublish(any(), any());
    }

    @Test
    public void testRegistryUnpublish() throws Exception {
        when(mockRegistryStore.getRegistryId()).thenReturn("registryId");
        when(mockFedAdminService.getLocalRegistryIdentityMetacard()).thenReturn(optMetacard);
        when(mockIdentity.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)).thenReturn(
                registryId);

        setupSerialExecutor();

        registryStorePublisher.registryPublish(mockRegistryStore, UNPUBLISH);

        verify(mockRegPubService, Mockito.times(1)).unpublish(any(), any());
    }

    @Test
    public void testRegistryUnpublishNoId() throws Exception {
        when(mockRegistryStore.getRegistryId()).thenReturn("");

        registryStorePublisher.registryPublish(mockRegistryStore, UNPUBLISH);

        verify(mockRegPubService, Mockito.times(0)).unpublish(any(), any());
    }

    @Test
    public void testHandleFalseEvent() throws Exception {
        when(mockServiceEvent.getType()).thenReturn(ServiceEvent.UNREGISTERING);

        registryStorePublisher.handleEvent(event);

        verify(mockRegPubService, Mockito.times(0)).publish(any(), any());
    }

    @Test
    public void testHandleEventNoAction() throws Exception {
        when(mockServiceEvent.getType()).thenReturn(ServiceEvent.MODIFIED);
        registryStorePublisher.handleEvent(event);

        verify(mockRegPubService, Mockito.times(0)).publish(any(), any());
        verify(mockRegPubService, Mockito.times(0)).unpublish(any(), any());
    }

    @Test
    public void testHandleEventPublish() throws Exception {
        when(mockServiceEvent.getType()).thenReturn(ServiceEvent.MODIFIED);
        when(bundleContext.getService(any())).thenReturn(mockRegistryStore);
        when(mockRegistryStore.getConfigurationPid()).thenReturn("servicePid");
        when(mockRegistryStore.isAvailable(any())).thenReturn(true);
        when(mockRegistryStore.isAutoPush()).thenReturn(false);
        when(mockRegistryStore.getRegistryId()).thenReturn("registryId");

        registryStorePublisher.bindRegistryStore(serviceReference);

        when(mockRegistryStore.isAutoPush()).thenReturn(true);

        registryStorePublisher.handleEvent(event);

        verify(registryStorePublisher, Mockito.times(1)).registryPublish(any(), eq(PUBLISH));
    }

    @Test
    public void testHandleEventUnpublish() throws Exception {
        when(mockServiceEvent.getType()).thenReturn(ServiceEvent.MODIFIED);
        when(bundleContext.getService(any())).thenReturn(mockRegistryStore);
        when(mockRegistryStore.getConfigurationPid()).thenReturn("servicePid");
        when(mockRegistryStore.isAvailable(any())).thenReturn(true);
        when(mockRegistryStore.isAutoPush()).thenReturn(true);
        when(mockRegistryStore.getRegistryId()).thenReturn("registryId");

        registryStorePublisher.bindRegistryStore(serviceReference);

        when(mockRegistryStore.isAutoPush()).thenReturn(false);

        registryStorePublisher.handleEvent(event);

        verify(registryStorePublisher, Mockito.times(1)).registryPublish(any(), eq(PUBLISH));
        verify(registryStorePublisher, Mockito.times(1)).registryPublish(any(), eq(UNPUBLISH));
    }

    @Test
    public void testDestroy() throws Exception {
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        registryStorePublisher.destroy();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(0)).shutdownNow();
    }

    @Test
    public void testDestroyTerminateTasks() throws Exception {
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
        registryStorePublisher.destroy();
        verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void testDestroyInterupt() throws Exception {
        when(executorService.awaitTermination(anyLong(),
                any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));
        registryStorePublisher.destroy();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(1)).shutdownNow();
    }

    private void setupSerialExecutor() {
        doAnswer((args) -> {
            ((Runnable) args.getArguments()[0]).run();
            return null;
        }).when(executorService)
                .schedule(isA(Runnable.class), anyLong(), any());
    }
}
