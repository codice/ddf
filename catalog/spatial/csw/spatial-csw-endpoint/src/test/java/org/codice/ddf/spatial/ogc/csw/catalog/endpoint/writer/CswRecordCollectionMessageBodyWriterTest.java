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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.writer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class CswRecordCollectionMessageBodyWriterTest {

  private TransformerManager mockManager = mock(TransformerManager.class);

  private QueryResponseTransformer mockTransformer = mock(QueryResponseTransformer.class);

  private BinaryContent mockContent = mock(BinaryContent.class);

  @Test
  public void testWriteToWithSchema()
      throws WebApplicationException, IOException, JAXBException, CatalogTransformerException {
    CswRecordCollectionMessageBodyWriter writer =
        new CswRecordCollectionMessageBodyWriter(mockManager);
    when(mockManager.getTransformerBySchema(anyString())).thenReturn(mockTransformer);

    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    when(mockTransformer.transform(any(SourceResponse.class), any(Map.class)))
        .thenReturn(mockContent);
    when(mockContent.getInputStream()).thenReturn(new ByteArrayInputStream("bytes".getBytes()));

    CswRecordCollection collection = createCswRecordCollection(6);
    collection.setNumberOfRecordsMatched(22);
    collection.setNumberOfRecordsReturned(6);
    final String exampleSchema = "http://example.com/schema";
    collection.setOutputSchema(exampleSchema);
    collection.setById(true);
    collection.setResultType(ResultType.HITS);
    QName example = new QName("example");

    collection.setElementName(Arrays.asList(example));
    collection.setElementSetType(ElementSetType.BRIEF);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writer.writeTo(collection, null, null, null, null, null, stream);

    verify(mockTransformer).transform(any(SourceResponse.class), captor.capture());

    Map arguments = captor.getValue();
    assertThat(arguments.get(CswConstants.WRITE_NAMESPACES), is(false));
    assertThat(arguments.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), is(exampleSchema));
    assertThat(arguments.get(CswConstants.RESULT_TYPE_PARAMETER), is(ResultType.HITS));
    assertThat(arguments.get(CswConstants.IS_BY_ID_QUERY), is(true));
    assertThat(arguments.get(CswConstants.ELEMENT_SET_TYPE), is(ElementSetType.BRIEF));
    assertThat(((QName[]) arguments.get(CswConstants.ELEMENT_NAMES))[0], is(example));
  }

  @Test
  public void testWriteValidate()
      throws WebApplicationException, IOException, JAXBException, CatalogTransformerException {
    CswRecordCollectionMessageBodyWriter writer =
        new CswRecordCollectionMessageBodyWriter(mockManager);
    when(mockManager.getTransformerBySchema(anyString())).thenReturn(mockTransformer);

    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    when(mockTransformer.transform(any(SourceResponse.class), any(Map.class)))
        .thenReturn(mockContent);
    when(mockContent.getInputStream()).thenReturn(new ByteArrayInputStream("bytes".getBytes()));

    CswRecordCollection collection = createCswRecordCollection(6);
    collection.setNumberOfRecordsMatched(22);
    collection.setNumberOfRecordsReturned(6);
    collection.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    collection.setById(true);
    QName example = new QName("example");

    collection.setElementName(Arrays.asList(example));
    collection.setElementSetType(ElementSetType.BRIEF);
    collection.setResultType(ResultType.VALIDATE);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writer.writeTo(collection, null, null, null, null, null, stream);

    verify(mockManager, times(1)).getTransformerBySchema(anyString());
    verify(mockTransformer).transform(any(SourceResponse.class), captor.capture());

    Map arguments = captor.getValue();
    assertThat(arguments.get(CswConstants.WRITE_NAMESPACES), is(false));
    assertThat(
        arguments.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), is(CswConstants.CSW_OUTPUT_SCHEMA));
    assertThat(arguments.get(CswConstants.RESULT_TYPE_PARAMETER), is(ResultType.VALIDATE));
    assertThat(arguments.get(CswConstants.IS_BY_ID_QUERY), is(true));
    assertThat(arguments.get(CswConstants.ELEMENT_SET_TYPE), is(ElementSetType.BRIEF));
    assertThat(((QName[]) arguments.get(CswConstants.ELEMENT_NAMES))[0], is(example));
  }

  @Test
  public void testWriteToWithMimeType()
      throws WebApplicationException, IOException, JAXBException, CatalogTransformerException {
    CswRecordCollectionMessageBodyWriter writer =
        new CswRecordCollectionMessageBodyWriter(mockManager);
    when(mockManager.getTransformerByMimeType(any(String.class))).thenReturn(mockTransformer);
    when(mockTransformer.transform(any(SourceResponse.class), any(Map.class)))
        .thenReturn(mockContent);
    when(mockContent.getInputStream()).thenReturn(new ByteArrayInputStream("bytes".getBytes()));

    CswRecordCollection collection = createCswRecordCollection(6);
    collection.setNumberOfRecordsMatched(22);
    collection.setNumberOfRecordsReturned(6);
    collection.setById(true);
    collection.setResultType(ResultType.RESULTS);
    collection.setMimeType(MediaType.APPLICATION_JSON);
    QName example = new QName("example");

    collection.setElementName(Arrays.asList(example));
    collection.setElementSetType(ElementSetType.BRIEF);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    writer.writeTo(collection, null, null, null, null, null, stream);

    verify(mockManager, times(1)).getTransformerByMimeType(any(String.class));

    // TODO - assert lookup by mime type
    // TODO failure case
  }

  @Test
  public void testWriteToProductData() throws MimeTypeParseException, IOException {
    CswRecordCollectionMessageBodyWriter writer =
        new CswRecordCollectionMessageBodyWriter(mockManager);
    byte[] data = "SampleData".getBytes();
    ByteArrayInputStream productData = new ByteArrayInputStream(data);
    MimeType mimeType = new MimeType("text", "plain");

    MultivaluedMap<String, Object> httpHeaders = new MultivaluedHashMap<>();

    Resource resource = new ResourceImpl(productData, mimeType, "ResourceName");
    CswRecordCollection collection = new CswRecordCollection();
    collection.setMimeType(mimeType.toString());
    collection.setResource(resource);
    collection.setOutputSchema(
        "http://www.iana.org/assignments/media-types/application/octet-stream");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    writer.writeTo(collection, null, null, null, null, httpHeaders, stream);

    assertThat(stream.toByteArray(), is(equalTo(resource.getByteArray())));
  }

  private CswRecordCollection createCswRecordCollection(int resultCount) {
    CswRecordCollection collection = new CswRecordCollection();
    collection.setCswRecords(createMetacardList(resultCount));
    return collection;
  }

  private List<Metacard> createMetacardList(int count) {
    List<Metacard> list = new LinkedList<>();

    for (int i = 0; i <= count; i++) {
      MetacardImpl metacard = new MetacardImpl();

      metacard.setId("id_" + i);
      metacard.setSourceId("source_" + i);
      metacard.setTitle("title " + i);

      list.add(metacard);
    }

    return list;
  }
}
