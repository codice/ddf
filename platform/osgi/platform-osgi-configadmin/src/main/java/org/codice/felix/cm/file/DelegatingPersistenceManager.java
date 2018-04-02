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
package org.codice.felix.cm.file;

import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import org.apache.felix.cm.PersistenceManager;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.codice.felix.cm.internal.ConfigurationInitializable;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.codice.felix.cm.internal.ConfigurationStoragePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DelegatingPersistenceManager} is responsible for iterating over relevant configuration
 * plugin points prior to moving the configs to the inner {@link PersistenceManager}.
 *
 * <p>Read locks are maintained during configuration persistence and removal so that initializing
 * plugins do not miss configuration data.
 *
 * <p>By implementing {@link ServiceTrackerCustomizer} callbacks are provided for plugin
 * initialization to occur. The implementation is expressed in an inner class {@link
 * PluginTrackerCustomizer}.
 *
 * <p>Plugin initialization delivers the list of all known configurations to the plugins so they can
 * prepare for future updates and deletions. The {@code serviceStartingLock} ensures no additional
 * configuration is persisted or removed while configuration plugins are being initialized, the
 * consequences of which could mean plugins miss configuration and fall out of sync with the data
 * cache.
 *
 * <p>For plugin initialization, we read configuration data from the inner persistence manager
 * directly to avoid lock contention with the caching layer that Felix adds in the {@link
 * org.apache.felix.cm.impl.ConfigurationManager}. This means reading all the config admin
 * configuration data from disk every time a {@link ConfigurationPersistencePlugin} needs to
 * initialize.
 *
 * <p>Also relevant: https://github.com/codice/ddf/pull/2523#discussion_r150092423
 *
 * <p><b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to
 * maven bundle plugin 2.3.7</b>
 */
public class DelegatingPersistenceManager extends WrappedPersistenceManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingPersistenceManager.class);

  private final ServiceTracker<ConfigurationInitializable, ConfigurationInitializable>
      configPluginTracker;

  private final StampedLock serviceStartingLock = new StampedLock();

  public DelegatingPersistenceManager(PersistenceManager persistenceManager) {
    super(persistenceManager);
    this.configPluginTracker =
        new ServiceTracker<>(
            getBundleContext(), ConfigurationInitializable.class, new PluginTrackerCustomizer());
    configPluginTracker.open();
  }

  DelegatingPersistenceManager(
      PersistenceManager persistenceManager,
      ServiceTracker<ConfigurationInitializable, ConfigurationInitializable> tracker) {
    super(persistenceManager);
    this.configPluginTracker = tracker;
    configPluginTracker.open();
  }

  @Override
  public void close() throws Exception {
    configPluginTracker.close();
    super.close();
  }

  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    // Read lock to ensure plugin initialization is not currently occuring
    long stamp = serviceStartingLock.readLock();
    try {
      ConfigurationContextImpl context = createContext(pid, properties);
      if (context.shouldBeVisibleToPlugins()) {
        List<ConfigurationPersistencePlugin> plugins =
            asPersistencePlugins(configPluginTracker.getServices());
        for (ConfigurationPersistencePlugin plugin : plugins) {
          plugin.handleStore(context);
        }
      }
      super.store(pid, properties);
    } finally {
      serviceStartingLock.unlockRead(stamp);
    }
  }

  @Override
  public void delete(String pid) throws IOException {
    // Read lock to ensure plugin initialization is not currently occuring
    long stamp = serviceStartingLock.readLock();
    try {
      List<ConfigurationPersistencePlugin> plugins =
          asPersistencePlugins(configPluginTracker.getServices());
      for (ConfigurationPersistencePlugin plugin : plugins) {
        plugin.handleDelete(pid);
      }
      super.delete(pid);
    } finally {
      serviceStartingLock.unlockRead(stamp);
    }
  }

  /* Factory method visible for testing purposes - to inject a mock for the context */
  ConfigurationContextImpl createContext(String pid, Dictionary props) {
    return new ConfigurationContextImpl(pid, props);
  }

  /* Factory method visible for testing purposes - to inject a mock for the plugin */
  ConfigurationInitializable retrieveServiceObject(
      ServiceReference<ConfigurationInitializable> serviceReference) {
    return FrameworkUtil.getBundle(DelegatingPersistenceManager.class)
        .getBundleContext()
        .getService(serviceReference);
  }

  @SuppressWarnings(
      "squid:S3398" /* maintain proper lock encapsulation - inner class should call us */)
  private ConfigurationInitializable handleServiceAdded(
      ServiceReference<ConfigurationInitializable> serviceReference) {
    ConfigurationInitializable plugin = retrieveServiceObject(serviceReference);
    // Write lock because no configs should be getting saved or deleted while we initialize
    long stamp = serviceStartingLock.writeLock();
    try {
      // This enumeration is lazily reading all config admin configuration from disk (bundle cache)
      Enumeration<Dictionary<String, Object>> dictionaries =
          getInnerPersistenceManager().getDictionaries();
      Set<ConfigurationContext> configs = new HashSet<>();
      while (dictionaries.hasMoreElements()) {
        Dictionary<String, Object> props = dictionaries.nextElement();
        ConfigurationContextImpl context =
            new ConfigurationContextImpl((String) props.get(SERVICE_PID), props);
        if (context.shouldBeVisibleToPlugins()) {
          configs.add(context);
        }
      }
      plugin.initialize(configs);
    } catch (IOException e) {
      LOGGER.error(
          "{}. {}. Bundle {} [id = {}] requires a restart.",
          e.getMessage(),
          "This is a problem reading the bundle cache to fetch the current configuration",
          serviceReference.getBundle().getSymbolicName(),
          serviceReference.getBundle().getBundleId());
      LOGGER.debug("Problem reading bundle cache.", e);
      plugin.initialize(Collections.emptySet());
    } finally {
      if (plugin instanceof ConfigurationStoragePlugin) {
        setStoragePlugin((ConfigurationStoragePlugin) plugin);
      }
      serviceStartingLock.unlockWrite(stamp);
    }
    return plugin;
  }

  private static List<ConfigurationPersistencePlugin> asPersistencePlugins(
      Object[] serviceObjects) {
    if (serviceObjects == null) {
      return Collections.emptyList();
    }
    List<ConfigurationPersistencePlugin> plugins = new ArrayList<>(serviceObjects.length);
    for (Object object : serviceObjects) {
      if (object instanceof ConfigurationPersistencePlugin) {
        plugins.add((ConfigurationPersistencePlugin) object);
      }
    }
    return plugins;
  }

  private static BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(DelegatingPersistenceManager.class).getBundleContext();
  }

  /**
   * Service callbacks.
   *
   * <p>Implemented as an inner class to avoid passing the `this` pointer around prior to it being
   * fully constructed, which would be necessary if {@link DelegatingPersistenceManager} implemented
   * {@link ServiceTrackerCustomizer}.
   */
  class PluginTrackerCustomizer
      implements ServiceTrackerCustomizer<ConfigurationInitializable, ConfigurationInitializable> {

    @Override
    public ConfigurationInitializable addingService(
        ServiceReference<ConfigurationInitializable> serviceReference) {
      return handleServiceAdded(serviceReference);
    }

    @Override
    public void modifiedService(
        ServiceReference<ConfigurationInitializable> serviceReference,
        ConfigurationInitializable configurationPersistencePlugin) {
      // Listener for modified service properties. Does not apply to us. No customization required.
    }

    @Override
    public void removedService(
        ServiceReference<ConfigurationInitializable> serviceReference,
        ConfigurationInitializable configurationPersistencePlugin) {
      // Listener for service removal. Does not apply to us. No customization required.
    }
  }
}
