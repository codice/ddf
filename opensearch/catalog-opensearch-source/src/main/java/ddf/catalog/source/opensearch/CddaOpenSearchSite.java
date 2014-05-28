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

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.encryption.EncryptionService;
import ddf.util.XPathHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.geotools.filter.FilterTransformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * Federated site that talks via OpenSearch to the DDF platform. Communication is usually performed
 * via https which requires a keystore and trust store to be provided.
 * 
 */
public final class CddaOpenSearchSite implements FederatedSource, ConfiguredService,
        ConfigurationWatcher {
    private boolean isInitialized = false;

    // service properties
    private String shortname;

    private boolean lastAvailable;

    private Date lastAvailableDate;

    private boolean localQueryOnly;

    private boolean shouldConvertToBBox;

    private String endpointUrl;

    private String classification = "U";

    private String ownerProducer = "USA";

    private static final String ORGANIZATION = "DDF";

    private static final String TITLE = "OpenSearch DDF Federated Source";

    private static final String DESCRIPTION = "Queries DDF using the synchronous federated OpenSearch query";

    private static final long AVAILABLE_TIMEOUT_CHECK = 60000; // 60 seconds, in milliseconds

    private static final String DEFAULT_SITE_SECURITY_NAME = "ddf.DefaultSiteSecurity";

    // local variables
    private static final String NORMALIZE_XSLT = "atom2ddms.xsl";

    private static final String URL_SRC_PARAMETER = "src";

    private static final String LOCAL_SEARCH_PARAMETER = "local";

    private Document normalizeXslt;

    private static final transient Logger LOGGER = LoggerFactory.getLogger(CddaOpenSearchSite.class);

    private javax.xml.xpath.XPath xpath;

    private TransformerFactory tf = TransformerFactory.newInstance(
            net.sf.saxon.TransformerFactoryImpl.class.getName(), getClass().getClassLoader());

    private Configuration siteSecurityConfig;

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

    /**
     * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
     * overwritten using the setter methods.
     * 
     * @throws UnsupportedQueryException
     */
    public CddaOpenSearchSite() throws UnsupportedQueryException {
        lastAvailableDate = null;
        InputStream xsltStream = getClass().getResourceAsStream("/" + NORMALIZE_XSLT);
        try {
            normalizeXslt = OpenSearchSiteUtil.convertStreamToDocument(xsltStream);
        } catch (ConversionException ce) {
            throw new UnsupportedQueryException(
                    "Could not parse setup files, cannot talk to federated site.", ce);
        } finally {
            IOUtils.closeQuietly(xsltStream);
        }
        XPathFactory xpFactory = XPathFactory.newInstance();
        xpath = xpFactory.newXPath();

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
        openSearchConnection = new OpenSearchConnection(endpointUrl, null, keystorePassword, keystorePath, truststorePassword, truststorePath, username, password, encryptionService);
    }

    /**
     * Sets the context that will be used to get a reference to the ConfigurationAdmin. This allows
     * the security settings to be obtained.
     * 
     * @param context
     *            BundleContext that can retrieve a ConfigurationAdmin.
     */
    public void setContext(BundleContext context) {
        try {
            ServiceReference configAdminServiceRef = context
                    .getServiceReference(ConfigurationAdmin.class.getName());
            if (configAdminServiceRef != null) {
                ConfigurationAdmin ca = (ConfigurationAdmin) context
                        .getService(configAdminServiceRef);
                LOGGER.debug("configuration admin obtained: " + ca);
                if (ca != null) {
                    siteSecurityConfig = ca.getConfiguration(DEFAULT_SITE_SECURITY_NAME);
                    LOGGER.debug("site security config obtained: " + siteSecurityConfig);
                    // updateDefaultClassification();
                }
            }
        } catch (IOException ioe) {
            LOGGER.warn("Unable to obtain the configuration admin");
        }
    }

    private Map<String, String> updateDefaultClassification() {
        HashMap<String, String> securityProps = new HashMap<String, String>();
        LOGGER.debug("Assigning default classification values");
        if (siteSecurityConfig != null) {
            LOGGER.debug("setting properties from config admin");
            try {
                // siteSecurityConfig.update();
                @SuppressWarnings("unchecked")
                Dictionary<String, Object> propertyDictionary = (Dictionary<String, Object>) siteSecurityConfig
                        .getProperties();
                Enumeration<String> propertyKeys = propertyDictionary.keys();
                while (propertyKeys.hasMoreElements()) {
                    String currKey = propertyKeys.nextElement();
                    String currValue = propertyDictionary.get(currKey).toString();
                    securityProps.put(currKey, currValue);
                }

                LOGGER.debug("security properties: " + securityProps);

            } catch (Exception e) {
                LOGGER.warn(
                        "Exception thrown while trying to obtain default properties.  "
                                + "Setting all default classifications and owner/producers to U and USA respectively as a last resort.",
                        e);
                securityProps.clear(); // this is being cleared, so the
                                       // "last-resort" defaults specified in
                                       // the xsl will be used.

            }
        } else {
            LOGGER.info("site security config is null");
            securityProps.clear(); // this is being cleared, so the
                                   // "last-resort" defaults specified in the
                                   // xsl will be used.

        }
        return securityProps;
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
            } catch (Fault e) {
                LOGGER.warn("", e);
            }

            if(response != null && !(response.getStatus() >= 400)) {
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

        SourceResponseImpl response = new SourceResponseImpl(queryRequest, new ArrayList<Result>());

        WebClient client = openSearchConnection.getOpenSearchWebClient();

        ddf.security.Subject subject = null;
        if (queryRequest.hasProperties()) {
            Object subjectObj = queryRequest.getProperties()
                    .get(SecurityConstants.SECURITY_SUBJECT);
            subject = (ddf.security.Subject) subjectObj;
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
                try {
                    response = processResponse((InputStream) obj, queryRequest);
                } catch (ConversionException e) {
                    LOGGER.warn("Error occurred while trying to translate response from server, marking site as NOT available", e);
                    lastAvailable = false;
                }
            }
        }

        return response;
    }

    // Refactored from query() and made protected so JUnit tests could be written for this logic
    protected boolean setOpenSearchParameters(Query query, ddf.security.Subject subject, WebClient client) {
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
     * @throws UnsupportedQueryException
     */
    private SourceResponseImpl processResponse(InputStream is, QueryRequest queryRequest)
        throws ConversionException {
        List<Result> resultQueue = new ArrayList<Result>();
        Document atomDoc = null;

        try {
            atomDoc = OpenSearchSiteUtil.convertStreamToDocument(is);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Incoming response from OpenSearch site: "
                        + XPathHelper.xmlToString(atomDoc));
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

        Map<String, String> securityProps = updateDefaultClassification();

        Document ddmsDoc = OpenSearchSiteUtil.normalizeAtomToDDMS(tf, atomDoc, normalizeXslt,
                securityProps);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Incoming response from OpenSearch site normalized to DDMS: "
                    + XPathHelper.xmlToString(ddmsDoc));
        }
        NodeList list = ddmsDoc.getElementsByTagNameNS("http://metadata.dod.mil/mdr/ns/DDMS/2.0/",
                "Resource");

        String resultNum = evaluate("//opensearch:totalResults", atomDoc);
        long totalResults = 0;
        if (resultNum != null && !resultNum.isEmpty()) {
            totalResults = Integer.parseInt(resultNum);
        } else {
            // if no openseach:totalResults element, spec says to use list
            // of current items as totalResults
            totalResults = list.getLength();
        }

        // offset always comes in as 0 from DDF federation logic
        for (int i = 0; i < list.getLength(); i++) {
            try {
                Node curNode = list.item(i);
                String relevance = OpenSearchSiteUtil.popAttribute(curNode, "score");
                String id = OpenSearchSiteUtil.popAttribute(curNode, "id");
                String date = OpenSearchSiteUtil.popAttribute(curNode, "date");
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .newDocument();
                doc.appendChild(doc.importNode(curNode, true));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(XPathHelper.xmlToString(doc));
                }
                resultQueue.add(createResponse(doc, id, date, relevance));
            } catch (ParserConfigurationException pce) {
                throw new ConversionException("Couldn't convert node to document. ", pce);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Returning " + list.getLength() + " entries in response.");
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
     * @param inputDoc
     *            DDMS based Document
     * @param id
     *            id for the entry
     * @param dateStr
     *            String value of the date
     * @param relevance
     *            Relevance score (0 if N/A)
     * @return single response
     */
    private Result createResponse(Document inputDoc, String id, String dateStr, String relevance) {
        final MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata(XPathHelper.xmlToString(inputDoc));
        String title = evaluate(OpenSearchSiteUtil.XPATH_TITLE, inputDoc);

        metacard.setTitle(title);
        metacard.setId(id);

        Date modifiedDate = OpenSearchSiteUtil.parseDate(dateStr);
        if (modifiedDate != null) {
            metacard.setModifiedDate(modifiedDate);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("title = [" + title + "]");
            LOGGER.debug("id = [" + id + "]");
            LOGGER.debug("date = [" + dateStr + "]");

            if (modifiedDate != null) {
                LOGGER.debug("modifiedDate = " + modifiedDate.toString());
            } else {
                LOGGER.debug("modifiedDate is NULL");
            }
        }

        metacard.setSourceId(this.shortname);

        ResultImpl result = new ResultImpl(metacard);
        if (relevance == null || relevance.isEmpty()) {
            LOGGER.debug("couldn't find valid relevance. Setting relevance to 0");
            relevance = "0";
        }
        result.setRelevanceScore(new Double(relevance));

        return result;
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

    /**
     * Perform xpath evaluation and return value as a string.
     * 
     * @param xpathExpression
     * @param node
     * @return result of xpath evaluation
     * @throws XPathExpressionException
     */
    private String evaluate(String xpathExpression, Node node) {
        String result = "";
        synchronized (xpath) {
            try {
                xpath.setNamespaceContext(new OpenSearchNamespaceContext());
                result = xpath.evaluate(xpathExpression, node);
            } catch (XPathExpressionException xpee) {
                LOGGER.warn("Error while performing xpath, result may be missing information.",
                        xpee);
            } finally {
                xpath.reset();
            }

        }
        return result;
    }

    /**
     * Set URL of the endpoint.
     * 
     * @param endpointUrl
     *            Full url of the endpoint.
     */
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;

        // If the source is already initialized, adjust the endpoint URL
        if (isInitialized) {
            configureClient();
        }
    }

    /**
     * Get the URL of the endpoint.
     * 
     * @return
     */
    public String getEndpointUrl() {
        LOGGER.trace("getEndpointUrl:  endpointUrl = " + endpointUrl);
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
        return "1.0";
    }

    /**
     * Sets default classification in document from server.
     * 
     * @param classification
     *            default classification in string format
     */
    public void setClassification(String classification) {
        this.classification = classification;
    }

    /**
     * Get default classification.
     * 
     * @return default classification
     */
    public String getClassification() {
        return this.classification;
    }

    /**
     * Sets default ownerProducer in document from server.
     * 
     * @param ownerProducer
     *            default ownerProducer in string format
     */
    public void setOwnerProducer(String ownerProducer) {
        this.ownerProducer = ownerProducer;
    }

    /**
     * Get default ownerProducer.
     * 
     * @return default ownerProducer
     */
    public String getOwnerProducer() {
        return this.ownerProducer;
    }

    /**
     * Sets the boolean flag that indicates all queries executed should be to its local source only,
     * i.e., no federated or enterprise queries.
     * 
     * @param localQueryOnly
     *            true indicates only local queries, false indicates enterprise query
     */
    public void setLocalQueryOnly(boolean localQueryOnly) {
        LOGGER.trace("Setting localQueryOnly = " + localQueryOnly);
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
        throws ResourceNotFoundException, ResourceNotSupportedException {

        throw new ResourceNotFoundException("This source does not support resource retrieval.");
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
        LOGGER.debug("OpenSearch Source \"" + getId()
                + "\" does not support resource retrieval options.");
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

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
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
}
