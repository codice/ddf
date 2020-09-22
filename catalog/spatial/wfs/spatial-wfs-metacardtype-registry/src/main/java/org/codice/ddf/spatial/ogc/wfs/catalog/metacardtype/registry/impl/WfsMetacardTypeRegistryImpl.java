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
package org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.impl;

import com.google.common.base.Verify;
import ddf.catalog.data.MetacardType;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public final class WfsMetacardTypeRegistryImpl implements WfsMetacardTypeRegistry {

  public static final String FEATURE_NAME = "feature-name";

  public static final String SOURCE_ID = "source-id";

  private BundleContext bundleContext;

  private List<ServiceRegistration<MetacardType>> serviceRegistrations;

  private final Lock registryLock = new ReentrantLock();

  public WfsMetacardTypeRegistryImpl(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
    this.serviceRegistrations = new ArrayList<>();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<MetacardType> lookupMetacardTypeBySimpleName(String sourceId, String simpleName) {

    if (sourceId == null || simpleName == null) {
      return Optional.empty();
    }

    try {
      // acquire a lock to protect read access on serviceRegistrations
      registryLock.lock();
      Optional<ServiceRegistration<MetacardType>> serviceRegistrationOptional =
          serviceRegistrations.stream()
              .filter(s -> s.getReference().getProperty(SOURCE_ID) != null)
              .filter(s -> s.getReference().getProperty(SOURCE_ID).equals(sourceId))
              .filter(s -> s.getReference().getProperty(FEATURE_NAME) != null)
              .filter(s -> s.getReference().getProperty(FEATURE_NAME).equals(simpleName))
              .findFirst();

      if (serviceRegistrationOptional.isPresent()) {
        MetacardType featureMetacardType =
            bundleContext.getService(serviceRegistrationOptional.get().getReference());
        return Optional.of(featureMetacardType);
      }
    } finally {
      registryLock.unlock();
    }

    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  public void registerMetacardType(
      MetacardType metacardType, String sourceId, String featureSimpleName) {

    Verify.verifyNotNull(metacardType, "argument 'metacardType' may not be null.");
    Verify.verifyNotNull(sourceId, "argument 'sourceId' may not be null.");
    Verify.verifyNotNull(featureSimpleName, "argument 'featureSimpleName' may not be null.");

    Dictionary<String, String> properties = new DictionaryMap<>(2);
    properties.put(SOURCE_ID, sourceId);
    properties.put(FEATURE_NAME, featureSimpleName);
    ServiceRegistration<MetacardType> serviceRegistration =
        bundleContext.registerService(MetacardType.class, metacardType, properties);
    try {
      registryLock.lock();
      serviceRegistrations.add(serviceRegistration);
    } finally {
      registryLock.unlock();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void clear(String sourceId) {
    Verify.verifyNotNull(sourceId, "argument 'sourceId' may not be null.");
    try {
      registryLock.lock();
      for (Iterator<ServiceRegistration<MetacardType>> iter = serviceRegistrations.iterator();
          iter.hasNext(); ) {
        ServiceRegistration registration = iter.next();
        if (registration.getReference() != null
            && registration.getReference().getProperty(SOURCE_ID).equals(sourceId)) {
          registration.unregister();
          iter.remove();
        }
      }
    } finally {
      registryLock.unlock();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void clearAll() {
    try {
      registryLock.lock();
      serviceRegistrations.stream().forEach(ServiceRegistration::unregister);
      serviceRegistrations.clear();
    } finally {
      registryLock.unlock();
    }
  }
}
