/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import ogc.schema.opengis.BaseVisitor;
import ogc.schema.opengis.DepthFirstTraverserImpl;
import ogc.schema.opengis.filter.v_1_0_0.FilterType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Between;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Beyond;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.ComparisonOperatorsType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Contains;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Crosses;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.DWithin;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Disjoint;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.FilterCapabilities;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Intersect;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Like;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.LogicalOperators;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.NullCheck;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Overlaps;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.ScalarCapabilitiesType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.SimpleComparisons;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.SpatialCapabilitiesType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.SpatialOperatorsType;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Touches;
import ogc.schema.opengis.filter_capabilities.v_1_0_0.Within;
import ogc.schema.opengis.wfs.v_1_0_0.GetFeatureType;
import ogc.schema.opengis.wfs.v_1_0_0.QueryType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.CapabilityType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.DCPTypeType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.DescribeFeatureTypeType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.EmptyType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeListType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.GetCapabilitiesType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.GetFeatureTypeType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.GetType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.HTTPType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.ObjectFactory;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.OperationsType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.PostType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.RequestType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.ResultFormatType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.SchemaDescriptionLanguageType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.ServiceType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.WFSCapabilitiesType;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.visitor.EncodedFilterVisitor;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Implementation of a WFS server. Supports both HTTP GET and POST.
 * 
 */
@Path("/")
public class WfsEndpoint implements Wfs {

    private ObjectFactory capsObjectFactory = new ObjectFactory();

    private static final String SERVICE_NAME = "WFS";

    private static final String SERVICE_TITLE = "Web Feature Service";

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final String ERROR_PARSING_MSG = "Error parsing request: ";

    private FilterBuilder builder;

    private CatalogFramework framework;

    private FeatureTypeSchemaCache schemaCache;

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsEndpoint.class);

    private static final Configuration PARSER_CONFIG = new org.geotools.filter.v1_0.OGCConfiguration();

    @Context
    private UriInfo uri;

    /**
     * JAX-RS Server that represents a WFS v1.0.0 Server.
     */
    public WfsEndpoint(CatalogFramework ddf, FilterBuilder filterBuilder,
            FeatureTypeSchemaCache cache) {
        this.framework = ddf;
        this.builder = filterBuilder;
        this.schemaCache = cache;
    }

    /* Constructor for unit testing */
    public WfsEndpoint(CatalogFramework ddf, FilterBuilder filterBuilder, UriInfo uri,
            FeatureTypeSchemaCache cache) {
        this.framework = ddf;
        this.builder = filterBuilder;
        this.uri = uri;
        this.schemaCache = cache;
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public WFSCapabilitiesType getCapabilities(@QueryParam("")
    GetCapabilitiesRequest request) throws WfsException {
        LOGGER.debug("Got getCapabilites via HTTP GET");
        if (request == null) {
            throw new WfsException("GetCapabilities request is null");
        }
        // Validate request
        if (WfsConstants.GET_CAPABILITES.equalsIgnoreCase(request.getRequest())) {
            // Since we only support 1.0.0 we should return the capabilities
            // even if a different version was requested.
            return buildWfsCapabilitiesType();
        } else {
            throw createUnexpectedServiceException(request.getService(), request.getVersion(),
                    request.getRequest());
        }
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public WFSCapabilitiesType getCapabilities(
            ogc.schema.opengis.wfs.v_1_0_0.GetCapabilitiesType request) throws WfsException {
        LOGGER.debug("Got getCapabilites via HTTP POST");
        if (request == null) {
            throw new WfsException("GetCapabilities request is null");
        }
        // Validate the request
        if (validateRequestParameters(request.getService(), request.getVersion())) {
            return buildWfsCapabilitiesType();
        } else {
            throw createUnexpectedServiceException(request.getService(), request.getVersion(),
                    WfsConstants.GET_CAPABILITES);
        }
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public XmlSchema describeFeatureType(@QueryParam("")
    DescribeFeatureTypeRequest request) throws WfsException {
        LOGGER.debug("Got describeFeatureType via HTTP GET");
        if (request == null) {
            throw new WfsException("DescribeFeatureType request is null");
        }
        if (WfsConstants.DESCRIBE_FEATURE_TYPE.equalsIgnoreCase(request.getRequest())
                && validateRequestParameters(request.getService(), request.getVersion())) {
            if (request.getTypeName() == null) {
                return buildMultipleFeatureTypeImportSchema(schemaCache.getFeatureTypeQnames());
            } else {
                String[] types = request.getTypeName().split(WfsConstants.COMMA);
                Set<QName> qnames = new HashSet<QName>();
                for (String type : types) {
                    String[] parsed = type.split(WfsConstants.NAMESPACE_DELIMITER);
                    QName qname = null;
                    if (parsed.length == 2) {
                        qname = new QName(WfsConstants.NAMESPACE_URN_ROOT + parsed[0], parsed[1]);
                    } else {
                        qname = schemaCache.getQnamefromLocalPart(type);
                    }

                    if (null == schemaCache.getSchemaByQname(qname)) {
                        throw createUnknownTypeException(type);
                    }

                    qnames.add(qname);
                }
                return processQnamesFromDescribeFeature(qnames);
            }
        } else {
            throw createUnexpectedServiceException(request.getService(), request.getVersion(),
                    request.getRequest());
        }
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public XmlSchema describeFeatureType(
            ogc.schema.opengis.wfs.v_1_0_0.DescribeFeatureTypeType request) throws WfsException {
        LOGGER.debug("Got describeFeatureType via HTTP POST");
        if (request == null) {
            throw new WfsException("DescribeFeatureType request is null");
        }
        if (validateRequestParameters(request.getService(), request.getVersion())) {
            if (request.getTypeName().isEmpty()) {
                return buildMultipleFeatureTypeImportSchema(schemaCache.getFeatureTypeQnames());
            } else {
                Set<QName> qnames = new HashSet<QName>();
                for (QName qname : request.getTypeName()) {
                    if (null == schemaCache.getSchemaByQname(qname)) {
                        throw createUnknownTypeException(qname.toString());
                    }
                    qnames.add(qname);
                }
                return processQnamesFromDescribeFeature(qnames);
            }

        } else {
            throw createUnexpectedServiceException(request.getService(), request.getVersion(),
                    WfsConstants.DESCRIBE_FEATURE_TYPE);
        }
    }

    private XmlSchema processQnamesFromDescribeFeature(Set<QName> qnames) throws WfsException {
        if (qnames.isEmpty()) {
            throw new WfsException("No valid featureTypes available to describe");
        }
        if (qnames.size() == 1) {
            XmlSchema schema = schemaCache.getSchemaByQname(qnames.iterator().next());
            return schema;
        } else {
            return buildMultipleFeatureTypeImportSchema(qnames);
        }
    }

    private XmlSchema buildMultipleFeatureTypeImportSchema(Set<QName> qnames) throws WfsException {
        XmlSchema schema = new XmlSchema("", new XmlSchemaCollection());
        schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        schema.setTargetNamespace(XmlSchemaSerializer.XSD_NAMESPACE);
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("", XmlSchemaSerializer.XSD_NAMESPACE);
        schema.setNamespaceContext(nsMap);
        for (QName qName : qnames) {
            XmlSchemaImport schemaImport = new XmlSchemaImport(schema);
            schemaImport.setNamespace(qName.getNamespaceURI());
            URI fullUri = UriBuilder
                    .fromUri(uri.getBaseUri())
                    .queryParam("request", WfsConstants.DESCRIBE_FEATURE_TYPE)
                    .queryParam("version", WfsConstants.VERSION_1_0_0)
                    .queryParam("service", WfsConstants.WFS)
                    .queryParam(
                            "typeName",
                            (StringUtils.isEmpty(qName.getPrefix())) ? qName.getLocalPart() : qName
                                    .getPrefix() + ":" + qName.getLocalPart()).build();
            schemaImport.setSchemaLocation(fullUri.toString());
            schema.getExternals().add(schemaImport);
        }
        return schema;
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public WfsFeatureCollection getFeature(GetFeatureType request) throws WfsException {
        LOGGER.debug("Got getFeature via HTTP POST");
        if (request == null) {
            throw new WfsException("GetFeature request is null");
        }

        if (validateRequestParameters(request.getService(), request.getVersion())) {

            List<QueryType> incomingQueries = request.getQuery();
            List<Filter> anyOfFilters = new ArrayList<Filter>();

            for (QueryType queryType : incomingQueries) {
                if (queryType.getTypeName() == null) {
                    throw new WfsException("Query must contain a featureType name");
                }

                List<Filter> allOfFilters = new ArrayList<Filter>();
                // First get the Type
                Filter filter = builder.attribute(Metacard.CONTENT_TYPE).is()
                        .text(queryType.getTypeName().getLocalPart());
                allOfFilters.add(filter);

                if (queryType.getFilter() != null) {
                    FilterType filterType = queryType.getFilter();

                    LOGGER.debug("featureID query?{}", filterType.isSetFeatureId());
                    EncodedFilterVisitor visitor;
                    try {
                        visitor = new EncodedFilterVisitor(new DepthFirstTraverserImpl(),
                                new BaseVisitor(), builder);
                        filterType.accept(visitor);
                    } catch (UnsupportedOperationException e) {
                        throw new WfsException(e);
                    }

                    if (filterType.isSetComparisonOps() || filterType.isSetLogicOps()
                            || filterType.isSetSpatialOps()) {

                        InputStream inputStream = null;
                        try {
                            allOfFilters.add(parseFilter(filterType));
                        } catch (JAXBException e) {
                            throw new WfsException(ERROR_PARSING_MSG, e);
                        } catch (IOException e) {
                            throw new WfsException(ERROR_PARSING_MSG, e);
                        } catch (SAXException e) {
                            throw new WfsException(ERROR_PARSING_MSG, e);
                        } catch (ParserConfigurationException e) {
                            throw new WfsException(ERROR_PARSING_MSG, e);
                        } finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                    }

                    if (filterType.isSetFeatureId()) {
                        allOfFilters.add(builder.anyOf(visitor.getFeatureIdFilters()));
                    }
                }

                anyOfFilters.add(builder.allOf(allOfFilters));
            }
            Filter totalFilter = builder.anyOf(anyOfFilters);

            QueryImpl query = new QueryImpl(totalFilter);
            query.setStartIndex(1);
            query.setPageSize((request.getMaxFeatures() == null) ? DEFAULT_PAGE_SIZE : request
                    .getMaxFeatures().intValue());

            WfsFeatureCollection featureCollection = new WfsFeatureCollection();
            try {
                QueryResponse queryResponse = framework.query(new QueryRequestImpl(query, true));

                for (Result result : queryResponse.getResults()) {
                    featureCollection.getFeatureMembers().add(result.getMetacard());
                }

            } catch (UnsupportedQueryException e) {
                LOGGER.warn("Unable to query", e);
                throw new WfsException(e);
            } catch (SourceUnavailableException e) {
                LOGGER.warn("Unable to query", e);
                throw new WfsException(e);
            } catch (FederationException e) {
                LOGGER.warn("Unable to query", e);
                throw new WfsException(e);
            }

            return featureCollection;

        } else {
            throw createUnexpectedServiceException(request.getService(), request.getVersion(),
                    WfsConstants.GET_FEATURE);
        }
    }

    private Boolean validateRequestParameters(final String service, final String version) {
        return (service != null && version != null && WfsConstants.WFS.equalsIgnoreCase(service) && WfsConstants.VERSION_1_0_0
                .equalsIgnoreCase(version));
    }

    private WFSCapabilitiesType buildWfsCapabilitiesType() {
        WFSCapabilitiesType wfsCapabilities = new WFSCapabilitiesType();
        wfsCapabilities.setVersion(WfsConstants.VERSION_1_0_0);
        // Create the Service Type
        ServiceType serviceType = new ServiceType();
        serviceType.setName(SERVICE_NAME);
        serviceType.setTitle(SERVICE_TITLE);
        serviceType.setOnlineResource(uri.getBaseUri().toASCIIString());
        wfsCapabilities.setService(serviceType);

        // Create the HTTP types. The URI is that of this service.
        GetType httpGet = new GetType();
        httpGet.setOnlineResource(uri.getBaseUri().toASCIIString());
        PostType httpPost = new PostType();
        httpPost.setOnlineResource(uri.getBaseUri().toASCIIString());
        HTTPType httpGetType = new HTTPType();
        httpGetType.getGetOrPost().add(httpGet);
        HTTPType httpPostType = new HTTPType();
        httpPostType.getGetOrPost().add(httpPost);
        DCPTypeType dcpGet = new DCPTypeType();
        dcpGet.setHTTP(httpGetType);
        DCPTypeType dcpPost = new DCPTypeType();
        dcpPost.setHTTP(httpPostType);

        // GetCapabilites - Supports both GET and POST
        GetCapabilitiesType getCapsType = new GetCapabilitiesType();
        getCapsType.getDCPType().add(dcpGet);
        getCapsType.getDCPType().add(dcpPost);

        // DescribeFeatureType - Supports both GET and POST - Returns XMLSchemas
        DescribeFeatureTypeType describeFeatureType = new DescribeFeatureTypeType();
        describeFeatureType.getDCPType().add(dcpGet);
        describeFeatureType.getDCPType().add(dcpPost);
        SchemaDescriptionLanguageType sdlt = new SchemaDescriptionLanguageType();
        sdlt.getXMLSCHEMA().add(capsObjectFactory.createXMLSCHEMA(new EmptyType()).getValue());
        describeFeatureType.setSchemaDescriptionLanguage(sdlt);

        // GetFeature - Supports both GET and POST
        GetFeatureTypeType getFeatureType = new GetFeatureTypeType();
        ResultFormatType resultFormat = new ResultFormatType();
        resultFormat.getGML2().add(new EmptyType());
        getFeatureType.setResultFormat(resultFormat);
        getFeatureType.getDCPType().add(dcpGet);
        getFeatureType.getDCPType().add(dcpPost);

        // Create The Capability Type
        CapabilityType capability = new CapabilityType();
        RequestType wfsRequest = new RequestType();
        wfsRequest.getGetCapabilitiesOrDescribeFeatureTypeOrTransaction().add(
                capsObjectFactory.createRequestTypeGetCapabilities(getCapsType));
        wfsRequest.getGetCapabilitiesOrDescribeFeatureTypeOrTransaction().add(
                capsObjectFactory.createRequestTypeDescribeFeatureType(describeFeatureType));
        wfsRequest.getGetCapabilitiesOrDescribeFeatureTypeOrTransaction().add(
                capsObjectFactory.createRequestTypeGetFeature(getFeatureType));
        capability.setRequest(wfsRequest);
        wfsCapabilities.setCapability(capability);

        // Create the Feature Type List
        FeatureTypeListType featureTypeList = new FeatureTypeListType();
        OperationsType operations = new OperationsType();
        operations.getInsertOrUpdateOrDelete().add(capsObjectFactory.createQuery(new EmptyType()));
        featureTypeList.setOperations(operations);
        featureTypeList.getFeatureType().addAll(buildFeatureTypes());
        wfsCapabilities.setFeatureTypeList(featureTypeList);

        // Create the Filter_Capabilites - These are defined statically by the
        // DDF FilterAdapter implementation
        FilterCapabilities filterCapabilities = new FilterCapabilities();
        ScalarCapabilitiesType scalarCapabilites = new ScalarCapabilitiesType();
        scalarCapabilites.getLogicalOperatorsOrComparisonOperatorsOrArithmeticOperators().add(
                new LogicalOperators());
        ComparisonOperatorsType compOpsType = new ComparisonOperatorsType();
        compOpsType.getSimpleComparisonsOrLikeOrBetween().add(new Between());
        compOpsType.getSimpleComparisonsOrLikeOrBetween().add(new NullCheck());
        compOpsType.getSimpleComparisonsOrLikeOrBetween().add(new Like());
        compOpsType.getSimpleComparisonsOrLikeOrBetween().add(new SimpleComparisons());
        scalarCapabilites.getLogicalOperatorsOrComparisonOperatorsOrArithmeticOperators().add(
                compOpsType);
        filterCapabilities.setScalarCapabilities(scalarCapabilites);
        SpatialOperatorsType spatialOpsType = new SpatialOperatorsType();
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Beyond());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Contains());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Crosses());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Disjoint());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new DWithin());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Intersect());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Overlaps());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Touches());
        spatialOpsType.getBBOXOrEqualsOrDisjoint().add(new Within());
        SpatialCapabilitiesType spatialCaps = new SpatialCapabilitiesType();
        spatialCaps.setSpatialOperators(spatialOpsType);
        filterCapabilities.setSpatialCapabilities(spatialCaps);

        wfsCapabilities.setFilterCapabilities(filterCapabilities);

        return wfsCapabilities;
    }

    private List<FeatureTypeType> buildFeatureTypes() {

        Set<QName> featureTypeNames = schemaCache.getFeatureTypeQnames();
        List<FeatureTypeType> featureTypes = new ArrayList<FeatureTypeType>();
        for (QName typeName : featureTypeNames) {
            FeatureTypeType featureType = new FeatureTypeType();
            // Name and SRS are the only required fields.
            featureType.setName(typeName);
            featureType.setSRS(WfsConstants.EPSG_4326);
            featureType.setAbstract("DWithin and Beyond filters must use either \"METRE\" "
                    + "or \"FOOT\" as the value in the Distance element's units attribute.");
            featureTypes.add(featureType);

        }
        return featureTypes;
    }

    private InputStream marshalFilter(JAXBElement<FilterType> filterElement) throws JAXBException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FilterTypeContextFactory.getInstance().createMarshaller().marshal(filterElement, os);
        ByteArrayInputStream input = new ByteArrayInputStream(os.toByteArray());
        IOUtils.closeQuietly(os);

        return input;
    }

    private Filter parseFilter(FilterType filterType) throws JAXBException, IOException,
        SAXException, ParserConfigurationException {
        Parser filterParser = new Parser(PARSER_CONFIG);
        InputStream inputStream = null;

        JAXBElement<FilterType> filterElement = new ogc.schema.opengis.filter.v_1_0_0.ObjectFactory()
                .createFilter(filterType);
        inputStream = marshalFilter(filterElement);

        return (Filter) filterParser.parse(inputStream);
    }

    private WfsException createUnexpectedServiceException(final String service,
            final String version, final String request) {
        return new WfsException("Unexpected service (" + service + ") or version (" + version
                + ") in request " + request + ".");
    }

    private WfsException createUnknownTypeException(final String type) {
        return new WfsException("The type '" + type + "' is not known to this service.");
    }

}
