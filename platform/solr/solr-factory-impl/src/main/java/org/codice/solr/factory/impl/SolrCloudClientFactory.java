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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.zookeeper.KeeperException;
import org.codice.solr.factory.SolrClientFactory;
import org.codice.solr.factory.SolrConfigurationData;
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

  private static final String SOLR_DATA_DIR_PROP = "solr.data.dir";

  private static final String CONF_DIR = "conf";

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
  public boolean collectionExists(String collection) {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      return collectionExists(collection, client);
    } catch (SolrServerException | SolrException | SolrFactoryException | IOException e) {
      LOGGER.debug("Unable to verify if collection ({}) exists", collection, e);
    }
    return false;
  }

  @Override
  public void removeCollection(String collection) {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      CollectionAdminResponse response =
          CollectionAdminRequest.deleteCollection(collection).process(client);
      if (!response.isSuccess()) {
        throw new SolrFactoryException(
            "Failed to delete collection [" + collection + "]: " + response.getErrorMessages());
      }
    } catch (SolrServerException | SolrException | SolrFactoryException | IOException e) {
      LOGGER.debug("Unable to remove collection ({})", collection, e);
    }
  }

  @Override
  public void removeAlias(String alias) {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      CollectionAdminResponse response = CollectionAdminRequest.deleteAlias(alias).process(client);
      if (!response.isSuccess()) {
        throw new SolrFactoryException(
            "Failed to delete alias [" + alias + "]: " + response.getErrorMessages());
      }
    } catch (SolrServerException | SolrException | SolrFactoryException | IOException e) {
      LOGGER.debug("Unable to remove alias ({})", alias, e);
    }
  }

  @Override
  public void addConfiguration(
      String configurationName, List<SolrConfigurationData> configurationData) {
    checkConfig();
    boolean configExistsInZk = false;

    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      configExistsInZk =
          client.getZkStateReader().getZkClient().exists("/configs/" + configurationName, true);

      if (!configExistsInZk) {
        String solrDataBasePath = System.getProperty(SOLR_DATA_DIR_PROP);
        if (solrDataBasePath == null) {
          LOGGER.debug(
              "{} property not defined, will attempt to use Java tmpdir", SOLR_DATA_DIR_PROP);
          solrDataBasePath = System.getProperty("java.io.tmpdir");
        }
        File baseDir = Paths.get(solrDataBasePath, configurationName, CONF_DIR).toFile();
        boolean dirCreated = baseDir.mkdirs();
        LOGGER.debug(
            "Configuration directory {} created: {}", baseDir.getAbsolutePath(), dirCreated);

        storeConfigurationFiles(configurationName, baseDir, configurationData);
        Path configPath = baseDir.toPath();

        try (ZkClientClusterStateProvider zkStateProvider = newZkStateProvider(client)) {
          zkStateProvider.uploadConfig(configPath, configurationName);
        } catch (IOException e) {
          LOGGER.debug(
              "Failed to upload configurations for configuration: {}", configurationName, e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.debug(
          "Failed to check config status with Zookeeper for existing configuration: {}",
          configurationName,
          e);
    } catch (KeeperException e) {
      LOGGER.debug(
          "Failed to check config status with Zookeeper for existing configuration: {}",
          configurationName,
          e);
    }
  }

  @Override
  public void addCollection(
      String collection, Integer shardCountRequested, String configurationName) {
    int configShardCount = shardCountMap.getOrDefault(collection, shardCount);
    if (shardCountRequested != null) {
      configShardCount = shardCountRequested;
    }
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      createCollection(collection, Math.max(1, configShardCount), configurationName, client);
    } catch (SolrFactoryException e) {
      LOGGER.debug(
          "Unable to create collection: {} using configuration: {} and shardCount: {}",
          collection,
          configurationName,
          shardCount,
          e);
    }
  }

  @Override
  public void addCollectionToAlias(String alias, String collection, String collectionPrefix) {
    if (StringUtils.isBlank(alias) || StringUtils.isBlank(collection)) {
      return;
    }

    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();
      CollectionAdminResponse aliasResponse =
          new CollectionAdminRequest.ListAliases().process(client);
      String newCollections = collection;
      if (aliasResponse != null) {
        Map<String, String> aliases = aliasResponse.getAliases();
        if (aliases != null && aliases.containsKey(alias)) {
          String aliasedCollections = aliases.get(alias);
          String[] currentAliases = aliasedCollections.split(",");
          boolean aliasExists = false;
          for (String currentAlias : currentAliases) {
            if (currentAlias.equals(collection)) {
              aliasExists = true;
              break;
            }
          }
          if (aliasExists) {
            return;
          }

          List<String> newAliases = new ArrayList<>(Arrays.asList(currentAliases));
          newAliases.add(collection);

          if (StringUtils.isNotBlank(collectionPrefix)) {
            // Find existing collections in case parallel operations are creating collections
            CollectionAdminResponse response = new CollectionAdminRequest.List().process(client);

            if (response.getResponse() != null) {
              List<String> collections = (List<String>) response.getResponse().get("collections");
              if (collections != null) {
                for (String existingCollection : collections) {
                  if (existingCollection.startsWith(collectionPrefix)) {
                    if (!newAliases.contains(existingCollection)) {
                      newAliases.add(existingCollection);
                    }
                  }
                }
              }
            }
          }

          newCollections = String.join(",", newAliases);
        }
      }

      final String aliasedCollections = newCollections;
      RetryPolicy retryPolicy =
          new RetryPolicy()
              .withDelay(100, TimeUnit.MILLISECONDS)
              .withMaxDuration(3, TimeUnit.MINUTES)
              .retryOn(RemoteSolrException.class);
      CollectionAdminResponse response =
          Failsafe.with(retryPolicy)
              .get(
                  () ->
                      CollectionAdminRequest.createAlias(alias, aliasedCollections)
                          .process(client));

      if (response.getErrorMessages() != null && response.getErrorMessages().size() != 0) {
        LOGGER.warn(
            "Failed to update alias [{}}] with collections: [{}}], this will cause queries to be inconsistent. Error: {}",
            alias,
            newCollections,
            response.getErrorMessages());
      }
      if (!isAliasReady(client, alias)) {
        LOGGER.debug(
            "Alias [{}] was not created during collection [{}] creation", alias, collection);
      } else {
        LOGGER.trace("Alias [{}] updated with new list of collections: {}", alias, newCollections);
      }
    } catch (SolrServerException | IOException e) {
      LOGGER.warn("Failed to update alias [{}}]", alias, e);
    }
  }

  @Override
  public List<String> getCollectionsForAlias(String alias) {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();

      CollectionAdminResponse aliasResponse =
          new CollectionAdminRequest.ListAliases().process(client);
      if (aliasResponse != null) {
        Map<String, String> aliases = aliasResponse.getAliases();
        if (aliases != null && aliases.containsKey(alias)) {
          String aliasedCollections = aliases.get(alias);
          return Arrays.asList(aliasedCollections.split(","));
        }
      }
    } catch (SolrServerException | IOException e) {
      LOGGER.warn("Failed to get alias information [{}}]", alias, e);
    }
    return Collections.emptyList();
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

  private boolean isAliasReady(CloudSolrClient client, String alias) {
    try {
      boolean aliasReady =
          Failsafe.with(withRetry().retryWhen(false))
              .onFailure(
                  failure -> LOGGER.debug("Unable to get status on alias: {}", alias, failure))
              .get(() -> aliasExists(alias, client));
      if (!aliasReady) {
        LOGGER.debug("Solr ({}): Timeout while waiting for Alias to exist", alias);
      }
      return aliasReady;
    } catch (FailsafeException e) {
      LOGGER.debug("Solr({}): Failure waiting for Alias to exist", alias, e);
    }
    return false;
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

  protected void storeConfigurationFiles(
      String configurationName, File baseDir, List<SolrConfigurationData> configurationData) {

    for (SolrConfigurationData configData : configurationData) {
      File currentFile = new File(baseDir, configData.getFileName());
      if (!currentFile.exists())
        try (InputStream inputStream = configData.getConfigurationData();
            FileOutputStream outputStream = new FileOutputStream(currentFile)) {
          long byteCount = IOUtils.copyLarge(inputStream, outputStream);
          LOGGER.debug("Wrote out {} bytes for [{}].", byteCount, currentFile.getAbsoluteFile());
        } catch (IOException e) {
          LOGGER.warn("Unable to copy Solr configuration file: {}" + configData.getFileName(), e);
        }
    }
  }
}
