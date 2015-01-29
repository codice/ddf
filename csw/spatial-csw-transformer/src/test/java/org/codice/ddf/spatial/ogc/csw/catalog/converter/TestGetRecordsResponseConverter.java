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
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import com.thoughtworks.xstream.io.xml.XppReader;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.InputTransformer;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.cat.csw.v_2_0_2.SearchResultsType;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestGetRecordsResponseConverter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestGetRecordsResponseConverter.class);

    private static final String ID_PREFIX = "id_";

    private static final String SOURCE_PREFIX = "source_";

    private static final String TITLE_PREFIX = "title ";

    private static final String ID = "identifier";

    private static final String SOURCE = "source";

    private static final String PUBLISHER = "publisher";

    private static final String TITLE = "title";

    private static final String FORMAT = "format";

    private static final String RELATION = "relation";

    private static final String WKT = "POLYGON((4 1, 2 5, 4 5, 4 1))";

    private CswTransformProvider mockProvider = mock(CswTransformProvider.class);

    private TransformerManager mockInputManager = mock(TransformerManager.class);

    @Before
    public void setUp() {
        when(mockProvider.canConvert(any(Class.class))).thenReturn(true);

    }

    /**
     * This test acutally runs the full thread of calling the GetRecordsResponseConverter then calls the CswInputTransformer.
     */
    @Test
    public void testUnmarshalGetRecordsResponseFull() {

        XStream xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());

        CswTransformProvider provider = new CswTransformProvider(null, mockInputManager);

        when(mockInputManager.getTransformerBySchema(anyString()))
                .thenReturn(new CswRecordConverter(null));

        xstream.registerConverter(new GetRecordsResponseConverter(provider));
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
        assertThat(mc, not(nullValue()));
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
                        new String[] {
                                "POLYGON((52.139 5.121, 52.517 5.121, 52.517 4.468, 52.139 4.468, 52.139 5.121))"});
        assertMetacard(mc, expectedValues);

        expectedValues.clear();

        // verify second metacard's values
        mc = metacards.get(1);
        assertThat(mc, not(nullValue()));
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
                        new String[] {
                                "POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))"});
        assertMetacard(mc, expectedValues);

        expectedValues.clear();
    }

    @Test
    public void testUnmarshalParseXmlNamespaces() throws XmlPullParserException {

        XStream xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());




        xstream.registerConverter(new GetRecordsResponseConverter(mockProvider));
        xstream.alias("csw:GetRecordsResponse", CswRecordCollection.class);

        String xml =
                "<?xml version='1.0' encoding='UTF-8'?>"
                        + "<csw:GetRecordsResponse "
                        + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                        + "xmlns:dct=\"http://purl.org/dc/terms/\" "
                        + "xmlns:ows=\"http://www.opengis.net/ows\" "
                        + "xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" "
                        + "version=\"2.0.2\"><csw:SearchStatus "
                        + "timestamp=\"2014-11-11T10:53:32.152-06:00\"/>"
                        + "<csw:SearchResults numberOfRecordsMatched=\"1\" "
                        + "numberOfRecordsReturned=\"1\" nextRecord=\"0\" "
                        + "recordSchema=\"http://www.opengis.net/cat/csw/2.0.2\">"
                        + "<csw:Record>\n"
                        + "<dc:identifier>0a2e1b1d2a3755e70a96d61e6bddbc5d</dc:identifier>"
                        + "<dct:bibliographicCitation>0a2e1b1d2a3755e70a96d61e6bddbc5d</dct:bibliographicCitation>"
                        + "<dc:title>US woman attacks Gauguin painting</dc:title>"
                        + "<dct:alternative>US woman attacks Gauguin painting</dct:alternative>"
                        + "<dc:type>video</dc:type><dc:date>2011-04-06T04:49:20.230-05:00</dc:date>"
                        + "<dct:modified>2011-04-06T04:49:20.230-05:00</dct:modified>"
                        + "<dct:created>2011-04-06T04:49:20.230-05:00</dct:created>"
                        + "<dct:dateAccepted>2011-04-06T04:48:26.180-05:00</dct:dateAccepted>"
                        + "<dct:dateCopyrighted>2011-04-06T04:48:26.180-05:00</dct:dateCopyrighted><"
                        + "dct:dateSubmitted>2011-04-06T04:49:20.230-05:00</dct:dateSubmitted>"
                        + "<dct:issued>2011-04-06T04:49:20.230-05:00</dct:issued>"
                        + "<dc:publisher>ddf.distribution</dc:publisher>"
                        + "<ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                        + "    <ows:LowerCorner>-50.5056430529222 84.0285103635943</ows:LowerCorner>"
                        + "<ows:UpperCorner>-50.5056430529222 84.0285103635943</ows:UpperCorner>"
                        + "</ows:BoundingBox></csw:Record><"
                        + "/csw:SearchResults>"
                        + "</csw:GetRecordsResponse>";
        InputStream inStream = IOUtils.toInputStream(xml);

        ArgumentCaptor<UnmarshallingContext> captor = ArgumentCaptor
                .forClass(UnmarshallingContext.class);

        HierarchicalStreamReader reader = new XppReader(new InputStreamReader(inStream),
                XmlPullParserFactory.newInstance().newPullParser());
        xstream.unmarshal(reader, null, null);
        IOUtils.closeQuietly(inStream);

        verify(mockProvider, times(1))
                .unmarshal(any(HierarchicalStreamReader.class), captor.capture());

        UnmarshallingContext context = captor.getValue();

        assertThat(context, notNullValue());
        assertThat(context.get(CswConstants.WRITE_NAMESPACES), is(Map.class));
        Map<String, String> namespaces = (Map) context.get(CswConstants.WRITE_NAMESPACES);
        assertThat(namespaces.get(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.CSW_NAMESPACE_PREFIX), is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat(namespaces.get(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX), is(CswConstants.DUBLIN_CORE_SCHEMA));
        assertThat(namespaces.get(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX),
                is(CswConstants.DUBLIN_CORE_TERMS_SCHEMA));
        assertThat(namespaces.get(CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.OWS_NAMESPACE_PREFIX), is(CswConstants.OWS_NAMESPACE));


    }

    @Test
    public void testUnmarshalGetRecordsResponseConversionWithEmptyBoundingBox() {
        XStream xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());

        GetRecordsResponseConverter grrc = new GetRecordsResponseConverter(mockProvider);
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

        assertThat(cswRecords.getNumberOfRecordsReturned(), is(10L));
        assertThat(cswRecords.getNumberOfRecordsMatched(), is(479L));

        List<Metacard> metacards = cswRecords.getCswRecords();
        assertThat(metacards, not(nullValue()));
        assertThat(metacards.size(), equalTo(1));
    }

    @Test
    public void testMarshalRecordCollectionGetBrief()
            throws UnsupportedEncodingException, JAXBException {
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
        collection.setElementSetType(ElementSetType.BRIEF);

        ArgumentCaptor<MarshallingContext> captor = ArgumentCaptor
                .forClass(MarshallingContext.class);

        String xml = xstream.toXML(collection);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(totalResults))
                .marshal(any(Object.class), any(HierarchicalStreamWriter.class), captor.capture());

        MarshallingContext context = captor.getValue();
        assertThat(context, not(nullValue()));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat((ElementSetType) context.get(CswConstants.ELEMENT_SET_TYPE),
                is(ElementSetType.BRIEF));

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
        SearchResultsType resultsType = response.getSearchResults();
        assertThat(resultsType, not(nullValue()));
        assertThat(resultsType.getElementSet(), is(ElementSetType.BRIEF));
        assertThat(resultsType.getNumberOfRecordsMatched().intValue(), is(totalResults));
        assertThat(resultsType.getNumberOfRecordsReturned().intValue(), is(totalResults));
        assertThat(resultsType.getRecordSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testMarshalRecordCollectionGetSummary()
            throws UnsupportedEncodingException, JAXBException {
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
        collection.setElementSetType(ElementSetType.SUMMARY);

        ArgumentCaptor<MarshallingContext> captor = ArgumentCaptor
                .forClass(MarshallingContext.class);

        String xml = xstream.toXML(collection);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(totalResults))
                .marshal(any(Object.class), any(HierarchicalStreamWriter.class), captor.capture());

        MarshallingContext context = captor.getValue();
        assertThat(context, not(nullValue()));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat((ElementSetType) context.get(CswConstants.ELEMENT_SET_TYPE),
                is(ElementSetType.SUMMARY));

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
        SearchResultsType resultsType = response.getSearchResults();
        assertThat(resultsType, not(nullValue()));
        assertThat(resultsType.getElementSet(), is(ElementSetType.SUMMARY));
        assertThat(resultsType.getNumberOfRecordsMatched().intValue(), is(totalResults));
        assertThat(resultsType.getNumberOfRecordsReturned().intValue(), is(totalResults));
        assertThat(resultsType.getRecordSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testMarshalRecordCollectionGetFull()
            throws UnsupportedEncodingException, JAXBException {
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
        collection.setElementSetType(ElementSetType.FULL);

        ArgumentCaptor<MarshallingContext> captor = ArgumentCaptor
                .forClass(MarshallingContext.class);

        String xml = xstream.toXML(collection);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(totalResults))
                .marshal(any(Object.class), any(HierarchicalStreamWriter.class), captor.capture());

        MarshallingContext context = captor.getValue();
        assertThat(context, not(nullValue()));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat((ElementSetType) context.get(CswConstants.ELEMENT_SET_TYPE),
                is(ElementSetType.FULL));

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
        SearchResultsType resultsType = response.getSearchResults();
        assertThat(resultsType, not(nullValue()));
        assertThat(resultsType.getElementSet(), is(ElementSetType.FULL));
        assertThat(resultsType.getNumberOfRecordsMatched().intValue(), is(totalResults));
        assertThat(resultsType.getNumberOfRecordsReturned().intValue(), is(totalResults));
        assertThat(resultsType.getRecordSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
    }


    @Test
    public void testMarshalRecordCollectionHits()
            throws UnsupportedEncodingException, JAXBException {
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
        collection.setElementSetType(ElementSetType.FULL);
        collection.setResultType(ResultType.HITS);

        String xml = xstream.toXML(collection);

        // Verify the context arguments were set correctly
        verify(mockProvider, never())
                .marshal(any(Object.class), any(HierarchicalStreamWriter.class), any(MarshallingContext.class));

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
        SearchResultsType resultsType = response.getSearchResults();
        assertThat(resultsType, not(nullValue()));
        assertThat(resultsType.getElementSet(), is(ElementSetType.FULL));
        assertThat(resultsType.getNumberOfRecordsMatched().intValue(), is(totalResults));
        assertThat(resultsType.getNumberOfRecordsReturned().intValue(), is(0));
        assertThat(resultsType.getRecordSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testMarshalRecordCollectionGetElements()
            throws UnsupportedEncodingException, JAXBException {
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
        collection.setElementName(elements);
        ArgumentCaptor<MarshallingContext> captor = ArgumentCaptor
                .forClass(MarshallingContext.class);

        String xml = xstream.toXML(collection);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(totalResults))
                .marshal(any(Object.class), any(HierarchicalStreamWriter.class), captor.capture());

        MarshallingContext context = captor.getValue();
        assertThat(context, not(nullValue()));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat((ElementSetType) context.get(CswConstants.ELEMENT_SET_TYPE), is(nullValue()));
        assertThat(context.get(CswConstants.ELEMENT_NAMES), is(notNullValue()));
        List<QName> qnames = (List<QName>) context.get(CswConstants.ELEMENT_NAMES);
        assertThat(qnames.contains(CswRecordMetacardType.CSW_TITLE_QNAME), is(true));
        assertThat(qnames.contains(CswRecordMetacardType.CSW_SOURCE_QNAME), is(true));

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
        SearchResultsType resultsType = response.getSearchResults();
        assertThat(resultsType, not(nullValue()));
        assertThat(resultsType.getElementSet(), is(nullValue()));
        assertThat(resultsType.getNumberOfRecordsMatched().intValue(), is(totalResults));
        assertThat(resultsType.getNumberOfRecordsReturned().intValue(), is(totalResults));
        assertThat(resultsType.getRecordSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testMarshalRecordCollectionGetFirstPage()
            throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 6;
        final int startPosition = 1;
        final int totalResults = 22;
        final int expectedNext = 7;
        final int expectedReturn = 6;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionGetMiddlePage()
            throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 6;
        final int startPosition = 4;
        final int totalResults = 22;
        final int expectedNext = 10;
        final int expectedReturn = 6;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionGetLastPage()
            throws UnsupportedEncodingException, JAXBException {
        final int maxRecords = 6;
        final int startPosition = 18;
        final int totalResults = 22;
        final int expectedNext = 0;
        final int expectedReturn = 5;

        getRecords(maxRecords, startPosition, totalResults, expectedNext, expectedReturn);
    }

    @Test
    public void testMarshalRecordCollectionGetAllOnePage()
            throws UnsupportedEncodingException, JAXBException {
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

        ArgumentCaptor<MarshallingContext> captor = ArgumentCaptor
                .forClass(MarshallingContext.class);

        String xml = xstream.toXML(collection);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(totalResults))
                .marshal(any(Object.class), any(HierarchicalStreamWriter.class), captor.capture());

        MarshallingContext context = captor.getValue();
        assertThat(context, not(nullValue()));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat((ElementSetType) context.get(CswConstants.ELEMENT_SET_TYPE), is(nullValue()));
        assertThat(context.get(CswConstants.ELEMENT_NAMES), is(nullValue()));

        JAXBElement<GetRecordByIdResponseType> jaxb = (JAXBElement<GetRecordByIdResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordByIdResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
    }

    @Test
    public void testMarshalRecordCollectionFullXml()
            throws UnsupportedEncodingException, JAXBException {
        final int totalResults = 5;

        TransformerManager mockMetacardManager = mock(TransformerManager.class);
        when(mockMetacardManager.getTransformerBySchema(anyString())).thenReturn(
                new CswRecordConverter(null));
        GetRecordsResponseConverter rrConverter = new GetRecordsResponseConverter(new CswTransformProvider(mockMetacardManager, null));

        XStream xstream = new XStream(new StaxDriver(new NoNameCoder()));

        xstream.registerConverter(rrConverter);

        xstream.alias(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.GET_RECORDS_RESPONSE, CswRecordCollection.class);

        GetRecordsType getRecords = new GetRecordsType();
        QueryType query = new QueryType();
        ElementSetNameType set = new ElementSetNameType();
        set.setValue(ElementSetType.FULL);
        query.setElementSetName(set);
        ObjectFactory objectFactory = new ObjectFactory();
        getRecords.setAbstractQuery(objectFactory.createAbstractQuery(query));
        CswRecordCollection collection = createCswRecordCollection(getRecords, totalResults);
        collection.setElementSetType(ElementSetType.FULL);

        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        // Assert the GetRecordsResponse elements and attributes
        assertThat(response, not(nullValue()));
        SearchResultsType resultsType = response.getSearchResults();
        assertThat(resultsType, not(nullValue()));
        assertThat(resultsType.getElementSet(), is(ElementSetType.FULL));
        assertThat(resultsType.getNumberOfRecordsMatched().intValue(), is(totalResults));
        assertThat(resultsType.getNumberOfRecordsReturned().intValue(), is(totalResults));
        assertThat(resultsType.getRecordSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
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
        collection.setStartPosition(startPosition);

        String xml = xstream.toXML(collection);

        JAXBElement<GetRecordsResponseType> jaxb = (JAXBElement<GetRecordsResponseType>) getJaxBContext()
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        GetRecordsResponseType response = jaxb.getValue();
        assertThat(response.getSearchResults().getNumberOfRecordsMatched().intValue(),
                equalTo(totalResults));
        assertThat(response.getSearchResults().getNumberOfRecordsReturned().intValue(),
                equalTo(expectedReturn));
        //        assertThat(response.getSearchResults().getAbstractRecord().size(), equalTo(expectedReturn));
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
        GetRecordsResponseConverter rrConverter = new GetRecordsResponseConverter(mockProvider);

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
        //        collection.setRequest(request);
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
