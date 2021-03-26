/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.converter;

import net.opengis.kml.v_2_2_0.AbstractGeometryType;
import net.opengis.kml.v_2_2_0.LineStringType;
import net.opengis.kml.v_2_2_0.LinearRingType;
import net.opengis.kml.v_2_2_0.ModelType;
import net.opengis.kml.v_2_2_0.MultiGeometryType;
import net.opengis.kml.v_2_2_0.PointType;
import net.opengis.kml.v_2_2_0.PolygonType;
import org.locationtech.jts.geom.GeometryCollection;

public class KmlToJtsGeometryConverterTest {

  //  @Test
  //  public void testConvertNullGeometry() {
  //    org.locationtech.jts.geom.Geometry jtsGeometry = KmlToJtsGeometryConverter.from(null);
  //
  //    assertThat(jtsGeometry, nullValue());
  //  }
  //
  //  @Test
  //  public void testConvertPointGeometry() {
  //    InputStream stream =
  // KmlToJtsGeometryConverterTest.class.getResourceAsStream("/kmlPoint.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    assertThat(kml, notNullValue());
  //
  //    Point kmlPoint = ((Point) ((Placemark) kml.getFeature()).getGeometry());
  //    assertThat(kmlPoint, notNullValue());
  //
  //    org.locationtech.jts.geom.Geometry jtsGeometryPoint =
  // KmlToJtsGeometryConverter.from(kmlPoint);
  //    assertThat(jtsGeometryPoint, instanceOf(org.locationtech.jts.geom.Point.class));
  //
  //    assertSpecificGeometry(kmlPoint, jtsGeometryPoint);
  //  }
  //
  //  @Test
  //  public void testConvertLineStringGeometry() {
  //    InputStream stream =
  //        KmlToJtsGeometryConverterTest.class.getResourceAsStream("/kmlLineString.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //    assertThat(kml, notNullValue());
  //
  //    LineString kmlLineString = ((LineString) ((Placemark) kml.getFeature()).getGeometry());
  //    assertThat(kmlLineString, notNullValue());
  //
  //    org.locationtech.jts.geom.Geometry jtsGeometryLineString =
  //        KmlToJtsGeometryConverter.from(kmlLineString);
  //    assertThat(jtsGeometryLineString, instanceOf(org.locationtech.jts.geom.LineString.class));
  //
  //    assertSpecificGeometry(kmlLineString, jtsGeometryLineString);
  //  }
  //
  //  @Test
  //  public void testConvertLinearRingGeometry() {
  //    InputStream stream =
  //        KmlToJtsGeometryConverterTest.class.getResourceAsStream("/kmlLinearRing.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //    assertThat(kml, notNullValue());
  //
  //    LinearRing kmlLinearRing = ((LinearRing) ((Placemark) kml.getFeature()).getGeometry());
  //    assertThat(kmlLinearRing, notNullValue());
  //
  //    org.locationtech.jts.geom.Geometry jtsGeometryLinearRing =
  //        KmlToJtsGeometryConverter.from(kmlLinearRing);
  //    assertThat(jtsGeometryLinearRing, instanceOf(org.locationtech.jts.geom.LinearRing.class));
  //
  //    assertSpecificGeometry(kmlLinearRing, jtsGeometryLinearRing);
  //  }
  //
  //  @Test
  //  public void testConvertPolygonGeometry() {
  //    InputStream stream =
  // KmlToJtsGeometryConverterTest.class.getResourceAsStream("/kmlPolygon.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //    assertThat(kml, notNullValue());
  //
  //    Polygon kmlPolygon = ((Polygon) ((Placemark) kml.getFeature()).getGeometry());
  //    assertThat(kmlPolygon, notNullValue());
  //
  //    org.locationtech.jts.geom.Geometry jtsGeometryPolygon =
  //        KmlToJtsGeometryConverter.from(kmlPolygon);
  //    assertThat(jtsGeometryPolygon, instanceOf(org.locationtech.jts.geom.Polygon.class));
  //
  //    assertSpecificGeometry(kmlPolygon, jtsGeometryPolygon);
  //  }
  //
  //  @Test
  //  public void testConvertMultiGeometry() {
  //    InputStream stream =
  //        KmlToJtsGeometryConverterTest.class.getResourceAsStream("/kmlMultiGeometry.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //    assertThat(kml, notNullValue());
  //
  //    MultiGeometry multiGeometry = ((MultiGeometry) ((Placemark)
  // kml.getFeature()).getGeometry());
  //    assertThat(multiGeometry, notNullValue());
  //
  //    org.locationtech.jts.geom.Geometry jtsGeometryCollectionGeometry =
  //        KmlToJtsGeometryConverter.from(multiGeometry);
  //    assertThat(jtsGeometryCollectionGeometry, instanceOf(GeometryCollection.class));
  //
  //    assertSpecificGeometry(multiGeometry, jtsGeometryCollectionGeometry);
  //  }
  //
  //  @Test
  //  public void testConvertModelGeometry() {
  //    InputStream stream =
  // KmlToJtsGeometryConverterTest.class.getResourceAsStream("/kmlModel.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //    assertThat(kml, notNullValue());
  //
  //    Model model = ((Model) ((Placemark) kml.getFeature()).getGeometry());
  //    assertThat(model, notNullValue());
  //
  //    org.locationtech.jts.geom.Geometry jtsGeometryPointFromModel =
  //        KmlToJtsGeometryConverter.from(model);
  //    assertThat(jtsGeometryPointFromModel, instanceOf(org.locationtech.jts.geom.Point.class));
  //
  //    assertSpecificGeometry(model, jtsGeometryPointFromModel);
  //  }

  static void assertSpecificGeometry(
      AbstractGeometryType kmlGeometry, org.locationtech.jts.geom.Geometry jtsGeometry) {
    if (kmlGeometry instanceof PointType) {
      KmlToJtsPointConverterTest.assertJtsPoint(
          (PointType) kmlGeometry, (org.locationtech.jts.geom.Point) jtsGeometry);
    }

    if (kmlGeometry instanceof LineStringType) {
      KmlToJtsLineStringConverterTest.assertTestKmlLineString(
          (LineStringType) kmlGeometry, (org.locationtech.jts.geom.LineString) jtsGeometry);
    }

    if (kmlGeometry instanceof LinearRingType) {
      KmlToJtsLinearRingConverterTest.assertJtsLinearRing(
          (LinearRingType) kmlGeometry, (org.locationtech.jts.geom.LinearRing) jtsGeometry);
    }

    if (kmlGeometry instanceof PolygonType) {
      KmlToJtsPolygonConverterTest.assertJtsPolygon(
          (PolygonType) kmlGeometry, (org.locationtech.jts.geom.Polygon) jtsGeometry);
    }

    if (kmlGeometry instanceof MultiGeometryType) {
      KmlToJtsMultiGeometryConverterTest.assertJtsGeometryCollection(
          (MultiGeometryType) kmlGeometry, (GeometryCollection) jtsGeometry);
    }

    if (kmlGeometry instanceof ModelType) {
      KmlModelToJtsPointConverterTest.assertKmlModelToJtsPoint(
          (ModelType) kmlGeometry, (org.locationtech.jts.geom.Point) jtsGeometry);
    }
  }
}
