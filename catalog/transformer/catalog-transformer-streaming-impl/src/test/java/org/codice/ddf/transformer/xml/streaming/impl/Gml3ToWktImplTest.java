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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import ddf.catalog.validation.ValidationException;
import java.io.InputStream;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.geotools.gml3.GMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Gml3ToWktImplTest {

  private Gml3ToWkt gtw;

  private String createWktPoint(int x, int y) {
    return String.format("POINT (%s %s)", x, y);
  }

  private String createGmlPoint(int x, int y) {
    return String.format(
        "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos>%s %s</gml:pos></gml:Point>",
        x, y);
  }

  private String createSrsAwareGmlPoint(int x, int y, String srsName) {
    return String.format(
        "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"%s\"><gml:pos>%s %s</gml:pos></gml:Point>",
        srsName, x, y);
  }

  @Before
  public void setUp() {
    gtw = new Gml3ToWktImpl(new GMLConfiguration());
  }

  @Test
  public void testGmlPointToWkt() throws ValidationException {
    assertThat(gtw.convert(createGmlPoint(0, 0)), is(createWktPoint(0, 0)));
    assertThat(gtw.convert(createGmlPoint(0, 1)), is(createWktPoint(1, 0)));
    assertThat(gtw.convert(createGmlPoint(1, 0)), is(createWktPoint(0, 1)));
    assertThat(gtw.convert(createGmlPoint(1, 1)), is(createWktPoint(1, 1)));
  }

  @Test(expected = ValidationException.class)
  public void testBadGml() throws ValidationException {
    String badGml = createGmlPoint(0, 0).replaceAll("pos", "badType");
    gtw.convert(badGml);
  }

  @Test(expected = ValidationException.class)
  public void testBadInputStreamConvert() throws ValidationException {
    gtw.convert((InputStream) null);
  }

  @Test(expected = ValidationException.class)
  public void testBadInputStreamParseXml() throws ValidationException {
    gtw.convert((InputStream) null);
  }

  @Test(expected = ValidationException.class)
  public void testBadXmlStructure() throws ValidationException {
    String badXml = "<gml:Point></gml:Point>";
    gtw.convert(badXml);
  }

  @Test
  public void testConvertEnvelope() throws ValidationException {
    String gml =
        "<gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\"><gml:lowerCorner>1.0 0.0</gml:lowerCorner><gml:upperCorner>0.0 1.0</gml:upperCorner></gml:Envelope>";
    String wkt = gtw.convert(gml);
    assertThat(wkt, equalTo("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))"));
  }

  @Test
  public void testGmlPointToWktValidSrsName() throws ValidationException {
    assertThat(gtw.convert(createSrsAwareGmlPoint(0, 0, "EPSG:4326")), is(createWktPoint(0, 0)));
    assertThat(gtw.convert(createSrsAwareGmlPoint(0, 1, "EPSG:4326")), is(createWktPoint(0, 1)));
    assertThat(gtw.convert(createSrsAwareGmlPoint(1, 0, "EPSG:4326")), is(createWktPoint(1, 0)));
    assertThat(gtw.convert(createSrsAwareGmlPoint(1, 1, "EPSG:4326")), is(createWktPoint(1, 1)));
  }

  @Test
  public void testGmlPointToWktValidSrsNameUrn() throws ValidationException {
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(0, 0, "urn:x-ogc:def:crs:EPSG:4326")),
        is(createWktPoint(0, 0)));
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(0, 1, "urn:x-ogc:def:crs:EPSG:4326")),
        is(createWktPoint(0, 1)));
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(1, 0, "urn:x-ogc:def:crs:EPSG:4326")),
        is(createWktPoint(1, 0)));
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(1, 1, "urn:x-ogc:def:crs:EPSG:4326")),
        is(createWktPoint(1, 1)));
  }

  @Test
  public void testGmlPointToWktValidSrsNameUrl() throws ValidationException {
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(0, 0, "http://www.opengis.net/def/crs/EPSG/0/4326")),
        is(createWktPoint(0, 0)));
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(0, 1, "http://www.opengis.net/def/crs/EPSG/0/4326")),
        is(createWktPoint(0, 1)));
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(1, 0, "http://www.opengis.net/def/crs/EPSG/0/4326")),
        is(createWktPoint(1, 0)));
    assertThat(
        gtw.convert(createSrsAwareGmlPoint(1, 1, "http://www.opengis.net/def/crs/EPSG/0/4326")),
        is(createWktPoint(1, 1)));
  }

  @Test
  public void testGmlPointToWktInvalidSrsName() throws ValidationException {
    assertThat(gtw.convert(createSrsAwareGmlPoint(0, 0, "someSrs")), is(createWktPoint(0, 0)));
    assertThat(gtw.convert(createSrsAwareGmlPoint(0, 1, "someSrs")), is(createWktPoint(1, 0)));
    assertThat(gtw.convert(createSrsAwareGmlPoint(1, 0, "someSrs")), is(createWktPoint(0, 1)));
    assertThat(gtw.convert(createSrsAwareGmlPoint(1, 1, "someSrs")), is(createWktPoint(1, 1)));
  }

  @Test
  public void testGmlLinearRingToLinearString() throws ValidationException {
    String gmlFilename = "/linearRing.gml";
    InputStream inputStream = getInputStream(gmlFilename);

    String wkt = gtw.convert(inputStream);
    assertThat(wkt, is(notNullValue()));
    assertThat(wkt, not(containsString("LINEARRING")));
    assertThat(wkt, containsString("LINESTRING"));
  }

  @Test
  public void testGmlGeometryCollectionsContainingLinearRing() throws ValidationException {
    String gmlFilename = "/multipleCollections.gml";
    InputStream inputStream = getInputStream(gmlFilename);

    String wkt = gtw.convert(inputStream);
    assertThat(wkt, is(notNullValue()));
    assertThat(wkt, not(containsString("LINEARRING")));
    assertThat(wkt, containsString("LINESTRING"));
    assertThat(wkt, containsString("GEOMETRYCOLLECTION"));
    assertThat(wkt, containsString("POINT"));
  }

  @Test
  public void testGmlGeometryCollectionWithMultiPolygon() throws ValidationException {
    String gmlFilename = "/multiPolygon.gml";
    InputStream inputStream = getInputStream(gmlFilename);

    String wkt = gtw.convert(inputStream);
    assertThat(wkt, is(notNullValue()));
    assertThat(wkt, not(containsString("LINEARRING")));
    assertThat(wkt, containsString("MULTIPOLYGON"));
    assertThat(wkt, containsString("GEOMETRYCOLLECTION"));
  }

  private InputStream getInputStream(String filename) {
    return this.getClass().getResourceAsStream(filename);
  }
}
