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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl.WorkspaceTransformerImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.SecurityService;
import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceMetacardFilter;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceQueryBuilder;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;

public class WorkspaceServiceImplTest {

  private static final int MAX_SUBSCRIPTIONS = 100;

  private CatalogFramework catalogFramework;

  private WorkspaceTransformerImpl workspaceTransformer;

  private SecurityService securityService;

  private WorkspaceServiceImpl workspaceService;

  private PersistentStore persistentStore;

  private WorkspaceQueryBuilder workspaceQueryBuilder;

  @Before
  public void setup() {
    catalogFramework = mock(CatalogFramework.class);
    workspaceTransformer = mock(WorkspaceTransformerImpl.class);
    securityService = mock(SecurityService.class);
    persistentStore = mock(PersistentStore.class);
    workspaceQueryBuilder = mock(WorkspaceQueryBuilder.class);
    WorkspaceMetacardFilter workspaceMetacardFilter = mock(WorkspaceMetacardFilter.class);

    when(workspaceMetacardFilter.filter(any())).thenReturn(true);

    workspaceService =
        new WorkspaceServiceImpl(
            catalogFramework,
            workspaceTransformer,
            workspaceQueryBuilder,
            securityService,
            persistentStore,
            MAX_SUBSCRIPTIONS);
  }

  private void mockCatalogFrameworkQuery(String id, String subject)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          PersistenceException {

    when(securityService.addSystemSubject(any()))
        .thenReturn(Collections.singletonMap(SecurityConstants.SECURITY_SUBJECT, subject));

    QueryResponse queryResponse = mock(QueryResponse.class);
    Result result = mock(Result.class);
    Metacard metacard = mock(Metacard.class);
    when(metacard.getMetacardType()).thenReturn(MetacardImpl.BASIC_METACARD);
    Attribute attribute = mock(Attribute.class);
    when(attribute.getValue()).thenReturn(id);
    when(metacard.getAttribute(Metacard.ID)).thenReturn(attribute);
    when(metacard.getTags()).thenReturn(Collections.singleton(WorkspaceConstants.WORKSPACE_TAG));
    when(persistentStore.get(SubscriptionsPersistentStore.SUBSCRIPTIONS_TYPE))
        .thenReturn(Collections.singletonList(Collections.singletonMap("id_txt", id)));

    when(result.getMetacard()).thenReturn(metacard);

    List<Result> resultList = Collections.singletonList(result);
    when(queryResponse.getResults()).thenReturn(resultList);
    when(catalogFramework.query(any())).thenReturn(queryResponse);
    Filter filter = mock(Filter.class);
    when(workspaceQueryBuilder.createFilter(Collections.singleton(id))).thenReturn(filter);
  }

  @Test
  public void testGetWorkspaceMetacards()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          PersistenceException {

    String id = "123";
    String subject = "subject";

    mockCatalogFrameworkQuery(id, subject);

    List<WorkspaceMetacardImpl> workspaceMetacards = workspaceService.getWorkspaceMetacards();

    assertMetacardList(id, subject, workspaceMetacards);
  }

  private void assertMetacardList(
      String id, String subject, List<WorkspaceMetacardImpl> workspaceMetacards)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    ArgumentCaptor<QueryRequest> argumentCaptor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(catalogFramework).query(argumentCaptor.capture());

    assertThat(
        argumentCaptor.getValue().getProperties().get(SecurityConstants.SECURITY_SUBJECT),
        is(subject));
    assertThat(workspaceMetacards, hasSize(1));
    assertThat(workspaceMetacards.get(0).getId(), is(id));
  }

  @Test
  public void testGetWorkspaceMetacardsByWorkspaceId()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          PersistenceException {

    String id = "123";
    String subject = "subject";

    mockCatalogFrameworkQuery(id, subject);

    List<WorkspaceMetacardImpl> workspaceMetacards =
        workspaceService.getWorkspaceMetacards(Collections.singleton(id));

    assertMetacardList(id, subject, workspaceMetacards);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetWorkspaceMetacardsByWorkspaceIdWithException()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          PersistenceException {

    String id = "123";

    mockCatalogFrameworkQuery(id, "subject");
    when(catalogFramework.query(any())).thenThrow(UnsupportedQueryException.class);

    List<WorkspaceMetacardImpl> workspaceMetacards =
        workspaceService.getWorkspaceMetacards(Collections.singleton(id));

    assertThat(workspaceMetacards, hasSize(0));
  }

  /**
   * If the catalog framework throws an exception, then getWoekspaceMetacards should return an empty
   * list.
   *
   * @throws UnsupportedQueryException
   * @throws SourceUnavailableException
   * @throws FederationException
   * @throws PersistenceException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWorkspaceMetacardsWithException()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          PersistenceException {

    mockCatalogFrameworkQuery("123", "subject");
    when(catalogFramework.query(any())).thenThrow(UnsupportedQueryException.class);

    List<WorkspaceMetacardImpl> workspaceMetacards = workspaceService.getWorkspaceMetacards();

    assertThat(workspaceMetacards, hasSize(0));
  }

  @Test
  public void testToString() {
    assertThat(workspaceService.toString(), notNullValue());
  }

  @Test
  public void testGetQueryMetacards() {

    Metacard metacard = mock(Metacard.class);
    when(metacard.getMetacardType()).thenReturn(MetacardImpl.BASIC_METACARD);

    String xml = "<xml/>";

    when(workspaceTransformer.xmlToMetacard(xml)).thenReturn(metacard);

    WorkspaceMetacardImpl workspaceMetacard = mock(WorkspaceMetacardImpl.class);
    when(workspaceMetacard.getQueries()).thenReturn(Collections.singletonList(xml));

    List<QueryMetacardImpl> metacards = workspaceService.getQueryMetacards(workspaceMetacard);

    assertThat(metacards, hasSize(1));
  }
}
