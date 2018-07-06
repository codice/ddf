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
package org.codice.ddf.config.service.impl;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codice.ddf.config.Config;
import org.codice.ddf.config.ConfigEvent;
import org.codice.ddf.config.ConfigGroup;
import org.codice.ddf.config.ConfigSingleton;
import org.codice.ddf.config.service.eventing.impl.ConfigEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigTracker {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTracker.class);

  private final Map<String, Config> previous = Maps.newHashMap();

  private final Map<String, Config> current = Maps.newHashMap();

  private final Map<String, Set<Config>> fileToConfig = Maps.newHashMap();

  public ConfigEvent install(String filename, Set<Config> configs) {
    LOGGER.error("##### Start ConfigServiceImpl::install");
    LOGGER.error("##### Received configs: {}", configs);
    fileToConfig.put(filename, configs);
    updatePrevious();
    configs.forEach(this::updateCurrent);
    LOGGER.error("##### End ConfigServiceImpl::install");
    return diff();
  }

  public ConfigEvent update(String filename, Set<Config> configs) {
    LOGGER.error("##### Start ConfigServiceImpl::update");
    LOGGER.error("##### Received configs: {}", configs);
    fileToConfig.put(filename, configs);
    updatePrevious();
    configs.forEach(this::updateCurrent);
    LOGGER.error("##### End ConfigServiceImpl::update");
    return diff();
  }

  public ConfigEvent remove(String filename) {
    LOGGER.error("##### Start ConfigServiceImpl::remove");
    Set<Config> configs = fileToConfig.get(filename);
    // current.values().removeIf(v -> v.equals(aValue));

    Set<String> groupIds =
        fileToConfig
            .values()
            .stream()
            .flatMap(Collection::stream)
            .filter(ConfigGroup.class::isInstance)
            .map(ConfigGroup.class::cast)
            .map(v -> v.getType() + "-" + v.getId())
            .collect(Collectors.toSet());
    Set<String> singletonIds =
        fileToConfig
            .values()
            .stream()
            .flatMap(Collection::stream)
            .filter(ConfigSingleton.class::isInstance)
            .map(ConfigSingleton.class::cast)
            .map(ConfigSingleton::getType)
            .map(Class::toString)
            .collect(Collectors.toSet());

    previous.keySet().removeAll(groupIds);
    previous.keySet().removeAll(singletonIds);
    current.keySet().removeAll(groupIds);
    current.keySet().removeAll(singletonIds);

    fileToConfig.remove(filename);

    ConfigEvent configEvent =
        new ConfigEventImpl(Collections.emptySet(), Collections.emptySet(), configs);

    LOGGER.error("##### End ConfigServiceImpl::remove");

    return configEvent;
  }

  public ConfigEvent diff() {
    LOGGER.error("##### Start ConfigServiceImpl::diff");
    MapDifference<String, Config> diff = Maps.difference(previous, current);

    Map<String, MapDifference.ValueDifference<Config>> entriesDiffering = diff.entriesDiffering();
    Set<Config> updatedConfigs =
        entriesDiffering.values().stream().map(e -> e.rightValue()).collect(Collectors.toSet());

    Map<String, Config> entriesOnlyOnRight = diff.entriesOnlyOnRight();
    Set<Config> addedConfigs = entriesOnlyOnRight.values().stream().collect(Collectors.toSet());

    Map<String, Config> entriesOnlyOnLeft = diff.entriesOnlyOnLeft();
    Set<Config> removedConfigs = entriesOnlyOnLeft.values().stream().collect(Collectors.toSet());

    ConfigEvent configEvent = new ConfigEventImpl(addedConfigs, updatedConfigs, removedConfigs);

    LOGGER.error("##### End ConfigServiceImpl::diff");
    return configEvent;
  }

  private void updatePrevious() {
    LOGGER.error("##### Start ConfigServiceImpl::updatePrevious()");
    this.previous.putAll(this.current);
    LOGGER.error("##### End ConfigServiceImpl::updatePrevious()");
  }

  private void updateCurrent(Config c) {
    if (c instanceof ConfigGroup) {
      current.put(((ConfigGroup) c).getId(), c);
    } else if (c instanceof ConfigSingleton) {
      current.put(c.getType().getName(), c);
    }
  }

  public <T extends ConfigSingleton> Optional<T> get(Class<T> type) {
    LOGGER.error("##### ConfigServiceImpl::get(type)");
    return current.values().stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  public <T extends ConfigGroup> Optional<T> get(Class<T> type, String id) {
    LOGGER.error("##### ConfigServiceImpl::get(type, id)");
    return configs(type).filter(c -> c.getId().equals(id)).findFirst();
  }

  public <T extends ConfigGroup> Stream<T> configs(Class<T> type) {
    LOGGER.error("##### ConfigServiceImpl::configs(type)");
    return current.values().stream().filter(type::isInstance).map(type::cast);
  }
}
