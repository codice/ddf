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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import ogc.schema.opengis.filter.v_1_0_0.BinaryLogicOpType;
import ogc.schema.opengis.filter.v_1_0_0.LogicOpsType;
import ogc.schema.opengis.filter.v_1_0_0.PropertyIsLikeType;
import ogc.schema.opengis.filter.v_1_0_0.SpatialOpsType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.BBOX;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.FilterCapabilities;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Intersect;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.SpatialCapabilitiesType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.SpatialOperatorsType;
import ogc.schema.opengis.wfs.v_1_0_0.GetFeatureType;
import ogc.schema.opengis.wfs.v_1_0_0.QueryType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeListType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.WFSCapabilitiesType;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsUriResolver;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;

public class WfsSourceTest {

  private static final String ONE_TEXT_PROPERTY_SCHEMA =
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

  private static final String ORDER_PERSON = "orderperson";

  private static final String ORDER_DOG = "orderdog";

  private static final Integer MAX_FEATURES = 10;

  private static final Integer ONE_FEATURE = 1;

  private static final Integer TWO_FEATURES = 2;

  private static final String SRS_NAME = "EPSG:4326";

  private static final String LITERAL = "literal";

  private static final Comparator<QueryType> QUERY_TYPE_COMPARATOR =
      (queryType1, queryType2) -> {
        String typeName1 = queryType1.getTypeName().getLocalPart();
        String typeName2 = queryType2.getTypeName().getLocalPart();
        return typeName1.compareTo(typeName2);
      };

  private final GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();

  private Wfs mockWfs = mock(Wfs.class);

  private WFSCapabilitiesType mockCapabilities = new WFSCapabilitiesType();

  private WfsFeatureCollection mockFeatureCollection = mock(WfsFeatureCollection.class);

  private BundleContext mockContext = mock(BundleContext.class);

  private List<QName> sampleFeatures;

  private WfsUriResolver wfsUriResolver = new WfsUriResolver();

  private WfsSource source;

  private ClientFactoryFactory mockClientFactory = mock(ClientFactoryFactory.class);

  private AvailabilityTask mockAvailabilityTask = mock(AvailabilityTask.class);

  private EncryptionService encryptionService = mock(EncryptionService.class);

  private void setUpMocks(
      final String schema,
      final List<Object> supportedGeos,
      final String srsName,
      final Integer numFeatures,
      final Integer numResults)
      throws WfsException, SecurityServiceException {

    SecureCxfClientFactory mockFactory = mock(SecureCxfClientFactory.class);
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
    mockCapabilities.setFilterCapabilities(new FilterCapabilities());
    mockCapabilities.getFilterCapabilities().setSpatialCapabilities(new SpatialCapabilitiesType());
    mockCapabilities
        .getFilterCapabilities()
        .getSpatialCapabilities()
        .setSpatialOperators(new SpatialOperatorsType());
    if (null != supportedGeos && !supportedGeos.isEmpty()) {
      mockCapabilities
          .getFilterCapabilities()
          .getSpatialCapabilities()
          .getSpatialOperators()
          .getBBOXOrEqualsOrDisjoint()
          .addAll(supportedGeos);
    }

    // DescribeFeatureType Response
    XmlSchema xmlSchema = null;
    if (null != schema) {
      XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
      wfsUriResolver.setGmlNamespace(Wfs10Constants.GML_NAMESPACE);
      wfsUriResolver.setWfsNamespace(Wfs10Constants.WFS_NAMESPACE);
      schemaCollection.setSchemaResolver(wfsUriResolver);
      xmlSchema =
          schemaCollection.read(new StreamSource(new ByteArrayInputStream(schema.getBytes())));
    }

    when(mockWfs.describeFeatureType(any(DescribeFeatureTypeRequest.class))).thenReturn(xmlSchema);

    sampleFeatures = new ArrayList<>();
    mockCapabilities.setFeatureTypeList(new FeatureTypeListType());
    if (numFeatures != null) {
      for (int ii = 0; ii < numFeatures; ii++) {

        FeatureTypeType feature = new FeatureTypeType();
        QName qName;
        if (ii == 0) {
          qName = new QName("SampleFeature" + ii);
        } else {
          qName = new QName("http://example.com", "SampleFeature" + ii, "Prefix" + ii);
        }
        sampleFeatures.add(qName);
        feature.setName(qName);
        // feature.setName(SAMPLE_FEATURE[ii]);
        if (null != srsName) {
          feature.setSRS(srsName);
        }
        mockCapabilities.getFeatureTypeList().getFeatureType().add(feature);
      }
    }

    // GetFeature Response
    when(mockWfs.getFeature(any(GetFeatureType.class))).thenReturn(mockFeatureCollection);

    when(mockFeatureCollection.getFeatureMembers())
        .thenAnswer(
            new Answer<List<Metacard>>() {
              @Override
              public List<Metacard> answer(InvocationOnMock invocation) {
                // Create as many metacards as there are features
                Integer resultsToReturn = numResults;
                if (resultsToReturn == null && numFeatures != null) {
                  resultsToReturn = numFeatures;
                }
                List<Metacard> metacards = new ArrayList<Metacard>(resultsToReturn);
                for (int i = 0; i < resultsToReturn; i++) {
                  MetacardImpl mc = new MetacardImpl();
                  mc.setId("ID_" + String.valueOf(i + 1));
                  metacards.add(mc);
                }

                return metacards;
              }
            });

    when(mockAvailabilityTask.isAvailable()).thenReturn(true);

    source =
        new WfsSource(
            new GeotoolsFilterAdapterImpl(),
            mockContext,
            mockAvailabilityTask,
            mockClientFactory,
            encryptionService);
  }

  @Test
  public void testAvailability() throws WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
    assertTrue(source.isAvailable());
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testQueryEmptyQueryList()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testPropertyIsLikeQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    assertTrue(query.getFilter().isSetComparisonOps());
    assertTrue(query.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
  }

  @Test
  public void testTwoPropertyQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(TWO_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    // The Text Properties should be ORed
    assertTrue(query.getFilter().isSetLogicOps());
    assertTrue(query.getFilter().getLogicOps().getValue() instanceof BinaryLogicOpType);
  }

  @Test
  public void testContentTypeQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
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
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0))); // SAMPLE_FEATURE[0]));
    assertTrue(query.getFilter().isSetComparisonOps());
    assertTrue(query.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
  }

  @Test
  public void testContentTypeAndNoPropertyQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);

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
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertEquals(ONE_FEATURE.intValue(), getFeatureType.getQuery().size());
    assertEquals(sampleFeatures.get(0), getFeatureType.getQuery().get(0).getTypeName());
  }

  @Test
  public void testTwoContentTypeAndNoPropertyQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);

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
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, twoContentTypeQuery);
    Collections.sort(getFeatureType.getQuery(), QUERY_TYPE_COMPARATOR);
    assertEquals(TWO_FEATURES.intValue(), getFeatureType.getQuery().size());
    assertEquals(sampleFeatures.get(0), getFeatureType.getQuery().get(0).getTypeName());
  }

  @Test
  public void testAndQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
    Filter propertyIsLikeFilter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);
    Filter contentTypeFilter =
        builder.attribute(Metacard.ANY_TEXT).is().like().text(sampleFeatures.get(0).getLocalPart());
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.allOf(propertyIsLikeFilter, contentTypeFilter));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    assertTrue(query.getFilter().isSetLogicOps());
    assertTrue(query.getFilter().getLogicOps().getValue() instanceof BinaryLogicOpType);
  }

  @Test
  public void testIntersectQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(
        ONE_GML_PROPERTY_SCHEMA,
        Arrays.asList(new Intersect(), new BBOX()),
        SRS_NAME,
        ONE_FEATURE,
        null);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    assertTrue(query.getFilter().isSetSpatialOps());
    assertTrue(query.getFilter().getSpatialOps().getValue() instanceof SpatialOpsType);
  }

  @Test
  public void testTwoIntersectQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(
        TWO_GML_PROPERTY_SCHEMA,
        Arrays.asList(new Intersect(), new BBOX()),
        SRS_NAME,
        ONE_FEATURE,
        null);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    // The Text Properties should be ORed
    assertNotNull(query.getFilter());
    assertTrue(query.getFilter().isSetLogicOps());
    assertTrue(query.getFilter().getLogicOps().getValue() instanceof LogicOpsType);
  }

  @Test
  public void testBboxQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    List<Object> bbox = new ArrayList<Object>();
    bbox.add(new BBOX());
    setUpMocks(ONE_GML_PROPERTY_SCHEMA, bbox, SRS_NAME, ONE_FEATURE, null);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    assertTrue(query.getFilter().isSetSpatialOps());
    assertTrue(query.getFilter().getSpatialOps().getValue() instanceof SpatialOpsType);
  }

  @Test
  public void testGmlImport()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    List<Object> bbox = new ArrayList<Object>();
    bbox.add(new BBOX());
    setUpMocks(GML_IMPORT_SCHEMA, bbox, SRS_NAME, ONE_FEATURE, null);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(intersectQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, intersectQuery);
    assertTrue(getFeatureType.getQuery().size() == ONE_FEATURE);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    assertTrue(query.getFilter().isSetSpatialOps());
    assertTrue(query.getFilter().getSpatialOps().getValue() instanceof SpatialOpsType);
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testNoGeoAttribuesQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);
    Filter intersectFilter =
        builder.attribute(Metacard.ANY_GEO).is().intersecting().wkt(POLYGON_WKT);
    QueryImpl intersectQuery = new QueryImpl(intersectFilter);
    intersectQuery.setPageSize(MAX_FEATURES);

    source.query(new QueryRequestImpl(intersectQuery));
  }

  @Test
  public void testTwoFeatureTypesQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(propertyIsLikeQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, propertyIsLikeQuery);
    assertTrue(getFeatureType.getQuery().size() == TWO_FEATURES);
    Collections.sort(getFeatureType.getQuery(), QUERY_TYPE_COMPARATOR);
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    assertTrue(query.getFilter().isSetComparisonOps());
    assertTrue(query.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
    QueryType query2 = getFeatureType.getQuery().get(1);
    assertTrue(query2.getTypeName().equals(sampleFeatures.get(1)));
    assertTrue(query2.getFilter().isSetComparisonOps());
    assertTrue(query2.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=1, should get 4 results back - metacards 1 thru 4.
   *
   * @throws WfsException
   * @throws SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingStartIndexOne()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 4;
    int startIndex = 1;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, MAX_FEATURES, null);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(pageSize));
    assertThat(response.getHits(), equalTo(new Long(MAX_FEATURES)));

    // Verify that metacards 1 thru 4 were returned since pageSize=4
    assertCorrectMetacardsReturned(results, startIndex, pageSize);
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=2, should get 4 results back - metacards 2 thru 5.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingStartIndexTwo()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 4;
    int startIndex = 2;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, MAX_FEATURES, null);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(pageSize));
    assertThat(response.getHits(), equalTo(new Long(MAX_FEATURES)));

    // Verify that metacards 2 thru 5 were returned since pageSize=4
    assertCorrectMetacardsReturned(results, startIndex, pageSize);
  }

  /**
   * Given 2 features (and metacards) exist that match search criteria, since page size=4 and
   * startIndex=3, should get 0 results back and total hits of 2.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingStartIndexGreaterThanNumberOfFeatures()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 4;
    int startIndex = 3;
    int numFeatures = 2;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, numFeatures, null);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(0));
    assertThat(response.getHits(), equalTo(new Long(numFeatures)));
  }

  // Simulates query by ID (which is analogous to clicking on link in search
  // results to
  // view associated metacard in XML)
  @Test
  public void testPaging()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 4;
    int startIndex = 1;
    int numFeatures = 1;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, numFeatures, null);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(1));
    assertThat(response.getHits(), equalTo(new Long(numFeatures)));
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=20 (which
   * is larger than number of features) and startIndex=1, should get 10 results back - metacards 1
   * thru 10.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingPageSizeExceedsFeatureCountStartIndexOne()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 20;
    int startIndex = 1;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, MAX_FEATURES, null);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(MAX_FEATURES));
    assertThat(response.getHits(), equalTo(new Long(MAX_FEATURES)));

    // Verify that metacards 1 thru 10 were returned
    assertCorrectMetacardsReturned(results, startIndex, MAX_FEATURES);
  }

  /**
   * Given 10 features (and metacards) exist that match search criteria, since page size=20 (which
   * is larger than number of features) and startIndex=2, should get 9 results back - metacards 2
   * thru 10.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingPageSizeExceedsFeatureCountStartIndexTwo()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 20;
    int startIndex = 2;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, MAX_FEATURES, null);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(MAX_FEATURES - 1));
    assertThat(response.getHits(), equalTo(new Long(MAX_FEATURES)));

    // Verify that metacards 2 thru 10 were returned
    assertCorrectMetacardsReturned(results, startIndex, MAX_FEATURES - 1);
  }

  /**
   * Verify that, per DDF Query API Javadoc, if the startIndex is negative, the WfsSource throws an
   * UnsupportedQueryException.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testPagingStartIndexNegative()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 4;
    int startIndex = -1;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, MAX_FEATURES, null);

    executeQuery(startIndex, pageSize);
  }

  /**
   * Verify that, per DDF Query API Javadoc, if the startIndex is zero, the WfsSource throws an
   * UnsupportedQueryException.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testPagingStartIndexZero()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {
    int pageSize = 4;
    int startIndex = 0;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, MAX_FEATURES, null);

    executeQuery(startIndex, pageSize);
  }

  /**
   * Verify that if page size is negative, WfsSource defaults it to the max features that can be
   * returned.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingPageSizeNegative()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = -1;
    int startIndex = 1;
    int numResults = WfsSource.WFS_MAX_FEATURES_RETURNED + 10;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, 1, numResults);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(WfsSource.WFS_MAX_FEATURES_RETURNED));
    assertThat(response.getHits(), equalTo(new Long(numResults)));
  }

  /**
   * Verify that if page size is zero, WfsSource defaults it to the max features that can be
   * returned.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingPageSizeZero()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = 0;
    int startIndex = 1;
    int numResults = WfsSource.WFS_MAX_FEATURES_RETURNED + 10;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, 1, numResults);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(WfsSource.WFS_MAX_FEATURES_RETURNED));
    assertThat(response.getHits(), equalTo(new Long(numResults)));
  }

  /**
   * Given 1010 features (and metacards) exist that match search criteria, since page size=1001
   * (which is larger than max number of features the WfsSource allows to be returned) and
   * startIndex=1, should get 1000 results back, but a total hits of 1010.
   *
   * @throws WfsException, SecurityServiceException
   * @throws TransformerConfigurationException
   * @throws UnsupportedQueryException
   */
  @Test
  public void testPagingPageSizeExceedsMaxFeaturesThatCanBeReturned()
      throws WfsException, SecurityServiceException, TransformerConfigurationException,
          UnsupportedQueryException {

    int pageSize = WfsSource.WFS_MAX_FEATURES_RETURNED + 1;
    int startIndex = 1;
    int numResults = WfsSource.WFS_MAX_FEATURES_RETURNED + 10;

    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, 1, numResults);

    SourceResponse response = executeQuery(startIndex, pageSize);
    List<Result> results = response.getResults();

    assertThat(results.size(), is(WfsSource.WFS_MAX_FEATURES_RETURNED));
    assertThat(response.getHits(), equalTo(new Long(numResults)));
  }

  @Test
  public void testGetContentTypes() throws WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, 2, null);
    Set<ContentType> contentTypes = source.getContentTypes();
    assertTrue(contentTypes.size() == TWO_FEATURES);
    for (ContentType contentType : contentTypes) {
      assertTrue(
          sampleFeatures.get(0).getLocalPart().equals(contentType.getName())
              || sampleFeatures.get(1).getLocalPart().equals(contentType.getName()));
    }
  }

  @Test
  public void testQueryTwoFeaturesOneInvalid()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(TWO_TEXT_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);
    Filter orderPersonFilter = builder.attribute(ORDER_PERSON).is().like().text(LITERAL);
    Filter mctFeature1Filter =
        builder
            .attribute(Metacard.CONTENT_TYPE)
            .is()
            .like()
            .text(sampleFeatures.get(0).getLocalPart());
    Filter feature1Filter = builder.allOf(Arrays.asList(orderPersonFilter, mctFeature1Filter));
    Filter orderDogFilter = builder.attribute("FAKE").is().like().text(LITERAL);
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
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, inQuery);
    assertEquals(ONE_FEATURE.intValue(), getFeatureType.getQuery().size());
    QueryType query = getFeatureType.getQuery().get(0);
    assertTrue(query.getTypeName().equals(sampleFeatures.get(0)));
    // The Text Properties should be ORed
    assertTrue(query.getFilter().isSetComparisonOps());
    assertTrue(query.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
    PropertyIsLikeType pilt = (PropertyIsLikeType) query.getFilter().getComparisonOps().getValue();
    assertEquals(ORDER_PERSON, pilt.getPropertyName().getContent());
  }

  @Test
  public void testQueryTwoFeaturesWithMixedPropertyNames()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(TWO_TEXT_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);
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
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertMaxFeatures(getFeatureType, inQuery);
    Collections.sort(getFeatureType.getQuery(), QUERY_TYPE_COMPARATOR);
    assertEquals(TWO_FEATURES.intValue(), getFeatureType.getQuery().size());
    // Feature 1
    QueryType query = getFeatureType.getQuery().get(0);
    assertThat(query.getTypeName(), equalTo(sampleFeatures.get(0)));
    // this should only have 1 filter which is a comparison
    assertTrue(query.getFilter().isSetComparisonOps());
    assertTrue(query.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
    PropertyIsLikeType pilt = (PropertyIsLikeType) query.getFilter().getComparisonOps().getValue();
    assertNotNull(pilt);
    assertEquals(ORDER_PERSON, pilt.getPropertyName().getContent());
    // Feature 2
    QueryType query2 = getFeatureType.getQuery().get(1);
    assertTrue(query2.getTypeName().equals(sampleFeatures.get(1)));
    // this should only have 1 filter which is a comparison
    assertTrue(query2.getFilter().isSetComparisonOps());
    assertTrue(query2.getFilter().getComparisonOps().getValue() instanceof PropertyIsLikeType);
    PropertyIsLikeType pilt2 =
        (PropertyIsLikeType) query2.getFilter().getComparisonOps().getValue();
    assertEquals(ORDER_DOG, pilt2.getPropertyName().getContent());
  }

  @Test
  public void testIDQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);

    QueryImpl idQuery = new QueryImpl(builder.attribute(Core.ID).is().text(ORDER_PERSON));

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(idQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertEquals(
        ONE_FEATURE.intValue(), getFeatureType.getQuery().get(0).getFilter().getFeatureId().size());

    assertEquals(
        ORDER_PERSON, getFeatureType.getQuery().get(0).getFilter().getFeatureId().get(0).getFid());
  }

  @Test
  public void testTwoIDQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(NO_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);

    Filter idFilter1 = builder.attribute(Core.ID).is().text(ORDER_PERSON);
    Filter idFilter2 = builder.attribute(Core.ID).is().text(ORDER_DOG);

    QueryImpl twoIDQuery = new QueryImpl(builder.anyOf(Arrays.asList(idFilter1, idFilter2)));

    ArgumentCaptor<GetFeatureType> captor = ArgumentCaptor.forClass(GetFeatureType.class);
    source.query(new QueryRequestImpl(twoIDQuery));
    verify(mockWfs).getFeature(captor.capture());

    GetFeatureType getFeatureType = captor.getValue();
    assertEquals(
        TWO_FEATURES.intValue(),
        getFeatureType.getQuery().get(0).getFilter().getFeatureId().size());

    assertTrue(
        ORDER_PERSON.equals(
                getFeatureType.getQuery().get(0).getFilter().getFeatureId().get(0).getFid())
            || ORDER_PERSON.equals(
                getFeatureType.getQuery().get(0).getFilter().getFeatureId().get(1).getFid()));

    assertTrue(
        ORDER_DOG.equals(
                getFeatureType.getQuery().get(0).getFilter().getFeatureId().get(0).getFid())
            || ORDER_DOG.equals(
                getFeatureType.getQuery().get(0).getFilter().getFeatureId().get(1).getFid()));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testOneIDOnePropertyQuery()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, TWO_FEATURES, null);

    Filter idFilter = builder.attribute(Core.ID).is().text(ORDER_PERSON);
    Filter propertyIsLikeFilter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);

    QueryImpl query = new QueryImpl(builder.anyOf(Arrays.asList(propertyIsLikeFilter, idFilter)));

    // we are verifying that mixing featureID filters with other filters is
    // not supported
    source.query(new QueryRequestImpl(query));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testNoFeatures()
      throws UnsupportedQueryException, WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, 0, null);
    QueryImpl propertyIsLikeQuery =
        new QueryImpl(builder.attribute(Metacard.ANY_TEXT).is().like().text("literal"));
    propertyIsLikeQuery.setPageSize(MAX_FEATURES);
    // when(mockWfs.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(null);

    source.query(new QueryRequestImpl(propertyIsLikeQuery));
  }

  @Test
  public void testTimeoutConfiguration() throws WfsException, SecurityServiceException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);

    source.setConnectionTimeout(10000);
    source.setReceiveTimeout(10000);
    // Perform test
    assertEquals(source.getConnectionTimeout().intValue(), 10000);
    assertEquals(source.getReceiveTimeout().intValue(), 10000);
  }

  @Test
  public void testClientFactoryIsCreatedCorrectlyWhenUsernameAndPasswordAreConfigured()
      throws SecurityServiceException, WfsException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);

    final String wfsUrl = "http://localhost/wfs";
    final String username = "test_user";
    final String password = "encrypted_password";
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
            .put("disableCnCheck", disableCnCheck)
            .put("connectionTimeout", connectionTimeout)
            .put("receiveTimeout", receiveTimeout)
            .put("pollInterval", 1)
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
      throws SecurityServiceException, WfsException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);

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
  public void testClientFactoryIsCreatedCorrectlyWhenNoAuthIsConfigured()
      throws SecurityServiceException, WfsException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);

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
  public void testNoWfsClientRefreshWhenConfigurationDoesNotChange()
      throws SecurityServiceException, WfsException {
    setUpMocks(ONE_TEXT_PROPERTY_SCHEMA, null, null, ONE_FEATURE, null);

    verify(mockClientFactory)
        .getSecureCxfClientFactory(
            anyString(),
            eq(Wfs.class),
            any(List.class),
            isA(MarkableStreamInterceptor.class),
            anyBoolean(),
            anyBoolean(),
            anyInt(),
            anyInt());

    verify(mockWfs).getCapabilities(any(GetCapabilitiesRequest.class));
    verify(mockWfs).describeFeatureType(any(DescribeFeatureTypeRequest.class));

    final String wfsUrl = "http://localhost/wfs";
    final Boolean disableCnCheck = false;
    final Integer initialConnectionTimeout = 10000;
    final Integer initialReceiveTimeout = 20000;

    source.setWfsUrl(wfsUrl);
    source.setDisableCnCheck(disableCnCheck);
    source.setConnectionTimeout(initialConnectionTimeout);
    source.setReceiveTimeout(initialReceiveTimeout);
    source.setPollInterval(1);

    final Map<String, Object> configuration =
        ImmutableMap.<String, Object>builder()
            .put("wfsUrl", wfsUrl)
            .put("disableCnCheck", disableCnCheck)
            .put("connectionTimeout", initialConnectionTimeout)
            .put("receiveTimeout", initialReceiveTimeout)
            .put("pollInterval", 1)
            .build();
    source.refresh(configuration);

    verifyNoMoreInteractions(mockClientFactory);

    verify(mockWfs).getCapabilities(any(GetCapabilitiesRequest.class));
    verify(mockWfs).describeFeatureType(any(DescribeFeatureTypeRequest.class));
  }

  private SourceResponse executeQuery(int startIndex, int pageSize)
      throws UnsupportedQueryException {

    Filter filter = builder.attribute(Metacard.ANY_TEXT).is().like().text(LITERAL);

    Query query = new QueryImpl(filter, startIndex, pageSize, null, false, 0);
    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = source.query(request);

    return response;
  }

  private void assertMaxFeatures(GetFeatureType getFeatureType, Query inQuery) {
    int pageSize =
        (inQuery.getStartIndex() / MAX_FEATURES + 1)
            * inQuery.getPageSize()
            * WfsSource.WFS_QUERY_PAGE_SIZE_MULTIPLIER;
    assertTrue(getFeatureType.getMaxFeatures().equals(BigInteger.valueOf(pageSize)));
  }

  private void assertCorrectMetacardsReturned(
      List<Result> results, int startIndex, int expectedNumberOfMetacards) {

    for (int i = 0; i < expectedNumberOfMetacards; i++) {
      int id = startIndex + i;
      assertThat(results.get(i).getMetacard().getId(), equalTo("ID_" + String.valueOf(id)));
    }
  }
}
