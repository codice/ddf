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
import org.apache.felix.cm.PersistenceManager;
import org.codice.felix.cm.internal.ConfigurationContextFactory;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The {@link DelegatingPersistenceManager} is responsible for iterating over relevant configuration
 * plugin points prior to moving the configs to the inner {@link PersistenceManager}.
 */
public class DelegatingPersistenceManager extends WrappedPersistenceManager {

  private final ServiceTracker configPersistenceTracker;

  private final ServiceRegistration<ConfigurationContextFactory> contextFactoryRegistration;

  public DelegatingPersistenceManager(PersistenceManager persistenceManager) {
    this(
        persistenceManager,
        new ServiceTracker(getBundleContext(), ConfigurationPersistencePlugin.class, null),
        getBundleContext()
            .registerService(
                ConfigurationContextFactory.class, new ConfigurationContextFactoryImpl(), null));
  }

  DelegatingPersistenceManager(
      PersistenceManager persistenceManager,
      ServiceTracker tracker,
      ServiceRegistration<ConfigurationContextFactory> registration) {
    super(persistenceManager);
    tracker.open();

    this.configPersistenceTracker = tracker;
    this.contextFactoryRegistration = registration;
  }

  @Override
  public void close() throws Exception {
    configPersistenceTracker.close();
    if (contextFactoryRegistration != null) {
      contextFactoryRegistration.unregister();
    }
    super.close();
  }

  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    ConfigurationContextImpl context = createContext(pid, properties);
    if (context.shouldBeVisibleForProcessing()) {
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
}
