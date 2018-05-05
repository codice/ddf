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
package org.codice.ddf.catalog.ui.forms.filter;

import java.io.StringWriter;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;
import net.opengis.filter.v_2_0.FilterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@SuppressWarnings("squid:S1075" /* Will parameterize only if necessary. */)
public class FilterWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterWriter.class);

  private static final String FILTER_XSD_RESOURCE_PATH = "/schemas/filter.xsd";

  private static final String SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

  private final Marshaller marshaller;

  /**
   * Create a {@link FilterWriter}.
   *
   * <p>Accessing {@link javax.xml.XMLConstants#W3C_XML_SCHEMA_NS_URI} through the constants class
   * causes an error: {@code javax.xml.XMLConstants cannot be found by catalog-ui-search}.
   * Workaround is to use {@link #SCHEMA_LANGUAGE} constant instead.
   *
   * <p>See <a
   * href="https://docs.oracle.com/javase/8/docs/api/javax/xml/validation/SchemaFactory.html">
   * SchemaFactory</a> for more information.
   *
   * <p>See <a href="http://schemas.opengis.net/filter/2.0/">Filter 2.0</a> for original OGC schema
   * documents.
   *
   * @param validationEnabled true if all XML writing done by this {@link FilterWriter} should also
   *     be validated against the <a href="http://schemas.opengis.net/filter/2.0/filter.xsd">filter
   *     schema</a>. False otherwise.
   * @throws JAXBException if a problem occurs setting up and configuring JAXB.
   */
  public FilterWriter(boolean validationEnabled) throws JAXBException {
    this.marshaller = JAXBContext.newInstance(FilterType.class).createMarshaller();
    if (validationEnabled) {
      LOGGER.info("Loading filter schemas...");
      URL schemaLocation = FilterWriter.class.getResource(FILTER_XSD_RESOURCE_PATH);
      SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
      try {
        marshaller.setSchema(schemaFactory.newSchema(schemaLocation));
      } catch (SAXException e) {
        throw new JAXBException("Error reading filter schema", e);
      }
    }
  }

  public String marshal(JAXBElement element) throws JAXBException {
    StringWriter writer = new StringWriter();
    marshaller.marshal(element, writer);
    return writer.toString();
  }
}
