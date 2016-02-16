/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
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
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.CswRecordMapperFilterVisitor;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.geotools.feature.NameImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.InsertResultType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.cat.csw.v_2_0_2.SchemaComponentType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionSummaryType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorsType;
import net.opengis.filter.v_1_1_0.EID;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.GeometryOperandsType;
import net.opengis.filter.v_1_1_0.IdCapabilitiesType;
import net.opengis.filter.v_1_1_0.LogicalOperators;
import net.opengis.filter.v_1_1_0.ScalarCapabilitiesType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.BoundingBoxType;
import net.opengis.ows.v_1_0_0.CodeType;
import net.opengis.ows.v_1_0_0.DCP;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.HTTP;
import net.opengis.ows.v_1_0_0.OnlineResourceType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import net.opengis.ows.v_1_0_0.RequestMethodType;
import net.opengis.ows.v_1_0_0.ResponsiblePartySubsetType;
import net.opengis.ows.v_1_0_0.ServiceIdentification;
import net.opengis.ows.v_1_0_0.ServiceProvider;

/**
 * CswEndpoint provides a server implementation of the Catalogue Service for Web (CSW) 2.0.2.
 */
public class CswEndpoint implements Csw {

    protected static final String SERVICE_TITLE = "Catalog Service for the Web";

    protected static final String SERVICE_ABSTRACT = "DDF CSW Endpoint";

    protected static final List<String> SERVICE_TYPE_VERSION =
            Collections.unmodifiableList(Arrays.asList(CswConstants.VERSION_2_0_2));

    protected static final List<SpatialOperatorNameType> SPATIAL_OPERATORS =
            Collections.unmodifiableList(Arrays.asList(SpatialOperatorNameType.BBOX,
                    SpatialOperatorNameType.BEYOND,
                    SpatialOperatorNameType.CONTAINS,
                    SpatialOperatorNameType.CROSSES,
                    SpatialOperatorNameType.DISJOINT,
                    SpatialOperatorNameType.D_WITHIN,
                    SpatialOperatorNameType.INTERSECTS,
                    SpatialOperatorNameType.OVERLAPS,
                    SpatialOperatorNameType.TOUCHES,
                    SpatialOperatorNameType.WITHIN));

    protected static final List<ComparisonOperatorType> COMPARISON_OPERATORS =
            Collections.unmodifiableList(Arrays.asList(ComparisonOperatorType.BETWEEN,
                    ComparisonOperatorType.NULL_CHECK,
                    ComparisonOperatorType.LIKE,
                    ComparisonOperatorType.EQUAL_TO,
                    ComparisonOperatorType.GREATER_THAN,
                    ComparisonOperatorType.GREATER_THAN_EQUAL_TO,
                    ComparisonOperatorType.LESS_THAN,
                    ComparisonOperatorType.LESS_THAN_EQUAL_TO,
                    ComparisonOperatorType.EQUAL_TO,
                    ComparisonOperatorType.NOT_EQUAL_TO));

    protected static final String PROVIDER_NAME = "DDF";

    protected static final String SERVICE_IDENTIFICATION = "ServiceIdentification";

    protected static final String SERVICE_PROVIDER = "ServiceProvider";

    protected static final String OPERATIONS_METADATA = "OperationsMetadata";

    protected static final String FILTER_CAPABILITIES = "Filter_Capabilities";

    protected static final List<String> GET_CAPABILITIES_PARAMS = Collections.unmodifiableList(
            Arrays.asList(SERVICE_IDENTIFICATION,
                    SERVICE_PROVIDER,
                    OPERATIONS_METADATA,
                    FILTER_CAPABILITIES));

    private static final List<String> ELEMENT_NAMES = Arrays.asList("brief", "summary", "full");

    private static final Logger LOGGER = LoggerFactory.getLogger(CswEndpoint.class);

    private static final Configuration PARSER_CONFIG =
            new org.geotools.filter.v1_1.OGCConfiguration();

    private static final String DEFAULT_OUTPUT_FORMAT = MediaType.APPLICATION_XML;

    private static final String OCTET_STREAM_OUTPUT_SCHEMA =
            "http://www.iana.org/assignments/media-types/application/octet-stream";

    private static final String ERROR_MULTI_PRODUCT_RETRIEVAL =
            "Can only retrieve product for one record.";

    private static final String ERROR_SCHEMA_FORMAT_PRODUCT_RETRIEVAL =
            "Unable to retrieve product because both output format and schema are not set "
                    + "to '%s', '%s' respectively, or, cannot get record due to "
                    + "incorrect output format and schema.";

    private static final String ERROR_ID_PRODUCT_RETRIEVAL =
            "Unable to retrieve product for ID: %s";

    private static Map<String, Element> documentElements = new HashMap<String, Element>();

    private static JAXBContext jaxBContext;

    private final TransformerManager mimeTypeTransformerManager;

    private final TransformerManager schemaTransformerManager;

    private final TransformerManager inputTransformerManager;

    private FilterBuilder builder;

    private BundleContext context;

    private CatalogFramework framework;

    private CapabilitiesType capabilitiesType;

    @Context
    private UriInfo uri;

    /**
     * JAX-RS Server that represents a CSW v2.0.2 Server.
     */
    public CswEndpoint(BundleContext context, CatalogFramework ddf, FilterBuilder filterBuilder,
            TransformerManager mimeTypeManager, TransformerManager schemaManager,
            TransformerManager inputManager) {
        LOGGER.trace("Entering: CSW Endpoint constructor.");
        this.context = context;
        this.framework = ddf;
        this.builder = filterBuilder;
        this.mimeTypeTransformerManager = mimeTypeManager;
        this.schemaTransformerManager = schemaManager;
        this.inputTransformerManager = inputManager;
        LOGGER.trace("Exiting: CSW Endpoint constructor.");
    }

    /* Constructor for unit testing */
    public CswEndpoint(BundleContext context, CatalogFramework ddf, FilterBuilder filterBuilder,
            UriInfo uri, TransformerManager manager, TransformerManager schemaManager,
            TransformerManager inputManager) {
        this(context, ddf, filterBuilder, manager, schemaManager, inputManager);
        this.uri = uri;
    }

    public static synchronized JAXBContext getJaxBContext() throws JAXBException {
        if (jaxBContext == null) {

            jaxBContext = JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:"
                    + "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0");
        }
        return jaxBContext;
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public CapabilitiesType getCapabilities(@QueryParam("") GetCapabilitiesRequest request)
            throws CswException {

        LOGGER.trace("Entering: getCapabilities.");
        capabilitiesType = buildCapabilitiesType();

        if (request.getAcceptVersions() != null) {
            validateVersion(request.getAcceptVersions());
        }

        List<String> sectionList = null;
        if (request.getSections() != null) {
            String[] sections = request.getSections()
                    .split(",");
            sectionList = Arrays.asList(sections);
        }

        LOGGER.trace("Exiting: getCapabilities.");

        return buildCapabilitiesType(sectionList);
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws CswException {
        capabilitiesType = buildCapabilitiesType();

        LOGGER.trace("Entering: getCapabilities.");
        if (request.getAcceptVersions() != null) {
            validateVersion(request.getAcceptVersions()
                    .toString());
        }

        List<String> sectionList = null;
        if (request.getSections() != null) {
            sectionList = request.getSections()
                    .getSection();
        }

        LOGGER.trace("Exiting: getCapabilities.");

        return buildCapabilitiesType(sectionList);
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public DescribeRecordResponseType describeRecord(@QueryParam("") DescribeRecordRequest request)
            throws CswException {
        if (request == null) {
            throw new CswException("DescribeRecordRequest request is null");
        }

        validateOutputFormat(request.getOutputFormat());
        validateSchemaLanguage(request.getSchemaLanguage());

        Map<String, String> namespacePrefixToUriMappings =
                request.parseNamespaces(request.getNamespace());

        validateTypeNameToNamespaceMappings(request.getTypeName(),
                request.getNamespace(),
                namespacePrefixToUriMappings);

        if (request.getVersion() != null) {
            validateVersion(request.getVersion());
        }

        List<QName> types = typeStringToQNames(request.getTypeName(), namespacePrefixToUriMappings);

        return buildDescribeRecordResponseFromTypes(types, request.getVersion());
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public DescribeRecordResponseType describeRecord(DescribeRecordType request)
            throws CswException {
        if (request == null) {
            throw new CswException("DescribeRecordRequest request is null");
        }

        validateOutputFormat(request.getOutputFormat());
        validateSchemaLanguage(request.getSchemaLanguage());

        return buildDescribeRecordResponseFromTypes(request.getTypeName(),
                CswConstants.VERSION_2_0_2);
    }

    @Override
    @GET
    @Produces({MediaType.WILDCARD})
    public CswRecordCollection getRecords(@QueryParam("") GetRecordsRequest request)
            throws CswException {
        if (request == null) {
            throw new CswException("GetRecordsRequest request is null");
        } else {
            LOGGER.debug("{} attempting to get records.", request.getRequest());
        }
        if (StringUtils.isEmpty(request.getVersion())) {
            request.setVersion(CswConstants.VERSION_2_0_2);
        } else {
            validateVersion(request.getVersion());
        }

        LOGGER.trace("Exiting getRecords");

        return getRecords(request.get202RecordsType());
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.WILDCARD})
    public CswRecordCollection getRecords(GetRecordsType request) throws CswException {
        if (request == null) {
            throw new CswException("GetRecordsType request is null");
        } else {
            LOGGER.debug("{} attempting to get records.", request.getService());
        }

        validateOutputFormat(request.getOutputFormat());
        validateOutputSchema(request.getOutputSchema());

        if (request.getAbstractQuery() != null) {
            if (!request.getAbstractQuery()
                    .getValue()
                    .getClass()
                    .equals(QueryType.class)) {
                throw new CswException("Unknown QueryType: " + request.getAbstractQuery()
                        .getValue()
                        .getClass());
            }

            QueryType query = (QueryType) request.getAbstractQuery()
                    .getValue();

            validateTypes(query.getTypeNames(), CswConstants.VERSION_2_0_2);
            validateElementNames(query);

            if (query.getConstraint() != null &&
                    query.getConstraint()
                            .isSetFilter() && query.getConstraint()
                    .isSetCqlText()) {
                throw new CswException("A Csw Query can only have a Filter or CQL constraint");
            }
        }
        LOGGER.trace("Exiting getRecords.");
        return queryCsw(request);
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public CswRecordCollection getRecordById(@QueryParam("") GetRecordByIdRequest request)
            throws CswException {
        if (request == null) {
            throw new CswException("GetRecordByIdRequest request is null");
        }

        String outputFormat = request.getOutputFormat();
        String outputSchema = request.getOutputSchema();
        validateOutputFormat(outputFormat);
        validateOutputSchema(outputSchema);

        if (StringUtils.isNotBlank(request.getId())) {
            List<String> ids = Arrays.asList(request.getId()
                    .split(CswConstants.COMMA));
            // Check if the request wants to retrieve a product.
            if (isProductRetrieval(ids, outputFormat, outputSchema)) {
                LOGGER.debug("{} is attempting to retrieve product for ID: {}",
                        request.getService(),
                        ids.get(0));
                return queryProductById(ids.get(0));
            }

            LOGGER.debug("{} is attempting to retrieve records: {}", request.getService(), ids);
            CswRecordCollection response = queryById(ids);
            response.setOutputSchema(outputSchema);
            if (StringUtils.isNotBlank(request.getElementSetName())) {
                response.setElementSetType(ElementSetType.fromValue(request.getElementSetName()));
            } else {
                response.setElementSetType(ElementSetType.SUMMARY);
            }
            LOGGER.debug("{} successfully retrieved record(s): {}",
                    request.getRequest(),
                    request.getId());
            return response;
        } else {
            throw new CswException("A GetRecordById Query must contain an ID.",
                    CswConstants.MISSING_PARAMETER_VALUE,
                    "id");
        }
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public CswRecordCollection getRecordById(GetRecordByIdType request) throws CswException {
        if (request == null) {
            throw new CswException("GetRecordByIdRequest request is null");
        }

        String outputFormat = request.getOutputFormat();
        String outputSchema = request.getOutputSchema();
        validateOutputFormat(outputFormat);
        validateOutputSchema(outputSchema);

        List<String> ids = request.getId();
        if (!ids.isEmpty()) {
            // Check if the request wants to retrieve a product.
            if (isProductRetrieval(ids, outputFormat, outputSchema)) {
                LOGGER.debug("{} is attempting to retrieve product for: {}",
                        request.getService(),
                        ids.get(0));
                return queryProductById(ids.get(0));
            }

            LOGGER.debug("{} is attempting to retrieve records: {}", request.getService(), ids);
            CswRecordCollection response = queryById(ids);
            response.setOutputSchema(outputSchema);
            if (request.isSetElementSetName() && request.getElementSetName()
                    .getValue() != null) {
                response.setElementSetType(request.getElementSetName()
                        .getValue());
            } else {
                response.setElementSetType(ElementSetType.SUMMARY);
            }
            LOGGER.debug("{} successfully retrieved record(s): {}",
                    request.getService(),
                    request.getId());

            return response;
        } else {
            throw new CswException("A GetRecordById Query must contain an ID.",
                    CswConstants.MISSING_PARAMETER_VALUE,
                    "id");
        }
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public TransactionResponseType transaction(CswTransactionRequest request) throws CswException {
        if (request == null) {
            throw new CswException("TransactionRequest request is null");
        }

        TransactionResponseType response = new TransactionResponseType();
        TransactionSummaryType summary = new TransactionSummaryType();
        summary.setTotalInserted(BigInteger.valueOf(0));
        summary.setTotalUpdated(BigInteger.valueOf(0));
        summary.setTotalDeleted(BigInteger.valueOf(0));
        response.setTransactionSummary(summary);
        response.setVersion(CswConstants.VERSION_2_0_2);

        int numInserted = 0;
        for (InsertAction insertAction : request.getInsertActions()) {
            CreateRequest createRequest = new CreateRequestImpl(insertAction.getRecords());
            try {
                CreateResponse createResponse = framework.create(createRequest);
                if (request.isVerbose()) {
                    response.getInsertResult()
                            .add(getInsertResultFromResponse(createResponse));
                }

                numInserted += createResponse.getCreatedMetacards()
                        .size();
            } catch (IngestException | SourceUnavailableException e) {
                throw new CswException("Unable to insert record(s).",
                        CswConstants.TRANSACTION_FAILED,
                        insertAction.getHandle());
            }
        }
        LOGGER.debug("{} records inserted.", numInserted);
        response.getTransactionSummary()
                .setTotalInserted(BigInteger.valueOf(numInserted));

        int numUpdated = 0;
        for (UpdateAction updateAction : request.getUpdateActions()) {
            try {
                numUpdated += updateRecords(updateAction);
            } catch (CswException | FederationException | IngestException |
                    SourceUnavailableException | UnsupportedQueryException e) {
                throw new CswException("Unable to update record(s).",
                        CswConstants.TRANSACTION_FAILED,
                        updateAction.getHandle());
            }
        }
        LOGGER.debug("{} records inserted.", numInserted);
        response.getTransactionSummary()
                .setTotalUpdated(BigInteger.valueOf(numUpdated));

        int numDeleted = 0;
        for (DeleteAction deleteAction : request.getDeleteActions()) {
            try {
                numDeleted += deleteRecords(deleteAction);
            } catch (CswException | FederationException | IngestException |
                    SourceUnavailableException | UnsupportedQueryException e) {
                throw new CswException("Unable to delete record(s).",
                        CswConstants.TRANSACTION_FAILED,
                        deleteAction.getHandle());
            }
        }
        LOGGER.debug("{} records deleted.", numDeleted);
        response.getTransactionSummary()
                .setTotalDeleted(BigInteger.valueOf(numDeleted));

        return response;
    }

    private InsertResultType getInsertResultFromResponse(CreateResponse createResponse)
            throws CswException {
        InsertResultType result = new InsertResultType();
        WKTReader reader = new WKTReader();
        for (Metacard metacard : createResponse.getCreatedMetacards()) {
            BoundingBoxType boundingBox = new BoundingBoxType();
            Geometry geometry = null;
            String bbox = null;
            try {
                if (metacard.getAttribute(CswConstants.BBOX_PROP) != null) {
                    bbox = metacard.getAttribute(CswConstants.BBOX_PROP)
                            .getValue()
                            .toString();
                    geometry = reader.read(bbox);
                } else if (StringUtils.isNotBlank(metacard.getLocation())) {
                    bbox = metacard.getLocation();
                    geometry = reader.read(bbox);
                }
            } catch (ParseException e) {
                LOGGER.warn("Unable to parse BoundingBox : {}", bbox, e);
            }
            BriefRecordType briefRecordType = new BriefRecordType();
            if (geometry != null) {
                Envelope bounds = geometry.getEnvelopeInternal();
                if (bounds != null) {
                    boundingBox.setCrs(CswConstants.SRS_NAME);
                    boundingBox.setLowerCorner(Arrays.asList(bounds.getMinX(), bounds.getMinY()));
                    boundingBox.setUpperCorner(Arrays.asList(bounds.getMaxX(), bounds.getMaxY()));
                    briefRecordType.getBoundingBox()
                            .add(new net.opengis.ows.v_1_0_0.ObjectFactory().createBoundingBox(
                                    boundingBox));
                }
            }
            SimpleLiteral identifier = new SimpleLiteral();
            identifier.getContent()
                    .add(metacard.getId());
            briefRecordType.getIdentifier()
                    .add(new JAXBElement<>(CswConstants.DC_IDENTIFIER_QNAME,
                            SimpleLiteral.class,
                            identifier));
            SimpleLiteral title = new SimpleLiteral();
            title.getContent()
                    .add(metacard.getTitle());
            briefRecordType.getTitle()
                    .add(new JAXBElement<>(CswConstants.DC_TITLE_QNAME,
                            SimpleLiteral.class,
                            title));
            SimpleLiteral type = new SimpleLiteral();
            type.getContent()
                    .add(metacard.getContentTypeName());
            briefRecordType.setType(type);
            result.getBriefRecord()
                    .add(briefRecordType);
        }
        return result;
    }

    private int deleteRecords(DeleteAction deleteAction)
            throws CswException, FederationException, IngestException, SourceUnavailableException,
            UnsupportedQueryException {
        List<QName> qNames = typeStringToQNames(deleteAction.getTypeName(),
                deleteAction.getPrefixToUriMappings());
        Filter filter = buildFilter(deleteAction.getConstraint(), qNames).getVisitedFilter();
        QueryImpl query = new QueryImpl(filter);
        query.setPageSize(-1);

        QueryRequest queryRequest = new QueryRequestImpl(query);
        QueryResponse response = framework.query(queryRequest);

        List<String> ids = new ArrayList<>();

        for (Result result : response.getResults()) {
            if (result != null && result.getMetacard() != null) {
                ids.add(result.getMetacard()
                        .getId());
            }
        }

        if (ids.size() > 0) {
            DeleteRequestImpl deleteRequest =
                    new DeleteRequestImpl(ids.toArray(new String[ids.size()]));

            LOGGER.debug("Attempting to delete {} metacards. ", ids.size());
            DeleteResponse deleteResponse = framework.delete(deleteRequest);

            return deleteResponse.getDeletedMetacards()
                    .size();
        }

        return 0;
    }

    private int updateRecords(UpdateAction updateAction)
            throws CswException, FederationException, IngestException, SourceUnavailableException,
            UnsupportedQueryException {
        if (updateAction.getMetacard() != null) {
            Metacard newRecord = updateAction.getMetacard();

            if (newRecord.getId() != null) {
                UpdateRequest updateRequest = new UpdateRequestImpl(newRecord.getId(), newRecord);
                LOGGER.debug("Attempting to update {} ", newRecord.getId());
                UpdateResponse updateResponse = framework.update(updateRequest);
                return updateResponse.getUpdatedMetacards()
                        .size();
            } else {
                throw new CswException(
                        "Unable to update record.  No ID was specified in the request.",
                        CswConstants.MISSING_PARAMETER_VALUE,
                        updateAction.getHandle());

            }
        } else if (updateAction.getConstraint() != null) {
            QueryConstraintType constraint = updateAction.getConstraint();
            Filter filter = buildFilter(constraint,
                    typeStringToQNames(updateAction.getTypeName(),
                            updateAction.getPrefixToUriMappings())).getVisitedFilter();

            QueryImpl query = new QueryImpl(filter);
            query.setPageSize(-1);

            QueryRequest queryRequest = new QueryRequestImpl(query);
            QueryResponse response = framework.query(queryRequest);

            if (response.getHits() > 0) {
                Map<String, Serializable> recordProperties = updateAction.getRecordProperties();

                List<String> updatedMetacardIdsList = new ArrayList<>();
                List<Metacard> updatedMetacards = new ArrayList<>();

                for (Result result : response.getResults()) {
                    Metacard metacard = result.getMetacard();

                    if (metacard != null) {
                        for (Entry<String, Serializable> recordProperty : recordProperties.entrySet()) {
                            Attribute attribute = new AttributeImpl(recordProperty.getKey(),
                                    recordProperty.getValue());
                            metacard.setAttribute(attribute);
                        }
                        updatedMetacardIdsList.add(metacard.getId());
                        updatedMetacards.add(metacard);
                    }
                }

                if (updatedMetacardIdsList.size() > 0) {
                    String[] updatedMetacardIds =
                            updatedMetacardIdsList.toArray(new String[updatedMetacardIdsList.size()]);
                    UpdateRequest updateRequest = new UpdateRequestImpl(updatedMetacardIds,
                            updatedMetacards);

                    LOGGER.debug("Attempting to update {} metacards.",
                            updatedMetacardIdsList.size());
                    UpdateResponse updateResponse = framework.update(updateRequest);
                    return updateResponse.getUpdatedMetacards()
                            .size();
                }
            }
        }
        return 0;
    }

    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void unknownService(@QueryParam("") CswRequest request) throws CswException {
        if (request.getService() == null) {
            throw new CswException("Missing service value",
                    CswConstants.MISSING_PARAMETER_VALUE,
                    "service");
        }
        throw new CswException("Unknown service (" + request.getService() + ")",
                CswConstants.INVALID_PARAMETER_VALUE,
                "service");
    }

    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void unknownService() throws CswException {
        throw new CswException("Unknown Service", CswConstants.INVALID_PARAMETER_VALUE, "service");
    }

    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void unknownOperation(@QueryParam("") CswRequest request) throws CswException {
        throw new CswException("No such operation: " + request.getRequest(),
                CswConstants.OPERATION_NOT_SUPPORTED,
                request.getRequest());
    }

    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public void unknownOperation() throws CswException {
        throw new CswException("No such operation", CswConstants.OPERATION_NOT_SUPPORTED, null);
    }

    /**
     * Validates TypeName to namspace uri mapping in query request.
     *
     * @param typeNames                    this can be a comma separated list of types which
     *                                     can be prefixed with prefixes.
     *                                     example csw:Record
     * @param namespaces                   the namespace parameter from the request
     *                                     example NAMESPACE=xmlns(csw=http://www.opengis.net/cat/csw/2.0.2)
     * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri
     *                                     example key=csw value=http://www.opengis.net/cat/csw/2.0.2
     * @throws CswException
     */
    private void validateTypeNameToNamespaceMappings(String typeNames, String namespaces,
            Map<String, String> namespacePrefixToUriMappings) throws CswException {

        // No typeName in query.
        if (StringUtils.isBlank(typeNames)) {
            return;
        }

        String[] types = typeNames.split(CswConstants.COMMA);
        String prefix = null;

        for (String type : types) {
            if (type.contains(CswConstants.NAMESPACE_DELIMITER)) {
                // Get the prefix. For example in csw:Record, get csw.
                prefix = type.split(CswConstants.NAMESPACE_DELIMITER)[0];
            } else {
                prefix = "";
            }

            // if the prefix does not map to a provided namespace, throw an exception.
            if (!namespacePrefixToUriMappings.containsKey(prefix)) {
                throw new CswException(
                        "Unable to map [" + type + "] to one of the following namespaces ["
                                + namespaces + "].");
            }
        }
    }

    /**
     * Returns a list of QNames based on typeNames and namespaces given
     *
     * @param typeNames                    this can be a comma separated list of types which
     *                                     can be prefixed with prefixes.
     *                                     example csw:Record
     * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri
     *                                     example key=csw value=http://www.opengis.net/cat/csw/2.0.2
     * @return List of QNames so that types and namespaces are associated
     */
    private List<QName> typeStringToQNames(String typeNames,
            Map<String, String> namespacePrefixToUriMappings) throws CswException {
        List<QName> qNames = new ArrayList<QName>();
        if (typeNames == null) {
            return qNames;
        }

        String[] types = typeNames.split(CswConstants.COMMA);

        for (String typeName : types) {
            // if type name is in the format prefix:localPart (eg. csw:Record).
            if (typeName.indexOf(CswConstants.NAMESPACE_DELIMITER) != -1) {
                String prefix = typeName.substring(0,
                        typeName.indexOf(CswConstants.NAMESPACE_DELIMITER));
                String localPart = typeName.substring(
                        typeName.indexOf(CswConstants.NAMESPACE_DELIMITER) + 1);
                QName qname = new QName(getNamespaceFromType(prefix,
                        localPart,
                        namespacePrefixToUriMappings), localPart, prefix);
                qNames.add(qname);
            } else {
                QName qname = new QName(getNamespaceFromType("",
                        typeName,
                        namespacePrefixToUriMappings), typeName);
                qNames.add(qname);
            }
        }
        return qNames;
    }

    /**
     * for a single type, or localName, this returns the corresponding namespace
     * from the qualified list of namespaces.
     *
     * @param typePrefix                   prefix to a typeName
     *                                     example csw is the prefix in the typeName csw:Record
     * @param type                         a single type that has already been split
     * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri
     *                                     example key=csw value=http://www.opengis.net/cat/csw/2.0.2
     * @return corresponding namespace for the given type
     */
    private String getNamespaceFromType(String typePrefix, String type,
            Map<String, String> namespacePrefixToUriMappings) throws CswException {
        if (namespacePrefixToUriMappings == null) {
            return "";
        }

        String namespaceUri = namespacePrefixToUriMappings.get(typePrefix);

        if (namespaceUri == null) {
            throw createUnknownTypeException(type);
        }
        return namespaceUri;
    }

    private DescribeRecordResponseType buildDescribeRecordResponseFromTypes(List<QName> types,
            String version) throws CswException {

        validateFullyQualifiedTypes(types);

        DescribeRecordResponseType response = new DescribeRecordResponseType();
        List<SchemaComponentType> schemas = new ArrayList<SchemaComponentType>();

        if (types.isEmpty() || types.contains(new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.CSW_RECORD_LOCAL_NAME))) {
            schemas.add(getSchemaComponentType());
        }

        response.setSchemaComponent(schemas);
        return response;
    }

    private CswRecordCollection queryCsw(GetRecordsType request) throws CswException {
        if (LOGGER.isDebugEnabled()) {
            try {
                Writer writer = new StringWriter();
                try {
                    Marshaller marshaller = getJaxBContext().createMarshaller();
                    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                    JAXBElement<GetRecordsType> jaxbElement = new ObjectFactory().createGetRecords(
                            request);
                    marshaller.marshal(jaxbElement, writer);
                } catch (JAXBException e) {
                    LOGGER.debug("Unable to marshall {} to XML.  Exception {}",
                            GetRecordsType.class,
                            e);
                }
                LOGGER.debug(writer.toString());
            } catch (Exception e) {
                LOGGER.debug("Unable to create debug message for getRecordsType: {}", e);
            }
        }

        QueryType query = (QueryType) request.getAbstractQuery()
                .getValue();

        CswRecordCollection response = new CswRecordCollection();
        response.setRequest(request);
        response.setOutputSchema(request.getOutputSchema());
        response.setMimeType(request.getOutputFormat());
        response.setElementName(query.getElementName());
        response.setElementSetType((query.getElementSetName() != null) ?
                query.getElementSetName()
                        .getValue() :
                null);
        response.setResultType((ResultType) ObjectUtils.defaultIfNull(request.getResultType(),
                ResultType.HITS));

        if (ResultType.HITS.equals(request.getResultType())
                || ResultType.RESULTS.equals(request.getResultType())) {
            CswRecordMapperFilterVisitor filterVisitor = buildFilter(query.getConstraint(),
                    query.getTypeNames());
            QueryImpl frameworkQuery = new QueryImpl(filterVisitor.getVisitedFilter());
            frameworkQuery.setSortBy(buildSort(query.getSortBy()));

            if (ResultType.HITS.equals(request.getResultType()) || request.getMaxRecords()
                    .intValue() < 1) {
                frameworkQuery.setStartIndex(1);
                frameworkQuery.setPageSize(1);
            } else {
                frameworkQuery.setStartIndex(request.getStartPosition()
                        .intValue());
                frameworkQuery.setPageSize(request.getMaxRecords()
                        .intValue());
            }
            QueryRequest queryRequest = null;
            boolean isDistributed = request.getDistributedSearch() != null && (
                    request.getDistributedSearch()
                            .getHopCount()
                            .longValue() > 1);

            if (isDistributed && CollectionUtils.isEmpty(filterVisitor.getSourceIds())) {
                queryRequest = new QueryRequestImpl(frameworkQuery, true);
            } else if (isDistributed && !CollectionUtils.isEmpty(filterVisitor.getSourceIds())) {
                queryRequest = new QueryRequestImpl(frameworkQuery, filterVisitor.getSourceIds());
            } else {
                queryRequest = new QueryRequestImpl(frameworkQuery, false);
            }

            try {
                LOGGER.debug("Attempting to execute query: {}", response.getRequest());
                QueryResponse queryResponse = framework.query(queryRequest);
                response.setSourceResponse(queryResponse);
            } catch (UnsupportedQueryException e) {
                LOGGER.warn("Unable to query", e);
                throw new CswException(e);
            } catch (SourceUnavailableException e) {
                LOGGER.warn("Unable to query", e);
                throw new CswException(e);
            } catch (FederationException e) {
                LOGGER.warn("Unable to query", e);
                throw new CswException(e);
            }
        }
        return response;
    }

    private CswRecordMapperFilterVisitor buildFilter(QueryConstraintType constraint,
            List<QName> typeNames) throws CswException {
        CswRecordMapperFilterVisitor visitor = new CswRecordMapperFilterVisitor();
        Filter filter = null;
        if (constraint != null) {
            if (constraint.isSetCqlText()) {
                try {
                    filter = CQL.toFilter(constraint.getCqlText());
                } catch (CQLException e) {
                    throw new CswException("Unable to parse CQL Constraint: " + e.getMessage(), e);
                }
            } else if (constraint.isSetFilter()) {
                FilterType constraintFilter = constraint.getFilter();
                filter = parseFilter(constraintFilter);
            }
        } else {
            // not supported by catalog:
            //filter = Filter.INCLUDE;
            filter = builder.attribute(Metacard.ID)
                    .is()
                    .like()
                    .text(FilterDelegate.WILDCARD_CHAR);
        }

        if (filter == null) {
            throw new CswException("Invalid Filter Expression",
                    CswConstants.NO_APPLICABLE_CODE,
                    null);
        }
        visitor.setVisitedFilter(filter);
        if (typeNames.contains(new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.CSW_RECORD_LOCAL_NAME,
                CswConstants.CSW_NAMESPACE_PREFIX))) {

            try {
                visitor.setVisitedFilter((Filter) filter.accept(visitor, null));
            } catch (UnsupportedOperationException ose) {
                throw new CswException(ose.getMessage(),
                        CswConstants.INVALID_PARAMETER_VALUE,
                        null);
            }
        }
        return visitor;
    }

    private SortBy buildSort(SortByType sort) throws CswException {
        if (sort == null || sort.getSortProperty() == null) {
            return null;
        }

        SortBy[] sortByArr = parseSortBy(sort);

        if (sortByArr.length > 1) {
            LOGGER.warn("Query request has multiple sort criteria, only primary will be used");
        }

        SortBy sortBy = sortByArr[0];

        if (sortBy.getPropertyName() == null) {
            LOGGER.warn("No property name in primary sort criteria");
            return null;
        }

        if (!DefaultCswRecordMap.getDefaultCswRecordMap()
                .hasDefaultMetacardFieldForPrefixedString(sortBy.getPropertyName()
                                .getPropertyName(),
                        sortBy.getPropertyName()
                                .getNamespaceContext())) {
            throw new CswException("Property " + sortBy.getPropertyName()
                    .getPropertyName() + " is not a valid SortBy Field",
                    CswConstants.INVALID_PARAMETER_VALUE,
                    "SortProperty");
        }

        String name = DefaultCswRecordMap.getDefaultCswRecordMap()
                .getDefaultMetacardFieldForPrefixedString(sortBy.getPropertyName()
                                .getPropertyName(),
                        sortBy.getPropertyName()
                                .getNamespaceContext());

        PropertyName propName = new AttributeExpressionImpl(new NameImpl(name));

        return new SortByImpl(propName, sortBy.getSortOrder());
    }

    private SchemaComponentType getSchemaComponentType() throws CswException {
        SchemaComponentType schemaComponentType = new SchemaComponentType();
        List<Object> listOfObject = new ArrayList<Object>();
        listOfObject.add(getDocElementFromResourcePath("csw/2.0.2/record.xsd"));
        schemaComponentType.setContent(listOfObject);
        schemaComponentType.setSchemaLanguage(CswConstants.XML_SCHEMA_LANGUAGE);
        schemaComponentType.setTargetNamespace(CswConstants.CSW_OUTPUT_SCHEMA);
        return schemaComponentType;
    }

    private Element getDocElementFromResourcePath(String resourcePath) throws CswException {
        Element element = documentElements.get(resourcePath);
        if (element == null) {
            element = loadDocElementFromResourcePath(resourcePath);
            documentElements.put(resourcePath, element);
        }
        return element;
    }

    private Element loadDocElementFromResourcePath(String resourcePath) throws CswException {
        URL recordUrl = context.getBundle()
                .getResource(resourcePath);

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        Document doc;
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(recordUrl.openStream());
        } catch (ParserConfigurationException e) {
            throw new CswException(e);
        } catch (SAXException e) {
            throw new CswException(e);
        } catch (IOException e) {
            throw new CswException(e);
        }

        if (doc == null) {
            throw new CswException(
                    "Document was NULL in attempting to parse from resource path '" + resourcePath
                            + "'");
        }
        return doc.getDocumentElement();
    }

    /**
     * Creates a CapabilitiesType object with only specified sections to be returned as a
     * GetCapabilities response.
     *
     * @param sections The list of desired sections for the GetCapabilities response
     * @return The constructed CapabilitiesType object, containing only the user-specified sections
     */
    private CapabilitiesType buildCapabilitiesType(List<String> sections) {

        // If no sections are specified, return them all
        if (sections == null || sections.size() == 0) {
            return capabilitiesType;
        }

        CapabilitiesType cswCapabilities = new CapabilitiesType();
        cswCapabilities.setVersion(capabilitiesType.getVersion());

        // Grab the desired sections from the global capabilitiesType variable
        for (String section : sections) {
            if (section.equalsIgnoreCase(SERVICE_IDENTIFICATION)) {
                cswCapabilities.setServiceIdentification(capabilitiesType.getServiceIdentification());
            } else if (section.equalsIgnoreCase(SERVICE_PROVIDER)) {
                cswCapabilities.setServiceProvider(capabilitiesType.getServiceProvider());
            } else if (section.equalsIgnoreCase(OPERATIONS_METADATA)) {
                cswCapabilities.setOperationsMetadata(capabilitiesType.getOperationsMetadata());
            }
        }

        // filterCapabilities is required.  Add it even if it isn't in the sections list.
        cswCapabilities.setFilterCapabilities(capabilitiesType.getFilterCapabilities());

        return cswCapabilities;
    }

    /**
     * Creates a full CapabilitiesType element to be returned as the GetCapabilities response
     *
     * @return The constructed CapabilitiesType object
     */
    private CapabilitiesType buildCapabilitiesType() {
        if (capabilitiesType == null) {
            CapabilitiesType cswCapabilities = new CapabilitiesType();
            cswCapabilities.setVersion(CswConstants.VERSION_2_0_2);
            cswCapabilities.setServiceIdentification(buildServiceIdentification());
            cswCapabilities.setServiceProvider(buildServiceProvider());
            cswCapabilities.setOperationsMetadata(buildOperationsMetadata());
            cswCapabilities.setFilterCapabilities(buildFilterCapabilities());
            return cswCapabilities;
        } else {
            capabilitiesType.setOperationsMetadata(buildOperationsMetadata());
            return capabilitiesType;
        }
    }

    /**
     * Creates the Filter_Capabilities section of the GetCapabilities response. These are defined
     * statically by the DDF FilterAdapter implementation TODO: If the implementation changes,
     * update this method to reflect the changes.
     *
     * @return The constructed FilterCapabilities object
     */
    private FilterCapabilities buildFilterCapabilities() {
        // Create the FilterCapabilites - These are defined statically by the
        // DDF FilterAdapter implementation
        FilterCapabilities filterCapabilities = new FilterCapabilities();
        ScalarCapabilitiesType scalarCapabilities = new ScalarCapabilitiesType();
        ComparisonOperatorsType cot = new ComparisonOperatorsType();
        cot.setComparisonOperator(COMPARISON_OPERATORS);
        scalarCapabilities.setLogicalOperators(new LogicalOperators());
        scalarCapabilities.setComparisonOperators(cot);
        filterCapabilities.setScalarCapabilities(scalarCapabilities);

        SpatialOperatorsType spatialOpsType = new SpatialOperatorsType();
        ArrayList<SpatialOperatorType> spatialOpTypes = new ArrayList<SpatialOperatorType>();
        for (SpatialOperatorNameType sont : SPATIAL_OPERATORS) {
            SpatialOperatorType sot = new SpatialOperatorType();
            sot.setName(sont);
            spatialOpTypes.add(sot);
        }
        GeometryOperandsType geometryOperands = new GeometryOperandsType();
        List<QName> geoOperandsList = geometryOperands.getGeometryOperand();

        geoOperandsList.add(new QName(CswConstants.GML_SCHEMA,
                CswConstants.GML_POINT,
                CswConstants.GML_NAMESPACE_PREFIX));
        geoOperandsList.add(new QName(CswConstants.GML_SCHEMA,
                CswConstants.GML_LINESTRING,
                CswConstants.GML_NAMESPACE_PREFIX));
        geoOperandsList.add(new QName(CswConstants.GML_SCHEMA,
                CswConstants.GML_POLYGON,
                CswConstants.GML_NAMESPACE_PREFIX));

        spatialOpsType.setSpatialOperator(spatialOpTypes);
        SpatialCapabilitiesType spatialCaps = new SpatialCapabilitiesType();
        spatialCaps.setSpatialOperators(spatialOpsType);
        spatialCaps.setGeometryOperands(geometryOperands);
        filterCapabilities.setSpatialCapabilities(spatialCaps);

        IdCapabilitiesType idCapabilities = new IdCapabilitiesType();
        idCapabilities.getEIDOrFID()
                .add(new EID());

        filterCapabilities.setIdCapabilities(idCapabilities);

        return filterCapabilities;
    }

    /**
     * Creates the OperationsMetadata portion of the GetCapabilities response TODO: As these
     * operations are implemented or added, update their descriptions to ensure they match up with
     * the functionality
     *
     * @return The constructed OperationsMetadata object
     */
    private OperationsMetadata buildOperationsMetadata() {

        OperationsMetadata om = new OperationsMetadata();

        List<QName> getAndPost = Arrays.asList(CswConstants.GET, CswConstants.POST);

        // Builds GetCapabilities operation metadata
        Operation getCapabilitiesOp = buildOperation(CswConstants.GET_CAPABILITIES, getAndPost);
        addOperationParameter("sections", GET_CAPABILITIES_PARAMS, getCapabilitiesOp);

        // Builds DescribeRecord operation metadata
        Operation describeRecordOp = buildOperation(CswConstants.DESCRIBE_RECORD, getAndPost);
        addOperationParameter(CswConstants.TYPE_NAME_PARAMETER,
                Arrays.asList(CswConstants.CSW_RECORD),
                describeRecordOp);
        Set<String> mimeTypeSet = new HashSet<>();
        mimeTypeSet.add(DEFAULT_OUTPUT_FORMAT);
        mimeTypeSet.addAll(mimeTypeTransformerManager.getAvailableMimeTypes());
        List<String> mimeTypes = new ArrayList<>(mimeTypeSet);
        addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, describeRecordOp);
        addOperationParameter("schemaLanguage",
                CswConstants.VALID_SCHEMA_LANGUAGES,
                describeRecordOp);

        // Builds GetRecords operation metadata
        Operation getRecordsOp = buildOperation(CswConstants.GET_RECORDS, getAndPost);
        addOperationParameter(CswConstants.RESULT_TYPE_PARAMETER,
                Arrays.asList("hits", "results", "validate"),
                getRecordsOp);
        addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, getRecordsOp);
        addOperationParameter(CswConstants.OUTPUT_SCHEMA_PARAMETER,
                schemaTransformerManager.getAvailableSchemas(),
                getRecordsOp);
        addOperationParameter(CswConstants.TYPE_NAMES_PARAMETER,
                Arrays.asList(CswConstants.CSW_RECORD),
                getRecordsOp);
        addOperationParameter(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER,
                CswConstants.CONSTRAINT_LANGUAGES,
                getRecordsOp);
        addFederatedCatalogs(getRecordsOp);

        // Builds GetRecordById operation metadata
        mimeTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        List<String> supportedSchemas = schemaTransformerManager.getAvailableSchemas();
        supportedSchemas.add(OCTET_STREAM_OUTPUT_SCHEMA);
        Operation getRecordByIdOp = buildOperation(CswConstants.GET_RECORD_BY_ID, getAndPost);
        addOperationParameter(CswConstants.OUTPUT_SCHEMA_PARAMETER,
                supportedSchemas,
                getRecordByIdOp);
        addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, getRecordByIdOp);
        addOperationParameter(CswConstants.RESULT_TYPE_PARAMETER,
                Arrays.asList("hits", "results", "validate"),
                getRecordByIdOp);
        addOperationParameter(CswConstants.ELEMENT_SET_NAME_PARAMETER,
                ELEMENT_NAMES,
                getRecordByIdOp);

        // Builds Transactions operation metadata
        Operation transactionOp = buildOperation(CswConstants.TRANSACTION,
                Arrays.asList(CswConstants.POST));
        addOperationParameter(CswConstants.TYPE_NAMES_PARAMETER,
                inputTransformerManager.getAvailableIds(),
                transactionOp);
        addOperationParameter(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER,
                CswConstants.CONSTRAINT_LANGUAGES,
                transactionOp);

        List<Operation> ops = Arrays.asList(getCapabilitiesOp,
                describeRecordOp,
                getRecordsOp,
                getRecordByIdOp,
                transactionOp);
        om.setOperation(ops);

        om.getParameter()
                .add(createDomainType(CswConstants.SERVICE, CswConstants.CSW));
        om.getParameter()
                .add(createDomainType(CswConstants.VERSION, CswConstants.VERSION_2_0_2));

        return om;
    }

    /**
     * Creates the ServiceIdentification portion of the GetCapabilities response TODO: Add more
     * DDF-specific information if desired (Fees, Keywords, and AccessConstraints are all currently
     * empty, and the abstract is very basic)
     *
     * @return The constructed ServiceIdentification object
     */
    private ServiceIdentification buildServiceIdentification() {

        ServiceIdentification si = new ServiceIdentification();
        si.setTitle(SERVICE_TITLE);
        si.setServiceTypeVersion(SERVICE_TYPE_VERSION);
        CodeType type = new CodeType();
        type.setValue(CswConstants.CSW);
        si.setServiceType(type);
        si.setAbstract(SERVICE_ABSTRACT);
        return si;
    }

    /**
     * Creates the ServiceProvider portion of the GetCapabilities response TODO: Add more
     * DDF-specific information if desired
     *
     * @return The constructed ServiceProvider object
     */
    private ServiceProvider buildServiceProvider() {
        ServiceProvider sp = new ServiceProvider();
        sp.setProviderName(PROVIDER_NAME);
        sp.setProviderSite(new OnlineResourceType());
        sp.setServiceContact(new ResponsiblePartySubsetType());
        return sp;
    }

    /**
     * Creates an Operation object for the OperationsMetadata section TODO: We currently don't use
     * the constraint or metadata elements, those can be added in as desired
     *
     * @param name  The name of the operation
     * @param types The request types supported (GET/POST)
     * @return The constructed Operation object
     */
    private Operation buildOperation(String name, List<QName> types) {
        Operation op = new Operation();

        op.setName(name);
        ArrayList<DCP> dcpList = new ArrayList<DCP>();
        DCP dcp = new DCP();
        HTTP http = new HTTP();
        for (QName type : types) {
            RequestMethodType rmt = new RequestMethodType();
            rmt.setHref(uri.getBaseUri()
                    .toASCIIString());
            JAXBElement<RequestMethodType> requestElement = new JAXBElement<RequestMethodType>(type,
                    RequestMethodType.class,
                    rmt);
            if (type.equals(CswConstants.POST)) {
                requestElement.getValue()
                        .getConstraint()
                        .add(createDomainType(CswConstants.POST_ENCODING, CswConstants.XML));
            }
            http.getGetOrPost()
                    .add(requestElement);
        }
        dcp.setHTTP(http);
        dcpList.add(dcp);
        op.setDCP(dcpList);
        return op;
    }

    private void addOperationParameter(String name, List<String> params, Operation op) {
        DomainType dt = createDomainType(name, params);
        op.getParameter()
                .add(dt);
    }

    private DomainType createDomainType(String name, String value) {
        return createDomainType(name, Collections.singletonList(value));
    }

    private DomainType createDomainType(String name, List<String> params) {
        DomainType dt = new DomainType();
        dt.setName(name);
        dt.setValue(params);
        return dt;
    }

    private void addFederatedCatalogs(Operation operation) {
        List<String> sourceIds = new ArrayList<>(framework.getSourceIds());
        sourceIds.remove(framework.getId());
        operation.getConstraint()
                .add(createDomainType(CswConstants.FEDERATED_CATALOGS, sourceIds));
    }

    /**
     * Verifies that that if types are passed, then they are fully qualified
     *
     * @param types List of QNames representing types
     */
    private void validateFullyQualifiedTypes(List<QName> types) throws CswException {
        for (QName type : types) {
            if (StringUtils.isBlank(type.getNamespaceURI())) {
                throw new CswException("Unqualified type name: '" + type.getLocalPart() + "'",
                        CswConstants.INVALID_PARAMETER_VALUE,
                        null);
            }
        }
    }

    /**
     * Verifies that if types are passed, then they exist.
     *
     * @param types   List of QNames representing types
     * @param version the specified version of the types
     */
    private void validateTypes(List<QName> types, String version) throws CswException {
        if (types == null || types.size() == 0) {
            // No type at all is valid, just return
            return;
        }

        if (types.size() == 1) {
            if (!types.get(0)
                    .equals(new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                            CswConstants.CSW_RECORD_LOCAL_NAME))) {
                throw createUnknownTypeException(types.get(0)
                        .toString());
            }

        }
    }

    /**
     * Verifies that if the ElementName or ElementSetName is passed, that they are
     * valid and mutually exclusive according to the OpenGIS CSW spec.
     *
     * @param query QueryType to be validated
     */
    private void validateElementNames(QueryType query) throws CswException {

        if (query.isSetElementSetName() && query.isSetElementName()) {
            throw new CswException("ElementSetName and ElementName must be mutually exclusive",
                    CswConstants.INVALID_PARAMETER_VALUE,
                    "ElementName");
        } else if (query.isSetElementName() && query.getElementName()
                .size() > 0) {

            for (QName elementName : query.getElementName()) {
                String elementNameString = elementName.getLocalPart();
                if (!ELEMENT_NAMES.contains(elementNameString)) {
                    throw new CswException("Unknown ElementName " + elementNameString,
                            CswConstants.INVALID_PARAMETER_VALUE,
                            "ElementName");
                }
            }
        } else if (query.isSetElementSetName() && query.getElementSetName()
                .getValue() == null) {
            throw new CswException("Unknown ElementSetName",
                    CswConstants.INVALID_PARAMETER_VALUE,
                    "ElementSetName");
        }

    }

    private void validateOutputSchema(String schema) throws CswException {
        if (schema == null || schemaTransformerManager.getTransformerBySchema(schema) != null
                || schema.equals(OCTET_STREAM_OUTPUT_SCHEMA)) {
            return;
        }
        throw createUnknownSchemaException(schema);
    }

    private void validateVersion(String versions) throws CswException {
        if (!versions.contains(CswConstants.VERSION_2_0_2)) {
            throw new CswException(
                    "Version(s) " + versions + " is not supported, we currently support version "
                            + CswConstants.VERSION_2_0_2,
                    CswConstants.VERSION_NEGOTIATION_FAILED,
                    null);
        }
    }

    private void validateOutputFormat(String format) throws CswException {
        if (!StringUtils.isEmpty(format)) {
            if (!DEFAULT_OUTPUT_FORMAT.equals(format)
                    && !mimeTypeTransformerManager.getAvailableMimeTypes()
                    .contains(format)) {
                if (!MediaType.APPLICATION_OCTET_STREAM.equals(format)) {
                    throw new CswException("Invalid output format '" + format + "'",
                            CswConstants.INVALID_PARAMETER_VALUE,
                            "outputformat");
                }
            }
        }
    }

    private void validateSchemaLanguage(String schemaLanguage) throws CswException {
        if (!StringUtils.isEmpty(schemaLanguage)) {
            if (!CswConstants.VALID_SCHEMA_LANGUAGES.contains(schemaLanguage)) {
                throw new CswException("Invalid schema language '" + schemaLanguage + "'",
                        CswConstants.INVALID_PARAMETER_VALUE,
                        "schemaLanguage");
            }
        }
    }

    private void notImplemented(final String methodName) throws CswException {
        throw new CswException("The method " + methodName + " is not yet implemented.");
    }

    private CswException createUnknownTypeException(final String type) {
        return new CswException("The type '" + type + "' is not known to this service.",
                CswConstants.INVALID_PARAMETER_VALUE,
                null);
    }

    private CswException createUnknownSchemaException(final String schema) {
        return new CswException("The schema '" + schema + "' is not known to this service.",
                CswConstants.INVALID_PARAMETER_VALUE,
                "OutputSchema");
    }

    private InputStream marshalJaxB(JAXBElement<?> filterElement) throws JAXBException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getJaxBContext().createMarshaller()
                .marshal(filterElement, os);
        ByteArrayInputStream input = new ByteArrayInputStream(os.toByteArray());
        IOUtils.closeQuietly(os);

        return input;
    }

    private Filter parseFilter(FilterType filterType) throws CswException {
        if (!filterType.isSetComparisonOps() && !filterType.isSetId() && !filterType.isSetLogicOps()
                && !filterType.isSetSpatialOps()) {
            throw new CswException("Empty Filter provided. Unable to preform query.",
                    CswConstants.INVALID_PARAMETER_VALUE,
                    "Filter");
        }
        JAXBElement<FilterType> filterElement =
                new net.opengis.filter.v_1_1_0.ObjectFactory().createFilter(filterType);

        return (Filter) parseJaxB(filterElement);
    }

    private SortBy[] parseSortBy(SortByType sortByType) throws CswException {
        JAXBElement<SortByType> sortByElement =
                new net.opengis.filter.v_1_1_0.ObjectFactory().createSortBy(sortByType);

        return (SortBy[]) parseJaxB(sortByElement);
    }

    private Object parseJaxB(JAXBElement<?> element) throws CswException {
        Parser parser = new Parser(PARSER_CONFIG);
        InputStream inputStream = null;

        try {
            inputStream = marshalJaxB(element);
            return parser.parse(inputStream);
        } catch (JAXBException e) {
            throw new CswException("Failed to parse Element: (JAXBException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE,
                    null);
        } catch (IOException e) {
            throw new CswException("Failed to parse Element: (IOException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE,
                    null);
        } catch (SAXException e) {
            throw new CswException("Failed to parse Element: (SAXException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE,
                    null);
        } catch (ParserConfigurationException e) {
            throw new CswException(
                    "Failed to parse Element: (ParserConfigurationException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE,
                    null);
        } catch (RuntimeException e) {
            throw new CswException("Failed to parse Element: (RuntimeException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE,
                    null);
        }
    }

    private CswRecordCollection queryById(List<String> ids) throws CswException {
        List<Filter> filters = new ArrayList<>();
        for (String id : ids) {
            filters.add(builder.attribute(Metacard.ID)
                    .is()
                    .equalTo()
                    .text(id));
        }

        Filter anyOfFilter = builder.anyOf(filters);
        QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(anyOfFilter), false);
        try {
            CswRecordCollection response = new CswRecordCollection();
            response.setById(true);

            QueryResponse queryResponse = framework.query(queryRequest);
            response.setSourceResponse(queryResponse);

            List<Metacard> metacards = new LinkedList<>();
            for (Result result : queryResponse.getResults()) {
                metacards.add(result.getMetacard());
            }
            response.setCswRecords(metacards);

            return response;
        } catch (FederationException | SourceUnavailableException | UnsupportedQueryException e) {
            throw new CswException(e);
        }
    }

    private boolean isProductRetrieval(List<String> ids, String outputFormat, String outputSchema)
            throws CswException {
        if (outputSchema.equals(OCTET_STREAM_OUTPUT_SCHEMA)
                && outputFormat.equals(MediaType.APPLICATION_OCTET_STREAM)) {
            if (ids.size() == 1) {
                return true;
            } else {
                throw new CswException(ERROR_MULTI_PRODUCT_RETRIEVAL);
            }
        } else if ((outputSchema.equals(OCTET_STREAM_OUTPUT_SCHEMA) && !outputFormat.equals(
                MediaType.APPLICATION_OCTET_STREAM)) || (!outputSchema.equals(
                OCTET_STREAM_OUTPUT_SCHEMA)
                && outputFormat.equals(MediaType.APPLICATION_OCTET_STREAM))) {
            throw new CswException(String.format(ERROR_SCHEMA_FORMAT_PRODUCT_RETRIEVAL,
                    MediaType.APPLICATION_OCTET_STREAM,
                    OCTET_STREAM_OUTPUT_SCHEMA));
        }
        return false;
    }

    private CswRecordCollection queryProductById(String id) throws CswException {

        final ResourceRequestById resourceRequest = new ResourceRequestById(id);

        ResourceResponse resourceResponse;
        try {
            resourceResponse = framework.getLocalResource(resourceRequest);
        } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
            throw new CswException(String.format(ERROR_ID_PRODUCT_RETRIEVAL, id), e);
        }
        Resource resource = resourceResponse.getResource();
        MimeType mimeType = resource.getMimeType();
        if (mimeType == null) {
            try {
                mimeType = new MimeType(MediaType.APPLICATION_OCTET_STREAM);
                resource = new ResourceImpl(resource.getInputStream(),
                        mimeType,
                        resource.getName());
            } catch (MimeTypeParseException e) {
                throw new CswException(String.format(
                        "Could not create mime type upon null mimeType, for mime %s.",
                        MediaType.APPLICATION_OCTET_STREAM), e);
            }
        }
        CswRecordCollection cswRecordCollection = new CswRecordCollection();
        cswRecordCollection.setResource(resource);
        cswRecordCollection.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
        LOGGER.debug("{} successfully retrieved product for ID: {}", id);
        return cswRecordCollection;
    }
}
