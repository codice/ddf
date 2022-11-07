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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import ddf.catalog.data.types.Core;
import ddf.catalog.resource.impl.ResourceImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswXmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a CSW GetRecords response, extracting the record resource or the search results and CSW
 * records.
 */
public class GetRecordsMessageBodyReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsMessageBodyReader.class);

  public static final String BYTES_SKIPPED = "bytes-skipped";

  private CswXmlParser parser;

  private HierarchicalStreamDriver driver;

  private XStream xstream;

  private DataHolder argumentHolder;

  private Pattern filenamePattern = Pattern.compile(".*filename=\\\"?([^\\\"]+)\\\"?.*");

  public GetRecordsMessageBodyReader(
      CswXmlParser parser, Converter converter, CswSourceConfiguration configuration) {
    this.parser = parser;
    driver = new Xpp3Driver(new GetRecordsResponseNameCoder());
    xstream = new XStream(driver);
    xstream.allowTypesByWildcard(new String[] {"ddf.**", "org.codice.**"});
    xstream.setClassLoader(this.getClass().getClassLoader());
    xstream.registerConverter(converter);
    xstream.alias(CswConstants.GET_RECORDS_RESPONSE, CswRecordCollection.class);
    xstream.alias(
        CswConstants.CSW_NAMESPACE_PREFIX
            + CswConstants.NAMESPACE_DELIMITER
            + CswConstants.GET_RECORDS_RESPONSE,
        CswRecordCollection.class);
    buildArguments(configuration);
  }

  private void buildArguments(CswSourceConfiguration configuration) {
    argumentHolder = xstream.newDataHolder();
    argumentHolder.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, configuration.getOutputSchema());
    argumentHolder.put(CswConstants.CSW_MAPPING, configuration.getMetacardCswMappings());
    argumentHolder.put(CswConstants.AXIS_ORDER_PROPERTY, configuration.getCswAxisOrder());
    argumentHolder.put(Core.RESOURCE_URI, configuration.getMetacardMapping(Core.RESOURCE_URI));
    argumentHolder.put(Core.THUMBNAIL, configuration.getMetacardMapping(Core.THUMBNAIL));
    argumentHolder.put(CswConstants.TRANSFORMER_LOOKUP_KEY, TransformerManager.SCHEMA);
    argumentHolder.put(CswConstants.TRANSFORMER_LOOKUP_VALUE, configuration.getOutputSchema());
  }

  public CswRecordCollection readFrom(Map<String, List<String>> httpHeaders, InputStream inStream)
      throws IOException {

    String mediaType =
        Optional.ofNullable(getFirst(httpHeaders, "Content-Type"))
            .orElse("application/octet-stream")
            .split(";")[0]
            .trim();

    CswRecordCollection cswRecords = null;
    Map<String, Serializable> resourceProperties = new HashMap<>();
    // Check if the server returned a Partial Content response (hopefully in response to a range
    // header)
    String contentRangeHeader = getFirst(httpHeaders, "Content-Range");
    if (StringUtils.isNotBlank(contentRangeHeader)) {
      contentRangeHeader =
          StringUtils.substringBetween(contentRangeHeader.toLowerCase(), "bytes ", "-");
      long bytesSkipped = Long.parseLong(contentRangeHeader);
      resourceProperties.put(BYTES_SKIPPED, Long.valueOf(bytesSkipped));
    }

    // If the following HTTP header exists and its value is true, the input stream will contain
    // raw product data
    String productRetrievalHeader =
        getFirst(httpHeaders, CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER);
    if (productRetrievalHeader != null && productRetrievalHeader.equalsIgnoreCase("TRUE")) {
      String fileName = handleContentDispositionHeader(httpHeaders);
      cswRecords = new CswRecordCollection();
      cswRecords.setResource(new ResourceImpl(inStream, mediaType, fileName));
      cswRecords.setResourceProperties(resourceProperties);
      return cswRecords;
    }

    // Save original response for any exception message that might need to be
    // created
    String originalCswResponse = IOUtils.toString(inStream, StandardCharsets.UTF_8);
    LOGGER.debug("Converting to CswRecordCollection: \n {}", originalCswResponse);

    cswRecords = unmarshalWithStaxReader(originalCswResponse);
    return cswRecords;
  }

  private CswRecordCollection unmarshalWithStaxReader(String originalInputStream)
      throws IOException {
    CswRecordCollection cswRecords;

    try (InputStream inputStream =
        new ByteArrayInputStream(originalInputStream.getBytes("UTF-8"))) {
      HierarchicalStreamReader reader = driver.createReader(inputStream);
      cswRecords = (CswRecordCollection) xstream.unmarshal(reader, null, argumentHolder);
    } catch (XStreamException e) {
      throw new IOException("Unable to parse response from CSW server.", e);
    }

    return cswRecords;
  }

  /**
   * Check Content-Disposition header for filename and return it
   *
   * @param httpHeaders The HTTP headers
   * @return the filename
   */
  private String handleContentDispositionHeader(Map<String, List<String>> httpHeaders) {
    String contentDispositionHeader = getFirst(httpHeaders, "Content-Disposition");
    if (StringUtils.isNotBlank(contentDispositionHeader)) {
      Matcher filenameMatcher = filenamePattern.matcher(contentDispositionHeader);
      if (filenameMatcher.matches() && filenameMatcher.groupCount() > 0) {
        String filename = filenameMatcher.group(1);
        if (StringUtils.isNotBlank(filename)) {
          LOGGER.debug("Found Content-Disposition header, changing resource name to {}", filename);
          return filename;
        }
      }
    }
    return "";
  }

  private String getFirst(Map<String, List<String>> httpHeaders, String key) {
    String result = null;
    if (httpHeaders.containsKey(key)) {
      result = httpHeaders.get(key).get(0);
    }
    return result;
  }
}
