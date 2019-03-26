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
package ddf.catalog.transformer.metacard.geojson;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.geo.formatter.CompositeGeometry;
import ddf.geo.formatter.GeometryCollection;
import ddf.geo.formatter.LineString;
import ddf.geo.formatter.MultiLineString;
import ddf.geo.formatter.MultiPoint;
import ddf.geo.formatter.MultiPolygon;
import ddf.geo.formatter.Point;
import ddf.geo.formatter.Polygon;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the {@link GeoJsonMetacardTransformer} */
public class GeoJsonMetacardTransformerTest {

  public static final String DEFAULT_TITLE = "myTitle";

  public static final String DEFAULT_VERSION = "myVersion";

  public static final String DEFAULT_TYPE = "myType";

  public static final String DEFAULT_LOCATION = "POINT (1 0)";

  public static final String DEFAULT_SOURCE_ID = "ddfChild";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(GeoJsonMetacardTransformerTest.class);

  private static final JSONParser PARSER = new JSONParser();

  private static final String SOURCE_ID_PROPERTY = "source-id";

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacard() throws CatalogTransformerException {
    new GeoJsonMetacardTransformer().transform(null, null);
  }

  /**
   * Tests that proper JSON output is received when no {@link Core#LOCATION} is found.
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws ParseException
   */
  @Test
  public void testNoGeo() throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    assertThat(obj2.get("geometry"), nullValue());
    verifyBasicMetacardJson(now, obj2);
  }

  /**
   * Tests that improper WKT throws an exception
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws ParseException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testwithBadGeo() throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    String badWktMissingComma =
        "MULTILINESTRING ((10 10, 20 20, 10 40)(40 40, 30 30, 40 20, 30 10))";
    metacard.setLocation(badWktMissingComma);
    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    transformer.transform(metacard, null);
  }

  /**
   * Tests that a POINT Geography can be returned in JSON
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws ParseException
   */
  @Test
  public void testwithPointGeo() throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation(DEFAULT_LOCATION);
    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(Point.TYPE));
    List<Double> coords = (List<Double>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(coords.size(), is(2));
    assertThat(coords.get(0), equalTo(1.0));
    assertThat(coords.get(1), equalTo(0.0));

    verifyBasicMetacardJson(now, obj2);
  }

  /**
   * Tests that a LineString Geography can be returned in JSON
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws ParseException
   */
  @Test
  public void testwithLineStringGeo()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation("LINESTRING (30 10, 10 30, 40 40)");

    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(LineString.TYPE));
    List<List<Double>> coordsList =
        (List<List<Double>>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(coordsList.size(), is(3));
    for (List list : coordsList) {
      assertEquals(list.size(), 2);
    }
    assertThat(coordsList.get(0).get(0), equalTo(30.0));
    assertThat(coordsList.get(0).get(1), equalTo(10.0));
    assertThat(coordsList.get(1).get(0), equalTo(10.0));
    assertThat(coordsList.get(1).get(1), equalTo(30.0));
    assertThat(coordsList.get(2).get(0), equalTo(40.0));
    assertThat(coordsList.get(2).get(1), equalTo(40.0));

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testwithMultiPointGeo()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation("MULTIPOINT ((30 10), (10 30), (40 40))");
    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(MultiPoint.TYPE));
    List<List<Double>> coordsList =
        (List<List<Double>>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(coordsList.size(), is(3));
    for (List list : coordsList) {
      assertEquals(list.size(), 2);
    }
    assertThat(coordsList.get(0).get(0), equalTo(30.0));
    assertThat(coordsList.get(0).get(1), equalTo(10.0));
    assertThat(coordsList.get(1).get(0), equalTo(10.0));
    assertThat(coordsList.get(1).get(1), equalTo(30.0));
    assertThat(coordsList.get(2).get(0), equalTo(40.0));
    assertThat(coordsList.get(2).get(1), equalTo(40.0));

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testwithMultiLineStringGeo()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation("MULTILINESTRING ((10 10, 20 20, 10 40),(40 40, 30 30, 40 20, 30 10))");
    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(MultiLineString.TYPE));
    List<List<List<Double>>> coordsList =
        (List<List<List<Double>>>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(coordsList.size(), is(2));
    List<List<Double>> list1 = coordsList.get(0);
    List<List<Double>> list2 = coordsList.get(1);
    assertThat(list1.get(0).get(0), equalTo(10.0));
    assertThat(list1.get(0).get(1), equalTo(10.0));
    assertThat(list1.get(1).get(0), equalTo(20.0));
    assertThat(list1.get(1).get(1), equalTo(20.0));
    assertThat(list1.get(2).get(0), equalTo(10.0));
    assertThat(list1.get(2).get(1), equalTo(40.0));

    assertThat(list2.get(0).get(0), equalTo(40.0));
    assertThat(list2.get(0).get(1), equalTo(40.0));
    assertThat(list2.get(1).get(0), equalTo(30.0));
    assertThat(list2.get(1).get(1), equalTo(30.0));
    assertThat(list2.get(2).get(0), equalTo(40.0));
    assertThat(list2.get(2).get(1), equalTo(20.0));
    assertThat(list2.get(3).get(0), equalTo(30.0));
    assertThat(list2.get(3).get(1), equalTo(10.0));

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testwithPolygonGeoNoHole()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");

    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(Polygon.TYPE));
    List<List> listOfRings = (List<List>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(listOfRings.size(), is(1));

    List<List> exteriorRing = listOfRings.get(0);

    testPopularPolygon(exteriorRing);

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testwithPolygonGeoWithHole()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation(
        "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10),(20 30, 35 35, 30 20, 20 30))");

    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(Polygon.TYPE));
    List<List> listOfRings = (List<List>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(listOfRings.size(), is(2));

    List<List> exteriorRing = listOfRings.get(0);
    List<List> interiorRing = listOfRings.get(1);

    testPopularPolygon(exteriorRing);

    List<Double> interiorlist1 = interiorRing.get(0);
    List<Double> interiorlist2 = interiorRing.get(1);
    List<Double> interiorlist3 = interiorRing.get(2);
    List<Double> interiorlist4 = interiorRing.get(3);

    assertThat(interiorlist1.get(0), equalTo(20.0));
    assertThat(interiorlist1.get(1), equalTo(30.0));
    assertThat(interiorlist2.get(0), equalTo(35.0));
    assertThat(interiorlist2.get(1), equalTo(35.0));
    assertThat(interiorlist3.get(0), equalTo(30.0));
    assertThat(interiorlist3.get(1), equalTo(20.0));
    assertThat(interiorlist4.get(0), equalTo(20.0));
    assertThat(interiorlist4.get(1), equalTo(30.0));

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testWithMultiPolygonGeo()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation(
        "MULTIPOLYGON (((30 10, 10 20, 20 40, 40 40, 30 10)),((15 5, 40 10, 10 20, 5 10, 15 5)))");

    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(MultiPolygon.TYPE));

    List<List> listOfPolygons = (List<List>) geometryMap.get(CompositeGeometry.COORDINATES_KEY);
    assertThat(listOfPolygons.size(), is(2));

    List<List> polygon1 = listOfPolygons.get(0);
    List<List> polygon2 = listOfPolygons.get(1);

    List<List> polygon1FirstRing = polygon1.get(0);
    List<List> polygon2FirstRing = polygon2.get(0);

    testPopularPolygon(polygon1FirstRing);

    testSecondPolygon(polygon2FirstRing);

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testWithGeometryCollection()
      throws CatalogTransformerException, IOException, ParseException {

    Date now = new Date();

    MetacardImpl metacard = new MetacardImpl();

    metacard.setLocation("GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))");

    setupBasicMetacard(now, metacard);

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();

    BinaryContent content = transformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), GeoJsonMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    LOGGER.debug(jsonText);
    Object object = PARSER.parse(jsonText);

    JSONObject obj2 = (JSONObject) object;

    Map geometryMap = (Map) obj2.get("geometry");
    assertThat(geometryMap.get(CompositeGeometry.TYPE_KEY).toString(), is(GeometryCollection.TYPE));

    assertThat(geometryMap.get(CompositeGeometry.GEOMETRIES_KEY), notNullValue());

    verifyBasicMetacardJson(now, obj2);
  }

  @Test
  public void testWithMultiValueAttributes() throws Exception {
    Set<AttributeDescriptor> descriptors =
        new HashSet(MetacardImpl.BASIC_METACARD.getAttributeDescriptors());
    descriptors.add(
        new AttributeDescriptorImpl(
            "multi-string", true, true, false, true, /* multivalued */ BasicTypes.STRING_TYPE));
    MetacardType type = new MetacardTypeImpl("multi", descriptors);
    MetacardImpl metacard = new MetacardImpl(type);

    metacard.setAttribute("multi-string", (Serializable) Arrays.asList("foo", "bar"));

    GeoJsonMetacardTransformer transformer = new GeoJsonMetacardTransformer();
    BinaryContent content = transformer.transform(metacard, null);
    String jsonText = new String(content.getByteArray());
    JSONObject json = (JSONObject) PARSER.parse(jsonText);

    Map properties = (Map) json.get("properties");
    List<String> strings = (List<String>) properties.get("multi-string");
    assertThat(strings.get(0), is("foo"));
    assertThat(strings.get(1), is("bar"));
  }

  protected void testPopularPolygon(List<List> exteriorRing) {

    // (30 10, 10 20, 20 40, 40 40, 30 10)
    List<Double> list1 = exteriorRing.get(0);
    List<Double> list2 = exteriorRing.get(1);
    List<Double> list3 = exteriorRing.get(2);
    List<Double> list4 = exteriorRing.get(3);
    List<Double> list5 = exteriorRing.get(4);

    assertThat(list1.get(0), equalTo(30.0));
    assertThat(list1.get(1), equalTo(10.0));
    assertThat(list2.get(0), equalTo(10.0));
    assertThat(list2.get(1), equalTo(20.0));
    assertThat(list3.get(0), equalTo(20.0));
    assertThat(list3.get(1), equalTo(40.0));
    assertThat(list4.get(0), equalTo(40.0));
    assertThat(list4.get(1), equalTo(40.0));
    assertThat(list5.get(0), equalTo(30.0));
    assertThat(list5.get(1), equalTo(10.0));
  }

  protected void testSecondPolygon(List<List> exteriorRing) {
    // (15 5, 40 10, 10 20, 5 10, 15 5)
    List<Double> list1 = exteriorRing.get(0);
    List<Double> list2 = exteriorRing.get(1);
    List<Double> list3 = exteriorRing.get(2);
    List<Double> list4 = exteriorRing.get(3);
    List<Double> list5 = exteriorRing.get(4);

    assertThat(list1.get(0), equalTo(15.0));
    assertThat(list1.get(1), equalTo(5.0));
    assertThat(list2.get(0), equalTo(40.0));
    assertThat(list2.get(1), equalTo(10.0));
    assertThat(list3.get(0), equalTo(10.0));
    assertThat(list3.get(1), equalTo(20.0));
    assertThat(list4.get(0), equalTo(5.0));
    assertThat(list4.get(1), equalTo(10.0));
    assertThat(list5.get(0), equalTo(15.0));
    assertThat(list5.get(1), equalTo(5.0));
  }

  private void setupBasicMetacard(Date now, MetacardImpl metacard) {
    metacard.setCreatedDate(now);
    metacard.setModifiedDate(now);
    metacard.setMetadata("<xml></xml>");
    metacard.setContentTypeName(DEFAULT_TYPE);
    metacard.setContentTypeVersion(DEFAULT_VERSION);
    byte[] buffer = {8};
    metacard.setThumbnail(buffer);
    // metacard.setSourceId(DEFAULT_SOURCE_ID) ;
    metacard.setTitle(DEFAULT_TITLE);
    metacard.setSourceId(DEFAULT_SOURCE_ID);
    try {
      metacard.setResourceURI(new URI("http://example.com"));
    } catch (URISyntaxException e) {
      LOGGER.warn("URI Syntax exception setting resource URI", e);
    }
  }

  private void verifyBasicMetacardJson(Date now, JSONObject obj2) {
    assertThat(obj2.size(), is(3)); // no extra members
    assertThat(obj2.get("type").toString(), equalTo("Feature"));
    assertThat(obj2.get("properties"), notNullValue());
    Map properties = ((Map) obj2.get("properties"));
    assertThat(properties.size(), is(10)); // no extra
    // properties
    assertThat(toString(properties.get(Core.TITLE)), is(DEFAULT_TITLE));
    assertThat(toString(properties.get(Metacard.CONTENT_TYPE)), is(DEFAULT_TYPE));
    assertThat(toString(properties.get(Metacard.CONTENT_TYPE_VERSION)), is(DEFAULT_VERSION));
    SimpleDateFormat dateFormat =
        new SimpleDateFormat(GeoJsonMetacardTransformer.ISO_8601_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    assertThat(toString(properties.get(Core.CREATED)), is(dateFormat.format(now)));
    assertThat(toString(properties.get(Core.EXPIRATION)), nullValue());
    assertThat(toString(properties.get(Metacard.EFFECTIVE)), nullValue());
    assertThat(toString(properties.get(Core.MODIFIED)), is(dateFormat.format(now)));
    assertThat(toString(properties.get(Core.THUMBNAIL)), is("CA=="));
    assertThat(toString(properties.get(Core.METADATA)), is("<xml></xml>"));
    assertThat(toString(properties.get(Core.RESOURCE_URI)), is("http://example.com"));
    assertThat(toString(properties.get(SOURCE_ID_PROPERTY)), is(DEFAULT_SOURCE_ID));
    assertThat(
        toString(properties.get(GeoJsonMetacardTransformer.METACARD_TYPE_PROPERTY_KEY)),
        is(MetacardImpl.BASIC_METACARD.getName()));
  }

  private String toString(Object object) {

    if (object != null) {
      return object.toString();
    }
    return null;
  }
}
