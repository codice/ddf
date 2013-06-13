/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source.opensearch;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.commons.io.IOUtils;
import org.geotools.filter.FilterTransformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.Result;
import ddf.catalog.data.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.ResourceResponseImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.SourceResponseImpl;
import ddf.catalog.resource.ResourceImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;


/**
 * Federated site that talks via OpenSearch to the DDF platform. Communication is
 * usually performed via https which requires a keystore and trust store to be provided.
 *
 */
public final class OpenSearchSource implements FederatedSource
{
    static final String BAD_URL_MESSAGE = "Bad url given for remote source";
    static final String COULD_NOT_RETRIEVE_RESOURCE_MESSAGE = "Could not retrieve resource";
    public static final String NAME = "name";
    private boolean isInitialized = false;

    // service properties
    private String shortname;
    private String version;
    private boolean lastAvailable;
    private Date lastAvailableDate;
    private boolean localQueryOnly;
    private boolean shouldConvertToBBox;
    private String endpointUrl;
    private String classification = "U";
    private String ownerProducer = "USA";

    private InputTransformer inputTransformer;
    private static final String ORGANIZATION = "DDF";
    private static final String TITLE = "OpenSearch DDF Federated Source";
    private static final String DESCRIPTION = "Queries DDF using the synchronous federated OpenSearch query";
    private static final long AVAILABLE_TIMEOUT_CHECK = 60000;  // 60 seconds, in milliseconds
	private static final String DEFAULT_SITE_SECURITY_NAME = "ddf.DefaultSiteSecurity";

    private static final String URL_PARAMETERS_DELIMITER = "?";
    private static final String URL_PARAMETER_SEPARATOR = "&";
    private static final String URL_SRC_PARAMETER = "src=";
    private static final String LOCAL_SEARCH_PARAMETER = URL_SRC_PARAMETER + "local";

    private static XLogger logger = new XLogger( LoggerFactory.getLogger( OpenSearchSource.class ) );

    private javax.xml.xpath.XPath xpath;
    private Configuration siteSecurityConfig;
    private SecureRemoteConnection connection;
    private FilterAdapter filterAdapter;
    private boolean isRestSearch;
    
    // expensive creation, meant to be done once
    private static final Abdera ABDERA = new Abdera();

    /**
     * Creates an OpenSearch Site instance. Sets an initial default
     * endpointUrl that can be overwritten using the setter methods.
     *
     * @throws ddf.catalog.source.UnsupportedQueryException
     */
    public OpenSearchSource(SecureRemoteConnection connection, FilterAdapter filterAdapter)
    {
        this.version = "1.0";
        this.filterAdapter= filterAdapter;
        this.connection = connection;
        endpointUrl = "https://example.com?q={searchTerms}&src={fs:routeTo?}&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        lastAvailableDate = null;

        XPathFactory xpFactory = XPathFactory.newInstance();
        xpath = xpFactory.newXPath();
    }

    /**
     * Called when this OpenSearch Source is created, but after all of the
     * setter methods have been called for each property specified in the
     * metatype.xml file.
     */
    public void init()
    {
        isInitialized = true;
        configureEndpointUrl();
    }


    public void destroy()
    {
        logger.info( "Nothing to destroy.");
    }

    /**
     * Sets the context that will be used to get a reference to the
     * ConfigurationAdmin. This allows the security settings to be obtained.
     *
     * @param context BundleContext that can retrieve a ConfigurationAdmin.
     */
    public void setContext( BundleContext context )
    {
        try
        {
            ServiceReference configAdminServiceRef = context.getServiceReference( ConfigurationAdmin.class.getName() );
            if ( configAdminServiceRef != null )
            {
                ConfigurationAdmin ca = (ConfigurationAdmin) context.getService( configAdminServiceRef );
                logger.debug( "configuration admin obtained: " + ca );
                if ( ca != null )
                {
                    siteSecurityConfig = ca.getConfiguration( DEFAULT_SITE_SECURITY_NAME );
                    logger.debug( "site security config obtained: " + siteSecurityConfig );
                    // updateDefaultClassification();
                }
            }
        }
        catch ( IOException ioe )
        {
            logger.warn( "Unable to obtain the configuration admin" );
        }
    }


    private Map<String, String> updateDefaultClassification()
    {
        HashMap<String, String> securityProps = new HashMap<String, String>();
        logger.debug( "Assigning default classification values" );
        if ( siteSecurityConfig != null )
        {
            logger.debug( "setting properties from config admin" );
            try
            {
                // siteSecurityConfig.update();
                @SuppressWarnings( "unchecked" )
                Dictionary<String, Object> propertyDictionary = (Dictionary<String, Object>) siteSecurityConfig
                    .getProperties();
                Enumeration<String> propertyKeys = propertyDictionary.keys();
                while ( propertyKeys.hasMoreElements() )
                {
                    String currKey = propertyKeys.nextElement();
                    String currValue = propertyDictionary.get( currKey ).toString();
                    securityProps.put( currKey, currValue );
                }

                logger.debug( "security properties: " + securityProps );

            }
            catch ( Exception e )
            {
                logger
                    .warn(
                        "Exception thrown while trying to obtain default properties.  "
                                + "Setting all default classifications and owner/producers to U and USA respectively as a last resort.",
                        e );
                securityProps.clear(); // this is being cleared, so the
                                       // "last-resort" defaults specified in
                                       // the xsl will be used.

            }
        }
        else
        {
            logger.info( "site security config is null" );
            securityProps.clear(); // this is being cleared, so the
                                   // "last-resort" defaults specified in the
                                   // xsl will be used.

        }
        return securityProps;
    }


    @Override
    public boolean isAvailable()
    {
        boolean isAvailable;
        if ( !lastAvailable
                || ( lastAvailableDate.before( new Date( System.currentTimeMillis() - AVAILABLE_TIMEOUT_CHECK ) ) ) )
        {
            StringBuilder url = new StringBuilder( endpointUrl );
            InputStream is = null;
            try
            {
                // create basic query (single search phrase)
                blankOutQuery(url);
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Calling URL with test query to check availability: " + url.toString() );
                }
                // call service
                BinaryContent data = null;
                try {
                    data = connection.getData( url.toString() );
                } catch (MalformedURLException e) {
                    logger.info("Could not retrieve data." , e);
                    return false;
                } catch (IOException e) {
                    logger.info("Could not retrieve data." , e);
                    return false;
                }
                if(data == null) {
                    return false;
                }
                is = data.getInputStream();
                // check for ANY response
                Document availableDoc = OpenSearchSiteUtil.convertStreamToDocument( is );
                String allContent = evaluate( "/atom:feed", availableDoc );
                if ( !allContent.isEmpty() && allContent.length() > 0 )
                {
                    logger.debug( "Found atom feed parent, marking site as available." );
                    isAvailable = true;
                    lastAvailableDate = new Date();
                }
                else
                {
                    logger.debug( "No atom feed parent found, marking site as NOT available." );
                    isAvailable = false;
                }

            }
            catch ( UnsupportedQueryException uqe )
            {
                logger.warn( "Calling site threw error, marking as NOT available.", uqe );
                isAvailable = false;
            } catch (ConversionException ce)
            {
                logger.warn( "Calling site threw error, marking as NOT available.", ce );
                isAvailable = false;
            } finally
            {
                IOUtils.closeQuietly( is );
            }
        }
        else
        {
            isAvailable = lastAvailable;
        }
        lastAvailable = isAvailable;
        return isAvailable;
    }

    //TODO: actually use the callback!
    @Override
    public boolean isAvailable(SourceMonitor callback) {
    	return isAvailable() ;
    }

    @Override
    public SourceResponse query( QueryRequest queryRequest ) throws UnsupportedQueryException
    {
        String methodName = "query";
        logger.entry( methodName );

        Query query = queryRequest.getQuery();

        if (logger.isDebugEnabled())
        {
            logger.debug( "Received query: " + query );
        }

        SourceResponseImpl response = new SourceResponseImpl( queryRequest, new ArrayList<Result>() );
        Subject user = (Subject) queryRequest.getPropertyValue(Constants.SUBJECT_PROPERTY);

        String url = createUrl( query, user );

        // If the url is non-null, then it is valid and can be used to search the site.
        if ( url != null )
        {
            BinaryContent data = null ;
            try {
                data = connection.getData(url);
                if(data == null) {
                    return response;
                }
            } catch (MalformedURLException e) {
               throw new UnsupportedQueryException("Could not complete query.", e);
            } catch (IOException e) {
               throw new UnsupportedQueryException("Could not complete query.", e);
            }
            response = processResponse( data.getInputStream(), queryRequest );
        }
        else {
            logger.debug("URL created was null.");
        }

        logger.exit( methodName );

        return response;
    }



    // Refactored from query() and made protected so JUnit tests could be written for this logic
    protected String createUrl( Query query, Subject user )
    {
        if (logger.isDebugEnabled())
        {
            FilterTransformer transform = new FilterTransformer();
            transform.setIndentation( 2 );
            try
            {
                logger.debug( transform.transform( query ) );
            }
            catch ( TransformerException e )
            {
                logger.debug( "Error transforming query to XML", e );
            }
        }

        OpenSearchFilterVisitor visitor = new OpenSearchFilterVisitor();
        query.accept( visitor, null );

        String urlStr = null;

        ContextualSearch contextualFilter = visitor.getContextualSearch();

        // All queries must have at least a search phrase to be valid, hence this check
        // for a contextual filter with a non-empty search phrase
        if ( contextualFilter != null && !contextualFilter.getSearchPhrase().trim().isEmpty() )
        {
            StringBuilder url = new StringBuilder( endpointUrl );
            url = OpenSearchSiteUtil.populateSearchOptions( url, query, user );
            url = OpenSearchSiteUtil.populateContextual( url, contextualFilter.getSearchPhrase() );

            TemporalFilter temporalFilter = visitor.getTemporalSearch();
            if ( temporalFilter != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug( "startDate = " + temporalFilter.getStartDate().toString() );
                    logger.debug( "endDate = " + temporalFilter.getEndDate().toString() );
                }
                url = OpenSearchSiteUtil.populateTemporal( url, temporalFilter );
            }

            SpatialFilter spatialFilter = visitor.getSpatialSearch();
            if ( spatialFilter != null )
            {
                if ( spatialFilter instanceof SpatialDistanceFilter )
                {
                    try {
                        url = OpenSearchSiteUtil.populateGeospatial( url, (SpatialDistanceFilter) spatialFilter, shouldConvertToBBox );
                    } catch (UnsupportedQueryException e) {
                        logger.info("Problem with populating geospatial criteria. ", e);
                    }
                }
                else
                {
                    try {
                        url = OpenSearchSiteUtil.populateGeospatial( url, spatialFilter, shouldConvertToBBox );
                    } catch (UnsupportedQueryException e) {
                        logger.info("Problem with populating geospatial criteria. ", e);
                    }
                }
            }

            url = blankOutQuery( url );

            urlStr = url.toString();
        }

        logger.debug( "Populated URL being called: " + urlStr );
        
        // if it cannot be done by OpenSearch, possibly by REST
        if(urlStr == null) {
            
            RestFilterDelegate delegate = null;
            RestUrl restUrl = newRestUrl();
            
            if(restUrl != null) {
                delegate = new RestFilterDelegate(restUrl);
            }
            
            if(delegate != null) {
                
                try {
                    filterAdapter.adapt(query, delegate);
                    this.isRestSearch = true;
                    urlStr = delegate.getRestUrl().buildUrl();
                } catch (UnsupportedQueryException e) {
                    logger.debug("Not a REST request.", e);
                }
                
            }
            
        }

        return urlStr;
    }


    /**
     * Blanks out the rest of the query for the options that were not passed in.
     *
     * @param url
     * @return
     */
    private StringBuilder blankOutQuery( StringBuilder url )
    {
        try
        {
            OpenSearchSiteUtil.populateSearchOptions( url, null, null );
            OpenSearchSiteUtil.populateContextual( url, "Iraq" );
            OpenSearchSiteUtil.populateTemporal( url, null );
            OpenSearchSiteUtil.populateGeospatial( url, (SpatialDistanceFilter) null, shouldConvertToBBox );
        }
        catch ( UnsupportedQueryException ce )
        {
            logger.debug( "Wasn't able to clear out the rest of the query, URL may be invalid." );
        }

        return url;
    }

    /**
     * @param is
     * @param queryRequest
     * @return
     * @throws ddf.catalog.source.UnsupportedQueryException
     */
    private SourceResponseImpl processResponse( InputStream is, QueryRequest queryRequest ) throws UnsupportedQueryException
    {
        List<Result> resultQueue = new ArrayList<Result>();

        if (isRestSearch) {
            Metacard metacard = null;
            try {
                metacard = inputTransformer.transform(is);
            } catch (IOException e) {
                logger.debug("Problem with transformation.", e);
            } catch (CatalogTransformerException e) {
                logger.debug("Problem with transformation.", e);
            }
            if (metacard != null) {
                metacard.setSourceId(getId());
                ResultImpl result = new ResultImpl(metacard);
                resultQueue.add(result);
                SourceResponseImpl response = new SourceResponseImpl(
                        queryRequest, resultQueue);
                response.setHits(resultQueue.size());
                return response;
            }

        }
        
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Parser parser = null;
        org.apache.abdera.model.Document<Feed> atomDoc;
        try {

            Thread.currentThread().setContextClassLoader(OpenSearchSource.class.getClassLoader());
            parser = ABDERA.getParser();
            atomDoc = parser.parse(is);

        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        Feed feed = atomDoc.getRoot();

        updateDefaultClassification();

        List<Entry> entries = feed.getEntries();
        for (Entry entry : entries)
        {
            resultQueue.add( createResponseFromEntry(entry) );
        }

        long totalResults = entries.size();

//        org.apache.abdera.xpath.XPath xp = ABDERA.getXPath();
//        Map<String, String> ns = xp.getDefaultNamespaces();
//        ns.put(OpenSearchConstants.OS_PREFIX, OpenSearchConstants.OPENSEARCH_NS);
//        String resultNum = xp.valueOf( "//os:totalResults", atomDoc, ns );

        // OSGi has some weird issues with Abdera's XPath, so just traverse down the element tree
        Element totalResultsElement = atomDoc.getRoot().getExtension( OpenSearchConstants.TOTAL_RESULTS );

        if (totalResultsElement != null) {
            try {
                totalResults = Long.parseLong( totalResultsElement.getText() );
            } catch (NumberFormatException e) {
                // totalResults is already initialized to the correct value, so don't do anything here.
            }
        }

        SourceResponseImpl response = new SourceResponseImpl( queryRequest, resultQueue );
        response.setHits( totalResults );

        return response;
    }


    /**
     * Creates a single response from input parameters. Performs XPath
     * operations on the document to retrieve data not passed in.
     *
     * @param entry a single Atom entry
     * @return single response
     * @throws ddf.catalog.source.UnsupportedQueryException
     */
    private Result createResponseFromEntry( Entry entry) throws UnsupportedQueryException
    {
        //id
        String id = entry.getId().getPath();
        //getPath() returns catalog:id:<id>, so we parse out the <id>
        if (id != null && !id.isEmpty())
        {
            id = id.substring(id.lastIndexOf(':') + 1);
        }

        //content
        String content = null;
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {

            Thread.currentThread().setContextClassLoader(OpenSearchSource.class.getClassLoader());
            content = entry.getContent();

        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        final MetacardImpl metacard = getMetacardImpl(parseContent(content, id));

        metacard.setSourceId( this.shortname );

        //TODO resultSource. should we store this data?
//        QName resultSourceElementQName = new QName("http://a9.com/-/opensearch/extensions/federation/1.0/", "resultSource", "fs");
//        QName sourceIdAttributeQName = new QName("http://a9.com/-/opensearch/extensions/federation/1.0/", "sourceId", "fs");
//        String originSourceId = entry.getExtension(resultSourceElementQName).getAttributeValue(sourceIdAttributeQName)

        //relevance
        QName relevanceElementQName = new QName("http://a9.com/-/opensearch/extensions/relevance/1.0/", "score", "relevance");
        String relevance = entry.getSimpleExtension(relevanceElementQName);

        //title
        //only set this if it's unset
        String title = metacard.getTitle();
        if (title != null && !title.isEmpty())
        {
            metacard.setTitle(entry.getTitle());
        }

        //updated
        //only set this if it's unset
        if (metacard.getModifiedDate() != null)
        {
            metacard.setModifiedDate(entry.getUpdated());
        }

        //published
        //only set this if it's unset
        if (metacard.getCreatedDate() != null)
        {
            metacard.setCreatedDate(entry.getPublished());
        }

        //category. maps to metadata-content-type

        String contentType = metacard.getContentTypeName();
        if (contentType != null && !contentType.isEmpty())
        {
            ClassLoader tccl2 = Thread.currentThread().getContextClassLoader();
            try {

                Thread.currentThread().setContextClassLoader(OpenSearchSource.class.getClassLoader());
                List<Category> categories = entry.getCategories();
                if (!categories.isEmpty() && categories.get(0) != null)
                {
                    metacard.setContentTypeName(categories.get(0).toString());
                }

            } finally {
                Thread.currentThread().setContextClassLoader(tccl2);
            }

        }

        //TODO geos. potentially parse and use this if geos is missing from content

        ResultImpl result = new ResultImpl(metacard);
        if ( relevance == null || relevance.isEmpty() )
        {
            logger.debug( "couldn't find valid relevance. Setting relevance to 0" );
            relevance = "0";
        }
        result.setRelevanceScore( new Double( relevance ) );

        return result;
    }

    private MetacardImpl getMetacardImpl(Metacard oldMetacard)
    {
        MetacardImpl metacard;

        if (oldMetacard == null)
        {
            metacard = new MetacardImpl();
        }
        else
        {
            metacard = new MetacardImpl(oldMetacard);
        }

        return metacard;
    }

    private Metacard parseContent(String content, String id)
    {
        if (inputTransformer != null && content != null && !content.isEmpty())
        {
            try
            {
                return inputTransformer.transform(new ByteArrayInputStream(content.getBytes()), id);
            } catch (IOException e)
            {
                logger.warn("Unable to read metacard content from Atom feed.", e);
            } catch (CatalogTransformerException e)
            {
                logger.warn("Unable to convert metacard content from Atom feed into Metacard object.", e);
            }
        }
        return null;
    }


    /**
     * Perform xpath evaluation and return value as a string.
     *
     * @param xpathExpression
     * @param node
     * @return result of xpath evaluation
     * @throws javax.xml.xpath.XPathExpressionException
     */
    private String evaluate( String xpathExpression, Node node ) throws UnsupportedQueryException
    {
        String result = "";
        synchronized ( xpath )
        {
            try
            {
                xpath.setNamespaceContext( new OpenSearchNamespaceContext() );
                result = xpath.evaluate( xpathExpression, node );
            }
            catch ( XPathExpressionException xpee )
            {
                logger.warn( "Error while performing xpath, result may be missing information.", xpee );
            }
            finally
            {
                xpath.reset();
            }

        }
        return result;
    }


    /**
     * Set URL of the endpoint.
     * 
     * @param endpointUrl Full url of the endpoint.
     */
    public void setEndpointUrl( String endpointUrl )
    {
        this.endpointUrl = endpointUrl;
        
        // If the source is already initialized, adjust the endpoint URL
        if ( isInitialized )
        {
            configureEndpointUrl();
        }
    }


    /**
     * Get the URL of the endpoint.
     * 
     * @return
     */
    public String getEndpointUrl()
    {
        logger.trace( "getEndpointUrl:  endpointUrl = " + endpointUrl );
        return endpointUrl;
    }


    @Override
    public String getDescription()
    {
        return DESCRIPTION;
    }


    @Override
    public String getOrganization()
    {
        return ORGANIZATION;
    }


    @Override
    public String getId()
    {
        return shortname;
    }

    /**
     * Sets the shortname for this site. This shortname is
     * used to identify the site when performing federated queries.
     * @param shortname Name of this site.
     */
    public void setShortname( String shortname )
    {
    	this.shortname = shortname;
    }


    @Override
    public String getTitle()
    {
        return TITLE;
    }


    @Override
    public String getVersion()
    {
        return version;
    }


    /**
     * Sets default classification in document from server.
     * 
     * @param classification default classification in string format
     */
    public void setClassification( String classification )
    {
        this.classification = classification;
    }


    /**
     * Get default classification.
     * 
     * @return default classification
     */
    public String getClassification()
    {
        return this.classification;
    }


    /**
     * Sets default ownerProducer in document from server.
     * 
     * @param ownerProducer default ownerProducer in string format
     */
    public void setOwnerProducer( String ownerProducer )
    {
        this.ownerProducer = ownerProducer;
    }


    /**
     * Get default ownerProducer.
     * 
     * @return default ownerProducer
     */
    public String getOwnerProducer()
    {
        return this.ownerProducer;
    }


    /**
     * Sets file path of truststore.
     * 
     * @param trustStore
     */
    public void setTrustStoreLocation( String trustStore )
    {
        connection.setTrustStoreLocation(trustStore);
    }


    /**
     * Get file path of trust store.
     * 
     * @return file location.
     */
    public String getTrustStoreLocation()
    {
        return connection.getTrustStoreLocation();
    }


    /**
     * Sets file path of the client keystore.
     * 
     * @param keyStore
     */
    public void setKeyStoreLocation( String keyStore )
    {
        connection.setKeyStoreLocation(keyStore);
    }


    /**
     * Get file path of the client keystore.
     * 
     * @return keystore file path.
     */
    public String getKeyStoreLocation()
    {
        return connection.getKeyStoreLocation();
    }


    /**
     * Sets the password of the truststore.
     * 
     * @param trustStorePass
     */
    public void setTrustStorePassword( String trustStorePass )
    {
        connection.setTrustStorePassword(trustStorePass);
    }


    /**
     * Get the password of the truststore.
     * 
     * @return
     */
    public String getTrustStorePassword()
    {
        return connection.getTrustStorePassword();
    }


    /**
     * Sets the password of the keystore. Note: Private key alias and keystore
     * must have the same password.
     * 
     * @param keyStorePass
     */
    public void setKeyStorePassword( String keyStorePass )
    {
        connection.setKeyStorePassword(keyStorePass);
    }


    /**
     * Gets the password of the keystore.
     * 
     * @return
     */
    public String getKeyStorePassword()
    {
        return connection.getKeyStorePassword();
    }


    public void setInputTransformer(InputTransformer inputTransformer)
    {
        this.inputTransformer = inputTransformer;
    }

    public InputTransformer getInputTransformer()
    {
        return inputTransformer;
    }

    /**
     * Sets the boolean flag that indicates all queries executed should be to its 
     * local source only, i.e., no federated or enterprise queries.
     * 
     * @param localQueryOnly true indicates only local queries, false indicates enterprise query
     */
    public void setLocalQueryOnly( boolean localQueryOnly )
    {
        logger.trace( "Setting localQueryOnly = " + localQueryOnly );
        this.localQueryOnly = localQueryOnly;
        
        // If the source is already initialized, adjust the endpoint URL
        if ( isInitialized )
        {
            configureEndpointUrl();
        }
    }


    /**
     * Get the boolean flag that indicates only local queries are being executed
     * by this OpenSearch Source.
     * 
     * @return true indicates only local queries, false indicates enterprise query
     */
    public boolean getLocalQueryOnly()
    {
        return localQueryOnly;
    }
    
    /**
     * Sets the boolean flag that tells the code to convert point-radius and
     * polygon geometries to a bounding box before sending them.
     * 
     * @param shouldConvertToBBox
     */
    public void setShouldConvertToBBox( boolean shouldConvertToBBox )
    {
        this.shouldConvertToBBox = shouldConvertToBBox;
    }


    /**
     * Get the boolean flag that determines if point-radius and polygon
     * geometries should be converting to bounding boxes before sending.
     * 
     * @return
     */
    public boolean getShouldConvertToBBox()
    {
        return shouldConvertToBBox;
    }

    @Override
    public ResourceResponse retrieveResource(URI uri,
            Map<String, Serializable> requestProperties)
            throws ResourceNotFoundException, ResourceNotSupportedException {

        if (requestProperties == null) {
            throw new ResourceNotFoundException(
                    "Could not retrieve resource with null properties.");
        }

        Serializable serializableId = requestProperties.get(Metacard.ID);

        if (serializableId != null) {

            String metacardId = serializableId.toString();
            RestUrl restUrl = null;
            try {
                restUrl = RestUrl.newInstance(endpointUrl);
            } catch (MalformedURLException e) {
                throw new ResourceNotFoundException(
                        COULD_NOT_RETRIEVE_RESOURCE_MESSAGE + ": "
                                + BAD_URL_MESSAGE, e);
            } catch (URISyntaxException e) {
                throw new ResourceNotFoundException(
                        COULD_NOT_RETRIEVE_RESOURCE_MESSAGE + ": "
                                + BAD_URL_MESSAGE, e);
            }

            if (restUrl != null) {
                restUrl.setId(metacardId);
                restUrl.setRetrieveResource(true);
                BinaryContent binaryContent = null;
                try {
                    binaryContent = connection.getData(restUrl.buildUrl());
                } catch (IOException e) {
                    throw new ResourceNotFoundException(
                            COULD_NOT_RETRIEVE_RESOURCE_MESSAGE, e);
                }
                if (binaryContent != null) {
                    return new ResourceResponseImpl(new ResourceImpl(
                            binaryContent.getInputStream(),
                            binaryContent.getMimeType(), getId()
                                    + "_Resource_Retrieval:"
                                    + System.currentTimeMillis()));
                }
            }
        }

        throw new ResourceNotFoundException(COULD_NOT_RETRIEVE_RESOURCE_MESSAGE);
    }

    private RestUrl newRestUrl() {
        RestUrl restUrl = null;
        try {
            restUrl = RestUrl.newInstance(endpointUrl);
        } catch (MalformedURLException e) {
            logger.info(BAD_URL_MESSAGE, e);
        } catch (URISyntaxException e) {
            logger.info(BAD_URL_MESSAGE, e);
        }
        return restUrl;
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
		logger.trace("ENTERING/EXITING: getOptions");
		logger.debug("OpenSearch Source \"" + getId() + "\" does not support resource retrieval options.");
		return Collections.emptySet(); 
	}

	
	/**
	 * Adjusts the endpoint URL based on whether only local queries should be
	 * executed by this OpenSearch Source. If the <code>localQueryOnly</code> attribute is
	 * set to true, then the endpoint URL's <code>src</code> parameter is overridden to be
	 * <code>src=local</code>. If the <code>src</code> parameter is currently set to <code>local</code>
	 * and this source is not currently configured for local queries only, then the endpoint
	 * URL's <code>src</code> parameter is reset to the default value of <code>{fs:routeTo?}</code>,
	 * which will resolve to an enterprise search.
	 * 
	 * This is primarily relevant for DDF-to-DDF federation via OpenSearch to
     * prevent a circular federation issue using OpenSearch enterprise queries.
	 */
	// Scope is protected to allow JUnit testing
	protected void configureEndpointUrl()
	{
	    String methodName = "configureEndpointUrl";
	    logger.entry( methodName + ":   endpointUrl = " + endpointUrl );
	    
	    // If only executing local queries, then change the src parameter in the
	    // OpenSearch endpoint URL so that "src=local"
	    if ( localQueryOnly )
        {
	        // If there is no src parameter in the URL, then append it with a value of "local"
            if ( !endpointUrl.contains( URL_SRC_PARAMETER ) )
            {
                endpointUrl += URL_PARAMETER_SEPARATOR + LOCAL_SEARCH_PARAMETER;
            }
            
            // Otherwise, extract all of the endpoint URL's parameters and loop through them,
            // looking for the src parameter and overriding its current value with "local"
            else
            {
                // Parse the current endpoint URL, splitting it between the address and the parameter list
                // which are separated by a question mark (?)
                int start = endpointUrl.indexOf( URL_PARAMETERS_DELIMITER );
                String params = endpointUrl.substring( start + 1 );
                
                // Build up the modified endpoint URL (using the same class variable), starting
                // with the address portion that was parsed previously
                endpointUrl = endpointUrl.substring( 0, start ) + URL_PARAMETERS_DELIMITER;
                
                // Loop thorugh the parameters list, appending each one back on the modified
                // endpoint URL, except substitute src=local when the src parameter is detected
                String[] paramList = params.split( URL_PARAMETER_SEPARATOR );
                for ( int i=0; i < paramList.length; i++ )
                {
                    logger.trace( "Param:  [" + paramList[i] + "]" );
                    if ( i > 0 )
                    {
                        endpointUrl += URL_PARAMETER_SEPARATOR;
                    }
                    if ( paramList[i].contains( URL_SRC_PARAMETER ) )
                    {
                        endpointUrl += LOCAL_SEARCH_PARAMETER;
                    }
                    else
                    {
                        endpointUrl += paramList[i];

                    }
                }
            }
        }
	    
	    // If not local query only and src parameter is currently set to "local",
	    // then restore src parameter to default
	    else if ( endpointUrl.contains( LOCAL_SEARCH_PARAMETER ) )
	    {
	        endpointUrl = endpointUrl.replace( LOCAL_SEARCH_PARAMETER, "src=" + OpenSearchSiteUtil.SRC );
	    }
	    
	    logger.exit( methodName + ":   endpointUrl = " + endpointUrl );
	}
}
