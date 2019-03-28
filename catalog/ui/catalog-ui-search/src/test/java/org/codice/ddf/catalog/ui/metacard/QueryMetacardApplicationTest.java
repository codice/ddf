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
package org.codice.ddf.catalog.ui.metacard;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static spark.Spark.stop;

import com.jayway.restassured.RestAssured;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;
import spark.Request;
import spark.Spark;

@RunWith(MockitoJUnitRunner.class)
public class QueryMetacardApplicationTest {

  private static final CatalogFramework CATALOG_FRAMEWORK = mock(CatalogFramework.class);

  private static final EndpointUtil ENDPOINT_UTIL = mock(EndpointUtil.class);

  private static final FilterBuilder FILTER_BUILDER = mock(FilterBuilder.class);

  private static final QueryMetacardApplication APPLICATION =
      new QueryMetacardApplication(CATALOG_FRAMEWORK, ENDPOINT_UTIL, FILTER_BUILDER);

  private static String localhostUrl;

  @BeforeClass
  public static void setUpClass() {
    Spark.port(getAvailablePort());
    APPLICATION.init();
    Spark.awaitInitialization();
    localhostUrl = format("http://localhost:%d/queries", Spark.port());
  }

  @AfterClass
  public static void tearDownClass() {
    stop();
  }

  @Before
  public void setup() {
    AttributeBuilder attributeBuilder = mock(AttributeBuilder.class);
    when(FILTER_BUILDER.attribute(any(String.class))).thenReturn(attributeBuilder);

    ExpressionBuilder expressionBuilder = mock(ExpressionBuilder.class);
    when(attributeBuilder.is()).thenReturn(expressionBuilder);

    ContextualExpressionBuilder contextualExpressionBuilder =
        mock(ContextualExpressionBuilder.class);
    when(expressionBuilder.like()).thenReturn(contextualExpressionBuilder);

    Filter filter = mock(Filter.class);
    when(contextualExpressionBuilder.text(any(String.class))).thenReturn(filter);
  }

  @Test
  public void testCreateQueryMetacard() throws Exception {
    String content = getFileContents("/queries/basic.json");

    doReturn(content).when(ENDPOINT_UTIL).safeGetBody(any(Request.class));

    doReturn(
            new CreateResponseImpl(
                new CreateRequestImpl(Collections.emptyList()),
                Collections.emptyMap(),
                Collections.singletonList(new MetacardImpl())))
        .when(CATALOG_FRAMEWORK)
        .create(any(CreateRequest.class));

    int statusCode = RestAssured.given().body(content).post(localhostUrl).statusCode();

    assertThat(statusCode, is(201));
  }

  @Test
  public void testRetrieveQueryMetacardSpecifiedId() throws Exception {
    doReturn(new MetacardImpl()).when(ENDPOINT_UTIL).getMetacardById(any(String.class));

    int statusCode = RestAssured.given().get(localhostUrl + "/123").statusCode();

    assertThat(statusCode, is(200));
  }

  @Test
  public void testRetrieveAllQueryMetacards() throws Exception {
    Result result = new ResultImpl(new MetacardImpl());
    QueryResponse queryResponse = mock(QueryResponse.class);

    doReturn(Collections.singletonList(result)).when(queryResponse).getResults();
    doReturn(queryResponse).when(CATALOG_FRAMEWORK).query(any(QueryRequest.class));

    int statusCode = RestAssured.given().get(localhostUrl).statusCode();

    assertThat(statusCode, is(200));
  }

  @Test
  public void testUpdateQueryMetacard() throws Exception {
    String content = getFileContents("/queries/basic.json");

    UpdateRequest request = new UpdateRequestImpl("", null);
    List<Update> updates = Collections.singletonList(new UpdateImpl(new MetacardImpl(), null));

    doReturn(content).when(ENDPOINT_UTIL).safeGetBody(any(Request.class));

    doReturn(new UpdateResponseImpl(request, Collections.emptyMap(), updates))
        .when(CATALOG_FRAMEWORK)
        .update(any(UpdateRequest.class));

    int statusCode = RestAssured.given().body(content).put(localhostUrl + "/123").statusCode();

    assertThat(statusCode, is(200));
  }

  @Test
  public void testDeleteQueryMetacard() throws Exception {
    doReturn(null).when(CATALOG_FRAMEWORK).delete(any(DeleteRequest.class));

    int statusCode = RestAssured.given().delete(localhostUrl + "/123").statusCode();

    assertThat(statusCode, is(204));
  }

  private static String getFileContents(String resource) {
    try (InputStream inputStream =
        QueryMetacardApplicationTest.class.getResourceAsStream(resource)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      String message = String.format("Unable to find resource [%s]", resource);
      throw new AssertionError(message, e);
    }
  }

  private static int getAvailablePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new AssertionError("Could not autobind to available port", e);
    } finally {
      tryCloseSocket(socket);
    }
  }

  private static void tryCloseSocket(@Nullable ServerSocket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      throw new AssertionError(
          "Problem while enumerating ports (specifically, port " + socket.getLocalPort() + ")", e);
    }
  }
}
