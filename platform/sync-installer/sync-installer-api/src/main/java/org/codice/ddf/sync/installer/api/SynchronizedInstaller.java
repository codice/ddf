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
package org.codice.ddf.sync.installer.api;

import java.util.EnumSet;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.karaf.features.FeaturesService;
import org.osgi.service.cm.Configuration;

/**
 * Performs various installations/configuration related operations and waits for expected states to
 * be met before returning. WARNING: This service should be used with extreme caution. OSGI is a
 * highly async framework, manually waiting for other bundle/service states to be met is typically a
 * sign of a code smell or poor design. This service is primarily useful for determining when the
 * system is in a ready state for users to interact with or testing.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * * be removed in a future version of the library. </b>
 */
public interface SynchronizedInstaller {

  /**
   * Waits up to 10 minutes for the system to reach a state that is ready for user operation.
   *
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void waitForBootFinish() throws InterruptedException;

  /**
   * Waits for boot features to be installed and all bundles to reach an Active state
   *
   * @param maxWaitTime max period to wait, in milliseconds, for boot to finish before failing
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void waitForBootFinish(long maxWaitTime) throws InterruptedException;

  /**
   * Creates a Managed Service using the Managed Service Factory matching the provided factory pid.
   * Waits for the service to be registered before returning For Managed Services not created from a
   * Managed Service Factory, use {@link #updateManagedService(String, Map, String)} instead.
   *
   * @param factoryPid the factory pid of the Managed Service Factory
   * @param properties the service properties for the Managed Service
   * @param bundleLocation the bundle location the configuration should be associated with. See
   *     {@link Configuration#getBundleLocation()} for more details.
   * @return configuration of the service created
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  Configuration createManagedFactoryService(
      String factoryPid, @Nullable Map<String, Object> properties, @Nullable String bundleLocation)
      throws InterruptedException;

  /**
   * Creates a Managed Service using the Managed Service Factory matching the provided factory pid.
   * Waits for the service to be registered before returning For Managed Services not created from a
   * Managed Service Factory, use {@link #updateManagedService(String, Map, String)} instead.
   *
   * @param maxWaitTime the max time to wait, in milliseconds, for the service to appear before
   *     failing
   * @param factoryPid the factory pid of the Managed Service Factory
   * @param properties the service properties for the Managed Service
   * @param bundleLocation the bundle location the configuration should be associated with. See
   *     {@link Configuration#getBundleLocation()} for more details.
   * @return configuration of the service created
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  Configuration createManagedFactoryService(
      long maxWaitTime,
      String factoryPid,
      @Nullable Map<String, Object> properties,
      @Nullable String bundleLocation)
      throws InterruptedException;

  /**
   * Waits for Managed Service to come up before updating its properties. Waits for the properties
   * to be reflected in the service's Configuration. Does not guarantee the bean has received the
   * values from the Configuration. For services created from a Managed Service Factory, use {@link
   * #createManagedFactoryService(String, Map, String)} instead.
   *
   * @param servicePid persistent identifier of the Managed Service to update
   * @param properties service configuration properties
   * @param bundleLocation the bundle location the configuration should be associated with. See
   *     {@link Configuration#getBundleLocation()} for more details.
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void updateManagedService(
      String servicePid, Map<String, Object> properties, @Nullable String bundleLocation)
      throws InterruptedException;

  /**
   * Waits for Managed Service to come up before updating it's properties. Waits for the properties
   * to reflected in the services Configuration. Does not guarantee the bean has received the values
   * from the Configuration. For services created from a Managed Service Factory, use {@link
   * #createManagedFactoryService(String, Map, String)} instead.
   *
   * @param maxWaitTime the max time to wait, in milliseconds, for the service to appear & changes
   *     to be reflected in service configuration before failing
   * @param servicePid persistent identifier of the Managed Service to start
   * @param properties service configuration properties
   * @param bundleLocation the bundle location the configuration should be associated with. See
   *     {@link Configuration#getBundleLocation()} for more details.
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void updateManagedService(
      long maxWaitTime,
      String servicePid,
      Map<String, Object> properties,
      @Nullable String bundleLocation)
      throws InterruptedException;

  /**
   * Waits for a service with a matching service pid to appear before returning.
   *
   * @param servicePid persistent identifier of the Managed Service to wait for
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void waitForServiceToBeAvailable(String servicePid) throws InterruptedException;

  /**
   * Waits for a service with a matching service pid to appear before returning.
   *
   * @param maxWaitTime the max time, in milliseconds, to wait for the service to appear before
   *     failing
   * @param servicePid persistent identifier of the Managed Service to wait for
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void waitForServiceToBeAvailable(long maxWaitTime, String servicePid) throws InterruptedException;

  /**
   * Installs and starts one or more features. Waits until the state of all specified features are
   * Started and all bundles are Active before returning.
   *
   * @param feature name of feature to be started
   * @param additionalFeatures names of additional features to be started
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void installFeatures(String feature, String... additionalFeatures) throws InterruptedException;

  /**
   * Installs and starts one or more features. Waits until the state of all specified features are
   * Started and all bundles are Active before returning.
   *
   * @param options additional feature installation options
   * @param feature name of feature to be started
   * @param additionalFeatures names of additional features to be started
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void installFeatures(
      EnumSet<FeaturesService.Option> options, String feature, String... additionalFeatures)
      throws InterruptedException;
  /**
   * Installs and starts one or more features. Waits until the state of all specified features are
   * Started and all bundles are Active before returning.
   *
   * @param maxWaitTime the max wait time, in milliseconds, for features to start and bundles to
   *     reach an Active state
   * @param options additional feature installation options
   * @param feature name of feature to be started
   * @param additionalFeatures names of additional features to be started
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void installFeatures(
      long maxWaitTime,
      EnumSet<FeaturesService.Option> options,
      String feature,
      String... additionalFeatures)
      throws InterruptedException;

  /**
   * Uninstalls one or more features. Waits for the all bundles to reach an Active state before
   * returning
   *
   * @param feature name of feature to uninstall
   * @param additionalFeatures names of additional features to uninstall
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void uninstallFeatures(String feature, String... additionalFeatures) throws InterruptedException;

  /**
   * Uninstalls one or more features. Waits for the all bundles to reach an Active state before
   * returning
   *
   * @param options additional options for feature uninstalls
   * @param feature name of feature to uninstall
   * @param additionalFeatures names of additional features to uninstall
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void uninstallFeatures(
      EnumSet<FeaturesService.Option> options, String feature, String... additionalFeatures)
      throws InterruptedException;

  /**
   * Uninstalls one or more features. Waits until all bundles reach an Active state before
   * returning.
   *
   * @param options additional options for feature uninstalls
   * @param maxWaitTime the max wait time, in milliseconds, for features to start and bundles to
   *     reach an Active state
   * @param feature name of feature to be started
   * @param additionalFeatures names of additional features to be started
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void uninstallFeatures(
      long maxWaitTime,
      EnumSet<FeaturesService.Option> options,
      String feature,
      String... additionalFeatures)
      throws InterruptedException;

  /**
   * Stops the specified bundles.
   *
   * @param symbolicName symbolic name of installed bundle to stop
   * @param additionalSymbolicNames symbolic names of additional installed bundles to stop
   * @throws SynchronizedInstallerException if an error occurs while stopping bundles
   */
  void stopBundles(String symbolicName, String... additionalSymbolicNames);

  /**
   * Starts the specified bundles. In the case of a fragment, the Resolved state waited for before
   * returning.
   *
   * @param symbolicName symbolic name of installed bundle to start
   * @param additionalSymbolicNames symbolic names of additional installed bundles to start
   * @throws SynchronizedInstallerException if an error occurs while starting bundles
   */
  void startBundles(String symbolicName, String... additionalSymbolicNames);

  /**
   * Waits for installed bundles with the specified symbolic names to be in an Active before
   * returning. In the case of a fragment, the Resolved state waited for before returning. If no
   * symbolicNames are specified, this method will wait for all installed bundles to reach an Active
   * state before returning.
   *
   * @param symbolicNames symbolic names of installed bundles to wait to reach an Active state
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void waitForBundles(String... symbolicNames) throws InterruptedException;

  /**
   * Waits for installed bundles with the specified symbolic names to be in an Active before
   * returning. In the case of a fragment, the Resolved state waited for before returning. If no
   * symbolicNames are specified, this method will wait for all installed bundles to reach an Active
   * state before returning.
   *
   * @param maxWaitTime max time to wait, in milliseconds, for bundles to reach an Active state
   * @param symbolicNames symbolic names of installed bundles to wait to reach an Active state
   * @throws SynchronizedInstallerException if an error occurs while performing wait
   * @throws SynchronizedInstallerTimeoutException if condition takes longer than max wait time to
   *     be met
   * @throws InterruptedException if thread is interrupted while waiting for condition to be met
   */
  void waitForBundles(long maxWaitTime, String... symbolicNames) throws InterruptedException;
}
