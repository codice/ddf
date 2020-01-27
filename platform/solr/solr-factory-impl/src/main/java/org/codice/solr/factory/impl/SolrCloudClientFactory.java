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
package org.codice.solr.factory.impl;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.zookeeper.KeeperException;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class used to create new {@link CloudSolrClient} clients. <br>
 * Uses the following system properties when creating an instance:
 *
 * <ul>
 *   <li>solr.cloud.replicationFactor: Replication factor used when creating a new collection
 *   <li>solr.cloud.shardCount: Shard count used when creating a new collection
 *   <li>solr.cloud.maxShardPerNode: Maximum shard per node value used when creating a new
 *       collection
 *   <li>solr.cloud.zookeeper: Comma-separated list of Zookeeper hosts
 *   <li>org.codice.ddf.system.threadPoolSize: Solr query thread pool size
 * </ul>
 */
public class SolrCloudClientFactory implements SolrClientFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCloudClientFactory.class);

  private final int shardCount = NumberUtils.toInt(System.getProperty("solr.cloud.shardCount"), 2);

  private final int replicationFactor =
      NumberUtils.toInt(System.getProperty("solr.cloud.replicationFactor"), 2);

  private final int maximumShardsPerNode =
      NumberUtils.toInt(System.getProperty("solr.cloud.maxShardPerNode"), 2);

  private final Map<String, Integer> shardCountMap = new HashMap<>();

  private final Map<String, Integer> replicationFactorMap = new HashMap<>();

  private final Map<String, Integer> maximumShardsPerNodeMap = new HashMap<>();

  private String zookeeperHosts;

  public SolrCloudClientFactory() {
    String collectionList = System.getProperty("solr.cloud.collections");
    if (StringUtils.isBlank(collectionList)) {
      LOGGER.warn(
          "No solr.cloud.collections configuration found. Using default cloud configuration settings");
    } else {
      for (String collection : collectionList.split(",")) {
        shardCountMap.put(
            collection,
            NumberUtils.toInt(System.getProperty("solr.cloud." + collection + ".shardCount"), 2));
        replicationFactorMap.put(
            collection,
            NumberUtils.toInt(
                System.getProperty("solr.cloud." + collection + ".replicationFactor"), 2));
        maximumShardsPerNodeMap.put(
            collection,
            NumberUtils.toInt(
                System.getProperty("solr.cloud." + collection + ".maxShardPerNode"), 2));
      }
    }
    zookeeperHosts = System.getProperty("solr.cloud.zookeeper");
  }

  @Override
  public org.codice.solr.client.solrj.SolrClient newClient(String core) {
    checkConfig();
    LOGGER.debug(
        "Solr({}): Creating a Solr Cloud client using Zookeeper hosts [{}]", core, zookeeperHosts);
    SolrClientAdapter adaptor =
        new SolrClientAdapter(core, () -> createSolrCloudClient(zookeeperHosts, core));
    try {
      if (!adaptor.isAvailable(30, 5, TimeUnit.SECONDS)) {
        LOGGER.warn("Solr Client {} is not available after 30 seconds", core);
      }
    } catch (InterruptedException e) {
      LOGGER.error("Unable to connect to solr client {}: {} ", core, e.getStackTrace());
    }
    return adaptor;
  }

  @Override
  public boolean isSolrCloud() {
    return true;
  }

  @Override
  public boolean isAvailable() {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      return client.getIdField() != null;
    } catch (Exception e) {
      LOGGER.trace("Unable to check availability", e);
    }
    return false;
  }

  @VisibleForTesting
  SolrClient createSolrCloudClient(String zookeeperHosts, String collection) {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();

      try {
        if (!aliasExists(collection, client)) {
          try {
            uploadCoreConfiguration(collection, client);
          } catch (SolrFactoryException e) {
            LOGGER.debug("Solr({}): Unable to upload configuration to Solr Cloud", collection, e);
            return null;
          }

          try {
            createCollection(
                collection,
                Math.max(1, shardCountMap.getOrDefault(collection, shardCount)),
                collection,
                client);
          } catch (SolrFactoryException e) {
            LOGGER.debug("Solr({}): Unable to create collection on Solr Cloud", collection, e);
            return null;
          }
        }
      } catch (IOException | SolrServerException e) {
        LOGGER.debug("Unable to determine if {} is an alias", collection, e);
      }

      client.setDefaultCollection(collection);
      return closer.returning(client);
    }
  }

  @VisibleForTesting
  CloudSolrClient newCloudSolrClient(String zookeeperHosts) {
    return new CloudSolrClient.Builder(
            Arrays.asList(zookeeperHosts.split(",")),
            Optional.ofNullable(System.getProperty("solr.cloud.zookeeper.chroot")))
        .build();
  }

  @VisibleForTesting
  RetryPolicy withRetry() {
    return new RetryPolicy().withMaxRetries(120).withDelay(1, TimeUnit.SECONDS);
  }

  public void createCollection(
      String collection, int shardCount, String configurationName, CloudSolrClient client)
      throws SolrFactoryException {
    try {
      if (aliasExists(collection, client)) {
        LOGGER.debug(
            "Solr({}): Collection exists as an Alias, will not create collection", collection);
        return;
      }

      if (!collectionExists(collection, client)) {
        CollectionAdminResponse response =
            CollectionAdminRequest.createCollection(
                    collection,
                    configurationName,
                    shardCount,
                    replicationFactorMap.getOrDefault(collection, replicationFactor))
                .setMaxShardsPerNode(
                    maximumShardsPerNodeMap.getOrDefault(collection, maximumShardsPerNode))
                .process(client);
        if (!response.isSuccess()) {
          throw new SolrFactoryException(
              "Failed to create collection [" + collection + "]: " + response.getErrorMessages());
        }
        if (!isCollectionReady(client, collection)) {
          throw new SolrFactoryException(
              "Solr collection [" + collection + "] was not ready in time.");
        }
      } else {
        LOGGER.debug("Solr({}): Collection already exists", collection);
      }
    } catch (SolrServerException | SolrException | IOException e) {
      throw new SolrFactoryException("Failed to create collection: " + collection, e);
    }
  }

  private boolean aliasExists(String aliasName, CloudSolrClient client)
      throws IOException, SolrServerException {
    CollectionAdminResponse aliasResponse =
        new CollectionAdminRequest.ListAliases().process(client);
    if (aliasResponse != null) {
      Map<String, String> aliases = aliasResponse.getAliases();
      if (aliases != null && aliases.containsKey(aliasName)) {
        LOGGER.debug("Solr Alias name exists: {}", aliasName);
        return true;
      }
    }
    return false;
  }

  private boolean collectionExists(String collection, CloudSolrClient client)
      throws SolrFactoryException, IOException, SolrServerException {
    CollectionAdminResponse response = new CollectionAdminRequest.List().process(client);

    if (response.getResponse() == null) {
      throw new SolrFactoryException("Failed to get a list of existing collections");
    }

    List<String> collections = (List<String>) response.getResponse().get("collections");

    if (collections == null) {
      throw new SolrFactoryException("Failed to get a list of existing collections");
    }

    return collections.contains(collection);
  }

  @SuppressWarnings({
    "deprecation" /* Pre-existing use of ConfigurationFileProxy until redesigned */,
    "squid:CallToDeprecatedMethod" /* Pre-existing use of ConfigurationFileProxy until redesigned */
  })
  private void uploadCoreConfiguration(String collection, CloudSolrClient client)
      throws SolrFactoryException {
    boolean configExistsInZk;

    try {
      configExistsInZk =
          client.getZkStateReader().getZkClient().exists("/configs/" + collection, true);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SolrFactoryException(
          "Failed to check config status with Zookeeper for collection: " + collection, e);
    } catch (KeeperException e) {
      throw new SolrFactoryException(
          "Interrupted while checking config status with Zookeeper for collection: " + collection,
          e);
    }

    if (!configExistsInZk) {
      ConfigurationStore configStore = ConfigurationStore.getInstance();

      if (System.getProperty("solr.data.dir") != null) {
        configStore.setDataDirectoryPath(System.getProperty("solr.data.dir"));
      }

      ConfigurationFileProxy configProxy = new ConfigurationFileProxy(configStore);
      configProxy.writeSolrConfiguration(collection);
      Path configPath =
          Paths.get(configProxy.getDataDirectory().getAbsolutePath(), collection, "conf");

      try (ZkClientClusterStateProvider zkStateProvider = newZkStateProvider(client)) {
        zkStateProvider.uploadConfig(configPath, collection);
      } catch (IOException e) {
        throw new SolrFactoryException(
            "Failed to upload configurations for collection: " + collection, e);
      }
    }
  }

  @VisibleForTesting
  ZkClientClusterStateProvider newZkStateProvider(CloudSolrClient client) {
    return new ZkClientClusterStateProvider(
        client.getZkStateReader().getZkClient().getZkServerAddress());
  }

  private boolean isCollectionReady(CloudSolrClient client, String collection) {
    try {
      boolean collectionCreated =
          Failsafe.with(withRetry().retryWhen(false))
              .onFailure(
                  failure ->
                      LOGGER.debug(
                          "Solr({}): All attempts failed to read Zookeeper state for collection existence",
                          collection,
                          failure))
              .get(() -> client.getZkStateReader().getClusterState().hasCollection(collection));

      if (!collectionCreated) {
        LOGGER.debug("Solr({}): Timeout while waiting for collection to be created", collection);
        return false;
      }
    } catch (FailsafeException e) {
      LOGGER.debug(
          "Solr({}): Retry failure while waiting for collection to be created", collection, e);
      return false;
    }
    try {
      boolean shardsStarted =
          Failsafe.with(withRetry().retryWhen(false))
              .onFailure(
                  failure ->
                      LOGGER.debug(
                          "Solr({}): All attempts failed to read Zookeeper state for collection's shard count",
                          collection,
                          failure))
              .get(
                  () ->
                      client
                              .getZkStateReader()
                              .getClusterState()
                              .getCollection(collection)
                              .getSlices()
                              .size()
                          == shardCountMap.getOrDefault(collection, shardCount));

      if (!shardsStarted) {
        LOGGER.debug("Solr({}): Timeout while waiting for collection shards to start", collection);
      }
      return shardsStarted;
    } catch (FailsafeException e) {
      LOGGER.debug("Solr({}): Retry failure waiting for collection shards to start", collection, e);
      return false;
    }
  }


  protected void checkConfig() {
    if (StringUtils.isBlank(zookeeperHosts)) {
      zookeeperHosts = System.getProperty("solr.cloud.zookeeper");
    }

    if (StringUtils.isBlank(zookeeperHosts)) {
      LOGGER.warn(
          "Cannot create Solr Cloud client without Zookeeper host list system property [solr.cloud.zookeeper] being set.");
      throw new IllegalStateException("system property 'solr.cloud.zookeeper' is not configured");
    }
  }
}
