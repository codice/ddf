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
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.codice.ddf.config.ConfigService;
import org.codice.ddf.config.mapping.ConfigMapping;
import org.codice.ddf.config.mapping.ConfigMapping.Id;
import org.codice.ddf.config.mapping.ConfigMappingException;
import org.codice.ddf.config.mapping.ConfigMappingProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class OSGIConfigMappingProvider implements ConfigMappingProvider, Closeable {
  private final BundleContext context;

  private final Object lock = new Object();

  private final ServiceReference<ConfigMappingProvider> reference;

  private final ConfigMappingProvider provider;

  private final AtomicInteger ranking;

  OSGIConfigMappingProvider(BundleContext context, ServiceReference<ConfigMappingProvider> ref) {
    this.context = context;
    this.reference = ref;
    this.provider = context.getService(ref);
    this.ranking =
        new AtomicInteger(OSGIConfigMappingProvider.getInteger(ref, Constants.SERVICE_RANKING));
  }

  @Override
  public void close() {
    context.ungetService(reference);
  }

  void reinit() {
    ranking.set(OSGIConfigMappingProvider.getInteger(reference, Constants.SERVICE_RANKING));
  }

  @Override
  public int getRanking() {
    return ranking.get();
  }

  @Override
  public boolean isPartial() {
    return provider.isPartial();
  }

  @Override
  public boolean canProvideFor(ConfigMapping mapping) {
    return provider.canProvideFor(mapping);
  }

  @Override
  public boolean canProvideFor(Id id) {
    return provider.canProvideFor(id);
  }

  @Override
  public Map<String, Object> provide(Id id, ConfigService config) throws ConfigMappingException {
    return provider.provide(id, config);
  }

  @Override
  public int compareTo(ConfigMappingProvider provider) {
    synchronized (lock) {
      if (provider instanceof OSGIConfigMappingProvider) {
        // rely on service reference comparison which will use ranking and service order
        return reference.compareTo(((OSGIConfigMappingProvider) provider).reference);
      }
      return Integer.compare(getRanking(), provider.getRanking());
    }
  }

  @Override
  public String toString() {
    return "OSGIConfigMappingProvider[ranking="
        + ranking
        + ", provider="
        + provider
        + ", reference="
        + reference
        + "]";
  }

  private static Set<String> getStringPlus(ServiceReference<?> ref, String propertyName) {
    final Set<String> set = new HashSet<>();
    final Object prop = ref.getProperty(propertyName);

    if (prop instanceof String) {
      set.add((String) prop);
    } else if (prop instanceof Collection) {
      ((Collection<?>) prop).stream().map(String::valueOf).forEach(set::add);
    } else if ((prop != null) && prop.getClass().isArray()) {
      final int length = Array.getLength(prop);

      for (int i = 0; i < length; i++) {
        set.add(String.valueOf(Array.get(prop, i)));
      }
    } // else - unsupported type or null so return empty set
    return set;
  }

  private static int getInteger(ServiceReference<?> ref, String propertyName) {
    final Object prop = ref.getProperty(propertyName);

    if (prop instanceof Number) {
      return ((Number) prop).intValue();
    }
    return 0;
  }
}
