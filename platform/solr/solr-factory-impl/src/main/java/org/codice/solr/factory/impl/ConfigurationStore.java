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
