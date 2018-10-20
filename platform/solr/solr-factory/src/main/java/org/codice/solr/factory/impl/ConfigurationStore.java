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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores external configuration properties to be used across POJOs. */
public class ConfigurationStore {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

  private static ConfigurationStore uniqueInstance;

  private String dataDirectoryPath;

  private boolean forceAutoCommit;

  private boolean disableTextPath;

  private boolean inMemory;

  private Double nearestNeighborDistanceLimit;

  /**
   * Constructs a new configuration store.
   *
   * <p>Although this class was originally defined to be a singleton, its usage is now problematic
   * as it is expected that settings stored here are for every possible usage of the configuration
   * store. However, we have places in the product where we are hard-coded to use an in-memory
   * embedded solr server and other places where we fallback to the one configured Solr server.
   *
   * <p>To circumvent the bug that this one place creates when it starts re-configuring the store to
   * use an in-memory Solr server, we were forced to make this constructor public. The {@link
   * EmbeddedSolrFactory}Solr factory class was properly modified to rely on the passed in
   * configuration store to extract its properties instead of relying on the singleton; thus
   * allowing a mixture of settings in the system.
   */
  @SuppressWarnings(
      "PMD.UnnecessaryConstructor" /* Added to emphasize the fact tha we have a public default ctor
                                   that should be used only in specific case as the majority of the system still relies on the
                                   singleton for now */)
  public ConfigurationStore() { // everything is already initialized
  }

  /** @return a unique instance of {@link ConfigurationStore} */
  public static synchronized ConfigurationStore getInstance() {

    if (uniqueInstance == null) {
      LOGGER.debug("Creating new instance of {}", ConfigurationStore.class.getSimpleName());
      uniqueInstance = new ConfigurationStore();
    }

    return uniqueInstance;
  }

  /** @return true, if text path indexing has been disabled */
  public boolean isDisableTextPath() {
    return disableTextPath;
  }

  /**
   * @param disableTextPath When set to true, this will turn off text path indexing for every
   *     subsequent update or insert.
   */
  public void setDisableTextPath(boolean disableTextPath) {
    this.disableTextPath = disableTextPath;
  }

  public String getDataDirectoryPath() {
    return dataDirectoryPath;
  }

  public void setDataDirectoryPath(String dataDirectoryPath) {
    this.dataDirectoryPath = dataDirectoryPath;
  }

  public Double getNearestNeighborDistanceLimit() {
    return nearestNeighborDistanceLimit;
  }

  public void setNearestNeighborDistanceLimit(Double nearestNeighborDistanceLimit) {
    this.nearestNeighborDistanceLimit = Math.abs(nearestNeighborDistanceLimit);
  }

  /** @return true, if forcing auto commit is turned on */
  public boolean isForceAutoCommit() {
    return forceAutoCommit;
  }

  /** @return true, if index stored in memory */
  public boolean isInMemory() {
    return inMemory;
  }

  public void setInMemory(boolean isInMemory) {
    inMemory = isInMemory;
  }

  /**
   * @param forceAutoCommit When set to true, this will force a soft commit upon every solr
   *     transaction such as insert, delete,
   */
  public void setForceAutoCommit(boolean forceAutoCommit) {
    this.forceAutoCommit = forceAutoCommit;
  }
}
