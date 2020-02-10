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
package org.codice.ddf.transformer.xml.streaming.impl;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import ddf.catalog.data.types.Validation;
import ddf.catalog.validation.ValidationException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.codice.ddf.transformer.xml.streaming.AbstractSaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerUtils;
import org.codice.ddf.transformer.xml.streaming.lib.SaxEventToXmlElementConverter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.gml2.GMLHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A sax event handler to handle GML sax events and stores the data as a WKT. Delegates GML events
 * to {@link org.locationtech.jts.io.gml2.GMLHandler}. Uses {@link
 * org.locationtech.jts.io.WKTWriter} to write the Geometries to WKT. Note: ONLY CAN PARSE GML2
 * points. Will throw hard-to-debug Null Pointer Exceptions if used with GML3 or other GML2
 * geometries. {@inheritDoc}
 */
public class GmlHandler extends AbstractSaxEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GmlHandler.class);

  private static Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();

  GMLHandler gh;

  WKTWriter wktWriter;

  private List<Attribute> attributes;

  private boolean readingGml = false;

  private boolean readingGml3 = false;

  private SaxEventToXmlElementConverter gml3Element;

  private Gml3ToWkt gml3Converter;

  private Deque<String> state;

  private SaxEventHandlerUtils saxEventHandlerUtils = new SaxEventHandlerUtils();

  static {
    attributeDescriptors.addAll(new ValidationAttributes().getAttributeDescriptors());
    attributeDescriptors.add(new CoreAttributes().getAttributeDescriptor(Metacard.GEOGRAPHY));
  }

  public GmlHandler(GMLHandler gmlHandler, Gml3ToWkt gml3Converter) {
    this.gh = gmlHandler;
    this.gml3Converter = gml3Converter;
    wktWriter = new WKTWriter();
    attributes = new ArrayList<>();
    try {
      gml3Element = new SaxEventToXmlElementConverter();
    } catch (UnsupportedEncodingException | XMLStreamException e) {
      LOGGER.debug("Error constructing new SaxEventToXmlElementConverter()", e);
    }
    state = new ArrayDeque<>();
  }

  /** @return list of {@link Attribute} (should be all <Metacard.GEOGRAPHY, WKT strings>) */
  @Override
  public List<Attribute> getAttributes() {
    return saxEventHandlerUtils.getCombinedMultiValuedAttributes(
        getSupportedAttributeDescriptors(), attributes);
  }

  @Override
  public Set<AttributeDescriptor> getSupportedAttributeDescriptors() {
    return attributeDescriptors;
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    try {
      gml3Element.addNamespace(prefix, uri);
    } catch (XMLStreamException e) {
      LOGGER.debug("Error adding namespace to SaxEventToXmlConverter()", e);
    }
  }

  /**
   * Takes in a sax event from {@link
   * org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}, and if it is a gml:Point
   * or a sub-element of a gml:Point, it will hand the event off to the GML handler for further
   * parsing.
   *
   * @param uri the URI that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param localName the localName that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param qName the qName that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param attributes the attributes that are passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if (readingGml || uri.equalsIgnoreCase("http://www.opengis.net/gml")) {
      state.push(localName);
      readingGml = true;
      try {
        gml3Element.toElement(uri, localName, attributes);
      } catch (XMLStreamException e) {
        LOGGER.debug("Error writing toElement in SaxEventToXmlConverter()", e);
      }
      if (localName.equalsIgnoreCase("pos")) {
        readingGml3 = true;
      }
      if (!readingGml3) {

        try {
          gh.startElement(uri, localName, qName, attributes);
        } catch (SAXException e) {
          LOGGER.debug("GML threw a SAX exception", e);
        }
      }
    }
  }

  /**
   * Takes in a sax event from {@link
   * org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}, and if it is a gml:Point
   * or a sub-element of a gml:Point, it will * hand the event off to the GML handler for further
   * parsing. If it is a gml:Point, the point is stored as a WKT in the attributes list
   *
   * @param namespaceURI the namespaceURI that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param localName the localName that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param qName the qName that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   */
  @Override
  public void endElement(String namespaceURI, String localName, String qName) {
    if (readingGml) {
      try {
        gml3Element.toElement(namespaceURI, localName);
      } catch (XMLStreamException e) {
        LOGGER.debug("Error writing to element in SaxEventToXmlConverter()", e);
      }
      if (!readingGml3) {
        try {
          gh.endElement(namespaceURI, localName, qName);
        } catch (SAXException e) {
          LOGGER.debug("GML threw a SAX exception", e);
        }
      }
      state.pop();
      if (state.isEmpty()) {
        readingGml = false;
        if (!readingGml3) {
          Geometry geo = gh.getGeometry();
          attributes.add(new AttributeImpl(Metacard.GEOGRAPHY, wktWriter.write(geo)));
        } else {
          try {
            attributes.add(
                new AttributeImpl(
                    Metacard.GEOGRAPHY, gml3Converter.convert(gml3Element.toString())));
          } catch (ValidationException e) {
            this.attributes.add(
                new AttributeImpl(Validation.VALIDATION_ERRORS, "geospatial-handler"));
          }
        }
        readingGml3 = false;
        gml3Element.reset();
      }
    }
  }

  /**
   * Takes in a sax event from {@link
   * org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}, and if it is a gml:Point
   * or a sub-element of a gml:Point, it will * hand the event off to the GML handler for further
   * parsing.
   *
   * @param ch the ch that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param start the start that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   * @param length the length that is passed in by {@link
   *     org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate}
   */
  @Override
  public void characters(char[] ch, int start, int length) {
    if (readingGml) {
      try {
        gml3Element.toElement(ch, start, length);
      } catch (XMLStreamException e) {
        LOGGER.debug("Error writing to element in SaxEventToXmlConverter()", e);
      }
      if (!readingGml3) {
        try {
          gh.characters(ch, start, length);
        } catch (SAXException e) {
          LOGGER.debug("GML threw a SAX exception", e);
        }
      }
    }
  }
}
