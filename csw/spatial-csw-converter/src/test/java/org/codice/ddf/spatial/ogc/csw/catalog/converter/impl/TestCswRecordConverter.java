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
package org.codice.ddf.spatial.ogc.csw.catalog.converter.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.WstxDriver;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class TestCswRecordConverter {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(TestCswRecordConverter.class);

    private static DateTimeFormatter dateFormatter;

    private static final String SOURCE = "CSW_SOURCE";

    private static final String THUMBNAIL_URL = "THUMBNAIL_URL";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.DEBUG);

        dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
    }

    @Test
    public void testConstruction() {
        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        assertDateMappings(converter.cswToMetacardAttributeNames);
    }

    @Test
    public void testConstructionWithNullConfiguration() {
        CswRecordConverter converter = new CswRecordConverter(null, null, null, null, false);
        assertDefaultDateMappings(converter.cswToMetacardAttributeNames);
    }

    @Test
    public void testConstructionWithNoDateMappings() {
        CswRecordConverter converter = new CswRecordConverter(null, null, null, null, false);
        assertDefaultDateMappings(converter.cswToMetacardAttributeNames);
    }

    @Test
    public void testUnmarshalSingleCswRecordToMetacard() {
        XStream xstream = new XStream(new WstxDriver());

        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record_Text.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                startsWith("urn:uuid:e933"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_SUBJECT).getValue(),
                equalTo("Land titles"));

        Date returned = (Date) mc.getAttribute(CswRecordMetacardType.CSW_DATE).getValue();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2003,  4,  9);
        Date expected = new Date(cal.getTimeInMillis());

        assertThat(returned, equalTo(expected));
        assertThat(mc.getResourceURI(), is(nullValue()));
    }

    @Test
    public void testUnmarshalSingleCswRecordToMetacardContentTypeMapsToFormat() {
        XStream xstream = new XStream(new WstxDriver());

        Map<String, String> metacardAttributeMappings = getMetacardAttributeMappings();
        metacardAttributeMappings.put(Metacard.CONTENT_TYPE, CswRecordMetacardType.CSW_FORMAT);
        
        CswRecordConverter converter = new CswRecordConverter(metacardAttributeMappings, CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));
        assertThat((String) mc.getContentTypeName(), is("PDF"));
    }

    @Test
    public void testUnmarshalCswRecordGeometryToMetacard() {
        XStream xstream = new XStream(new WstxDriver());

        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class
                .getResourceAsStream("/Csw_Record_with_Geometry.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));

    }

    /**
     * CSW Record has multiple elements of same name and it is an element that had to be uniquely
     * qualified between CSW and basic Metacard, e.g., "title" vs. "csw.title"
     */
    @Test
    public void testUnmarshalCswRecordMultipleTitles() {
        XStream xstream = new XStream(new WstxDriver());

        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class
                .getResourceAsStream("/Csw_Record_MultiValueFields.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));
        LOGGER.debug("Metacard title = {}", (String) mc.getAttribute(Metacard.TITLE).getValue());
        LOGGER.debug("CSW title = {}", (String) mc.getAttribute(CswRecordMetacardType.CSW_TITLE)
                .getValue());
        assertThat(mc.getTitle(), equalTo("First title"));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_TITLE, new String[] {"First title",
            "Second title"});
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_SUBJECT, new String[] {"Subject 1",
            "Subject 2"});
    }

    @Test
    public void testUnmarshalCswRecordMultipleResourceUri() {
        XStream xstream = new XStream(new WstxDriver());

        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, CswRecordMetacardType.CSW_SOURCE, null, false);
        
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class
                .getResourceAsStream("/Csw_Record_MultiValueFields.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);
        assertThat(mc.getResourceURI().toString(), equalTo("http://example.com/product.pdf"));
    }

    // This test exercises all of the attributes shared between a basic metacard
    // and
    // the CSW metacard type, e.g., title, created/modified/effective dates.
    @Test
    public void testUnmarshalCswRecordMetacardAttributeOverlap() {
        XStream xstream = new XStream(new WstxDriver());

        CswRecordConverter converter = new CswRecordConverter(null, CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW_Source");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));

        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                startsWith("08976079-9c53-465f-b921-97d0717262f5"));
        assertThat((String) mc.getAttribute(Metacard.ID).getValue(), equalTo((String) mc
                .getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue()));

        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_TYPE).getValue(),
                equalTo("IMAGE-PRODUCT"));

        // Verify extensible CSW attributes in metacard were populated
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo("2003-01-28T07:09:16Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_SUBMITTED).getValue(),
                equalTo("2003-05-14T19:15:15Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo("2013-05-15T19:15:15Z"));

        assertDates(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED, mc);
        assertDates(Metacard.MODIFIED, CswRecordMetacardType.CSW_MODIFIED, mc);
    }

    @Test
    public void testUnmarshalCswRecordWithCustomDateMappings() {
        XStream xstream = new XStream(new WstxDriver());

        // Custom date mappings
        Map<String, String> metacardAttributeMappings = new HashMap<String, String>();
        metacardAttributeMappings.put(Metacard.EFFECTIVE, CswRecordMetacardType.CSW_MODIFIED);
        metacardAttributeMappings.put(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED);
        metacardAttributeMappings.put(Metacard.MODIFIED, CswRecordMetacardType.CSW_DATE_SUBMITTED);

        CswRecordConverter converter = new CswRecordConverter(metacardAttributeMappings, CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        converter.setMetacardType(new CswRecordMetacardType());

        converter.setSourceId("CSW_Source");
        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));

        // Verify extensible CSW attributes in metacard were populated
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo("2003-01-28T07:09:16Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_SUBMITTED).getValue(),
                equalTo("2003-05-14T19:15:15Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo("2013-05-15T19:15:15Z"));

        assertDates(Metacard.EFFECTIVE, CswRecordMetacardType.CSW_MODIFIED, mc);
        assertDates(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED, mc);
        assertDates(Metacard.MODIFIED, CswRecordMetacardType.CSW_DATE_SUBMITTED, mc);
    }

    @Test
    public void testUnmarshalCswRecordWithProductAndThumbnail() throws URISyntaxException,
        IOException, JAXBException {
        XStream xstream = new XStream(new WstxDriver());
        
        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, CswRecordMetacardType.CSW_SOURCE, CswRecordMetacardType.CSW_REFERENCES, false);
        
        converter.setMetacardType(new CswRecordMetacardType());
        converter.setSourceId(SOURCE);

        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record.xml");

        // get the URL to the thumbnail image and stick it in the xml string
        // this makes the test filesystem indepedent
        URL thumbnail = TestCswRecordConverter.class.getResource("/ddf_globe.png");
        String xml = null;
        if (thumbnail != null) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer);
            xml = writer.toString();
            xml = xml.replace(THUMBNAIL_URL, thumbnail.toString());
        }

        Metacard mc = (Metacard) xstream.fromXML(xml);

        assertThat(mc, not(nullValue()));

        // Verify resource URI populated
        String productUrl = "http://example.com/product.pdf";
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_SOURCE).getValue(),
                equalTo(productUrl));
        assertThat(mc.getResourceURI(), equalTo(new URI(productUrl)));

        // Verify the thumbnail is populated
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_REFERENCES).getValue(),
                equalTo(thumbnail.toString()));
        assertThat(mc.getThumbnail(), equalTo(getThumbnailByteArray(thumbnail)));
    }

    /**
     * Verifies that Zulu time zone is valid in ISO 8601 date.
     */
    @Test
    public void testConvertISODateMetacardAttribute() {
        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        String dateStr = "2013-05-03T17:25:04Z";
        Serializable ser = converter.convertStringValueToMetacardValue(
                AttributeFormat.DATE, dateStr);
        assertThat(ser, not(nullValue()));
        assertThat(Date.class.isAssignableFrom(ser.getClass()), is(true));
        Date date = (Date) ser;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertThat(cal.get(Calendar.MONTH), equalTo(Calendar.MAY));
        assertThat(cal.get(Calendar.YEAR), equalTo(2013));
        assertThat(cal.get(Calendar.DAY_OF_MONTH), equalTo(3));
    }

    /**
     * Verifies that if metacard's date has an invalid timezone in the ISO 8601 format that the
     * current time is returned.
     */
    @Test
    public void testConvertInvalidTimeZoneInDateMetacardAttribute() {
        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        String dateStr = "2013-05-13T10:56:39EDT";
        Serializable ser = converter.convertStringValueToMetacardValue(
                AttributeFormat.DATE, dateStr);

        assertDateConversion(ser, Calendar.getInstance());
    }

    /**
     * Verifies that if the metacard's date is not in ISO 8601 format the current time is returned.
     */
    @Test
    public void testConvertInvalidDateMetacardAttribute() {
        CswRecordConverter converter = new CswRecordConverter(this.getMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        String dateStr = "26021000ZFEB11";
        Serializable ser = converter.convertStringValueToMetacardValue(
                AttributeFormat.DATE, dateStr);

        assertDateConversion(ser, Calendar.getInstance());
    }

    /**
     * Test to verify that a metacard's content type is set to the CSW Record's type field.
     */
    @Test
    public void testSetMetacardContentTypeToCswRecordType() throws ParserConfigurationException,
        SAXException, IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/Csw_Record.xml");
        HierarchicalStreamReader reader = new DomReader(doc);
        
        Map<String, String> metacardAttributeMappings = this.getMetacardAttributeMappings();
        metacardAttributeMappings.put(Metacard.CONTENT_TYPE, CswRecordMetacardType.CSW_TYPE);
        
        CswRecordConverter cswRecordConverter = new CswRecordConverter(metacardAttributeMappings, CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL, null, null, false);
        
        cswRecordConverter.setMetacardType(new CswRecordMetacardType());

        // Perform test
        Metacard metacard = (Metacard) cswRecordConverter.unmarshal(reader, null);

        // Verify
        LOGGER.debug("metacard id: {}", metacard.getId());
        LOGGER.debug("metacard content type: {}", metacard.getContentTypeName());
        assertThat(metacard.getContentTypeName(), is("IMAGE-PRODUCT"));
    }
    
    @Test
    public void testMarshalRecord() throws UnsupportedEncodingException, JAXBException {
        CswRecordConverter cswRecordConverter = createRecordConverter();
        Metacard metacard = getTestMetacard();
        XStream xstream = new XStream(new StaxDriver(new NoNameCoder()));

        xstream.registerConverter(cswRecordConverter);

        xstream.alias(CswConstants.CSW_RECORD, MetacardImpl.class);
        
        xstream.toXML(metacard);
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private CswRecordConverter createRecordConverter() {
        CswRecordConverter cswRecordConverter;
        Map<String, String> prefixToUriMapping = new HashMap<String, String>();

        prefixToUriMapping.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        prefixToUriMapping.put(CswConstants.XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX,
                XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
        prefixToUriMapping.put(CswConstants.XML_SCHEMA_NAMESPACE_PREFIX,
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        prefixToUriMapping.put(CswConstants.OWS_NAMESPACE_PREFIX, CswConstants.OWS_NAMESPACE);
        prefixToUriMapping.put(CswConstants.CSW_NAMESPACE_PREFIX, CswConstants.CSW_OUTPUT_SCHEMA);
        prefixToUriMapping.put(CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX,
                CswConstants.DUBLIN_CORE_SCHEMA);
        prefixToUriMapping.put(CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
                CswConstants.DUBLIN_CORE_TERMS_SCHEMA);
        cswRecordConverter = new CswRecordConverter(
                new HashMap<String, String>(),
                prefixToUriMapping,
                "productRetrievalMethod",
                "resourceUriMapping",
                "thumbnailMapping",
                true);
        return cswRecordConverter;
    }

    private Metacard getTestMetacard() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setContentTypeName("I have some content type");
        metacard.setContentTypeVersion("1.0.0");
        metacard.setCreatedDate(new Date());
        metacard.setEffectiveDate(new Date());
        metacard.setId("ID");
        metacard.setLocation("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");
        metacard.setMetadata("metadata a whole bunch of metadata");
        metacard.setModifiedDate(new Date());
        metacard.setResourceSize("123 is the size");
        metacard.setSourceId("sourceID");
        metacard.setTitle("This is my title");

        Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
        descriptors.add(new AttributeDescriptorImpl("id", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("version", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("end_date", false, false, false, false,
                BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl("filename", false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("height", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("index_id", false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("other_tags_xml", false, false, false, false,
                BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl("repository_id", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("start_date", false, false, false, false,
                BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl("style_id", false, false, false, false,
                BasicTypes.INTEGER_TYPE));
        descriptors.add(new AttributeDescriptorImpl("width", false, false, false, false,
                BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl("ground_geom", false, false, false, false,
                BasicTypes.GEO_TYPE));

        metacard.setType(new MetacardTypeImpl("test_type", descriptors));

        return metacard;
    }

    private void assertListStringAttribute(Metacard mc, String attrName, String[] expectedValues) {
        List<?> values = (List<?>) mc.getAttribute(attrName).getValues();
        assertThat(values, not(nullValue()));
        assertThat(values.size(), equalTo(expectedValues.length));

        List<String> valuesList = new ArrayList<String>();
        valuesList.addAll((List<? extends String>) values);
        LOGGER.debug("valuesList: {}", valuesList);
        assertThat(valuesList, hasItems(expectedValues));
    }

    private void assertDates(String metacardAttribute, String cswAttribute, Metacard mc) {
        Date date = (Date) mc.getAttribute(metacardAttribute).getValue();
        String expectedDateStr = (String) mc.getAttribute(cswAttribute).getValue();
        Date expectedDate = dateFormatter.parseDateTime(expectedDateStr).toDate();
        assertThat(date.getTime(), equalTo(expectedDate.getTime()));
    }

    private void assertDefaultDateMappings(Map<String, String> dateMappings) {
        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_TITLE), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_TITLE), equalTo(Metacard.TITLE));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_CREATED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_CREATED), equalTo(Metacard.CREATED));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_DATE_SUBMITTED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_DATE_SUBMITTED),
                equalTo(Metacard.MODIFIED));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_MODIFIED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_MODIFIED), equalTo(Metacard.MODIFIED));
    }

    private void assertDateMappings(Map<String, String> dateMappings) {
        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_TITLE), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_TITLE), equalTo(Metacard.TITLE));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_CREATED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_CREATED), equalTo(Metacard.EFFECTIVE));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_DATE_SUBMITTED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_DATE_SUBMITTED),
                equalTo(Metacard.CREATED));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_MODIFIED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_MODIFIED), equalTo(Metacard.MODIFIED));
    }
    
    private void assertDateConversion(Serializable ser, Calendar expectedDate) {
        assertThat(ser, not(nullValue()));
        assertThat(Date.class.isAssignableFrom(ser.getClass()), is(true));
        Date date = (Date) ser;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // Calendar now = Calendar.getInstance();
        assertThat(cal.get(Calendar.MONTH), equalTo(expectedDate.get(Calendar.MONTH)));
        assertThat(cal.get(Calendar.YEAR), equalTo(expectedDate.get(Calendar.YEAR)));
        assertThat(cal.get(Calendar.DAY_OF_MONTH), equalTo(expectedDate.get(Calendar.DAY_OF_MONTH)));
    }
    
    private Map<String, String> getMetacardAttributeMappings() {
        Map<String, String> metacardAttributeMappings = new HashMap<String, String>();
        metacardAttributeMappings.put(Metacard.EFFECTIVE, CswRecordMetacardType.CSW_CREATED);
        metacardAttributeMappings.put(Metacard.CREATED, CswRecordMetacardType.CSW_DATE_SUBMITTED);
        metacardAttributeMappings.put(Metacard.MODIFIED, CswRecordMetacardType.CSW_MODIFIED);
        return metacardAttributeMappings;
    }

    private byte[] getThumbnailByteArray(URL url) throws IOException {
        InputStream is = url.openStream();
        byte[] thumbnail = IOUtils.toByteArray(is);
        IOUtils.closeQuietly(is);
        return thumbnail;
    }
}
