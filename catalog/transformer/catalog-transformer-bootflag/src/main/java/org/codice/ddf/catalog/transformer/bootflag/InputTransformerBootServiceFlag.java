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
package org.codice.ddf.catalog.transformer.bootflag;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.transform.InputTransformer;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.platform.bootflag.BootServiceFlag;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Waits for configured {@link InputTransformer}s. This check is only done once initially on system
 * startup. Once this service is available in the OSGi registry with the service property {@code id}
 * equal to {@code inputTransformerServiceFlag}, it indicates that all the {@link InputTransformer}s
 * are available.
 *
 * <p>If an {@link InputTransformer} that was initially waited for becomes no longer available in
 * the system, and this service is already published, the service will not be unpublished.
 */
public class InputTransformerBootServiceFlag implements BootServiceFlag {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(InputTransformerBootServiceFlag.class);

  private static final long DEFAULT_TRANSFORMER_WAIT_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(20);

  private static final long DEFAULT_TRANSFORMER_CHECK_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);

  private static final String TRANSFORMER_ID_PROPERTY = "id";

  private ExecutorService executorService;

  private final long transformerWaitTimeoutMillis;

  private final long transformerCheckPeriodMillis;

  private final InputTransformerIds inputTransformerIds;

  private final List<ServiceReference<InputTransformer>> inputTransformers;

  /**
   * Creates a new {@code InputTransformerBootServiceFlag} and begins waiting for the list of
   * configured {@link InputTransformer}s.
   *
   * @param inputTransformerIds list of {@link InputTransformer} ids to wait for
   * @param inputTransformers {@link InputTransformer}s in the OSGi service registry
   */
  public InputTransformerBootServiceFlag(
      InputTransformerIds inputTransformerIds,
      List<ServiceReference<InputTransformer>> inputTransformers) {
    this.inputTransformerIds = inputTransformerIds;
    this.inputTransformers = inputTransformers;
    this.executorService = Executors.newSingleThreadExecutor();
    this.transformerCheckPeriodMillis = DEFAULT_TRANSFORMER_CHECK_PERIOD_MILLIS;
    this.transformerWaitTimeoutMillis = DEFAULT_TRANSFORMER_WAIT_TIMEOUT_MILLIS;
    executorService.submit(
        () -> waitForInputTransformers(FrameworkUtil.getBundle(this.getClass())));
  }

  @VisibleForTesting
  InputTransformerBootServiceFlag(
      InputTransformerIds inputTransformerIds,
      List<ServiceReference<InputTransformer>> inputTransformers,
      Bundle bundle,
      long transformerCheckPeriodMillis,
      long transformerWaitTimeoutMillis) {
    this.inputTransformerIds = inputTransformerIds;
    this.inputTransformers = inputTransformers;
    this.transformerCheckPeriodMillis = transformerCheckPeriodMillis;
    this.transformerWaitTimeoutMillis = transformerWaitTimeoutMillis;
    waitForInputTransformers(bundle);
  }

  /**
   * Waits up to a configured ({@code org.codice.ddf.cdm.transformerWaitTimeoutSeconds} system
   * property) amount of time for the configured {@link InputTransformer} services to be available.
   */
  @SuppressWarnings({
    "squid:S1481" /* Sonar thinks variables are unused */,
    "squid:S1854" /* Useless assignment for variable Sonar thinks is unused */
  })
  private void waitForInputTransformers(Bundle bundle) {
    Set<String> configuredInputTransformers = inputTransformerIds.getIds();
    if (configuredInputTransformers.isEmpty()) {
      publishService(bundle);
      destroy();
      return;
    }

    Set<String> requiredInputTransformers = new HashSet<>(configuredInputTransformers);
    LOGGER.trace(
        "Beginning to wait for InputTransformers for {} seconds",
        TimeUnit.MILLISECONDS.toSeconds(transformerWaitTimeoutMillis));

    Failsafe.with(
            new RetryPolicy()
                .withMaxDuration(transformerWaitTimeoutMillis, TimeUnit.MILLISECONDS)
                .retryWhen(false)
                .withDelay(transformerCheckPeriodMillis, TimeUnit.MILLISECONDS))
        .withFallback(
            () -> {
              throw new IllegalArgumentException(
                  String.format(
                      "Not all expected Input Transformers were found within %s seconds. Expected: %s, but didn't find: %s. "
                          + "CDMs will not be able to start without all required InputTransformers. Either remove the un-found "
                          + "IDs from the InputTransformer ID JSON files under %s, or ensure the bundles providing "
                          + "the missing InputTransformers are started.",
                      TimeUnit.MILLISECONDS.toSeconds(transformerWaitTimeoutMillis),
                      configuredInputTransformers,
                      requiredInputTransformers,
                      inputTransformerIds.getTransformerPath()));
            })
        .get(() -> checkForInputTransformers(requiredInputTransformers));
    publishService(bundle);
    destroy();
  }

  private boolean checkForInputTransformers(Set<String> requiredInputTransformers) {
    for (ServiceReference serviceReference : inputTransformers) {
      Object propertyValue = serviceReference.getProperty(TRANSFORMER_ID_PROPERTY);
      if (propertyValue instanceof String) {
        requiredInputTransformers.remove(propertyValue);
        LOGGER.debug("Remaining property values {}", requiredInputTransformers);
      }
    }

    if (requiredInputTransformers.isEmpty()) {
      LOGGER.trace("Finished finding all InputTransformers");
      return true;
    }

    return false;
  }

  public void destroy() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  private void publishService(Bundle bundle) {
    BundleContext bundleContext = bundle.getBundleContext();
    if (bundleContext == null) {
      String name = bundle.getSymbolicName();
      LOGGER.debug("Failed to get the bundle context for bundle {}", name);
      throw new IllegalStateException(String.format("No bundle context available for %s", name));
    }

    Dictionary<String, String> props = new DictionaryMap<>();
    props.put("id", "inputTransformerBootFlag");
    bundleContext.registerService(BootServiceFlag.class, this, props);
  }
}
