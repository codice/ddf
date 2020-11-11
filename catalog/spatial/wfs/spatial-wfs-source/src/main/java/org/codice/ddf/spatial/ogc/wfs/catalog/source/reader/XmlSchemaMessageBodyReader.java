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
package org.codice.ddf.spatial.ogc.wfs.catalog.source.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsUriResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

@Provider
public class XmlSchemaMessageBodyReader implements MessageBodyReader<XmlSchema> {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlSchemaMessageBodyReader.class);

  private static final String XSD_PREFIX = "xsd";

  private static final XPathExpression IS_SCHEMA_XPATH;

  protected WfsUriResolver wfsUriResolver = new WfsUriResolver();

  static {
    XPathExpression expression = null;
    try {
      XPathFactory xPathFactory = XPathFactory.newInstance();
      NamespaceMap namespaceMap = new NamespaceMap();
      namespaceMap.add(XSD_PREFIX, XMLConstants.W3C_XML_SCHEMA_NS_URI);
      XPath xpath = xPathFactory.newXPath();
      xpath.setNamespaceContext(namespaceMap);
      expression = xpath.compile("boolean(/" + XSD_PREFIX + ":schema)");
    } catch (XPathExpressionException e) {
      throw new ExceptionInInitializerError(e);
    }
    IS_SCHEMA_XPATH = expression;
  }

  @Override
  public boolean isReadable(
      Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
    return XmlSchema.class.isAssignableFrom(clazz);
  }

  @Override
  public XmlSchema readFrom(
      Class<XmlSchema> clazz,
      Type type,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> headers,
      InputStream inStream)
      throws IOException, WebApplicationException {

    String input = IOUtils.toString(inStream, StandardCharsets.UTF_8);
    inStream.reset();

    if (isValid(new InputSource(inStream))) {
      XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
      schemaCollection.init();
      schemaCollection.setSchemaResolver(wfsUriResolver);
      return schemaCollection.read(new StringReader(input));
    } else {
      LOGGER.debug("Did not receive valid XML Schema, instead got: \n{}", input);
      return null;
    }
  }

  /**
   * Checks that the given InputSource represents a valid schema. Schemas may contain external links
   * and resolving them is slow, so instead of doing full validation against the XML schema schema,
   * we just check for the "xsd:schema" element at the root.
   *
   * @param inputSource the schema to validate
   */
  private boolean isValid(InputSource inputSource) {
    try {
      return (boolean) IS_SCHEMA_XPATH.evaluate(inputSource, XPathConstants.BOOLEAN);
    } catch (XPathExpressionException e) {
      throw new XmlSchemaException("Unable to validate schema", e);
    }
  }
}
