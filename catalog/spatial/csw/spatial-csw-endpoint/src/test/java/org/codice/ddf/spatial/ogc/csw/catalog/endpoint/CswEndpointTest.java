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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.QueryFilterTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.cat.csw.v_2_0_2.SchemaComponentType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionSummaryType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.ows.v_1_0_0.AcceptVersionsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import net.opengis.ows.v_1_0_0.SectionsType;
import net.opengis.ows.v_1_0_0.ServiceIdentification;
import net.opengis.ows.v_1_0_0.ServiceProvider;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.UpdateActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswActionTransformerProvider;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.sort.SortBy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswEndpointTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswEndpointTest.class);

  private static final String BAD_TYPE = "MyBadType";

  private static final String VALID_TYPES = "csw:Record,csw:Record";

  private static final String BAD_TYPES = "unknown:Record,foo:Bar";

  private static final String VALID_TYPE = "Record";

  private static final String VALID_PREFIX = "csw";

  private static final String VALID_PREFIX_LOCAL_TYPE = VALID_PREFIX + ":" + VALID_TYPE;

  private static final String CONTEXTUAL_TEST_ATTRIBUTE = "csw:title";

  private static final String CQL_CONTEXTUAL_PATTERN = "some title";

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";

  private static final String CQL_CONTEXTUAL_LIKE_QUERY =
      CONTEXTUAL_TEST_ATTRIBUTE + " Like '" + CQL_CONTEXTUAL_PATTERN + "'";

  private static final String GMD_CONTEXTUAL_LIKE_QUERY =
      GmdConstants.APISO_PREFIX + "title Like '" + CQL_CONTEXTUAL_PATTERN + "'";

  private static final String RANGE_VALUE = "bytes=100-";

  private static final int BATCH_TOTAL = 70;

  private static final String THIRD_PARTY_TYPE_NAME = "thirdPartyTypeName";

  private static UriInfo mockUriInfo = mock(UriInfo.class);

  private static Bundle mockBundle = mock(Bundle.class);

  private static BundleContext mockBundleContext = mock(BundleContext.class);

  private static CswEndpoint csw;

  private static CatalogFramework catalogFramework = mock(CatalogFramework.class);

  private static TransformerManager mockMimeTypeManager = mock(TransformerManager.class);

  private static TransformerManager mockSchemaManager = mock(TransformerManager.class);

  private static TransformerManager mockInputManager = mock(TransformerManager.class);

  private static CswActionTransformerProvider mockCswActionTransformerProvider =
      mock(CswActionTransformerProvider.class);

  private static QueryResponseTransformer mockTransformer = mock(QueryResponseTransformer.class);

  private static QName cswQnameOutPutSchema = new QName(CswConstants.CSW_OUTPUT_SCHEMA);

  private static final long RESULT_COUNT = 10;

  private static final long TOTAL_COUNT = 10;

  private Validator validator = mock(Validator.class);

  private List<QueryResponse> queryResponseBatch;

  private CswQueryFactory queryFactory = mock(CswQueryFactory.class);

  @org.junit.Before
  public void setUpBeforeClass()
      throws URISyntaxException, SourceUnavailableException, UnsupportedQueryException,
          FederationException, ParseException, IngestException, CswException,
          InvalidSyntaxException {
    URI mockUri = new URI("http://example.com/services/csw");
    when(mockUriInfo.getBaseUri()).thenReturn(mockUri);
    URL resourceUrl = CswEndpointTest.class.getResource("/record.xsd");
    URL resourceUrlDot = CswEndpointTest.class.getResource(".");
    when(mockBundle.getResource("record.xsd")).thenReturn(resourceUrl);
    when(mockBundle.getResource("csw/2.0.2/record.xsd")).thenReturn(resourceUrl);
    when(mockBundle.getResource("gmd/record_gmd.xsd")).thenReturn(resourceUrl);
    when(mockBundle.getResource(".")).thenReturn(resourceUrlDot);
    when(mockBundle.getBundleContext()).thenReturn(mockBundleContext);
    ServiceReference<QueryFilterTransformer> serviceReference = mock(ServiceReference.class);
    when(serviceReference.getProperty(
            QueryFilterTransformer.QUERY_FILTER_TRANSFORMER_TYPE_NAMES_FIELD))
        .thenReturn(ImmutableList.of(CswConstants.CSW_RECORD, THIRD_PARTY_TYPE_NAME));
    when(mockBundleContext.getServiceReferences(QueryFilterTransformer.class, null))
        .thenReturn(Collections.singletonList(serviceReference));

    when(mockCswActionTransformerProvider.getTransformer(anyString())).thenReturn(Optional.empty());
    csw =
        new CswEndpointStub(
            catalogFramework,
            mockMimeTypeManager,
            mockSchemaManager,
            mockInputManager,
            mockCswActionTransformerProvider,
            validator,
            queryFactory,
            mockBundle);
    csw.setUri(mockUriInfo);
    when(mockMimeTypeManager.getAvailableMimeTypes())
        .thenReturn(Arrays.asList(MediaType.APPLICATION_XML));
    when(mockSchemaManager.getAvailableSchemas())
        .thenReturn(new ArrayList<>(Arrays.asList(CswConstants.CSW_OUTPUT_SCHEMA)));
    when(mockSchemaManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA))
        .thenReturn(mockTransformer);
    when(mockInputManager.getAvailableIds()).thenReturn(Arrays.asList(CswConstants.CSW_RECORD));

    reset(catalogFramework);

    queryResponseBatch = getQueryResponseBatch(20, BATCH_TOTAL);

    QueryResponse[] qrRest =
        queryResponseBatch.subList(1, queryResponseBatch.size()).toArray(new QueryResponse[0]);
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenReturn(queryResponseBatch.get(0), qrRest);
    when(catalogFramework.getSourceIds())
        .thenReturn(new HashSet<>(Arrays.asList("source1", "source2", "source3")));
    CreateResponseImpl createResponse =
        new CreateResponseImpl(null, null, Arrays.asList(new MetacardImpl()));
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    QueryRequest queryRequest = mock(QueryRequest.class);
    Query query = mock(Query.class);
    when(query.getStartIndex()).thenReturn(1);
    when(query.getSortBy()).thenReturn(mock(SortBy.class));
    when(query.requestsTotalResultsCount()).thenReturn(true);
    when(query.getTimeoutMillis()).thenReturn(1L);
    when(queryRequest.getQuery()).thenReturn(query);

    when(queryFactory.getQuery(any(GetRecordsType.class))).thenReturn(queryRequest);
    when(queryFactory.getQuery(any(QueryConstraintType.class), anyString()))
        .thenReturn(queryRequest);
    when(queryFactory.updateQueryRequestTags(any(QueryRequest.class), anyString()))
        .thenReturn(queryRequest);
  }

  @Test
  public void testThirdPartyTypeNames() throws Exception {

    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }

    assertThat(
        countTypeNames(
            ct,
            CswConstants.DESCRIBE_RECORD,
            CswConstants.TYPE_NAME_PARAMETER,
            THIRD_PARTY_TYPE_NAME),
        is(1L));
    assertThat(
        countTypeNames(
            ct, CswConstants.GET_RECORDS, CswConstants.TYPE_NAMES_PARAMETER, THIRD_PARTY_TYPE_NAME),
        is(1L));
  }

  @Test
  public void testCapabilitiesRequestServiceProvider() {
    // Should only return the ServiceProvider section
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setSections(CswEndpoint.SERVICE_PROVIDER);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
    assertThat(ct.getServiceIdentification(), nullValue());
    verifyFilterCapabilities(ct);
    verifyServiceProvider(ct);
  }

  @Test
  public void testCapabilitiesRequestServiceIdentification() {
    // Should only return the ServiceIdentification section
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setSections(CswEndpoint.SERVICE_IDENTIFICATION);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
    verifyFilterCapabilities(ct);
    assertThat(ct.getServiceProvider(), nullValue());
    verifyServiceIdentification(ct);
  }

  @Test
  public void testCapabilitiesRequestOperationsMetadata() {
    // Should only return the OperationsMetadata section
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setSections(CswEndpoint.OPERATIONS_METADATA);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyFilterCapabilities(ct);
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getServiceProvider(), nullValue());
    verifyOperationsMetadata(ct);
  }

  @Test
  public void testCapabilitiesRequestFilterCapabilities() {
    // Should only return the Filter_Capabilities section
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setSections(CswEndpoint.FILTER_CAPABILITIES);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getServiceProvider(), nullValue());
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testCapabilitiesRequestAllSections() {
    // Should return all sections
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setSections(
        CswEndpoint.SERVICE_IDENTIFICATION
            + ","
            + CswEndpoint.SERVICE_PROVIDER
            + ","
            + CswEndpoint.OPERATIONS_METADATA
            + ","
            + CswEndpoint.FILTER_CAPABILITIES);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyServiceIdentification(ct);
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testCapabilitiesRequestBadSection() {
    // Shouldn't return any sections
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setSections("bad");

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getServiceProvider(), nullValue());
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testCapabilitiesRequestNoSections() {
    // Should return all sections
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyServiceIdentification(ct);
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testCapabilitiesRequestNoVersion() {
    // Should return all sections
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
    gcr.setAcceptVersions(null);
    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyServiceIdentification(ct);
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testCapabilitiesFederatedCatalogs() {
    GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gcr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    assertThat(ct.getOperationsMetadata(), notNullValue());
    for (Operation operation : ct.getOperationsMetadata().getOperation()) {
      if (StringUtils.equals(operation.getName(), CswConstants.GET_RECORDS)) {
        for (DomainType constraint : operation.getConstraint()) {
          if (StringUtils.equals(constraint.getName(), CswConstants.FEDERATED_CATALOGS)) {
            assertThat(constraint.getValue().size(), is(3));
            return;
          }
        }
      }
    }
    fail(
        "Didn't find ["
            + CswConstants.FEDERATED_CATALOGS
            + "] in request ["
            + CswConstants.GET_RECORDS
            + "]");
  }

  @Test
  public void testGetCapabilitiesTypeServiceIdentification() {
    // Should only return the ServiceIdentification section
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    SectionsType stv = new SectionsType();
    stv.setSection(Arrays.asList(CswEndpoint.SERVICE_IDENTIFICATION));
    gct.setSections(stv);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyServiceIdentification(ct);
    verifyFilterCapabilities(ct);
    assertThat(ct.getServiceProvider(), nullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
  }

  @Test
  public void testGetCapabilitiesTypeServiceProvider() {
    // Should only return the ServiceProvider section
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    SectionsType stv = new SectionsType();
    stv.setSection(Arrays.asList(CswEndpoint.SERVICE_PROVIDER));
    gct.setSections(stv);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
  }

  @Test
  public void testGetCapabilitiesTypeOperationsMetadata() {
    // Should only return the OperationsMetadata section
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    SectionsType stv = new SectionsType();
    stv.setSection(Arrays.asList(CswEndpoint.OPERATIONS_METADATA));
    gct.setSections(stv);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyFilterCapabilities(ct);
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getServiceProvider(), nullValue());
  }

  @Test
  public void testGetCapabilitiesTypeFilterCapabilities() {
    // Should only return the Filter_Capabilities section
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    SectionsType stv = new SectionsType();
    stv.setSection(Arrays.asList(CswEndpoint.FILTER_CAPABILITIES));
    gct.setSections(stv);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyFilterCapabilities(ct);
    assertThat(ct.getOperationsMetadata(), nullValue());
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getServiceProvider(), nullValue());
  }

  @Test
  public void testGetCapabilitiesTypeAllSections() {
    // Should return all sections
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    SectionsType stv = new SectionsType();
    stv.setSection(
        Arrays.asList(
            CswEndpoint.SERVICE_IDENTIFICATION,
            CswEndpoint.SERVICE_PROVIDER,
            CswEndpoint.OPERATIONS_METADATA,
            CswEndpoint.FILTER_CAPABILITIES));
    gct.setSections(stv);
    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyServiceIdentification(ct);
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testGetCapabilitiesTypeBadSection() {
    // Shouldn't return any sections
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    SectionsType stv = new SectionsType();
    stv.setSection(Arrays.asList("bad"));
    gct.setSections(stv);

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyFilterCapabilities(ct);
    assertThat(ct.getServiceIdentification(), nullValue());
    assertThat(ct.getServiceProvider(), nullValue());
    assertThat(ct.getOperationsMetadata(), nullValue());
  }

  @Test
  public void testGetCapabilitiesTypeNoSections() {
    // Should return all sections
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    gct.setSections(null);
    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyServiceIdentification(ct);
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testGetCapabilitiesTypeNoVersion() {
    // Should return all sections
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
    gct.setAcceptVersions(null);
    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    verifyOperationsMetadata(ct);
    verifyServiceIdentification(ct);
    verifyServiceProvider(ct);
    verifyFilterCapabilities(ct);
  }

  @Test
  public void testGetCapabilitiesTypeFederatedCatalogs() {
    GetCapabilitiesType gct = createDefaultGetCapabilitiesType();

    CapabilitiesType ct = null;
    try {
      ct = csw.getCapabilities(gct);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(ct, notNullValue());
    assertThat(ct.getOperationsMetadata(), notNullValue());
    for (Operation operation : ct.getOperationsMetadata().getOperation()) {
      if (StringUtils.equals(operation.getName(), CswConstants.GET_RECORDS)) {
        for (DomainType constraint : operation.getConstraint()) {
          if (StringUtils.equals(constraint.getName(), CswConstants.FEDERATED_CATALOGS)) {
            assertThat(constraint.getValue().size(), is(3));
            return;
          }
        }
      }
    }
    fail(
        "Didn't find ["
            + CswConstants.FEDERATED_CATALOGS
            + "] in request ["
            + CswConstants.GET_RECORDS
            + "]");
  }

  @Test
  public void testDescribeRecordRequestSingleTypePassed() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE);
    LOGGER.info("Resource directory is {}", this.getClass().getResource(".").getPath());
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("CswException caught during getCapabilities GET request: " + e.getMessage());
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    // Assert that it returned all record types.
    assertThat(drrt.getSchemaComponent().size(), is(1));
    LOGGER.info("got response \n{}\n", drrt.toString());
  }

  @Test
  public void testPostDescribeRecordRequestSingleTypePassed() {
    DescribeRecordType drt = createDefaultDescribeRecordType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    drt.setTypeName(typeNames);
    DescribeRecordResponseType drrt = null;

    try {
      drrt = csw.describeRecord(drt);
    } catch (CswException e) {
      fail("CswException caught during describeRecord POST request: " + e.getMessage());
    }

    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testPostDescribeRecordRequestGMDTypePassed() {
    DescribeRecordType drt = createDefaultDescribeRecordType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(
        new QName(
            GmdConstants.GMD_NAMESPACE, GmdConstants.GMD_LOCAL_NAME, GmdConstants.GMD_PREFIX));
    drt.setTypeName(typeNames);
    DescribeRecordResponseType drrt = null;

    try {
      drrt = csw.describeRecord(drt);
    } catch (CswException e) {
      fail("CswException caught during describeRecord POST request: " + e.getMessage());
    }

    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testDescribeRecordRequestNoTypesPassed() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    LOGGER.info("Resource directory is {}", this.getClass().getResource(".").getPath());
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("CswException caught during describeRecord GET request: " + e.getMessage());
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    // Assert that it returned all record types.
    assertThat(drrt.getSchemaComponent().size(), is(2));
    LOGGER.info("got response \n{}\n", drrt.toString());
  }

  @Test
  public void testPostDescribeRecordRequestNoTypesPassed() {
    // Should only return the ServiceProvider section

    DescribeRecordType request = createDefaultDescribeRecordType();

    LOGGER.info("Resource directory is {}", this.getClass().getResource(".").getPath());

    DescribeRecordResponseType drrt = null;

    try {
      drrt = csw.describeRecord(request);
    } catch (CswException e) {
      fail("CswException caught during describeRecord POST request: " + e.getMessage());
    }

    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    // Assert that it returned all record types.
    assertThat(drrt.getSchemaComponent().size(), is(2));

    LOGGER.info("got response \n{}\n", drrt.toString());
  }

  @Test
  public void testDescribeRecordRequestMultipleTypes() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + ",csw:test");
    DescribeRecordResponseType drrt = null;

    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("CswException caught during describeRecord GET request: " + e.getMessage());
    }

    // spec does not say specifically it should throw an exception,
    // and NSG interoperability tests require to skip the unknown ones, and
    // potentially return an empty list if none are known
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testPostDescribeRecordRequestMultipleTypes() {
    DescribeRecordType drt = createDefaultDescribeRecordType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, "test", VALID_PREFIX));
    drt.setTypeName(typeNames);
    DescribeRecordResponseType drrt = null;

    try {
      drrt = csw.describeRecord(drt);
    } catch (CswException e) {
      fail("CswException caught during describeRecord GET request: " + e.getMessage());
    }

    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testDescribeRecordRequestInvalidType() throws CswException {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + "," + VALID_PREFIX + ":" + BAD_TYPE);
    DescribeRecordResponseType response = csw.describeRecord(drr);

    // spec does not say specifically it should throw an exception,
    // and NSG interoperability tests require to skip the unknown ones, and
    // potentially return an empty list if none are known
    assertThat(response.getSchemaComponent().size(), is(1));
  }

  @Test
  public void testPostDescribeRecordRequestInvalidType() throws CswException {
    DescribeRecordType drt = createDefaultDescribeRecordType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, BAD_TYPE, VALID_PREFIX));
    drt.setTypeName(typeNames);
    DescribeRecordResponseType response = csw.describeRecord(drt);

    assertThat(response.getSchemaComponent().size(), is(1));
  }

  @Test
  public void testDescribeRecordSingleTypeSingleNamespaceNoPrefixes() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_TYPE);
    drr.setNamespace("xmlns(" + CswConstants.CSW_OUTPUT_SCHEMA + ")");
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("DescribeRecord failed with message '" + e.getMessage() + "'");
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testDescribeRecordSingleTypeSingleNamespaceNoPrefixesBadType() throws CswException {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(BAD_TYPE);
    drr.setNamespace("xmlns(" + CswConstants.CSW_OUTPUT_SCHEMA + ")");

    DescribeRecordResponseType response = csw.describeRecord(drr);

    assertThat(response.getSchemaComponent(), is(empty()));
  }

  @Test
  public void testDescribeRecordMultipleTypesMultipleNamespacesNominal() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + ",csw:test");
    drr.setNamespace("xmlns(" + VALID_PREFIX + "=" + CswConstants.CSW_OUTPUT_SCHEMA + ")");
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("DescribeRecord failed with message '" + e.getMessage() + "'");
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testDescribeRecordMultipleTypesMultipleNamespacesMultiplePrefixes() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + ",csw2:test4");
    drr.setNamespace(
        "xmlns("
            + VALID_PREFIX
            + "="
            + CswConstants.CSW_OUTPUT_SCHEMA
            + ")"
            + ","
            + "xmlns("
            + "csw2"
            + "="
            + CswConstants.CSW_OUTPUT_SCHEMA
            + "2"
            + ")");
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("DescribeRecord failed with message '" + e.getMessage() + "'");
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testDescribeRecordMultipleTypesMultipleNamespacesMultiplePrefixesMismatchedPrefix() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + ",csw3:test4");
    drr.setNamespace(
        "xmlns("
            + VALID_PREFIX
            + "="
            + CswConstants.CSW_OUTPUT_SCHEMA
            + ")"
            + ","
            + "xmlns("
            + "csw2"
            + "="
            + CswConstants.CSW_OUTPUT_SCHEMA
            + "2"
            + ")");
    try {
      csw.describeRecord(drr);
      fail("Should have thrown an exception indicating an invalid type.");
    } catch (CswException e) {
      LOGGER.info("Correctly got exception " + e.getMessage());
      return;
    }
    fail("Should have gotten exception.");
  }

  @Test(expected = CswException.class)
  public void testDescribeRecordUsePrefixNoNamespace() throws CswException {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE);
    drr.setNamespace(null);
    csw.describeRecord(drr);
  }

  @Test(expected = CswException.class)
  public void testDescribeRecordOnlyLocalPart() throws CswException {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_TYPE);
    drr.setNamespace(null);
    csw.describeRecord(drr);
  }

  @Test(expected = CswException.class)
  public void testDescribeRecordOnlyLocalPartMultipleTypes() throws CswException {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_TYPE + ",test,test2");
    drr.setNamespace(null);
    csw.describeRecord(drr);
  }

  @Test
  public void testDescribeRecordValidOutputFormat() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE);
    drr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("DescribeRecord failed with message '" + e.getMessage() + "'");
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testDescribeRecordValidSchemaLanguage() {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE);
    drr.setSchemaLanguage(CswConstants.SCHEMA_LANGUAGE_X_SCHEMA);
    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drr);
    } catch (CswException e) {
      fail("DescribeRecord failed with message '" + e.getMessage() + "'");
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testPostDescribeRecordValidSchemaLanguage() {
    DescribeRecordType drt = createDefaultDescribeRecordType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    drt.setTypeName(typeNames);
    drt.setSchemaLanguage(CswConstants.SCHEMA_LANGUAGE_X_SCHEMA);

    DescribeRecordResponseType drrt = null;
    try {
      drrt = csw.describeRecord(drt);
    } catch (CswException e) {
      fail("DescribeRecord failed with message '" + e.getMessage() + "'");
    }
    assertThat(drrt, notNullValue());
    assertThat(drrt.getSchemaComponent(), notNullValue());
    List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
    assertThat(schemaComponents.size(), is(1));
  }

  @Test
  public void testGetRecordsValidInput() throws CswException {
    GetRecordsRequest grr = createDefaultGetRecordsRequest();
    csw.getRecords(grr);
  }

  @Test(expected = CswException.class)
  public void testGetRecordsNullRequest() throws CswException {
    GetRecordsRequest grr = null;
    csw.getRecords(grr);
  }

  @Test
  public void testGetRecordsNoVersion() throws CswException {
    GetRecordsRequest grr = createDefaultGetRecordsRequest();
    grr.setVersion(null);

    csw.getRecords(grr);
  }

  @Test(expected = CswException.class)
  public void testGetRecordsInvalidTypeNames() throws CswException {
    GetRecordsRequest grr = createDefaultGetRecordsRequest();
    grr.setTypeNames(BAD_TYPES);

    csw.getRecords(grr);
  }

  @Test
  public void testPostGetRecordsValidInput() throws CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();
    csw.getRecords(grr);
  }

  @Test(expected = CswException.class)
  public void testPostGetRecordsNullRequest() throws CswException {
    GetRecordsType grr = null;
    csw.getRecords(grr);
  }

  /** Test Valid GetRecords request, no exceptions should be thrown */
  @Test
  public void testPostGetRecordsValidElementNames() throws CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);
    List<QName> elementNameList =
        Arrays.asList(new QName("brief"), new QName("summary"), new QName("full"));
    query.setElementName(elementNameList);
    grr.setAbstractQuery(jaxbQuery);

    csw.getRecords(grr);
  }

  @Test
  public void testPostGetRecordsValidElementSetNames() throws CswException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    QueryType query = new QueryType();

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);
    ElementSetNameType elsnt = new ElementSetNameType();
    elsnt.setValue(ElementSetType.BRIEF);
    query.setElementSetName(elsnt);
    grr.setAbstractQuery(jaxbQuery);

    csw.getRecords(grr);
  }

  @Test
  public void testPostGetRecordsResults()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    grr.setResultType(ResultType.RESULTS);
    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

    query.setConstraint(constraint);
    ElementSetNameType esnt = new ElementSetNameType();
    esnt.setValue(ElementSetType.SUMMARY);
    query.setElementSetName(esnt);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);
    final String exampleSchema = CswConstants.CSW_OUTPUT_SCHEMA;
    grr.setOutputSchema(exampleSchema);
    final String exampleMime = "application/xml";
    grr.setOutputFormat(exampleMime);

    CswRecordCollection collection = csw.getRecords(grr);

    assertThat(collection.getMimeType(), is(exampleMime));
    assertThat(collection.getOutputSchema(), is(exampleSchema));
    assertThat(collection.getSourceResponse(), notNullValue());
    assertThat(collection.getResultType(), is(ResultType.RESULTS));
    assertThat(collection.getElementSetType(), is(ElementSetType.SUMMARY));
  }

  @Test
  public void testPostGetRecordsGmdCswOutputSchema()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    grr.setResultType(ResultType.RESULTS);
    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(
        new QName(
            GmdConstants.GMD_NAMESPACE, GmdConstants.GMD_LOCAL_NAME, GmdConstants.GMD_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(GMD_CONTEXTUAL_LIKE_QUERY);

    query.setConstraint(constraint);
    ElementSetNameType esnt = new ElementSetNameType();
    esnt.setValue(ElementSetType.SUMMARY);
    query.setElementSetName(esnt);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);
    final String exampleSchema = CswConstants.CSW_OUTPUT_SCHEMA;
    grr.setOutputSchema(exampleSchema);
    final String exampleMime = "application/xml";
    grr.setOutputFormat(exampleMime);

    CswRecordCollection collection = csw.getRecords(grr);

    assertThat(collection.getMimeType(), is(exampleMime));
    assertThat(collection.getOutputSchema(), is(exampleSchema));
    assertThat(collection.getSourceResponse(), notNullValue());
    assertThat(collection.getResultType(), is(ResultType.RESULTS));
    assertThat(collection.getElementSetType(), is(ElementSetType.SUMMARY));
  }

  @Test
  public void testPostGetRecordsHits()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    grr.setResultType(ResultType.HITS);
    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    CswRecordCollection collection = csw.getRecords(grr);

    assertThat(collection.getCswRecords(), is(empty()));
    assertThat(collection.getResultType(), is(ResultType.HITS));
  }

  @Test
  public void testPostGetRecordsValidate()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException {
    GetRecordsType grr = createDefaultPostRecordsRequest();

    grr.setResultType(ResultType.VALIDATE);
    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
    query.setTypeNames(typeNames);
    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

    query.setConstraint(constraint);
    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);

    grr.setAbstractQuery(jaxbQuery);

    CswRecordCollection collection = csw.getRecords(grr);

    assertThat(collection.getCswRecords(), is(empty()));
    assertThat(collection.getNumberOfRecordsMatched(), is(0L));
    assertThat(collection.getNumberOfRecordsReturned(), is(0L));
  }

  @Test
  public void testGetRecordById()
      throws CswException, FederationException, SourceUnavailableException,
          UnsupportedQueryException {
    final GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
    getRecordByIdRequest.setId("123");
    getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordByIdRequest.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    getRecordByIdRequest.setElementSetName("full");

    final Metacard metacard = new MetacardImpl();

    final List<Result> mockResults = Collections.singletonList(new ResultImpl(metacard));
    final QueryResponseImpl queryResponse =
        new QueryResponseImpl(null, mockResults, mockResults.size());
    doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

    final CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdRequest, null);
    verifyCswRecordCollection(cswRecordCollection, metacard);

    assertThat(cswRecordCollection.getElementSetType(), is(ElementSetType.FULL));
  }

  @Test
  public void testPostGetRecordById()
      throws CswException, FederationException, SourceUnavailableException,
          UnsupportedQueryException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setId(Collections.singletonList("123,456"));
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordByIdType.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

    final Metacard metacard1 = new MetacardImpl();
    final Metacard metacard2 = new MetacardImpl();

    final List<Result> mockResults =
        Arrays.asList(new ResultImpl(metacard1), new ResultImpl(metacard2));
    final QueryResponse queryResponse =
        new QueryResponseImpl(null, mockResults, mockResults.size());
    doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

    final CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdType, null);
    verifyCswRecordCollection(cswRecordCollection, metacard1, metacard2);

    // "summary" is the default if none is specified in the request.
    assertThat(cswRecordCollection.getElementSetType(), is(ElementSetType.SUMMARY));
  }

  @Test
  public void testRetrieveProductGetRecordById()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
    getRecordByIdRequest.setId("123");
    getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdRequest.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    setUpMocksForProductRetrieval(true);

    CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdRequest, null);

    assertThat(cswRecordCollection.getResource(), is(notNullValue()));
  }

  @Test
  public void testRetrieveProductGetRecordByIdWithRange()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
    getRecordByIdRequest.setId("123");
    getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdRequest.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    setUpMocksForProductRetrieval(true);

    CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdRequest, RANGE_VALUE);
    assertThat(cswRecordCollection.getResource(), is(notNullValue()));
  }

  @Test(expected = CswException.class)
  public void testRetrieveProductGetRecordByIdWithInvalidRangeHeader()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
    getRecordByIdRequest.setId("123");
    getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdRequest.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    setUpMocksForProductRetrieval(true);

    csw.getRecordById(getRecordByIdRequest, "100");
  }

  @Test
  public void testPostRetrieveProductGetRecordById()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));
    setUpMocksForProductRetrieval(true);

    CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdType, null);

    assertThat(cswRecordCollection.getResource(), is(notNullValue()));
  }

  @Test
  public void testPostRetrieveProductGetRecordByIdWithRange()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));
    setUpMocksForProductRetrieval(true);

    CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdType, RANGE_VALUE);

    assertThat(cswRecordCollection.getResource(), is(notNullValue()));
  }

  @Test(expected = CswException.class)
  public void testPostRetrieveProductGetRecordByIdWithInvalidRange()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));
    setUpMocksForProductRetrieval(true);

    csw.getRecordById(getRecordByIdType, "100");
  }

  @Test
  public void testPostRetrieveProductGetRecordByIdWithNoMimeType()
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException, CswException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));
    setUpMocksForProductRetrieval(false);

    CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdType, null);

    assertThat(cswRecordCollection.getResource(), is(notNullValue()));
    assertThat(
        cswRecordCollection.getResource().getMimeType().toString(),
        is(MediaType.APPLICATION_OCTET_STREAM));
  }

  @Test(expected = CswException.class)
  public void testPostRetrieveProductGetRecordByIdWithNoResource()
      throws CswException, FederationException, SourceUnavailableException,
          UnsupportedQueryException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException {

    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));
    when(catalogFramework.getLocalResource(any(ResourceRequest.class)))
        .thenThrow(ResourceNotFoundException.class);

    csw.getRecordById(getRecordByIdType, null);
  }

  @Test(expected = CswException.class)
  public void testPostRetrieveProductGetRecordByIdWithMultiIds()
      throws CswException, FederationException, SourceUnavailableException,
          UnsupportedQueryException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Arrays.asList("123", "124"));

    csw.getRecordById(getRecordByIdType, null);
  }

  @Test(expected = CswException.class)
  public void testPostRetrieveProductGetRecordByIdIncorrectOutputFormat()
      throws CswException, FederationException, SourceUnavailableException,
          UnsupportedQueryException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordByIdType.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));

    csw.getRecordById(getRecordByIdType, null);
  }

  @Test(expected = CswException.class)
  public void testPostRetrieveProductGetRecordByIdIncorrectSchema()
      throws CswException, FederationException, SourceUnavailableException,
          UnsupportedQueryException, ResourceNotFoundException, IOException,
          ResourceNotSupportedException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_OCTET_STREAM);
    getRecordByIdType.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    getRecordByIdType.setId(Collections.singletonList("123"));

    csw.getRecordById(getRecordByIdType, null);
  }

  private void verifyCswRecordCollection(
      final CswRecordCollection cswRecordCollection, final Metacard... expectedRecords) {
    final SourceResponse response = cswRecordCollection.getSourceResponse();
    assertThat(response, notNullValue());

    final List<Result> results = response.getResults();
    assertThat(results.size(), is(expectedRecords.length));

    for (int i = 0; i < results.size(); ++i) {
      final Result result = results.get(i);
      assertThat(result, notNullValue());
      assertThat(result.getMetacard(), is(expectedRecords[i]));
    }

    final List<Metacard> cswRecordResults = cswRecordCollection.getCswRecords();
    assertThat(cswRecordResults.size(), is(expectedRecords.length));

    for (int i = 0; i < cswRecordResults.size(); ++i) {
      final Metacard metacard = cswRecordResults.get(i);
      assertThat(metacard, is(expectedRecords[i]));
    }

    assertThat(cswRecordCollection.isById(), is(true));
    assertThat(cswRecordCollection.getOutputSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
  }

  @Test(expected = CswException.class)
  public void testGetRecordByIdWithNoId() throws CswException {
    final GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
    getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordByIdRequest.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

    csw.getRecordById(getRecordByIdRequest, null);
  }

  @Test(expected = CswException.class)
  public void testPostGetRecordByWithIdNoId() throws CswException {
    final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
    getRecordByIdType.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordByIdType.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

    csw.getRecordById(getRecordByIdType, null);
  }

  @Test(expected = CswException.class)
  public void testGetUnknownService() throws CswException {
    CswRequest request = new CswRequest();
    csw.unknownService(request);
  }

  @Test(expected = CswException.class)
  public void testPostUnknownService() throws CswException {
    csw.unknownService();
  }

  @Test(expected = CswException.class)
  public void testGetUnknownOperation() throws CswException {
    CswRequest request = new CswRequest();
    csw.unknownOperation(request);
  }

  @Test(expected = CswException.class)
  public void testPostUnknownOperation() throws CswException {
    csw.unknownOperation();
  }

  @Test
  public void testGetRange() throws UnsupportedQueryException {
    String validOffset = "bytes=100-";
    String validRange = "bytes=200-3000";

    long bytesToSkipOffset = csw.getRange(validOffset);
    long bytesToSkipRange = csw.getRange(validRange);

    assertThat(bytesToSkipOffset, is(100L));
    assertThat(bytesToSkipRange, is(200L));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testGetRangeInvalidRangeHeader() throws UnsupportedQueryException {
    String invalidRange = "100";

    csw.getRange(invalidRange);
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testGetRangeInvalidRangeOffset() throws UnsupportedQueryException {
    String invalidRange = "bytes=100-200-300";

    csw.getRange(invalidRange);
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testGetRangeInvalidRangeOffsetNotNumeric() throws UnsupportedQueryException {
    String invalidRange = "bytes=NotNumeric";

    csw.getRange(invalidRange);
  }

  /** Tests to see that JAXB configuration is working */
  @Test
  public void testMarshallDescribeRecord() {

    DescribeRecordResponseType response = new DescribeRecordResponseType();

    List<SchemaComponentType> schemas = new ArrayList<>();

    SchemaComponentType schemaComponentType = new SchemaComponentType();
    schemas.add(schemaComponentType);
    response.setSchemaComponent(schemas);

    JAXBContext context;
    try {

      context =
          JAXBContext.newInstance(
              "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1");
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
      StringWriter sw = new StringWriter();

      JAXBElement<DescribeRecordResponseType> wrappedResponse =
          new JAXBElement<>(cswQnameOutPutSchema, DescribeRecordResponseType.class, response);

      marshaller.marshal(wrappedResponse, sw);

      LOGGER.info("Response: {}", sw.toString());

    } catch (JAXBException e) {
      fail("Could not marshall message, Error: " + e.getMessage());
    }
  }

  private void verifyMarshalResponse(
      TransactionResponseType response, String contextPath, QName qName) {
    // Verify the response will marshal
    try {
      JAXBContext context = JAXBContext.newInstance(contextPath);
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
      StringWriter sw = new StringWriter();

      JAXBElement<TransactionResponseType> wrappedResponse =
          new JAXBElement<>(qName, TransactionResponseType.class, response);

      marshaller.marshal(wrappedResponse, sw);

      LOGGER.info("Response: {}", sw.toString());

    } catch (JAXBException e) {
      fail("Could not marshal message, Error: " + e.getMessage());
    }
  }

  @Test
  public void testIngestTransaction()
      throws CswException, SourceUnavailableException, FederationException, IngestException {
    CswTransactionRequest request = new CswTransactionRequest();
    request
        .getInsertActions()
        .add(new InsertActionImpl(CswConstants.CSW_TYPE, null, Arrays.asList(new MetacardImpl())));

    TransactionResponseType response = csw.transaction(request);
    assertThat(response, notNullValue());
    assertThat(response.getInsertResult().isEmpty(), is(true));
    assertThat(response.getTransactionSummary(), notNullValue());
    TransactionSummaryType summary = response.getTransactionSummary();
    assertThat(summary.getTotalDeleted().intValue(), is(0));
    assertThat(summary.getTotalUpdated().intValue(), is(0));
    assertThat(summary.getTotalInserted().intValue(), is(1));

    verifyMarshalResponse(
        response,
        "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
        cswQnameOutPutSchema);
  }

  @Test
  public void testIngestVerboseTransaction()
      throws CswException, SourceUnavailableException, FederationException, IngestException {
    CswTransactionRequest request = new CswTransactionRequest();
    request
        .getInsertActions()
        .add(new InsertActionImpl(CswConstants.CSW_TYPE, null, Arrays.asList(new MetacardImpl())));
    request.setVerbose(true);

    TransactionResponseType response = csw.transaction(request);
    assertThat(response, notNullValue());
    assertThat(response.getInsertResult().size(), is(1));
    assertThat(response.getTransactionSummary(), notNullValue());
    TransactionSummaryType summary = response.getTransactionSummary();
    assertThat(summary.getTotalDeleted().intValue(), is(0));
    assertThat(summary.getTotalUpdated().intValue(), is(0));
    assertThat(summary.getTotalInserted().intValue(), is(1));

    String contextPath =
        StringUtils.join(
            new String[] {
              CswConstants.OGC_CSW_PACKAGE,
              CswConstants.OGC_FILTER_PACKAGE,
              CswConstants.OGC_GML_PACKAGE,
              CswConstants.OGC_OWS_PACKAGE
            },
            ":");
    verifyMarshalResponse(
        response, contextPath, new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.TRANSACTION));
  }

  @Test
  public void testDeleteTransaction()
      throws CswException, UnsupportedQueryException, SourceUnavailableException,
          FederationException, IngestException {
    DeleteType deleteType = mock(DeleteType.class);

    doReturn(CswConstants.CSW_RECORD).when(deleteType).getTypeName();
    doReturn("").when(deleteType).getHandle();

    QueryConstraintType queryConstraintType = new QueryConstraintType();
    queryConstraintType.setCqlText("title = \"foo\"");
    doReturn(queryConstraintType).when(deleteType).getConstraint();

    List<DeleteResponse> delBatch = getDelBatch(queryResponseBatch);

    DeleteResponse[] delRest = delBatch.subList(1, delBatch.size()).toArray(new DeleteResponse[0]);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(delBatch.get(0), delRest);

    DeleteAction deleteAction =
        new DeleteActionImpl(deleteType, DefaultCswRecordMap.getPrefixToUriMapping());

    CswTransactionRequest deleteRequest = new CswTransactionRequest();
    deleteRequest.getDeleteActions().add(deleteAction);
    deleteRequest.setVersion(CswConstants.VERSION_2_0_2);
    deleteRequest.setService(CswConstants.CSW);
    deleteRequest.setVerbose(false);

    TransactionResponseType response = csw.transaction(deleteRequest);
    assertThat(response, notNullValue());

    TransactionSummaryType summary = response.getTransactionSummary();
    assertThat(summary, notNullValue());

    assertThat(summary.getTotalDeleted().intValue(), is(BATCH_TOTAL));
    assertThat(summary.getTotalInserted().intValue(), is(0));
    assertThat(summary.getTotalUpdated().intValue(), is(0));

    verifyMarshalResponse(
        response,
        "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
        cswQnameOutPutSchema);
  }

  @Test
  public void testDeleteBatching() throws Exception {
    // configure query responses
    queryResponseBatch = getQueryResponseBatch(500, 800);

    QueryResponse[] qrRest =
        queryResponseBatch.subList(1, queryResponseBatch.size()).toArray(new QueryResponse[0]);
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenReturn(queryResponseBatch.get(0), qrRest);

    // configure delete responses
    DeleteType deleteType = mock(DeleteType.class);

    doReturn(CswConstants.CSW_RECORD).when(deleteType).getTypeName();
    doReturn("").when(deleteType).getHandle();

    QueryConstraintType queryConstraintType = new QueryConstraintType();
    queryConstraintType.setCqlText("title = \"foo\"");
    doReturn(queryConstraintType).when(deleteType).getConstraint();

    List<DeleteResponse> delBatch = getDelBatch(queryResponseBatch);

    DeleteResponse[] delRest = delBatch.subList(1, delBatch.size()).toArray(new DeleteResponse[0]);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(delBatch.get(0), delRest);

    DeleteAction deleteAction =
        new DeleteActionImpl(deleteType, DefaultCswRecordMap.getPrefixToUriMapping());

    CswTransactionRequest deleteRequest = new CswTransactionRequest();
    deleteRequest.getDeleteActions().add(deleteAction);

    TransactionResponseType response = csw.transaction(deleteRequest);
    assertThat(response.getTransactionSummary().getTotalDeleted().intValue(), equalTo(800));
    verify(catalogFramework, times(4)).query(any());
    verify(catalogFramework, times(2)).delete(any());
  }

  @Test
  public void testUpdateTransactionWithNewRecord()
      throws CswException, FederationException, IngestException, SourceUnavailableException,
          UnsupportedQueryException {
    List<Update> updatedMetacards = new ArrayList<>();
    updatedMetacards.add(new UpdateImpl(new MetacardImpl(), new MetacardImpl()));

    UpdateResponse updateResponse = new UpdateResponseImpl(null, null, updatedMetacards);
    doReturn(updateResponse).when(catalogFramework).update(any(UpdateRequest.class));

    MetacardImpl updatedMetacard = new MetacardImpl();
    updatedMetacard.setId("123");
    UpdateAction updateAction = new UpdateActionImpl(updatedMetacard, CswConstants.CSW_RECORD, "");

    CswTransactionRequest transactionRequest = new CswTransactionRequest();
    transactionRequest.getUpdateActions().add(updateAction);
    transactionRequest.setVersion(CswConstants.VERSION_2_0_2);
    transactionRequest.setService(CswConstants.CSW);
    transactionRequest.setVerbose(false);

    TransactionResponseType response = csw.transaction(transactionRequest);
    assertThat(response, notNullValue());

    TransactionSummaryType summary = response.getTransactionSummary();
    assertThat(summary, notNullValue());

    assertThat(summary.getTotalDeleted().intValue(), is(0));
    assertThat(summary.getTotalInserted().intValue(), is(0));
    assertThat(summary.getTotalUpdated().intValue(), is(1));

    verifyMarshalResponse(
        response,
        "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
        cswQnameOutPutSchema);

    ArgumentCaptor<UpdateRequest> updateRequestArgumentCaptor =
        ArgumentCaptor.forClass(UpdateRequest.class);

    verify(catalogFramework, times(1)).update(updateRequestArgumentCaptor.capture());

    UpdateRequest actualUpdateRequest = updateRequestArgumentCaptor.getValue();
    assertThat(actualUpdateRequest.getUpdates().size(), is(1));
    assertThat(actualUpdateRequest.getUpdates().get(0).getValue().getId(), is("123"));
  }

  @Test
  public void testUpdateTransactionWithConstraint()
      throws CswException, FederationException, IngestException, SourceUnavailableException,
          UnsupportedQueryException {
    List<Result> results = new ArrayList<>();

    MetacardImpl firstResult = new MetacardImpl();
    firstResult.setId("123");
    firstResult.setTitle("Title one");
    firstResult.setAttribute("subject", "Subject one");
    results.add(new ResultImpl(firstResult));

    MetacardImpl secondResult = new MetacardImpl();
    secondResult.setId("789");
    secondResult.setTitle("Title two");
    secondResult.setAttribute("subject", "Subject two");
    results.add(new ResultImpl(secondResult));

    QueryResponse queryResponse = new QueryResponseImpl(null, results, results.size());
    QueryResponse emptyResponse = new QueryResponseImpl(null, Collections.emptyList(), 0);
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenReturn(queryResponse)
        .thenReturn(emptyResponse);

    List<Update> updatedMetacards = new ArrayList<>();
    updatedMetacards.add(new UpdateImpl(new MetacardImpl(), new MetacardImpl()));
    updatedMetacards.add(new UpdateImpl(new MetacardImpl(), new MetacardImpl()));

    UpdateResponse updateResponse = new UpdateResponseImpl(null, null, updatedMetacards);
    doReturn(updateResponse).when(catalogFramework).update(any(UpdateRequest.class));

    Map<String, Serializable> recordProperties = new HashMap<>();
    recordProperties.put("title", "foo");
    recordProperties.put("subject", "bar");

    QueryConstraintType constraint = new QueryConstraintType();
    constraint.setCqlText("title = 'fake'");

    UpdateAction updateAction =
        new UpdateActionImpl(
            recordProperties,
            CswConstants.CSW_RECORD,
            "",
            constraint,
            DefaultCswRecordMap.getDefaultCswRecordMap().getPrefixToUriMapping());

    CswTransactionRequest updateRequest = new CswTransactionRequest();
    updateRequest.getUpdateActions().add(updateAction);
    updateRequest.setVersion(CswConstants.VERSION_2_0_2);
    updateRequest.setService(CswConstants.CSW);
    updateRequest.setVerbose(false);

    TransactionResponseType response = csw.transaction(updateRequest);
    assertThat(response, notNullValue());

    TransactionSummaryType summary = response.getTransactionSummary();
    assertThat(summary, notNullValue());

    assertThat(summary.getTotalDeleted().intValue(), is(0));
    assertThat(summary.getTotalInserted().intValue(), is(0));
    assertThat(summary.getTotalUpdated().intValue(), is(2));

    verifyMarshalResponse(
        response,
        "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
        cswQnameOutPutSchema);

    ArgumentCaptor<UpdateRequest> updateRequestArgumentCaptor =
        ArgumentCaptor.forClass(UpdateRequest.class);

    verify(catalogFramework, times(1)).update(updateRequestArgumentCaptor.capture());

    UpdateRequest actualUpdateRequest = updateRequestArgumentCaptor.getValue();

    List<Map.Entry<Serializable, Metacard>> updates = actualUpdateRequest.getUpdates();
    assertThat(updates.size(), is(2));

    Metacard firstUpdate = updates.get(0).getValue();
    assertThat(firstUpdate.getId(), is("123"));
    assertThat(firstUpdate.getTitle(), is("foo"));
    assertThat(firstUpdate.getAttribute("subject").getValue(), is("bar"));

    Metacard secondUpdate = updates.get(1).getValue();
    assertThat(secondUpdate.getId(), is("789"));
    assertThat(secondUpdate.getTitle(), is("foo"));
    assertThat(secondUpdate.getAttribute("subject").getValue(), is("bar"));
  }

  /**
   * Creates default GetCapabilities GET request, with no sections specified
   *
   * @return Vanilla GetCapabilitiesRequest object
   */
  private GetCapabilitiesRequest createDefaultGetCapabilitiesRequest() {
    GetCapabilitiesRequest gcr = new GetCapabilitiesRequest();
    gcr.setService(CswConstants.CSW);
    gcr.setAcceptVersions(CswConstants.VERSION_2_0_2);
    gcr.setRequest(CswConstants.GET_CAPABILITIES);
    return gcr;
  }

  /**
   * Creates default DescribeRecordRequest GET request, with no sections specified
   *
   * @return Vanilla DescribeRecordRequest object
   */
  private DescribeRecordRequest createDefaultDescribeRecordRequest() {
    DescribeRecordRequest drr = new DescribeRecordRequest();
    drr.setService(CswConstants.CSW);
    drr.setVersion(CswConstants.VERSION_2_0_2);
    drr.setRequest(CswConstants.DESCRIBE_RECORD);
    drr.setNamespace("xmlns(" + VALID_PREFIX + "=" + CswConstants.CSW_OUTPUT_SCHEMA + ")");
    return drr;
  }

  /**
   * Creates default GetRecordsRequest GET request, with no sections specified
   *
   * @return Vanilla valid GetRecordsRequest object
   */
  private GetRecordsRequest createDefaultGetRecordsRequest() {
    GetRecordsRequest grr = new GetRecordsRequest();
    grr.setService(CswConstants.CSW);
    grr.setVersion(CswConstants.VERSION_2_0_2);
    grr.setRequest(CswConstants.GET_RECORDS);
    grr.setNamespace(
        CswConstants.XMLNS_DEFINITION_PREFIX
            + CswConstants.CSW_NAMESPACE_PREFIX
            + CswConstants.EQUALS_CHAR
            + CswConstants.CSW_OUTPUT_SCHEMA
            + CswConstants.XMLNS_DEFINITION_POSTFIX
            + CswConstants.COMMA
            + CswConstants.XMLNS_DEFINITION_PREFIX
            + CswConstants.OGC_NAMESPACE_PREFIX
            + CswConstants.EQUALS_CHAR
            + CswConstants.OGC_SCHEMA
            + CswConstants.XMLNS_DEFINITION_POSTFIX
            + CswConstants.COMMA
            + CswConstants.XMLNS_DEFINITION_PREFIX
            + CswConstants.GML_NAMESPACE_PREFIX
            + CswConstants.EQUALS_CHAR
            + CswConstants.GML_SCHEMA
            + CswConstants.XMLNS_DEFINITION_POSTFIX
            + CswConstants.COMMA);

    grr.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
    grr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
    grr.setTypeNames(VALID_TYPES);
    return grr;
  }

  /**
   * Creates default GetRecordsType POST request, with no sections specified
   *
   * @return Vanilla valid GetRecordsType object
   */
  private GetRecordsType createDefaultPostRecordsRequest() {
    GetRecordsType grr = new GetRecordsType();

    grr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
    grr.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

    QueryType query = new QueryType();
    List<QName> typeNames = new ArrayList<>();
    typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));

    query.setTypeNames(typeNames);

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<>(cswQnameOutPutSchema, QueryType.class, query);
    grr.setAbstractQuery(jaxbQuery);
    return grr;
  }

  private DescribeRecordType createDefaultDescribeRecordType() {
    return new DescribeRecordType();
  }

  /**
   * Creates default GetCapabilities POST request, with no sections specified
   *
   * @return Vanilla GetCapabilitiesType object
   */
  private GetCapabilitiesType createDefaultGetCapabilitiesType() {
    GetCapabilitiesType gct = new GetCapabilitiesType();
    gct.setService(CswConstants.CSW);
    AcceptVersionsType avt = new AcceptVersionsType();
    avt.setVersion(CswEndpoint.SERVICE_TYPE_VERSION);
    gct.setAcceptVersions(avt);
    return gct;
  }

  /**
   * Helper method to verify the ServiceProvider section matches the endpoint's definition
   *
   * @param ct The CapabilitiesType to verify
   */
  private void verifyServiceProvider(CapabilitiesType ct) {
    ServiceProvider sp = ct.getServiceProvider();
    assertThat(sp.getProviderName(), is(CswEndpoint.PROVIDER_NAME));
  }

  /**
   * Helper method to verify the ServiceIdentification section matches the endpoint's definition
   *
   * @param ct The CapabilitiesType to verify
   */
  private void verifyServiceIdentification(CapabilitiesType ct) {
    ServiceIdentification si = ct.getServiceIdentification();
    assertThat(si.getTitle(), is(CswEndpoint.SERVICE_TITLE));
    assertThat(si.getAbstract(), is(CswEndpoint.SERVICE_ABSTRACT));
    assertThat(si.getServiceType().getValue(), is(CswConstants.CSW));
    assertThat(si.getServiceTypeVersion(), is(Arrays.asList(CswConstants.VERSION_2_0_2)));
  }

  /**
   * Helper method to verify the OperationsMetadata section matches the endpoint's definition
   *
   * @param ct The CapabilitiesType to verify
   */
  private void verifyOperationsMetadata(CapabilitiesType ct) {
    OperationsMetadata om = ct.getOperationsMetadata();
    List<Operation> opList = om.getOperation();
    ArrayList<String> opNames = new ArrayList<>();
    for (Operation op : opList) {
      opNames.add(op.getName());
      if (StringUtils.equals(CswConstants.TRANSACTION, op.getName())
          || StringUtils.equals(CswConstants.GET_RECORDS, op.getName())) {
        for (DomainType parameter : op.getParameter()) {
          if (StringUtils.equals(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER, parameter.getName())) {
            assertThat(
                parameter.getValue(),
                contains(
                    CswConstants.CONSTRAINT_LANGUAGE_FILTER, CswConstants.CONSTRAINT_LANGUAGE_CQL));
          } else if (StringUtils.equals(CswConstants.TYPE_NAMES_PARAMETER, parameter.getName())) {
            if (StringUtils.equals(op.getName(), CswConstants.TRANSACTION)) {
              assertThat(parameter.getValue(), contains(CswConstants.CSW_RECORD));
            } else {
              assertThat(
                  parameter.getValue(), hasItems(CswConstants.CSW_RECORD, THIRD_PARTY_TYPE_NAME));
            }
          }
        }
      }
    }
    assertThat(opNames.contains(CswConstants.GET_CAPABILITIES), is(true));
    assertThat(opNames.contains(CswConstants.DESCRIBE_RECORD), is(true));
    assertThat(opNames.contains(CswConstants.GET_RECORDS), is(true));
    assertThat(opNames.contains(CswConstants.GET_RECORD_BY_ID), is(true));
    assertThat(opNames.contains(CswConstants.TRANSACTION), is(true));
  }

  /**
   * Helper method to verify the FilterCapabilities section matches the endpoint's definition
   *
   * @param ct The CapabilitiesType to verify
   */
  private void verifyFilterCapabilities(CapabilitiesType ct) {
    FilterCapabilities fc = ct.getFilterCapabilities();

    assertThat(fc.getIdCapabilities(), notNullValue());
    assertThat(fc.getIdCapabilities().getEIDOrFID(), hasSize(1));

    assertThat(fc.getScalarCapabilities(), notNullValue());
    assertThat(
        CswEndpoint.COMPARISON_OPERATORS,
        hasSize(
            fc.getScalarCapabilities().getComparisonOperators().getComparisonOperator().size()));
    for (ComparisonOperatorType cot : CswEndpoint.COMPARISON_OPERATORS) {
      assertThat(
          fc.getScalarCapabilities().getComparisonOperators().getComparisonOperator(),
          hasItem(cot));
    }

    assertThat(fc.getSpatialCapabilities(), notNullValue());
    assertThat(
        CswEndpoint.SPATIAL_OPERATORS,
        hasSize(fc.getSpatialCapabilities().getSpatialOperators().getSpatialOperator().size()));
    for (SpatialOperatorType sot :
        fc.getSpatialCapabilities().getSpatialOperators().getSpatialOperator()) {
      assertThat(CswEndpoint.SPATIAL_OPERATORS, hasItem(sot.getName()));
    }
  }

  private QueryResponse getQueryResponse() {
    List<Result> results = new LinkedList<>();
    for (int i = 0; i < RESULT_COUNT; i++) {
      Result result = new ResultImpl();
      results.add(result);
    }
    return new QueryResponseImpl(null, results, TOTAL_COUNT);
  }

  private void setUpMocksForProductRetrieval(boolean includeMimeType)
      throws ResourceNotFoundException, IOException, ResourceNotSupportedException {
    ResourceResponse resourceResponse = mock(ResourceResponse.class);
    Resource resource = mock(Resource.class);
    if (includeMimeType) {
      MimeType mimeType = mock(MimeType.class);
      when(resource.getMimeType()).thenReturn(mimeType);
    }
    when(resourceResponse.getResource()).thenReturn(resource);
    when(catalogFramework.getLocalResource(any(ResourceRequest.class)))
        .thenReturn(resourceResponse);
  }

  private List<QueryResponse> getQueryResponseBatch(int batchSize, int total) {
    Queue<Result> results = new ArrayDeque<>();
    for (int i = 1; i <= total; i++) {
      MetacardImpl metacard = new MetacardImpl();
      metacard.setId(i + "");
      results.add(new ResultImpl(metacard));
    }

    List<QueryResponse> queryResponses = new ArrayList<>();
    while (!results.isEmpty()) {
      List<Result> batchList = new ArrayList<>();
      for (int i = 0; i < batchSize; i++) {
        Result result = results.poll();
        if (result == null) {
          break;
        }
        batchList.add(result);
      }
      queryResponses.add(new QueryResponseImpl(null, batchList, total));
    }

    // Add one empty response list to the end
    queryResponses.add(new QueryResponseImpl(null, Collections.emptyList(), 0));
    return queryResponses;
  }

  private List<DeleteResponse> getDelBatch(List<QueryResponse> qrBatch) {
    List<DeleteResponse> results = new ArrayList<>();
    List<Metacard> deletedMetacards =
        qrBatch
            .stream()
            .map(SourceResponse::getResults)
            .flatMap(Collection::stream)
            .map(Result::getMetacard)
            .collect(Collectors.toList());

    Iterables.partition(deletedMetacards, CswEndpoint.DEFAULT_BATCH)
        .forEach(metacards -> results.add(new DeleteResponseImpl(null, null, metacards)));

    return results;
  }

  private long countTypeNames(
      CapabilitiesType ct, String operationName, String parameterName, String typeName) {
    return ct.getOperationsMetadata()
        .getOperation()
        .stream()
        .filter(Objects::nonNull)
        .filter(operation -> operation.getName().equals(operationName))
        .map(Operation::getParameter)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .filter(domainType -> domainType.getName().equals(parameterName))
        .map(DomainType::getValue)
        .flatMap(Collection::stream)
        .filter(s -> s.equals(typeName))
        .count();
  }

  public static class CswEndpointStub extends CswEndpoint {

    private Bundle bundle;

    public CswEndpointStub(
        CatalogFramework ddf,
        TransformerManager mimeTypeManager,
        TransformerManager schemaManager,
        TransformerManager inputManager,
        CswActionTransformerProvider cswActionTransformerProvider,
        Validator validator,
        CswQueryFactory queryFactory,
        Bundle bundle) {
      super(
          ddf,
          mimeTypeManager,
          schemaManager,
          inputManager,
          cswActionTransformerProvider,
          validator,
          queryFactory);
      this.bundle = bundle;
    }

    @Override
    Bundle getBundle() {
      return bundle;
    }
  }
}
