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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ddf.catalog.transform.QueryResponseTransformer;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.cat.csw.v_2_0_2.SchemaComponentType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionType;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
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
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * CswEndpoint provides a server implementation of the Catalogue Service for Web (CSW) 2.0.2.
 */
public class CswEndpoint implements Csw {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswEndpoint.class);

    private final TransformerManager transformerManager;

    private FilterBuilder builder;

    private BundleContext context;

    private CatalogFramework framework;

    private CapabilitiesType capabilitiesType;

    private DatatypeFactory datatypeFactory;
    
    protected static final String SERVICE_TITLE = "Catalog Service for the Web";

    protected static final String SERVICE_ABSTRACT = "DDF CSW Endpoint";
        
    protected static final List<String> SERVICE_TYPE_VERSION = Arrays
            .asList(CswConstants.VERSION_2_0_2);

    protected static final List<SpatialOperatorNameType> SPATIAL_OPERATORS = Arrays.asList(
            SpatialOperatorNameType.BBOX, SpatialOperatorNameType.BEYOND,
            SpatialOperatorNameType.CONTAINS, SpatialOperatorNameType.CROSSES,
            SpatialOperatorNameType.DISJOINT, SpatialOperatorNameType.D_WITHIN,
            SpatialOperatorNameType.INTERSECTS, SpatialOperatorNameType.OVERLAPS,
            SpatialOperatorNameType.TOUCHES, SpatialOperatorNameType.WITHIN);
    
    protected static final List<ComparisonOperatorType> COMPARISON_OPERATORS = Arrays.asList(
            ComparisonOperatorType.BETWEEN, ComparisonOperatorType.NULL_CHECK,
            ComparisonOperatorType.LIKE, ComparisonOperatorType.EQUAL_TO,
            ComparisonOperatorType.GREATER_THAN, ComparisonOperatorType.GREATER_THAN_EQUAL_TO,
            ComparisonOperatorType.LESS_THAN, ComparisonOperatorType.LESS_THAN_EQUAL_TO,
            ComparisonOperatorType.EQUAL_TO, ComparisonOperatorType.NOT_EQUAL_TO);

    protected static final String PROVIDER_NAME = "DDF";

    protected static final String SERVICE_IDENTIFICATION = "ServiceIdentification";

    protected static final String SERVICE_PROVIDER = "ServiceProvider";

    protected static final String OPERATIONS_METADATA = "OperationsMetadata";

    protected static final String FILTER_CAPABILITIES = "Filter_Capabilities";

    protected static final List<String> GET_CAPABILITIES_PARAMS = Arrays.asList(
            SERVICE_IDENTIFICATION, SERVICE_PROVIDER, OPERATIONS_METADATA, FILTER_CAPABILITIES);

    private static Map<String, Element> documentElements = new HashMap<String, Element>();

    private static JAXBContext jaxBContext;

    private static final Configuration PARSER_CONFIG = new org.geotools.filter.v1_1.OGCConfiguration();

    private static final String DEFAULT_OUTPUT_FORMAT = MediaType.APPLICATION_XML;

    @Context
    private UriInfo uri;

    /**
     * JAX-RS Server that represents a CSW v2.0.2 Server.
     */
    public CswEndpoint(BundleContext context, CatalogFramework ddf, FilterBuilder filterBuilder,
            TransformerManager manager) {
        this.context = context;
        this.framework = ddf;
        this.builder = filterBuilder;
        this.transformerManager = manager;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            LOGGER.warn("Failed to construct datatypeFactory.  Exception {}", e);
        }
    }

    /* Constructor for unit testing */
    public CswEndpoint(BundleContext context, CatalogFramework ddf, FilterBuilder filterBuilder,
            UriInfo uri, TransformerManager manager) {
        this(context, ddf, filterBuilder, manager);
        this.uri = uri;
    }

    public static synchronized JAXBContext getJaxBContext() throws JAXBException {
        if (jaxBContext == null) {
            jaxBContext = JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:" +  
                    "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0");
        }
        return jaxBContext;
    }
    
    @Override
    @GET
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public CapabilitiesType getCapabilities(@QueryParam("")
        GetCapabilitiesRequest request) throws CswException {

        capabilitiesType = buildCapabilitiesType();

        if (request.getAcceptVersions() != null) {
            validateVersion(request.getAcceptVersions());
        }

        List<String> sectionList = null;
        if (request.getSections() != null) {
            String[] sections = request.getSections().split(",");
            sectionList = Arrays.asList(sections);
        }

        return buildCapabilitiesType(sectionList);
    }

    @Override
    @POST
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws CswException {
        capabilitiesType = buildCapabilitiesType();

        if (request.getAcceptVersions() != null) {
            validateVersion(request.getAcceptVersions().toString());
        }

        List<String> sectionList = null;
        if (request.getSections() != null) {
            sectionList = request.getSections().getSection();
        }

        return buildCapabilitiesType(sectionList);
    }

    @Override
    @GET
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public DescribeRecordResponseType describeRecord(@QueryParam("")
        DescribeRecordRequest request) throws CswException {

        if (request == null) {
            throw new CswException("DescribeRecordRequest request is null");
        }
        
        validateOutputFormat(request.getOutputFormat());

        validateSchemaLanguage(request.getSchemaLanguage());

        Map<String, String> namespacePrefixToUriMappings = request.parseNamespaces(request.getNamespace());
        
        validateTypeNameToNamespaceMappings(request.getTypeName(), request.getNamespace(), namespacePrefixToUriMappings);
        
        if (request.getVersion() != null) {
            validateVersion(request.getVersion());
        }
  
        List<QName> types = typeStringToQNames(request.getTypeName(), namespacePrefixToUriMappings);

        return buildDescribeRecordResponseFromTypes(types, request.getVersion());
    }


    @Override
    @POST
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public DescribeRecordResponseType describeRecord(DescribeRecordType request)
        throws CswException {

        if (request == null) {
            throw new CswException("DescribeRecordRequest request is null");
        }

        validateOutputFormat(request.getOutputFormat());
        validateSchemaLanguage(request.getSchemaLanguage());

        return buildDescribeRecordResponseFromTypes(request.getTypeName(), CswConstants.VERSION_2_0_2);
    }


    @Override
    @GET
    @Produces({ MediaType.WILDCARD })
    public CswRecordCollection getRecords(@QueryParam("")
        GetRecordsRequest request) throws CswException {

        if (request == null) {
            throw new CswException("GetRecordsRequest request is null");
        }
        if (StringUtils.isEmpty(request.getVersion())) {
            request.setVersion(CswConstants.VERSION_2_0_2);
        } else {
            validateVersion(request.getVersion());
        }

        return getRecords(request.get_2_0_2_RecordsType());
    }

    @Override
    @POST
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.WILDCARD })
    public CswRecordCollection getRecords(GetRecordsType request) throws CswException {
        
        if (request == null) {
            throw new CswException("GetRecordsType request is null");
        }

        validateOutputFormat(request.getOutputFormat());

        validateOutputSchema(request.getOutputSchema());
        
        if (request.getAbstractQuery() != null) {
            if (!request.getAbstractQuery().getValue().getClass().equals(QueryType.class)) {
                throw new CswException("Unknown QueryType: "
                        + request.getAbstractQuery().getValue().getClass());
            }
            
            QueryType query = (QueryType) request.getAbstractQuery().getValue();
            
            validateTypes(query.getTypeNames(), CswConstants.VERSION_2_0_2);
                        
            if (query.getConstraint() != null &&
                    query.getConstraint().isSetFilter() && query.getConstraint().isSetCqlText()) {
                throw new CswException("A Csw Query can only have a Filter or CQL constraint");
            }
        }

        return queryCsw(request);
    }

    @Override
    @GET
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public CswRecordCollection getRecordById(@QueryParam("")
        GetRecordByIdRequest request) throws CswException {
        if (request == null){
            throw new CswException("GetRecordByIdRequest request is null");
        }
        
        validateOutputFormat(request.getOutputFormat());

        validateOutputSchema(request.getOutputSchema());
        
        if (StringUtils.isNotBlank(request.getId())) {
            List<String> ids = Arrays.<String> asList(request.getId().split(CswConstants.COMMA));
            
            CswRecordCollection response = queryById(ids);
            response.setOutputSchema(request.getOutputSchema());
            if (StringUtils.isNotBlank(request.getElementSetName())){
                response.setElementSetType(ElementSetType.fromValue(request.getElementSetName()));
            } else {
                response.setElementSetType(ElementSetType.SUMMARY);
            }
            return response;
        }else{
            throw new CswException("A GetRecordById Query must contain an ID.",
                    CswConstants.MISSING_PARAMETER_VALUE, "id");
        }
    }

    @Override
    @POST
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public CswRecordCollection getRecordById(GetRecordByIdType request) throws CswException {
        if (request == null) {
            throw new CswException("GetRecordByIdRequest request is null");
        }

        validateOutputFormat(request.getOutputFormat());

        validateOutputSchema(request.getOutputSchema());

        if (!request.getId().isEmpty()) {
            CswRecordCollection response = queryById(request.getId());
            response.setOutputSchema(request.getOutputSchema());
            if (request.isSetElementSetName() && request.getElementSetName().getValue() != null) {
                response.setElementSetType(request.getElementSetName().getValue());
            } else {
                response.setElementSetType(ElementSetType.SUMMARY);
            }
            return response;
        } else {
            throw new CswException("A GetRecordById Query must contain an ID.",
                    CswConstants.MISSING_PARAMETER_VALUE, "id");
        }
    }

    @Override
    @POST
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public TransactionResponseType transaction(TransactionType request) throws CswException {
        notImplemented("Transaction_POST");
        return null;
    }

    @GET
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public void unknownService(@QueryParam("")
        CswRequest request) throws CswException {
        if(request.getService() == null) {
            throw new CswException("Missing service value",
                    CswConstants.MISSING_PARAMETER_VALUE, "service");            
        }
        throw new CswException("Unknown service (" + request.getService() + ")",
                CswConstants.INVALID_PARAMETER_VALUE, "service");
    }

    @POST
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public void unknownService() throws CswException {
        throw new CswException("Unknown Service", CswConstants.INVALID_PARAMETER_VALUE, "service");
    }

    @GET
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public void unknownOperation(@QueryParam("")
        CswRequest request) throws CswException {
        throw new CswException("No such operation: " + request.getRequest(),
                CswConstants.OPERATION_NOT_SUPPORTED, request.getRequest());
    }

    @POST
    @Consumes({ "text/xml", "application/xml" })
    @Produces({ "text/xml", "application/xml" })
    public void unknownOperation() throws CswException {
        throw new CswException("No such operation", CswConstants.OPERATION_NOT_SUPPORTED, null);
    }
    

    /**
     * Validates TypeName to namspace uri mapping in query request.
     * 
     * @param typeNames this can be a comma separated list of types which
     *                  can be prefixed with prefixes.
     *                  example csw:Record
     * @param namespaces the namespace parameter from the request
     *                   example NAMESPACE=xmlns(csw=http://www.opengis.net/cat/csw/2.0.2)
     * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri
     *                  example key=csw value=http://www.opengis.net/cat/csw/2.0.2
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
                throw new CswException("Unable to map [" + type
                        + "] to one of the following namespaces [" + namespaces + "].");

            }
        }

    }
    
    /**
    *
    * Returns a list of QNames based on typeNames and namespaces given
    *
    *
    * @param typeNames this can be a comma separated list of types which
    *                  can be prefixed with prefixes.
    *                  example csw:Record
    * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri
    *                  example key=csw value=http://www.opengis.net/cat/csw/2.0.2
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
                String prefix = typeName.substring(0, typeName.indexOf(CswConstants.NAMESPACE_DELIMITER));
                String localPart = typeName.substring(typeName.indexOf(CswConstants.NAMESPACE_DELIMITER) + 1);
                QName qname = new QName(getNamespaceFromType(prefix, localPart, namespacePrefixToUriMappings), localPart, prefix);
                qNames.add(qname);
            } else {
                QName qname = new QName(getNamespaceFromType("", typeName, namespacePrefixToUriMappings), typeName);
                qNames.add(qname);
            }
        }

        return qNames;
    }


    /**
     * for a single type, or localName, this returns the corresponding namespace
     * from the qualified list of namespaces.
     *
     * @param typePrefix prefix to a typeName
     *                  example csw is the prefix in the typeName csw:Record
     * @param type a single type that has already been split
     * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri
     *                  example key=csw value=http://www.opengis.net/cat/csw/2.0.2
     * @return corresponding namespace for the given type
     */
    private String getNamespaceFromType(String typePrefix, String type, Map<String, String> namespacePrefixToUriMappings) throws CswException {
        if (namespacePrefixToUriMappings == null) {
            return "";
        }
        
        String namespaceUri = namespacePrefixToUriMappings.get(typePrefix);
        
        if(namespaceUri == null) {
            throw createUnknownTypeException(type);
        }

        return namespaceUri;
    }

    private DescribeRecordResponseType buildDescribeRecordResponseFromTypes(List<QName> types,
            String version) throws CswException {

        validateFullyQualifiedTypes(types);

        DescribeRecordResponseType response = new DescribeRecordResponseType();
        List<SchemaComponentType> schemas = new ArrayList<SchemaComponentType>();
        System.out.println("KCW: " + Arrays.toString(types.toArray()));

        if (types.isEmpty() || types.contains(
                new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CSW_RECORD_LOCAL_NAME))) {
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

                    JAXBElement<GetRecordsType> jaxbElement = new ObjectFactory().createGetRecords(request);
                    marshaller.marshal(jaxbElement, writer);
                } catch (JAXBException e) {
                    LOGGER.debug("Unable to marshall {} to XML.  Exception {}", GetRecordsType.class, e);
                }
                LOGGER.debug(writer.toString());
            } catch (Exception e) {
                LOGGER.debug("Unable to create debug message for getRecordsType: {}", e);
            }
        }
        
        QueryType query = (QueryType) request.getAbstractQuery().getValue();

        CswRecordCollection response = new CswRecordCollection();
        response.setRequest(request);
        response.setOutputSchema(request.getOutputSchema());
        response.setMimeType(request.getOutputFormat());
        response.setResultType((request.getResultType() == null) ? ResultType.HITS : request.getResultType());
        
        if (ResultType.HITS.equals(request.getResultType()) || ResultType.RESULTS.equals(request.getResultType())) {
            QueryImpl frameworkQuery = new QueryImpl(buildFilter(query));
            frameworkQuery.setSortBy(buildSort(query.getSortBy()));
    
            if (ResultType.HITS.equals(request.getResultType()) || request.getMaxRecords().intValue() < 1) {
                frameworkQuery.setStartIndex(1);
                frameworkQuery.setPageSize(1);
            } else {
                frameworkQuery.setStartIndex(request.getStartPosition().intValue());
                frameworkQuery.setPageSize(request.getMaxRecords().intValue());                
            }
            boolean federated = (request.getDistributedSearch() != null)
                    && (request.getDistributedSearch().getHopCount().longValue() > 1);
    
            long count = 0;
            List<Metacard> metacards = new LinkedList<Metacard>();
            long hits = 0;
            try {
                QueryResponse queryResponse = framework.query(new QueryRequestImpl(frameworkQuery, federated));
                response.setSourceResponse(queryResponse);
    
//                hits = queryResponse.getHits();
//                count = queryResponse.getResults().size();
//                for (Result result : queryResponse.getResults()) {
//                    metacards.add(result.getMetacard());
//                }
//
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

            //response.setNumberOfRecordsMatched(hits);
//            if (ResultType.HITS.equals(request.getResultType()) || request.getMaxRecords().intValue() < 1) {
//                response.setNumberOfRecordsReturned(0);
//
//            } else {
//                response.setNumberOfRecordsReturned(count);
//                response.setCswRecords(metacards);
//            }
        }
        
        return response;
    }

    private Filter buildFilter(QueryType query) throws CswException {
        Filter filter = null;
        if (query.getConstraint() != null) {
            if (query.getConstraint().isSetCqlText()) {
                try {
                    filter = CQL.toFilter(query.getConstraint().getCqlText());
                } catch (CQLException e) {
                    throw new CswException("Unable to parse CQL Constraint: " + e.getMessage(), e);
                }
            } else if (query.getConstraint().isSetFilter()) {
                FilterType constraintFilter = query.getConstraint().getFilter();
                filter = parseFilter(constraintFilter);
            }
        } else {
            // not supported by catalog:
            //filter = Filter.INCLUDE;
            filter = builder.attribute(Metacard.ID).is().like().text(FilterDelegate.WILDCARD_CHAR);
        }
        
        if(filter == null) {
            throw new CswException("Invalid Filter Expression", CswConstants.NO_APPLICABLE_CODE, null);
        }
        if (query.getTypeNames().contains(new QName(CswConstants.CSW_OUTPUT_SCHEMA, 
                CswConstants.CSW_RECORD_LOCAL_NAME, CswConstants.CSW_NAMESPACE_PREFIX))) {
            
            try {
                FilterVisitor f = new CswRecordMapperFilterVisitor();
                filter = (Filter) filter.accept(f, null);
            } catch(UnsupportedOperationException ose) {
                throw new CswException(ose.getMessage(), CswConstants.INVALID_PARAMETER_VALUE, null);
            }
        }
        return filter;
    }
    
    private SortBy buildSort(SortByType sort) throws CswException {
        if (sort == null || sort.getSortProperty() == null) {
            return null;
        }
        
        
        SortBy[] sortByArr = parseSortBy(sort);

        if(sortByArr.length > 1) {
            LOGGER.warn("Query request has multiple sort criteria, only primary will be used");
        }
        
        SortBy sortBy = sortByArr[0];

        if(sortBy.getPropertyName() == null) {
            LOGGER.warn("No property name in primary sort criteria");
            return null;
        }

        if (!DefaultCswRecordMap.getDefaultCswRecordMap().hasDefaultMetacardFieldForPrefixedString(
                sortBy.getPropertyName().getPropertyName(),
                sortBy.getPropertyName().getNamespaceContext())) {
            throw new CswException("Property " + sortBy.getPropertyName().getPropertyName()
                    + " is not a valid SortBy Field", CswConstants.INVALID_PARAMETER_VALUE,
                    "SortProperty");
        }

        String name = DefaultCswRecordMap.getDefaultCswRecordMap()
                .getDefaultMetacardFieldForPrefixedString(
                        sortBy.getPropertyName().getPropertyName(),
                        sortBy.getPropertyName().getNamespaceContext());

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
        if (element == null){
            element = loadDocElementFromResourcePath(resourcePath);
            documentElements.put(resourcePath, element);
        }
        return element;
    }

    private Element loadDocElementFromResourcePath(String resourcePath) throws CswException {
        URL recordUrl = context.getBundle().getResource(resourcePath);

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        Document doc;
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(recordUrl.openStream());
        } catch (ParserConfigurationException e) {
            throw new CswException(e);
        } catch (SAXException  e) {
            throw new CswException(e);
        } catch (IOException e) {
            throw new CswException(e);
        }

        if (doc == null) {
            throw new CswException("Document was NULL in attempting to parse from resource path '" + resourcePath + "'");
        }
        return doc.getDocumentElement();
    }

    /**
     * Creates a CapabilitiesType object with only specified sections to be returned as a
     * GetCapabilities response.
     * 
     * @param sections
     *            The list of desired sections for the GetCapabilities response
     *
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
                cswCapabilities.setServiceIdentification(capabilitiesType
                        .getServiceIdentification());
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

        geoOperandsList.add(new QName(CswConstants.GML_SCHEMA, CswConstants.GML_POINT,
                CswConstants.GML_NAMESPACE_PREFIX));
        geoOperandsList.add(new QName(CswConstants.GML_SCHEMA, CswConstants.GML_LINESTRING,
                CswConstants.GML_NAMESPACE_PREFIX));
        geoOperandsList.add(new QName(CswConstants.GML_SCHEMA, CswConstants.GML_POLYGON,
                CswConstants.GML_NAMESPACE_PREFIX));

        spatialOpsType.setSpatialOperator(spatialOpTypes);
        SpatialCapabilitiesType spatialCaps = new SpatialCapabilitiesType();
        spatialCaps.setSpatialOperators(spatialOpsType);
        spatialCaps.setGeometryOperands(geometryOperands);
        filterCapabilities.setSpatialCapabilities(spatialCaps);

        IdCapabilitiesType idCapabilities = new IdCapabilitiesType();
        idCapabilities.getEIDOrFID().add(new EID());
        
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
                Arrays.asList(CswConstants.CSW_RECORD), describeRecordOp);
        List<String> mimeTypes = new ArrayList();
        mimeTypes.add(DEFAULT_OUTPUT_FORMAT);
        mimeTypes.addAll(transformerManager.getAvailableMimeTypes());
        addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, describeRecordOp);
        addOperationParameter("schemaLanguage", CswConstants.VALID_SCHEMA_LANGUAGES,
                describeRecordOp);

        // Builds GetRecords operation metadata
        Operation getRecordsOp = buildOperation(CswConstants.GET_RECORDS, getAndPost);
        addOperationParameter(CswConstants.RESULT_TYPE_PARAMETER,
                Arrays.asList("hits", "results", "validate"), getRecordsOp);
        addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, getRecordsOp);
        addOperationParameter(CswConstants.OUTPUT_SCHEMA_PARAMETER,
                transformerManager.getAvailableSchemas(),
                getRecordsOp);
        addOperationParameter(CswConstants.TYPE_NAMES_PARAMETER,
                Arrays.asList(CswConstants.CSW_RECORD), getRecordsOp);
        addOperationParameter(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER, Arrays.asList(
                        CswConstants.CONSTRAINT_LANGUAGE_FILTER,
                        CswConstants.CONSTRAINT_LANGUAGE_CQL),
                getRecordsOp);

        // Builds GetRecordById operation metadata
        Operation getRecordByIdOp = buildOperation(CswConstants.GET_RECORD_BY_ID, getAndPost);
        addOperationParameter(CswConstants.OUTPUT_SCHEMA_PARAMETER,
                transformerManager.getAvailableSchemas(),
                getRecordByIdOp);
        addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, getRecordByIdOp);
        addOperationParameter(CswConstants.RESULT_TYPE_PARAMETER,
                Arrays.asList("hits", "results", "validate"), getRecordByIdOp);
        addOperationParameter(CswConstants.ELEMENT_SET_NAME_PARAMETER,
                Arrays.asList("brief", "summary", "full"), getRecordByIdOp);

        List<Operation> ops = Arrays.asList(getCapabilitiesOp, describeRecordOp, getRecordsOp,
                getRecordByIdOp);
        om.setOperation(ops);

        om.getParameter().add(createDomainType(CswConstants.SERVICE, CswConstants.CSW));
        om.getParameter().add(createDomainType(CswConstants.VERSION, CswConstants.VERSION_2_0_2));

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
     * @param name
     *            The name of the operation
     * @param types
     *            The request types supported (GET/POST)
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
            rmt.setHref(uri.getBaseUri().toASCIIString());
            JAXBElement<RequestMethodType> requestElement = new JAXBElement<RequestMethodType>(
                    type, RequestMethodType.class, rmt);
            if(type.equals(CswConstants.POST)) {
                requestElement.getValue().getConstraint()
                        .add(createDomainType(CswConstants.POST_ENCODING, CswConstants.XML));
            }
            http.getGetOrPost().add(requestElement);
        }
        dcp.setHTTP(http);
        dcpList.add(dcp);
        op.setDCP(dcpList);
        

        return op;
    }

    private void addOperationParameter(String name, List<String> params, Operation op) {
        DomainType dt = createDomainType(name, params);
        op.getParameter().add(dt);
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

    /**
     * Verifies that that if types are passed, then they are fully qualified
     * 
     * @param types
     *            List of QNames representing types
     */
    private void validateFullyQualifiedTypes(List<QName> types) throws CswException {
        for (QName type : types) {
            if (StringUtils.isBlank(type.getNamespaceURI())) {
                throw new CswException("Unqualified type name: '" + type.getLocalPart() + "'",
                        CswConstants.INVALID_PARAMETER_VALUE, null);
            }
        }
    }

    
    /**
     * Verifies that if types are passed, then they exist.
     * 
     * @param types
     *            List of QNames representing types
     * @param version the specified version of the types
     */
    private void validateTypes(List<QName> types, String version) throws CswException {
        if (types == null || types.size() == 0) {
            // No type at all is valid, just return
            return;
        }

        if (types.size() == 1) {
            if (!types.get(0).equals(new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                    CswConstants.CSW_RECORD_LOCAL_NAME))) {
                throw createUnknownTypeException(types.get(0).toString());
            }

        }
    }

    private void validateOutputSchema(String schema) throws CswException {
        if (schema == null || transformerManager.getTransformerBySchema(schema) != null) {
            return;
        }
        throw createUnknownSchemaException(schema);
    }
    
    private void validateVersion(String versions) throws CswException {
        if (!versions.contains(CswConstants.VERSION_2_0_2)) {
            throw new CswException("Version(s) " + versions
                    + " is not supported, we currently support version "
                    + CswConstants.VERSION_2_0_2,
                    CswConstants.VERSION_NEGOTIATION_FAILED, null);
        }
    }

    private void validateOutputFormat(String format) throws CswException {
        if (!StringUtils.isEmpty(format)) {
            if (!DEFAULT_OUTPUT_FORMAT.equals(format) && !transformerManager.getAvailableMimeTypes().contains(format)){
                throw new CswException("Invalid output format '"
                        + format + "'", CswConstants.INVALID_PARAMETER_VALUE, "outputformat");
            }
        }
    }

    private void validateSchemaLanguage(String schemaLanguage) throws CswException {
        if (!StringUtils.isEmpty(schemaLanguage)) {
            if (!CswConstants.VALID_SCHEMA_LANGUAGES.contains(schemaLanguage)) {
                throw new CswException("Invalid schema language '" + schemaLanguage + "'",
                        CswConstants.INVALID_PARAMETER_VALUE, "schemaLanguage");
            }
        }
    }

    private void notImplemented(final String methodName) throws CswException {
        throw new CswException("The method " + methodName + " is not yet implemented.");
    }

    private CswException createUnknownTypeException(final String type) {
        return new CswException("The type '" + type + "' is not known to this service.",
                CswConstants.INVALID_PARAMETER_VALUE, null);
    }

    private CswException createUnknownSchemaException(final String schema) {
        return new CswException("The schema '" + schema + "' is not known to this service.",
                CswConstants.INVALID_PARAMETER_VALUE, "OutputSchema");
    }

    private InputStream marshalJaxB(JAXBElement<?> filterElement) throws JAXBException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getJaxBContext().createMarshaller().marshal(filterElement, os);
        ByteArrayInputStream input = new ByteArrayInputStream(os.toByteArray());
        IOUtils.closeQuietly(os);

        return input;
    }

    private Filter parseFilter(FilterType filterType) throws CswException {
        if (!filterType.isSetComparisonOps() && !filterType.isSetId()
                && !filterType.isSetLogicOps() && !filterType.isSetSpatialOps()) {
            throw new CswException("Empty Filter provided. Unable to preform query.",
                    CswConstants.INVALID_PARAMETER_VALUE, "Filter");
        }
        JAXBElement<FilterType> filterElement = new net.opengis.filter.v_1_1_0.ObjectFactory()
                .createFilter(filterType);

        return (Filter) parseJaxB(filterElement);
    }

    private SortBy[] parseSortBy(SortByType sortByType) throws CswException {
        JAXBElement<SortByType> sortByElement = new net.opengis.filter.v_1_1_0.ObjectFactory()
                .createSortBy(sortByType);

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
                    CswConstants.INVALID_PARAMETER_VALUE, null);
        } catch (IOException e) {
            throw new CswException("Failed to parse Element: (IOException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE, null);
        } catch (SAXException e) {
            throw new CswException("Failed to parse Element: (SAXException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE, null);
        } catch (ParserConfigurationException e) {
            throw new CswException("Failed to parse Element: (ParserConfigurationException): "
                    + e.getMessage(), CswConstants.INVALID_PARAMETER_VALUE, null);
        } catch (RuntimeException e) {
            throw new CswException("Failed to parse Element: (RuntimeException): " + e.getMessage(),
                    CswConstants.INVALID_PARAMETER_VALUE, null);
        }
    }
    
    private CswRecordCollection queryById(List<String> ids) throws CswException {
        List<Filter> filters = new ArrayList<Filter>();
        for (String id : ids) {
            filters.add(builder.attribute(Metacard.ID).is().equalTo().text(id));
        }

        Filter anyOfFilter = builder.anyOf(filters);
        QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(anyOfFilter), false);
        try {
            CswRecordCollection response = new CswRecordCollection();
            response.setById(true);
            QueryResponse queryResponse = framework.query(queryRequest);
            List<Metacard> metacards = new LinkedList<Metacard>();
            for (Result result : queryResponse.getResults()) {
                metacards.add(result.getMetacard());
            }
            response.setCswRecords(metacards);
            return response;
        } catch (UnsupportedQueryException e) {
            throw new CswException(e);
        } catch (SourceUnavailableException e) {
            throw new CswException(e);
        } catch (FederationException e) {
            throw new CswException(e);
        }
    }
}
