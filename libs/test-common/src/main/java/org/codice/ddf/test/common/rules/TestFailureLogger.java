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
package org.codice.ddf.test.common.rules;

import com.google.common.collect.ImmutableMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test rule used to log the state of the OSGi bundles after a test failure. The state of all
 * bundles will be logged if the log level for this class is set to DEBUG, otherwise only the list
 * of inactive bundles will be logged.
 */
public class TestFailureLogger extends TestWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestFailureLogger.class);

  private static final String[] BUNDLE_LOG_TITLES = new String[] {"ID", "State", "Version", "Name"};

  private static final String BUNDLE_LOG_FORMAT = "%3s | %12s | %20s | %s";

  private static final Map<Integer, String> BUNDLE_STATES =
      new ImmutableMap.Builder<Integer, String>()
          .put(Bundle.UNINSTALLED, "UNINSTALLED")
          .put(Bundle.INSTALLED, "INSTALLED")
          .put(Bundle.RESOLVED, "RESOLVED")
          .put(Bundle.STARTING, "STARTING")
          .put(Bundle.STOPPING, "STOPPING")
          .put(Bundle.ACTIVE, "ACTIVE")
          .build();

  @Override
  @SuppressWarnings(
      "squid:S2629") // Don't need to check for argument evaluation before logging errors
  protected void failed(Throwable e, Description description) {
    BundleContext bundleContext =
        FrameworkUtil.getBundle(description.getTestClass()).getBundleContext();
    BundleDiagnostics bundleDiagnostics = getBundleDiagnostics(bundleContext);

    LOGGER.error(
        "Test {} failed with exception {} - {}",
        description.getDisplayName(),
        e.getClass().getSimpleName(),
        e.getMessage());

    e.printStackTrace(new PrintWriter(new StringWriter()));
    LOGGER.error(e.toString());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Listing all bundles");
      LOGGER.debug(String.format(BUNDLE_LOG_FORMAT, (Object[]) BUNDLE_LOG_TITLES));

      logBundleInfo(bundleDiagnostics, Arrays.asList(bundleContext.getBundles()), LOGGER::debug);
    } else {
      List<Bundle> bundles =
          Arrays.stream(bundleContext.getBundles())
              .filter(bundleDiagnostics::isNotActive)
              .filter(bundleDiagnostics::isNotFragment)
              .collect(Collectors.toList());

      if (!bundles.isEmpty()) {
        LOGGER.error("Listing inactive bundles");
        LOGGER.error(String.format(BUNDLE_LOG_FORMAT, (Object[]) BUNDLE_LOG_TITLES));
        logBundleInfo(bundleDiagnostics, bundles, LOGGER::error);
      }
    }
  }

  private static BundleDiagnostics getBundleDiagnostics(BundleContext bundleContext) {
    ServiceReference<BundleService> bundleServiceReference =
        bundleContext.getServiceReference(BundleService.class);

    if (bundleServiceReference == null) {
      return new PlainBundleDiagnostics();
    } else {
      return new KarafBundleDiagnostics(bundleContext.getService(bundleServiceReference));
    }
  }

  private void logBundleInfo(
      BundleDiagnostics bundleDiagnostics, List<Bundle> bundles, Consumer<String> logger) {

    for (Bundle bundle : bundles) {
      StringBuilder bundleInfo = new StringBuilder();

      bundleInfo.append(
          String.format(
              BUNDLE_LOG_FORMAT,
              bundle.getBundleId(),
              bundleDiagnostics.getBundleState(bundle),
              bundle.getVersion().toString(),
              bundle.getSymbolicName()));

      if (bundleDiagnostics.isNotActive(bundle)) {
        bundleInfo.append(bundleDiagnostics.getDetails(bundle));
      }

      logger.accept(bundleInfo.toString());
    }
  }

  private abstract static class BundleDiagnostics {
    abstract String getBundleState(Bundle bundle);

    abstract boolean isNotActive(Bundle bundle);

    abstract boolean isNotFragment(Bundle bundle);

    StringBuilder getDetails(Bundle bundle) {
      StringBuilder details = new StringBuilder(" [ ");
      Dictionary<String, String> headers = bundle.getHeaders();
      Enumeration<String> keys = headers.keys();

      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        details.append(key).append("=").append(headers.get(key)).append(", ");
      }

      details.append(" ]");
      return details;
    }
  }

  private static class PlainBundleDiagnostics extends BundleDiagnostics {

    @Override
    String getBundleState(Bundle bundle) {
      return BUNDLE_STATES.getOrDefault(bundle.getState(), "UNKNOWN");
    }

    @Override
    boolean isNotActive(Bundle bundle) {
      return bundle.getState() != Bundle.ACTIVE;
    }

    @Override
    boolean isNotFragment(Bundle bundle) {
      return bundle.getHeaders().get("Fragment-Host") == null;
    }
  }

  private static class KarafBundleDiagnostics extends BundleDiagnostics {
    private final BundleService bundleService;

    KarafBundleDiagnostics(BundleService bundleService) {
      this.bundleService = bundleService;
    }

    @Override
    public String getBundleState(Bundle bundle) {
      return bundleService.getInfo(bundle).getState().toString();
    }

    @Override
    public boolean isNotActive(Bundle bundle) {
      return bundleService.getInfo(bundle).getState() != BundleState.Active;
    }

    @Override
    public boolean isNotFragment(Bundle bundle) {
      return !bundleService.getInfo(bundle).isFragment();
    }

    @Override
    public StringBuilder getDetails(Bundle bundle) {
      StringBuilder details = super.getDetails(bundle);
      details.append('\n');
      details.append(bundleService.getDiag(bundle));

      return details;
    }
  }
}
