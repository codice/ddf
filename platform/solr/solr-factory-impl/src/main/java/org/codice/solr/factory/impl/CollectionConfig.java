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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(CollectionConfig.class);

  private final int shardCount;

  private final int replicationFactor;

  private final int maximumShardsPerNode;

  public CollectionConfig(String collection) {
    this(collection, Paths.get(System.getProperty("ddf.home", ""), "etc/solr/configsets"));
  }

  public CollectionConfig(String collection, Path collectionConfigs) {
    File matchingConfig =
        collectionConfigs.resolve(Paths.get(collection, "collection.properties")).toFile();
    Properties collectionProps = new Properties();
    if (matchingConfig.exists()) {
      try (InputStream collectionConfigStream = new FileInputStream(matchingConfig)) {
        collectionProps.load(collectionConfigStream);
      } catch (IOException e) {
        LOGGER.debug("Unable to read matching collection properties file.", e);
      }
    }
    if (collectionProps.size() == 0) {
      File defaultConfig = collectionConfigs.resolve("default/collection.properties").toFile();
      if (defaultConfig.exists()) {
        try (InputStream defaultConfigStream = new FileInputStream(defaultConfig)) {
          collectionProps.load(defaultConfigStream);
        } catch (IOException e) {
          LOGGER.debug("Unable to read default collection properties file.", e);
        }
      }
    }

    shardCount =
        NumberUtils.toInt(
            collectionProps.getProperty("numShards", System.getProperty("solr.cloud.shardCount")),
            2);

    replicationFactor =
        NumberUtils.toInt(
            collectionProps.getProperty(
                "replicationFactor", System.getProperty("solr.cloud.replicationFactor")),
            2);

    maximumShardsPerNode =
        NumberUtils.toInt(
            collectionProps.getProperty(
                "maxShardsPerNode", System.getProperty("solr.cloud.maxShardPerNode")),
            2);
  }

  public int getShardCount() {
    return shardCount;
  }

  public int getReplicationFactor() {
    return replicationFactor;
  }

  public int getMaximumShardsPerNode() {
    return maximumShardsPerNode;
  }
}
