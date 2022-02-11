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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.resource.Resource;
import ddf.security.encryption.EncryptionService;
import ddf.security.permission.Permissions;
import ddf.security.permission.impl.PermissionsImpl;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.codice.ddf.spatial.ogc.csw.catalog.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.api.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.api.CswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.api.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollectionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManagerImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.GetRecordsResponseConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.MetacardCswRecordMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GetRecordsMessageBodyReaderTest {

  private Converter mockProvider = mock(Converter.class);

  private EncryptionService encryptionService = mock(EncryptionService.class);

  private Permissions permissions = new PermissionsImpl();

  private CswRecordMap cswRecordMap = new MetacardCswRecordMap();

  @Before
  public void setUp() {
    when(mockProvider.canConvert(any(Class.class))).thenReturn(true);
  }

  @Test
  public void testConfigurationArguments() throws Exception {
    CswRecordMap cswRecordMap = new MetacardCswRecordMap();
    CswSourceConfiguration config = new CswSourceConfiguration(encryptionService, permissions);
    config.setMetacardCswMappings(cswRecordMap.getCswToMetacardAttributeNames());
    config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    config.setCswAxisOrder(CswAxisOrder.LAT_LON);
    config.putMetacardCswMapping(Core.THUMBNAIL, CswConstants.CSW_REFERENCES);
    config.putMetacardCswMapping(Core.RESOURCE_URI, CswConstants.CSW_SOURCE);

    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);
    ArgumentCaptor<UnmarshallingContext> captor =
        ArgumentCaptor.forClass(UnmarshallingContext.class);

    try (InputStream is =
        GetRecordsMessageBodyReaderTest.class.getResourceAsStream("/getRecordsResponse.xml")) {
      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
      reader.readFrom(null, null, null, null, httpHeaders, is);
    }

    // Verify the context arguments were set correctly
    verify(mockProvider, times(1)).unmarshal(any(HierarchicalStreamReader.class), captor.capture());

    UnmarshallingContext context = captor.getValue();
    // Assert the properties needed for CswRecordConverter
    assertThat(context.get(CswConstants.CSW_MAPPING), notNullValue());
    Object cswMapping = context.get(CswConstants.CSW_MAPPING);
    assertThat(cswMapping, instanceOf(Map.class));
    assertThat(context.get(Core.RESOURCE_URI), instanceOf(String.class));
    assertThat(context.get(Core.RESOURCE_URI), is(CswConstants.CSW_SOURCE));
    assertThat(context.get(Core.THUMBNAIL), instanceOf(String.class));
    assertThat(context.get(Core.THUMBNAIL), is(CswConstants.CSW_REFERENCES));
    assertThat(context.get(CswConstants.AXIS_ORDER_PROPERTY), instanceOf(CswAxisOrder.class));
    assertThat(context.get(CswConstants.AXIS_ORDER_PROPERTY), is(CswAxisOrder.LAT_LON));

    // Assert the output Schema is set.
    assertThat(context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), instanceOf(String.class));
    assertThat(
        context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), is(CswConstants.CSW_OUTPUT_SCHEMA));

    assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_KEY), instanceOf(String.class));
    assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_KEY), is(TransformerManagerImpl.SCHEMA));
    assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_VALUE), instanceOf(String.class));
    assertThat(
        context.get(CswConstants.TRANSFORMER_LOOKUP_VALUE), is(CswConstants.CSW_OUTPUT_SCHEMA));
  }

  @Test
  public void testFullThreadCswRecordCollection() throws Exception {
    Metacard metacard = createMetacard();
    List<Metacard> inputMetacards = Collections.singletonList(metacard);
    CswRecordCollection collection = new CswRecordCollectionImpl();
    collection.setCswRecords(inputMetacards);

    MetacardType cswMetacardType =
        new MetacardTypeImpl(
            CswConstants.CSW_METACARD_TYPE_NAME,
            Arrays.asList(
                new ContactAttributes(),
                new LocationAttributes(),
                new MediaAttributes(),
                new TopicAttributes(),
                new AssociationsAttributes()));

    CswRecordConverter recordConverter = new CswRecordConverter(cswMetacardType);
    TransformerManager mockInputManager = mock(TransformerManagerImpl.class);
    when(mockInputManager.getTransformerByProperty(anyString(), anyString()))
        .thenReturn(recordConverter);
    CswTransformProvider metacardProvider = new CswTransformProvider(null, mockInputManager);
    GetRecordsResponseConverter provider = new GetRecordsResponseConverter(metacardProvider);

    CswSourceConfiguration config = createConfig();

    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(provider, config);
    CswRecordCollection cswRecords = null;

    try (InputStream is =
        GetRecordsMessageBodyReaderTest.class.getResourceAsStream("/getRecordsResponse.xml")) {

      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();

      cswRecords = reader.readFrom(null, null, null, null, httpHeaders, is);
    }
    List<Metacard> metacards = cswRecords.getCswRecords();
    assertThat(metacards, hasSize(3));
    assertThat(metacards.get(0).getMetacardType().getName(), is("csw:Record"));
    assertThat(metacards.get(0).getTitle(), containsString("title"));
  }

  @Test
  public void testFullThreadCswRecordCollectionAltPrefixes() throws Exception {
    Metacard metacard = createMetacard();
    List<Metacard> inputMetacards = Collections.singletonList(metacard);
    CswRecordCollection collection = new CswRecordCollectionImpl();
    collection.setCswRecords(inputMetacards);

    MetacardType cswMetacardType =
        new MetacardTypeImpl(
            CswConstants.CSW_METACARD_TYPE_NAME,
            Arrays.asList(
                new ContactAttributes(),
                new LocationAttributes(),
                new MediaAttributes(),
                new TopicAttributes(),
                new AssociationsAttributes()));

    CswRecordConverter recordConverter = new CswRecordConverter(cswMetacardType);
    TransformerManager mockInputManager = mock(TransformerManagerImpl.class);
    when(mockInputManager.getTransformerByProperty(anyString(), anyString()))
        .thenReturn(recordConverter);
    CswTransformProvider metacardProvider = new CswTransformProvider(null, mockInputManager);
    GetRecordsResponseConverter provider = new GetRecordsResponseConverter(metacardProvider);

    CswSourceConfiguration config = createConfig();

    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(provider, config);
    CswRecordCollection cswRecords = null;

    try (InputStream is =
        GetRecordsMessageBodyReaderTest.class.getResourceAsStream(
            "/getRecordsResponse-alt-prefixes.xml")) {

      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();

      cswRecords = reader.readFrom(null, null, null, null, httpHeaders, is);
    }
    List<Metacard> metacards = cswRecords.getCswRecords();
    assertThat(metacards.size(), is(3));
    assertThat(metacards.get(0).getMetacardType().getName(), is("csw:Record"));
    assertThat(metacards.get(0).getTitle(), containsString("title"));
  }

  // verifies UTF-8 encoding configured properly when XML includes foreign text with special
  // characters
  @Test
  public void testGetMultipleMetacardsWithForeignText() throws Exception {
    List<Metacard> inputMetacards = new ArrayList<>();
    MetacardImpl metacard = new MetacardImpl();
    inputMetacards.add(metacard);
    CswRecordCollection collection = new CswRecordCollectionImpl();
    collection.setCswRecords(inputMetacards);
    when(mockProvider.unmarshal(any(), any())).thenReturn(collection);
    CswSourceConfiguration config = new CswSourceConfiguration(encryptionService, permissions);
    config.setMetacardCswMappings(cswRecordMap.getCswToMetacardAttributeNames());
    config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

    CswRecordCollection cswRecords = null;
    try (InputStream is =
        GetRecordsMessageBodyReaderTest.class.getResourceAsStream(
            "/geomaticsGetRecordsResponse.xml")) {
      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
      cswRecords = reader.readFrom(null, null, null, null, httpHeaders, is);
    }
    List<Metacard> metacards = cswRecords.getCswRecords();
    assertThat(metacards, contains(metacard));
  }

  @Test
  public void testReadProductData() throws Exception {
    CswSourceConfiguration config = new CswSourceConfiguration(encryptionService, permissions);
    config.setMetacardCswMappings(cswRecordMap.getCswToMetacardAttributeNames());
    config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

    String sampleData = "SampleData";
    byte[] data = sampleData.getBytes();
    CswRecordCollection cswRecords = null;
    try (ByteArrayInputStream dataInputStream = new ByteArrayInputStream(data)) {
      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
      httpHeaders.add(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER, "TRUE");
      httpHeaders.add(
          HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=ResourceName"));
      MediaType mediaType = new MediaType("text", "plain");

      cswRecords = reader.readFrom(null, null, null, mediaType, httpHeaders, dataInputStream);
    }

    Resource resource = cswRecords.getResource();
    assertThat(resource, notNullValue());
    assertThat(resource.getName(), is("ResourceName"));
    assertThat(resource.getMimeType().toString(), is(MediaType.TEXT_PLAIN));
    assertThat(resource.getByteArray(), is(data));
  }

  @Test
  public void testPartialContentResponseHandling() throws Exception {
    CswSourceConfiguration config = new CswSourceConfiguration(encryptionService, permissions);
    config.setMetacardCswMappings(cswRecordMap.getCswToMetacardAttributeNames());
    config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

    String sampleData = "SampleData";
    byte[] data = sampleData.getBytes();
    CswRecordCollection cswRecords = null;
    try (ByteArrayInputStream dataInputStream = new ByteArrayInputStream(data)) {
      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
      httpHeaders.add(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER, "TRUE");
      httpHeaders.add(
          HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=ResourceName"));
      httpHeaders.add(
          HttpHeaders.CONTENT_RANGE,
          String.format("bytes 1-%d/%d", sampleData.length() - 1, sampleData.length()));
      MediaType mediaType = new MediaType("text", "plain");

      cswRecords = reader.readFrom(null, null, null, mediaType, httpHeaders, dataInputStream);
    }

    Resource resource = cswRecords.getResource();

    // assert that the CswRecordCollection properly extracted the bytes skipped from the Partial
    // Content response
    assertThat(
        cswRecords.getResourceProperties().get(GetRecordsMessageBodyReader.BYTES_SKIPPED), is(1L));

    // assert that the input stream has not been skipped at this stage. Since AbstractCswSource has
    // the number
    // of bytes that was attempted to be skipped, the stream must be aligned there instead.
    assertThat(resource.getByteArray(), is(data));
  }

  @Test
  public void testPartialContentNotSupportedHandling() throws Exception {
    CswSourceConfiguration config = new CswSourceConfiguration(encryptionService, permissions);
    config.setMetacardCswMappings(cswRecordMap.getCswToMetacardAttributeNames());
    config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

    String sampleData = "SampleData";
    byte[] data = sampleData.getBytes();
    CswRecordCollection cswRecords = null;
    try (ByteArrayInputStream dataInputStream = new ByteArrayInputStream(data)) {
      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
      httpHeaders.add(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER, "TRUE");
      httpHeaders.add(
          HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=ResourceName"));
      MediaType mediaType = new MediaType("text", "plain");

      cswRecords = reader.readFrom(null, null, null, mediaType, httpHeaders, dataInputStream);
    }

    Resource resource = cswRecords.getResource();

    // assert that the CswRecordCollection property is not set if the server does not support
    // Partial Content responses
    assertThat(
        cswRecords.getResourceProperties().get(GetRecordsMessageBodyReader.BYTES_SKIPPED),
        nullValue());

    // assert that the input stream has not been skipped at this stage. Since AbstractCswSource has
    // the number
    // of bytes that was attempted to be skipped, the stream must be aligned there instead.
    assertThat(resource.getByteArray(), is(data));
  }

  @Test(expected = WebApplicationException.class)
  public void testExceptionReport() throws Exception {
    GetRecordsMessageBodyReader reader =
        new GetRecordsMessageBodyReader(
            mockProvider, new CswSourceConfiguration(encryptionService, permissions));

    try (InputStream is =
        GetRecordsMessageBodyReaderTest.class.getResourceAsStream("/exceptionReport.xml")) {
      MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
      reader.readFrom(null, null, null, null, httpHeaders, is);
    }
  }

  private Metacard createMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("metacard1");
    metacard.setTitle("title1");
    return metacard;
  }

  private CswSourceConfiguration createConfig() {
    CswSourceConfiguration config = new CswSourceConfiguration(encryptionService, permissions);
    Map<String, String> mappings = new HashMap<>();
    mappings.put(Core.CREATED, "dateSubmitted");
    mappings.put(Metacard.EFFECTIVE, "created");
    mappings.put(Core.MODIFIED, "modified");
    mappings.put(Metacard.CONTENT_TYPE, "type");
    config.setMetacardCswMappings(mappings);
    config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    config.setCswAxisOrder(CswAxisOrder.LAT_LON);
    config.putMetacardCswMapping(Core.THUMBNAIL, CswConstants.CSW_REFERENCES);
    config.putMetacardCswMapping(Core.RESOURCE_URI, CswConstants.CSW_SOURCE);
    return config;
  }
}
