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
import static org.mockito.Mockito.anyString;
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
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import ddf.security.SubjectIdentity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.And;
import org.opengis.filter.Filter;

@RunWith(MockitoJUnitRunner.class)
public class SubscribedWorkspaceNotifierTest {

  SubscribedWorkspaceNotifier subscribedWorkspaceNotifier;

  @Mock EmailNotifier emailNotifier;

  @Mock QueryResponse queryResponse;

  @Mock CatalogFramework catalogFramework;

  @Before
  public void setup()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    WorkspaceMetacardImpl workspaceMetacard = mock(WorkspaceMetacardImpl.class);
    QueryResponse workspaceMetacardQueryResponse = mock(QueryResponse.class);
    ExpressionBuilder expressionBuilder = mock(ExpressionBuilder.class);
    AttributeBuilder attributeBuilder = mock(AttributeBuilder.class);
    SubjectIdentity subjectIdentity = mock(SubjectIdentity.class);
    FilterBuilder filterBuilder = mock(FilterBuilder.class);
    QueryRequest queryRequest = mock(QueryRequest.class);
    Attribute attribute = mock(Attribute.class);
    Subject subject = mock(Subject.class);
    Result result = mock(Result.class);


    List<Result> results = new ArrayList<>(Arrays.asList(new Result[] {result}));
    Map<String, Serializable> properties = new HashMap<>();
    properties.put("id", "1234abcd");
    List<Serializable> values =
        new ArrayList<>(Arrays.asList(new Serializable[] {"test@email.com"}));
    Set<String> metacardTags = new HashSet<>();
    metacardTags.add(WorkspaceConstants.WORKSPACE_TAG);

    when(filterBuilder.allOf(any(Filter.class), any(Filter.class))).thenReturn(mock(And.class));
    when(filterBuilder.attribute(anyString())).thenReturn(attributeBuilder);
    when(attributeBuilder.is()).thenReturn(expressionBuilder);
    when(expressionBuilder.text(anyString())).thenReturn(mock(Filter.class));
    when(queryResponse.getResults()).thenReturn(results);
    when(queryResponse.getRequest()).thenReturn(queryRequest);
    when(queryRequest.getProperties()).thenReturn(properties);
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenReturn(workspaceMetacardQueryResponse);
    when(workspaceMetacardQueryResponse.getResults()).thenReturn(results);
    when(result.getMetacard()).thenReturn(workspaceMetacard);
    when(workspaceMetacard.getAttribute(Core.METACARD_OWNER)).thenReturn(attribute);
    when(workspaceMetacard.getMetacardType()).thenReturn(mock(MetacardType.class));
    when(workspaceMetacard.getTags()).thenReturn(metacardTags);
    when(attribute.getValues()).thenReturn(values);
    ThreadContext.bind(subject);
    when(subjectIdentity.getUniqueIdentifier(any(Subject.class))).thenReturn("test@email.com");

    subscribedWorkspaceNotifier = new SubscribedWorkspaceNotifier();
    subscribedWorkspaceNotifier.setEmailNotifierService(emailNotifier);
    subscribedWorkspaceNotifier.setCatalogFramework(catalogFramework);
    subscribedWorkspaceNotifier.setSubjectIdentity(subjectIdentity);
    subscribedWorkspaceNotifier.setFilterBuilder(filterBuilder);
  }

  @Test(expected = PluginExecutionException.class)
  public void testProcessWithNullResponse() throws Exception {
    subscribedWorkspaceNotifier.process(null);
  }

  @Test
  public void testProcess() throws Exception {
    assertThat(subscribedWorkspaceNotifier.process(queryResponse), is(queryResponse));
    verify(emailNotifier, times(1))
        .sendEmailsForWorkspace(any(WorkspaceMetacardImpl.class), anyLong());
  }
}
