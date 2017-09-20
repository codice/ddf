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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.resource.Resource;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CswRecordCollectionMessageBodyWriter generates an xml response for a {@link CswRecordCollection}
 */
@Provider
public class CswRecordCollectionMessageBodyWriter
    implements MessageBodyWriter<CswRecordCollection> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CswRecordCollectionMessageBodyWriter.class);

  private static final List<String> XML_MIME_TYPES =
      Collections.unmodifiableList(Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML));

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";

  private final TransformerManager transformerManager;

  public CswRecordCollectionMessageBodyWriter(TransformerManager manager) {
    this.transformerManager = manager;
  }

  @Override
  public long getSize(
      CswRecordCollection recordCollection,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType) {
    return -1;
  }

  @Override
  public boolean isWriteable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return CswRecordCollection.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(
      CswRecordCollection recordCollection,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream outStream)
      throws IOException, WebApplicationException {

    final String mimeType = recordCollection.getMimeType();
    LOGGER.debug(
        "Attempting to transform RecordCollection with mime-type: {} & outputSchema: {}",
        mimeType,
        recordCollection.getOutputSchema());
    QueryResponseTransformer transformer;
    Map<String, Serializable> arguments = new HashMap<String, Serializable>();
    if (StringUtils.isBlank(recordCollection.getOutputSchema())
        && StringUtils.isNotBlank(mimeType)
        && !XML_MIME_TYPES.contains(mimeType)) {
      transformer = transformerManager.getTransformerByMimeType(mimeType);
    } else if (OCTET_STREAM_OUTPUT_SCHEMA.equals(recordCollection.getOutputSchema())) {
      Resource resource = recordCollection.getResource();
      httpHeaders.put(HttpHeaders.CONTENT_TYPE, Arrays.asList(resource.getMimeType()));
      httpHeaders.put(
          HttpHeaders.CONTENT_DISPOSITION,
          Arrays.asList(String.format("inline; filename=\"%s\"", resource.getName())));
      // Custom HTTP header to represent that the product data will be returned in the response.
      httpHeaders.put(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER, Arrays.asList("true"));
      // Accept-ranges header to represent that ranges in bytes are accepted.
      httpHeaders.put(CswConstants.ACCEPT_RANGES_HEADER, Arrays.asList(CswConstants.BYTES));
      ByteArrayInputStream in = new ByteArrayInputStream(resource.getByteArray());
      IOUtils.copy(in, outStream);
      return;
    } else {
      transformer = transformerManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA);
      if (recordCollection.getElementName() != null) {
        arguments.put(CswConstants.ELEMENT_NAMES, recordCollection.getElementName().toArray());
      }
      arguments.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, recordCollection.getOutputSchema());
      arguments.put(CswConstants.ELEMENT_SET_TYPE, recordCollection.getElementSetType());
      arguments.put(CswConstants.IS_BY_ID_QUERY, recordCollection.isById());
      arguments.put(CswConstants.GET_RECORDS, recordCollection.getRequest());
      arguments.put(CswConstants.RESULT_TYPE_PARAMETER, recordCollection.getResultType());
      arguments.put(CswConstants.WRITE_NAMESPACES, false);
    }

    if (transformer == null) {
      throw new WebApplicationException(
          new CatalogTransformerException("Unable to locate Transformer."));
    }

    BinaryContent content = null;
    try {
      content = transformer.transform(recordCollection.getSourceResponse(), arguments);
    } catch (CatalogTransformerException e) {
      throw new WebApplicationException(e);
    }

    if (content != null) {
      try (InputStream inputStream = content.getInputStream()) {
        IOUtils.copy(inputStream, outStream);
      }
    } else {
      throw new WebApplicationException(
          new CatalogTransformerException("Transformer returned null."));
    }
  }
}
