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
package org.codice.ddf.admin.application.service.impl;

import static org.codice.ddf.test.mockito.PrivilegedVerificationMode.privileged;
import static org.codice.ddf.test.mockito.StackContainsDoPrivilegedCalls.stackContainsDoPrivilegedCall;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import java.lang.management.ManagementFactory;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.commons.collections.ListUtils;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.admin.application.plugin.ApplicationPlugin;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.admin.core.impl.ServiceImpl;
import org.codice.ddf.sync.installer.api.SynchronizedInstaller;
import org.codice.ddf.test.mockito.StackCaptor;
import org.hamcrest.Matchers;
import org.hamcrest.io.FileMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationServiceBeanTest {
  private static final String TEST_FEATURE_DESCRIPTION =
      "Mock Feature for ApplicationServiceBean tests";

  private static final String TEST_FEATURE_DETAILS = "TestFeatureDetails";

  private static final String TEST_FEATURE_STATUS = "TestStatus";

  private static final String TEST_APP_NAME = "TestApp";

  private static final String TEST_VERSION = "0.0.0";

  private static final String TEST_APP_DESCRIP = "Test app for testGetApplicationTree";

  private static final String TEST_FEATURE_NAME = "TestFeature";

  private static final String TEST_LOCATION = "TestLocation";

  private static final String TEST_REPO_NAME = "TestRepo";

  private static final String DO_PRIVILEGED_STACK_ELEMENT =
      "java.security.AccessController.doPrivileged";

  private static final String WRAPPER_KEY = "wrapper.key";

  private static final String RESTART_JVM = "karaf.restart.jvm";

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private ApplicationService testAppService;

  private ConfigurationAdmin testConfigAdminExt;

  private Application testApp;

  private SynchronizedInstaller mockSyncInstaller;

  private SystemService mockSystemService;

  private BundleContext bundleContext;

  private MBeanServer mBeanServer;

  private ObjectName objectName;

  private WrapperManagerMXBean mockWrapperManager;

  private Path ddfBin;

  @Before
  public void setUp() throws Exception {
    testAppService = mock(ApplicationServiceImpl.class);
    testConfigAdminExt = mock(ConfigurationAdmin.class);
    testApp = mock(ApplicationImpl.class);
    mockSyncInstaller = mock(SynchronizedInstaller.class);
    mockSystemService = mock(SystemService.class);

    when(testApp.getName()).thenReturn(TEST_APP_NAME);
    when(testApp.getDescription()).thenReturn(TEST_APP_DESCRIP);
    bundleContext = mock(BundleContext.class);
    mBeanServer = mock(MBeanServer.class);
    objectName =
        new ObjectName(ApplicationService.class.getName() + ":service=application-service");
    mockWrapperManager = mock(WrapperManagerMXBean.class);
    ManagementFactory.getPlatformMBeanServer()
        .registerMBean(
            mockWrapperManager, new ObjectName("org.tanukisoftware.wrapper:type=WrapperManager"));
    doNothing().when(mockWrapperManager).restart();
    System.setProperty(RESTART_JVM, "true");
    System.setProperty("ddf.home", testFolder.getRoot().toString());
    ddfBin = testFolder.newFolder("bin").toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
  }

  @After
  public void tearDown() throws Exception {
    System.getProperties().remove(RESTART_JVM);
    System.getProperties().remove(WRAPPER_KEY);
    ManagementFactory.getPlatformMBeanServer()
        .unregisterMBean(new ObjectName("org.tanukisoftware.wrapper:type=WrapperManager"));
  }

  /**
   * Tests the {@link ApplicationServiceBean#init()} method
   *
   * @throws Exception
   */
  @Test
  public void testInit() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    serviceBean.init();

    verify(mBeanServer).registerMBean(serviceBean, objectName);
  }

  /**
   * Tests the {@link ApplicationServiceBean#init()} method for the case where the serviceBean has
   * already been initialized
   *
   * @throws Exception
   */
  @Test
  public void testInitTwice() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    when(mBeanServer.registerMBean(any(Object.class), any(ObjectName.class)))
        .thenThrow(new InstanceAlreadyExistsException())
        .thenReturn(null);

    serviceBean.init();

    verify(mBeanServer, atMost(1)).unregisterMBean(objectName);
    verify(mBeanServer, times(2)).registerMBean(serviceBean, objectName);
  }

  /**
   * Tests the {@link ApplicationServiceBean#init()} method for the case where an exception other
   * than the InstanceAlreadyExistsException is thrown by mBeanServer.registerMBean(....)
   *
   * @throws Exception
   */
  @Test(expected = ApplicationServiceException.class)
  public void testInitWhenRegisterMBeanThrowsInstanceAlreadyExistsException() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    when(mBeanServer.registerMBean(any(Object.class), any(ObjectName.class)))
        .thenThrow(new NullPointerException());

    serviceBean.init();
  }

  /**
   * Tests the {@link ApplicationServiceBean#destroy()} method
   *
   * @throws Exception
   */
  @Test
  public void testDestroy() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    serviceBean.destroy();

    verify(mBeanServer).unregisterMBean(objectName);
  }

  /**
   * Tests the {@link ApplicationServiceBean#destroy()} method for the case where an
   * InstanceNotFoundException is thrown by mBeanServer.unregisterMBean(...)
   *
   * @throws Exception
   */
  @Test(expected = ApplicationServiceException.class)
  public void testDestroyWhenUnregisterMBeanThrowsInstanceNotFoundException() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    doThrow(new InstanceNotFoundException()).when(mBeanServer).unregisterMBean(objectName);

    serviceBean.destroy();
  }

  /**
   * Tests the {@link ApplicationServiceBean#destroy()} method for the case where an
   * MBeanRegistrationException is thrown by mBeanServer.unregisterMBean(...)
   *
   * @throws Exception
   */
  @Test(expected = ApplicationServiceException.class)
  public void testDestroyWhenUnregisterMBeanThrowsMBeanRegistrationException() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    doThrow(new MBeanRegistrationException(new Exception()))
        .when(mBeanServer)
        .unregisterMBean(any(ObjectName.class));

    serviceBean.destroy();
  }

  @Test
  public void testInstallProfile() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    serviceBean.installFeature("profile-name");

    verify(mockSyncInstaller).installFeatures(Matchers.any(EnumSet.class), eq("profile-name"));
  }

  @Test
  public void testInstallFeatureCallIsPrivileged() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    serviceBean.installFeature("profile-name");

    verify(mockSyncInstaller, privileged(times(1)))
        .installFeatures(Matchers.any(EnumSet.class), eq("profile-name"));
  }

  @Test
  public void testUninstallFeatureCallIsPrivileged() throws Exception {
    StackCaptor stackCaptor = new StackCaptor();

    stackCaptor
        .doCaptureStack()
        .when(mockSyncInstaller)
        .uninstallFeatures(Matchers.any(EnumSet.class), anyString());

    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    serviceBean.uninstallFeature("profile-name");

    assertThat(stackCaptor.getStack(), stackContainsDoPrivilegedCall());
  }

  /**
   * Tests the {@link ApplicationServiceBean#getInstallationProfiles()} method
   *
   * @throws Exception
   */
  @Test
  public void testGetInstallationProfiles() throws Exception {
    Feature testFeature1 = mock(Feature.class);
    Feature testFeature2 = mock(Feature.class);
    Dependency testDependency1 = mock(Dependency.class);
    Dependency testDependency2 = mock(Dependency.class);

    when(testFeature1.getName()).thenReturn(TEST_FEATURE_NAME);
    when(testFeature2.getName()).thenReturn(TEST_FEATURE_NAME);
    when(testDependency1.getName()).thenReturn(TEST_FEATURE_NAME);
    when(testDependency2.getName()).thenReturn(TEST_FEATURE_NAME);
    when(testFeature1.getDescription()).thenReturn(TEST_FEATURE_DESCRIPTION);
    when(testFeature2.getDescription()).thenReturn(TEST_FEATURE_DESCRIPTION);

    List<Dependency> dependencies1 = new ArrayList<>();
    dependencies1.add(testDependency1);
    List<Dependency> dependencies2 = new ArrayList<>();
    dependencies2.add(testDependency2);

    when(testFeature1.getDependencies()).thenReturn(dependencies1);
    when(testFeature2.getDependencies()).thenReturn(dependencies2);

    List<Feature> featureList = new ArrayList<>();
    featureList.add(testFeature1);
    featureList.add(testFeature2);
    when(testAppService.getInstallationProfiles()).thenReturn(featureList);

    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    List<Map<String, Object>> result = serviceBean.getInstallationProfiles();

    assertThat(
        "Should contain the nodes set up previously.",
        (String) result.get(0).get("name"),
        is(TEST_FEATURE_NAME));
    assertThat("Should have two entries.", result.size(), is(2));
  }

  /**
   * Tests the {@link ApplicationServiceBean#getServices(String)} method
   *
   * @throws Exception
   */
  @Test
  public void testGetServices() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    Bundle testBundle = mock(Bundle.class);
    Bundle[] bundles = {testBundle};
    when(bundleContext.getBundles()).thenReturn(bundles);

    List<Service> services = new ArrayList<>();
    Service testService1 = new ServiceImpl();
    List<Map<String, Object>> testService1Configs = new ArrayList<>();
    Map<String, Object> testConfig1 = new HashMap<>();
    testConfig1.put("bundle_location", TEST_LOCATION);
    testService1Configs.add(testConfig1);
    services.add(testService1);
    testService1.put("configurations", testService1Configs);

    BundleInfo testBundle1 = mock(BundleInfo.class);
    Set<BundleInfo> testBundles = new HashSet<>();
    testBundles.add(testBundle1);

    when(testApp.getBundles()).thenReturn(testBundles);
    when(testBundle1.getLocation()).thenReturn(TEST_LOCATION);
    when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
    when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(services);

    assertThat(
        "Should find the given services.",
        serviceBean.getServices(TEST_APP_NAME).get(0),
        is(testService1));
  }

  /**
   * Tests the {@link ApplicationServiceBean#getServices(String)} method for the case where the
   * services do not have the "configurations" key
   *
   * @throws Exception
   */
  @Test
  public void testGetServicesNotContainsKey() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    Bundle testBundle = mock(Bundle.class);
    Bundle[] bundles = {testBundle};
    when(bundleContext.getBundles()).thenReturn(bundles);

    List<Service> services = new ArrayList<>();
    Service testService2 = mock(Service.class);
    Service testService1 = mock(Service.class);
    services.add(testService1);
    services.add(testService2);
    when(testService1.get("factory")).thenReturn(true);
    when(testService2.get("factory")).thenReturn(false);

    BundleInfo testBundle1 = mock(BundleInfo.class);
    Set<BundleInfo> testBundles = new HashSet<>();
    testBundles.add(testBundle1);

    when(testApp.getBundles()).thenReturn(testBundles);
    when(testBundle1.getLocation()).thenReturn(TEST_LOCATION);
    when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
    when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(services);

    assertThat(
        "Should not find any services.",
        serviceBean.getServices(TEST_APP_NAME),
        is(ListUtils.EMPTY_LIST));
  }

  /**
   * Tests the {@link ApplicationServiceBean#getServices(String)} method for the case where the
   * services do not have the "configurations" key and there is MetatypeInformation present for each
   * service.
   *
   * <p>This test mostly just checks that
   *
   * @throws Exception
   */
  @Test
  public void testGetServicesMetatypeInfo() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    ServiceTracker testServiceTracker = mock(ServiceTracker.class);
    serviceBean.setServiceTracker(testServiceTracker);
    MetaTypeService testMTS = mock(MetaTypeService.class);
    MetaTypeInformation testMTI = mock(MetaTypeInformation.class);
    when(testServiceTracker.getService()).thenReturn(testMTS);
    when(testMTS.getMetaTypeInformation(any(Bundle.class))).thenReturn(testMTI);
    when(testMTI.getPids()).thenReturn(new String[] {"001", "002"});
    when(testMTI.getFactoryPids()).thenReturn(new String[] {"001", "002"});

    Bundle testBundle = mock(Bundle.class);
    Bundle[] bundles = {testBundle};
    when(bundleContext.getBundles()).thenReturn(bundles);
    when(testBundle.getLocation()).thenReturn(TEST_LOCATION);

    List<Service> services = new ArrayList<>();
    Service testService2 = mock(Service.class);
    Service testService1 = mock(Service.class);
    services.add(testService1);
    services.add(testService2);

    List<Map<String, Object>> testService1Configs = new ArrayList<>();
    Map<String, Object> testConfig1 = new HashMap<>();
    testConfig1.put("bundle_location", TEST_LOCATION);
    testService1Configs.add(testConfig1);

    List<Map<String, Object>> testService2Configs = new ArrayList<>();
    Map<String, Object> testConfig2 = new HashMap<>();
    testConfig2.put("bundle_location", TEST_LOCATION);
    testService1Configs.add(testConfig2);

    when(testService1.get("factory")).thenReturn(true);
    when(testService2.get("factory")).thenReturn(false);
    when(testService1.get("configurations")).thenReturn(testService1Configs);
    when(testService2.get("configurations")).thenReturn(testService2Configs);
    when(testService1.get("id")).thenReturn("001");
    when(testService2.get("id")).thenReturn("002");

    BundleInfo testBundle1 = mock(BundleInfo.class);
    Set<BundleInfo> testBundles = new HashSet<>();
    testBundles.add(testBundle1);

    when(testApp.getBundles()).thenReturn(testBundles);
    when(testBundle1.getLocation()).thenReturn(TEST_LOCATION);
    when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
    when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(services);

    assertThat(
        "Should find the given services.",
        serviceBean.getServices(TEST_APP_NAME).get(0),
        is(testService1));
  }

  /**
   * Tests the {@link ApplicationServiceBean#getServices(String)} method for the case where an
   * ApplicationServiceException is thrown
   *
   * @throws Exception
   */
  // TODO RAP 29 Aug 16: DDF-2443 - Fix test to not depend on specific log output
  @Test
  public void testGetServicesASE() throws Exception {
    ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final Appender mockAppender = mock(Appender.class);
    when(mockAppender.getName()).thenReturn("MOCK");
    root.addAppender(mockAppender);
    root.setLevel(Level.ALL);

    ApplicationServiceBean serviceBean = newApplicationServiceBean();

    Bundle testBundle = mock(Bundle.class);
    Bundle[] bundles = {testBundle};
    when(bundleContext.getBundles()).thenReturn(bundles);

    List<Service> services = new ArrayList<>();
    Service testService1 = new ServiceImpl();
    services.add(testService1);

    when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
    when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(services);

    serviceBean.getServices(TEST_APP_NAME);
  }

  /**
   * Tests the {@link ApplicationServiceBean#getApplicationPlugins()} method and the {@link
   * ApplicationServiceBean#setApplicationPlugins(List)} method
   *
   * @throws Exception
   */
  @Test
  public void testGetSetApplicationPlugins() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    ApplicationPlugin testPlugin1 = mock(ApplicationPlugin.class);
    ApplicationPlugin testPlugin2 = mock(ApplicationPlugin.class);
    List<ApplicationPlugin> pluginList = new ArrayList<>();
    pluginList.add(testPlugin1);
    pluginList.add(testPlugin2);

    serviceBean.setApplicationPlugins(pluginList);

    assertEquals(pluginList, serviceBean.getApplicationPlugins());
  }

  /**
   * Tests the {@link ApplicationServiceBean#getAllFeatures()} method
   *
   * @throws Exception
   */
  @Test
  public void testGetAllFeatures() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    List<FeatureDetails> testFeatureDetailsList = new ArrayList<>();
    FeatureDetails testFeatureDetails1 = mock(FeatureDetails.class);
    testFeatureDetailsList.add(testFeatureDetails1);
    when(testFeatureDetails1.getName()).thenReturn(TEST_FEATURE_DETAILS);
    when(testFeatureDetails1.getVersion()).thenReturn(TEST_VERSION);
    when(testFeatureDetails1.getStatus()).thenReturn(TEST_FEATURE_STATUS);
    when(testFeatureDetails1.getRepository()).thenReturn(TEST_REPO_NAME);

    when(testAppService.getAllFeatures()).thenReturn(testFeatureDetailsList);

    assertThat(
        "Features returned should match testFeatureDetailsList features",
        (String) serviceBean.getAllFeatures().get(0).get("name"),
        is(testFeatureDetailsList.get(0).getName()));
    verify(testAppService).getAllFeatures();
  }

  /**
   * Tests the {@link ApplicationServiceBean#getPluginsForApplication(String)} method
   *
   * @throws Exception
   */
  @Test
  public void testGetPluginsForApplication() throws Exception {
    ApplicationServiceBean serviceBean = newApplicationServiceBean();
    ApplicationPlugin testPlugin1 = mock(ApplicationPlugin.class);
    ApplicationPlugin testPlugin2 = mock(ApplicationPlugin.class);
    List<ApplicationPlugin> pluginList = new ArrayList<>();
    pluginList.add(testPlugin1);
    pluginList.add(testPlugin2);
    Map<String, Object> plugin1JSON = new HashMap<>();
    plugin1JSON.put("TestAppJSON", "Plugin1");
    Map<String, Object> plugin2JSON = new HashMap<>();
    plugin2JSON.put("TestAppJSON", "Plugin2");

    when(testPlugin1.matchesAssocationName(TEST_APP_NAME)).thenReturn(true);
    when(testPlugin2.matchesAssocationName(TEST_APP_NAME)).thenReturn(true);
    when(testPlugin1.toJSON()).thenReturn(plugin1JSON);
    when(testPlugin2.toJSON()).thenReturn(plugin2JSON);

    serviceBean.setApplicationPlugins(pluginList);

    assertThat(
        "Should return the list of plugins given to it.",
        serviceBean.getPluginsForApplication(TEST_APP_NAME).get(0),
        is(plugin1JSON));
  }

  @Test
  public void testRestartWithKarafRestart() throws Exception {
    final ApplicationServiceBean serviceBean = newApplicationServiceBean();

    serviceBean.restart();

    assertThat(
        "Restart system property was cleared", System.getProperty(RESTART_JVM), equalTo("false"));
    assertThat(ddfBin.resolve("restart.jvm").toFile(), FileMatchers.anExistingFile());
    verify(mockSystemService).halt();
    verify(mockWrapperManager, Mockito.never()).restart();
  }

  @Test
  public void testRestartWithWrapperRestart() throws Exception {
    final ApplicationServiceBean serviceBean = newApplicationServiceBean();

    System.setProperty(WRAPPER_KEY, "abc");

    serviceBean.restart();

    assertThat(
        "Restart system property was not cleared",
        System.getProperty(RESTART_JVM),
        equalTo("true"));
    assertThat(ddfBin.resolve("restart.jvm").toFile(), Matchers.not(FileMatchers.anExistingFile()));
    verify(mockSystemService, Mockito.never()).halt();
    verify(mockWrapperManager).restart();
  }

  private ApplicationServiceBean newApplicationServiceBean() throws ApplicationServiceException {
    return new ApplicationServiceBean(
        testAppService, testConfigAdminExt, mBeanServer, mockFeaturesService, mockSystemService) {
      @Override
      protected BundleContext getContext() {
        return bundleContext;
      }
    };
  }

  public static interface WrapperManagerMXBean {

    public void restart() throws MBeanException;
  }
}
