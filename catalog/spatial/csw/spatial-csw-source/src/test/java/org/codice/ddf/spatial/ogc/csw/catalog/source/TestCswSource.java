/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.shiro.subject.Subject;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.custommonkey.xmlunit.Diff;
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

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.service.SecurityServiceException;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.filter.v_1_1_0.SortOrderType;

public class TestCswSource extends TestCswSourceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCswSource.class);

    private static final String CSW_RECORD_QNAME =
            "{" + CswConstants.CSW_OUTPUT_SCHEMA + "}" + CswConstants.CSW_RECORD_LOCAL_NAME;

    private CswTransformProvider mockProvider = mock(CswTransformProvider.class);

    @Test
    public void testParseCapabilities() throws CswException, SecurityServiceException {
        CswSource source = getCswSource(createMockCsw(), mockContext);

        assertTrue(source.isAvailable());
        assertEquals(10, source.getContentTypes().size());
        Set<ContentType> expected = generateContentType(
                Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        assertThat(source.getContentTypes(), is(expected));
    }

    @Test
    public void testInitialContentList() throws CswException, SecurityServiceException {

        CswSource source = getCswSource(createMockCsw(), mockContext);

        assertTrue(source.isAvailable());
        assertEquals(10, source.getContentTypes().size());
        Set<ContentType> expected = generateContentType(
                Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        assertThat(source.getContentTypes(), is(expected));
    }

    @Test
    public void testAddingContentTypesOnQueries()
            throws CswException, UnsupportedQueryException, SecurityServiceException {
        Csw mockCsw = createMockCsw();

        List<String> expectedNames = new LinkedList<>(
                Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));

        ServiceRegistration<?> mockRegisteredMetacardType = (ServiceRegistration<?>) mock(
                ServiceRegistration.class);
        LOGGER.info("mockRegisteredMetacardType: {}", mockRegisteredMetacardType);
        doReturn(mockRegisteredMetacardType).when(mockContext)
                .registerService(eq(MetacardType.class.getName()), any(CswRecordMetacardType.class),
                        Matchers.<Dictionary<String, ?>>any());
        ServiceReference<?> mockServiceReference = (ServiceReference<?>) mock(
                ServiceReference.class);
        doReturn(mockServiceReference).when(mockRegisteredMetacardType).getReference();
        when(mockServiceReference.getProperty(eq(Metacard.CONTENT_TYPE))).thenReturn(expectedNames);

        CswSource source = getCswSource(mockCsw, mockContext);

        assertEquals(10, source.getContentTypes().size());

        Set<ContentType> expected = generateContentType(expectedNames);
        assertThat(source.getContentTypes(), is(expected));

        CswRecordCollection collection = generateCswCollection("/getRecordsResponse.xml");

        when(mockCsw.getRecords(any(GetRecordsType.class))).thenReturn(collection);

        QueryImpl propertyIsLikeQuery = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text("*"));

        expectedNames.add("dataset");
        expectedNames.add("dataset 2");
        expectedNames.add("dataset 3");
        expected = generateContentType(expectedNames);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));

        assertEquals(13, source.getContentTypes().size());
        assertThat(source.getContentTypes(), is(expected));
    }

    @Test
    public void testPropertyIsLikeQuery()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        final String searchPhrase = "*th*e";
        final int pageSize = 10;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 10;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl propertyIsLikeQuery = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        propertyIsLikeQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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

        String xml = getGetRecordsTypeAsXml(getRecordsType);
        Diff xmlDiff = new Diff(getRecordsControlXml202, xml);

        if (!xmlDiff.similar()) {
            LOGGER.error("Unexpected XML request sent");
            LOGGER.error("Expected: {}", getRecordsControlXml202);
            LOGGER.error("Actual: {}", xml);
        }

        assertXMLEqual(getRecordsControlXml202, xml);
    }

    @Test
    public void testQueryWithSorting()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        final String TITLE = "title";

        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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
        assertThat(
                cswQuery.getSortBy().getSortProperty().get(0).getPropertyName().getContent().get(0)
                        .toString(), equalTo(TITLE));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(),
                is(SortOrderType.DESC));
    }

    @Test
    public void testQueryWithSortByDistance()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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
    public void testQueryWithSortByRelevance()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {
        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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
        assertThat(
                cswQuery.getSortBy().getSortProperty().get(0).getPropertyName().getContent().get(0)
                        .toString(), equalTo(Metacard.TITLE));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(),
                is(SortOrderType.DESC));
    }

    @Test
    public void testQueryWithSortByTemporal()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {
        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        query.setPageSize(pageSize);
        SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
        query.setSortBy(sortBy);

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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
        assertThat(
                cswQuery.getSortBy().getSortProperty().get(0).getPropertyName().getContent().get(0)
                        .toString(), equalTo(Metacard.MODIFIED));
        assertThat(cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(),
                is(SortOrderType.DESC));
    }

    /**
     * Test to verify content type mapping is configurable.
     * The CSW Source should be able to map a csw:Record field to Content Type.
     */
    @Test
    public void testPropertyIsEqualToQueryContentTypeIsMappedToFormat()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        int pageSize = 10;
        int numRecordsReturned = 1;
        long numRecordsMatched = 1;
        String format = "myContentType";

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl propertyIsEqualToQuery = new QueryImpl(
                builder.attribute(Metacard.CONTENT_TYPE).is().text(format));
        propertyIsEqualToQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, CswRecordMetacardType.CSW_FORMAT);
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

        String xml = getGetRecordsTypeAsXml(getRecordsType);
        Diff xmlDiff = new Diff(getRecordsControlXml202ContentTypeMappedToFormat, xml);

        if (!xmlDiff.similar()) {
            LOGGER.error("Unexpected XML request sent");
            LOGGER.error("Expected: {}", getRecordsControlXml202ContentTypeMappedToFormat);
            LOGGER.error("Actual: {}", xml);
        }

        assertXMLEqual(getRecordsControlXml202ContentTypeMappedToFormat, xml);
    }

    /**
     * Test to verify content type version mapping is correct. The CSW Source should be able to map
     * a csw:Record field to Content Type.
     */
    @Test
    public void testPropertyIsLikeContentTypeVersion()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        int pageSize = 10;
        int numRecordsReturned = 1;
        long numRecordsMatched = 1;
        String format = "myContentType";
        String version = "2.0";

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        Filter ctfilter = builder.attribute(Metacard.CONTENT_TYPE).is().text(format);
        Filter versionFilter = builder.attribute(Metacard.CONTENT_TYPE_VERSION).is().like()
                .text(version);
        Filter filter = builder.allOf(ctfilter, versionFilter);

        QueryImpl propertyIsEqualToQuery = new QueryImpl(filter);
        propertyIsEqualToQuery.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, CswRecordMetacardType.CSW_FORMAT);
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

        String xml = getGetRecordsTypeAsXml(getRecordsType);
        Diff xmlDiff = new Diff(getRecordsControlXml202ContentTypeMappedToFormat, xml);

        if (!xmlDiff.similar()) {
            LOGGER.error("Unexpected XML request sent");
            LOGGER.error("Expected: {}", getRecordsControlXml202ContentTypeMappedToFormat);
            LOGGER.error("Actual: {}", xml);
        }

        assertXMLEqual(getRecordsControlXml202ContentTypeMappedToFormat, xml);
    }

    @Test
    public void testAbsoluteTemporalSearchPropertyIsBetweenQuery()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\" "
                + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" "
                + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" "
                + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" "
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" "
                + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n "
                + "    <Query typeNames=\"csw:Record\">\r\n"
                + "        <ElementSetName>full</ElementSetName>\r\n"
                + "        <Constraint version=\"1.1.0\">\r\n" // Line break
                + "            <ogc:Filter>\r\n" // Line break
                + "                <ogc:PropertyIsBetween>\r\n"
                + "                    <ogc:PropertyName>effective</ogc:PropertyName>\r\n"
                + "                    <ogc:LowerBoundary>\r\n"
                + "                        <ogc:Literal>START_DATE_TIME</ogc:Literal>\r\n"
                + "                    </ogc:LowerBoundary>\r\n"
                + "                    <ogc:UpperBoundary>\r\n"
                + "                        <ogc:Literal>END_DATE_TIME</ogc:Literal>\r\n"
                + "                    </ogc:UpperBoundary>\r\n"
                + "                </ogc:PropertyIsBetween>\r\n" // Line break
                + "            </ogc:Filter>\r\n" // Line break
                + "        </Constraint>\r\n" // Line break
                + "    </Query>\r\n" // Line break
                + "</GetRecords>";

        final int pageSize = 10;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 10;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        // Create start and end date times that are before current time
        DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate = new DateTime(2013, 12, 31, 0, 0, 0, 0);
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

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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

        String xml = getGetRecordsTypeAsXml(getRecordsType);
        Diff xmlDiff = new Diff(expectedXml, xml);

        if (!xmlDiff.similar()) {
            LOGGER.error("Unexpected XML request sent");
            LOGGER.error("Expected: {}", expectedXml);
            LOGGER.error("Actual: {}", xml);
        }

        assertXMLEqual(expectedXml, xml);
    }

    @Test
    public void testAbsoluteTemporalSearchTwoRanges()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
                + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\"\r\n"
                + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\"\r\n"
                + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\""
                + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\""
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\""
                + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\r\n"
                + "    <Query typeNames=\"csw:Record\">\r\n"
                + "        <ElementSetName>full</ElementSetName>\r\n"
                + "        <Constraint version=\"1.1.0\">\r\n" // Line break
                + "            <ogc:Filter>\r\n" // Line break
                + "                <ogc:Or>\r\n" // Line break
                + "                    <ogc:PropertyIsBetween>\r\n"
                + "                        <ogc:PropertyName>effective</ogc:PropertyName>\r\n"
                + "                        <ogc:LowerBoundary>\r\n"
                + "                            <ogc:Literal>START1_DATE_TIME</ogc:Literal>\r\n"
                + "                        </ogc:LowerBoundary>\r\n"
                + "                        <ogc:UpperBoundary>\r\n"
                + "                            <ogc:Literal>END1_DATE_TIME</ogc:Literal>\r\n"
                + "                        </ogc:UpperBoundary>\r\n"
                + "                    </ogc:PropertyIsBetween>\r\n"
                + "                    <ogc:PropertyIsBetween>\r\n"
                + "                        <ogc:PropertyName>effective</ogc:PropertyName>\r\n"
                + "                        <ogc:LowerBoundary>\r\n"
                + "                            <ogc:Literal>START2_DATE_TIME</ogc:Literal>\r\n"
                + "                        </ogc:LowerBoundary>\r\n"
                + "                        <ogc:UpperBoundary>\r\n"
                + "                            <ogc:Literal>END2_DATE_TIME</ogc:Literal>\r\n"
                + "                        </ogc:UpperBoundary>\r\n"
                + "                    </ogc:PropertyIsBetween>\r\n"
                + "                </ogc:Or>\r\n" // Line break
                + "            </ogc:Filter>\r\n" // Line break
                + "        </Constraint>\r\n" // Line break
                + "    </Query>\r\n" // Line break
                + "</GetRecords>\r\n";

        final int pageSize = 10;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 10;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        DateTime startDate = new DateTime(2012, 5, 1, 0, 0, 0, 0);
        DateTime endDate = new DateTime(2012, 12, 31, 0, 0, 0, 0);
        DateTime startDate2 = new DateTime(2013, 5, 1, 0, 0, 0, 0);
        DateTime endDate2 = new DateTime(2013, 12, 31, 0, 0, 0, 0);

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

        CswSource cswSource = getCswSource(mockCsw, mockContext);
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

        String xml = getGetRecordsTypeAsXml(getRecordsType);
        Diff xmlDiff = new Diff(expectedXml, xml);

        if (!xmlDiff.similar()) {
            LOGGER.error("Unexpected XML request sent");
            LOGGER.error("Expected: {}", expectedXml);
            LOGGER.error("Actual: {}", xml);
        }

        assertXMLEqual(expectedXml, xml);
    }

    @Test(expected = UnsupportedQueryException.class)
    public void testCswSourceNoFilterCapabilities()
            throws CswException, UnsupportedQueryException, SecurityServiceException {
        // Setup
        CapabilitiesType mockCapabilitiesType = mock(CapabilitiesType.class);
        when(mockCsw.getCapabilities(any(GetCapabilitiesRequest.class)))
                .thenReturn(mockCapabilitiesType);

        CswSource cswSource = getCswSource(mockCsw, mockContext);
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);
        QueryImpl propertyIsLikeQuery = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text("junk"));
        propertyIsLikeQuery.setPageSize(10);

        cswSource.query(new QueryRequestImpl(propertyIsLikeQuery));

    }

    @Test
    public void testTimeoutConfiguration() throws SecurityServiceException {

        // Setup
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        CswSource cswSource = getCswSource(mockCsw, mockContext);
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put("connectionTimeout", 10000);
        configuration.put("receiveTimeout", 10000);
        cswSource.refresh(configuration);

        assertEquals(cswSource.getConnectionTimeout().intValue(), 10000);
        assertEquals(cswSource.getReceiveTimeout().intValue(), 10000);
    }

    @Test
    public void testRefresh() throws SecurityServiceException {
        CswSource cswSource = getCswSource(null, null);

        cswSource.refresh(null);

        Map<String, Object> configuration = new HashMap<>();
        cswSource.refresh(configuration);

    }

    @Test
    public void testQueryWithAlternateQueryType()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        final QName expectedQname = new QName("http://example.com", "example", "abc");
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        query.setPageSize(pageSize);

        CswSource cswSource = getCswSource(mockCsw, mockContext, null, expectedQname.toString(),
                expectedQname.getPrefix());

        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        cswSource.query(new QueryRequestImpl(query));

        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();

        assertThat(cswQuery.getTypeNames().size(), is(1));
        assertThat(cswQuery.getTypeNames().get(0), is(expectedQname));
    }

    @Test
    public void testQueryWithDefaultQueryType()
            throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException,
            SAXException, IOException, SecurityServiceException {

        // Setup
        final String searchPhrase = "*";
        final int pageSize = 1;
        final int numRecordsReturned = 1;
        final long numRecordsMatched = 1;

        setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

        try {
            configureMockCsw(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
        } catch (CswException e) {
            fail("Could not configure Mock Remote CSW: " + e.getMessage());
        }

        QueryImpl query = new QueryImpl(
                builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
        query.setPageSize(pageSize);

        // Verify passing a null config for qname/prefix falls back to CSW Record
        CswSource cswSource = getCswSource(mockCsw, mockContext, null, null, null);
        cswSource.setCswUrl(URL);
        cswSource.setId(ID);

        // Perform test
        cswSource.query(new QueryRequestImpl(query));

        ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
        try {
            verify(mockCsw, atLeastOnce()).getRecords(captor.capture());
        } catch (CswException e) {
            fail("Could not verify mock CSW record count: " + e.getMessage());
        }
        GetRecordsType getRecordsType = captor.getValue();

        QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();

        assertThat(cswQuery.getTypeNames().size(), is(1));
        assertThat(cswQuery.getTypeNames().get(0).toString(), is(CSW_RECORD_QNAME));
    }

    @Test
    public void testCreateResults() throws SecurityServiceException {
        CswSource cswSource = getCswSource(mockCsw, mockContext, null, null, null);
        CswRecordCollection recordCollection = new CswRecordCollection();
        final int total = 2;
        List<Metacard> metacards = new ArrayList<>(total);
        for (int i = 0; i <= total; i++) {
            String id = "ID_" + String.valueOf(i);
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);
            metacard.setContentTypeName("myContentType");
            metacard.setResourceURI(URI.create("http://example.com/resource"));
            if (i == 1) {
                metacard.setAttribute(Metacard.RESOURCE_DOWNLOAD_URL,
                        "http://example.com/SECOND/RESOURCE");
            }
            metacards.add(metacard);
        }
        recordCollection.getCswRecords().addAll(metacards);
        List<Result> results = cswSource.createResults(recordCollection);

        assertThat(results, notNullValue());
        assertThat(results.size(), is(recordCollection.getCswRecords().size()));
        assertThat(results.get(0).getMetacard().getResourceURI(),
                is(recordCollection.getCswRecords().get(0).getResourceURI()));
        assertThat(results.get(1).getMetacard().getResourceURI(), is(URI.create(
                recordCollection.getCswRecords().get(1).getAttribute(Metacard.RESOURCE_DOWNLOAD_URL)
                        .getValue().toString())));

    }

    private CswSourceConfiguration getStandardCswSourceConfiguration(String contentTypeMapping,
            String queryTypeQName, String queryTypePrefix) {
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        if (contentTypeMapping == null) {
            cswSourceConfiguration.setContentTypeMapping(CswRecordMetacardType.CSW_TYPE);
        } else {
            cswSourceConfiguration.setContentTypeMapping(contentTypeMapping);
        }
        cswSourceConfiguration.setQueryTypePrefix(queryTypePrefix);
        cswSourceConfiguration.setQueryTypeQName(queryTypeQName);
        cswSourceConfiguration.setId(ID);
        cswSourceConfiguration.setCswUrl(URL);
        cswSourceConfiguration.setModifiedDateMapping(Metacard.MODIFIED);
        cswSourceConfiguration.setIdentifierMapping(CswRecordMetacardType.CSW_IDENTIFIER);
        cswSourceConfiguration.setUsername("user");
        cswSourceConfiguration.setPassword("pass");
        return cswSourceConfiguration;
    }

    private CswSource getCswSource(Csw csw, BundleContext context) throws SecurityServiceException {
        return getCswSource(csw, context, null);
    }

    private CswSource getCswSource(Csw csw, BundleContext context, String contentMapping,
            String queryTypeQName, String queryTypePrefix) throws SecurityServiceException {

        CswSourceConfiguration cswSourceConfiguration = getStandardCswSourceConfiguration(
                contentMapping, queryTypeQName, queryTypePrefix);
        cswSourceConfiguration.setContentTypeMapping(contentMapping);

        SecureCxfClientFactory mockFactory = mock(SecureCxfClientFactory.class);
        try {
            doReturn(csw).when(mockFactory)
                    .getClientForBasicAuth(any(String.class), any(String.class));
            doReturn(csw).when(mockFactory).getClientForSubject(any(Subject.class));
            doReturn(csw).when(mockFactory).getUnsecuredClient();
        } catch (SecurityServiceException sse) {
            fail("Mock CSW from mockFactory failed");
        }

        CswSource cswSource = new CswSource(mockContext, cswSourceConfiguration, mockProvider,
                mockFactory);
        cswSource.setFilterAdapter(new GeotoolsFilterAdapterImpl());
        cswSource.setFilterBuilder(builder);
        cswSource.setContext(context);
        cswSource.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        cswSource.setAvailabilityTask(mockAvailabilityTask);
        cswSource.configureCswSource();

        return cswSource;
    }

    private CswSource getCswSource(Csw csw, BundleContext context, String contentMapping)
            throws SecurityServiceException {

        return getCswSource(csw, context, contentMapping, CSW_RECORD_QNAME,
                CswConstants.CSW_NAMESPACE_PREFIX);
    }
}
