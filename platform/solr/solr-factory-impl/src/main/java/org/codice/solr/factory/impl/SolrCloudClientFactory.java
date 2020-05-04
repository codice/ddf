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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
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
 *   <li>solr.cloud.zookeeper: Comma-separated list of Zookeeper hosts
 *   <li>org.codice.ddf.system.threadPoolSize: Solr query thread pool size
 * </ul>
 */
public class SolrCloudClientFactory implements SolrClientFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCloudClientFactory.class);

  @Override
  public org.codice.solr.client.solrj.SolrClient newClient(String collection) {
    Validate.notNull(collection, "invalid null Solr core name");

    String zookeeperHosts = System.getProperty("solr.cloud.zookeeper");
    checkConfig(zookeeperHosts);

    LOGGER.debug(
        "Solr({}): Creating a Solr Cloud client with configuration using Zookeeper hosts [{}]",
        collection,
        zookeeperHosts);

    return new SolrClientAdapter(
        collection,
        () ->
            AccessController.doPrivileged(
                (PrivilegedAction<SolrClient>)
                    () -> {
                      return createSolrCloudClient(zookeeperHosts, collection);
                    }));
  }

  @VisibleForTesting
  SolrClient createSolrCloudClient(String zookeeperHosts, String collection) {
    try (final Closer closer = new Closer()) {
      CloudSolrClient client = closer.with(newCloudSolrClient(zookeeperHosts));
      client.connect();

      if (!isAliasCollection(collection, client)) {
        try {
          uploadCoreConfiguration(collection, client);
        } catch (SolrFactoryException e) {
          LOGGER.debug("Unable to create collection: {} ", collection, e);
          return null;
        }

        try {
          if (createCollection(collection, client)) {
            waitForCollection(collection, client);
          }

        } catch (SolrFactoryException e) {
          LOGGER.debug("Solr({}): Unable to create collection on SolrCloud", collection, e);
          return null;
        }
      }
      client.setDefaultCollection(collection);
      return closer.returning(client);
    } catch (LinkageError | Exception e) {
      LOGGER.debug("Solr({}): Unable to create SolrCloud client", collection, e);
      return null;
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

  private void waitForCollection(final String collection, final CloudSolrClient client) {
    RetryPolicy retryPolicy =
        new RetryPolicy()
            .withDelay(100, TimeUnit.MILLISECONDS)
            .withMaxDuration(3, TimeUnit.MINUTES)
            .retryWhen(false);
    Failsafe.with(retryPolicy).run(() -> collectionExists(collection, client));
  }

  public boolean createCollection(String collection, CloudSolrClient client)
      throws SolrFactoryException {
    try {
      if (isAliasCollection(collection, client)) {
        LOGGER.debug(
            "Solr({}): Collection exists as an Alias, will not create collection", collection);
        return false;
      }

      if (!collectionExists(collection, client)) {
        CollectionConfig config = new CollectionConfig(collection);
        CollectionAdminResponse response =
            CollectionAdminRequest.createCollection(
                    collection, collection, config.getShardCount(), config.getReplicationFactor())
                .setMaxShardsPerNode(config.getMaximumShardsPerNode())
                .process(client);
        if (!response.isSuccess()) {
          throw new SolrFactoryException(
              "Failed to create collection [" + collection + "]: " + response.getErrorMessages());
        }
        if (!isCollectionReady(client, collection, config.getShardCount())) {
          throw new SolrFactoryException(
              "Solr collection [" + collection + "] was not ready in time.");
        }
      } else {
        LOGGER.debug("Solr({}): Collection already exists", collection);
        return false;
      }
    } catch (SolrServerException | SolrException | IOException e) {
      throw new SolrFactoryException("Failed to create collection: " + collection, e);
    }

    return true;
  }

  private boolean isAliasCollection(String collection, CloudSolrClient client)
      throws IOException, SolrServerException {
    CollectionAdminResponse aliasResponse =
        new CollectionAdminRequest.ListAliases().process(client);
    if (aliasResponse != null) {
      Map<String, String> aliases = aliasResponse.getAliases();
      if (aliases != null && aliases.containsKey(collection)) {
        LOGGER.debug(
            "Solr({}): Collection exists as an Alias, will not create collection", collection);
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  boolean collectionExists(String collection, CloudSolrClient client)
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
  @VisibleForTesting
  void uploadCoreConfiguration(String collection, CloudSolrClient client)
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
      Path configPath = new Configsets().get(collection);

      LOGGER.info(
          "Configuration for collection [{}] not present in Zookeeper. Uploading configset from [{}].",
          collection,
          configPath.toString());

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

  private boolean isCollectionReady(CloudSolrClient client, String collection, int shardCount) {
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
                          == shardCount);

      if (!shardsStarted) {
        LOGGER.debug("Solr({}): Timeout while waiting for collection shards to start", collection);
      }
      return shardsStarted;
    } catch (FailsafeException e) {
      LOGGER.debug("Solr({}): Retry failure waiting for collection shards to start", collection, e);
      return false;
    }
  }

  protected void checkConfig(String zookeeperHosts) {
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
