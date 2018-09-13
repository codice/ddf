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
package org.codice.ddf.catalog.ui.query.monitor.email;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import ddf.security.SubjectIdentity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubscribedWorkspaceNotifierTest {

  private SubscribedWorkspaceNotifier subscribedWorkspaceNotifier;

  @Mock private EmailNotifier mockEmailNotifier;

  @Mock private QueryResponse mockQueryResponse;

  @Mock private CatalogFramework mockCatalogFramework;

  @Mock private SubjectIdentity mockSubjectIdentity;

  @Mock private QueryRequest mockQueryRequest;

  @Mock private Attribute mockAttribute;

  @Before
  public void setup()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    WorkspaceMetacardImpl mockWorkspaceMetacard = mock(WorkspaceMetacardImpl.class);
    QueryResponse mockWorkspaceMetacardQueryResponse = mock(QueryResponse.class);
    Subject mockSubject = mock(Subject.class);
    Result mockResult = mock(Result.class);
    ThreadContext.bind(mockSubject);

    when(mockQueryResponse.getResults()).thenReturn(Collections.singletonList(mockResult));
    when(mockQueryResponse.getRequest()).thenReturn(mockQueryRequest);
    when(mockQueryRequest.getProperties()).thenReturn(Collections.singletonMap("id", "1234abcd"));
    when(mockCatalogFramework.query(any(QueryRequest.class)))
        .thenReturn(mockWorkspaceMetacardQueryResponse);
    when(mockWorkspaceMetacardQueryResponse.getResults())
        .thenReturn(Collections.singletonList(mockResult));
    when(mockResult.getMetacard()).thenReturn(mockWorkspaceMetacard);
    when(mockWorkspaceMetacard.getAttribute(Core.METACARD_OWNER)).thenReturn(mockAttribute);
    when(mockWorkspaceMetacard.getMetacardType()).thenReturn(mock(MetacardType.class));
    when(mockWorkspaceMetacard.getTags())
        .thenReturn(Collections.singleton(WorkspaceConstants.WORKSPACE_TAG));
    when(mockAttribute.getValues()).thenReturn(Collections.singletonList("test@email.com"));
    when(mockSubjectIdentity.getUniqueIdentifier(any(Subject.class))).thenReturn("test@email.com");

    subscribedWorkspaceNotifier =
        new SubscribedWorkspaceNotifier(
            mockSubjectIdentity,
            mockEmailNotifier,
            new GeotoolsFilterBuilder(),
            mockCatalogFramework);
  }

  @Test(expected = PluginExecutionException.class)
  public void testProcessWithNullResponse() throws Exception {
    subscribedWorkspaceNotifier.process(null);
  }

  @Test
  public void testProcessWithNoResults() throws PluginExecutionException {
    when(mockQueryResponse.getResults()).thenReturn(new ArrayList<Result>());
    verifyEmailsSent(0);
  }

  @Test
  public void testProcessWithNoOwner() throws PluginExecutionException {
    when(mockAttribute.getValues()).thenReturn(Collections.emptyList());
    verifyEmailsSent(0);
  }

  @Test
  public void testProcessWhenOwnerIsNotUser() throws PluginExecutionException {
    when(mockSubjectIdentity.getUniqueIdentifier(any(Subject.class)))
        .thenReturn("testUser@email.com");
    verifyEmailsSent(0);
  }

  @Test
  public void testProcessWithNullQueryId() throws PluginExecutionException {
    when(mockQueryRequest.getProperties()).thenReturn(new HashMap<>());
    verifyEmailsSent(0);
  }

  @Test
  public void testProcess() throws Exception {
    verifyEmailsSent(1);
  }

  private void verifyEmailsSent(int numEmailsSent) throws PluginExecutionException {
    assertThat(subscribedWorkspaceNotifier.process(mockQueryResponse), is(mockQueryResponse));
    verify(mockEmailNotifier, times(numEmailsSent))
        .sendEmailsForWorkspace(any(WorkspaceMetacardImpl.class), anyLong());
  }
}
