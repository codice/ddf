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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.Marshaller;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.parser.xml.XmlParserConfigurator;

public class CswXmlParser {

  private final Parser xmlParser;

  private ParserConfigurator marshallConfig;

  private ParserConfigurator unmarshallConfig;

  public CswXmlParser() {
    this(new XmlParser());
  }

  public CswXmlParser(Parser parser) {
    this(parser, new ArrayList<>());
  }

  public CswXmlParser(Parser parser, List<String> additionalContextPaths) {
    this.xmlParser = parser;

    initializeParserConfig(additionalContextPaths);
  }

  private void initializeParserConfig(List<String> additionalContextPaths) {
    List<String> contextPath =
        new ArrayList<>(
            List.of(
                CswConstants.OGC_CSW_PACKAGE,
                CswConstants.OGC_FILTER_PACKAGE,
                CswConstants.OGC_GML_PACKAGE,
                CswConstants.OGC_OWS_PACKAGE));

    if (additionalContextPaths != null) {
      contextPath.addAll(additionalContextPaths);
    }

    NamespacePrefixMapper mapper =
        new NamespacePrefixMapper() {

          private final Map<String, String> prefixMap =
              Map.of(
                  CswConstants.CSW_OUTPUT_SCHEMA,
                  CswConstants.CSW_NAMESPACE_PREFIX,
                  CswConstants.OWS_NAMESPACE,
                  CswConstants.OWS_NAMESPACE_PREFIX,
                  CswConstants.XML_SCHEMA_LANGUAGE,
                  CswConstants.XML_SCHEMA_NAMESPACE_PREFIX,
                  CswConstants.OGC_SCHEMA,
                  CswConstants.OGC_NAMESPACE_PREFIX,
                  CswConstants.GML_SCHEMA,
                  CswConstants.GML_NAMESPACE_PREFIX,
                  CswConstants.DUBLIN_CORE_SCHEMA,
                  CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX,
                  CswConstants.DUBLIN_CORE_TERMS_SCHEMA,
                  CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
                  GmdConstants.GMD_NAMESPACE,
                  GmdConstants.GMD_PREFIX);

          @Override
          public String getPreferredPrefix(
              String namespaceUri, String suggestion, boolean requirePrefix) {
            return prefixMap.get(namespaceUri);
          }
        };

    marshallConfig = new XmlParserConfigurator();
    marshallConfig.setClassLoader(CswXmlParser.class.getClassLoader());
    marshallConfig.setContextPath(contextPath);

    marshallConfig.addProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshallConfig.addProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);

    unmarshallConfig = new XmlParserConfigurator();
    unmarshallConfig.setClassLoader(CswXmlParser.class.getClassLoader());
    unmarshallConfig.setContextPath(contextPath);
  }

  public void marshal(Object obj, OutputStream outputStream) throws ParserException {
    xmlParser.marshal(marshallConfig, obj, outputStream);
  }

  public String marshal(Object obj) throws ParserException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      xmlParser.marshal(marshallConfig, obj, outputStream);
      return outputStream.toString();
    } catch (IOException e) {
      throw new ParserException("Unable to marshal CSW object to string.", e);
    }
  }

  public <T> T unmarshal(Class<? extends T> cls, InputStream inputStream) throws ParserException {
    return xmlParser.unmarshal(unmarshallConfig, cls, inputStream);
  }

  public <T> T unmarshal(Class<? extends T> cls, String input) throws ParserException {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes())) {
      return unmarshal(cls, inputStream);
    } catch (IOException e) {
      throw new ParserException("Unable to unmarshal CSW string to object.", e);
    }
  }
}
