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
package org.codice.ddf.sync.installer.impl;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.sync.installer.api.SynchronizedInstaller;
import org.codice.ddf.sync.installer.api.SynchronizedInstallerException;
import org.codice.ddf.sync.installer.api.SynchronizedInstallerTimeoutException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizedInstallerImpl implements SynchronizedInstaller {

  private static final String BUNDLE_DIAG_FORMAT =
      "%n---------------------------------------------------------------%n"
          + "Bundle Name: %s%n"
          + "Status: %s%n"
          + "Diagnosis:%n%s%n";

  private static final String SERVICE_PID_FILTER = "(" + Constants.SERVICE_PID + "=%s)";

  private static final long DEFAULT_MAX_FEATURE_WAIT = TimeUnit.MINUTES.toMillis(10);

  private static final long DEFAULT_MAX_SERVICE_WAIT = TimeUnit.MINUTES.toMillis(10);

  private static final long DEFAULT_MAX_BUNDLE_WAIT = TimeUnit.MINUTES.toMillis(10);

  private static final long DEFAULT_POLLING_INTERVAL = TimeUnit.SECONDS.toMillis(3);

  private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizedInstallerImpl.class);

  private BundleContext bundleContext;

  private ConfigurationAdmin configAdmin;

  private FeaturesService featuresService;

  private BundleService bundleService;

  public SynchronizedInstallerImpl(
      BundleContext bundleContext,
      ConfigurationAdmin configAdmin,
      FeaturesService featuresService,
      BundleService bundleService) {
    this.bundleContext = bundleContext;
    this.configAdmin = configAdmin;
    this.featuresService = featuresService;
    this.bundleService = bundleService;
  }

  @Override
  public void waitForBootFinish() throws InterruptedException {
    waitForBootFinish(DEFAULT_MAX_FEATURE_WAIT);
  }

  @Override
  public void waitForBootFinish(long maxWaitTime) throws InterruptedException {
    Callable<Boolean> isBootFinishedAvailable =
        () -> {
          LOGGER.debug("Waiting for BootFinished service to appear.");
          return bundleContext.getServiceReference(BootFinished.class) != null;
        };

    long startTime = System.currentTimeMillis();

    wait(
        isBootFinishedAvailable,
        maxWaitTime,
        DEFAULT_POLLING_INTERVAL,
        "BootFinished service did not appear within [" + maxWaitTime + "] ms");
    waitForBundles(getRemainingTime(startTime, maxWaitTime));
  }

  @Override
  public Configuration createManagedFactoryService(
      String factoryPid, @Nullable Map<String, Object> properties, @Nullable String bundleLocation)
      throws InterruptedException {
    return createManagedFactoryService(
        DEFAULT_MAX_SERVICE_WAIT, factoryPid, properties, bundleLocation);
  }

  @Override
  public Configuration createManagedFactoryService(
      long maxWaitTime,
      String factoryPid,
      @Nullable Map<String, Object> properties,
      @Nullable String bundleLocation)
      throws InterruptedException {
    LOGGER.info("Creating managed service of factorypid [{}]", factoryPid);

    Configuration createdConfig;
    try {
      createdConfig = configAdmin.createFactoryConfiguration(factoryPid, bundleLocation);
      createdConfig.setBundleLocation(bundleLocation);
    } catch (IOException e) {
      throw new SynchronizedInstallerException(
          "Failed to initialize managed service factory configuration with fpid of ["
              + factoryPid
              + "]",
          e);
    }

    try {
      if (properties == null || properties.isEmpty()) {
        createdConfig.update();
      } else {
        createdConfig.update(toDic(properties));
      }
    } catch (IOException e) {
      throw new SynchronizedInstallerException(
          "Failed to update created managed service factory configuration with pid of ["
              + createdConfig.getPid()
              + "]",
          e);
    }

    waitForServiceToBeAvailable(maxWaitTime, createdConfig.getPid());
    return createdConfig;
  }

  @Override
  public void updateManagedService(
      String servicePid, Map<String, Object> properties, @Nullable String bundleLocation)
      throws InterruptedException {
    updateManagedService(DEFAULT_MAX_SERVICE_WAIT, servicePid, properties, bundleLocation);
  }

  @Override
  @SuppressWarnings("squid:S1192") /* Ignoring recommendation to refactor "] ms" into a constant */
  public void updateManagedService(
      long maxWaitTime,
      String servicePid,
      Map<String, Object> properties,
      @Nullable String bundleLocation)
      throws InterruptedException {
    long startTime = System.currentTimeMillis();
    waitForServiceToBeAvailable(maxWaitTime, servicePid);

    LOGGER.debug(
        "Registering service configuration listener to listen to service with pid of {}.",
        servicePid);
    SynchronizedConfigurationListener listener = getConfigListener(servicePid);
    ServiceRegistration<?> registration =
        bundleContext.registerService(ConfigurationListener.class.getName(), listener, null);

    Callable<Boolean> isServiceUpdated =
        () -> {
          LOGGER.trace(
              "Waiting for service with pid [{}] to reflect configuration updates...", servicePid);
          return listener.isUpdated();
        };

    LOGGER.info("Updating configuration of service with pid [{}].", servicePid);

    try {
      Configuration config = configAdmin.getConfiguration(servicePid, bundleLocation);
      config.setBundleLocation(bundleLocation);
      config.update(toDic(properties));

      LOGGER.debug("Updated configuration of service with pid [{}].", servicePid);
      wait(
          isServiceUpdated,
          getRemainingTime(startTime, maxWaitTime),
          DEFAULT_POLLING_INTERVAL,
          "Managed service failed to update within [" + maxWaitTime + "] ms.");
    } catch (InterruptedException | SynchronizedInstallerException e) {
      throw e;
    } catch (IOException e) {
      throw new SynchronizedInstallerException(
          "Failed to update managed service configuration with pid of [" + servicePid + "]", e);
    } catch (Exception e) {
      throw new SynchronizedInstallerException(
          "Exception was thrown while waiting for service with pid of ["
              + servicePid
              + "] to be updated.",
          e);
    } finally {
      registration.unregister();
    }
  }

  @Override
  public void waitForServiceToBeAvailable(String servicePid) throws InterruptedException {
    waitForServiceToBeAvailable(DEFAULT_MAX_SERVICE_WAIT, servicePid);
  }

  @Override
  public void waitForServiceToBeAvailable(long maxWaitTime, String servicePid)
      throws InterruptedException {
    ServiceTracker st = getServiceTracker(bundleContext, getServiceIdFilter(servicePid));
    st.open(true);

    Callable<Boolean> isServiceAvailable =
        () -> {
          LOGGER.trace("Waiting for service with pid [{}] to be registered", servicePid);
          return st.getService() != null;
        };

    try {
      wait(
          isServiceAvailable,
          maxWaitTime,
          DEFAULT_POLLING_INTERVAL,
          "Managed service failed to appear after [" + maxWaitTime + "] ms.");
    } finally {
      st.close();
    }
  }

  @Override
  public void installFeatures(String feature, String... additionalFeatures)
      throws InterruptedException {
    installFeatures(EnumSet.noneOf(FeaturesService.Option.class), feature, additionalFeatures);
  }

  @Override
  public void installFeatures(
      EnumSet<FeaturesService.Option> options, String feature, String... additionalFeatures)
      throws InterruptedException {
    installFeatures(DEFAULT_MAX_FEATURE_WAIT, options, feature, additionalFeatures);
  }

  @Override
  public void installFeatures(
      long maxWaitTime,
      EnumSet<FeaturesService.Option> options,
      String feature,
      String... additionalFeatures)
      throws InterruptedException {
    Set<String> featuresToInstall =
        featuresFromNames(feature, additionalFeatures)
            .filter(f -> !featuresService.isInstalled(f))
            .map(Feature::getName)
            .collect(Collectors.toSet());
    String featureNames = String.join(", ", featuresToInstall);

    if (featuresToInstall.isEmpty()) {
      return;
    }

    LOGGER.info("Installing the following features: [{}]", featureNames);
    long startTime = System.currentTimeMillis();

    try {
      featuresService.installFeatures(featuresToInstall, options);
    } catch (Exception e) {
      throw new SynchronizedInstallerException(
          "Failed to install features [" + String.join(", ", featuresToInstall) + "]", e);
    }

    waitForBundles(getRemainingTime(startTime, maxWaitTime));
    LOGGER.info("Finished installing features in {} ms", (System.currentTimeMillis() - startTime));
  }

  @Override
  public void uninstallFeatures(String feature, String... additionalFeatures)
      throws InterruptedException {
    uninstallFeatures(EnumSet.noneOf(FeaturesService.Option.class), feature, additionalFeatures);
  }

  @Override
  public void uninstallFeatures(
      EnumSet<FeaturesService.Option> options, String feature, String... additionalFeatures)
      throws InterruptedException {
    uninstallFeatures(DEFAULT_MAX_FEATURE_WAIT, options, feature, additionalFeatures);
  }

  @Override
  public void uninstallFeatures(
      long maxWaitTime,
      EnumSet<FeaturesService.Option> options,
      String feature,
      String... additionalFeatures)
      throws InterruptedException {
    Set<String> featuresToUninstall =
        featuresFromNames(feature, additionalFeatures)
            .filter(featuresService::isInstalled)
            .map(Feature::getName)
            .collect(Collectors.toSet());

    if (featuresToUninstall.isEmpty()) {
      return;
    }

    String featureNames = String.join(", ", featuresToUninstall);
    LOGGER.info("Uninstalling the following features: [{}]", featureNames);
    long startTime = System.currentTimeMillis();
    try {
      featuresService.uninstallFeatures(featuresToUninstall, options);
    } catch (Exception e) {
      throw new SynchronizedInstallerException(
          "Failed to uninstall features [" + String.join(", ", featuresToUninstall) + "]", e);
    }

    waitForBundles(getRemainingTime(startTime, maxWaitTime));
    LOGGER.info(
        "Finished uninstalling features [{}] in [{}] ms",
        featureNames,
        (System.currentTimeMillis() - startTime));
  }

  @Override
  public void stopBundles(String symbolicName, String... additionalSymbolicNames) {
    Set<String> toStop = toSet(symbolicName, additionalSymbolicNames);
    String bundleNames = String.join(", ", toStop);
    LOGGER.info("Stopping the following bundles:[{}]", bundleNames);
    long startTime = System.currentTimeMillis();

    for (Bundle bundle : bundleContext.getBundles()) {
      if (toStop.contains(bundle.getSymbolicName())) {
        try {
          bundle.stop();
        } catch (BundleException e) {
          throw new SynchronizedInstallerException(
              "Failed to stop bundle [" + bundle.getSymbolicName() + "]");
        }
      }
    }

    LOGGER.info("Finished stopping bundles in [{}] ms", (System.currentTimeMillis() - startTime));
  }

  @Override
  public void startBundles(String symbolicName, String... additionalSymbolicNames) {
    Set<String> toStart = toSet(symbolicName, additionalSymbolicNames);
    String bundlesNames = String.join(", ", toStart);
    LOGGER.info("Starting the following bundles:[{}]", bundlesNames);
    long startTime = System.currentTimeMillis();

    for (Bundle bundle : bundleContext.getBundles()) {
      if (toStart.contains(bundle.getSymbolicName())) {
        try {
          bundle.start();
        } catch (BundleException e) {
          throw new SynchronizedInstallerException(
              "Failed to start bundle [" + bundle.getSymbolicName() + "]");
        }
      }
    }

    LOGGER.info("Finished starting bundles in [{}] ms", (System.currentTimeMillis() - startTime));
  }

  @Override
  public void waitForBundles(String... symbolicNames) throws InterruptedException {
    waitForBundles(DEFAULT_MAX_BUNDLE_WAIT, symbolicNames);
  }

  @Override
  public void waitForBundles(long maxWaitTime, String... symbolicNames)
      throws InterruptedException {
    Set<String> toWaitFor =
        symbolicNames.length == 0
            ? getAllBundleSymbolicNames()
            : Arrays.stream(symbolicNames).collect(Collectors.toSet());

    Callable<Boolean> areBundlesReady = () -> areBundlesReady(toWaitFor);

    try {
      wait(
          areBundlesReady,
          maxWaitTime,
          DEFAULT_POLLING_INTERVAL,
          "Failed waiting for bundles to reach a ready state. Check logs for more details.");
    } catch (SynchronizedInstallerException e) {
      printBundleDiags(getUnavailableBundles(toWaitFor).getFailedAndUnavailableBundles());
      throw e;
    }
  }

  private Set<String> getAllBundleSymbolicNames() {
    return Arrays.stream(bundleContext.getBundles())
        .map(Bundle::getSymbolicName)
        .collect(Collectors.toSet());
  }

  @VisibleForTesting
  void wait(
      Callable<Boolean> conditionIsMet, long maxWait, long pollInterval, String onFailureMessage)
      throws InterruptedException {
    final long startTime = System.currentTimeMillis();

    LOGGER.trace("Waiting for condition to be met. Max wait time: [{}] ms", maxWait);

    while (true) {
      try {
        if (conditionIsMet.call()) {
          return;
        }

        long remainingTime = getRemainingTime(startTime, maxWait);
        if (remainingTime <= 0) {
          LOGGER.trace("Condition not met within [{}]", maxWait);
          throw new SynchronizedInstallerTimeoutException(onFailureMessage);
        }

        Thread.sleep(Math.min(remainingTime, pollInterval));
      } catch (SynchronizedInstallerException | InterruptedException e) {
        throw e;
      } catch (Exception e) {
        throw new SynchronizedInstallerException(
            "Exception thrown while waiting for condition to be met.", e);
      }
    }
  }

  private void printBundleDiags(Collection<Bundle> toPrint) {
    StringBuilder sb = new StringBuilder();
    toPrint.forEach(bundle -> appendBundleDiag(bundle, sb));
    LOGGER.error("Printing unsatisfied bundle requirements: {}", sb);
  }

  private void appendBundleDiag(Bundle bundle, StringBuilder sb) {
    sb.append(
        String.format(
            BUNDLE_DIAG_FORMAT,
            bundle.getHeaders().get(Constants.BUNDLE_NAME),
            bundleService.getInfo(bundle).getState().toString(),
            this.bundleService.getDiag(bundle)));
  }

  @VisibleForTesting
  BundleStates getUnavailableBundles(Set<String> toCheck) {
    Set<Bundle> unavailableBundles = new HashSet<>();
    Set<Bundle> failedBundles = new HashSet<>();

    for (Bundle bundle : bundleContext.getBundles()) {
      if (!toCheck.contains(bundle.getSymbolicName())) {
        continue;
      }

      BundleInfo bundleInfo = bundleService.getInfo(bundle);
      BundleState bundleState = bundleInfo.getState();

      if (BundleState.Failure.equals(bundleState)) {
        failedBundles.add(bundle);
        continue;
      }

      if (bundleInfo.isFragment()) {
        if (!BundleState.Resolved.equals(bundleState)) {
          unavailableBundles.add(bundle);
        }
      } else if (!BundleState.Active.equals(bundleState)) {
        unavailableBundles.add(bundle);
      }
    }

    return new BundleStates(unavailableBundles, failedBundles);
  }

  private boolean areBundlesReady(Set<String> toCheck) {
    BundleStates states = getUnavailableBundles(toCheck);

    if (!states.getFailedBundles().isEmpty()) {
      throw new SynchronizedInstallerException(
          "Bundles failed to start ["
              + bundleSymbolicNamesToString(states.getFailedBundles())
              + "]");
    }

    if (!states.getUnavailableBundles().isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        String unavailableBundles = bundleSymbolicNamesToString(states.getUnavailableBundles());
        LOGGER.debug(
            "Waiting on the following bundles to reach a ready state [{}]", unavailableBundles);
      }
      return false;
    }

    return true;
  }

  @VisibleForTesting
  ServiceTracker getServiceTracker(BundleContext bundleContext, Filter filter) {
    return new ServiceTracker<>(bundleContext, filter, null);
  }

  @VisibleForTesting
  SynchronizedConfigurationListener getConfigListener(String pid) {
    return new SynchronizedConfigurationListener(pid);
  }

  private String bundleSymbolicNamesToString(Collection<Bundle> bundles) {
    return bundles.stream().map(Bundle::getSymbolicName).collect(Collectors.joining(", "));
  }

  private Stream<Feature> featuresFromNames(String feature, String... additionalFeatures) {
    return toStream(feature, additionalFeatures).map(this::getFeature);
  }

  private Feature getFeature(String featureName) {
    try {
      return featuresService.getFeature(featureName);
    } catch (Exception e) {
      throw new SynchronizedInstallerException(
          "Failed to retrieve feature [" + featureName + "]", e);
    }
  }

  private Filter getServiceIdFilter(String servicePid) {
    try {
      return bundleContext.createFilter(String.format(SERVICE_PID_FILTER, servicePid));
    } catch (Exception e) {
      throw new SynchronizedInstallerException("Failed to create service filter.", e);
    }
  }

  /**
   * Covers the case where the remaining time is shorter than the poll interval. If shorter, return
   * pollInterval instead.
   */
  private long getRemainingTime(long startTime, long maxWaitTime) {
    return (startTime + maxWaitTime) - System.currentTimeMillis();
  }

  @SuppressWarnings("squid:S1149" /* required by the OSGi API. */)
  private Dictionary<String, Object> toDic(Map<String, Object> props) {
    Dictionary<String, Object> dic = new Hashtable<>();
    props.forEach(dic::put);
    return dic;
  }

  private <T> Set<T> toSet(T ele, T... eles) {
    return toStream(ele, eles).collect(Collectors.toSet());
  }

  private <T> Stream<T> toStream(T ele, T... eles) {
    return Stream.concat(Stream.of(ele), Stream.of(eles));
  }

  @VisibleForTesting
  public static class SynchronizedConfigurationListener implements ConfigurationListener {

    private final String pid;

    private volatile boolean updated;

    public SynchronizedConfigurationListener(String pid) {
      this.pid = pid;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
      LOGGER.debug("Configuration event received by listener: {}", event);
      if (event.getPid().equals(pid) && ConfigurationEvent.CM_UPDATED == event.getType()) {
        LOGGER.debug("configuration event received matching pid & update event: {}", event);
        updated = true;
      }
    }

    public boolean isUpdated() {
      return updated;
    }
  }

  private static class BundleStates {
    private Set<Bundle> unavailableBundles;

    private Set<Bundle> failedBundles;

    BundleStates(Set<Bundle> unavailableBundles, Set<Bundle> failedBundles) {
      this.unavailableBundles =
          unavailableBundles == null ? Collections.emptySet() : unavailableBundles;
      this.failedBundles = failedBundles == null ? Collections.emptySet() : failedBundles;
    }

    Set<Bundle> getUnavailableBundles() {
      return unavailableBundles;
    }

    Set<Bundle> getFailedBundles() {
      return failedBundles;
    }

    Set<Bundle> getFailedAndUnavailableBundles() {
      return Stream.concat(getUnavailableBundles().stream(), getFailedBundles().stream())
          .collect(Collectors.toSet());
    }
  }
}
