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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import ogc.schema.opengis.gml.v_2_1_2.AbstractGeometryType;
import ogc.schema.opengis.gml.v_2_1_2.GeometryAssociationType;
import ogc.schema.opengis.gml.v_2_1_2.GeometryCollectionType;
import ogc.schema.opengis.gml.v_2_1_2.LineStringMemberType;
import ogc.schema.opengis.gml.v_2_1_2.LineStringType;
import ogc.schema.opengis.gml.v_2_1_2.LinearRingMemberType;
import ogc.schema.opengis.gml.v_2_1_2.LinearRingType;
import ogc.schema.opengis.gml.v_2_1_2.MultiLineStringType;
import ogc.schema.opengis.gml.v_2_1_2.MultiPointType;
import ogc.schema.opengis.gml.v_2_1_2.MultiPolygonType;
import ogc.schema.opengis.gml.v_2_1_2.PointMemberType;
import ogc.schema.opengis.gml.v_2_1_2.PointType;
import ogc.schema.opengis.gml.v_2_1_2.PolygonMemberType;
import ogc.schema.opengis.gml.v_2_1_2.PolygonType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.xml.sax.SAXException;

public class Wfs10JTStoGML200ConverterTest {

  private static final String XMLUNIT_IDENTICAL = "XML Identical";

  private static final String XMLUNIT_SIMILAR = "XML Similar";

  private static final String POLYGON = "POLYGON ((30 -10, 30 30, 10 30, 10 -10, 30 -10))";

  private static final String POLYGON_COORDS =
      "30.0,-10.0 30.0,30.0 10.0,30.0 10.0,-10.0 30.0,-10.0";

  private static final String POLYGON_GML =
      "<gml:Polygon xmlns:gml='http://www.opengis.net/gml'>"
          + "<gml:outerBoundaryIs><gml:LinearRing>"
          + "<gml:coordinates>30.0,-10.0 30.0,30.0 10.0,30.0 10.0,-10.0 30.0,-10.0</gml:coordinates>"
          + "</gml:LinearRing></gml:outerBoundaryIs></gml:Polygon>";

  private static final String LINESTRING = "LINESTRING (30 -10, 30 30, 10 30, 10 -10)";

  private static final String LINESTRING_COORDS = "30.0,-10.0 30.0,30.0 10.0,30.0 10.0,-10.0";

  private static final String LINESTRING_GML =
      "<gml:LineString xmlns:gml='http://www.opengis.net/gml'><gml:coordinates>30.0,-10.0 30.0,30.0 10.0,30.0 10.0,-10.0</gml:coordinates></gml:LineString>";

  private static final String POINT = "POINT (30 -10)";

  private static final String POINT_COORDS = "30.0,-10.0";

  private static final String POINT_GML =
      "<gml:Point xmlns:gml='http://www.opengis.net/gml'><gml:coordinates>30.0,-10.0</gml:coordinates></gml:Point>";

  private static final String MULTIPOINT = "MULTIPOINT (10 40, 40 30, 20 20, 30 10)";

  private static final String MULTIPOINT_COORDS1 = "10.0,40.0";

  private static final String MULTIPOINT_COORDS2 = "40.0,30.0";

  private static final String MULTIPOINT_COORDS3 = "20.0,20.0";

  private static final String MULTIPOINT_COORDS4 = "30.0,10.0";

  private static final String MULTIPOINT_GML =
      "<gml:MultiPoint xmlns:gml='http://www.opengis.net/gml'><gml:pointMember><gml:Point>"
          + "<gml:coordinates>10.0,40.0</gml:coordinates></gml:Point></gml:pointMember><gml:pointMember><gml:Point>"
          + "<gml:coordinates>40.0,30.0</gml:coordinates></gml:Point></gml:pointMember><gml:pointMember><gml:Point>"
          + "<gml:coordinates>20.0,20.0</gml:coordinates></gml:Point></gml:pointMember><gml:pointMember><gml:Point>"
          + "<gml:coordinates>30.0,10.0</gml:coordinates></gml:Point></gml:pointMember></gml:MultiPoint>";

  private static final String MULTILINESTRING =
      "MULTILINESTRING ((10 10, 20 20, 10 40),(40 40, 30 30, 40 20, 30 10))";

  private static final String MULTILINESTRING_GML =
      "<gml:MultiLineString xmlns:gml='http://www.opengis.net/gml'><gml:lineStringMember>"
          + "<gml:LineString><gml:coordinates>10.0,10.0 20.0,20.0 10.0,40.0</gml:coordinates></gml:LineString></gml:lineStringMember>"
          + "<gml:lineStringMember><gml:LineString><gml:coordinates>40.0,40.0 30.0,30.0 40.0,20.0 30.0,10.0</gml:coordinates></gml:LineString>"
          + "</gml:lineStringMember></gml:MultiLineString>";

  private static final String MULTILINESTRING_COORD1 = "10.0,10.0 20.0,20.0 10.0,40.0";

  private static final String MULTILINESTRING_COORD2 = "40.0,40.0 30.0,30.0 40.0,20.0 30.0,10.0";

  private static final String MULTIPOLYGON =
      "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)),((20 35, 10 30, 10 10, 30 5, 45 20, 20 35),(30 20, 20 15, 20 25, 30 20)))";

  private static final String MULTIPOLYGON_COORDS1 = "40.0,40.0 20.0,45.0 45.0,30.0 40.0,40.0";

  private static final String MULTIPOLYGON_COORDS2 = "40.0,40.0 20.0,45.0 45.0,30.0 40.0,40.0";

  private static final String MULTIPOLYGON_GML =
      "<gml:MultiPolygon xmlns:gml='http://www.opengis.net/gml'><gml:polygonMember>"
          + "<gml:Polygon><gml:outerBoundaryIs><gml:LinearRing><gml:coordinates>40.0,40.0 20.0,45.0 45.0,30.0 40.0,40.0</gml:coordinates>"
          + "</gml:LinearRing></gml:outerBoundaryIs></gml:Polygon></gml:polygonMember><gml:polygonMember><gml:Polygon><gml:outerBoundaryIs>"
          + "<gml:LinearRing><gml:coordinates>20.0,35.0 10.0,30.0 10.0,10.0 30.0,5.0 45.0,20.0 20.0,35.0</gml:coordinates></gml:LinearRing>"
          + "</gml:outerBoundaryIs><gml:innerBoundaryIs><gml:LinearRing><gml:coordinates>30.0,20.0 20.0,15.0 20.0,25.0 30.0,20.0</gml:coordinates>"
          + "</gml:LinearRing></gml:innerBoundaryIs></gml:Polygon></gml:polygonMember></gml:MultiPolygon>";

  private static final String GEOMETRYCOLLECTION =
      "GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))";

  private static final String GEOMETRYCOLLECTION_POINT_COORD = "4.0,6.0";

  private static final String GEOMETRYCOLLECTION_LINESTRING_COORD = "4.0,6.0 7.0,10.0";

  private static final String GEOMETRYCOLLECTION_GML =
      "<gml:MultiGeometry xmlns:gml='http://www.opengis.net/gml'><gml:geometryMember>"
          + "<gml:Point xmlns:gml='http://www.opengis.net/gml'><gml:coordinates>4.0,6.0</gml:coordinates></gml:Point></gml:geometryMember>"
          + "<gml:geometryMember><gml:LineString xmlns:gml='http://www.opengis.net/gml'><gml:coordinates>4.0,6.0 7.0,10.0</gml:coordinates>"
          + "</gml:LineString></gml:geometryMember></gml:MultiGeometry>";

  private static final String GEOMETRYCOLLECTION_NS1 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<ns1:_GeometryCollection xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns1=\"http://www.opengis.net/gml\">"
          + "    <ns1:geometryMember>        <ns1:Point>            <ns1:coordinates>        4.0,6.0       </ns1:coordinates>"
          + "        </ns1:Point>    </ns1:geometryMember>    <ns1:geometryMember>        <ns1:LineString>            "
          + "<ns1:coordinates>        4.0,6.0 7.0,10.0       </ns1:coordinates>        </ns1:LineString>    "
          + "</ns1:geometryMember></ns1:_GeometryCollection>";

  private StringWriter writer = null;

  @BeforeClass
  public static void setupTestClass() throws JAXBException, ParseException {
    XMLUnit.setIgnoreWhitespace(Boolean.TRUE);
    XMLUnit.setNormalizeWhitespace(Boolean.TRUE);
  }

  @Before
  public void preTest() throws JAXBException {
    writer = new StringWriter();
  }

  @Test
  public void testGeometryToPolygonGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    Polygon polygon = (Polygon) getGeometryFromWkt(POLYGON);
    assertThat(polygon == null, is(Boolean.FALSE));

    String polygonGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(polygon).replaceAll("\n", "");
    assertThat(StringUtils.isEmpty(polygonGML), is(Boolean.FALSE));

    Diff diff = XMLUnit.compareXML(polygonGML, POLYGON_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
  }

  @Test
  public void testGMLToPolygonType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    Polygon polygon = (Polygon) getGeometryFromWkt(POLYGON);
    assertThat(polygon == null, is(Boolean.FALSE));

    String polygonGML = Wfs10JTStoGML200Converter.convertGeometryToGML(polygon);
    PolygonType polygonType =
        (PolygonType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(polygonGML, Wfs10Constants.POLYGON);
    assertThat(null != polygonType, is(Boolean.TRUE));
    assertThat(polygonType.isSetInnerBoundaryIs(), is(Boolean.FALSE));
    assertThat(polygonType.isSetOuterBoundaryIs(), is(Boolean.TRUE));
    LinearRingMemberType linearRingMemberType = polygonType.getOuterBoundaryIs();
    JAXBElement<? extends AbstractGeometryType> geometry = linearRingMemberType.getGeometry();
    LinearRingType linearRingType = (LinearRingType) geometry.getValue();
    String coordinates = linearRingType.getCoordinates().getValue().replaceAll("\n", "").trim();
    assertThat(POLYGON_COORDS.equals(coordinates), is(Boolean.TRUE));
  }

  @Test
  public void testPolygonTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    Polygon polygon = (Polygon) getGeometryFromWkt(POLYGON);
    assertThat(polygon == null, is(Boolean.FALSE));
    String polygonGML = Wfs10JTStoGML200Converter.convertGeometryToGML(polygon);
    PolygonType polygonType =
        (PolygonType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(polygonGML, Wfs10Constants.POLYGON);
    assertThat(null != polygonType, is(Boolean.TRUE));
    JAXBElement<PolygonType> polygonTypeJAXBElement =
        (JAXBElement<PolygonType>) Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(polygonType);
    assertThat(
        polygonTypeJAXBElement
            .getName()
            .getLocalPart()
            .equals(Wfs10Constants.POLYGON.getLocalPart()),
        is(Boolean.TRUE));
    assertThat(polygonTypeJAXBElement.getDeclaredType() == PolygonType.class, is(Boolean.TRUE));

    JAXB.marshal(polygonTypeJAXBElement, writer);
    String xml = writer.toString();
    Diff diff = XMLUnit.compareXML(xml, POLYGON_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.FALSE));
  }

  @Test
  public void testGeometryToLineStringGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    LineString lineString = (LineString) getGeometryFromWkt(LINESTRING);
    assertThat(lineString == null, is(Boolean.FALSE));
    String lineStringGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(lineString).replaceAll("\n", "").trim();

    assertThat(StringUtils.isEmpty(lineStringGML), is(Boolean.FALSE));
    Diff diff = XMLUnit.compareXML(lineStringGML, LINESTRING_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
  }

  @Test
  public void testGMLToLineStringType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    LineString lineString = (LineString) getGeometryFromWkt(LINESTRING);
    assertThat(lineString == null, is(Boolean.FALSE));
    String lineStringGML = Wfs10JTStoGML200Converter.convertGeometryToGML(lineString);
    LineStringType lineStringType =
        (LineStringType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                lineStringGML, Wfs10Constants.LINESTRING);

    assertThat(
        Wfs10Constants.LINESTRING
            .getLocalPart()
            .equals(lineStringType.getJAXBElementName().getLocalPart()),
        is(Boolean.TRUE));
    String coords = lineStringType.getCoordinates().getValue().replaceAll("\n", "").trim();
    assertThat(coords.isEmpty(), is(Boolean.FALSE));
    assertThat(LINESTRING_COORDS.equals(coords), is(Boolean.TRUE));
  }

  @Test
  public void testLineStringTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    LineString lineString = (LineString) getGeometryFromWkt(LINESTRING);
    assertThat(lineString == null, is(Boolean.FALSE));
    String lineStringGML = Wfs10JTStoGML200Converter.convertGeometryToGML(lineString);
    LineStringType lineStringType =
        (LineStringType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                lineStringGML, Wfs10Constants.LINESTRING);
    JAXBElement<LineStringType> lineStringTypeJAXBElement =
        (JAXBElement<LineStringType>)
            Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(lineStringType);
    XMLUnit.setNormalizeWhitespace(Boolean.TRUE);
    JAXB.marshal(lineStringTypeJAXBElement, writer);
    String xml = writer.toString();
    Diff diff = XMLUnit.compareXML(xml, LINESTRING_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.FALSE));
  }

  @Test
  public void testGeometryToPointGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    Point point = (Point) getGeometryFromWkt(POINT);
    assertThat(point == null, is(Boolean.FALSE));
    String pointGML = Wfs10JTStoGML200Converter.convertGeometryToGML(point).replaceAll("\n", "");

    assertThat(StringUtils.isEmpty(pointGML), is(Boolean.FALSE));
    Diff diff = XMLUnit.compareXML(pointGML, POINT_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
  }

  @Test
  public void testGMLToPointType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    String pointGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(POINT))
            .replaceAll("\n", "");
    PointType pointType =
        (PointType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(pointGML, Wfs10Constants.POINT);
    assertThat(
        Wfs10Constants.POINT.getLocalPart().equals(pointType.getJAXBElementName().getLocalPart()),
        is(Boolean.TRUE));
    String coords = pointType.getCoordinates().getValue().replaceAll("\n", "").trim();
    assertThat(POINT_COORDS.equals(coords), is(Boolean.TRUE));
  }

  @Test
  public void testPointTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    String pointGML = Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(POINT));
    PointType pointType =
        (PointType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(pointGML, Wfs10Constants.POINT);
    JAXBElement<PointType> pointTypeJAXBElement =
        (JAXBElement<PointType>) Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(pointType);

    JAXB.marshal(pointTypeJAXBElement, writer);
    String xml = writer.toString();
    Diff diff = XMLUnit.compareXML(xml, POINT_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.FALSE));
  }

  @Test
  public void testGeometryToMultiPointGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    MultiPoint multiPoint = (MultiPoint) getGeometryFromWkt(MULTIPOINT);
    assertThat(multiPoint == null, is(Boolean.FALSE));
    String multiPointGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(multiPoint).replaceAll("\n", "");

    assertThat(StringUtils.isEmpty(multiPointGML), is(Boolean.FALSE));
    Diff diff = XMLUnit.compareXML(multiPointGML, MULTIPOINT_GML);
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
  }

  @Test
  public void testGMLToMultiPointType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    String multiPointGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(MULTIPOINT));
    MultiPointType multiPointType =
        (MultiPointType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                multiPointGML, Wfs10Constants.MULTI_POINT);
    List<JAXBElement<? extends GeometryAssociationType>> geoMembers =
        multiPointType.getGeometryMember();
    assertThat(geoMembers.isEmpty(), is(Boolean.FALSE));
    assertThat(
        Wfs10Constants.MULTI_POINT
            .getLocalPart()
            .equals(multiPointType.getJAXBElementName().getLocalPart()),
        is(Boolean.TRUE));
    assertThat(geoMembers.size() == 4, is(Boolean.TRUE));

    String coords1 = extractPointMemberTypeCoordinates(geoMembers.get(0));
    assertThat(MULTIPOINT_COORDS1.equals(coords1), is(Boolean.TRUE));

    String coords2 = extractPointMemberTypeCoordinates(geoMembers.get(1));
    assertThat(MULTIPOINT_COORDS2.equals(coords2), is(Boolean.TRUE));

    String coords3 = extractPointMemberTypeCoordinates(geoMembers.get(2));
    assertThat(MULTIPOINT_COORDS3.equals(coords3), is(Boolean.TRUE));

    String coords4 = extractPointMemberTypeCoordinates(geoMembers.get(3));
    assertThat(MULTIPOINT_COORDS4.equals(coords4), is(Boolean.TRUE));
  }

  private String extractPointMemberTypeCoordinates(
      JAXBElement<? extends GeometryAssociationType> jaxbElement1)
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    assertThat(
        Wfs10Constants.POINT_MEMBER.getLocalPart().equals(jaxbElement1.getName().getLocalPart()),
        is(Boolean.TRUE));
    PointMemberType pointMemberType1 = (PointMemberType) jaxbElement1.getValue();
    JAXBElement<? extends AbstractGeometryType> geometry1 = pointMemberType1.getGeometry();
    assertThat(
        Wfs10Constants.POINT.getLocalPart().equals(geometry1.getName().getLocalPart()),
        is(Boolean.TRUE));
    return ((PointType) geometry1.getValue())
        .getCoordinates()
        .getValue()
        .replaceAll("\n", "")
        .trim();
  }

  @Test
  public void testMultiPointTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String multiPointGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(MULTIPOINT));
    MultiPointType multiPointType =
        (MultiPointType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                multiPointGML, Wfs10Constants.MULTI_POINT);
    JAXBElement<MultiPointType> multiPointTypeJAXBElement =
        (JAXBElement<MultiPointType>)
            Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(multiPointType);

    JAXB.marshal(multiPointTypeJAXBElement, writer);
    String xml = writer.toString();
    Diff diff = XMLUnit.compareXML(xml, MULTIPOINT_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.FALSE));
  }

  @Test
  public void testGeometryToMultiLineStringGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    MultiLineString multiLineString = (MultiLineString) getGeometryFromWkt(MULTILINESTRING);
    assertThat(multiLineString == null, is(Boolean.FALSE));
    String multiLineStringGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(multiLineString).replaceAll("\n", "");

    assertThat(StringUtils.isEmpty(multiLineStringGML), is(Boolean.FALSE));
    Diff diff = XMLUnit.compareXML(multiLineStringGML, MULTILINESTRING_GML);
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
  }

  @Test
  public void testGMLToMultiLineStringType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String multiLineStringGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(MULTILINESTRING));
    MultiLineStringType multiLineStringType =
        (MultiLineStringType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                multiLineStringGML, Wfs10Constants.MULTI_LINESTRING);
    List<JAXBElement<? extends GeometryAssociationType>> jaxbElements =
        multiLineStringType.getGeometryMember();

    assertThat(jaxbElements.isEmpty(), is(Boolean.FALSE));
    assertThat(jaxbElements.size() == 2, is(Boolean.TRUE));

    String coordinates1 = extractLineStringMemberCoordinates(jaxbElements.get(0));
    assertThat(coordinates1.isEmpty(), is(Boolean.FALSE));
    assertThat(MULTILINESTRING_COORD1.equals(coordinates1), is(Boolean.TRUE));

    String coordinates2 = extractLineStringMemberCoordinates(jaxbElements.get(1));
    assertThat(coordinates2.isEmpty(), is(Boolean.FALSE));
    assertThat(MULTILINESTRING_COORD2.equals(coordinates2), is(Boolean.TRUE));
  }

  private String extractLineStringMemberCoordinates(JAXBElement element1)
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    assertThat(
        Wfs10Constants.LINESTRING_MEMBER.getLocalPart().equals(element1.getName().getLocalPart()),
        is(Boolean.TRUE));
    LineStringMemberType lsMemberType1 = (LineStringMemberType) element1.getValue();
    JAXBElement geometry1 = lsMemberType1.getGeometry();
    LineStringType lineStringType = (LineStringType) geometry1.getValue();
    return lineStringType.getCoordinates().getValue().replaceAll("\n", "").trim();
  }

  @Test
  public void testMultiLineStringTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String multiLineString =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(MULTILINESTRING));
    MultiLineStringType multiLineStringType =
        (MultiLineStringType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                multiLineString, Wfs10Constants.MULTI_LINESTRING);
    JAXBElement<MultiLineStringType> multiLineStringTypeJAXBElement =
        (JAXBElement<MultiLineStringType>)
            Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(multiLineStringType);

    JAXB.marshal(multiLineStringTypeJAXBElement, writer);
    String xml = writer.toString();
    Diff diff = XMLUnit.compareXML(xml, MULTILINESTRING_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.FALSE));
  }

  @Test
  public void testGeometryToMultiPolygonGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    MultiPolygon multiPolygon = (MultiPolygon) getGeometryFromWkt(MULTIPOLYGON);
    assertThat(multiPolygon == null, is(Boolean.FALSE));
    String multiPolygonGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(multiPolygon).replaceAll("\n", "");

    assertThat(StringUtils.isEmpty(multiPolygonGML), is(Boolean.FALSE));
    Diff diff = XMLUnit.compareXML(multiPolygonGML, MULTIPOLYGON_GML);
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
  }

  @Test
  public void testGMLToMultiPolygonType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String multiPolygonGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(MULTIPOLYGON));
    MultiPolygonType multiPolygonType =
        (MultiPolygonType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                multiPolygonGML, Wfs10Constants.MULTI_POLYGON);
    multiPolygonType.getJAXBElementName();
    List<JAXBElement<? extends GeometryAssociationType>> geometryMembers =
        multiPolygonType.getGeometryMember();
    assertThat(geometryMembers.size() == 2, is(Boolean.TRUE));

    PolygonMemberType polygonMemberType1 = (PolygonMemberType) geometryMembers.get(0).getValue();
    String coords1 = extractPolygonMemberCoordinates(polygonMemberType1);
    assertThat(MULTIPOLYGON_COORDS1.equals(coords1), is(Boolean.TRUE));

    PolygonMemberType polygonMemberType2 = (PolygonMemberType) geometryMembers.get(0).getValue();
    String coords2 = extractPolygonMemberCoordinates(polygonMemberType2);
    assertThat(MULTIPOLYGON_COORDS2.equals(coords2), is(Boolean.TRUE));
  }

  private String extractPolygonMemberCoordinates(PolygonMemberType polygonMemberType1)
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {
    JAXBElement<? extends AbstractGeometryType> polygonGeometry1 = polygonMemberType1.getGeometry();
    assertThat(
        Wfs10Constants.POLYGON.getLocalPart().equals(polygonGeometry1.getName().getLocalPart()),
        is(Boolean.TRUE));
    PolygonType polygonType1 = (PolygonType) polygonGeometry1.getValue();
    LinearRingMemberType linearRingMemberType1 = polygonType1.getOuterBoundaryIs();
    JAXBElement<? extends AbstractGeometryType> linearRingGeometry1 =
        linearRingMemberType1.getGeometry();
    LinearRingType linearRingType1 = (LinearRingType) linearRingGeometry1.getValue();
    return linearRingType1.getCoordinates().getValue().replaceAll("\n", "").trim();
  }

  @Test
  public void testMultiPolygonTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String multiPolygonGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(MULTIPOLYGON));
    MultiPolygonType multiPolygonType =
        (MultiPolygonType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                multiPolygonGML, Wfs10Constants.MULTI_POLYGON);
    JAXBElement<MultiPolygonType> multiPolygonTypeJAXBElement =
        (JAXBElement<MultiPolygonType>)
            Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(multiPolygonType);

    JAXB.marshal(multiPolygonTypeJAXBElement, writer);
    String xml = writer.toString();
    Diff diff = XMLUnit.compareXML(xml, MULTIPOLYGON_GML);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.FALSE));
  }

  @Test
  public void testTwo() throws Exception {
    Geometry geometryFromWkt = getGeometryFromWkt(GEOMETRYCOLLECTION);
    String gml = Wfs10JTStoGML200Converter.convertGeometryCollectionToGML(geometryFromWkt);
    GeometryCollectionType geometryCollectionType =
        (GeometryCollectionType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                gml, Wfs10Constants.GEOMETRY_COLLECTION);
    JAXBElement<GeometryCollectionType> jaxbElement =
        (JAXBElement<GeometryCollectionType>)
            Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(geometryCollectionType);
    assertFalse(jaxbElement == null);
  }

  @Test
  public void testGeometryToGeometryCollectionGML()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    GeometryCollection geometryCollection =
        (GeometryCollection) getGeometryFromWkt(GEOMETRYCOLLECTION);
    assertThat(geometryCollection == null, is(Boolean.FALSE));
    String geometryCollectionGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(geometryCollection).replaceAll("\n", "");

    assertThat(StringUtils.isEmpty(geometryCollectionGML), is(Boolean.FALSE));
    Diff diff = XMLUnit.compareXML(geometryCollectionGML, GEOMETRYCOLLECTION_GML);
    assertTrue(XMLUNIT_IDENTICAL, diff.identical());
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
  }

  @Test
  public void testGMLToGeometryCollectionType()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String geometryCollectionGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(GEOMETRYCOLLECTION));
    GeometryCollectionType geometryCollectionType =
        (GeometryCollectionType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                geometryCollectionGML, Wfs10Constants.GEOMETRY_COLLECTION);
    assertFalse(geometryCollectionType == null);
    List<JAXBElement<? extends GeometryAssociationType>> geometryMembers =
        geometryCollectionType.getGeometryMember();
    assertThat(CollectionUtils.isEmpty(geometryMembers), is(Boolean.FALSE));
    assertThat(geometryMembers.size() == 2, is(Boolean.TRUE));

    GeometryAssociationType geometryAssociationType = geometryMembers.get(0).getValue();
    JAXBElement<? extends AbstractGeometryType> jaxbElement = geometryAssociationType.getGeometry();
    assertThat(
        Wfs10Constants.POINT.getLocalPart().equals(jaxbElement.getName().getLocalPart()),
        is(Boolean.TRUE));
    PointType pointType = (PointType) jaxbElement.getValue();
    assertThat(pointType == null, is(Boolean.FALSE));
    assertThat(
        GEOMETRYCOLLECTION_POINT_COORD.equals(pointType.getCoordinates().getValue().trim()),
        is(Boolean.TRUE));

    GeometryAssociationType geometryAssociationType2 = geometryMembers.get(1).getValue();
    JAXBElement<? extends AbstractGeometryType> jaxbElement2 =
        geometryAssociationType2.getGeometry();
    assertThat(
        Wfs10Constants.LINESTRING.getLocalPart().equals(jaxbElement2.getName().getLocalPart()),
        is(Boolean.TRUE));
    LineStringType lineStringType = (LineStringType) jaxbElement2.getValue();
    assertThat(lineStringType == null, is(Boolean.FALSE));
    assertThat(
        GEOMETRYCOLLECTION_LINESTRING_COORD.equals(
            lineStringType.getCoordinates().getValue().trim()),
        is(Boolean.TRUE));
  }

  @Test
  public void testGeometryCollectionTypeToJAXB()
      throws JAXBException, SAXException, IOException, ParseException, NullPointerException {

    String geometryCollectionGML =
        Wfs10JTStoGML200Converter.convertGeometryToGML(getGeometryFromWkt(GEOMETRYCOLLECTION))
            .replaceAll("\n", "");
    GeometryCollectionType geometryCollectionType =
        (GeometryCollectionType)
            Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                geometryCollectionGML, Wfs10Constants.GEOMETRY_COLLECTION);
    JAXBElement<GeometryCollectionType> geometryCollectionTypeJAXBElement =
        (JAXBElement<GeometryCollectionType>)
            Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(geometryCollectionType);

    JAXB.marshal(geometryCollectionTypeJAXBElement, writer);
    String xml = writer.toString().replaceAll("\n", "");
    Diff diff = XMLUnit.compareXML(xml, GEOMETRYCOLLECTION_NS1);
    assertTrue(XMLUNIT_SIMILAR, diff.similar());
    assertThat(diff.similar(), is(Boolean.TRUE));
    assertThat(diff.identical(), is(Boolean.TRUE));
  }

  private Geometry getGeometryFromWkt(String wkt) throws ParseException {
    return new WKTReader().read(wkt);
  }
}
