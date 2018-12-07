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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.encryption.EncryptionService;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
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
import org.xml.sax.SAXException;

public class CswSourceTest extends TestCswSourceBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswSourceTest.class);

  private Converter mockProvider = mock(Converter.class);

  private EncryptionService encryptionService = mock(EncryptionService.class);

  @Test
  public void testParseCapabilities() throws CswException, SecurityServiceException {
    AbstractCswSource source = getCswSource(createMockCsw(), mockContext);

    assertThat(source.isAvailable(), is(true));
    assertThat(source.getContentTypes(), hasSize(10));
    Set<ContentType> expected =
        generateContentType(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
    assertThat(source.getContentTypes(), is(expected));
  }

  @Test
  public void testInitialContentList() throws CswException, SecurityServiceException {
    AbstractCswSource source = getCswSource(createMockCsw(), mockContext);

    assertThat(source.isAvailable(), is(true));
    assertThat(source.getContentTypes(), hasSize(10));
    Set<ContentType> expected =
        generateContentType(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
    assertThat(source.getContentTypes(), is(expected));
  }

  @Test
  public void testAddingContentTypesOnQueries()
      throws CswException, UnsupportedQueryException, SecurityServiceException {
    Csw mockCsw = createMockCsw();

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

    AbstractCswSource source = getCswSource(mockCsw, mockContext);

    assertThat(source.getContentTypes(), hasSize(10));

    Set<ContentType> expected = generateContentType(expectedNames);
    assertThat(source.getContentTypes(), is(expected));

    CswRecordCollection collection = generateCswCollection("/getRecordsResponse.xml");

    when(mockCsw.getRecords(any(GetRecordsType.class))).thenReturn(collection);

    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("*"));

    expectedNames.add("dataset");
    expectedNames.add("dataset 2");
    expectedNames.add("dataset 3");
    expected = generateContentType(expectedNames);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));

    assertThat(source.getContentTypes(), hasSize(13));
    assertThat(source.getContentTypes(), is(expected));
  }

  @Test
  public void testPropertyIsLikeQuery()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    propertyIsLikeQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testQueryWitNaturalSorting()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {
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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    query.setSortBy(SortBy.NATURAL_ORDER);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testQueryWitNullSorting()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {
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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    query.setSortBy(null);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testQueryWithSorting()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testQueryWithSortByDistance()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {
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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testQueryWithSortByTemporal()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {
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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testPropertyIsEqualToQueryContentTypeIsMappedToFormat()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    QueryImpl propertyIsEqualToQuery =
        new QueryImpl(builder.attribute(Metacard.CONTENT_TYPE).is().text(format));
    propertyIsEqualToQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext, CswConstants.CSW_FORMAT);
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
   * Test to verify content type version mapping is correct. The CSW Source should be able to map a
   * csw:Record field to Content Type.
   */
  @Test
  public void testPropertyIsLikeContentTypeVersion()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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
    Filter versionFilter =
        builder.attribute(Metacard.CONTENT_TYPE_VERSION).is().like().text(version);
    Filter filter = builder.allOf(ctfilter, versionFilter);

    QueryImpl propertyIsEqualToQuery = new QueryImpl(filter);
    propertyIsEqualToQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext, CswConstants.CSW_FORMAT);
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
    Diff xmlDiff = new Diff(getRecordsControlXml202ConteTypeAndVersion, xml);

    if (!xmlDiff.similar()) {
      LOGGER.error("Unexpected XML request sent");
      LOGGER.error("Expected:\n {}", getRecordsControlXml202ConteTypeAndVersion);
      LOGGER.error("Actual:\n {}", xml);
    }

    assertXMLEqual(getRecordsControlXml202ConteTypeAndVersion, xml);
  }

  @Test
  public void testAbsoluteTemporalSearchPropertyIsBetweenQuery()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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
    Filter temporalFilter =
        builder
            .attribute(Metacard.EFFECTIVE)
            .is()
            .during()
            .dates(startDate.toDate(), endDate.toDate());
    QueryImpl temporalQuery = new QueryImpl(temporalFilter);
    temporalQuery.setPageSize(pageSize);

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

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
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);

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

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
    cswSource.setCswUrl(URL);
    cswSource.setId(ID);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("junk"));
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

    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext);
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
  public void testRefreshWithNullConfiguration() throws SecurityServiceException {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null, encryptionService);
    CswSourceConfiguration defaultCswSourceConfiguration =
        getStandardCswSourceConfiguration(null, null, null, encryptionService);

    // Assert that the default configuration is set
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);

    cswSource.refresh(null);

    // Assert that the configuration does not change with a null map
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);
  }

  @Test
  public void testRefreshWithEmptyConfiguration() throws SecurityServiceException {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null, encryptionService);
    CswSourceConfiguration defaultCswSourceConfiguration =
        getStandardCswSourceConfiguration(null, null, null, encryptionService);

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
  public void testRefresh() throws SecurityServiceException {
    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext, "contentType");
    CswSourceConfiguration defaultCswSourceConfiguration =
        getStandardCswSourceConfiguration("contentType", "qname", "queryprefix", encryptionService);

    // Assert Defaults
    assertDefaultCswSourceConfiguration(
        cswSource.cswSourceConfiguration, defaultCswSourceConfiguration);

    // Set Configuration Map
    Map<String, Object> configuration = getConfigurationMap(cswSource);
    when(encryptionService.decryptValue(PASSWORD)).thenReturn(PASSWORD);

    // Call Refresh
    cswSource.refresh(configuration);

    // Get Configuration
    CswSourceConfiguration cswSourceConfiguration = cswSource.cswSourceConfiguration;

    // Assert Refresh Changes
    assertConfigurationAfterRefresh(cswSourceConfiguration);
  }

  @Test
  public void testSetPassword() {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null, encryptionService);
    when(encryptionService.decryptValue("secret")).thenReturn("secret");

    cswSource.setPassword("secret");

    // The password is first initialized to "pass" on creation of the AbstractCswSource.
    // Verify the encryption service was called with "secret".
    // Assert the password is equal to "secret".
    verify(encryptionService, times(2)).decryptValue("secret");
    assertThat(cswSource.cswSourceConfiguration.getPassword(), is(equalTo("secret")));
  }

  @Test
  public void testSetPasswordWithEmptyPassword() {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null, encryptionService);
    when(encryptionService.decryptValue("")).thenReturn("");

    cswSource.setPassword("");

    // The password is first initialized to "pass" on creation of the AbstractCswSource.
    // Verify the encryption service was called with an empty password.
    // Assert the password is equal to "".
    verify(encryptionService, times(2)).decryptValue("");
    assertThat(cswSource.cswSourceConfiguration.getPassword(), is(equalTo("")));
  }

  @Test
  public void testSetPasswordWithNullEncryptionService() {
    AbstractCswSource cswSource = getCswSource(null, null, "type", null, null, null);

    cswSource.setPassword("secret");

    // Verify the encryption service was not called.
    // Assert the password is equal to the value it was set to.
    verify(encryptionService, times(0)).decryptValue("secret");
    assertThat(cswSource.cswSourceConfiguration.getPassword(), is(equalTo("secret")));
  }

  @Test
  public void testRefreshWithRegisterForEvents() throws Exception {
    String subscriptionId = "subscriptionid";
    String eventEndpoint = "https://ddf/services/csw/subscriptions";
    CswSourceStub cswSource = (CswSourceStub) getCswSource(mockCsw, mockContext, "contentType");

    CswSubscribe client = mock(CswSubscribe.class);
    Response response = mock(Response.class);
    AcknowledgementType ack = mock(AcknowledgementType.class);
    when(client.createRecordsSubscription(any(GetRecordsType.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.readEntity(any(Class.class))).thenReturn(ack);

    when(ack.getRequestId()).thenReturn(subscriptionId);
    when(cswSource.getSubscriberClientFactory().getClientForSubject(cswSource.getSubject()))
        .thenReturn(client);

    // Set Configuration Map
    Map<String, Object> configuration = getConfigurationMap(cswSource);
    configuration.put(cswSource.REGISTER_FOR_EVENTS, true);
    configuration.put(cswSource.EVENT_SERVICE_ADDRESS, eventEndpoint);

    // Call Refresh
    cswSource.refresh(configuration);

    // Get Configuration
    CswSourceConfiguration cswSourceConfiguration = cswSource.cswSourceConfiguration;

    // Assert Refresh Changes
    Assert.assertTrue(cswSourceConfiguration.isRegisterForEvents());
    Assert.assertEquals(cswSourceConfiguration.getEventServiceAddress(), eventEndpoint);
    verify(ack).getRequestId();
  }

  @Test
  public void testRefreshWithUpdateRegisterForEvents() throws Exception {
    String subscriptionId = "subscriptionid";
    String eventEndpoint = "https://ddf/services/csw/subscriptions";
    CswSourceStub cswSource = (CswSourceStub) getCswSource(mockCsw, mockContext, "contentType");
    cswSource.filterlessSubscriptionId = subscriptionId;
    CswSubscribe client = mock(CswSubscribe.class);
    Response response = mock(Response.class);
    AcknowledgementType ack = mock(AcknowledgementType.class);
    when(client.createRecordsSubscription(any(GetRecordsType.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.readEntity(any(Class.class))).thenReturn(ack);

    when(ack.getRequestId()).thenReturn(subscriptionId);
    when(cswSource.getSubscriberClientFactory().getClientForSubject(cswSource.getSubject()))
        .thenReturn(client);

    cswSource.cswSourceConfiguration.setRegisterForEvents(true);
    cswSource.cswSourceConfiguration.setEventServiceAddress(eventEndpoint + "/original");

    // Set Configuration Map
    Map<String, Object> configuration = getConfigurationMap(cswSource);
    configuration.put(cswSource.REGISTER_FOR_EVENTS, true);
    configuration.put(cswSource.EVENT_SERVICE_ADDRESS, eventEndpoint);

    // Call Refresh
    cswSource.refresh(configuration);

    // Get Configuration
    CswSourceConfiguration cswSourceConfiguration = cswSource.cswSourceConfiguration;

    // Assert Refresh Changes
    Assert.assertTrue(cswSourceConfiguration.isRegisterForEvents());
    Assert.assertEquals(cswSourceConfiguration.getEventServiceAddress(), eventEndpoint);
    verify(client).deleteRecordsSubscription(subscriptionId);
    verify(ack).getRequestId();
  }

  @Test
  public void testRefreshWithUnregisterForEvents() throws Exception {
    String subscriptionId = "subscriptionid";
    String eventEndpoint = "https://ddf/services/csw/subscriptions";
    CswSourceStub cswSource = (CswSourceStub) getCswSource(mockCsw, mockContext, "contentType");
    cswSource.filterlessSubscriptionId = subscriptionId;
    CswSubscribe client = mock(CswSubscribe.class);
    Response response = mock(Response.class);
    AcknowledgementType ack = mock(AcknowledgementType.class);
    when(client.createRecordsSubscription(any(GetRecordsType.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.readEntity(any(Class.class))).thenReturn(ack);

    when(ack.getRequestId()).thenReturn(subscriptionId);
    when(cswSource.getSubscriberClientFactory().getClientForSubject(cswSource.getSubject()))
        .thenReturn(client);

    cswSource.cswSourceConfiguration.setRegisterForEvents(true);
    cswSource.cswSourceConfiguration.setEventServiceAddress(eventEndpoint);

    // Set Configuration Map
    Map<String, Object> configuration = getConfigurationMap(cswSource);
    configuration.put(cswSource.REGISTER_FOR_EVENTS, false);
    configuration.put(cswSource.EVENT_SERVICE_ADDRESS, eventEndpoint);

    // Call Refresh
    cswSource.refresh(configuration);

    // Get Configuration
    CswSourceConfiguration cswSourceConfiguration = cswSource.cswSourceConfiguration;

    // Assert Refresh Changes
    Assert.assertFalse(cswSourceConfiguration.isRegisterForEvents());
    Assert.assertEquals(cswSourceConfiguration.getEventServiceAddress(), eventEndpoint);
    verify(client).deleteRecordsSubscription(subscriptionId);
  }

  @Test
  public void testQueryWithAlternateQueryType()
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);

    AbstractCswSource cswSource =
        getCswSource(
            mockCsw,
            mockContext,
            null,
            expectedQname.getPrefix() + ":" + expectedQname.getLocalPart(),
            expectedQname.getNamespaceURI(),
            null);

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
      throws JAXBException, UnsupportedQueryException, DatatypeConfigurationException, SAXException,
          IOException, SecurityServiceException {

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

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);

    // Verify passing a null config for qname/prefix falls back to CSW Record
    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext, null, null, null, null);
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
    assertThat(
        cswQuery.getTypeNames().get(0),
        is(new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CSW_RECORD_LOCAL_NAME)));
  }

  @Test
  public void testCreateResults() throws SecurityServiceException {
    AbstractCswSource cswSource =
        getCswSource(mockCsw, mockContext, null, null, null, encryptionService);
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
  public void testRetrieveResourceUsingReader()
      throws URISyntaxException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException, CswException {
    configureMockCsw();
    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext, null, null, null, null);
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
  public void testRetrieveResourceUsingReaderBasicAuth()
      throws URISyntaxException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException, CswException {
    configureMockCsw();
    AbstractCswSource cswSource = getCswSource(mockCsw, mockContext, null, null, null, null);
    ResourceReader reader = mock(ResourceReader.class);
    when(reader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(mock(ResourceResponse.class));
    cswSource.setResourceReader(reader);

    cswSource.setUsername("user");
    cswSource.setPassword("secret");

    Map<String, Serializable> props = new HashMap<>();
    props.put(Core.ID, "ID");
    cswSource.retrieveResource(new URI("http://example.com/resource"), props);
    verify(reader, times(1))
        .retrieveResource(
            any(URI.class),
            argThat(
                allOf(
                    hasEntry("username", (Serializable) "user"),
                    hasEntry("password", (Serializable) "secret"))));
  }

  @Test
  public void testRetrieveResourceUsingGetRecordById()
      throws CswException, ResourceNotFoundException, IOException, ResourceNotSupportedException,
          URISyntaxException {
    Csw csw = createMockCsw();
    CswRecordCollection collection = mock(CswRecordCollection.class);
    Resource resource = mock(Resource.class);
    when(collection.getResource()).thenReturn(resource);
    when(csw.getRecordById(any(GetRecordByIdRequest.class), anyString())).thenReturn(collection);
    AbstractCswSource cswSource = getCswSource(csw, mockContext, null, null, null, null);
    ResourceReader reader = mock(ResourceReader.class);
    when(reader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(mock(ResourceResponse.class));
    cswSource.setResourceReader(reader);

    Map<String, Serializable> props = new HashMap<>();
    props.put(Core.ID, "ID");
    cswSource.retrieveResource(new URI("http://example.com/resource"), props);
    // Verify
    verify(csw, times(1)).getRecordById(any(GetRecordByIdRequest.class), any(String.class));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testRetrieveResourceUsingGetRecordByIdWithNoId()
      throws CswException, ResourceNotFoundException, IOException, ResourceNotSupportedException,
          URISyntaxException {
    Csw csw = createMockCsw();
    CswRecordCollection collection = mock(CswRecordCollection.class);
    Resource resource = mock(Resource.class);
    when(collection.getResource()).thenReturn(resource);
    when(csw.getRecordById(any(GetRecordByIdRequest.class), anyString())).thenReturn(collection);
    AbstractCswSource cswSource = getCswSource(csw, mockContext, null, null, null, null);
    ResourceReader reader = mock(ResourceReader.class);
    when(reader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(mock(ResourceResponse.class));
    cswSource.setResourceReader(reader);

    Map<String, Serializable> props = new HashMap<>();
    cswSource.retrieveResource(new URI("http://example.com/resource"), props);
  }

  private CswSourceConfiguration getStandardCswSourceConfiguration(
      String contentTypeMapping,
      String queryTypeQName,
      String queryTypePrefix,
      EncryptionService encryptionService) {
    CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration(encryptionService);
    if (encryptionService != null) {
      when(encryptionService.decryptValue("pass")).thenReturn("pass");
    }
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

  private AbstractCswSource getCswSource(Csw csw, BundleContext context)
      throws SecurityServiceException {
    return getCswSource(csw, context, null);
  }

  private AbstractCswSource getCswSource(Csw csw, BundleContext context, String contentMapping) {
    return getCswSource(
        csw,
        context,
        contentMapping,
        CswConstants.CSW_RECORD,
        CswConstants.CSW_OUTPUT_SCHEMA,
        encryptionService);
  }

  private AbstractCswSource getCswSource(
      Csw csw,
      BundleContext context,
      String contentMapping,
      String queryTypeQName,
      String queryTypePrefix,
      EncryptionService encryptionService) {

    CswSourceConfiguration cswSourceConfiguration =
        getStandardCswSourceConfiguration(
            contentMapping, queryTypeQName, queryTypePrefix, encryptionService);
    cswSourceConfiguration.putMetacardCswMapping(Metacard.CONTENT_TYPE, contentMapping);

    SecureCxfClientFactory mockFactory = mock(SecureCxfClientFactory.class);
    doReturn(csw).when(mockFactory).getClient();
    doReturn(csw).when(mockFactory).getClientForSubject(nullable(Subject.class));

    ClientFactoryFactory clientFactoryFactory = mock(ClientFactoryFactory.class);
    when(clientFactoryFactory.getSecureCxfClientFactory(nullable(String.class), any(Class.class)))
        .thenReturn(mockFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            nullable(String.class),
            nullable(Class.class),
            nullable(List.class),
            nullable(Interceptor.class),
            anyBoolean(),
            anyBoolean()))
        .thenReturn(mockFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            nullable(String.class),
            nullable(Class.class),
            nullable(List.class),
            nullable(Interceptor.class),
            anyBoolean(),
            anyBoolean(),
            nullable(PropertyResolver.class)))
        .thenReturn(mockFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            nullable(String.class),
            nullable(Class.class),
            nullable(List.class),
            nullable(Interceptor.class),
            anyBoolean(),
            anyBoolean(),
            nullable(Integer.class),
            nullable(Integer.class)))
        .thenReturn(mockFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            nullable(String.class),
            nullable(Class.class),
            nullable(List.class),
            nullable(Interceptor.class),
            anyBoolean(),
            anyBoolean(),
            nullable(Integer.class),
            nullable(Integer.class),
            nullable(String.class),
            nullable(String.class)))
        .thenReturn(mockFactory);
    when(clientFactoryFactory.getSecureCxfClientFactory(
            nullable(String.class),
            nullable(Class.class),
            nullable(List.class),
            nullable(Interceptor.class),
            anyBoolean(),
            anyBoolean(),
            nullable(Integer.class),
            nullable(Integer.class),
            nullable(String.class),
            nullable(String.class),
            nullable(String.class)))
        .thenReturn(mockFactory);

    CswSourceStub cswSource =
        new CswSourceStub(
            mockContext,
            cswSourceConfiguration,
            mockProvider,
            clientFactoryFactory,
            encryptionService);
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
    assertThat(cswSourceConfiguration.getUsername(), is(USERNAME));
    assertThat(cswSourceConfiguration.getPassword(), is(PASSWORD));
    assertThat(cswSourceConfiguration.getCertAlias(), is(CERT_ALIAS));
    assertThat(cswSourceConfiguration.getKeystorePath(), is(KEYSTORE_PATH));
    assertThat(cswSourceConfiguration.getSslProtocol(), is(SSL_PROTOCOL));
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
    configuration.put(cswSource.USERNAME_PROPERTY, USERNAME);
    configuration.put(cswSource.PASSWORD_PROPERTY, PASSWORD);
    configuration.put(cswSource.CERT_ALIAS_PROPERTY, CERT_ALIAS);
    configuration.put(cswSource.KEYSTORE_PATH_PROPERTY, KEYSTORE_PATH);
    configuration.put(cswSource.SSL_PROTOCOL_PROPERTY, SSL_PROTOCOL);
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
