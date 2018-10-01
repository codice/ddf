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
package org.codice.ddf.itests.kernel;

import static org.awaitility.Awaitility.await;
import static org.codice.ddf.test.common.options.DebugOptions.defaultDebuggingOptions;
import static org.codice.ddf.test.common.options.DistributionOptions.kernelDistributionOption;
import static org.codice.ddf.test.common.options.FeatureOptions.addBootFeature;
import static org.codice.ddf.test.common.options.FeatureOptions.addFeatureRepo;
import static org.codice.ddf.test.common.options.LoggingOptions.defaultLogging;
import static org.codice.ddf.test.common.options.LoggingOptions.logLevelOption;
import static org.codice.ddf.test.common.options.PortOptions.defaultPortsOptions;
import static org.codice.ddf.test.common.options.TestResourcesOptions.getTestResource;
import static org.codice.ddf.test.common.options.TestResourcesOptions.includeTestResources;
import static org.codice.ddf.test.common.options.VmOptions.defaultVmOptions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.karaf.features.FeaturesService;
import org.awaitility.Duration;
import org.codice.ddf.sync.installer.api.SynchronizedInstaller;
import org.codice.ddf.sync.installer.api.SynchronizedInstallerException;
import org.codice.ddf.test.ExampleMSFInstance;
import org.codice.ddf.test.ExampleService;
import org.codice.ddf.test.common.DependencyVersionResolver;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.codice.ddf.test.common.annotations.PaxExamRule;
import org.codice.ddf.test.common.features.FeatureUtilities;
import org.codice.ddf.test.common.features.TestUtilitiesFeatures;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITSynchronizedInstaller {

  private static final String EXAMPLE_FEATURE = "example-feature";

  private static final String EXAMPLE_BUNDLE_SYM_NAME = "example-bundle";

  private static final String TEST_FEATURE_PATH = getTestResource("/test-features.xml");

  @Inject private BundleContext bundleContext;

  @Inject private SynchronizedInstaller syncInstaller;

  @Inject private FeaturesService featuresService;

  @Inject private ConfigurationAdmin configAdmin;

  private static Bundle testBundle;

  @Rule public PaxExamRule paxExamRule = new PaxExamRule(this);

  @Configuration
  public static Option[] examConfiguration() {
    return options(
        kernelDistributionOption(),
        defaultVmOptions(),
        defaultDebuggingOptions(),
        defaultPortsOptions(),
        defaultLogging(),
        logLevelOption("org.codice.ddf.test", "TRACE"),
        logLevelOption("org.codice.ddf.sync.installer.impl", "TRACE"),
        includeTestResources(),
        addFeatureRepo(FeatureUtilities.toFeatureRepo(TEST_FEATURE_PATH)),
        addBootFeature(TestUtilitiesFeatures.testCommon(), TestUtilitiesFeatures.awaitility()));
  }

  @BeforeExam
  public void beforeExam() throws Exception {
    startExampleBundle();
    featuresService.installFeature(EXAMPLE_FEATURE);
    syncInstaller.waitForBundles();
  }

  @After
  public void afterTest() throws Exception {
    startExampleBundle();
    featuresService.installFeature(EXAMPLE_FEATURE);
    resetExampleMSProps();
    deleteAllExampleMSFServices();
  }

  private void startExampleBundle() throws Exception {
    if (testBundle == null) {
      testBundle =
          bundleContext.installBundle(
              maven()
                  .groupId("ddf.test")
                  .artifactId("example-bundle")
                  .version(DependencyVersionResolver.resolver())
                  .getURL());
    }
    testBundle.start();
  }

  @Test
  public void createManagedServiceFactoryWithProps() throws Exception {
    Map<String, Object> props = new HashMap<>();
    props.put(ExampleMSFInstance.EXAMPLE_PROP_NAME, "testValue");

    org.osgi.service.cm.Configuration createdConfig =
        syncInstaller.createManagedFactoryService(
            ExampleMSFInstance.FACTORY_PID, props, getExampleBundleLocation());

    assertThat(getServices(ExampleMSFInstance.class).size(), is(1));
    assertThat(
        getServiceReference(ExampleMSFInstance.class).getProperty(SERVICE_PID),
        is(createdConfig.getPid()));
    assertThat(getService(ExampleMSFInstance.class).getExampleProp(), is("testValue"));
  }

  @Test
  public void createManagedServiceFactoryWithNoProps() throws Exception {
    org.osgi.service.cm.Configuration createdConfig =
        syncInstaller.createManagedFactoryService(
            ExampleMSFInstance.FACTORY_PID, new HashMap<>(), getExampleBundleLocation());

    assertThat(getServices(ExampleMSFInstance.class).size(), is(1));
    assertThat(
        getServiceReference(ExampleMSFInstance.class).getProperty(SERVICE_PID),
        is(createdConfig.getPid()));
    assertThat(
        getService(ExampleMSFInstance.class).getExampleProp(),
        is(ExampleMSFInstance.DEFAULT_EXAMPLE_PROP_VALUE));
  }

  @Test
  public void updatedManagedService() throws Exception {
    Map<String, Object> newProps = new HashMap<>();
    newProps.put(ExampleService.EXAMPLE_PROP_NAME, "testValue");
    syncInstaller.updateManagedService(ExampleService.PID, newProps, getExampleBundleLocation());
    assertThat(
        configAdmin
            .getConfiguration(ExampleService.PID)
            .getProperties()
            .get(ExampleService.EXAMPLE_PROP_NAME),
        is("testValue"));
  }

  @Test
  public void updatedManagedServiceOfManagedServiceFactory() throws Exception {
    org.osgi.service.cm.Configuration createdConfig =
        syncInstaller.createManagedFactoryService(
            ExampleMSFInstance.FACTORY_PID, new Hashtable<>(), getExampleBundleLocation());

    assertThat(getServices(ExampleMSFInstance.class).size(), is(1));
    assertThat(
        getServiceReference(ExampleMSFInstance.class).getProperty(SERVICE_PID),
        is(createdConfig.getPid()));
    assertThat(
        getService(ExampleMSFInstance.class).getExampleProp(),
        is(ExampleMSFInstance.DEFAULT_EXAMPLE_PROP_VALUE));

    Map<String, Object> newProps = new HashMap<>();
    newProps.put(ExampleMSFInstance.EXAMPLE_PROP_NAME, "testValue");
    syncInstaller.updateManagedService(
        createdConfig.getPid(), newProps, getExampleBundleLocation());

    assertThat(getServices(ExampleMSFInstance.class).size(), is(1));
    assertThat(
        getServiceReference(ExampleMSFInstance.class).getProperty(SERVICE_PID),
        is(createdConfig.getPid()));
    assertThat(
        configAdmin
            .getConfiguration(createdConfig.getPid())
            .getProperties()
            .get(ExampleMSFInstance.EXAMPLE_PROP_NAME),
        is("testValue"));
  }

  @Test
  public void waitForServiceAvailability() {
    ServiceRegistration<ExampleService> reg = null;
    Dictionary<String, String> props = new Hashtable<>();
    props.put(SERVICE_PID, "pax.exam.test.service");

    Runnable runnable =
        () -> {
          try {
            syncInstaller.waitForServiceToBeAvailable("pax.exam.test.service");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };
    Future future = Executors.newSingleThreadExecutor().submit(runnable);

    try {
      reg = bundleContext.registerService(ExampleService.class, new ExampleService(null), props);
      await().atMost(Duration.ONE_MINUTE).until(future::isDone);
    } finally {
      reg.unregister();
    }
  }

  @Test
  public void installFeatures() throws Exception {
    featuresService.uninstallFeature(EXAMPLE_FEATURE);
    syncInstaller.installFeatures(EXAMPLE_FEATURE);
    assertThat(featuresService.isInstalled(featuresService.getFeature(EXAMPLE_FEATURE)), is(true));
  }

  @Test
  public void uninstallFeatures() throws Exception {
    syncInstaller.uninstallFeatures(EXAMPLE_FEATURE);
    assertThat(!featuresService.isInstalled(featuresService.getFeature(EXAMPLE_FEATURE)), is(true));
  }

  @Test
  public void waitForFeatures() throws Exception {
    testBundle.stop();

    Runnable runnable =
        () -> {
          try {
            syncInstaller.installFeatures(EXAMPLE_FEATURE);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };

    Future future = Executors.newSingleThreadExecutor().submit(runnable);
    testBundle.start();
    await().atMost(Duration.ONE_MINUTE).until(future::isDone);
  }

  @Test
  public void stopBundles() throws SynchronizedInstallerException {
    syncInstaller.stopBundles(EXAMPLE_BUNDLE_SYM_NAME);
    assertThat(testBundle.getState(), is(Bundle.RESOLVED));
  }

  @Test
  public void startBundles() throws Exception {
    testBundle.stop();
    syncInstaller.startBundles(EXAMPLE_BUNDLE_SYM_NAME);
    assertThat(testBundle.getState(), is(Bundle.ACTIVE));
  }

  @Test
  public void waitForBundles() throws Exception {
    testBundle.stop();

    Runnable runnable =
        () -> {
          try {
            syncInstaller.waitForBundles();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };

    Future future = Executors.newSingleThreadExecutor().submit(runnable);
    testBundle.start();
    await().atMost(Duration.ONE_MINUTE).until(future::isDone);
  }

  private void deleteAllExampleMSFServices() throws IOException, InvalidSyntaxException {
    configurations(
            String.format(
                "(%s=%s)", ConfigurationAdmin.SERVICE_FACTORYPID, ExampleMSFInstance.FACTORY_PID))
        .forEach(this::deleteServiceConfig);
    await()
        .atMost(Duration.ONE_MINUTE)
        .until(() -> getServices(ExampleMSFInstance.class).isEmpty());
  }

  private void deleteServiceConfig(org.osgi.service.cm.Configuration config) {
    try {
      config.delete();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void resetExampleMSProps() throws IOException {
    Dictionary<String, Object> defaultProps = new Hashtable<>();
    defaultProps.put(ExampleService.EXAMPLE_PROP_NAME, ExampleService.DEFAULT_EXAMPLE_PROP_VALUE);
    configAdmin.getConfiguration(ExampleService.PID).update(defaultProps);
    await()
        .atMost(Duration.ONE_MINUTE)
        .until(
            () ->
                getService(ExampleService.class)
                    .getExampleProp()
                    .equals(ExampleService.DEFAULT_EXAMPLE_PROP_VALUE));
  }

  private Stream<org.osgi.service.cm.Configuration> configurations(String filter)
      throws IOException, InvalidSyntaxException {
    org.osgi.service.cm.Configuration[] configs = configAdmin.listConfigurations(filter);
    return configs == null ? Stream.empty() : Stream.of(configs);
  }

  private String getExampleBundleLocation() {
    return getService(ExampleService.class).getBundleLocation();
  }

  private <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
    return bundleContext.getServiceReference(clazz);
  }

  private <S> S getService(Class<S> clazz) {
    return bundleContext.getService(getServiceReference(clazz));
  }

  private <S> List<S> getServices(Class<S> clazz) throws InvalidSyntaxException {
    return bundleContext
        .getServiceReferences(clazz, null)
        .stream()
        .map(bundleContext::getService)
        .collect(Collectors.toList());
  }
}
