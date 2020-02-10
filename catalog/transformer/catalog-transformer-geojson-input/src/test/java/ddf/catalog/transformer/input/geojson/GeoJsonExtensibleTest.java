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
package ddf.catalog.transformer.input.geojson;

import static ddf.catalog.data.impl.BasicTypes.DOUBLE_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeRegistryImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.codice.ddf.platform.util.SortedServiceList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeoJsonExtensibleTest {

  private GeoJsonInputTransformer transformer;

  public static final String DEFAULT_TITLE = "myTitle";

  public static final String DEFAULT_ID = "myId";

  public static final long SAMPLE_A_FREQUENCY = 14000000;

  public static final long SAMPLE_A_MIN_FREQUENCY = 10000000;

  public static final long SAMPLE_A_MAX_FREQUENCY = 20000000;

  public static final int SAMPLE_A_ANGLE = 180;

  public static final String DEFAULT_VERSION = "myVersion";

  public static final String DEFAULT_TYPE = "myType";

  public static final byte[] DEFAULT_BYTES = {8};

  private static final double DEFAULT_TEMPERATURE = 101.5;

  private static final String NUMBER_REVIEWERS_ATTRIBUTE_KEY = "number-reviewers";

  private static final String PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY = "precise-height-meters";

  private static final String REVIEWED_ATTRIBUTE_KEY = "reviewed";

  private static final String PRECISE_LENGTH_METERS_ATTRIBUTE_KEY = "precise-length-meters";

  private static final String DESCRIPTION_ATTRIBUTE_KEY = "description";

  private static final String ROWS_ATTRIBUTE_KEY = "rows";

  private static final String COLUMNS_ATTRIBUTE_KEY = "columns";

  private static final String TEMPERATURE_KEY = "temperature";

  private static final String DEFAULT_DESCRIPTION = "sample description";

  private static final int DEFAULT_ROWS = 100;

  private static final int DEFAULT_COLUMNS = 5;

  private static final String DEFAULT_MODIFIED_DATE = "2012-09-01T00:09:19.368+0000";

  private static final String DEFAULT_CREATED_DATE = "2012-08-01T00:09:19.368+0000";

  private static final String SAMPLE_A_METACARD_TYPE_NAME = "MetacardTypeA";

  private static final String SAMPLE_B_METACARD_TYPE_NAME = "MetacardTypeB";

  private static final String DEFAULT_URI = "http://example.com";

  private static final Object DEFAULT_EXPIRATION_DATE = "2013-09-01T00:09:19.368+0000";

  private static final Object DEFAULT_EFFECTIVE_DATE = "2012-08-15T00:09:19.368+0000";

  private static final String sampleJsonExtensibleA() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"multi-string\":[\"foo\", \"bar\"],"
        + "\"temperature\":\"101.5\","
        + "        \"frequency\":\"14000000\","
        + "        \"min-frequency\":\"10000000\","
        + "        \"max-frequency\":\"20000000\","
        + "        \"angle\":\"180\","
        + "        \"id\":\"myId\","
        + "        \"metacard-type\":\"MetacardTypeA\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"Point\","
        + "        \"coordinates\":["
        + "                30.0,"
        + "                10.0"
        + "        ]"
        + "    }"
        + "}";
  }

  private static final String sampleJsonExtensibleANoMetacardType() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"temperature\":\"101.5\","
        + "        \"frequency\":\"14000000\","
        + "        \"min-frequency\":\"10000000\","
        + "        \"max-frequency\":\"20000000\","
        + "        \"angle\":\"180\","
        + "        \"id\":\"myId\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"Point\","
        + "        \"coordinates\":["
        + "                30.0,"
        + "                10.0"
        + "        ]"
        + "    }"
        + "}";
  }

  private static final String sampleJsonExtensibleAUnregisteredMetacardType() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"temperature\":\"101.5\","
        + "        \"frequency\":\"14000000\","
        + "        \"min-frequency\":\"10000000\","
        + "        \"max-frequency\":\"20000000\","
        + "        \"angle\":\"180\","
        + "        \"id\":\"myId\","
        + "        \"metacard-type\":\"unregistered-type\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"Point\","
        + "        \"coordinates\":["
        + "                30.0,"
        + "                10.0"
        + "        ]"
        + "    }"
        + "}";
  }

  private static final String sampleBasicMetacard() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"id\":\"myId\","
        + "        \"thumbnail\":\"CA==\","
        + "        \"resource-uri\":\"http:\\/\\/example.com\","
        + "        \"created\":\"2012-08-01T00:09:19.368+0000\","
        + "        \"metadata-content-type-version\":\"myVersion\","
        + "        \"metadata-content-type\":\"myType\","
        + "        \"metadata\":\"<xml><\\/xml>\","
        + "        \"temperature\":\"101.5\","
        + "        \"modified\":\"2012-09-01T00:09:19.368+0000\","
        + "        \"effective\":\"2012-08-15T00:09:19.368+0000\","
        + "        \"expiration\":\"2013-09-01T00:09:19.368+0000\","
        + "     \"metadata-target-namespace\":\"targetNamespace\","
        + "        \"metacard-type\":\"ddf.metacard\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"LineString\","
        + "        \"coordinates\":["
        + "            ["
        + "                30.0,"
        + "                10.0"
        + "            ],"
        + "            ["
        + "                10.0,"
        + "                30.0"
        + "            ],"
        + "            ["
        + "                40.0,"
        + "                40.0"
        + "            ]"
        + "        ]"
        + "    }"
        + "}";
  }

  private static final String sampleBasicMetacardNoMetacardType() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"id\":\"myId\","
        + "        \"thumbnail\":\"CA==\","
        + "        \"resource-uri\":\"http:\\/\\/example.com\","
        + "        \"created\":\"2012-08-01T00:09:19.368+0000\","
        + "        \"metadata-content-type-version\":\"myVersion\","
        + "        \"metadata-content-type\":\"myType\","
        + "        \"metadata\":\"<xml><\\/xml>\","
        + "        \"temperature\":\"101.5\","
        + "        \"modified\":\"2012-09-01T00:09:19.368+0000\","
        + "        \"effective\":\"2012-08-15T00:09:19.368+0000\","
        + "        \"expiration\":\"2013-09-01T00:09:19.368+0000\","
        + "     \"metadata-target-namespace\":\"targetNamespace\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"LineString\","
        + "        \"coordinates\":["
        + "            ["
        + "                30.0,"
        + "                10.0"
        + "            ],"
        + "            ["
        + "                10.0,"
        + "                30.0"
        + "            ],"
        + "            ["
        + "                40.0,"
        + "                40.0"
        + "            ]"
        + "        ]"
        + "    }"
        + "}";
  }

  private static final String sampleJsonExtensibleB() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"columns\":\"5\","
        + "        \"rows\":\"100\","
        + "        \"temperature\":\"101.5\","
        + "        \"description\":\"sample description\","
        + "        \"id\":\"myId\","
        + "        \"reviewed\":\"true\","
        + "        \"precise-length-meters\":\""
        + Double.MAX_VALUE
        + "\","
        + "        \"precise-height-meters\":\""
        + Float.MAX_VALUE
        + "\","
        + "        \"number-reviewers\":\""
        + Short.MAX_VALUE
        + "\","
        + "        \"metacard-type\":\"MetacardTypeB\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"Point\","
        + "        \"coordinates\":["
        + "                30.0,"
        + "                10.0"
        + "        ]"
        + "    }"
        + "}";
  }

  private static final String sampleJsonExtensibleBWithBadField() {
    return "{"
        + "    \"properties\":{"
        + "        \"title\":\"myTitle\","
        + "        \"columns\":\"5\","
        + "        \"rows\":\"100\","
        + "        \"description\":\"sample description\","
        + "        \"id\":\"myId\","
        + "        \"reviewed\":\"true\","
        + "\"temperature\":\"101.5\","
        + "        \"precise-length-meters\":\"ThisIsNotADouble\","
        + "        \"precise-height-meters\":\""
        + Float.MAX_VALUE
        + "\","
        + "        \"number-reviewers\":\""
        + Short.MAX_VALUE
        + "\","
        + "        \"metacard-type\":\"MetacardTypeB\""
        + "    },"
        + "    \"type\":\"Feature\","
        + "    \"geometry\":{"
        + "        \"type\":\"Point\","
        + "        \"coordinates\":["
        + "                30.0,"
        + "                10.0"
        + "        ]"
        + "    }"
        + "}";
  }

  @Before
  public void setUp() {
    transformer = new GeoJsonInputTransformer();
    transformer.setMetacardTypes(prepareMetacardTypes());

    AttributeRegistry attributeRegistry = new AttributeRegistryImpl();
    attributeRegistry.register(
        new AttributeDescriptorImpl(TEMPERATURE_KEY, true, true, true, true, DOUBLE_TYPE));

    transformer.setAttributeRegistry(attributeRegistry);
  }

  @Test
  public void testExtensibleGeoJsonA() throws IOException, CatalogTransformerException {
    ByteArrayInputStream geoJsonInput =
        new ByteArrayInputStream(sampleJsonExtensibleA().getBytes());
    Metacard metacard = transformer.transform(geoJsonInput);

    assertEquals(DEFAULT_TITLE, metacard.getTitle());
    assertEquals(DEFAULT_ID, metacard.getId());
    assertTrue(metacard.getAttribute("frequency").getValue() instanceof Long);
    assertEquals(SAMPLE_A_FREQUENCY, (Long) metacard.getAttribute("frequency").getValue(), 0);
    assertTrue(metacard.getAttribute("max-frequency").getValue() instanceof Long);
    assertEquals(
        SAMPLE_A_MAX_FREQUENCY, (Long) metacard.getAttribute("max-frequency").getValue(), 0);
    assertTrue(metacard.getAttribute("min-frequency").getValue() instanceof Long);
    assertEquals(
        SAMPLE_A_MIN_FREQUENCY, (Long) metacard.getAttribute("min-frequency").getValue(), 0);
    assertTrue(metacard.getAttribute("angle").getValue() instanceof Integer);
    assertTrue(SAMPLE_A_ANGLE == (Integer) metacard.getAttribute("angle").getValue());
    assertEquals(SAMPLE_A_METACARD_TYPE_NAME, metacard.getMetacardType().getName());

    assertThat(metacard.getAttribute(TEMPERATURE_KEY).getValue(), is(DEFAULT_TEMPERATURE));
  }

  @Test
  public void testExtensibleGeoJsonB() throws IOException, CatalogTransformerException {
    ByteArrayInputStream geoJsonInput =
        new ByteArrayInputStream(sampleJsonExtensibleB().getBytes());
    Metacard metacard = transformer.transform(geoJsonInput);

    assertEquals(DEFAULT_TITLE, metacard.getTitle());
    assertEquals(DEFAULT_ID, metacard.getId());
    assertTrue(metacard.getAttribute(COLUMNS_ATTRIBUTE_KEY).getValue() instanceof Integer);
    assertTrue(
        DEFAULT_COLUMNS == (Integer) metacard.getAttribute(COLUMNS_ATTRIBUTE_KEY).getValue());
    assertTrue(metacard.getAttribute(ROWS_ATTRIBUTE_KEY).getValue() instanceof Integer);
    assertTrue(DEFAULT_ROWS == (Integer) metacard.getAttribute(ROWS_ATTRIBUTE_KEY).getValue());
    assertTrue(metacard.getAttribute(DESCRIPTION_ATTRIBUTE_KEY).getValue() instanceof String);
    assertEquals(
        DEFAULT_DESCRIPTION, (String) metacard.getAttribute(DESCRIPTION_ATTRIBUTE_KEY).getValue());
    assertEquals(SAMPLE_B_METACARD_TYPE_NAME, metacard.getMetacardType().getName());
    assertTrue(
        metacard.getAttribute(PRECISE_LENGTH_METERS_ATTRIBUTE_KEY).getValue() instanceof Double);
    assertEquals(
        Double.MAX_VALUE,
        (Double) metacard.getAttribute(PRECISE_LENGTH_METERS_ATTRIBUTE_KEY).getValue(),
        0);
    assertTrue(metacard.getAttribute(REVIEWED_ATTRIBUTE_KEY).getValue() instanceof Boolean);
    assertTrue((Boolean) metacard.getAttribute(REVIEWED_ATTRIBUTE_KEY).getValue());
    assertTrue(
        metacard.getAttribute(PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY).getValue() instanceof Float);
    assertEquals(
        Float.MAX_VALUE,
        (Float) metacard.getAttribute(PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY).getValue(),
        0);
    assertTrue(metacard.getAttribute(NUMBER_REVIEWERS_ATTRIBUTE_KEY).getValue() instanceof Short);
    assertEquals(
        Short.MAX_VALUE,
        (Short) metacard.getAttribute(NUMBER_REVIEWERS_ATTRIBUTE_KEY).getValue(),
        0);

    assertThat(metacard.getAttribute(TEMPERATURE_KEY).getValue(), is(DEFAULT_TEMPERATURE));

    // test that other attributes not contained in MetacardTypeB are not in resulting metacard
    assertNull(metacard.getAttribute("created"));
    assertNull(metacard.getMetadata());
  }

  @Test
  public void testExtensibleGeoJsonBNonParseableField()
      throws IOException, CatalogTransformerException {
    ByteArrayInputStream geoJsonInput =
        new ByteArrayInputStream(sampleJsonExtensibleBWithBadField().getBytes());
    Metacard metacard = transformer.transform(geoJsonInput);

    // all sample B fields should be added to metacard except the non-parseable one
    assertEquals(DEFAULT_TITLE, metacard.getTitle());
    assertEquals(DEFAULT_ID, metacard.getId());
    assertTrue(metacard.getAttribute(COLUMNS_ATTRIBUTE_KEY).getValue() instanceof Integer);
    assertTrue(
        DEFAULT_COLUMNS == (Integer) metacard.getAttribute(COLUMNS_ATTRIBUTE_KEY).getValue());
    assertTrue(metacard.getAttribute(ROWS_ATTRIBUTE_KEY).getValue() instanceof Integer);
    assertTrue(DEFAULT_ROWS == (Integer) metacard.getAttribute(ROWS_ATTRIBUTE_KEY).getValue());
    assertTrue(metacard.getAttribute(DESCRIPTION_ATTRIBUTE_KEY).getValue() instanceof String);
    assertEquals(
        DEFAULT_DESCRIPTION, (String) metacard.getAttribute(DESCRIPTION_ATTRIBUTE_KEY).getValue());
    assertEquals(SAMPLE_B_METACARD_TYPE_NAME, metacard.getMetacardType().getName());
    assertTrue(metacard.getAttribute(REVIEWED_ATTRIBUTE_KEY).getValue() instanceof Boolean);
    assertTrue((Boolean) metacard.getAttribute(REVIEWED_ATTRIBUTE_KEY).getValue());
    assertTrue(
        metacard.getAttribute(PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY).getValue() instanceof Float);
    assertEquals(
        Float.MAX_VALUE,
        (Float) metacard.getAttribute(PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY).getValue(),
        0);
    assertTrue(metacard.getAttribute(NUMBER_REVIEWERS_ATTRIBUTE_KEY).getValue() instanceof Short);
    assertEquals(
        Short.MAX_VALUE,
        (Short) metacard.getAttribute(NUMBER_REVIEWERS_ATTRIBUTE_KEY).getValue(),
        0);

    assertThat(metacard.getAttribute(TEMPERATURE_KEY).getValue(), is(DEFAULT_TEMPERATURE));

    assertNull(metacard.getAttribute(PRECISE_LENGTH_METERS_ATTRIBUTE_KEY));
  }

  @Test
  public void testExtensibleGeoJsonNoMetacardType()
      throws IOException, CatalogTransformerException {
    ByteArrayInputStream geoJsonInput =
        new ByteArrayInputStream(sampleJsonExtensibleANoMetacardType().getBytes());
    Metacard metacard = transformer.transform(geoJsonInput);

    // since no metacard type was specified only the Basic Metacard Type attributes should be
    // available. These are defined in MetacardImpl.BASIC_METACARD
    // none of the custom attributes defined in SampleMetacardTypeA will be added to the
    // metacard.
    assertEquals(DEFAULT_TITLE, metacard.getTitle());
    assertEquals(DEFAULT_ID, metacard.getId());
    assertEquals(MetacardImpl.BASIC_METACARD.getName(), metacard.getMetacardType().getName());

    assertEquals(DEFAULT_TEMPERATURE, metacard.getAttribute(TEMPERATURE_KEY).getValue());

    assertNull(metacard.getAttribute("frequency"));
    assertNull(metacard.getAttribute("max-frequency"));
    assertNull(metacard.getAttribute("min-frequency"));
    assertNull(metacard.getAttribute("angle"));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testExtensibleGeoJsonUnregisteredMetacardType()
      throws IOException, CatalogTransformerException {
    ByteArrayInputStream geoJsonInput =
        new ByteArrayInputStream(sampleJsonExtensibleAUnregisteredMetacardType().getBytes());
    transformer.transform(geoJsonInput);
  }

  @Test
  public void testBasicMetacardType()
      throws IOException, CatalogTransformerException, ParseException {
    ByteArrayInputStream geoJsonInput = new ByteArrayInputStream(sampleBasicMetacard().getBytes());
    Metacard metacard = transformer.transform(geoJsonInput);

    verifyBasics(metacard);
  }

  @Test
  public void testBasicMetacardTypeNoMetacardType()
      throws IOException, CatalogTransformerException, ParseException {
    ByteArrayInputStream geoJsonInput =
        new ByteArrayInputStream(sampleBasicMetacardNoMetacardType().getBytes());
    Metacard metacard = transformer.transform(geoJsonInput);

    verifyBasics(metacard);
  }

  @Test
  public void testTransformWithoutIdParam() throws IOException, CatalogTransformerException {
    Metacard metacard =
        transformer.transform(new ByteArrayInputStream(sampleJsonExtensibleA().getBytes()));

    assertThat(metacard.getMetacardType().getName(), is("MetacardTypeA"));
    assertThat(metacard.getId(), is(DEFAULT_ID));
  }

  @Test
  public void testNonEmptyPropertyType() throws IOException, CatalogTransformerException {
    Metacard metacard =
        transformer.transform(
            new ByteArrayInputStream(sampleJsonExtensibleB().getBytes()), "customIdTest");

    assertThat(metacard.getMetacardType().getName(), is("MetacardTypeB"));
    assertThat(metacard.getId(), is("customIdTest"));
  }

  @Test
  public void testEmptyPropertyTypeWithNonNullTransformers()
      throws IOException, CatalogTransformerException {
    transformer.setInputTransformers(new SortedServiceList());
    Metacard metacard =
        transformer.transform(
            new ByteArrayInputStream(sampleBasicMetacardNoMetacardType().getBytes()), DEFAULT_ID);

    assertThat(metacard.getMetacardType().getName(), is("ddf.metacard"));
    assertThat(metacard.getTitle(), is(DEFAULT_TITLE));
  }

  @Test
  public void testNullMetacardTypesWithNonNullTransformers()
      throws IOException, CatalogTransformerException {
    transformer.setInputTransformers(new SortedServiceList());
    // tests the branch conditional for null metacard types in the getMetacard() method
    transformer.setMetacardTypes(null);
    Metacard metacard1 =
        transformer.transform(
            new ByteArrayInputStream(sampleBasicMetacardNoMetacardType().getBytes()), DEFAULT_ID);

    assertThat(metacard1.getMetacardType().getName(), is("ddf.metacard"));
    assertThat(metacard1.getTitle(), is(DEFAULT_TITLE));

    transformer.setMetacardTypes(prepareMetacardTypes());
    Metacard metacard2 =
        transformer.transform(
            new ByteArrayInputStream(sampleBasicMetacardNoMetacardType().getBytes()), DEFAULT_ID);

    // tests that metacards created with null metacard types and empty property types are the same
    assertThat(metacard2, is(metacard1));
  }

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testInvalidMetacardType() throws IOException, CatalogTransformerException {
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage(
        "MetacardType specified in input has not been registered with the system. Cannot parse input. MetacardType name:");

    MetacardType invalidMetacardType = new MetacardTypeImpl("invalid", new HashSet<>());
    transformer.setMetacardTypes(Collections.singletonList(invalidMetacardType));
    transformer.transform(new ByteArrayInputStream(sampleBasicMetacard().getBytes()), DEFAULT_ID);
  }

  @Test
  public void testTransformNullInput() throws IOException, CatalogTransformerException {
    thrown.expect(CatalogTransformerException.class);
    thrown.expectMessage("Cannot transform null input.");

    transformer.transform(null);
  }

  private List<MetacardType> prepareMetacardTypes() {
    return Arrays.asList(sampleMetacardTypeA(), sampleMetacardTypeB(), MetacardImpl.BASIC_METACARD);
  }

  private void verifyBasics(Metacard metacard) throws ParseException {
    assertEquals(DEFAULT_TITLE, metacard.getTitle());
    assertEquals(DEFAULT_URI, metacard.getResourceURI().toString());
    assertEquals(DEFAULT_TYPE, metacard.getContentTypeName());
    assertEquals(DEFAULT_VERSION, metacard.getContentTypeVersion());
    assertEquals("<xml></xml>", metacard.getMetadata());
    SimpleDateFormat dateFormat =
        new SimpleDateFormat(GeoJsonInputTransformer.ISO_8601_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(DEFAULT_CREATED_DATE, dateFormat.format(metacard.getCreatedDate()));
    assertEquals(DEFAULT_MODIFIED_DATE, dateFormat.format(metacard.getModifiedDate()));
    assertEquals(DEFAULT_EXPIRATION_DATE, dateFormat.format(metacard.getExpirationDate()));
    assertEquals(DEFAULT_EFFECTIVE_DATE, dateFormat.format(metacard.getEffectiveDate()));
    assertArrayEquals(DEFAULT_BYTES, metacard.getThumbnail());
    assertEquals(DEFAULT_TEMPERATURE, metacard.getAttribute(TEMPERATURE_KEY).getValue());
    assertEquals(MetacardImpl.BASIC_METACARD.getName(), metacard.getMetacardType().getName());

    WKTReader reader = new WKTReader();

    Geometry geometry = reader.read(metacard.getLocation());

    Coordinate[] coords = geometry.getCoordinates();

    assertThat(coords[0].x, is(30.0));
    assertThat(coords[0].y, is(10.0));

    assertThat(coords[1].x, is(10.0));
    assertThat(coords[1].y, is(30.0));

    assertThat(coords[2].x, is(40.0));
    assertThat(coords[2].y, is(40.0));
  }

  private MetacardType sampleMetacardTypeA() {
    Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
    descriptors.add(
        new AttributeDescriptorImpl(
            "frequency",
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            "min-frequency",
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            "max-frequency",
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            "angle",
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            "multi-string",
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Metacard.ID,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Metacard.TITLE,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    return new MetacardTypeImpl(SAMPLE_A_METACARD_TYPE_NAME, descriptors);
  }

  private MetacardType sampleMetacardTypeB() {
    Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
    descriptors.add(
        new AttributeDescriptorImpl(
            COLUMNS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            ROWS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            DESCRIPTION_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Metacard.ID,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Metacard.TITLE,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            REVIEWED_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.BOOLEAN_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            PRECISE_LENGTH_METERS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.DOUBLE_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.FLOAT_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            NUMBER_REVIEWERS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.SHORT_TYPE));

    return new MetacardTypeImpl("MetacardTypeB", descriptors);
  }
}
