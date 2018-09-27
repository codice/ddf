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
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.parsers.ParserConfigurationException;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class Gml3ToWktImpl implements Gml3ToWkt {

  private static final Logger LOGGER = LoggerFactory.getLogger(Gml3ToWkt.class);

  private static final String EPSG_4326 = "EPSG:4326";

  private final ThreadLocal<Parser> parser;

  private static final ThreadLocal<WKTWriter> WKT_WRITER = ThreadLocal.withInitial(WKTWriter::new);

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  public Gml3ToWktImpl(Configuration gmlConfiguration) {
    parser =
        ThreadLocal.withInitial(
            () -> {
              Parser gmlParser = new Parser(gmlConfiguration);
              gmlParser.setStrict(false);
              return gmlParser;
            });
  }

  public String convert(String xml) throws ValidationException {
    try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
      return convert(stream);
    } catch (IOException e) {
      LOGGER.debug("IO exception during conversion of {}", xml, e);
      throw new ValidationExceptionImpl(
          e, Collections.singletonList("IO exception during conversion"), new ArrayList<>());
    }
  }

  public String convert(InputStream xml) throws ValidationException {
    Object parsedObject = parseXml(xml);

    if (parsedObject instanceof Envelope) {
      parsedObject = JTS.toGeometry((Envelope) parsedObject);
    }

    if (parsedObject instanceof Geometry) {
      Geometry geometry = standardizeGeometry((Geometry) parsedObject);

      try {
        geometry = applyMathTransform(geometry);
      } catch (TransformException e) {
        LOGGER.debug("Failed to transform geometry's CRS", e);
        throw new ValidationExceptionImpl(
            e, Collections.singletonList("Cannot transform geometry's CRS"), new ArrayList<>());
      }

      return WKT_WRITER.get().write(geometry);
    }

    LOGGER.debug("Unknown object parsed from GML and unable to convert to WKT");
    throw new ValidationExceptionImpl(
        "", Collections.singletonList("Couldn't not convert GML to WKT"), new ArrayList<>());
  }

  public Object parseXml(InputStream xml) throws ValidationException {
    try {
      return parser.get().parse(xml);
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

  /**
   * Iterate over the geometry collection and convert the non-standard LinearRing to LineString
   *
   * @param geometryCollection the geometryCollection to be iterated
   * @return an edited geometryCollection with LineString in place of LinearRing
   */
  private GeometryCollection standardizeGeometryCollection(GeometryCollection geometryCollection) {
    ArrayList<Geometry> geometries = new ArrayList<>();

    for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
      Geometry geometry = standardizeGeometry(geometryCollection.getGeometryN(i));
      geometries.add(geometry);
    }

    if (geometryCollection.getClass() == MultiPoint.class) {
      return GEOMETRY_FACTORY.createMultiPoint(geometries.toArray(new Point[0]));
    }

    if (geometryCollection.getClass() == MultiLineString.class) {
      return GEOMETRY_FACTORY.createMultiLineString(geometries.toArray(new LineString[0]));
    }

    if (geometryCollection.getClass() == MultiPolygon.class) {
      return GEOMETRY_FACTORY.createMultiPolygon(geometries.toArray(new Polygon[0]));
    }

    return GEOMETRY_FACTORY.createGeometryCollection(geometries.toArray(new Geometry[0]));
  }

  /**
   * Convert the non-standard LinearRing geometry to a LineString geometry
   *
   * @param geometry LinearRing to be converted
   * @return new LineString geometry
   */
  private Geometry linearRingToLineString(Geometry geometry) {
    return GEOMETRY_FACTORY.createLineString(((LinearRing) geometry).getCoordinateSequence());
  }

  /**
   * Applies the math transformation to change Coordinate Reference Systems
   *
   * @param geometry Geometry to be transformed
   * @return a Geometry with the transformation applied
   * @throws ValidationException
   * @throws TransformException
   */
  private Geometry applyMathTransform(Geometry geometry)
      throws ValidationException, TransformException {
    CoordinateReferenceSystem geometryCrs = getGeometryCrs(geometry);
    return JTS.transform(geometry, getLatLonTransform(geometryCrs));
  }

  /**
   * Standardizes the geometry by converting any instances of LinearRing to LineString.
   *
   * @param geometry the Geometry that may contain LinearRing
   * @return a new Geometry with LineStrings in place of LinearRings
   * @throws ValidationException
   */
  private Geometry standardizeGeometry(Geometry geometry) {
    if (geometry instanceof GeometryCollection) {
      return standardizeGeometryCollection((GeometryCollection) geometry);
    }

    if (geometry instanceof LinearRing) {
      return linearRingToLineString(geometry);
    }

    return geometry;
  }

  /**
   * Get the CRS from the provided geometry, defaulting to WGS84 if CRS not found in geometry.
   *
   * @param geometry a geometry used to determine CRS
   * @return Extracted CRS or default
   */
  private CoordinateReferenceSystem getGeometryCrs(Geometry geometry) {
    if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
      return (CoordinateReferenceSystem) geometry.getUserData();
    }

    LOGGER.trace("CRS not found in geometry, defaulting to WGS84");
    return DefaultGeographicCRS.WGS84;
  }

  private MathTransform getLatLonTransform(CoordinateReferenceSystem sourceCrs)
      throws ValidationException {
    try {
      return CRS.findMathTransform(sourceCrs, CRS.decode(EPSG_4326, false));
    } catch (FactoryException e) {
      throw new ValidationExceptionImpl(
          "Failed to find EPSG:4326 CRS, do you have the dependencies added?", e);
    }
  }
}
