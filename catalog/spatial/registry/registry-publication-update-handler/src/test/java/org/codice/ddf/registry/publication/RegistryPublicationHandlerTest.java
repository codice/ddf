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
package org.codice.ddf.registry.publication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.RegistryPublicationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;

public class RegistryPublicationHandlerTest {

  @Mock private RegistryPublicationService service;

  @Mock private ScheduledExecutorService executorService;

  private MetacardImpl mcard;

  private RegistryPublicationHandler rph;

  private Event event;

  private Dictionary<String, Object> eventProperties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mcard = new MetacardImpl();
    mcard.setAttribute(Metacard.TAGS, RegistryConstants.REGISTRY_TAG);
    rph = new RegistryPublicationHandler(service, executorService);
    eventProperties = new Hashtable<>();
    eventProperties.put("ddf.catalog.event.metacard", mcard);
    event = new Event("myevent", eventProperties);
  }

  @Test
  public void testNullMetacard() {
    Dictionary<String, Object> eventProperties = new Hashtable<>();
    Event event = new Event("myevent", eventProperties);
    when(executorService.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
        .thenReturn(null);
    rph.handleEvent(event);
    verify(executorService, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void testNonRegistryMetacard() {
    mcard.setAttribute(Metacard.TAGS, Metacard.DEFAULT_TAG);
    when(executorService.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
        .thenReturn(null);
    rph.handleEvent(event);
    verify(executorService, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void testRegistryMetacardExecutorCall() {
    when(executorService.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
        .thenReturn(null);
    rph.handleEvent(event);
    verify(executorService, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void testProcessUpdateNoPublications() throws Exception {
    mcard.setAttribute(Metacard.TAGS, Metacard.DEFAULT_TAG);
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, new ArrayList<>());
    setupSerialExecutor();
    rph.handleEvent(event);
    verify(service, never()).update(mcard);
  }

  @Test
  public void testProcessUpdateEmptyPublications() throws Exception {
    setupSerialExecutor();
    rph.handleEvent(event);
    verify(service, never()).update(mcard);
  }

  @Test
  public void testProcessUpdateNoLastPublishDate() throws Exception {
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "mylocation");
    setupSerialExecutor();
    rph.handleEvent(event);
    verify(service, times(1)).update(mcard);
  }

  @Test
  public void testProcessUpdateNoUpdateNeeded() throws Exception {
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "mylocation");
    Date now = new Date();
    mcard.setModifiedDate(now);
    mcard.setAttribute(RegistryObjectMetacardType.LAST_PUBLISHED, now);
    setupSerialExecutor();
    rph.handleEvent(event);
    verify(service, never()).update(mcard);
  }

  @Test
  public void testProcessUpdate() throws Exception {
    doNothing().when(service).update(any(Metacard.class));
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "mylocation");
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);
    mcard.setModifiedDate(now);
    mcard.setAttribute(RegistryObjectMetacardType.LAST_PUBLISHED, before);
    setupSerialExecutor();
    rph.handleEvent(event);
    verify(service, times(1)).update(mcard);
  }

  @Test
  public void testProcessUpdateException() throws Exception {
    doThrow(new FederationAdminException("Test Error")).when(service).update(any(Metacard.class));
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "mylocation");
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);
    mcard.setModifiedDate(now);
    mcard.setAttribute(RegistryObjectMetacardType.LAST_PUBLISHED, before);
    setupSerialExecutor();
    rph.handleEvent(event);
    verify(service).update(mcard);
    assertThat(
        mcard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED).getValue(), equalTo(before));
  }

  @Test
  public void testDestroy() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
    rph.destroy();
    verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(0)).shutdownNow();
  }

  @Test
  public void testDestroyTerminateTasks() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
    rph.destroy();
    verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  @Test
  public void testDestroyInterupt() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class)))
        .thenThrow(new InterruptedException("interrupt"));
    rph.destroy();
    verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  private void setupSerialExecutor() {
    doAnswer(
            (args) -> {
              ((Runnable) args.getArguments()[0]).run();
              return null;
            })
        .when(executorService)
        .schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }
}
