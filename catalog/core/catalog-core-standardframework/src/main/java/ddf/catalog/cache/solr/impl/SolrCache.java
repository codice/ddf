/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.cache.solr.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SchemaFields;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.solr.factory.SolrClientFactory;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Catalog cache implementation using Apache Solr */
public class SolrCache implements SolrCacheMBean {

  public static final String METACARD_CACHE_CORE_NAME = "metacard_cache";

  public static final String METACARD_SOURCE_NAME = "metacard_source" + SchemaFields.TEXT_SUFFIX;

  public static final String METACARD_ID_NAME = "original_id" + SchemaFields.TEXT_SUFFIX;

  // the unique id field used in the platform solr standalone solrClientAdaptor
  public static final String METACARD_UNIQUE_ID_NAME = "id" + SchemaFields.TEXT_SUFFIX;

  public static final String CACHED_DATE = "cached" + SchemaFields.DATE_SUFFIX;

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCache.class);

  private ObjectName objectName;

  private final SolrClientAdaptor solrClientAdaptor;

  private AtomicBoolean dirty = new AtomicBoolean(false);

  private ScheduledExecutorService scheduler;

  private long expirationIntervalInMinutes = 10;

  private long expirationAgeInMinutes = TimeUnit.DAYS.toMinutes(7);

  /**
   * Constructor.
   *
   * @param adapter injected implementation of {@link FilterAdapter}
   * @param solrClientFactory factory used to create new {@link
   *     org.apache.solr.client.solrj.SolrClient} instances
   * @param solrFilterDelegateFactory factory used to create new {@link
   *     ddf.catalog.source.solr.SolrFilterDelegate} instances
   */
  public SolrCache(
      FilterAdapter adapter,
      SolrClientFactory solrClientFactory,
      SolrFilterDelegateFactory solrFilterDelegateFactory) {
    this.solrClientAdaptor =
        new SolrClientAdaptor(
            METACARD_CACHE_CORE_NAME, adapter, solrClientFactory, solrFilterDelegateFactory);
    this.solrClientAdaptor.init();

    configureCacheExpirationScheduler();

    configureMBean();
  }

  // For unit testing purposes.
  SolrCache(SolrClientAdaptor solrClientAdaptor) {
    this.solrClientAdaptor = solrClientAdaptor;

    configureMBean();
  }

  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    return solrClientAdaptor.getSolrMetacardClient().query(request);
  }

  public void create(Collection<Metacard> metacards) {
    if (metacards == null || metacards.size() == 0) {
      return;
    }

    List<Metacard> updatedMetacards = new ArrayList<>();
    for (Metacard metacard : metacards) {
      if (metacard != null) {
        if (StringUtils.isNotBlank(metacard.getSourceId())
            && StringUtils.isNotBlank(metacard.getId())) {
          updatedMetacards.add(metacard);
        }
      } else {
        LOGGER.debug("metacard in result was null");
      }
    }

    try {
      solrClientAdaptor.getSolrMetacardClient().add(updatedMetacards, false);
      dirty.set(true);
    } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
      LOGGER.info("Solr solrClientAdaptor exception caching metacard(s)", e);
    }
  }

  public void delete(DeleteRequest deleteRequest) {
    if (deleteRequest == null) {
      return;
    }

    String attributeName = deleteRequest.getAttributeName();
    if (StringUtils.isBlank(attributeName)) {
      LOGGER.debug("Attribute name cannot be empty. Cannot delete from cache.");
      return;
    }

    String fieldName = attributeName + SchemaFields.TEXT_SUFFIX;
    if (fieldName.equals(METACARD_UNIQUE_ID_NAME)) {
      fieldName = METACARD_ID_NAME;
    }

    try {
      solrClientAdaptor
          .getSolrMetacardClient()
          .deleteByIds(fieldName, deleteRequest.getAttributeValues(), false);
      dirty.set(true);
    } catch (SolrServerException | IOException e) {
      LOGGER.info("Solr solrClientAdaptor exception while deleting from cache", e);
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
    LOGGER.debug(
        "Configuring cache expiration scheduler with an expiration interval of {} minute(s).",
        expirationIntervalInMinutes);
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("solrCacheThread"));
    scheduler.scheduleAtFixedRate(
        new ExpirationRunner(), 0, expirationIntervalInMinutes, TimeUnit.MINUTES);
  }

  private void configureMBean() {
    LOGGER.debug("Registering Cache Manager Service MBean");
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    try {
      objectName = new ObjectName(SolrCacheMBean.OBJECTNAME);
    } catch (MalformedObjectNameException e) {
      LOGGER.info("Could not create object name", e);
    }

    try {
      try {

        mbeanServer.registerMBean(new StandardMBean(this, SolrCacheMBean.class), objectName);
      } catch (InstanceAlreadyExistsException e) {
        LOGGER.debug("Re-registering Cache Manager MBean");
        mbeanServer.unregisterMBean(objectName);
        mbeanServer.registerMBean(new StandardMBean(this, SolrCacheMBean.class), objectName);
      }
    } catch (Exception e) {
      LOGGER.debug("Could not register MBean.", e);
    }
  }

  private void shutdownCacheExpirationScheduler() {
    if (scheduler != null && !scheduler.isShutdown()) {
      LOGGER.debug("Shutting down cache expiration scheduler.");
      scheduler.shutdown();
      try {
        // Wait up to 60 seconds existing tasks to terminate
        if (!scheduler.awaitTermination(60, SECONDS)) {
          scheduler.shutdownNow();
          // Wait up to 60 seconds for tasks to respond to being cancelled
          if (!scheduler.awaitTermination(60, SECONDS)) {
            LOGGER.debug("Cache expiration scheduler did not terminate.");
          }
        }
      } catch (InterruptedException e) {
        // (Recancel/cancel if current thread also interrupted
        scheduler.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    } else {
      LOGGER.debug("Cache expiration scheduler already shutdown.");
    }
  }

  public void forceCommit() {
    try {
      if (dirty.compareAndSet(true, false)) {
        solrClientAdaptor.commit();
      }
    } catch (SolrServerException | IOException e) {
      LOGGER.info("Unable to commit changes to cache.", e);
    }
  }

  public void shutdown() {
    LOGGER.debug("Shutting down cache expiration scheduler.");
    shutdownCacheExpirationScheduler();
    LOGGER.debug("Shutting down Solr solrClientAdaptor.");
    try {
      solrClientAdaptor.close();
    } catch (IOException e) {
      LOGGER.info("Failed to shutdown Solr solrClientAdaptor.", e);
    }
  }

  @Override
  public void removeAll() throws IOException, SolrServerException {
    solrClientAdaptor.getSolrMetacardClient().deleteByQuery("*:*");
  }

  @Override
  public void removeById(String[] ids) throws IOException, SolrServerException {
    List<String> idList = Arrays.asList(ids);
    solrClientAdaptor.getSolrMetacardClient().deleteByIds(METACARD_ID_NAME, idList, false);
  }

  @Override
  public List<Metacard> query(Filter filter) throws UnsupportedQueryException {
    QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(filter), true);

    SourceResponse response = solrClientAdaptor.getSolrMetacardClient().query(queryRequest);
    return getMetacardsFromResponse(response);
  }

  Set<ContentType> getContentTypes() {
    return solrClientAdaptor.getSolrMetacardClient().getContentTypes();
  }

  private List<Metacard> getMetacardsFromResponse(SourceResponse sourceResponse) {
    if (CollectionUtils.isNotEmpty(sourceResponse.getResults())) {
      List<Metacard> metacards = new ArrayList<>(sourceResponse.getResults().size());
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
        solrClientAdaptor.deleteByQuery(
            CACHED_DATE + ":[* TO NOW-" + expirationAgeInMinutes + "MINUTES]");
      } catch (SolrServerException | IOException e) {
        LOGGER.info("Unable to expire cache.", e);
      }
    }
  }
}
