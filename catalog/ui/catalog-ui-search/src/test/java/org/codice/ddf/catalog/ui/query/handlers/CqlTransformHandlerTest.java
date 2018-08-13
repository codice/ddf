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
package org.codice.ddf.catalog.ui.query.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.activation.MimeType;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import spark.Request;
import spark.Response;

public class CqlTransformHandlerTest {

  private BundleContext mockBundleContext;
  private List<ServiceReference> queryResponseTransformers;
  private CqlTransformHandler cqlTransformHandler;
  private ServiceReference<QueryResponseTransformer> mockQueryResponseTransformer;
  private BinaryContent mockBinaryContent;
  private EndpointUtil mockEndpointUtil;
  private Request mockRequest;
  private Response mockResponse;
  private CqlQueryResponse mockCqlQueryResponse;
  private QueryResponse mockQueryResponse;
  private QueryResponseTransformer mockServiceReference;
  private ServletOutputStream mockServletOutputStream;
  private HttpServletResponse mockHttpServiceResponse;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    mockBundleContext = mock(BundleContext.class);
    queryResponseTransformers = new ArrayList<>();
    mockQueryResponseTransformer = mock(ServiceReference.class);
    when(mockQueryResponseTransformer.getProperty("id")).thenReturn("kml");

    String message = "test";
    MimeType mimeType = new MimeType("application/vnd.google-earth.kml+xml");
    mockBinaryContent =
        new BinaryContentImpl(new ByteArrayInputStream(message.getBytes()), mimeType);

    queryResponseTransformers.add(mockQueryResponseTransformer);
    cqlTransformHandler = new CqlTransformHandler(queryResponseTransformers, mockBundleContext);

    mockEndpointUtil = mock(EndpointUtil.class);
    mockRequest = mock(Request.class);
    mockResponse = mock(Response.class);

    when(mockEndpointUtil.safeGetBody(mockRequest))
        .thenReturn(
            "{\"src\":\"ddf.distribution\",\"start\":1,\"count\":250,\"cql\":\"anyText ILIKE '*'\",\"sorts\":[{\"attribute\":\"modified\",\"direction\":\"descending\"}],\"id\":\"7a491439-948e-431b-815e-a04f32fecec9\"}");

    mockCqlQueryResponse = mock(CqlQueryResponse.class);

    when(mockEndpointUtil.executeCqlQuery(any(CqlRequest.class))).thenReturn(mockCqlQueryResponse);

    cqlTransformHandler.setEndpointUtil(mockEndpointUtil);

    mockQueryResponse = mock(QueryResponse.class);
    when(mockCqlQueryResponse.getQueryResponse()).thenReturn(mockQueryResponse);

    mockServiceReference = mock(QueryResponseTransformer.class);
    when(mockBundleContext.getService(mockQueryResponseTransformer))
        .thenReturn(mockServiceReference);
    when(mockServiceReference.transform(mockQueryResponse, Collections.emptyMap()))
        .thenReturn(mockBinaryContent);

    mockServletOutputStream = mock(ServletOutputStream.class);

    mockHttpServiceResponse = mock(HttpServletResponse.class);

    when(mockResponse.raw()).thenReturn(mockHttpServiceResponse);

    when(mockHttpServiceResponse.getOutputStream()).thenReturn(mockServletOutputStream);
  }

  @Test
  public void testNoServiceFound() throws Exception {
    when(mockRequest.params(":transformerId")).thenReturn("xml");

    String res = (String) cqlTransformHandler.handle(mockRequest, mockResponse);

    assertThat(res, is("{\"message\":\"Service not found\"}"));
  }

  @Test
  public void testServiceFoundWithValidResponseAndGzip() throws Exception {
    when(mockRequest.headers(HttpHeaders.ACCEPT_ENCODING)).thenReturn("gzip");

    when(mockRequest.params(":transformerId")).thenReturn("kml");

    String res = (String) cqlTransformHandler.handle(mockRequest, mockResponse);

    assertThat(res, is(""));
  }

  @Test
  public void testServiceFoundWithValidResponseNoGzip() throws Exception {
    when(mockRequest.headers(HttpHeaders.ACCEPT_ENCODING)).thenReturn("");

    when(mockRequest.params(":transformerId")).thenReturn("kml");

    String res = (String) cqlTransformHandler.handle(mockRequest, mockResponse);

    assertThat(res, is(""));
  }
}
