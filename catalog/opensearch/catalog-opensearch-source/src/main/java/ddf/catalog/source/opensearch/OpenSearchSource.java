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
package ddf.catalog.source.opensearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.stax2.XMLInputFactory2;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.geotools.filter.FilterTransformer;
import org.jdom2.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.FileBackedOutputStream;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

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
import ddf.security.settings.SecuritySettingsService;

/**
 * Federated site that talks via OpenSearch to the DDF platform. Communication is usually performed
 * via https which requires a keystore and trust store to be provided.
 *
 */
public class OpenSearchSource implements FederatedSource, ConfiguredService {

    public static final String NAME = "name";

    public static final String BYTES_TO_SKIP = "BytesToSkip";

    public static final String BYTES_SKIPPED = "BytesSkipped";

    static final String BAD_URL_MESSAGE = "Bad url given for remote source";

    static final String COULD_NOT_RETRIEVE_RESOURCE_MESSAGE = "Could not retrieve resource";

    static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

    static final String BYTES = "bytes";

    private static final String ORGANIZATION = "DDF";

    private static final String TITLE = "OpenSearch DDF Federated Source";

    private static final String DESCRIPTION = "Queries DDF using the synchronous federated OpenSearch query";

    private static final long AVAILABLE_TIMEOUT_CHECK = 60000; // 60 seconds, in milliseconds

    private static final String URL_SRC_PARAMETER = "src";

    private static final String LOCAL_SEARCH_PARAMETER = "local";

    private static final String HEADER_RANGE = "Range";

    private static final String BYTES_EQUAL = "bytes=";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchSource.class);

    protected OpenSearchConnection openSearchConnection;

    private boolean isInitialized = false;

    // service properties
    private String shortname;

    private boolean lastAvailable;

    private Date lastAvailableDate = null;

    private boolean localQueryOnly;

    private boolean shouldConvertToBBox;

    private String endpointUrl;

    private FilterAdapter filterAdapter;

    private String configurationPid;

    private SecuritySettingsService securitySettingsService;

    private List<String> parameters;

    private String username;

    private String password;

    private long receiveTimeout = 0;

    private XMLInputFactory xmlInputFactory;

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
        configureXmlInputFactory();
    }

    private void configureXmlInputFactory() {
        xmlInputFactory = XMLInputFactory2.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);

    }

    public void destroy() {
        LOGGER.info("Nothing to destroy.");
    }

    protected void configureClient() {
        openSearchConnection = new OpenSearchConnection(endpointUrl, filterAdapter,
                securitySettingsService, username, password);
    }

    @Override
    public boolean isAvailable() {
        boolean isAvailable = false;
        if (!lastAvailable || (lastAvailableDate
                .before(new Date(System.currentTimeMillis() - AVAILABLE_TIMEOUT_CHECK)))) {

            WebClient client = openSearchConnection.getOpenSearchWebClient();

            Response response = null;
            try {
                response = client.head();
            } catch (Exception e) {
                LOGGER.warn("Web Client was unable to connect to endpoint.", e);
            }

            if (response != null && !(response.getStatus() >= 404 || response.getStatus() == 400
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
        if (isAvailable()) {
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

        WebClient openSearchWebClient = openSearchConnection.getOpenSearchWebClient();

        Subject subject = null;
        if (queryRequest.hasProperties()) {
            Object subjectObj = queryRequest.getProperties()
                    .get(SecurityConstants.SECURITY_SUBJECT);
            subject = (Subject) subjectObj;
            RestSecurity.setSubjectOnClient(subject, openSearchWebClient);
        }

        Query query = queryRequest.getQuery();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received query: " + query);
        }

        boolean canDoOpenSearch = setOpenSearchParameters(query, subject, openSearchWebClient);

        if (canDoOpenSearch) {

            InputStream responseStream = performRequest(openSearchWebClient);

            response = new SourceResponseImpl(queryRequest, new ArrayList<Result>());

            if (responseStream != null) {
                response = processResponse(responseStream, queryRequest);
            }
        } else {
            Client restClient = openSearchConnection
                    .newRestClient(endpointUrl, queryRequest.getQuery(), (String) metacardId,
                            false);

            if (restClient != null) {
                WebClient restWebClient = openSearchConnection.getWebClientFromClient(restClient);

                if (queryRequest.hasProperties()) {
                    Object subjectObj = queryRequest.getProperties()
                            .get(SecurityConstants.SECURITY_SUBJECT);
                    subject = (Subject) subjectObj;
                    RestSecurity.setSubjectOnClient(subject, restWebClient);
                }

                InputStream responseStream = performRequest(restWebClient);

                Metacard metacard = null;
                List<Result> resultQueue = new ArrayList<Result>();
                try (FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(
                        1000000)) {
                    IOUtils.copyLarge(responseStream, fileBackedOutputStream);
                    InputTransformer inputTransformer = null;
                    try (InputStream inputStream = fileBackedOutputStream.asByteSource()
                            .openStream()) {
                        inputTransformer = getInputTransformer(inputStream);
                    } catch (IOException e) {
                        LOGGER.debug("Problem with transformation.", e);
                    }
                    if (inputTransformer != null) {
                        try (InputStream inputStream = fileBackedOutputStream.asByteSource()
                                .openStream()) {
                            metacard = inputTransformer.transform(inputStream);
                        } catch (IOException e) {
                            LOGGER.debug("Problem with transformation.", e);
                        }
                    }
                } catch (IOException | CatalogTransformerException e) {
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

    /**
     * Performs a GET request on the client and returns the entity as an InputStream.
     *
     * @param client Client to perform the GET request on.
     * @return The entity of the response as an InputStream.
     * @throws UnsupportedQueryException
     */
    private InputStream performRequest(WebClient client) throws UnsupportedQueryException {
        Response clientResponse = client.get();

        InputStream stream = null;
        Object entityObj = clientResponse.getEntity();
        if (entityObj != null) {
            stream = (InputStream) entityObj;
        }
        if (Response.Status.OK.getStatusCode() != clientResponse.getStatus()) {
            String error = "";
            try {
                if (stream != null) {
                    error = IOUtils.toString(stream);
                }
            } catch (IOException ioe) {
                LOGGER.debug("Could not convert error message to a string for output.", ioe);
            }
            String errorMsg =
                    "Received error code from remote source (status " + clientResponse.getStatus()
                            + "): " + error;
            LOGGER.warn(errorMsg);
            throw new UnsupportedQueryException(errorMsg);
        }

        return stream;
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
        if (contextualFilter != null && StringUtils
                .isNotEmpty(contextualFilter.getSearchPhrase())) {
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
                        OpenSearchSiteUtil
                                .populateGeospatial(client, (SpatialDistanceFilter) spatialFilter,
                                        shouldConvertToBBox, parameters);
                    } catch (UnsupportedQueryException e) {
                        LOGGER.info("Problem with populating geospatial criteria. ", e);
                    }
                } else {
                    try {
                        OpenSearchSiteUtil
                                .populateGeospatial(client, spatialFilter, shouldConvertToBBox,
                                        parameters);
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
    private SourceResponseImpl processResponse(InputStream is, QueryRequest queryRequest)
            throws UnsupportedQueryException {
        List<Result> resultQueue = new ArrayList<>();

        SyndFeedInput syndFeedInput = new SyndFeedInput();
        SyndFeed syndFeed = null;
        try {
            syndFeed = syndFeedInput.build(new InputStreamReader(is));
        } catch (FeedException e) {
            LOGGER.error("Unable to read RSS/Atom feed.", e);
        }

        List<SyndEntry> entries = null;
        long totalResults = 0;
        if (syndFeed != null) {
            entries = syndFeed.getEntries();
            for (SyndEntry entry : entries) {
                resultQueue.addAll(createResponseFromEntry(entry));
            }
            totalResults = entries.size();
            List<Element> foreignMarkup = syndFeed.getForeignMarkup();
            for (Element element : foreignMarkup) {
                if (element.getName().equals("totalResults")) {
                    try {
                        totalResults = Long.parseLong(element.getContent(0).getValue());
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        // totalResults is already initialized to the correct value, so don't change it here.
                        LOGGER.debug("Received invalid number of results.", e);
                    }
                }
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
    private List<Result> createResponseFromEntry(SyndEntry entry) throws UnsupportedQueryException {
        String id = entry.getUri();
        if (id != null && !id.isEmpty()) {
            id = id.substring(id.lastIndexOf(':') + 1);
        }

        List<SyndContent> contents = entry.getContents();
        List<SyndCategory> categories = entry.getCategories();
        List<Metacard> metacards = new ArrayList<>();
        List<Element> foreignMarkup = entry.getForeignMarkup();
        String relevance = "";
        String source = "";
        for (Element element : foreignMarkup) {
            if (element.getName().equals("score")) {
                relevance = element.getContent(0).getValue();
            }
        }
        //we currently do not support downloading content via an RSS enclosure, this support can be added at a later date if we decide to include it
        for (SyndContent content : contents) {
            MetacardImpl metacard = getMetacardImpl(parseContent(content.getValue(), id));
            metacard.setSourceId(this.shortname);
            String title = metacard.getTitle();
            if (StringUtils.isEmpty(title)) {
                metacard.setTitle(entry.getTitle());
            }
            if (!source.isEmpty()) {
                metacard.setSourceId(source);
            }
            metacards.add(metacard);
        }
        for (int i = 0; i < categories.size() && i < metacards.size(); i++) {
            SyndCategory category = categories.get(i);
            Metacard metacard = metacards.get(i);
            if (StringUtils.isBlank(metacard.getContentTypeName())) {
                ((MetacardImpl) metacard).setContentTypeName(category.getName());
            }
        }

        List<Result> results = new ArrayList<>();
        for (Metacard metacard : metacards) {
            ResultImpl result = new ResultImpl(metacard);
            if (relevance == null || relevance.isEmpty()) {
                LOGGER.debug("couldn't find valid relevance. Setting relevance to 0");
                relevance = "0";
            }
            result.setRelevanceScore(new Double(relevance));
            results.add(result);
        }

        return results;
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
        if (content != null) {
            InputTransformer inputTransformer = getInputTransformer(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            if (inputTransformer != null && !content.isEmpty()) {
                try {
                    return inputTransformer.transform(
                            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), id);
                } catch (IOException e) {
                    LOGGER.warn("Unable to read metacard content from Atom feed.", e);
                } catch (CatalogTransformerException e) {
                    LOGGER.warn(
                            "Unable to convert metacard content from Atom feed into Metacard object.",
                            e);
                }
            }
        }
        return null;
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

    /**
     * Set URL of the endpoint.
     *
     * @param endpointUrl
     *            Full url of the endpoint.
     */
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;

        if (isInitialized) {
            configureClient();
        }
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

    private InputTransformer getInputTransformer(InputStream inputStream) {
        XMLStreamReader xmlStreamReader = null;
        try {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();
                if (next == XMLStreamConstants.START_ELEMENT) {
                    String namespaceUri = xmlStreamReader.getNamespaceURI();
                    InputTransformer transformerReference = lookupTransformerReference(
                            namespaceUri);
                    if (transformerReference != null) {
                        return transformerReference;
                    }
                }
            }
        } catch (XMLStreamException | InvalidSyntaxException e) {
            LOGGER.error("Failed to parse transformer namespace", e);
        } finally {
            try {
                if (xmlStreamReader != null) {
                    xmlStreamReader.close();
                }
            } catch (XMLStreamException e) {
                LOGGER.error("failed to close namespace reader", e);
            }
        }
        return null;
    }

    protected InputTransformer lookupTransformerReference(String namespaceUri)
            throws InvalidSyntaxException {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            Collection<ServiceReference<InputTransformer>> transformerReference = bundleContext
                    .getServiceReferences(InputTransformer.class, "(schema=" + namespaceUri + ")");
            if (transformerReference.size() == 1) {
                return bundleContext.getService(transformerReference.iterator().next());
            } else if (transformerReference.size() > 1) {
                LOGGER.error("ambiguous transformer schema " + namespaceUri);
            }
        }
        return null;
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
     * Get the boolean flag that determines if point-radius and polygon geometries should be
     * converting to bounding boxes before sending.
     *
     * @return
     */
    public boolean getShouldConvertToBBox() {
        return shouldConvertToBBox;
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

    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> requestProperties)
            throws ResourceNotFoundException, ResourceNotSupportedException, IOException {

        Long bytesToSkip = Long.valueOf(0);
        final String methodName = "retrieveResource";
        LOGGER.trace("ENTRY: {}", methodName);

        if (requestProperties == null) {
            throw new ResourceNotFoundException(
                    "Could not retrieve resource with null properties.");
        }

        Serializable serializableId = requestProperties.get(Metacard.ID);

        Subject subject = (Subject) requestProperties.get(SecurityConstants.SECURITY_SUBJECT);

        if (serializableId != null) {
            String metacardId = serializableId.toString();
            Client restClient = openSearchConnection
                    .newRestClient(endpointUrl, null, metacardId, true);

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

                if (subject != null) {
                    RestSecurity.setSubjectOnClient(subject, webClient);
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
                    if (contentType != null) {
                        if (contentType instanceof String) {
                            content = (String) contentType;
                        } else if (contentType instanceof Collection
                                && ((Collection) contentType).size() > 0) {
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
                            LOGGER.info("Adding {} to response properties with value = {}",
                                    BYTES_SKIPPED, true);
                            responseProperties.put(BYTES_SKIPPED, true);
                        }
                    }

                    LOGGER.trace("EXIT: {}", methodName);

                    //DDF-643
                    ResourceResponseImpl resourceResponse = new ResourceResponseImpl(
                            new ResourceImpl((InputStream) binaryContent, mimeType,
                                    getId() + "_Resource_Retrieval:" + System.currentTimeMillis()));
                    resourceResponse.setProperties(responseProperties);
                    return resourceResponse;
                }
            }
        }

        LOGGER.trace("EXIT: {}", methodName);
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
        LOGGER.debug("OpenSearch Source \"{}\" does not support resource retrieval options.",
                getId());
        return Collections.emptySet();
    }

    @Override
    public String getConfigurationPid() {
        return configurationPid;
    }

    @Override
    public void setConfigurationPid(String configurationPid) {
        this.configurationPid = configurationPid;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = Arrays.asList(parameters.split(","));
    }

    public void setParameters(List<String> parameters) {
        // workaround for KARAF-1701
        if (parameters.size() == 1 && parameters.get(0).contains(",")) {
            this.parameters = Arrays.asList(parameters.get(0).split(","));
        } else {
            this.parameters = parameters;
        }
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
     * Gets the receive timeout that is being used for current messages.
     *
     * @return the timeout in milliseconds.
     */
    public long getReceiveTimeout() {
        return this.receiveTimeout;
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

    public void setSecuritySettings(SecuritySettingsService settingsService) {
        this.securitySettingsService = settingsService;
    }
}
