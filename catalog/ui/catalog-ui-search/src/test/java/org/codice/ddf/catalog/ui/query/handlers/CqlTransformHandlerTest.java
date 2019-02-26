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

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.MimeType;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import spark.Request;
import spark.Response;

public class CqlTransformHandlerTest {

  private List<ServiceReference> queryResponseTransformers;
  private CqlTransformHandler cqlTransformHandler;
  private BinaryContent binaryContent;

  @Mock private ServiceReference<QueryResponseTransformer> mockServiceReference;
  @Mock private BundleContext mockBundleContext;
  @Mock private EndpointUtil mockEndpointUtil;
  @Mock private Request mockRequest;
  @Mock private CqlQueryResponse mockCqlQueryResponse;
  @Mock private QueryResponse mockQueryResponse;
  @Mock private QueryResponseTransformer mockQueryResponseTransformer;
  @Mock private ServletOutputStream mockServletOutputStream;
  @Mock private HttpServletResponse mockHttpServletResponse;

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final String GZIP = "gzip";
  private static final String NO_GZIP = "";
  private static final String QUERY_PARAM = ":transformerId";
  private static final String RETURN_ID = "kml";
  private static final String OTHER_RETURN_ID = "xml";
  private static final String MIME_TYPE = "application/vnd.google-earth.kml+xml";
  private static final String SAFE_BODY =
      "{\"src\":\"ddf.distribution\",\"start\":1,\"count\":250,\"cql\":\"anyText ILIKE '*'\",\"sorts\":[{\"attribute\":\"modified\",\"direction\":\"descending\"}],\"id\":\"7a491439-948e-431b-815e-a04f32fecec9\"}";
  private static final String CONTENT = "test";
  private static final String SERVICE_NOT_FOUND = "\"Service not found\"";
  private static final String SERVICE_SUCCESS = GSON.toJson("");
  private static final String ATTACHMENT_REGEX =
      "^attachment;filename=\"export-\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z."
          + RETURN_ID
          + "\"$";

  private class MockResponse extends Response {

    private String contentType = "";
    private int statusCode = 0;
    private Map<String, String> headers = new HashMap<>();
    private HttpServletResponse mockHttpServletResponse;

    MockResponse(HttpServletResponse mockHttpServletResponse) {
      this.mockHttpServletResponse = mockHttpServletResponse;
    }

    @Override
    public void type(String contentType) {
      this.contentType = contentType;
    }

    @Override
    public String type() {
      return this.contentType;
    }

    @Override
    public void status(int statusCode) {
      this.statusCode = statusCode;
    }

    @Override
    public int status() {
      return this.statusCode;
    }

    @Override
    public void header(String header, String value) {
      headers.put(header, value);
    }

    Map<String, String> getHeaders() {
      return this.headers;
    }

    @Override
    public HttpServletResponse raw() {
      return this.mockHttpServletResponse;
    }
  }

  private MockResponse mockResponse;

  @Before
  public void setUp() throws Exception {

    initMocks(this);

    when(mockHttpServletResponse.getOutputStream()).thenReturn(mockServletOutputStream);

    mockResponse = new MockResponse(mockHttpServletResponse);

    queryResponseTransformers = new ArrayList<>();

    when(mockServiceReference.getProperty(Core.ID)).thenReturn(RETURN_ID);
    when(mockServiceReference.getProperty("mime-type")).thenReturn(ImmutableList.of(MIME_TYPE));

    MimeType mimeType = new MimeType(MIME_TYPE);
    binaryContent = new BinaryContentImpl(new ByteArrayInputStream(CONTENT.getBytes()), mimeType);

    queryResponseTransformers.add(mockServiceReference);

    cqlTransformHandler =
        new CqlTransformHandler(queryResponseTransformers, mockBundleContext, mockEndpointUtil);

    when(mockEndpointUtil.safeGetBody(mockRequest)).thenReturn(SAFE_BODY);

    when(mockEndpointUtil.executeCqlQuery(any(CqlRequest.class))).thenReturn(mockCqlQueryResponse);

    when(mockCqlQueryResponse.getQueryResponse()).thenReturn(mockQueryResponse);

    when(mockBundleContext.getService(mockServiceReference))
        .thenReturn(mockQueryResponseTransformer);

    when(mockQueryResponseTransformer.transform(any(QueryResponse.class), anyMap()))
        .thenReturn(binaryContent);
  }

  @Test
  public void testNoServiceFound() throws Exception {
    when(mockRequest.params(QUERY_PARAM)).thenReturn(OTHER_RETURN_ID);

    String res = GSON.toJson(cqlTransformHandler.handle(mockRequest, mockResponse));

    assertThat(res, containsString(SERVICE_NOT_FOUND));
    assertThat(mockResponse.status(), is(HttpStatus.NOT_FOUND_404));
  }

  @Test
  public void testServiceFoundWithValidResponseAndGzip() throws Exception {
    when(mockRequest.headers(HttpHeaders.ACCEPT_ENCODING)).thenReturn(GZIP);

    when(mockRequest.params(QUERY_PARAM)).thenReturn(RETURN_ID);

    String res = GSON.toJson(cqlTransformHandler.handle(mockRequest, mockResponse));

    assertThat(res, is(SERVICE_SUCCESS));
    assertThat(mockResponse.status(), is(HttpStatus.OK_200));
    assertThat(
        mockResponse.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION),
        matchesPattern(ATTACHMENT_REGEX));
    assertThat(mockResponse.getHeaders().get(HttpHeaders.CONTENT_ENCODING), is(GZIP));
    assertThat(mockResponse.type(), is(MIME_TYPE));
  }

  @Test
  public void testServiceFoundWithValidResponseNoGzip() throws Exception {
    when(mockRequest.headers(HttpHeaders.ACCEPT_ENCODING)).thenReturn(NO_GZIP);

    when(mockRequest.params(QUERY_PARAM)).thenReturn(RETURN_ID);

    String res = GSON.toJson(cqlTransformHandler.handle(mockRequest, mockResponse));

    assertThat(res, is(SERVICE_SUCCESS));
    assertThat(mockResponse.status(), is(HttpStatus.OK_200));
    assertThat(
        mockResponse.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION),
        matchesPattern(ATTACHMENT_REGEX));
    assertNull(mockResponse.getHeaders().get(HttpHeaders.CONTENT_ENCODING));
    assertThat(mockResponse.type(), is(MIME_TYPE));
  }
}
