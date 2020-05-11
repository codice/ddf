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
package org.codice.ddf.catalog.plugin.clientinfo;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Ensure that client information is available to the catalog framework. */
@RunWith(MockitoJUnitRunner.class)
public class ClientInfoPluginTest {

  private static final String CLIENT_INFO_KEY = "client-info";

  @Mock private CreateRequest mockCreateRequest;

  @Mock private UpdateRequest mockUpdateRequest;

  @Mock private DeleteRequest mockDeleteRequest;

  @Mock private QueryRequest mockQueryRequest;

  @Mock private ResourceRequest mockResourceRequest;

  @Mock private DeleteResponse mockDeleteResponse;

  @Mock private QueryResponse mockQueryResponse;

  @Mock private ResourceResponse mockResourceResponse;

  @Mock private Map<String, Metacard> mockMetacardMap;

  @Mock private Metacard mockMetacard;

  private Object testableValue;

  private ClientInfoPlugin plugin;

  private Map<String, Serializable> properties;

  @Before
  public void setup() throws Exception {
    testableValue = new Serializable() {};
    ThreadContext.put(CLIENT_INFO_KEY, testableValue);
    plugin = new ClientInfoPlugin();
    properties = new HashMap<>();
  }

  @After
  public void cleanup() throws Exception {
    ThreadContext.remove(CLIENT_INFO_KEY);
  }

  @Test
  public void testNoClientInfoDoesNothing() throws Exception {
    ThreadContext.remove(CLIENT_INFO_KEY);
    prepareMockOperation(mockCreateRequest);
    plugin.processPreCreate(mockCreateRequest);
    assertThat(properties.get(CLIENT_INFO_KEY), nullValue());
  }

  @Test
  public void testClientInfoNotSerializableDoesNothing() throws Exception {
    testableValue = new Object();
    ThreadContext.put(CLIENT_INFO_KEY, testableValue);
    prepareMockOperation(mockCreateRequest);
    plugin.processPreCreate(mockCreateRequest);
    assertThat(properties.get(CLIENT_INFO_KEY), nullValue());
  }

  @Test
  public void testPreCreate() throws Exception {
    prepareMockOperation(mockCreateRequest);
    plugin.processPreCreate(mockCreateRequest);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPreUpdate() throws Exception {
    prepareMockOperation(mockUpdateRequest);
    plugin.processPreUpdate(mockUpdateRequest, mockMetacardMap);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPreDelete() throws Exception {
    prepareMockOperation(mockDeleteRequest);
    plugin.processPreDelete(mockDeleteRequest);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPreQuery() throws Exception {
    prepareMockOperation(mockQueryRequest);
    plugin.processPreQuery(mockQueryRequest);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPreResource() throws Exception {
    prepareMockOperation(mockResourceRequest);
    plugin.processPreResource(mockResourceRequest);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPostDelete() throws Exception {
    prepareMockOperation(mockDeleteResponse);
    plugin.processPostDelete(mockDeleteResponse);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPostQuery() throws Exception {
    prepareMockOperation(mockQueryResponse);
    plugin.processPostQuery(mockQueryResponse);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  @Test
  public void testPostResource() throws Exception {
    prepareMockOperation(mockResourceResponse);
    plugin.processPostResource(mockResourceResponse, mockMetacard);
    assertThat(properties.get(CLIENT_INFO_KEY), is(testableValue));
  }

  private void prepareMockOperation(Operation request) throws Exception {
    when(request.getProperties()).thenReturn(properties);
  }
}
