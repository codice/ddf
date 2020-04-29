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

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.cache.CachePutPlugin;
import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.configuration.SearchCapabilityConfiguration;
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
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SchemaFields;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrMetacardClient;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.StandardMBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.client.solrj.UnavailableSolrException;
import org.codice.solr.factory.SolrClientFactory;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Catalog cache implementation using Apache Solr */
public class SolrCache implements SolrCacheMBean {

  public static final String METACARD_CACHE_CORE_NAME = "metacard_cache";

  public static final String METACARD_SOURCE_NAME = "metacard_source" + SchemaFields.TEXT_SUFFIX;

  public static final String METACARD_ID_NAME = "original_id" + SchemaFields.TEXT_SUFFIX;

  // the unique id field used in CacheSolrMetacardClient
  public static final String METACARD_UNIQUE_ID_NAME = "id" + SchemaFields.TEXT_SUFFIX;

  public static final String CACHED_DATE = "cached" + SchemaFields.DATE_SUFFIX;

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCache.class);

  private static final CachePutPlugin REQUIRED_ATTRIBUTES_PUT_PLUGIN =
      metacard -> {
        if (StringUtils.isNotBlank(metacard.getSourceId())
            && StringUtils.isNotBlank(metacard.getId())) {
          return Optional.of(metacard);
        }
        return Optional.empty();
      };

  private final SolrClient client;

  private final SolrMetacardClient metacardClient;

  private final AtomicBoolean dirty = new AtomicBoolean(false);

  private final Supplier<ScheduledExecutorService> schedulerCreator;

  private ScheduledExecutorService scheduler;

  private long expirationIntervalInMinutes = 10;

  private long expirationAgeInMinutes = TimeUnit.DAYS.toMinutes(7);

  private final List<CachePutPlugin> cachePutPlugins;

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
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver dynamicSchemaResolver,
      List<CachePutPlugin> cachePutPlugins) {
    this(
        adapter,
        solrClientFactory.newClient(METACARD_CACHE_CORE_NAME),
        solrFilterDelegateFactory,
        dynamicSchemaResolver,
        cachePutPlugins);
  }

  @VisibleForTesting
  SolrCache(
      SolrClient client,
      CacheSolrMetacardClient metacardClient,
      List<CachePutPlugin> cachePutPlugins) {
    this(client, metacardClient, SolrCache::createScheduler, cachePutPlugins);
  }

  @VisibleForTesting
  SolrCache(
      SolrClient client,
      CacheSolrMetacardClient metacardClient,
      Supplier<ScheduledExecutorService> schedulerCreator,
      List<CachePutPlugin> cachePutPlugins) {
    this.client = client;
    this.metacardClient = metacardClient;
    this.schedulerCreator = schedulerCreator;
    this.cachePutPlugins = cachePutPlugins;
    configureCacheExpirationScheduler();
    configureMBean();
  }

  @SuppressWarnings(
      "squid:UnusedPrivateMethod" /* used by another constructor and required to be able to use the client in 2 places */)
  private SolrCache(
      FilterAdapter adapter,
      SolrClient client,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver dynamicSchemaResolver,
      List<CachePutPlugin> cachePutPlugins) {
    this(
        client,
        new CacheSolrMetacardClient(
            client, adapter, solrFilterDelegateFactory, dynamicSchemaResolver),
        cachePutPlugins);
  }

  /**
   * Sets the fields that may be used for anyText expansion.
   *
   * @param anyTextFieldWhitelist
   */
  public void setAnyTextFieldWhitelist(List<String> anyTextFieldWhitelist) {
    ConfigurationStore.getInstance().setAnyTextFieldWhitelist(anyTextFieldWhitelist);
  }

  /**
   * Sets the fields that may NOT be used for anyText expansion.
   *
   * @param anyTextFieldBlacklist
   */
  public void setAnyTextFieldBlacklist(List<String> anyTextFieldBlacklist) {
    ConfigurationStore.getInstance().setAnyTextFieldBlacklist(anyTextFieldBlacklist);
  }

  public void setSearchCapabilityConfiguration(
      SearchCapabilityConfiguration searchCapabilityConfiguration) {
    metacardClient.setSearchCapabilityConfiguration(searchCapabilityConfiguration);
  }

  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    return metacardClient.query(request);
  }

  public void put(Collection<Metacard> metacards) {
    if (CollectionUtils.isEmpty(metacards)) {
      return;
    }

    List<Metacard> updatedMetacards = applyCachePutPlugins(metacards);

    if (CollectionUtils.isEmpty(updatedMetacards)) {
      return;
    }

    try {
      metacardClient.add(updatedMetacards, false);
      dirty.set(true);
    } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
      LOGGER.info("Solr client exception caching metacard(s)", e);
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
      metacardClient.deleteByIds(fieldName, deleteRequest.getAttributeValues(), false);
      dirty.set(true);
    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info("Solr client exception while deleting from cache", e);
    }
  }

  public void setExpirationIntervalInMinutes(long expirationInterval) {
    this.expirationIntervalInMinutes = expirationInterval;
    configureCacheExpirationScheduler();
  }

  public void setExpirationAgeInMinutes(long expirationAgeInMinutes) {
    this.expirationAgeInMinutes = expirationAgeInMinutes;
  }

  private List<Metacard> applyCachePutPlugins(Collection<Metacard> metacards) {
    List<Metacard> updatedMetacards = new ArrayList<>();
    for (Metacard metacard : metacards) {
      if (metacard != null) {
        applyCachePutPlugins(metacard).ifPresent(updatedMetacards::add);
      } else {
        LOGGER.debug("metacard in result was null");
      }
    }
    return updatedMetacards;
  }

  private Optional<Metacard> applyCachePutPlugins(Metacard metacard) {
    Optional<Metacard> updatedMetacard = REQUIRED_ATTRIBUTES_PUT_PLUGIN.process(metacard);
    if (updatedMetacard.isPresent()) {
      for (CachePutPlugin cachePutPlugin : cachePutPlugins) {
        updatedMetacard = cachePutPlugin.process(updatedMetacard.get());
        if (!updatedMetacard.isPresent()) {
          return updatedMetacard;
        }
      }
    }
    return updatedMetacard;
  }

  private void configureCacheExpirationScheduler() {
    shutdownCacheExpirationScheduler();
    LOGGER.debug(
        "Configuring cache expiration scheduler with an expiration interval of {} minute(s).",
        expirationIntervalInMinutes);
    this.scheduler = schedulerCreator.get();
    scheduler.scheduleAtFixedRate(
        new ExpirationRunner(), 0, expirationIntervalInMinutes, TimeUnit.MINUTES);
  }

  private void configureMBean() {
    LOGGER.debug("Registering Cache Manager Service MBean");
    final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    final ObjectName objectName;

    try {
      objectName = new ObjectName(SolrCacheMBean.OBJECT_NAME);
    } catch (MalformedObjectNameException e) {
      LOGGER.info("Could not create object name", e);
      return;
    }
    try {
      registerMBean(mbeanServer, objectName);
    } catch (Exception e) {
      LOGGER.debug("Could not register MBean.", e);
    }
  }

  private void registerMBean(MBeanServer mbeanServer, ObjectName objectName)
      throws IOException, NotCompliantMBeanException, MBeanException, OperationsException {
    try {
      mbeanServer.registerMBean(new StandardMBean(this, SolrCacheMBean.class), objectName);
    } catch (InstanceAlreadyExistsException e) {
      LOGGER.debug("Re-registering Cache Manager MBean");
      mbeanServer.unregisterMBean(objectName);
      mbeanServer.registerMBean(new StandardMBean(this, SolrCacheMBean.class), objectName);
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
        client.commit();
      }
    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info("Unable to commit changes to cache.", e);
    }
  }

  public void shutdown() {
    LOGGER.debug("Shutting down cache expiration scheduler.");
    shutdownCacheExpirationScheduler();
    LOGGER.debug("Shutting down Solr client.");
    try {
      client.close();
    } catch (IOException e) {
      LOGGER.info("Failed to shutdown Solr client.", e);
    }
  }

  @Override
  public void removeAll() throws IOException, SolrServerException {
    metacardClient.deleteByQuery("*:*");
  }

  @Override
  public void removeById(String[] ids) throws IOException, SolrServerException {
    List<String> idList = Arrays.asList(ids);
    metacardClient.deleteByIds(METACARD_ID_NAME, idList, false);
  }

  @Override
  public List<Metacard> query(Filter filter) throws UnsupportedQueryException {
    QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(filter), true);

    SourceResponse response = metacardClient.query(queryRequest);
    return getMetacardsFromResponse(response);
  }

  Set<ContentType> getContentTypes() {
    return metacardClient.getContentTypes();
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

  private static ScheduledExecutorService createScheduler() {
    return Executors.newSingleThreadScheduledExecutor(
        StandardThreadFactoryBuilder.newThreadFactory("solrCacheThread"));
  }

  private class ExpirationRunner implements Runnable {
    @Override
    public void run() {
      try {
        LOGGER.debug("Expiring cache.");
        client.deleteByQuery(CACHED_DATE + ":[* TO NOW-" + expirationAgeInMinutes + "MINUTES]");
      } catch (UnavailableSolrException e) {
        LOGGER.debug("Unable to expire cache.", e);
      } catch (SolrServerException | SolrException | IOException e) {
        LOGGER.info("Unable to expire cache; {}", e.getMessage());
        LOGGER.debug("Cache expiration error.", e);
      }
    }
  }
}
