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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codice.ddf.config.Config;
import org.codice.ddf.config.ConfigEvent;
import org.codice.ddf.config.ConfigGroup;
import org.codice.ddf.config.ConfigService;
import org.codice.ddf.config.ConfigSingleton;
import org.codice.ddf.config.mapping.ConfigMapping;
import org.codice.ddf.config.mapping.ConfigMappingException;
import org.codice.ddf.config.mapping.ConfigMappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigMappingImpl implements ConfigMapping {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMappingImpl.class);

  // marker to indicate that a config mapping depends on all instances/ids of a given config type
  private static final Set<String> ALL = Collections.emptySet();

  // marker to representing a fake instance id representing any instances
  private static final String ANY = "*";

  private final ConfigService config;

  private final ConfigMapping.Id id;

  private final SortedSet<ConfigMappingProvider> providers;

  // keyed by config type with corresponding set of instances or ALL if not a group or dependent on
  // all instances
  private final Map<Class<? extends Config>, Set<String>> dependents = new ConcurrentHashMap<>();

  public ConfigMappingImpl(
      ConfigService config, ConfigMapping.Id id, Stream<ConfigMappingProvider> providers) {
    this.config = new DependentConfigService(config);
    this.id = id;
    this.providers =
        Collections.synchronizedSortedSet(providers.collect(Collectors.toCollection(TreeSet::new)));
    try {
      // first resolution to compute the initial dependents, use the ANY instance if none defined
      // just in case the providers are referencing it. This will be useful providers capable of
      // providing for any instances
      resolve(ConfigMapping.Id.of(id.getName(), id.getInstance().orElse(ConfigMappingImpl.ANY)));
    } catch (ConfigMappingException e) { // ignore
    }
  }

  @Override
  public Id getId() {
    return id;
  }

  /**
   * Checks if this config mapping currently has any providers for it.
   *
   * @return <code>true</code> if at least one provider is capable of providing all mapped
   *     properties for this config mapping; <code>false</code> otherwise
   */
  public boolean isAvailable() {
    // must have at least 1 non partial provider
    final boolean available = !providers.stream().allMatch(ConfigMappingProvider::isPartial);

    LOGGER.debug("ConfigMappingImpl[{}].isAvailable() = {}", id, available);
    return available;
  }

  /**
   * Binds the specified provider as a new provider to use when resolving mapped properties.
   *
   * @param provider the new provider to use when resolving mapped properties
   * @return <code>true</code> if the provider wasn't already bound; <code>false</code> otherwise
   */
  boolean bind(ConfigMappingProvider provider) {
    final boolean bound = providers.add(provider);

    LOGGER.debug("ConfigMappingImpl[{}].bind({}) = {}", id, provider, bound);
    return bound;
  }

  /**
   * Rebinds an existing provider with a new one to this mapping.
   *
   * @param old the old provider to be unbound
   * @param provider the new provider to be bound (if it can provide for this mapping)
   * @return <code>true</code> if a change in provider occurred; <code>false</code> otherwise
   */
  boolean rebind(ConfigMappingProvider old, ConfigMappingProvider provider) {
    boolean updated = providers.remove(old);

    if (updated) {
      LOGGER.debug("ConfigMappingImpl[{}].rebind({}, {}) - unbound", id, old, provider);
    }
    if (provider.canProvideFor(id)) {
      LOGGER.debug("ConfigMappingImpl[{}].rebind({}, {}) - bound", id, old, provider);
      providers.add(provider);
      updated = true;
    }
    return updated;
  }

  /**
   * Unbinds the specified provider from this mapping such that it no longer participates in
   * resolving mapped properties.
   *
   * @param provider the provider to remove from this mapping
   * @return <code>true</code> if the provider was already bound; <code>false</code> otherwise
   */
  boolean unbind(ConfigMappingProvider provider) {
    final boolean unbound = providers.remove(provider);

    if (unbound && LOGGER.isDebugEnabled()) {
      LOGGER.debug("ConfigMappingImpl[{}].unbind({})", id, provider);
    }
    return unbound;
  }

  boolean isAffectedBy(ConfigEvent event) {
    final boolean affected =
        Stream.of(event.addedConfigs(), event.updatedConfigs(), event.removedConfigs())
            .flatMap(Function.identity())
            .anyMatch(this::isAffectedBy);

    LOGGER.debug("ConfigMappingImpl[{}].isAffectedBy({}) = {}", id, event, affected);
    return affected;
  }

  boolean isAffectedBy(Config config) {
    final Set<String> instances = dependents.get(config.getType());

    if (instances == null) {
      LOGGER.debug(
          "ConfigMappingImpl[{}].isAffectedBy({}) = false; type not supported", id, config);
      return false;
    } else if (!(config instanceof ConfigGroup)) {
      LOGGER.debug("ConfigMappingImpl[{}].isAffectedBy({}) = true; type supported", id, config);
      return true;
    } else if (instances == ConfigMappingImpl.ALL) { // identity check
      LOGGER.debug(
          "ConfigMappingImpl[{}].isAffectedBy({}) = true; all instances supported", id, config);
      return true;
    }
    final String instanceId = ((ConfigGroup) config).getId();

    if (instances.contains(instanceId)) {
      LOGGER.debug(
          "ConfigMappingImpl[{}].isAffectedBy({}) = true; instance [{}] supported",
          id,
          config,
          instanceId);
      return true;
    }
    LOGGER.debug(
        "ConfigMappingImpl[{}].isAffectedBy({}) = false; instance [{}] not supported",
        id,
        config,
        instanceId);
    return false;
  }

  @Override
  public Map<String, Object> resolve() throws ConfigMappingException {
    LOGGER.debug("ConfigMappingImpl[{}].resolve()", id);
    final Map<String, Object> properties = resolve(id);

    LOGGER.debug("resolution = {}", properties);
    return properties;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof ConfigMappingImpl) {
      return id.equals(((ConfigMappingImpl) obj).id);
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("ConfigMappingImpl[%s, providers=%s]", id, providers);
  }

  private Map<String, Object> resolve(ConfigMapping.Id id) throws ConfigMappingException {
    final Map<String, Object> properties = new HashMap<>();

    synchronized (providers) {
      // process them from lowest priority to highest such that higher one can override
      providers.stream().map(p -> p.provide(id, config)).forEach(properties::putAll);
    }
    return properties;
  }

  /**
   * Proxy config service class use to intercept config retrieval in order to help identify what
   * this config mapping depends on.
   */
  class DependentConfigService implements ConfigService {
    private final ConfigService config;

    DependentConfigService(ConfigService config) {
      this.config = config;
    }

    @Override
    public <T extends ConfigSingleton> Optional<T> get(Class<T> clazz) {
      // insert or replace the entry with an indicator that we depend on all instances for `type`
      dependents.put(Config.getType(clazz), ConfigMappingImpl.ALL);
      return config.get(clazz);
    }

    @Override
    public <T extends ConfigGroup> Optional<T> get(Class<T> clazz, String id) {
      // insert this specific id for the given type unless we already depend on all
      dependents.compute(
          Config.getType(clazz),
          (t, set) -> {
            if (set == ConfigMappingImpl.ALL) { // identity check
              // this type is dependent on all instances so nothing to do
              return set;
            }
            if (set == null) {
              set = new ConcurrentSkipListSet<>();
            }
            if (id != ConfigMappingImpl.ANY) { // identity check
              set.add(id);
            } // else - id used during first resolution when none defined; don't cache it
            return set;
          });
      return config.get(clazz, id);
    }

    @Override
    public <T extends ConfigGroup> Stream<T> configs(Class<T> type) {
      // insert or replace the entry with an indicator that we depend on all instances for `type`
      // even though we might only care about a subclass of the actual config object type, we will
      // still mark the whole config object type with ALL since we cannot validate which actual
      // subclasses might come in later - at worst, we might recompute more often then required
      dependents.put(Config.getType(type), ConfigMappingImpl.ALL);
      return config.configs(type);
    }
  }
}
