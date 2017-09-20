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
import static org.hamcrest.Matchers.nullValue;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class GeoUtilTest {

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

    CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
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
    CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
    Geometry lonLatGeom = GeospatialUtil.transformToEPSG4326LonLatFormat(null, sourceCRS);
  }

  @Test(expected = GeoFormatException.class)
  public void testTransformEpsg4326LonLatBadSrs() throws GeoFormatException {
    GeometryFactory gf = new GeometryFactory();
    Coordinate coord = new Coordinate(25.22, 33.45);
    Point point = gf.createPoint(coord);
    Geometry geom = GeospatialUtil.transformToEPSG4326LonLatFormat(point, "ESPG:Bad");
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
    Geometry geom = GeospatialUtil.transformToEPSG4326LonLatFormat(null, "EPSG:4326");
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
}
