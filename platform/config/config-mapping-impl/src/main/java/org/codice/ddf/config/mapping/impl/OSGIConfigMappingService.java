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

import java.io.Closeable;
import java.util.List;
import org.codice.ddf.config.ConfigService;
import org.codice.ddf.config.mapping.ConfigMappingListener;
import org.codice.ddf.config.mapping.ConfigMappingProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSGIConfigMappingService extends ConfigMappingServiceImpl implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(OSGIConfigMappingService.class);

  private final ServiceTracker<ConfigMappingProvider, OSGIConfigMappingProvider> tracker;

  public OSGIConfigMappingService(ConfigService config, List<ConfigMappingListener> listeners) {
    super(config, listeners);
    this.tracker =
        new ServiceTracker<>(getBundleContext(), ConfigMappingProvider.class, new Customizer());
  }

  @SuppressWarnings("unused" /* called by blueprint */)
  public void init() {
    LOGGER.debug("OSGIConfigMappingService:init()");
    try {
      tracker.open();
    } finally {
      LOGGER.debug("OSGIConfigMappingService:init() - done");
    }
  }

  @Override
  public void close() {
    LOGGER.debug("OSGIConfigMappingService:close()");
    tracker.close();
  }

  BundleContext getBundleContext() {
    final Bundle bundle = FrameworkUtil.getBundle(OSGIConfigMappingService.class);

    if (bundle != null) {
      return bundle.getBundleContext();
    }
    throw new IllegalStateException("missing bundle for ConfigMappingServiceImpl");
  }

  class Customizer
      implements ServiceTrackerCustomizer<ConfigMappingProvider, OSGIConfigMappingProvider> {
    @Override
    public OSGIConfigMappingProvider addingService(ServiceReference<ConfigMappingProvider> ref) {
      final OSGIConfigMappingProvider provider =
          new OSGIConfigMappingProvider(getBundleContext(), ref);

      LOGGER.debug("adding OSGI provider: {}", provider);
      bind(provider);
      return provider;
    }

    @Override
    public void modifiedService(
        ServiceReference<ConfigMappingProvider> ref, OSGIConfigMappingProvider provider) {
      LOGGER.debug("updating OSGI provider: {}", provider);
      // update its service properties and rebind it
      provider.reinit();
      bind(provider);
    }

    @Override
    public void removedService(
        ServiceReference<ConfigMappingProvider> ref, OSGIConfigMappingProvider provider) {
      LOGGER.debug("removing OSGI provider: {}", provider);
      unbind(provider);
      provider.close();
    }
  }
}
