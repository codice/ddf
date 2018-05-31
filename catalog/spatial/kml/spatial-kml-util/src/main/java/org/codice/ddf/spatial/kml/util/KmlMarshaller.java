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
package org.codice.ddf.spatial.kml.util;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmlMarshaller {

  private Unmarshaller unmarshaller;
  private JAXBContext jaxbContext;

  private static final Logger LOGGER = LoggerFactory.getLogger(KmlMarshaller.class);

  private static final String UTF_8 = "UTF-8";

  public KmlMarshaller() {
    try {
      this.jaxbContext = JAXBContext.newInstance(Kml.class);
      this.unmarshaller = jaxbContext.createUnmarshaller();
    } catch (JAXBException e) {
      LOGGER.debug("Unable to create JAXB Context.  Setting to null.");
      this.jaxbContext = null;
    }
  }

  public Optional<Kml> unmarshal(InputStream inputStream) {
    if (unmarshaller == null) {
      return Optional.empty();
    }

    final XMLInputFactory xmlInputFactory = XMLUtils.getInstance().getSecureXmlInputFactory();

    try {
      XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);
      JAXBElement<Kml> unmarshal = unmarshaller.unmarshal(xmlStreamReader, Kml.class);
      Kml kml = unmarshal.getValue();
      return Optional.of(kml);
    } catch (JAXBException | XMLStreamException e) {
      LOGGER.debug("Exception while unmarshalling default style resource.", e);
      return Optional.empty();
    }
  }

  public String marshal(Kml kml) {
    String kmlResultString;
    StringWriter writer = new StringWriter();

    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
      marshaller.setProperty(Marshaller.JAXB_ENCODING, UTF_8);
      marshaller.marshal(kml, writer);
    } catch (JAXBException e) {
      LOGGER.debug("Failed to marshal KML: ", e);
    }

    kmlResultString = writer.toString();

    return kmlResultString;
  }
}
