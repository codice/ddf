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
package ddf.catalog.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.cache.impl.ResourceCache;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.impl.LiteralImpl;
import ddf.catalog.filter.impl.PropertyIsEqualToLiteral;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
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
import ddf.catalog.resource.download.DownloadException;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.resourceretriever.LocalResourceRetriever;
import ddf.catalog.resourceretriever.RemoteResourceRetriever;
import ddf.catalog.resourceretriever.ResourceRetriever;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.Masker;
import ddf.catalog.util.impl.SourceDescriptorComparator;
import ddf.catalog.util.impl.SourcePoller;

/**
 * CatalogFrameworkImpl is the core class of DDF. It is used for query, create, update, delete, and
 * resource retrieval operations.
 *
 * @author ddf.isgs@lmco.com
 */
@SuppressWarnings("deprecation")
public class CatalogFrameworkImpl extends DescribableImpl
        implements ConfigurationWatcher, CatalogFramework {

    protected static final String FAILED_BY_GET_RESOURCE_PLUGIN = "Error during Pre/PostResourcePlugin.";

    static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private static final String PRE_INGEST_ERROR = "Error during pre-ingest service invocation:\n\n";

    private static final String DEFAULT_RESOURCE_NOT_FOUND_MESSAGE = "Unknown resource request";

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(CatalogFrameworkImpl.class));

    private static final String FANOUT_MESSAGE =
            "Fanout proxy does not support " + "create, update, and delete operations";

    /**
     * The {@link List} of {@link CatalogProvider}s to use as the local Metadata Catalog for CRUD
     * requests. Although a {@link List} is supported, only the first {@link CatalogProvider} in the
     * list will be used as the local catalog provider.
     */
    protected List<CatalogProvider> catalogProviders;

    /**
     * The {@link List} of pre-ingest plugins to execute on the ingest request before metacard(s)
     * are created, updated, or deleted in the catalog.
     */
    protected List<PreIngestPlugin> preIngest;

    /**
     * The {@link List} of post-ingest plugins to execute on the ingest response after metacard(s)
     * have been created, updated, or deleted in the catalog.
     */
    protected List<PostIngestPlugin> postIngest;

    /**
     * The {@link List} of pre-query plugins to execute on the query request before the query is
     * executed on the catalog.
     */
    protected List<PreQueryPlugin> preQuery;

    /**
     * The {@link List} of post-query plugins to execute on the query response after a query has
     * been executed on the catalog.
     */
    protected List<PostQueryPlugin> postQuery;

    /**
     * The {@link List} of pre-resource plugins to execute on the resource request before the
     * resource is retrieved.
     */
    protected List<PreResourcePlugin> preResource;

    /**
     * The {@link List} of post-resource plugins to execute on the resource response after the
     * resource has been retrieved.
     */
    protected List<PostResourcePlugin> postResource;

    /**
     * The {@link List} of {@link ConnectedSource}s configured for this catalog and that will be
     * searched on all queries.
     */
    protected List<ConnectedSource> connectedSources;

    /**
     * The {@link List} of {@link FederatedSource}s configured for this catalog and will be searched
     * on enterprise and site-specific queries.
     */
    protected List<FederatedSource> federatedSources;

    /**
     * The default federation strategy (e.g. Sorted).
     */
    protected FederationStrategy defaultFederationStrategy;

    /**
     * The {@link List} of {@link ResourceReader}s configured for this catalog and that can be used
     * to retrieve a resource.
     */
    protected List<ResourceReader> resourceReaders;

    /**
     * The OSGi bundle context for this catalog framework.
     */
    protected BundleContext context;

    // TODO make this private

    // TODO make this private
    protected int threadPoolSize;

    /**
     * An {@link ExecutorService} used to manage threaded operations
     */
    protected ExecutorService pool;

    // TODO make this private

    protected ResourceCache productCache;

    protected DownloadsStatusEventPublisher retrieveStatusEventPublisher;

    protected boolean notificationEnabled = true;

    protected boolean activityEnabled = true;

    protected ReliableResourceDownloadManager reliableResourceDownloadManager;

    // The local catalog provider, which is set to the first item in the {@link List} of
    // {@link CatalogProvider}s.
    // Keep this private to make sure subclasses don't use it.
    private CatalogProvider catalog;

    private Masker masker;

    private SourcePoller poller;

    private boolean fanoutEnabled = false;

    private QueryResponsePostProcessor queryResponsePostProcessor;

    /**
     * Instantiates a new CatalogFrameworkImpl
     *
     * @param context                    The BundleContext that will be utilized by this instance.
     * @param catalogProvider            The {@link CatalogProvider} used for query, create, update, and delete operations.
     * @param preIngest                  A {@link List} of {@link PreIngestPlugin}(s) that will be invoked prior to the
     *                                   ingest operation.
     * @param postIngest                 A list of {@link PostIngestPlugin}(s) that will be invoked after the ingest
     *                                   operation.
     * @param preQuery                   A {@link List} of {@link PreQueryPlugin}(s) that will be invoked prior to the
     *                                   query operation.
     * @param postQuery                  A {@link List} of {@link PostQueryPlugin}(s) that will be invoked after the query
     *                                   operation.
     * @param preResource                A {@link List} of {@link PreResourcePlugin}(s) that will be invoked prior to the
     *                                   getResource operation.
     * @param postResource               A {@link List} of {@link PostResourcePlugin}(s) that will be invoked after the
     *                                   getResource operation.
     * @param connectedSources           {@link List} of {@link ConnectedSource}(s) that will be searched on all queries
     * @param federatedSources           A {@link List} of {@link FederatedSource}(s) that will be searched on an
     *                                   enterprise query.
     * @param resourceReaders            Set of {@link ResourceReader}(s) that will be get a {@link Resource}
     * @param queryStrategy              The default federation strategy (e.g. Sorted).
     * @param queryResponsePostProcessor The {@link QueryResponsePostProcessor} to use to do extra processing on the
     *                                   response before calling any post-query plug-ins registered.
     * @param pool                       An ExecutorService used to manage threaded operations.
     * @param poller                     An {@link SourcePoller} used to poll source availability.
     * @deprecated Use
     * {@link #CatalogFrameworkImpl(List, BundleContext, List, List, List, List, List, List, List, List, List, FederationStrategy, QueryResponsePostProcessor, ExecutorService, SourcePoller, ResourceCache, DownloadsStatusEventPublisher, ReliableResourceDownloadManager)}
     */
    public CatalogFrameworkImpl(BundleContext context, CatalogProvider catalogProvider,
            List<PreIngestPlugin> preIngest, List<PostIngestPlugin> postIngest,
            List<PreQueryPlugin> preQuery, List<PostQueryPlugin> postQuery,
            List<PreResourcePlugin> preResource, List<PostResourcePlugin> postResource,
            List<ConnectedSource> connectedSources, List<FederatedSource> federatedSources,
            List<ResourceReader> resourceReaders, FederationStrategy queryStrategy,
            QueryResponsePostProcessor queryResponsePostProcessor, ExecutorService pool,
            SourcePoller poller, ResourceCache resourceCache,
            DownloadsStatusEventPublisher eventPublisher, ReliableResourceDownloadManager rrdm) {
        this(Collections.singletonList(catalogProvider), context, preIngest, postIngest, preQuery,
                postQuery, preResource, postResource, connectedSources, federatedSources,
                resourceReaders, queryStrategy, queryResponsePostProcessor, pool, poller,
                resourceCache, eventPublisher, rrdm);
    }

    /**
     * Instantiates a new CatalogFrameworkImpl
     *
     * @param catalogProviders           A {@link List} of {@link CatalogProvider} used for query, create, update, and
     *                                   delete operations. Only the first item in this list is used as the local catalog
     *                                   provider. A list is used to be able to detect when an actual CatalogProvider is
     *                                   instantiated and bound by blueprint.
     * @param context                    The BundleContext that will be utilized by this instance.
     * @param preIngest                  A {@link List} of {@link PreIngestPlugin}(s) that will be invoked prior to the
     *                                   ingest operation.
     * @param postIngest                 A list of {@link PostIngestPlugin}(s) that will be invoked after the ingest
     *                                   operation.
     * @param preQuery                   A {@link List} of {@link PreQueryPlugin}(s) that will be invoked prior to the
     *                                   query operation.
     * @param postQuery                  A {@link List} of {@link PostQueryPlugin}(s) that will be invoked after the query
     *                                   operation.
     * @param preResource                A {@link List} of {@link PreResourcePlugin}(s) that will be invoked prior to the
     *                                   getResource operation.
     * @param postResource               A {@link List} of {@link PostResourcePlugin}(s) that will be invoked after the
     *                                   getResource operation.
     * @param connectedSources           {@link List} of {@link ConnectedSource}(s) that will be searched on all queries
     * @param federatedSources           A {@link List} of {@link FederatedSource}(s) that will be searched on an
     *                                   enterprise query.
     * @param resourceReaders            Set of {@link ResourceReader}(s) that will be get a {@link Resource}.
     * @param queryStrategy              The default federation strategy (e.g. Sorted).
     * @param queryResponsePostProcessor The {@link QueryResponsePostProcessor} to use to do extra processing on the
     *                                   response before calling any post-query plug-ins registered.
     * @param pool                       An ExecutorService used to manage threaded operations.
     * @param poller                     An {@link SourcePoller} used to poll source availability.
     */

    // NOTE: The List<CatalogProvider> argument is first because when it was the second
    // argument (like in the deprecated constructor above) the following error occurs during
    // DDF startup:
    // org.osgi.service.blueprint.container.ComponentDefinitionException:
    // Unable to convert value BeanRecipe[name='#recipe-125'] to type class java.util.ArrayList
    // Caused by:
    // org.osgi.service.blueprint.container.ComponentDefinitionException: Multiple matching
    // constructors found on class ddf.catalog.CatalogFrameworkImpl for arguments
    // [org.eclipse.osgi.framework.internal.core.BundleContextImpl@191e31ea,
    // ddf.catalog.util.SortedServiceList@6013a567, ddf.catalog.util.SortedServiceList@29d03e78,
    // ddf.catalog.util.SortedServiceList@26b54dba, ddf.catalog.util.SortedServiceList@49020230,
    // ddf.catalog.util.SortedServiceList@22ddc2c2, ddf.catalog.util.SortedServiceList@d1d6070,
    // org.apache.aries.blueprint.container.ReferenceListRecipe$ProvidedObject@1073f623,
    // org.apache.aries.blueprint.container.ReferenceListRecipe$ProvidedObject@2d247c45,
    // org.apache.aries.blueprint.container.ReferenceListRecipe$ProvidedObject@365aad2a,
    // ddf.catalog.util.SortedServiceList@3a65fca,
    // ddf.catalog.federation.impl.SortedFederationStrategy@7b1ebc46,
    // java.util.concurrent.ThreadPoolExecutor@1edad6d0, ddf.catalog.util.SourcePoller@314d0183]
    // when instanciating bean ddf: [public
    // ddf.catalog.CatalogFrameworkImpl(org.osgi.framework.BundleContext,ddf.catalog.source.CatalogProvider,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,ddf.catalog.federation.FederationStrategy,java.util.concurrent.ExecutorService,ddf.catalog.util.SourcePoller),
    // public
    // ddf.catalog.CatalogFrameworkImpl(org.osgi.framework.BundleContext,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,java.util.List,ddf.catalog.federation.FederationStrategy,java.util.concurrent.ExecutorService,ddf.catalog.util.SourcePoller)]

    // Don't exactly know what the problem is, but it has something to do with DDF's ListConverter
    // and blueprint trying to convert List<CatalogProvider>. Additionally,
    // the List<CatalogProvider> argument cannot be adjacent to the other List<T> arguments in the
    // signature - it must be separated by another type, hence the BundleContext
    // argument being second.
    public CatalogFrameworkImpl(List<CatalogProvider> catalogProviders, BundleContext context,
            List<PreIngestPlugin> preIngest, List<PostIngestPlugin> postIngest,
            List<PreQueryPlugin> preQuery, List<PostQueryPlugin> postQuery,
            List<PreResourcePlugin> preResource, List<PostResourcePlugin> postResource,
            List<ConnectedSource> connectedSources, List<FederatedSource> federatedSources,
            List<ResourceReader> resourceReaders, FederationStrategy queryStrategy,
            QueryResponsePostProcessor queryResponsePostProcessor, ExecutorService pool,
            SourcePoller poller, ResourceCache resourceCache,
            DownloadsStatusEventPublisher eventPublisher, ReliableResourceDownloadManager rrdm) {
        this.context = context;
        this.catalogProviders = catalogProviders;
        if (LOGGER.isDebugEnabled()) {
            if (this.catalogProviders != null) {
                LOGGER.info("catalog providers list size = " + this.catalogProviders.size());
            } else {
                LOGGER.info("catalog providers list is NULL");
            }
        }

        this.preIngest = preIngest;
        this.postIngest = postIngest;
        this.preQuery = preQuery;
        this.postQuery = postQuery;
        this.preResource = preResource;
        this.postResource = postResource;
        this.connectedSources = connectedSources;
        this.federatedSources = federatedSources;
        this.resourceReaders = resourceReaders;
        this.defaultFederationStrategy = queryStrategy;
        this.queryResponsePostProcessor = queryResponsePostProcessor;
        this.poller = poller;
        this.productCache = resourceCache;
        this.retrieveStatusEventPublisher = eventPublisher;
        this.reliableResourceDownloadManager = rrdm;
        synchronized (this) {
            this.pool = pool;
        }
    }

    public void setFanoutEnabled(boolean fanoutEnabled) {
        this.fanoutEnabled = fanoutEnabled;
    }

    public void setReliableResourceDownloadManager(ReliableResourceDownloadManager rrdm) {
        this.reliableResourceDownloadManager = rrdm;
    }

    public void setProductCache(ResourceCache productCache) {
        LOGGER.debug("Injecting productCache");
        this.productCache = productCache;
    }

    public void setProductCacheDirectory(String productCacheDirectory) {
        LOGGER.debug("Setting product cache directory to {}", productCacheDirectory);
        this.productCache.setProductCacheDirectory(productCacheDirectory);
    }

    public void setCacheDirMaxSizeMegabytes(long maxSize) {
        LOGGER.debug("Setting product cache max size to {}", maxSize);
        this.productCache.setCacheDirMaxSizeMegabytes(maxSize);
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        LOGGER.debug("Setting cacheEnabled = {}", cacheEnabled);
        this.reliableResourceDownloadManager.setCacheEnabled(cacheEnabled);
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        LOGGER.debug("Setting notificationEnabled = {}", notificationEnabled);
        this.notificationEnabled = notificationEnabled;
        retrieveStatusEventPublisher.setNotificationEnabled(notificationEnabled);
    }

    public void setActivityEnabled(boolean activityEnabled) {
        LOGGER.debug("Setting activityEnabled = {}", activityEnabled);
        this.activityEnabled = activityEnabled;
        retrieveStatusEventPublisher.setActivityEnabled(activityEnabled);
    }

    /**
     * Set the delay, in seconds, between product retrieval retry attempts.
     *
     * @param delayBetweenAttempts
     */
    public void setDelayBetweenRetryAttempts(int delayBetweenAttempts) {
        LOGGER.debug("Setting delayBetweenRetryAttempts = {} s", delayBetweenAttempts);
        this.reliableResourceDownloadManager.setDelayBetweenAttempts(delayBetweenAttempts);
    }

    /**
     * Maximum number of attempts to try and retrieve product
     */
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        LOGGER.debug("Setting maxRetryAttempts = {}", maxRetryAttempts);
        this.reliableResourceDownloadManager.setMaxRetryAttempts(maxRetryAttempts);
    }

    /**
     * Set the frequency, in seconds, to monitor the product retrieval.
     * If this amount of time passes with no bytes being retrieved for
     * the product, then the monitor will start a new download attempt.
     *
     * @param retrievalMonitorPeriod
     */
    public void setRetrievalMonitorPeriod(int retrievalMonitorPeriod) {
        LOGGER.debug("Setting retrievalMonitorPeriod = {} s", retrievalMonitorPeriod);
        this.reliableResourceDownloadManager.setMonitorPeriod(retrievalMonitorPeriod);
    }

    public void setCacheWhenCanceled(boolean cacheWhenCanceled) {
        LOGGER.debug("Setting cacheWhenCanceled = {}", cacheWhenCanceled);
        this.reliableResourceDownloadManager.setCacheWhenCanceled(cacheWhenCanceled);
    }

    public void setRetrieveStatusEventPublisher(
            DownloadsStatusEventPublisher retrieveStatusEventPublisher) {
        this.retrieveStatusEventPublisher = retrieveStatusEventPublisher;
    }

    /**
     * Invoked by blueprint when a {@link CatalogProvider} is created and bound to this
     * CatalogFramework instance.
     * <p/>
     * The local catalog provider will be set to the first item in the {@link List} of
     * {@link CatalogProvider}s bound to this CatalogFramework.
     *
     * @param catalogProvider the {@link CatalogProvider} being bound to this CatalogFramework instance
     */
    public void bind(CatalogProvider catalogProvider) {
        LOGGER.trace("ENTERING: bind with CatalogProvider arg");

        LOGGER.info("catalog providers list size = " + this.catalogProviders.size());

        // The list of catalog providers is sorted by OSGi service ranking, hence should
        // always set the local catalog provider to the first item in the list.
        this.catalog = catalogProviders.get(0);

        LOGGER.trace("EXITING: bind with CatalogProvider arg");
    }

    /**
     * Invoked by blueprint when a {@link CatalogProvider} is deleted and unbound from this
     * CatalogFramework instance.
     * <p/>
     * The local catalog provider will be reset to the new first item in the {@link List} of
     * {@link CatalogProvider}s bound to this CatalogFramework. If this list of catalog providers is
     * currently empty, then the local catalog provider will be set to <code>null</code>.
     *
     * @param catalogProvider the {@link CatalogProvider} being unbound from this CatalogFramework instance
     */
    public void unbind(CatalogProvider catalogProvider) {
        LOGGER.trace("ENTERING: unbind with CatalogProvider arg");

        if (this.catalogProviders.size() > 0) {
            LOGGER.info("catalog providers list size = " + this.catalogProviders.size());
            LOGGER.info("Setting catalog to first provider in list");

            // The list of catalog providers is sorted by OSGi service ranking, hence should
            // always set the local catalog provider to the first item in the list.
            this.catalog = catalogProviders.get(0);
        } else {
            LOGGER.info("Setting catalog = NULL");
            this.catalog = null;
        }

        LOGGER.trace("EXITING: unbind with CatalogProvider arg");
    }

    /**
     * Sets the {@link Masker}
     *
     * @param masker the {@link Masker} this framework will use
     */
    public void setMasker(Masker masker) {
        synchronized (this) {

            this.masker = masker;
            if (this.getId() != null) {
                masker.setId(getId());
            }
        }

    }

    /**
     * Sets the source id to identify this framework (DDF). This is also referred to as the site
     * name.
     *
     * @param sourceId the sourceId to set
     */
    @Override
    public void setId(String sourceId) {
        LOGGER.debug("Setting id = " + sourceId);
        synchronized (this) {
            super.setId(sourceId);
            if (masker != null) {
                masker.setId(sourceId);
            }
        }
    }

    @Override
    public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
            throws SourceUnavailableException {
        final String methodName = "getSourceInfo";
        SourceInfoResponse response;
        Set<SourceDescriptor> sourceDescriptors;
        LOGGER.entry(methodName);

        if (fanoutEnabled) {
            return getFanoutSourceInfo(sourceInfoRequest);
        }

        boolean addCatalogProviderDescriptor = false;
        try {
            validateSourceInfoRequest(sourceInfoRequest);
            // Obtain the source information based on the sourceIds in the
            // request

            sourceDescriptors = new LinkedHashSet<>();
            Set<String> requestedSourceIds = sourceInfoRequest.getSourceIds();

            // If it is an enterprise request than add all source information for the enterprise
            if (sourceInfoRequest.isEnterprise()) {

                sourceDescriptors = getFederatedSourceDescriptors(federatedSources, true);
                // If Ids are specified check if they are known sources
            } else if (requestedSourceIds != null) {
                LOGGER.debug("getSourceRequest contains requested source ids");
                Set<FederatedSource> discoveredSources = new HashSet<>();
                boolean containsId = false;

                for (String requestedSourceId : requestedSourceIds) {
                    // Check if the requestedSourceId can be found in the known federatedSources
                    for (FederatedSource federatedSource : this.federatedSources) {

                        if (requestedSourceId.equals(federatedSource.getId())) {
                            containsId = true;
                            LOGGER.debug("found federated source: " + requestedSourceId);
                            discoveredSources.add(federatedSource);
                            break;
                        }

                    }
                    if (!containsId) {
                        LOGGER.debug("Unable to find source: " + requestedSourceId);

                        // Check for the local catalog provider, DDF sourceId represents this
                        if (requestedSourceId.equals(getId())) {
                            LOGGER.debug(
                                    "adding CatalogSourceDescriptor since it was in sourceId list as: "
                                            + requestedSourceId);
                            addCatalogProviderDescriptor = true;
                        }
                    }
                    containsId = false;

                }

                sourceDescriptors = getFederatedSourceDescriptors(discoveredSources,
                        addCatalogProviderDescriptor);

            } else {
                // only add the local catalogProviderdescriptor
                addCatalogSourceDescriptor(sourceDescriptors);
            }

            response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing getSourceInfo", re);
            throw new SourceUnavailableException(
                    "Exception during runtime while performing getSourceInfo");

        }

        LOGGER.exit(methodName);
        return response;
    }

    /**
     * Retrieves the {@link SourceDescriptor} info for all {@link FederatedSource}s in the fanout
     * configuration, but the all of the source info, e.g., content types, for all of the available
     * {@link FederatedSource}s is packed into one {@SourceDescriptor
     * <p/>
     * } for the
     * fanout configuration with the fanout's site name in it. This keeps the individual
     * {@link FederatedSource}s' source info hidden from the external client.
     */
    public SourceInfoResponse getFanoutSourceInfo(SourceInfoRequest sourceInfoRequest)
            throws SourceUnavailableException {

        final String methodName = "getSourceInfo";
        SourceInfoResponse response;
        SourceDescriptorImpl sourceDescriptor;
        LOGGER.entry(methodName);
        try {

            // request
            if (sourceInfoRequest == null) {
                IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                        "SourceInfoRequest was null");
                LOGGER.throwing(illegalArgumentException);
                throw illegalArgumentException;
            }

            Set<SourceDescriptor> sourceDescriptors = new LinkedHashSet<>();
            Set<String> ids = sourceInfoRequest.getSourceIds();

            // Only return source descriptor information if this sourceId is
            // specified
            if (ids != null && !ids.isEmpty()) {
                for (String id : ids) {
                    if (!id.equals(this.getId())) {
                        SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                                "Unknown source: " + id);
                        LOGGER.warn("Throwing SourceUnavilableExcption for unknown source: {}", id,
                                sourceUnavailableException);
                        throw sourceUnavailableException;

                    }
                }

            }
            // Fanout will only add one source descriptor with all the contents
            Set<ContentType> contentTypes = new HashSet<>();

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
            LOGGER.warn("Exception during runtime while performing create", re);
            throw new SourceUnavailableException(
                    "Exception during runtime while performing getSourceInfo", re);

        }
        LOGGER.exit(methodName);
        return response;

    }

    /**
     * Creates a {@link Set} of {@link SourceDescriptor} based on the incoming list of
     * {@link Source}.
     *
     * @param sources {@link Collection} of {@link Source} to obtain descriptor information from
     * @return new {@link Set} of {@link SourceDescriptor}
     */
    private Set<SourceDescriptor> getFederatedSourceDescriptors(Collection<FederatedSource> sources,
            boolean addCatalogProviderDescriptor) {
        SourceDescriptorImpl sourceDescriptor;
        Set<SourceDescriptor> sourceDescriptors = new HashSet<>();
        if (sources != null) {
            for (Source source : sources) {
                if (source != null) {
                    String sourceId = source.getId();
                    LOGGER.debug("adding sourceId: " + sourceId);

                    // check the poller for cached information
                    if (poller != null && poller.getCachedSource(source) != null) {
                        source = poller.getCachedSource(source);
                    }

                    sourceDescriptor = new SourceDescriptorImpl(sourceId, source.getContentTypes());
                    sourceDescriptor.setVersion(source.getVersion());
                    sourceDescriptor.setAvailable(source.isAvailable());

                    sourceDescriptors.add(sourceDescriptor);
                }
            }
        }
        if (addCatalogProviderDescriptor) {
            addCatalogSourceDescriptor(sourceDescriptors);
        }

        Set<SourceDescriptor> orderedDescriptors = new TreeSet<>(new SourceDescriptorComparator());

        orderedDescriptors.addAll(sourceDescriptors);
        return orderedDescriptors;

    }

    private void validateSourceInfoRequest(SourceInfoRequest sourceInfoRequest) {
        if (sourceInfoRequest == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                    "SourceInfoRequest was null");
            LOGGER.throwing(illegalArgumentException);
            throw illegalArgumentException;
        }
    }

    /**
     * Adds the local catalog's {@link SourceDescriptor} to the set of {@link SourceDescriptor}s for
     * this framework.
     *
     * @param descriptors the set of {@link SourceDescriptor}s to add the local catalog's descriptor to
     */
    protected void addCatalogSourceDescriptor(Set<SourceDescriptor> descriptors) {
        /*
         * DDF-1614 if (catalog != null && descriptors != null ) { SourceDescriptorImpl descriptor =
         * new SourceDescriptorImpl(getId(), catalog.getContentTypes());
         * descriptor.setVersion(this.getVersion()); descriptors.add(descriptor); }
         */
        // DDF-1614: Even when no local catalog provider is configured should still
        // return a local site with the framework's ID and version (and no content types
        // since there is no catalog provider).
        // But when a local catalog provider is configured, include its content types in the
        // local site info.
        if (descriptors != null) {
            Set<ContentType> contentTypes = new HashSet<>();
            if (catalog != null) {
                contentTypes = catalog.getContentTypes();
            }
            SourceDescriptorImpl descriptor = new SourceDescriptorImpl(this.getId(), contentTypes);
            descriptor.setVersion(this.getVersion());
            descriptors.add(descriptor);
        }
    }

    @Override
    public CreateResponse create(CreateRequest createRequest)
            throws IngestException, SourceUnavailableException {
        final String methodName = "create";
        LOGGER.entry(methodName);

        if (fanoutEnabled) {
            IngestException ingestException = new IngestException(FANOUT_MESSAGE);
            LOGGER.throwing(ingestException);
            throw ingestException;
        }

        CreateRequest createReq = createRequest;

        validateCreateRequest(createReq);

        if (!sourceIsAvailable(catalog)) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform create operation.");
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on create operation, local provider not available. {}"
                                + " metacards failed to ingest. {}",
                        createReq.getMetacards().size(), buildIngestLog(createReq),
                        sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        CreateResponse createResponse = null;

        Exception ingestError = null;
        try {
            for (PreIngestPlugin plugin : preIngest) {
                try {
                    createReq = plugin.process(createReq);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            validateCreateRequest(createReq);

            // Call the create on the catalog
            LOGGER.debug("Calling catalog.create() with " + createReq.getMetacards().size()
                    + " entries.");
            createResponse = catalog.create(createRequest);
        } catch (IngestException iee) {
            INGEST_LOGGER.warn("Ingest error", iee);
            ingestError = iee;
            throw iee;
        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR, see);
            ingestError = see;
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());
        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing create", re);
            ingestError = re;
            throw new IngestException("Exception during runtime while performing create");
        } finally {
            if (ingestError != null && INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on create operation. {} metacards failed to ingest. {}",
                        createReq.getMetacards().size(), buildIngestLog(createReq), ingestError);
            }
        }

        try {
            createResponse = validateFixCreateResponse(createResponse, createReq);
            for (final PostIngestPlugin plugin : postIngest) {
                try {
                    createResponse = plugin.process(createResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
        } catch (RuntimeException re) {
            LOGGER.warn(
                    "Exception during runtime while performing doing post create operations (plugins and pubsub)",
                    re);

        } finally {
            LOGGER.exit(methodName);
        }

        // if debug is enabled then catalog might take a significant performance hit w/r/t string
        // building
        if (INGEST_LOGGER.isDebugEnabled()) {
            INGEST_LOGGER.debug("{} metacards were successfully ingested. {}",
                    createReq.getMetacards().size(), buildIngestLog(createReq));
        }
        return createResponse;
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest)
            throws IngestException, SourceUnavailableException {
        final String methodName = "update";
        LOGGER.entry(methodName);

        if (fanoutEnabled) {
            IngestException ingestException = new IngestException(FANOUT_MESSAGE);
            LOGGER.throwing(ingestException);
            throw ingestException;
        }

        if (!sourceIsAvailable(catalog)) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform update operation.");
            LOGGER.throwing(sourceUnavailableException);
            throw sourceUnavailableException;
        }
        UpdateRequest updateReq = updateRequest;
        validateUpdateRequest(updateReq);
        UpdateResponse updateResponse = null;
        try {

            for (PreIngestPlugin plugin : preIngest) {
                try {
                    updateReq = plugin.process(updateReq);
                } catch (PluginExecutionException e) {
                    LOGGER.warn("error processing update in PreIngestPlugin", e);
                }
            }
            validateUpdateRequest(updateReq);

            // Call the create on the catalog
            LOGGER.debug("Calling catalog.update() with " + updateRequest.getUpdates().size()
                    + " updates.");
            updateResponse = catalog.update(updateReq);

            // Handle the posting of messages to pubsub
            updateResponse = validateFixUpdateResponse(updateResponse, updateReq);
            for (final PostIngestPlugin plugin : postIngest) {
                try {
                    updateResponse = plugin.process(updateResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info("Plugin exception", e);
                }
            }

        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR, see);
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing update", re);
            throw new IngestException("Exception during runtime while performing update");

        } finally {
            LOGGER.exit(methodName);
        }

        return updateResponse;
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest)
            throws IngestException, SourceUnavailableException {
        final String methodName = "delete";
        LOGGER.entry(methodName);

        if (fanoutEnabled) {
            IngestException ingestException = new IngestException(FANOUT_MESSAGE);
            LOGGER.throwing(ingestException);
            throw ingestException;
        }

        if (!sourceIsAvailable(catalog)) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform delete operation.");
            LOGGER.throwing(sourceUnavailableException);
            throw sourceUnavailableException;
        }

        validateDeleteRequest(deleteRequest);
        DeleteResponse deleteResponse = null;
        try {
            for (PreIngestPlugin plugin : preIngest) {
                try {
                    deleteRequest = plugin.process(deleteRequest);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            validateDeleteRequest(deleteRequest);

            // Call the Provider delete method
            LOGGER.debug(
                    "Calling catalog.delete() with " + deleteRequest.getAttributeValues().size()
                            + " entries.");
            deleteResponse = catalog.delete(deleteRequest);

            // Post results to be available for pubsub
            deleteResponse = validateFixDeleteResponse(deleteResponse, deleteRequest);
            for (final PostIngestPlugin plugin : postIngest) {
                try {
                    deleteResponse = plugin.process(deleteResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info("Plugin exception", e);
                }
            }

        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR + see.getMessage(), see);
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing delete", re);
            throw new IngestException("Exception during runtime while performing delete");

        } finally {
            LOGGER.exit(methodName);
        }

        return deleteResponse;
    }

    @Override
    public QueryResponse query(QueryRequest fedQueryRequest)
            throws UnsupportedQueryException, FederationException {
        return query(fedQueryRequest, null);
    }

    /**
     * Determines if this catalog framework has any {@link ConnectedSource}s configured.
     *
     * @return true if this framework has any connected sources configured, false otherwise
     */
    protected boolean connectedSourcesExist() {
        return connectedSources != null && connectedSources.size() > 0;
    }

    /**
     * Determines if the specified {@link QueryRequest} is a federated query, meaning it is either
     * an enterprise query or it lists specific sources to be queried by their source IDs.
     *
     * @param queryRequest the {@link QueryRequest}
     * @return true if the request is an enterprise or site-specific query, false otherwise
     */
    protected boolean isFederated(QueryRequest queryRequest) {
        Set<String> sourceIds = queryRequest.getSourceIds();

        return queryRequest.isEnterprise() || sourceIds != null && (sourceIds.size() > 1
                || sourceIds.size() == 1 && !sourceIds.contains("") && !sourceIds.contains(null)
                && !sourceIds.contains(getId()));
    }

    @Override
    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
            throws UnsupportedQueryException, FederationException {
        return query(queryRequest, strategy, false);
    }

    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy,
            boolean overrideFanoutRename) throws UnsupportedQueryException, FederationException {

        String methodName = "query";
        LOGGER.entry(methodName);
        FederationStrategy fedStrategy = strategy;
        QueryResponse queryResponse = null;
        QueryRequest queryReq = queryRequest;

        try {
            validateQueryRequest(queryReq);

            if (fanoutEnabled) {
                // Force an enterprise query
                queryReq = new QueryRequestImpl(queryRequest.getQuery(), true, null,
                        queryRequest.getProperties());
            }

            for (PreQueryPlugin service : preQuery) {
                try {
                    queryReq = service.process(queryReq);
                } catch (PluginExecutionException see) {
                    LOGGER.warn("Error executing PreQueryPlugin: " + see.getMessage(), see);
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }

            validateQueryRequest(queryReq);

            if (fedStrategy == null) {
                if (defaultFederationStrategy == null) {
                    FederationException federationException = new FederationException(
                            "No Federation Strategies exist.  Cannot execute federated query.");
                    LOGGER.throwing(federationException);
                    throw federationException;
                } else {
                    LOGGER.debug("FederationStratgy was not specified, using default strategy: "
                            + defaultFederationStrategy.getClass());
                    fedStrategy = defaultFederationStrategy;
                }
            }

            queryResponse = doQuery(queryReq, fedStrategy);

            validateFixQueryResponse(queryResponse, queryReq, overrideFanoutRename);

            for (PostQueryPlugin service : postQuery) {
                try {
                    queryResponse = service.process(queryResponse);
                } catch (PluginExecutionException see) {
                    LOGGER.warn("Error executing PostQueryPlugin: " + see.getMessage(), see);
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing query", re);
            throw new UnsupportedQueryException("Exception during runtime while performing query");

        } finally {
            LOGGER.exit(methodName);
        }

        return queryResponse;

    }

    /**
     * Executes a query using the specified {@link QueryRequest} and {@link FederationStrategy}.
     * Based on the isEnterprise and sourceIds list in the query request, the federated query may
     * include the local provider and {@link ConnectedSource}s.
     *
     * @param queryRequest the {@link QueryRequest}
     * @param strategy
     * @return the {@link QueryResponse}
     * @throws FederationException
     */
    private QueryResponse doQuery(QueryRequest queryRequest, FederationStrategy strategy)
            throws FederationException {
        String methodName = "doQuery";
        LOGGER.entry(methodName);

        Set<ProcessingDetails> exceptions = new HashSet<>();
        Set<String> sourceIds = queryRequest.getSourceIds();
        LOGGER.debug("source ids: " + sourceIds);
        List<Source> sourcesToQuery = new ArrayList<>();
        boolean addConnectedSources = false;
        boolean addCatalogProvider = false;
        boolean sourceFound;

        // Check if it's an enterprise query
        if (queryRequest.isEnterprise()) {
            addConnectedSources = true;
            addCatalogProvider = hasCatalogProvider();

            if (sourceIds != null && !sourceIds.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Enterprise Query also included specific sites which will now be ignored");
                }
                sourceIds.clear();
            }

            // add all the federated sources
            for (FederatedSource source : federatedSources) {
                if (sourceIsAvailable(source)) {
                    sourcesToQuery.add(source);
                } else {
                    exceptions.add(createUnavailableProcessingDetails(source));
                }
            }

        } else if (sourceIds != null && !sourceIds.isEmpty()) {
            // it's a targeted federated query
            if (includesLocalSources(sourceIds)) {
                LOGGER.debug("Local source is included in sourceIds");
                addConnectedSources = connectedSourcesExist();
                addCatalogProvider = hasCatalogProvider();
                sourceIds.remove(getId());
                sourceIds.remove(null);
                sourceIds.remove("");
            }

            // See if we still have sources to look up by name
            if (sourceIds.size() > 0) {
                // TODO make this more efficient
                // In fanout cases, we may have multiple sources with the same
                // ID. So go through all of them.
                for (String id : sourceIds) {
                    LOGGER.debug("Looking up source ID = " + id);
                    sourceFound = false;
                    for (FederatedSource source : federatedSources) {
                        if (id != null && id.equals(source.getId())) {
                            sourceFound = true;
                            if (sourceIsAvailable(source)) {
                                sourcesToQuery.add(source);
                            } else {
                                exceptions.add(createUnavailableProcessingDetails(source));
                            }
                        }
                    }
                    if (!sourceFound) {
                        exceptions.add(new ProcessingDetailsImpl(id,
                                new Exception("Source id is not found")));
                    }
                }
            }
        } else {
            // default to local sources
            addConnectedSources = connectedSourcesExist();
            addCatalogProvider = hasCatalogProvider();
        }

        if (addConnectedSources) {
            // add Connected Sources
            for (ConnectedSource source : connectedSources) {
                if (sourceIsAvailable(source)) {
                    sourcesToQuery.add(source);
                } else {
                    // do nothing -- we don't care if a connected source is
                    // unavailable.
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Connected Source \"" + source.getId()
                                + " is unavailable and will not be queried.");
                    }
                }
            }
        }

        if (addCatalogProvider) {
            if (sourceIsAvailable(catalog)) {
                sourcesToQuery.add(catalog);
            } else {
                exceptions.add(createUnavailableProcessingDetails(catalog));
            }
        }

        if (sourcesToQuery.isEmpty()) {
            // We have nothing to query at all.
            FederationException federationException = new FederationException(
                    "SiteNames could not be resolved to valid sites, or none of the sites were available.");
            LOGGER.throwing(XLogger.Level.DEBUG, federationException);
            // TODO change to SourceUnavailableException
            throw federationException;
        }

        LOGGER.debug("Calling strategy.federate()");

        QueryResponse response = strategy.federate(sourcesToQuery, queryRequest);
        queryResponsePostProcessor.processResponse(response);
        return addProcessingDetails(exceptions, response);
    }

    /**
     * Adds any exceptions to the query response's processing details.
     *
     * @param exceptions the set of exceptions to include in the response's {@link ProcessingDetails}. Can
     *                   be empty, but cannot be null.
     * @param response   the {@link QueryResponse} to add the exceptions to
     * @return the modified {@link QueryResponse}
     */
    protected QueryResponse addProcessingDetails(Set<ProcessingDetails> exceptions,
            QueryResponse response) {

        if (!exceptions.isEmpty()) {
            // we have exceptions to merge in
            if (response == null) {
                LOGGER.error(
                        "Could not add Query exceptions to a QueryResponse because the list of ProcessingDetails was null -- according to the API this should not happen");
            } else {
                // need to merge them together.
                Set<ProcessingDetails> sourceDetails = response.getProcessingDetails();
                sourceDetails.addAll(exceptions);
            }
        }
        return response;
    }

    /**
     * Determines if the local catlog provider's source ID is included in the list of source IDs. A
     * source ID in the list of null or an empty string are treated the same as the local source's
     * actual ID being in the list.
     *
     * @param sourceIds the list of source IDs to examine
     * @return true if the list includes the local source's ID, false otherwise
     */
    private boolean includesLocalSources(Set<String> sourceIds) {
        return sourceIds != null && (sourceIds.contains(getId()) || sourceIds.contains("")
                || sourceIds.contains(null));
    }

    /**
     * Whether this {@link CatalogFramework} is configured with a {@link CatalogProvider}.
     * {@link ddf.catalog.FanoutCatalogFramework} does not.
     *
     * @return true if this {@link CatalogFrameworkImpl} has a {@link CatalogProvider} configured,
     * false otherwise
     */
    protected boolean hasCatalogProvider() {
        if (!this.fanoutEnabled && this.catalog != null) {
            LOGGER.trace("hasCatalogProvider() returning true");
            return true;
        }

        LOGGER.trace("hasCatalogProvider() returning false");
        return false;
    }

    /**
     * @param source
     * @return
     */
    private ProcessingDetailsImpl createUnavailableProcessingDetails(Source source) {
        ProcessingDetailsImpl exception = new ProcessingDetailsImpl();
        SourceUnavailableException sue = new SourceUnavailableException(
                "Source \"" + source.getId() + "\" is unavailable and will not be queried");
        exception.setException(sue);
        exception.setSourceId(source.getId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Source Unavailable", sue);
        }
        return exception;
    }

    @Override
    public BinaryContent transform(Metacard metacard, String transformerShortname,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        ServiceReference[] refs;
        try {
            // TODO replace shortname with id
            refs = context.getServiceReferences(MetacardTransformer.class.getName(),
                    "(|" + "(" + Constants.SERVICE_SHORTNAME + "=" + transformerShortname + ")"
                            + "(" + Constants.SERVICE_ID + "=" + transformerShortname + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid transformer shortName: " + transformerShortname, e);
        }
        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException(
                    "Transformer " + transformerShortname + " not found");
        } else {
            MetacardTransformer transformer = (MetacardTransformer) context.getService(refs[0]);
            if (metacard != null) {
                return transformer.transform(metacard, arguments);
            } else {
                throw new IllegalArgumentException("Metacard is null.");
            }
        }
    }

    @Override
    public BinaryContent transform(SourceResponse response, String transformerShortname,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        ServiceReference[] refs;
        try {
            refs = context.getServiceReferences(QueryResponseTransformer.class.getName(),
                    "(|" + "(" + Constants.SERVICE_SHORTNAME + "=" + transformerShortname + ")"
                            + "(" + Constants.SERVICE_ID + "=" + transformerShortname + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid transformer id: " + transformerShortname,
                    e);
        }

        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException(
                    "Transformer " + transformerShortname + " not found");
        } else {
            QueryResponseTransformer transformer = (QueryResponseTransformer) context
                    .getService(refs[0]);
            if (response != null) {
                return transformer.transform(response, arguments);
            } else {
                throw new IllegalArgumentException("QueryResponse is null.");
            }
        }
    }

    @Override
    public ResourceResponse getLocalResource(ResourceRequest resourceRequest)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getLocalResource";
        LOGGER.debug("ENTERING: " + methodName);
        ResourceResponse resourceResponse;
        if (fanoutEnabled) {
            LOGGER.debug("getLocalResource call received, fanning it out to all sites.");
            resourceResponse = getEnterpriseResource(resourceRequest);
        } else {
            resourceResponse = getResource(resourceRequest, false, getId());
        }
        LOGGER.debug("EXITING: " + methodName);
        return resourceResponse;
    }

    @Override
    public ResourceResponse getResource(ResourceRequest resourceRequest, String resourceSiteName)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getResource";
        LOGGER.debug("ENTERING: " + methodName);
        ResourceResponse resourceResponse;
        if (fanoutEnabled) {
            LOGGER.debug("getResource call received, fanning it out to all sites.");
            resourceResponse = getEnterpriseResource(resourceRequest);
        } else {
            resourceResponse = getResource(resourceRequest, false, resourceSiteName);
        }
        LOGGER.debug("EXITING: " + methodName);
        return resourceResponse;
    }

    @Override
    public ResourceResponse getEnterpriseResource(ResourceRequest resourceRequest)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getEnterpriseResource";
        LOGGER.debug("ENTERING: " + methodName);
        ResourceResponse resourceResponse = getResource(resourceRequest, true, null);
        LOGGER.debug("EXITING: " + methodName);
        return resourceResponse;
    }

    @Override
    public Set<String> getSourceIds() {
        Set<String> sources = new HashSet<>(federatedSources.size() + 1);
        sources.add(getId());
        if (!fanoutEnabled) {
            for (FederatedSource source : federatedSources) {
                sources.add(source.getId());
            }
        }
        return new TreeSet<>(sources);
    }

    @SuppressWarnings("javadoc")
    protected ResourceResponse getResource(ResourceRequest resourceRequest, boolean isEnterprise,
            String resourceSiteName)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getResource";
        LOGGER.entry(methodName);
        ResourceResponse resourceResponse = null;
        ResourceRequest resourceReq = resourceRequest;
        String resourceSourceName = resourceSiteName;

        if (fanoutEnabled) {
            isEnterprise = true;
        }

        if (resourceSourceName == null && !isEnterprise) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    "resourceSiteName cannot be null when obtaining resource.");
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }

        validateGetResourceRequest(resourceReq);
        try {

            for (PreResourcePlugin plugin : preResource) {
                try {
                    resourceReq = plugin.process(resourceReq);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }

            Map<String, Serializable> requestProperties = resourceReq.getProperties();
            LOGGER.debug("Attempting to get resource from siteName: {}", resourceSourceName);
            // At this point we pull out the properties and use them.
            Serializable sourceIdProperty = requestProperties.get(ResourceRequest.SOURCE_ID);
            if (sourceIdProperty != null) {
                resourceSourceName = sourceIdProperty.toString();
            }

            Serializable enterpriseProperty = requestProperties.get(ResourceRequest.IS_ENTERPRISE);
            if (enterpriseProperty != null) {
                if (Boolean.parseBoolean(enterpriseProperty.toString())) {
                    isEnterprise = true;
                }
            }

            // check if the resourceRequest has an ID only
            // If so, the metacard needs to be found and the Resource URI
            StringBuilder resolvedSourceIdHolder = new StringBuilder();

            ResourceInfo resourceInfo = getResourceInfo(resourceReq, resourceSourceName,
                    isEnterprise, resolvedSourceIdHolder, requestProperties);
            if (resourceInfo == null) {
                ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                        "Resource could not be found for the given attribute value: " + resourceReq
                                .getAttributeValue());
                LOGGER.throwing(resourceNotFoundException);
                throw resourceNotFoundException;
            }
            URI responseURI = resourceInfo.getResourceUri();
            Metacard metacard = resourceInfo.getMetacard();

            String resolvedSourceId = resolvedSourceIdHolder.toString();
            LOGGER.debug("resolvedSourceId = {}", resolvedSourceId);
            LOGGER.debug("ID = {}", getId());

            if (isEnterprise) {
                // since resolvedSourceId specifies what source the product
                // metacard resides on, we can just
                // change resourceSiteName to be that value, and then the
                // following if-else statements will
                // handle retrieving the product on the correct source
                resourceSourceName = resolvedSourceId;
            }

            String key;
            try {
                key = new CacheKey(metacard, resourceRequest).generateKey();
            } catch (Exception e1) {
                LOGGER.error("resource not found", e1);
                throw new ResourceNotFoundException(e1);
            }
            if (productCache != null && productCache.containsValid(key, metacard)) {
                try {
                    Resource resource = productCache.getValid(key, metacard);
                    resourceResponse = new ResourceResponseImpl(resourceRequest, requestProperties,
                            resource);
                    LOGGER.info("Successfully retrieved product from cache for metacard ID = {}",
                            metacard.getId());
                } catch (Exception ce) {
                    LOGGER.info(
                            "Unable to get resource from cache. Have to retrieve it from source {}",
                            resourceSourceName, ce);
                }
            }

            if (resourceResponse == null) {
                // retrieve product from specified federated site if not in cache
                if (!resourceSourceName.equals(getId())) {
                    LOGGER.debug("Searching federatedSource {} for resource.", resourceSourceName);
                    LOGGER.debug("metacard for product found on source: {}", resolvedSourceId);
                    FederatedSource source = null;

                    for (FederatedSource fedSource : federatedSources) {
                        if (resourceSourceName.equals(fedSource.getId())) {
                            LOGGER.debug("Adding federated site to federated query: {}",
                                    fedSource.getId());
                            source = fedSource;
                            break;
                        }
                    }

                    if (source != null) {
                        LOGGER.debug("Retrieving product from remote source {}", source.getId());
                        ResourceRetriever retriever = new RemoteResourceRetriever(source,
                                responseURI, requestProperties);
                        try {
                            resourceResponse = reliableResourceDownloadManager
                                    .download(resourceRequest, metacard, retriever);
                        } catch (DownloadException e) {
                            LOGGER.info("Unable to download resource", e);
                        }
                    } else {
                        LOGGER.warn("Could not find federatedSource: {}", resourceSourceName);
                    }
                } else {
                    LOGGER.debug("Retrieving product from local source {}", resourceSourceName);
                    ResourceRetriever retriever = new LocalResourceRetriever(resourceReaders,
                            responseURI, requestProperties);
                    try {
                        resourceResponse = reliableResourceDownloadManager
                                .download(resourceRequest, metacard, retriever);
                    } catch (DownloadException e) {
                        LOGGER.info("Unable to download resource", e);
                    }
                }
            }

            resourceResponse = validateFixGetResourceResponse(resourceResponse, resourceReq);

            for (PostResourcePlugin plugin : postResource) {
                try {
                    resourceResponse = plugin.process(resourceResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }

        } catch (RuntimeException e) {
            LOGGER.error("RuntimeException caused by: ", e);
            throw new ResourceNotFoundException("Unable to find resource");
        } catch (StopProcessingException e) {
            LOGGER.error("resource not supported", e);
            throw new ResourceNotSupportedException(FAILED_BY_GET_RESOURCE_PLUGIN + e.getMessage());
        }

        if (resourceResponse == null) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    "Resource could not be found for the given attribute value: " + resourceReq
                            .getAttributeValue());
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }

        LOGGER.exit(methodName);
        return resourceResponse;
    }

    /**
     * String representation of this {@code CatalogFrameworkImpl}.
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Retrieves a resource using the specified URI assuming this catalog framework has a
     * {@link ResourceReader} with a scheme that matches the scheme in the specified URI.
     *
     * @param resourceUri
     * @param properties
     * @return the {@link ResourceResponse}
     * @throws ResourceNotFoundException if a {@link ResourceReader} with the input URI's scheme is not found
     */
    protected ResourceResponse getResourceUsingResourceReader(URI resourceUri,
            Map<String, Serializable> properties) throws ResourceNotFoundException {
        final String methodName = "getResourceUsingResourceReader";
        LOGGER.entry(methodName);
        ResourceResponse resource = null;

        if (resourceUri == null) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    "Unable to find resource due to null URI");
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }

        for (ResourceReader reader : resourceReaders) {
            if (reader != null) {
                String scheme = resourceUri.getScheme();
                if (reader.getSupportedSchemes().contains(scheme)) {
                    try {
                        LOGGER.debug("Found an acceptable resource reader (" + reader.getId()
                                + ") for URI " + resourceUri.toASCIIString());
                        resource = reader.retrieveResource(resourceUri, properties);
                        if (resource != null) {
                            break;
                        } else {
                            LOGGER.info("Resource returned from ResourceReader " + reader.getId()
                                    + " was null.  Checking other readers for URI: " + resourceUri);
                        }
                    } catch (ResourceNotFoundException | IOException | ResourceNotSupportedException e) {
                        LOGGER.debug(
                                "Enterprise Search: Product not found using resource reader with name {}",
                                reader.getId(), e);
                    }
                }
            }
        }

        if (resource == null) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    "Resource Readers could not find resource (or returned null resource) for URI: "
                            + resourceUri.toASCIIString() + ". Scheme: " + resourceUri.getScheme());
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }
        LOGGER.debug("Received resource, sending back: " + resource.getResource().getName());
        LOGGER.exit(methodName);

        return resource;
    }

    /**
     * Retrieves a resource by URI.
     * <p/>
     * The {@link ResourceRequest} can specify either the product's URI or ID. If the product ID is
     * specified, then the matching {@link Metacard} must first be retrieved and the product URI
     * extracted from this {@link Metacard}.
     *
     * @param resourceRequest
     * @param site
     * @param isEnterprise
     * @param federatedSite
     * @param requestProperties
     * @return
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     */
    protected URI getResourceURI(ResourceRequest resourceRequest, String site, boolean isEnterprise,
            StringBuilder federatedSite, Map<String, Serializable> requestProperties)
            throws ResourceNotSupportedException, ResourceNotFoundException {

        String methodName = "getResourceURI";
        LOGGER.entry(methodName);

        URI resourceUri;
        String name = resourceRequest.getAttributeName();
        try {
            if (ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI.equals(name)) {
                // because this is a get resource by product uri, we already
                // have the product uri to return
                LOGGER.debug("get resource by product uri");
                Object value = resourceRequest.getAttributeValue();

                if (value instanceof URI) {
                    resourceUri = (URI) value;

                    Query propertyEqualToUriQuery = createPropertyIsEqualToQuery(
                            Metacard.RESOURCE_URI, resourceUri.toString());

                    // if isEnterprise, go out and obtain the actual source
                    // where the product's metacard is stored.
                    QueryRequest queryRequest = new QueryRequestImpl(propertyEqualToUriQuery,
                            isEnterprise,
                            Collections.singletonList(site == null ? this.getId() : site),
                            resourceRequest.getProperties());

                    QueryResponse queryResponse = query(queryRequest);
                    if (queryResponse.getResults().size() > 0) {
                        Metacard result = queryResponse.getResults().get(0).getMetacard();
                        federatedSite.append(result.getSourceId());
                        LOGGER.debug(
                                "Trying to lookup resource URI " + resourceUri + " for metacardId: "
                                        + resourceUri);

                        if (!requestProperties.containsKey(Metacard.ID)) {
                            requestProperties.put(Metacard.ID, result.getId());
                        }
                        if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
                            requestProperties.put(Metacard.RESOURCE_URI, result.getResourceURI());
                        }
                    } else {
                        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                                "Could not resolve source id for URI by doing a URI based query: "
                                        + resourceUri);
                        LOGGER.error("could not resolve source id for URI",
                                resourceNotFoundException);
                        throw resourceNotFoundException;
                    }
                } else {
                    ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                            "The GetResourceRequest with attribute value of class '" + value
                                    .getClass() + "' is not supported by this instance"
                                    + " of the CatalogFramework.");
                    LOGGER.throwing(resourceNotSupportedException);
                    throw resourceNotSupportedException;
                }
            } else if (ResourceRequest.GET_RESOURCE_BY_ID.equals(name)) {
                // since this is a get resource by id, we need to obtain the
                // product URI
                LOGGER.debug("get resource by id");
                Object value = resourceRequest.getAttributeValue();
                if (value instanceof String) {
                    String metacardId = (String) value;
                    LOGGER.debug("metacardId = " + metacardId + ",   site = " + site);
                    QueryRequest queryRequest = new QueryRequestImpl(
                            createMetacardIdQuery(metacardId), isEnterprise,
                            Collections.singletonList(site == null ? this.getId() : site),
                            resourceRequest.getProperties());

                    QueryResponse queryResponse = query(queryRequest);
                    if (queryResponse.getResults().size() > 0) {
                        Metacard result = queryResponse.getResults().get(0).getMetacard();
                        resourceUri = result.getResourceURI();
                        federatedSite.append(result.getSourceId());
                        LOGGER.debug(
                                "Trying to lookup resource URI " + resourceUri + " for metacardId: "
                                        + metacardId);
                    } else {
                        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                                "Could not resolve source id for URI by doing an id based query: "
                                        + metacardId);
                        LOGGER.throwing(resourceNotFoundException);
                        throw resourceNotFoundException;
                    }

                    if (!requestProperties.containsKey(Metacard.ID)) {
                        requestProperties.put(Metacard.ID, metacardId);
                    }
                    if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
                        requestProperties.put(Metacard.RESOURCE_URI, resourceUri);
                    }
                } else {
                    ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                            "The GetResourceRequest with attribute value of class '" + value
                                    .getClass() + "' is not supported by this instance"
                                    + " of the CatalogFramework.");
                    LOGGER.throwing(resourceNotSupportedException);
                    throw resourceNotSupportedException;
                }
            } else {
                ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                        "The GetResourceRequest with attribute name '" + name
                                + "' is not supported by this instance"
                                + " of the CatalogFramework.");
                LOGGER.throwing(resourceNotSupportedException);
                throw resourceNotSupportedException;
            }
        } catch (UnsupportedQueryException | FederationException e) {

            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    DEFAULT_RESOURCE_NOT_FOUND_MESSAGE, e);
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }

        LOGGER.debug("Returning resourceURI: " + resourceUri);
        LOGGER.exit(methodName);
        if (resourceUri == null) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    DEFAULT_RESOURCE_NOT_FOUND_MESSAGE);
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }
        return resourceUri;
    }

    /**
     * Retrieves a resource by URI.
     * <p/>
     * The {@link ResourceRequest} can specify either the product's URI or ID. If the product ID is
     * specified, then the matching {@link Metacard} must first be retrieved and the product URI
     * extracted from this {@link Metacard}.
     *
     * @param resourceRequest
     * @param site
     * @param isEnterprise
     * @param federatedSite
     * @param requestProperties
     * @return
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     */
    protected ResourceInfo getResourceInfo(ResourceRequest resourceRequest, String site,
            boolean isEnterprise, StringBuilder federatedSite,
            Map<String, Serializable> requestProperties)
            throws ResourceNotSupportedException, ResourceNotFoundException {

        String methodName = "getResourceInfo";
        LOGGER.entry(methodName);

        Metacard metacard;
        URI resourceUri;
        String name = resourceRequest.getAttributeName();
        try {
            if (ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI.equals(name)) {
                // because this is a get resource by product uri, we already
                // have the product uri to return
                LOGGER.debug("get resource by product uri");
                Object value = resourceRequest.getAttributeValue();

                if (value instanceof URI) {
                    resourceUri = (URI) value;

                    Query propertyEqualToUriQuery = createPropertyIsEqualToQuery(
                            Metacard.RESOURCE_URI, resourceUri.toString());

                    // if isEnterprise, go out and obtain the actual source
                    // where the product's metacard is stored.
                    QueryRequest queryRequest = new QueryRequestImpl(propertyEqualToUriQuery,
                            isEnterprise,
                            Collections.singletonList(site == null ? this.getId() : site),
                            resourceRequest.getProperties());

                    QueryResponse queryResponse = query(queryRequest, null, true);
                    if (queryResponse.getResults().size() > 0) {
                        metacard = queryResponse.getResults().get(0).getMetacard();
                        federatedSite.append(metacard.getSourceId());
                        LOGGER.debug(
                                "Trying to lookup resource URI " + resourceUri + " for metacardId: "
                                        + resourceUri);

                        if (!requestProperties.containsKey(Metacard.ID)) {
                            requestProperties.put(Metacard.ID, metacard.getId());
                        }
                        if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
                            requestProperties.put(Metacard.RESOURCE_URI, metacard.getResourceURI());
                        }
                    } else {
                        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                                "Could not resolve source id for URI by doing a URI based query: "
                                        + resourceUri);
                        LOGGER.throwing(resourceNotFoundException);
                        throw resourceNotFoundException;
                    }
                } else {
                    ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                            "The GetResourceRequest with attribute value of class '" + value
                                    .getClass() + "' is not supported by this instance"
                                    + " of the CatalogFramework.");
                    LOGGER.throwing(resourceNotSupportedException);
                    throw resourceNotSupportedException;
                }
            } else if (ResourceRequest.GET_RESOURCE_BY_ID.equals(name)) {
                // since this is a get resource by id, we need to obtain the
                // product URI
                LOGGER.debug("get resource by id");
                Object value = resourceRequest.getAttributeValue();
                if (value instanceof String) {
                    String metacardId = (String) value;
                    LOGGER.debug("metacardId = " + metacardId + ",   site = " + site);
                    QueryRequest queryRequest = new QueryRequestImpl(
                            createMetacardIdQuery(metacardId), isEnterprise,
                            Collections.singletonList(site == null ? this.getId() : site),
                            resourceRequest.getProperties());

                    QueryResponse queryResponse = query(queryRequest, null, true);
                    if (queryResponse.getResults().size() > 0) {
                        metacard = queryResponse.getResults().get(0).getMetacard();
                        resourceUri = metacard.getResourceURI();
                        federatedSite.append(metacard.getSourceId());
                        LOGGER.debug(
                                "Trying to lookup resource URI " + resourceUri + " for metacardId: "
                                        + metacardId);
                    } else {
                        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                                "Could not resolve source id for URI by doing an id based query: "
                                        + metacardId);
                        LOGGER.throwing(resourceNotFoundException);
                        throw resourceNotFoundException;
                    }

                    if (!requestProperties.containsKey(Metacard.ID)) {
                        requestProperties.put(Metacard.ID, metacardId);
                    }
                    if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
                        requestProperties.put(Metacard.RESOURCE_URI, resourceUri);
                    }
                } else {
                    ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                            "The GetResourceRequest with attribute value of class '" + value
                                    .getClass() + "' is not supported by this instance"
                                    + " of the CatalogFramework.");
                    LOGGER.throwing(resourceNotSupportedException);
                    throw resourceNotSupportedException;
                }
            } else {
                ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                        "The GetResourceRequest with attribute name '" + name
                                + "' is not supported by this instance"
                                + " of the CatalogFramework.");
                LOGGER.throwing(resourceNotSupportedException);
                throw resourceNotSupportedException;
            }
        } catch (UnsupportedQueryException | FederationException e) {

            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    DEFAULT_RESOURCE_NOT_FOUND_MESSAGE, e);
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }

        LOGGER.debug("Returning resourceURI: " + resourceUri);
        LOGGER.exit(methodName);
        if (resourceUri == null) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    DEFAULT_RESOURCE_NOT_FOUND_MESSAGE);
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }

        return new ResourceInfo(metacard, resourceUri);
    }

    protected Query createMetacardIdQuery(String metacardId) {
        return createPropertyIsEqualToQuery(Metacard.ID, metacardId);
    }

    protected Query createPropertyIsEqualToQuery(String propertyName, String literal) {
        return new QueryImpl(new PropertyIsEqualToLiteral(new PropertyNameImpl(propertyName),
                new LiteralImpl(literal)));
    }

    /**
     * Checks that the specified source is valid and available.
     *
     * @param source the {@link Source} to check availability of
     * @return true if the {@link Source} is available, false otherwise
     */
    protected boolean sourceIsAvailable(Source source) {
        if (source == null) {
            LOGGER.warn("source is null, therefore not available");
            return false;
        }
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking if source \"" + source.getId() + "\" is available...");
            }

            // source is considered available unless we have checked and seen otherwise
            boolean available = true;
            Source cachedSource = poller.getCachedSource(source);
            if (cachedSource != null) {
                available = cachedSource.isAvailable();
            }

            if (!available) {
                LOGGER.warn("source \"" + source.getId() + "\" is not available");
            }
            return available;
        } catch (ServiceUnavailableException e) {
            LOGGER.warn("Caught ServiceUnavaiableException", e);
            return false;
        } catch (Exception e) {
            LOGGER.warn("Caught Exception", e);
            return false;
        }
    }

    /**
     * Validates that the {@link CreateResponse} has one or more {@link Metacard}s in it that were
     * created in the catalog, and that the original {@link CreateRequest} is included in the
     * response.
     *
     * @param createResponse the original {@link CreateResponse} returned from the catalog provider
     * @param createRequest  the original {@link CreateRequest} sent to the catalog provider
     * @return the updated {@link CreateResponse}
     * @throws IngestException if original {@link CreateResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    protected CreateResponse validateFixCreateResponse(CreateResponse createResponse,
            CreateRequest createRequest) throws IngestException {
        if (createResponse != null) {
            if (createResponse.getCreatedMetacards() == null) {
                IngestException ingestException = new IngestException(
                        "CatalogProvider returned null list of results from create method.");
                LOGGER.throwing(ingestException);
                throw ingestException;
            }
            if (createResponse.getRequest() == null) {
                createResponse = new CreateResponseImpl(createRequest,
                        createResponse.getProperties(), createResponse.getCreatedMetacards());
            }
        } else {
            IngestException ingestException = new IngestException(
                    "CatalogProvider returned null CreateResponse Object.");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
        return createResponse;
    }

    /**
     * Validates that the {@link UpdateResponse} has one or more {@link Metacard}s in it that were
     * updated in the catalog, and that the original {@link UpdateRequest} is included in the
     * response.
     *
     * @param updateResponse the original {@link UpdateResponse} returned from the catalog provider
     * @param updateRequest  the original {@link UpdateRequest} sent to the catalog provider
     * @return the updated {@link UpdateResponse}
     * @throws IngestException if original {@link UpdateResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    protected UpdateResponse validateFixUpdateResponse(UpdateResponse updateResponse,
            UpdateRequest updateRequest) throws IngestException {
        UpdateResponse updateResp = updateResponse;
        if (updateResp != null) {
            if (updateResp.getUpdatedMetacards() == null) {
                IngestException ingestException = new IngestException(
                        "CatalogProvider returned null list of results from update method.");
                LOGGER.throwing(ingestException);
                throw ingestException;
            }
            if (updateResp.getRequest() == null) {
                updateResp = new UpdateResponseImpl(updateRequest, updateResponse.getProperties(),
                        updateResponse.getUpdatedMetacards());
            }
        } else {
            IngestException ingestException = new IngestException(
                    "CatalogProvider returned null UpdateResponse Object.");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
        return updateResp;
    }

    /**
     * Validates that the {@link DeleteResponse} has one or more {@link Metacard}s in it that were
     * deleted in the catalog, and that the original {@link DeleteRequest} is included in the
     * response.
     *
     * @param deleteResponse the original {@link DeleteResponse} returned from the catalog provider
     * @param deleteRequest  the original {@link DeleteRequest} sent to the catalog provider
     * @return the updated {@link DeleteResponse}
     * @throws IngestException if original {@link DeleteResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    protected DeleteResponse validateFixDeleteResponse(DeleteResponse deleteResponse,
            DeleteRequest deleteRequest) throws IngestException {
        DeleteResponse delResponse = deleteResponse;
        if (delResponse != null) {
            if (delResponse.getDeletedMetacards() == null) {
                IngestException ingestException = new IngestException(
                        "CatalogProvider returned null list of results from delete method.");
                LOGGER.throwing(ingestException);
                throw ingestException;
            }
            if (delResponse.getRequest() == null) {
                delResponse = new DeleteResponseImpl(deleteRequest, delResponse.getProperties(),
                        delResponse.getDeletedMetacards());
            }
        } else {
            IngestException ingestException = new IngestException(
                    "CatalogProvider returned null DeleteResponse Object.");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
        return delResponse;
    }

    /**
     * Validates that the {@link ResourceResponse} has a {@link Resource} in it that was retrieved,
     * and that the original {@link ResourceRequest} is included in the response.
     *
     * @param getResourceResponse the original {@link ResourceResponse} returned from the source
     * @param getResourceRequest  the original {@link ResourceRequest} sent to the source
     * @return the updated {@link ResourceResponse}
     * @throws ResourceNotFoundException if the original {@link ResourceResponse} is null or the resource could not be
     *                                   found
     */
    protected ResourceResponse validateFixGetResourceResponse(ResourceResponse getResourceResponse,
            ResourceRequest getResourceRequest) throws ResourceNotFoundException {
        ResourceResponse resourceResponse = getResourceResponse;
        if (getResourceResponse != null) {
            if (getResourceResponse.getResource() == null) {
                ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                        "Resource was returned as null, meaning it could not be found.");
                LOGGER.throwing(resourceNotFoundException);
                throw resourceNotFoundException;
            }
            if (getResourceResponse.getRequest() == null) {
                resourceResponse = new ResourceResponseImpl(getResourceRequest,
                        getResourceResponse.getProperties(), getResourceResponse.getResource());
            }
        } else {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    "CatalogProvider returned null ResourceResponse Object.");
            LOGGER.throwing(resourceNotFoundException);
            throw resourceNotFoundException;
        }
        return resourceResponse;
    }

    /**
     * Validates that the {@link QueryResponse} has a non-null list of {@link Result}s in it, and
     * that the original {@link QueryRequest} is included in the response.
     *
     * @param sourceResponse       the original {@link ddf.catalog.operation.SourceResponse} returned from the source
     * @param queryRequest         the original {@link ddf.catalog.operation.QueryRequest} sent to the source
     * @param overrideFanoutRename
     * @return the updated {@link QueryResponse}
     * @throws UnsupportedQueryException if the original {@link QueryResponse} is null or the results list is null
     */
    protected SourceResponse validateFixQueryResponse(SourceResponse sourceResponse,
            QueryRequest queryRequest, boolean overrideFanoutRename)
            throws UnsupportedQueryException {
        SourceResponse sourceResp = sourceResponse;
        if (fanoutEnabled && !overrideFanoutRename) {
            sourceResp = replaceSourceId((QueryResponse) sourceResponse);
        }
        if (sourceResp != null) {
            if (sourceResp.getResults() == null) {
                UnsupportedQueryException unsupportedQueryException = new UnsupportedQueryException(
                        "CatalogProvider returned null list of results from query method.");
                LOGGER.throwing(unsupportedQueryException);
                throw unsupportedQueryException;
            }
            if (sourceResp.getRequest() == null) {
                sourceResp = new SourceResponseImpl(queryRequest, sourceResp.getProperties(),
                        sourceResp.getResults());
            }
        } else {
            UnsupportedQueryException unsupportedQueryException = new UnsupportedQueryException(
                    "CatalogProvider returned null QueryResponse Object.");
            LOGGER.throwing(unsupportedQueryException);
            throw unsupportedQueryException;
        }
        return sourceResp;
    }

    /**
     * Replaces the site name(s) of {@link FederatedSource}s in the {@link QueryResponse} with the
     * fanout's site name to keep info about the {@link FederatedSource}s hidden from the external
     * client.
     *
     * @param queryResponse the original {@link QueryResponse} from the query request
     * @return the updated {@link QueryResponse} with all site names replaced with fanout's site
     * name
     */
    protected QueryResponse replaceSourceId(QueryResponse queryResponse) {
        LOGGER.debug("ENTERING: replaceSourceId()");
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
        LOGGER.debug("EXITING: replaceSourceId()");
        return newResponse;
    }

    /**
     * Validates that the {@link CreateRequest} is non-null and has a non-empty list of
     * {@link Metacard}s in it.
     *
     * @param createRequest the {@link CreateRequest}
     * @throws IngestException if the {@link CreateRequest} is null, or request has a null or empty list of
     *                         {@link Metacard}s
     */
    protected void validateCreateRequest(CreateRequest createRequest) throws IngestException {
        if (createRequest == null) {
            IngestException ingestException = new IngestException(
                    "CreateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
        List<Metacard> entries = createRequest.getMetacards();
        if (entries == null || entries.size() == 0) {
            IngestException ingestException = new IngestException(
                    "Cannot perform ingest with null/empty entry list, either passed in from endpoint, or as output from PreIngestPlugins");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
    }

    /**
     * Validates that the {@link UpdateRequest} is non-null, has a non-empty list of
     * {@link Metacard}s in it, and a non-null attribute name (which specifies if the update is
     * being done by product URI or ID).
     *
     * @param updateRequest the {@link UpdateRequest}
     * @throws IngestException if the {@link UpdateRequest} is null, or has null or empty {@link Metacard} list,
     *                         or a null attribute name.
     */
    protected void validateUpdateRequest(UpdateRequest updateRequest) throws IngestException {
        if (updateRequest == null) {
            IngestException ingestException = new IngestException(
                    "UpdateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
        List<Entry<Serializable, Metacard>> entries = updateRequest.getUpdates();
        if (entries == null || entries.size() == 0 || updateRequest.getAttributeName() == null) {
            IngestException ingestException = new IngestException(
                    "Cannot perform update with null/empty attribute value list or null attributeName, either passed in from endpoint, or as output from PreIngestPlugins");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
    }

    /**
     * Validates that the {@link DeleteRequest} is non-null, has a non-empty list of
     * {@link Metacard}s in it, and a non-null attribute name (which specifies if the delete is
     * being done by product URI or ID).
     *
     * @param deleteRequest the {@link DeleteRequest}
     * @throws IngestException if the {@link DeleteRequest} is null, or has null or empty {@link Metacard} list,
     *                         or a null attribute name
     */
    protected void validateDeleteRequest(DeleteRequest deleteRequest) throws IngestException {
        if (deleteRequest == null) {
            IngestException ingestException = new IngestException(
                    "DeleteRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
        List<?> entries = deleteRequest.getAttributeValues();
        if (entries == null || entries.size() == 0 || deleteRequest.getAttributeName() == null) {
            IngestException ingestException = new IngestException(
                    "Cannot perform delete with null/empty attribute value list or null attributeName, either passed in from endpoint, or as output from PreIngestPlugins");
            LOGGER.throwing(ingestException);
            throw ingestException;
        }
    }

    /**
     * Validates that the {@link ResourceRequest} is non-null, a non-null attribute name (which
     * specifies if the retrieval is being done by product URI or ID), and a non-null attribute
     * value.
     *
     * @param getResourceRequest the {@link ResourceRequest}
     * @throws ResourceNotSupportedException if the {@link ResourceRequest} is null, or has a null attribute value or name
     */
    protected void validateGetResourceRequest(ResourceRequest getResourceRequest)
            throws ResourceNotSupportedException {
        if (getResourceRequest == null) {
            ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                    "GetResourceRequest was null, either passed in from endpoint, or as output from PreResourcePlugin");
            LOGGER.throwing(resourceNotSupportedException);
            throw resourceNotSupportedException;
        }
        Object value = getResourceRequest.getAttributeValue();
        if (value == null || getResourceRequest.getAttributeName() == null) {
            ResourceNotSupportedException resourceNotSupportedException = new ResourceNotSupportedException(
                    "Cannot perform getResource with null attribute value or null attributeName, either passed in from endpoint, or as output from PreResourcePlugin");
            LOGGER.throwing(resourceNotSupportedException);
            throw resourceNotSupportedException;
        }
    }

    /**
     * Validates that the {@link QueryRequest} is non-null and that the query in it is non-null.
     *
     * @param queryRequest the {@link QueryRequest}
     * @throws UnsupportedQueryException if the {@link QueryRequest} is null or the query in it is null
     */
    protected void validateQueryRequest(QueryRequest queryRequest)
            throws UnsupportedQueryException {
        if (queryRequest == null) {
            UnsupportedQueryException unsupportedQueryException = new UnsupportedQueryException(
                    "QueryRequest was null, either passed in from endpoint, or as output from a PreQuery Plugin");
            LOGGER.throwing(unsupportedQueryException);
            throw unsupportedQueryException;
        }

        if (queryRequest.getQuery() == null) {
            UnsupportedQueryException unsupportedQueryException = new UnsupportedQueryException(
                    "Cannot perform query with null query, either passed in from endpoint, or as output from a PreQuery Plugin");
            LOGGER.throwing(unsupportedQueryException);
            throw unsupportedQueryException;
        }

        if (fanoutEnabled) {
            Set<String> sources = queryRequest.getSourceIds();
            if (sources != null) {
                for (String querySourceId : sources) {
                    LOGGER.debug("validating requested sourceId {}", querySourceId);
                    if (!querySourceId.equals(this.getId())) {
                        UnsupportedQueryException unsupportedQueryException = new UnsupportedQueryException(
                                "Unknown source: " + querySourceId);
                        LOGGER.debug(
                                "Throwing unsupportedQueryException due to unknown sourceId: {}",
                                querySourceId, unsupportedQueryException);
                        throw unsupportedQueryException;
                    }
                }
            }
        }
    }

    /**
     * Helper method to build ingest log strings
     */
    private String buildIngestLog(CreateRequest createReq) {
        StringBuilder strBuilder = new StringBuilder();
        List<Metacard> metacards = createReq.getMetacards();
        final String newLine = System.getProperty("line.separator");

        for (int i = 0; i < metacards.size(); i++) {
            Metacard card = metacards.get(i);
            strBuilder.append(newLine).append("Batch #: ").append(i + 1).append(" | ");
            if (card != null) {
                if (card.getTitle() != null) {
                    strBuilder.append("Metacard Title: ").append(card.getTitle()).append(" | ");
                }
                if (card.getId() != null) {
                    strBuilder.append("Metacard ID: ").append(card.getId()).append(" | ");
                }
            } else {
                strBuilder.append("Null Metacard");
            }
        }
        return strBuilder.toString();
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getLocalResourceOptions(String metacardId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getLocalResourceOptions");

        Map<String, Set<String>> optionsMap;
        try {
            QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                    false, Collections.singletonList(getId()), null);
            QueryResponse queryResponse = query(queryRequest);
            List<Result> results = queryResponse.getResults();

            if (results.size() > 0) {
                Metacard metacard = results.get(0).getMetacard();
                optionsMap = Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT,
                        getOptionsFromLocalProvider(metacard));
            } else {

                String message = "Could not find metacard " + metacardId + " on local source";
                ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                        message);
                LOGGER.throwing(XLogger.Level.DEBUG, resourceNotFoundException);
                LOGGER.trace("EXITING: getLocalResourceOptions");
                throw resourceNotFoundException;
            }
        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error finding metacard " + metacardId, e);
            LOGGER.trace("EXITING: getLocalResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query",
                    e);
        } catch (FederationException e) {
            LOGGER.warn("Error federating query for metacard " + metacardId, e);
            LOGGER.trace("EXITING: getLocalResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Federation issue",
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Metacard couldn't be found " + metacardId, e);
            LOGGER.trace("EXITING: getLocalResourceOptions");
            throw new ResourceNotFoundException("Query returned null metacard", e);
        }

        LOGGER.trace("EXITING: getLocalResourceOptions");

        return optionsMap;
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getEnterpriseResourceOptions");
        Set<String> supportedOptions = Collections.emptySet();

        try {
            QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                    true, null, null);
            QueryResponse queryResponse = query(queryRequest);
            List<Result> results = queryResponse.getResults();

            if (results.size() > 0) {
                Metacard metacard = results.get(0).getMetacard();
                String sourceIdOfResult = metacard.getSourceId();

                if (sourceIdOfResult != null && sourceIdOfResult.equals(getId())) {
                    // found entry on local source
                    supportedOptions = getOptionsFromLocalProvider(metacard);
                } else if (sourceIdOfResult != null && !sourceIdOfResult.equals(getId())) {
                    // found entry on federated source
                    supportedOptions = getOptionsFromFederatedSource(metacard, sourceIdOfResult);
                }
            } else {
                String message = "Unable to find metacard " + metacardId + " on enterprise.";
                LOGGER.debug(message);
                LOGGER.trace("EXITING: getEnterpriseResourceOptions");
                throw new ResourceNotFoundException(message);
            }

        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error finding metacard " + metacardId, e);
            LOGGER.trace("EXITING: getEnterpriseResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query",
                    e);
        } catch (FederationException e) {
            LOGGER.warn("Error federating query for metacard " + metacardId, e);
            LOGGER.trace("EXITING: getEnterpriseResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Federation issue",
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Metacard couldn't be found " + metacardId, e);
            LOGGER.trace("EXITING: getEnterpriseResourceOptions");
            throw new ResourceNotFoundException("Query returned null metacard", e);
        }

        LOGGER.trace("EXITING: getEnterpriseResourceOptions");
        return Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT, supportedOptions);
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getResourceOptions");
        Map<String, Set<String>> optionsMap;
        try {
            LOGGER.debug("source id to get options from: " + sourceId);
            QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                    false, Collections.singletonList(sourceId == null ? this.getId() : sourceId),
                    null);
            QueryResponse queryResponse = query(queryRequest);
            List<Result> results = queryResponse.getResults();

            if (results.size() > 0) {
                Metacard metacard = results.get(0).getMetacard();
                // DDF-1763: Check if the source ID passed in is null, empty,
                // or the local provider.
                if (StringUtils.isEmpty(sourceId) || sourceId.equals(getId())) {
                    optionsMap = Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT,
                            getOptionsFromLocalProvider(metacard));
                } else {
                    optionsMap = Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT,
                            getOptionsFromFederatedSource(metacard, sourceId));
                }
            } else {

                String message = "Could not find metacard " + metacardId + " on source " + sourceId;
                ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                        message);
                LOGGER.throwing(XLogger.Level.DEBUG, resourceNotFoundException);
                LOGGER.trace("EXITING: getResourceOptions");
                throw resourceNotFoundException;
            }
        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error finding metacard " + metacardId, e);
            LOGGER.trace("EXITING: getResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query",
                    e);
        } catch (FederationException e) {
            LOGGER.warn("Error federating query for metacard " + metacardId, e);
            LOGGER.trace("EXITING: getResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Federation issue",
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Metacard couldn't be found " + metacardId, e);
            LOGGER.trace("EXITING: getResourceOptions");
            throw new ResourceNotFoundException("Query returned null metacard", e);
        }

        LOGGER.trace("EXITING: getResourceOptions");
        return optionsMap;
    }

    /**
     * Get the supported options from the {@link ResourceReader} that matches the scheme in the
     * specified {@link Metacard}'s URI. Only look in the local provider for the specified
     * {@link Metacard}.
     *
     * @param metacard the {@link Metacard} to get the supported options for
     * @return the {@link Set} of supported options for the metacard
     */
    @Deprecated
    private Set<String> getOptionsFromLocalProvider(Metacard metacard) {
        LOGGER.trace("ENTERING: getOptionsFromLocalProvider");
        Set<String> supportedOptions = Collections.emptySet();
        URI resourceUri = metacard.getResourceURI();
        for (ResourceReader reader : resourceReaders) {
            LOGGER.debug("reader id: " + reader.getId());
            Set<String> rrSupportedSchemes = reader.getSupportedSchemes();
            String metacardScheme = resourceUri.getScheme();
            if (metacardScheme != null && rrSupportedSchemes.contains(metacardScheme)) {
                supportedOptions = reader.getOptions(metacard);
            }
        }

        LOGGER.trace("EXITING: getOptionsFromLocalProvider");
        return supportedOptions;
    }

    /**
     * Get the supported options from the {@link ResourceReader} that matches the scheme in the
     * specified {@link Metacard}'s URI. Only look in the specified source for the {@link Metacard}.
     *
     * @param metacard the {@link Metacard} to get the supported options for
     * @param sourceId the ID of the federated source to look for the {@link Metacard}
     * @return the {@link Set} of supported options for the metacard
     * @throws ResourceNotFoundException if the {@link Source} cannot be found for the source ID
     */
    @Deprecated
    private Set<String> getOptionsFromFederatedSource(Metacard metacard, String sourceId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getOptionsFromFederatedSource");

        FederatedSource source = null;
        for (FederatedSource fedSource : federatedSources) {
            if (sourceId.equals(fedSource.getId())) {
                source = fedSource;
                break;
            }
        }

        if (source != null) {
            LOGGER.trace("EXITING: getOptionsFromFederatedSource");

            return source.getOptions(metacard);
        } else {
            String message = "Unable to find source corresponding to given site name: " + sourceId;
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                    message);
            LOGGER.throwing(XLogger.Level.DEBUG, resourceNotFoundException);
            LOGGER.trace("EXITING: getOptionsFromFederatedSource");

            throw resourceNotFoundException;
        }
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> properties) {
        String methodName = "configurationUpdateCallback";
        LOGGER.debug("ENTERING: " + methodName);

        if (properties != null && !properties.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(properties.toString());
            }

            String ddfSiteName = properties.get(ConfigurationManager.SITE_NAME);
            if (StringUtils.isNotBlank(ddfSiteName)) {
                LOGGER.debug("ddfSiteName = " + ddfSiteName);
                this.setId(ddfSiteName);
            }

            String ddfVersion = properties.get(ConfigurationManager.VERSION);
            if (StringUtils.isNotBlank(ddfVersion)) {
                LOGGER.debug("ddfVersion = " + ddfVersion);
                this.setVersion(ddfVersion);
            }

            String ddfOrganization = properties.get(ConfigurationManager.ORGANIZATION);
            if (StringUtils.isNotBlank(ddfOrganization)) {
                LOGGER.debug("ddfOrganization = " + ddfOrganization);
                this.setOrganization(ddfOrganization);
            }
        } else {
            LOGGER.debug("properties are NULL or empty");
        }

        LOGGER.debug("EXITING: " + methodName);
    }

    protected static class ResourceInfo {
        private Metacard metacard;

        private URI resourceUri;

        public ResourceInfo(Metacard metacard, URI uri) {
            this.metacard = metacard;
            this.resourceUri = uri;
        }

        public Metacard getMetacard() {
            return metacard;
        }

        public URI getResourceUri() {
            return resourceUri;
        }
    }

}
