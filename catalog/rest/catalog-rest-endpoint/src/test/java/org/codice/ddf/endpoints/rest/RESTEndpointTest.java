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
package org.codice.ddf.endpoints.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.tika.TikaMimeTypeResolver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.attachment.impl.AttachmentParserImpl;
import org.junit.Test;

public class RESTEndpointTest {

  private static final int OK = 200;

  private static final int NOT_FOUND = 404;

  private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

  private static final String LOCAL_RETRIEVE_SERVLET_PATH = "/abc123456def";

  private static final String LOCAL_RETRIEVE_ADDRESS =
      "http://localhost:8181/services/catalog/abc123456def";

  private static final String FED_RETRIEVE_SERVLET_PATH = "/sources/test/abc123456def";

  private static final String FED_RETRIEVE_ADDRESS =
      "http://localhost:8181/services/catalog/sources/test/abc123456def";

  private static final String GET_SITENAME = "test";

  private static final String GET_ID = "abc123456def";

  private static final String GET_STREAM = "Test string for inputstream.";

  private static final String GET_OUTPUT_TYPE = "UTF-8";

  private static final String GET_MIME_TYPE = "text/xml";

  private static final String GET_KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";

  private static final String GET_FILENAME = "example.xml";

  /**
   * Test using a Head request to find out if Range headers are supported and to get resource size
   * of a local resource for use when using Range headers.
   */
  @Test
  public void testHeadRequestLocal() throws Exception {
    HttpServletResponse response = headTest(true);

    assertEquals("", response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verifyHeaders(response);
  }

  /**
   * Test using a Head request to find out if Range headers are supported and to get resource size
   * of a resource at a federated site for use when using Range headers.
   */
  @Test
  public void testHeadRequestFederated() throws Exception {
    HttpServletResponse response = headTest(false);

    assertEquals("", response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verifyHeaders(response);
  }

  @SuppressWarnings({"unchecked"})
  private HttpServletResponse headTest(boolean local) throws Exception {

    MetacardImpl metacard;
    List<Result> list = new ArrayList<>();
    Result result = mock(Result.class);
    InputStream inputStream;
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getOutputStream()).thenReturn(new MockServletOutputStream());

    CatalogFramework framework = givenCatalogFramework();
    list.add(result);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(list);
    when(framework.query(isA(QueryRequest.class), isNull())).thenReturn(queryResponse);

    metacard = new MetacardImpl();
    metacard.setSourceId(GET_SITENAME);
    when(result.getMetacard()).thenReturn(metacard);

    Resource resource = mock(Resource.class);
    inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
    when(resource.getInputStream()).thenReturn(inputStream);
    when(resource.getMimeTypeValue()).thenReturn(GET_MIME_TYPE);
    when(resource.getName()).thenReturn(GET_FILENAME);
    when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class)))
        .thenReturn(resource);

    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");

    CatalogService catalogService =
        new CatalogService(
            framework, new AttachmentParserImpl(mimeTypeMapper), mock(AttributeRegistry.class));
    catalogService.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    catalogService.setFilterBuilder(filterBuilder);

    RESTEndpoint restEndpoint = new RESTEndpoint(catalogService);

    if (local) {
      when(request.getPathInfo()).thenReturn(LOCAL_RETRIEVE_SERVLET_PATH);
      when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_RETRIEVE_ADDRESS));
    } else {
      when(request.getPathInfo()).thenReturn(FED_RETRIEVE_SERVLET_PATH);
      when(request.getRequestURL()).thenReturn(new StringBuffer(FED_RETRIEVE_ADDRESS));
    }

    restEndpoint.doHead(request, response);

    return response;
  }

  /** Tests local retrieve with a null QueryResponse */
  @Test
  public void testGetDocumentLocalNullQueryResponse() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.QUERY_RESPONSE_TEST);
    HttpServletResponse response = executeTest(framework, transformer, true);
    verify(response).setStatus(NOT_FOUND);
    assertEquals(
        response.getOutputStream().toString(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /** Tests federated retrieve with a null QueryResponse */
  @Test
  public void testGetDocumentFedNullQueryResponse() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.QUERY_RESPONSE_TEST);
    HttpServletResponse response = executeTest(framework, transformer, false);
    verify(response).setStatus(NOT_FOUND);
    assertEquals(
        response.getOutputStream().toString(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /** Tests local retrieve with a null Metacard */
  @Test
  public void testGetDocumentLocalNullMetacard() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.METACARD_TEST);
    HttpServletResponse response = executeTest(framework, transformer, true);
    verify(response).setStatus(NOT_FOUND);
    assertEquals(
        response.getOutputStream().toString(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /** Tests federated retrieve with a null Metacard */
  @Test
  public void testGetDocumentFedNullMetacard() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.METACARD_TEST);
    HttpServletResponse response = executeTest(framework, transformer, false);
    verify(response).setStatus(NOT_FOUND);
    assertEquals(
        response.getOutputStream().toString(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /** Tests local retrieve with a successful response */
  @Test
  public void testGetDocumentLocalSuccess() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.SUCCESS_TEST);
    HttpServletResponse response = executeTest(framework, transformer, true);

    assertEquals(GET_STREAM, response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verifyHeaders(response);
  }

  @Test
  public void testGetDocumentKml() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.KML_TEST);
    HttpServletResponse response = executeTest(framework, transformer, true);

    assertEquals(GET_STREAM, response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verify(response).addHeader("Accept-Ranges", "bytes");
    verify(response).setContentType(GET_KML_MIME_TYPE);
    verify(response).addHeader("Content-Disposition", "inline; filename=\"abc123456def.kml\"");
  }

  /** Tests federated retrieve with a successful response */
  @Test
  public void testGetDocumentFedSuccess() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.SUCCESS_TEST);
    HttpServletResponse response = executeTest(framework, transformer, false);

    assertEquals(GET_STREAM, response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verifyHeaders(response);
  }

  /** Tests retrieving a local resource with a successful response */
  @Test
  public void testGetDocumentResourceLocalSuccess() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.RESOURCE_TEST);
    HttpServletResponse response = executeTest(framework, transformer, true);

    assertEquals(GET_STREAM, response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verifyHeaders(response);
  }

  /** Tests retrieving a federated resource with a successful response */
  @Test
  public void testGetDocumentResourceFedSuccess() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    String transformer = mockTestSetup(framework, TestType.RESOURCE_TEST);
    HttpServletResponse response = executeTest(framework, transformer, false);

    assertEquals(GET_STREAM, response.getOutputStream().toString());
    verify(response).setStatus(OK);
    verifyHeaders(response);
  }

  private void verifyHeaders(HttpServletResponse response) {
    verify(response).addHeader("Accept-Ranges", "bytes");
    verify(response).setContentType(GET_MIME_TYPE);
    verify(response).addHeader("Content-Disposition", "inline; filename=\"example.xml\"");
  }

  /**
   * Creates the mock setup for the GET tests above. Parameters provide the CatalogFramework, which
   * will be setup for the test, and also specify which test case is being run.
   */
  @SuppressWarnings({"unchecked"})
  private String mockTestSetup(CatalogFramework framework, TestType testType) throws Exception {
    String transformer = null;
    QueryResponse queryResponse = mock(QueryResponse.class);
    when(framework.query(isA(QueryRequest.class), isNull())).thenReturn(queryResponse);

    List<Result> list = null;
    MetacardImpl metacard = null;
    Result result = mock(Result.class);
    InputStream inputStream;

    switch (testType) {
      case QUERY_RESPONSE_TEST:
        when(queryResponse.getResults()).thenReturn(list);
        break;

      case METACARD_TEST:
        list = new ArrayList<>();
        list.add(result);
        when(queryResponse.getResults()).thenReturn(list);

        when(result.getMetacard()).thenReturn(metacard);
        break;

      case RESOURCE_TEST:
        transformer = "resource";
        /* FALLTHRU */
        // fall through
      case SUCCESS_TEST:
        list = new ArrayList<>();
        list.add(result);
        when(queryResponse.getResults()).thenReturn(list);

        metacard = new MetacardImpl();
        metacard.setSourceId(GET_SITENAME);
        when(result.getMetacard()).thenReturn(metacard);

        Resource resource = mock(Resource.class);
        inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
        when(resource.getInputStream()).thenReturn(inputStream);
        when(resource.getMimeTypeValue()).thenReturn(GET_MIME_TYPE);
        when(resource.getName()).thenReturn(GET_FILENAME);
        when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class)))
            .thenReturn(resource);
        break;

      case KML_TEST:
        transformer = "kml";
        list = new ArrayList<>();
        list.add(result);
        when(queryResponse.getResults()).thenReturn(list);

        metacard = new MetacardImpl();
        metacard.setSourceId(GET_SITENAME);
        when(result.getMetacard()).thenReturn(metacard);

        BinaryContent content = mock(BinaryContent.class);
        inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
        when(content.getInputStream()).thenReturn(inputStream);
        when(content.getMimeTypeValue()).thenReturn(GET_KML_MIME_TYPE);
        when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class)))
            .thenReturn(content);
        break;
    }

    return transformer;
  }

  private HttpServletResponse executeTest(
      CatalogFramework framework, String transformer, boolean local) throws Exception {

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getOutputStream()).thenReturn(new MockServletOutputStream());

    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");

    CatalogService catalogService =
        new CatalogService(
            framework, new AttachmentParserImpl(mimeTypeMapper), mock(AttributeRegistry.class));

    catalogService.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    catalogService.setFilterBuilder(filterBuilder);

    RESTEndpoint restEndpoint = new RESTEndpoint(catalogService);

    if (local) {
      when(request.getPathInfo()).thenReturn(LOCAL_RETRIEVE_SERVLET_PATH);
      when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_RETRIEVE_ADDRESS));
    } else {
      when(request.getPathInfo()).thenReturn(FED_RETRIEVE_SERVLET_PATH);
      when(request.getRequestURL()).thenReturn(new StringBuffer(FED_RETRIEVE_ADDRESS));
    }

    restEndpoint.doGet(request, response);

    return response;
  }

  private CatalogFramework givenCatalogFramework()
      throws IngestException, SourceUnavailableException {
    CatalogFramework framework = mock(CatalogFramework.class);
    Metacard returnMetacard = mock(Metacard.class);

    when(returnMetacard.getId()).thenReturn(RESTEndpointTest.SAMPLE_ID);
    when(framework.create(isA(CreateRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));
    when(framework.create(isA(CreateStorageRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));
    return framework;
  }

  protected enum TestType {
    QUERY_RESPONSE_TEST,
    METACARD_TEST,
    SUCCESS_TEST,
    RESOURCE_TEST,
    KML_TEST
  }

  private static class MockServletOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream os = new ByteArrayOutputStream();

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      // ignore
    }

    @Override
    public void write(int b) throws IOException {
      os.write(b);
    }

    @Override
    public String toString() {
      return os.toString();
    }
  }
}
