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
package org.codice.ddf.libs.geo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class GeoUtilTest {

  private static final int DEFAULT_DISTANCE_TOLERANCE = 100;

  private static final int DEFAULT_MAX_VERTICES = 32;

  @Test
  public void testDMSLatNorth() throws GeoFormatException {
    String dmsLat = "60:33:22.5N";
    Double degLat = GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(dmsLat);
    assertThat(degLat, is(60.55625));
  }

  @Test
  public void testDMSLatNorthNoSeconds() throws GeoFormatException {
    String dmsLat = "60:33N";
    Double degLat = GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(dmsLat);
    assertThat(degLat, is(60.55));
  }

  @Test
  public void testDMSLatSouth() throws GeoFormatException {
    String dmsLat = "60:33:22.5S";
    Double degLat = GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(dmsLat);
    assertThat(degLat, is(-60.55625));
  }

  @Test
  public void testDMSLonEast() throws GeoFormatException {
    String dmsLon = "100:22:33.6E";
    Double degLon = GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(dmsLon);
    assertThat(degLon, closeTo(100.376, .00001));
  }

  @Test
  public void testDMSLonEastNoSeconds() throws GeoFormatException {
    String dmsLon = "100:30E";
    Double degLon = GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(dmsLon);
    assertThat(degLon, closeTo(100.5, .00001));
  }

  @Test
  public void testDMSLonWest() throws GeoFormatException {
    String dmsLon = "100:22:33.6W";
    Double degLon = GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(dmsLon);
    assertThat(degLon, closeTo(-100.376, .00001));
  }

  @Test(expected = GeoFormatException.class)
  public void invalidLatHemisphere() throws GeoFormatException {
    String invalidLat = "100:22:33.6W";
    GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(invalidLat);
  }

  @Test(expected = GeoFormatException.class)
  public void invalidLonHemisphere() throws GeoFormatException {
    String invalidLon = "60:33:22.5N";
    GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(invalidLon);
  }

  @Test
  public void testNullLat() throws GeoFormatException {
    Double lat = GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(null);
    assertThat(lat, nullValue());
  }

  @Test
  public void testNullLon() throws GeoFormatException {
    Double lon = GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(null);
    assertThat(lon, nullValue());
  }

  @Test
  public void testTransformEpsg4326LonLat() throws GeoFormatException {
    GeometryFactory gf = new GeometryFactory();
    Coordinate coord = new Coordinate(25.22, 33.45);
    Point point = gf.createPoint(coord);
    Geometry convertedGeometry =
        GeospatialUtil.transformToEPSG4326LonLatFormat(point, GeospatialUtil.EPSG_4326);
    assertThat(convertedGeometry.getCoordinates()[0].x, is(33.45));
    assertThat(convertedGeometry.getCoordinates()[0].y, is(25.22));
  }

  @Test
  public void testTransformEpsg4326UTM()
      throws FactoryException, TransformException, GeoFormatException {
    double lon = 33.45;
    double lat = 25.22;
    double easting = 545328.48;
    double northing = 2789384.24;

    CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:32636");
    GeometryFactory geometryFactory = new GeometryFactory();
    Coordinate utmCoordinate = new Coordinate(easting, northing);
    Point utmPoint = geometryFactory.createPoint(utmCoordinate);
    Envelope envelope = JTS.toEnvelope(utmPoint);
    Geometry utmGeometry = JTS.toGeometry(envelope);
    Geometry lonLatGeom = GeospatialUtil.transformToEPSG4326LonLatFormat(utmGeometry, sourceCRS);
    assertThat(lonLatGeom.getCoordinates()[0].x, closeTo(lon, .00001));
    assertThat(lonLatGeom.getCoordinates()[0].y, closeTo(lat, .00001));
  }

  @Test
  public void testTransformEpsg4326EpsgMatch()
      throws FactoryException, TransformException, GeoFormatException {
    double lon = 33.45;
    double lat = 25.22;

    CoordinateReferenceSystem sourceCRS = CRS.decode(GeospatialUtil.EPSG_4326);
    GeometryFactory geometryFactory = new GeometryFactory();
    Coordinate coordinate = new Coordinate(lon, lat);
    Point utmPoint = geometryFactory.createPoint(coordinate);
    Envelope envelope = JTS.toEnvelope(utmPoint);
    Geometry utmGeometry = JTS.toGeometry(envelope);
    Geometry lonLatGeom = GeospatialUtil.transformToEPSG4326LonLatFormat(utmGeometry, sourceCRS);
    assertThat(lonLatGeom.getCoordinates()[0].x, closeTo(lon, .00001));
    assertThat(lonLatGeom.getCoordinates()[0].y, closeTo(lat, .00001));
  }

  @Test
  public void testTransformEpsg4326EpsgNoSourceCRS()
      throws FactoryException, TransformException, GeoFormatException {
    double lon = 33.45;
    double lat = 25.22;

    CoordinateReferenceSystem sourceCRS = null;
    GeometryFactory geometryFactory = new GeometryFactory();
    Coordinate coordinate = new Coordinate(lon, lat);
    Point utmPoint = geometryFactory.createPoint(coordinate);
    Envelope envelope = JTS.toEnvelope(utmPoint);
    Geometry utmGeometry = JTS.toGeometry(envelope);
    Geometry lonLatGeom = GeospatialUtil.transformToEPSG4326LonLatFormat(utmGeometry, sourceCRS);
    assertThat(lonLatGeom.getCoordinates()[0].x, closeTo(lon, .00001));
    assertThat(lonLatGeom.getCoordinates()[0].y, closeTo(lat, .00001));
  }

  @Test(expected = GeoFormatException.class)
  public void testTransformEpsg4326EpsgNullGeom()
      throws FactoryException, TransformException, GeoFormatException {
    CoordinateReferenceSystem sourceCRS = CRS.decode(GeospatialUtil.EPSG_4326);
    GeospatialUtil.transformToEPSG4326LonLatFormat(null, sourceCRS);
  }

  @Test(expected = GeoFormatException.class)
  public void testTransformEpsg4326LonLatBadSrs() throws GeoFormatException {
    GeometryFactory gf = new GeometryFactory();
    Coordinate coord = new Coordinate(25.22, 33.45);
    Point point = gf.createPoint(coord);
    GeospatialUtil.transformToEPSG4326LonLatFormat(point, "ESPG:Bad");
  }

  @Test
  public void testTransformEpsg4326LonLatNullSrs() throws GeoFormatException {
    GeometryFactory gf = new GeometryFactory();
    Coordinate coord = new Coordinate(25.22, 33.45);
    Point point = gf.createPoint(coord);
    Geometry geom = GeospatialUtil.transformToEPSG4326LonLatFormat(point, (String) null);
    assertThat(geom, is(point));
  }

  @Test(expected = GeoFormatException.class)
  public void testTransformEpsg4326LonLatNullGeom() throws GeoFormatException {
    GeospatialUtil.transformToEPSG4326LonLatFormat(null, "EPSG:4326");
  }

  @Test(expected = GeoFormatException.class)
  public void testLatDegreeExcept() throws GeoFormatException {
    String invalidLat = "AB:CD:EF.GN";
    GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(invalidLat);
  }

  @Test(expected = GeoFormatException.class)
  public void testLatMinuteExcept() throws GeoFormatException {
    String invalidLat = "12:CD:EF.GN";
    GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(invalidLat);
  }

  @Test(expected = GeoFormatException.class)
  public void testLatSecondExcept() throws GeoFormatException {
    String invalidLat = "12:34:EF.GN";
    GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(invalidLat);
  }

  @Test(expected = GeoFormatException.class)
  public void testLatRangeInvalidMin() throws GeoFormatException {
    String invalidLat = "100:00:00.0S";
    GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(invalidLat);
  }

  @Test(expected = GeoFormatException.class)
  public void testLatRangeInvalidMax() throws GeoFormatException {
    String invalidLat = "100:00:00.0N";
    GeospatialUtil.parseDMSLatitudeWithDecimalSeconds(invalidLat);
  }

  @Test(expected = GeoFormatException.class)
  public void testLonDegreeExcept() throws GeoFormatException {
    String invalidLon = "AB:CD:EF.GW";
    GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(invalidLon);
  }

  @Test(expected = GeoFormatException.class)
  public void testLonMinuteExcept() throws GeoFormatException {
    String invalidLon = "12:CD:EF.GW";
    GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(invalidLon);
  }

  @Test(expected = GeoFormatException.class)
  public void testLonSecondExcept() throws GeoFormatException {
    String invalidLon = "12:34:EF.GW";
    GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(invalidLon);
  }

  @Test(expected = GeoFormatException.class)
  public void testLonRangeInvalidMin() throws GeoFormatException {
    String invalidLon = "181:00:00.0E";
    GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(invalidLon);
  }

  @Test(expected = GeoFormatException.class)
  public void testLonRangeInvalidMax() throws GeoFormatException {
    String invalidLon = "181:00:00.0W";
    GeospatialUtil.parseDMSLongitudeWithDecimalSeconds(invalidLon);
  }

  @Test
  public void testConvertPointRadiusToCirclePolygon() throws ParseException {
    double lat = 43.25;
    double lon = -123.45;
    double radius = 100;

    WKTReader reader = new WKTReader();
    Geometry expectedCircle =
        reader.read(
            "POLYGON ((-123.44876816366177 43.250004839118525, -123.44879054324761 43.24982907875929, -123.44885940119903 43.24965988702688, -123.44897209100492 43.24950376578656, -123.44912428182974 43.249366714570776, -123.44931012498019 43.24925400003869, -123.44952247868014 43.249169953603044, -123.44975318251424 43.24911780499826, -123.44999337099466 43.24909955818223, -123.45023381420664 43.24911591433818, -123.45046527244881 43.24916624493329, -123.45067885124946 43.24924861586916, -123.4508663431246 43.24935986179615, -123.45102054295182 43.24949570773787, -123.45113552484636 43.249650933354495, -123.45120686990035 43.24981957353561, -123.45123183603215 43.249995147617355, -123.4512094634135 43.25017090841798, -123.45114061141513 43.25034010152336, -123.45102792564194 43.250496224859205, -123.45087573631548 43.25063327857405, -123.45068989190095 43.250745995628286, -123.45047753436687 43.250830044225154, -123.45024682471237 43.25088219430127, -123.45000662931133 43.250900441674666, -123.4497661791322 43.25088408507735, -123.44953471493693 43.25083375310927, -123.44932113210359 43.25075138007788, -123.44913363873012 43.25064013165182, -123.44897944016698 43.250504283187965, -123.44886446210654 43.250349055410105, -123.44879312287296 43.25018041375767, -123.44876816366177 43.250004839118525))");
    Geometry circle =
        GeospatialUtil.createCirclePolygon(
            lat, lon, radius, DEFAULT_MAX_VERTICES, DEFAULT_DISTANCE_TOLERANCE);
    assertThat(
        circle.getCoordinates().length,
        Is.is(equalTo(DEFAULT_MAX_VERTICES + 1))); // additional coordinate for closing the polygon
    assertThat(circle, Is.is(equalTo(expectedCircle)));
  }

  @Test
  public void testReduceVertices() {
    double lat = 43.25;
    double lon = -123.45;
    double radius = 500000;
    int maxVertices = 10;

    Geometry circle = GeospatialUtil.createCirclePolygon(lat, lon, radius, maxVertices, 0.5);

    assertThat(circle.getCoordinates().length, Is.is(lessThan(maxVertices + 1)));
  }
}
