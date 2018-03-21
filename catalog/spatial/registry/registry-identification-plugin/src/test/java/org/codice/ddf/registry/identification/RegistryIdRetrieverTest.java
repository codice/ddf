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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryResponseImpl;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.concurrent.Callable;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RegistryIdRetrieverTest {

  private RegistryIdRetriever registryIdRetriever;

  private FilterBuilder builder = new GeotoolsFilterBuilder();

  @Mock private CatalogFramework framework;

  @Mock private Security security;

  @Before
  public void setup() {
    registryIdRetriever = new RegistryIdRetriever(security);
    registryIdRetriever.setCatalogFramework(framework);
    registryIdRetriever.setFilterBuilder(builder);
  }

  @Test
  public void testGetRegistryIdInfoNormalRegistryMetacard() throws Exception {
    Metacard metacard = getDefaultMetacard();
    QueryResponseImpl response =
        new QueryResponseImpl(null, Collections.singletonList(new ResultImpl(metacard)), 1L);
    when(security.runAsAdminWithException(any(PrivilegedExceptionAction.class)))
        .thenAnswer(invocation -> ((PrivilegedExceptionAction) invocation.getArguments()[0]).run());
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(invocation -> ((Callable) invocation.getArguments()[0]).call());
    when(framework.query(any(QueryRequest.class))).thenReturn(response);
    RegistryIdRetriever.RegistryIdInfo idInfo = registryIdRetriever.getRegistryIdInfo();

    assertThat(idInfo.getRegistryIds(), contains("regId"));
    assertThat(idInfo.getLocalRegistryIds(), is(empty()));
    assertThat(idInfo.getRemoteMetacardIds(), is(empty()));
  }

  @Test
  public void testGetRegistryIdInfoInternalRegistryMetacard() throws Exception {
    Metacard metacard = getDefaultInternalMetacard();
    QueryResponseImpl response =
        new QueryResponseImpl(null, Collections.singletonList(new ResultImpl(metacard)), 1L);
    when(security.runAsAdminWithException(any(PrivilegedExceptionAction.class)))
        .thenAnswer(invocation -> ((PrivilegedExceptionAction) invocation.getArguments()[0]).run());
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(invocation -> ((Callable) invocation.getArguments()[0]).call());
    when(framework.query(any(QueryRequest.class))).thenReturn(response);
    RegistryIdRetriever.RegistryIdInfo idInfo = registryIdRetriever.getRegistryIdInfo();
    assertThat(idInfo.getRemoteMetacardIds(), contains("remoteMcardId"));
    assertThat(idInfo.getLocalRegistryIds(), is(empty()));
    assertThat(idInfo.getRegistryIds(), is(empty()));
  }

  @Test
  public void testGetRegistryIdInfoLocalRegistryMetacard() throws Exception {
    Metacard metacard = getDefaultLocalMetacard();
    QueryResponseImpl response =
        new QueryResponseImpl(null, Collections.singletonList(new ResultImpl(metacard)), 1L);
    when(security.runAsAdminWithException(any(PrivilegedExceptionAction.class)))
        .thenAnswer(invocation -> ((PrivilegedExceptionAction) invocation.getArguments()[0]).run());
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(invocation -> ((Callable) invocation.getArguments()[0]).call());
    when(framework.query(any(QueryRequest.class))).thenReturn(response);
    RegistryIdRetriever.RegistryIdInfo idInfo = registryIdRetriever.getRegistryIdInfo();

    assertThat(idInfo.getRegistryIds(), contains("regId"));
    assertThat(idInfo.getLocalRegistryIds(), contains("regId"));
    assertThat(idInfo.getRemoteMetacardIds(), is(empty()));
  }

  private MetacardImpl getDefaultMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("id");
    metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "regId");
    return metacard;
  }

  private MetacardImpl getDefaultLocalMetacard() {
    MetacardImpl metacard = getDefaultMetacard();
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
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
