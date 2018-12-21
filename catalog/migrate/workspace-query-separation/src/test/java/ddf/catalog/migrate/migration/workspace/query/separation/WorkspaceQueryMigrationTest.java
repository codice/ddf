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
package ddf.catalog.migrate.migration.workspace.query.separation;

import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants.WORKSPACE_QUERIES;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.transform.InputTransformer;
import ddf.security.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WorkspaceQueryMigrationTest {

  @Mock private QueryResponse queryResponse;

  @Mock private CatalogFramework catalogFramework;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder filterBuilder;

  @Mock private InputTransformer xmlInputTransformer;

  @Mock private Result result;

  @Mock private Security security;

  @Mock private Subject subject;

  private WorkspaceQueryMigration workspaceQueryMigration;

  private Metacard workspace;

  private static final String WORKSPACE_ID = "workspaceId";

  @Before
  public void setup() throws Exception {
    workspaceQueryMigration =
        new WorkspaceQueryMigration(catalogFramework, filterBuilder, xmlInputTransformer);
    workspaceQueryMigration.setSecurity(security);

    when(security.runAsAdmin(any(PrivilegedAction.class))).thenReturn(subject);

    doAnswer(
            new Answer<Void>() {
              public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
              }
            })
        .when(subject)
        .execute(any(Runnable.class));

    workspace = getWorkspaceMetacard(WORKSPACE_ID);
    doReturn(workspace).when(result).getMetacard();

    doReturn(Collections.singletonList(result)).when(queryResponse).getResults();

    workspace = getWorkspaceMetacard(WORKSPACE_ID);
    doReturn(workspace).when(result).getMetacard();

    doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

    doReturn(new MetacardImpl()).when(xmlInputTransformer).transform(any(InputStream.class));

    Metacard query1 = getQueryMetacard("queryId1");
    Metacard query2 = getQueryMetacard("queryId2");

    CreateResponse createResponse = mock(CreateResponse.class);
    doReturn(ImmutableList.of(query1, query2)).when(createResponse).getCreatedMetacards();

    doReturn(createResponse).when(catalogFramework).create(any(CreateRequest.class));
  }

  @Test
  public void testDataMigration() throws Exception {
    workspaceQueryMigration.migrate();

    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogFramework, times(1)).update(any(UpdateRequest.class));

    assertThat(
        workspace.getAttribute(WORKSPACE_QUERIES).getValues(),
        is(ImmutableList.of("queryId1", "queryId2")));
  }

  @Test
  public void testMigrateUpdateRequest() throws Exception {
    ArgumentCaptor<UpdateRequest> updateRequestArgumentCaptor =
        ArgumentCaptor.forClass(UpdateRequest.class);

    workspaceQueryMigration.migrate();

    verify(catalogFramework, times(1)).update(updateRequestArgumentCaptor.capture());

    UpdateRequest actualUpdateRequest = updateRequestArgumentCaptor.getValue();
    Metacard updateMetacard = actualUpdateRequest.getUpdates().get(0).getValue();

    assertThat(actualUpdateRequest.getUpdates().get(0).getKey(), is(WORKSPACE_ID));
    assertThat(
        updateMetacard.getAttribute(WORKSPACE_QUERIES).getValues(),
        is(ImmutableList.of("queryId1", "queryId2")));
  }

  @Test
  public void testMigrateCreateRequest() throws Exception {
    ArgumentCaptor<CreateRequest> createRequestArgumentCaptor =
        ArgumentCaptor.forClass(CreateRequest.class);

    workspaceQueryMigration.migrate();

    verify(catalogFramework, times(1)).create(createRequestArgumentCaptor.capture());

    CreateRequest actualCreateRequest = createRequestArgumentCaptor.getValue();
    List<Metacard> createMetacards = actualCreateRequest.getMetacards();

    assertThat(createMetacards, hasSize(2));
    assertThat(createMetacards.get(0).getTags(), is(ImmutableSet.of("query")));
    assertThat(createMetacards.get(1).getTags(), is(ImmutableSet.of("query")));
  }

  private Metacard getQueryMetacard(String id) {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.ID, id));
    return metacard;
  }

  private Metacard getWorkspaceMetacard(String id) throws IOException {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.ID, id));

    InputStream resourceStream =
        WorkspaceQueryMigrationTest.class.getResourceAsStream("/query.xml");
    String queryStr = IOUtils.toString(resourceStream, "UTF-8");

    metacard.setAttribute(
        new AttributeImpl(WORKSPACE_QUERIES, (Serializable) ImmutableList.of(queryStr, queryStr)));

    return metacard;
  }
}
