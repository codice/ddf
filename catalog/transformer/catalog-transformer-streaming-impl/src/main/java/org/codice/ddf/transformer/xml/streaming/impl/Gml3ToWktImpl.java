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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.XSDParserDelegate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class Gml3ToWktImpl implements Gml3ToWkt {

  private static final Logger LOGGER = LoggerFactory.getLogger(Gml3ToWktImpl.class);

  private static final String EPSG_4326 = "EPSG:4326";

  private static final SAXParserFactory PARSER_FACTORY =
      XMLUtils.getInstance().getSecureSAXParserFactory();

  static {
    PARSER_FACTORY.setNamespaceAware(true);
  }

  private final Supplier<XSDParserDelegate> gmlParserDelegateSupplier;

  public Gml3ToWktImpl(final Supplier<XSDParserDelegate> gmlParserDelegateSupplier) {
    this.gmlParserDelegateSupplier = gmlParserDelegateSupplier;
  }

  @Override
  public String convert(String xml) throws ValidationException {
    try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
      return convert(stream);
    } catch (IOException e) {
      LOGGER.debug("IO exception during conversion of {}", xml, e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("IO exception during conversion"), new ArrayList<>());
    }
  }

  @Override
  public String convert(InputStream xml) throws ValidationException {
    Object parsedObject = parseXml(xml);

    if (parsedObject instanceof Envelope) {
      parsedObject = JTS.toGeometry((Envelope) parsedObject);
    }

    if (parsedObject instanceof Geometry) {
      try {
        Geometry geometry = convertCRS((Geometry) parsedObject);
        return new WKTWriter().write(geometry);
      } catch (TransformException e) {
        LOGGER.debug("Failed to transform geometry to lon/lat", e);
        throw new ValidationExceptionImpl(
            e,
            Collections.singletonList("Cannot transform geometry to lon/lat"),
            new ArrayList<>());
      }
    }
    LOGGER.debug("Unknown object parsed from GML and unable to convert to WKT");
    throw new ValidationExceptionImpl(
        "", Collections.singletonList("Couldn't not convert GML to WKT"), new ArrayList<>());
  }

  @Override
  public Object parseXml(InputStream xml) throws ValidationException {
    try {
      final XMLReader reader = PARSER_FACTORY.newSAXParser().getXMLReader();
      final XSDParserDelegate gmlParserDelegate = gmlParserDelegateSupplier.get();
      reader.setContentHandler(gmlParserDelegate);
      reader.parse(new InputSource(xml));
      return gmlParserDelegate.getParsedObject();
    } catch (ParserConfigurationException | IOException e) {
      LOGGER.debug("Failed to read gml InputStream", e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("Cannot read gml"), new ArrayList<>());
    } catch (SAXException e) {
      LOGGER.debug("Failed to parse gml xml", e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("Cannot parse gml xml"), new ArrayList<>());
    }
  }

  private Geometry convertCRS(Geometry geometry) throws ValidationException, TransformException {
    return JTS.transform(geometry, getLatLonTransform());
  }

  private MathTransform getLatLonTransform() throws ValidationException {
    try {
      return CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRS.decode(EPSG_4326, false));
    } catch (FactoryException e) {
      throw new ValidationExceptionImpl(
          "Failed to find EPSG:4326 CRS, do you have the dependencies added?", e);
    }
  }

  public static Gml3ToWkt newGml2ToWkt() {
    return new Gml3ToWktImpl(org.geotools.gml2.GMLParserDelegate::new);
  }

  public static Gml3ToWkt newGml3ToWkt() {
    return new Gml3ToWktImpl(org.geotools.gml3.GMLParserDelegate::new);
  }

  public static Gml3ToWkt newGml32ToWkt() {
    return new Gml3ToWktImpl(
        () -> new XSDParserDelegate(new org.geotools.gml3.v3_2.GMLConfiguration()));
  }
}
