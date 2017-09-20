/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.api.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

@RunWith(MockitoJUnitRunner.class)
public class RegistryStoreCleanupHandlerTest {
  @Mock private ExecutorService executorService;

  @Mock private FederationAdminService federationAdmin;

  @Mock private BundleContext context;

  @Mock private RegistryStore store;

  private MetacardImpl mcard;

  private Event event;

  private Dictionary<String, Object> eventProperties;

  private RegistryStoreCleanupHandler cleanupHandler;

  @Before
  public void setup() {

    cleanupHandler =
        new RegistryStoreCleanupHandler() {
          @Override
          BundleContext getBundleContext() {
            return context;
          }
        };
    cleanupHandler.setExecutor(executorService);
    cleanupHandler.setFederationAdminService(federationAdmin);
    setupSerialExecutor();

    when(context.getService(any(ServiceReference.class))).thenReturn(store);
    when(store.getRegistryId()).thenReturn("registryId");

    mcard = new MetacardImpl();
    mcard.setId("id");
    mcard.setModifiedDate(new Date());
    mcard.setAttribute(Metacard.TAGS, RegistryConstants.REGISTRY_TAG_INTERNAL);
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "registryId"));
    mcard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REMOTE_REGISTRY_ID, "registryId"));
    eventProperties = new Hashtable<>();
  }

  @Test
  public void testBindRegistryStoreNullReference() throws Exception {
    ServiceReference ref = getServiceReference("servicePid");
    handleEvent(
        null, ref, ServiceEvent.UNREGISTERING, "registryId", Collections.singletonList(mcard));
    verify(federationAdmin, never())
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(mcard.getId()));
  }

  @Test
  public void testUnregisterEvent() throws Exception {
    ServiceReference ref = getServiceReference("servicePid");
    handleEvent(
        ref, ref, ServiceEvent.UNREGISTERING, "registryId", Collections.singletonList(mcard));
    verify(federationAdmin)
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(mcard.getId()));
  }

  @Test
  public void testUnregisterEventCleanupDisabled() throws Exception {
    cleanupHandler.setCleanupRelatedMetacards(false);
    ServiceReference ref = getServiceReference("servicePid");
    handleEvent(
        ref, ref, ServiceEvent.UNREGISTERING, "registryId", Collections.singletonList(mcard));
    verify(federationAdmin, never())
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(mcard.getId()));
  }

  @Test
  public void testUnregisterEventWrongServiceEvent() throws Exception {
    ServiceReference ref = getServiceReference("servicePid");
    handleEvent(ref, ref, ServiceEvent.REGISTERED, "registryId", Collections.singletonList(mcard));
    verify(federationAdmin, never())
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(mcard.getId()));
  }

  @Test
  public void testUnregisterEventUnrelatedService() throws Exception {
    ServiceReference ref1 = getServiceReference("servicePid");
    ServiceReference ref2 = getServiceReference("differentServicePid");
    handleEvent(
        ref1, ref2, ServiceEvent.UNREGISTERING, "registryId", Collections.singletonList(mcard));
    verify(federationAdmin, never())
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(mcard.getId()));
  }

  @Test
  public void testUnregisterEventNoRelatedMetacards() throws Exception {
    ServiceReference ref = getServiceReference("servicePid");
    handleEvent(ref, ref, ServiceEvent.UNREGISTERING, "registryId", Collections.emptyList());
    verify(federationAdmin, never())
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(mcard.getId()));
  }

  @Test
  public void testDestroy() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
    cleanupHandler.destroy();
    verify(executorService, times(1)).awaitTermination(60L, TimeUnit.SECONDS);
    verify(executorService, times(0)).shutdownNow();
  }

  @Test
  public void testDestroyTerminateTasks() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
    cleanupHandler.destroy();
    verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  @Test
  public void testDestroyInterupt() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class)))
        .thenThrow(new InterruptedException("interrupt"));
    cleanupHandler.destroy();
    verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  private void handleEvent(
      ServiceReference bindRef,
      ServiceReference eventRef,
      int serviceEventType,
      String searchRegId,
      List<Metacard> metacards)
      throws Exception {
    cleanupHandler.bindRegistryStore(bindRef);
    ServiceEvent serviceEvent = mock(ServiceEvent.class);
    when(serviceEvent.getType()).thenReturn(serviceEventType);
    when(serviceEvent.getServiceReference()).thenReturn(eventRef);
    eventProperties.put(EventConstants.EVENT, serviceEvent);
    when(federationAdmin.getInternalRegistryMetacards()).thenReturn(metacards);
    event = new Event("myevent", eventProperties);
    cleanupHandler.handleEvent(event);
  }

  private ServiceReference getServiceReference(String pid) {
    ServiceReference reference = mock(ServiceReference.class);
    when(reference.getProperty(Constants.SERVICE_PID)).thenReturn(pid);
    return reference;
  }

  private void setupSerialExecutor() {
    doAnswer(
            (args) -> {
              ((Runnable) args.getArguments()[0]).run();
              return null;
            })
        .when(executorService)
        .execute(any());
  }
}
