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
package ddf.catalog.source.opensearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathFactory;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.geotools.filter.FilterTransformer;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.encryption.EncryptionService;

/**
 * Federated site that talks via OpenSearch to the DDF platform. Communication is usually performed
 * via https which requires a keystore and trust store to be provided.
 * 
 */
public final class OpenSearchSource implements FederatedSource, ConfiguredService,
        ConfigurationWatcher {

    static final String BAD_URL_MESSAGE = "Bad url given for remote source";

    static final String COULD_NOT_RETRIEVE_RESOURCE_MESSAGE = "Could not retrieve resource";

    public static final String NAME = "name";

    private boolean isInitialized = false;

    // service properties
    private String shortname;

    private boolean lastAvailable;

    private Date lastAvailableDate = null;

    private boolean localQueryOnly;

    private boolean shouldConvertToBBox;

    private String endpointUrl;

    private InputTransformer inputTransformer;

    private static final String ORGANIZATION = "DDF";

    private static final String TITLE = "OpenSearch DDF Federated Source";

    private static final String DESCRIPTION = "Queries DDF using the synchronous federated OpenSearch query";

    private static final long AVAILABLE_TIMEOUT_CHECK = 60000; // 60 seconds, in milliseconds

    private static final String URL_SRC_PARAMETER = "src";

    private static final String LOCAL_SEARCH_PARAMETER = "local";

    static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

    private static final String HEADER_RANGE = "Range";

    public static final String BYTES_TO_SKIP = "BytesToSkip";
    
    public static final String BYTES_SKIPPED = "BytesSkipped";

    static final String BYTES = "bytes";

    private static final String BYTES_EQUAL = "bytes=";

    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(OpenSearchSource.class));

    private FilterAdapter filterAdapter;

    // expensive creation, meant to be done once
    private static final Abdera ABDERA = new Abdera();

    private String configurationPid;

    protected OpenSearchConnection openSearchConnection;

    private EncryptionService encryptionService;

    private List<String> parameters;

    private String username;

    private String password;

    private String keystorePassword;

    private String truststorePassword;

    private String keystorePath;

    private String truststorePath;
    
    private long receiveTimeout = 0;

    /**
     * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
     * overwritten using the setter methods.
     * 
     * @throws ddf.catalog.source.UnsupportedQueryException
     */
    public OpenSearchSource(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    /**
     * Called when this OpenSearch Source is created, but after all of the setter methods have been
     * called for each property specified in the metatype.xml file.
     */
    public void init() {
        isInitialized = true;
        configureClient();
    }

    public void destroy() {
        LOGGER.info("Nothing to destroy.");
    }

    protected void configureClient() {
        openSearchConnection = new OpenSearchConnection(endpointUrl, filterAdapter, keystorePassword, keystorePath, truststorePassword, truststorePath, username, password, encryptionService);
    }

    @Override
    public boolean isAvailable() {
        boolean isAvailable = false;
        if (!lastAvailable
                || (lastAvailableDate.before(new Date(System.currentTimeMillis()
                        - AVAILABLE_TIMEOUT_CHECK)))) {

            WebClient client = openSearchConnection.getOpenSearchWebClient();

            Response response = null;
            try {
                response = client.head();
            } catch (ClientException e) {
                LOGGER.warn("Web Client was unable to connect to endpoint.", e);
            }

            if(response != null && !(response.getStatus() >= 404 || response.getStatus() == 400
                    || response.getStatus() == 402)) {
                isAvailable = true;
                lastAvailableDate = new Date();
            }
        } else {
            isAvailable = lastAvailable;
        }
        lastAvailable = isAvailable;
        return isAvailable;
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        if(isAvailable()) {
            callback.setAvailable();
            return true;
        } else {
            callback.setUnavailable();
            return false;
        }
    }

    @Override
    public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
        String methodName = "query";
        LOGGER.trace(methodName);

        Serializable metacardId = queryRequest.getPropertyValue(Metacard.ID);
        SourceResponseImpl response = null;

        WebClient client = openSearchConnection.getOpenSearchWebClient();

        Subject subject = null;
        if (queryRequest.hasProperties()) {
            Object subjectObj = queryRequest.getProperties()
                    .get(SecurityConstants.SECURITY_SUBJECT);
            subject = (Subject) subjectObj;
            client = openSearchConnection.setSubjectOnWebClient(client, subject);
        }

        Query query = queryRequest.getQuery();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received query: " + query);
        }

        boolean canDoOpenSearch = setOpenSearchParameters(query, subject, client);

        if(canDoOpenSearch) {

            Response clientResponse = client.get();

            Object obj = clientResponse.getEntity();

            response = new SourceResponseImpl(queryRequest,
                    new ArrayList<Result>());

            if (obj != null) {
                response = processResponse((InputStream) obj, queryRequest);
            }
        } else {
            Client restClient = openSearchConnection.newRestClient(endpointUrl,
                    queryRequest.getQuery(),
                    (String) metacardId, false);

            if (restClient != null) {
                WebClient webClient = openSearchConnection.getWebClientFromClient(restClient);

                if (queryRequest.hasProperties()) {
                    Object subjectObj = queryRequest.getProperties()
                            .get(SecurityConstants.SECURITY_SUBJECT);
                    subject = (Subject) subjectObj;
                    webClient = openSearchConnection.setSubjectOnWebClient(webClient, subject);
                }

                Response clientResponse = webClient.get();

                Object obj = clientResponse.getEntity();

                Metacard metacard = null;
                List<Result> resultQueue = new ArrayList<Result>();
                try {
                    metacard = inputTransformer.transform((InputStream) obj);
                } catch (IOException e) {
                    LOGGER.debug("Problem with transformation.", e);
                } catch (CatalogTransformerException e) {
                    LOGGER.debug("Problem with transformation.", e);
                }
                if (metacard != null) {
                    metacard.setSourceId(getId());
                    ResultImpl result = new ResultImpl(metacard);
                    resultQueue.add(result);
                    response = new SourceResponseImpl(queryRequest, resultQueue);
                    response.setHits(resultQueue.size());
                }
            }
        }

        LOGGER.trace(methodName);

        return response;
    }

    // Refactored from query() and made protected so JUnit tests could be written for this logic
    protected boolean setOpenSearchParameters(Query query, Subject subject, WebClient client) {
        if (LOGGER.isDebugEnabled()) {
            FilterTransformer transform = new FilterTransformer();
            transform.setIndentation(2);
            try {
                LOGGER.debug(transform.transform(query));
            } catch (TransformerException e) {
                LOGGER.debug("Error transforming query to XML", e);
            }
        }

        OpenSearchFilterVisitor visitor = new OpenSearchFilterVisitor();
        query.accept(visitor, null);

        ContextualSearch contextualFilter = visitor.getContextualSearch();

        //TODO fix this so we aren't just triggering off of a contextual query
        if(contextualFilter != null && StringUtils.isNotEmpty(contextualFilter.getSearchPhrase())) {
            // All queries must have at least a search phrase to be valid, hence this check
            // for a contextual filter with a non-empty search phrase
            OpenSearchSiteUtil.populateSearchOptions(client, query, subject, parameters);
            OpenSearchSiteUtil
                    .populateContextual(client, contextualFilter.getSearchPhrase(), parameters);

            TemporalFilter temporalFilter = visitor.getTemporalSearch();
            if (temporalFilter != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("startDate = " + temporalFilter.getStartDate().toString());
                    LOGGER.debug("endDate = " + temporalFilter.getEndDate().toString());
                }
                OpenSearchSiteUtil.populateTemporal(client, temporalFilter, parameters);
            }

            SpatialFilter spatialFilter = visitor.getSpatialSearch();
            if (spatialFilter != null) {
                if (spatialFilter instanceof SpatialDistanceFilter) {
                    try {
                        OpenSearchSiteUtil.populateGeospatial(client,
                                (SpatialDistanceFilter) spatialFilter, shouldConvertToBBox,
                                parameters);
                    } catch (UnsupportedQueryException e) {
                        LOGGER.info("Problem with populating geospatial criteria. ", e);
                    }
                } else {
                    try {
                        OpenSearchSiteUtil.populateGeospatial(client, spatialFilter,
                                shouldConvertToBBox, parameters);
                    } catch (UnsupportedQueryException e) {
                        LOGGER.info("Problem with populating geospatial criteria. ", e);
                    }
                }
            }

            if (localQueryOnly) {
                client.replaceQueryParam(URL_SRC_PARAMETER, LOCAL_SEARCH_PARAMETER);
            } else {
                client.replaceQueryParam(URL_SRC_PARAMETER, "");
            }
            return true;
        }
        return false;
    }

    /**
     * @param is
     * @param queryRequest
     * @return
     * @throws ddf.catalog.source.UnsupportedQueryException
     */
    private SourceResponseImpl processResponse(InputStream is, QueryRequest queryRequest) throws UnsupportedQueryException {
        List<Result> resultQueue = new ArrayList<Result>();

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Parser parser = null;
        org.apache.abdera.model.Document<Feed> atomDoc;
        try {

            Thread.currentThread().setContextClassLoader(OpenSearchSource.class.getClassLoader());
            parser = ABDERA.getParser();
            atomDoc = parser.parse(new InputStreamReader(is));

        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        Feed feed = atomDoc.getRoot();

        List<Entry> entries = feed.getEntries();
        for (Entry entry : entries) {
            resultQueue.add(createResponseFromEntry(entry));
        }

        long totalResults = entries.size();

        // OSGi has some weird issues with Abdera's XPath, so just traverse down the element tree
        Element totalResultsElement = atomDoc.getRoot().getExtension(
                OpenSearchConstants.TOTAL_RESULTS);

        if (totalResultsElement != null) {
            try {
                totalResults = Long.parseLong(totalResultsElement.getText());
            } catch (NumberFormatException e) {
                // totalResults is already initialized to the correct value, so don't change it
                // here.
                LOGGER.debug("Received invalid number of results.", e);
            }
        }

        SourceResponseImpl response = new SourceResponseImpl(queryRequest, resultQueue);
        response.setHits(totalResults);

        return response;
    }

    /**
     * Creates a single response from input parameters. Performs XPath operations on the document to
     * retrieve data not passed in.
     * 
     * @param entry
     *            a single Atom entry
     * @return single response
     * @throws ddf.catalog.source.UnsupportedQueryException
     */
    private Result createResponseFromEntry(Entry entry) throws UnsupportedQueryException {
        // id
        String id = entry.getId().getPath();
        // getPath() returns catalog:id:<id>, so we parse out the <id>
        if (id != null && !id.isEmpty()) {
            id = id.substring(id.lastIndexOf(':') + 1);
        }

        // content
        String content = null;
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {

            Thread.currentThread().setContextClassLoader(OpenSearchSource.class.getClassLoader());
            content = entry.getContent();

        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        final MetacardImpl metacard = getMetacardImpl(parseContent(content, id));

        metacard.setSourceId(this.shortname);

        // TODO resultSource. should we store this data?
        // QName resultSourceElementQName = new
        // QName("http://a9.com/-/opensearch/extensions/federation/1.0/", "resultSource", "fs");
        // QName sourceIdAttributeQName = new
        // QName("http://a9.com/-/opensearch/extensions/federation/1.0/", "sourceId", "fs");
        // String originSourceId =
        // entry.getExtension(resultSourceElementQName).getAttributeValue(sourceIdAttributeQName)

        // relevance
        QName relevanceElementQName = new QName(
                "http://a9.com/-/opensearch/extensions/relevance/1.0/", "score", "relevance");
        String relevance = entry.getSimpleExtension(relevanceElementQName);

        // title
        // only set this if it's unset
        String title = metacard.getTitle();
        if (title != null && !title.isEmpty()) {
            metacard.setTitle(entry.getTitle());
        }

        // updated
        // only set this if it's unset
        if (metacard.getModifiedDate() != null) {
            metacard.setModifiedDate(entry.getUpdated());
        }

        // published
        // only set this if it's unset
        if (metacard.getCreatedDate() != null) {
            metacard.setCreatedDate(entry.getPublished());
        }

        // category. maps to metadata-content-type

        String contentType = metacard.getContentTypeName();
//DDF-893        if (contentType != null && !contentType.isEmpty()) {
        if (StringUtils.isEmpty(contentType)) {
            ClassLoader tccl2 = Thread.currentThread().getContextClassLoader();
            try {

                Thread.currentThread().setContextClassLoader(
                        OpenSearchSource.class.getClassLoader());
                List<Category> categories = entry.getCategories();
                if (!categories.isEmpty() && categories.get(0) != null) {
                    String term = categories.get(0).toString();
                    //TODO Parse content type value from the <category> element's term attribute
                    // <category> element is of the format:
                    //     <category xmlns="http://www.w3.org/2005/Atom" term="collectorPosition" />
                    metacard.setContentTypeName(term);
                }

            } finally {
                Thread.currentThread().setContextClassLoader(tccl2);
            }

        }

        // TODO geos. potentially parse and use this if geos is missing from content

        ResultImpl result = new ResultImpl(metacard);
        if (relevance == null || relevance.isEmpty()) {
            LOGGER.debug("couldn't find valid relevance. Setting relevance to 0");
            relevance = "0";
        }
        result.setRelevanceScore(new Double(relevance));

        return result;
    }

    private MetacardImpl getMetacardImpl(Metacard oldMetacard) {
        MetacardImpl metacard;

        if (oldMetacard == null) {
            metacard = new MetacardImpl();
        } else {
            metacard = new MetacardImpl(oldMetacard);
        }

        return metacard;
    }

    // Update the WebClient with a Range header instructing the endpoint to skip bytesToSkip bytes.
    private void constructRangeHeader(WebClient webClient, Long bytesToSkip) {
        StringBuilder headerValue = new StringBuilder(BYTES_EQUAL);
        headerValue.append(bytesToSkip.toString());
        headerValue.append("-");

        webClient.header(HEADER_RANGE, headerValue.toString());
    }

    private Metacard parseContent(String content, String id) {
        if (inputTransformer != null && content != null && !content.isEmpty()) {
            try {
                return inputTransformer.transform(new ByteArrayInputStream(content.getBytes()), id);
            } catch (IOException e) {
                LOGGER.warn("Unable to read metacard content from Atom feed.", e);
            } catch (CatalogTransformerException e) {
                LOGGER.warn(
                        "Unable to convert metacard content from Atom feed into Metacard object.",
                        e);
            }
        }
        return null;
    }

    /**
     * Set URL of the endpoint.
     * 
     * @param endpointUrl
     *            Full url of the endpoint.
     */
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;

        if(isInitialized) {
            configureClient();
        }
    }

    /**
     * Get the URL of the endpoint.
     * 
     * @return
     */
    public String getEndpointUrl() {
        LOGGER.trace("getEndpointUrl:  endpointUrl = {}", endpointUrl);
        return endpointUrl;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getOrganization() {
        return ORGANIZATION;
    }

    @Override
    public String getId() {
        return shortname;
    }

    /**
     * Sets the shortname for this site. This shortname is used to identify the site when performing
     * federated queries.
     * 
     * @param shortname
     *            Name of this site.
     */
    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    public void setInputTransformer(InputTransformer inputTransformer) {
        this.inputTransformer = inputTransformer;
    }

    public InputTransformer getInputTransformer() {
        return inputTransformer;
    }

    /**
     * Sets the boolean flag that indicates all queries executed should be to its local source only,
     * i.e., no federated or enterprise queries.
     * 
     * @param localQueryOnly
     *            true indicates only local queries, false indicates enterprise query
     */
    public void setLocalQueryOnly(boolean localQueryOnly) {
        LOGGER.trace("Setting localQueryOnly = {}", localQueryOnly);
        this.localQueryOnly = localQueryOnly;
    }

    /**
     * Get the boolean flag that indicates only local queries are being executed by this OpenSearch
     * Source.
     * 
     * @return true indicates only local queries, false indicates enterprise query
     */
    public boolean getLocalQueryOnly() {
        return localQueryOnly;
    }

    /**
     * Sets the boolean flag that tells the code to convert point-radius and polygon geometries to a
     * bounding box before sending them.
     * 
     * @param shouldConvertToBBox
     */
    public void setShouldConvertToBBox(boolean shouldConvertToBBox) {
        this.shouldConvertToBBox = shouldConvertToBBox;
    }

    /**
     * Get the boolean flag that determines if point-radius and polygon geometries should be
     * converting to bounding boxes before sending.
     * 
     * @return
     */
    public boolean getShouldConvertToBBox() {
        return shouldConvertToBBox;
    }

    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> requestProperties)
            throws ResourceNotFoundException, ResourceNotSupportedException, IOException {

        Long bytesToSkip = Long.valueOf(0);
        final String methodName = "retrieveResource";
        LOGGER.entry(methodName);

        if (requestProperties == null) {
            throw new ResourceNotFoundException("Could not retrieve resource with null properties.");
        }

        Serializable serializableId = requestProperties.get(Metacard.ID);

        Subject subject = (Subject) requestProperties.get(SecurityConstants.SECURITY_SUBJECT);

        if (serializableId != null) {
            String metacardId = serializableId.toString();
            Client restClient = openSearchConnection.newRestClient(endpointUrl, null, metacardId, true);

            if (restClient != null) {
                Object binaryContent = null;
                MimeType mimeType = null;
                
                WebClient webClient = openSearchConnection.getWebClientFromClient(restClient);

                // If a bytesToSkip property is present add range header
                Map<String, Serializable> responseProperties = new HashMap<String, Serializable>();
                if (requestProperties.containsKey(BYTES_TO_SKIP)) {
                    bytesToSkip = (Long) requestProperties.get(BYTES_TO_SKIP);
                    LOGGER.debug("Setting Range header with bytes to skip: {}", bytesToSkip);
                    constructRangeHeader(webClient, bytesToSkip);
                }

                if(subject != null) {
                    webClient = openSearchConnection.setSubjectOnWebClient(webClient, subject);
                }

                Response clientResponse = null;
                try {
                    clientResponse = webClient.get();
                } catch (Exception e) {
                    LOGGER.warn("Error while trying to retreiveResource from OpenSearch Source", e);
                    throw new ResourceNotFoundException(COULD_NOT_RETRIEVE_RESOURCE_MESSAGE);
                }
                
                if (clientResponse == null) {
                    LOGGER.warn("Error while trying to retreiveResource from OpenSearch Source");
                    throw new ResourceNotFoundException(COULD_NOT_RETRIEVE_RESOURCE_MESSAGE);
                }

                Object contentType = clientResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE);
                try {
                    mimeType = new MimeType("application/octet-stream");
                    String content = null;
                    if(contentType != null) {
                        if(contentType instanceof String) {
                            content = (String) contentType;
                        } else if(contentType instanceof Collection && ((Collection) contentType).size() > 0) {
                            content = (String) ((Collection) contentType).iterator().next();
                        }
                    }
                    mimeType = new MimeType(content);
                } catch (MimeTypeParseException e) {
                    LOGGER.debug("Error creating mime type with input [{}] defaulting to {}",
                            contentType, "application/octet-stream");
                }

                binaryContent = clientResponse.getEntity();

                if (binaryContent != null) {

                    if (requestProperties.containsKey(BYTES_TO_SKIP)) {

                        // Since we sent a range header an accept-ranges header should be returned if the
                        // remote endpoint support it.  If is not present, the inputStream hasn't skipped ahead
                        // by the given number of bytes, so we need to take care of it here.
                        String rangeHeader = clientResponse.getHeaderString(HEADER_ACCEPT_RANGES);

                        //DDF-643: Set response property indicating remote JSON Source did the byte skipping
                        // so that Catalog Framework's download manager will not try to also skip bytes.
                        if ((rangeHeader != null) && (rangeHeader.equals(BYTES))) {
                            LOGGER.info("Adding {} to response properties with value = {}", BYTES_SKIPPED, true);
                            responseProperties.put(BYTES_SKIPPED, true);
                        }                       
                    }

                    LOGGER.exit(methodName);
                    
                    //DDF-643
                    ResourceResponseImpl resourceResponse = new ResourceResponseImpl(new ResourceImpl(
                            (InputStream) binaryContent, mimeType, getId()
                                    + "_Resource_Retrieval:" + System.currentTimeMillis()));
                    resourceResponse.setProperties(responseProperties);
                    return resourceResponse;
                }
            }
        }

        LOGGER.exit(methodName);
        throw new ResourceNotFoundException(COULD_NOT_RETRIEVE_RESOURCE_MESSAGE);
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        LOGGER.trace("ENTERING/EXITING: getOptions");
        LOGGER.debug("OpenSearch Source \"{}\" does not support resource retrieval options.", getId());
        return Collections.emptySet();
    }

    @Override
    public String getConfigurationPid()
    {
        return configurationPid;
    }

    @Override
    public void setConfigurationPid(String configurationPid)
    {
        this.configurationPid = configurationPid;
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> properties) {
        String setTrustStorePath = properties.get(ConfigurationManager.TRUST_STORE);
        if (StringUtils.isNotBlank(setTrustStorePath)) {
            LOGGER.debug("Setting trust store path: " + setTrustStorePath);
            truststorePath = setTrustStorePath;
        }

        String setTrustStorePassword = properties
                .get(ConfigurationManager.TRUST_STORE_PASSWORD);
        if (StringUtils.isNotBlank(setTrustStorePassword)) {
            if (openSearchConnection.getEncryptionService() == null) {
                LOGGER.error(
                        "The StsRealm has a null Encryption Service. Unable to decrypt the encrypted "
                                + "trustStore password. Setting decrypted password to null.");
                truststorePassword = setTrustStorePassword;
            } else {
                setTrustStorePassword = openSearchConnection.getEncryptionService().decryptValue(setTrustStorePassword);
                LOGGER.debug("Setting trust store password.");
                truststorePassword = setTrustStorePassword;
            }
        }

        String setKeyStorePath = properties.get(ConfigurationManager.KEY_STORE);
        if (StringUtils.isNotBlank(setKeyStorePath)) {
            LOGGER.debug("Setting key store path: " + setKeyStorePath);
            openSearchConnection.setKeyStorePath(setKeyStorePath);
            keystorePath = setKeyStorePath;
        }

        String setKeyStorePassword = properties
                .get(ConfigurationManager.KEY_STORE_PASSWORD);
        if (StringUtils.isNotBlank(setKeyStorePassword)) {
            if (openSearchConnection.getEncryptionService() == null) {
                LOGGER.error(
                        "The StsRealm has a null Encryption Service. Unable to decrypt the encrypted "
                                + "keyStore password. Setting decrypted password to null.");
                keystorePassword = setKeyStorePassword;
            } else {
                setKeyStorePassword = openSearchConnection.getEncryptionService().decryptValue(setKeyStorePassword);
                LOGGER.debug("Setting key store password.");
                keystorePassword = setKeyStorePassword;
            }
        }

        if(isInitialized) {
            configureClient();
        }
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        // workaround for KARAF-1701
        if (parameters.size() == 1 && parameters.get(0).contains(",")) {
            this.parameters = Arrays.asList(parameters.get(0).split(","));
        } else {
            this.parameters = parameters;
        }
    }

    public void setParameters(String parameters) {
        this.parameters = Arrays.asList(parameters.split(","));
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Sets the receive timeout for messages sent from this provider.
     *
     * @param receiveTimeout timeout in milliseconds. 0 sets it to NOT timeout.
     */
    public void setReceiveTimeout(long receiveTimeout) {
        LOGGER.debug("Setting timeout to {}", receiveTimeout);
        this.receiveTimeout = receiveTimeout;
    }

    /**
     * Gets the receive timeout that is being used for current messages.
     *
     * @return the timeout in milliseconds.
     */
    public long getReceiveTimeout() {
        return this.receiveTimeout;
    }
}
