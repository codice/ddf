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
package org.codice.ddf.config.agent.osgi;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.lang.ArrayUtils;
import org.codice.ddf.config.ConfigEvent;
import org.codice.ddf.config.ConfigGroup;
import org.codice.ddf.config.ConfigListener;
import org.codice.ddf.config.ConfigService;
import org.codice.ddf.config.mapping.ConfigMapping;
import org.codice.ddf.config.mapping.ConfigMappingEvent;
import org.codice.ddf.config.mapping.ConfigMappingEvent.Type;
import org.codice.ddf.config.mapping.ConfigMappingException;
import org.codice.ddf.config.mapping.ConfigMappingListener;
import org.codice.ddf.config.mapping.ConfigMappingService;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigAdminAgent
    implements SynchronousConfigurationListener,
        ServiceListener,
        ConfigMappingListener,
        ConfigListener,
        Closeable {
  public static final String INSTANCE_KEY = "org.codice.ddf.config.instance";

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigAdminAgent.class);

  private static final int THREAD_POOL_DEFAULT_SIZE = 1;

  private static final ExecutorService EXECUTOR = ConfigAdminAgent.createExecutor();

  private final ConfigurationAdmin configAdmin;

  private final ConfigMappingService mapper;

  private final ConfigService config;

  // keyed by config group type with the corresponding sets of factory pids to create objects for
  private final Map<Class<? extends ConfigGroup>, Set<String>> factories = new HashMap<>();

  private final Map<String, Dictionary<String, Object>> cache = new ConcurrentHashMap<>();

  private final Object lock = new Object();

  public ConfigAdminAgent(
      ConfigurationAdmin configAdmin, ConfigMappingService mapper, ConfigService config) {
    this.configAdmin = configAdmin;
    this.mapper = mapper;
    this.config = config;
  }

  @SuppressWarnings("unused" /* called by blueprint */)
  public void init() {
    LOGGER.debug("ConfigAdminAgent:init()");
    try {
      final BundleContext context = getBundleContext();

      // start by registering a service listener
      context.addServiceListener(this);
      // then process all existing config objects but do that on a separate thread to not delay
      // the initialization
      ConfigAdminAgent.EXECUTOR.execute(this::initConfigurations);
      // finally process all registered services for the PIDs they identify on a separate thread
      // also
      ConfigAdminAgent.EXECUTOR.execute(this::initServices);
    } finally {
      LOGGER.debug("ConfigAdminAgent:init() - done");
    }
  }

  private void initConfigurations() {
    LOGGER.debug("ConfigAdminAgent:initConfigurations()");
    try {
      configurations().forEach(this::updateConfiguration);
    } catch (IOException | ConfigMappingException e) { // ignore
      LOGGER.error("failed to initialize existing config objects: {}", e.getMessage());
      LOGGER.debug("initialization failure: {}", e, e);
    } finally {
      LOGGER.debug("ConfigAdminAgent:initConfigurations() - done");
    }
  }

  private void initServices() {
    LOGGER.debug("ConfigAdminAgent:initServices()");
    final BundleContext context = getBundleContext();

    try {
      ConfigAdminAgent.serviceReferences(context)
          .flatMap(ConfigAdminAgent::servicePids)
          .map(mapper::getMapping)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .forEach(this::updateConfigObjectFor);
    } catch (ConfigMappingException e) { // ignore
      LOGGER.error("failed to initialize registered services: {}", e.getMessage());
      LOGGER.debug("initialization failure: {}", e, e);
    } finally {
      LOGGER.debug("ConfigAdminAgent:initServices() - done");
    }
  }

  @Override
  public void close() {
    LOGGER.debug("ConfigAdminAgent::close()");
    final BundleContext context = getBundleContext();

    context.removeServiceListener(this);
  }

  @SuppressWarnings("unused" /* called from blueprint */)
  public void setFactories(Map<Class<? extends ConfigGroup>, Set<String>> factories) {
    synchronized (lock) {
      LOGGER.debug("ConfigAdminAgent:setFactories({})", factories);
      // determine which factory pids are no longer referenced and remove all corresponding managed
      // service factories
      final Set<String> newFactoryPids =
          factories.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

      this.factories
          .values()
          .stream()
          .flatMap(Set::stream)
          .filter(((Predicate<Object>) newFactoryPids::contains).negate())
          .forEach(this::removeAllConfigObjectsFor);
      this.factories.clear();
      this.factories.putAll(factories);
      // process all configured config instances we have to monitor
      factories.forEach(this::findConfigMappingsFor);
    }
  }

  @Override
  public void serviceChanged(ServiceEvent event) {
    synchronized (lock) {
      final ServiceReference<?> ref = event.getServiceReference();
      final int type = event.getType();
      // DDF_Custom_Mime_Type_Resolver.b61903e6-59cb-4394-8060-03e803c56652
      LOGGER.debug("ConfigAdminAgent::serviceChanged() - type = [{}], service = [{}]", type, ref);
      if ((type == ServiceEvent.REGISTERED) || (type == ServiceEvent.MODIFIED)) {
        // filter the pids that already exist in config admin as those would have alreay been
        // processed
        ConfigAdminAgent.servicePids(ref)
            .filter(this::configurationDoesNotExist)
            .map(mapper::getMapping)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(this::updateConfigObjectFor);
        LOGGER.debug("done processing all PIDs for service: {}", ref);
      } else {
        LOGGER.debug("ignoring event [{}] for service [{}]", type, ref);
      }
    }
  }

  @Override
  public void configurationEvent(ConfigurationEvent event) {
    synchronized (lock) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "ConfigAdminAgent:configurationEvent(type={}, pid={}, factoryPid={})",
            event.getType(),
            event.getPid(),
            event.getFactoryPid());
      }
      final String pid = event.getPid();

      try {
        switch (event.getType()) {
          case ConfigurationEvent.CM_UPDATED:
          case ConfigurationEvent.CM_LOCATION_CHANGED:
            final ConfigurationAdmin cfgAdmin = getService(event.getReference());
            final Configuration cfg = ConfigAdminAgent.getConfiguration(cfgAdmin, event.getPid());

            if (cfg != null) {
              updateConfiguration(cfg);
            }
            break;
          case ConfigurationEvent.CM_DELETED:
            cache.remove(pid);
            return;
          default:
            return;
        }
      } catch (InvalidSyntaxException | IOException | ConfigMappingException e) { // ignore
        LOGGER.warn("failed to process event for config object '{}': {}", pid, e.getMessage());
        LOGGER.debug("config event failure: {}", e, e);
      }
    }
  }

  @Override
  public void mappingChanged(ConfigMappingEvent event) {
    synchronized (lock) {
      LOGGER.debug("ConfigAdminAgent:mappingChanged({})", event);
      if (event.getType() == Type.REMOVED) {
        removeConfigObjectFor(event.getMapping());
      } else {
        updateConfigObjectFor(event.getMapping());
      }
    }
  }

  @Override
  public void configChanged(ConfigEvent event) {
    synchronized (lock) {
      LOGGER.debug("ConfigAdminAgent:configChanged({})", event);
      // only check updates for config instances that maps to factory pids we are monitoring
      // start with config instances that were removed
      event
          .removedConfigs()
          .filter(ConfigGroup.class::isInstance)
          .map(ConfigGroup.class::cast)
          .forEach(this::removeConfigObjectFor);
      // handle new and updated ones the same way to make sure we have corresponding cfg objects if
      // for whatever reasons we had missed them
      // for updates of existing mappings, these will be handled by the mapper and we will get
      // notified if there is any changes, what we care about here is simply to detect those for
      // which
      // we do not have a corresponding config object
      Stream.concat(event.addedConfigs(), event.updatedConfigs())
          .filter(ConfigGroup.class::isInstance)
          .map(ConfigGroup.class::cast)
          .forEach(this::findConfigMappingFor);
    }
  }

  BundleContext getBundleContext() {
    final Bundle bundle = FrameworkUtil.getBundle(ConfigAdminAgent.class);

    if (bundle != null) {
      return bundle.getBundleContext();
    }
    throw new IllegalStateException("missing bundle for ConfigAdminAgent");
  }

  private void findConfigMappingsFor(Class<? extends ConfigGroup> type, Set<String> factoryPids) {
    LOGGER.debug("ConfigAdminAgent:findConfigMappingsFor({}, {})", type, factoryPids);
    config.configs(type).forEach(c -> findConfigMappingFor(c, factoryPids));
  }

  private void findConfigMappingFor(ConfigGroup cfgInstance) {
    LOGGER.debug("ConfigAdminAgent:findConfigMappingFor({})", cfgInstance);
    final Set<String> factoryPids = factories.get(cfgInstance.getType());

    if (factoryPids == null) { // not monitoring those so ignore
      return;
    }
    findConfigMappingFor(cfgInstance, factoryPids);
  }

  private void findConfigMappingFor(ConfigGroup cfgInstance, Set<String> factoryPids) {
    factoryPids.forEach(pid -> findConfigMappingFor(cfgInstance, pid));
  }

  private void findConfigMappingFor(ConfigGroup cfgInstance, String factoryPid) {
    final String type = cfgInstance.getType().getName();
    final String id = cfgInstance.getId();

    LOGGER.debug("ConfigAdminAgent:findConfigMappingFor({}-{}, {})", type, id, factoryPid);
    final ConfigMapping mapping = mapper.getMapping(factoryPid, id).orElse(null);

    if (mapping != null) {
      LOGGER.debug(
          "found config mapping [{}] for config instance [{}-{}]", mapping.getId(), type, id);
      updateConfigObjectFor(mapping);
    } else {
      LOGGER.debug("no config mappings found for config instance [{}-{}]", type, id);
    }
  }

  private void removeAllConfigObjectsFor(String factoryPid) {
    LOGGER.debug("ConfigAdminAgent:removeAllConfigObjectsFor({})", factoryPid);

    try {
      configurations(factoryPid).forEach(ConfigAdminAgent::deleteConfiguration);
    } catch (IOException e) { // ignore
      LOGGER.error("failed to remove config objects '{}': {}", factoryPid, e.getMessage());
      LOGGER.debug("config objects '{}' removal failure: {}", factoryPid, e, e);
    }
  }

  private void removeConfigObjectFor(ConfigGroup cfgInstance) {
    LOGGER.debug("ConfigAdminAgent:removeConfigObjectFor({})", cfgInstance);
    final Class<? extends ConfigGroup> type = cfgInstance.getType();
    final Set<String> factoryPids = factories.get(type);

    if (factoryPids == null) { // not monitoring those so ignore
      return;
    }
    factoryPids.forEach(pid -> removeConfigObjectFor(cfgInstance, pid));
  }

  private void removeConfigObjectFor(ConfigGroup cfgInstance, String factoryPid) {
    final Class<? extends ConfigGroup> type = cfgInstance.getType();
    final String typeName = type.getName();
    final String id = cfgInstance.getId();

    try {
      final Configuration cfg = getConfiguration(factoryPid, id);

      if (cfg != null) {
        deleteConfiguration(cfg);
      }
    } catch (InvalidSyntaxException | IOException e) {
      LOGGER.warn(
          "failed to remove config object for config instance '{}-{}': {}",
          typeName,
          id,
          e.getMessage());
      LOGGER.debug("config object removal failure: {}", e, e);
    }
  }

  private void removeConfigObjectFor(ConfigMapping mapping) {
    synchronized (lock) {
      LOGGER.debug("ConfigAdminAgent:removeConfigObjectFor({})", mapping);
      try {
        final String instance = mapping.getId().getInstance().orElse(null);
        final String pid = mapping.getId().getName();
        final Configuration cfg;

        if (instance != null) { // a managed service factory
          cfg = getConfiguration(pid, instance);
        } else {
          cfg = getConfiguration(pid);
        }
        if (cfg != null) {
          deleteConfiguration(cfg);
        }
      } catch (InvalidSyntaxException | ConfigMappingException | IOException e) {
        LOGGER.warn(
            "failed to update config object for config mapping '{}': {}",
            mapping.getId(),
            e.getMessage());
        LOGGER.debug("config object update failure: {}", e, e);
      }
    }
  }

  private void updateConfigObjectFor(ConfigMapping mapping) {
    synchronized (lock) {
      LOGGER.debug("ConfigAdminAgent:updateConfigObjectFor({})", mapping);
      try {
        final String instance = mapping.getId().getInstance().orElse(null);
        final String pid = mapping.getId().getName();
        final Configuration cfg;

        if (instance != null) { // a managed service factory
          cfg = getOrCreateConfiguration(pid, instance);
        } else {
          // get or create the first version
          // location as null to make sure it is bound to the first bundles that registers the
          // managed service
          cfg = configAdmin.getConfiguration(pid, null);
          LOGGER.debug("created/updated config object for pid [{}]", pid);
        }
        updateConfigurationWithMapping(cfg, ConfigAdminAgent.getProperties(cfg), mapping);
      } catch (InvalidSyntaxException | ConfigMappingException | IOException e) {
        LOGGER.warn(
            "failed to update config object for config mapping '{}': {}",
            mapping.getId(),
            e.getMessage());
        LOGGER.debug("config object update failure: {}", e, e);
      }
    }
  }

  private void updateConfiguration(Configuration cfg) {
    LOGGER.debug("ConfigAdminAgent:updateConfiguration({})", cfg);
    final String pid = cfg.getPid();
    final Dictionary<String, Object> cachedProperties = cache.get(pid);
    final Dictionary<String, Object> properties = ConfigAdminAgent.getProperties(cfg);

    if ((cachedProperties != null) && ConfigAdminAgent.equals(properties, cachedProperties)) {
      // nothing to update - properties are still the same
      return;
    }
    final String factoryPid = cfg.getFactoryPid();
    final Optional<ConfigMapping> mapping;

    if (factoryPid != null) { // see if we know its instance id
      final String instance = Objects.toString(properties.get(ConfigAdminAgent.INSTANCE_KEY), null);

      if (instance == null) {
        // we cannot handle that one specifically, check if we have mappings for the factory and
        // if we do, log this as an error since we should have an instance for the factories we
        // handled
        if (mapper.getMapping(factoryPid).isPresent()) {
          LOGGER.error(
              "unable to map managed service factory '{}'; missing instance from config object '{}'",
              factoryPid,
              pid);
        } else {
          LOGGER.debug(
              "unknown managed service factory; missing instance from config object [{}]", pid);
        }
        return;
      } else {
        LOGGER.debug(
            "found instance id from config object [{}]; handling it as [{}-{}]",
            pid,
            factoryPid,
            instance);
        mapping = mapper.getMapping(factoryPid, instance);
      }
    } else {
      mapping = mapper.getMapping(pid);
    }
    mapping.ifPresent(m -> updateConfigurationWithMapping(cfg, properties, m));
  }

  private void updateConfigurationWithMapping(
      Configuration cfg, Dictionary<String, Object> properties, ConfigMapping mapping) {
    LOGGER.debug(
        "ConfigAdminAgent:updateConfigurationWithMapping({}, {}, {})", cfg, properties, mapping);
    final String pid = cfg.getPid();

    try {
      final String instance = mapping.getId().getInstance().orElse(null);

      // compute the new mapping values
      mapping.resolve().forEach((k, v) -> ConfigAdminAgent.putOrRemove(properties, k, v));
      updateConfigurationProperties(cfg, instance, properties);
    } catch (IOException | ConfigMappingException e) {
      LOGGER.error("failed to update config object '{}': {}", pid, e.getMessage());
      LOGGER.debug("config object update failure", e);
    }
  }

  private void updateConfigurationProperties(
      Configuration cfg, @Nullable String instance, @Nullable Dictionary<String, Object> properties)
      throws IOException {
    synchronized (lock) {
      final String pid = cfg.getPid();
      final Dictionary<String, Object> cachedProperties = cache.get(pid);

      if (properties == null) { // initialize the properties
        properties = new DictionaryMap<>();
      }
      // keep the instance up to date
      if (instance != null) {
        properties.put(ConfigAdminAgent.INSTANCE_KEY, instance);
      } else {
        properties.remove(ConfigAdminAgent.INSTANCE_KEY);
      }
      // only update configAdmin if the dictionary content has changed
      if ((cachedProperties == null) || !ConfigAdminAgent.equals(cachedProperties, properties)) {
        LOGGER.debug("updating config object [{}] with: {}", pid, properties);
        Dictionary<String, Object> old = null;

        try {
          old = cache.put(pid, properties);
          cfg.update(properties);
        } catch (IOException e) {
          if (old == null) {
            cache.remove(pid);
          } else {
            cache.put(pid, old);
          }
          throw e;
        }
      }
    }
  }

  @Nullable
  private Configuration getConfiguration(String pid) throws InvalidSyntaxException, IOException {
    return ConfigAdminAgent.getConfiguration(configAdmin, pid);
  }

  @Nullable
  private Configuration getConfiguration(String factoryPid, String instance)
      throws InvalidSyntaxException, IOException {
    final Configuration[] cfgs =
        configAdmin.listConfigurations(
            String.format(
                "(&(service.factoryPid=%s)(%s=%s))",
                factoryPid,
                ConfigAdminAgent.INSTANCE_KEY,
                ConfigAdminAgent.escapeFilterValue(instance)));

    if (ArrayUtils.isNotEmpty(cfgs)) {
      LOGGER.debug("found config object '{}-{}' as {}", factoryPid, instance, cfgs[0].getPid());
      return cfgs[0];
    }
    LOGGER.debug("config object '{}-{}' not found", factoryPid, instance);
    return null;
  }

  private Configuration getOrCreateConfiguration(String factoryPid, String instance)
      throws InvalidSyntaxException, IOException {
    Configuration cfg = getConfiguration(factoryPid, instance);

    if (cfg != null) {
      return cfg;
    }
    // create the first version
    // location as null to make sure it is bound to the first bundles that registers the
    // managed service factory
    cfg = configAdmin.createFactoryConfiguration(factoryPid, null);
    LOGGER.debug(
        "created new config object [{}-{}] with pid [{}]", factoryPid, instance, cfg.getPid());
    return cfg;
  }

  private boolean configurationDoesNotExist(String pid) {
    try {
      final boolean exist = getConfiguration(pid) != null;

      LOGGER.debug("ConfigAdminAgent:configurationDoesNotExist({}) - {}", pid, !exist);
      return !exist;
    } catch (InvalidSyntaxException | IOException e) { // ignore and assume it doesn't exist
      LOGGER.debug("ConfigAdminAgent:configurationDoesNotExist({}) - true: {}", pid, e, e);
      return true;
    }
  }

  private Stream<Configuration> configurations() throws IOException {
    try {
      final Configuration[] configurations = configAdmin.listConfigurations(null);

      return (configurations != null) ? Stream.of(configurations) : Stream.empty();
    } catch (InvalidSyntaxException e) { // should never happen
      LOGGER.error("failed to retrieve existing configurations: {}", e.getMessage());
      LOGGER.debug("configuration retrieval failure: {}", e, e);
      return Stream.empty();
    }
  }

  @Nullable
  private Stream<Configuration> configurations(String factoryPid) throws IOException {
    try {
      final Configuration[] configurations =
          configAdmin.listConfigurations(
              String.format(
                  "(&(service.factoryPid=%s)(%s=*))", factoryPid, ConfigAdminAgent.INSTANCE_KEY));

      return (configurations != null) ? Stream.of(configurations) : Stream.empty();
    } catch (InvalidSyntaxException e) { // should never happen
      LOGGER.error("failed to retrieve " + factoryPid + " configurations: {}", e.getMessage());
      LOGGER.debug(factoryPid + " configuration retrieval failure: {}", e, e);
      return Stream.empty();
    }
  }

  private <S> S getService(ServiceReference<S> serviceReference) {
    return AccessController.doPrivileged(
        (PrivilegedAction<S>) () -> getBundleContext().getService(serviceReference));
  }

  private static void deleteConfiguration(Configuration cfg) {
    final String pid = cfg.getPid();

    try {
      cfg.delete(); // cache will be cleanup when we get the event back from cfg admin
      LOGGER.debug("deleted config object '{}'", pid);
    } catch (IOException e) { // ignore
      LOGGER.error("failed to remove config object '{}': {}", pid, e.getMessage());
      LOGGER.debug("config object '{}' removal failure: {}", pid, e, e);
    }
  }

  @Nullable
  private static Configuration getConfiguration(ConfigurationAdmin configAdmin, String pid)
      throws InvalidSyntaxException, IOException {
    // we use listConfigurations() to not bind the config object to our bundle if it was not bound
    // yet as we want to make sure that it will be bound to its corresponding service
    final String filter = String.format("(%s=%s)", org.osgi.framework.Constants.SERVICE_PID, pid);
    final Configuration[] configs = configAdmin.listConfigurations(filter);

    return ArrayUtils.isNotEmpty(configs) ? configs[0] : null;
  }

  private static void putOrRemove(Dictionary<String, Object> properties, String key, Object value) {
    if (value != null) {
      properties.put(key, value);
    } else {
      properties.remove(key);
    }
  }

  private static Dictionary<String, Object> getProperties(Configuration cfg) {
    final Dictionary<String, Object> properties = cfg.getProperties();

    return (properties != null) ? properties : new DictionaryMap<>();
  }

  private static boolean equals(Dictionary<String, Object> x, Dictionary<String, Object> y) {
    if (x.size() != y.size()) {
      return false;
    }
    for (final Enumeration<String> e = x.keys(); e.hasMoreElements(); ) {
      final String key = e.nextElement();

      if (!Objects.deepEquals(x.get(key), y.get(key))) {
        return false;
      }
    }
    return true;
  }

  private static String escapeFilterValue(String s) {
    return s.replaceAll("[(]", "\\\\(")
        .replaceAll("[)]", "\\\\)")
        .replaceAll("[=]", "\\\\=")
        .replaceAll("[\\*]", "\\\\*");
  }

  private static Stream<ServiceReference<?>> serviceReferences(BundleContext context) {
    try {
      final ServiceReference<?>[] refs = context.getServiceReferences((String) null, null);

      return (refs != null) ? Stream.of(refs) : Stream.empty();
    } catch (InvalidSyntaxException e) { // should never happen
      LOGGER.error("failed to retrieved existing services: {}", e.getMessage());
      LOGGER.debug("service retrieval failure: {}", e, e);
      return Stream.empty();
    }
  }

  private static Stream<String> servicePids(ServiceReference<?> ref) {
    final Object prop = ref.getProperty(Constants.SERVICE_PID);

    if (prop instanceof String) {
      LOGGER.debug("found service [{}] PID: {}", ref, prop);
      return Stream.of((String) prop);
    } else if (prop instanceof Collection) {
      LOGGER.debug("found service [{}] PIDs: {}", ref, prop);
      return ((Collection<?>) prop).stream().map(String::valueOf);
    } else if ((prop != null) && prop.getClass().isArray()) {
      final int length = Array.getLength(prop);

      return StreamSupport.stream(
          Spliterators.spliterator(
              new Iterator<String>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                  return i < length;
                }

                @Override
                public String next() {
                  if (!hasNext()) {
                    throw new NoSuchElementException();
                  }
                  final String s = String.valueOf(Array.get(prop, i++));

                  LOGGER.debug("found service [{}] PID: {}", ref, s);
                  return s;
                }

                @Override
                public void remove() {
                  throw new UnsupportedOperationException();
                }
              },
              length,
              Spliterator.ORDERED),
          false);
    } // else - unsupported type or null so return empty stream
    return Stream.empty();
  }

  private static ExecutorService createExecutor() throws NumberFormatException {
    return Executors.newFixedThreadPool(
        ConfigAdminAgent.THREAD_POOL_DEFAULT_SIZE,
        StandardThreadFactoryBuilder.newThreadFactory("ConfigAdminAgent"));
  }
}
