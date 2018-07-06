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
package org.codice.ddf.config.mapping.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.codice.ddf.config.ConfigEvent;
import org.codice.ddf.config.ConfigListener;
import org.codice.ddf.config.ConfigService;
import org.codice.ddf.config.mapping.ConfigMapping;
import org.codice.ddf.config.mapping.ConfigMappingEvent;
import org.codice.ddf.config.mapping.ConfigMappingEvent.Type;
import org.codice.ddf.config.mapping.ConfigMappingListener;
import org.codice.ddf.config.mapping.ConfigMappingProvider;
import org.codice.ddf.config.mapping.ConfigMappingService;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigMappingServiceImpl implements ConfigMappingService, ConfigListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMappingServiceImpl.class);

  private static final int THREAD_POOL_DEFAULT_SIZE = 16;

  private static final ExecutorService EXECUTOR = ConfigMappingServiceImpl.createExecutor();

  private final ConfigService config;

  private final SortedSet<ConfigMappingProvider> providers =
      Collections.synchronizedSortedSet(new TreeSet<>());

  private final List<ConfigMappingListener> listeners;

  private final Map<ConfigMapping.Id, ConfigMappingImpl> mappings = new ConcurrentHashMap<>();

  public ConfigMappingServiceImpl(ConfigService config, List<ConfigMappingListener> listeners) {
    this.config = config;
    this.listeners = listeners;
  }

  public void bind(ConfigMappingProvider provider) {
    LOGGER.debug("ConfigMappingServiceImpl::bind({})", provider);
    if (providers.add(provider)) {
      LOGGER.debug("bound provider: {}", provider);
      // find config mapping that needs to be updated
      mappings.values().stream().filter(provider::canProvideFor).forEach(m -> bind(provider, m));
    } else { // it was already there so rebind it
      LOGGER.debug("rebound provider: {}", provider);
      // don't filter mappings since we do not know anymore which one the old version was providing
      // for, since the updated instance could have change its response for canProvide()
      mappings.forEach((id, m) -> rebind(provider, provider, m));
    }
  }

  public boolean rebind(ConfigMappingProvider old, ConfigMappingProvider provider) {
    LOGGER.debug("ConfigMappingServiceImpl::rebind({}, {})", old, provider);
    // first check if the old one was bound and remove it
    if (!providers.remove(old)) {
      LOGGER.debug("old provider not found: {}", provider);
      return false;
    }
    LOGGER.debug("rebound provider: {}", provider);
    providers.add(provider);
    // find config mapping that needs to be updated
    if (old == provider) { // identity check
      // don't filter mappings since we do not know anymore which one the old version was providing
      // for, since the updated instance could have change its response for canProvide()
      mappings.forEach((id, m) -> rebind(provider, provider, m));
    } else {
      mappings
          .values()
          .stream()
          .filter(m -> old.canProvideFor(m) || provider.canProvideFor(m))
          .forEach(m -> rebind(old, provider, m));
    }
    return true;
  }

  public boolean unbind(ConfigMappingProvider provider) {
    LOGGER.debug("ConfigMappingServiceImpl::unbind({})", provider);
    if (providers.remove(provider)) {
      // find config mapping that needs to be updated
      // to be safe do not filter since we cannot be sure the canProvide() will yield the original
      // results
      mappings.forEach((id, m) -> unbind(provider, m));
      LOGGER.debug("unbound provider: {}", provider);
      return true;
    }
    return false;
  }

  @Override
  public Optional<ConfigMapping> getMapping(String name) {
    LOGGER.debug("ConfigMappingServiceImpl::getMapping({})", name);
    return getMapping(ConfigMapping.Id.of(name));
  }

  @Override
  public Optional<ConfigMapping> getMapping(String name, String instance) {
    LOGGER.debug("ConfigMappingServiceImpl::getMapping({}, {})", name, instance);
    return getMapping(ConfigMapping.Id.of(name, instance));
  }

  @Override
  public Optional<ConfigMapping> getMapping(ConfigMapping.Id id) {
    LOGGER.debug("ConfigMappingServiceImpl::getMapping({})", id);
    return Optional.ofNullable(mappings.computeIfAbsent(id, this::newMapping))
        .filter(ConfigMappingImpl::isAvailable)
        .map(ConfigMapping.class::cast);
  }

  @Override
  public void configChanged(ConfigEvent event) {
    LOGGER.debug("ConfigMappingServiceImpl::configChanged({})", event);
    mappings
        .values()
        .stream()
        .filter(ConfigMappingImpl::isAvailable)
        .filter(m -> m.isAffectedBy(event))
        .forEach(this::notifyUpdated);
  }

  @Nullable
  private ConfigMappingImpl newMapping(ConfigMapping.Id id) {
    LOGGER.debug("ConfigMappingServiceImpl::newMapping({})", id);
    // search all registered providers to find those that supports the specified mapping
    final ConfigMappingImpl mapping =
        new ConfigMappingImpl(config, id, providers.stream().filter(p -> p.canProvideFor(id)));

    if (mapping.isAvailable()) {
      notify(Type.CREATED, mapping);
    }
    return mapping;
  }

  private void bind(ConfigMappingProvider provider, ConfigMappingImpl mapping) {
    LOGGER.debug("ConfigMappingServiceImpl::bind({}, {})", provider, mapping);
    final boolean wasAvailable = mapping.isAvailable();

    if (mapping.bind(provider)) {
      LOGGER.debug("mapping was updated; wasAvailable={}", wasAvailable);
      notify(!wasAvailable ? Type.CREATED : Type.UPDATED, mapping);
    }
  }

  private void rebind(
      ConfigMappingProvider old, ConfigMappingProvider provider, ConfigMappingImpl mapping) {
    LOGGER.debug("ConfigMappingServiceImpl::rebind({}, {}, {})", old, provider, mapping);
    final boolean wasAvailable = mapping.isAvailable();

    if (mapping.rebind(old, provider)) {
      final boolean isAvailable = mapping.isAvailable();

      LOGGER.debug(
          "mapping was updated; wasAvailable = {}, isAvailable = {}", wasAvailable, isAvailable);
      if (wasAvailable) {
        notify(!isAvailable ? Type.REMOVED : Type.UPDATED, mapping);
      } else if (isAvailable) {
        notify(Type.CREATED, mapping);
      }
    }
  }

  private void unbind(ConfigMappingProvider provider, ConfigMappingImpl mapping) {
    LOGGER.debug("ConfigMappingServiceImpl::unbind({}, {})", provider, mapping);
    if (mapping.unbind(provider)) {
      LOGGER.debug("mapping was updated");
      notify(mapping.isAvailable() ? Type.UPDATED : Type.REMOVED, mapping);
    }
  }

  private void notifyUpdated(ConfigMapping mapping) {
    notify(Type.UPDATED, mapping);
  }

  private void notify(ConfigMappingEvent.Type type, ConfigMapping mapping) {
    LOGGER.debug("ConfigMappingServiceImpl::notify({}, {})", type, mapping);
    final ConfigMappingEventImpl event = new ConfigMappingEventImpl(type, mapping);

    listeners.forEach(
        l -> ConfigMappingServiceImpl.EXECUTOR.execute(() -> l.mappingChanged(event)));
  }

  private static ExecutorService createExecutor() throws NumberFormatException {
    return Executors.newFixedThreadPool(
        ConfigMappingServiceImpl.THREAD_POOL_DEFAULT_SIZE,
        StandardThreadFactoryBuilder.newThreadFactory("ConfigMappingService"));
  }
}
