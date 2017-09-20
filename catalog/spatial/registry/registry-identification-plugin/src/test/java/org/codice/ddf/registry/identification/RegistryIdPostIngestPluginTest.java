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
package org.codice.ddf.registry.identification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RegistryIdPostIngestPluginTest {

  private RegistryIdPostIngestPlugin registryIdPostIngestPlugin;

  private FilterBuilder builder = new GeotoolsFilterBuilder();

  @Mock private CatalogFramework framework;

  @Mock private ScheduledExecutorService executorService;

  @Mock private Security security;

  @Before
  public void setup() throws Exception {
    registryIdPostIngestPlugin = new RegistryIdPostIngestPlugin(security);
    registryIdPostIngestPlugin.setCatalogFramework(framework);
    registryIdPostIngestPlugin.setExecutorService(executorService);
    registryIdPostIngestPlugin.setFilterBuilder(builder);
  }

  @Test
  public void testProcessCreate() throws Exception {
    CreateResponse response =
        new CreateResponseImpl(null, null, Collections.singletonList(getDefaultMetacard()));
    registryIdPostIngestPlugin.process(response);
    assertThat(registryIdPostIngestPlugin.getRegistryIds().size(), equalTo(1));
    assertThat(registryIdPostIngestPlugin.getRegistryIds().iterator().next(), equalTo("regId"));
  }

  @Test
  public void testProcessCreateLocal() throws Exception {
    MetacardImpl metacard = getDefaultMetacard();
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
    CreateResponse response =
        new CreateResponseImpl(null, null, Collections.singletonList(metacard));
    registryIdPostIngestPlugin.process(response);
    assertThat(registryIdPostIngestPlugin.getRegistryIds().size(), equalTo(1));
    assertThat(registryIdPostIngestPlugin.getLocalRegistryIds().size(), equalTo(1));
    assertThat(
        registryIdPostIngestPlugin.getLocalRegistryIds().iterator().next(), equalTo("regId"));
  }

  @Test
  public void testProcessCreateInternal() throws Exception {
    CreateResponse response =
        new CreateResponseImpl(null, null, Collections.singletonList(getDefaultInternalMetacard()));
    registryIdPostIngestPlugin.process(response);
    assertThat(registryIdPostIngestPlugin.getRegistryIds().size(), equalTo(0));
    assertThat(registryIdPostIngestPlugin.getRemoteMetacardIds().size(), equalTo(1));
    assertThat(
        registryIdPostIngestPlugin.getRemoteMetacardIds().iterator().next(),
        equalTo("remoteMcardId"));
  }

  @Test
  public void testProcessCreateNullResponse() throws Exception {
    registryIdPostIngestPlugin.process((CreateResponse) null);
  }

  @Test
  public void testProcessDelete() throws Exception {
    CreateResponse createResponse =
        new CreateResponseImpl(null, null, Collections.singletonList(getDefaultMetacard()));
    registryIdPostIngestPlugin.process(createResponse);
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(null, null, Collections.singletonList(getDefaultMetacard()));
    registryIdPostIngestPlugin.process(deleteResponse);
    assertThat(registryIdPostIngestPlugin.getRegistryIds().size(), equalTo(0));
  }

  @Test
  public void testProcessDeleteLocal() throws Exception {
    MetacardImpl metacard = getDefaultMetacard();
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
    CreateResponse createResponse =
        new CreateResponseImpl(null, null, Collections.singletonList(metacard));
    registryIdPostIngestPlugin.process(createResponse);
    assertThat(registryIdPostIngestPlugin.getLocalRegistryIds().size(), equalTo(1));
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(null, null, Collections.singletonList(metacard));
    registryIdPostIngestPlugin.process(deleteResponse);
    assertThat(registryIdPostIngestPlugin.getLocalRegistryIds().size(), equalTo(0));
  }

  @Test
  public void testProcessDeleteInternal() throws Exception {
    CreateResponse createResponse =
        new CreateResponseImpl(null, null, Collections.singletonList(getDefaultInternalMetacard()));
    registryIdPostIngestPlugin.process(createResponse);
    DeleteResponse deleteResponse =
        new DeleteResponseImpl(null, null, Collections.singletonList(getDefaultInternalMetacard()));
    registryIdPostIngestPlugin.process(deleteResponse);
    assertThat(registryIdPostIngestPlugin.getRemoteMetacardIds().size(), equalTo(0));
  }

  @Test
  public void testInit() throws Exception {
    Metacard metacard = getDefaultMetacard();
    QueryResponseImpl response =
        new QueryResponseImpl(null, Collections.singletonList(new ResultImpl(metacard)), 1L);
    when(security.runAsAdminWithException(any(PrivilegedExceptionAction.class)))
        .thenAnswer(invocation -> ((PrivilegedExceptionAction) invocation.getArguments()[0]).run());
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(invocation -> ((Callable) invocation.getArguments()[0]).call());
    when(framework.query(any(QueryRequest.class))).thenReturn(response);
    registryIdPostIngestPlugin.init();
    assertThat(registryIdPostIngestPlugin.getRegistryIds().size(), equalTo(1));
    assertThat(registryIdPostIngestPlugin.getRegistryIds().iterator().next(), equalTo("regId"));
  }

  @Test
  public void testInitCatalogNotAvailable() throws Exception {
    when(security.runAsAdminWithException(any(PrivilegedExceptionAction.class)))
        .thenThrow(new PrivilegedActionException(new UnsupportedQueryException("exception")));
    registryIdPostIngestPlugin.init();
    assertThat(registryIdPostIngestPlugin.getRegistryIds().size(), equalTo(0));
    verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void testDestroy() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
    registryIdPostIngestPlugin.destroy();
    verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(0)).shutdownNow();
  }

  @Test
  public void testDestroyTerminateTasks() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
    registryIdPostIngestPlugin.destroy();
    verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  @Test
  public void testDestroyInterupt() throws Exception {
    when(executorService.awaitTermination(anyLong(), any(TimeUnit.class)))
        .thenThrow(new InterruptedException("interrupt"));
    registryIdPostIngestPlugin.destroy();
    verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    verify(executorService, times(1)).shutdownNow();
  }

  private MetacardImpl getDefaultMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("id");
    metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "regId");
    return metacard;
  }

  private Metacard getDefaultInternalMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("id");
    metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG_INTERNAL));
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "regId");
    metacard.setAttribute(RegistryObjectMetacardType.REMOTE_REGISTRY_ID, "remoteRegId");
    metacard.setAttribute(RegistryObjectMetacardType.REMOTE_METACARD_ID, "remoteMcardId");
    return metacard;
  }
}
