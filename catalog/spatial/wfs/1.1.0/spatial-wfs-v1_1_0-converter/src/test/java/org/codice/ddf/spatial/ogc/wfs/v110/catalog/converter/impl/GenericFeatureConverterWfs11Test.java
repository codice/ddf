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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.converter.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlGeometryConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.junit.Before;
import org.junit.Test;

public class GenericFeatureConverterWfs11Test {

  private static final String GML = "GML";

  private XStream xStream;

  @Before
  public void setUp() {
    MetacardMapper metacardMapper = getMetacardMapper();
    GenericFeatureConverterWfs11 converter = getConverter(metacardMapper);
    xStream = getxStream(converter);
  }

  @Test
  public void testPointSrs26713Pos() throws IOException {
    try (InputStream is = open("/point-eps26713-pos.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(metacard.getLocation(), is("POINT (598566.26906782 4914058.52150682)"));
    }
  }

  @Test
  public void testLinearRingSrs26713PosList() throws IOException {
    try (InputStream is = open("/linearring-eps26713-poslist.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(metacard.getLocation(), is("LINEARRING (0 0, 0 10, 10 10, 10 0, 0 0)"));
    }
  }

  @Test
  public void testLinearRingSrs26713PointProperty() throws IOException {
    try (InputStream is = open("/linearring-eps26713-pointproperty.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(metacard.getLocation(), is("POINT (0 0)"));
    }
  }

  @Test
  public void testLineStringSrs26713() throws IOException {
    try (InputStream is = open("/linestring-eps26713.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(
          metacard.getLocation(),
          is(
              "LINESTRING (598566.26906782 4914058.52150682, 598557.69477679 4914085.33977038, 598494.67747205 4914254.76475676, 598513.91541973 4914348.11723536, 598593.95441378 4914440.37147034, 598656.43584271 4914500.87603708, 598659.42742456 4914521.61846645, 598652.10605456 4914532.58195028, 598648.43115534 4914544.16261933, 598635.63083991 4914558.16462797, 598554.58586588 4914636.67913744, 598498.3814486 4914752.4469346, 598489.81000042 4914778.04541272, 598421.49284933 4914873.66244196, 598409.19817361 4914931.57794141, 598404.90249957 4914948.64642947, 598340.17638384 4915068.66645978, 598342.49317686 4915118.07263127, 598346.0824185 4915117.02463364, 598566.26906782 4914058.52150682)"));
    }
  }

  @Test
  public void testCurveSrs26713() throws IOException {
    try (InputStream is = open("/curve-eps26713.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(
          metacard.getLocation(),
          is(
              "MULTILINESTRING ((598566.26906782 4914058.52150682, 598557.69477679 4914085.33977038, 598494.67747205 4914254.76475676, 598513.91541973 4914348.11723536, 598593.95441378 4914440.37147034, 598656.43584271 4914500.87603708, 598659.42742456 4914521.61846645, 598652.10605456 4914532.58195028, 598648.43115534 4914544.16261933, 598635.63083991 4914558.16462797, 598554.58586588 4914636.67913744, 598498.3814486 4914752.4469346, 598489.81000042 4914778.04541272, 598421.49284933 4914873.66244196, 598409.19817361 4914931.57794141, 598404.90249957 4914948.64642947, 598340.17638384 4915068.66645978, 598342.49317686 4915118.07263127, 598346.0824185 4915117.02463364, 598566.26906782 4914058.52150682))"));
    }
  }

  @Test
  public void testPolygonCurveSrs26713() throws IOException {
    try (InputStream is = open("/polygon-eps26713.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(
          metacard.getLocation(),
          is("POLYGON ((45.256 -110.45, 46.46 -109.48, 43.84 -109.86, 45.256 -110.45))"));
    }
  }

  @Test
  public void testSurfaceSrs26713() throws IOException {
    try (InputStream is = open("/surface-eps26713.xml")) {
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(metacard.getLocation(), notNullValue());
      assertThat(metacard.getLocation(), is("MULTIPOLYGON (((0 0, 0 1, 1 1, 1 0, 0 0)))"));
    }
  }

  @Test
  public void testCoordinatesAreSwappedWhenConverterCoordinateOrderIsLatLon() throws IOException {
    try (InputStream is = open("/polygon-eps26713.xml")) {
      MetacardMapper metacardMapper = getMetacardMapper();
      GenericFeatureConverterWfs11 converter = getConverter(metacardMapper);
      converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
      xStream = getxStream(converter);
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(
          metacard.getLocation(),
          is("POLYGON ((-110.45 45.256, -109.48 46.46, -109.86 43.84, -110.45 45.256))"));
    }
  }

  @Test
  public void testBasicMetacardAttributesArePopulatedEvenWhenNoMetacardMapperExists()
      throws IOException {
    try (InputStream is = open("/basic-metacard-attributes.xml")) {
      GenericFeatureConverterWfs11 converter = getConverter();
      xStream = getxStream(converter);
      Metacard metacard = (Metacard) xStream.fromXML(is);
      assertThat(metacard.getTitle(), is("The Title"));
      assertThat(
          metacard.getCreatedDate().toInstant(), is(Instant.parse("2019-07-05T21:05:00.000Z")));
    }
  }

  private InputStream open(String resource) {
    return GenericFeatureConverterWfs11Test.class.getResourceAsStream(resource);
  }

  private MetacardMapper getMetacardMapper() {
    MetacardMapper mockMapper = mock(MetacardMapper.class);
    doReturn(Core.LOCATION).when(mockMapper).getMetacardAttribute("the_geom");
    return mockMapper;
  }

  private GenericFeatureConverterWfs11 getConverter() {
    GenericFeatureConverterWfs11 converter =
        new GenericFeatureConverterWfs11("urn:x-ogc:def:crs:EPSG:26713");
    converter.setMetacardType(buildMetacardType());
    return converter;
  }

  private GenericFeatureConverterWfs11 getConverter(MetacardMapper metacardMapper) {
    GenericFeatureConverterWfs11 converter = new GenericFeatureConverterWfs11(metacardMapper);
    converter.setSrs("urn:x-ogc:def:crs:EPSG:26713");
    converter.setMetacardType(buildMetacardType());
    return converter;
  }

  private XStream getxStream(GenericFeatureConverterWfs11 converter) {
    XStream xstream = new XStream(new WstxDriver());
    xstream.registerConverter(converter);
    xstream.registerConverter(new GmlGeometryConverter());
    xstream.alias("featureMember", Metacard.class);
    return xstream;
  }

  private MetacardType buildMetacardType() {

    XmlSchema schema = new XmlSchema();
    schema.getElements().putAll(buildElementMap(schema));

    return new FeatureMetacardType(
        schema, new QName("roads"), new HashSet<>(), Wfs11Constants.GML_3_1_1_NAMESPACE);
  }

  private Map<QName, XmlSchemaElement> buildElementMap(XmlSchema schema) {
    Map<QName, XmlSchemaElement> elementMap = new HashMap<>();

    XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
    gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
    gmlElement.setSchemaTypeName(new QName(Wfs11Constants.GML_3_1_1_NAMESPACE, GML));
    gmlElement.setName("the_geom");
    elementMap.put(new QName("the_geom"), gmlElement);

    return elementMap;
  }
}
