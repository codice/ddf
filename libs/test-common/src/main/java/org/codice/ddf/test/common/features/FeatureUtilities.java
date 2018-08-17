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
package org.codice.ddf.test.common.features;

import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.platform.util.XMLUtils;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FeatureUtilities {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUtilities.class);

  public static final String FEATURE_NAME_XPATH = "//*[local-name() = 'feature']/@name";

  private BundleService bundleService;

  /**
   * Returns a list of feature names defined in a feature file.
   *
   * @param featureFilePath
   * @return feature names in feature file
   */
  public static List<String> getFeaturesFromFeatureRepo(String featureFilePath) {
    XPath xPath = XPathFactory.newInstance().newXPath();
    List<String> featureNames = new ArrayList<>();

    try (FileInputStream fi = new FileInputStream(new File(featureFilePath))) {
      Document featuresFile = XMLUtils.getInstance().getSecureDocumentBuilder(false).parse(fi);

      NodeList features =
          (NodeList)
              xPath.compile(FEATURE_NAME_XPATH).evaluate(featuresFile, XPathConstants.NODESET);

      for (int i = 0; i < features.getLength(); i++) {
        featureNames.add(features.item(i).getNodeValue());
      }
    } catch (ParserConfigurationException
        | XPathExpressionException
        | IOException
        | SAXException e) {
      throw new RuntimeException(
          "Unable to read features names in feature file at: " + featureFilePath, e);
    }
    return featureNames;
  }

  /**
   * Converts the given feature file into a list of feature name parameters for parameterized
   * testing.
   *
   * @param featureFilePath
   * @return feature name parameters
   */
  public static List<Object[]> featureRepoToFeatureParameters(String featureFilePath) {
    return featureRepoToFeatureParameters(featureFilePath, Collections.emptyList());
  }

  /**
   * Converts the given feature file into a list of feature name parameters for parameterized
   * testing.
   *
   * @param featureFilePath
   * @param ignoredFeatures excludes the specified features from the parameters
   * @return feature name parameters
   */
  public static List<Object[]> featureRepoToFeatureParameters(
      String featureFilePath, List<String> ignoredFeatures) {
    return getFeaturesFromFeatureRepo(featureFilePath)
        .stream()
        .filter(f -> !ignoredFeatures.contains(f))
        .map(feat -> new Object[] {feat})
        .collect(Collectors.toList());
  }

  /**
   * Creates a feature repo object from the specified feature file
   *
   * @param filePath
   * @return
   */
  public static FeatureRepo toFeatureRepo(String filePath) {
    return new FeatureRepoImpl(new UrlProvisionOption("file:" + filePath));
  }

  /**
   * Creates a feature object from the specified feature file
   *
   * @param filePath
   * @return
   */
  public static Feature toFeature(String filePath, String feature) {
    return new FeatureImpl(toFeatureRepo(filePath).getFeatureFileUrl(), feature);
  }

  // DDF-3768 ServiceManager should be moved to test-common and this duplicate code removed.
  public static final long FEATURES_AND_BUNDLES_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

  private static final Map<Integer, String> BUNDLE_STATES =
      new ImmutableMap.Builder<Integer, String>()
          .put(Bundle.UNINSTALLED, "UNINSTALLED")
          .put(Bundle.INSTALLED, "INSTALLED")
          .put(Bundle.RESOLVED, "RESOLVED")
          .put(Bundle.STARTING, "STARTING")
          .put(Bundle.STOPPING, "STOPPING")
          .put(Bundle.ACTIVE, "ACTIVE")
          .build();

  public void printInactiveBundles() {
    printInactiveBundles(LOGGER::error, LOGGER::error);
  }

  public <S> S getService(Class<S> aClass) {
    return getService(getBundleContext().getServiceReference(aClass));
  }

  public <S> S getService(ServiceReference<S> serviceReference) {
    return getBundleContext().getService(serviceReference);
  }

  public <S> ServiceReference<S> getServiceReference(Class<S> aClass) {
    return getBundleContext().getServiceReference(aClass);
  }

  public BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(FeatureUtilities.class);
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
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
              LOGGER.info(
                  "{} bundle not ready yet.\n{}", bundleName, bundleService.getDiag(bundle));
              ready = false;
            }
          } else if (bundleState != null) {
            if (BundleState.Failure.equals(bundleState)) {
              printInactiveBundles();
              fail("The bundle " + bundleName + " failed.");
            } else if (!BundleState.Active.equals(bundleState)) {
              LOGGER.debug(
                  "{} bundle not ready with state {}\n{}",
                  bundleName,
                  bundleState,
                  bundleService.getDiag(bundle));
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
        Thread.sleep(1000);
      }
    }
  }

  /**
   * Uninstalls the specified feature.
   *
   * @param featuresService
   * @param featureName
   * @throws Exception
   */
  public void uninstallFeature(FeaturesService featuresService, String featureName)
      throws Exception {
    long startTime = System.currentTimeMillis();
    LOGGER.info("{} feature uninstalling", featureName);
    featuresService.uninstallFeature(featureName);
    LOGGER.info(
        "{} feature uninstalled in {} ms.", featureName, (System.currentTimeMillis() - startTime));
  }

  /**
   * Installs the specified feature. Waits for all bundles to move into the Active state.
   *
   * @param featuresService
   * @param featureName
   * @throws Exception
   */
  public void installFeature(FeaturesService featuresService, String featureName) throws Exception {
    long startTime = System.currentTimeMillis();
    LOGGER.info("{} feature installing", featureName);
    featuresService.installFeature(featureName);
    waitForRequiredBundles("");
    LOGGER.info(
        "{} feature installed in {} ms.", featureName, (System.currentTimeMillis() - startTime));
  }

  /**
   * Installs and uninstalls the specified feature. Ensures all bundles move into the Active state
   * before uninstalling.
   *
   * @param featuresService
   * @param featureName
   * @throws Exception
   */
  public void installAndUninstallFeature(FeaturesService featuresService, String featureName)
      throws Exception {
    installFeature(featuresService, featureName);
    uninstallFeature(featuresService, featureName);
  }
}
