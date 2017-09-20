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
package org.codice.ddf.registry.federationadmin.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RefreshRegistryEntriesTest {

  private static final String TEST_METACARD_ID = "MetacardId";

  private static final String TEST_ID = "TestId";

  private static final String TEST_REG_ID = "TestRegId";

  private static final String TEST_XML_STRING = "SomeValidStringVersionOfXml";

  @Mock private FederationAdminServiceImpl federationAdminService;

  @Mock private ScheduledExecutorService executorService;

  @Mock private Security security;

  @Mock private Subject subject;

  private RefreshRegistryEntries refreshRegistryEntries;

  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  @Mock private RegistryStore registryStore;

  @Before
  public void setUp() throws Exception {
    refreshRegistryEntries = spy(new RefreshRegistryEntries(security));
    refreshRegistryEntries.setFederationAdminService(federationAdminService);
    refreshRegistryEntries.setRegistryStores(Collections.emptyList());
    refreshRegistryEntries.setFilterBuilder(filterBuilder);
    refreshRegistryEntries.setExecutor(executorService);
    when(registryStore.getId()).thenReturn(TEST_ID);
    when(registryStore.getRegistryId()).thenReturn(TEST_REG_ID);
    when(security.runAsAdmin(any())).thenCallRealMethod();
    when(security.runAsAdminWithException(any())).thenCallRealMethod();
    when(security.getSystemSubject()).thenReturn(subject);
    setupSerialExecutor();
  }

  @Test
  public void testCreateRemoteEntries() throws Exception {
    Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
    when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.emptyList());
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMetacard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);
    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.isAvailable()).thenReturn(true);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService)
        .addRegistryEntries(Collections.singletonList(remoteMetacard), null);
  }

  @Test
  public void testCreateRemoteEntriesSourceUnavailable() throws Exception {
    Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
    when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.emptyList());
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMetacard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.isAvailable()).thenReturn(false);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService, never())
        .addRegistryEntries(Collections.singletonList(remoteMetacard), null);
  }

  @Test
  public void testCreateRemoteEntriesPullNotAllowed() throws Exception {
    Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
    when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.emptyList());
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMetacard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(false);
    when(registryStore.isAvailable()).thenReturn(true);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService, never())
        .addRegistryEntries(Collections.singletonList(remoteMetacard), null);
  }

  @Test
  public void testSubscriptionEntityRemovalNoRemoteEntries() throws Exception {
    Metacard localMetacard = getPopulatedTestRegistryMetacard("mcardId", "testRegId", 0, true);
    when(federationAdminService.getInternalRegistryMetacards())
        .thenReturn(Collections.singletonList(localMetacard));

    SourceResponse response = new SourceResponseImpl(null, Collections.emptyList());
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.getId()).thenReturn(TEST_ID);
    when(registryStore.getRegistryId()).thenReturn("remoteRegId");
    when(registryStore.isAvailable()).thenReturn(true);
    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService)
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(localMetacard.getId()));
  }

  @Test
  public void testSubscriptionEntityRemoval() throws Exception {
    Metacard localMetacard = getPopulatedTestRegistryMetacard("mcardId", "testRegId", 0, true);
    when(federationAdminService.getInternalRegistryMetacards())
        .thenReturn(Collections.singletonList(localMetacard));
    Metacard remoteMetacard = getPopulatedTestRegistryMetacard("mcardId2", "testRegId2", 0, true);
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMetacard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.getId()).thenReturn(TEST_ID);
    when(registryStore.getRegistryId()).thenReturn("remoteRegId");
    when(registryStore.isAvailable()).thenReturn(true);
    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService)
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(localMetacard.getId()));
  }

  @Test
  public void testSubscriptionEntityRemovalFailedQuery() throws Exception {
    Metacard localMetacard = getPopulatedTestRegistryMetacard("mcardId", "testRegId", 0, true);
    when(federationAdminService.getInternalRegistryMetacards())
        .thenReturn(Collections.singletonList(localMetacard));

    when(registryStore.query(any(QueryRequest.class)))
        .thenThrow(new UnsupportedQueryException("query error"));

    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.getId()).thenReturn(TEST_ID);
    when(registryStore.getRegistryId()).thenReturn("remoteRegId");
    when(registryStore.isAvailable()).thenReturn(true);
    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService, never())
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList(localMetacard.getId()));
  }

  @Test
  public void testWriteRemoteUpdates() throws Exception {
    Metacard localMcard =
        getPopulatedTestRegistryMetacard(
            TEST_METACARD_ID, RegistryObjectMetacardType.REGISTRY_ID, 0, true);
    Metacard remoteMcard =
        getPopulatedTestRegistryMetacard(
            "remoteMcardId", RegistryObjectMetacardType.REGISTRY_ID, 1000L);

    when(federationAdminService.getInternalRegistryMetacards())
        .thenReturn(Collections.singletonList(localMcard));
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMcard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.isAvailable()).thenReturn(true);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService).updateRegistryEntry(remoteMcard);
  }

  @Test
  public void testNoUpdatesOnLocal() throws Exception {
    MetacardImpl localMcard =
        getPopulatedTestRegistryMetacard(
            TEST_METACARD_ID, RegistryObjectMetacardType.REGISTRY_ID, 0, true);
    localMcard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
    Metacard remoteMcard =
        getPopulatedTestRegistryMetacard(
            "remoteMcardId", RegistryObjectMetacardType.REGISTRY_ID, 1000L);

    when(federationAdminService.getInternalRegistryMetacards())
        .thenReturn(Collections.singletonList(localMcard));
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMcard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.isAvailable()).thenReturn(true);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
  }

  @Test
  public void testNoUpdatesOnOlder() throws Exception {
    MetacardImpl localMcard =
        getPopulatedTestRegistryMetacard(
            TEST_METACARD_ID, RegistryObjectMetacardType.REGISTRY_ID, 0, true);
    Metacard remoteMcard =
        getPopulatedTestRegistryMetacard(
            "remoteMcardId", RegistryObjectMetacardType.REGISTRY_ID, -5000L);

    when(federationAdminService.getInternalRegistryMetacards())
        .thenReturn(Collections.singletonList(localMcard));
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(remoteMcard)));
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.isAvailable()).thenReturn(true);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
  }

  @Test
  public void testMultipleStores() throws Exception {
    Metacard mcard = getPopulatedTestRegistryMetacard();
    RegistryStore registryStore2 = mock(RegistryStore.class);
    when(registryStore2.getId()).thenReturn("id2");
    when(registryStore2.getRegistryId()).thenReturn("regId2");
    when(registryStore2.isAvailable()).thenReturn(true);
    when(registryStore2.isPullAllowed()).thenReturn(true);
    SourceResponse response =
        new SourceResponseImpl(null, Collections.singletonList(new ResultImpl(mcard)));
    when(registryStore2.query(any(QueryRequest.class))).thenReturn(response);
    when(registryStore.query(any(QueryRequest.class))).thenThrow(new UnsupportedQueryException());
    when(registryStore.isAvailable()).thenReturn(true);
    when(registryStore.isPullAllowed()).thenReturn(true);

    when(registryStore2.query(any(QueryRequest.class))).thenReturn(response);

    List<RegistryStore> stores = new ArrayList<>();
    stores.add(registryStore);
    stores.add(registryStore2);
    refreshRegistryEntries.setRegistryStores(stores);
    refreshRegistryEntries.refreshRegistryEntries();
    verify(federationAdminService).addRegistryEntries(Collections.singletonList(mcard), null);
  }

  @Test
  public void testAddUpdateAndDeleteAtOnce() throws Exception {
    //returned metacards
    Metacard createdMetacard = getPopulatedTestRegistryMetacard("createId", "createRegId", 0, true);
    Metacard updatedMetacard =
        getPopulatedTestRegistryMetacard("updateId", "updateRegId", 5000, true);
    //local metacards
    Metacard localUpdatedMetacard =
        getPopulatedTestRegistryMetacard("localUpdateId", "updateRegId", 0, true, "updateId");
    Metacard localDeletedMetacard =
        getPopulatedTestRegistryMetacard(
            "localDeleteId", "deleteRegId", 0, true, "deleteRemoteMcardId");

    List<Result> remoteMcards = new ArrayList<>();
    remoteMcards.add(new ResultImpl(createdMetacard));
    remoteMcards.add(new ResultImpl(updatedMetacard));
    List<Metacard> localMcards = new ArrayList<>();
    localMcards.add(localDeletedMetacard);
    localMcards.add(localUpdatedMetacard);
    when(federationAdminService.getInternalRegistryMetacards()).thenReturn(localMcards);
    when(registryStore.getRegistryId()).thenReturn("remoteRegId");

    SourceResponse response = new SourceResponseImpl(null, remoteMcards);
    when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

    refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
    when(registryStore.isPullAllowed()).thenReturn(true);
    when(registryStore.isAvailable()).thenReturn(true);

    refreshRegistryEntries.refreshRegistryEntries();

    verify(federationAdminService)
        .addRegistryEntries(Collections.singletonList(createdMetacard), null);
    verify(federationAdminService).updateRegistryEntry(updatedMetacard);
    verify(federationAdminService)
        .deleteRegistryEntriesByMetacardIds(Collections.singletonList("localDeleteId"));
  }

  @Test
  public void testDestroy() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
    refreshRegistryEntries.destroy();
    verify(executorService, times(1)).awaitTermination(60L, TimeUnit.SECONDS);
    verify(executorService, times(0)).shutdownNow();
  }

  @Test
  public void testDestroyTerminateTasks() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
    refreshRegistryEntries.destroy();
    verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  @Test
  public void testDestroyInterupt() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class)))
        .thenThrow(new InterruptedException("interrupt"));
    refreshRegistryEntries.destroy();
    verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  private MetacardImpl getPopulatedTestRegistryMetacard() {
    return getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, 0);
  }

  private MetacardImpl getPopulatedTestRegistryMetacard(String id, long dateOffset) {
    return getPopulatedTestRegistryMetacard(TEST_METACARD_ID, id, dateOffset, false);
  }

  private MetacardImpl getPopulatedTestRegistryMetacard(String id, String regId, long dateOffset) {
    return getPopulatedTestRegistryMetacard(id, regId, dateOffset, false);
  }

  private MetacardImpl getPopulatedTestRegistryMetacard(
      String id, String regId, long dateOffset, boolean internal) {
    return getPopulatedTestRegistryMetacard(id, regId, dateOffset, internal, "remoteMcardId");
  }

  private MetacardImpl getPopulatedTestRegistryMetacard(
      String id, String regId, long dateOffset, boolean internal, String remoteMcardId) {
    MetacardImpl registryMetacard = new MetacardImpl(new RegistryObjectMetacardType());
    registryMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, regId));
    registryMetacard.setAttribute(
        new AttributeImpl(Metacard.MODIFIED, new Date(new Date().getTime() + dateOffset)));
    if (internal) {
      registryMetacard.setAttribute(
          new AttributeImpl(RegistryObjectMetacardType.REMOTE_METACARD_ID, remoteMcardId));
      registryMetacard.setAttribute(
          new AttributeImpl(RegistryObjectMetacardType.REMOTE_REGISTRY_ID, "remoteRegId"));
      registryMetacard.setAttribute(
          new AttributeImpl(
              Metacard.TAGS, Collections.singletonList(RegistryConstants.REGISTRY_TAG_INTERNAL)));
    } else {
      registryMetacard.setAttribute(
          new AttributeImpl(
              Metacard.TAGS, Collections.singletonList(RegistryConstants.REGISTRY_TAG)));
    }
    registryMetacard.setAttribute(new AttributeImpl(Metacard.ID, id));
    registryMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, TEST_XML_STRING));
    return registryMetacard;
  }

  private void setupSerialExecutor() throws InterruptedException {
    doAnswer(
            (args) -> {
              List<Callable<RefreshRegistryEntries.RemoteResult>> callables =
                  ((List<Callable<RefreshRegistryEntries.RemoteResult>>) args.getArguments()[0]);
              List<Future<RefreshRegistryEntries.RemoteResult>> results = new ArrayList<>();
              for (Callable<RefreshRegistryEntries.RemoteResult> callable : callables) {

                results.add(
                    new Future<RefreshRegistryEntries.RemoteResult>() {
                      @Override
                      public boolean cancel(boolean mayInterruptIfRunning) {
                        return false;
                      }

                      @Override
                      public boolean isCancelled() {
                        return false;
                      }

                      @Override
                      public boolean isDone() {
                        return false;
                      }

                      @Override
                      public RefreshRegistryEntries.RemoteResult get()
                          throws InterruptedException, ExecutionException {
                        return null;
                      }

                      @Override
                      public RefreshRegistryEntries.RemoteResult get(long timeout, TimeUnit unit)
                          throws InterruptedException, ExecutionException, TimeoutException {
                        try {
                          return callable.call();
                        } catch (Exception e) {
                          throw new ExecutionException(e);
                        }
                      }
                    });
              }
              return results;
            })
        .when(executorService)
        .invokeAll(any());
  }
}
