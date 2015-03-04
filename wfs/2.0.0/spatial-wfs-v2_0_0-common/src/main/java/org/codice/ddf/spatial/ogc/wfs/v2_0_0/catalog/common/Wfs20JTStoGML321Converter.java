/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.gml2.GMLWriter;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Wfs20JTStoGML321Converter {

    private static final ObjectFactory gml320ObjectFactory = new ObjectFactory();

    public static DirectPositionType convertToDirectPositionType(Coordinate coordinate, String srsName) {
        DirectPositionType directPositionType = gml320ObjectFactory.createDirectPositionType();
        directPositionType.getValue().add(new Double(coordinate.x));
        directPositionType.getValue().add(new Double(coordinate.y));
        directPositionType.setSrsName(srsName);

        if (!Double.isNaN(coordinate.z)) {
            directPositionType.getValue().add(new Double(coordinate.z));
        }
        return directPositionType;
    }

    public static PointType convertToPointType(Point point, String srsName) {
        PointType pointType = gml320ObjectFactory.createPointType();
        pointType.setPos(convertToDirectPositionType(point.getCoordinate(), srsName));
        pointType.setSrsName(srsName);
        return pointType;
    }

    public static JAXBElement<PointType> convertPointTypeToJAXB(PointType pointType) {
        return gml320ObjectFactory.createPoint(pointType);
    }

    public static LineStringType convertToLineStringType(LineString line, String srsName) {
        LineStringType lineStringType = gml320ObjectFactory.createLineStringType();
        CoordinatesType coordinatesType = gml320ObjectFactory.createCoordinatesType();
        StringBuffer stringBuffer = new StringBuffer();
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
        return gml320ObjectFactory.createLineString(lineStringType);
    }

    public static PolygonType convertToPolygonType(Polygon polygon, String srsName) {
        PolygonType polygonType = gml320ObjectFactory.createPolygonType();

        //exterior
        LineString lineString = polygon.getExteriorRing();
        LinearRing linearRing = lineString.getFactory()
                .createLinearRing(lineString.getCoordinateSequence());
        RingType ringType = convertToRingType(linearRing, srsName);
        JAXBElement<RingType> ringTypeJAXBElement = gml320ObjectFactory.createRing(ringType);
        AbstractRingPropertyType abstractRingPropertyType = gml320ObjectFactory
                .createAbstractRingPropertyType();
        abstractRingPropertyType.setAbstractRing(ringTypeJAXBElement);
        polygonType.setExterior(abstractRingPropertyType);

        //interiors
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            LineString interiorRingN = polygon.getInteriorRingN(i);
            LinearRing linearRing1 = interiorRingN.getFactory()
                    .createLinearRing(interiorRingN.getCoordinateSequence());
            RingType ringType1 = convertToRingType(linearRing1, srsName);
            JAXBElement<RingType> ringTypeJAXBElement1 = gml320ObjectFactory.createRing(ringType1);
            AbstractRingPropertyType abstractRingPropertyType1 = gml320ObjectFactory
                    .createAbstractRingPropertyType();
            abstractRingPropertyType1.setAbstractRing(ringTypeJAXBElement1);
            polygonType.getInterior().add(abstractRingPropertyType1);
        }
        polygonType.setSrsName(srsName);
        return polygonType;
    }

    public static JAXBElement<PolygonType> convertPolygonTypeToJAXB(PolygonType polygonType) {
        return gml320ObjectFactory.createPolygon(polygonType);
    }

    public static JAXBElement<MultiPointType> convertMultiPointTypeToJAXB(
            MultiPointType multiPointType) {
        return gml320ObjectFactory.createMultiPoint(multiPointType);
    }

    public static MultiPointType convertToMultiPointType(MultiPoint multiPoint, String srsName) {
        MultiPointType multiPointType = gml320ObjectFactory.createMultiPointType();
        for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
            Point point = (Point) multiPoint.getGeometryN(i);
            PointPropertyType pointPropertyType = gml320ObjectFactory.createPointPropertyType();
            pointPropertyType.setPoint(convertToPointType(point, srsName));
            multiPointType.getPointMember().add(pointPropertyType);
        }
        multiPointType.setSrsName(srsName);
        return multiPointType;
    }

    public static RingType convertToRingType(LinearRing line, String srsName) {
        RingType ringType = gml320ObjectFactory.createRingType();
        CurvePropertyType curvePropertyType = gml320ObjectFactory.createCurvePropertyType();
        LineStringType curve = convertToLineStringType(line, srsName);
        JAXBElement<LineStringType> lineStringTypeJAXBElement = gml320ObjectFactory
                .createLineString(curve);
        curvePropertyType.setAbstractCurve(lineStringTypeJAXBElement);
        ringType.getCurveMember().add(curvePropertyType);
        return ringType;
    }

    /**
     * Converts a @link com.vividsolutions.jts.geom.MultiPolygon to a @link net.opengis.gml.v_3_2_1.MultiSurfaceType
     * Note:  MultiPolygon maps to gml MultiSurfaceType
     *
     * @param multiPolygon
     * @return MultiSurfaceType
     */
    public static MultiSurfaceType convertToMultiSurfaceType(MultiPolygon multiPolygon, String srsName) {
        MultiSurfaceType multiSurfaceType = gml320ObjectFactory.createMultiSurfaceType();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            Polygon poly = (Polygon) multiPolygon.getGeometryN(i);
            PolygonType polygonType = convertToPolygonType(poly, srsName);
            JAXBElement<PolygonType> polygonTypeJAXBElement = gml320ObjectFactory
                    .createPolygon(polygonType);
            SurfacePropertyType surfacePropertyType = gml320ObjectFactory
                    .createSurfacePropertyType();
            surfacePropertyType.setAbstractSurface(polygonTypeJAXBElement);
            multiSurfaceType.getSurfaceMember().add(surfacePropertyType);
        }
        multiSurfaceType.setSrsName(srsName);
        return multiSurfaceType;
    }

    public static JAXBElement<MultiSurfaceType> convertMultiSurfaceTypeToJAXB(
            MultiSurfaceType multiSurfaceType) {
        return gml320ObjectFactory.createMultiSurface(multiSurfaceType);
    }

    public static MultiGeometryType convertToMultiGeometryType(GeometryCollection multiGeometry,
            String srsName) {
        final MultiGeometryType multiGeometryType = gml320ObjectFactory.createMultiGeometryType();

        for (int index = 0; index < multiGeometry.getNumGeometries(); index++) {
            final Geometry geometry = multiGeometry.getGeometryN(index);
            multiGeometryType.getGeometryMember().add(createGeometryPropertyType(geometry, srsName));
        }
        return multiGeometryType;
    }

    public static JAXBElement<MultiGeometryType> convertMultiGeometryTypeToJAXB(MultiGeometryType multiGeometryType) {
        return gml320ObjectFactory.createMultiGeometry(multiGeometryType);
    }

    private static GeometryPropertyType createGeometryPropertyType(Geometry geometry, String srsName) {
        final GeometryPropertyType geometryPropertyType = gml320ObjectFactory
                .createGeometryPropertyType();
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
            MultiCurveType multiCurveType = convertToMultiLineStringType((MultiLineString) geometry,
                    srsName);
            geometryPropertyType.setAbstractGeometry(convertMultiCurveTypeToJAXB(multiCurveType));
        } else if (geometry instanceof MultiPolygon) {
            MultiSurfaceType multiSurfaceType = convertToMultiSurfaceType((MultiPolygon) geometry,
                    srsName);
            geometryPropertyType
                    .setAbstractGeometry(convertMultiSurfaceTypeToJAXB(multiSurfaceType));
        } else if (geometry instanceof GeometryCollection) {
            MultiGeometryType multiGeometryType = convertToMultiGeometryType((GeometryCollection) geometry, srsName);
            geometryPropertyType
                    .setAbstractGeometry(convertMultiGeometryTypeToJAXB(multiGeometryType));
        } else {
            throw new IllegalArgumentException();
        }
        return geometryPropertyType;
    }

    public static DirectPositionType convertCoordinateToDirectPositionType(Coordinate coordinate) {
        final DirectPositionType directPosition = gml320ObjectFactory.createDirectPositionType();

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
        return gml320ObjectFactory.createMultiCurve(multiCurveType);
    }

    // MultiLineStringType maps to MultiCurveType in opengis API
    public static MultiCurveType convertToMultiLineStringType(MultiLineString multiLineString,
            String srsName) {
        final MultiCurveType multiCurveType = gml320ObjectFactory.createMultiCurveType();
        for (int index = 0; index < multiLineString.getNumGeometries(); index++) {
            final LineString lineString = (LineString) multiLineString.getGeometryN(index);
            multiCurveType.getCurveMember().add(createCurvePropertyType(lineString));
        }
        multiCurveType.setSrsName(srsName);
        return multiCurveType;
    }

    private static CurvePropertyType createCurvePropertyType(LineString lineString) {
        final CurvePropertyType curvePropertyType = gml320ObjectFactory.createCurvePropertyType();
        curvePropertyType.setAbstractCurve(createElementJAXB(lineString));
        return curvePropertyType;
    }

    private static JAXBElement<LineStringType> createElementJAXB(LineString lineString) {
        return gml320ObjectFactory.createLineString(convertGeometryType(lineString));
    }

    private static LineStringType convertGeometryType(LineString lineString) {

        final LineStringType resultLineString = gml320ObjectFactory.createLineStringType();

        for (DirectPositionType directPosition : convertCoordinates(lineString.getCoordinates())) {
            final JAXBElement<DirectPositionType> pos = gml320ObjectFactory
                    .createPos(directPosition);
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
