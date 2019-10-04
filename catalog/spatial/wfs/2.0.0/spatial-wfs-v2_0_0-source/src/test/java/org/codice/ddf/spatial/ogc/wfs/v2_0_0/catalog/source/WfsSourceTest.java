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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import net.opengis.filter.v_2_0_0.ConformanceType;
import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.filter.v_2_0_0.SortByType;
import net.opengis.filter.v_2_0_0.SortOrderType;
import net.opengis.ows.v_1_1_0.DomainType;
import net.opengis.ows.v_1_1_0.ValueType;
import net.opengis.wfs.v_2_0_0.FeatureTypeListType;
import net.opengis.wfs.v_2_0_0.FeatureTypeType;
import net.opengis.wfs.v_2_0_0.GetFeatureType;
import net.opengis.wfs.v_2_0_0.QueryType;
import net.opengis.wfs.v_2_0_0.WFSCapabilitiesType;
import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl.MetacardMapperImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsUriResolver;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20FeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source.reader.FeatureCollectionMessageBodyReaderWfs20;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;

public class WfsSourceTest {
  private static final Map<String, String> NAMESPACE_CONTEXT =
      ImmutableMap.of(
          "wfs", "http://www.opengis.net/wfs/2.0", "ogc", "http://www.opengis.net/fes/2.0");

  private static final String ONE_TEXT_PROPERTY_SCHEMA =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"title\" type=\"xs:string\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "</xs:schema>";

  private static final String SAMPLE_FEATURE_NAME = "SampleFeature";

  private static final Integer NULL_NUM_RETURNED = 9889;

  private static final String LITERAL = "literal";

  private static JAXBContext jaxbContext;

  private final GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();

  private Wfs mockWfs = mock(Wfs.class);

  private WFSCapabilitiesType mockCapabilities = new WFSCapabilitiesType();

  private BundleContext mockContext = mock(BundleContext.class);

  private AvailabilityTask mockAvailabilityTask = mock(AvailabilityTask.class);

  private FeatureCollectionMessageBodyReaderWfs20 mockReader =
      mock(FeatureCollectionMessageBodyReaderWfs20.class);

  private Wfs20FeatureCollection mockFeatureCollection = mock(Wfs20FeatureCollection.class);

  private SecureCxfClientFactory mockFactory = mock(SecureCxfClientFactory.class);

  private ClientFactoryFactory mockClientFactory = mock(ClientFactoryFactory.class);

  private EncryptionService encryptionService = mock(EncryptionService.class);

  private ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);

  private List<MetacardMapper> metacardMappers = new ArrayList<>();

  @BeforeClass
  public static void setupClass() {
    try {
      jaxbContext = JAXBContext.newInstance("net.opengis.wfs.v_2_0_0");
    } catch (JAXBException e) {
      fail(e.getMessage());
    }
  }

  private WfsSource getWfsSource(
      final String schema, final FilterCapabilities filterCapabilities, final int numFeatures)
      throws WfsException {

    return getWfsSource(schema, filterCapabilities, numFeatures, false);
  }

  private WfsSource getWfsSource(
      final String schema,
      final FilterCapabilities filterCapabilities,
      final int numFeatures,
      final boolean throwExceptionOnDescribeFeatureType)
      throws WfsException {
    mockFactory = mock(SecureCxfClientFactory.class);
    when(mockFactory.getClient()).thenReturn(mockWfs);

    when(mockClientFactory.getSecureCxfClientFactory(any(), any())).thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(),
            any(),
            any(),
            any(),
            anyBoolean(),
            anyBoolean(),
            anyInt(),
            anyInt(),
            anyString(),
            anyString()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(),
            any(),
            any(),
            any(),
            anyBoolean(),
            anyBoolean(),
            anyInt(),
            anyInt(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(mockFactory);

    // GetCapabilities Response
    when(mockWfs.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(mockCapabilities);

    mockCapabilities.setFilterCapabilities(filterCapabilities);

    when(mockAvailabilityTask.isAvailable()).thenReturn(true);

    mockCapabilities.setFeatureTypeList(new FeatureTypeListType());
    for (int ii = 0; ii < numFeatures; ii++) {
      FeatureTypeType feature = new FeatureTypeType();
      QName qName;
      if (ii == 0) {
        qName = new QName(SAMPLE_FEATURE_NAME + ii);
      } else {
        qName = new QName("http://example.com", SAMPLE_FEATURE_NAME + ii, "Prefix" + ii);
      }
      feature.setName(qName);
      feature.setDefaultCRS(GeospatialUtil.EPSG_4326_URN);
      mockCapabilities.getFeatureTypeList().getFeatureType().add(feature);
    }

    XmlSchema xmlSchema = null;
    if (StringUtils.isNotBlank(schema)) {
      XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
      WfsUriResolver wfsUriResolver = new WfsUriResolver();
      wfsUriResolver.setGmlNamespace(Wfs20Constants.GML_3_2_NAMESPACE);
      wfsUriResolver.setWfsNamespace(Wfs20Constants.WFS_2_0_NAMESPACE);
      schemaCollection.setSchemaResolver(wfsUriResolver);
      xmlSchema =
          schemaCollection.read(new StreamSource(new ByteArrayInputStream(schema.getBytes())));
    }

    if (throwExceptionOnDescribeFeatureType) {
      when(mockWfs.describeFeatureType(any(DescribeFeatureTypeRequest.class)))
          .thenThrow(new WfsException(""));

    } else {
      when(mockWfs.describeFeatureType(any(DescribeFeatureTypeRequest.class)))
          .thenReturn(xmlSchema);
    }
    // when(mockWfs.getFeatureCollectionReader()).thenReturn(mockReader);

    // GetFeature Response
    when(mockWfs.getFeature(any(GetFeatureType.class))).thenReturn(mockFeatureCollection);

    when(mockFeatureCollection.getNumberReturned()).thenReturn(BigInteger.valueOf(numFeatures));

    when(mockFeatureCollection.getMembers())
        .thenAnswer(
            new Answer<List<Metacard>>() {
              @Override
              public List<Metacard> answer(InvocationOnMock invocation) {
                // Create as many metacards as there are features
                List<Metacard> metacards = new ArrayList<>(numFeatures);
                for (int i = 0; i < numFeatures; i++) {
                  MetacardImpl mc = new MetacardImpl();
                  mc.setId("ID_" + (i + 1));
                  metacards.add(mc);
                }

                return metacards;
              }
            });

    MetacardMapper mockMapper = mock(MetacardMapper.class);
    doReturn(SAMPLE_FEATURE_NAME).when(mockMapper).getFeatureType();
    metacardMappers.add(mockMapper);

    final ScheduledFuture<?> mockAvailabilityPollFuture = mock(ScheduledFuture.class);
    doReturn(mockAvailabilityPollFuture)
        .when(mockScheduler)
        .scheduleWithFixedDelay(any(), anyInt(), anyInt(), any());

    WfsSource source = new WfsSource(mockClientFactory, encryptionService, mockScheduler);
    source.setContext(mockContext);
    source.setFilterAdapter(new GeotoolsFilterAdapterImpl());
    source.setMetacardToFeatureMapper(metacardMappers);
    source.setPollInterval(10);
    source.init();
    return source;
  }

  private WfsSource getWfsSource(
      final String schema,
      final FilterCapabilities filterCapabilities,
      final int numFeatures,
      final boolean throwExceptionOnDescribeFeatureType,
      boolean prefix,
      int numReturned)
      throws WfsException {

    mockFactory = mock(SecureCxfClientFactory.class);
    when(mockFactory.getClient()).thenReturn(mockWfs);

    when(mockClientFactory.getSecureCxfClientFactory(any(), any())).thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(),
            any(),
            any(),
            any(),
            anyBoolean(),
            anyBoolean(),
            anyInt(),
            anyInt(),
            anyString(),
            anyString()))
        .thenReturn(mockFactory);
    when(mockClientFactory.getSecureCxfClientFactory(
            anyString(),
            any(),
            any(),
            any(),
            anyBoolean(),
            anyBoolean(),
            anyInt(),
            anyInt(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(mockFactory);

    // GetCapabilities Response
    when(mockWfs.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(mockCapabilities);

    when(mockFeatureCollection.getMembers())
        .thenAnswer(
            new Answer<List<Metacard>>() {
              @Override
              public List<Metacard> answer(InvocationOnMock invocation) {
                // Create as many metacards as there are features
                List<Metacard> metacards = new ArrayList<>(numFeatures);
                for (int i = 0; i < numFeatures; i++) {
                  MetacardImpl mc = new MetacardImpl();
                  mc.setId("ID_" + (i + 1));
                  metacards.add(mc);
                }

                return metacards;
              }
            });

    if (numReturned != NULL_NUM_RETURNED) {
      when(mockFeatureCollection.getNumberReturned()).thenReturn(BigInteger.valueOf(numReturned));
    } else {
      when(mockFeatureCollection.getNumberReturned()).thenReturn(null);
    }

    when(mockWfs.getFeature(any(GetFeatureType.class))).thenReturn(mockFeatureCollection);
    mockCapabilities.setFilterCapabilities(filterCapabilities);

    when(mockAvailabilityTask.isAvailable()).thenReturn(true);

    mockCapabilities.setFeatureTypeList(new FeatureTypeListType());
    for (int ii = 0; ii < numFeatures; ii++) {
      FeatureTypeType feature = new FeatureTypeType();
      QName qName;
      if (prefix) {
        qName = new QName("http://example.com", SAMPLE_FEATURE_NAME + ii, "Prefix" + ii);
      } else {
        qName = new QName("http://example.com", SAMPLE_FEATURE_NAME + ii);
      }
      feature.setName(qName);
      feature.setDefaultCRS(GeospatialUtil.EPSG_4326_URN);
      mockCapabilities.getFeatureTypeList().getFeatureType().add(feature);
    }

    XmlSchema xmlSchema = null;
    if (StringUtils.isNotBlank(schema)) {
      XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
      WfsUriResolver wfsUriResolver = new WfsUriResolver();
      wfsUriResolver.setGmlNamespace(Wfs20Constants.GML_3_2_NAMESPACE);
      wfsUriResolver.setWfsNamespace(Wfs20Constants.WFS_2_0_NAMESPACE);
      schemaCollection.setSchemaResolver(wfsUriResolver);
      xmlSchema =
          schemaCollection.read(new StreamSource(new ByteArrayInputStream(schema.getBytes())));
    }

    if (throwExceptionOnDescribeFeatureType) {
      when(mockWfs.describeFeatureType(any(DescribeFeatureTypeRequest.class)))
          .thenThrow(new WfsException(""));

    } else {
      when(mockWfs.describeFeatureType(any(DescribeFeatureTypeRequest.class)))
          .thenReturn(xmlSchema);
    }

    final ScheduledFuture<?> mockAvailabilityPollFuture = mock(ScheduledFuture.class);
    doReturn(mockAvailabilityPollFuture)
        .when(mockScheduler)
        .scheduleWithFixedDelay(any(), anyInt(), anyInt(), any());

    WfsSource wfsSource = new WfsSource(mockClientFactory, encryptionService, mockScheduler);
    wfsSource.setContext(mockContext);
    wfsSource.setFilterAdapter(new GeotoolsFilterAdapterImpl());
    wfsSource.setFeatureCollectionReader(mockReader);
    wfsSource.setMetacardToFeatureMapper(metacardMappers);
    wfsSource.setPollInterval(10);
    wfsSource.init();
    return wfsSource;
  }

  @Test
  public void testAvailability() throws WfsException {
    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1);
    assertTrue(source.isAvailable());
  }

  @Test
  public void testParseCapabilities() throws WfsException {
    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1);

    assertTrue(source.isAvailable());
    assertThat(source.featureTypeFilters.size(), is(1));
    WfsFilterDelegate delegate =
        source.featureTypeFilters.get(new QName(SAMPLE_FEATURE_NAME + "0"));
    assertThat(delegate, notNullValue());
  }

  @Test
  public void testParseCapabilitiesNoFeatures() throws WfsException {
    WfsSource source = getWfsSource("", MockWfsServer.getFilterCapabilities(), 0);

    assertThat(
        "The source should not be available because the GetCapabilities response has no features.",
        source.isAvailable(),
        is(false));
    assertThat(
        "The source should not have any feature type filters because the GetCapabilities response has no features.",
        source.featureTypeFilters.size(),
        is(0));
  }

  @Test
  public void testParseCapabilitiesNoFilterCapabilities() throws WfsException {
    WfsSource source = getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, null, 1);

    assertThat(
        "The source should not be available because the GetCapabilities response has no filter capabilities.",
        source.isAvailable(),
        is(false));
    assertThat(
        "The source should not have any feature type filters because the GetCapabilities response has no filter capabilities.",
        source.featureTypeFilters.size(),
        is(0));
  }

  @Test
  public void testConfigureFeatureTypes() throws WfsException {
    ArgumentCaptor<DescribeFeatureTypeRequest> captor =
        ArgumentCaptor.forClass(DescribeFeatureTypeRequest.class);

    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1);

    final String SAMPLE_FEATURE_NAME0 = SAMPLE_FEATURE_NAME + "0";

    verify(mockWfs).describeFeatureType(captor.capture());

    DescribeFeatureTypeRequest describeFeatureType = captor.getValue();

    // sample feature 0 does not have a prefix
    assertThat(SAMPLE_FEATURE_NAME0, equalTo(describeFeatureType.getTypeName()));

    assertTrue(source.isAvailable());
    assertThat(source.featureTypeFilters.size(), is(1));
    WfsFilterDelegate delegate = source.featureTypeFilters.get(new QName(SAMPLE_FEATURE_NAME0));
    assertThat(delegate, notNullValue());

    assertThat(source.getContentTypes().size(), is(1));

    List<ContentType> types = new ArrayList<>();
    types.addAll(source.getContentTypes());

    assertTrue(SAMPLE_FEATURE_NAME0.equals(types.get(0).getName()));
  }

  @Test
  public void testConfigureFeatureTypesDescribeFeatureException() throws WfsException {
    ArgumentCaptor<DescribeFeatureTypeRequest> captor =
        ArgumentCaptor.forClass(DescribeFeatureTypeRequest.class);

    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, true);

    final String SAMPLE_FEATURE_NAME0 = SAMPLE_FEATURE_NAME + "0";

    verify(mockWfs).describeFeatureType(captor.capture());

    DescribeFeatureTypeRequest describeFeatureType = captor.getValue();

    // sample feature 0 does not have a prefix
    assertThat(SAMPLE_FEATURE_NAME0, equalTo(describeFeatureType.getTypeName()));

    assertTrue(source.featureTypeFilters.isEmpty());

    assertTrue(source.getContentTypes().isEmpty());
  }

  @Test
  public void testTypeNameHasPrefix() throws Exception {

    // Setup
    final String TITLE = "title";
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 3, false, true, 3);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    // Perform test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Validate
    List<JAXBElement<?>> queryList = featureType.getAbstractQueryExpression();
    for (JAXBElement<?> queryType : queryList) {
      Object val = queryType.getValue();
      QueryType queryTypeVal = (QueryType) val;
      assertThat(queryTypeVal.getTypeNames().get(0), containsString("Prefix"));
      assertThat(queryTypeVal.getTypeNames().get(0), containsString(":"));
      assertThat(queryTypeVal.getTypeNames().get(0), containsString("SampleFeature"));
    }
  }

  @Test
  public void testTypeNameHasNoPrefix() throws Exception {

    // Setup
    final String TITLE = "title";
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 3, false, false, 3);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    // Perform test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Validate
    List<JAXBElement<?>> queryList = featureType.getAbstractQueryExpression();
    for (JAXBElement<?> queryType : queryList) {
      Object val = queryType.getValue();
      QueryType queryTypeVal = (QueryType) val;
      assertThat(queryTypeVal.getTypeNames().get(0), containsString("SampleFeature"));
      assertThat(queryTypeVal.getTypeNames().get(0), is(not(containsString("Prefix"))));
      assertThat(queryTypeVal.getTypeNames().get(0), is(not(containsString(":"))));
    }
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=0, should get 4 results back - metacards 1 thru 4.
   */
  @Test
  public void testPagingStartIndexZero() throws Exception {

    // Setup
    int pageSize = 4;
    int startIndex = 0;

    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);
    Filter filter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);
    Query query = new QueryImpl(filter, startIndex, pageSize, null, false, 0);

    // Execute
    GetFeatureType featureType = source.buildGetFeatureRequest(query);
    BigInteger startIndexGetFeature = featureType.getStartIndex();
    BigInteger countGetFeature = featureType.getCount();

    // Verify
    assertThat(countGetFeature.intValue(), is(pageSize));
    assertThat(startIndexGetFeature.intValue(), is(startIndex));
  }

  /**
   * Verify that, per DDF Query API Javadoc, if the startIndex is negative, the WfsSource throws an
   * UnsupportedQueryException.
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testPagingStartIndexNegative() throws Exception {
    // Setup
    int pageSize = 4;
    int startIndex = -1;

    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);
    Filter filter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);
    Query query = new QueryImpl(filter, startIndex, pageSize, null, false, 0);

    // Execute
    source.buildGetFeatureRequest(query);
  }

  /**
   * Verify that, per DDF Query API Javadoc, if the startIndex is negative, the WfsSource throws an
   * UnsupportedQueryException.
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testPagingPageSizeNegative() throws Exception {
    // Setup
    int pageSize = -4;
    int startIndex = 0;

    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);
    Filter filter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);
    Query query = new QueryImpl(filter, startIndex, pageSize, null, false, 0);

    // Execute
    source.buildGetFeatureRequest(query);
  }

  @Test
  public void testResultNumReturnedNegative() throws Exception {
    // Setup
    final String TITLE = "title";
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 3, false, true, -1);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);
    QueryRequestImpl queryReq = new QueryRequestImpl(query);

    // Perform test
    source.query(queryReq);
  }

  /**
   * If numberReturned is null, then query should return back size equivalent to the number of
   * members in the feature collection.
   */
  @Test
  public void testResultNumReturnedIsNull() throws Exception {
    // Setup
    final String TITLE = "title";
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA,
            MockWfsServer.getFilterCapabilities(),
            3,
            false,
            true,
            NULL_NUM_RETURNED);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);
    QueryRequestImpl queryReq = new QueryRequestImpl(query);

    // Perform test
    SourceResponse resp = source.query(queryReq);
    assertEquals(3, resp.getResults().size());
  }

  /**
   * If numberReturned is null, then query should return back size equivalent to the number of
   * members in the feature collection.
   */
  @Test
  public void testResultNumReturnedIsWrong() throws Exception {
    // Setup
    final String TITLE = "title";
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 3, false, true, 5);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);
    QueryRequestImpl queryReq = new QueryRequestImpl(query);

    // Perform test
    SourceResponse resp = source.query(queryReq);
    assertEquals(3, resp.getResults().size());
  }

  @Test
  public void testResultNumReturnedIsZero() throws Exception {
    // Setup
    final String TITLE = "title";
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 3, false, true, 0);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(TITLE, SortOrder.DESCENDING);
    query.setSortBy(sortBy);
    QueryRequestImpl queryReq = new QueryRequestImpl(query);

    // Perform test
    SourceResponse resp = source.query(queryReq);
    assertEquals(3, resp.getResults().size());
  }

  /**
   * Verify that the SortBy is set with the mapped Feature Property and a ASC sort order. In this
   * case, the incoming sort property of TEMPORAL is mapped to myTemporalFeatureProperty.
   *
   * <p><?xml version="1.0" encoding="UTF-8" standalone="yes"?> <ns5:GetFeature startIndex="1"
   * count="1" service="WFS" version="2.0.0" xmlns:ns2="http://www.opengis.net/ows/1.1"
   * xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml"
   * xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/wfs/2.0"> <ns5:Query
   * typeNames="SampleFeature0" handle="SampleFeature0"> <Filter> <PropertyIsLike wildCard="*"
   * singleChar="?" escapeChar="!"> <Literal>*</Literal> <ValueReference>title</ValueReference>
   * </PropertyIsLike> </Filter> <SortBy> <SortProperty>
   * <ValueReference>myTemporalFeatureProperty</ValueReference> <SortOrder>ASC</SortOrder>
   * </SortProperty> </SortBy> </ns5:Query> </ns5:GetFeature>
   */
  @Test
  public void testSortingAscendingSortingSupported() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final String mockTemporalFeatureProperty = "myTemporalFeatureProperty";
    final String mockFeatureType = "{http://example.com}" + SAMPLE_FEATURE_NAME + 0;
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false, false, 0);
    MetacardMapper mockMetacardMapper = mock(MetacardMapper.class);
    when(mockMetacardMapper.getFeatureType()).thenReturn(mockFeatureType);
    when(mockMetacardMapper.getSortByTemporalFeatureProperty())
        .thenReturn(mockTemporalFeatureProperty);
    List<MetacardMapper> mappers = new ArrayList<MetacardMapper>(1);
    mappers.add(mockMetacardMapper);
    source.setMetacardToFeatureMapper(mappers);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING);
    query.setSortBy(sortBy);

    // Perform Test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Verify
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();
    JAXBElement<?> abstractSortingClause = queryType.getAbstractSortingClause();
    SortByType sortByType = (SortByType) abstractSortingClause.getValue();
    assertThat(
        sortByType.getSortProperty().get(0).getValueReference(), is(mockTemporalFeatureProperty));
    assertThat(
        sortByType.getSortProperty().get(0).getSortOrder().name(), is(SortOrderType.ASC.value()));
  }

  /**
   * Verify that the SortBy is NOT set. In this case, sorting is not supported in the capabilities.
   */
  @Test
  public void testSortingAscendingSortingNotSupported() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final String mockTemporalFeatureProperty = "myTemporalFeatureProperty";
    final String mockFeatureType = "{http://example.com}" + SAMPLE_FEATURE_NAME + 0;
    final int pageSize = 1;

    // Set ImplementsSorting to FALSE (sorting not supported)
    FilterCapabilities mockCapabilitiesSortingNotSupported = MockWfsServer.getFilterCapabilities();
    ConformanceType conformance = mockCapabilitiesSortingNotSupported.getConformance();
    List<DomainType> domainTypes = conformance.getConstraint();
    for (DomainType domainType : domainTypes) {
      if (StringUtils.equals(domainType.getName(), "ImplementsSorting")) {
        ValueType valueType = new ValueType();
        valueType.setValue("FALSE");
        domainType.setDefaultValue(valueType);
        break;
      }
    }

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, mockCapabilitiesSortingNotSupported, 1, false, false, 0);
    MetacardMapper mockMetacardMapper = mock(MetacardMapper.class);
    when(mockMetacardMapper.getFeatureType()).thenReturn(mockFeatureType);
    when(mockMetacardMapper.getSortByTemporalFeatureProperty())
        .thenReturn(mockTemporalFeatureProperty);
    List<MetacardMapper> mappers = new ArrayList<>(1);
    mappers.add(mockMetacardMapper);
    source.setMetacardToFeatureMapper(mappers);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING);
    query.setSortBy(sortBy);

    // Perform Test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Verify
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();
    assertFalse(queryType.isSetAbstractSortingClause());
  }

  /**
   * Verify that the SortBy is NOT set. In this case, there is no mapping for the incoming sort
   * property of TEMPORAL so no SortBy should be set.
   *
   * <p><?xml version="1.0" encoding="UTF-8" standalone="yes"?> <ns5:GetFeature startIndex="1"
   * count="1" service="WFS" version="2.0.0" xmlns:ns2="http://www.opengis.net/ows/1.1"
   * xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml"
   * xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/wfs/2.0"> <ns5:Query
   * typeNames="SampleFeature0" handle="SampleFeature0"> <Filter> <PropertyIsLike wildCard="*"
   * singleChar="?" escapeChar="!"> <Literal>*</Literal> <ValueReference>title</ValueReference>
   * </PropertyIsLike> </Filter> </ns5:Query> </ns5:GetFeature>
   */
  @Test
  public void testSortingAscendingNoFeaturePropertyMappingSortingSupported() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final String mockFeatureType = "{http://example.com}" + SAMPLE_FEATURE_NAME + 0;
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false, false, 0);
    MetacardMapper mockMetacardMapper = mock(MetacardMapper.class);
    when(mockMetacardMapper.getFeatureType()).thenReturn(mockFeatureType);

    List<MetacardMapper> mappers = new ArrayList<>(1);
    mappers.add(mockMetacardMapper);
    source.setMetacardToFeatureMapper(mappers);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING);
    query.setSortBy(sortBy);

    // Perform Test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Verify
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();
    assertFalse(queryType.isSetAbstractSortingClause());
  }

  /**
   * Verify that the SortBy is set with the mapped Feature Property and a DESC sort order. In this
   * case, the incoming sort property of TEMPORAL is mapped to myTemporalFeatureProperty.
   *
   * <p><?xml version="1.0" encoding="UTF-8" standalone="yes"?> <ns5:GetFeature startIndex="1"
   * count="1" service="WFS" version="2.0.0" xmlns:ns2="http://www.opengis.net/ows/1.1"
   * xmlns="http://www.opengis.net/fes/2.0" xmlns:ns4="http://www.opengis.net/gml"
   * xmlns:ns3="http://www.w3.org/1999/xlink" xmlns:ns5="http://www.opengis.net/wfs/2.0"> <ns5:Query
   * typeNames="SampleFeature0" handle="SampleFeature0"> <Filter> <PropertyIsLike wildCard="*"
   * singleChar="?" escapeChar="!"> <Literal>*</Literal> <ValueReference>title</ValueReference>
   * </PropertyIsLike> </Filter> <SortBy> <SortProperty>
   * <ValueReference>myTemporalFeatureProperty</ValueReference> <SortOrder>DESC</SortOrder>
   * </SortProperty> </SortBy> </ns5:Query> </ns5:GetFeature>
   */
  @Test
  public void testSortingDescendingSortingSupported() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final String mockTemporalFeatureProperty = "myTemporalFeatureProperty";
    final String mockFeatureType = "{http://example.com}" + SAMPLE_FEATURE_NAME + 0;
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false, false, 0);
    MetacardMapper mockMetacardMapper = mock(MetacardMapper.class);
    when(mockMetacardMapper.getFeatureType()).thenReturn(mockFeatureType);
    when(mockMetacardMapper.getSortByTemporalFeatureProperty())
        .thenReturn(mockTemporalFeatureProperty);
    List<MetacardMapper> mappers = new ArrayList<>(1);
    mappers.add(mockMetacardMapper);
    source.setMetacardToFeatureMapper(mappers);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    query.setSortBy(sortBy);

    // Perform Test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Verify
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();
    JAXBElement<?> abstractSortingClause = queryType.getAbstractSortingClause();
    SortByType sortByType = (SortByType) abstractSortingClause.getValue();
    assertThat(
        sortByType.getSortProperty().get(0).getValueReference(), is(mockTemporalFeatureProperty));
    assertThat(
        sortByType.getSortProperty().get(0).getSortOrder().name(), is(SortOrderType.DESC.value()));
  }

  @Test
  public void testSortingNoSortBySortingSupported() throws Exception {
    // Setup
    final String searchPhrase = "*";
    final int pageSize = 1;

    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false, false, 0);

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase));
    query.setPageSize(pageSize);

    // Perform Test
    GetFeatureType featureType = source.buildGetFeatureRequest(query);

    // Verify
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();
    assertFalse(queryType.isSetAbstractSortingClause());
  }

  @Test
  public void testTimeoutConfiguration() throws WfsException {
    WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false, false, 0);

    source.setConnectionTimeout(10000);
    source.setReceiveTimeout(10000);

    // Perform test
    assertEquals(source.getConnectionTimeout().intValue(), 10000);
    assertEquals(source.getReceiveTimeout().intValue(), 10000);
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenUsernameAndPasswordAreConfigured()
      throws WfsException {
    final WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false);

    final String wfsUrl = "http://localhost/wfs";
    final String username = "test_user";
    final String password = "encrypted_password";
    final String authenticationType = "basic";
    final Boolean disableCnCheck = false;
    final Integer connectionTimeout = 10000;
    final Integer receiveTimeout = 20000;

    source.setPollInterval(1);

    doReturn("unencrypted_password").when(encryptionService).decryptValue(password);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", wfsUrl)
            .put("username", username)
            .put("password", password)
            .put("authenticationType", authenticationType)
            .put("disableCnCheck", disableCnCheck)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("forceSpatialFilter", "NO_FILTER")
            .build();
    source.refresh(configuration);

    verify(mockClientFactory)
        .getSecureCxfClientFactory(
            eq(wfsUrl),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            eq(disableCnCheck),
            eq(false),
            eq(connectionTimeout),
            eq(receiveTimeout),
            eq(username),
            eq("unencrypted_password"));
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenCertAliasAndKeystorePathAreConfigured()
      throws WfsException {
    final WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false);

    final String wfsUrl = "http://localhost/wfs";
    final Boolean disableCnCheck = false;
    final Integer connectionTimeout = 10000;
    final Integer receiveTimeout = 20000;
    final String certAlias = "mycert";
    final String keystorePath = "/path/to/keystore";
    final String sslProtocol = "TLSv1.2";

    source.setCertAlias(certAlias);
    source.setKeystorePath(keystorePath);
    source.setSslProtocol(sslProtocol);
    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", wfsUrl)
            .put("disableCnCheck", disableCnCheck)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("forceSpatialFilter", "NO_FILTER")
            .build();
    source.refresh(configuration);

    verify(mockClientFactory)
        .getSecureCxfClientFactory(
            eq(wfsUrl),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            eq(disableCnCheck),
            eq(false),
            eq(connectionTimeout),
            eq(receiveTimeout),
            eq(certAlias),
            eq(keystorePath),
            eq(sslProtocol));
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenNoAuthIsConfigured() throws WfsException {
    final WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false);

    final String wfsUrl = "http://localhost/wfs";
    final Boolean disableCnCheck = false;
    final Integer connectionTimeout = 10000;
    final Integer receiveTimeout = 20000;

    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", wfsUrl)
            .put("disableCnCheck", disableCnCheck)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("forceSpatialFilter", "NO_FILTER")
            .build();
    source.refresh(configuration);

    verify(mockClientFactory)
        .getSecureCxfClientFactory(
            eq(wfsUrl),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            eq(disableCnCheck),
            eq(false),
            eq(connectionTimeout),
            eq(receiveTimeout));
  }

  @Test
  public void testSearchByType() throws Exception {
    // Setup
    int pageSize = 10;
    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);
    Filter filter =
        builder.attribute(Metacard.CONTENT_TYPE).is().equalTo().text(SAMPLE_FEATURE_NAME + "0");
    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(pageSize);

    // Execute
    GetFeatureType featureType = source.buildGetFeatureRequest(query);
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();

    // Validate
    assertEquals(SAMPLE_FEATURE_NAME + "0", queryType.getTypeNames().get(0));
  }

  @Test
  public void testSearchByMultipleTypes() throws Exception {
    // Setup
    int pageSize = 10;
    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);
    Filter filter0 =
        builder.attribute(Metacard.CONTENT_TYPE).is().equalTo().text(SAMPLE_FEATURE_NAME + "8");
    Filter filter1 =
        builder.attribute(Metacard.CONTENT_TYPE).is().equalTo().text(SAMPLE_FEATURE_NAME + "9");
    Filter filter2 = builder.attribute(Metacard.ANY_TEXT).is().like().text("*");

    Filter typeSearchFilters = builder.anyOf(filter0, filter1);

    QueryImpl query = new QueryImpl(builder.allOf(filter2, typeSearchFilters));
    query.setPageSize(pageSize);

    // Execute
    GetFeatureType featureType = source.buildGetFeatureRequest(query);
    int numTypes = featureType.getAbstractQueryExpression().size();

    // Validate
    assertEquals(2, numTypes);
  }

  @Test
  public void testSrsNameProvided() throws Exception {

    int pageSize = 10;
    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);
    source.setSrsName(GeospatialUtil.EPSG_4326);

    Filter filter =
        builder.attribute(Metacard.CONTENT_TYPE).is().equalTo().text(SAMPLE_FEATURE_NAME + "0");
    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(pageSize);

    // Execute
    GetFeatureType featureType = source.buildGetFeatureRequest(query);
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();

    assertThat(queryType.getSrsName(), is(GeospatialUtil.EPSG_4326));
  }

  @Test
  public void testSrsNameNotProvided() throws Exception {

    int pageSize = 10;
    WfsSource source =
        getWfsSource(ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 10, false);

    Filter filter =
        builder.attribute(Metacard.CONTENT_TYPE).is().equalTo().text(SAMPLE_FEATURE_NAME + "0");
    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(pageSize);

    // Execute
    GetFeatureType featureType = source.buildGetFeatureRequest(query);
    QueryType queryType = (QueryType) featureType.getAbstractQueryExpression().get(0).getValue();

    assertThat(queryType.getSrsName(), nullValue());
  }

  @Test
  public void testSourceUsesMetacardMapperToMapMetacardAttributesToFeatureProperties()
      throws Exception {
    final MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType("{http://example.com}SampleFeature0");
    metacardMapper.setResourceUriMapping("title");
    metacardMappers.add(metacardMapper);

    final WfsSource source =
        getWfsSource(
            ONE_TEXT_PROPERTY_SCHEMA, MockWfsServer.getFilterCapabilities(), 1, false, false, 1);

    final Filter filter = builder.attribute(Core.RESOURCE_URI).is().equalTo().text("anything");
    final Query query = new QueryImpl(filter);
    final QueryRequest queryRequest = new QueryRequestImpl(query);
    source.query(queryRequest);

    final ArgumentCaptor<GetFeatureType> getFeatureCaptor =
        ArgumentCaptor.forClass(GetFeatureType.class);
    verify(mockWfs).getFeature(getFeatureCaptor.capture());

    final GetFeatureType getFeatureType = getFeatureCaptor.getValue();
    final String getFeatureXml = marshal(getFeatureType);
    assertThat(
        getFeatureXml,
        hasXPath(
                "/wfs:GetFeature/wfs:Query/ogc:Filter/ogc:PropertyIsEqualTo[ogc:ValueReference='title' and ogc:Literal='anything']")
            .withNamespaceContext(NAMESPACE_CONTEXT));
  }

  private String marshal(final GetFeatureType getFeatureType) throws JAXBException {
    Writer writer = new StringWriter();
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.marshal(getGetFeatureTypeJaxbElement(getFeatureType), writer);
    return writer.toString();
  }

  private JAXBElement<GetFeatureType> getGetFeatureTypeJaxbElement(
      final GetFeatureType getFeatureType) {
    return new JAXBElement<>(
        new QName("http://www.opengis.net/wfs/2.0", "GetFeature"),
        GetFeatureType.class,
        getFeatureType);
  }
}
