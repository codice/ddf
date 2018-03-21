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
package org.codice.ddf.confluence.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.confluence.api.SearchResource;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.sort.SortOrder;

public class ConfluenceSourceTest {
  private static final String JSON_RESPONSE = getFileContent("full_response.json");

  private TestConfluenceSource confluence;

  private FilterBuilder builder = new GeotoolsFilterBuilder();

  private FilterAdapter adapter = new GeotoolsFilterAdapterImpl();

  private SecureCxfClientFactory<SearchResource> factory;

  private EncryptionService encryptionService;

  private ResourceReader reader;

  private SearchResource client;

  private Response clientResponse;

  private ConfluenceInputTransformer transformer;

  @Before
  public void setup() {

    MetacardType type = new MetacardTypeImpl("confluence", (List) null);
    transformer = new ConfluenceInputTransformer(type);

    encryptionService = mock(EncryptionService.class);
    reader = mock(ResourceReader.class);
    factory = mock(SecureCxfClientFactory.class);
    client = mock(SearchResource.class);
    clientResponse = mock(Response.class);
    when(factory.getClient()).thenReturn(client);
    doReturn(clientResponse)
        .when(client)
        .search(
            anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt(), anyBoolean());
    when(encryptionService.decryptValue(anyString())).thenReturn("decryptedPass");
    confluence = new TestConfluenceSource(adapter, encryptionService, transformer, reader, factory);
    confluence.setAvailabilityPollInterval(1);
    confluence.setConfigurationPid("configPid");
    confluence.setEndpointUrl("https://confluence/rest/api/content");
    confluence.setExpandedSections(Collections.singletonList("expandedField"));
    confluence.setUsername("username");
    confluence.setPassword("password");
    confluence.setIncludeArchivedSpaces(false);
    List<String> additionalAttributes = new ArrayList<>();
    additionalAttributes.add("attrib1=val1");
    additionalAttributes.add("attrib2=val1,val2,val3");
    confluence.setAttributeOverrides(additionalAttributes);
  }

  @Test
  public void testQuery() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("anyText").is().like().text("searchValue"),
                1,
                1,
                new SortByImpl("title", SortOrder.DESCENDING),
                false,
                1000));
    InputStream entity = new ByteArrayInputStream(JSON_RESPONSE.getBytes(StandardCharsets.UTF_8));
    when(clientResponse.getEntity()).thenReturn(entity);
    when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(1L));
    assertThat(response.getResults().get(0).getMetacard(), notNullValue());
  }

  @Test
  public void testQuerySortOnNonConfluenceAttribute() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("anyText").is().like().text("searchValue"),
                1,
                1,
                new SortByImpl("someAttribute", SortOrder.DESCENDING),
                false,
                1000));
    InputStream entity = new ByteArrayInputStream(JSON_RESPONSE.getBytes(StandardCharsets.UTF_8));
    when(clientResponse.getEntity()).thenReturn(entity);
    when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(1L));
    assertThat(response.getResults().get(0).getMetacard(), notNullValue());
  }

  @Test
  public void testQueryNullSort() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("anyText").is().like().text("searchValue"),
                1,
                1,
                null,
                false,
                1000));
    InputStream entity = new ByteArrayInputStream(JSON_RESPONSE.getBytes(StandardCharsets.UTF_8));
    when(clientResponse.getEntity()).thenReturn(entity);
    when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(1L));
    assertThat(response.getResults().get(0).getMetacard(), notNullValue());
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testFailedQuery() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("anyText").is().like().text("searchValue"),
                1,
                1,
                new SortByImpl("title", SortOrder.DESCENDING),
                false,
                1000));
    InputStream entity =
        new ByteArrayInputStream("Something bad happened".getBytes(StandardCharsets.UTF_8));
    when(clientResponse.getEntity()).thenReturn(entity);
    when(clientResponse.getStatus())
        .thenReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    SourceResponse response = confluence.query(request);
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testFailedQueryStreamError() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("anyText").is().like().text("searchValue"),
                1,
                1,
                new SortByImpl("title", SortOrder.DESCENDING),
                false,
                1000));
    InputStream entity = mock(InputStream.class);
    when(entity.read(any())).thenThrow(new IOException("exception"));
    when(clientResponse.getEntity()).thenReturn(entity);
    when(clientResponse.getStatus())
        .thenReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    SourceResponse response = confluence.query(request);
  }

  @Test
  public void testQueryWithSpace() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("anyText").is().like().text("searchValue"),
                1,
                1,
                new SortByImpl("title", SortOrder.DESCENDING),
                false,
                1000));
    InputStream entity = new ByteArrayInputStream(JSON_RESPONSE.getBytes(StandardCharsets.UTF_8));
    confluence.setConfluenceSpaces(Collections.singletonList("DDF"));
    when(clientResponse.getEntity()).thenReturn(entity);
    when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(1L));
    Metacard mcard = response.getResults().get(0).getMetacard();
    assertThat(mcard, notNullValue());
    assertThat(mcard.getAttribute("attrib1").getValue(), is("val1"));
    assertThat(mcard.getAttribute("attrib2").getValues().size(), is(3));
  }

  @Test
  public void testNonConfluenceQuery() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("metacard-tags").is().like().text("nonConfluecneTag"),
                1,
                1,
                new SortByImpl("title", SortOrder.DESCENDING),
                false,
                1000));

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(0L));
  }

  @Test
  public void testQueryNonConfluenceAttribute() throws Exception {
    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                builder.attribute("someAttribute").is().like().text("someValue"),
                1,
                1,
                null,
                false,
                1000));

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(0L));
  }

  @Test
  public void testIsAvailable() throws Exception {
    WebClient mockClient = mock(WebClient.class);
    when(factory.getWebClient()).thenReturn(mockClient);
    Response mockResponse = mock(Response.class);
    when(mockClient.head()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
    assertThat(confluence.isAvailable(), is(true));
  }

  @Test
  public void testCachedAvailable() throws Exception {
    WebClient mockClient = mock(WebClient.class);
    when(factory.getWebClient()).thenReturn(mockClient);
    Response mockResponse = mock(Response.class);
    when(mockClient.head()).thenReturn(mockResponse);
    when(mockResponse.getStatus())
        .thenReturn(
            Response.Status.OK.getStatusCode(),
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    confluence.setAvailabilityPollInterval(100000);
    assertThat(confluence.isAvailable(), is(true));
    assertThat(confluence.isAvailable(), is(true));
  }

  @Test
  public void testCachedAvailableExpired() throws Exception {
    WebClient mockClient = mock(WebClient.class);
    when(factory.getWebClient()).thenReturn(mockClient);
    Response mockResponse = mock(Response.class);
    when(mockClient.head()).thenReturn(mockResponse);
    when(mockResponse.getStatus())
        .thenReturn(
            Response.Status.OK.getStatusCode(),
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

    confluence.setAvailabilityPollInterval(5);
    assertThat(confluence.isAvailable(), is(true));
    Thread.sleep(10);
    assertThat(confluence.isAvailable(), is(false));
  }

  @Test
  public void testAvailibilityConnectionException() throws Exception {
    WebClient mockClient = mock(WebClient.class);
    when(factory.getWebClient()).thenReturn(mockClient);
    when(mockClient.head()).thenThrow(new RuntimeException("Connection exception"));
    assertThat(confluence.isAvailable(), is(false));
  }

  @Test
  public void testAvaliabilitySourceMonitor() throws Exception {
    SourceMonitor monitor = mock(SourceMonitor.class);
    WebClient mockClient = mock(WebClient.class);
    when(factory.getWebClient()).thenReturn(mockClient);
    Response mockResponse = mock(Response.class);
    when(mockClient.head()).thenReturn(mockResponse);
    when(mockResponse.getStatus())
        .thenReturn(
            Response.Status.OK.getStatusCode(),
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    assertThat(confluence.isAvailable(monitor), is(true));
    verify(monitor).setAvailable();
    Thread.sleep(10);
    assertThat(confluence.isAvailable(monitor), is(false));
    verify(monitor).setUnavailable();
  }

  @Test
  public void testRetrieveResource() throws Exception {
    when(reader.retrieveResource(any(), any())).thenReturn(null);
    confluence.retrieveResource(new URI("/my/uri"), new HashMap<>());
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(reader).retrieveResource(uriCaptor.capture(), mapCaptor.capture());
    assertThat(mapCaptor.getValue().get("username"), is("username"));
    assertThat(mapCaptor.getValue().get("password"), is("decryptedPass"));
  }

  @Test
  public void testInitNoEndpointUrl() throws Exception {
    ConfluenceSource source = new ConfluenceSource(adapter, encryptionService, transformer, reader);
    source.setUsername("myname");
    source.setPassword("mypass");
    source.init();
    assertThat(source.getClientFactory(), is(nullValue()));
  }

  class TestConfluenceSource extends ConfluenceSource {
    private SecureCxfClientFactory<SearchResource> mockFactory;

    public TestConfluenceSource(
        FilterAdapter adapter,
        EncryptionService encryptionService,
        ConfluenceInputTransformer transformer,
        ResourceReader reader,
        SecureCxfClientFactory<SearchResource> mockFactory) {
      super(adapter, encryptionService, transformer, reader);
      this.mockFactory = mockFactory;
    }

    @Override
    public void init() {}

    public SecureCxfClientFactory<SearchResource> getClientFactory() {
      return mockFactory;
    }
  }

  private static String getFileContent(String filePath) {
    try {
      return IOUtils.toString(
          ConfluenceSourceTest.class.getClassLoader().getResourceAsStream(filePath),
          StandardCharsets.UTF_8.toString());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read filepath: " + filePath);
    }
  }
}
