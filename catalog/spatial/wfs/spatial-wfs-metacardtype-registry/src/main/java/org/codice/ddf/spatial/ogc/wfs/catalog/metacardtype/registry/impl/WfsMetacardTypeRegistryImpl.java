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
import java.util.List;
import java.util.Optional;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public final class WfsMetacardTypeRegistryImpl implements WfsMetacardTypeRegistry {

  public static final String FEATURE_NAME = "feature-name";

  public static final String SOURCE_ID = "source-id";

  private BundleContext bundleContext;

  private List<ServiceRegistration<MetacardType>> serviceRegistrations;

  public WfsMetacardTypeRegistryImpl(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
    this.serviceRegistrations = new ArrayList<>();
  }

  /** {@inheritDoc} */
  public Optional<MetacardType> lookupMetacardTypeBySimpleName(String sourceId, String simpleName) {

    if (sourceId == null || simpleName == null) {
      return Optional.empty();
    }

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

    return Optional.empty();
  }

  /** {@inheritDoc} */
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
    serviceRegistrations.add(serviceRegistration);
  }

  /** {@inheritDoc} */
  public void clear() {
    serviceRegistrations.stream().forEach(ServiceRegistration::unregister);
    serviceRegistrations.clear();
  }
}
