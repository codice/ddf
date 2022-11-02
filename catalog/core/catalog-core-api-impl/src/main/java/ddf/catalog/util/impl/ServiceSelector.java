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
package ddf.catalog.util.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ServiceSelector maintains a sorted set of bound OSGi services and provides user access to
 * those services. It covers 2 use-cases: point-to-point and pub-sub.
 *
 * <p>In the point-to-point scenario a user needs to select a single service from the set of
 * available services. The appropriate service to select is determined by the
 * ServiceSelectionStrategy implementation that is provided to the ServiceSelector constructor. It
 * can be retrieved by calling the 'getService()' method.
 *
 * <p>In the pub-sub scenario, the user needs to access all of the service implementations in the
 * internal set. An unmodifiable view of the internal set can be retrieved by a call to
 * 'getAllServices()'.
 *
 * <p>In both scenarios, the set order is determined by the Comparator object supplied to the
 * ServiceSelector at creation.
 *
 * <p>OSGi requires a type converter to be registered for this class. Typically, it will also be
 * used a reference-listener.
 *
 * <p>Example:
 *
 * <pre>
 * {@Code
 *
 *    <type-converters>
 *      <bean id="serviceSelectorConverter" class="ddf.catalog.util.impl.ServiceSelectorConverter"/>
 *    </type-converters>
 *
 *    <bean id="geoCoderFactory" class="ddf.catalog.util.impl.ServiceSelector"/>
 *
 *    <reference-list id="geoCoderList" interface="org.codice.ddf.spatial.geocoder.GeoCoder"
 *                    availability="optional">
 *
 *      <reference-listener bind-method="bindService" unbind-method="unbindService"
 *                          ref="geoCoderFactory"/>
 *    </reference-list>
 *
 * }
 * </pre>
 *
 * @param <T> - The type of the service to be served up by this implementation of ServiceSelector
 *     {@link ddf.catalog.util.impl.ServiceSelectionStrategy} {@link
 *     ddf.catalog.util.impl.ServiceSelectorConverter} {@link
 *     ddf.catalog.util.impl.ServiceComparator}
 */
public class ServiceSelector<T> {

  private ServiceSelectionStrategy<T> serviceSelectionStrategy;

  private SortedSet<ServiceReference<T>> serviceSet;

  private T service;

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSelector.class);

  /**
   * This default constructor is equivalent to calling: new ServiceSelector(new
   * ddf.catalog.util.impl.ServiceComparator(), new FirstElementServiceSelectionStrategy())
   */
  public ServiceSelector() {
    this(new ServiceComparator(), new FirstElementServiceSelectionStrategy());
  }

  /**
   * This constructor allows the user to set the comparator to be used by this ServiceSelector which
   * allows them to set the internal set order. It uses a FirstElementServiceSelectionStrategy.
   *
   * @param serviceComparator - The comparator used to determine the internal set order.
   */
  public ServiceSelector(Comparator serviceComparator) {
    this(serviceComparator, new FirstElementServiceSelectionStrategy<T>());
  }

  public ServiceSelector(ServiceSelectionStrategy serviceSelectionStrategy) {
    this(new ServiceComparator(), serviceSelectionStrategy);
  }

  public ServiceSelector(
      Comparator serviceComparator, ServiceSelectionStrategy serviceSelectionStrategy) {

    if (serviceComparator == null) {
      throw new IllegalArgumentException(
          "ServiceSelector(): constructor argument 'serviceComparator' may not be null.");
    }

    if (serviceSelectionStrategy == null) {
      throw new IllegalArgumentException(
          "ServiceSelector(): constructor argument 'serviceSelectionStrategy' may not be null.");
    }

    this.serviceSet = new ConcurrentSkipListSet<ServiceReference<T>>(serviceComparator);
    this.serviceSelectionStrategy = serviceSelectionStrategy;
  }

  BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(ServiceSelector.class);

    if (bundle != null) {
      return bundle.getBundleContext();
    }

    return null;
  }

  public T getService() {
    return this.service;
  }

  public SortedSet<ServiceReference<T>> getAllServices() {
    return Collections.unmodifiableSortedSet(serviceSet);
  }

  public void bindService(ServiceReference serviceReference) {
    LOGGER.trace("Entering: bindService(ServiceReference)");

    if (serviceReference != null) {
      serviceSet.add(serviceReference);
      resetService();
    }

    LOGGER.trace("Exiting: bindService(ServiceReference)");
  }

  public void unbindService(ServiceReference serviceReference) {
    LOGGER.trace("Entering: unbindService(ServiceReference)");

    if (serviceReference != null) {
      serviceSet.remove(serviceReference);
      resetService();
    }

    LOGGER.trace("Exiting: unbindService(ServiceReference)");
  }

  private void resetService() {
    if (serviceSet.isEmpty()) {
      this.service = null;
      return;
    }

    SortedSet<ServiceReference<T>> unmodifiableServiceSet =
        Collections.unmodifiableSortedSet(this.serviceSet);
    ServiceReference<T> preferredServiceReference =
        serviceSelectionStrategy.selectService(unmodifiableServiceSet);

    this.service = null;

    if (preferredServiceReference != null) {

      // extract the preferred Service from the stored ServiceReferences;
      BundleContext bundleContext = this.getBundleContext();

      if (bundleContext != null) {
        this.service = (T) bundleContext.getService(preferredServiceReference);
      }
    }
  }
}
