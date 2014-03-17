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
package ddf.catalog.fanout;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.cache.CacheException;
import ddf.catalog.CatalogFramework;
import ddf.catalog.cache.CacheKey;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.impl.CatalogFrameworkImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resourceretriever.RemoteResourceRetriever;
import ddf.catalog.resourceretriever.ResourceRetriever;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.RemoteSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.util.impl.SourcePoller;

/**
 * {@link FanoutCatalogFramework} evaluates all {@link Operation}s as enterprise-wide federated
 * {@link Operation}s. A {@link FanoutCatalogFramework} has no {@link CatalogProvider} configured
 * for it, hence no ingest operations are supported. All source names for any
 * {@link FederatedSource}s or {@link ConnectedSource}s in the {@link FanoutCatalogFramework}'s
 * configuration are hidden from the external client.
 * <p>
 * This {@link CatalogFramework} may be used for the following reasons:
 * <ol>
 * <li>A single node being exposed from an enterprise (hiding the enterprise from an external
 * client)</li>
 * <li>To ensure each {@link Query} is searches all {@link Source}s</li>
 * <li>Backwards compatibility (e.g., federating with older versions)</li>
 * </ol>
 * </p>
 * 
 */
@SuppressWarnings("deprecation")
public class FanoutCatalogFramework extends CatalogFrameworkImpl {
    private static final String EXCEPTION_MESSAGE = "FanoutCatalogFramework does not support create, update, and delete operations";

    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(FanoutCatalogFramework.class));

    /**
     * Instantiates a new FanoutCatalogFramework, ignoring the provided {@link CatalogProvider}
     * 
     * @param context
     *            - The BundleContext that will be utilized by this instance.
     * @param catalog
     *            - The {@link CatalogProvider} used for query operations.
     * @param preIngest
     *            - A {@link List} of {@link PreIngestPlugin}(s) that will be invoked prior to the
     *            ingest operation.
     * @param postIngest
     *            - A list of {@link PostIngestPlugin}(s) that will be invoked after the ingest
     *            operation.
     * @param preQuery
     *            - A {@link List} of {@link PreQueryPlugin}(s) that will be invoked prior to the
     *            query operation.
     * @param postQuery
     *            - A {@link List} of {@link PostQueryPlugin}(s) that will be invoked after the
     *            query operation.
     * @param preResource
     *            - A {@link List} of {@link PreResourcePlugin}(s) that will be invoked prior to the
     *            getResource operation.
     * @param postResource
     *            - A {@link List} of {@link PreResourcePlugin}(s) that will be invoked after the
     *            getResource operation.
     * @param connectedSites
     *            - {@link List} of {@link ConnectedSource}(s) that will be searched on all queries
     * 
     * @param federatedSites
     *            - A {@link List} of {@link FederatedSource}(s) that will be searched on an
     *            enterprise query.
     * @param resourceReaders
     *            - set of {@link ResourceReader}(s) that will be get a {@link Resource}
     * @param queryStrategy
     *            - The default federation strategy (e.g. Sorted).
     * @param pool
     *            - An ExecutorService used to manage threaded operations.
     * @param poller
     *            - An {@link SourcePoller} used to poll source availability.
     */
    public FanoutCatalogFramework(BundleContext context, CatalogProvider catalog,
            List<PreIngestPlugin> preIngest, List<PostIngestPlugin> postIngest,
            List<PreQueryPlugin> preQuery, List<PostQueryPlugin> postQuery,
            List<PreResourcePlugin> preResource, List<PostResourcePlugin> postResource,
            List<ConnectedSource> connectedSites, List<FederatedSource> federatedSites,
            List<ResourceReader> resourceReaders, FederationStrategy queryStrategy,
            ExecutorService pool, SourcePoller poller) {

        super(Collections.singletonList(catalog), context, preIngest, postIngest, preQuery,
                postQuery, preResource, postResource, connectedSites, federatedSites,
                resourceReaders, queryStrategy, pool, poller);
        logger.debug("\n\n Starting FanoutCatalogFramework\n\n");
    }

    /**
     * Instantiates a new FanoutCatalogFramework without a {@link CatalogProvider}.
     * 
     * @param context
     *            - The BundleContext that will be utilized by this instance.
     * @param preIngest
     *            - A {@link List} of {@link PreIngestPlugin}(s) that will be invoked prior to the
     *            ingest operation.
     * @param postIngest
     *            - A list of {@link PostIngestPlugin}(s) that will be invoked after the ingest
     *            operation.
     * @param preQuery
     *            - A {@link List} of {@link PreQueryPlugin}(s) that will be invoked prior to the
     *            query operation.
     * @param postQuery
     *            - A {@link List} of {@link PostQueryPlugin}(s) that will be invoked after the
     *            query operation.
     * @param preResource
     *            - A {@link List} of {@link PreResourcePlugin}(s) that will be invoked prior to the
     *            getResource operation.
     * @param postResource
     *            - A {@link List} of {@link PostResourcePlugin}(s) that will be invoked after the
     *            getResource operation.
     * @param connectedSites
     *            - {@link List} of {@link ConnectedSource}(s) that will be searched on all queries
     * 
     * @param federatedSites
     *            - A {@link List} of {@link FederatedSource}(s) that will be searched on an
     *            enterprise query.
     * @param resourceReaders
     *            - set of {@link ResourceReader}(s) that will be get a {@link Resource}
     * @param queryStrategy
     *            - The default federation strategy (e.g. Sorted).
     * @param pool
     *            - An ExecutorService used to manage threaded operations.
     * @param poller
     *            - An {@link SourcePoller} used to poll source availability.
     */
    public FanoutCatalogFramework(BundleContext context, List<PreIngestPlugin> preIngest,

    List<PostIngestPlugin> postIngest,

    List<PreQueryPlugin> preQuery, List<PostQueryPlugin> postQuery,
            List<PreResourcePlugin> preResource, List<PostResourcePlugin> postResource,
            List<ConnectedSource> connectedSites, List<FederatedSource> federatedSites,
            List<ResourceReader> resourceReaders, FederationStrategy queryStrategy,
            ExecutorService pool, SourcePoller poller) {

        this(context, null, preIngest, postIngest, preQuery, postQuery, preResource, postResource,
                connectedSites, federatedSites, resourceReaders, queryStrategy, pool, poller);
    }

    @Override
    public QueryResponse query(QueryRequest queryRequest) throws UnsupportedQueryException,
        FederationException {
        return query(queryRequest, null);
    }

    /**
     * Always executes an enterprise {@link Query}, replacing the {@link Source} ID in the
     * {@link QueryResponse} with this {@link CatalogFramework}'s id so that the ids of all
     * {@link FederatedSource}s remain hidden from the external client.
     * 
     * @param queryRequest
     *            the {@link QueryRequest}
     * @param strategy
     *            the {@link FederationStrategy}
     * @return the {@QueryResponse}
     * @throws UnsupportedQueryException
     *             {@inheritDoc}
     * @throws FederationException
     *             {@inheritDoc}
     */
    @Override
    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
        throws UnsupportedQueryException, FederationException {
        // TODO make this private/static/final
        String methodName = "query";
        logger.entry(methodName);

        validateQueryRequest(queryRequest);

        // Force an enterprise query
        QueryResponse queryResponse = super.query(new QueryRequestImpl(queryRequest.getQuery(),
                true, null, queryRequest.getProperties()), strategy);
        queryResponse = replaceSourceId(queryResponse);

        logger.exit(methodName);
        return queryResponse;
    }

    /**
     * Always returns {@code false} for fanout configuration.
     * 
     * @return {@code false}
     */
    @Override
    protected boolean hasCatalogProvider() {
        return false;
    }

    /**
     * Always throws an {@link IngestException} since create/ingest operations are not supported in
     * fanout configuration.
     * 
     * @param create
     *            the {@link CreateRequest}
     * @return the {@link CreateResponse}, which never happens in fanout
     * @throws IngestException
     *             always thrown in fanout configuration
     */
    @Override
    public CreateResponse create(CreateRequest create) throws IngestException {
        throw new IngestException(EXCEPTION_MESSAGE);
    }

    /**
     * Always throws an {@link IngestException} since delete operations are not supported in fanout
     * configuration.
     * 
     * @param delete
     *            the {@link DeleteRequest}
     * @return the {@link DeleteResponse}, which never happens in fanout
     * @throws IngestException
     *             always thrown in fanout configuration
     */
    @Override
    public DeleteResponse delete(DeleteRequest delete) throws IngestException {
        throw new IngestException(EXCEPTION_MESSAGE);
    }

    /**
     * Always throws an {@link IngestException} since update operations are not supported in fanout
     * configuration.
     * 
     * @param update
     *            the {@link UpdateRequest}
     * @return the {@link UpdateResponse}, which never happens
     * @throws IngestException
     *             always thrown
     */
    @Override
    public UpdateResponse update(UpdateRequest update) throws IngestException {
        throw new IngestException(EXCEPTION_MESSAGE);
    }

    /**
     * Always searches the entire enterprise for the resource to retrieve in fanout configuration.
     * 
     * @param resourceRequest
     *            the {@link ResourceRequest}
     * @return the {@link ResourceResponse}
     * @throws IOException
     *             {@inheritDoc}
     * @throws ResourceNotFoundException
     *             {@inheritDoc}
     * @throws ResourceNotSupportedException
     *             {@inheritDoc}
     */
    @Override
    public ResourceResponse getLocalResource(ResourceRequest resourceRequest) throws IOException,
        ResourceNotFoundException, ResourceNotSupportedException {
        logger.debug("getLocalResource call received, fanning it out to all sites.");
        return super.getEnterpriseResource(resourceRequest);
    }

    /**
     * Always searches the entire enterprise for the resource to retrieve in fanout configuration.
     * 
     * @param resourceRequest
     *            the {@link ResourceRequest}
     * @return the {@link ResourceResponse}
     * @throws IOException
     *             {@inheritDoc}
     * @throws ResourceNotFoundException
     *             {@inheritDoc}
     * @throws ResourceNotSupportedException
     *             {@inheritDoc}
     */
    @Override
    public ResourceResponse getResource(ResourceRequest resourceRequest, String resourceSiteName)
        throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        logger.debug("getResource call received, fanning it out to all sites.");
        return super.getEnterpriseResource(resourceRequest);
    }

    /**
     * Retrieves the {@link SourceDescriptor} info for all {@link FederatedSource}s in the fanout
     * configuration, but the all of the source info, e.g., content types, for all of the available
     * {@link FederatedSource}s is packed into one {@SourceDescriptor
     * 
     * } for the
     * fanout configuration with the fanout's site name in it. This keeps the individual
     * {@link FederatedSource}s' source info hidden from the external client.
     */
    @Override
    public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
        throws SourceUnavailableException {

        final String methodName = "getSourceInfo";
        SourceInfoResponse response = null;
        SourceDescriptorImpl sourceDescriptor = null;
        logger.entry(methodName);
        try {

            // request
            if (sourceInfoRequest == null) {
                logger.error("Received null sourceInfoRequest");
                throw new IllegalArgumentException("SourceInfoRequest was null");
            }

            Set<SourceDescriptor> sourceDescriptors = new LinkedHashSet<SourceDescriptor>();
            Set<String> ids = sourceInfoRequest.getSourceIds();

            // Only return source descriptor information if this sourceId is
            // specified
            if (ids != null && !ids.isEmpty()) {
                for (String id : ids) {
                    if (!id.equals(this.getId())) {
                        logger.warn("Throwing SourceUnavilableExcption for unknown source: " + id);
                        throw new SourceUnavailableException("Unknown source: " + id);

                    }
                }

            }
            // Fanout will only add one source descriptor with all the contents
            Set<ContentType> contentTypes = new HashSet<ContentType>();

            // Add a set of all contentTypes from the federated sources
            for (FederatedSource source : federatedSources) {
                if (source != null && source.isAvailable() && source.getContentTypes() != null) {
                    contentTypes.addAll(source.getContentTypes());
                }
            }

            // only reveal this sourceDescriptor, not the federated sources
            sourceDescriptor = new SourceDescriptorImpl(this.getId(), contentTypes);
            sourceDescriptor.setVersion(this.getVersion());
            sourceDescriptors.add(sourceDescriptor);

            response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

        } catch (RuntimeException re) {
            logger.warn("Exception during runtime while performing create", re);
            throw new SourceUnavailableException(
                    "Exception during runtime while performing getSourceInfo", re);

        }
        logger.exit(methodName);
        return response;

    }

    /**
     * Replaces the site name(s) of {@link FederatedSource}s in the {@link QueryResponse} with the
     * fanout's site name to keep info about the {@link FederatedSource}s hidden from the external
     * client.
     * 
     * @param queryResponse
     *            the original {@link QueryResponse} from the query request
     * @return the updated {@link QueryResponse} with all site names replaced with fanout's site
     *         name
     */
    protected QueryResponse replaceSourceId(QueryResponse queryResponse) {
        // TODO DDF-968 Update this so it does it in a streaming manner.
        List<Result> results = queryResponse.getResults();
        QueryResponseImpl newResponse = new QueryResponseImpl(queryResponse.getRequest(),
                queryResponse.getProperties());
        for (Result result : results) {
            MetacardImpl newMetacard = new MetacardImpl(result.getMetacard());
            newMetacard.setSourceId(this.getId());
            ResultImpl newResult = new ResultImpl(newMetacard);
            // Copy over scores
            newResult.setDistanceInMeters(result.getDistanceInMeters());
            newResult.setRelevanceScore(result.getRelevanceScore());
            newResponse.addResult(newResult, false);
        }
        newResponse.setHits(queryResponse.getHits());
        newResponse.closeResultQueue();
        return newResponse;
    }

    /**
     * Always returns only the fanout's source ID, e.g., ddf-fanout. The source IDs for all
     * federated sources in the fanout are hidden from external clients.
     * 
     * @return a {@link Set} of one that includes only the fanout's source ID
     */
    @Override
    public Set<String> getSourceIds() {
        // Only return the fanoutSourceId
        Set<String> sources = new HashSet<String>(1);
        sources.add(this.getId());
        return sources;
    }

    /**
     * Validate that the {@link QueryRequest} is non-null and that if the request includes source
     * ID(s), that the ID(s) are only for the fanout's source ID, not IDs for specific federated
     * source(s) which are hidden in a fanout configuration.
     * 
     * @param queryRequest
     *            the {@link QueryRequest}
     * @throws UnsupportedQueryException
     *             if request is null or a non-fanout source ID is specified in the request
     */
    @Override
    protected void validateQueryRequest(QueryRequest queryRequest) throws UnsupportedQueryException {
        if (queryRequest == null) {
            throw new UnsupportedQueryException("QueryRequest was null");
        }

        Set<String> sources = queryRequest.getSourceIds();
        if (sources != null) {
            for (String querySourceId : sources) {
                logger.debug("validating requested sourceId" + querySourceId);
                if (!querySourceId.equals(this.getId())) {
                    logger.debug("Throwing unsupportedQueryException due to unknown sourceId: "
                            + querySourceId);
                    throw new UnsupportedQueryException("Unknown source: " + querySourceId);
                }
            }
        }
    }

    /**
     * Retrieve a resource from the enterprise or specified {@link RemoteSource} .
     * 
     * First perform an entry query on all the {@link FederatedSource}s and {@link ConnectedSource}
     * s. This is done to locate which source the {@link Metacard} resides on. Next, get the
     * resource URI from the {@link Metacard}. Finally, do a
     * {@link RemoteSource#retrieveResource(URI, Map)}.
     */
    // TODO: This is TECHNICAL DEBT. The reason that we had to override
    // CatalogframeworkImpl's getResource
    // is because there was an error when doing a getResource using a Fanout
    // configuration. Currently,
    // getResource is implemented to first perform an entry query on all the
    // federated/connected sources.
    // It does this to locate which source the entry resides on. Once that is
    // discovered, we then get the
    // resource URI from the metacard. After that we can finally do a
    // retrieveResource request on the Source.
    // DDF-1120 captures this issue.
    @Override
    public ResourceResponse getResource(ResourceRequest resourceRequest, boolean isEnterprise,
            String resourceSiteName) throws IOException, ResourceNotFoundException,
        ResourceNotSupportedException {
        String methodName = "getResource";
        logger.entry(methodName);
        ResourceResponse resourceResponse = null;

        validateGetResourceRequest(resourceRequest);

        try {

            for (PreResourcePlugin plugin : preResource) {
                try {
                    resourceRequest = plugin.process(resourceRequest);
                } catch (PluginExecutionException e) {
                    logger.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }

            Map<String, Serializable> requestProperties = resourceRequest.getProperties();
            logger.debug("Attempting to get resource from source id: " + resourceSiteName);

            URI resourceUri = null;
            Metacard metacard = null;
            Serializable attributeValue = resourceRequest.getAttributeValue();
            String attributeName = resourceRequest.getAttributeName();

            if (ResourceRequest.GET_RESOURCE_BY_ID.equals(attributeName)) {
                String metacardId = (String) attributeValue;
                logger.debug("Get Resource By ID.  Need to obtain resource URL from metacard: "
                        + metacardId);

                QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                        true, null, null);

                QueryResponse queryResponse = query(queryRequest);
                List<Result> results = queryResponse.getResults();
                if (!results.isEmpty()) {
                    Result result = results.get(0);
                    if (result != null) {
                        metacard = result.getMetacard();
                        if (metacard != null) {
                            resourceUri = metacard.getResourceURI();
                        }
                    }
                }
            } else if (ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI.equals(attributeName)) {
                if (attributeValue instanceof URI) {
                    resourceUri = (URI) attributeValue;
                    
                    Query propertyEqualToUriQuery = createPropertyIsEqualToQuery(
                            Metacard.RESOURCE_URI, resourceUri.toString());

                    // if isEnterprise, go out and obtain the actual source
                    // where the product's metacard is stored.
                    QueryRequest queryRequest = new QueryRequestImpl(propertyEqualToUriQuery,
                            isEnterprise, null, null);

                    QueryResponse queryResponse = query(queryRequest);
                    if (queryResponse.getResults().size() > 0) {
                        metacard = queryResponse.getResults().get(0).getMetacard();
                    }
                }
            }

            if (resourceUri == null) {
                throw new ResourceNotSupportedException(
                        "Error finding resource URI.  Cannot get product without it.");
            }
            
            if (cacheEnabled) {
                String key;
                try {
                    key = new CacheKey(metacard, resourceRequest).generateKey();
                } catch (CacheException e1) {
                    throw new ResourceNotFoundException(e1);
                }

                if (productCache.contains(key)) {
                    try {
                        Resource resource = productCache.get(key);
                        resourceResponse = new ResourceResponseImpl(resourceRequest,
                                requestProperties, resource);
                        logger.info(
                                "Successfully retrieved product from cache for metacard ID = {}",
                                metacard.getId());
                    } catch (CacheException ce) {
                        logger.info(
                                "Unable to get resource from cache. Have to retrieve it from the Source");
                    }
                }
            }


            // If resource not retrieved from cache, then invoke retrieveResource on 
            // all federated sources
            if (resourceResponse == null) {
	            for (FederatedSource currSource : federatedSources) {
	                ResourceRetriever retriever = new RemoteResourceRetriever(currSource, resourceUri, requestProperties);
	                try {
//	                    resourceResponse = currSource.retrieveResource(resourceUri, requestProperties);	                    
                        resourceResponse = retriever.retrieveResource();
	                } catch (ResourceNotFoundException e) {
	                    logger.debug("source: " + currSource.getId() + " does not contain resource.");
	                } catch (ResourceNotSupportedException e) {
	                    logger.debug("source: " + currSource.getId() + " does not support resource.");
	                } catch (IOException e) {
	                    logger.debug("error obtaining resource on source: " + currSource.getId());
	                }
	
	                if (resourceResponse != null) {
                        // Sources do not create ResourceResponses with the original ResourceRequest, hence
                        // it is added here because it will be needed for caching
                        resourceResponse = new ResourceResponseImpl(resourceRequest, resourceResponse.getProperties(), resourceResponse.getResource());
                        resourceResponse = cacheProduct(metacard, resourceResponse, retriever);
	                    break;
	                }
	            }
            }

            if (resourceResponse == null) {
                // Didn't find the resource on any of the federated sources.
                // Check the connected sources
                for (ConnectedSource currSource : connectedSources) {
                    ResourceRetriever retriever = new RemoteResourceRetriever(currSource, resourceUri, requestProperties);
                    try {
//                        resourceResponse = currSource.retrieveResource(resourceUri, requestProperties);
                        resourceResponse = retriever.retrieveResource();
                    } catch (ResourceNotFoundException e) {
                        logger.debug("source: " + currSource.getId()
                                + " does not contain resource.");
                    } catch (ResourceNotSupportedException e) {
                        logger.debug("source: " + currSource.getId()
                                + " does not support resource.");
                    } catch (IOException e) {
                        logger.debug("error obtaining resource on source: " + currSource.getId());
                    }

                    if (resourceResponse != null) {
                    	// Sources do not create ResourceResponses with the original ResourceRequest, hence
                        // it is added here because it will be needed for caching
                        resourceResponse = new ResourceResponseImpl(resourceRequest, resourceResponse.getProperties(), resourceResponse.getResource());
                        resourceResponse = cacheProduct(metacard, resourceResponse, retriever);
                        break;
                    }
                }
            }

            for (PostResourcePlugin plugin : postResource) {
                try {
                    resourceResponse = plugin.process(resourceResponse);
                } catch (PluginExecutionException e) {
                    logger.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
        } catch (RuntimeException e) {
            logger.error("Error in getResource()", e);
            throw new ResourceNotFoundException("Unable to find resource");
        } catch (StopProcessingException e) {
            logger.warn(FAILED_BY_GET_RESOURCE_PLUGIN + e.getMessage());
            throw new ResourceNotSupportedException(FAILED_BY_GET_RESOURCE_PLUGIN + e.getMessage());
        } catch (UnsupportedQueryException e) {
            throw new ResourceNotSupportedException("Could not query for resource's resource URI",
                    e);
        } catch (FederationException e) {
            throw new ResourceNotSupportedException(
                    "Error while federating query for resource's resource URI", e);
        }

        if (resourceResponse == null) {
            throw new ResourceNotFoundException(
                    "Resource could not be found for the given attribute value: "
                            + resourceRequest.getAttributeValue());
        }

        logger.exit(methodName);
        return resourceResponse;
    }

}
