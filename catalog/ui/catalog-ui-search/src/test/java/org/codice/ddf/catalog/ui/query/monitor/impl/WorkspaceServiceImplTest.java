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
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceServiceImplTest {

  private WorkspaceServiceImpl workspaceServiceImpl;

  private static final String TEST_ID = "123";

  private static final String TEST_SUBJECT = "subject";

  @Mock private CatalogFramework catalogFramework;

  @Mock private WorkspaceTransformerImpl workspaceTransformer;

  @Mock private SecurityService securityService;

  @Mock private PersistentStore persistentStore;

  @Mock private WorkspaceQueryBuilder workspaceQueryBuilder;

  @Mock private WorkspaceMetacardFilter workspaceMetacardFilter;

  @Before
  public void setup() {
    when(workspaceMetacardFilter.filter(any())).thenReturn(true);

    workspaceServiceImpl =
        new WorkspaceServiceImpl(
            catalogFramework,
            workspaceTransformer,
            workspaceQueryBuilder,
            securityService,
            persistentStore);

    workspaceServiceImpl.setMaxSubscriptions(100);
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

    mockCatalogFrameworkQuery(TEST_ID, TEST_SUBJECT);

    List<WorkspaceMetacardImpl> workspaceMetacards = workspaceServiceImpl.getWorkspaceMetacards();

    assertMetacardList(TEST_ID, TEST_SUBJECT, workspaceMetacards);
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

    mockCatalogFrameworkQuery(TEST_ID, TEST_SUBJECT);

    List<WorkspaceMetacardImpl> workspaceMetacards =
        workspaceServiceImpl.getWorkspaceMetacards(Collections.singleton(TEST_ID));

    assertMetacardList(TEST_ID, TEST_SUBJECT, workspaceMetacards);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetWorkspaceMetacardsByWorkspaceIdWithException()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          PersistenceException {

    mockCatalogFrameworkQuery(TEST_ID, TEST_SUBJECT);
    when(catalogFramework.query(any())).thenThrow(UnsupportedQueryException.class);

    List<WorkspaceMetacardImpl> workspaceMetacards =
        workspaceServiceImpl.getWorkspaceMetacards(Collections.singleton(TEST_ID));

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

    mockCatalogFrameworkQuery(TEST_ID, TEST_SUBJECT);
    when(catalogFramework.query(any())).thenThrow(UnsupportedQueryException.class);

    List<WorkspaceMetacardImpl> workspaceMetacards = workspaceServiceImpl.getWorkspaceMetacards();

    assertThat(workspaceMetacards, hasSize(0));
  }

  @Test
  public void testToString() {
    assertThat(workspaceServiceImpl.toString(), notNullValue());
  }

  @Test
  public void testGetQueryMetacards() {

    Metacard metacard = mock(Metacard.class);
    when(metacard.getMetacardType()).thenReturn(MetacardImpl.BASIC_METACARD);

    String xml = "<xml/>";

    when(workspaceTransformer.xmlToMetacard(xml)).thenReturn(metacard);

    WorkspaceMetacardImpl workspaceMetacard = mock(WorkspaceMetacardImpl.class);
    when(workspaceMetacard.getQueries()).thenReturn(Collections.singletonList(xml));

    List<QueryMetacardImpl> metacards = workspaceServiceImpl.getQueryMetacards(workspaceMetacard);

    assertThat(metacards, hasSize(1));
  }
}
