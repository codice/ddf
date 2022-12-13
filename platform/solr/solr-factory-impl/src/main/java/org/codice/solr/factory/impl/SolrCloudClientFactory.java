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
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.ConfigSetAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.ContentStreamBase;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
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

  private static final RetryPolicyBuilder<Boolean>
      ABORT_WHEN_INTERRUPTED_AND_RETRY_UNTIL_NO_ERROR_AND_A_COLLECTION_IS_CREATED =
          RetryPolicy.<Boolean>builder()
              .handleResult(false)
              .abortOn(
                  InterruptedIOException.class,
                  InterruptedException.class,
                  VirtualMachineError.class)
              .withBackoff(Duration.ofMillis(10), Duration.ofMinutes(1))
              .withMaxAttempts(-1);

  private static final ScheduledExecutorService SCHEDULED_EXECUTOR =
      SolrCloudClientFactory.createExecutor();

  @Override
  public SolrClient newClient(String collection) {
    Validate.notNull(collection, "invalid null Solr core name");

    String zookeeperHosts = System.getProperty("solr.cloud.zookeeper", "localhost:2181");

    LOGGER.debug(
        "Solr({}): Creating a Solr Cloud client with configuration using Zookeeper hosts [{}]",
        collection,
        zookeeperHosts);

    return AccessController.doPrivileged(
        (PrivilegedAction<SolrClient>) () -> createSolrCloudClient(zookeeperHosts, collection));
  }

  @VisibleForTesting
  SolrClient createSolrCloudClient(String zookeeperHosts, String collection) {
    try {
      CloudSolrClient client = newCloudSolrClient(zookeeperHosts);
      client.setDefaultCollection(collection);

      Failsafe.with(
              ABORT_WHEN_INTERRUPTED_AND_RETRY_UNTIL_NO_ERROR_AND_A_COLLECTION_IS_CREATED
                  .onRetry(
                      e ->
                          LOGGER.debug(
                              "Solr client ({}) creation failed; retrying again: {}",
                              collection,
                              e.getLastException().getMessage()))
                  .onAbort(e -> LOGGER.debug("Solr client ({}) creation interrupted", collection))
                  .build())
          .with(SCHEDULED_EXECUTOR)
          .onFailure(
              e -> LOGGER.error("Solr client ({}) creation failed", collection, e.getException()))
          .onSuccess(e -> LOGGER.info("Solr client ({}) creation was successful", collection))
          .runAsync(() -> createCollectionIfMissing(collection, client));

      return client;
    } catch (LinkageError | Exception e) {
      LOGGER.info("Solr({}): Unable to create SolrCloud client", collection, e);
      return null;
    }
  }

  private boolean createCollectionIfMissing(String collection, CloudSolrClient client)
      throws SolrServerException, IOException, SolrFactoryException {
    client.connect();

    if (!isAliasCollection(collection, client)) {
      uploadCoreConfiguration(collection, client);
      createCollection(collection, client);
      return collectionExists(collection, client);
    } else {
      return true;
    }
  }

  @VisibleForTesting
  CloudSolrClient newCloudSolrClient(String zookeeperHosts) {
    return new CloudSolrClient.Builder(
            Arrays.asList(zookeeperHosts.split(",")),
            Optional.ofNullable(System.getProperty("solr.cloud.zookeeper.chroot")))
        .build();
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

  @VisibleForTesting
  void uploadCoreConfiguration(String collection, CloudSolrClient client)
      throws SolrFactoryException {

    try {
      ConfigSetAdminResponse.List list =
          new ConfigSetAdminRequest.List().process(client, collection);

      if (list.getConfigSets().contains(collection)) {
        return;
      }

      Configsets configSets = new Configsets();
      Path configPath = configSets.get(collection);

      LOGGER.info(
          "Configuration for collection [{}] not present in Solr. Uploading configset from [{}].",
          collection,
          configPath);
      new ConfigSetAdminRequest.Upload()
          .setUploadStream(
              new ContentStreamBase.ByteArrayStream(configSets.createZip(collection), collection))
          .setConfigSetName(collection)
          .process(client, collection);
    } catch (IOException | SolrServerException e) {
      throw new SolrFactoryException(
          "Failed to upload configurations for collection: " + collection, e);
    }
  }

  private boolean isCollectionReady(CloudSolrClient client, String collection, int shardCount) {
    try {
      boolean collectionCreated = client.getClusterState().hasCollection(collection);

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
          client.getClusterState().getCollection(collection).getSlices().size() == shardCount;
      if (!shardsStarted) {
        LOGGER.debug("Solr({}): Timeout while waiting for collection shards to start", collection);
      }
      return shardsStarted;
    } catch (FailsafeException e) {
      LOGGER.debug("Solr({}): Retry failure waiting for collection shards to start", collection, e);
      return false;
    }
  }

  private static ScheduledExecutorService createExecutor() throws NumberFormatException {
    return Executors.newScheduledThreadPool(
        NumberUtils.toInt(
            AccessController.doPrivileged(
                (PrivilegedAction<String>)
                    () -> System.getProperty("org.codice.ddf.system.threadPoolSize")),
            128),
        StandardThreadFactoryBuilder.newThreadFactory("SolrCloudClientFactory"));
  }
}
