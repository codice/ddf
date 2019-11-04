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
package org.codice.ddf.itests.common;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.karaf.features.FeatureState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;

public interface ServiceManager {

  BundleContext getBundleContext();

  /**
   * Creates a Managed Service that is created from a Managed Service Factory. Waits for the
   * asynchronous call that the properties have been updated and the service can be used.
   *
   * <p>For Managed Services not created from a Managed Service Factory, use {@link
   * #startManagedService(String, Map)} instead.
   *
   * @param factoryPid the factory pid of the Managed Service Factory
   * @param properties the service properties for the Managed Service
   * @throws IOException if access to persistent storage fails
   */
  Configuration createManagedService(String factoryPid, Map<String, Object> properties)
      throws IOException;

  /**
   * Starts a Managed Service. Waits for the asynchronous call that the properties have been updated
   * and the service can be used.
   *
   * <p>For Managed Services created from a Managed Service Factory, use {@link
   * #createManagedService(String, Map)} instead.
   *
   * @param servicePid persistent identifier of the Managed Service to start
   * @param properties service configuration properties
   * @throws IOException thrown if if access to persistent storage fails
   */
  void startManagedService(String servicePid, Map<String, Object> properties) throws IOException;

  /**
   * Stops a managed service.
   *
   * @param servicePid persistent identifier of the Managed Service to stop
   * @throws IOException thrown if if access to persistent storage fails
   */
  void stopManagedService(String servicePid) throws IOException;

  /**
   * Installs and starts one or more features.
   *
   * @param wait if {@code true}, this method will wait until the state of all the features is
   *     {@code Started} and all bundles are {@code Active} before returning
   * @param featureNames names of the features to install and start
   * @throws Exception thrown if one of the features fails to be installed or started
   */
  void startFeature(boolean wait, String... featureNames) throws Exception;

  /**
   * Stops and uninstalls one or more features.
   *
   * @param wait if {@code true}, this method will wait until the state of all the features is
   *     {@code Uninstalled} and all bundles are {@code Active} before returning
   * @param featureNames names of the features to install and start
   * @throws Exception thrown if one of the features fails to be installed or started
   */
  void stopFeature(boolean wait, String... featureNames) throws Exception;

  /**
   * Restarts one or more bundles. The bundles will be stopped in the order provided and started in
   * the reverse order.
   *
   * @param bundleSymbolicNames list of bundle symbolic names to restart
   * @throws BundleException if one of the bundles fails to stop or start
   */
  void restartBundles(String... bundleSymbolicNames) throws BundleException;

  void stopBundle(String bundleSymbolicName) throws BundleException;

  void startBundle(String bundleSymbolicName) throws BundleException;

  void installBundle(String locationIdentifier) throws BundleException;

  void uninstallBundle(String bundleSymbolicName) throws BundleException;

  void waitForAllBundles() throws InterruptedException;

  void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException;

  /**
   * Waits for one or more bundle to be uninstalled.
   *
   * @param bundleSymbolicNames symbolic names of the bundles to wait for
   */
  void waitForBundleUninstall(String... bundleSymbolicNames);

  void waitForFeature(String featureName, Predicate<FeatureState> predicate) throws Exception;

  void waitForHttpEndpoint(String path) throws InterruptedException;

  void waitForSourcesToBeAvailable(String restPath, String... sources) throws InterruptedException;

  Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid);

  void printInactiveBundles();

  void printInactiveBundlesInfo();

  <S> ServiceReference<S> getServiceReference(Class<S> aClass);

  <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s)
      throws InvalidSyntaxException;

  <S> S getService(ServiceReference<S> serviceReference);

  <S> S getService(Class<S> aClass);
}
