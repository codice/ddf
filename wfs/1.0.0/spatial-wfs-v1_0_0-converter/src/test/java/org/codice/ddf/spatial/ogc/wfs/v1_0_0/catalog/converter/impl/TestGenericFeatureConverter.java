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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.converter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.EnhancedStaxDriver;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GenericFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlGeometryConverter;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants;
import org.junit.Ignore;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.WstxDriver;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

public class TestGenericFeatureConverter {

    private static final String FEATURE_TYPE = "video_data_set";

    private static final String SOURCE_ID = "WFS";

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

    @Test
    @Ignore  //DDF-733
    public void testUnmarshalSingleFeatureXmlToObject() {
        XStream xstream = new XStream(new WstxDriver());

        MetacardType metacardType = buildMetacardType();
        GenericFeatureConverter converter = new GenericFeatureConverter();
        converter.setMetacardType(buildMetacardType());

        converter.setSourceId(SOURCE_ID);
        xstream.registerConverter(converter);
        xstream.registerConverter(new GmlGeometryConverter());

        xstream.alias(FEATURE_TYPE, MetacardImpl.class);
        InputStream is = TestGenericFeatureConverter.class
                .getResourceAsStream("/video_data_set.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertEquals("video_data_set.2", mc.getId());
        assertEquals(FEATURE_TYPE, mc.getContentTypeName());
        assertEquals(metacardType.getName(), mc.getMetacardType().getName());
        assertEquals(SOURCE_ID, mc.getSourceId());
        assertEquals("video_data_set.2", mc.getTitle());

        assertEquals(2L, mc.getAttribute(PROPERTY_PREFIX + ID_ELEMENT).getValue());
        assertEquals(Long.valueOf(1L), mc.getAttribute(PROPERTY_PREFIX + VERSION_ELEMENT)
                .getValue());
        assertEquals(DatatypeConverter.parseDateTime("2005-04-07T09:54:38.983").getTime(), mc
                .getAttribute(PROPERTY_PREFIX + END_DATE_ELEMENT).getValue());
        assertEquals("/data/test_suite/video/video/videoFile.mpg",
                mc.getAttribute(PROPERTY_PREFIX + FILENAME_ELEMENT).getValue());
        assertEquals(720L, mc.getAttribute(PROPERTY_PREFIX + HEIGHT_ELEMENT).getValue());
        assertEquals("a8a55092f0afae881099637ef7746cd8d7066270d9af4cf0f52c41dab53c4005", mc
                .getAttribute(PROPERTY_PREFIX + INDEX_ID_ELEMENT).getValue());
        assertEquals(getOtherTagsXml(), mc.getAttribute(PROPERTY_PREFIX + OTHER_TAGS_XML_ELEMENT)
                .getValue());
        assertEquals(26L, mc.getAttribute(PROPERTY_PREFIX + REPOSITORY_ID_ELEMENT).getValue());
        assertEquals(DatatypeConverter.parseDateTime("2005-04-07T09:53:39.000").getTime(), mc
                .getAttribute(PROPERTY_PREFIX + START_DATE_ELEMENT).getValue());
        assertEquals(1280L, mc.getAttribute(PROPERTY_PREFIX + WIDTH_ELEMENT).getValue());

        assertEquals(getLocation(), mc.getLocation());
        assertEquals(mc.getLocation(), mc.getAttribute(PROPERTY_PREFIX + GROUND_GEOM_ELEMENT)
                .getValue());

        assertNotNull(mc.getCreatedDate());
        assertNotNull(mc.getEffectiveDate());
        assertNotNull(mc.getModifiedDate());

        assertNotNull(mc.getContentTypeNamespace());
        assertEquals(mc.getContentTypeNamespace().toString(), WfsConstants.NAMESPACE_URN_ROOT
                + metacardType.getName());
    }

    @Test
    public void testUnmarshalFeatureCollectionXmlToObject() {
        XStream xstream = new XStream(new WstxDriver());
        FeatureCollectionConverterWfs10 fcConverter = new FeatureCollectionConverterWfs10();
        Map<String, FeatureConverter> fcMap = new HashMap<String, FeatureConverter>();

        GenericFeatureConverter converter = new GenericFeatureConverter();

        fcMap.put("video_data_set", converter);
        fcConverter.setFeatureConverterMap(fcMap);

        xstream.registerConverter(fcConverter);

        converter.setMetacardType(buildMetacardType());
        xstream.registerConverter(converter);
        xstream.registerConverter(new GmlGeometryConverter());
        xstream.alias("FeatureCollection", WfsFeatureCollection.class);
        InputStream is = TestGenericFeatureConverter.class
                .getResourceAsStream("/video_data_set_collection.xml");

        WfsFeatureCollection wfc = (WfsFeatureCollection) xstream.fromXML(is);
        assertEquals(4, wfc.getFeatureMembers().size());
        Metacard mc = wfc.getFeatureMembers().get(0);
        assertEquals(mc.getId(), "video_data_set.1");

    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnmarshalNoMetacardTypeRegisteredInConverter() throws Throwable {
        XStream xstream = new XStream(new WstxDriver());
        xstream.registerConverter(new GenericFeatureConverter());
        xstream.registerConverter(new GmlGeometryConverter());
        xstream.alias(FEATURE_TYPE, Metacard.class);
        InputStream is = TestGenericFeatureConverter.class
                .getResourceAsStream("/video_data_set.xml");
        try {
            WfsFeatureCollection wfs = (WfsFeatureCollection) xstream.fromXML(is);
        } catch (Exception e) {
            throw e.getCause();
        }
    }

    @Test
    public void testMetacardCollectionToFeatureCollectionXml() {

        XStream xstream = new XStream(new EnhancedStaxDriver());

        xstream.setMode(XStream.NO_REFERENCES);
        xstream.registerConverter(new FeatureCollectionConverterWfs10());
        xstream.registerConverter(new GenericFeatureConverter());
        xstream.registerConverter(new GmlGeometryConverter());
        // Required the Implementing class. The interface would not work...
        xstream.alias("wfs:FeatureCollection", WfsFeatureCollection.class);

        Metacard mc = new SampleMetacard().getMetacard();
        WfsFeatureCollection wfc = new WfsFeatureCollection();
        wfc.getFeatureMembers().add(mc);
        MetacardImpl mc2 = new SampleMetacard().getMetacard();
        // Ignore the hack stuff, this was just to imitate having two different
        // "MetacardTypes"
        mc2.setType(new MetacardType() {

            @Override
            public String getName() {
                return "otherType";
            }

            @Override
            public Set<AttributeDescriptor> getAttributeDescriptors() {
                return BasicTypes.BASIC_METACARD.getAttributeDescriptors();
            }

            @Override
            public AttributeDescriptor getAttributeDescriptor(String arg0) {
                return BasicTypes.BASIC_METACARD.getAttributeDescriptor(arg0);
            }
        });
        wfc.getFeatureMembers().add(mc2);

        String xml = xstream.toXML(wfc);
    }

    @Test
    @Ignore  //DDF-733
    public void testReadCdata() {
        XStream xstream = new XStream(new WstxDriver());
        String contents = "<tag>my cdata contents</tag>";
        String xml = "<string><![CDATA[" + contents + "]]></string>";
        String results = (String) xstream.fromXML(xml);

        assertEquals(contents, results);
    }

    private MetacardType buildMetacardType() {

        XmlSchema schema = new XmlSchema();
        schema.getElements().putAll(buildElementMap(schema));

        return new FeatureMetacardType(schema, new QName(FEATURE_TYPE), new ArrayList<String>(), Wfs10Constants.GML_NAMESPACE);

    }

    private Map<QName, XmlSchemaElement> buildElementMap(XmlSchema schema) {
        Map<QName, XmlSchemaElement> elementMap = new HashMap<QName, XmlSchemaElement>();
        elementMap.put(new QName(ID_ELEMENT),
                buildSchemaElement(ID_ELEMENT, schema, Constants.XSD_LONG));
        elementMap.put(new QName(VERSION_ELEMENT),
                buildSchemaElement(VERSION_ELEMENT, schema, Constants.XSD_LONG));
        elementMap.put(new QName(END_DATE_ELEMENT),
                buildSchemaElement(END_DATE_ELEMENT, schema, Constants.XSD_DATETIME));
        elementMap.put(new QName(FILENAME_ELEMENT),
                buildSchemaElement(FILENAME_ELEMENT, schema, Constants.XSD_STRING));
        elementMap.put(new QName(HEIGHT_ELEMENT),
                buildSchemaElement(HEIGHT_ELEMENT, schema, Constants.XSD_LONG));
        elementMap.put(new QName(INDEX_ID_ELEMENT),
                buildSchemaElement(INDEX_ID_ELEMENT, schema, Constants.XSD_STRING));
        elementMap.put(new QName(OTHER_TAGS_XML_ELEMENT),
                buildSchemaElement(OTHER_TAGS_XML_ELEMENT, schema, Constants.XSD_STRING));
        elementMap.put(new QName(REPOSITORY_ID_ELEMENT),
                buildSchemaElement(REPOSITORY_ID_ELEMENT, schema, Constants.XSD_LONG));
        elementMap.put(new QName(START_DATE_ELEMENT),
                buildSchemaElement(START_DATE_ELEMENT, schema, Constants.XSD_DATETIME));
        elementMap.put(new QName(STYLE_ID_ELEMENT),
                buildSchemaElement(STYLE_ID_ELEMENT, schema, Constants.XSD_DECIMAL));
        elementMap.put(new QName(WIDTH_ELEMENT),
                buildSchemaElement(WIDTH_ELEMENT, schema, Constants.XSD_LONG));

        XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
        gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement.setSchemaTypeName(new QName(Wfs10Constants.GML_NAMESPACE, GML));
        gmlElement.setName(GROUND_GEOM_ELEMENT);
        elementMap.put(new QName(GROUND_GEOM_ELEMENT), gmlElement);

        return elementMap;
    }

    private XmlSchemaElement buildSchemaElement(String elementName, XmlSchema schema, QName typeName) {
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
        return "MULTIPOLYGON (((138.62436068196084 -34.933447128860166, 138.624054045753 "
                + "-34.9344123244184, 138.624051098627 -34.93441204108663, "
                + "138.62436068196084 -34.933447128860166)))";
    }

}
