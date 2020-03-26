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
package ddf.catalog.source.solr;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationStore {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

  private static ConfigurationStore uniqueInstance;

  private String dataDirectoryPath;

  private boolean forceAutoCommit;

  private boolean disableTextPath;

  private boolean inMemory;

  private Double nearestNeighborDistanceLimit;

  private List<String> anyTextFieldWhitelist = new ArrayList<>();

  private List<String> anyTextFieldBlacklist = new ArrayList<>();

  private List<ConfigurationListener> listeners = new ArrayList<>();

  private ConfigurationStore() { // everything is already initialized
  }

  public static synchronized ConfigurationStore getInstance() {

    if (uniqueInstance == null) {
      LOGGER.debug("Creating new instance of {}", ConfigurationStore.class.getSimpleName());
      uniqueInstance = new ConfigurationStore();
    }

    return uniqueInstance;
  }

  public boolean isDisableTextPath() {
    return disableTextPath;
  }

  public void setDisableTextPath(boolean disableTextPath) {
    this.disableTextPath = disableTextPath;

    notifyListeners();
  }

  public String getDataDirectoryPath() {
    return dataDirectoryPath;
  }

  public void setDataDirectoryPath(String dataDirectoryPath) {
    this.dataDirectoryPath = dataDirectoryPath;

    notifyListeners();
  }

  public Double getNearestNeighborDistanceLimit() {
    return nearestNeighborDistanceLimit;
  }

  public void setNearestNeighborDistanceLimit(Double nearestNeighborDistanceLimit) {
    this.nearestNeighborDistanceLimit = Math.abs(nearestNeighborDistanceLimit);

    notifyListeners();
  }

  public boolean isForceAutoCommit() {
    return forceAutoCommit;
  }

  public boolean isInMemory() {
    return inMemory;
  }

  public void setInMemory(boolean isInMemory) {
    inMemory = isInMemory;

    notifyListeners();
  }

  public void setForceAutoCommit(boolean forceAutoCommit) {
    this.forceAutoCommit = forceAutoCommit;
  }

  public void setAnyTextFieldWhitelist(List<String> fieldWhitelist) {
    this.anyTextFieldWhitelist.clear();
    if (fieldWhitelist != null) {
      this.anyTextFieldWhitelist.addAll(fieldWhitelist);
    }

    notifyListeners();
  }

  public List<String> getAnyTextFieldWhitelist() {
    return new ArrayList<>(anyTextFieldWhitelist);
  }

  public void setAnyTextFieldBlacklist(List<String> fieldBlacklist) {
    this.anyTextFieldBlacklist.clear();
    if (fieldBlacklist != null) {
      this.anyTextFieldBlacklist.addAll(fieldBlacklist);
    }

    notifyListeners();
  }

  public List<String> getAnyTextFieldBlacklist() {
    return new ArrayList<>(anyTextFieldBlacklist);
  }

  public void addConfigurationListener(ConfigurationListener listener) {
    this.listeners.add(listener);
  }

  public void removeConfigurationListener(ConfigurationListener listener) {
    this.listeners.remove(listener);
  }

  private void notifyListeners() {
    listeners
        .stream()
        .forEach(listener -> new Thread(() -> listener.configurationUpdated()).start());
  }
}
