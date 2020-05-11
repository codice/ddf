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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
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
import net.opengis.wfs.v_1_1_0.GetFeatureType;
import net.opengis.wfs.v_1_1_0.QueryType;
import net.opengis.wfs.v_1_1_0.ResultTypeType;
import net.opengis.wfs.v_1_1_0.WFSCapabilitiesType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollectionImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl.MetacardMapperImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsUriResolver;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;

public class WfsSourceTest {
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

  private static final Comparator<QueryType> QUERY_TYPE_COMPARATOR =
      (queryType1, queryType2) -> {
        String typeName1 = queryType1.getTypeName().get(0).getLocalPart();
        String typeName2 = queryType2.getTypeName().get(0).getLocalPart();
        return typeName1.compareTo(typeName2);
      };

  private static JAXBContext jaxbContext;

  private final GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();

  private Wfs mockWfs = mock(Wfs.class);

  private WFSCapabilitiesType mockCapabilities = new WFSCapabilitiesType();

  private BundleContext mockContext = mock(BundleContext.class);

  private List<QName> sampleFeatures;

  private WfsUriResolver wfsUriResolver = new WfsUriResolver();

  private WfsSource source;

  private ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);

  private EncryptionService encryptionService = mock(EncryptionService.class);

  private WfsMetacardTypeRegistry mockWfsMetacardTypeRegistry = mock(WfsMetacardTypeRegistry.class);

  private ClientFactoryFactory mockClientFactoryFactory = mock(ClientFactoryFactory.class);

  private List<MetacardMapper> metacardMappers = new ArrayList<>();

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

    SecureCxfClientFactory mockFactory = mock(SecureCxfClientFactory.class);
    when(mockFactory.getClient()).thenReturn(mockWfs);

    when(mockClientFactoryFactory.getSecureCxfClientFactory(any(), any())).thenReturn(mockFactory);
    when(mockClientFactoryFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(mockFactory);
    when(mockClientFactoryFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
        .thenReturn(mockFactory);
    when(mockClientFactoryFactory.getSecureCxfClientFactory(
            anyString(), any(), any(), any(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
        .thenReturn(mockFactory);
    when(mockClientFactoryFactory.getSecureCxfClientFactory(
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
    when(mockClientFactoryFactory.getSecureCxfClientFactory(
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
              supportedGeos
                  .stream()
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

    source = new WfsSource(mockClientFactoryFactory, encryptionService, mockScheduler);
    source.setFilterAdapter(new GeotoolsFilterAdapterImpl());
    source.setContext(mockContext);
    source.setWfsMetacardTypeRegistry(mockWfsMetacardTypeRegistry);
    source.setMetacardTypeEnhancers(Collections.emptyList());
    source.setMetacardMappers(metacardMappers);
    source.setPollInterval(10);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(twoContentTypeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(inQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
    assertMaxFeatures(getFeatureType, inQuery);

    List<QueryType> filterQueries =
        getFeatureType
            .getQuery()
            .stream()
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(inQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(idQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(twoIDQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    final ArgumentCaptor<GetFeatureType> getFeatureCaptor =
        ArgumentCaptor.forClass(GetFeatureType.class);
    verify(mockWfs, times(2)).getFeature(getFeatureCaptor.capture());

    GetFeatureType getFeatureType = getFeatureCaptor.getAllValues().get(1);
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
            .build();
    source.refresh(configuration);

    verify(mockClientFactoryFactory)
        .getSecureCxfClientFactory(
            eq(wfsUrl),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            eq(disableCnCheck),
            eq(allowRedirects),
            eq(connectionTimeout),
            eq(receiveTimeout),
            eq(username),
            eq("unencrypted_password"));
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
            .build();
    source.refresh(configuration);

    verify(mockClientFactoryFactory)
        .getSecureCxfClientFactory(
            eq(wfsUrl),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            eq(disableCnCheck),
            eq(allowRedirects),
            eq(connectionTimeout),
            eq(receiveTimeout),
            eq(certAlias),
            eq(keystorePath),
            eq(sslProtocol));
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
            .build();
    source.refresh(configuration);

    verify(mockClientFactoryFactory)
        .getSecureCxfClientFactory(
            eq(wfsUrl),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            eq(disableCnCheck),
            eq(allowRedirects),
            eq(connectionTimeout),
            eq(receiveTimeout));
  }

  @Test
  public void testQueryLatLonCoordinateOrder() throws Exception {
    mapSchemaToFeatures(ONE_GML_PROPERTY_SCHEMA, ONE_FEATURE);
    setUpMocks(Collections.singletonList("DWithin"), SRS_NAME, ONE_FEATURE, ONE_FEATURE);

    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("coordinateOrder", LAT_LON_ORDER)
            .put("forceSpatialFilter", "NO_FILTER")
            .put("allowRedirects", false)
            .put("disableCnCheck", false)
            .put("pollInterval", 1)
            .build();
    source.refresh(configuration);

    final Filter withinFilter =
        builder.attribute(Metacard.ANY_GEO).is().withinBuffer().wkt(POINT_WKT, 10.0);
    final Query withinQuery = new QueryImpl(withinFilter);

    final ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(withinQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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
            .put("coordinateOrder", LON_LAT_ORDER)
            .put("forceSpatialFilter", "NO_FILTER")
            .put("allowRedirects", false)
            .put("disableCnCheck", false)
            .put("pollInterval", 1)
            .build();
    source.refresh(configuration);

    final Filter withinFilter =
        builder.attribute(Metacard.ANY_GEO).is().withinBuffer().wkt(POINT_WKT, 10.0);
    final Query withinQuery = new QueryImpl(withinFilter);

    final ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(withinQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getAllValues().get(1);
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

    final ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs, times(2)).getFeature(captor.capture());

    final GetFeatureType getHits = captor.getAllValues().get(0);
    assertThat(getHits.getResultType(), is(ResultTypeType.HITS));
    assertThat(getHits.getMaxFeatures(), is(nullValue()));

    final GetFeatureType getResults = captor.getAllValues().get(1);
    assertThat(getResults.getResultType(), is(ResultTypeType.RESULTS));
    assertMaxFeatures(getResults, propertyIsLikeQuery);

    for (final GetFeatureType getFeatureType : captor.getAllValues()) {
      assertThat(getFeatureType.getQuery().size(), is(ONE_FEATURE));
      final QueryType query = getFeatureType.getQuery().get(0);
      assertThat(query.getTypeName().get(0), is(sampleFeatures.get(0)));
      assertThat(query.getFilter().isSetComparisonOps(), is(true));
      assertThat(
          query.getFilter().getComparisonOps().getValue(),
          is(instanceOf(PropertyIsLikeType.class)));
    }
  }

  private SourceResponse executeQuery(int startIndex, int pageSize)
      throws UnsupportedQueryException {

    Filter filter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);

    Query query = new QueryImpl(filter, startIndex, pageSize, null, false, 0);
    QueryRequest request = new QueryRequestImpl(query);

    return source.query(request);
  }

  private void assertMaxFeatures(GetFeatureType getFeatureType, Query inQuery) {
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

  private String marshal(final GetFeatureType getFeatureType) throws JAXBException {
    Writer writer = new StringWriter();
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.marshal(getGetFeatureTypeJaxbElement(getFeatureType), writer);
    return writer.toString();
  }

  private JAXBElement<GetFeatureType> getGetFeatureTypeJaxbElement(
      final GetFeatureType getFeatureType) {
    return new JAXBElement<>(
        new QName("http://www.opengis.net/wfs", "GetFeature"),
        GetFeatureType.class,
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

  private static GetFeatureType withResultType(final ResultTypeType resultType) {
    return argThat(new IsGetFeatureRequestWithResultType(resultType));
  }

  private static class IsGetFeatureRequestWithResultType implements ArgumentMatcher<GetFeatureType> {
    private final ResultTypeType resultType;

    private IsGetFeatureRequestWithResultType(final ResultTypeType resultType) {
      this.resultType = resultType;
    }

    @Override
    public boolean matches(final GetFeatureType featureType) {
      return Objects.equals(featureType.getResultType(), resultType);
    }
  }
}
