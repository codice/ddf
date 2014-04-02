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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import net.opengis.cat.csw.v_2_0_2.AbstractRecordType;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.RecordType;
import net.opengis.cat.csw.v_2_0_2.SummaryRecordType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.ows.v_1_0_0.BoundingBoxType;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.WstxDriver;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class TestGetRecordsResponseConverter {

    
    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestGetRecordsResponseConverter.class);
    
    private static final String ID_PREFIX = "id_";
    private static final String SOURCE_PREFIX = "source_";
    private static final String TITLE_PREFIX = "title ";

    private static final String ID = "identifier";
    private static final String SOURCE = "source";
    private static final String TITLE = "title";
    private static final String FORMAT = "format";
    private static final String RELATION = "relation";

    private static final String WKT = "POLYGON((4 1, 2 5, 4 5, 4 1))";
    
    @Test
    public void testGetRecordsResponseConversion() {
        XStream xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        
        RecordConverterFactory factory = new CswRecordConverterFactory();

        xstream.registerConverter(new GetRecordsResponseConverter(Arrays.asList(factory)));
        xstream.alias("GetRecordsResponse", CswRecordCollection.class);

        String xml = "<csw:GetRecordsResponse xmlns:csw=\"http://www.opengis.net/cat/csw\">\r\n"
                + "  <csw:SearchStatus status=\"subset\" timestamp=\"2013-05-01T02:13:36+0200\"/>\r\n"
                + "  <csw:SearchResults elementSet=\"full\" nextRecord=\"11\" numberOfRecordsMatched=\"479\" numberOfRecordsReturned=\"10\" recordSchema=\"csw:Record\">\r\n"
                + "    <csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw\">\r\n"
                + "      <dc:identifier xmlns:dc=\"http://purl.org/dc/elements/1.1/\">{8C1F6297-EC96-4302-A01E-14988C9149FD}</dc:identifier>\r\n"
                + "      <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">title 1</dc:title>\r\n"
                + "      <dct:modified xmlns:dct=\"http://purl.org/dc/terms/\">2008-12-15</dct:modified>\r\n"
                + "      <dc:subject xmlns:dc=\"http://purl.org/dc/elements/1.1/\">subject 1</dc:subject>\r\n"
                + "      <dc:subject xmlns:dc=\"http://purl.org/dc/elements/1.1/\">second subject</dc:subject>\r\n"
                + "      <dct:abstract xmlns:dct=\"http://purl.org/dc/terms/\">abstract 1</dct:abstract>\r\n"
                + "      <dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">copyright 1</dc:rights>\r\n"
                + "      <dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">copyright 2</dc:rights>\r\n"
                + "      <dc:language xmlns:dc=\"http://purl.org/dc/elements/1.1/\">english</dc:language>      \r\n"
                + "      <ows:BoundingBox xmlns:ows=\"http://www.opengis.net/ows\" crs=\"EPSG:RD_New (28992)\">\r\n"
                + "        <ows:LowerCorner>5.121 52.139</ows:LowerCorner>\r\n"
                + "        <ows:UpperCorner>4.468 52.517</ows:UpperCorner>\r\n"
                + "      </ows:BoundingBox>   \r\n"
                + "      <dc:type xmlns:dc=\"http://purl.org/dc/elements/1.1/\">dataset</dc:type>\r\n"
                + "      <dc:format xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Shapefile</dc:format> \r\n"
                + "    </csw:Record>\r\n"
                + "    <csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw\">\r\n"
                + "      <dc:identifier xmlns:dc=\"http://purl.org/dc/elements/1.1/\">{23362852-F370-4369-B0B2-BE74B2859614}</dc:identifier>\r\n"
                + "      <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">mc2 title</dc:title>\r\n"
                + "      <dct:modified xmlns:dct=\"http://purl.org/dc/terms/\">2010-12-15</dct:modified>\r\n"
                + "      <dc:subject xmlns:dc=\"http://purl.org/dc/elements/1.1/\">first subject</dc:subject>\r\n"
                + "      <dc:subject xmlns:dc=\"http://purl.org/dc/elements/1.1/\">subject 2</dc:subject>\r\n"
                + "      <dct:abstract xmlns:dct=\"http://purl.org/dc/terms/\">mc2 abstract</dct:abstract>\r\n"
                + "      <dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">first copyright</dc:rights>\r\n"
                + "      <dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">second copyright</dc:rights>\r\n"
                + "      <dc:language xmlns:dc=\"http://purl.org/dc/elements/1.1/\">english</dc:language>\r\n"
                + "      <ows:BoundingBox xmlns:ows=\"http://www.opengis.net/ows\" crs=\"EPSG:RD_New (28992)\">\r\n"
                + "        <ows:LowerCorner>6.121 53.139</ows:LowerCorner>\r\n"
                + "        <ows:UpperCorner>5.468 53.517</ows:UpperCorner>\r\n"
                + "      </ows:BoundingBox>\r\n"
                + "      <dc:type xmlns:dc=\"http://purl.org/dc/elements/1.1/\">dataset 2</dc:type>\r\n"
                + "      <dc:format xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Shapefile 2</dc:format>\r\n"
                + "    </csw:Record>\r\n" + "  </csw:SearchResults>\r\n"
                + "</csw:GetRecordsResponse>";
        InputStream inStream = IOUtils.toInputStream(xml);
        CswRecordCollection cswRecords = (CswRecordCollection) xstream.fromXML(inStream);
        IOUtils.closeQuietly(inStream);

        List<Metacard> metacards = cswRecords.getCswRecords();
        assertThat(metacards, not(nullValue()));
        assertThat(metacards.size(), equalTo(2));

        // verify first metacard's values
        Metacard mc = metacards.get(0);
        Map<String, Object> expectedValues = new HashMap<String, Object>();
        expectedValues.put(Metacard.ID, "{8C1F6297-EC96-4302-A01E-14988C9149FD}");
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER,
                new String[] {"{8C1F6297-EC96-4302-A01E-14988C9149FD}"});
        expectedValues.put(Metacard.TITLE, "title 1");
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {"title 1"});
        String expectedModifiedDateStr = "2008-12-15";
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        Date expectedModifiedDate = dateFormatter.parseDateTime(expectedModifiedDateStr).toDate();
        expectedValues.put(CswRecordMetacardType.CSW_MODIFIED,
                new String[] {expectedModifiedDateStr});
        expectedValues.put(Metacard.MODIFIED, expectedModifiedDate);
        expectedValues.put(CswRecordMetacardType.CSW_SUBJECT, new String[] {"subject 1",
            "second subject"});
        expectedValues.put(CswRecordMetacardType.CSW_ABSTRACT, new String[] {"abstract 1"});
        expectedValues.put(CswRecordMetacardType.CSW_RIGHTS, new String[] {"copyright 1",
            "copyright 2"});
        expectedValues.put(CswRecordMetacardType.CSW_LANGUAGE, new String[] {"english"});
        expectedValues.put(CswRecordMetacardType.CSW_TYPE, "dataset");
        expectedValues.put(CswRecordMetacardType.CSW_FORMAT, new String[] {"Shapefile"});
        expectedValues.put(Metacard.GEOGRAPHY,
                "POLYGON((52.139 5.121, 52.517 5.121, 52.517 4.468, 52.139 4.468, 52.139 5.121))");
        expectedValues
                .put(CswRecordMetacardType.OWS_BOUNDING_BOX,
                        new String[] {"POLYGON((52.139 5.121, 52.517 5.121, 52.517 4.468, 52.139 4.468, 52.139 5.121))"});
        assertMetacard(mc, expectedValues);

        expectedValues.clear();

        // verify second metacard's values
        mc = metacards.get(1);
        expectedValues = new HashMap<String, Object>();
        expectedValues.put(Metacard.ID, "{23362852-F370-4369-B0B2-BE74B2859614}");
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER,
                new String[] {"{23362852-F370-4369-B0B2-BE74B2859614}"});
        expectedValues.put(Metacard.TITLE, "mc2 title");
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {"mc2 title"});
        expectedModifiedDateStr = "2010-12-15";
        dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        expectedModifiedDate = dateFormatter.parseDateTime(expectedModifiedDateStr).toDate();
        expectedValues.put(CswRecordMetacardType.CSW_MODIFIED,
                new String[] {expectedModifiedDateStr});
        expectedValues.put(Metacard.MODIFIED, expectedModifiedDate);
        expectedValues.put(CswRecordMetacardType.CSW_SUBJECT, new String[] {"first subject",
            "subject 2"});
        expectedValues.put(CswRecordMetacardType.CSW_ABSTRACT, new String[] {"mc2 abstract"});
        expectedValues.put(CswRecordMetacardType.CSW_RIGHTS, new String[] {"first copyright",
            "second copyright"});
        expectedValues.put(CswRecordMetacardType.CSW_LANGUAGE, new String[] {"english"});
        expectedValues.put(CswRecordMetacardType.CSW_TYPE, "dataset 2");
        expectedValues.put(CswRecordMetacardType.CSW_FORMAT, new String[] {"Shapefile 2"});
        expectedValues.put(Metacard.GEOGRAPHY,
                "POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))");
        expectedValues
                .put(CswRecordMetacardType.OWS_BOUNDING_BOX,
                        new String[] {"POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))"});
        assertMetacard(mc, expectedValues);

        expectedValues.clear();
    }

    @Test
    public void testGetRecordsResponseConversionWithEmptyBoundingBox() {
        XStream xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        
        RecordConverterFactory factory = new CswRecordConverterFactory();
        
        GetRecordsResponseConverter grrc = new GetRecordsResponseConverter(Arrays.asList(factory));
        grrc.setUnmarshalConverterSchema(CswConstants.CSW_OUTPUT_SCHEMA,
                getDefaultMetacardAttributeMappings(), CswConstants.SOURCE_URI_PRODUCT_RETRIEVAL,
                null, null, false);
        xstream.registerConverter(grrc);
        xstream.alias("GetRecordsResponse", CswRecordCollection.class);

        String xml = "<csw:GetRecordsResponse xmlns:csw=\"http://www.opengis.net/cat/csw\">\r\n"
                + "  <csw:SearchStatus status=\"subset\" timestamp=\"2013-05-01T02:13:36+0200\"/>\r\n"
                + "  <csw:SearchResults elementSet=\"full\" nextRecord=\"11\" numberOfRecordsMatched=\"479\" numberOfRecordsReturned=\"10\" recordSchema=\"csw:Record\">\r\n"
                + "    <csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw\">\r\n"
                + "      <dc:identifier xmlns:dc=\"http://purl.org/dc/elements/1.1/\">{8C1F6297-EC96-4302-A01E-14988C9149FD}</dc:identifier>\r\n"
                + "      <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">title 1</dc:title>\r\n"
                + "      <dct:modified xmlns:dct=\"http://purl.org/dc/terms/\">2008-12-15</dct:modified>\r\n"
                + "      <dc:subject xmlns:dc=\"http://purl.org/dc/elements/1.1/\">subject 1</dc:subject>\r\n"
                + "      <dc:subject xmlns:dc=\"http://purl.org/dc/elements/1.1/\">second subject</dc:subject>\r\n"
                + "      <dct:abstract xmlns:dct=\"http://purl.org/dc/terms/\">abstract 1</dct:abstract>\r\n"
                + "      <dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">copyright 1</dc:rights>\r\n"
                + "      <dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">copyright 2</dc:rights>\r\n"
                + "      <dc:language xmlns:dc=\"http://purl.org/dc/elements/1.1/\">english</dc:language>      \r\n"
                + "      <ows:BoundingBox xmlns:ows=\"http://www.opengis.net/ows\" crs=\"EPSG:RD_New (28992)\">\r\n"
                + "        <ows:LowerCorner></ows:LowerCorner>\r\n"
                + "        <ows:UpperCorner></ows:UpperCorner>\r\n"
                + "      </ows:BoundingBox>   \r\n"
                + "      <dc:type xmlns:dc=\"http://purl.org/dc/elements/1.1/\">dataset</dc:type>\r\n"
                + "      <dc:format xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Shapefile</dc:format> \r\n"
                + "    </csw:Record>\r\n" + "  </csw:SearchResults>\r\n"
                + "</csw:GetRecordsResponse>";
        InputStream inStream = IOUtils.toInputStream(xml);
        CswRecordCollection cswRecords = (CswRecordCollection) xstream.fromXML(inStream);
        IOUtils.closeQuietly(inStream);

        List<Metacard> metacards = cswRecords.getCswRecords();
        assertThat(metacards, not(nullValue()));
        assertThat(metacards.size(), equalTo(1));

        // verify first metacard's values
        Metacard mc = metacards.get(0);
        Map<String, Object> expectedValues = new HashMap<String, Object>();
        expectedValues.put(Metacard.ID, "{8C1F6297-EC96-4302-A01E-14988C9149FD}");
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER,
                new String[] {"{8C1F6297-EC96-4302-A01E-14988C9149FD}"});
        expectedValues.put(Metacard.TITLE, "title 1");
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {"title 1"});
        String expectedModifiedDateStr = "2008-12-15";
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        Date expectedModifiedDate = dateFormatter.parseDateTime(expectedModifiedDateStr).toDate();
        expectedValues.put(CswRecordMetacardType.CSW_MODIFIED,
                new String[] {expectedModifiedDateStr});
        expectedValues.put(Metacard.MODIFIED, expectedModifiedDate);
        expectedValues.put(CswRecordMetacardType.CSW_SUBJECT, new String[] {"subject 1",
            "second subject"});
        expectedValues.put(CswRecordMetacardType.CSW_ABSTRACT, new String[] {"abstract 1"});
        expectedValues.put(CswRecordMetacardType.CSW_RIGHTS, new String[] {"copyright 1",
            "copyright 2"});
        expectedValues.put(CswRecordMetacardType.CSW_LANGUAGE, new String[] {"english"});
        expectedValues.put(CswRecordMetacardType.CSW_TYPE, "dataset");
        expectedValues.put(CswRecordMetacardType.CSW_FORMAT, new String[] {"Shapefile"});
        expectedValues.put(Metacard.GEOGRAPHY, null);
        expectedValues.put(CswRecordMetacardType.OWS_BOUNDING_BOX, null);
        assertMetacard(mc, expectedValues);
    }

    @Test
    public void testMarshalRecordCollectionGetBrief() throws UnsupportedEncodingException, JAXBException {
        final int totalResults = 5;

        XStream xstream = createXStream(CswConstants.GET_RECORDS_RESPONSE);
        GetRecordsType getRecords = new GetRecordsType();
        QueryType query = new QueryType();
        ElementSetNameType set = new ElementSetNameType();
        set.setValue(ElementSetType.BRIEF);
        query.setElementSetName(set);
        ObjectFactory objectFactory = new ObjectFactory();
        getRecords.setAbstractQuery(objectFactory.createAbstractQuery(query));
        CswRecordCollection collection = createCswRecordCollection(getRecords, totalResults);
        
        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));
       
        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(totalResults));

        List<JAXBElement<? extends AbstractRecordType>> records = response.getSearchResults().getAbstractRecord();
        int counter = 1;
        for (JAXBElement<? extends AbstractRecordType> abRecord : records) {
            AbstractRecordType abstractRecord = abRecord.getValue();
            assertTrue(abstractRecord instanceof BriefRecordType);
            BriefRecordType briefRecord = (BriefRecordType) abstractRecord;
            assertThat(briefRecord.getIdentifier().get(0).getValue().getContent().get(0), equalTo(ID_PREFIX + counter));
            assertThat(briefRecord.getTitle().get(0).getValue().getContent().get(0), equalTo(TITLE_PREFIX + counter));
            assertThat(briefRecord.getType(), equalTo(null));
            
            counter++;
        }
    }

    @Test
    public void testMarshalRecordCollectionGetSummary() throws UnsupportedEncodingException, JAXBException {
        final int totalResults = 5;

        XStream xstream = createXStream(CswConstants.GET_RECORDS_RESPONSE);
        GetRecordsType getRecords = new GetRecordsType();
        QueryType query = new QueryType();
        ElementSetNameType set = new ElementSetNameType();
        set.setValue(ElementSetType.SUMMARY);
        query.setElementSetName(set);
        ObjectFactory objectFactory = new ObjectFactory();
        getRecords.setAbstractQuery(objectFactory.createAbstractQuery(query));
        CswRecordCollection collection = createCswRecordCollection(getRecords, totalResults);
        
        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));
       
        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(totalResults));

        List<JAXBElement<? extends AbstractRecordType>> records = response.getSearchResults().getAbstractRecord();
        int counter = 1;
        for (JAXBElement<? extends AbstractRecordType> abRecord : records) {
            AbstractRecordType abstractRecord = abRecord.getValue();
            assertTrue(abstractRecord instanceof SummaryRecordType);
            SummaryRecordType summaryRecord = (SummaryRecordType) abstractRecord;
            assertThat(summaryRecord.getIdentifier().size(), equalTo(1));
            assertThat(summaryRecord.getIdentifier().get(0).getValue().getContent().get(0), equalTo(ID_PREFIX + counter));
            assertThat(summaryRecord.getTitle().size(), equalTo(1));
            assertThat(summaryRecord.getTitle().get(0).getValue().getContent().get(0), equalTo(TITLE_PREFIX + counter));
            assertThat(summaryRecord.getFormat().size(), equalTo(2));
            assertThat(summaryRecord.getFormat().get(0).getValue().getContent().get(0), equalTo(FORMAT));
            assertThat(summaryRecord.getFormat().get(1).getValue().getContent().get(0), equalTo(FORMAT));
            assertThat(summaryRecord.getRelation().size(), equalTo(1));
            assertThat(summaryRecord.getRelation().get(0).getValue().getContent().get(0), equalTo(RELATION));
            assertThat(summaryRecord.getType(), equalTo(null));
            
            BoundingBoxType bBox = summaryRecord.getBoundingBox().get(0).getValue();
            
            assertThat(bBox.getLowerCorner().get(0), equalTo(Double.valueOf(2)));
            assertThat(bBox.getLowerCorner().get(1), equalTo(Double.valueOf(1)));
            assertThat(bBox.getUpperCorner().get(0), equalTo(Double.valueOf(4)));
            assertThat(bBox.getUpperCorner().get(1), equalTo(Double.valueOf(5)));
            
            counter++;
        }
    }

    @Test
    public void testMarshalRecordCollectionGetFull() throws UnsupportedEncodingException, JAXBException {
        final int totalResults = 5;

        XStream xstream = createXStream(CswConstants.GET_RECORDS_RESPONSE);
        GetRecordsType getRecords = new GetRecordsType();
        QueryType query = new QueryType();
        ElementSetNameType set = new ElementSetNameType();
        set.setValue(ElementSetType.FULL);
        query.setElementSetName(set);
        ObjectFactory objectFactory = new ObjectFactory();
        getRecords.setAbstractQuery(objectFactory.createAbstractQuery(query));
        CswRecordCollection collection = createCswRecordCollection(getRecords, totalResults);
        
        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));
       
        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(totalResults));

        List<JAXBElement<? extends AbstractRecordType>> records = response.getSearchResults().getAbstractRecord();
        int counter = 1;
        for (JAXBElement<? extends AbstractRecordType> abRecord : records) {
            AbstractRecordType abstractRecord = abRecord.getValue();
            assertTrue(abstractRecord instanceof RecordType);
            RecordType record = (RecordType) abstractRecord;
            List<JAXBElement<SimpleLiteral>> list = record.getDCElement();
            int titleCounter = 0;
            int idCounter = 0;
            int sourceCounter = 0;
            int relationCounter = 0;
            int formatCounter = 0;
            for (JAXBElement<SimpleLiteral> jaxbSL : list) {
                SimpleLiteral sl = jaxbSL.getValue();
                if (jaxbSL.getName().getLocalPart().equals(TITLE)) {
                    titleCounter++;
                    assertThat(sl.getContent().get(0), equalTo(TITLE_PREFIX + counter));
                } else if (jaxbSL.getName().getLocalPart().equals(ID)) {
                    idCounter++;
                    assertThat(sl.getContent().get(0), equalTo(ID_PREFIX + counter));
                } else if (jaxbSL.getName().getLocalPart().equals(SOURCE)) {
                    sourceCounter++;
                    assertThat(sl.getContent().get(0), equalTo(SOURCE_PREFIX + counter));
                } else if (jaxbSL.getName().getLocalPart().equals(RELATION)) {
                    relationCounter++;
                    assertThat(sl.getContent().get(0), equalTo(RELATION));
                } else if (jaxbSL.getName().getLocalPart().equals(FORMAT)) {
                    formatCounter++;
                    assertThat(sl.getContent().get(0), equalTo(FORMAT));
                } else {
                    fail("Unexpected Field: " + jaxbSL.getName().getLocalPart());
                }
            }
            
            assertThat(titleCounter, equalTo(1));
            assertThat(idCounter, equalTo(1));
            assertThat(sourceCounter, equalTo(1));
            assertThat(relationCounter, equalTo(1));
            assertThat(formatCounter, equalTo(2));

            counter++;
        }
    }

    @Test
    public void testMarshalRecordCollectionBoundingBox() throws UnsupportedEncodingException, JAXBException {
        final int totalResults = 1;
        final double minX = 2;
        final double minY = 1;
        final double maxX = 4;
        final double maxY = 5;

        XStream xstream = createXStream(CswConstants.GET_RECORDS_RESPONSE);
        GetRecordsType getRecords = new GetRecordsType();
        QueryType query = new QueryType();
        ElementSetNameType set = new ElementSetNameType();
        set.setValue(ElementSetType.FULL);
        query.setElementSetName(set);
        ObjectFactory objectFactory = new ObjectFactory();
        getRecords.setAbstractQuery(objectFactory.createAbstractQuery(query));
        CswRecordCollection collection = createCswRecordCollection(getRecords, totalResults);
        
        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));
       
        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(totalResults));

        List<JAXBElement<? extends AbstractRecordType>> records = response.getSearchResults().getAbstractRecord();
        AbstractRecordType abstractRecord = records.get(0).getValue();

        RecordType record = (RecordType) abstractRecord;

        List<JAXBElement<BoundingBoxType>> bBoxList = record.getBoundingBox();
        assertThat(bBoxList.size(), equalTo(1));
        BoundingBoxType bBox = bBoxList.get(0).getValue();
        
        assertThat(bBox.getLowerCorner().size(), equalTo(2));
        assertThat(bBox.getUpperCorner().size(), equalTo(2));

        assertThat(bBox.getUpperCorner().get(0), equalTo(maxX));
        assertThat(bBox.getUpperCorner().get(1), equalTo(maxY));
        assertThat(bBox.getLowerCorner().get(0), equalTo(minX));
        assertThat(bBox.getLowerCorner().get(1), equalTo(minY));
    }

    
    @Test
    public void testMarshalRecordCollectionGetElements() throws UnsupportedEncodingException, JAXBException {
        final int totalResults = 5;

        XStream xstream = createXStream(CswConstants.GET_RECORDS_RESPONSE);
        GetRecordsType getRecords = new GetRecordsType();
        QueryType query = new QueryType();
        List<QName> elements = new LinkedList<QName>();
        elements.add(CswRecordMetacardType.CSW_TITLE_QNAME);
        elements.add(CswRecordMetacardType.CSW_SOURCE_QNAME);
        query.setElementName(elements);
        
        ObjectFactory objectFactory = new ObjectFactory();
        getRecords.setAbstractQuery(objectFactory.createAbstractQuery(query));
        CswRecordCollection collection = createCswRecordCollection(getRecords, totalResults);
        
        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));
       
        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(totalResults));

        List<JAXBElement<? extends AbstractRecordType>> records = response.getSearchResults().getAbstractRecord();
        int counter = 1;
        for (JAXBElement<? extends AbstractRecordType> abRecord : records) {
            AbstractRecordType abstractRecord = abRecord.getValue();
            assertTrue(abstractRecord instanceof RecordType);
            RecordType record = (RecordType) abstractRecord;
            List<JAXBElement<SimpleLiteral>> list = record.getDCElement();
            int titleCounter = 0;
            int sourceCounter = 0;
            for (JAXBElement<SimpleLiteral> jaxbSL : list) {
                SimpleLiteral sl = jaxbSL.getValue();
                if (jaxbSL.getName().getLocalPart().equals(TITLE)) {
                    titleCounter++;
                    assertThat(sl.getContent().get(0), equalTo(TITLE_PREFIX + counter));
                } else if (jaxbSL.getName().getLocalPart().equals(SOURCE)) {
                    sourceCounter++;
                    assertThat(sl.getContent().get(0), equalTo(SOURCE_PREFIX + counter));
                } else {
                    fail("Unexpected Field: " + jaxbSL.getName().getLocalPart());
                }
            }
            
            assertThat(titleCounter, equalTo(1));
            assertThat(sourceCounter, equalTo(1));
 
            counter++;
        }
    }

    @Test
    public void testMarshalRecordCollectionGetFirstPage() throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 6;
        final int startPosition = 1;
        final int totalResults = 22;
        final int expectedNext = 7;
        final int expectedReturn = 6;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionGetMiddlePage() throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 6;
        final int startPosition = 4;
        final int totalResults = 22;
        final int expectedNext = 10;
        final int expectedReturn = 6;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionGetLastPage() throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 6;
        final int startPosition = 18;
        final int totalResults = 22;
        final int expectedNext = 0;
        final int expectedReturn = 5;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionGetAllOnePage() throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 23;
        final int startPosition = 1;
        final int totalResults = 22;
        final int expectedNext = 0;
        final int expectedReturn = 22;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionById() throws UnsupportedEncodingException,
        JAXBException {
        final int totalResults = 2;

        XStream xstream = createXStream(CswConstants.GET_RECORD_BY_ID_RESPONSE);
        CswRecordCollection collection = createCswRecordCollection(null, totalResults);
        collection.setById(true);

        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordByIdResponseType> jaxb = (JAXBElement<GetRecordByIdResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordByIdResponseType response = jaxb.getValue();
        assertThat(response.getAbstractRecord().size(), equalTo(totalResults));

        List<JAXBElement<? extends AbstractRecordType>> records = response.getAbstractRecord();
        int counter = 1;
        for (JAXBElement<? extends AbstractRecordType> abRecord : records) {
            AbstractRecordType abstractRecord = abRecord.getValue();
            assertTrue(abstractRecord instanceof RecordType);
            RecordType record = (RecordType) abstractRecord;
            List<JAXBElement<SimpleLiteral>> list = record.getDCElement();
            int titleCounter = 0;
            int idCounter = 0;
            int sourceCounter = 0;
            for (JAXBElement<SimpleLiteral> jaxbSL : list) {
                SimpleLiteral sl = jaxbSL.getValue();
                if (jaxbSL.getName().getLocalPart().equals(TITLE)) {
                    titleCounter++;
                    assertThat(sl.getContent().get(0), equalTo(TITLE_PREFIX + counter));
                } else if (jaxbSL.getName().getLocalPart().equals(ID)) {
                    idCounter++;
                    assertThat(sl.getContent().get(0), equalTo(ID_PREFIX + counter));
                } else if (jaxbSL.getName().getLocalPart().equals(SOURCE)) {
                    sourceCounter++;
                    assertThat(sl.getContent().get(0), equalTo(SOURCE_PREFIX + counter));
                } else {
                    fail("Unexpected Field: " + jaxbSL.getName().getLocalPart());
                }
            }

            assertThat(titleCounter, equalTo(1));
            assertThat(idCounter, equalTo(1));
            assertThat(sourceCounter, equalTo(1));

            counter++;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////

    private void getRecords(final int maxRecords, final int startPosition, final int totalResults,
            final int expectedNext, final int expectedReturn) throws JAXBException,
        UnsupportedEncodingException {
        XStream xstream = createXStream(CswConstants.GET_RECORDS_RESPONSE);
        GetRecordsType query = new GetRecordsType();
        query.setMaxRecords(BigInteger.valueOf(maxRecords));
        query.setStartPosition(BigInteger.valueOf(startPosition));
        CswRecordCollection collection = createCswRecordCollection(query, totalResults);
        
        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));
       
        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getNumberOfRecordsMatched().intValue(), equalTo(totalResults));
        assertThat(response.getSearchResults().getNumberOfRecordsReturned().intValue(), equalTo(expectedReturn));
        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(expectedReturn));
        assertThat(response.getSearchResults().getNextRecord().intValue(), equalTo(expectedNext));
    }

    private void assertMetacard(Metacard mc, Map<String, Object> expectedValues) {
        assertThat(mc.getId(), equalTo((String) expectedValues.get(Metacard.ID)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_IDENTIFIER,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_IDENTIFIER));
        assertThat(mc.getTitle(), equalTo((String) expectedValues.get(Metacard.TITLE)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_TITLE,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_TITLE));
        assertThat(mc.getModifiedDate(), equalTo((Date) expectedValues.get(Metacard.MODIFIED)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_MODIFIED,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_MODIFIED));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_SUBJECT,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_SUBJECT));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_ABSTRACT,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_ABSTRACT));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_RIGHTS,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_RIGHTS));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_LANGUAGE,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_LANGUAGE));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_TYPE).getValue(),
                equalTo((String) expectedValues.get(CswRecordMetacardType.CSW_TYPE)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_FORMAT,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_FORMAT));
        assertThat(mc.getLocation(), equalTo((String) expectedValues.get(Metacard.GEOGRAPHY)));
        assertListStringAttribute(mc, CswRecordMetacardType.OWS_BOUNDING_BOX,
                (String[]) expectedValues.get(CswRecordMetacardType.OWS_BOUNDING_BOX));
    }

    private void assertListStringAttribute(Metacard mc, String attrName, String[] expectedValues) {
        if (mc.getAttribute(attrName) != null) {
            List<?> values = (List<?>) mc.getAttribute(attrName).getValues();
            assertThat(values, not(nullValue()));
            assertThat(values.size(), equalTo(expectedValues.length));

            List<String> valuesList = new ArrayList<String>();
            valuesList.addAll((List<? extends String>) values);
            assertThat(valuesList, hasItems(expectedValues));
        }
    }

    private Map<String, String> getDefaultMetacardAttributeMappings() {
        Map<String, String> metacardAttributeMappings = new HashMap<String, String>();
        metacardAttributeMappings.put(Metacard.EFFECTIVE, CswRecordMetacardType.CSW_CREATED);
        metacardAttributeMappings.put(Metacard.CREATED, CswRecordMetacardType.CSW_DATE_SUBMITTED);
        metacardAttributeMappings.put(Metacard.MODIFIED, CswRecordMetacardType.CSW_MODIFIED);
        return metacardAttributeMappings;

    }

    private XStream createXStream(final String elementName) {
        RecordConverterFactory factory = new CswRecordConverterFactory();
        GetRecordsResponseConverter rrConverter = new GetRecordsResponseConverter(
                Arrays.asList(factory));

        XStream xstream = new XStream(new StaxDriver(new NoNameCoder()));

        xstream.registerConverter(rrConverter);

        xstream.alias(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                + elementName, CswRecordCollection.class);
        return xstream;
    }

    private CswRecordCollection createCswRecordCollection(GetRecordsType request, int resultCount) {
        CswRecordCollection collection = new CswRecordCollection();
        
        int first = 1;
        int last = 2;
        if (request != null) {

            first = request.getStartPosition().intValue();
            int next = request.getMaxRecords().intValue() + first;
            last = next - 1;
            if (last >= resultCount) {
                last = resultCount;
                next = 0;
            }
        }
        int returned = last - first + 1;

        collection.setCswRecords(createMetacardList(first, last));
        collection.setNumberOfRecordsMatched(resultCount);
        collection.setNumberOfRecordsReturned(returned);
        collection.setRequest(request);
        return collection;
    }
    
    private List<Metacard> createMetacardList(int start, int finish) {
        List<Metacard> list = new LinkedList<Metacard>();
        
        for (int i = start; i <= finish; i++) {
            MetacardImpl metacard = new MetacardImpl();
            
            metacard.setId(ID_PREFIX + i);
            metacard.setSourceId(SOURCE_PREFIX + i);
            metacard.setTitle(TITLE_PREFIX + i);
            // for testing a attribute with multiple values
            AttributeDescriptor ad = new AttributeDescriptorImpl(FORMAT, true, true, true, true,
                    BasicTypes.STRING_TYPE);
            Set<AttributeDescriptor> ads = new HashSet<AttributeDescriptor>(metacard
                    .getMetacardType().getAttributeDescriptors());
            ads.add(ad);
            metacard.setType(new MetacardTypeImpl("test", ads));
            metacard.setLocation(WKT);
            AttributeImpl attr = new AttributeImpl(FORMAT, FORMAT);
            attr.addValue(FORMAT);
            metacard.setAttribute(attr);
            // for testing a attribute with no attribute descriptor
            metacard.setAttribute(RELATION, RELATION);
                
            list.add(metacard);
        }
        
        return list;
    }
    
    private JAXBContext getJaxBContext() throws JAXBException {
        JAXBContext context = null;
        String contextPath = StringUtils.join(new String[] {
            CswConstants.OGC_CSW_PACKAGE, CswConstants.OGC_FILTER_PACKAGE, 
            CswConstants.OGC_GML_PACKAGE, CswConstants.OGC_OWS_PACKAGE}, ":");

        context = JAXBContext.newInstance(contextPath,
                CswJAXBElementProvider.class.getClassLoader());
        return context;
    }    
}
