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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

  private Set<String> anyTextFieldsCache = new HashSet<>();

  private Set<String> filteredAnyTextFieldsCache = new HashSet<>();

  private List<String> anyTextFieldWhitelist = new ArrayList<>();

  private List<String> anyTextFieldBlacklist = new ArrayList<>();

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

  public boolean isForceAutoCommit() {
    return forceAutoCommit;
  }

  public boolean isInMemory() {
    return inMemory;
  }

  public void setInMemory(boolean isInMemory) {
    inMemory = isInMemory;
  }

  public void setForceAutoCommit(boolean forceAutoCommit) {
    this.forceAutoCommit = forceAutoCommit;
  }

  public void setAnyTextFieldWhitelist(List<String> fieldWhitelist) {
    this.anyTextFieldWhitelist.clear();
    if (fieldWhitelist != null) {
      this.anyTextFieldWhitelist.addAll(fieldWhitelist);
    }

    filterAnyTextFieldCache();
  }

  public List<String> getAnyTextFieldWhitelist() {
    return new ArrayList<>(anyTextFieldWhitelist);
  }

  public void addAnyTextWhitelistField(String whitelistField) {
    if (whitelistField != null) {
      this.anyTextFieldWhitelist.add(whitelistField);
    }

    filterAnyTextFieldCache();
  }

  public void removeAnyTextWhiteListField(String whitelistField) {
    this.anyTextFieldWhitelist.remove(whitelistField);

    filterAnyTextFieldCache();
  }

  public void setAnyTextFieldBlacklist(List<String> fieldBlacklist) {
    this.anyTextFieldBlacklist.clear();
    if (fieldBlacklist != null) {
      this.anyTextFieldBlacklist.addAll(fieldBlacklist);
    }

    filterAnyTextFieldCache();
  }

  public List<String> getAnyTextFieldBlacklist() {
    return new ArrayList<>(anyTextFieldBlacklist);
  }

  public void addAnyTextBlacklistField(String blacklistField) {
    this.anyTextFieldBlacklist.add(blacklistField);

    filterAnyTextFieldCache();
  }

  public void removeAnyTextBlacklistField(String blacklistField) {
    this.anyTextFieldBlacklist.remove(blacklistField);

    filterAnyTextFieldCache();
  }

  public void addAnyTextField(String anyTextField) {
    anyTextFieldsCache.add(anyTextField);

    filterAnyTextFieldCache();
  }

  public void removeAnyTextField(String anyTextField) {
    anyTextFieldsCache.remove(anyTextField);

    filterAnyTextFieldCache();
  }

  public void clearAnyTextFieldCache() {
    anyTextFieldsCache.clear();
    filteredAnyTextFieldsCache.clear();
  }

  public Set<String> getAnyTextFieldsCache() {
    return anyTextFieldsCache;
  }

  public Set<String> getFilteredAnyTextFields() {
    return filteredAnyTextFieldsCache;
  }

  private void filterAnyTextFieldCache() {
    Set<String> filteredList = new HashSet<>();

    ConfigurationStore config = ConfigurationStore.getInstance();
    List<String> anyTextFieldWhitelist = config.getAnyTextFieldWhitelist();
    List<String> anyTextFieldBlacklist = config.getAnyTextFieldBlacklist();
    if (!anyTextFieldBlacklist.isEmpty()) {
      filteredList.addAll(anyTextFieldsCache);
      for (String blacklistField : anyTextFieldBlacklist) {
        String blacklist;
        if (!blacklistField.endsWith(SchemaFields.TEXT_SUFFIX)) {
          blacklist = blacklistField + SchemaFields.TEXT_SUFFIX;
        } else {
          blacklist = blacklistField;
        }
        filteredList.removeAll(
            anyTextFieldsCache
                .stream()
                .filter(field -> field.matches(blacklist))
                .collect(Collectors.toList()));
      }
    }

    if (!anyTextFieldWhitelist.isEmpty()) {
      for (String whitelistField : anyTextFieldWhitelist) {
        String whitelist;
        if (!whitelistField.endsWith(SchemaFields.TEXT_SUFFIX)) {
          whitelist = whitelistField + SchemaFields.TEXT_SUFFIX;
        } else {
          whitelist = whitelistField;
        }
        filteredList.addAll(
            anyTextFieldsCache
                .stream()
                .filter(field -> field.matches(whitelist))
                .collect(Collectors.toList()));
      }
    }

    if (anyTextFieldBlacklist.isEmpty() && anyTextFieldWhitelist.isEmpty()) {
      filteredList.addAll(anyTextFieldsCache);
    }

    filteredAnyTextFieldsCache = filteredList;
  }
}
