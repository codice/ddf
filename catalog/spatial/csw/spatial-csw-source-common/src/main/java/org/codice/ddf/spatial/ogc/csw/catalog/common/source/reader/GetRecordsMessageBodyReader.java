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

import com.google.common.net.HttpHeaders;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.XppDriver;
import ddf.catalog.data.types.Core;
import ddf.catalog.resource.impl.ResourceImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollectionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManagerImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswRecordCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom JAX-RS MessageBodyReader for parsing a CSW GetRecords response, extracting the search
 * results and CSW records.
 */
public class GetRecordsMessageBodyReader implements MessageBodyReader<CswRecordCollection> {
  public static final String BYTES_SKIPPED = "bytes-skipped";
  private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsMessageBodyReader.class);
  private XStream xstream;

  private DataHolder argumentHolder;

  public GetRecordsMessageBodyReader(Converter converter, CswSourceConfiguration configuration) {
    xstream = new XStream(new XppDriver());
    xstream.allowTypesByWildcard(new String[] {"ddf.**", "org.codice.**"});
    xstream.setClassLoader(this.getClass().getClassLoader());
    xstream.registerConverter(converter);
    xstream.alias(CswConstants.GET_RECORDS_RESPONSE, CswRecordCollectionImpl.class);
    xstream.alias(
        CswConstants.CSW_NAMESPACE_PREFIX
            + CswConstants.NAMESPACE_DELIMITER
            + CswConstants.GET_RECORDS_RESPONSE,
        CswRecordCollectionImpl.class);
    buildArguments(configuration);
  }

  private void buildArguments(CswSourceConfiguration configuration) {
    argumentHolder = xstream.newDataHolder();
    argumentHolder.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, configuration.getOutputSchema());
    argumentHolder.put(CswConstants.CSW_MAPPING, configuration.getMetacardCswMappings());
    argumentHolder.put(CswConstants.AXIS_ORDER_PROPERTY, configuration.getCswAxisOrder());
    argumentHolder.put(Core.RESOURCE_URI, configuration.getMetacardMapping(Core.RESOURCE_URI));
    argumentHolder.put(Core.THUMBNAIL, configuration.getMetacardMapping(Core.THUMBNAIL));
    argumentHolder.put(CswConstants.TRANSFORMER_LOOKUP_KEY, TransformerManagerImpl.SCHEMA);
    argumentHolder.put(CswConstants.TRANSFORMER_LOOKUP_VALUE, configuration.getOutputSchema());
  }

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return CswRecordCollectionImpl.class.isAssignableFrom(type);
  }

  public CswRecordCollection readFrom(
      Class<CswRecordCollection> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream inStream)
      throws IOException, WebApplicationException {

    CswRecordCollection cswRecords = null;
    Map<String, Serializable> resourceProperties = new HashMap<>();
    // Check if the server returned a Partial Content response (hopefully in response to a range
    // header)
    String contentRangeHeader = httpHeaders.getFirst(HttpHeaders.CONTENT_RANGE);
    if (StringUtils.isNotBlank(contentRangeHeader)) {
      contentRangeHeader =
          StringUtils.substringBetween(contentRangeHeader.toLowerCase(), "bytes ", "-");
      long bytesSkipped = Long.parseLong(contentRangeHeader);
      resourceProperties.put(BYTES_SKIPPED, Long.valueOf(bytesSkipped));
    }

    // If the following HTTP header exists and its value is true, the input stream will contain
    // raw product data
    String productRetrievalHeader =
        httpHeaders.getFirst(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER);
    if (productRetrievalHeader != null && productRetrievalHeader.equalsIgnoreCase("TRUE")) {
      String fileName = handleContentDispositionHeader(httpHeaders);
      cswRecords = new CswRecordCollectionImpl();
      cswRecords.setResource(new ResourceImpl(inStream, mediaType.toString(), fileName));
      cswRecords.setResourceProperties(resourceProperties);
      return cswRecords;
    }

    // Save original response for any exception message that might need to be
    // created
    String originalCswResponse = IOUtils.toString(inStream, StandardCharsets.UTF_8);
    LOGGER.debug(
        "Converting to CswRecordCollection: \n {}", LogSanitizer.sanitize(originalCswResponse));

    cswRecords = unmarshalWithStaxReader(originalCswResponse);
    return cswRecords;
  }

  private CswRecordCollection unmarshalWithStaxReader(String originalInputStream)
      throws IOException {
    CswRecordCollection cswRecords;
    XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);

    InputStream inStream = new ByteArrayInputStream(originalInputStream.getBytes("UTF-8"));
    XMLStreamReader xmlStreamReader = null;
    try (InputStreamReader inputStreamReader =
        new InputStreamReader(inStream, StandardCharsets.UTF_8)) {
      xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStreamReader);
      HierarchicalStreamReader reader = new StaxReader(new QNameMap(), xmlStreamReader);
      cswRecords = (CswRecordCollection) xstream.unmarshal(reader, null, argumentHolder);
    } catch (XMLStreamException | XStreamException e) {
      // If an ExceptionReport is sent from the remote CSW site it will be sent with an
      // JAX-RS "OK" status, hence the ErrorResponse exception mapper will not fire.
      // Instead the ExceptionReport will come here and be treated like a GetRecords
      // response, resulting in an XStreamException since ExceptionReport cannot be
      // unmarshalled. So this catch clause is responsible for catching that XStream
      // exception and creating a JAX-RS response containing the original stream
      // (with the ExceptionReport) and rethrowing it as a WebApplicatioNException,
      // which CXF will wrap as a ClientException that the CswSource catches, converts
      // to a CswException, and logs.
      throw new WebApplicationException(e, createResponse(originalInputStream));
    } finally {
      IOUtils.closeQuietly(inStream);
      try {
        if (xmlStreamReader != null) {
          xmlStreamReader.close();
        }
      } catch (XMLStreamException e) {
        // ignore
      }
    }
    return cswRecords;
  }

  private Response createResponse(String originalCswResponse) {
    ResponseBuilder responseBuilder;
    try (ByteArrayInputStream bis =
        new ByteArrayInputStream(originalCswResponse.getBytes(StandardCharsets.UTF_8))) {
      responseBuilder = Response.ok(bis);
    } catch (IOException e) {
      responseBuilder = Response.ok();
    }
    responseBuilder.type("text/xml");
    return responseBuilder.build();
  }

  /**
   * Check Content-Disposition header for filename and return it
   *
   * @param httpHeaders The HTTP headers
   * @return the filename
   */
  private String handleContentDispositionHeader(MultivaluedMap<String, String> httpHeaders) {
    String contentDispositionHeader = httpHeaders.getFirst(HttpHeaders.CONTENT_DISPOSITION);
    if (StringUtils.isNotBlank(contentDispositionHeader)) {
      ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeader);
      String filename = contentDisposition.getParameter("filename");
      if (StringUtils.isNotBlank(filename)) {
        LOGGER.debug("Found Content-Disposition header, changing resource name to {}", filename);
        return filename;
      }
    }
    return "";
  }
}
