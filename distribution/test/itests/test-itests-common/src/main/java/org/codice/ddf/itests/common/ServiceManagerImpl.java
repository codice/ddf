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

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayway.restassured.response.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.management.NotCompliantMBeanException;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.core.api.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceManagerImpl implements ServiceManager {

  private static final Map<Integer, String> BUNDLE_STATES =
      new ImmutableMap.Builder<Integer, String>()
          .put(Bundle.UNINSTALLED, "UNINSTALLED")
          .put(Bundle.INSTALLED, "INSTALLED")
          .put(Bundle.RESOLVED, "RESOLVED")
          .put(Bundle.STARTING, "STARTING")
          .put(Bundle.STOPPING, "STOPPING")
          .put(Bundle.ACTIVE, "ACTIVE")
          .build();

  private static final long MANAGED_SERVICE_TIMEOUT =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  private static final long FEATURES_AND_BUNDLES_TIMEOUT = TimeUnit.MINUTES.toMillis(20);

  private static final long HTTP_ENDPOINT_TIMEOUT =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManagerImpl.class);

  private static final int CONFIG_UPDATE_WAIT_INTERVAL_MILLIS = 5;

  private final MetaTypeService metatype;

  private final AdminConfig adminConfig;

  private BundleService bundleService;

  private FeaturesService featuresService;

  private BundleContext bundleContext;

  ServiceManagerImpl(
      MetaTypeService metatype,
      AdminConfig adminConfig,
      BundleContext bundleContext,
      BundleService bundleService,
      FeaturesService featuresService) {
    this.metatype = metatype;
    this.adminConfig = adminConfig;
    this.bundleService = bundleService;
    this.bundleContext = bundleContext;
    this.featuresService = featuresService;
  }

  @Override
  public BundleContext getBundleContext() {
    return bundleContext;
  }

  @Override
  public Configuration createManagedService(String factoryPid, Map<String, Object> properties)
      throws IOException {

    Configuration sourceConfig = adminConfig.createFactoryConfiguration(factoryPid, null);

    startManagedService(sourceConfig, properties);

    return sourceConfig;
  }

  @Override
  public void startManagedService(String servicePid, Map<String, Object> properties)
      throws IOException {
    Configuration sourceConfig = adminConfig.getConfiguration(servicePid, null);

    startManagedService(sourceConfig, properties);
  }

  @Override
  public void stopManagedService(String servicePid) throws IOException {
    Configuration sourceConfig = adminConfig.getConfiguration(servicePid, null);

    try {
      adminConfig.getAdminConsoleService().delete(sourceConfig.getPid());
    } catch (NotCompliantMBeanException ignored) {
    }
  }

  private void startManagedService(Configuration sourceConfig, Map<String, Object> properties)
      throws IOException {
    ServiceManagerImpl.ServiceConfigurationListener listener =
        new ServiceConfigurationListener(sourceConfig.getPid());

    if (bundleContext == null) {
      LOGGER.info("Unable to get bundle context.");
      return;
    }

    ServiceRegistration<?> serviceRegistration =
        bundleContext.registerService(ConfigurationListener.class.getName(), listener, null);

    try {
      waitForService(sourceConfig);
    } catch (NotCompliantMBeanException ignored) {
    }

    try {
      adminConfig.getAdminConsoleService().update(sourceConfig.getPid(), properties);
    } catch (NotCompliantMBeanException ignored) {
    }

    await("Waiting for configuration to be updated")
        .atMost(MANAGED_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS)
        .pollDelay(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
        .until(listener::isUpdated);

    serviceRegistration.unregister();

    if (!listener.isUpdated()) {
      throw new RuntimeException(
          String.format(
              "Service configuration %s was not updated within %d minute timeout.",
              sourceConfig.getPid(), TimeUnit.MILLISECONDS.toMinutes(MANAGED_SERVICE_TIMEOUT)));
    }
  }

  private void waitForService(Configuration sourceConfig) throws NotCompliantMBeanException {
    await("Waiting for service: " + sourceConfig.getPid())
        .atMost(MANAGED_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS)
        .pollDelay(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
        .until(
            () ->
                adminConfig
                    .getAdminConsoleService()
                    .listServices()
                    .stream()
                    .map(Service::getId)
                    .anyMatch(
                        id ->
                            id.equals(sourceConfig.getPid())
                                || id.equals(sourceConfig.getFactoryPid())));
  }

  @Override
  public void startFeature(boolean wait, String... featureNames) throws Exception {
    for (String featureName : featureNames) {
      FeatureState state = featuresService.getState(featureName);

      if (FeatureState.Installed != state) {
        featuresService.installFeature(featureName, EnumSet.of(NoAutoRefreshBundles));
      }
    }

    if (wait) {
      for (String featureName : featureNames) {
        waitForFeature(featureName, state -> state == FeatureState.Started);
      }

      waitForAllBundles();
    }
  }

  @Override
  public void stopFeature(boolean wait, String... featureNames) throws Exception {
    List<String> waitFeatures = new ArrayList<>();
    for (String featureName : featureNames) {
      if (isFeatureInstalled(featureName)) {
        featuresService.uninstallFeature(featureName, EnumSet.of(NoAutoRefreshBundles));
        waitFeatures.add(featureName);
      }
    }

    if (wait) {
      for (String featureName : waitFeatures) {
        waitForFeature(featureName, state -> state == FeatureState.Uninstalled);
      }

      waitForAllBundles();
    }
  }

  private boolean isFeatureInstalled(String featureName) throws Exception {
    return Arrays.stream(featuresService.listInstalledFeatures())
        .map(Feature::getName)
        .anyMatch(name -> name.equals(featureName));
  }

  @Override
  public void restartBundles(String... bundleSymbolicNames) throws BundleException {
    LOGGER.debug("Restarting bundles {}", bundleSymbolicNames);

    Map<String, Bundle> bundleLookup =
        Arrays.stream(bundleContext.getBundles())
            .collect(Collectors.toMap(Bundle::getSymbolicName, Function.identity(), (a, b) -> a));

    List<Bundle> bundles =
        Arrays.stream(bundleSymbolicNames).map(bundleLookup::get).collect(Collectors.toList());

    for (Bundle bundle : bundles) {
      bundle.stop();
    }

    for (Bundle bundle : Lists.reverse(bundles)) {
      bundle.start();
    }
  }

  @Override
  public void stopBundle(String bundleSymbolicName) throws BundleException {
    for (Bundle bundle : bundleContext.getBundles()) {
      if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
        bundle.stop();
      }
    }
  }

  @Override
  public void startBundle(String bundleSymbolicName) throws BundleException {
    for (Bundle bundle : bundleContext.getBundles()) {
      if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
        bundle.start();
      }
    }
  }

  @Override
  public void uninstallBundle(String bundleSymbolicName) throws BundleException {
    for (Bundle bundle : bundleContext.getBundles()) {
      if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
        bundle.uninstall();
        await(String.format("Bundle %s uninstalled", bundleSymbolicName))
            .atMost(FEATURES_AND_BUNDLES_TIMEOUT, TimeUnit.MILLISECONDS)
            .until(() -> bundle.getState() == Bundle.UNINSTALLED);
        break;
      }
    }
  }

  @Override
  public void waitForAllBundles() {
    waitForRequiredBundles("");
  }

  @Override
  public void waitForRequiredBundles(String symbolicNamePrefix) {
    await("Waiting for bundles with prefix to be ready: " + symbolicNamePrefix)
        .atMost(FEATURES_AND_BUNDLES_TIMEOUT, TimeUnit.MILLISECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(
            () ->
                Arrays.stream(bundleContext.getBundles())
                    .filter(bundle -> bundle.getSymbolicName().startsWith(symbolicNamePrefix))
                    .allMatch(this::isBundleReady));
  }

  private boolean isBundleReady(Bundle bundle) {
    String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
    BundleInfo info = bundleService.getInfo(bundle);
    BundleState state = info.getState();

    boolean ready;
    if (info.isFragment()) {
      ready = BundleState.Resolved.equals(state);
    } else {
      if (BundleState.Failure.equals(state)) {
        printInactiveBundles();
        LOGGER.error("The bundle " + name + " failed.");
      }
      ready = BundleState.Active.equals(state);
    }

    if (!ready) {
      LOGGER.info("{} bundle not ready yet", name);
    }

    return ready;
  }

  @Override
  public void waitForBundleUninstall(String... bundleSymbolicNames) {
    Set<String> symbolicNamesSet = Sets.newHashSet(bundleSymbolicNames);
    LOGGER.info("Waiting for bundles {} to be uninstalled...", symbolicNamesSet);

    List<Long> bundleIds =
        Arrays.stream(bundleContext.getBundles())
            .filter(b -> symbolicNamesSet.contains(b.getSymbolicName()))
            .map(Bundle::getBundleId)
            .collect(Collectors.toList());

    await(String.format("Bundles %s uninstalled", symbolicNamesSet))
        .atMost(FEATURES_AND_BUNDLES_TIMEOUT, TimeUnit.MILLISECONDS)
        .until(() -> bundleIds.stream().noneMatch(id -> bundleContext.getBundle(id) != null));

    LOGGER.info("Bundles {} uninstalled", symbolicNamesSet);
  }

  @Override
  public void waitForFeature(String featureName, Predicate<FeatureState> predicate) {
    await("Waiting for feature " + featureName + " to start")
        .atMost(FEATURES_AND_BUNDLES_TIMEOUT, TimeUnit.MILLISECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> isFeatureReady(featureName, predicate));
  }

  private boolean isFeatureReady(String featureName, Predicate<FeatureState> predicate)
      throws Exception {
    Feature feature = featuresService.getFeature(featureName);
    FeatureState state = featuresService.getState(feature.getId());
    return predicate.test(state);
  }

  @Override
  public void waitForHttpEndpoint(String path) {
    LOGGER.info("Waiting for {}", path);

    await("Waiting for " + path)
        .atMost(HTTP_ENDPOINT_TIMEOUT, TimeUnit.MILLISECONDS)
        .until(() -> isHttpEndpointReady(path));

    LOGGER.info("{} ready.", path);
  }

  private boolean isHttpEndpointReady(String path) {
    Response response =
        given().header("X-Requested-With", "XMLHttpRequest").header("Origin", path).get(path);

    String body = response.getBody().asString();
    LOGGER.debug("Response body: {}", body);

    return response.getStatusCode() == 200 && body.length() > 0;
  }

  @Override
  public void waitForSourcesToBeAvailable(String restPath, String... sources) {
    String path = restPath + "sources";
    LOGGER.info("Waiting for sources at {}", path);

    await("Waiting for sources at " + path)
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> areSourcesAvailable(restPath, Arrays.asList(sources)));

    LOGGER.info("Sources at {} ready.", path);
  }

  private boolean areSourcesAvailable(String restPath, List<String> sources) {
    Response response = get(restPath + "sources");
    String body = response.getBody().asString();
    List<String> ids = response.getBody().jsonPath().getList("id");

    boolean isResponseOk =
        response.getStatusCode() == 200
            && StringUtils.isNotBlank(body)
            && !body.contains("false")
            && ids != null;

    if (isResponseOk) {
      return ids.containsAll(sources);
    } else {
      return false;
    }
  }

  @Override
  public Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid) {
    Map<String, Object> properties = new HashMap<>();
    ObjectClassDefinition bundleMetatype = getObjectClassDefinition(symbolicName, factoryPid);
    if (bundleMetatype != null) {
      for (AttributeDefinition attributeDef :
          bundleMetatype.getAttributeDefinitions(ObjectClassDefinition.ALL)) {
        if (attributeDef.getID() != null) {
          if (attributeDef.getDefaultValue() != null) {
            if (attributeDef.getCardinality() == 0) {
              properties.put(
                  attributeDef.getID(),
                  getAttributeValue(attributeDef.getDefaultValue()[0], attributeDef.getType()));
            } else {
              properties.put(attributeDef.getID(), attributeDef.getDefaultValue());
            }
          } else if (attributeDef.getCardinality() != 0) {
            properties.put(attributeDef.getID(), new String[0]);
          }
        }
      }
    } else {
      LOGGER.debug("Metatype was null, returning an empty properties Map");
    }

    return properties;
  }

  private Object getAttributeValue(String value, int type) {
    switch (type) {
      case AttributeDefinition.BOOLEAN:
        return Boolean.valueOf(value);
      case AttributeDefinition.BYTE:
        return Byte.valueOf(value);
      case AttributeDefinition.DOUBLE:
        return Double.valueOf(value);
      case AttributeDefinition.CHARACTER:
        return value.toCharArray()[0];
      case AttributeDefinition.FLOAT:
        return Float.valueOf(value);
      case AttributeDefinition.INTEGER:
        return Integer.valueOf(value);
      case AttributeDefinition.LONG:
        return Long.valueOf(value);
      case AttributeDefinition.SHORT:
        return Short.valueOf(value);
      case AttributeDefinition.PASSWORD:
      case AttributeDefinition.STRING:
      default:
        return value;
    }
  }

  private ObjectClassDefinition getObjectClassDefinition(String symbolicName, String pid) {
    Bundle[] bundles = bundleContext.getBundles();
    for (Bundle bundle : bundles) {
      if (symbolicName.equals(bundle.getSymbolicName())) {
        try {
          MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
          if (mti != null) {
            return mti.getObjectClassDefinition(pid, Locale.getDefault().toString());
          }
        } catch (IllegalArgumentException ignore) {
        }
      }
    }
    return null;
  }

  @Override
  public void printInactiveBundles() {
    printInactiveBundles(LOGGER::error, LOGGER::error);
  }

  @Override
  public void printInactiveBundlesInfo() {
    printInactiveBundles(LOGGER::info, LOGGER::info);
  }

  private void printInactiveBundles(
      Consumer<String> headerConsumer, BiConsumer<String, Object[]> logConsumer) {
    headerConsumer.accept("Listing inactive bundles");

    for (Bundle bundle : bundleContext.getBundles()) {
      if (bundle.getState() != Bundle.ACTIVE) {
        Dictionary<String, String> headers = bundle.getHeaders();
        if (headers.get("Fragment-Host") != null) {
          continue;
        }

        StringBuilder headerString = new StringBuilder("[ ");
        Enumeration<String> keys = headers.keys();

        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          headerString.append(key).append("=").append(headers.get(key)).append(", ");
        }

        headerString.append(" ]");
        logConsumer.accept(
            "\n\tBundle: {}_v{} | {}\n\tHeaders: {}",
            new Object[] {
              bundle.getSymbolicName(),
              bundle.getVersion(),
              BUNDLE_STATES.getOrDefault(bundle.getState(), "UNKNOWN"),
              headerString
            });
      }
    }
  }

  @Override
  public <S> ServiceReference<S> getServiceReference(Class<S> aClass) {
    return bundleContext.getServiceReference(aClass);
  }

  @Override
  public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s)
      throws InvalidSyntaxException {
    return bundleContext.getServiceReferences(aClass, s);
  }

  @Override
  public <S> S getService(ServiceReference<S> serviceReference) {
    await("Service to be available: " + serviceReference)
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .until(() -> bundleContext.getService(serviceReference), notNullValue());
    return bundleContext.getService(serviceReference);
  }

  @Override
  public <S> S getService(Class<S> aClass) {
    return getService(bundleContext.getServiceReference(aClass));
  }

  private static class ServiceConfigurationListener implements ConfigurationListener {

    private final String pid;

    private boolean updated;

    ServiceConfigurationListener(String pid) {
      this.pid = pid;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
      LOGGER.info("Configuration event received: {}", event);
      if (event.getPid().equals(pid) && ConfigurationEvent.CM_UPDATED == event.getType()) {
        updated = true;
      }
    }

    public boolean isUpdated() {
      return updated;
    }
  }
}
