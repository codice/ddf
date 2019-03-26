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
import static org.hamcrest.Matchers.contains;
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

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Security;
import ddf.catalog.data.types.Topic;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.confluence.api.SearchResource;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
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

  private ClientFactoryFactory clientFactoryFactory;

  private EncryptionService encryptionService;

  private ResourceReader reader;

  private AttributeRegistry registry;

  private SearchResource client;

  private Response clientResponse;

  private ConfluenceInputTransformer transformer;

  @Before
  public void setup() {

    MetacardType type =
        new MetacardTypeImpl("confluence", MetacardImpl.BASIC_METACARD.getAttributeDescriptors());
    transformer = new ConfluenceInputTransformer(type, Collections.emptyList());

    encryptionService = mock(EncryptionService.class);
    reader = mock(ResourceReader.class);
    factory = mock(SecureCxfClientFactory.class);
    clientFactoryFactory = mock(ClientFactoryFactory.class);
    client = mock(SearchResource.class);
    registry = mock(AttributeRegistry.class);
    clientResponse = mock(Response.class);
    when(factory.getClient()).thenReturn(client);
    doReturn(clientResponse)
        .when(client)
        .search(
            anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt(), anyBoolean());
    when(encryptionService.decryptValue(anyString())).thenReturn("decryptedPass");
    when(registry.lookup("attrib1"))
        .thenReturn(
            Optional.of(
                new AttributeDescriptorImpl(
                    "attrib1", true, true, true, false, BasicTypes.STRING_TYPE)));
    when(registry.lookup("attrib2"))
        .thenReturn(
            Optional.of(
                new AttributeDescriptorImpl(
                    "attrib2", true, true, true, true, BasicTypes.STRING_TYPE)));
    confluence =
        new TestConfluenceSource(
            adapter,
            encryptionService,
            transformer,
            reader,
            registry,
            factory,
            clientFactoryFactory);
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
    ConfluenceSource source =
        new ConfluenceSource(
            adapter, encryptionService, transformer, reader, registry, clientFactoryFactory);
    source.setUsername("myname");
    source.setPassword("mypass");
    source.init();
    assertThat(source.getClientFactory(), is(nullValue()));
  }

  @Test
  public void testAttributeOverrides() throws Exception {
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

    setupRegistryEntry("dateAttribute", BasicTypes.DATE_TYPE);
    setupRegistryEntry("boolAttribute", BasicTypes.BOOLEAN_TYPE);
    setupRegistryEntry("longAttribute", BasicTypes.LONG_TYPE);
    setupRegistryEntry("intAttribute", BasicTypes.INTEGER_TYPE);
    setupRegistryEntry("shortAttribute", BasicTypes.SHORT_TYPE);
    setupRegistryEntry("floatAttribute", BasicTypes.FLOAT_TYPE);
    setupRegistryEntry("doubleAttribute", BasicTypes.DOUBLE_TYPE);
    setupRegistryEntry("binaryAttribute", BasicTypes.BINARY_TYPE);
    setupRegistryEntry("badAttribute", BasicTypes.INTEGER_TYPE);
    when(registry.lookup("missingAttribute")).thenReturn(Optional.empty());

    Instant now = Instant.now();

    List<String> additionalAttributes = new ArrayList<>();
    additionalAttributes.add("attrib1=val1");
    additionalAttributes.add("attrib2=val1,val2,val3");
    additionalAttributes.add("dateAttribute=2018-06-28T10:44:00+07:00");
    additionalAttributes.add("boolAttribute=true");
    additionalAttributes.add("longAttribute=12345678900000");
    additionalAttributes.add("intAttribute=1234");
    additionalAttributes.add("shortAttribute=1");
    additionalAttributes.add("floatAttribute=1.1");
    additionalAttributes.add("doubleAttribute=1.23456");
    additionalAttributes.add("binaryAttribute=binaryString");
    additionalAttributes.add("badAttribute=1.23456");
    additionalAttributes.add("missingAttribute=something");

    confluence.setAttributeOverrides(additionalAttributes);

    SourceResponse response = confluence.query(request);
    assertThat(response.getHits(), is(1L));
    Metacard mcard = response.getResults().get(0).getMetacard();
    assertThat(mcard, notNullValue());
    assertThat(mcard.getAttribute("attrib1").getValue(), is("val1"));
    assertThat(mcard.getAttribute("attrib2").getValues().size(), is(3));
    assertThat(
        mcard.getAttribute("dateAttribute").getValue(),
        is(DatatypeConverter.parseDateTime("2018-06-28T10:44:00+07:00").getTime()));
    assertThat(mcard.getAttribute("boolAttribute").getValue(), is(true));
    assertThat(mcard.getAttribute("longAttribute").getValue(), is(12345678900000L));
    assertThat(mcard.getAttribute("intAttribute").getValue(), is(1234));
    assertThat(mcard.getAttribute("shortAttribute").getValue(), is((short) 1));
    assertThat(mcard.getAttribute("floatAttribute").getValue(), is(1.1f));
    assertThat(mcard.getAttribute("doubleAttribute").getValue(), is(1.23456));
    assertThat(
        mcard.getAttribute("binaryAttribute").getValue(),
        is("binaryString".getBytes(Charset.forName("UTF-8"))));
    assertThat(mcard.getAttribute("badAttribute"), is(nullValue()));
    assertThat(mcard.getAttribute("missingAttribute"), is(nullValue()));
  }

  @Test
  public void verifyAllMappings() throws Exception {
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
    Metacard mcard = response.getResults().get(0).getMetacard();
    assertThat(
        mcard.getAttribute(Core.CREATED).getValue(),
        is(DatatypeConverter.parseDateTime("2013-09-18T14:50:42.616-07:00").getTime()));
    assertThat(
        mcard.getAttribute(Core.MODIFIED).getValue(),
        is(DatatypeConverter.parseDateTime("2015-06-16T19:21:39.141-07:00").getTime()));
    assertThat(
        mcard.getAttribute(Core.METACARD_CREATED).getValue(),
        is(DatatypeConverter.parseDateTime("2013-09-18T14:50:42.616-07:00").getTime()));
    assertThat(
        mcard.getAttribute(Core.METACARD_MODIFIED).getValue(),
        is(DatatypeConverter.parseDateTime("2015-06-16T19:21:39.141-07:00").getTime()));
    assertThat(mcard.getTags(), contains("confluence", "resource"));
    assertThat(mcard.getId(), is("1179681"));
    assertThat(mcard.getTitle(), is("Formatting Source Code"));
    assertThat(
        mcard.getAttribute(Associations.EXTERNAL).getValues(),
        contains("https://codice.atlassian.net/wiki/display/DDF/Formatting+Source+Code"));
    assertThat(mcard.getAttribute(Contact.CREATOR_NAME).getValue(), is("another"));
    assertThat(mcard.getAttribute(Contact.CONTRIBUTOR_NAME).getValue(), is("first.last"));
    assertThat(mcard.getAttribute(Media.TYPE).getValue(), is("text/html"));
    assertThat(mcard.getAttribute(Security.ACCESS_GROUPS).getValue(), is("ddf-developers"));
    assertThat(mcard.getAttribute(Security.ACCESS_INDIVIDUALS).getValue(), is("first.last"));
    assertThat(mcard.getAttribute(Topic.CATEGORY).getValue(), is("page"));
    assertThat(
        mcard.getAttribute(Topic.VOCABULARY).getValue(),
        is(
            "https://developer.atlassian.com/confdev/confluence-server-rest-api/advanced-searching-using-cql/cql-field-reference#CQLFieldReference-titleTitleType"));
    assertThat(mcard.getAttribute(Topic.KEYWORD).getValue(), is("testlabel"));
  }

  private void setupRegistryEntry(String attributeName, AttributeType type) {
    when(registry.lookup(attributeName))
        .thenReturn(
            Optional.of(new AttributeDescriptorImpl(attributeName, true, true, true, false, type)));
  }

  class TestConfluenceSource extends ConfluenceSource {
    private SecureCxfClientFactory<SearchResource> mockFactory;

    public TestConfluenceSource(
        FilterAdapter adapter,
        EncryptionService encryptionService,
        ConfluenceInputTransformer transformer,
        ResourceReader reader,
        AttributeRegistry registry,
        SecureCxfClientFactory<SearchResource> mockFactory,
        ClientFactoryFactory clientFactoryFactory) {
      super(adapter, encryptionService, transformer, reader, registry, clientFactoryFactory);
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
