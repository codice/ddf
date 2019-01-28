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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PolygonPropertyType;
import net.opengis.gml.v_3_1_1.PolygonType;
import org.jvnet.ogc.gml.v_3_1_1.ObjectFactoryInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311Constants;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311ConverterInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311CoordinateConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311GeometryConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311PolygonConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311SRSReferenceGroupConverterInterface;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link JTSToGML311GeometryConverter} that provides a means of customizing
 * the GML output. By default, the {@code CswJTSToCML311GeometryConverter} behaves identically to
 * the {@code JTSToGML311GeometryConverter}, but the output of the converter can be customized by
 * supplying a {@code Map} of configuration properties.
 */
public class CswJTSToGML311GeometryConverter extends JTSToGML311GeometryConverter {

  public static final String USE_POS_LIST_GEO_CONVERTER_PROP_KEY = "usePosList";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CswJTSToGML311GeometryConverter.class);

  private final JTSToGML311CoordinateConverter cswCoordinateConverter;

  private final JTSToGML311ConverterInterface<LinearRingType, AbstractRingPropertyType, LinearRing>
      cswLinearRingConverter;

  private final JTSToGML311ConverterInterface<PolygonType, PolygonPropertyType, Polygon>
      cswPolygonConverter;

  /**
   * Constructs a JTS to GML Geometry converter that is functionally identical
   * to the converter constructed by
   * {@link JTSToGML311GeometryConverter#JTSToGML311GeometryConverter()
   */
  public CswJTSToGML311GeometryConverter() {
    this(null);
  }

  /**
   * Constructs a JTS to GML Geometry converter that is functionally identical
   * to the converter constructed by
   * {@link JTSToGML311GeometryConverter#JTSToGML311GeometryConverter()
   * with the exception that the GML that output is customized based on the
   * properties contained in a property {@link Map}.
   *
   * Valid properties include:
   *
   *    {@code USE_POS_LIST_GEO_CONVERTER_PROP_KEY}, ["true" | "false" ] -
   *       When "true", {@link LinearRingType}s constructed by the
   *       converter will have the posList, rather than the
   *       posOrPointPropertyOrPointRep, member variable set and populated.
   *       When converted to a string, this results in the GML containing a
   *       single <posList> element rather than a list of <pos> elements.
   *
   * @param propertyMap
   *      A map of properties that indicate a desired set of customizations
   *      to the GML output by the converter.
   *
   */
  public CswJTSToGML311GeometryConverter(Map<String, String> propertyMap) {
    this(
        JTSToGML311Constants.DEFAULT_OBJECT_FACTORY,
        JTSToGML311Constants.DEFAULT_SRS_REFERENCE_GROUP_CONVERTER,
        propertyMap);
  }

  /**
   * Constructs a JTS to GML Geometry converter that is functionally identical to the converter
   * constructed by {@link
   * JTSToGML311GeometryConverter#JTSToGML311GeometryConverter(ObjectFactoryInterface,
   * JTSToGML311SRSReferenceGroupConverterInterface)} with the exception that the GML that output is
   * customized based on the properties contained in a property {@link Map}.
   *
   * <p>Valid properties include:
   *
   * <p>{@code USE_POS_LIST_GEO_CONVERTER_PROP_KEY}, ["true" | "false" ] - When "true", {@link
   * LinearRingType}s constructed by the converter will have the posList, rather than the
   * posOrPointPropertyOrPointRep, member variable set and populated. When converted to a string,
   * this results in the GML containing a single <posList> element rather than a list of <pos>
   * elements.
   *
   * @param objectFactory
   * @param srsReferenceGroupConverter
   * @param propertyMap A map of properties that indicate a desired set of customizations to the GML
   *     output by the converter.
   */
  public CswJTSToGML311GeometryConverter(
      ObjectFactoryInterface objectFactory,
      JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter,
      Map<String, String> propertyMap) {
    super(objectFactory, srsReferenceGroupConverter);

    if (null == propertyMap) {
      propertyMap = new HashMap<String, String>();
    }

    this.cswCoordinateConverter =
        new JTSToGML311CoordinateConverter(objectFactory, srsReferenceGroupConverter);
    this.cswLinearRingConverter =
        new CswJTSToGML311LinearRingConverter(
            objectFactory,
            srsReferenceGroupConverter,
            this.cswCoordinateConverter,
            Boolean.valueOf(propertyMap.get(USE_POS_LIST_GEO_CONVERTER_PROP_KEY)));
    this.cswPolygonConverter =
        new JTSToGML311PolygonConverter(
            objectFactory, srsReferenceGroupConverter, this.cswLinearRingConverter);
  }

  /** @see {@code JTSToGML311GeometryConverter#doCreateGeometryType(Geometry geometry} */
  @Override
  protected AbstractGeometryType doCreateGeometryType(Geometry geometry)
      throws IllegalArgumentException {
    if (geometry instanceof LinearRing) {
      LOGGER.debug("Creating LinearRingType");
      return cswLinearRingConverter.createGeometryType((LinearRing) geometry);
    } else if (geometry instanceof Polygon) {
      LOGGER.debug("Creating PolygonType");
      return cswPolygonConverter.createGeometryType((Polygon) geometry);
    } else {
      LOGGER.debug("Passing Geometry to superclass for default doCreateGeometryType processing");
      return super.doCreateGeometryType(geometry);
    }
  }

  /**
   * @see {@code JTSToGML311GeometryConverter#createElement(Geometry)
   */
  @Override
  public JAXBElement<? extends AbstractGeometryType> createElement(Geometry geometry)
      throws IllegalArgumentException {

    if (geometry instanceof LinearRing) {
      LOGGER.debug("Creating LinearRing");
      return cswLinearRingConverter.createElement((LinearRing) geometry);
    } else if (geometry instanceof Polygon) {
      LOGGER.debug("Creating Polygon");
      return cswPolygonConverter.createElement((Polygon) geometry);
    } else {
      LOGGER.debug("Passing Geometry to superclass for default createElement processing");
      return super.createElement(geometry);
    }
  }
}
