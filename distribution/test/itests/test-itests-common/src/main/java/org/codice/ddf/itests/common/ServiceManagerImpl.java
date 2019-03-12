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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;

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
import org.osgi.framework.FrameworkUtil;
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

  public static final long MANAGED_SERVICE_TIMEOUT =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  public static final long FEATURES_AND_BUNDLES_TIMEOUT = TimeUnit.MINUTES.toMillis(20);

  public static final long HTTP_ENDPOINT_TIMEOUT =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManagerImpl.class);

  private static final int CONFIG_UPDATE_WAIT_INTERVAL_MILLIS = 5;

  private final MetaTypeService metatype;

  private final AdminConfig adminConfig;

  private BundleService bundleService;

  public ServiceManagerImpl(MetaTypeService metatype, AdminConfig adminConfig) {
    this.metatype = metatype;
    this.adminConfig = adminConfig;
  }

  @Override
  public BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(getClass());
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
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
    } catch (NotCompliantMBeanException e) {
      // ignore
    }
  }

  private void startManagedService(Configuration sourceConfig, Map<String, Object> properties)
      throws IOException {
    ServiceManagerImpl.ServiceConfigurationListener listener =
        new ServiceManagerImpl.ServiceConfigurationListener(sourceConfig.getPid());

    BundleContext bundleContext = getBundleContext();

    bundleContext = waitForBundleContext(bundleContext);

    if (bundleContext == null) {
      LOGGER.info("Unable to get bundle context.");
      return;
    }

    ServiceRegistration<?> serviceRegistration =
        bundleContext.registerService(ConfigurationListener.class.getName(), listener, null);

    try {
      waitForService(sourceConfig);
    } catch (NotCompliantMBeanException e) {
      // ignore
    }

    try {
      adminConfig.getAdminConsoleService().update(sourceConfig.getPid(), properties);
    } catch (NotCompliantMBeanException e) {
      // ignore
    }

    long millis = 0;
    while (!listener.isUpdated() && millis < MANAGED_SERVICE_TIMEOUT) {
      try {
        Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS);
        millis += CONFIG_UPDATE_WAIT_INTERVAL_MILLIS;
      } catch (InterruptedException e) {
        LOGGER.info("Interrupted exception while trying to sleep for configuration update", e);
      }
      LOGGER.info("Waiting for configuration to be updated...{}ms", millis);
    }

    serviceRegistration.unregister();

    if (!listener.isUpdated()) {
      throw new RuntimeException(
          String.format(
              "Service configuration %s was not updated within %d minute timeout.",
              sourceConfig.getPid(), TimeUnit.MILLISECONDS.toMinutes(MANAGED_SERVICE_TIMEOUT)));
    }
  }

  private BundleContext waitForBundleContext(BundleContext bundleContext) {
    long millis = 0;
    while (bundleContext == null && millis < MANAGED_SERVICE_TIMEOUT) {
      try {
        Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS);
        millis += CONFIG_UPDATE_WAIT_INTERVAL_MILLIS;
      } catch (InterruptedException e) {
        LOGGER.info("Interrupted exception while trying to sleep for bundle context", e);
      }
      LOGGER.info("Waiting for bundle context...{}ms", millis);
      bundleContext = getBundleContext();
    }
    return bundleContext;
  }

  private void waitForService(Configuration sourceConfig) throws NotCompliantMBeanException {
    long waitForService = 0;
    boolean serviceStarted = false;
    List<Service> servicesList;
    do {
      try {
        Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS);
        waitForService += CONFIG_UPDATE_WAIT_INTERVAL_MILLIS;
      } catch (InterruptedException e) {
        LOGGER.info("Interrupted waiting for service to init");
      }

      if (waitForService >= MANAGED_SERVICE_TIMEOUT) {
        printInactiveBundles();
        throw new RuntimeException(
            String.format(
                "Service %s not initialized within %d minute timeout",
                sourceConfig.getPid(), TimeUnit.MILLISECONDS.toMinutes(MANAGED_SERVICE_TIMEOUT)));
      }

      servicesList = adminConfig.getAdminConsoleService().listServices();
      for (Service service : servicesList) {
        String id = String.valueOf(service.getId());
        if (id.equals(sourceConfig.getPid()) || id.equals(sourceConfig.getFactoryPid())) {
          serviceStarted = true;
          break;
        }
      }

    } while (!serviceStarted);
  }

  @Override
  public void startFeature(boolean wait, String... featureNames) throws Exception {
    for (String featureName : featureNames) {
      FeatureState state = getFeaturesService().getState(featureName);

      if (FeatureState.Installed != state) {
        getFeaturesService().installFeature(featureName, EnumSet.of(NoAutoRefreshBundles));
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
        getFeaturesService().uninstallFeature(featureName, EnumSet.of(NoAutoRefreshBundles));
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
    Feature[] features = getFeaturesService().listInstalledFeatures();
    for (Feature feature : features) {
      if (feature.getName().equals(featureName)) {
        return true;
      }
    }
    return false;
  }

  // TODO - we should really make this a bundle and inject this.
  private FeaturesService getFeaturesService() throws InterruptedException {
    FeaturesService featuresService = null;
    boolean ready = false;
    long timeoutLimit = System.currentTimeMillis() + FEATURES_AND_BUNDLES_TIMEOUT;
    while (!ready) {
      Bundle bundle = FrameworkUtil.getBundle(getClass());
      if (bundle != null) {
        ServiceReference<FeaturesService> featuresServiceRef =
            bundle.getBundleContext().getServiceReference(FeaturesService.class);
        try {
          if (featuresServiceRef != null) {
            featuresService = getBundleContext().getService(featuresServiceRef);
            if (featuresService != null) {
              ready = true;
            }
          }
        } catch (NullPointerException e) {
          // ignore
        }
      }

      if (!ready) {
        if (System.currentTimeMillis() > timeoutLimit) {
          fail(
              String.format(
                  "Feature service could not be resolved within %d minutes.",
                  TimeUnit.MILLISECONDS.toMinutes(FEATURES_AND_BUNDLES_TIMEOUT)));
        }
        Thread.sleep(1000);
      }
    }

    return featuresService;
  }

  @Override
  public void restartBundles(String... bundleSymbolicNames) throws BundleException {
    LOGGER.debug("Restarting bundles {}", bundleSymbolicNames);

    Map<String, Bundle> bundleLookup =
        Arrays.stream(getBundleContext().getBundles())
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
    for (Bundle bundle : getBundleContext().getBundles()) {
      if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
        bundle.stop();
      }
    }
  }

  @Override
  public void startBundle(String bundleSymbolicName) throws BundleException {
    for (Bundle bundle : getBundleContext().getBundles()) {
      if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
        bundle.start();
      }
    }
  }

  @Override
  public void uninstallBundle(String bundleSymbolicName) throws BundleException {
    for (Bundle bundle : getBundleContext().getBundles()) {
      if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
        bundle.uninstall();
        WaitCondition.expect(String.format("Bundle %s uninstalled", bundleSymbolicName))
            .within(FEATURES_AND_BUNDLES_TIMEOUT, TimeUnit.MILLISECONDS)
            .until(() -> bundle.getState() == Bundle.UNINSTALLED);
        break;
      }
    }
  }

  @Override
  public void waitForAllBundles() throws InterruptedException {
    waitForRequiredBundles("");
  }

  @Override
  public void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException {
    boolean ready = false;
    if (bundleService == null) {
      bundleService = getService(BundleService.class);
    }

    long timeoutLimit = System.currentTimeMillis() + FEATURES_AND_BUNDLES_TIMEOUT;
    while (!ready) {
      List<Bundle> bundles = Arrays.asList(getBundleContext().getBundles());

      ready = true;
      for (Bundle bundle : bundles) {
        if (bundle.getSymbolicName().startsWith(symbolicNamePrefix)) {
          String bundleName = bundle.getHeaders().get(Constants.BUNDLE_NAME);
          BundleInfo bundleInfo = bundleService.getInfo(bundle);
          BundleState bundleState = bundleInfo.getState();
          if (bundleInfo.isFragment()) {
            if (!BundleState.Resolved.equals(bundleState)) {
              LOGGER.info("{} bundle not ready yet", bundleName);
              ready = false;
            }
          } else if (bundleState != null) {
            if (BundleState.Failure.equals(bundleState)) {
              printInactiveBundles();
              fail("The bundle " + bundleName + " failed.");
            } else if (!BundleState.Active.equals(bundleState)) {
              LOGGER.info("{} bundle not ready with state {}", bundleName, bundleState);
              ready = false;
            }
          }
        }
      }

      if (!ready) {
        if (System.currentTimeMillis() > timeoutLimit) {
          printInactiveBundles();
          fail(
              String.format(
                  "Bundles and blueprint did not start within %d minutes.",
                  TimeUnit.MILLISECONDS.toMinutes(FEATURES_AND_BUNDLES_TIMEOUT)));
        }
        LOGGER.info("Bundles not up, sleeping...");
        Thread.sleep(1000);
      }
    }
  }

  @Override
  public void waitForBundleUninstall(String... bundleSymbolicNames) {
    Set<String> symbolicNamesSet = Sets.newHashSet(bundleSymbolicNames);
    LOGGER.info("Waiting for bundles {} to be uninstalled...", symbolicNamesSet);

    List<Long> bundleIds =
        Arrays.stream(getBundleContext().getBundles())
            .filter(b -> symbolicNamesSet.contains(b.getSymbolicName()))
            .map(Bundle::getBundleId)
            .collect(Collectors.toList());

    WaitCondition.expect(String.format("Bundles %s uninstalled", symbolicNamesSet))
        .within(FEATURES_AND_BUNDLES_TIMEOUT, TimeUnit.MILLISECONDS)
        .until(
            () ->
                bundleIds
                    .stream()
                    .filter(id -> getBundleContext().getBundle(id) != null)
                    .collect(Collectors.toList())
                    .isEmpty());

    LOGGER.info("Bundles {} uninstalled", symbolicNamesSet);
  }

  @Override
  public void waitForFeature(String featureName, Predicate<FeatureState> predicate)
      throws Exception {
    boolean ready = false;

    long timeoutLimit = System.currentTimeMillis() + FEATURES_AND_BUNDLES_TIMEOUT;
    FeaturesService featuresService = getFeaturesService();

    while (!ready) {
      FeatureState state = null;

      if (featuresService != null) {
        Feature feature = featuresService.getFeature(featureName);
        state = featuresService.getState(feature.getName() + "/" + feature.getVersion());

        if (state == null) {
          LOGGER.debug("No Feature found for featureName: {}", featureName);
          return;
        } else if (predicate.test(state)) {
          ready = true;
        }
      }

      if (!ready) {
        if (System.currentTimeMillis() > timeoutLimit) {
          printInactiveBundles();
          fail(
              String.format(
                  "Feature did not change to State [" + predicate + "] within %d minutes.",
                  TimeUnit.MILLISECONDS.toMinutes(FEATURES_AND_BUNDLES_TIMEOUT)));
        }
        LOGGER.info("Still waiting on feature [{}], current state [{}]...", featureName, state);
        Thread.sleep(1000);
      }
    }
  }

  @Override
  public void waitForHttpEndpoint(String path) throws InterruptedException {
    LOGGER.info("Waiting for {}", path);

    long timeoutLimit = System.currentTimeMillis() + HTTP_ENDPOINT_TIMEOUT;
    boolean available = false;

    while (!available) {
      Response response =
          given().header("X-Requested-With", "XMLHttpRequest").header("Origin", path).get(path);
      available = response.getStatusCode() == 200 && response.getBody().asString().length() > 0;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Response body: {}", response.getBody().asString());
      }
      if (!available) {
        if (System.currentTimeMillis() > timeoutLimit) {
          printInactiveBundles();
          fail(
              String.format(
                  "%s did not start within %d minutes.",
                  path, TimeUnit.MILLISECONDS.toMinutes(HTTP_ENDPOINT_TIMEOUT)));
        }
        Thread.sleep(100);
      }
    }

    LOGGER.info("{} ready.", path);
  }

  @Override
  public void waitForSourcesToBeAvailable(String restPath, String... sources)
      throws InterruptedException {
    String path = restPath + "sources";
    LOGGER.info("Waiting for sources at {}", path);

    long timeoutLimit =
        System.currentTimeMillis() + AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;
    boolean available = false;

    while (!available) {
      Response response = get(path);
      String body = response.getBody().asString();
      if (StringUtils.isNotBlank(body)) {
        available =
            response.getStatusCode() == 200
                && body.length() > 0
                && !body.contains("false")
                && response.getBody().jsonPath().getList("id") != null;
        if (available) {
          List<Object> ids = response.getBody().jsonPath().getList("id");
          for (String source : sources) {
            if (!ids.contains(source)) {
              available = false;
            }
          }
        }
      }
      if (!available) {
        if (System.currentTimeMillis() > timeoutLimit) {
          response.prettyPrint();
          printInactiveBundles();
          fail("Sources at " + path + " did not start in time.");
        }
        Thread.sleep(1000);
      }
    }

    LOGGER.info("Sources at {} ready.", path);
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
    Bundle[] bundles = getBundleContext().getBundles();
    for (Bundle bundle : bundles) {
      if (symbolicName.equals(bundle.getSymbolicName())) {
        try {
          MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
          if (mti != null) {
            try {
              ObjectClassDefinition ocd =
                  mti.getObjectClassDefinition(pid, Locale.getDefault().toString());
              if (ocd != null) {
                return ocd;
              }
            } catch (IllegalArgumentException e) {
              // ignoring
            }
          }
        } catch (IllegalArgumentException iae) {
          // ignoring
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

    for (Bundle bundle : getBundleContext().getBundles()) {
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
    return getBundleContext().getServiceReference(aClass);
  }

  @Override
  public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s)
      throws InvalidSyntaxException {
    return getBundleContext().getServiceReferences(aClass, s);
  }

  @Override
  public <S> S getService(ServiceReference<S> serviceReference) {
    WaitCondition.expect("Service to be available: " + serviceReference)
        .within(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .until(() -> getBundleContext().getService(serviceReference), notNullValue());
    return getBundleContext().getService(serviceReference);
  }

  @Override
  public <S> S getService(Class<S> aClass) {
    return getService(getBundleContext().getServiceReference(aClass));
  }

  private class ServiceConfigurationListener implements ConfigurationListener {

    private final String pid;

    private boolean updated;

    public ServiceConfigurationListener(String pid) {
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
