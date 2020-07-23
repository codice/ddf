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

import static com.jayway.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.hamcrest.Matchers.hasXPath;

import com.jayway.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.ArrayUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that holds the state of the system at some given point and provides a way to get back to
 * that state.
 */
public class SystemStateManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(SystemStateManager.class);

  private static final int FEATURE_STOP_RETRY_COUNT = 3;

  private static final String CONFIGURATION_FILTER = "(" + Constants.SERVICE_PID + "=*)";

  private static SystemStateManager instance;

  private List<String> baseFeatures;

  private Map<String, Configuration> baseConfigurations = new HashMap<>();

  private ServiceManager serviceManager;

  private FeaturesService features;

  private KarafConsole console;

  private AdminConfig adminConfig;

  private boolean stateInitiallized = false;

  SystemStateManager(
      ServiceManager serviceManager,
      FeaturesService features,
      AdminConfig adminConfig,
      KarafConsole console) {
    this.serviceManager = serviceManager;
    this.features = features;
    this.adminConfig = adminConfig;
    this.console = console;
  }

  public static SystemStateManager getManager(
      ServiceManager serviceManager,
      FeaturesService features,
      AdminConfig adminConfig,
      KarafConsole console) {
    // not worried about threading here since the tests are called serially
    if (instance == null) {
      instance = new SystemStateManager(serviceManager, features, adminConfig, console);
    }
    return instance;
  }

  public void setSystemBaseState(Runnable runnable, boolean overwrite) {
    if (overwrite || !stateInitiallized) {
      runnable.run();
      captureSystemState();
      stateInitiallized = true;
    }
  }

  public void waitForSystemBaseState() {
    if (stateInitiallized) {
      resetSystem();
    } else {
      throw new IllegalStateException("The base system state has not been set");
    }
  }

  private void resetSystem() {
    LOGGER.info("Resetting system to base state");
    try {
      long start = System.currentTimeMillis();
      // reset the features
      List<String> currentFeatures =
          Arrays.stream(features.listInstalledFeatures())
              .map(Feature::getName)
              .collect(Collectors.toList());
      List<String> featuresToStart =
          baseFeatures.stream()
              .filter(e -> !currentFeatures.contains(e))
              .collect(Collectors.toList());
      List<String> featuresToStop =
          currentFeatures.stream()
              .filter(e -> !baseFeatures.contains(e))
              .collect(Collectors.toList());
      for (String feature : featuresToStart) {
        LOGGER.debug("Starting feature {}", feature);
        serviceManager.startFeature(false, feature);
      }
      serviceManager.waitForAllBundles();
      stopFeatures(featuresToStop);
      serviceManager.waitForAllBundles();

      // reset the configurations
      Configuration[] configs = adminConfig.listConfigurations(CONFIGURATION_FILTER);
      Map<String, Configuration> currentConfigurations = new HashMap<>();
      for (Configuration config : configs) {
        currentConfigurations.put(config.getPid(), config);
      }
      Map<String, Configuration> addedConfigs = new HashMap<>(currentConfigurations);
      addedConfigs.keySet().removeAll(baseConfigurations.keySet());
      for (Configuration config : addedConfigs.values()) {
        LOGGER.debug("Deleting configuration {}", config.getPid());
        config.delete();
      }

      Map<String, Configuration> removedConfigs = new HashMap<>(baseConfigurations);
      removedConfigs.keySet().removeAll(currentConfigurations.keySet());
      for (Configuration config : removedConfigs.values()) {
        LOGGER.debug("Adding configuration {}", config.getPid());
        Configuration newConfig = adminConfig.getConfiguration(config.getPid(), null);
        newConfig.update(config.getProperties());
      }

      for (Configuration config : baseConfigurations.values()) {
        if (currentConfigurations.containsKey(config.getPid())
            && !propertiesMatch(
                config.getProperties(),
                currentConfigurations.get(config.getPid()).getProperties())) {
          LOGGER.debug("Updating configuration {}", config.getPid());
          config.update(baseConfigurations.get(config.getPid()).getProperties());
        }
      }

      serviceManager.waitForAllBundles();

      // reset the catalog
      clearCatalogAndWait();
      console.runCommand(
          "catalog:import --provider --force --skip-signature-verification  itest-catalog-entries.zip");
      LOGGER.debug("Reset took {} sec", (System.currentTimeMillis() - start) / 1000.0);

    } catch (Exception e) {
      LOGGER.error("Error resetting system configuration.", e);
    }
  }

  private void clearCatalogAndWait() {
    clearCatalog();
    clearCache();
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, SECONDS)
        .until(this::isCatalogEmpty);
  }

  public void clearCatalog() {
    String output =
        console.runCommand(
            AbstractIntegrationTest.REMOVE_ALL,
            AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS);
    LOGGER.debug("{} output: {}", AbstractIntegrationTest.REMOVE_ALL, output);
  }

  private void clearCache() {
    String output =
        console.runCommand(
            "catalog:removeall -f -p --cache",
            AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS);
    LOGGER.debug("{} output: {}", "catalog:removeall -f -p --cache", output);
  }

  private boolean isCatalogEmpty() {

    try {
      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .auth()
              .basic("admin", "admin")
              .post(AbstractIntegrationTest.CSW_PATH.getUrl())
              .then();
      response.body(hasXPath("/GetRecordsResponse/SearchResults[@numberOfRecordsMatched=\"0\"]"));
      return true;
    } catch (AssertionError e) {
      return false;
    }
  }

  private void stopFeatures(List<String> featuresToStop) {
    // we try a couple of times here because some features might not stop the first time
    // due to dependencies
    List<String> stoppedFeatures = new ArrayList<>();
    for (int i = 0; i < FEATURE_STOP_RETRY_COUNT; i++) {
      stoppedFeatures.clear();
      for (String feature : featuresToStop) {
        LOGGER.debug("Stopping feature {}", feature);
        try {
          serviceManager.stopFeature(false, feature);
          stoppedFeatures.add(feature);
        } catch (Exception e) {
          LOGGER.debug("Failed to stop feature {}", feature);
        }
      }
      featuresToStop.removeAll(stoppedFeatures);
    }
  }

  private boolean propertiesMatch(
      Dictionary<String, Object> dictionary1, Dictionary<String, Object> dictionary2) {
    if (dictionary1.size() != dictionary2.size()) {
      return false;
    }
    Enumeration<String> keys = dictionary1.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      Object o = dictionary1.get(key);
      Object o1 = dictionary2.get(key);
      if (o.getClass().isArray() && o1.getClass().isArray()) {
        if (!ArrayUtils.isEquals(o, o1)) {
          return false;
        }
      } else if (!o.equals(o1)) {
        return false;
      }
    }
    return true;
  }

  private void captureSystemState() {
    LOGGER.info("Capturing system state");
    try {
      baseFeatures =
          Arrays.stream(features.listInstalledFeatures())
              .map(Feature::getName)
              .collect(Collectors.toList());
      Configuration[] configs = adminConfig.listConfigurations(CONFIGURATION_FILTER);
      for (Configuration config : configs) {
        baseConfigurations.put(config.getPid(), config);
      }
      console.runCommand(
          "catalog:export --provider --force --skip-signature-verification --delete=false --output \"./itest-catalog-entries.zip\" --cql \"\\\"metacard-tags\\\" not like 'geonames'\"");
      LOGGER.info("Feature Count: {}", baseFeatures.size());
      LOGGER.info("Configuration Count: {}", baseConfigurations.size());
    } catch (Exception e) {
      LOGGER.error("Error capturing system configuration.", e);
    }
  }
}
