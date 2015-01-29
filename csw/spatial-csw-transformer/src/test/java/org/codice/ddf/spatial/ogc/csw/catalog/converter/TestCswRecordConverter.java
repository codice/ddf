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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.io.xml.XppReader;
import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static net.opengis.cat.csw.v_2_0_2.ElementSetType.BRIEF;
import static net.opengis.cat.csw.v_2_0_2.ElementSetType.FULL;
import static net.opengis.cat.csw.v_2_0_2.ElementSetType.SUMMARY;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCswRecordConverter {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(TestCswRecordConverter.class);

    private static DateTimeFormatter dateFormatter;

    private static final String SOURCE = "CSW_SOURCE";

    private static final String THUMBNAIL_URL = "THUMBNAIL_URL";

    private static final String TEST_URI = "http://host:port/my/product.pdf";

    private static final DatatypeFactory XSD_FACTORY;

    private static final GregorianCalendar CREATED_DATE = new GregorianCalendar(2014, 10, 1);

    private static final GregorianCalendar MODIFIED_DATE = new GregorianCalendar(2016, 10, 1);

    private static final GregorianCalendar EFFECTIVE_DATE = new GregorianCalendar(2015, 10, 1);

    private static String MODIFIED;

    private static String EFFECTIVE;

    private static String CREATED;

    private static CswRecordConverter converter;

    private static ActionProvider mockActionProvider;

    private static Action mockAction;

    private static final String ACTION_URL = "http://example.com/source/id?transform=resource";

    static {
        DatatypeFactory factory = null;
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("Failed to create xsdFactory: {}", e.getMessage());
        }
        XSD_FACTORY = factory;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        MODIFIED = XSD_FACTORY.newXMLGregorianCalendar(MODIFIED_DATE).toXMLFormat();
        EFFECTIVE = XSD_FACTORY.newXMLGregorianCalendar(EFFECTIVE_DATE).toXMLFormat();
        CREATED = XSD_FACTORY.newXMLGregorianCalendar(CREATED_DATE).toXMLFormat();

        mockActionProvider = mock(ActionProvider.class);
        mockAction = mock(Action.class);
        when(mockActionProvider.getAction(any(Metacard.class))).thenReturn(mockAction);
        when(mockAction.getUrl()).thenReturn(new URL(ACTION_URL));

        converter = new CswRecordConverter(mockActionProvider);
    }

    @Test
    public void testUnmarshalSingleCswRecordToMetacard() {
        XStream xstream = new XStream(new WstxDriver());

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
        cal.set(2003, 4, 9);
        Date expected = new Date(cal.getTimeInMillis());

        assertThat(returned, equalTo(expected));
        assertThat(mc.getResourceURI(), is(nullValue()));
    }

    @Test
    public void testUnmarshalNoNamespaceDeclaration() throws IOException, SAXException {
        XStream xstream = new XStream(new XppDriver());

        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        xstream.alias("csw:Record", MetacardImpl.class);
        InputStream is = IOUtils.toInputStream(getRecordNoNamespaceDeclaration());
        Metacard mc = (Metacard) xstream.fromXML(is);

        Metacard expectedMetacard = getTestMetacard();
        assertThat(mc, not(nullValue()));
        assertThat(mc.getContentTypeName(), equalTo(expectedMetacard.getContentTypeName()));
        assertThat(mc.getCreatedDate(), equalTo(expectedMetacard.getCreatedDate()));
        assertThat(mc.getEffectiveDate(), equalTo(expectedMetacard.getEffectiveDate()));
        assertThat(mc.getId(), equalTo(expectedMetacard.getId()));
        assertThat(mc.getModifiedDate(), equalTo(expectedMetacard.getModifiedDate()));
        assertThat(mc.getTitle(), equalTo(expectedMetacard.getTitle()));
        assertThat(mc.getResourceURI(), equalTo(expectedMetacard.getResourceURI()));
    }

    @Test
    public void testUnmarshalWriteNamespaces()
            throws IOException, SAXException, XmlPullParserException {
        XStream xstream = new XStream(new XppDriver());

        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        xstream.alias("csw:Record", MetacardImpl.class);
        InputStream is = IOUtils.toInputStream(getRecordNoNamespaceDeclaration());

        HierarchicalStreamReader reader = new XppReader(new InputStreamReader(is), XmlPullParserFactory
                .newInstance().newPullParser());
        DataHolder args = xstream.newDataHolder();
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.CSW_NAMESPACE_PREFIX, CswConstants.CSW_OUTPUT_SCHEMA);
        namespaces.put(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX, CswConstants.DUBLIN_CORE_SCHEMA);
        namespaces.put(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                        + CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
                CswConstants.DUBLIN_CORE_TERMS_SCHEMA);
        namespaces.put(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.OWS_NAMESPACE_PREFIX, CswConstants.OWS_NAMESPACE);
        args.put(CswConstants.WRITE_NAMESPACES, namespaces);
        Metacard mc = (Metacard) xstream.unmarshal(reader, null, args);

        Metacard expectedMetacard = getTestMetacard();
        assertThat(mc, not(nullValue()));
        assertThat(mc.getContentTypeName(), equalTo(expectedMetacard.getContentTypeName()));
        assertThat(mc.getCreatedDate(), equalTo(expectedMetacard.getCreatedDate()));
        assertThat(mc.getEffectiveDate(), equalTo(expectedMetacard.getEffectiveDate()));
        assertThat(mc.getId(), equalTo(expectedMetacard.getId()));
        assertThat(mc.getModifiedDate(), equalTo(expectedMetacard.getModifiedDate()));
        assertThat(mc.getTitle(), equalTo(expectedMetacard.getTitle()));
        assertThat(mc.getResourceURI(), equalTo(expectedMetacard.getResourceURI()));
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(mc.getMetadata(), getControlRecord());
    }

    @Test
    public void testUnmarshalSingleCswRecordToMetacardContentTypeMapsToFormat()
            throws ParserConfigurationException, IOException, SAXException {
        XStream xstream = new XStream(new WstxDriver());

        Map<String, String> metacardAttributeMappings = getMetacardAttributeMappings();
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_FORMAT, Metacard.CONTENT_TYPE);

        xstream.registerConverter(converter);

        xstream.alias("csw:Record", MetacardImpl.class);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/Csw_Record.xml");
        HierarchicalStreamReader reader = new DomReader(doc);
        DataHolder holder = xstream.newDataHolder();
        holder.put(CswConstants.CSW_MAPPING, metacardAttributeMappings);

        Metacard mc = (Metacard) xstream.unmarshal(reader, null, holder);

        assertThat(mc, not(nullValue()));
        assertThat(mc.getContentTypeName(), is("PDF"));
    }

    @Test
    public void testUnmarshalCswRecordGeometryToMetacard() {
        XStream xstream = new XStream(new WstxDriver());

        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class
                .getResourceAsStream("/Csw_Record_with_Geometry.xml");
        Metacard mc = (Metacard) xstream.fromXML(is);

        assertThat(mc, not(nullValue()));
        assertThat(mc.getLocation(), not(nullValue()));
    }

    /**
     * CSW Record has multiple elements of same name and it is an element that had to be uniquely
     * qualified between CSW and basic Metacard, e.g., "title" vs. "csw.title"
     */
    @Test
    public void testUnmarshalCswRecordMultipleTitles() {
        XStream xstream = new XStream(new WstxDriver());

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
        // CREATED
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo("2003-01-28T07:09:16Z"));


        assertDates(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED, mc);

        // EFFECTIVE
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_ACCEPTED).getValue(),
                equalTo("2013-07-12T16:16:16Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_COPYRIGHTED).getValue(),
                equalTo("2013-07-12T16:16:16Z"));

        assertDates(Metacard.EFFECTIVE, CswRecordMetacardType.CSW_DATE_ACCEPTED, mc);

        // MODIFIED
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo("2013-05-15T19:15:15Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_SUBMITTED).getValue(),
                equalTo("2003-05-14T19:15:15Z"));

        assertDates(Metacard.MODIFIED, CswRecordMetacardType.CSW_MODIFIED, mc);

    }

    @Test
    public void testUnmarshalCswRecordMetacard_NoModifiedDate() {
        Metacard mc = buildMetacardFromCSW("/Csw_Record_without_ModifiedDate.xml");
        assertThat(mc, not(nullValue()));

        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                startsWith("08976079-9c53-465f-d921-97d0717262f5"));
        assertThat((String) mc.getAttribute(Metacard.ID).getValue(), equalTo((String) mc
                .getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue()));

        // Verify extensible CSW attributes in metacard were populated
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo("2003-01-28T07:09:16Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_SUBMITTED).getValue(),
                equalTo("2003-05-14T19:15:15Z"));

        assertDates(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED, mc);
        assertDates(Metacard.MODIFIED, CswRecordMetacardType.CSW_DATE_SUBMITTED, mc);
    }

    @Test
    public void testUnmarshalCswRecordMetacard_NoDateSubmitted() {
        Metacard mc = buildMetacardFromCSW("/Csw_Record_without_DateSubmitted.xml");
        assertThat(mc, not(nullValue()));

        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                startsWith("08976079-9c53-465f-c921-97d0717262f5"));
        assertThat((String) mc.getAttribute(Metacard.ID).getValue(), equalTo((String) mc
                .getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue()));

        // Verify extensible CSW attributes in metacard were populated
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo("2003-01-28T07:09:16Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo("2013-05-15T19:15:15Z"));

        assertDates(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED, mc);
        assertDates(Metacard.MODIFIED, CswRecordMetacardType.CSW_MODIFIED, mc);
    }

    @Test
    public void testUnmarshalCswRecordMetacard_DateMappings() throws XmlPullParserException {
        XStream xstream = new XStream(new WstxDriver());

        xstream.registerConverter(converter);

        xstream.alias("csw:Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record.xml");

        HierarchicalStreamReader reader = new XppReader(new InputStreamReader(is), XmlPullParserFactory
                .newInstance().newPullParser());
        DataHolder args = xstream.newDataHolder();
        Map<String, String> dateMappings = new HashMap<>();
        dateMappings.put(CswRecordMetacardType.CSW_DATE_SUBMITTED, Metacard.EFFECTIVE);
        dateMappings.put(CswRecordMetacardType.CSW_CREATED, Metacard.CREATED);
        dateMappings.put(CswRecordMetacardType.CSW_MODIFIED, Metacard.MODIFIED);
        dateMappings.put(CswRecordMetacardType.CSW_TYPE, Metacard.CONTENT_TYPE);
        args.put(CswConstants.CSW_MAPPING, dateMappings);
        Metacard mc = (Metacard) xstream.unmarshal(reader, null, args);

        assertThat(mc, not(nullValue()));

        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                is(equalTo("08976079-9c53-465f-b921-97d0717262f5")));
        assertThat((String) mc.getAttribute(Metacard.ID).getValue(), equalTo((String) mc
                .getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue()));

        // Verify extensible CSW attributes in metacard were populated
        // CREATED
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo("2003-01-28T07:09:16Z"));


        assertDates(Metacard.CREATED, CswRecordMetacardType.CSW_CREATED, mc);

        // EFFECTIVE
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_ACCEPTED).getValue(),
                equalTo("2013-07-12T16:16:16Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_COPYRIGHTED).getValue(),
                equalTo("2013-07-12T16:16:16Z"));

        assertDates(Metacard.EFFECTIVE, CswRecordMetacardType.CSW_DATE_SUBMITTED, mc);

        // MODIFIED
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo("2013-05-15T19:15:15Z"));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_DATE_SUBMITTED).getValue(),
                equalTo("2003-05-14T19:15:15Z"));

        assertDates(Metacard.MODIFIED, CswRecordMetacardType.CSW_MODIFIED, mc);
    }

    private Metacard buildMetacardFromCSW(String cswFileName) {
        XStream xstream = new XStream(new WstxDriver());

        xstream.registerConverter(converter);

        xstream.alias("Record", MetacardImpl.class);
        InputStream is = TestCswRecordConverter.class.getResourceAsStream(cswFileName);
        Metacard mc = (Metacard) xstream.fromXML(is);
        return mc;
    }

    @Test
    public void testUnmarshalCswRecordWithCustomDateMappings()
            throws ParserConfigurationException, IOException, SAXException {
        XStream xstream = new XStream(new WstxDriver());

        // Custom date mappings
        Map<String, String> metacardAttributeMappings = new HashMap<String, String>();
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_MODIFIED, Metacard.EFFECTIVE);
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_CREATED, Metacard.CREATED);
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_DATE_SUBMITTED, Metacard.MODIFIED);

        xstream.registerConverter(converter);

        xstream.alias("csw:Record", MetacardImpl.class);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/Csw_Record.xml");
        HierarchicalStreamReader reader = new DomReader(doc);
        DataHolder holder = xstream.newDataHolder();
        holder.put(CswConstants.CSW_MAPPING, metacardAttributeMappings);

        Metacard mc = (Metacard) xstream.unmarshal(reader, null, holder);

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
            IOException, JAXBException, ParserConfigurationException, SAXException {
        XStream xstream = new XStream(new WstxDriver());

        xstream.registerConverter(converter);

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

        xstream.alias("csw:Record", MetacardImpl.class);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(IOUtils.toInputStream(xml));
        HierarchicalStreamReader reader = new DomReader(doc);
        DataHolder holder = xstream.newDataHolder();
        holder.put(Metacard.RESOURCE_URI, CswRecordMetacardType.CSW_SOURCE);
        holder.put(Metacard.THUMBNAIL, CswRecordMetacardType.CSW_REFERENCES);

        Metacard mc = (Metacard) xstream.unmarshal(reader, null, holder);

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
        String dateStr = "2013-05-03T17:25:04Z";
        Serializable ser = converter.convertStringValueToMetacardValue(
                AttributeFormat.DATE, dateStr);
        assertThat(ser, not(nullValue()));
        assertThat(Date.class.isAssignableFrom(ser.getClass()), is(true));
        Date date = (Date) ser;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
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
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_TYPE, Metacard.CONTENT_TYPE);
        UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);
        context.put(CswConstants.CSW_MAPPING, metacardAttributeMappings);

        // Perform test
        Metacard metacard = (Metacard) converter.unmarshal(reader, context);

        // Verify
        LOGGER.debug("metacard id: {}", metacard.getId());
        LOGGER.debug("metacard content type: {}", metacard.getContentTypeName());
        assertThat(metacard.getContentTypeName(), is("IMAGE-PRODUCT"));
    }

    @Test
    public void testMarshalRecord() throws IOException, JAXBException, SAXException,
            XpathException {
        Metacard metacard = getTestMetacard();

        StringWriter stringWriter = new StringWriter();
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        MarshallingContext context = new TreeMarshaller(writer, null, null);

        converter.marshal(metacard, writer, context);

        String xml = stringWriter.toString();
        assertThat(xml, containsString(CswConstants.CSW_RECORD));
        assertRecordXml(xml, metacard, FULL);
    }

    @Test
    public void testMarshalBriefRecord() throws IOException, JAXBException, SAXException,
            XpathException {
        Metacard metacard = getTestMetacard();

        StringWriter stringWriter = new StringWriter();
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        MarshallingContext context = new TreeMarshaller(writer, null, null);
        context.put(CswConstants.ELEMENT_SET_TYPE, ElementSetType.BRIEF);

        converter.marshal(metacard, writer, context);

        String xml = stringWriter.toString();
        assertThat(xml, containsString(CswConstants.CSW_BRIEF_RECORD));
        assertRecordXml(xml, metacard, BRIEF);
    }

    @Test
    public void testMarshalSummaryRecord() throws IOException, JAXBException, SAXException,
            XpathException {
        Metacard metacard = getTestMetacard();

        StringWriter stringWriter = new StringWriter();
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        MarshallingContext context = new TreeMarshaller(writer, null, null);
        context.put(CswConstants.ELEMENT_SET_TYPE, ElementSetType.SUMMARY);

        converter.marshal(metacard, writer, context);

        String xml = stringWriter.toString();
        assertThat(xml, containsString(CswConstants.CSW_SUMMARY_RECORD));
        assertRecordXml(xml, metacard, SUMMARY);
    }

    @Test
    public void testMarshalRecordWithActionProvider()
            throws IOException, JAXBException, SAXException, XpathException, URISyntaxException {
        MetacardImpl metacard = getTestMetacard();
        metacard.setResourceURI(new URI(TEST_URI));

        StringWriter stringWriter = new StringWriter();
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        MarshallingContext context = new TreeMarshaller(writer, null, null);
        context.put(CswConstants.ELEMENT_NAMES, Arrays.asList(new QName("source")));
        context.put(CswConstants.WRITE_NAMESPACES, true);

        CswRecordConverter testConverter = new CswRecordConverter(null);
        testConverter.marshal(metacard, writer, context);

        String xml = "<?xml version='1.0' encoding='UTF-8'?>" + stringWriter.toString();

        XMLUnit.setIgnoreWhitespace(true);

        final String test1 =
                "<?xml version='1.0' encoding='UTF-8'?><csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"><source>"
                        + TEST_URI + "</source></csw:Record>";

        assertXMLEqual(test1, xml);

        stringWriter.getBuffer().setLength(0);
        converter.marshal(metacard, writer, context);

        String xml2 = "<?xml version='1.0' encoding='UTF-8'?>" + stringWriter.toString();

        final String test2 =
                "<?xml version='1.0' encoding='UTF-8'?><csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"><source>"
                        + ACTION_URL
                        + "</source></csw:Record>";

        assertXMLEqual(test2, xml2);
    }

    @Test
    public void testMarshalRecordWithNamespaces() throws IOException, JAXBException, SAXException,
            XpathException {
        Metacard metacard = getTestMetacard();

        StringWriter stringWriter = new StringWriter();
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        MarshallingContext context = new TreeMarshaller(writer, null, null);
        context.put(CswConstants.WRITE_NAMESPACES, true);

        converter.marshal(metacard, writer, context);

        String xml = stringWriter.toString();
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(getControlRecord(), xml);
    }

    @Test
    public void testMetacardTransform() throws IOException, JAXBException, SAXException,
            XpathException, CatalogTransformerException {
        Metacard metacard = getTestMetacard();

        Map<String, Serializable> args = new HashMap<>();
        args.put(CswConstants.WRITE_NAMESPACES, true);

        BinaryContent content = converter.transform(metacard, args);

        String xml = IOUtils.toString(content.getInputStream());
        assertThat(xml,
                containsString("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"));
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(getControlRecord(), xml);
    }

    @Test
    public void testMetacardTransformOmitXmlDeclaration() throws IOException, JAXBException, SAXException,
            XpathException, CatalogTransformerException {
        Metacard metacard = getTestMetacard();

        Map<String, Serializable> args = new HashMap<>();
        args.put(CswConstants.WRITE_NAMESPACES, true);
        args.put(CswConstants.OMIT_XML_DECLARATION, true);

        BinaryContent content = converter.transform(metacard, args);

        String xml = IOUtils.toString(content.getInputStream());
        assertThat(xml, not(containsString(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")));
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(getControlRecord(), xml);
    }

    @Test
    public void testMetacardTransformOmitNamespaces() throws IOException, JAXBException, SAXException,
            XpathException, CatalogTransformerException {
        Metacard metacard = getTestMetacard();

        Map<String, Serializable> args = new HashMap<>();
        args.put(CswConstants.WRITE_NAMESPACES, false);

        BinaryContent content = converter.transform(metacard, args);

        String xml = IOUtils.toString(content.getInputStream());
        assertThat(xml, containsString("<csw:Record>"));
    }

    @Test
    public void testInputTransformWithNoNamespaceDeclaration()
            throws IOException, CatalogTransformerException {
        InputStream is = IOUtils.toInputStream(getRecordNoNamespaceDeclaration());
        Metacard mc = converter.transform(is);

        Metacard expectedMetacard = getTestMetacard();
        assertThat(mc, not(nullValue()));
        assertThat(mc.getContentTypeName(), equalTo(expectedMetacard.getContentTypeName()));
        assertThat(mc.getCreatedDate(), equalTo(expectedMetacard.getCreatedDate()));
        assertThat(mc.getEffectiveDate(), equalTo(expectedMetacard.getEffectiveDate()));
        assertThat(mc.getId(), equalTo(expectedMetacard.getId()));
        assertThat(mc.getModifiedDate(), equalTo(expectedMetacard.getModifiedDate()));
        assertThat(mc.getTitle(), equalTo(expectedMetacard.getTitle()));
        assertThat(mc.getResourceURI(), equalTo(expectedMetacard.getResourceURI()));
    }

    @Test
    public void testInputTransform() throws IOException, CatalogTransformerException {

              InputStream is = TestCswRecordConverter.class.getResourceAsStream("/Csw_Record.xml");
        Metacard mc = converter.transform(is);

        assertThat(mc, not(nullValue()));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                startsWith("08976079-9c53-465f-b921-97d0717262f5"));
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private MetacardImpl getTestMetacard() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setContentTypeName("I have some content type");
        metacard.setContentTypeVersion("1.0.0");
        metacard.setCreatedDate(CREATED_DATE.getTime());
        metacard.setEffectiveDate(EFFECTIVE_DATE.getTime());
        metacard.setId("ID");
        metacard.setLocation("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");
        metacard.setMetadata("metadata a whole bunch of metadata");
        metacard.setModifiedDate(MODIFIED_DATE.getTime());
        metacard.setResourceSize("123TB");
        metacard.setSourceId("sourceID");
        metacard.setTitle("This is my title");
        try {
            metacard.setResourceURI(new URI(ACTION_URL));
        } catch (URISyntaxException e) {
            LOGGER.debug("URISyntaxException", e);
        }

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
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_MODIFIED),
                equalTo(Metacard.MODIFIED));
    }

    private void assertDateMappings(Map<String, String> dateMappings) {
        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_TITLE), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_TITLE), equalTo(Metacard.TITLE));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_CREATED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_CREATED),
                equalTo(Metacard.EFFECTIVE));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_DATE_SUBMITTED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_DATE_SUBMITTED),
                equalTo(Metacard.CREATED));

        assertThat(dateMappings.containsKey(CswRecordMetacardType.CSW_MODIFIED), is(true));
        assertThat(dateMappings.get(CswRecordMetacardType.CSW_MODIFIED),
                equalTo(Metacard.MODIFIED));
    }

    private void assertDateConversion(Serializable ser, Calendar expectedDate) {
        assertThat(ser, not(nullValue()));
        assertThat(Date.class.isAssignableFrom(ser.getClass()), is(true));
        Date date = (Date) ser;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertThat(cal.get(Calendar.MONTH), equalTo(expectedDate.get(Calendar.MONTH)));
        assertThat(cal.get(Calendar.YEAR), equalTo(expectedDate.get(Calendar.YEAR)));
        assertThat(cal.get(Calendar.DAY_OF_MONTH),
                equalTo(expectedDate.get(Calendar.DAY_OF_MONTH)));
    }

    private Map<String, String> getMetacardAttributeMappings() {
        Map<String, String> metacardAttributeMappings = new HashMap<String, String>();
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_CREATED, Metacard.EFFECTIVE);
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_DATE_SUBMITTED, Metacard.CREATED);
        metacardAttributeMappings.put(CswRecordMetacardType.CSW_MODIFIED, Metacard.MODIFIED);
        return metacardAttributeMappings;
    }

    private byte[] getThumbnailByteArray(URL url) throws IOException {
        InputStream is = url.openStream();
        byte[] thumbnail = IOUtils.toByteArray(is);
        IOUtils.closeQuietly(is);
        return thumbnail;
    }

    private String getControlRecord() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" "
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                + "xmlns:dct=\"http://purl.org/dc/terms/\" "
                + "xmlns:ows=\"http://www.opengis.net/ows\">\n"
                + "  <dct:created>" + CREATED + "</dct:created>\n"
                + "  <dc:date>" + MODIFIED + "</dc:date>\n"
                + "  <dct:modified>" + MODIFIED + "</dct:modified>\n"
                + "  <dct:dateSubmitted>" + MODIFIED + "</dct:dateSubmitted>\n"
                + "  <dct:issued>" + MODIFIED + "</dct:issued>\n"
                + "  <dc:identifier>ID</dc:identifier>\n"
                + "  <dct:bibliographicCitation>ID</dct:bibliographicCitation>\n"
                + "  <dc:source>" + ACTION_URL + "</dc:source>\n"
                + "  <dc:title>This is my title</dc:title>\n"
                + "  <dct:alternative>This is my title</dct:alternative>\n"
                + "  <dc:type>I have some content type</dc:type>\n"
                + "  <dct:dateAccepted>" + EFFECTIVE + "</dct:dateAccepted>\n"
                + "  <dct:dateCopyrighted>" + EFFECTIVE + "</dct:dateCopyrighted>\n"
                + "  <dc:publisher>sourceID</dc:publisher>\n"
                + "  <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                + "    <ows:LowerCorner>10.0 10.0</ows:LowerCorner>\n"
                + "    <ows:UpperCorner>40.0 40.0</ows:UpperCorner>\n"
                + "  </ows:BoundingBox>\n"
                + "</csw:Record>\n";
    }

    private String getRecordNoNamespaceDeclaration() {
        return "<csw:Record>\n"
                + "  <dct:created>" + CREATED + "</dct:created>\n"
                + "  <dc:date>" + MODIFIED + "</dc:date>\n"
                + "  <dct:modified>" + MODIFIED + "</dct:modified>\n"
                + "  <dct:dateSubmitted>" + MODIFIED + "</dct:dateSubmitted>\n"
                + "  <dct:issued>" + MODIFIED + "</dct:issued>\n"
                + "  <dc:identifier>ID</dc:identifier>\n"
                + "  <dct:bibliographicCitation>ID</dct:bibliographicCitation>\n"
                + "  <dc:source>" + ACTION_URL + "</dc:source>\n"
                + "  <dc:title>This is my title</dc:title>\n"
                + "  <dct:alternative>This is my title</dct:alternative>\n"
                + "  <dc:type>I have some content type</dc:type>\n"
                + "  <dct:dateAccepted>" + EFFECTIVE + "</dct:dateAccepted>\n"
                + "  <dct:dateCopyrighted>" + EFFECTIVE + "</dct:dateCopyrighted>\n"
                + "  <dc:publisher>sourceID</dc:publisher>\n"
                + "  <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                + "    <ows:LowerCorner>10.0 10.0</ows:LowerCorner>\n"
                + "    <ows:UpperCorner>40.0 40.0</ows:UpperCorner>\n"
                + "  </ows:BoundingBox>\n"
                + "</csw:Record>\n";
    }

    private void assertRecordXml(String xml, Metacard metacard, ElementSetType elemntSetType) {
        switch (elemntSetType) {
        case FULL:
            assertThat(xml, containsString("<dct:bibliographicCitation>" + metacard.getId()
                    + "</dct:bibliographicCitation>"));
            assertThat(xml, containsString(
                    "<dct:alternative>" + metacard.getTitle() + "</dct:alternative>"));
            assertThat(xml, containsString("<dc:date>" + MODIFIED + "</dc:date>"));
            assertThat(xml, containsString("<dct:modified>" + MODIFIED + "</dct:modified>"));
            assertThat(xml, containsString("<dct:created>" + CREATED + "</dct:created>"));
            assertThat(xml,
                    containsString("<dct:dateAccepted>" + EFFECTIVE + "</dct:dateAccepted>"));
            assertThat(xml,
                    containsString("<dct:dateCopyrighted>" + EFFECTIVE + "</dct:dateCopyrighted>"));
            assertThat(xml,
                    containsString("<dct:dateSubmitted>" + MODIFIED + "</dct:dateSubmitted>"));
            assertThat(xml, containsString("<dct:issued>" + MODIFIED + "</dct:issued>"));
            assertThat(xml,
                    containsString("<dc:source>" + metacard.getResourceURI() + "</dc:source>"));
            assertThat(xml,
                    containsString("<dc:publisher>" + metacard.getSourceId() + "</dc:publisher>"));

        case SUMMARY:
            // This seems weak but we only have a default mapping for modified
            assertThat(xml, containsString("<dct:modified>" + MODIFIED + "</dct:modified>"));
            //            assertThat(xml, containsString("<dc:subject>" + metacard.getId() + "</dc:subject>"));
            //            assertThat(xml, containsString("<dc:format>" + metacard.getId() + "</dc:format>"));
            //            assertThat(xml, containsString("<dc:relation>" + metacard.getId() + "</dc:relation>"));
            //            assertThat(xml, containsString("<dc:abstract>" + metacard.getId() + "</dc:abstract>"));
            //            assertThat(xml, containsString("<dc:spatial>" + metacard.getId() + "</dc:spatial>"));
            //            assertThat(xml, containsString("<dc:modified>" + metacard.getId() + "</dc:modified>"));

        case BRIEF:
            assertThat(xml,
                    containsString("<dc:identifier>" + metacard.getId() + "</dc:identifier>"));
            assertThat(xml, containsString("<dc:title>" + metacard.getTitle() + "</dc:title>"));
            assertThat(xml,
                    containsString("<dc:type>" + metacard.getContentTypeName() + "</dc:type>"));
        }

        // TODO - assert the reverse - if brief then it shouldn't have the others
    }
}
