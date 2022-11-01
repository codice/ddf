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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.custommonkey.xmlunit.Diff;
import org.geotools.filter.FilterFactoryImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswSourceTest extends TestCswSourceBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswSourceTest.class);

  private Converter mockProvider = mock(Converter.class);

  @Test
  public void testParseCapabilities() throws Exception {
    AbstractCswSource source = getCswSource(createMockCswClient(), mockContext);

    assertThat(source.isAvailable(), is(true));
    assertThat(source.getContentTypes(), hasSize(10));
    Set<ContentType> expected =
        generateContentType(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
    assertThat(source.getContentTypes(), is(expected));
  }

  @Test
  public void testInitialContentList() throws Exception {
    AbstractCswSource source = getCswSource(createMockCswClient(), mockContext);

    assertThat(source.isAvailable(), is(true));
    assertThat(source.getContentTypes(), hasSize(10));
    Set<ContentType> expected =
        generateContentType(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
    assertThat(source.getContentTypes(), is(expected));
  }

  @Test
  public void testAddingContentTypesOnQueries() throws Exception {
    CswClient cswClient = createMockCswClient();

    List<String> expectedNames =
        new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));

    ServiceRegistration<?> mockRegisteredMetacardType =
        (ServiceRegistration<?>) mock(ServiceRegistration.class);
    LOGGER.info("mockRegisteredMetacardType: {}", mockRegisteredMetacardType);
    doReturn(mockRegisteredMetacardType)
        .when(mockContext)
        .registerService(eq(MetacardType.class.getName()), any(MetacardType.class), any());
    ServiceReference<?> mockServiceReference = (ServiceReference<?>) mock(ServiceReference.class);
    doReturn(mockServiceReference).when(mockRegisteredMetacardType).getReference();
    when(mockServiceReference.getProperty(eq(Metacard.CONTENT_TYPE))).thenReturn(expectedNames);

    AbstractCswSource source = getCswSource(cswClient, mockContext);

    assertThat(source.getContentTypes(), hasSize(10));

    Set<ContentType> expected = generateContentType(expectedNames);
    assertThat(source.getContentTypes(), is(expected));

    CswRecordCollection collection = generateCswCollection("/getRecordsResponse.xml");

    when(cswClient.getRecords(any(GetRecordsType.class))).thenReturn(collection);

    QueryRequestImpl propertyIsLikeQuery =
        new QueryRequestImpl(
            new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("*")));

    expectedNames.add("dataset");
    expectedNames.add("dataset 2");
    expectedNames.add("dataset 3");
    expected = generateContentType(expectedNames);

    source.query(propertyIsLikeQuery);

    assertThat(source.getContentTypes(), hasSize(13));
    assertThat(source.getContentTypes(), is(expected));
  }

  @Test
  public void testPropertyIsLikeQuery() throws Exception {

    // Setup
    final String searchPhrase = "*th*e";
    final int pageSize = 10;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 10;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    propertyIsLikeQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(propertyIsLikeQuery));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
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
  public void testQueryWitNaturalSorting() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    query.setSortBy(SortBy.NATURAL_ORDER);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(query));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
    assertThat(cswQuery.getSortBy(), nullValue());
  }

  @Test
  public void testQueryWitNullSorting() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    query.setSortBy(null);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(query));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
    assertThat(cswQuery.getSortBy(), nullValue());
  }

  @Test
  public void testQueryWithSorting() throws Exception {

    final String TITLE = "title";

    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(query));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
    assertThat(cswQuery.getSortBy().getSortProperty().size(), is(1));
    assertThat(
        cswQuery
            .getSortBy()
            .getSortProperty()
            .get(0)
            .getPropertyName()
            .getContent()
            .get(0)
            .toString(),
        equalTo(TITLE));
    assertThat(
        cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(), is(SortOrderType.DESC));
  }

  @Test
  public void testQueryWithSortByDistance() throws Exception {

    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(query));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
    assertThat(cswQuery.getSortBy(), nullValue());
  }

  @Test
  public void testQueryWithSortByRelevance() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(query));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
    assertThat(cswQuery.getSortBy().getSortProperty().size(), is(1));
    assertThat(
        cswQuery
            .getSortBy()
            .getSortProperty()
            .get(0)
            .getPropertyName()
            .getContent()
            .get(0)
            .toString(),
        equalTo(Core.TITLE));
    assertThat(
        cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(), is(SortOrderType.DESC));
  }

  @Test
  public void testQueryWithSortByTemporal() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    SourceResponse response = cswSource.query(new QueryRequestImpl(query));

    // Verify
    Assert.assertNotNull(response);
    assertThat(response.getResults().size(), is(numRecordsReturned));
    assertThat(response.getHits(), is(numRecordsMatched));
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();
    assertThat(cswQuery.getSortBy().getSortProperty().size(), is(1));
    assertThat(
        cswQuery
            .getSortBy()
            .getSortProperty()
            .get(0)
            .getPropertyName()
            .getContent()
            .get(0)
            .toString(),
        equalTo(Core.MODIFIED));
    assertThat(
        cswQuery.getSortBy().getSortProperty().get(0).getSortOrder(), is(SortOrderType.DESC));
  }

  /**
   * Test to verify content type mapping is configurable. The CSW Source should be able to map a
   * csw:Record field to Content Type.
   */
  @Test
  public void testPropertyIsEqualToQueryContentTypeIsMappedToFormat() throws Exception {

    // Setup
    int pageSize = 10;
    int numRecordsReturned = 1;
    long numRecordsMatched = 1;
    String format = "myContentType";

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    try {
      configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
    } catch (CswException e) {
      fail("Could not configure Mock Remote CSW: " + e.getMessage());
    }

    QueryImpl propertyIsEqualToQuery =
        new QueryImpl(builder.attribute(Metacard.CONTENT_TYPE).is().text(format));
    propertyIsEqualToQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext, CswConstants.CSW_FORMAT);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    cswSource.query(new QueryRequestImpl(propertyIsEqualToQuery));

    // Verify
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    // getRecords() is called two times. Once for initial CSW source
    // configuration and
    // a second time for the actual content type query.
    verify(mockCswClient, times(2)).getRecords(captor.capture());
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
   * Test to verify content type version mapping is correct. The CSW Source should be able to map a
   * csw:Record field to Content Type.
   */
  @Test
  public void testPropertyIsLikeContentTypeVersion() throws Exception {

    // Setup
    int pageSize = 10;
    int numRecordsReturned = 1;
    long numRecordsMatched = 1;
    String format = "myContentType";
    String version = "2.0";

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    Filter ctfilter = builder.attribute(Metacard.CONTENT_TYPE).is().text(format);
    Filter versionFilter =
        builder.attribute(Metacard.CONTENT_TYPE_VERSION).is().like().text(version);
    Filter filter = builder.allOf(ctfilter, versionFilter);

    QueryImpl propertyIsEqualToQuery = new QueryImpl(filter);
    propertyIsEqualToQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext, CswConstants.CSW_FORMAT);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    cswSource.query(new QueryRequestImpl(propertyIsEqualToQuery));

    // Verify
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    // getRecords() is called two times. Once for initial CSW source
    // configuration and
    // a second time for the actual content type query.
    verify(mockCswClient, times(2)).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    String xml = getGetRecordsTypeAsXml(getRecordsType);
    Diff xmlDiff = new Diff(getRecordsControlXml202ConteTypeAndVersion, xml);

    if (!xmlDiff.similar()) {
      LOGGER.error("Unexpected XML request sent");
      LOGGER.error("Expected:\n {}", getRecordsControlXml202ConteTypeAndVersion);
      LOGGER.error("Actual:\n {}", xml);
    }

    assertXMLEqual(getRecordsControlXml202ConteTypeAndVersion, xml);
  }

  @Test
  public void testAbsoluteTemporalSearchPropertyIsBetweenQuery() throws Exception {

    // Setup
    String expectedXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
            + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\" "
            + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" "
            + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" "
            + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\" "
            + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" "
            + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n "
            + "    <Query typeNames=\"csw:Record\">\r\n"
            + "        <ElementSetName>full</ElementSetName>\r\n"
            + "        <Constraint version=\"1.1.0\">\r\n"
            + "            <ogc:Filter>\r\n"
            + "                <ogc:PropertyIsBetween>\r\n"
            + "                    <ogc:PropertyName>effective</ogc:PropertyName>\r\n"
            + "                    <ogc:LowerBoundary>\r\n"
            + "                        <ogc:Literal>START_DATE_TIME</ogc:Literal>\r\n"
            + "                    </ogc:LowerBoundary>\r\n"
            + "                    <ogc:UpperBoundary>\r\n"
            + "                        <ogc:Literal>END_DATE_TIME</ogc:Literal>\r\n"
            + "                    </ogc:UpperBoundary>\r\n"
            + "                </ogc:PropertyIsBetween>\r\n"
            + "            </ogc:Filter>\r\n"
            + "        </Constraint>\r\n"
            + "    </Query>\r\n"
            + "</GetRecords>";

    final int pageSize = 10;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 10;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    // Create start and end date times that are before current time
    DateTime startDate = new DateTime(2013, 5, 1, 0, 0, 0, 0);
    DateTime endDate = new DateTime(2013, 12, 31, 0, 0, 0, 0);
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    // Load the expected start and end date time into the excepted result
    // XML
    expectedXml = expectedXml.replace("START_DATE_TIME", fmt.print(startDate));
    expectedXml = expectedXml.replace("END_DATE_TIME", fmt.print(endDate));

    // Single absolute time range to search across
    Filter temporalFilter =
        builder
            .attribute(Metacard.EFFECTIVE)
            .is()
            .during()
            .dates(startDate.toDate(), endDate.toDate());
    QueryImpl temporalQuery = new QueryImpl(temporalFilter);
    temporalQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    cswSource.query(new QueryRequestImpl(temporalQuery));

    // Verify
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
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
  public void testAbsoluteTemporalSearchTwoRanges() throws Exception {

    // Setup
    String expectedXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n"
            + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\"\r\n"
            + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\"\r\n"
            + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\""
            + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\""
            + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\""
            + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\r\n"
            + "    <Query typeNames=\"csw:Record\">\r\n"
            + "        <ElementSetName>full</ElementSetName>\r\n"
            + "        <Constraint version=\"1.1.0\">\r\n"
            + "            <ogc:Filter>\r\n"
            + "                <ogc:Or>\r\n"
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
            + "                </ogc:Or>\r\n"
            + "            </ogc:Filter>\r\n"
            + "        </Constraint>\r\n"
            + "    </Query>\r\n"
            + "</GetRecords>\r\n";

    final int pageSize = 10;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 10;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

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
    Filter temporalFilter1 =
        builder
            .attribute(Metacard.EFFECTIVE)
            .is()
            .during()
            .dates(startDate.toDate(), endDate.toDate());
    Filter temporalFilter2 =
        builder
            .attribute(Metacard.EFFECTIVE)
            .is()
            .during()
            .dates(startDate2.toDate(), endDate2.toDate());
    Filter temporalFilter = filterFactory.or(temporalFilter1, temporalFilter2);
    QueryImpl temporalQuery = new QueryImpl(temporalFilter);
    temporalQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    cswSource.query(new QueryRequestImpl(temporalQuery));

    // Verify
    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
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
  public void testCswSourceNoFilterCapabilities() throws Exception {
    // Setup
    CapabilitiesType mockCapabilitiesType = mock(CapabilitiesType.class);
    when(mockCswClient.getCapabilities(any())).thenReturn(mockCapabilitiesType);

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("junk"));
    propertyIsLikeQuery.setPageSize(10);

    cswSource.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testTimeoutConfiguration() throws Exception {

    // Setup
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    try {
      configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);
    } catch (CswException e) {
      fail("Could not configure Mock Remote CSW: " + e.getMessage());
    }

    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    HashMap<String, Object> configuration = new HashMap<>();
    configuration.put("connectionTimeout", 10000);
    configuration.put("receiveTimeout", 10000);
    configuration.put("pollInterval", 5);
    cswSource.refresh(configuration);

    assertThat(cswSource.getConnectionTimeout().intValue(), is(10000));
    assertThat(cswSource.getReceiveTimeout().intValue(), is(10000));
  }

  @Test
  public void testRefreshWithNullConfiguration() throws Exception {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null);
    CswSourceConfiguration defaultCswSourceConfiguration =
        getStandardCswSourceConfiguration(null, null, null);

    // Assert that the default configuration is set
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);

    cswSource.refresh(null);

    // Assert that the configuration does not change with a null map
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);
  }

  @Test
  public void testRefreshWithEmptyConfiguration() throws Exception {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null);
    CswSourceConfiguration defaultCswSourceConfiguration =
        getStandardCswSourceConfiguration(null, null, null);

    Map<String, Object> configuration = new HashMap<>();

    // Assert that the default configuration is set
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);

    cswSource.refresh(configuration);

    // Assert that the configuration does not change with an empty map
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);
  }

  @Test
  public void testRefresh() throws Exception {
    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext, "contentType");
    CswSourceConfiguration defaultCswSourceConfiguration =
        getStandardCswSourceConfiguration("contentType", "qname", "queryprefix");

    // Assert Defaults
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);

    // Set Configuration Map
    Map<String, Object> configuration = getConfigurationMap(cswSource);

    // Call Refresh
    cswSource.refresh(configuration);

    // Get Configuration
    CswSourceConfiguration cswSourceConfiguration = cswSource.cswSourceConfiguration;

    // Assert Refresh Changes
    assertConfigurationAfterRefresh(cswSourceConfiguration);
  }

  @Test
  public void testSetPassword() {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null);

    cswSource.setPassword("secret");

    assertThat(cswSource.cswSourceConfiguration.getPassword(), is(equalTo("secret")));
  }

  @Test
  public void testSetPasswordWithEmptyPassword() {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null);

    cswSource.setPassword("");

    assertThat(cswSource.cswSourceConfiguration.getPassword(), is(equalTo("")));
  }

  @Test
  public void testQueryWithAlternateQueryType() throws Exception {

    // Setup
    final QName expectedQname = new QName("http://example.com", "example", "abc");
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);

    AbstractCswSource cswSource =
        getCswSource(
            mockCswClient,
            mockContext,
            null,
            expectedQname.getPrefix() + ":" + expectedQname.getLocalPart(),
            expectedQname.getNamespaceURI());

    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    cswSource.query(new QueryRequestImpl(query));

    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();

    assertThat(cswQuery.getTypeNames().size(), is(1));
    assertThat(cswQuery.getTypeNames().get(0), is(expectedQname));
  }

  @Test
  public void testQueryWithDefaultQueryType() throws Exception {

    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;
    final int numRecordsReturned = 1;
    final long numRecordsMatched = 1;

    setupMockContextForMetacardTypeRegistrationAndUnregistration(getDefaultContentTypes());

    configureMockCswClient(numRecordsReturned, numRecordsMatched, CswConstants.VERSION_2_0_2);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);

    // Verify passing a null config for qname/prefix falls back to CSW Record
    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext, null, null, null);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

    // Perform test
    cswSource.query(new QueryRequestImpl(query));

    ArgumentCaptor<GetRecordsType> captor = ArgumentCaptor.forClass(GetRecordsType.class);
    verify(mockCswClient, atLeastOnce()).getRecords(captor.capture());
    GetRecordsType getRecordsType = captor.getValue();

    QueryType cswQuery = (QueryType) getRecordsType.getAbstractQuery().getValue();

    assertThat(cswQuery.getTypeNames().size(), is(1));
    assertThat(
        cswQuery.getTypeNames().get(0),
        is(new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CSW_RECORD_LOCAL_NAME)));
  }

  @Test
  public void testCreateResults() throws Exception {
    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext, null, null, null);
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
        metacard.setAttribute(Core.RESOURCE_DOWNLOAD_URL, "http://example.com/SECOND/RESOURCE");
      }
      metacards.add(metacard);
    }
    recordCollection.getCswRecords().addAll(metacards);
    List<Result> results = cswSource.createResults(recordCollection);

    assertThat(results, notNullValue());
    assertThat(results.size(), is(recordCollection.getCswRecords().size()));
    assertThat(
        results.get(0).getMetacard().getResourceURI(),
        is(recordCollection.getCswRecords().get(0).getResourceURI()));
    assertThat(
        results.get(1).getMetacard().getResourceURI(),
        is(
            URI.create(
                recordCollection
                    .getCswRecords()
                    .get(1)
                    .getAttribute(Core.RESOURCE_DOWNLOAD_URL)
                    .getValue()
                    .toString())));
  }

  @Test
  public void testRetrieveResourceUsingReader() throws Exception {
    configureMockCswClient(0, 0L, CswConstants.VERSION_2_0_2);
    AbstractCswSource cswSource = getCswSource(mockCswClient, mockContext, null, null, null);
    ResourceReader reader = mock(ResourceReader.class);
    when(reader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(mock(ResourceResponse.class));
    cswSource.setResourceReader(reader);

    Map<String, Serializable> props = new HashMap<>();
    props.put(Core.ID, "ID");
    cswSource.retrieveResource(new URI("http://example.com/resource"), props);
    // Verify
    verify(reader, times(1)).retrieveResource(any(URI.class), anyMap());
  }

  @Test
  public void testRetrieveResourceUsingGetRecordById() throws Exception {
    CswClient cswClient = createMockCswClient();
    CswRecordCollection collection = mock(CswRecordCollection.class);
    Resource resource = mock(Resource.class);
    when(collection.getResource()).thenReturn(resource);
    when(cswClient.getRecordById(any(GetRecordByIdType.class))).thenReturn(collection);
    AbstractCswSource cswSource = getCswSource(cswClient, mockContext, null, null, null);
    ResourceReader reader = mock(ResourceReader.class);
    when(reader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(mock(ResourceResponse.class));
    cswSource.setResourceReader(reader);

    Map<String, Serializable> props = new HashMap<>();
    props.put(Core.ID, "ID");
    cswSource.retrieveResource(new URI("http://example.com/resource"), props);
    // Verify
    verify(cswClient, times(1)).getRecordById(any(GetRecordByIdType.class));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testRetrieveResourceUsingGetRecordByIdWithNoId() throws Exception {
    CswClient cswClient = createMockCswClient();
    CswRecordCollection collection = mock(CswRecordCollection.class);
    Resource resource = mock(Resource.class);
    when(collection.getResource()).thenReturn(resource);
    when(cswClient.getRecordById(any(GetRecordByIdType.class))).thenReturn(collection);
    AbstractCswSource cswSource = getCswSource(cswClient, mockContext, null, null, null);
    ResourceReader reader = mock(ResourceReader.class);
    when(reader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(mock(ResourceResponse.class));
    cswSource.setResourceReader(reader);

    Map<String, Serializable> props = new HashMap<>();
    cswSource.retrieveResource(new URI("http://example.com/resource"), props);
  }

  private CswSourceConfiguration getStandardCswSourceConfiguration(
      String contentTypeMapping, String queryTypeQName, String queryTypePrefix) {
    CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
    if (contentTypeMapping == null) {
      cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, CswConstants.CSW_TYPE);
    } else {
      cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, contentTypeMapping);
    }
    cswSourceConfiguration.setQueryTypeNamespace(queryTypePrefix);
    cswSourceConfiguration.setQueryTypeName(queryTypeQName);
    cswSourceConfiguration.setId(ID);
    cswSourceConfiguration.setCswUrl(URL);
    cswSourceConfiguration.putMetacardCswMapping(Core.MODIFIED, Core.MODIFIED);
    cswSourceConfiguration.putMetacardCswMapping(Core.ID, CswConstants.CSW_IDENTIFIER);
    cswSourceConfiguration.setCswAxisOrder(CswAxisOrder.LON_LAT);
    cswSourceConfiguration.setUsername("user");
    cswSourceConfiguration.setPassword("pass");
    return cswSourceConfiguration;
  }

  private AbstractCswSource getCswSource(CswClient cswClient, BundleContext context) {
    return getCswSource(cswClient, context, null);
  }

  private AbstractCswSource getCswSource(
      CswClient cswClient, BundleContext context, String contentMapping) {
    return getCswSource(
        cswClient,
        context,
        contentMapping,
        CswConstants.CSW_RECORD,
        CswConstants.CSW_OUTPUT_SCHEMA);
  }

  private AbstractCswSource getCswSource(
      CswClient cswClient,
      BundleContext context,
      String contentMapping,
      String queryTypeQName,
      String queryTypePrefix) {

    CswSourceConfiguration cswSourceConfiguration =
        getStandardCswSourceConfiguration(contentMapping, queryTypeQName, queryTypePrefix);
    cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, contentMapping);

    CswSourceStub cswSource =
        new CswSourceStub(mockContext, cswSourceConfiguration, mockProvider, cswClient);
    cswSource.setFilterAdapter(new GeotoolsFilterAdapterImpl());
    cswSource.setFilterBuilder(builder);
    cswSource.setContext(context);
    cswSource.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    cswSource.setAvailabilityTask(mockAvailabilityTask);
    cswSource.configureCswSource();

    return cswSource;
  }

  private void assertDefaultCswSourceConfiguration(
      CswSourceConfiguration cswSourceConfiguration,
      CswSourceConfiguration defaultCswSourceConfiguration) {
    assertThat(
        cswSourceConfiguration.getAuthenticationType(),
        is(defaultCswSourceConfiguration.getAuthenticationType()));
    assertThat(
        cswSourceConfiguration.getUsername(), is(defaultCswSourceConfiguration.getUsername()));
    assertThat(
        cswSourceConfiguration.getPassword(), is(defaultCswSourceConfiguration.getPassword()));
    assertThat(cswSourceConfiguration.getId(), is(defaultCswSourceConfiguration.getId()));
    assertThat(
        cswSourceConfiguration.getConnectionTimeout(),
        is(defaultCswSourceConfiguration.getConnectionTimeout()));
    assertThat(
        cswSourceConfiguration.getReceiveTimeout(),
        is(defaultCswSourceConfiguration.getReceiveTimeout()));
    assertThat(
        cswSourceConfiguration.getMetacardMapping(Core.ID),
        is(defaultCswSourceConfiguration.getMetacardMapping(Core.ID)));
    assertThat(
        cswSourceConfiguration.getDisableCnCheck(),
        is(defaultCswSourceConfiguration.getDisableCnCheck()));
    assertThat(
        cswSourceConfiguration.getCswAxisOrder().toString(),
        is(defaultCswSourceConfiguration.getCswAxisOrder().toString()));
    assertThat(
        cswSourceConfiguration.isSetUsePosList(),
        is(defaultCswSourceConfiguration.isSetUsePosList()));
    assertThat(
        cswSourceConfiguration.getMetacardMapping(Core.CREATED),
        is(defaultCswSourceConfiguration.getMetacardMapping(Core.CREATED)));
    assertThat(
        cswSourceConfiguration.getMetacardMapping(Metacard.EFFECTIVE),
        is(defaultCswSourceConfiguration.getMetacardMapping(Metacard.EFFECTIVE)));
    assertThat(
        cswSourceConfiguration.getMetacardMapping(Core.MODIFIED),
        is(defaultCswSourceConfiguration.getMetacardMapping(Core.MODIFIED)));
    assertThat(
        cswSourceConfiguration.getMetacardMapping(Metacard.CONTENT_TYPE),
        is(defaultCswSourceConfiguration.getMetacardMapping(Metacard.CONTENT_TYPE)));
    assertThat(
        cswSourceConfiguration.getPollIntervalMinutes(),
        is(defaultCswSourceConfiguration.getPollIntervalMinutes()));
    assertThat(cswSourceConfiguration.getCswUrl(), is(defaultCswSourceConfiguration.getCswUrl()));
    assertThat(
        cswSourceConfiguration.isCqlForced(), is(defaultCswSourceConfiguration.isCqlForced()));
  }

  private void assertConfigurationAfterRefresh(CswSourceConfiguration cswSourceConfiguration) {
    assertThat(cswSourceConfiguration.getAuthenticationType(), is(AUTHENTICATION_TYPE));
    assertThat(cswSourceConfiguration.getUsername(), is(USERNAME));
    assertThat(cswSourceConfiguration.getPassword(), is(PASSWORD));
    assertThat(cswSourceConfiguration.getId(), is(ID));
    assertThat(cswSourceConfiguration.getConnectionTimeout(), is(CONNECTION_TIMEOUT));
    assertThat(cswSourceConfiguration.getReceiveTimeout(), is(RECEIVE_TIMEOUT));
    assertThat(cswSourceConfiguration.getOutputSchema(), is(OUTPUT_SCHEMA));
    assertThat(cswSourceConfiguration.getQueryTypeName(), is(QUERY_TYPE_NAME));
    assertThat(cswSourceConfiguration.getQueryTypeNamespace(), is(QUERY_TYPE_NAMESPACE));
    assertThat(cswSourceConfiguration.getMetacardMapping(Core.ID), is(IDENTIFIER_MAPPING));
    assertThat(cswSourceConfiguration.getDisableCnCheck(), is(false));
    assertThat(cswSourceConfiguration.getCswAxisOrder().toString(), is(COORDINATE_ORDER));
    assertThat(cswSourceConfiguration.isSetUsePosList(), is(false));
    assertThat(cswSourceConfiguration.getMetacardMapping(Core.CREATED), is(CREATED_DATE));
    assertThat(cswSourceConfiguration.getMetacardMapping(Metacard.EFFECTIVE), is(EFFECTIVE_DATE));
    assertThat(cswSourceConfiguration.getMetacardMapping(Core.MODIFIED), is(MODIFIED_DATE));
    assertThat(cswSourceConfiguration.getMetacardMapping(Metacard.CONTENT_TYPE), is(CONTENT_TYPE));
    assertThat(cswSourceConfiguration.getPollIntervalMinutes(), is(POLL_INTERVAL));
    assertThat(cswSourceConfiguration.getCswUrl(), is(URL));
    assertThat(cswSourceConfiguration.isCqlForced(), is(false));
  }

  private Map<String, Object> getConfigurationMap(AbstractCswSource cswSource) {
    Map<String, Object> configuration = new HashMap<>();
    configuration.put(cswSource.AUTHENTICATION_TYPE, AUTHENTICATION_TYPE);
    configuration.put(cswSource.USERNAME_PROPERTY, USERNAME);
    configuration.put(cswSource.PASSWORD_PROPERTY, PASSWORD);
    configuration.put(cswSource.ID_PROPERTY, ID);
    configuration.put(cswSource.CONNECTION_TIMEOUT_PROPERTY, CONNECTION_TIMEOUT);
    configuration.put(cswSource.RECEIVE_TIMEOUT_PROPERTY, RECEIVE_TIMEOUT);
    configuration.put(cswSource.OUTPUT_SCHEMA_PROPERTY, OUTPUT_SCHEMA);
    configuration.put(cswSource.QUERY_TYPE_NAME_PROPERTY, QUERY_TYPE_NAME);
    configuration.put(cswSource.QUERY_TYPE_NAMESPACE_PROPERTY, QUERY_TYPE_NAMESPACE);
    configuration.put(cswSource.METACARD_MAPPINGS_PROPERTY, metacardMappings);
    configuration.put(cswSource.DISABLE_CN_CHECK_PROPERTY, false);
    configuration.put(cswSource.COORDINATE_ORDER_PROPERTY, COORDINATE_ORDER);
    configuration.put(cswSource.USE_POS_LIST_PROPERTY, false);
    configuration.put(cswSource.POLL_INTERVAL_PROPERTY, POLL_INTERVAL);
    configuration.put(cswSource.CSWURL_PROPERTY, URL);
    configuration.put(cswSource.IS_CQL_FORCED_PROPERTY, false);
    return configuration;
  }
}
