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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.opengis.gml.v_3_2_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_2_1.CoordinatesType;
import net.opengis.gml.v_3_2_1.CurvePropertyType;
import net.opengis.gml.v_3_2_1.DirectPositionType;
import net.opengis.gml.v_3_2_1.GeometryPropertyType;
import net.opengis.gml.v_3_2_1.LineStringType;
import net.opengis.gml.v_3_2_1.MultiCurveType;
import net.opengis.gml.v_3_2_1.MultiGeometryType;
import net.opengis.gml.v_3_2_1.MultiPointType;
import net.opengis.gml.v_3_2_1.MultiSurfaceType;
import net.opengis.gml.v_3_2_1.ObjectFactory;
import net.opengis.gml.v_3_2_1.PointPropertyType;
import net.opengis.gml.v_3_2_1.PointType;
import net.opengis.gml.v_3_2_1.PolygonType;
import net.opengis.gml.v_3_2_1.RingType;
import net.opengis.gml.v_3_2_1.SurfacePropertyType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.gml2.GMLWriter;

public class Wfs20JTStoGML321Converter {

  private static final ObjectFactory GML320_OBJECT_FACTORY = new ObjectFactory();

  public static DirectPositionType convertToDirectPositionType(
      Coordinate coordinate, String srsName) {
    DirectPositionType directPositionType = GML320_OBJECT_FACTORY.createDirectPositionType();
    directPositionType.getValue().add(Double.valueOf(coordinate.x));
    directPositionType.getValue().add(Double.valueOf(coordinate.y));
    directPositionType.setSrsName(srsName);

    if (!Double.isNaN(coordinate.z)) {
      directPositionType.getValue().add(Double.valueOf(coordinate.z));
    }
    return directPositionType;
  }

  public static PointType convertToPointType(Point point, String srsName) {
    PointType pointType = GML320_OBJECT_FACTORY.createPointType();
    pointType.setPos(convertToDirectPositionType(point.getCoordinate(), srsName));
    pointType.setSrsName(srsName);
    return pointType;
  }

  public static JAXBElement<PointType> convertPointTypeToJAXB(PointType pointType) {
    return GML320_OBJECT_FACTORY.createPoint(pointType);
  }

  public static LineStringType convertToLineStringType(LineString line, String srsName) {
    LineStringType lineStringType = GML320_OBJECT_FACTORY.createLineStringType();
    CoordinatesType coordinatesType = GML320_OBJECT_FACTORY.createCoordinatesType();
    StringBuilder stringBuffer = new StringBuilder();
    for (int i = 0; i < line.getCoordinateSequence().size(); i++) {
      Coordinate coordinate = line.getCoordinateSequence().getCoordinate(i);
      if (i != 0) {
        stringBuffer.append(" ");
      }
      stringBuffer.append(coordinate.x).append(",").append(coordinate.y);
      if (!Double.isNaN(coordinate.z)) {
        stringBuffer.append(",").append(coordinate.z);
      }
    }
    coordinatesType.setValue(stringBuffer.toString());
    lineStringType.setCoordinates(coordinatesType);
    lineStringType.setSrsName(srsName);
    return lineStringType;
  }

  public static JAXBElement<LineStringType> convertLineStringTypeToJAXB(
      LineStringType lineStringType) {
    return GML320_OBJECT_FACTORY.createLineString(lineStringType);
  }

  public static PolygonType convertToPolygonType(Polygon polygon, String srsName) {
    PolygonType polygonType = GML320_OBJECT_FACTORY.createPolygonType();

    // exterior
    LineString lineString = polygon.getExteriorRing();
    LinearRing linearRing =
        lineString.getFactory().createLinearRing(lineString.getCoordinateSequence());
    RingType ringType = convertToRingType(linearRing, srsName);
    JAXBElement<RingType> ringTypeJAXBElement = GML320_OBJECT_FACTORY.createRing(ringType);
    AbstractRingPropertyType abstractRingPropertyType =
        GML320_OBJECT_FACTORY.createAbstractRingPropertyType();
    abstractRingPropertyType.setAbstractRing(ringTypeJAXBElement);
    polygonType.setExterior(abstractRingPropertyType);

    // interiors
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      LineString interiorRingN = polygon.getInteriorRingN(i);
      LinearRing linearRing1 =
          interiorRingN.getFactory().createLinearRing(interiorRingN.getCoordinateSequence());
      RingType ringType1 = convertToRingType(linearRing1, srsName);
      JAXBElement<RingType> ringTypeJAXBElement1 = GML320_OBJECT_FACTORY.createRing(ringType1);
      AbstractRingPropertyType abstractRingPropertyType1 =
          GML320_OBJECT_FACTORY.createAbstractRingPropertyType();
      abstractRingPropertyType1.setAbstractRing(ringTypeJAXBElement1);
      polygonType.getInterior().add(abstractRingPropertyType1);
    }
    polygonType.setSrsName(srsName);
    return polygonType;
  }

  public static JAXBElement<PolygonType> convertPolygonTypeToJAXB(PolygonType polygonType) {
    return GML320_OBJECT_FACTORY.createPolygon(polygonType);
  }

  public static JAXBElement<MultiPointType> convertMultiPointTypeToJAXB(
      MultiPointType multiPointType) {
    return GML320_OBJECT_FACTORY.createMultiPoint(multiPointType);
  }

  public static MultiPointType convertToMultiPointType(MultiPoint multiPoint, String srsName) {
    MultiPointType multiPointType = GML320_OBJECT_FACTORY.createMultiPointType();
    for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
      Point point = (Point) multiPoint.getGeometryN(i);
      PointPropertyType pointPropertyType = GML320_OBJECT_FACTORY.createPointPropertyType();
      pointPropertyType.setPoint(convertToPointType(point, srsName));
      multiPointType.getPointMember().add(pointPropertyType);
    }
    multiPointType.setSrsName(srsName);
    return multiPointType;
  }

  public static RingType convertToRingType(LinearRing line, String srsName) {
    RingType ringType = GML320_OBJECT_FACTORY.createRingType();
    CurvePropertyType curvePropertyType = GML320_OBJECT_FACTORY.createCurvePropertyType();
    LineStringType curve = convertToLineStringType(line, srsName);
    JAXBElement<LineStringType> lineStringTypeJAXBElement =
        GML320_OBJECT_FACTORY.createLineString(curve);
    curvePropertyType.setAbstractCurve(lineStringTypeJAXBElement);
    ringType.getCurveMember().add(curvePropertyType);
    return ringType;
  }

  /**
   * Converts a @link org.locationtech.jts.geom.MultiPolygon to a @link
   * net.opengis.gml.v_3_2_1.MultiSurfaceType Note: MultiPolygon maps to gml MultiSurfaceType
   *
   * @param multiPolygon
   * @return MultiSurfaceType
   */
  public static MultiSurfaceType convertToMultiSurfaceType(
      MultiPolygon multiPolygon, String srsName) {
    MultiSurfaceType multiSurfaceType = GML320_OBJECT_FACTORY.createMultiSurfaceType();
    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      Polygon poly = (Polygon) multiPolygon.getGeometryN(i);
      PolygonType polygonType = convertToPolygonType(poly, srsName);
      JAXBElement<PolygonType> polygonTypeJAXBElement =
          GML320_OBJECT_FACTORY.createPolygon(polygonType);
      SurfacePropertyType surfacePropertyType = GML320_OBJECT_FACTORY.createSurfacePropertyType();
      surfacePropertyType.setAbstractSurface(polygonTypeJAXBElement);
      multiSurfaceType.getSurfaceMember().add(surfacePropertyType);
    }
    multiSurfaceType.setSrsName(srsName);
    return multiSurfaceType;
  }

  public static JAXBElement<MultiSurfaceType> convertMultiSurfaceTypeToJAXB(
      MultiSurfaceType multiSurfaceType) {
    return GML320_OBJECT_FACTORY.createMultiSurface(multiSurfaceType);
  }

  public static MultiGeometryType convertToMultiGeometryType(
      GeometryCollection multiGeometry, String srsName) {
    final MultiGeometryType multiGeometryType = GML320_OBJECT_FACTORY.createMultiGeometryType();

    for (int index = 0; index < multiGeometry.getNumGeometries(); index++) {
      final Geometry geometry = multiGeometry.getGeometryN(index);
      multiGeometryType.getGeometryMember().add(createGeometryPropertyType(geometry, srsName));
    }
    return multiGeometryType;
  }

  public static JAXBElement<MultiGeometryType> convertMultiGeometryTypeToJAXB(
      MultiGeometryType multiGeometryType) {
    return GML320_OBJECT_FACTORY.createMultiGeometry(multiGeometryType);
  }

  private static GeometryPropertyType createGeometryPropertyType(
      Geometry geometry, String srsName) {
    final GeometryPropertyType geometryPropertyType =
        GML320_OBJECT_FACTORY.createGeometryPropertyType();
    if (geometry instanceof Point) {
      PointType pointType = convertToPointType((Point) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertPointTypeToJAXB(pointType));
    } else if (geometry instanceof LineString) {
      LineStringType lineStringType = convertToLineStringType((LineString) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertLineStringTypeToJAXB(lineStringType));
    } else if (geometry instanceof Polygon) {
      PolygonType polygonType = convertToPolygonType((Polygon) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertPolygonTypeToJAXB(polygonType));
    } else if (geometry instanceof MultiPoint) {
      MultiPointType multiPointType = convertToMultiPointType((MultiPoint) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertMultiPointTypeToJAXB(multiPointType));
    } else if (geometry instanceof MultiLineString) {
      MultiCurveType multiCurveType =
          convertToMultiLineStringType((MultiLineString) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertMultiCurveTypeToJAXB(multiCurveType));
    } else if (geometry instanceof MultiPolygon) {
      MultiSurfaceType multiSurfaceType =
          convertToMultiSurfaceType((MultiPolygon) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertMultiSurfaceTypeToJAXB(multiSurfaceType));
    } else if (geometry instanceof GeometryCollection) {
      MultiGeometryType multiGeometryType =
          convertToMultiGeometryType((GeometryCollection) geometry, srsName);
      geometryPropertyType.setAbstractGeometry(convertMultiGeometryTypeToJAXB(multiGeometryType));
    } else {
      throw new IllegalArgumentException();
    }
    return geometryPropertyType;
  }

  public static DirectPositionType convertCoordinateToDirectPositionType(Coordinate coordinate) {
    final DirectPositionType directPosition = GML320_OBJECT_FACTORY.createDirectPositionType();

    directPosition.getValue().add(coordinate.x);
    directPosition.getValue().add(coordinate.y);
    if (!Double.isNaN(coordinate.z)) {
      directPosition.getValue().add(coordinate.z);
    }
    return directPosition;
  }

  public static DirectPositionType[] convertCoordinates(Coordinate[] coordinates) {
    if (coordinates == null) {
      return null;
    } else {
      final DirectPositionType[] directPositions = new DirectPositionType[coordinates.length];
      for (int index = 0; index < coordinates.length; index++) {

        directPositions[index] = convertCoordinateToDirectPositionType(coordinates[index]);
      }
      return directPositions;
    }
  }

  // MultiLineStringType maps to MultiCurveType in opengis API
  public static JAXBElement<MultiCurveType> convertMultiCurveTypeToJAXB(
      MultiCurveType multiCurveType) {
    return GML320_OBJECT_FACTORY.createMultiCurve(multiCurveType);
  }

  // MultiLineStringType maps to MultiCurveType in opengis API
  public static MultiCurveType convertToMultiLineStringType(
      MultiLineString multiLineString, String srsName) {
    final MultiCurveType multiCurveType = GML320_OBJECT_FACTORY.createMultiCurveType();
    for (int index = 0; index < multiLineString.getNumGeometries(); index++) {
      final LineString lineString = (LineString) multiLineString.getGeometryN(index);
      multiCurveType.getCurveMember().add(createCurvePropertyType(lineString));
    }
    multiCurveType.setSrsName(srsName);
    return multiCurveType;
  }

  private static CurvePropertyType createCurvePropertyType(LineString lineString) {
    final CurvePropertyType curvePropertyType = GML320_OBJECT_FACTORY.createCurvePropertyType();
    curvePropertyType.setAbstractCurve(createElementJAXB(lineString));
    return curvePropertyType;
  }

  private static JAXBElement<LineStringType> createElementJAXB(LineString lineString) {
    return GML320_OBJECT_FACTORY.createLineString(convertGeometryType(lineString));
  }

  private static LineStringType convertGeometryType(LineString lineString) {

    final LineStringType resultLineString = GML320_OBJECT_FACTORY.createLineStringType();

    for (DirectPositionType directPosition : convertCoordinates(lineString.getCoordinates())) {
      final JAXBElement<DirectPositionType> pos = GML320_OBJECT_FACTORY.createPos(directPosition);
      resultLineString.getPosOrPointPropertyOrPointRep().add(pos);
    }
    return resultLineString;
  }

  public static String convertGeometryToGML(Geometry geometry) throws JAXBException {
    GMLWriter gmlWriter = new GMLWriter(true);
    String gml = gmlWriter.write(geometry);
    return gml;
  }
}
