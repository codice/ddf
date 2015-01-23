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
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.geotools.filter.FilterFactoryImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCswSource extends TestCswSourceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCswSource.class);

    private CswTransformProvider mockProvider = mock(CswTransformProvider.class);


    @Test
    public void testParseCapabilities() throws CswException {
        CswSource source = getCswSource(createRemoteCsw(), mockContext, new ArrayList<String>());

        assertTrue(source.isAvailable());
        assertEquals(10, source.getContentTypes().size());
        Set<ContentType> expected = generateContentType(Arrays.asList("a", "b", "c",
                "d", "e", "f", "g", "h", "i", "j"));
        assertThat(source.getContentTypes(), is(expected));
    }

    @Test
    public void testInitialContentList() throws CswException {

        CswSource source = getCswSource(createRemoteCsw(), mockContext, Arrays.asList("x", "y"));

        assertTrue(source.isAvailable());
        assertEquals(12, source.getContentTypes().size());
        Set<ContentType> expected = generateContentType(Arrays.asList("a", "b", "c",
            "d", "e", "f", "g", "h", "i", "j", "x", "y"));
        assertThat(source.getContentTypes(), is(expected));
    }

    @Test
    public void testAddingContentTypesOnQueries() throws CswException, UnsupportedQueryException {
        RemoteCsw remote = createRemoteCsw();

        List<String> expectedNames = new LinkedList<String>(Arrays.asList("a", "b", "c",
                "d", "e", "f", "g", "h", "i", "j"));

        ServiceRegistration<?> mockRegisteredMetacardType = (ServiceRegistration<?>) mock(ServiceRegistration.class);
        LOGGER.info("mockRegisteredMetacardType: {}", mockRegisteredMetacardType);
        doReturn(mockRegisteredMetacardType).when(mockContext).registerService(
                eq(MetacardType.class.getName()), any(CswRecordMetacardType.class),
                Matchers.<Dictionary<String, ?>> any());
        ServiceReference<?> mockServiceReference = (ServiceReference<?>) mock(ServiceReference.class);
        doReturn(mockServiceReference).when(mockRegisteredMetacardType).getReference();
        when(mockServiceReference.getProperty(eq(Metacard.CONTENT_TYPE))).thenReturn(expectedNames);

        CswSource source = getCswSource(remote, mockContext, new ArrayList<String>());

        assertEquals(10, source.getContentTypes().size());

        Set<ContentType> expected = generateContentType(expectedNames);
        assertThat(source.getContentTypes(), is(expected));

        CswRecordCollection collection = generateCswCollection("/getRecordsResponse.xml");

        when(remote.getRecords(any(GetRecordsType.class))).thenReturn(collection);

        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is()
                .like().text("*"));

        expectedNames.add("dataset");
        expectedNames.add("dataset 2");
        expected = generateContentType(expectedNames);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));

        assertEquals(12, source.getContentTypes().size());
        assertThat(source.getContentTypes(), is(expected));
    }

    @Test
    public void testPropertyIsLikeQuery() throws JAXBException, UnsupportedQueryException,
        DatatypeConfigurationException, SAXException, IOException {

        // Setup
        final String searchPhrase = "*th*e";
        final int pageSize = 10;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 10;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is()
                .like().text(searchPhrase));
        propertyIsLikeQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        SourceResponse response = cswSource.query(new QueryRequestImpl(propertyIsLikeQuery));

        // Verify
        Assert.assertNotNull(response);
        assertThat(response.getResults().size(), is(numRecordsReturned));
        assertThat(response.getHits(), is(numRecordsMatched));
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        String xml = getGetRecordsTypeAsXml(getRecordsType, CswConstants.VERSION_2_0_2);
        LOGGER.debug(xml);
        assertXMLEqual(xml, getRecordsControlXml202);
    }

    @Test
    public void testQueryWithSorting() throws JAXBException, UnsupportedQueryException,
        DatatypeConfigurationException, SAXException, IOException {

        final String TITLE = "title";

        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is()
                .like().text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        SourceResponse response = cswSource.query(new QueryRequestImpl(query));

        // Verify
        Assert.assertNotNull(response);
        assertThat(response.getResults().size(), is(numRecordsReturned));
        assertThat(response.getHits(), is(numRecordsMatched));
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
        assertThat(cswQuery.getSortBy().getSortProperty().size(), is(1));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getPropertyName().getContent()
                .get(0).toString(), equalTo(TITLE));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(),
                is(SortOrderType.DESC));
    }

    @Test
    public void testQueryWithSortByDistance() throws JAXBException, UnsupportedQueryException,
        DatatypeConfigurationException, SAXException, IOException {

        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like()
                .text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        SourceResponse response = cswSource.query(new QueryRequestImpl(query));

        // Verify
        Assert.assertNotNull(response);
        assertThat(response.getResults().size(), is(numRecordsReturned));
        assertThat(response.getHits(), is(numRecordsMatched));
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
        assertThat(cswQuery.getSortBy(), nullValue());
    }

    @Test
    public void testQueryWithSortByRelevance() throws JAXBException, UnsupportedQueryException,
        DatatypeConfigurationException, SAXException, IOException {
        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like()
                .text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        SourceResponse response = cswSource.query(new QueryRequestImpl(query));

        // Verify
        Assert.assertNotNull(response);
        assertThat(response.getResults().size(), is(numRecordsReturned));
        assertThat(response.getHits(), is(numRecordsMatched));
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
        assertThat(cswQuery.getSortBy().getSortProperty().size(), is(1));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getPropertyName().getContent()
                .get(0).toString(), equalTo(Metacard.TITLE));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(),
                is(SortOrderType.DESC));
    }

    @Test
    public void testQueryWithSortByTemporal() throws JAXBException, UnsupportedQueryException,
        DatatypeConfigurationException, SAXException, IOException {
        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like()
                .text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        SourceResponse response = cswSource.query(new QueryRequestImpl(query));

        // Verify
        Assert.assertNotNull(response);
        assertThat(response.getResults().size(), is(numRecordsReturned));
        assertThat(response.getHits(), is(numRecordsMatched));
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
        assertThat(cswQuery.getSortBy().getSortProperty().size(), is(1));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getPropertyName().getContent()
                .get(0).toString(), equalTo(Metacard.MODIFIED));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(),
                is(SortOrderType.DESC));
    }

    /**
     * Test to verify content type mapping is configurable.
     * The CSW Source should be able to map a csw:Record field to Content Type.
     */
    @Test
    public void testPropertyIsEqualToQueryContentTypeIsMappedToFormat() throws JAXBException,
        UnsupportedQueryException, DatatypeConfigurationException, SAXException, IOException {

        // Setup
        int pageSize = 10;
        int numRecordsReturned = 1;
        long numRecordsMatched = 1;
        String format = "myContentType";

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl propertyIsEqualToQuery = new QueryImpl(builder.attribute(Metacard.CONTENT_TYPE)
                .is().text(format));
        propertyIsEqualToQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>(), CswRecordMetacardType.CSW_FORMAT);
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        cswSource.query(new QueryRequestImpl(propertyIsEqualToQuery));

        // Verify
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        // getRecords() is called two times. Once for initial CSW source
        // configuration and
        // a second time for the actual content type query.
        try {
            verify(mockCsw, times(2)).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify Mock CSW record count " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        String xml = getGetRecordsTypeAsXml(getRecordsType, CswConstants.VERSION_2_0_2);
        LOGGER.debug(xml);
        assertXMLEqual(xml, getRecordsControlXml202ContentTypeMappedToFormat);
    }

    /**
     * Test to verify content type version mapping is correct. The CSW Source should be able to map
     * a csw:Record field to Content Type.
     */
    @Test
    public void testPropertyIsLikeContentTypeVersion() throws JAXBException,
        UnsupportedQueryException, DatatypeConfigurationException, SAXException, IOException {

        // Setup
        int pageSize = 10;
        int numRecordsReturned = 1;
        long numRecordsMatched = 1;
        String format = "myContentType";
        String version = "2.0";

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        Filter ctfilter = builder.attribute(Metacard.CONTENT_TYPE).is().text(format);
        Filter versionFilter = builder.attribute(Metacard.CONTENT_TYPE_VERSION).is().like()
                .text(version);
        Filter filter = builder.allOf(ctfilter, versionFilter);

        QueryImpl propertyIsEqualToQuery = new QueryImpl(filter);
        propertyIsEqualToQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>(),
                CswRecordMetacardType.CSW_FORMAT);
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        cswSource.query(new QueryRequestImpl(propertyIsEqualToQuery));

        // Verify
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        // getRecords() is called two times. Once for initial CSW source
        // configuration and
        // a second time for the actual content type query.
        try {
            verify(mockCsw, times(2)).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify Mock CSW record count " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        String xml = getGetRecordsTypeAsXml(getRecordsType, CswConstants.VERSION_2_0_2);
        LOGGER.debug(xml);
        assertXMLEqual(xml, getRecordsControlXml202ContentTypeMappedToFormat);
    }

    @Test
    public void testAbsoluteTemporalSearchPropertyIsBetweenQuery() throws JAXBException,
        UnsupportedQueryException, DatatypeConfigurationException, SAXException, IOException {

        // Setup
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">\r\n"
                + "    <ns10:Query typeNames=\"Record\" xmlns=\"\" xmlns:ns10=\"http://www.opengis.net/cat/csw/2.0.2\">\r\n"
                + "        <ns10:ElementSetName>full</ns10:ElementSetName>\r\n"
                + "        <ns10:Constraint version=\"1.1.0\">\r\n"
                + "            <ns2:Filter>\r\n" + "                <ns2:PropertyIsBetween>\r\n"
                + "                    <ns2:PropertyName>effective</ns2:PropertyName>\r\n"
                + "                    <ns2:LowerBoundary>\r\n"
                + "                        <ns2:Literal>START_DATE_TIME</ns2:Literal>\r\n"
                + "                    </ns2:LowerBoundary>\r\n"
                + "                    <ns2:UpperBoundary>\r\n"
                + "                        <ns2:Literal>END_DATE_TIME</ns2:Literal>\r\n"
                + "                    </ns2:UpperBoundary>\r\n"
                + "                </ns2:PropertyIsBetween>\r\n" + "            </ns2:Filter>\r\n"
                + "        </ns10:Constraint>\r\n" + "    </ns10:Query>\r\n" + "</GetRecords>";

        final int pageSize = 10;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 10;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        // Create start and end date times that are before current time
        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2013, 12, 31, 0, 0, 0, 0);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        // Load the expected start and end date time into the excepted result
        // XML
        expectedXml = expectedXml.replace("START_DATE_TIME", fmt.print(startDate));
        expectedXml = expectedXml.replace("END_DATE_TIME", fmt.print(endDate));

        // Single absolute time range to search across
        Filter temporalFilter = builder.attribute(Metacard.EFFECTIVE).is().during()
                .dates(startDate.toDate(), endDate.toDate());
        QueryImpl temporalQuery = new QueryImpl(temporalFilter);
        temporalQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        cswSource.setEffectiveDateMapping(Metacard.EFFECTIVE);

        // Perform test
        cswSource.query(new QueryRequestImpl(temporalQuery));

        // Verify
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify Mock CSW record count " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        String xml = getGetRecordsTypeAsXml(getRecordsType, CswConstants.VERSION_2_0_2);
        LOGGER.debug(xml);
        assertXMLEqual(expectedXml, xml);
    }

    @Test
    public void testAbsoluteTemporalSearchTwoRanges() throws JAXBException,
        UnsupportedQueryException, DatatypeConfigurationException, SAXException, IOException {

        // Setup
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ns4:GetRecords resultType=\"results\" outputFormat=\"application/xml\"\r\n"
                + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\"\r\n"
                + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.w3.org/1999/xlink\"\r\n"
                + "    xmlns=\"http://www.opengis.net/ows\" xmlns:ns4=\"http://www.opengis.net/cat/csw/2.0.2\"\r\n"
                + "    xmlns:ns3=\"http://www.opengis.net/ogc\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\"\r\n"
                + "    xmlns:ns5=\"http://www.opengis.net/gml\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\"\r\n"
                + "    xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">\r\n"
                + "    <ns4:Query typeNames=\"Record\" xmlns=\"\"\r\n"
                + "        xmlns:ns10=\"http://www.opengis.net/ows\">\r\n"
                + "        <ns4:ElementSetName>full</ns4:ElementSetName>\r\n"
                + "        <ns4:Constraint version=\"1.1.0\">\r\n" + "            <ns3:Filter>\r\n"
                + "                <ns3:Or>\r\n"
                + "                    <ns3:PropertyIsBetween>\r\n"
                + "                        <ns3:PropertyName>effective</ns3:PropertyName>\r\n"
                + "                        <ns3:LowerBoundary>\r\n"
                + "                            <ns3:Literal>START1_DATE_TIME</ns3:Literal>\r\n"
                + "                        </ns3:LowerBoundary>\r\n"
                + "                        <ns3:UpperBoundary>\r\n"
                + "                            <ns3:Literal>END1_DATE_TIME</ns3:Literal>\r\n"
                + "                        </ns3:UpperBoundary>\r\n"
                + "                    </ns3:PropertyIsBetween>\r\n"
                + "                    <ns3:PropertyIsBetween>\r\n"
                + "                        <ns3:PropertyName>effective</ns3:PropertyName>\r\n"
                + "                        <ns3:LowerBoundary>\r\n"
                + "                            <ns3:Literal>START2_DATE_TIME</ns3:Literal>\r\n"
                + "                        </ns3:LowerBoundary>\r\n"
                + "                        <ns3:UpperBoundary>\r\n"
                + "                            <ns3:Literal>END2_DATE_TIME</ns3:Literal>\r\n"
                + "                        </ns3:UpperBoundary>\r\n"
                + "                    </ns3:PropertyIsBetween>\r\n"
                + "                </ns3:Or>\r\n" + "            </ns3:Filter>\r\n"
                + "        </ns4:Constraint>\r\n" + "    </ns4:Query>\r\n"
                + "</ns4:GetRecords>\r\n";

        final int pageSize = 10;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 10;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        DateTime startDate = new DateTime(2012, 5, 1, 0, 0, 0, 0);
        DateTime endDate =  new DateTime(2012, 12, 31, 0, 0, 0, 0);
        DateTime startDate2 = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate2 =  new DateTime(2013, 12, 31, 0, 0, 0, 0);

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        // Load the expected start and end date time into the excepted result
        // XML
        expectedXml = expectedXml.replace("START1_DATE_TIME", fmt.print(startDate));
        expectedXml = expectedXml.replace("END1_DATE_TIME", fmt.print(endDate));
        expectedXml = expectedXml.replace("START2_DATE_TIME", fmt.print(startDate2));
        expectedXml = expectedXml.replace("END2_DATE_TIME", fmt.print(endDate2));



        // Single absolute time range to search across
        FilterFactory filterFactory = new FilterFactoryImpl();
        Filter temporalFilter1 = builder.attribute(Metacard.EFFECTIVE).is().during()
                .dates(startDate.toDate(), endDate.toDate());
        Filter temporalFilter2 = builder.attribute(Metacard.EFFECTIVE).is().during()
                .dates(startDate2.toDate(), endDate2.toDate());
        Filter temporalFilter = filterFactory.or(temporalFilter1, temporalFilter2);
        QueryImpl temporalQuery = new QueryImpl(temporalFilter);
        temporalQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        cswSource.setEffectiveDateMapping(Metacard.EFFECTIVE);
        // Perform test
        cswSource.query(new QueryRequestImpl(temporalQuery));

        // Verify
        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify Mock CSW record count " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        String xml = getGetRecordsTypeAsXml(getRecordsType, CswConstants.VERSION_2_0_2);
        LOGGER.debug(xml);
        assertXMLEqual(expectedXml, xml);
    }

    @Test
    public void testHandleClientException() throws JAXBException, UnsupportedQueryException,
        DatatypeConfigurationException, SAXException, IOException {

        // Setup
        try {
            configureMockRemoteCsw();
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is()
                .like().text("junk"));
        propertyIsLikeQuery.setPageSize(10);

        String exceptionReportXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<ows:ExceptionReport version=\"1.2.0\" xmlns:ns16=\"http://www.opengis.net/ows/1.1\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:cat=\"http://www.opengis.net/cat/csw\" xmlns:gco=\"http://www.isotc211.org/2005/gco\" xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\" xmlns:ins=\"http://www.inspire.org\" xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "    <ows:Exception exceptionCode=\"INVALID_PARAMETER_VALUE\" locator=\"QueryConstraint\">\r\n"
                + "        <ows:ExceptionText>Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported</ows:ExceptionText>\r\n"
                + "    </ows:Exception>\r\n" + "</ows:ExceptionReport>";
        ByteArrayInputStream bis = new ByteArrayInputStream(exceptionReportXml.getBytes());
        Response.ResponseBuilder responseBuilder = Response.ok(bis);
        responseBuilder.type("text/xml");
        Response jaxrsResponse = responseBuilder.build();

        WebApplicationException webApplicationException = new WebApplicationException(jaxrsResponse);
        ClientException clientException = mock(ClientException.class);
        when(clientException.getCause()).thenReturn(webApplicationException);
        try {
            when(mockCsw.getRecords(any(GetRecordsType.class))).thenThrow(clientException);
        } catch (CswException e) {
            fail("Could not verify Mock CSW record count " + e.getMessage());
        }

        CswSource cswSource = getCswSource(mockCsw, mockContext, new ArrayList<String>());

        // Perform test
        try {
            cswSource.query(new QueryRequestImpl(propertyIsLikeQuery));
            fail();
        } catch (UnsupportedQueryException e) {
            assertThat(e.getMessage(), containsString("exceptionCode = INVALID_PARAMETER_VALUE"));
            assertThat(e.getMessage(), containsString("Error received from CSW Server"));
            assertThat(
                    e.getMessage(),
                    containsString("Only dc:title,dct:modified,dc:subject,dct:dateSubmitted,dct:alternative,dc:format,dct:created,dc:type,dct:abstract,dc:identifier,dc:creator  are currently supported"));
        }
    }

    @Test(expected = UnsupportedQueryException.class)
    public void testCswSourceNoFilterCapabilities() throws CswException, UnsupportedQueryException {
        // Setup
        CapabilitiesType mockCapabilitiesType = mock(CapabilitiesType.class);
        when(mockCsw.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(
                mockCapabilitiesType);

        CswSource cswSource = getCswSource(mockCsw, mockContext, new ArrayList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);
        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is()
                .like().text("junk"));
        propertyIsLikeQuery.setPageSize(10);

        cswSource.query(new QueryRequestImpl(propertyIsLikeQuery));

    }

    @Test
    public void testTimeoutConfiguration() {

        final String TITLE = "title";

        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockRemoteCsw(numRecordsReturned, numRecordsMatched,
                    CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        CswSource cswSource = getCswSource(mockCsw, mockContext, new LinkedList<String>());
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        cswSource.setConnectionTimeout(10000);
        cswSource.setReceiveTimeout(10000);

        // Perform test
        cswSource.updateTimeouts();

        verify(mockCsw, atLeastOnce()).setTimeouts(any(Integer.class), any(Integer.class));
    }

    @Test
    public void testRefresh(){
        CswSource cswSource = getCswSource(null, null, Collections.<String> emptyList());

        cswSource.refresh(null);

        Map<String, Object> configuration = new HashMap<String, Object>();
        cswSource.refresh(configuration);

    }

    private CswSourceConfiguration getStandardCswSourceConfiguration(String contentTypeMapping) {
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        if (contentTypeMapping == null) {
            cswSourceConfiguration.setContentTypeMapping(CswRecordMetacardType.CSW_TYPE);
        } else {
            cswSourceConfiguration.setContentTypeMapping(contentTypeMapping);
        }
        cswSourceConfiguration.setId(ID);
        cswSourceConfiguration.setCswUrl(URL);
        cswSourceConfiguration.setModifiedDateMapping(Metacard.MODIFIED);
        return cswSourceConfiguration;
    }

    private CswSource getCswSource(RemoteCsw remoteCsw, BundleContext context, List<String> contentTypes)  {
        return getCswSource(remoteCsw, context, contentTypes, null);
    }

    private CswSource getCswSource(RemoteCsw remoteCsw, BundleContext context,
            List<String> contentTypes, String contentMapping) {

        CswSourceConfiguration cswSourceConfiguration = getStandardCswSourceConfiguration(
                contentMapping);
        cswSourceConfiguration.setContentTypeMapping(contentMapping);
        CswSource cswSource = new CswSource(remoteCsw, mockContext, cswSourceConfiguration,
                mockProvider);
        cswSource.setFilterAdapter(new GeotoolsFilterAdapterImpl());
        cswSource.setFilterBuilder(builder);
        cswSource.setContext(context);
        cswSource.setContentTypeNames(contentTypes);
        cswSource.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        cswSource.setAvailabilityTask(mockAvailabilityTask);
        cswSource.configureCswSource();
        return cswSource;
    }

}
