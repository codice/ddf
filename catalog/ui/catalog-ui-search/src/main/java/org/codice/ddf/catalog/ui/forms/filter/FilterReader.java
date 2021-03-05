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

import static java.lang.String.format;

import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.platform.util.XMLUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/** Provide functions for hydrating Filter XML into Filter JAXB objects. */
public class FilterReader {
  private final JAXBContext context;

  public FilterReader() throws JAXBException {
    this.context = JAXBContext.newInstance(FilterType.class);
  }

  public JAXBElement<FilterType> unmarshalFilter(InputStream inputStream) throws JAXBException {
    return unmarshal(inputStream, FilterType.class);
  }

  @SuppressWarnings("unchecked")
  private <T> JAXBElement<T> unmarshal(InputStream inputStream, Class<T> tClass)
      throws JAXBException {
    SAXParserFactory factory = XMLUtils.getInstance().getSecureSAXParserFactory();
    factory.setNamespaceAware(true);

    SAXParser parser;
    try {
      parser = factory.newSAXParser();
    } catch (SAXException | ParserConfigurationException e) {
      throw new JAXBException("Could not create SAX parser", e);
    }

    XMLReader reader;
    try {
      reader = parser.getXMLReader();
    } catch (SAXException e) {
      throw new JAXBException("Could not get XML reader", e);
    }

    Source xmlSource = new SAXSource(reader, new InputSource(inputStream));
    Unmarshaller unmarshaller = context.createUnmarshaller();
    Object result = unmarshaller.unmarshal(xmlSource);

    if (!(result instanceof JAXBElement)) {
      throw new JAXBException("Unmarshaller did not return a JAXB object");
    }

    JAXBElement element = (JAXBElement) result;
    Object data = element.getValue();

    if (!tClass.isInstance(data)) {
      throw new JAXBException(
          format(
              "Unexpected data binding, expected %s but got %s",
              tClass.getName(), data.getClass().getName()));
    }

    return (JAXBElement<T>) element;
  }
}
