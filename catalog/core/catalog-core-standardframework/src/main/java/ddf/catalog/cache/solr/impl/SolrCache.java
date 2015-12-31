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
package ddf.catalog.cache.solr.impl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.solr.factory.SolrServerFactory;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SchemaFields;
import ddf.catalog.source.solr.SolrFilterDelegate;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrMetacardClient;

/**
 * Catalog cache implementation using Apache Solr 4
 */
public class SolrCache implements SolrCacheMBean {

    public static final String METACARD_CACHE_CORE_NAME = "metacard_cache";

    public static final String METACARD_SOURCE_NAME = "metacard_source" + SchemaFields.TEXT_SUFFIX;

    public static final String METACARD_ID_NAME = "original_id" + SchemaFields.TEXT_SUFFIX;

    // the unique id field used in the platform solr standalone server
    public static final String METACARD_UNIQUE_ID_NAME = "id" + SchemaFields.TEXT_SUFFIX;

    public static final String CACHED_DATE = "cached" + SchemaFields.DATE_SUFFIX;

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCache.class);

    private FilterAdapter filterAdapter;

    private ObjectName objectName;

    private String url = SolrServerFactory.getDefaultHttpsAddress();

    private SolrServer server;

    private SolrFilterDelegateFactory solrFilterDelegateFactory;

    private SolrMetacardClient client;

    private AtomicBoolean dirty = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;

    private long expirationIntervalInMinutes = 10;

    private long expirationAgeInMinutes = TimeUnit.DAYS.toMinutes(7);

    private MBeanServer mbeanServer;

    private SystemBaseUrl systemBaseUrl;

    /**
     * Convenience constructor that creates a the Solr server
     *
     * @param adapter injected implementation of FilterAdapter
     */
    public SolrCache(FilterAdapter adapter, SolrFilterDelegateFactory solrFilterDelegateFactory,
            SystemBaseUrl sbu) {
        this.filterAdapter = adapter;
        this.solrFilterDelegateFactory = solrFilterDelegateFactory;
        this.systemBaseUrl = sbu;
        this.url = systemBaseUrl.constructUrl("/solr");
        this.updateServer(this.url);
        configureCacheExpirationScheduler();

        try {
            objectName = new ObjectName(SolrCacheMBean.OBJECTNAME);
        } catch (MalformedObjectNameException e) {
            LOGGER.info("Could not create object name", e);
        }

        configureMBean();
    }

    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        return client.query(request);
    }

    public void create(Collection<Metacard> metacards) {
        if (metacards == null || metacards.size() == 0) {
            return;
        }

        List<Metacard> updatedMetacards = new ArrayList<>();
        for (Metacard metacard : metacards) {
            if (metacard != null) {
                if (StringUtils.isNotBlank(metacard.getSourceId()) && StringUtils.isNotBlank(
                        metacard.getId())) {
                    updatedMetacards.add(metacard);
                }
            } else {
                LOGGER.debug("metacard in result was null");
            }
        }

        try {
            client.add(updatedMetacards, false);
            dirty.set(true);
        } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
            LOGGER.warn("Solr server exception caching metacard(s)", e);
        }
    }

    public void delete(DeleteRequest deleteRequest) {
        if (deleteRequest == null) {
            return;
        }

        String attributeName = deleteRequest.getAttributeName();
        if (StringUtils.isBlank(attributeName)) {
            LOGGER.warn("Attribute name cannot be empty. Cannot delete from cache.");
            return;
        }

        String fieldName = attributeName + SchemaFields.TEXT_SUFFIX;
        if (fieldName.equals(METACARD_UNIQUE_ID_NAME)) {
            fieldName = METACARD_ID_NAME;
        }

        try {
            client.deleteByIds(fieldName, deleteRequest.getAttributeValues(), false);
            dirty.set(true);
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Solr server exception while deleting from cache", e);
        }
    }

    public void setExpirationIntervalInMinutes(long expirationInterval) {
        this.expirationIntervalInMinutes = expirationInterval;
        configureCacheExpirationScheduler();
    }

    public void setExpirationAgeInMinutes(long expirationAgeInMinutes) {
        this.expirationAgeInMinutes = expirationAgeInMinutes;
    }

    private void configureCacheExpirationScheduler() {
        shutdownCacheExpirationScheduler();
        LOGGER.info(
                "Configuring cache expiration scheduler with an expiration interval of {} minute(s).",
                expirationIntervalInMinutes);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new ExpirationRunner(),
                0,
                expirationIntervalInMinutes,
                TimeUnit.MINUTES);
    }

    private void configureMBean() {
        LOGGER.info("Registering Cache Manager Service MBean");
        mbeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            try {

                mbeanServer.registerMBean(new StandardMBean(this, SolrCacheMBean.class),
                        objectName);
            } catch (InstanceAlreadyExistsException e) {
                LOGGER.info("Re-registering Cache Manager MBean");
                mbeanServer.unregisterMBean(objectName);
                mbeanServer.registerMBean(new StandardMBean(this, SolrCacheMBean.class),
                        objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not register MBean.", e);
        }
    }

    private void shutdownCacheExpirationScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            LOGGER.debug("Shutting down cache expiration scheduler.");
            scheduler.shutdown();
            try {
                // Wait up to 60 seconds existing tasks to terminate
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    // Wait up to 60 seconds for tasks to respond to being cancelled
                    if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                        LOGGER.warn("Cache expiration scheduler did not terminate.");
                    }
                }
            } catch (InterruptedException e) {
                // (Recancel/cancel if current thread also interrupted
                scheduler.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread()
                        .interrupt();
            }
        } else {
            LOGGER.debug("Cache expiration scheduler already shutdown.");
        }
    }

    public void updateServer(String newUrl) {
        LOGGER.info("New url {}", newUrl);

        if (newUrl != null) {
            if (!StringUtils.equalsIgnoreCase(newUrl.trim(), url) || server == null) {

                this.url = newUrl.trim();

                if (server != null) {
                    LOGGER.info(
                            "Shutting down the connection manager to the Solr Server and releasing allocated resources.");
                    server.shutdown();
                    LOGGER.info("Shutdown complete.");
                }

                server = SolrServerFactory.getHttpSolrServer(url, METACARD_CACHE_CORE_NAME);
                client = new CacheSolrMetacardClient(this.server,
                        filterAdapter,
                        solrFilterDelegateFactory);
            }
        } else {
            this.url = null;
        }
    }

    public void forceCommit() {
        try {
            if (dirty.compareAndSet(true, false)) {
                server.commit();
            }
        } catch (SolrServerException | IOException e) {
            LOGGER.warn("Unable to commit changes to cache.", e);
        }
    }

    public String getMetacardId(SolrDocument doc) {
        return doc.getFieldValue(METACARD_ID_NAME)
                .toString();
    }

    public String getMetacardSource(SolrDocument doc) {
        return doc.getFieldValue(METACARD_SOURCE_NAME)
                .toString();
    }

    public void shutdown() {
        LOGGER.info("Shutting down cache expiration scheduler.");
        shutdownCacheExpirationScheduler();
        LOGGER.info("Shutting down solr server.");
        server.shutdown();
    }

    @Override
    public void removeAll() throws IOException, SolrServerException {
        client.deleteByQuery("*:*");
    }

    @Override
    public void removeById(String[] ids) throws IOException, SolrServerException {
        List<String> idList = Arrays.asList(ids);
        client.deleteByIds(METACARD_ID_NAME, idList, false);
    }

    @Override
    public List<Metacard> query(Filter filter) throws UnsupportedQueryException {
        QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(filter), true);

        SourceResponse response = client.query(queryRequest);
        return getMetacardsFromResponse(response);
    }

    private List<Metacard> getMetacardsFromResponse(SourceResponse sourceResponse) {
        if (CollectionUtils.isNotEmpty(sourceResponse.getResults())) {
            List<Metacard> metacards = new ArrayList<>(sourceResponse.getResults()
                    .size());
            for (Result result : sourceResponse.getResults()) {
                metacards.add(result.getMetacard());
            }
            return metacards;
        }
        return Collections.emptyList();
    }

    private class ExpirationRunner implements Runnable {

        @Override
        public void run() {
            try {
                LOGGER.debug("Expiring cache.");
                server.deleteByQuery(
                        CACHED_DATE + ":[* TO NOW-" + expirationAgeInMinutes + "MINUTES]");
            } catch (SolrServerException | IOException e) {
                LOGGER.warn("Unable to expire cache.", e);
            }
        }
    }

    private class CacheSolrMetacardClient extends SolrMetacardClient {

        public CacheSolrMetacardClient(SolrServer solrServer, FilterAdapter catalogFilterAdapter,
                SolrFilterDelegateFactory solrFilterDelegateFactory) {
            super(solrServer,
                    catalogFilterAdapter,
                    solrFilterDelegateFactory,
                    new DynamicSchemaResolver());
        }

        @Override
        public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
            MetacardImpl metacard = super.createMetacard(doc);

            metacard.setSourceId(getMetacardSource(doc));
            metacard.setId(getMetacardId(doc));

            return metacard;
        }

        public UpdateResponse delete(String query) throws IOException, SolrServerException {
            return server.deleteByQuery(query);
        }

        @Override
        protected SolrQuery getSolrQuery(QueryRequest request,
                SolrFilterDelegate solrFilterDelegate) throws UnsupportedQueryException {
            SolrQuery query = super.getSolrQuery(request, solrFilterDelegate);

            if (request.isEnterprise()) {
                return query;
            }

            List<SolrQuery> sourceQueries = new ArrayList<>();
            for (String source : request.getSourceIds()) {
                sourceQueries.add(solrFilterDelegate.propertyIsEqualTo(StringUtils.removeEnd(
                        METACARD_SOURCE_NAME,
                        SchemaFields.TEXT_SUFFIX), source, true));
            }
            if (sourceQueries.size() > 0) {
                SolrQuery allSourcesQuery;
                if (sourceQueries.size() > 1) {
                    allSourcesQuery = solrFilterDelegate.or(sourceQueries);
                } else {
                    allSourcesQuery = sourceQueries.get(0);
                }
                query = solrFilterDelegate.and(Lists.newArrayList(query, allSourcesQuery));
            }

            removeOuterParenthesesIfFunctionPresent(query);

            return query;
        }

        @Override
        protected SolrInputDocument getSolrInputDocument(Metacard metacard)
                throws MetacardCreationException {
            SolrInputDocument solrInputDocument = super.getSolrInputDocument(metacard);

            solrInputDocument.addField(CACHED_DATE, new Date());

            if (StringUtils.isNotBlank(metacard.getSourceId())) {
                solrInputDocument.addField(METACARD_SOURCE_NAME, metacard.getSourceId());
                solrInputDocument.setField(METACARD_UNIQUE_ID_NAME,
                        metacard.getSourceId() + metacard.getId());
                solrInputDocument.addField(METACARD_ID_NAME, metacard.getId());
            }

            return solrInputDocument;
        }
    }
}
