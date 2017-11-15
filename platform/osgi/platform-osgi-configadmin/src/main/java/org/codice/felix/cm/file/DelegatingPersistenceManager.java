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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import org.apache.felix.cm.PersistenceManager;
import org.codice.felix.cm.internal.ConfigurationContextFactory;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The {@link DelegatingPersistenceManager} is responsible for iterating over relevant configuration
 * plugin points prior to moving the configs to the inner {@link PersistenceManager}.
 *
 * <p><b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to
 * maven bundle plugin 2.3.7</b>
 */
public class DelegatingPersistenceManager extends WrappedPersistenceManager {
  private final ServiceTracker<ConfigurationPersistencePlugin, ConfigurationPersistencePlugin>
      configPersistenceTracker;

  public DelegatingPersistenceManager(
      PersistenceManager persistenceManager, StampedLock configDeadZoneLock) {
    this(
        persistenceManager,
        new ServiceTracker<>(
            getBundleContext(),
            ConfigurationPersistencePlugin.class,
            new ConfigInterceptingCustomizer(configDeadZoneLock)));
  }

  DelegatingPersistenceManager(
      PersistenceManager persistenceManager,
      ServiceTracker<ConfigurationPersistencePlugin, ConfigurationPersistencePlugin> tracker) {
    super(persistenceManager);
    this.configPersistenceTracker = tracker;
    configPersistenceTracker.open();
  }

  @Override
  public void close() throws Exception {
    configPersistenceTracker.close();
    super.close();
  }

  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    ConfigurationContextImpl context = createContext(pid, properties);
    if (context.shouldBeVisibleToPlugins()) {
      List<ConfigurationPersistencePlugin> plugins =
          asPlugins(configPersistenceTracker.getServices());
      for (ConfigurationPersistencePlugin plugin : plugins) {
        plugin.handleStore(context);
      }
    }
    super.store(pid, properties);
  }

  @Override
  public void delete(String pid) throws IOException {
    List<ConfigurationPersistencePlugin> plugins =
        asPlugins(configPersistenceTracker.getServices());
    for (ConfigurationPersistencePlugin plugin : plugins) {
      plugin.handleDelete(pid);
    }
    super.delete(pid);
  }

  /* Factory method visible for testing purposes only */
  ConfigurationContextImpl createContext(String pid, Dictionary props) {
    return new ConfigurationContextImpl(pid, props);
  }

  private static List<ConfigurationPersistencePlugin> asPlugins(Object[] serviceObjects) {
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

  private static class ConfigInterceptingCustomizer
      implements ServiceTrackerCustomizer<
          ConfigurationPersistencePlugin, ConfigurationPersistencePlugin> {
    private final ConfigurationContextFactory factory = new ConfigurationContextFactoryImpl();
    private final StampedLock configDeadZoneLock;

    ConfigInterceptingCustomizer(StampedLock configDeadZoneLock) {
      this.configDeadZoneLock = configDeadZoneLock;
    }

    @Override
    public ConfigurationPersistencePlugin addingService(
        ServiceReference<ConfigurationPersistencePlugin> serviceReference) {
      ConfigurationPersistencePlugin plugin = getBundleContext().getService(serviceReference);
      // Write lock because no configs should be getting saved or deleted while we initialize
      long stamp = configDeadZoneLock.writeLock();
      try {
        plugin.initialize(factory);
      } finally {
        configDeadZoneLock.unlockWrite(stamp);
      }
      return plugin;
    }

    @Override
    public void modifiedService(
        ServiceReference<ConfigurationPersistencePlugin> serviceReference,
        ConfigurationPersistencePlugin configurationPersistencePlugin) {
      // Listener for modified service properties. Does not apply to us. No customization required.
    }

    @Override
    public void removedService(
        ServiceReference<ConfigurationPersistencePlugin> serviceReference,
        ConfigurationPersistencePlugin configurationPersistencePlugin) {
      // Listener for service removal. Does not apply to us. No customization required.
    }
  }
}
