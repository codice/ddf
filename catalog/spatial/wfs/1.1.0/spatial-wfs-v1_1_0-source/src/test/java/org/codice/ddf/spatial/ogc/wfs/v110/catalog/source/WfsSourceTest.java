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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import static org.codice.ddf.libs.geo.util.GeospatialUtil.LAT_LON_ORDER;
import static org.codice.ddf.libs.geo.util.GeospatialUtil.LON_LAT_ORDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import ddf.security.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import ddf.security.service.SecurityManager;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import net.opengis.filter.v_1_1_0.BinaryLogicOpType;
import net.opengis.filter.v_1_1_0.DistanceBufferType;
import net.opengis.filter.v_1_1_0.FeatureIdType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.LogicOpsType;
import net.opengis.filter.v_1_1_0.PropertyIsLikeType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.filter.v_1_1_0.SpatialOpsType;
import net.opengis.gml.v_3_1_1.PointType;
import net.opengis.wfs.v_1_1_0.FeatureTypeListType;
import net.opengis.wfs.v_1_1_0.FeatureTypeType;
import net.opengis.wfs.v_1_1_0.QueryType;
import net.opengis.wfs.v_1_1_0.ResultTypeType;
import net.opengis.wfs.v_1_1_0.WFSCapabilitiesType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.cxf.client.ClientBuilder;
import org.codice.ddf.cxf.client.ClientBuilderFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.cxf.client.impl.ClientBuilderImpl;
import org.codice.ddf.cxf.oauth.OAuthSecurity;
import org.codice.ddf.security.jaxrs.SamlSecurity;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollectionImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl.MetacardMapperImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsUriResolver;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;

public class WfsSourceTest {

  private static final String SAMPLE_WFS_URL = "http://www.someserver.com/wfs/cwwfs.cgi";

  private static final Map<String, String> NAMESPACE_CONTEXT =
      ImmutableMap.of("wfs", "http://www.opengis.net/wfs", "ogc", "http://www.opengis.net/ogc");

  private static final String ONE_TEXT_PROPERTY_SCHEMA_PERSON =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"orderperson\" type=\"xs:string\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "</xs:schema>";

  private static final String ONE_TEXT_PROPERTY_SCHEMA_DOG =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"orderdog\" type=\"xs:string\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "</xs:schema>";

  private static final String TWO_TEXT_PROPERTY_SCHEMA =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"orderperson\" type=\"xs:string\"/>"
          + "<xs:element name=\"orderdog\" type=\"xs:string\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "</xs:schema>";

  private static final String NO_PROPERTY_SCHEMA =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "</xs:schema>";

  private static final String ONE_GML_PROPERTY_SCHEMA =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
          + "xmlns:gml=\"http://www.opengis.net/gml\" "
          + "targetNamespace=\"http://www.opengis.net/gml\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"orderperson\" type=\"gml:FakeGmlProperty\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "<xs:complexType name=\"FakeGmlProperty\" />"
          + "</xs:schema>";

  private static final String TWO_GML_PROPERTY_SCHEMA =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
          + "xmlns:gml=\"http://www.opengis.net/gml\" "
          + "targetNamespace=\"http://www.opengis.net/gml\">"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"orderperson\" type=\"gml:FakeGmlProperty\"/>"
          + "<xs:element name=\"orderdog\" type=\"gml:FakeGmlProperty\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "<xs:complexType name=\"FakeGmlProperty\" />"
          + "</xs:schema>";

  private static final String GML_IMPORT_SCHEMA =
      "<?xml version=\"1.0\"?>"
          + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
          + "xmlns:gml=\"http://www.opengis.net/gml\" "
          + "targetNamespace=\"http://www.opengis.net/gml\">"
          + "<xs:import namespace=\"http://www.opengis.net/gml\" schemaLocation=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\"/>"
          + "<xs:element name=\"shiporder\">"
          + "<xs:complexType>"
          + "<xs:sequence>"
          + "<xs:element name=\"orderperson\" type=\"gml:FakeGmlProperty\"/>"
          + "</xs:sequence>"
          + "</xs:complexType>"
          + "</xs:element>"
          + "<xs:complexType name=\"FakeGmlProperty\" />"
          + "</xs:schema>";

  private static final String POLYGON_WKT = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";

  private static final String POINT_WKT = "POINT (30 -10)";

  private static final String ORDER_PERSON = "orderperson";

  private static final String ORDER_DOG = "orderdog";

  private static final int MAX_FEATURES = 10;

  private static final int ONE_FEATURE = 1;

  private static final int TWO_FEATURES = 2;

  private static final String SRS_NAME = "EPSG:4326";

  private static final String LITERAL = "literal";

  private static final String MOCK_TEMPORAL_SORT_PROPERTY = "myTemporalSortProperty";
  private static final String MOCK_RELEVANCE_SORT_PROPERTY = "myRelevanceSortProperty";
  private static final String MOCK_DISTANCE_SORT_PROPERTY = "myDistanceSortProperty";

  private static final String WFS_ID = "WFS_ID";

  private static final Comparator<QueryType> QUERY_TYPE_COMPARATOR =
      (queryType1, queryType2) -> {
        String typeName1 = queryType1.getTypeName().get(0).getLocalPart();
        String typeName2 = queryType2.getTypeName().get(0).getLocalPart();
        return typeName1.compareTo(typeName2);
      };

  private static JAXBContext jaxbContext;

  private final GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();

  private ExtendedWfs mockWfs = mock(ExtendedWfs.class);

  private WFSCapabilitiesType mockCapabilities = new WFSCapabilitiesType();

  private BundleContext mockContext = mock(BundleContext.class);

  private List<QName> sampleFeatures;

  private WfsUriResolver wfsUriResolver = new WfsUriResolver();

  private WfsSource source;

  private ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);

  private EncryptionService encryptionService = mock(EncryptionService.class);

  private WfsMetacardTypeRegistry mockWfsMetacardTypeRegistry = mock(WfsMetacardTypeRegistry.class);

  private ClientBuilderFactory clientBuilderFactory = mock(ClientBuilderFactory.class);

  private List<MetacardMapper> metacardMappers = new ArrayList<>();

  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @BeforeClass
  public static void setupClass() {
    try {
      jaxbContext = JAXBContext.newInstance("net.opengis.wfs.v_1_1_0");
    } catch (JAXBException e) {
      fail(e.getMessage());
    }
  }

  private void setUpMocks(
      final List<String> supportedGeos, final String srsName, final int results, final int hits)
      throws WfsException {

    SecureCxfClientFactory<ExtendedWfs> mockFactory = mock(SecureCxfClientFactory.class);
    when(mockFactory.getClient()).thenReturn(mockWfs);

    clientBuilderFactory = mock(ClientBuilderFactory.class);
    ClientBuilder<ExtendedWfs> clientBuilder =
        new ClientBuilderImpl<ExtendedWfs>(
            mock(OAuthSecurity.class),
            mock(SamlSecurity.class),
            mock(SecurityLogger.class),
            mock(SecurityManager.class)) {
          @Override
          public SecureCxfClientFactory<ExtendedWfs> build() {
            return mockFactory;
          }
        };

    when(clientBuilderFactory.<ExtendedWfs>getClientBuilder()).thenReturn(clientBuilder);

    // GetCapabilities Response
    when(mockWfs.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(mockCapabilities);
    mockCapabilities.setFilterCapabilities(new FilterCapabilities());
    mockCapabilities.getFilterCapabilities().setSpatialCapabilities(new SpatialCapabilitiesType());
    mockCapabilities
        .getFilterCapabilities()
        .getSpatialCapabilities()
        .setSpatialOperators(new SpatialOperatorsType());
    if (CollectionUtils.isNotEmpty(supportedGeos)) {
      mockCapabilities
          .getFilterCapabilities()
          .getSpatialCapabilities()
          .getSpatialOperators()
          .getSpatialOperator()
          .addAll(
              supportedGeos.stream()
                  .map(
                      opName -> {
                        SpatialOperatorType spatialOperatorType = new SpatialOperatorType();
                        spatialOperatorType.setName(SpatialOperatorNameType.fromValue(opName));
                        return spatialOperatorType;
                      })
                  .collect(Collectors.toList()));
    }

    sampleFeatures = new ArrayList<>();
    mockCapabilities.setFeatureTypeList(new FeatureTypeListType());

    for (int ii = 0; ii < results; ii++) {
      FeatureTypeType feature = new FeatureTypeType();
      QName qName;
      if (ii == 0) {
        qName = new QName("SampleFeature" + ii);
      } else {
        qName = new QName("http://example.com", "SampleFeature" + ii, "Prefix" + ii);
      }
      sampleFeatures.add(qName);
      feature.setName(qName);
      if (null != srsName) {
        feature.setDefaultSRS(srsName);
      }
      mockCapabilities.getFeatureTypeList().getFeatureType().add(feature);
    }

    List<Metacard> metacards = new ArrayList<>(results);
    for (int i = 0; i < results; i++) {
      MetacardImpl mc = new MetacardImpl();
      mc.setId("ID_" + (i + 1));
      metacards.add(mc);
    }
    when(mockWfs.getFeature(withResultType(ResultTypeType.HITS)))
        .thenReturn(new WfsFeatureCollectionImpl(hits));
    when(mockWfs.getFeature(withResultType(ResultTypeType.RESULTS)))
        .thenReturn(new WfsFeatureCollectionImpl(results, metacards));

    final ScheduledFuture<?> mockAvailabilityPollFuture = mock(ScheduledFuture.class);
    doReturn(mockAvailabilityPollFuture)
        .when(mockScheduler)
        .scheduleWithFixedDelay(any(), anyInt(), anyInt(), any());

    source = new WfsSource(clientBuilderFactory, encryptionService, mockScheduler);
    source.setId(WFS_ID);
    source.setFilterAdapter(new GeotoolsFilterAdapterImpl());
    source.setContext(mockContext);
    source.setWfsMetacardTypeRegistry(mockWfsMetacardTypeRegistry);
    source.setMetacardTypeEnhancers(Collections.emptyList());
    source.setMetacardMappers(metacardMappers);
    source.setPollInterval(10);
    source.setWfsUrl(SAMPLE_WFS_URL);
    source.setSupportsStartIndex(false);
    source.init();
  }

  @Test
  public void testAvailability() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    assertThat(source.isAvailable(), is(true));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testQueryEmptyQueryList() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testPropertyIsLikeQuery() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    assertThat(query.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
  }

  @Test
  public void testTwoPropertyQuery() throws Exception {
    mapSchemaToFeatures(TWO_TEXT_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    // The Text Properties should be ORed
    assertThat(query.getFilter().isSetLogicOps(), is(true));
    assertThat(query.getFilter().getLogicOps().getValue(), is(instanceOf(BinaryLogicOpType.class)));
  }

  @Test
  public void testContentTypeQuery() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    Filter propertyIsLikeFilter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);
    Filter contentTypeFilter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(0).getLocalPart()); // .text(SAMPLE_FEATURE[0].getLocalPart());
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.allOf(propertyIsLikeFilter, contentTypeFilter));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0))); // SAMPLE_FEATURE[0]));
    assertThat(query.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
  }

  @Test
  public void testContentTypeAndNoPropertyQuery() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);

    Filter contentTypeFilter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(0).getLocalPart());
    QueryImpl propertyIsLikeQuery = new QueryImpl(contentTypeFilter);

    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertThat(ONE_FEATURE, is(getFeatureType.getQuery().size()));
    assertThat(sampleFeatures.get(0), is(getFeatureType.getQuery().get(0).getTypeName().get(0)));
  }

  @Test
  public void testTwoContentTypeAndNoPropertyQuery() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, TWO_FEATURES);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);

    Filter contentTypeFilter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(0).getLocalPart());
    Filter contentTypeFilter2 =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(1).getLocalPart());
    QueryImpl twoContentTypeQuery =
        new QueryImpl(builder.anyOf(Arrays.asList(contentTypeFilter, contentTypeFilter2)));

    twoContentTypeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(twoContentTypeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, twoContentTypeQuery);
    getFeatureType.getQuery().sort(QUERY_TYPE_COMPARATOR);
    assertThat(TWO_FEATURES, is(getFeatureType.getQuery().size()));
    assertThat(sampleFeatures.get(0), is(getFeatureType.getQuery().get(0).getTypeName().get(0)));
  }

  @Test
  public void testAndQuery() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    Filter propertyIsLikeFilter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);
    Filter contentTypeFilter =
        builder.attribute(Metacard.ANY_TEXT).is().like().text(sampleFeatures.get(0).getLocalPart());
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.allOf(propertyIsLikeFilter, contentTypeFilter));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    assertThat(query.getFilter().isSetLogicOps(), is(true));
    assertThat(query.getFilter().getLogicOps().getValue(), is(instanceOf(BinaryLogicOpType.class)));
  }

  @Test
  public void testIntersectQuery() throws Exception {
    mapSchemaToFeatures(ONE_GML_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(Arrays.asList("Intersects", "BBOX"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    assertThat(query.getFilter().isSetSpatialOps(), is(true));
    assertThat(query.getFilter().getSpatialOps().getValue(), is(instanceOf(SpatialOpsType.class)));
  }

  @Test
  public void testTwoIntersectQuery() throws Exception {
    mapSchemaToFeatures(TWO_GML_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(Arrays.asList("Intersects", "BBOX"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    // The Text Properties should be ORed
    assertThat(query.getFilter(), notNullValue());
    assertThat(query.getFilter().isSetLogicOps(), is(true));
    assertThat(query.getFilter().getLogicOps().getValue(), is(instanceOf(LogicOpsType.class)));
  }

  @Test
  public void testBboxQuery() throws Exception {
    mapSchemaToFeatures(ONE_GML_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(Collections.singletonList("BBOX"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    assertThat(query.getFilter().isSetSpatialOps(), is(true));
    assertThat(query.getFilter().getSpatialOps().getValue(), is(instanceOf(SpatialOpsType.class)));
  }

  @Test
  public void testGmlImport() throws Exception {
    mapSchemaToFeatures(GML_IMPORT_SCHEMA, ONE_FEATURE);
    setUpMocks(Collections.singletonList("BBOX"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    assertThat(query.getFilter().isSetSpatialOps(), is(true));
    assertThat(query.getFilter().getSpatialOps().getValue(), is(instanceOf(SpatialOpsType.class)));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testNoGeoAttributesQuery() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    source.query(new QueryRequestImpl(intersectQuery));
  }

  @Test
  public void testTwoFeatureTypesQuery() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, TWO_FEATURES);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertThat(getFeatureType.getQuery().size(), is(TWO_FEATURES));
    Collections.sort(getFeatureType.getQuery(), QUERY_TYPE_COMPARATOR);
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    assertThat(query.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
    QueryType query2 = getFeatureType.getQuery().get(1);
    assertThat(query2.getTypeName().get(0), is(sampleFeatures.get(1)));
    assertThat(query2.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query2.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=1, should get 4 results back - metacards 1 thru 4.
   */
  @Test
  public void testPagingStartIndexOne() throws Exception {
    int pageSize = 4;
    int startIndex = 1;
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(response.getHits(), equalTo((long) MAX_FEATURES));

    // Verify that metacards 1 thru 4 were returned since pageSize=4
    assertCorrectMetacardsReturned(results, startIndex, pageSize);
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=2, should get 4 results back - metacards 2 thru 5.
   */
  @Test
  public void testPagingStartIndexTwo() throws Exception {
    int pageSize = 4;
    int startIndex = 2;
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(pageSize));
    assertThat(response.getHits(), equalTo((long) MAX_FEATURES));

    // Verify that metacards 2 thru 5 were returned since pageSize=4
    assertCorrectMetacardsReturned(results, startIndex, pageSize);
  }

  /**
   * Given 2 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=3, should get 0 results back and total hits of 2.
   */
  @Test
  public void testPagingStartIndexGreaterThanNumberOfFeatures() throws Exception {
    int pageSize = 4;
    int startIndex = 3;
    int numFeatures = 2;
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, numFeatures);
    setUpMocks(null, null, numFeatures, numFeatures);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(0));
    assertThat(response.getHits(), is((long) numFeatures));
  }

  // Simulates query by ID (which is analogous to clicking on link in search
  // results to view associated metacard in XML)
  @Test
  public void testPaging() throws Exception {
    int pageSize = 4;
    int startIndex = 1;
    int numFeatures = 1;
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, numFeatures);
    setUpMocks(null, null, numFeatures, numFeatures);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(1));
    assertThat(response.getHits(), is((long) numFeatures));
  }

  /** Since page size=4 and startIndex=5, should get 4 results back and total hits of 10. */
  @Test
  public void testPagingToSecondPage() throws Exception {
    int pageSize = 4;
    int startIndex = 5;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(response.getHits(), equalTo((long) MAX_FEATURES));

    // Verify that metacards 5 thru 8 were returned
    assertCorrectMetacardsReturned(results, startIndex, 4);
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=20 (which
   * is larger than number of features) and startIndex=1, should get 10 results back - metacards 1
   * thru 10.
   */
  @Test
  public void testPagingPageSizeExceedsFeatureCountStartIndexOne() throws Exception {
    int pageSize = 20;
    int startIndex = 1;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(MAX_FEATURES));
    assertThat(response.getHits(), equalTo((long) MAX_FEATURES));

    // Verify that metacards 1 thru 10 were returned
    assertCorrectMetacardsReturned(results, startIndex, MAX_FEATURES);
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=20 (which
   * is larger than number of features) and startIndex=2, should get 9 results back - metacards 2
   * thru 10.
   */
  @Test
  public void testPagingPageSizeExceedsFeatureCountStartIndexTwo() throws Exception {
    int pageSize = 20;
    int startIndex = 2;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(MAX_FEATURES - 1));
    assertThat(response.getHits(), equalTo((long) MAX_FEATURES));

    // Verify that metacards 2 thru 10 were returned
    assertCorrectMetacardsReturned(results, startIndex, MAX_FEATURES - 1);
  }

  /**
   * Verify that, per DDF Query API Javadoc, if the startIndex is negative, the WfsSource throws an
   * UnsupportedQueryException.
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testPagingStartIndexNegative() throws Exception {
    int pageSize = 4;
    int startIndex = -1;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    executeQuery(startIndex, pageSize);
  }

  /**
   * Verify that, per DDF Query API Javadoc, if the startIndex is zero, the WfsSource throws an
   * UnsupportedQueryException.
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testPagingStartIndexZero() throws Exception {
    int pageSize = 4;
    int startIndex = 0;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, MAX_FEATURES, MAX_FEATURES);

    executeQuery(startIndex, pageSize);
  }

  /**
   * Verify that if page size is negative, WfsSource defaults it to the max features that can be
   * returned.
   */
  @Test
  public void testPagingPageSizeNegative() throws Exception {
    int pageSize = -1;
    int startIndex = 1;
    int numResults = WfsSource.WFS_MAX_FEATURES_RETURNED + 10;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, WfsSource.WFS_MAX_FEATURES_RETURNED, numResults);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(WfsSource.WFS_MAX_FEATURES_RETURNED));
    assertThat(response.getHits(), is((long) numResults));
  }

  /**
   * Verify that if page size is zero, WfsSource defaults it to the max features that can be
   * returned.
   */
  @Test
  public void testPagingPageSizeZero() throws Exception {
    int pageSize = 0;
    int startIndex = 1;
    int numResults = WfsSource.WFS_MAX_FEATURES_RETURNED + 10;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, WfsSource.WFS_MAX_FEATURES_RETURNED, numResults);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(WfsSource.WFS_MAX_FEATURES_RETURNED));
    assertThat(response.getHits(), is((long) numResults));
  }

  /**
   * Given 1010 features (and metacards) exist that match search criteria, since page size=1001
   * (which is larger than max number of features the WfsSource allows to be returned) and
   * startIndex=1, should get 1000 results back, but a total hits of 1010.
   */
  @Test
  public void testPagingPageSizeExceedsMaxFeaturesThatCanBeReturned() throws Exception {
    int pageSize = WfsSource.WFS_MAX_FEATURES_RETURNED + 1;
    int startIndex = 1;
    int numResults = WfsSource.WFS_MAX_FEATURES_RETURNED + 10;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, pageSize, numResults);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(WfsSource.WFS_MAX_FEATURES_RETURNED));
    assertThat(response.getHits(), is((long) numResults));
  }

  @Test
  public void testPagingToSecondPageWithStartIndex() throws Exception {
    int pageSize = 4;
    int startIndex = 5;

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, MAX_FEATURES);
    setUpMocks(null, null, pageSize, MAX_FEATURES);

    List<Metacard> metacards = new ArrayList<>(pageSize);
    for (int i = startIndex; i < startIndex + pageSize; i++) {
      MetacardImpl mc = new MetacardImpl();
      mc.setId("ID_" + i);
      metacards.add(mc);
    }

    when(mockWfs.getFeature(withResultType(ResultTypeType.HITS)))
        .thenReturn(new WfsFeatureCollectionImpl(MAX_FEATURES));
    when(mockWfs.getFeature(hasStartIndex()))
        .thenReturn(new WfsFeatureCollectionImpl(pageSize, metacards));

    source.setSupportsStartIndex(true);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(response.getHits(), equalTo((long) MAX_FEATURES));

    // Verify that metacards 5 thru 8 were returned
    assertCorrectMetacardsReturned(results, startIndex, pageSize);
  }

  @Test
  public void testGetContentTypes() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, TWO_FEATURES);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);
    Set<ContentType> contentTypes = source.getContentTypes();
    assertThat(contentTypes.size(), is(TWO_FEATURES));
    for (ContentType contentType : contentTypes) {
      assertThat(
          sampleFeatures.get(0).getLocalPart().equals(contentType.getName())
              || sampleFeatures.get(1).getLocalPart().equals(contentType.getName()),
          is(true));
    }
  }

  @Test
  public void testQueryTwoFeaturesOneInvalid() throws Exception {
    mapSchemaToSingleFeature(TWO_TEXT_PROPERTY_SCHEMA, 0);
    mapSchemaToSingleFeature(ONE_TEXT_PROPERTY_SCHEMA_PERSON, 1);

    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);
    Filter orderDogFilter = builder.attribute(ORDER_DOG).is().like().text(LITERAL);
    Filter mctFeature1Filter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(0).getLocalPart());
    Filter feature1Filter = builder.allOf(Arrays.asList(orderDogFilter, mctFeature1Filter));
    Filter fakeFilter = builder.attribute("FAKE").is().like().text(LITERAL);
    Filter mctFeature2Filter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(1).getLocalPart());
    Filter feature2Filter = builder.allOf(Arrays.asList(fakeFilter, mctFeature2Filter));
    Filter totalFilter = builder.anyOf(Arrays.asList(feature1Filter, feature2Filter));

    QueryImpl inQuery = new QueryImpl(totalFilter);
    inQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(inQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, inQuery);

    List<QueryType> filterQueries =
        getFeatureType.getQuery().stream()
            .filter(QueryType::isSetFilter)
            .collect(Collectors.toList());
    assertThat(filterQueries, hasSize(ONE_FEATURE));

    QueryType query = filterQueries.get(0);
    assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
    // The Text Properties should be ORed
    assertThat(query.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
    PropertyIsLikeType pilt = (PropertyIsLikeType) query.getFilter().getComparisonOps().getValue();
    assertThat(ORDER_DOG, is(pilt.getPropertyName().getContent().get(0)));
  }

  @Test
  public void testQueryTwoFeaturesWithMixedPropertyNames() throws Exception {
    mapSchemaToSingleFeature(ONE_TEXT_PROPERTY_SCHEMA_PERSON, 0);
    mapSchemaToSingleFeature(ONE_TEXT_PROPERTY_SCHEMA_DOG, 1);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);
    Filter orderPersonFilter = builder.attribute(ORDER_PERSON).is().like().text(LITERAL);
    Filter mctFeature1Filter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(0).getLocalPart());
    Filter feature1Filter = builder.allOf(Arrays.asList(orderPersonFilter, mctFeature1Filter));
    Filter orderDogFilter = builder.attribute(ORDER_DOG).is().like().text(LITERAL);
    Filter mctFeature2Filter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(1).getLocalPart());
    Filter feature2Filter = builder.allOf(Arrays.asList(orderDogFilter, mctFeature2Filter));
    Filter totalFilter = builder.anyOf(Arrays.asList(feature1Filter, feature2Filter));

    QueryImpl inQuery = new QueryImpl(totalFilter);
    inQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(inQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, inQuery);
    getFeatureType.getQuery().sort(QUERY_TYPE_COMPARATOR);
    assertThat(TWO_FEATURES, is(getFeatureType.getQuery().size()));
    // Feature 1
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName().get(0), equalTo(sampleFeatures.get(0)));
    // this should only have 1 filter which is a comparison
    assertThat(query.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
    PropertyIsLikeType pilt = (PropertyIsLikeType) query.getFilter().getComparisonOps().getValue();
    assertThat(pilt, notNullValue());
    assertThat(ORDER_PERSON, is(pilt.getPropertyName().getContent().get(0)));
    // Feature 2
    QueryType query2 = getFeatureType.getQuery().get(1);
    assertThat(query2.getTypeName().get(0), is(sampleFeatures.get(1)));
    // this should only have 1 filter which is a comparison
    assertThat(query2.getFilter().isSetComparisonOps(), is(true));
    assertThat(
        query2.getFilter().getComparisonOps().getValue(), is(instanceOf(PropertyIsLikeType.class)));
    PropertyIsLikeType pilt2 =
        (PropertyIsLikeType) query2.getFilter().getComparisonOps().getValue();
    assertThat(ORDER_DOG, is(pilt2.getPropertyName().getContent().get(0)));
  }

  @Test
  public void testIDQuery() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, TWO_FEATURES);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);

    QueryImpl idQuery = new QueryImpl(builder.attribute(Core.ID).is().text(ORDER_PERSON));

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(idQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertThat(ONE_FEATURE, is(getFeatureType.getQuery().get(0).getFilter().getId().size()));

    assertThat(
        ORDER_PERSON,
        is(
            ((FeatureIdType) getFeatureType.getQuery().get(0).getFilter().getId().get(0).getValue())
                .getFid()));
  }

  @Test
  public void testTwoIDQuery() throws Exception {
    mapSchemaToFeatures(NO_PROPERTY_SCHEMA, TWO_FEATURES);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);

    Filter idFilter1 = builder.attribute(Core.ID).is().text(ORDER_PERSON);
    Filter idFilter2 = builder.attribute(Core.ID).is().text(ORDER_DOG);

    QueryImpl twoIDQuery = new QueryImpl(builder.anyOf(Arrays.asList(idFilter1, idFilter2)));

    ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(twoIDQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertThat(TWO_FEATURES, is(getFeatureType.getQuery().get(0).getFilter().getId().size()));

    assertThat(
        ORDER_PERSON.equals(
                ((FeatureIdType)
                        getFeatureType.getQuery().get(0).getFilter().getId().get(0).getValue())
                    .getFid())
            || ORDER_PERSON.equals(
                ((FeatureIdType)
                        getFeatureType.getQuery().get(0).getFilter().getId().get(1).getValue())
                    .getFid()),
        is(true));

    assertThat(
        ORDER_DOG.equals(
                ((FeatureIdType)
                        getFeatureType.getQuery().get(0).getFilter().getId().get(0).getValue())
                    .getFid())
            || ORDER_DOG.equals(
                ((FeatureIdType)
                        getFeatureType.getQuery().get(0).getFilter().getId().get(1).getValue())
                    .getFid()),
        is(true));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testOneIDOnePropertyQuery() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, TWO_FEATURES);
    setUpMocks(null, null, TWO_FEATURES, TWO_FEATURES);

    Filter idFilter = builder.attribute(Core.ID).is().text(ORDER_PERSON);
    Filter propertyIsLikeFilter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);

    QueryImpl query = new QueryImpl(builder.anyOf(Arrays.asList(propertyIsLikeFilter, idFilter)));

    // we are verifying that mixing featureID filters with other filters is not supported
    source.query(new QueryRequestImpl(query));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testNoFeatures() throws Exception {
    setUpMocks(null, null, 0, TWO_FEATURES);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testSourceUsesMetacardMapperToMapMetacardAttributesToFeatureProperties()
      throws Exception {
    final MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType("SampleFeature0");
    metacardMapper.setTitleMapping("orderperson");
    metacardMapper.setResourceUriMapping("orderdog");
    metacardMappers.add(metacardMapper);

    mapSchemaToFeatures(TWO_TEXT_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);

    final Filter filter =
        builder.allOf(
            builder.attribute(Core.TITLE).is().equalTo().text("something"),
            builder.attribute(Core.RESOURCE_URI).is().equalTo().text("anything"));
    final Query query = new QueryImpl(filter);
    final QueryRequest queryRequest = new QueryRequestImpl(query);
    source.query(queryRequest);

    final ArgumentCaptor<ExtendedGetFeatureType> getFeatureCaptor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    verify(mockWfs, times(2)).getFeature(getFeatureCaptor.capture());

    ExtendedGetFeatureType getFeatureType = getFeatureCaptor.getAllValues().get(1);
    final String getFeatureXml = marshal(getFeatureType);
    assertThat(
        getFeatureXml,
        hasXPath(
                "/wfs:GetFeature/wfs:Query/ogc:Filter/ogc:And/ogc:PropertyIsEqualTo[ogc:PropertyName='orderperson' and ogc:Literal='something']")
            .withNamespaceContext(NAMESPACE_CONTEXT));
    assertThat(
        getFeatureXml,
        hasXPath(
                "/wfs:GetFeature/wfs:Query/ogc:Filter/ogc:And/ogc:PropertyIsEqualTo[ogc:PropertyName='orderdog' and ogc:Literal='anything']")
            .withNamespaceContext(NAMESPACE_CONTEXT));
  }

  @Test
  public void testTimeoutConfiguration() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);

    source.setConnectionTimeout(10000);
    source.setReceiveTimeout(10000);

    assertThat(source.getConnectionTimeout(), is(10000));
    assertThat(source.getReceiveTimeout(), is(10000));
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenUsernameAndPasswordAreConfigured()
      throws WfsException {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);

    final String wfsUrl = "http://localhost/wfs";
    final String username = "test_user";
    final String password = "encrypted_password";
    final String authenticationType = "basic";
    final Boolean disableCnCheck = false;
    final Boolean allowRedirects = true;
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
            .put("allowRedirects", allowRedirects)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("supportsStartIndex", false)
            .build();
    source.refresh(configuration);

    verify(clientBuilderFactory, times(2)).getClientBuilder();
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenCertAliasAndKeystorePathAreConfigured()
      throws WfsException {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);

    final String wfsUrl = "http://localhost/wfs";
    final Boolean disableCnCheck = false;
    final Boolean allowRedirects = true;
    final Integer connectionTimeout = 10000;
    final Integer receiveTimeout = 20000;
    final String certAlias = "mycert";
    final String keystorePath = "/path/to/keystore";
    final String sslProtocol = "TLSv1.2";

    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", wfsUrl)
            .put("disableCnCheck", disableCnCheck)
            .put("allowRedirects", allowRedirects)
            .put("certAlias", certAlias)
            .put("keystorePath", keystorePath)
            .put("sslProtocol", sslProtocol)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("supportsStartIndex", false)
            .build();
    source.refresh(configuration);

    verify(clientBuilderFactory, times(2)).getClientBuilder();
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenNoAuthIsConfigured() throws WfsException {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);

    final String wfsUrl = "http://localhost/wfs";
    final Boolean disableCnCheck = false;
    final Boolean allowRedirects = true;
    final Integer connectionTimeout = 10000;
    final Integer receiveTimeout = 20000;

    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", wfsUrl)
            .put("disableCnCheck", disableCnCheck)
            .put("allowRedirects", allowRedirects)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("supportsStartIndex", false)
            .build();
    source.refresh(configuration);

    verify(clientBuilderFactory, times(2)).getClientBuilder();
  }

  @Test
  public void testQueryLatLonCoordinateOrder() throws Exception {
    mapSchemaToFeatures(ONE_GML_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(Collections.singletonList("DWithin"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);

    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", "http://localhost/wfs")
            .put("coordinateOrder", LAT_LON_ORDER)
            .put("forceSpatialFilter", "NO_FILTER")
            .put("allowRedirects", false)
            .put("disableCnCheck", false)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("supportsStartIndex", false)
            .build();
    source.refresh(configuration);

    final Filter withinFilter =
        builder.attribute(Metacard.ANY_GEO).is().withinBuffer().wkt(POINT_WKT, 10.0);
    final Query withinQuery = new QueryImpl(withinFilter);

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(withinQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertThat(getFeatureType.getQuery(), hasSize(1));

    final QueryType query = getFeatureType.getQuery().get(0);
    assertThat(
        query.getFilter().getSpatialOps().getValue(), is(instanceOf(DistanceBufferType.class)));

    final DistanceBufferType distanceBufferType =
        (DistanceBufferType) query.getFilter().getSpatialOps().getValue();
    assertThat(distanceBufferType.getGeometry().getValue(), is(instanceOf(PointType.class)));

    final PointType pointType = (PointType) distanceBufferType.getGeometry().getValue();
    assertThat(pointType.getCoordinates().getValue(), is("-10.0,30.0"));
  }

  @Test
  public void testQueryLonLatCoordinateOrder() throws Exception {
    mapSchemaToFeatures(ONE_GML_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(Collections.singletonList("DWithin"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);

    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", "http://localhost/wfs")
            .put("coordinateOrder", LON_LAT_ORDER)
            .put("forceSpatialFilter", "NO_FILTER")
            .put("allowRedirects", false)
            .put("disableCnCheck", false)
            .put("pollInterval", 1)
            .put("disableSorting", false)
            .put("supportsStartIndex", false)
            .build();
    source.refresh(configuration);

    final Filter withinFilter =
        builder.attribute(Metacard.ANY_GEO).is().withinBuffer().wkt(POINT_WKT, 10.0);
    final Query withinQuery = new QueryImpl(withinFilter);

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(withinQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    ExtendedGetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertThat(getFeatureType.getQuery(), hasSize(1));

    final QueryType query = getFeatureType.getQuery().get(0);
    assertThat(
        query.getFilter().getSpatialOps().getValue(), is(instanceOf(DistanceBufferType.class)));

    final DistanceBufferType distanceBufferType =
        (DistanceBufferType) query.getFilter().getSpatialOps().getValue();
    assertThat(distanceBufferType.getGeometry().getValue(), is(instanceOf(PointType.class)));

    final PointType pointType = (PointType) distanceBufferType.getGeometry().getValue();
    assertThat(pointType.getCoordinates().getValue(), is("30.0,-10.0"));
  }

  @Test
  public void testQuerySendsHitsRequestBeforeResultsRequest() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    final ExtendedGetFeatureType getHits = captor.getAllValues().get(0);
    assertThat(getHits.getResultType(), is(ResultTypeType.HITS));
    assertThat(getHits.getMaxFeatures(), is(nullValue()));

    final ExtendedGetFeatureType getResults = captor.getAllValues().get(1);
    assertThat(getResults.getResultType(), is(ResultTypeType.RESULTS));
    assertMaxFeatures(getResults, propertyIsLikeQuery);

    for (final ExtendedGetFeatureType getFeatureType : captor.getAllValues()) {
      assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
      final QueryType query = getFeatureType.getQuery().get(0);
      assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
      assertThat(query.getFilter().isSetComparisonOps(), is(true));
      assertThat(
          query.getFilter().getComparisonOps().getValue(),
          is(instanceOf(PropertyIsLikeType.class)));
    }
  }

  @Test
  public void testSortingNoSortBy() throws Exception {
    // Setup
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    propertyIsLikeQuery.setPageSize(1);

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    final ExtendedGetFeatureType getResults = captor.getAllValues().get(1);
    assertThat(getResults.getResultType(), is(ResultTypeType.RESULTS));
    for (final ExtendedGetFeatureType getFeatureType : captor.getAllValues()) {
      assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
      final QueryType queryType = getFeatureType.getQuery().get(0);
      assertThat(queryType.isSetSortBy(), is(false));
    }
  }

  /**
   * WFS 1.1.0 Sorting uses the following format: Valid sort orders are "ASC" and "DESC". Ref:
   * http://schemas.opengis.net/filter/1.1.0/sort.xsd <wfs:Query typeName="QName QName">
   * <wfs:PropertyName>QName</wfs:PropertyName> <ogc:Filter> <ogc:Equals> <ogc:PropertyName/>
   * <gml:Point>... </gml:Point> </ogc:Equals> </ogc:Filter> <ogc:SortBy> <ogc:SortProperty>
   * <ogc:PropertyName>property</ogc:PropertyName> <ogc:SortOrder>ASC</ogc:SortOrder>
   * </ogc:SortProperty> </ogc:SortBy> </wfs:Query>
   */
  @Test
  public void testSortingSortOrderAscending() throws Exception {
    // Setup
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(
        MOCK_TEMPORAL_SORT_PROPERTY, MOCK_RELEVANCE_SORT_PROPERTY, MOCK_DISTANCE_SORT_PROPERTY);
    source.setMetacardMappers(metacardMappers);
    propertyIsLikeQuery.setSortBy(new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING));

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    for (final ExtendedGetFeatureType getFeatureType : captor.getAllValues()) {
      assertFeature(getFeatureType, true, MOCK_TEMPORAL_SORT_PROPERTY, "ASC");
    }
  }

  @Test
  public void testSortingSortOrderDescending() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(
        MOCK_TEMPORAL_SORT_PROPERTY, MOCK_RELEVANCE_SORT_PROPERTY, MOCK_DISTANCE_SORT_PROPERTY);
    source.setMetacardMappers(metacardMappers);
    propertyIsLikeQuery.setSortBy(new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING));

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    for (final ExtendedGetFeatureType getFeatureType : captor.getAllValues()) {
      assertFeature(getFeatureType, true, MOCK_TEMPORAL_SORT_PROPERTY, "DESC");
    }
  }

  @Test
  public void testSortingDisabled() throws Exception {
    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(
        MOCK_TEMPORAL_SORT_PROPERTY, MOCK_RELEVANCE_SORT_PROPERTY, MOCK_DISTANCE_SORT_PROPERTY);
    source.setMetacardMappers(metacardMappers);
    source.setDisableSorting(true);
    propertyIsLikeQuery.setSortBy(new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING));

    final ArgumentCaptor<ExtendedGetFeatureType> captor =
        ArgumentCaptor.forClass(ExtendedGetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    for (final ExtendedGetFeatureType getFeatureType : captor.getAllValues()) {
      assertFeature(getFeatureType, false, MOCK_TEMPORAL_SORT_PROPERTY, "ASC");
    }
  }

  @Test
  public void testSortingNoSortMapping() throws Exception {
    // if sorting is enabled but there is no sort mapping, throw an UnsupportedQueryException
    expectedEx.expect(UnsupportedQueryException.class);
    expectedEx.expectMessage("Source WFS_ID does not support specified sort property title");

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(null, null, null);
    source.setMetacardMappers(metacardMappers);
    source.setDisableSorting(false);
    propertyIsLikeQuery.setSortBy(new SortByImpl("title", SortOrder.ASCENDING));

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testSortingNoSortOrder() throws Exception {
    // if sort order is missing, throw UnsupportedQueryException
    expectedEx.expect(UnsupportedQueryException.class);
    expectedEx.expectMessage(
        "Source WFS_ID does not support specified sort property TEMPORAL with sort order null");

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(
        MOCK_TEMPORAL_SORT_PROPERTY, MOCK_RELEVANCE_SORT_PROPERTY, MOCK_DISTANCE_SORT_PROPERTY);
    source.setMetacardMappers(metacardMappers);
    propertyIsLikeQuery.setSortBy(new SortByImpl(Result.TEMPORAL, (String) null));

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testSortingNoSortProperty() throws Exception {
    // if sort property is missing, throw UnsupportedQueryException
    expectedEx.expect(UnsupportedQueryException.class);
    expectedEx.expectMessage(
        "Source WFS_ID does not support specified sort property null with sort order SortOrder[ASCENDING]");

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(
        MOCK_TEMPORAL_SORT_PROPERTY, MOCK_RELEVANCE_SORT_PROPERTY, MOCK_DISTANCE_SORT_PROPERTY);
    source.setMetacardMappers(metacardMappers);
    propertyIsLikeQuery.setSortBy(new SortByImpl(null, "ASC"));

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testSortingBadSortOrder() throws Exception {
    // if sort order is invalid throw UnsupportedQueryException
    expectedEx.expect(UnsupportedQueryException.class);
    expectedEx.expectMessage(
        "Source WFS_ID does not support specified sort property TEMPORAL with sort order SortOrder[foo]");

    mapSchemaToFeatures(ONE_TEXT_PROPERTY_SCHEMA_PERSON, ONE_FEATURE);
    setUpMocks(null, null, ONE_FEATURE, ONE_FEATURE);
    final QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    setupMapper(
        MOCK_TEMPORAL_SORT_PROPERTY, MOCK_RELEVANCE_SORT_PROPERTY, MOCK_DISTANCE_SORT_PROPERTY);
    source.setMetacardMappers(metacardMappers);
    propertyIsLikeQuery.setSortBy(new SortByImpl(Result.TEMPORAL, "foo"));

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  private void assertFeature(
      ExtendedGetFeatureType getFeatureType,
      boolean sortingEnabled,
      String sortProperty,
      String sortOrder) {
    assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
    final QueryType queryType = getFeatureType.getQuery().get(0);
    if (sortingEnabled) {
      assertThat(queryType.isSetSortBy(), is(true));
      assertThat(queryType.getSortBy().getSortProperty().size(), is(1));
      assertThat(
          queryType.getSortBy().getSortProperty().get(0).getPropertyName().getContent().size(),
          is(1));
      assertThat(
          queryType.getSortBy().getSortProperty().get(0).getPropertyName().getContent().get(0),
          is(sortProperty));
      assertThat(
          queryType.getSortBy().getSortProperty().get(0).getSortOrder().value(), is(sortOrder));
    } else {
      assertThat(queryType.isSetSortBy(), is(false));
    }
  }

  private void setupMapper(
      String temporalSortProperty, String relevanceSortProperty, String distanceSortProperty) {
    final MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setSortByTemporalFeatureProperty(temporalSortProperty);
    metacardMapper.setSortByDistanceFeatureProperty(relevanceSortProperty);
    metacardMapper.setSortByRelevanceFeatureProperty(distanceSortProperty);
    metacardMapper.setFeatureType("SampleFeature0");
    metacardMappers.add(metacardMapper);
  }

  private SourceResponse executeQuery(int startIndex, int pageSize)
      throws UnsupportedQueryException {

    Filter filter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);

    Query query = new QueryImpl(filter, startIndex, pageSize, null, false, 0);
    QueryRequest request = new QueryRequestImpl(query);

    return source.query(request);
  }

  private void assertMaxFeatures(ExtendedGetFeatureType getFeatureType, Query inQuery) {
    int pageSize = (inQuery.getStartIndex() / MAX_FEATURES + 1) * inQuery.getPageSize();
    assertThat(getFeatureType.getMaxFeatures(), is(BigInteger.valueOf(pageSize)));
  }

  private void assertCorrectMetacardsReturned(
      List<Result> results, int startIndex, int expectedNumberOfMetacards) {

    assertThat(results, hasSize(expectedNumberOfMetacards));
    for (int i = 0; i < expectedNumberOfMetacards; i++) {
      int id = startIndex + i;
      assertThat(results.get(i).getMetacard().getId(), equalTo("ID_" + id));
    }
  }

  private String marshal(final ExtendedGetFeatureType getFeatureType) throws JAXBException {
    Writer writer = new StringWriter();
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.marshal(getGetFeatureTypeJaxbElement(getFeatureType), writer);
    return writer.toString();
  }

  private JAXBElement<ExtendedGetFeatureType> getGetFeatureTypeJaxbElement(
      final ExtendedGetFeatureType getFeatureType) {
    return new JAXBElement<>(
        new QName("http://www.opengis.net/wfs", "GetFeature"),
        ExtendedGetFeatureType.class,
        getFeatureType);
  }

  private XmlSchema loadSchema(final String schemaXml) {
    final XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
    wfsUriResolver.setGmlNamespace(Wfs11Constants.GML_3_1_1_NAMESPACE);
    wfsUriResolver.setWfsNamespace(Wfs11Constants.WFS_NAMESPACE);
    schemaCollection.setSchemaResolver(wfsUriResolver);
    return schemaCollection.read(new StreamSource(new ByteArrayInputStream(schemaXml.getBytes())));
  }

  private void mapSchemaToFeatures(final String schemaXml, final int numFeatures)
      throws WfsException {
    for (int i = 0; i < numFeatures; i++) {
      mapSchemaToSingleFeature(schemaXml, i);
    }
  }

  private void mapSchemaToSingleFeature(final String schemaXml, final int featureNum)
      throws WfsException {
    final XmlSchema schema = loadSchema(schemaXml);
    final String typeName;
    if (featureNum == 0) {
      typeName = "SampleFeature0";
    } else {
      typeName = String.format("Prefix%1$d:SampleFeature%1$d", featureNum);
    }
    doReturn(schema).when(mockWfs).describeFeatureType(withTypeName(typeName));
  }

  private static DescribeFeatureTypeRequest withTypeName(final String typeName) {
    return argThat(new IsDescribeFeatureTypeRequestForTypeName(typeName));
  }

  private static class IsDescribeFeatureTypeRequestForTypeName
      implements ArgumentMatcher<DescribeFeatureTypeRequest> {
    private final String typeName;

    private IsDescribeFeatureTypeRequestForTypeName(final String typeName) {
      this.typeName = typeName;
    }

    @Override
    public boolean matches(final DescribeFeatureTypeRequest request) {
      return Objects.equals(request.getTypeName(), typeName);
    }
  }

  private static ExtendedGetFeatureType withResultType(final ResultTypeType resultType) {
    return argThat(new IsGetFeatureRequestWithResultType(resultType));
  }

  private static class IsGetFeatureRequestWithResultType
      implements ArgumentMatcher<ExtendedGetFeatureType> {
    private final ResultTypeType resultType;

    private IsGetFeatureRequestWithResultType(final ResultTypeType resultType) {
      this.resultType = resultType;
    }

    @Override
    public boolean matches(final ExtendedGetFeatureType featureType) {
      return featureType != null
          && Objects.equals(featureType.getResultType(), resultType)
          && !featureType.isSetStartIndex()
          && featureType.startIndex == null;
    }
  }

  private static ExtendedGetFeatureType hasStartIndex() {
    return argThat(new IsGetFeatureRequestWithStartIndex());
  }

  private static class IsGetFeatureRequestWithStartIndex
      implements ArgumentMatcher<ExtendedGetFeatureType> {

    @Override
    public boolean matches(final ExtendedGetFeatureType featureType) {
      return featureType != null
          && Objects.equals(featureType.getResultType(), ResultTypeType.RESULTS)
          && featureType.isSetStartIndex()
          && featureType.startIndex.intValue() > 0;
    }
  }
}
