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
package org.codice.ddf.admin.configurator.impl;

import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.internal.admin.configurator.actions.BundleActions;
import org.codice.ddf.internal.admin.configurator.actions.ConfiguratorSuite;
import org.codice.ddf.internal.admin.configurator.actions.FeatureActions;
import org.codice.ddf.internal.admin.configurator.actions.ManagedServiceActions;
import org.codice.ddf.internal.admin.configurator.actions.PropertyActions;
import org.codice.ddf.internal.admin.configurator.actions.ServiceActions;
import org.codice.ddf.internal.admin.configurator.actions.ServiceReader;

/** Provides access to a ConfiguratorFactory service and a collection of Action services */
public class ConfiguratorSuiteImpl implements ConfiguratorSuite {
  private final ConfiguratorFactory configuratorFactory;

  private final BundleActions bundleActions;

  private final FeatureActions featureActions;

  private final ManagedServiceActions managedServiceActions;

  private final PropertyActions propertyActions;

  private final ServiceActions serviceActions;

  private final ServiceReader serviceReader;

  public ConfiguratorSuiteImpl(
      ConfiguratorFactory configuratorFactory,
      BundleActions bundleActions,
      FeatureActions featureActions,
      ManagedServiceActions managedServiceActions,
      PropertyActions propertyActions,
      ServiceActions serviceActions,
      ServiceReader serviceReader) {
    this.configuratorFactory = configuratorFactory;
    this.bundleActions = bundleActions;
    this.featureActions = featureActions;
    this.managedServiceActions = managedServiceActions;
    this.propertyActions = propertyActions;
    this.serviceActions = serviceActions;
    this.serviceReader = serviceReader;
  }

  @Override
  public ConfiguratorFactory getConfiguratorFactory() {
    return configuratorFactory;
  }

  @Override
  public BundleActions getBundleActions() {
    return bundleActions;
  }

  @Override
  public FeatureActions getFeatureActions() {
    return featureActions;
  }

  @Override
  public ManagedServiceActions getManagedServiceActions() {
    return managedServiceActions;
  }

  @Override
  public PropertyActions getPropertyActions() {
    return propertyActions;
  }

  @Override
  public ServiceActions getServiceActions() {
    return serviceActions;
  }

  @Override
  public ServiceReader getServiceReader() {
    return serviceReader;
  }
}
