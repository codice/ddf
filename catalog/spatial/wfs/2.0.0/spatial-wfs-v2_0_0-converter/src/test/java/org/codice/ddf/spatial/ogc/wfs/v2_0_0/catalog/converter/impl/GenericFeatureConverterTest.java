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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.EnhancedStaxDriver;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlGeometryConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20FeatureCollection;
import org.junit.Ignore;
import org.junit.Test;

public class GenericFeatureConverterTest {

  private static final String FEATURE_TYPE = "video_data_set";

  private static final String SOURCE_ID = "WFS_2_0";

  private static final String GML = "GML";

  private static final String PROPERTY_PREFIX = FEATURE_TYPE + ".";

  private static final String ID_ELEMENT = "id";

  private static final String FILENAME_ELEMENT = "filename";

  private static final String VERSION_ELEMENT = "version";

  private static final String END_DATE_ELEMENT = "end_date";

  private static final String HEIGHT_ELEMENT = "height";

  private static final String INDEX_ID_ELEMENT = "index_id";

  private static final String OTHER_TAGS_XML_ELEMENT = "other_tags_xml";

  private static final String REPOSITORY_ID_ELEMENT = "repository_id";

  private static final String START_DATE_ELEMENT = "start_date";

  private static final String STYLE_ID_ELEMENT = "style_id";

  private static final String WIDTH_ELEMENT = "width";

  private static final String GROUND_GEOM_ELEMENT = "ground_geom";

  private static final String STATES_TITLE_ELEMENT = "STATE_NAME";

  private static final String STATES_FEATURE_TYPE = "states";

  @Test
  @Ignore // DDF-733
  public void testUnmarshalSingleFeatureXmlToObject() {
    XStream xstream = new XStream(new WstxDriver());

    MetacardType metacardType = buildMetacardType();
    GenericFeatureConverterWfs20 converter = new GenericFeatureConverterWfs20();
    converter.setMetacardType(buildMetacardType());

    converter.setSourceId(SOURCE_ID);
    converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
    xstream.registerConverter(converter);
    xstream.registerConverter(new GmlGeometryConverter());

    xstream.alias(FEATURE_TYPE, MetacardImpl.class);
    InputStream is = GenericFeatureConverterTest.class.getResourceAsStream("/video_data_set.xml");
    Metacard mc = (Metacard) xstream.fromXML(is);

    assertEquals("video_data_set.2", mc.getId());
    assertEquals(FEATURE_TYPE, mc.getContentTypeName());
    assertEquals(metacardType.getName(), mc.getMetacardType().getName());
    assertEquals(SOURCE_ID, mc.getSourceId());
    assertEquals("video_data_set.2", mc.getTitle());

    assertEquals(2L, mc.getAttribute(PROPERTY_PREFIX + ID_ELEMENT).getValue());
    assertEquals(Long.valueOf(1L), mc.getAttribute(PROPERTY_PREFIX + VERSION_ELEMENT).getValue());
    assertEquals(
        DatatypeConverter.parseDateTime("2005-04-07T09:54:38.983").getTime(),
        mc.getAttribute(PROPERTY_PREFIX + END_DATE_ELEMENT).getValue());
    assertEquals(
        "/data/test_suite/video/video/videoFile.mpg",
        mc.getAttribute(PROPERTY_PREFIX + FILENAME_ELEMENT).getValue());
    assertEquals(720L, mc.getAttribute(PROPERTY_PREFIX + HEIGHT_ELEMENT).getValue());
    assertEquals(
        "a8a55092f0afae881099637ef7746cd8d7066270d9af4cf0f52c41dab53c4005",
        mc.getAttribute(PROPERTY_PREFIX + INDEX_ID_ELEMENT).getValue());
    assertEquals(
        getOtherTagsXml(), mc.getAttribute(PROPERTY_PREFIX + OTHER_TAGS_XML_ELEMENT).getValue());
    assertEquals(26L, mc.getAttribute(PROPERTY_PREFIX + REPOSITORY_ID_ELEMENT).getValue());
    assertEquals(
        DatatypeConverter.parseDateTime("2005-04-07T09:53:39.000").getTime(),
        mc.getAttribute(PROPERTY_PREFIX + START_DATE_ELEMENT).getValue());
    assertEquals(1280L, mc.getAttribute(PROPERTY_PREFIX + WIDTH_ELEMENT).getValue());

    assertEquals(getLocation(), mc.getLocation());
    assertEquals(
        mc.getLocation(), mc.getAttribute(PROPERTY_PREFIX + GROUND_GEOM_ELEMENT).getValue());

    assertNotNull(mc.getAttribute(Core.CREATED).getValue());
    assertNotNull(mc.getEffectiveDate());
    assertNotNull(mc.getAttribute(Core.MODIFIED).getValue());

    assertNotNull(mc.getContentTypeNamespace());
    assertEquals(
        mc.getContentTypeNamespace().toString(),
        Wfs20Constants.NAMESPACE_URN_ROOT + metacardType.getName());
  }

  @Test
  public void testUnmarshalFeatureCollectionXmlToObject() {
    XStream xstream = new XStream(new WstxDriver());
    FeatureCollectionConverterWfs20 fcConverter = new FeatureCollectionConverterWfs20();
    Map<String, FeatureConverter> fcMap = new HashMap<String, FeatureConverter>();

    GenericFeatureConverterWfs20 converter = new GenericFeatureConverterWfs20();

    fcMap.put("video_data_set", converter);
    fcConverter.setFeatureConverterMap(fcMap);

    xstream.registerConverter(fcConverter);

    converter.setMetacardType(buildMetacardType());
    converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
    xstream.registerConverter(converter);
    xstream.registerConverter(new GmlGeometryConverter());
    xstream.alias("FeatureCollection", Wfs20FeatureCollection.class);
    InputStream is =
        GenericFeatureConverterTest.class.getResourceAsStream("/video_data_set_collection.xml");
    Wfs20FeatureCollection wfc = (Wfs20FeatureCollection) xstream.fromXML(is);
    assertEquals(4, wfc.getMembers().size());
    Metacard mc = wfc.getMembers().get(0);
    assertEquals(mc.getId(), "video_data_set.1");
  }

  @Test
  public void testUnmarshalMultiQueryFeatureCollectionXmlToObject() {
    XStream xstream = new XStream(new WstxDriver());
    FeatureCollectionConverterWfs20 fcConverter = new FeatureCollectionConverterWfs20();
    Map<String, FeatureConverter> fcMap = new HashMap<String, FeatureConverter>();

    GenericFeatureConverterWfs20 converter = new GenericFeatureConverterWfs20();

    fcMap.put("states", converter);
    fcMap.put("streams", converter);
    fcConverter.setFeatureConverterMap(fcMap);

    xstream.registerConverter(fcConverter);

    converter.setMetacardType(buildStatesMetacardType());
    converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
    xstream.registerConverter(converter);
    xstream.alias("FeatureCollection", Wfs20FeatureCollection.class);
    InputStream is = GenericFeatureConverterTest.class.getResourceAsStream("/geoserver_sample.xml");
    Wfs20FeatureCollection wfc = (Wfs20FeatureCollection) xstream.fromXML(is);
    assertEquals(7, wfc.getMembers().size());
    Metacard mc = wfc.getMembers().get(0);

    assertEquals(mc.getId(), "states.10");

    // Verifies that lat/lon was swapped to lon/lat order for the WKT conversion
    // to set the metacard's location
    assertTrue(
        mc.getLocation()
            .startsWith(
                "MULTIPOLYGON (((-89.104965 36.953869, -89.129585 36.86644, -89.166496 36.843422000000004,"));
  }

  @Test
  public void testGeoServerLatLonSwappingForMultiPolygon() {
    XStream xstream = new XStream(new WstxDriver());
    FeatureCollectionConverterWfs20 fcConverter = new FeatureCollectionConverterWfs20();
    Map<String, FeatureConverter> fcMap = new HashMap<String, FeatureConverter>();

    GenericFeatureConverterWfs20 converter = new GenericFeatureConverterWfs20();

    fcMap.put("states", converter);
    fcMap.put("streams", converter);
    fcConverter.setFeatureConverterMap(fcMap);

    xstream.registerConverter(fcConverter);

    converter.setMetacardType(buildStatesMetacardType());
    converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
    xstream.registerConverter(converter);
    xstream.alias("FeatureCollection", Wfs20FeatureCollection.class);
    InputStream is =
        GenericFeatureConverterTest.class.getResourceAsStream("/geoserver_sample_polygon.xml");
    Wfs20FeatureCollection wfc = (Wfs20FeatureCollection) xstream.fromXML(is);
    assertEquals(1, wfc.getMembers().size());
    Metacard mc = wfc.getMembers().get(0);

    assertEquals(mc.getId(), "states.10");

    // Verifies that lat/lon was swapped to lon/lat order for the WKT conversion
    // to set the metacard's location
    assertTrue(
        mc.getLocation()
            .startsWith(
                "MULTIPOLYGON (((-89.1 36.1, -89.1 37.1, -88.1 37.1, -88.1 36.1, -89.1 36.1"));
  }

  @Test
  public void testGeoServerLatLonSwappingForPoint() {
    XStream xstream = new XStream(new WstxDriver());
    FeatureCollectionConverterWfs20 fcConverter = new FeatureCollectionConverterWfs20();
    Map<String, FeatureConverter> fcMap = new HashMap<String, FeatureConverter>();

    GenericFeatureConverterWfs20 converter = new GenericFeatureConverterWfs20();

    fcMap.put("states", converter);
    fcMap.put("streams", converter);
    fcConverter.setFeatureConverterMap(fcMap);

    xstream.registerConverter(fcConverter);

    converter.setMetacardType(buildStatesMetacardType());
    converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
    xstream.registerConverter(converter);
    xstream.alias("FeatureCollection", Wfs20FeatureCollection.class);
    InputStream is =
        GenericFeatureConverterTest.class.getResourceAsStream("/geoserver_sample_point.xml");
    Wfs20FeatureCollection wfc = (Wfs20FeatureCollection) xstream.fromXML(is);
    assertEquals(1, wfc.getMembers().size());
    Metacard mc = wfc.getMembers().get(0);

    assertEquals(mc.getId(), "states.10");

    // Verifies that lat/lon was swapped to lon/lat order for the WKT conversion
    // to set the metacard's location
    assertTrue(mc.getLocation().startsWith("POINT (-123.26 49.41)"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnmarshalNoMetacardTypeRegisteredInConverter() throws Throwable {
    XStream xstream = new XStream(new WstxDriver());
    xstream.registerConverter(new GenericFeatureConverterWfs20());
    xstream.registerConverter(new GmlGeometryConverter());
    xstream.alias(FEATURE_TYPE, Metacard.class);
    InputStream is = GenericFeatureConverterTest.class.getResourceAsStream("/video_data_set.xml");
    try {
      xstream.fromXML(is);
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test
  public void testMetacardCollectionToFeatureCollectionXml() {

    XStream xstream = new XStream(new EnhancedStaxDriver());

    xstream.setMode(XStream.NO_REFERENCES);
    xstream.registerConverter(new FeatureCollectionConverterWfs20());
    xstream.registerConverter(new GenericFeatureConverterWfs20());
    xstream.registerConverter(new GmlGeometryConverter());
    // Required the Implementing class. The interface would not work...
    xstream.alias(
        Wfs20Constants.WFS_NAMESPACE_PREFIX + ":" + "FeatureCollection",
        Wfs20FeatureCollection.class);

    Metacard mc = new SampleMetacard().getMetacard();
    Wfs20FeatureCollection wfc = new Wfs20FeatureCollection();
    wfc.getMembers().add(mc);
    MetacardImpl mc2 = new SampleMetacard().getMetacard();
    // Ignore the hack stuff, this was just to imitate having two different
    // "MetacardTypes"
    mc2.setType(
        new MetacardType() {

          @Override
          public String getName() {
            return "otherType";
          }

          @Override
          public Set<AttributeDescriptor> getAttributeDescriptors() {
            return MetacardImpl.BASIC_METACARD.getAttributeDescriptors();
          }

          @Override
          public AttributeDescriptor getAttributeDescriptor(String arg0) {
            return MetacardImpl.BASIC_METACARD.getAttributeDescriptor(arg0);
          }
        });
    wfc.getMembers().add(mc2);

    String xml = xstream.toXML(wfc);
  }

  @Test
  @Ignore // DDF-733
  public void testReadCdata() {
    XStream xstream = new XStream(new WstxDriver());
    String contents = "<tag>my cdata contents</tag>";
    String xml = "<string><![CDATA[" + contents + "]]></string>";
    String results = (String) xstream.fromXML(xml);

    assertEquals(contents, results);
  }

  /*
   * This test will check is the MetacardMapper maps the feature value of 'states.STATE_NAME' to the metacard property 'title'.
   */
  @Test
  public void testUnmarshalMultiQueryFeatureCollectionXmlToObjectWithMetacardMapper() {
    // Create Metacard Mapper
    String featureProp = "ext.states.STATE_NAME";
    String metacardAttr = "title";
    MetacardMapper metacardMapper = mock(MetacardMapper.class);
    when(metacardMapper.getMetacardAttribute(featureProp)).thenReturn(metacardAttr);

    XStream xstream = new XStream(new WstxDriver());
    FeatureCollectionConverterWfs20 fcConverter = new FeatureCollectionConverterWfs20();
    Map<String, FeatureConverter> fcMap = new HashMap<String, FeatureConverter>();

    GenericFeatureConverterWfs20 converter = new GenericFeatureConverterWfs20(metacardMapper);

    fcMap.put("states", converter);
    fcMap.put("streams", converter);
    fcConverter.setFeatureConverterMap(fcMap);

    xstream.registerConverter(fcConverter);

    converter.setMetacardType(buildStatesMetacardType());
    converter.setCoordinateOrder(GeospatialUtil.LAT_LON_ORDER);
    xstream.registerConverter(converter);
    xstream.alias("FeatureCollection", Wfs20FeatureCollection.class);
    InputStream is = GenericFeatureConverterTest.class.getResourceAsStream("/geoserver_sample.xml");
    Wfs20FeatureCollection wfc = (Wfs20FeatureCollection) xstream.fromXML(is);
    assertEquals(7, wfc.getMembers().size());
    Metacard mc = wfc.getMembers().get(0);
    assertEquals(mc.getTitle(), "Missouri");

    // Verifies that lat/lon was swapped to lon/lat order for the WKT conversion
    // to set the metacard's location
    assertTrue(
        mc.getLocation()
            .startsWith(
                "MULTIPOLYGON (((-89.104965 36.953869, -89.129585 36.86644, -89.166496 36.843422000000004,"));
  }

  private MetacardType buildMetacardType() {

    XmlSchema schema = new XmlSchema();
    schema.getElements().putAll(buildElementMap(schema));

    return new FeatureMetacardType(
        schema, new QName(FEATURE_TYPE), new ArrayList<String>(), Wfs20Constants.GML_3_2_NAMESPACE);
  }

  private Map<QName, XmlSchemaElement> buildElementMap(XmlSchema schema) {
    Map<QName, XmlSchemaElement> elementMap = new HashMap<QName, XmlSchemaElement>();
    elementMap.put(
        new QName(ID_ELEMENT), buildSchemaElement(ID_ELEMENT, schema, Constants.XSD_LONG));
    elementMap.put(
        new QName(VERSION_ELEMENT),
        buildSchemaElement(VERSION_ELEMENT, schema, Constants.XSD_LONG));
    elementMap.put(
        new QName(END_DATE_ELEMENT),
        buildSchemaElement(END_DATE_ELEMENT, schema, Constants.XSD_DATETIME));
    elementMap.put(
        new QName(FILENAME_ELEMENT),
        buildSchemaElement(FILENAME_ELEMENT, schema, Constants.XSD_STRING));
    elementMap.put(
        new QName(HEIGHT_ELEMENT), buildSchemaElement(HEIGHT_ELEMENT, schema, Constants.XSD_LONG));
    elementMap.put(
        new QName(INDEX_ID_ELEMENT),
        buildSchemaElement(INDEX_ID_ELEMENT, schema, Constants.XSD_STRING));
    elementMap.put(
        new QName(OTHER_TAGS_XML_ELEMENT),
        buildSchemaElement(OTHER_TAGS_XML_ELEMENT, schema, Constants.XSD_STRING));
    elementMap.put(
        new QName(REPOSITORY_ID_ELEMENT),
        buildSchemaElement(REPOSITORY_ID_ELEMENT, schema, Constants.XSD_LONG));
    elementMap.put(
        new QName(START_DATE_ELEMENT),
        buildSchemaElement(START_DATE_ELEMENT, schema, Constants.XSD_DATETIME));
    elementMap.put(
        new QName(STYLE_ID_ELEMENT),
        buildSchemaElement(STYLE_ID_ELEMENT, schema, Constants.XSD_DECIMAL));
    elementMap.put(
        new QName(WIDTH_ELEMENT), buildSchemaElement(WIDTH_ELEMENT, schema, Constants.XSD_LONG));

    XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
    gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
    gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
    gmlElement.setName(GROUND_GEOM_ELEMENT);
    elementMap.put(new QName(GROUND_GEOM_ELEMENT), gmlElement);

    return elementMap;
  }

  private MetacardType buildStatesMetacardType() {

    XmlSchema schema = new XmlSchema();
    schema.getElements().putAll(buildStatesElementMap(schema));

    return new FeatureMetacardType(
        schema,
        new QName(STATES_FEATURE_TYPE),
        new ArrayList<String>(),
        Wfs20Constants.GML_3_2_NAMESPACE);
  }

  private Map<QName, XmlSchemaElement> buildStatesElementMap(XmlSchema schema) {
    Map<QName, XmlSchemaElement> elementMap = new HashMap<QName, XmlSchemaElement>();

    elementMap.put(
        new QName(STATES_TITLE_ELEMENT),
        buildSchemaElement(STATES_TITLE_ELEMENT, schema, Constants.XSD_STRING));

    XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
    gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
    gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
    gmlElement.setName("the_geom");
    elementMap.put(new QName("the_geom"), gmlElement);

    return elementMap;
  }

  private XmlSchemaElement buildSchemaElement(
      String elementName, XmlSchema schema, QName typeName) {
    XmlSchemaElement element = new XmlSchemaElement(schema, true);
    element.setSchemaType(new XmlSchemaSimpleType(schema, false));
    element.setSchemaTypeName(typeName);
    element.setName(elementName);

    return element;
  }

  private String getOtherTagsXml() {
    return "<metadata>metadata goes here...</metadata>";
  }

  private String getLocation() {
    return "POLYGON ((117.6552810668945 -30.92013931274414, 117.661361694336 -30.92383384704589, 117.6666412353516 -30.93005561828613, "
        + "117.6663589477539 -30.93280601501464, 117.6594467163086 -30.93186187744141, 117.6541137695312 -30.93780517578125, "
        + "117.6519470214844 -30.94397163391114, 117.6455535888672 -30.94255638122559, 117.6336364746094 -30.93402862548828, "
        + "117.6355285644531 -30.92874908447266, 117.6326370239258 -30.92138862609864, 117.6395568847656 -30.92236137390137, "
        + "117.6433029174805 -30.91708374023438, 117.6454467773437 -30.91711044311523, 117.6484985351563 -30.92061042785645, "
        + "117.6504135131836 -30.92061042785645, 117.6504440307617 -30.91638946533203, 117.6552810668945 -30.92013931274414))";
  }
}
