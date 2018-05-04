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
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;
import net.opengis.filter.v_2_0.FilterType;
import org.xml.sax.SAXException;

public class FilterWriter {
  private static final String SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

  private static final String FILTER_SCHEMA_URL =
      "http://schemas.opengis.net/filter/2.0/filter.xsd";

  private final Marshaller marshaller;

  /**
   * Create a {@link FilterWriter}.
   *
   * <p>One important note regarding the {@link SchemaFactory}: {@link
   * javax.xml.XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI} cannot be used.
   *
   * <ol>
   *   <li>An error occurs - <i>{@link javax.xml.XMLConstants} cannot be found by
   *       catalog-ui-search</i>
   *   <li>The constant itself blows up the factory - {@code
   *       http://www.w3.org/2001/XMLSchema-instance}
   * </ol>
   *
   * See <a
   * href="https://docs.oracle.com/javase/8/docs/api/javax/xml/validation/SchemaFactory.html">
   * SchemaFactory</a> for more information.
   *
   * @param validationEnabled true if all XML writing done by this {@link FilterWriter} should also
   *     be validated against the <a href="http://schemas.opengis.net/filter/2.0/filter.xsd">filter
   *     schema</a>. False otherwise.
   * @throws JAXBException if a problem occurs setting up and configuring JAXB.
   */
  public FilterWriter(boolean validationEnabled) throws JAXBException {
    this.marshaller = JAXBContext.newInstance(FilterType.class).createMarshaller();
    if (validationEnabled) {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
      try {
        marshaller.setSchema(schemaFactory.newSchema(new URL(FILTER_SCHEMA_URL)));
      } catch (MalformedURLException | SAXException e) {
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
