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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.xml.bind.Marshaller;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParserConfigurator;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;

public class CswXmlParser {

  private final Parser xmlParser;

  private ParserConfigurator marshallConfig;

  private ParserConfigurator unmarshallConfig;

  public CswXmlParser(Parser parser) {
    this.xmlParser = parser;

    initializeParserConfig();
  }

  private void initializeParserConfig() {
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
    marshallConfig.setClassLoader(ObjectFactory.class.getClassLoader());
    marshallConfig.setContextPath(
        List.of(
            CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE,
            CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE));
    marshallConfig.addProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshallConfig.addProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);

    unmarshallConfig = new XmlParserConfigurator();
    unmarshallConfig.setClassLoader(ObjectFactory.class.getClassLoader());
    unmarshallConfig.setContextPath(
        List.of(
            CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE,
            CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE));
  }

  public void marshal(Object obj, OutputStream outputStream) throws ParserException {
    xmlParser.marshal(marshallConfig, obj, outputStream);
  }

  public <T> T unmarshal(Class<? extends T> cls, InputStream inputStream) throws ParserException {
    return xmlParser.unmarshal(unmarshallConfig, cls, inputStream);
  }
}
