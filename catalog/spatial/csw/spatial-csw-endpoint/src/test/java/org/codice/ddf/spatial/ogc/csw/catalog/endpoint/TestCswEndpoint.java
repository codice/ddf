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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.UpdateAction;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.styling.UomOgcMapping;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TEquals;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.EqualityExpressionBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.QueryResponseTransformer;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.DistributedSearchType;
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
import net.opengis.filter.v_1_1_0.BinaryComparisonOpType;
import net.opengis.filter.v_1_1_0.BinarySpatialOpType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.DistanceBufferType;
import net.opengis.filter.v_1_1_0.DistanceType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.LiteralType;
import net.opengis.filter.v_1_1_0.ObjectFactory;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SortPropertyType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.CoordType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PolygonType;
import net.opengis.ows.v_1_0_0.AcceptVersionsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import net.opengis.ows.v_1_0_0.SectionsType;
import net.opengis.ows.v_1_0_0.ServiceIdentification;
import net.opengis.ows.v_1_0_0.ServiceProvider;

public class TestCswEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCswEndpoint.class);

    private static final String BAD_VERSION = "4";

    private static final String BAD_TYPE = "MyBadType";

    private static final String VALID_TYPES = "csw:Record,csw:Record";

    private static final String BAD_TYPES = "unknown:Record,foo:Bar";

    private static final String VALID_TYPE = "Record";

    private static final String VALID_PREFIX = "csw";

    private static final String VALID_PREFIX_LOCAL_TYPE = VALID_PREFIX + ":" + VALID_TYPE;

    private static final String BAD_OUTPUT_FORMAT_XML = "application/xxx";

    private static final String BAD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XXX";

    private static final String CQL_BAD_QUERY = "bad query";

    private static final String CONTEXTUAL_TEST_ATTRIBUTE = "csw:title";

    private static final String SPATIAL_TEST_ATTRIBUTE = "location";

    private static final String CQL_FRAMEWORK_TEST_ATTRIBUTE = "title";

    private static final String TITLE_TEST_ATTRIBUTE = "dc:title";

    private static final String UNKNOWN_TEST_ATTRIBUTE = "unknownAttr";

    private static final String CQL_CONTEXTUAL_PATTERN = "some title";

    private static final String POLYGON_STR = "POLYGON((10 10, 10 25, 40 25, 40 10, 10 10))";

    private static final double REL_GEO_DISTANCE = 100;

    private static final String REL_GEO_UNITS = "kilometers";

    private static final double EXPECTED_GEO_DISTANCE = REL_GEO_DISTANCE * 1000;

    private static final String CQL_CONTEXTUAL_LIKE_QUERY =
            CONTEXTUAL_TEST_ATTRIBUTE + " Like '" + CQL_CONTEXTUAL_PATTERN + "'";

    private static final String CQL_FEDERATED_QUERY =
            "\"source-id\" = 'source1' AND " + CQL_CONTEXTUAL_LIKE_QUERY;

    private static final String CQL_SPATIAL_EQUALS_QUERY =
            "equals(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_DISJOINT_QUERY =
            "disjoint(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_INTERSECTS_QUERY =
            "intersects(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_TOUCHES_QUERY =
            "touches(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_CROSSES_QUERY =
            "crosses(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_WITHIN_QUERY =
            "within(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_CONTAINS_QUERY =
            "contains(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_OVERLAPS_QUERY =
            "overlaps(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ")";

    private static final String CQL_SPATIAL_DWITHIN_QUERY =
            "dwithin(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ", " + REL_GEO_DISTANCE
                    + ", " + REL_GEO_UNITS + ")";

    private static final String CQL_SPATIAL_BEYOND_QUERY =
            "beyond(" + SPATIAL_TEST_ATTRIBUTE + ", " + POLYGON_STR + ", " + REL_GEO_DISTANCE + ", "
                    + REL_GEO_UNITS + ")";

    private static final String TIMESTAMP = "2009-12-04T12:00:00Z";

    private static final String DURATION = "P40D";

    private static final String CQL_BEFORE = "before";

    private static final String CQL_AFTER = "after";

    private static final String CQL_DURING = "during";

    private static final String CQL_BEFORE_OR_DURING = "before or during";

    private static final String CQL_DURING_OR_AFTER = "during OR after";

    private static UriInfo mockUriInfo = mock(UriInfo.class);

    private static Bundle mockBundle = mock(Bundle.class);

    private static CswEndpoint csw;

    private static FilterBuilder filterBuilder = mock(FilterBuilder.class);

    private static CatalogFramework catalogFramework = mock(CatalogFramework.class);

    private static BundleContext mockContext = mock(BundleContext.class);

    private static Geometry polygon;

    private static net.opengis.gml.v_3_1_1.ObjectFactory gmlObjectFactory;

    private static ObjectFactory filterObjectFactory;

    private static TransformerManager mockMimeTypeManager = mock(TransformerManager.class);

    private static TransformerManager mockSchemaManager = mock(TransformerManager.class);

    private static TransformerManager mockInputManager = mock(TransformerManager.class);

    private static QueryResponseTransformer mockTransformer = mock(QueryResponseTransformer.class);

    private static QName cswQnameOutPutSchema = new QName(CswConstants.CSW_OUTPUT_SCHEMA);

    private static ArgumentCaptor<QueryRequest> argument;

    private static final long RESULT_COUNT = 10;

    private static final long TOTAL_COUNT = 10;

    @BeforeClass
    public static void setUpBeforeClass()
            throws URISyntaxException, SourceUnavailableException, UnsupportedQueryException,
            FederationException, ParseException, IngestException {
        URI mockUri = new URI("http://example.com/services/csw");
        when(mockUriInfo.getBaseUri()).thenReturn(mockUri);
        when(mockContext.getBundle()).thenReturn(mockBundle);
        URL resourceUrl = TestCswEndpoint.class.getResource("/record.xsd");
        URL resourceUrlDot = TestCswEndpoint.class.getClass().getResource(".");
        when(mockBundle.getResource("record.xsd")).thenReturn(resourceUrl);
        when(mockBundle.getResource("csw/2.0.2/record.xsd")).thenReturn(resourceUrl);
        when(mockBundle.getResource(".")).thenReturn(resourceUrlDot);

        AttributeBuilder attrBuilder = mock(AttributeBuilder.class);
        ExpressionBuilder exprBuilder = mock(ExpressionBuilder.class);
        ContextualExpressionBuilder likeExprBuilder = mock(ContextualExpressionBuilder.class);
        when(likeExprBuilder.text(Matchers.anyString())).thenReturn(Filter.INCLUDE);
        when(exprBuilder.like()).thenReturn(likeExprBuilder);
        when(exprBuilder.equalTo()).thenReturn(mock(EqualityExpressionBuilder.class));
        when(attrBuilder.is()).thenReturn(exprBuilder);
        when(filterBuilder.attribute(Metacard.ID)).thenReturn(attrBuilder);
        when(filterBuilder.anyOf(anyList())).thenReturn(mock(Or.class));
        csw = new CswEndpoint(mockContext, catalogFramework, filterBuilder, mockUriInfo,
                mockMimeTypeManager, mockSchemaManager, mockInputManager);

        polygon = new WKTReader().read(POLYGON_STR);
        gmlObjectFactory = new net.opengis.gml.v_3_1_1.ObjectFactory();
        filterObjectFactory = new ObjectFactory();
        when(mockMimeTypeManager.getAvailableMimeTypes())
                .thenReturn(Arrays.asList(MediaType.APPLICATION_XML));
        when(mockSchemaManager.getAvailableSchemas())
                .thenReturn(Arrays.asList(CswConstants.CSW_OUTPUT_SCHEMA));
        when(mockSchemaManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA))
                .thenReturn(mockTransformer);
        when(mockInputManager.getAvailableIds()).thenReturn(Arrays.asList(CswConstants.CSW_RECORD));
    }

    @org.junit.Before
    public void before()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException {
        QueryResponseImpl response = new QueryResponseImpl(null, new LinkedList<Result>(), 0);
        argument = ArgumentCaptor.forClass(QueryRequest.class);
        reset(catalogFramework);
        when(catalogFramework.query(argument.capture())).thenReturn(response);
        when(catalogFramework.getSourceIds())
                .thenReturn(new HashSet<>(Arrays.asList("source1", "source2", "source3")));
        CreateResponseImpl createResponse = new CreateResponseImpl(null, null,
                Arrays.<Metacard>asList(new MetacardImpl()));
        when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);
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
        assertNotNull(ct);
        assertNull(ct.getOperationsMetadata());
        assertNull(ct.getServiceIdentification());
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
        assertNotNull(ct);
        assertNull(ct.getOperationsMetadata());
        verifyFilterCapabilities(ct);
        assertNull(ct.getServiceProvider());
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
        assertNotNull(ct);
        verifyFilterCapabilities(ct);
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getServiceProvider());
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
        assertNotNull(ct);
        assertNull(ct.getOperationsMetadata());
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getServiceProvider());
        verifyFilterCapabilities(ct);
    }

    @Test
    public void testCapabilitiesRequestAllSections() {
        // Should return all sections
        GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
        gcr.setSections(
                CswEndpoint.SERVICE_IDENTIFICATION + "," + CswEndpoint.SERVICE_PROVIDER + ","
                        + CswEndpoint.OPERATIONS_METADATA + "," + CswEndpoint.FILTER_CAPABILITIES);

        CapabilitiesType ct = null;
        try {
            ct = csw.getCapabilities(gcr);
        } catch (CswException e) {
            fail("CswException caught during getCapabilities GET request: " + e.getMessage());
        }
        assertNotNull(ct);
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
        assertNotNull(ct);
        assertNull(ct.getOperationsMetadata());
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getServiceProvider());
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
        assertNotNull(ct);
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
        assertNotNull(ct);
        verifyOperationsMetadata(ct);
        verifyServiceIdentification(ct);
        verifyServiceProvider(ct);
        verifyFilterCapabilities(ct);
    }

    @Test(expected = CswException.class)
    public void testCapabilitiesRequestBadVersion() throws CswException {
        // Should throw an exception
        GetCapabilitiesRequest gcr = createDefaultGetCapabilitiesRequest();
        gcr.setAcceptVersions(CswConstants.VERSION_2_0_1);
        csw.getCapabilities(gcr);
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
        assertNotNull(ct);
        assertNotNull(ct.getOperationsMetadata());
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
        fail("Didn't find [" + CswConstants.FEDERATED_CATALOGS + "] in request ["
                + CswConstants.GET_RECORDS + "]");
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
        assertNotNull(ct);
        verifyServiceIdentification(ct);
        verifyFilterCapabilities(ct);
        assertNull(ct.getServiceProvider());
        assertNull(ct.getOperationsMetadata());
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
        assertNotNull(ct);
        verifyServiceProvider(ct);
        verifyFilterCapabilities(ct);
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getOperationsMetadata());
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
        assertNotNull(ct);
        verifyOperationsMetadata(ct);
        verifyFilterCapabilities(ct);
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getServiceProvider());
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
        assertNotNull(ct);
        verifyFilterCapabilities(ct);
        assertNull(ct.getOperationsMetadata());
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getServiceProvider());
    }

    @Test
    public void testGetCapabilitiesTypeAllSections() {
        // Should return all sections
        GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
        SectionsType stv = new SectionsType();
        stv.setSection(
                Arrays.asList(CswEndpoint.SERVICE_IDENTIFICATION, CswEndpoint.SERVICE_PROVIDER,
                        CswEndpoint.OPERATIONS_METADATA, CswEndpoint.FILTER_CAPABILITIES));
        gct.setSections(stv);
        CapabilitiesType ct = null;
        try {
            ct = csw.getCapabilities(gct);
        } catch (CswException e) {
            fail("CswException caught during getCapabilities GET request: " + e.getMessage());
        }
        assertNotNull(ct);
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
        assertNotNull(ct);
        verifyFilterCapabilities(ct);
        assertNull(ct.getServiceIdentification());
        assertNull(ct.getServiceProvider());
        assertNull(ct.getOperationsMetadata());
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
        assertNotNull(ct);
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
        assertNotNull(ct);
        verifyOperationsMetadata(ct);
        verifyServiceIdentification(ct);
        verifyServiceProvider(ct);
        verifyFilterCapabilities(ct);
    }

    @Test(expected = CswException.class)
    public void testGetCapabilitiesTypeBadVersion() throws CswException {
        // Should throw an exception
        GetCapabilitiesType gct = createDefaultGetCapabilitiesType();
        AcceptVersionsType badVersion = new AcceptVersionsType();
        badVersion.getVersion().add(CswConstants.VERSION_2_0_1);
        gct.setAcceptVersions(badVersion);
        csw.getCapabilities(gct);
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
        assertNotNull(ct);
        assertNotNull(ct.getOperationsMetadata());
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
        fail("Didn't find [" + CswConstants.FEDERATED_CATALOGS + "] in request ["
                + CswConstants.GET_RECORDS + "]");
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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        // Assert that it returned all record types.
        assertEquals(drrt.getSchemaComponent().size(), 1);
        LOGGER.info("got response \n{}\n", drrt.toString());

    }

    @Test
    public void testPostDescribeRecordRequestSingleTypePassed() {
        DescribeRecordType drt = createDefaultDescribeRecordType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        drt.setTypeName(typeNames);
        DescribeRecordResponseType drrt = null;

        try {
            drrt = csw.describeRecord(drt);
        } catch (CswException e) {
            fail("CswException caught during describeRecord POST request: " + e.getMessage());
        }

        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        // Assert that it returned all record types.
        assertEquals(drrt.getSchemaComponent().size(), 1);
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

        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        // Assert that it returned all record types.
        assertEquals(drrt.getSchemaComponent().size(), 1);

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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

    }

    @Test
    public void testPostDescribeRecordRequestMultipleTypes() {
        DescribeRecordType drt = createDefaultDescribeRecordType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, "test", VALID_PREFIX));
        drt.setTypeName(typeNames);
        DescribeRecordResponseType drrt = null;

        try {
            drrt = csw.describeRecord(drt);
        } catch (CswException e) {
            fail("CswException caught during describeRecord GET request: " + e.getMessage());
        }

        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

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
        List<QName> typeNames = new ArrayList<QName>();
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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(1, schemaComponents.size());

    }

    @Test
    public void testDescribeRecordMultipleTypesMultipleNamespacesMultiplePrefixes() {
        DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
        drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + ",csw2:test4");
        drr.setNamespace(
                "xmlns(" + VALID_PREFIX + "=" + CswConstants.CSW_OUTPUT_SCHEMA + ")" + "," +
                        "xmlns(" + "csw2" + "=" + CswConstants.CSW_OUTPUT_SCHEMA + "2" + ")");
        DescribeRecordResponseType drrt = null;
        try {
            drrt = csw.describeRecord(drr);
        } catch (CswException e) {
            fail("DescribeRecord failed with message '" + e.getMessage() + "'");

        }
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(1, schemaComponents.size());

    }

    @Test
    public void testDescribeRecordMultipleTypesMultipleNamespacesMultiplePrefixesMismatchedPrefix() {
        DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
        drr.setTypeName(VALID_PREFIX_LOCAL_TYPE + ",csw3:test4");
        drr.setNamespace(
                "xmlns(" + VALID_PREFIX + "=" + CswConstants.CSW_OUTPUT_SCHEMA + ")" + "," +
                        "xmlns(" + "csw2" + "=" + CswConstants.CSW_OUTPUT_SCHEMA + "2" + ")");
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
        DescribeRecordResponseType drrt = null;

        drrt = csw.describeRecord(drr);
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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

    }

    @Test
    public void testDescribeRecordInvalidOutputFormat() {
        DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
        drr.setTypeName(VALID_TYPE);
        drr.setNamespace(null);
        drr.setOutputFormat(BAD_OUTPUT_FORMAT_XML);
        try {
            csw.describeRecord(drr);
            fail("Should have thrown an exception indicating an invalid type.");
        } catch (CswException e) {
            LOGGER.info("Correctly got exception " + e.getMessage());
            assertEquals(e.getMessage(), "Invalid output format '" + BAD_OUTPUT_FORMAT_XML + "'");
            return;
        }
        fail("Should have gotten exception.");

    }

    @Test
    public void testPostDescribeRecordValidOutputFormat() {
        DescribeRecordType drt = createDefaultDescribeRecordType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        drt.setTypeName(typeNames);
        drt.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);

        DescribeRecordResponseType drrt = null;
        try {
            drrt = csw.describeRecord(drt);
        } catch (CswException e) {
            fail("DescribeRecord failed with message '" + e.getMessage() + "'");

        }
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

    }

    @Test
    public void testPostDescribeRecordInvalidOutputFormat() {
        DescribeRecordType drt = createDefaultDescribeRecordType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        drt.setTypeName(typeNames);
        drt.setOutputFormat(BAD_OUTPUT_FORMAT_XML);
        try {
            csw.describeRecord(drt);
            fail("Should have thrown an exception indicating an invalid type.");
        } catch (CswException e) {
            LOGGER.info("Correctly got exception " + e.getMessage());
            assertEquals(e.getMessage(), "Invalid output format '" + BAD_OUTPUT_FORMAT_XML + "'");
            return;
        }

        fail("Should have thrown exception");

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
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

    }

    @Test
    public void testDescribeRecordInvalidSchemaLanguage() {
        DescribeRecordRequest drr = createDefaultDescribeRecordRequest();
        drr.setTypeName(VALID_TYPE);
        drr.setNamespace(null);
        drr.setSchemaLanguage(BAD_SCHEMA_LANGUAGE);
        try {
            csw.describeRecord(drr);
            fail("Should have thrown an exception indicating an invalid type.");
        } catch (CswException e) {
            LOGGER.info("Correctly got exception " + e.getMessage());
            assertEquals(e.getMessage(), "Invalid schema language '" + BAD_SCHEMA_LANGUAGE + "'");
            return;
        }
        fail("Should have gotten exception.");

    }

    @Test
    public void testPostDescribeRecordValidSchemaLanguage() {
        DescribeRecordType drt = createDefaultDescribeRecordType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        drt.setTypeName(typeNames);
        drt.setSchemaLanguage(CswConstants.SCHEMA_LANGUAGE_X_SCHEMA);

        DescribeRecordResponseType drrt = null;
        try {
            drrt = csw.describeRecord(drt);
        } catch (CswException e) {
            fail("DescribeRecord failed with message '" + e.getMessage() + "'");

        }
        assertNotNull(drrt);
        assertNotNull(drrt.getSchemaComponent());
        List<SchemaComponentType> schemaComponents = drrt.getSchemaComponent();
        assertEquals(schemaComponents.size(), 1);

    }

    @Test
    public void testPostDescribeRecordInvalidSchemaLanguage() {
        DescribeRecordType drt = createDefaultDescribeRecordType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        drt.setTypeName(typeNames);
        drt.setSchemaLanguage(BAD_SCHEMA_LANGUAGE);
        try {
            csw.describeRecord(drt);
            fail("Should have thrown an exception indicating an invalid type.");
        } catch (CswException e) {
            LOGGER.info("Correctly got exception " + e.getMessage());
            assertEquals(e.getMessage(), "Invalid schema language '" + BAD_SCHEMA_LANGUAGE + "'");
            return;
        }

        fail("Should have thrown exception");

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
    public void testGetRecordsInvalidVersion() throws CswException {
        GetRecordsRequest grr = createDefaultGetRecordsRequest();
        grr.setVersion(BAD_VERSION);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testGetRecordsInvalidOutputFormat() throws CswException {
        GetRecordsRequest grr = createDefaultGetRecordsRequest();
        grr.setOutputFormat(BAD_OUTPUT_FORMAT_XML);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testGetRecordsInvalidOutputSchema() throws CswException {
        GetRecordsRequest grr = createDefaultGetRecordsRequest();
        grr.setOutputSchema(BAD_SCHEMA_LANGUAGE);

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

    @Test(expected = CswException.class)
    public void testPostGetRecordsInvalidOutputFormat() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();
        grr.setOutputFormat(BAD_OUTPUT_FORMAT_XML);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsInvalidSchemaFormat() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();
        grr.setOutputSchema(BAD_OUTPUT_FORMAT_XML);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsInvalidTypeNames() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, BAD_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    /**
     * Test Valid GetRecords request, no exceptions should be thrown
     */

    @Test
    public void testPostGetRecordsValidElementNames() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);
        List<QName> elementNameList = Arrays
                .asList(new QName("brief"), new QName("summary"), new QName("full"));
        query.setElementName(elementNameList);
        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsInvalidElementNames() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);
        List<QName> elementNameList = Arrays.asList(new QName("brief"), new QName("sas"));
        query.setElementName(elementNameList);
        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsInvalidElementSetNames() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        query.setElementSetName(new ElementSetNameType());
        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @Test
    public void testPostGetRecordsValidElementSetNames() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);
        ElementSetNameType elsnt = new ElementSetNameType();
        elsnt.setValue(ElementSetType.BRIEF);
        query.setElementSetName(elsnt);
        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsElementNamesMutex() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);
        List<QName> elementNameList = Arrays.asList(new QName("brief"));
        ElementSetNameType elsnt = new ElementSetNameType();
        elsnt.setValue(ElementSetType.BRIEF);
        query.setElementSetName(elsnt);
        query.setElementName(elementNameList);
        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsDistributedSearchNotSet()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        csw.getRecords(grr);
        assertThat(argument.getValue().isEnterprise(), is(false));
        assertThat(argument.getValue().getSourceIds(), anyOf(nullValue(), empty()));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = CswException.class)
    public void testPostGetRecordsEmptyFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setFilter(new FilterType());

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsDistributedSearchSetToOne()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        DistributedSearchType distributedSearch = new DistributedSearchType();
        distributedSearch.setHopCount(BigInteger.ONE);

        grr.setDistributedSearch(distributedSearch);

        csw.getRecords(grr);
        assertThat(argument.getValue().isEnterprise(), is(false));
        assertThat(argument.getValue().getSourceIds(), anyOf(nullValue(), empty()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsDistributedSearchSetToTen()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        DistributedSearchType distributedSearch = new DistributedSearchType();
        distributedSearch.setHopCount(BigInteger.TEN);

        grr.setDistributedSearch(distributedSearch);

        csw.getRecords(grr);
        assertThat(argument.getValue().isEnterprise(), is(true));
        assertThat(argument.getValue().getSourceIds(), anyOf(nullValue(), empty()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsDistributedSearchSpecificSources()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        DistributedSearchType distributedSearch = new DistributedSearchType();
        distributedSearch.setHopCount(BigInteger.TEN);

        grr.setDistributedSearch(distributedSearch);

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));

        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();

        constraint.setCqlText(CQL_FEDERATED_QUERY);

        query.setConstraint(constraint);

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);
        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        assertThat(argument.getValue().isEnterprise(), is(false));
        assertThat(argument.getValue().getSourceIds(), contains("source1"));
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsInvalidCQLQuery() throws CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(CQL_BAD_QUERY);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @Test
    public void testPostGetRecordsContextualCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        assertThat(frameworkQuery.getFilter(), instanceOf(PropertyIsLike.class));
        PropertyIsLike like = (PropertyIsLike) frameworkQuery.getFilter();
        assertThat(like.getLiteral(), is(CQL_CONTEXTUAL_PATTERN));
        assertThat(((AttributeExpressionImpl) like.getExpression()).getPropertyName(),
                is(CQL_FRAMEWORK_TEST_ATTRIBUTE));
    }

    @Test
    public void testPostGetRecordsResults()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        grr.setResultType(ResultType.RESULTS);
        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

        query.setConstraint(constraint);
        ElementSetNameType esnt = new ElementSetNameType();
        esnt.setValue(ElementSetType.SUMMARY);
        query.setElementSetName(esnt);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);
        final String EXAMPLE_SCHEMA = CswConstants.CSW_OUTPUT_SCHEMA;
        grr.setOutputSchema(EXAMPLE_SCHEMA);
        final String EXAMPLE_MIME = "application/xml";
        grr.setOutputFormat(EXAMPLE_MIME);

        when(catalogFramework.query(argument.capture())).thenReturn(getQueryResponse());

        CswRecordCollection collection = csw.getRecords(grr);

        assertThat(collection.getMimeType(), is(EXAMPLE_MIME));
        assertThat(collection.getOutputSchema(), is(EXAMPLE_SCHEMA));
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
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        when(catalogFramework.query(argument.capture())).thenReturn(getQueryResponse());

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
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(CQL_CONTEXTUAL_LIKE_QUERY);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        when(catalogFramework.query(argument.capture())).thenReturn(getQueryResponse());

        CswRecordCollection collection = csw.getRecords(grr);

        assertThat(collection.getCswRecords(), is(empty()));
        assertThat(collection.getNumberOfRecordsMatched(), is(0L));
        assertThat(collection.getNumberOfRecordsReturned(), is(0L));
    }

    @Test
    public void testPostGetRecordsValidSort()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        grr.setResultType(ResultType.RESULTS);
        QueryType query = new QueryType();

        SortByType incomingSort = new SortByType();
        SortPropertyType propType = new SortPropertyType();
        PropertyNameType propName = new PropertyNameType();
        propName.setContent(Arrays.asList((Object) TITLE_TEST_ATTRIBUTE));
        propType.setPropertyName(propName);
        incomingSort.getSortProperty().add(propType);
        query.setSortBy(incomingSort);

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);

        SortBy resultSort = argument.getValue().getQuery().getSortBy();

        assertThat(resultSort.getPropertyName().getPropertyName(),
                is(CQL_FRAMEWORK_TEST_ATTRIBUTE));
        assertThat(resultSort.getSortOrder(), is(SortOrder.ASCENDING));
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordsSortOnUnknownField()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        grr.setResultType(ResultType.RESULTS);
        QueryType query = new QueryType();

        SortByType incomingSort = new SortByType();
        SortPropertyType propType = new SortPropertyType();
        PropertyNameType propName = new PropertyNameType();
        propName.setContent(Arrays.asList((Object) UNKNOWN_TEST_ATTRIBUTE));
        propType.setPropertyName(propName);
        incomingSort.getSortProperty().add(propType);
        query.setSortBy(incomingSort);

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
    }

    @Test
    public void testPostGetRecordsSpatialEqualsCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Equals.class, CQL_SPATIAL_EQUALS_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialDisjointCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Disjoint.class, CQL_SPATIAL_DISJOINT_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialIntersectsCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Intersects.class, CQL_SPATIAL_INTERSECTS_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialTouchesCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Touches.class, CQL_SPATIAL_TOUCHES_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialCrossesCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Crosses.class, CQL_SPATIAL_CROSSES_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialWithinCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Within.class, CQL_SPATIAL_WITHIN_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialContainsCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Contains.class, CQL_SPATIAL_CONTAINS_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialOverlapsCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialQuery(Overlaps.class, CQL_SPATIAL_OVERLAPS_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialDWithinCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialRelativeQuery(DWithin.class, CQL_SPATIAL_DWITHIN_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialBeyondCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        cqlSpatialRelativeQuery(Beyond.class, CQL_SPATIAL_BEYOND_QUERY);
    }

    @Test
    public void testPostGetRecordsSpatialEqualsOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();

        ogcSpatialQuery(Equals.class, filterObjectFactory.createEquals(op));
    }

    @Test
    public void testPostGetRecordsSpatialDisjointOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Disjoint.class, filterObjectFactory.createDisjoint(op));
    }

    @Test
    public void testPostGetRecordsSpatialIntersectsOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Intersects.class, filterObjectFactory.createIntersects(op));
    }

    @Test
    public void testPostGetRecordsSpatialTouchesOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Touches.class, filterObjectFactory.createTouches(op));
    }

    @Test
    public void testPostGetRecordsSpatialCrossesOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Crosses.class, filterObjectFactory.createCrosses(op));
    }

    @Test
    public void testPostGetRecordsSpatialWithinOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Within.class, filterObjectFactory.createWithin(op));
    }

    @Test
    public void testPostGetRecordsSpatialContainsOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Contains.class, filterObjectFactory.createContains(op));
    }

    @Test
    public void testPostGetRecordsSpatialOverlapsOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinarySpatialOpType op = createBinarySpatialOpType();
        ogcSpatialQuery(Overlaps.class, filterObjectFactory.createOverlaps(op));
    }

    @Test
    public void testPostGetRecordsSpatialDWithinOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        DistanceBufferType op = createDistanceBufferType();
        ogcSpatialRelativeQuery(DWithin.class, filterObjectFactory.createDWithin(op));
    }

    @Test
    public void testPostGetRecordsSpatialBeyondOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        DistanceBufferType op = createDistanceBufferType();
        ogcSpatialRelativeQuery(Beyond.class, filterObjectFactory.createBeyond(op));
    }

    @Test
    public void testGetGetRecordsSpatialDWithinOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        String constraint = createDistanceBufferQuery("DWithin");
        ogcSpatialRelativeQuery(DWithin.class, constraint);
    }

    @Test
    public void testGetGetRecordsSpatialBeyondOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        String constraint = createDistanceBufferQuery("Beyond");
        ogcSpatialRelativeQuery(Beyond.class, constraint);
    }

    @Test
    public void testPostGetRecordsTemporalPropertyIsLessOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinaryComparisonOpType op = createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED,
                TIMESTAMP);
        ogcTemporalQuery(Metacard.CREATED, filterObjectFactory.createPropertyIsLessThan(op),
                Before.class);
    }

    @Ignore("TODO: the functions this test tests has been augmented to play well with the limited capabilities of the Solr provider.  "
            + "These tests and the functions they test should be reenabled and refactored after DDF-311 is addressed")
    @Test
    public void testPostGetRecordsTemporalPropertyIsLessOrEqualOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinaryComparisonOpType op = createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED,
                TIMESTAMP);
        ogcOrdTemporalQuery(Metacard.CREATED,
                filterObjectFactory.createPropertyIsLessThanOrEqualTo(op), BegunBy.class,
                TEquals.class);
    }

    @Ignore("TODO: the functions this test tests has been augmented to play well with the limited capabilities of the Solr provider.  "
            + "These tests and the functions they test should be reenabled and refactored after DDF-311 is addressed")
    @Test
    public void testPostGetRecordsTemporalPropertyIsGreaterOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinaryComparisonOpType op = createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED,
                TIMESTAMP);
        ogcTemporalQuery(Metacard.CREATED, filterObjectFactory.createPropertyIsGreaterThan(op),
                After.class);
    }

    @Ignore("TODO: the functions this test tests has been augmented to play well with the limited capabilities of the Solr provider.  "
            + "These tests and the functions they test should be reenabled and refactored after DDF-311 is addressed")
    @Test
    public void testPostGetRecordsTemporalPropertyIsGreaterOrEqualOgcFilter()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {
        BinaryComparisonOpType op = createTemporalBinaryComparisonOpType(CswConstants.CSW_CREATED,
                TIMESTAMP);
        ogcOrdTemporalQuery(Metacard.CREATED,
                filterObjectFactory.createPropertyIsGreaterThanOrEqualTo(op), After.class,
                TEquals.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsTemporalBeforeCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {

        String[] cqlTextValues = new String[] {CswConstants.CSW_CREATED, CQL_BEFORE, TIMESTAMP};
        String cqlText = StringUtils.join(cqlTextValues, " ");
        cqlTemporalQuery(Metacard.CREATED, cqlText, new Class[] {Before.class});
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsTemporalAfterCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {

        String[] cqlTextValues = new String[] {CswRecordMetacardType.CSW_ISSUED, CQL_AFTER,
                TIMESTAMP};
        String cqlText = StringUtils.join(cqlTextValues, " ");
        cqlTemporalQuery(Metacard.MODIFIED, cqlText, new Class[] {After.class});
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsTemporalDuringCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {

        String[] cqlTextValues = new String[] {CswRecordMetacardType.CSW_DATE_ACCEPTED, CQL_DURING,
                TIMESTAMP, "/", DURATION};
        String cqlText = StringUtils.join(cqlTextValues, " ");
        cqlTemporalQuery(Metacard.EFFECTIVE, cqlText, new Class[] {During.class});
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsTemporalBeforeOrDuringCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {

        String[] cqlTextValues = new String[] {CswRecordMetacardType.CSW_DATE, CQL_BEFORE_OR_DURING,
                TIMESTAMP, "/", DURATION};
        String cqlText = StringUtils.join(cqlTextValues, " ");
        cqlTemporalQuery(Metacard.MODIFIED, cqlText, new Class[] {Before.class, During.class});
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostGetRecordsTemporalAfterOrDuringCQLQuery()
            throws CswException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {

        String[] cqlTextValues = new String[] {CswRecordMetacardType.CSW_VALID, CQL_DURING_OR_AFTER,
                TIMESTAMP, "/", DURATION};
        String cqlText = StringUtils.join(cqlTextValues, " ");
        cqlTemporalQuery(Metacard.EXPIRATION, cqlText, new Class[] {During.class, After.class});
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

        final List<Result> mockResults = Collections
                .<Result>singletonList(new ResultImpl(metacard));
        final QueryResponseImpl queryResponse = new QueryResponseImpl(null, mockResults,
                mockResults.size());
        doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

        final CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdRequest);
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

        final List<Result> mockResults = Arrays
                .<Result>asList(new ResultImpl(metacard1), new ResultImpl(metacard2));
        final QueryResponse queryResponse = new QueryResponseImpl(null, mockResults,
                mockResults.size());
        doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

        final CswRecordCollection cswRecordCollection = csw.getRecordById(getRecordByIdType);
        verifyCswRecordCollection(cswRecordCollection, metacard1, metacard2);

        // "summary" is the default if none is specified in the request.
        assertThat(cswRecordCollection.getElementSetType(), is(ElementSetType.SUMMARY));
    }

    private void verifyCswRecordCollection(final CswRecordCollection cswRecordCollection,
            final Metacard... expectedRecords) {
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
    public void testGetRecordByIdNoId() throws CswException {
        final GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
        getRecordByIdRequest.setOutputFormat(MediaType.APPLICATION_XML);
        getRecordByIdRequest.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

        csw.getRecordById(getRecordByIdRequest);
    }

    @Test(expected = CswException.class)
    public void testPostGetRecordByIdNoId() throws CswException {
        final GetRecordByIdType getRecordByIdType = new GetRecordByIdType();
        getRecordByIdType.setOutputFormat(MediaType.APPLICATION_XML);
        getRecordByIdType.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);

        csw.getRecordById(getRecordByIdType);
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

    /**
     * Tests to see that JAXB configuration is working
     */
    @Test
    public void testMarshallDescribeRecord() {

        DescribeRecordResponseType response = new DescribeRecordResponseType();

        List<SchemaComponentType> schemas = new ArrayList<SchemaComponentType>();

        SchemaComponentType schemaComponentType = new SchemaComponentType();
        schemas.add(schemaComponentType);
        response.setSchemaComponent(schemas);

        JAXBContext context;
        try {

            context = JAXBContext.newInstance(
                    "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1");
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();

            JAXBElement<DescribeRecordResponseType> wrappedResponse = new JAXBElement<DescribeRecordResponseType>(
                    cswQnameOutPutSchema, DescribeRecordResponseType.class, response);

            marshaller.marshal(wrappedResponse, sw);

            LOGGER.info("\nResponse\n" + sw.toString() + "\n\n");

        } catch (JAXBException e) {
            fail("Could not marshall message, Error: " + e.getMessage());
        }

    }

    private void verifyMarshalResponse(TransactionResponseType response, String contextPath,
            QName qName) {
        // Verify the response will marshal
        try {
            JAXBContext context = JAXBContext.newInstance(contextPath);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();

            JAXBElement<TransactionResponseType> wrappedResponse = new JAXBElement<>(qName,
                    TransactionResponseType.class, response);

            marshaller.marshal(wrappedResponse, sw);

            LOGGER.info("\nResponse\n" + sw.toString() + "\n\n");

        } catch (JAXBException e) {
            fail("Could not marshal message, Error: " + e.getMessage());
        }
    }

    @Test
    public void testIngestTransaction()
            throws CswException, SourceUnavailableException, FederationException, IngestException {
        CswTransactionRequest request = new CswTransactionRequest();
        request.getInsertActions().add(new InsertAction(CswConstants.CSW_TYPE, null,
                Arrays.<Metacard>asList(new MetacardImpl())));

        TransactionResponseType response = csw.transaction(request);
        assertThat(response, notNullValue());
        assertThat(response.getInsertResult().isEmpty(), is(true));
        assertThat(response.getTransactionSummary(), notNullValue());
        TransactionSummaryType summary = response.getTransactionSummary();
        assertThat(summary.getTotalDeleted().intValue(), is(0));
        assertThat(summary.getTotalUpdated().intValue(), is(0));
        assertThat(summary.getTotalInserted().intValue(), is(1));

        verifyMarshalResponse(response,
                "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
                cswQnameOutPutSchema);
    }

    @Test
    public void testIngestVerboseTransaction()
            throws CswException, SourceUnavailableException, FederationException, IngestException {
        CswTransactionRequest request = new CswTransactionRequest();
        request.getInsertActions().add(new InsertAction(CswConstants.CSW_TYPE, null,
                Arrays.<Metacard>asList(new MetacardImpl())));
        request.setVerbose(true);

        TransactionResponseType response = csw.transaction(request);
        assertThat(response, notNullValue());
        assertThat(response.getInsertResult().size(), is(1));
        assertThat(response.getTransactionSummary(), notNullValue());
        TransactionSummaryType summary = response.getTransactionSummary();
        assertThat(summary.getTotalDeleted().intValue(), is(0));
        assertThat(summary.getTotalUpdated().intValue(), is(0));
        assertThat(summary.getTotalInserted().intValue(), is(1));

        String contextPath = StringUtils
                .join(new String[] {CswConstants.OGC_CSW_PACKAGE, CswConstants.OGC_FILTER_PACKAGE,
                        CswConstants.OGC_GML_PACKAGE, CswConstants.OGC_OWS_PACKAGE}, ":");
        verifyMarshalResponse(response, contextPath,
                new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.TRANSACTION));
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

        List<Result> results = new ArrayList<>();
        results.add(new ResultImpl(new MetacardImpl()));
        results.add(new ResultImpl(new MetacardImpl()));

        QueryResponse queryResponse = new QueryResponseImpl(null, results, results.size());

        doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

        List<Metacard> deletedMetacards = new ArrayList<>();
        deletedMetacards.add(new MetacardImpl());
        deletedMetacards.add(new MetacardImpl());

        DeleteResponse deleteResponse = new DeleteResponseImpl(null, null, deletedMetacards);
        doReturn(deleteResponse).when(catalogFramework).delete(any(DeleteRequest.class));

        DeleteAction deleteAction = new DeleteAction(deleteType,
                DefaultCswRecordMap.getDefaultCswRecordMap().getPrefixToUriMapping());

        CswTransactionRequest deleteRequest = new CswTransactionRequest();
        deleteRequest.getDeleteActions().add(deleteAction);
        deleteRequest.setVersion(CswConstants.VERSION_2_0_2);
        deleteRequest.setService(CswConstants.CSW);
        deleteRequest.setVerbose(false);

        TransactionResponseType response = csw.transaction(deleteRequest);
        assertThat(response, notNullValue());

        TransactionSummaryType summary = response.getTransactionSummary();
        assertThat(summary, notNullValue());

        assertThat(summary.getTotalDeleted().intValue(), is(2));
        assertThat(summary.getTotalInserted().intValue(), is(0));
        assertThat(summary.getTotalUpdated().intValue(), is(0));

        verifyMarshalResponse(response,
                "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
                cswQnameOutPutSchema);
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
        UpdateAction updateAction = new UpdateAction(updatedMetacard, CswConstants.CSW_RECORD, "");

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

        verifyMarshalResponse(response,
                "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
                cswQnameOutPutSchema);

        ArgumentCaptor<UpdateRequest> updateRequestArgumentCaptor = ArgumentCaptor
                .forClass(UpdateRequest.class);

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
        doReturn(queryResponse).when(catalogFramework).query(any(QueryRequest.class));

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

        UpdateAction updateAction = new UpdateAction(recordProperties, CswConstants.CSW_RECORD, "",
                constraint, DefaultCswRecordMap.getDefaultCswRecordMap().getPrefixToUriMapping());

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

        verifyMarshalResponse(response,
                "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1",
                cswQnameOutPutSchema);

        ArgumentCaptor<UpdateRequest> updateRequestArgumentCaptor = ArgumentCaptor
                .forClass(UpdateRequest.class);

        verify(catalogFramework, times(1)).update(updateRequestArgumentCaptor.capture());

        UpdateRequest actualUpdateRequest = updateRequestArgumentCaptor.getValue();

        List<Map.Entry<Serializable, Metacard>> updates = actualUpdateRequest.getUpdates();
        assertThat(updates.size(), is(2));

        Metacard firstUpdate = updates.get(0).getValue();
        assertThat(firstUpdate.getId(), is("123"));
        assertThat(firstUpdate.getTitle(), is("foo"));
        assertThat((String) firstUpdate.getAttribute("subject").getValue(), is("bar"));

        Metacard secondUpdate = updates.get(1).getValue();
        assertThat(secondUpdate.getId(), is("789"));
        assertThat(secondUpdate.getTitle(), is("foo"));
        assertThat((String) secondUpdate.getAttribute("subject").getValue(), is("bar"));
    }

    /**
     * Runs a binary Spatial CQL Query, verifying that the right filter class is generated based on CQL
     *
     * @param clz Class of filter to generate
     * @param cql CQL Query String
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    private <N extends BinarySpatialOperator> void cqlSpatialQuery(Class<N> clz, String cql)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(cql);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        assertThat(frameworkQuery.getFilter(), instanceOf(clz));
        @SuppressWarnings("unchecked")
        N spatial = (N) frameworkQuery.getFilter();
        assertThat((Polygon) ((LiteralExpressionImpl) spatial.getExpression2()).getValue(),
                is(polygon));

        assertThat(((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
                is(SPATIAL_TEST_ATTRIBUTE));
    }

    /**
     * Runs a relative spatial CQL Query, verifying that the right filter class is generated based on CQL
     *
     * @param clz Class of filter to generate
     * @param cql CQL Query String
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    private <N extends DistanceBufferOperator> void cqlSpatialRelativeQuery(Class<N> clz,
            String cql)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(cql);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        assertThat(frameworkQuery.getFilter(), instanceOf(clz));
        @SuppressWarnings("unchecked")
        N spatial = (N) frameworkQuery.getFilter();
        assertThat((Polygon) ((LiteralExpressionImpl) spatial.getExpression2()).getValue(),
                is(polygon));

        assertThat(((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
                is(SPATIAL_TEST_ATTRIBUTE));

        assertThat(spatial.getDistanceUnits(), is(UomOgcMapping.METRE.name()));
        assertThat(spatial.getDistance(), is(EXPECTED_GEO_DISTANCE));
    }

    private BinaryComparisonOpType createTemporalBinaryComparisonOpType(String attr,
            String comparison) {
        BinaryComparisonOpType comparisonOp = new BinaryComparisonOpType();

        PropertyNameType propName = new PropertyNameType();
        propName.getContent().add(attr);

        comparisonOp.getExpression().add(filterObjectFactory.createPropertyName(propName));

        LiteralType literal = new LiteralType();
        literal.getContent().add(comparison);

        comparisonOp.getExpression().add(filterObjectFactory.createLiteral(literal));
        return comparisonOp;
    }

    private BinarySpatialOpType createBinarySpatialOpType() {
        BinarySpatialOpType binarySpatialOps = new BinarySpatialOpType();

        PropertyNameType propName = new PropertyNameType();
        propName.getContent().add(SPATIAL_TEST_ATTRIBUTE);
        binarySpatialOps.setPropertyName(propName);

        binarySpatialOps.setGeometry(createPolygon());
        return binarySpatialOps;
    }

    private String createDistanceBufferQuery(String comparison) {
        String query =
                "      <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">"
                        +
                        "        <ogc:" + comparison + ">" +
                        "          <ogc:PropertyName>" + SPATIAL_TEST_ATTRIBUTE
                        + "</ogc:PropertyName>" +
                        "          <gml:Polygon gml:id=\"Pl001\">" +
                        "            <gml:exterior>" +
                        "              <gml:LinearRing>" +
                        "                <gml:pos>10 10</gml:pos>" +
                        "                <gml:pos>10 25</gml:pos>" +
                        "                <gml:pos>40 25</gml:pos>" +
                        "                <gml:pos>40 10</gml:pos>" +
                        "                <gml:pos>10 10</gml:pos>" +
                        "              </gml:LinearRing>" +
                        "            </gml:exterior>" +
                        "          </gml:Polygon>" +
                        "          <ogc:Distance units=\"" + REL_GEO_UNITS + "\">"
                        + REL_GEO_DISTANCE + "</ogc:Distance>" +
                        "        </ogc:" + comparison + ">" +
                        "      </ogc:Filter>";

        return query;
    }

    private DistanceBufferType createDistanceBufferType() {
        DistanceBufferType distanceBuffer = new DistanceBufferType();

        PropertyNameType propName = new PropertyNameType();
        propName.getContent().add(SPATIAL_TEST_ATTRIBUTE);
        distanceBuffer.setPropertyName(propName);

        DistanceType distance = filterObjectFactory.createDistanceType();
        distance.setUnits(REL_GEO_UNITS);
        distance.setContent(Double.toString(REL_GEO_DISTANCE));

        distanceBuffer.setDistance(distance);
        distanceBuffer.setGeometry(createPolygon());
        return distanceBuffer;
    }

    private JAXBElement<AbstractGeometryType> createPolygon() {
        PolygonType localPolygon = new PolygonType();

        LinearRingType ring = new LinearRingType();
        for (Coordinate coordinate : polygon.getCoordinates()) {
            CoordType coord = new CoordType();
            coord.setX(BigDecimal.valueOf(coordinate.x));
            coord.setY(BigDecimal.valueOf(coordinate.y));
            if (!Double.isNaN(coordinate.z)) {
                coord.setZ(BigDecimal.valueOf(coordinate.z));
            }
            ring.getCoord().add(coord);
        }
        AbstractRingPropertyType abstractRing = new AbstractRingPropertyType();
        abstractRing.setRing(gmlObjectFactory.createLinearRing(ring));
        localPolygon.setExterior(gmlObjectFactory.createExterior(abstractRing));

        JAXBElement<AbstractGeometryType> agt = new JAXBElement<AbstractGeometryType>(
                new QName("http://www.opengis.net/gml", "Polygon"), AbstractGeometryType.class,
                null, localPolygon);
        return agt;
    }

    /**
     * Runs a binary Spatial OGC Query, verifying that the right filter class is generated based on OGC Filter
     *
     * @param constraint The OGC Filter Constraint as an XML string
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    private <N extends DistanceBufferOperator> void ogcSpatialRelativeQuery(Class<N> clz,
            String constraint)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsRequest grr = createDefaultGetRecordsRequest();

        grr.setConstraintLanguage("FILTER");
        grr.setConstraint(constraint);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        assertThat(frameworkQuery.getFilter(), instanceOf(clz));
        @SuppressWarnings("unchecked")
        N spatial = (N) frameworkQuery.getFilter();
        assertThat((Polygon) ((LiteralExpressionImpl) spatial.getExpression2()).getValue(),
                is(polygon));

        assertThat(((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
                is(SPATIAL_TEST_ATTRIBUTE));
    }

    /**
     * Runs a binary Spatial OGC Query, verifying that the right filter class is generated based on OGC Filter
     *
     * @param spatialOps BinarySpatialOps query
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    private <N extends DistanceBufferOperator> void ogcSpatialRelativeQuery(Class<N> clz,
            JAXBElement<DistanceBufferType> spatialOps)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        FilterType filter = new FilterType();
        filter.setSpatialOps(spatialOps);

        constraint.setFilter(filter);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        assertThat(frameworkQuery.getFilter(), instanceOf(clz));
        @SuppressWarnings("unchecked")
        N spatial = (N) frameworkQuery.getFilter();
        assertThat((Polygon) ((LiteralExpressionImpl) spatial.getExpression2()).getValue(),
                is(polygon));

        assertThat(((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
                is(SPATIAL_TEST_ATTRIBUTE));
    }

    /**
     * Runs a binary Spatial OGC Query, verifying that the right filter class is generated based on OGC Filter
     *
     * @param spatialOps BinarySpatialOps query
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    private <N extends BinarySpatialOperator> void ogcSpatialQuery(Class<N> clz,
            JAXBElement<BinarySpatialOpType> spatialOps)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        FilterType filter = new FilterType();
        filter.setSpatialOps(spatialOps);

        constraint.setFilter(filter);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        assertThat(frameworkQuery.getFilter(), instanceOf(clz));
        @SuppressWarnings("unchecked")
        N spatial = (N) frameworkQuery.getFilter();
        assertThat((Polygon) ((LiteralExpressionImpl) spatial.getExpression2()).getValue(),
                is(polygon));

        assertThat(((AttributeExpressionImpl) spatial.getExpression1()).getPropertyName(),
                is(SPATIAL_TEST_ATTRIBUTE));
    }

    /**
     * Runs a binary Temporal OGC Query, verifying that the right filter class is generated based on
     * OGC Filter
     *
     * @param expectedAttr Exprected Mapped Attribute
     * @param temporalOps  The Temporal query, in terms of a binary comparison
     * @param clz          the Expected Class result
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    @SuppressWarnings("unchecked")
    private <N extends BinaryTemporalOperator> void ogcTemporalQuery(String expectedAttr,
            JAXBElement<BinaryComparisonOpType> temporalOps, Class<N> clz)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        Filter filter = generateTemporalFilter(temporalOps);

        assertThat(filter, instanceOf(clz));

        N temporal = (N) filter;
        assertThat(((AttributeExpressionImpl) temporal.getExpression1()).getPropertyName(),
                is(expectedAttr));
    }

    /**
     * Runs an Or'd query of multiple binary Temporal OGC Query, verifying that the right filter
     * class is generated based on OGC Filter
     *
     * @param expectedAttr Exprected Mapped Attribute
     * @param temporalOps  The Temporal query, in terms of a binary comparison
     * @param clzzes       the Expected Class result
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     * @throws CswException
     */
    @SuppressWarnings("unchecked")
    private void ogcOrdTemporalQuery(String expectedAttr,
            JAXBElement<BinaryComparisonOpType> temporalOps,
            Class<? extends BinaryTemporalOperator>... clzzes)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        Filter filter = generateTemporalFilter(temporalOps);

        assertThat(filter, instanceOf(Or.class));

        Or ordTemporal = (Or) filter;

        List<Filter> temporalFilters = ordTemporal.getChildren();

        List<Class<? extends BinaryTemporalOperator>> classes = new ArrayList<Class<? extends BinaryTemporalOperator>>();

        for (Filter temporal : temporalFilters) {
            assertThat(temporal, instanceOf(BinaryTemporalOperator.class));
            classes.add((Class<? extends BinaryTemporalOperator>) temporal.getClass());
        }
    }

    private Filter generateTemporalFilter(JAXBElement<BinaryComparisonOpType> temporalOps)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        FilterType filter = new FilterType();
        filter.setComparisonOps(temporalOps);

        constraint.setFilter(filter);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        return frameworkQuery.getFilter();
    }

    @SuppressWarnings("unchecked")
    private <N extends BinaryTemporalOperator> void cqlTemporalQuery(String expectedAttr,
            String cqlSpatialDwithinQuery, Class<N>[] classes)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            CswException {
        GetRecordsType grr = createDefaultPostRecordsRequest();

        QueryType query = new QueryType();
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));
        query.setTypeNames(typeNames);
        QueryConstraintType constraint = new QueryConstraintType();
        constraint.setCqlText(cqlSpatialDwithinQuery);

        query.setConstraint(constraint);
        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);

        grr.setAbstractQuery(jaxbQuery);

        csw.getRecords(grr);
        QueryImpl frameworkQuery = (QueryImpl) argument.getValue().getQuery();
        N temporal = null;
        if (classes.length > 1) {
            assertThat(frameworkQuery.getFilter(), instanceOf(Or.class));
            int i = 0;
            for (Filter filter : ((Or) frameworkQuery.getFilter()).getChildren()) {
                assertThat(filter, instanceOf(classes[i++]));
                temporal = (N) filter;
            }
        } else {
            assertThat(frameworkQuery.getFilter(), instanceOf(classes[0]));
            temporal = (N) frameworkQuery.getFilter();
        }
        assertThat(((AttributeExpressionImpl) temporal.getExpression1()).getPropertyName(),
                is(expectedAttr));
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
        grr.setNamespace(CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.CSW_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.CSW_OUTPUT_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA

                + CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.OGC_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.OGC_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA

                + CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.GML_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.GML_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA);

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
        List<QName> typeNames = new ArrayList<QName>();
        typeNames.add(new QName(CswConstants.CSW_OUTPUT_SCHEMA, VALID_TYPE, VALID_PREFIX));

        query.setTypeNames(typeNames);

        JAXBElement<QueryType> jaxbQuery = new JAXBElement<QueryType>(cswQnameOutPutSchema,
                QueryType.class, query);
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
        assertEquals(sp.getProviderName(), CswEndpoint.PROVIDER_NAME);
    }

    /**
     * Helper method to verify the ServiceIdentification section matches the endpoint's definition
     *
     * @param ct The CapabilitiesType to verify
     */
    private void verifyServiceIdentification(CapabilitiesType ct) {
        ServiceIdentification si = ct.getServiceIdentification();
        assertEquals(si.getTitle(), CswEndpoint.SERVICE_TITLE);
        assertEquals(si.getAbstract(), CswEndpoint.SERVICE_ABSTRACT);
        assertEquals(si.getServiceType().getValue(), CswConstants.CSW);
        assertEquals(si.getServiceTypeVersion(), Arrays.asList(CswConstants.VERSION_2_0_2));
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
            if (StringUtils.equals(CswConstants.TRANSACTION, op.getName()) || StringUtils
                    .equals(CswConstants.GET_RECORDS, op.getName())) {
                for (DomainType parameter : op.getParameter()) {
                    if (StringUtils.equals(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER,
                            parameter.getName())) {
                        assertThat(parameter.getValue(),
                                contains(CswConstants.CONSTRAINT_LANGUAGE_FILTER,
                                        CswConstants.CONSTRAINT_LANGUAGE_CQL));
                    } else if (StringUtils
                            .equals(CswConstants.TYPE_NAMES_PARAMETER, parameter.getName())) {
                        assertThat(parameter.getValue(), contains(CswConstants.CSW_RECORD));
                    }
                }
            }
        }
        assertTrue(opNames.contains(CswConstants.GET_CAPABILITIES));
        assertTrue(opNames.contains(CswConstants.DESCRIBE_RECORD));
        assertTrue(opNames.contains(CswConstants.GET_RECORDS));
        assertTrue(opNames.contains(CswConstants.GET_RECORD_BY_ID));
        assertTrue(opNames.contains(CswConstants.TRANSACTION));
    }

    /**
     * Helper method to verify the FilterCapabilities section matches the endpoint's definition
     *
     * @param ct The CapabilitiesType to verify
     */
    private void verifyFilterCapabilities(CapabilitiesType ct) {
        FilterCapabilities fc = ct.getFilterCapabilities();

        assertNotNull(fc.getIdCapabilities());
        assertTrue(fc.getIdCapabilities().getEIDOrFID().size() == 1);

        assertNotNull(fc.getScalarCapabilities());
        assertTrue(CswEndpoint.COMPARISON_OPERATORS.size() == fc.getScalarCapabilities()
                .getComparisonOperators().getComparisonOperator().size());
        for (ComparisonOperatorType cot : CswEndpoint.COMPARISON_OPERATORS) {
            assertTrue(fc.getScalarCapabilities().getComparisonOperators().getComparisonOperator()
                    .contains(cot));
        }

        assertNotNull(fc.getSpatialCapabilities());
        assertTrue(CswEndpoint.SPATIAL_OPERATORS.size() == fc.getSpatialCapabilities()
                .getSpatialOperators().getSpatialOperator().size());
        for (SpatialOperatorType sot : fc.getSpatialCapabilities().getSpatialOperators()
                .getSpatialOperator()) {
            assertTrue(CswEndpoint.SPATIAL_OPERATORS.contains(sot.getName()));
        }
    }

    private QueryResponse getQueryResponse() {
        List<Result> results = new LinkedList<Result>();
        for (int i = 0; i < RESULT_COUNT; i++) {
            Result result = new ResultImpl();
            results.add(result);
        }
        return new QueryResponseImpl(null, results, TOTAL_COUNT);
    }

}
